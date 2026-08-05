package pl.diplomat.infrastructure.adapter

import pl.diplomat.domain.model.ConversationThread
import pl.diplomat.domain.model.IncomingMessage
import pl.diplomat.domain.model.PhoneNumber
import pl.diplomat.domain.port.MessageRepositoryPort
import pl.diplomat.infrastructure.persistence.IncomingMessageDao
import pl.diplomat.infrastructure.persistence.WhitelistedContactDao
import pl.diplomat.infrastructure.persistence.toDomain
import pl.diplomat.infrastructure.persistence.toEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomMessageRepositoryAdapter(
    private val messageDao: IncomingMessageDao,
    private val contactDao: WhitelistedContactDao,
) : MessageRepositoryPort {

    override suspend fun save(message: IncomingMessage): Long =
        messageDao.insert(message.toEntity())

    override fun observeActiveConversations(): Flow<List<ConversationThread>> =
        messageDao.observeLatestPerContact().map { messages ->
            messages.mapNotNull { entity ->
                val contact = contactDao.findById(entity.contactId)?.toDomain() ?: return@mapNotNull null
                ConversationThread(
                    contact = contact,
                    lastMessage = entity.toDomain(),
                )
            }
        }

    override suspend fun findMessagesByContactId(contactId: Long): List<IncomingMessage> =
        messageDao.findByContactId(contactId).map { it.toDomain() }
}
