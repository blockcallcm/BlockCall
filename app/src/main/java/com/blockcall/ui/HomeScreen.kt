package com.blockcall.ui

import android.Manifest
import android.app.role.RoleManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Contacts
import androidx.compose.material.icons.rounded.GppGood
import androidx.compose.material.icons.rounded.NotificationsNone
import androidx.compose.material.icons.rounded.PhoneDisabled
import androidx.compose.material.icons.rounded.PlaylistAddCheck
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.blockcall.Setup
import com.blockcall.ALLOW_CONTACTS
import com.blockcall.ALLOW_LIST
import com.blockcall.ALLOW_NONE
import com.blockcall.Store
import com.blockcall.THEME_DARK
import com.blockcall.THEME_LIGHT
import com.blockcall.THEME_SYSTEM
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(store: Store, setup: Setup, onSetupChanged: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val settings by store.settings.collectAsStateWithLifecycle()
    val allowed by store.allowed.collectAsStateWithLifecycle()
    val log by store.log.collectAsStateWithLifecycle()

    // Re-evalúa el estado del horario solo mientras la pantalla está en primer plano.
    var tick by remember { mutableIntStateOf(0) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (true) {
                tick++; delay(30_000)
            }
        }
    }
    val filteringNow = remember(settings, tick) { store.isFilteringNow() }

    val notificationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) store.setNotify(true)
        onSetupChanged()
    }
    // Al terminar la configuración inicial, también se piden las notificaciones (opcionales).
    val contactsLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        onSetupChanged()
        if (!setup.hasNotifications) notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
    val roleLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        onSetupChanged()
        if (!setup.hasContacts) contactsLauncher.launch(Manifest.permission.READ_CONTACTS)
    }
    fun requestSetup() {
        if (!setup.hasRole) {
            val rm = context.getSystemService(RoleManager::class.java)
            roleLauncher.launch(rm.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING))
        } else if (!setup.hasContacts) {
            contactsLauncher.launch(Manifest.permission.READ_CONTACTS)
        }
    }

    var editing by remember { mutableStateOf<Boolean?>(null) } // true = inicio, false = fin

    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("BlockCall", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)

        // Los contactos solo hacen falta en el modo "Contactos"; el rol de filtro, siempre.
        val needsContacts = settings.allowMode == ALLOW_CONTACTS
        val ready = setup.hasRole && (setup.hasContacts || !needsContacts)
        StatusCard(
            enabled = settings.enabled,
            active = filteringNow && ready,
            scheduled = settings.scheduled,
            allowMode = settings.allowMode,
            problem = when {
                !setup.hasRole -> "BlockCall no es tu app de filtro de llamadas (quizá otra app lo tomó). Toca «Conceder»"
                needsContacts && !setup.hasContacts -> "Sin acceso a contactos: no se está filtrando ninguna llamada"
                else -> null
            },
            onToggle = { on ->
                store.setEnabled(on)
                if (on && !setup.ready) requestSetup()
            },
        )

        AnimatedVisibility(!setup.ready) {
            SetupCard(setup, onClick = ::requestSetup)
        }

        SectionCard {
            Text("¿Cuándo bloquear?", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(12.dp))
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                listOf("Siempre", "Por horario").forEachIndexed { i, label ->
                    SegmentedButton(
                        selected = settings.scheduled == (i == 1),
                        onClick = { store.setScheduled(i == 1) },
                        shape = SegmentedButtonDefaults.itemShape(i, 2),
                    ) { Text(label) }
                }
            }
            AnimatedVisibility(settings.scheduled) {
                Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TimeTile("Desde", settings.startMinute, Icons.Rounded.Bedtime, Modifier.weight(1f)) { editing = true }
                    TimeTile("Hasta", settings.endMinute, Icons.Rounded.WbSunny, Modifier.weight(1f)) { editing = false }
                }
            }
        }

        SectionCard {
            Text("¿Quién puede llamarte?", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(12.dp))
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                listOf(ALLOW_CONTACTS to "Contactos", ALLOW_LIST to "Mi lista", ALLOW_NONE to "Nadie").forEachIndexed { i, (mode, label) ->
                    SegmentedButton(
                        selected = settings.allowMode == mode,
                        onClick = { store.setAllowMode(mode) },
                        shape = SegmentedButtonDefaults.itemShape(i, 3),
                    ) { Text(label) }
                }
            }
            Spacer(Modifier.height(10.dp))
            val (modeIcon, modeText) = when (settings.allowMode) {
                ALLOW_NONE -> Icons.Rounded.PhoneDisabled to "No entra ninguna llamada: ni contactos, ni tu lista, ni insistentes"
                ALLOW_LIST -> if (allowed.isEmpty()) {
                    Icons.Rounded.WarningAmber to "Tu lista está vacía: se bloquearán todas las llamadas"
                } else {
                    Icons.Rounded.PlaylistAddCheck to "Solo entran los ${allowed.size} número(s) de la pestaña Permitidos"
                }
                else -> Icons.Rounded.Contacts to "Entran tus contactos y los números de la pestaña Permitidos"
            }
            RuleRow(modeIcon, modeText)
        }

        ToggleCard(
            icon = Icons.Rounded.Replay,
            title = "Permitir si insiste",
            body = if (settings.allowMode == ALLOW_NONE) {
                "No aplica con «Nadie»: todas las llamadas se rechazan"
            } else {
                "Si el mismo número llama 2 veces en menos de 3 minutos, la segunda llamada entra"
            },
            checked = settings.allowRepeated && settings.allowMode != ALLOW_NONE,
            onCheckedChange = store::setAllowRepeated,
            enabled = settings.allowMode != ALLOW_NONE,
        )

        ToggleCard(
            icon = Icons.Rounded.NotificationsNone,
            title = "Avisarme de bloqueos",
            body = "Notificación silenciosa con las llamadas bloqueadas (sin sonido ni vibración)",
            checked = settings.notify && setup.hasNotifications,
            onCheckedChange = { on ->
                if (on && !setup.hasNotifications) {
                    notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    store.setNotify(on)
                }
            },
        )

        SectionCard {
            Text("Reglas", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            when (settings.allowMode) {
                ALLOW_NONE -> RuleRow(Icons.Rounded.PhoneDisabled, "Ninguna llamada entra")
                ALLOW_LIST -> RuleRow(Icons.Rounded.Contacts, "Tus contactos NO entran si no están en la lista")
                else -> RuleRow(Icons.Rounded.Contacts, "Tus contactos siempre entran")
            }
            if (settings.allowMode != ALLOW_NONE) {
                RuleRow(Icons.Rounded.GppGood, "${allowed.size} número(s) en tu lista de permitidos")
                RuleRow(Icons.Rounded.VisibilityOff, "Números ocultos o privados: bloqueados")
                if (settings.allowRepeated) RuleRow(Icons.Rounded.Replay, "Llamadas insistentes: la segunda entra")
            }
            RuleRow(Icons.Rounded.Shield, "${log.size} llamada(s) bloqueada(s) en el historial")
        }

        SectionCard {
            Text("Apariencia", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(12.dp))
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                listOf(THEME_SYSTEM to "Sistema", THEME_LIGHT to "Claro", THEME_DARK to "Oscuro").forEachIndexed { i, (mode, label) ->
                    SegmentedButton(
                        selected = settings.theme == mode,
                        onClick = { store.setTheme(mode) },
                        shape = SegmentedButtonDefaults.itemShape(i, 3),
                    ) { Text(label) }
                }
            }
        }
    }

    editing?.let { isStart ->
        TimeDialog(
            initialMinute = if (isStart) settings.startMinute else settings.endMinute,
            title = if (isStart) "Bloquear desde" else "Bloquear hasta",
            onDismiss = { editing = null },
            onConfirm = { minute ->
                if (isStart) store.setStart(minute) else store.setEnd(minute)
                editing = null
            },
        )
    }
}

