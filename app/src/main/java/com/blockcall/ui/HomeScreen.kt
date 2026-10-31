package com.blockcall.ui

import android.Manifest
import android.app.role.RoleManager
import android.os.Build
import android.text.format.DateUtils
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.CallMade
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Contacts
import androidx.compose.material.icons.rounded.GppGood
import androidx.compose.material.icons.rounded.NotificationsNone
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PauseCircle
import androidx.compose.material.icons.rounded.PhoneDisabled
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PlaylistAddCheck
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.blockcall.ALLOW_CONTACTS
import com.blockcall.ALLOW_LIST
import com.blockcall.ALLOW_NONE
import com.blockcall.CountryCode
import com.blockcall.R
import com.blockcall.Setup
import com.blockcall.Store
import com.blockcall.THEME_DARK
import com.blockcall.THEME_LIGHT
import com.blockcall.THEME_SYSTEM
import com.blockcall.Tiles
import kotlinx.coroutines.delay
import java.util.Calendar

// Acentos fijos de los iconos de ajustes: se distinguen de un vistazo con cualquier tema.
private val Green = Color(0xFF30B566)
private val Blue = Color(0xFF2F80ED)
private val Orange = Color(0xFFF2994A)
private val Purple = Color(0xFF9B51E0)
private val Pink = Color(0xFFEB5787)

// Tarjeta de estado: el degradado del icono (índigo → violeta → magenta) y uno ámbar para la pausa.
// En tema oscuro, tonos más profundos: Material desaconseja colores muy saturados sobre fondo oscuro.
// Todos con contraste de texto ≥ 4,5:1 (WCAG AA).
private val BrandIndigo = Color(0xFF4F46E5)
private val BrandViolet = Color(0xFF7C3AED)
private val BrandMagenta = Color(0xFFC026D3)
private val BrandIndigoDark = Color(0xFF3730A3)
private val BrandVioletDark = Color(0xFF6D28D9)
private val BrandMagentaDark = Color(0xFFA21CAF)
private val PauseAmber = Color(0xFFFBBF24)
private val PauseOrange = Color(0xFFF59E0B)
private val PauseDeepOrange = Color(0xFFF97316)
private val OnPause = Color(0xFF2B1300)
// Ámbar dorado apagado (tono ~39°): los ámbares profundos tiran a naranja rojizo y se confunden con error.
private val PauseAmberDark = Color(0xFFD9A441)
private val PauseOrangeDark = Color(0xFFC9952F)
private val PauseDeepOrangeDark = Color(0xFFB8862B)

/**
 * Acerca el tono de un color fijo al color principal del tema, como MaterialColors.harmonize de Material:
 * gira el tono la mitad de la diferencia, como mucho 15°. Así los acentos combinan con el fondo de
 * pantalla (Material You) sin perder su significado. Solo se aplica a los iconos de los ajustes: ni el
 * degradado de marca ni el ámbar de la pausa (con un tema azul el giro lo volvería rojo, color de error).
 */
private fun Color.harmonizeWith(source: Color): Color {
    val hsv = FloatArray(3).also { android.graphics.Color.colorToHSV(toArgb(), it) }
    val target = FloatArray(3).also { android.graphics.Color.colorToHSV(source.toArgb(), it) }
    val diff = (target[0] - hsv[0] + 540f) % 360f - 180f
    hsv[0] = (hsv[0] + (diff / 2f).coerceIn(-15f, 15f) + 360f) % 360f
    return Color(android.graphics.Color.HSVToColor(hsv)).copy(alpha = alpha)
}

