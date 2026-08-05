package pl.diplomat.infrastructure.persistence

import pl.diplomat.domain.model.IncomingMessage
import pl.diplomat.domain.model.MessageSourceApp
import pl.diplomat.domain.model.MessageStatus

internal fun IncomingMessageEntity.toDomain(): IncomingMessage =
    IncomingMessage(
        id = id,
        contactId = contactId,
        text = text,
        timestamp = timestamp,
        sourceApp = MessageSourceApp.valueOf(sourceApp),
        status = MessageStatus.valueOf(status),
    )

internal fun IncomingMessage.toEntity(): IncomingMessageEntity =
    IncomingMessageEntity(
        id = id,
        contactId = contactId,
        text = text,
        timestamp = timestamp,
        sourceApp = sourceApp.name,
        status = status.name,
    )
