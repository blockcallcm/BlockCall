package com.blockcall.ui

import android.content.Intent
import android.provider.ContactsContract.CommonDataKinds.Phone
import android.text.format.DateUtils
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Contacts
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.GppGood
import androidx.compose.material.icons.rounded.PhoneDisabled
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PersonAddAlt
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.blockcall.Store

@Composable
fun AllowedScreen(store: Store, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val allowed by store.allowed.collectAsStateWithLifecycle()
    var adding by remember { mutableStateOf(false) }

    // El selector del sistema devuelve el teléfono elegido; leemos número y nombre.
    val pickContact = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val uri = result.data?.data ?: return@rememberLauncherForActivityResult
        context.contentResolver.query(uri, arrayOf(Phone.NUMBER, Phone.DISPLAY_NAME), null, null, null)?.use {
            if (it.moveToFirst()) store.addAllowed(it.getString(0).orEmpty(), it.getString(1).orEmpty())
        }
    }

    Box(modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            Header("Permitidos", "Números que siempre pueden llamarte")
            if (allowed.isEmpty()) {
                EmptyState(Icons.Rounded.GppGood, "Tu lista está vacía", "Agrega números de confianza a mano\no elígelos desde tus contactos.")
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 168.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(allowed, key = { it.number }) { item ->
                        Row(itemModifier(), verticalAlignment = Alignment.CenterVertically) {
                            Avatar(item.name)
                            Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
                                Text(item.name.ifBlank { item.number }, fontWeight = FontWeight.SemiBold)
                                if (item.name.isNotBlank()) {
                                    Text(item.number, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            IconButton(onClick = { store.removeAllowed(item.number) }) {
                                Icon(Icons.Rounded.DeleteOutline, contentDescription = "Quitar")
                            }
                        }
                    }
                }
            }
        }
        Column(
            Modifier.align(Alignment.BottomEnd).padding(20.dp),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ExtendedFloatingActionButton(
                onClick = { pickContact.launch(Intent(Intent.ACTION_PICK, Phone.CONTENT_URI)) },
                icon = { Icon(Icons.Rounded.Contacts, contentDescription = null) },
                text = { Text("Desde contactos") },
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
            )
            ExtendedFloatingActionButton(
                onClick = { adding = true },
                icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                text = { Text("Agregar número") },
            )
        }
    }

    if (adding) {
        AddNumberDialog(
            onDismiss = { adding = false },
            onConfirm = { number, name ->
                store.addAllowed(number, name)
                adding = false
            },
        )
    }
}

@Composable
fun BlockedScreen(store: Store, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val log by store.log.collectAsStateWithLifecycle()
    val allowed by store.allowed.collectAsStateWithLifecycle()

    Column(modifier.fillMaxSize()) {
        Header("Bloqueadas", "Llamadas que BlockCall rechazó por ti") {
            if (log.isNotEmpty()) {
                IconButton(onClick = store::clearLog) {
                    Icon(Icons.Rounded.DeleteSweep, contentDescription = "Borrar historial")
                }
            }
        }
        if (log.isEmpty()) {
            EmptyState(Icons.Rounded.PhoneDisabled, "Sin llamadas bloqueadas", "Aquí verás los números que fueron rechazados.")
        } else {
            LazyColumn(
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(log) { call ->
                    val hidden = call.number == Store.HIDDEN
                    val isAllowed = !hidden && allowed.any { it.number == call.number }
                    Row(itemModifier(), verticalAlignment = Alignment.CenterVertically) {
                        IconBadge(if (hidden) Icons.Rounded.VisibilityOff else Icons.Rounded.PhoneDisabled)
                        Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
                            Text(if (hidden) "Número oculto" else call.number, fontWeight = FontWeight.SemiBold)
                            Text(
                                DateUtils.getRelativeDateTimeString(context, call.time, DateUtils.MINUTE_IN_MILLIS, DateUtils.WEEK_IN_MILLIS, 0).toString(),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (!hidden && !isAllowed) {
                            TextButton(onClick = { store.addAllowed(call.number, "") }) {
                                Icon(Icons.Rounded.PersonAddAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.size(6.dp))
                                Text("Permitir")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Header(title: String, subtitle: String, action: @Composable () -> Unit = {}) {
    Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 12.dp, top = 16.dp, bottom = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        action()
    }
}

@Composable
private fun itemModifier() = Modifier
    .fillMaxWidth()
    .background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(20.dp))
    .padding(horizontal = 14.dp, vertical = 12.dp)

@Composable
private fun Avatar(name: String) {
    Box(
        Modifier.size(42.dp).background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        val initial = name.firstOrNull { it.isLetter() }
        if (initial != null) {
            Text(initial.uppercase(), color = MaterialTheme.colorScheme.onPrimaryContainer, fontWeight = FontWeight.Bold)
        } else {
            Icon(Icons.Rounded.Person, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
        }
    }
}

@Composable
private fun IconBadge(icon: ImageVector) {
    Box(
        Modifier.size(42.dp).background(MaterialTheme.colorScheme.errorContainer, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun EmptyState(icon: ImageVector, title: String, body: String) {
    Column(
        Modifier.fillMaxSize().padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Card(shape = CircleShape, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
            Icon(icon, contentDescription = null, modifier = Modifier.padding(24.dp).size(40.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
        }
        Spacer(Modifier.height(20.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(6.dp))
        Text(body, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun AddNumberDialog(onDismiss: () -> Unit, onConfirm: (String, String) -> Unit) {
    var number by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    val valid = number.count { it.isDigit() } >= 3

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Agregar número") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = number,
                    onValueChange = { v -> number = v.filter { it.isDigit() || it in "+ -()" } },
                    label = { Text("Número") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre (opcional)") },
                    singleLine = true,
                )
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(number, name) }, enabled = valid) { Text("Guardar") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}
