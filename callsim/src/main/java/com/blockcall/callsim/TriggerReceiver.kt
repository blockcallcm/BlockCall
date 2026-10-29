package com.blockcall.callsim

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.telecom.PhoneAccount
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager
import android.util.Log

class TriggerReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val telecom = context.getSystemService(TelecomManager::class.java)
        val handle = accountHandle(context)

        when (intent.action) {
            "register" -> {
                val account = PhoneAccount.builder(handle, "CallSim")
                    .setCapabilities(PhoneAccount.CAPABILITY_CALL_PROVIDER)
                    .addSupportedUriScheme(PhoneAccount.SCHEME_TEL)
                    .build()
                telecom.registerPhoneAccount(account)
                Log.i(TAG, "Cuenta registrada: ${handle.componentName.flattenToShortString()} ${handle.id}")
            }
            "call" -> {
                val hidden = intent.getBooleanExtra("hidden", false)
                val number = intent.getStringExtra("number").orEmpty()
                val extras = Bundle().apply {
                    putParcelable(TelecomManager.EXTRA_INCOMING_CALL_ADDRESS, Uri.fromParts(PhoneAccount.SCHEME_TEL, number, null))
                    putBoolean(SimConnectionService.EXTRA_HIDDEN, hidden)
                }
                telecom.addNewIncomingCall(handle, extras)
                Log.i(TAG, "Llamada inyectada: ${if (hidden) "oculto" else number}")
            }
        }
    }

    companion object {
        const val TAG = "CallSim"

        fun accountHandle(context: Context) =
            PhoneAccountHandle(ComponentName(context, SimConnectionService::class.java), "sim")
    }
}
