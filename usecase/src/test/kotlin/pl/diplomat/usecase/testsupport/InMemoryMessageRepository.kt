package pl.diplomat.usecase.testsupport

import pl.diplomat.domain.model.ConversationThread
import pl.diplomat.domain.model.IncomingMessage
import pl.diplomat.domain.port.MessageRepositoryPort
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

class InMemoryMessageRepository(
    private val contactRepository: InMemoryContactRepository,
) : MessageRepositoryPort {
    private val messages = MutableStateFlow<List<IncomingMessage>>(emptyList())
    private var nextId = 1L

    override suspend fun save(message: IncomingMessage): Long {
        val id = if (message.id == 0L) nextId++ else message.id
        val stored = message.copy(id = id)
        messages.update { current -> current + stored }
        return id
    }

    override fun observeActiveConversations(): Flow<List<ConversationThread>> =
        messages.map { all ->
            all.groupBy { it.contactId }
                .mapNotNull { (contactId, threadMessages) ->
                    val contact = contactRepository.findById(contactId) ?: return@mapNotNull null
                    val lastMessage = threadMessages.maxBy { it.timestamp }
                    ConversationThread(contact, lastMessage)
                }
                .sortedByDescending { it.lastMessage.timestamp }
        }

    override suspend fun findMessagesByContactId(contactId: Long): List<IncomingMessage> =
        messages.value.filter { it.contactId == contactId }
            .sortedByDescending { it.timestamp }

    fun snapshot(): List<IncomingMessage> = messages.value
}
