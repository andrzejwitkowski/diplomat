package pl.diplomat.domain.model

fun List<IncomingMessage>.toChatMessages(): List<ChatMessage> =
    map { message ->
        val role = if (message.isOutgoing) ChatRole.ASSISTANT else ChatRole.USER
        ChatMessage(role, message.toText())
    }

private fun IncomingMessage.toText(): String = when (content) {
    is MessageContent.TextOnly -> content.body
    is MessageContent.VisualOnly -> "(photo)"
    is MessageContent.VisualWithText -> content.body
}