/** [harmonizeWith] con el color principal del tema actual. */
@Composable
private fun Color.harmonized(): Color {
    val primary = MaterialTheme.colorScheme.primary
    return remember(this, primary) { harmonizeWith(primary) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(store: Store, setup: Setup, onSetupChanged: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val settings by store.settings.collectAsStateWithLifecycle()
    val allowed by store.allowed.collectAsStateWithLifecycle()
    val log by store.log.collectAsStateWithLifecycle()
    val countryCode = remember { CountryCode.of(context) }

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
    val paused = remember(settings, tick) { settings.isPaused() }
    val pauseLeftMs = remember(settings, tick) { settings.pausedUntil - System.currentTimeMillis() }
    val today = remember(log, tick) { log.count { DateUtils.isToday(it.time) } }
    val week = remember(log, tick) { log.count { System.currentTimeMillis() - it.time < 7 * DateUtils.DAY_IN_MILLIS } }

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
    var pickingPause by remember { mutableStateOf(false) }
    val noneMode = settings.allowMode == ALLOW_NONE

    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Column {
            Text("BlockCall", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(stringResource(R.string.home_subtitle), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        // Los contactos solo hacen falta en el modo "Contactos"; el rol de filtro, siempre.
        val needsContacts = settings.allowMode == ALLOW_CONTACTS
        val ready = setup.hasRole && (setup.hasContacts || !needsContacts)
        StatusCard(
            enabled = settings.enabled,
            active = filteringNow && ready && !paused,
            pausedUntil = if (paused) Tiles.until(context, settings.pausedUntil) else null,
            pauseLeftMinutes = ((pauseLeftMs + 59_999) / 60_000).toInt(),
            pauseProgress = pauseLeftMs.toFloat() / (settings.pausedUntil - settings.pausedAt).coerceAtLeast(1),
            scheduled = settings.scheduled,
            allowMode = settings.allowMode,
            problem = when {
                !setup.hasRole -> stringResource(R.string.problem_no_role)
                needsContacts && !setup.hasContacts -> stringResource(R.string.problem_no_contacts)
                else -> null
            },
            stats = listOf(today to R.string.stat_today, week to R.string.stat_week, allowed.size to R.string.stat_allowed),
            onToggle = { on ->
                store.setEnabled(on)
                if (on && !setup.ready) requestSetup()
            },
            onPause = store::pause,
            onPickTime = { pickingPause = true },
            onResume = store::resume,
        )

        AnimatedVisibility(!setup.ready) {
            SetupCard(setup, onClick = ::requestSetup)
        }

        SectionCard(stringResource(R.string.when_title)) {
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                listOf(R.string.when_always, R.string.when_schedule).forEachIndexed { i, label ->
                    SegmentedButton(
                        selected = settings.scheduled == (i == 1),
                        onClick = { store.setScheduled(i == 1) },
                        shape = SegmentedButtonDefaults.itemShape(i, 2),
                    ) { Text(stringResource(label)) }
                }
            }
            AnimatedVisibility(settings.scheduled) {
                Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TimeTile(stringResource(R.string.time_from), settings.startMinute, Icons.Rounded.Bedtime, Modifier.weight(1f)) { editing = true }
                    TimeTile(stringResource(R.string.time_to), settings.endMinute, Icons.Rounded.WbSunny, Modifier.weight(1f)) { editing = false }
                }
            }
        }

        SectionCard(stringResource(R.string.who_title)) {
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                listOf(
                    ALLOW_CONTACTS to R.string.who_contacts,
                    ALLOW_LIST to R.string.who_list,
                    ALLOW_NONE to R.string.who_none,
                ).forEachIndexed { i, (mode, label) ->
                    SegmentedButton(
                        selected = settings.allowMode == mode,
                        onClick = { store.setAllowMode(mode) },
                        shape = SegmentedButtonDefaults.itemShape(i, 3),
                    ) { Text(stringResource(label)) }
                }
            }
            Spacer(Modifier.height(12.dp))
            val (modeIcon, modeText) = when (settings.allowMode) {
                ALLOW_NONE -> Icons.Rounded.PhoneDisabled to stringResource(R.string.who_none_body)
                ALLOW_LIST -> if (allowed.isEmpty()) {
                    Icons.Rounded.WarningAmber to stringResource(R.string.who_list_empty)
                } else {
                    Icons.Rounded.PlaylistAddCheck to pluralStringResource(R.plurals.who_list_body, allowed.size, allowed.size)
                }
                else -> Icons.Rounded.Contacts to stringResource(R.string.who_contacts_body)
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(modeIcon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                Spacer(Modifier.size(12.dp))
                Text(modeText, style = MaterialTheme.typography.bodyMedium)
            }
        }

        SettingsGroup(stringResource(R.string.section_exceptions)) {
            SettingRow(
                icon = Icons.Rounded.Replay,
                accent = Green,
                title = stringResource(R.string.repeat_title),
                body = stringResource(if (noneMode) R.string.repeat_disabled else R.string.repeat_body),
                checked = settings.allowRepeated && !noneMode,
                onCheckedChange = store::setAllowRepeated,
                enabled = !noneMode,
            )
            RowDivider()
            SettingRow(
                icon = Icons.AutoMirrored.Rounded.CallMade,
                accent = Blue,
                title = stringResource(R.string.recent_title),
                body = stringResource(if (noneMode) R.string.repeat_disabled else R.string.recent_body),
                checked = settings.allowRecent && !noneMode,
                onCheckedChange = store::setAllowRecent,
                enabled = !noneMode,
            )
        }

        SettingsGroup(stringResource(R.string.section_protection)) {
            SettingRow(
                icon = Icons.Rounded.Public,
                accent = Orange,
                title = stringResource(R.string.foreign_title),
                body = countryCode?.let { stringResource(R.string.foreign_body, it) } ?: stringResource(R.string.foreign_unknown),
                checked = settings.blockForeign && countryCode != null,
                onCheckedChange = store::setBlockForeign,
                enabled = countryCode != null,
            )
            RowDivider()
            SettingRow(
                icon = Icons.AutoMirrored.Rounded.VolumeOff,
                accent = Purple,
                title = stringResource(R.string.silence_title),
                body = stringResource(R.string.silence_body),
                checked = settings.silence,
                onCheckedChange = store::setSilence,
            )
            RowDivider()
            SettingRow(
                icon = Icons.Rounded.NotificationsNone,
                accent = Pink,
                title = stringResource(R.string.notify_title),
                body = stringResource(R.string.notify_body),
                checked = settings.notify && setup.hasNotifications,
                onCheckedChange = { on ->
                    if (on && !setup.hasNotifications) {
                        notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        store.setNotify(on)
                    }
                },
            )
            // Solo Android 13+ permite pedir que se agregue.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                RowDivider()
                ActionRow(
                    icon = Icons.Rounded.Widgets,
                    accent = BrandIndigo,
                    title = stringResource(R.string.shortcut_title),
                    body = stringResource(R.string.shortcut_body),
                    action = stringResource(R.string.action_add),
                    onClick = { Tiles.requestAdd(context) },
                )
            }
        }

        SectionCard(stringResource(R.string.rules_title)) {
            when (settings.allowMode) {
                ALLOW_NONE -> RuleRow(Icons.Rounded.PhoneDisabled, stringResource(R.string.rule_none))
                ALLOW_LIST -> RuleRow(Icons.Rounded.Contacts, stringResource(R.string.rule_list))
                else -> RuleRow(Icons.Rounded.Contacts, stringResource(R.string.rule_contacts))
            }
            if (!noneMode) {
                RuleRow(Icons.Rounded.GppGood, pluralStringResource(R.plurals.rule_allowed_count, allowed.size, allowed.size))
                RuleRow(Icons.Rounded.VisibilityOff, stringResource(R.string.rule_hidden))
                if (settings.allowRepeated) RuleRow(Icons.Rounded.Replay, stringResource(R.string.rule_repeat))
                if (settings.allowRecent) RuleRow(Icons.AutoMirrored.Rounded.CallMade, stringResource(R.string.rule_recent))
            }
            if (settings.blockForeign && countryCode != null) RuleRow(Icons.Rounded.Public, stringResource(R.string.rule_foreign, countryCode))
            if (settings.silence) RuleRow(Icons.AutoMirrored.Rounded.VolumeOff, stringResource(R.string.rule_silence))
            RuleRow(Icons.Rounded.Shield, pluralStringResource(R.plurals.rule_blocked_count, log.size, log.size))
        }

        SectionCard(stringResource(R.string.appearance_title)) {
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                listOf(
                    THEME_SYSTEM to R.string.theme_system,
                    THEME_LIGHT to R.string.theme_light,
                    THEME_DARK to R.string.theme_dark,
                ).forEachIndexed { i, (mode, label) ->
                    SegmentedButton(
                        selected = settings.theme == mode,
                        onClick = { store.setTheme(mode) },
                        shape = SegmentedButtonDefaults.itemShape(i, 3),
                    ) { Text(stringResource(label)) }
                }
            }
        }
    }

    editing?.let { isStart ->
        TimeDialog(
            initialMinute = if (isStart) settings.startMinute else settings.endMinute,
            title = stringResource(if (isStart) R.string.dialog_block_from else R.string.dialog_block_until),
            onDismiss = { editing = null },
            onConfirm = { minute ->
                if (isStart) store.setStart(minute) else store.setEnd(minute)
                editing = null
            },
        )
    }

    if (pickingPause) {
        val now = Calendar.getInstance()
        TimeDialog(
            // Propuesta inicial: dentro de 3 horas, en punto.
            initialMinute = (now.get(Calendar.HOUR_OF_DAY) + 3) % 24 * 60,
            title = stringResource(R.string.dialog_pause_until),
            onDismiss = { pickingPause = false },
            onConfirm = { minute ->
                store.pauseUntil(nextTimeAt(minute))
                pickingPause = false
            },
        )
    }
}

