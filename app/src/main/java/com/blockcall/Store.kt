package com.blockcall

import android.content.Context
import android.content.SharedPreferences
import android.telephony.PhoneNumberUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.util.Calendar

data class AllowedNumber(val number: String, val name: String)

data class BlockedCall(val number: String, val time: Long, val silenced: Boolean = false)

data class Settings(
    val enabled: Boolean = false,
    val scheduled: Boolean = false,
    val startMinute: Int = 20 * 60,
    val endMinute: Int = 6 * 60,
    val allowRepeated: Boolean = true,
    val allowMode: Int = ALLOW_CONTACTS,
    val notify: Boolean = true,
    val lastSeen: Long = 0,
    val theme: Int = THEME_SYSTEM,
    val onboarded: Boolean = false,
    val silence: Boolean = false,
    val allowRecent: Boolean = true,
    val blockForeign: Boolean = false,
    val pausedUntil: Long = 0,
    val pausedAt: Long = 0,
    val pauseMinutes: Int = 0,
) {
    /** Pausa temporal en curso: entran todas las llamadas hasta [pausedUntil]. */
    fun isPaused(now: Long = System.currentTimeMillis()) = enabled && pausedUntil > now
}

/** Quién puede llamar mientras el filtro está activo. */
const val ALLOW_CONTACTS = 0 // Contactos + lista de permitidos
const val ALLOW_LIST = 1 // Solo la lista de permitidos
const val ALLOW_NONE = 2 // Nadie

const val THEME_SYSTEM = 0
const val THEME_LIGHT = 1
const val THEME_DARK = 2

/**
 * Persistencia mínima en SharedPreferences: lectura síncrona y rápida,
 * ideal para responder a la llamada en milisegundos sin librerías extra.
 */
class Store private constructor(context: Context) {

