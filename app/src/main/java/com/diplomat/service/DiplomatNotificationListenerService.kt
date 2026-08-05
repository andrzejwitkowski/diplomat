package com.diplomat.service

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.diplomat.DiplomatApplication
import com.diplomat.core.ContactWhitelist
import com.diplomat.data.repository.MessageRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Captures notifications from whitelisted messaging apps (SMS, WhatsApp),
 * extracts sender/body/timestamp/package and hands them to the repository.
 *
 * The user must grant "Notification access" in system settings before Android
 * will bind this service (see [com.diplomat.util.PermissionUtils]).
 */
class DiplomatNotificationListenerService : NotificationListenerService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val repository: MessageRepository
        get() = (application as DiplomatApplication).container.messageRepository

    override fun onListenerConnected() {
        super.onListenerConnected()
        // Keep the process alive so we do not miss notifications in the background.
        NotificationRelayForegroundService.start(this)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        val notification = sbn ?: return
        val packageName = notification.packageName

        if (!ContactWhitelist.allowsPackage(packageName)) return

        val captured = extract(notification) ?: return
        if (!ContactWhitelist.allowsSender(captured.sender)) return

        scope.launch {
            runCatching {
                repository.recordIncoming(
                    sender = captured.sender,
                    body = captured.body,
                    packageName = packageName,
                    timestamp = captured.timestamp,
                )
            }.onFailure { Log.e(TAG, "Failed to persist captured message", it) }
        }
    }

    /**
     * Pulls the sender (title), body (text) and timestamp out of the
     * notification's [Bundle]. Returns null when the notification carries no
     * usable text (e.g. summary/group notifications).
     */
    private fun extract(sbn: StatusBarNotification): Captured? {
        val extras = sbn.notification?.extras ?: return null

        val sender = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.trim()
        val body = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()?.trim()

        if (sender.isNullOrEmpty() || body.isNullOrEmpty()) return null

        return Captured(
            sender = sender,
            body = body,
            timestamp = if (sbn.postTime > 0) sbn.postTime else System.currentTimeMillis(),
        )
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private data class Captured(
        val sender: String,
        val body: String,
        val timestamp: Long,
    )

    private companion object {
        const val TAG = "DiplomatListener"
    }
}
