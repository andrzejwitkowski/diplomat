package pl.diplomat.infrastructure.notification

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import kotlinx.coroutines.launch
import pl.diplomat.domain.model.MessageContent
import pl.diplomat.infrastructure.DiplomatServiceLocator
import pl.diplomat.infrastructure.debug.DevLog
import pl.diplomat.infrastructure.debug.NotificationExtrasSummary

class DiplomatNotificationListenerService : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val packageName = sbn.packageName
        if (!NotificationParser.isSupportedPackage(packageName)) return

        val extras = sbn.notification.extras
        DevLog.log(
            "NOTIF",
            "received pkg=$packageName hasKey=${sbn.key.isNotBlank()} ${NotificationExtrasSummary.format(extras)}",
        )

        val locator = application as? DiplomatServiceLocator
        if (locator == null) {
            DevLog.log("ERROR", "application is not DiplomatServiceLocator")
            return
        }

        val parsedMessages = locator.notificationParser.parse(
            packageName = packageName,
            extras = extras,
            postedAtMillis = sbn.postTime,
            notificationKey = sbn.key,
        )
        if (parsedMessages.isEmpty()) {
            DevLog.log("PARSE", "failed pkg=$packageName")
            return
        }

        locator.applicationScope.launch {
            for (parsed in parsedMessages) {
                DevLog.log(
                    "PARSE",
                    "ok app=${parsed.sourceApp} outgoing=${parsed.isOutgoing} " +
                        "primarySenderLen=${parsed.senderPhone.length} " +
                        "extraCandidates=${parsed.additionalSenderCandidates.size} " +
                        contentMetadata(parsed.content),
                )
                locator.dispatchCapturedMessage(
                    locator.notificationParser.toRaw(parsed),
                    logTag = "RESULT",
                )
            }
        }
    }

    private fun contentMetadata(content: MessageContent): String = when (content) {
        is MessageContent.TextOnly -> "content=text textLen=${content.body.length}"
        is MessageContent.VisualOnly -> "content=visual kind=${content.kind}"
        is MessageContent.VisualWithText ->
            "content=visualWithText kind=${content.kind} textLen=${content.body.length}"
    }
}
