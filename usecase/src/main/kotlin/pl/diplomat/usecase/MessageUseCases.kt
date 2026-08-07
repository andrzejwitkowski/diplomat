package pl.diplomat.usecase

import pl.diplomat.domain.model.ConversationThread
import pl.diplomat.domain.model.IncomingMessage
import pl.diplomat.domain.model.MessageContent
import pl.diplomat.domain.model.MessageSourceApp
import pl.diplomat.domain.model.MessageStatus
import pl.diplomat.domain.model.PhoneNumber
import pl.diplomat.domain.model.WhitelistedContact
import pl.diplomat.domain.model.bodyText
import pl.diplomat.domain.port.ContactRepositoryPort
import pl.diplomat.domain.port.MessageRepositoryPort
import pl.diplomat.domain.port.SystemContactsPort
import kotlinx.coroutines.flow.Flow

data class RawIncomingMessage(
    val senderPhone: String,
    val content: MessageContent,
    val timestamp: Long,
    val sourceApp: MessageSourceApp,
    val notificationKey: String? = null,
    val additionalSenderCandidates: List<String> = emptyList(),
)

sealed class ProcessIncomingMessageResult {
    data class Saved(val message: IncomingMessage, val contact: WhitelistedContact) : ProcessIncomingMessageResult()
    data object RejectedNotWhitelisted : ProcessIncomingMessageResult()
    data object IgnoredDuplicate : ProcessIncomingMessageResult()
}

class ProcessIncomingMessageUseCase(
    private val contactRepository: ContactRepositoryPort,
    private val messageRepository: MessageRepositoryPort,
    private val systemContacts: SystemContactsPort,
) {
    suspend operator fun invoke(raw: RawIncomingMessage): ProcessIncomingMessageResult {
        val contact = resolveContact(raw)
            ?: return ProcessIncomingMessageResult.RejectedNotWhitelisted

        val status = when (val content = raw.content) {
            is MessageContent.VisualOnly -> MessageStatus.PENDING
            is MessageContent.TextOnly -> classifyText(content.body)
            is MessageContent.VisualWithText -> classifyText(content.body)
        }

        val message = IncomingMessage(
            id = 0,
            contactId = contact.id,
            content = raw.content,
            timestamp = raw.timestamp,
            sourceApp = raw.sourceApp,
            status = status,
            notificationKey = raw.notificationKey,
        )

        val id = messageRepository.save(message)
        if (id == -1L) return ProcessIncomingMessageResult.IgnoredDuplicate
        return ProcessIncomingMessageResult.Saved(message.copy(id = id), contact)
    }

    private suspend fun resolveContact(raw: RawIncomingMessage): WhitelistedContact? {
        val candidates = (listOf(raw.senderPhone) + raw.additionalSenderCandidates)
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
        for (candidate in candidates) {
            resolveContactCandidate(candidate)?.let { return it }
        }
        return null
    }

    private suspend fun resolveContactCandidate(sender: String): WhitelistedContact? {
        runCatching { PhoneNumber(sender) }.getOrNull()
            ?.let { contactRepository.findByPhoneNumber(it) }
            ?.let { return it }

        contactRepository.findByDisplayName(sender)?.let { return it }

        for (phone in systemContacts.findPhoneNumbersByDisplayName(sender)) {
            contactRepository.findByPhoneNumber(phone)?.let { return it }
        }
        return null
    }

    private fun classifyText(text: String): MessageStatus =
        if (isOneLiner(text)) MessageStatus.IGNORED_CONFIRMATION else MessageStatus.PENDING

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
