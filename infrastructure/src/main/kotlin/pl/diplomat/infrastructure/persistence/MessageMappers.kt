package pl.diplomat.infrastructure.persistence

import pl.diplomat.domain.model.IncomingMessage
import pl.diplomat.domain.model.MessageContent
import pl.diplomat.domain.model.MessageContentType
import pl.diplomat.domain.model.MessageSourceApp
import pl.diplomat.domain.model.MessageStatus
import pl.diplomat.domain.model.VisualMediaKind

internal fun IncomingMessageEntity.toDomain(): IncomingMessage =
    IncomingMessage(
        id = id,
        contactId = contactId,
        content = toMessageContent(contentType, mediaKind, text),
        timestamp = timestamp,
        sourceApp = MessageSourceApp.valueOf(sourceApp),
        status = MessageStatus.valueOf(status),
        notificationKey = notificationKey,
        isRead = isRead,
        isOutgoing = isOutgoing,
    )

internal fun IncomingMessage.toEntity(): IncomingMessageEntity {
    val storage = content.toStorage()
    return IncomingMessageEntity(
        id = id,
        contactId = contactId,
        text = storage.text,
        contentType = storage.contentType,
        mediaKind = storage.mediaKind,
        timestamp = timestamp,
        sourceApp = sourceApp.name,
        status = status.name,
        notificationKey = notificationKey,
        isRead = isRead,
        isOutgoing = isOutgoing,
    )
}

private data class MessageStorage(
    val contentType: String,
    val mediaKind: String,
    val text: String,
)

private fun toMessageContent(contentType: String, mediaKind: String, text: String): MessageContent {
    val kind = VisualMediaKind.valueOf(mediaKind)
    return when (MessageContentType.valueOf(contentType)) {
        MessageContentType.TEXT -> MessageContent.TextOnly(text)
        MessageContentType.IMAGE -> MessageContent.VisualOnly(kind)
        MessageContentType.IMAGE_WITH_TEXT -> MessageContent.VisualWithText(kind, text)
    }
}

private fun MessageContent.toStorage(): MessageStorage = when (this) {
    is MessageContent.TextOnly -> MessageStorage(
        contentType = MessageContentType.TEXT.name,
        mediaKind = VisualMediaKind.PHOTO.name,
        text = body,
    )
    is MessageContent.VisualOnly -> MessageStorage(
        contentType = MessageContentType.IMAGE.name,
        mediaKind = kind.name,
        text = "",
    )
    is MessageContent.VisualWithText -> MessageStorage(
        contentType = MessageContentType.IMAGE_WITH_TEXT.name,
        mediaKind = kind.name,
        text = body,
    )
}
