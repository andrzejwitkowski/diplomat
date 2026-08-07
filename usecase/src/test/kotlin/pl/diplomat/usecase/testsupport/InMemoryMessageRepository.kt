package pl.diplomat.usecase.testsupport

import pl.diplomat.domain.model.ConversationThread
import pl.diplomat.domain.model.IncomingMessage
import pl.diplomat.domain.port.MessageRepositoryPort
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class InMemoryMessageRepository(
    private val contactRepository: InMemoryContactRepository,
) : MessageRepositoryPort {
    private val messages = MutableStateFlow<List<IncomingMessage>>(emptyList())
    private val saveMutex = Mutex()
    private var nextId = 1L

    override suspend fun save(message: IncomingMessage): Long = saveMutex.withLock {
        val sinceTimestamp = message.timestamp - DUPLICATE_WINDOW_MS
        val untilTimestamp = message.timestamp + DUPLICATE_WINDOW_MS
        if (messages.value.any { existing ->
                existing.isRecentDuplicateOf(message, sinceTimestamp, untilTimestamp)
            }
        ) {
            return@withLock -1L
        }
        val id = if (message.id == 0L) nextId++ else message.id
        val stored = message.copy(id = id)
        messages.update { current -> current + stored }
        id
    }

    override fun observeActiveConversations(): Flow<List<ConversationThread>> =
        combine(contactRepository.observeAll(), messages) { contacts, all ->
            val contactsById = contacts.associateBy { it.id }
            all.groupBy { it.contactId }
                .mapNotNull { (contactId, threadMessages) ->
                    val contact = contactsById[contactId] ?: return@mapNotNull null
                    val lastMessage = threadMessages.maxWith(
                        compareBy<IncomingMessage> { it.timestamp }.thenBy { it.id },
                    )
                    val unreadCount = threadMessages.count { !it.isRead && !it.isOutgoing }
                    ConversationThread(contact, lastMessage, unreadCount)
                }
                .sortedByDescending { it.lastMessage.timestamp }
        }

    override suspend fun findMessagesByContactId(contactId: Long): List<IncomingMessage> =
        messages.value.filter { it.contactId == contactId }
            .sortedWith(compareByDescending<IncomingMessage> { it.timestamp }.thenByDescending { it.id })

    override fun observeMessagesByContactId(contactId: Long): Flow<List<IncomingMessage>> =
        messages.map { all ->
            all.filter { it.contactId == contactId }
                .sortedWith(compareByDescending<IncomingMessage> { it.timestamp }.thenByDescending { it.id })
        }

    override suspend fun markAllAsReadForContact(contactId: Long) {
        messages.update { current ->
            current.map { message ->
                if (message.contactId == contactId && !message.isRead) {
                    message.copy(isRead = true)
                } else {
                    message
                }
            }
        }
    }

    fun snapshot(): List<IncomingMessage> = messages.value

    companion object {
        private const val DUPLICATE_WINDOW_MS = 60_000L
    }
}

private fun IncomingMessage.isRecentDuplicateOf(
    candidate: IncomingMessage,
    sinceTimestamp: Long,
    untilTimestamp: Long,
): Boolean {
    if (content != candidate.content ||
        timestamp !in sinceTimestamp..untilTimestamp ||
        isOutgoing != candidate.isOutgoing
    ) {
        return false
    }
    return when (val key = candidate.notificationKey) {
        null -> contactId == candidate.contactId && notificationKey == null
        else -> notificationKey == key
    }
}
