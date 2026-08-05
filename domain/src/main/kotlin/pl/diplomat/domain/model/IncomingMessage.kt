package pl.diplomat.domain.model

enum class MessageSourceApp {
    SMS,
    WHATSAPP,
}

enum class MessageStatus {
    PENDING,
    IGNORED_CONFIRMATION,
    REPLIED,
}

data class IncomingMessage(
    val id: Long,
    val contactId: Long,
    val text: String,
    val timestamp: Long,
    val sourceApp: MessageSourceApp,
    val status: MessageStatus,
) {
    init {
        require(text.isNotBlank()) { "Message text cannot be blank" }
        require(contactId > 0) { "Contact id must be positive" }
        require(timestamp > 0) { "Timestamp must be positive" }
    }
}

data class ConversationThread(
    val contact: WhitelistedContact,
    val lastMessage: IncomingMessage,
)
