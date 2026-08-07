package pl.diplomat.infrastructure.adapter

import pl.diplomat.domain.model.ConversationThread
import pl.diplomat.domain.model.IncomingMessage
import pl.diplomat.domain.port.MessageRepositoryPort
import pl.diplomat.infrastructure.persistence.IncomingMessageDao
import pl.diplomat.infrastructure.persistence.WhitelistedContactDao
import pl.diplomat.infrastructure.persistence.toDomain
import pl.diplomat.infrastructure.persistence.toEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class RoomMessageRepositoryAdapter(
    private val messageDao: IncomingMessageDao,
    private val contactDao: WhitelistedContactDao,
) : MessageRepositoryPort {

    override suspend fun save(message: IncomingMessage): Long {
        val entity = message.toEntity()
        if (isRecentDuplicate(entity)) return DUPLICATE_ID
        return messageDao.insert(entity)
    }

    private suspend fun isRecentDuplicate(entity: IncomingMessageEntity): Boolean {
        val sinceTimestamp = entity.timestamp - DUPLICATE_WINDOW_MS
        return when (val notificationKey = entity.notificationKey) {
            null -> messageDao.hasRecentDuplicateByContact(
                contactId = entity.contactId,
                text = entity.text,
                contentType = entity.contentType,
                mediaKind = entity.mediaKind,
                sinceTimestamp = sinceTimestamp,
            )
            else -> messageDao.hasRecentDuplicateByNotificationKey(
                notificationKey = notificationKey,
                text = entity.text,
                contentType = entity.contentType,
                mediaKind = entity.mediaKind,
                sinceTimestamp = sinceTimestamp,
            )
        }
    }

    override fun observeActiveConversations(): Flow<List<ConversationThread>> =
        combine(
            messageDao.observeLatestPerContact(),
            contactDao.observeAll(),
            messageDao.observeUnreadCountsByContact(),
        ) { messages, contacts, unreadCounts ->
            val contactsById = contacts.associate { it.id to it.toDomain() }
            val unreadByContactId = unreadCounts.associate { it.contactId to it.unreadCount }
            messages.mapNotNull { entity ->
                val contact = contactsById[entity.contactId] ?: return@mapNotNull null
                ConversationThread(
                    contact = contact,
                    lastMessage = entity.toDomain(),
                    unreadCount = unreadByContactId[entity.contactId] ?: 0,
                )
            }
        }

    override suspend fun findMessagesByContactId(contactId: Long): List<IncomingMessage> =
        messageDao.findByContactId(contactId).map { it.toDomain() }

    override fun observeMessagesByContactId(contactId: Long): Flow<List<IncomingMessage>> =
        messageDao.observeByContactId(contactId).map { entities ->
            entities.map { it.toDomain() }
        }

    override suspend fun markAllAsReadForContact(contactId: Long) {
        messageDao.markAllAsReadForContact(contactId)
    }

    companion object {
        const val DUPLICATE_WINDOW_MS = 60_000L
        const val DUPLICATE_ID = -1L
    }
}
