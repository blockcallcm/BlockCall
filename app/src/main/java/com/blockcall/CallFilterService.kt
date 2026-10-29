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

        val response = CallResponse.Builder()
        if (block) {
            // Fuera del registro del teléfono: así otras apps (p. ej. Truecaller) no la anuncian.
            response.setDisallowCall(true)
                .setRejectCall(true)
                .setSkipNotification(true)
                .setSkipCallLog(true)
        }
        respondToCall(details, response.build())

        // Después de responder, para no retrasar la decisión. Un fallo aquí no debe tumbar el servicio.
        if (block) {
            try {
                val store = Store.get(this)
                store.addToLog(numberOf(details) ?: Store.HIDDEN)
                BlockedNotifier.show(this, store)
            } catch (e: Exception) {
                Log.w(TAG, "No se pudo registrar la llamada bloqueada", e)
            }
        }

        // Sin el número: el registro técnico del sistema no debe guardar datos personales.
        val ms = (System.nanoTime() - started) / 1_000_000
        Log.i(TAG, "${if (block) "BLOQUEADA" else "PERMITIDA ($reason)"} en $ms ms")
    }

    /** Devuelve [BLOCK] o el motivo por el que la llamada se deja pasar. */
    private fun decide(details: Call.Details): String {
        if (details.callDirection != Call.Details.DIRECTION_INCOMING) return "saliente"

        val store = Store.get(this)
        if (!store.isFilteringNow()) return "filtro inactivo"

        val mode = store.settings.value.allowMode
        if (mode == ALLOW_NONE) return BLOCK // Modo "Nadie": no entra ninguna llamada, ni insistentes.

        val number = numberOf(details) ?: return BLOCK // Número oculto o privado.

        if (store.isAllowed(number)) return "lista de permitidos"

        // Modo "Mi lista": los contactos no entran por el solo hecho de serlo.
        if (mode == ALLOW_CONTACTS) {
            // Sin permiso de contactos no podemos distinguirlos: mejor dejar pasar.
            if (checkSelfPermission(Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
                return "sin permiso de contactos"
            }
            if (isContact(number)) return "contacto"
        }
        if (store.isRepeatedCall(number)) return "insistente" // La segunda llamada entra.
        return BLOCK
    }

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
