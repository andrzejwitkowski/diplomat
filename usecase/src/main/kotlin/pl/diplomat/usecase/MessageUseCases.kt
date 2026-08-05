package pl.diplomat.usecase

import pl.diplomat.domain.model.ConversationThread
import pl.diplomat.domain.model.IncomingMessage
import pl.diplomat.domain.model.MessageSourceApp
import pl.diplomat.domain.model.MessageStatus
import pl.diplomat.domain.model.PhoneNumber
import pl.diplomat.domain.port.ContactRepositoryPort
import pl.diplomat.domain.port.MessageRepositoryPort
import kotlinx.coroutines.flow.Flow

data class RawIncomingMessage(
    val senderPhone: String,
    val text: String,
    val timestamp: Long,
    val sourceApp: MessageSourceApp,
)

sealed class ProcessIncomingMessageResult {
    data class Saved(val message: IncomingMessage) : ProcessIncomingMessageResult()
    data object RejectedNotWhitelisted : ProcessIncomingMessageResult()
}

class ProcessIncomingMessageUseCase(
    private val contactRepository: ContactRepositoryPort,
    private val messageRepository: MessageRepositoryPort,
) {
    suspend operator fun invoke(raw: RawIncomingMessage): ProcessIncomingMessageResult {
        val phoneNumber = runCatching { PhoneNumber(raw.senderPhone.trim()) }.getOrNull()
            ?: return ProcessIncomingMessageResult.RejectedNotWhitelisted

        val contact = contactRepository.findByPhoneNumber(phoneNumber)
            ?: return ProcessIncomingMessageResult.RejectedNotWhitelisted

        val status = if (isOneLiner(raw.text)) {
            MessageStatus.IGNORED_CONFIRMATION
        } else {
            MessageStatus.PENDING
        }

        val message = IncomingMessage(
            id = 0,
            contactId = contact.id,
            text = raw.text.trim(),
            timestamp = raw.timestamp,
            sourceApp = raw.sourceApp,
            status = status,
        )

        val id = messageRepository.save(message)
        return ProcessIncomingMessageResult.Saved(message.copy(id = id))
    }

    private fun isOneLiner(text: String): Boolean {
        val trimmed = text.trim()
        if (trimmed.length > ONE_LINER_MAX_LENGTH) return false
        if (trimmed.contains('?')) return false
        return true
    }

    companion object {
        const val ONE_LINER_MAX_LENGTH = 50
    }
}

class GetActiveConversationsUseCase(
    private val messageRepository: MessageRepositoryPort,
) {
    operator fun invoke(): Flow<List<ConversationThread>> =
        messageRepository.observeActiveConversations()
}
