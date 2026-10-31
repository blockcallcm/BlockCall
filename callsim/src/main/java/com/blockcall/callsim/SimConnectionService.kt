package com.blockcall.callsim

import android.os.Handler
import android.os.Looper
import android.telecom.Connection
import android.telecom.ConnectionRequest
import android.telecom.ConnectionService
import android.telecom.DisconnectCause
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager
import android.util.Log

/** Crea la conexión de la llamada falsa; Telecom la pasa por el filtro de llamadas como cualquier otra. */
class SimConnectionService : ConnectionService() {

    override fun onCreateIncomingConnection(account: PhoneAccountHandle?, request: ConnectionRequest): Connection {
        val hidden = request.extras.getBoolean(EXTRA_HIDDEN, false)
        val connection = SimConnection()
        connection.setAddress(
            request.address,
            if (hidden) TelecomManager.PRESENTATION_RESTRICTED else TelecomManager.PRESENTATION_ALLOWED,
        )
        connection.setInitializing()
        connection.setRinging()
        // Si nadie contesta, se cuelga sola para no dejar llamadas colgadas.
        Handler(Looper.getMainLooper()).postDelayed({
            if (connection.state == Connection.STATE_RINGING) connection.end(DisconnectCause.MISSED, "sin respuesta")
        }, 15_000)
        return connection
    }

    override fun onCreateOutgoingConnection(account: PhoneAccountHandle?, request: ConnectionRequest): Connection {
        val connection = SimConnection()
        connection.setAddress(request.address, TelecomManager.PRESENTATION_ALLOWED)
        connection.setDialing()
        // Se cuelga sola a los pocos segundos: solo importa que Telecom la registre como saliente.
        Handler(Looper.getMainLooper()).postDelayed({ connection.end(DisconnectCause.LOCAL, "saliente terminada") }, 4_000)
        return connection
    }

    override fun onCreateIncomingConnectionFailed(account: PhoneAccountHandle?, request: ConnectionRequest?) {
        Log.e(TriggerReceiver.TAG, "Telecom rechazó crear la llamada")
    }

    private class SimConnection : Connection() {
        override fun onAnswer() {
            Log.i(TriggerReceiver.TAG, "Contestada")
            setActive()
        }

        override fun onReject() = end(DisconnectCause.REJECTED, "rechazada")
        override fun onDisconnect() = end(DisconnectCause.LOCAL, "colgada")
        override fun onAbort() = end(DisconnectCause.CANCELED, "abortada")

        fun end(cause: Int, label: String) {
            Log.i(TriggerReceiver.TAG, "Fin de llamada: $label")
            setDisconnected(DisconnectCause(cause))
            destroy()
        }
    }

    companion object {
        const val EXTRA_HIDDEN = "callsim.hidden"
    }
}
