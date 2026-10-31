package com.blockcall.ui

import android.Manifest
import android.app.Activity
import android.app.role.RoleManager
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.CallMade
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Contacts
import androidx.compose.material.icons.rounded.DoNotDisturbOn
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.NotificationsNone
import androidx.compose.material.icons.rounded.PhoneDisabled
import androidx.compose.material.icons.rounded.PhoneInTalk
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.RocketLaunch
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.blockcall.R
import com.blockcall.Setup
import com.blockcall.Store

private enum class Step { WELCOME, ROLE, CONTACTS, NOTIFICATIONS, MAKER, DONE }

/** Fabricantes que bloquean el arranque de apps en segundo plano si no se activa "Inicio automático". */
private val AUTOSTART_MAKERS = setOf("xiaomi", "redmi", "poco", "vivo", "iqoo", "huawei", "honor")

/**
 * Asistente del primer inicio: pide, en orden y explicando cada aviso de Android,
 * todo lo que BlockCall necesita. Al terminar deja la protección encendida con los valores recomendados.
 */
@Composable
fun OnboardingScreen(store: Store, setup: Setup, onSetupChanged: () -> Unit) {
    val context = LocalContext.current
    val activity = context as Activity
    val maker = remember { Build.MANUFACTURER.lowercase() }
    val steps = remember {
        buildList {
            add(Step.WELCOME)
            add(Step.ROLE)
            add(Step.CONTACTS)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) add(Step.NOTIFICATIONS)
            if (maker in AUTOSTART_MAKERS) add(Step.MAKER)
            add(Step.DONE)
        }
    }
    var index by rememberSaveable { mutableIntStateOf(0) }
    val step = steps[index]
    fun next() {
        if (index < steps.lastIndex) index++
    }

    var roleTries by rememberSaveable { mutableIntStateOf(0) }
    var contactsDenied by rememberSaveable { mutableStateOf(false) }

    val roleLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        roleTries++
        onSetupChanged()
    }
    val contactsLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        contactsDenied = !granted
        onSetupChanged()
    }
    val notificationsLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        onSetupChanged()
        next()
    }

    // Cada paso avanza solo en cuanto su permiso queda concedido (o si ya lo estaba).
    LaunchedEffect(step, setup) {
        when {
            step == Step.ROLE && setup.hasRole -> next()
            step == Step.CONTACTS && setup.hasContacts -> next()
            step == Step.NOTIFICATIONS && setup.hasNotifications -> next()
            else -> Unit
        }
    }

    fun open(intent: Intent) = try {
        context.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        context.startActivity(appDetails(context.packageName))
    }

    // Surface fija el color de texto del tema (fuera del Scaffold, el texto saldría negro en modo oscuro).
    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
    Column(
        Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 24.dp, vertical = 16.dp),
    ) {
        LinearProgressIndicator(
            progress = { (index + 1).toFloat() / steps.size },
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            stringResource(R.string.ob_step, index + 1, steps.size),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp),
        )

        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            when (step) {
                Step.WELCOME -> {
                    Hero(Icons.Rounded.Shield, stringResource(R.string.ob_welcome_title), stringResource(R.string.ob_welcome_body))
                    Bullet(Icons.Rounded.BatteryChargingFull, stringResource(R.string.ob_welcome_battery))
                    Bullet(Icons.Rounded.WifiOff, stringResource(R.string.ob_welcome_offline))
                    Bullet(Icons.Rounded.PhoneInTalk, stringResource(R.string.ob_welcome_contacts))
                }
                Step.ROLE -> {
                    Hero(Icons.Rounded.PhoneDisabled, stringResource(R.string.ob_role_title), stringResource(R.string.ob_role_body))
                    if (roleTries > 0 && !setup.hasRole) {
                        Problem(stringResource(if (roleTries >= 2) R.string.ob_role_retry else R.string.ob_role_denied))
                    }
                    // Texto tal como lo muestra Android 16; otras versiones usan frases muy parecidas.
                    Notice(stringResource(R.string.ob_what_you_see), stringResource(R.string.ob_role_notice))
                    Bullet(Icons.Rounded.Info, stringResource(R.string.ob_role_phone))
                    Bullet(Icons.Rounded.WarningAmber, stringResource(R.string.ob_role_truecaller))
                }
                Step.CONTACTS -> {
                    Hero(Icons.Rounded.Contacts, stringResource(R.string.ob_contacts_title), stringResource(R.string.ob_contacts_body))
                    if (contactsDenied) Problem(stringResource(R.string.ob_contacts_denied))
                    Notice(stringResource(R.string.ob_what_you_see), stringResource(R.string.ob_contacts_notice))
                    Bullet(Icons.Rounded.WifiOff, stringResource(R.string.ob_contacts_private))
                }
                Step.NOTIFICATIONS -> {
                    Hero(Icons.Rounded.NotificationsNone, stringResource(R.string.ob_notif_title), stringResource(R.string.ob_notif_body))
                    Notice(stringResource(R.string.ob_what_you_see), stringResource(R.string.ob_notif_notice))
                }
                Step.MAKER -> {
                    val brand = Build.MANUFACTURER.replaceFirstChar { it.uppercase() }
                    Hero(Icons.Rounded.RocketLaunch, stringResource(R.string.ob_maker_title, brand), stringResource(R.string.ob_maker_body))
                    Notice(stringResource(R.string.ob_how_to), stringResource(R.string.ob_maker_notice))
                }
                Step.DONE -> {
                    Hero(Icons.Rounded.CheckCircle, stringResource(R.string.ob_done_title), stringResource(R.string.ob_done_body))
                    Bullet(Icons.Rounded.Shield, stringResource(R.string.ob_done_always))
                    Bullet(Icons.Rounded.Contacts, stringResource(R.string.who_contacts_body))
                    Bullet(Icons.Rounded.VisibilityOff, stringResource(R.string.rule_hidden))
                    Bullet(Icons.Rounded.Replay, stringResource(R.string.ob_done_repeat))
                    Bullet(Icons.AutoMirrored.Rounded.CallMade, stringResource(R.string.ob_done_recent))
                    Bullet(Icons.Rounded.NotificationsNone, stringResource(R.string.ob_done_notify))
                    if (!setup.hasRole) Problem(stringResource(R.string.ob_done_missing_role))
                }
            }
        }

        // Botones: acción principal + alternativa del paso.
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            when (step) {
                Step.WELCOME -> Primary(stringResource(R.string.ob_start), ::next)
                Step.ROLE -> {
                    if (roleTries >= 2) {
                        Primary(stringResource(R.string.ob_open_settings)) { open(Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS)) }
                    } else {
                        Primary(stringResource(R.string.ob_choose)) {
                            val rm = context.getSystemService(RoleManager::class.java)
                            roleLauncher.launch(rm.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING))
                        }
                    }
                    Secondary(stringResource(R.string.ob_later), ::next)
                }
                Step.CONTACTS -> {
                    val blocked = contactsDenied && !activity.shouldShowRequestPermissionRationale(Manifest.permission.READ_CONTACTS)
                    if (blocked) {
                        Primary(stringResource(R.string.ob_open_app_settings)) { open(appDetails(context.packageName)) }
                    } else {
                        Primary(stringResource(R.string.ob_allow_access)) { contactsLauncher.launch(Manifest.permission.READ_CONTACTS) }
                    }
                    Secondary(stringResource(R.string.ob_skip), ::next)
                }
                Step.NOTIFICATIONS -> {
                    Primary(stringResource(R.string.ob_allow_notifications)) { notificationsLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) }
                    Secondary(stringResource(R.string.ob_skip), ::next)
                }
                Step.MAKER -> {
                    Primary(stringResource(R.string.ob_open_settings)) { open(autostartIntent(maker, context.packageName)) }
                    Secondary(stringResource(R.string.ob_autostart_done), ::next)
                }
                Step.DONE -> Primary(stringResource(R.string.ob_finish), store::finishOnboarding)
            }
        }
    }
    }
}

