package pl.diplomat.infrastructure.notification

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import kotlinx.coroutines.launch
import pl.diplomat.domain.model.MessageContent
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
            "received pkg=$packageName hasKey=${sbn.key.isNotBlank()} ${NotificationExtrasSummary.format(extras)}",
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
            "ok app=${parsed.sourceApp} primarySenderLen=${parsed.senderPhone.length} " +
                "extraCandidates=${parsed.additionalSenderCandidates.size} " +
                contentMetadata(parsed.content),
        )

        val raw = locator.notificationParser.toRaw(parsed)
        locator.applicationScope.launch {
            when (val result = locator.processIncomingMessage(raw)) {
                is ProcessIncomingMessageResult.Saved -> {
                    DevLog.log(
                        "RESULT",
                        "saved contactId=${result.contact.id} status=${result.message.status} " +
                            "messageId=${result.message.id}",
                    )
                    locator.incomingMessageNotifier.notify(result.contact, result.message)
                }
                ProcessIncomingMessageResult.RejectedNotWhitelisted ->
                    DevLog.log(
                        "RESULT",
                        "rejected primarySenderLen=${raw.senderPhone.length} " +
                            "extraCandidates=${raw.additionalSenderCandidates.size}",
                    )
                ProcessIncomingMessageResult.IgnoredDuplicate ->
                    DevLog.log("RESULT", "duplicate")
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
