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

class RoomMessageRepositoryAdapter(
    private val messageDao: IncomingMessageDao,
    private val contactDao: WhitelistedContactDao,
) : MessageRepositoryPort {

    override suspend fun save(message: IncomingMessage): Long =
        messageDao.insert(message.toEntity())

    override fun observeActiveConversations(): Flow<List<ConversationThread>> =
        combine(
            messageDao.observeLatestPerContact(),
            contactDao.observeAll(),
        ) { messages, contacts ->
            val contactsById = contacts.associate { it.id to it.toDomain() }
            messages.mapNotNull { entity ->
                val contact = contactsById[entity.contactId] ?: return@mapNotNull null
                ConversationThread(
                    contact = contact,
                    lastMessage = entity.toDomain(),
                )
            }
        }

    override suspend fun findMessagesByContactId(contactId: Long): List<IncomingMessage> =
        messageDao.findByContactId(contactId).map { it.toDomain() }
}
