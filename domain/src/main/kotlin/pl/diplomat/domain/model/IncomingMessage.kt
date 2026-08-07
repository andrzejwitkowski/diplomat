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
    val content: MessageContent,
    val timestamp: Long,
    val sourceApp: MessageSourceApp,
    val status: MessageStatus,
    val notificationKey: String? = null,
    val isRead: Boolean = false,
    val isOutgoing: Boolean = false,
) {
    init {
        require(contactId > 0) { "Contact id must be positive" }
        require(timestamp > 0) { "Timestamp must be positive" }
    }
}

data class ConversationThread(
    val contact: WhitelistedContact,
    val lastMessage: IncomingMessage,
    val unreadCount: Int = 0,
)

data class ChannelMessageGroup(
    val sourceApp: MessageSourceApp,
    val messages: List<IncomingMessage>,
)
