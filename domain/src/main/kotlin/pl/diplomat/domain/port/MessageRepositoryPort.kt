package pl.diplomat.domain.port

import pl.diplomat.domain.model.ConversationThread
import pl.diplomat.domain.model.IncomingMessage
import kotlinx.coroutines.flow.Flow

interface MessageRepositoryPort {
    suspend fun save(message: IncomingMessage): Long
    suspend fun existsByNotificationKey(notificationKey: String): Boolean
    fun observeActiveConversations(): Flow<List<ConversationThread>>
    suspend fun findMessagesByContactId(contactId: Long): List<IncomingMessage>
}
