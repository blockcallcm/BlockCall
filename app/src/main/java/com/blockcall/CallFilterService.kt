package com.blockcall

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.ContactsContract
import android.telecom.Call
import android.telecom.CallScreeningService
import android.telecom.TelecomManager
import android.util.Log

/**
 * Android enlaza este servicio solo durante el instante en que entra una llamada,
 * decide y lo libera. No hay proceso en segundo plano: consumo de batería nulo.
 */
class CallFilterService : CallScreeningService() {

    override fun onScreenCall(details: Call.Details) {
        val started = System.nanoTime()
        val reason = try {
            decide(details)
        } catch (e: Exception) {
            "error: ${e.javaClass.simpleName}" // Ante cualquier fallo, nunca perder una llamada legítima.
        }
        val block = reason == BLOCK
        val silence = block && try { Store.get(this).settings.value.silence } catch (e: Exception) { false }

        val response = CallResponse.Builder()
        if (silence) {
            // Entra sin sonar ni vibrar: se puede contestar si se está mirando el teléfono; si no, queda como perdida.
            response.setSilenceCall(true)
        } else if (block) {
            // Fuera del registro del teléfono: así otras apps (p. ej. Truecaller) no la anuncian.
            response.setDisallowCall(true)
                .setRejectCall(true)
                .setSkipNotification(true)
                .setSkipCallLog(true)
        }
        respondToCall(details, response.build())

        // Después de responder, para no retrasar la decisión. Un fallo aquí no debe tumbar el servicio.
        if (details.callDirection != Call.Details.DIRECTION_INCOMING) {
            try {
                numberOf(details)?.let { Store.get(this).rememberOutgoing(it) }
            } catch (e: Exception) {
                Log.w(TAG, "No se pudo recordar la llamada saliente", e)
            }
        }
        if (block) {
            try {
                val store = Store.get(this)
                store.addToLog(numberOf(details) ?: Store.HIDDEN, silenced = silence)
                BlockedNotifier.show(this, store)
            } catch (e: Exception) {
                Log.w(TAG, "No se pudo registrar la llamada bloqueada", e)
            }
        }

        // Sin el número: el registro técnico del sistema no debe guardar datos personales.
        val ms = (System.nanoTime() - started) / 1_000_000
        val verdict = when {
            silence -> "SILENCIADA"
            block -> "BLOQUEADA"
            else -> "PERMITIDA ($reason)"
        }
        Log.i(TAG, "$verdict en $ms ms")
    }

    /** Devuelve [BLOCK] o el motivo por el que la llamada se deja pasar. */
    private fun decide(details: Call.Details): String {
        if (details.callDirection != Call.Details.DIRECTION_INCOMING) return "saliente"

        val store = Store.get(this)
        val settings = store.settings.value
        if (!settings.enabled) return "protección apagada"
        // Pausa temporal ("espero una llamada"): entra todo, también internacionales y ocultos.
        if (settings.isPaused()) return "en pausa"
        val number = numberOf(details)

        // Internacionales: se rechazan también fuera del horario y aunque insistan.
        if (settings.blockForeign && number != null && isForeign(number) && !isKnown(store, number)) return BLOCK

        if (!store.isFilteringNow()) return "filtro inactivo"

        val mode = settings.allowMode
        if (mode == ALLOW_NONE) return BLOCK // Modo "Nadie": no entra ninguna llamada, ni insistentes.

        if (number == null) return BLOCK // Número oculto o privado.

        if (store.isAllowed(number)) return "lista de permitidos"

        // Modo "Mi lista": los contactos no entran por el solo hecho de serlo.
        if (mode == ALLOW_CONTACTS) {
            // Sin permiso de contactos no podemos distinguirlos: mejor dejar pasar.
            if (!hasContactsPermission()) return "sin permiso de contactos"
            if (isContact(number)) return "contacto"
        }
        if (store.isRecentlyCalled(number)) return "le llamaste" // Te devuelven la llamada.
        if (store.isRepeatedCall(number)) return "insistente" // La segunda llamada entra.
        return BLOCK
    }

    private fun isForeign(number: String): Boolean {
        val own = CountryCode.of(this) ?: return false // Sin país detectado no hay forma de saberlo.
        return CountryCode.isForeign(number, own)
    }

    /** Contacto, permitido o número al que llamaste: entra aunque sea de otro país. */
    private fun isKnown(store: Store, number: String): Boolean =
        store.isAllowed(number) || store.isRecentlyCalled(number) ||
            !hasContactsPermission() || isContact(number) // Sin permiso, mejor dejar pasar.

    private fun hasContactsPermission() =
        checkSelfPermission(Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED

    private fun numberOf(details: Call.Details): String? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
            details.handlePresentation != TelecomManager.PRESENTATION_ALLOWED
        ) return null
        return details.handle?.schemeSpecificPart?.takeIf { it.isNotBlank() }
    }

    private fun isContact(number: String): Boolean {
        val uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number))
        contentResolver.query(uri, arrayOf(ContactsContract.PhoneLookup._ID), null, null, null)
            ?.use { return it.moveToFirst() }
        return false
    }

    private companion object {
        const val TAG = "BlockCall"
        const val BLOCK = "bloquear"
    }
}
