package pl.diplomat.domain.model

data class ConversationRange(
    val contactId: Long,
    val sourceApp: MessageSourceApp,
    val startMessageId: Long? = null,
    val endMessageId: Long? = null,
) {
    val isComplete: Boolean
        get() = startMessageId != null && endMessageId != null
}