/** Próxima vez que el reloj marque [minuteOfDay]: hoy si aún no ha pasado, si no mañana. */
private fun nextTimeAt(minuteOfDay: Int): Long {
    val target = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, minuteOfDay / 60)
        set(Calendar.MINUTE, minuteOfDay % 60)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    if (target.timeInMillis <= System.currentTimeMillis()) target.add(Calendar.DAY_OF_YEAR, 1)
    return target.timeInMillis
}

@Composable
private fun StatusCard(
    enabled: Boolean,
    active: Boolean,
    pausedUntil: String?,
    pauseLeftMinutes: Int,
    pauseProgress: Float,
    scheduled: Boolean,
    allowMode: Int,
    problem: String?,
    stats: List<Pair<Int, Int>>,
    onToggle: (Boolean) -> Unit,
    onPause: (Int) -> Unit,
    onPickTime: () -> Unit,
    onResume: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    // Encendida pero sin poder filtrar (p. ej. Truecaller recuperó el rol): se marca en rojo.
    val broken = enabled && problem != null
    val paused = pausedUntil != null && !broken
    val dark = colors.surface.luminance() < 0.5f
    // Activa (marca) y en pausa (ámbar de atención): colores fijos, no dependen del fondo de pantalla.
    // Ambos más profundos en tema oscuro.
    val pauseStart = if (dark) PauseAmberDark else PauseAmber
    val pauseMid = if (dark) PauseOrangeDark else PauseOrange
    val pauseEnd = if (dark) PauseDeepOrangeDark else PauseDeepOrange
    val start by animateColorAsState(
        when {
            broken -> colors.errorContainer
            paused -> pauseStart
            active -> if (dark) BrandIndigoDark else BrandIndigo
            else -> colors.surfaceContainerHighest
        },
        label = "start",
    )
    val mid by animateColorAsState(
        when {
            broken -> colors.errorContainer
            paused -> pauseMid
            active -> if (dark) BrandVioletDark else BrandViolet
            else -> colors.surfaceContainerHighest
        },
        label = "mid",
    )
    val end by animateColorAsState(
        when {
            broken -> colors.errorContainer
            paused -> pauseEnd
            active -> if (dark) BrandMagentaDark else BrandMagenta
            else -> colors.surfaceContainerHigh
        },
        label = "end",
    )
    val content by animateColorAsState(
        when {
            broken -> colors.onErrorContainer
            paused -> OnPause
            active -> Color.White
            else -> colors.onSurfaceVariant
        },
        label = "fg",
    )
    val (title, subtitle) = when {
        !enabled -> stringResource(R.string.status_off_title) to stringResource(R.string.status_off_body)
        broken -> stringResource(R.string.status_broken_title) to problem.orEmpty()
        paused -> stringResource(R.string.status_paused_title) to stringResource(R.string.status_paused_body, pausedUntil.orEmpty())
        active -> stringResource(R.string.status_active_title) to stringResource(
            when (allowMode) {
                ALLOW_NONE -> R.string.status_active_none
                ALLOW_LIST -> R.string.status_active_list
                else -> R.string.status_active_contacts
            },
        )
        scheduled -> stringResource(R.string.status_waiting_title) to stringResource(R.string.status_waiting_body)
        else -> stringResource(R.string.status_setup_title) to stringResource(R.string.status_setup_body)
    }

    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(32.dp))
            .background(Brush.linearGradient(0f to start, 0.6f to mid, 1f to end)),
    ) {
        // Escudo de fondo, solo decorativo: arriba a la derecha, sin pasar por detrás del interruptor.
        Icon(
            Icons.Rounded.Shield,
            contentDescription = null,
            tint = content.copy(alpha = 0.06f),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 36.dp, y = (-40).dp)
                .size(150.dp),
        )
        CompositionLocalProvider(LocalContentColor provides content) {
            Column(Modifier.padding(24.dp)) {
                ShieldBadge(active = active && !broken, paused = paused, tint = content)
                Spacer(Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = content.copy(alpha = 0.9f))
                    }
                    Spacer(Modifier.size(8.dp))
                    Switch(
                        checked = enabled,
                        onCheckedChange = onToggle,
                        // Contraste propio: la tarjeta ya usa el color primario de fondo.
                        colors = SwitchDefaults.colors(
                            checkedTrackColor = content,
                            checkedThumbColor = start,
                            checkedBorderColor = content,
                        ),
                    )
                }
                if (enabled && !broken) {
                    Spacer(Modifier.height(16.dp))
                    PausePanel(
                        leftMinutes = if (paused) pauseLeftMinutes else null,
                        progress = pauseProgress,
                        tint = content,
                        onPause = onPause,
                        onPickTime = onPickTime,
                        onResume = onResume,
                    )
                }
                Spacer(Modifier.height(20.dp))
                // Misma altura para los tres recuadros y etiquetas en una sola línea, aun con letra grande.
                Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    stats.forEach { (value, label) ->
                        Column(
                            Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .background(content.copy(alpha = 0.12f), RoundedCornerShape(18.dp))
                                .padding(horizontal = 12.dp, vertical = 12.dp),
                        ) {
                            Text("$value", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, maxLines = 1)
                            Text(
                                stringResource(label),
                                style = MaterialTheme.typography.labelSmall,
                                color = content.copy(alpha = 0.9f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Pausa temporal dentro de la tarjeta de estado. Sin pausa: elegir 30 min, 1 h o una hora concreta.
 * En pausa ([leftMinutes] != null): tiempo restante con barra de progreso y botón para reanudar.
 */
@Composable
private fun PausePanel(
    leftMinutes: Int?,
    progress: Float,
    tint: Color,
    onPause: (Int) -> Unit,
    onPickTime: () -> Unit,
    onResume: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(tint.copy(alpha = 0.10f), RoundedCornerShape(22.dp))
            .padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(36.dp)
                    .background(tint.copy(alpha = 0.14f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    if (leftMinutes != null) Icons.Rounded.Timer else Icons.Rounded.PauseCircle,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
            }
            Spacer(Modifier.size(12.dp))
            Column(Modifier.weight(1f)) {
                val title = when {
                    leftMinutes == null -> stringResource(R.string.pause_title)
                    leftMinutes < 60 -> stringResource(R.string.pause_left_minutes, leftMinutes)
                    leftMinutes % 60 == 0 -> stringResource(R.string.pause_left_hours_only, leftMinutes / 60)
                    else -> stringResource(R.string.pause_left_hours, leftMinutes / 60, leftMinutes % 60)
                }
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(
                    stringResource(if (leftMinutes != null) R.string.pause_running_body else R.string.pause_body),
                    style = MaterialTheme.typography.bodySmall,
                    color = tint.copy(alpha = 0.9f),
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        if (leftMinutes != null) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape)
                    .background(tint.copy(alpha = 0.18f)),
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(progress.coerceIn(0f, 1f))
                        .fillMaxHeight()
                        .background(tint, CircleShape),
                )
            }
            Spacer(Modifier.height(12.dp))
            Pill(stringResource(R.string.action_resume), Icons.Rounded.PlayArrow, tint, Modifier.fillMaxWidth(), onResume)
        } else {
            // Duraciones fijas en una fila y "Personalizada" debajo: caben aun con letra grande.
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Store.PAUSE_OPTIONS.forEach { minutes ->
                    val label = if (minutes < 60) {
                        stringResource(R.string.pause_minutes, minutes)
                    } else {
                        stringResource(R.string.pause_hours, minutes / 60)
                    }
                    Pill(label, null, tint, Modifier.weight(1f)) { onPause(minutes) }
                }
            }
            Spacer(Modifier.height(8.dp))
            Pill(stringResource(R.string.pause_custom), Icons.Rounded.Schedule, tint, Modifier.fillMaxWidth(), onPickTime)
        }
    }
}

@Composable
private fun Pill(text: String, icon: ImageVector?, tint: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Row(
        modifier
            .clip(CircleShape)
            .background(tint.copy(alpha = 0.16f))
            .clickable(onClick = onClick)
            // 48 dp de alto: el mínimo táctil de Material.
            .padding(horizontal = 8.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        icon?.let {
            Icon(it, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.size(6.dp))
        }
        Text(
            text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Escudo con una onda que late mientras la protección está activa (solo con la app en pantalla). */
@Composable
private fun ShieldBadge(active: Boolean, paused: Boolean, tint: Color) {
    Box(Modifier.size(72.dp), contentAlignment = Alignment.Center) {
        if (active) {
            val pulse by rememberInfiniteTransition(label = "pulse").animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(tween(2400, easing = LinearOutSlowInEasing), RepeatMode.Restart),
                label = "wave",
            )
            Box(
                Modifier
                    .size(64.dp)
                    .graphicsLayer {
                        val scale = 1f + pulse * 0.45f
                        scaleX = scale
                        scaleY = scale
                        alpha = 1f - pulse
                    }
                    .background(tint.copy(alpha = 0.3f), CircleShape),
            )
        }
        Box(
            Modifier
                .size(64.dp)
                .background(tint.copy(alpha = 0.16f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            val icon = when {
                paused -> Icons.Rounded.Pause
                active -> Icons.Rounded.GppGood
                else -> Icons.Rounded.Shield
            }
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(34.dp))
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
                Text(stringResource(R.string.setup_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(8.dp))
            if (!setup.hasRole) Text(stringResource(R.string.setup_role))
            if (!setup.hasContacts) Text(stringResource(R.string.setup_contacts))
            Spacer(Modifier.height(12.dp))
            Button(onClick = onClick) { Text(stringResource(R.string.action_grant)) }
        }
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(20.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

/** Grupo de interruptores con título, al estilo de los ajustes del sistema. */
@Composable
private fun SettingsGroup(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column {
        Text(
            title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(start = 8.dp, bottom = 8.dp),
        )
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(vertical = 4.dp), content = content)
        }
    }
}

/** Fila completa tocable: el interruptor se activa tocando cualquier parte. */
@Composable
private fun SettingRow(
    icon: ImageVector,
    accent: Color,
    title: String,
    body: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .toggleable(value = checked, enabled = enabled, role = Role.Switch, onValueChange = onCheckedChange)
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .alpha(if (enabled) 1f else 0.6f),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(40.dp)
                .background(accent.harmonized().copy(alpha = 0.16f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = accent.harmonized(), modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.size(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            Text(body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.size(12.dp))
        Switch(checked = checked, onCheckedChange = null, enabled = enabled)
    }
}

/** Como [SettingRow], pero con un botón de acción en lugar de interruptor. */
@Composable
private fun ActionRow(icon: ImageVector, accent: Color, title: String, body: String, action: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(40.dp)
                .background(accent.harmonized().copy(alpha = 0.16f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = accent.harmonized(), modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.size(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            Text(body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.size(12.dp))
        FilledTonalButton(onClick = onClick) { Text(action) }
    }
}

@Composable
private fun RowDivider() {
    HorizontalDivider(
        Modifier.padding(start = 70.dp, end = 16.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
    )
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
        confirmButton = { TextButton(onClick = { onConfirm(state.hour * 60 + state.minute) }) { Text(stringResource(R.string.action_ok)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}

fun formatMinute(minute: Int) = "%02d:%02d".format(minute / 60, minute % 60)