@Composable
private fun StatusCard(
    enabled: Boolean,
    active: Boolean,
    scheduled: Boolean,
    allowMode: Int,
    problem: String?,
    onToggle: (Boolean) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    // Encendida pero sin poder filtrar (p. ej. Truecaller recuperó el rol): se marca en rojo.
    val broken = enabled && problem != null
    val container by animateColorAsState(
        when {
            broken -> colors.errorContainer
            active -> colors.primary
            else -> colors.surfaceContainerHighest
        },
        label = "bg",
    )
    val content by animateColorAsState(
        when {
            broken -> colors.onErrorContainer
            active -> colors.onPrimary
            else -> colors.onSurfaceVariant
        },
        label = "fg",
    )
    val (title, subtitle) = when {
        !enabled -> "Protección desactivada" to "Todas las llamadas entran normalmente"
        broken -> "Protección detenida" to problem.orEmpty()
        active && allowMode == ALLOW_NONE -> "Protección activa" to "No entra ninguna llamada"
        active && allowMode == ALLOW_LIST -> "Protección activa" to "Solo entran los números de tu lista"
        active -> "Protección activa" to "Solo entran tus contactos y permitidos"
        scheduled -> "En espera" to "Se activará en el horario configurado"
        else -> "Falta configurar" to "Concede los permisos para empezar"
    }

    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = container, contentColor = content),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(24.dp)) {
            Box(
                Modifier
                    .size(64.dp)
                    .background(content.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.Shield, contentDescription = null, modifier = Modifier.size(34.dp))
            }
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = content.copy(alpha = 0.8f))
                }
                Switch(
                    checked = enabled,
                    onCheckedChange = onToggle,
                    // Contraste propio: la tarjeta ya usa el color primario de fondo.
                    colors = SwitchDefaults.colors(
                        checkedTrackColor = content,
                        checkedThumbColor = container,
                        checkedBorderColor = content,
                    ),
                )
            }
        }
    }
}

