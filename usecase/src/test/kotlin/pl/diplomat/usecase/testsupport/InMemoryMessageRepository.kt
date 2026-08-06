package pl.diplomat.usecase.testsupport

import pl.diplomat.domain.model.ConversationThread
import pl.diplomat.domain.model.IncomingMessage
import pl.diplomat.domain.port.MessageRepositoryPort
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
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

    override suspend fun existsByNotificationKey(notificationKey: String): Boolean =
        messages.value.any { it.notificationKey == notificationKey }

    override fun observeActiveConversations(): Flow<List<ConversationThread>> =
        combine(contactRepository.observeAll(), messages) { contacts, all ->
            val contactsById = contacts.associateBy { it.id }
            all.groupBy { it.contactId }
                .mapNotNull { (contactId, threadMessages) ->
                    val contact = contactsById[contactId] ?: return@mapNotNull null
                    val lastMessage = threadMessages.maxWith(
                        compareBy<IncomingMessage> { it.timestamp }.thenBy { it.id },
                    )
                    ConversationThread(contact, lastMessage)
                }
                .sortedByDescending { it.lastMessage.timestamp }
        }

    override suspend fun findMessagesByContactId(contactId: Long): List<IncomingMessage> =
        messages.value.filter { it.contactId == contactId }
            .sortedWith(compareByDescending<IncomingMessage> { it.timestamp }.thenByDescending { it.id })

    fun snapshot(): List<IncomingMessage> = messages.value
}
