package pl.diplomat.infrastructure.notification

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import kotlinx.coroutines.launch
import pl.diplomat.infrastructure.DiplomatServiceLocator
import pl.diplomat.usecase.ProcessIncomingMessageResult

class DiplomatNotificationListenerService : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val packageName = sbn.packageName
        if (!NotificationParser.isSupportedPackage(packageName)) return

        val locator = application as? DiplomatServiceLocator ?: return
        val parsed = locator.notificationParser.parse(
            packageName = packageName,
            extras = sbn.notification.extras,
            postedAtMillis = sbn.postTime,
            notificationKey = sbn.key,
        ) ?: return

        val raw = locator.notificationParser.toRaw(parsed)
        locator.applicationScope.launch {
            when (val result = locator.processIncomingMessage(raw)) {
                is ProcessIncomingMessageResult.Saved ->
                    locator.incomingMessageNotifier.notify(result.contact, result.message)
                else -> Unit
            }
        }
    }
}
