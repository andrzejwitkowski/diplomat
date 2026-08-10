package pl.diplomat.infrastructure.sms

import android.provider.Telephony
import pl.diplomat.domain.model.MessageContent
import pl.diplomat.domain.model.MessageSourceApp
import pl.diplomat.usecase.RawIncomingMessage

object MmsRowMapper {
    fun isOutgoing(messageBox: Int): Boolean =
        messageBox == Telephony.Mms.MESSAGE_BOX_SENT

    fun toRaw(
        id: Long,
        address: String,
        body: String,
        dateSeconds: Long,
        messageBox: Int,
    ): RawIncomingMessage? {
        if (messageBox != Telephony.Mms.MESSAGE_BOX_INBOX && messageBox != Telephony.Mms.MESSAGE_BOX_SENT) {
            return null
        }
        val phone = address.trim()
        val text = body.trim()
        if (phone.isEmpty() || text.isEmpty() || dateSeconds <= 0L || id <= 0L) return null
        return RawIncomingMessage(
            senderPhone = phone,
            content = MessageContent.TextOnly(text),
            timestamp = dateSeconds * 1_000L,
            sourceApp = MessageSourceApp.SMS,
            notificationKey = "mms:$id",
            isOutgoing = isOutgoing(messageBox),
        )
    }
}
