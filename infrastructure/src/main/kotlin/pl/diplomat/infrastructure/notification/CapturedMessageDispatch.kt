package pl.diplomat.infrastructure.notification

import pl.diplomat.infrastructure.DiplomatServiceLocator
import pl.diplomat.infrastructure.debug.DevLog
import pl.diplomat.usecase.ProcessIncomingMessageResult
import pl.diplomat.usecase.RawIncomingMessage

internal suspend fun DiplomatServiceLocator.dispatchCapturedMessage(
    raw: RawIncomingMessage,
    logTag: String,
) {
    when (val result = processIncomingMessage(raw)) {
        is ProcessIncomingMessageResult.Saved -> {
            DevLog.log(logTag, "saved outgoing=${result.message.isOutgoing} status=${result.message.status}")
            if (!result.message.isOutgoing) {
                incomingMessageNotifier.notify(result.contact, result.message)
            }
        }
        ProcessIncomingMessageResult.RejectedNotWhitelisted ->
            DevLog.log(
                logTag,
                "rejected outgoing=${raw.isOutgoing} primarySenderLen=${raw.senderPhone.length} " +
                    "extraCandidates=${raw.additionalSenderCandidates.size}",
            )
        ProcessIncomingMessageResult.IgnoredDuplicate ->
            DevLog.log(logTag, "duplicate outgoing=${raw.isOutgoing}")
    }
}
