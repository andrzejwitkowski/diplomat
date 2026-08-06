package pl.diplomat.infrastructure.persistence

import pl.diplomat.domain.model.IncomingMessage
import pl.diplomat.domain.model.MessageContent
import pl.diplomat.domain.model.MessageContentType
import pl.diplomat.domain.model.MessageSourceApp
import pl.diplomat.domain.model.MessageStatus

internal fun IncomingMessageEntity.toDomain(): IncomingMessage =
    IncomingMessage(
        id = id,
        contactId = contactId,
        content = toMessageContent(contentType, text),
        timestamp = timestamp,
        sourceApp = MessageSourceApp.valueOf(sourceApp),
        status = MessageStatus.valueOf(status),
    )

internal fun IncomingMessage.toEntity(): IncomingMessageEntity {
    val (storedType, storedText) = content.toStorage()
    return IncomingMessageEntity(
        id = id,
        contactId = contactId,
        text = storedText,
        contentType = storedType,
        timestamp = timestamp,
        sourceApp = sourceApp.name,
        status = status.name,
    )
}

private fun toMessageContent(contentType: String, text: String): MessageContent =
    when (MessageContentType.valueOf(contentType)) {
        MessageContentType.TEXT -> MessageContent.TextOnly(text)
        MessageContentType.IMAGE -> MessageContent.ImageOnly
        MessageContentType.IMAGE_WITH_TEXT -> MessageContent.ImageWithText(text)
    }

private fun MessageContent.toStorage(): Pair<String, String> = when (this) {
    is MessageContent.TextOnly -> MessageContentType.TEXT.name to body
    is MessageContent.ImageOnly -> MessageContentType.IMAGE.name to ""
    is MessageContent.ImageWithText -> MessageContentType.IMAGE_WITH_TEXT.name to body
}
