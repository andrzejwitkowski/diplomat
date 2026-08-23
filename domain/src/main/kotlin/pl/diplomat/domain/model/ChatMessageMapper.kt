package pl.diplomat.domain.model

/**
 * Converts [IncomingMessage] rows into [ChatMessage] entries suitable for an
 * OpenAI-compatible chat completion request. Outgoing messages become
 * ASSISTANT turns; incoming messages become USER turns. Image-only content is
 * represented as a short textual token so the prompt stays non-blank.
 */
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
