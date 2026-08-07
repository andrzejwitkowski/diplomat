package pl.diplomat.infrastructure.notification

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import kotlinx.coroutines.launch
import pl.diplomat.domain.model.MessageContent
import pl.diplomat.domain.model.bodyText
import pl.diplomat.infrastructure.DiplomatServiceLocator
import pl.diplomat.infrastructure.debug.DevLog
import pl.diplomat.infrastructure.debug.NotificationExtrasSummary
import pl.diplomat.usecase.ProcessIncomingMessageResult

class DiplomatNotificationListenerService : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val packageName = sbn.packageName
        if (!NotificationParser.isSupportedPackage(packageName)) return

        val extras = sbn.notification.extras
        DevLog.log(
            "NOTIF",
            "received pkg=$packageName key=${sbn.key} ${NotificationExtrasSummary.format(extras)}",
        )

        val locator = application as? DiplomatServiceLocator
        if (locator == null) {
            DevLog.log("ERROR", "application is not DiplomatServiceLocator")
            return
        }

        val parsed = locator.notificationParser.parse(
            packageName = packageName,
            extras = extras,
            postedAtMillis = sbn.postTime,
            notificationKey = sbn.key,
        )
        if (parsed == null) {
            DevLog.log("PARSE", "failed pkg=$packageName")
            return
        }

        DevLog.log(
            "PARSE",
            "ok app=${parsed.sourceApp} sender=${parsed.senderPhone} " +
                "candidates=${parsed.additionalSenderCandidates} " +
                "body=${contentPreview(parsed.content)}",
        )

        val raw = locator.notificationParser.toRaw(parsed)
        locator.applicationScope.launch {
            when (val result = locator.processIncomingMessage(raw)) {
                is ProcessIncomingMessageResult.Saved -> {
                    DevLog.log(
                        "RESULT",
                        "saved contact=${result.contact.displayName} " +
                            "status=${result.message.status} id=${result.message.id}",
                    )
                    locator.incomingMessageNotifier.notify(result.contact, result.message)
                }
                ProcessIncomingMessageResult.RejectedNotWhitelisted ->
                    DevLog.log(
                        "RESULT",
                        "rejected sender=${raw.senderPhone} " +
                            "candidates=${raw.additionalSenderCandidates}",
                    )
                ProcessIncomingMessageResult.IgnoredDuplicate ->
                    DevLog.log("RESULT", "duplicate key=${raw.notificationKey}")
            }
        }
    }

    private fun contentPreview(content: MessageContent): String =
        content.bodyText()?.take(120)?.replace('\n', ' ') ?: content.toString()
}
