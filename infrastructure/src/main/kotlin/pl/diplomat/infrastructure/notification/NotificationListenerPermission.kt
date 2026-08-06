package pl.diplomat.infrastructure.notification

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings

object NotificationListenerPermission {
    fun isGranted(context: Context): Boolean {
        val enabledListeners = Settings.Secure.getString(
            context.contentResolver,
            "enabled_notification_listeners",
        ) ?: return false
        val component = ComponentName(context, DiplomatNotificationListenerService::class.java)
        return enabledListeners.split(':').any { it.equals(component.flattenToString(), ignoreCase = true) }
    }

    fun settingsIntent(): Intent =
        Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
}