@Composable
private fun SetupCard(setup: Setup, onClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.WarningAmber, contentDescription = null)
                Spacer(Modifier.size(8.dp))
                Text("Permisos necesarios", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(8.dp))
            if (!setup.hasRole) Text("• Elegir BlockCall como app de filtro de llamadas")
            if (!setup.hasContacts) Text("• Acceso a contactos (para dejarlos pasar)")
            Spacer(Modifier.height(12.dp))
            Button(onClick = onClick) { Text("Conceder") }
        }
    }
}

@Composable
private fun SectionCard(content: @Composable () -> Unit) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(20.dp)) { content() }
    }
}

@Composable
private fun ToggleCard(
    icon: ImageVector,
    title: String,
    body: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true,
) {
    SectionCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.size(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.size(8.dp))
            Switch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
        }
    }
}

@Composable
private fun TimeTile(label: String, minute: Int, icon: ImageVector, modifier: Modifier, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        modifier = modifier,
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(6.dp))
                Text(label, style = MaterialTheme.typography.labelLarge)
            }
            Text(formatMinute(minute), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun RuleRow(icon: ImageVector, text: String) {
    Row(Modifier.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        Spacer(Modifier.size(12.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimeDialog(initialMinute: Int, title: String, onDismiss: () -> Unit, onConfirm: (Int) -> Unit) {
    val state = rememberTimePickerState(initialMinute / 60, initialMinute % 60, is24Hour = true)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { TimePicker(state = state) },
        confirmButton = { TextButton(onClick = { onConfirm(state.hour * 60 + state.minute) }) { Text("Aceptar") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}

fun formatMinute(minute: Int) = "%02d:%02d".format(minute / 60, minute % 60)