private fun appDetails(pkg: String) =
    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", pkg, null))

/** Pantalla de "Inicio automático" del fabricante; si no existe, la ficha de la app. */
private fun autostartIntent(maker: String, pkg: String): Intent = when (maker) {
    "xiaomi", "redmi", "poco" -> Intent().setComponent(
        ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity"),
    )
    else -> appDetails(pkg)
}

@Composable
private fun Hero(icon: ImageVector, title: String, body: String) {
    Box(
        Modifier
            .size(88.dp)
            .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(44.dp))
    }
    Spacer(Modifier.height(8.dp))
    Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
    Text(body, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Spacer(Modifier.height(4.dp))
}

@Composable
private fun Bullet(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
        Spacer(Modifier.size(12.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}

/** Recuadro que reproduce el aviso del sistema que el usuario está por ver. */
@Composable
private fun Notice(label: String, text: String) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.TouchApp, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(8.dp))
                Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.height(8.dp))
            Text(text, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun Problem(text: String) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
            Icon(Icons.Rounded.DoNotDisturbOn, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.size(10.dp))
            Text(text, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Start, color = Color.Unspecified)
        }
    }
}

@Composable
private fun Primary(text: String, onClick: () -> Unit) {
    Button(onClick = onClick, modifier = Modifier.fillMaxWidth().height(52.dp)) { Text(text) }
}

@Composable
private fun Secondary(text: String, onClick: () -> Unit) {
    TextButton(onClick = onClick, modifier = Modifier.padding(top = 4.dp)) { Text(text) }
}
