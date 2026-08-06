package pl.diplomat.infrastructure.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import pl.diplomat.domain.model.IncomingMessage
import pl.diplomat.domain.model.MessageStatus
import pl.diplomat.domain.model.WhitelistedContact
import pl.diplomat.domain.model.bodyText
import pl.diplomat.infrastructure.R

class IncomingMessageNotifier(private val context: Context) {

    fun notify(contact: WhitelistedContact, message: IncomingMessage) {
        if (message.status != MessageStatus.PENDING) return
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_email)
            .setContentTitle(contact.displayName)
            .setContentText(message.content.bodyText().orEmpty().ifBlank { context.getString(R.string.message_preview_image) })
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setAutoCancel(true)
            .setContentIntent(openAppPendingIntent())
            .build()

        NotificationManagerCompat.from(context).notify(
            (MESSAGE_NOTIFICATION_ID_BASE + message.id).toInt(),
            notification,
        )
    }

    private fun openAppPendingIntent(): PendingIntent {
        val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
            ?: Intent()
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        return PendingIntent.getActivity(context, 0, launchIntent, flags)
    }

    companion object {
        const val CHANNEL_ID = "incoming_messages"
        private const val MESSAGE_NOTIFICATION_ID_BASE = 10_000

        fun ensureChannel(context: Context) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
            val manager = context.getSystemService(NotificationManager::class.java) ?: return
            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.incoming_message_channel_name),
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = context.getString(R.string.incoming_message_channel_description)
            }
            manager.createNotificationChannel(channel)
        }
    }
}
