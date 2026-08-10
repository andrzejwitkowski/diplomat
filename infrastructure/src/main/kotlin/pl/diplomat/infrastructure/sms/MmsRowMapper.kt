package pl.diplomat.infrastructure.sms

import android.provider.Telephony
import pl.diplomat.domain.model.MessageContent
import pl.diplomat.domain.model.MessageSourceApp
import pl.diplomat.domain.model.VisualMediaKind
import pl.diplomat.usecase.RawIncomingMessage

data class MmsPartsContent(
    val text: String?,
    val mediaKind: VisualMediaKind?,
)

object MmsRowMapper {
    fun isOutgoing(messageBox: Int): Boolean =
        messageBox == Telephony.Mms.MESSAGE_BOX_SENT

    /** Outbox rows keep the same `_id` when they become sent — do not checkpoint past them. */
    fun isPendingOutbound(messageBox: Int): Boolean =
        messageBox == Telephony.Mms.MESSAGE_BOX_OUTBOX

    fun mediaKindFromContentType(contentType: String): VisualMediaKind? {
        val normalized = contentType.trim().lowercase()
        return when {
            normalized == "image/gif" -> VisualMediaKind.GIF
            normalized.startsWith("image/") -> VisualMediaKind.PHOTO
            normalized.startsWith("video/") -> VisualMediaKind.VIDEO
            else -> null
        }
    }

    fun resolveParts(parts: List<Pair<String, String?>>): MmsPartsContent {
        val textChunks = mutableListOf<String>()
        var mediaKind: VisualMediaKind? = null
        for ((contentType, text) in parts) {
            if (contentType.startsWith("text/", ignoreCase = true)) {
                text?.trim()?.takeIf { it.isNotEmpty() }?.let { textChunks.add(it) }
            }
            if (mediaKind == null) {
                mediaKind = mediaKindFromContentType(contentType)
            }
        }
        return MmsPartsContent(
            text = textChunks.joinToString("\n").takeIf { it.isNotBlank() },
            mediaKind = mediaKind,
        )
    }

    fun toRaw(
        id: Long,
        address: String,
        parts: MmsPartsContent,
        dateSeconds: Long,
        messageBox: Int,
    ): RawIncomingMessage? {
        if (messageBox != Telephony.Mms.MESSAGE_BOX_INBOX && messageBox != Telephony.Mms.MESSAGE_BOX_SENT) {
            return null
        }
        val phone = address.trim()
        if (phone.isEmpty() || dateSeconds <= 0L || id <= 0L) return null
        val text = parts.text?.trim()?.takeIf { it.isNotEmpty() }
        val content = when {
            parts.mediaKind != null && text != null -> MessageContent.VisualWithText(parts.mediaKind, text)
            parts.mediaKind != null -> MessageContent.VisualOnly(parts.mediaKind)
            text != null -> MessageContent.TextOnly(text)
            else -> return null
        }
        return RawIncomingMessage(
            senderPhone = phone,
            content = content,
            timestamp = dateSeconds * 1_000L,
            sourceApp = MessageSourceApp.SMS,
            notificationKey = "mms:$id",
            isOutgoing = isOutgoing(messageBox),
        )
    }
}
