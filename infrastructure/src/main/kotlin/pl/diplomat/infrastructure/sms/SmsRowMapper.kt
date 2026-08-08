package pl.diplomat.infrastructure.sms

import android.provider.Telephony
import pl.diplomat.domain.model.MessageContent
import pl.diplomat.domain.model.MessageSourceApp
import pl.diplomat.usecase.RawIncomingMessage

object SmsRowMapper {
    fun isOutgoing(type: Int): Boolean =
        type == Telephony.Sms.MESSAGE_TYPE_SENT

    /** Outbox/queued rows keep the same `_id` when they become sent — do not checkpoint past them. */
    fun isPendingOutbound(type: Int): Boolean =
        type == Telephony.Sms.MESSAGE_TYPE_OUTBOX || type == Telephony.Sms.MESSAGE_TYPE_QUEUED

    fun toRaw(
        id: Long,
        address: String,
        body: String,
        date: Long,
        type: Int,
    ): RawIncomingMessage? {
        if (type != Telephony.Sms.MESSAGE_TYPE_INBOX && type != Telephony.Sms.MESSAGE_TYPE_SENT) {
            return null
        }
        val phone = address.trim()
        val text = body.trim()
        if (phone.isEmpty() || text.isEmpty() || date <= 0L || id <= 0L) return null
        return RawIncomingMessage(
            senderPhone = phone,
            content = MessageContent.TextOnly(text),
            timestamp = date,
            sourceApp = MessageSourceApp.SMS,
            notificationKey = "sms:$id",
            isOutgoing = isOutgoing(type),
        )
    }
}