    private val app = context.applicationContext
    private val prefs: SharedPreferences = app.getSharedPreferences("blockcall", Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(readSettings())
    val settings: StateFlow<Settings> = _settings

    private val _allowed = MutableStateFlow(readAllowed())
    val allowed: StateFlow<List<AllowedNumber>> = _allowed

    private val _log = MutableStateFlow(readLog())
    val log: StateFlow<List<BlockedCall>> = _log

    // Todas las escrituras pasan por aquí y refrescan su flujo directamente
    // (los listeners de SharedPreferences son referencias débiles y R8 los pierde).
    // sync = escritura inmediata a disco (commit). La usa el servicio: tras bloquear, OnePlus
    // puede congelar el proceso antes de que termine una escritura diferida (apply) y se pierde.
    private fun edit(sync: Boolean = false, block: SharedPreferences.Editor.() -> Unit) {
        val editor = prefs.edit().apply(block)
        if (sync) editor.commit() else editor.apply()
        _settings.value = readSettings()
        _allowed.value = readAllowed()
        _log.value = readLog()
    }

    // --- Ajustes ---

    private fun readSettings() = Settings(
        enabled = prefs.getBoolean(KEY_ENABLED, false),
        scheduled = prefs.getBoolean(KEY_SCHEDULED, false),
        startMinute = prefs.getInt(KEY_START, 20 * 60),
        endMinute = prefs.getInt(KEY_END, 6 * 60),
        allowRepeated = prefs.getBoolean(KEY_REPEATED, true),
        // Compatibilidad: versiones anteriores guardaban solo el interruptor "Solo mi lista".
        allowMode = prefs.getInt(KEY_ALLOW_MODE, if (prefs.getBoolean(KEY_ONLY_LIST, false)) ALLOW_LIST else ALLOW_CONTACTS),
        notify = prefs.getBoolean(KEY_NOTIFY, true),
        lastSeen = prefs.getLong(KEY_LAST_SEEN, 0),
        theme = prefs.getInt(KEY_THEME, THEME_SYSTEM),
        onboarded = prefs.getBoolean(KEY_ONBOARDED, false),
        silence = prefs.getBoolean(KEY_SILENCE, false),
        allowRecent = prefs.getBoolean(KEY_RECENT_ON, true),
        blockForeign = prefs.getBoolean(KEY_FOREIGN, false),
        pausedUntil = prefs.getLong(KEY_PAUSED_UNTIL, 0),
        pausedAt = prefs.getLong(KEY_PAUSED_AT, 0),
        pauseMinutes = prefs.getInt(KEY_PAUSE_MINUTES, 0),
    )

    // Encender o apagar la protección cancela cualquier pausa pendiente.
    fun setEnabled(value: Boolean) {
        edit { putBoolean(KEY_ENABLED, value).putLong(KEY_PAUSED_UNTIL, 0) }
        onPauseChanged()
    }

    /** Botones de la cortina y aviso de pausa reflejan el estado nuevo. */
    private fun onPauseChanged() {
        Tiles.refresh(app)
        PauseNotifier.sync(app, readSettings())
    }
    /** Deja pasar todas las llamadas durante [minutes]; después la protección vuelve sola. */
    fun pause(minutes: Int) = pauseUntil(System.currentTimeMillis() + minutes * 60_000L, minutes)

    /** Pausa hasta una hora elegida por el usuario; [minutes] = 0 si no es una de [PAUSE_OPTIONS]. */
    fun pauseUntil(time: Long, minutes: Int = 0) {
        edit {
            putLong(KEY_PAUSED_UNTIL, time)
            putLong(KEY_PAUSED_AT, System.currentTimeMillis())
            putInt(KEY_PAUSE_MINUTES, minutes)
        }
        onPauseChanged()
    }

    fun resume() {
        edit { putLong(KEY_PAUSED_UNTIL, 0) }
        onPauseChanged()
    }

    fun setScheduled(value: Boolean) = edit { putBoolean(KEY_SCHEDULED, value) }
    fun setStart(minute: Int) = edit { putInt(KEY_START, minute) }
    fun setEnd(minute: Int) = edit { putInt(KEY_END, minute) }
    fun setAllowRepeated(value: Boolean) = edit { putBoolean(KEY_REPEATED, value) }
    fun setAllowMode(value: Int) = edit { putInt(KEY_ALLOW_MODE, value) }
    fun setNotify(value: Boolean) = edit { putBoolean(KEY_NOTIFY, value) }
    fun setSilence(value: Boolean) = edit { putBoolean(KEY_SILENCE, value) }
    fun setBlockForeign(value: Boolean) = edit { putBoolean(KEY_FOREIGN, value) }

    // Al apagarlo se olvidan los números guardados: no conservar más datos de los necesarios.
    fun setAllowRecent(value: Boolean) = edit {
        putBoolean(KEY_RECENT_ON, value)
        if (!value) putString(KEY_RECENT, "[]")
    }

    /** Marca el historial como visto: reinicia el contador de la pestaña y de la notificación. */
    fun markSeen() = edit { putLong(KEY_LAST_SEEN, System.currentTimeMillis()) }
    fun setTheme(value: Int) = edit { putInt(KEY_THEME, value) }
    fun setOnboarded() = edit { putBoolean(KEY_ONBOARDED, true) }

    /** Fin del asistente: protección encendida con los valores recomendados. */
    fun finishOnboarding() = edit {
        putBoolean(KEY_ONBOARDED, true)
        putBoolean(KEY_ENABLED, true)
        putBoolean(KEY_SCHEDULED, false)
        putInt(KEY_ALLOW_MODE, ALLOW_CONTACTS)
        putBoolean(KEY_REPEATED, true)
        putBoolean(KEY_NOTIFY, true)
        putBoolean(KEY_RECENT_ON, true)
    }

    /** Llamada insistente: el mismo número ya fue bloqueado hace menos de [REPEAT_WINDOW_MS]. */
    fun isRepeatedCall(number: String): Boolean {
        if (!readSettings().allowRepeated) return false
        val since = System.currentTimeMillis() - REPEAT_WINDOW_MS
        return readLog().any { it.time >= since && it.number != HIDDEN && PhoneNumberUtils.compare(it.number, number) }
    }

    /** ¿Debe filtrarse en este momento? Contempla horarios que cruzan la medianoche. */
    fun isFilteringNow(now: Calendar = Calendar.getInstance()): Boolean {
        val s = readSettings()
        if (!s.enabled) return false
        if (!s.scheduled || s.startMinute == s.endMinute) return true
        val minute = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
        return if (s.startMinute < s.endMinute) {
            minute >= s.startMinute && minute < s.endMinute
        } else {
            minute >= s.startMinute || minute < s.endMinute
        }
    }

    // --- Números a los que llamaste ---

    // Solo en el teléfono y solo [RECENT_WINDOW_MS]: lo justo para dejar pasar la devolución de una llamada.
    private fun readRecent(): List<BlockedCall> = try {
        val arr = JSONArray(prefs.getString(KEY_RECENT, "[]"))
        List(arr.length()) {
            val o = arr.getJSONObject(it)
            BlockedCall(o.getString("n"), o.getLong("t"))
        }
    } catch (_: JSONException) {
        emptyList()
    }

    /** Recuerda un número que marcó el usuario, si la opción está activa. */
    fun rememberOutgoing(number: String) {
        if (!readSettings().allowRecent) return
        val now = System.currentTimeMillis()
        val list = (listOf(BlockedCall(number, now)) +
            readRecent().filter { now - it.time < RECENT_WINDOW_MS && !PhoneNumberUtils.compare(it.number, number) })
            .take(MAX_RECENT)
        val arr = JSONArray()
        list.forEach { arr.put(JSONObject().put("n", it.number).put("t", it.time)) }
        edit(sync = true) { putString(KEY_RECENT, arr.toString()) }
    }

    /** ¿El usuario llamó a este número en los últimos [RECENT_WINDOW_MS]? */
    fun isRecentlyCalled(number: String): Boolean {
        if (!readSettings().allowRecent) return false
        val since = System.currentTimeMillis() - RECENT_WINDOW_MS
        return readRecent().any { it.time >= since && PhoneNumberUtils.compare(it.number, number) }
    }

    // --- Lista de permitidos ---

    private fun readAllowed(): List<AllowedNumber> {
        val arr = JSONArray(prefs.getString(KEY_ALLOWED, "[]"))
        return List(arr.length()) {
            val o = arr.getJSONObject(it)
            AllowedNumber(o.getString("n"), o.optString("name"))
        }
    }

    private fun writeAllowed(list: List<AllowedNumber>) {
        val arr = JSONArray()
        list.forEach { arr.put(JSONObject().put("n", it.number).put("name", it.name)) }
        edit { putString(KEY_ALLOWED, arr.toString()) }
    }

    fun addAllowed(number: String, name: String) {
        val clean = number.trim()
        if (clean.isEmpty() || isAllowed(clean)) return
        writeAllowed(listOf(AllowedNumber(clean, name.trim())) + readAllowed())
    }

    fun removeAllowed(number: String) = writeAllowed(readAllowed().filterNot { it.number == number })

    fun isAllowed(number: String): Boolean =
        readAllowed().any { PhoneNumberUtils.compare(it.number, number) }

    // --- Historial de bloqueadas ---

    // El historial no es crítico: si estuviera dañado se descarta en vez de romper el filtro.
    private fun readLog(): List<BlockedCall> = try {
        val arr = JSONArray(prefs.getString(KEY_LOG, "[]"))
        List(arr.length()) {
            val o = arr.getJSONObject(it)
            BlockedCall(o.getString("n"), o.getLong("t"), o.optBoolean("s"))
        }
    } catch (_: JSONException) {
        emptyList()
    }

    fun addToLog(number: String, silenced: Boolean = false) {
        val list = (listOf(BlockedCall(number, System.currentTimeMillis(), silenced)) + readLog()).take(MAX_LOG)
        val arr = JSONArray()
        list.forEach { arr.put(JSONObject().put("n", it.number).put("t", it.time).apply { if (it.silenced) put("s", true) }) }
        edit(sync = true) { putString(KEY_LOG, arr.toString()) }
    }

    fun clearLog() = edit { putString(KEY_LOG, "[]") }

    companion object {
        private const val KEY_ENABLED = "enabled"
        private const val KEY_SCHEDULED = "scheduled"
        private const val KEY_START = "start"
        private const val KEY_END = "end"
        private const val KEY_REPEATED = "repeated"
        private const val KEY_THEME = "theme"
        private const val KEY_ONLY_LIST = "only_list"
        private const val KEY_ALLOW_MODE = "allow_mode"
        private const val KEY_NOTIFY = "notify"
        private const val KEY_LAST_SEEN = "last_seen"
        private const val KEY_ONBOARDED = "onboarded"
        const val REPEAT_WINDOW_MS = 3 * 60 * 1000L
        private const val KEY_ALLOWED = "allowed"
        private const val KEY_LOG = "log"
        private const val MAX_LOG = 100
        private const val KEY_SILENCE = "silence"
        private const val KEY_FOREIGN = "block_foreign"
        private const val KEY_RECENT_ON = "allow_recent"
        private const val KEY_RECENT = "recent"
        private const val MAX_RECENT = 50
        const val RECENT_WINDOW_MS = 7 * 24 * 60 * 60 * 1000L
        private const val KEY_PAUSED_UNTIL = "paused_until"
        private const val KEY_PAUSED_AT = "paused_at"
        private const val KEY_PAUSE_MINUTES = "pause_minutes"

        /** Duraciones de la pausa temporal, en minutos. */
        val PAUSE_OPTIONS = listOf(30, 60, 120)

        /** Número vacío en el historial = llamada de número oculto. */
        const val HIDDEN = ""

        @Volatile private var instance: Store? = null

        fun get(context: Context): Store =
            instance ?: synchronized(this) { instance ?: Store(context).also { instance = it } }
    }
}
