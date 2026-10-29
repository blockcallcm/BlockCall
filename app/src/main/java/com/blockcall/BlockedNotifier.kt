package com.blockcall

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.text.format.DateFormat
import java.util.Date

/**
 * Una única notificación silenciosa que resume las llamadas bloqueadas sin ver.
 * Solo se crea cuando se bloquea una llamada: no hay nada corriendo en segundo plano.
 */
object BlockedNotifier {

    private const val CHANNEL = "blocked"
    private const val ID = 1

    fun canNotify(context: Context) =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    fun show(context: Context, store: Store) {
        val settings = store.settings.value
        if (!settings.notify || !canNotify(context)) return
        val unseen = store.log.value.filter { it.time > settings.lastSeen }
        val last = unseen.firstOrNull() ?: return

        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL, context.getString(R.string.channel_name), NotificationManager.IMPORTANCE_LOW).apply {
                description = context.getString(R.string.channel_description)
            },
        )

        val who = if (last.number == Store.HIDDEN) context.getString(R.string.hidden_number) else last.number
        val time = DateFormat.getTimeFormat(context).format(Date(last.time))
        val open = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java)
                .putExtra(MainActivity.EXTRA_TAB, MainActivity.TAB_BLOCKED)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        val notification = Notification.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_blocked)
            .setContentTitle(context.resources.getQuantityString(R.plurals.notification_title, unseen.size, unseen.size))
            .setContentText(context.getString(R.string.notification_text, who, time))
            .setNumber(unseen.size)
            .setContentIntent(open)
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .setCategory(Notification.CATEGORY_STATUS)
            .build()
        manager.notify(ID, notification)
    }

    fun cancel(context: Context) = context.getSystemService(NotificationManager::class.java).cancel(ID)
}
