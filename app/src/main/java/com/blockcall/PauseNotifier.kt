package com.blockcall

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Aviso fijo mientras dura la pausa: que nunca se olvide que están entrando todas las llamadas.
 * Lleva una cuenta atrás y se retira solo al terminar (setTimeoutAfter), sin nada en segundo plano.
 */
object PauseNotifier {

    private const val CHANNEL = "pause_notice"
    private const val ID = 2

    /** Muestra, actualiza o retira el aviso según el estado de la pausa. */
    fun sync(context: Context, settings: Settings) {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.deleteNotificationChannel("pause") // Canal de una versión de prueba (silencioso, sin icono).
        val left = settings.pausedUntil - System.currentTimeMillis()
        if (!settings.isPaused() || !BlockedNotifier.canNotify(context)) {
            manager.cancel(ID)
            return
        }
        manager.createNotificationChannel(
            // Importancia normal para que el icono se vea en la barra de estado, pero sin sonido ni vibración.
            NotificationChannel(CHANNEL, context.getString(R.string.pause_channel_name), NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = context.getString(R.string.pause_channel_description)
                setSound(null, null)
                enableVibration(false)
            },
        )

        val open = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val resume = PendingIntent.getBroadcast(
            context,
            0,
            Intent(context, ResumeReceiver::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        val notification = Notification.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_tile_pause)
            .setContentTitle(context.getString(R.string.pause_notification_title))
            .setContentText(context.getString(R.string.status_paused_body, Tiles.until(context, settings.pausedUntil)))
            // Cuenta atrás hasta el final de la pausa, dibujada por el sistema.
            .setWhen(settings.pausedUntil)
            .setShowWhen(true)
            .setUsesChronometer(true)
            .setChronometerCountDown(true)
            .setTimeoutAfter(left)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(open)
            .addAction(Notification.Action.Builder(null, context.getString(R.string.action_resume), resume).build())
            .setCategory(Notification.CATEGORY_STATUS)
            .build()
        manager.notify(ID, notification)
    }
}

/** Botón "Reanudar ahora" del aviso de pausa. */
class ResumeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) = Store.get(context).resume()
}
