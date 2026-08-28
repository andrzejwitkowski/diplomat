package pl.diplomat.domain.port

import pl.diplomat.domain.model.ConversationThread
import pl.diplomat.domain.model.IncomingMessage
import kotlinx.coroutines.flow.Flow

interface MessageRepositoryPort {
    /**
     * Persists [message]. Returns the new row id, or -1 when an identical notification was ignored.
     */
    suspend fun save(message: IncomingMessage): Long
    fun observeActiveConversations(): Flow<List<ConversationThread>>
    suspend fun findMessagesByContactId(contactId: Long): List<IncomingMessage>
    fun observeMessagesByContactId(contactId: Long): Flow<List<IncomingMessage>>
    suspend fun markAllAsReadForContact(contactId: Long)
    suspend fun deleteSmsForContact(contactId: Long)
}
