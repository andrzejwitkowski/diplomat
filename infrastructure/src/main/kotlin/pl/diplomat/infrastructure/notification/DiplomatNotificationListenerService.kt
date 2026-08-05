package pl.diplomat.infrastructure.notification

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import pl.diplomat.infrastructure.DiplomatServiceLocator
import pl.diplomat.usecase.ProcessIncomingMessageResult

class DiplomatNotificationListenerService : NotificationListenerService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val packageName = sbn.packageName
        if (!NotificationParser.isSupportedPackage(packageName)) return

        val parsed = NotificationParser.parse(
            packageName = packageName,
            extras = sbn.notification.extras,
            postedAtMillis = sbn.postTime,
        ) ?: return

        val locator = application as? DiplomatServiceLocator ?: return

        serviceScope.launch {
            when (locator.processIncomingMessage(NotificationParser.toRaw(parsed))) {
                is ProcessIncomingMessageResult.Saved -> Unit
                ProcessIncomingMessageResult.RejectedNotWhitelisted -> Unit
            }
        }
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }
}
