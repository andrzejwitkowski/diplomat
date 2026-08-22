package pl.diplomat.domain.model

enum class ChatRole {
    SYSTEM,
    USER,
    ASSISTANT,
}

data class ChatMessage(
    val role: ChatRole,
    val content: String,
) {
    init {
        require(content.isNotBlank()) { "Message content cannot be blank" }
    }
}
