package pl.diplomat.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class ChatMessageMapperTest {

    private fun message(id: Long, isOutgoing: Boolean, content: MessageContent) =
        IncomingMessage(
            id = id,
            contactId = 1,
            content = content,
            timestamp = 1000,
            sourceApp = MessageSourceApp.SMS,
            status = MessageStatus.PENDING,
            isOutgoing = isOutgoing,
        )

    @Test
    fun `maps outgoing to assistant and incoming to user`() {
        val messages = listOf(
            message(1, isOutgoing = false, MessageContent.TextOnly("hello")),
            message(2, isOutgoing = true, MessageContent.TextOnly("hi")),
        )

        val result = messages.toChatMessages()

        assertEquals(
            listOf(
                ChatMessage(ChatRole.USER, "hello"),
                ChatMessage(ChatRole.ASSISTANT, "hi"),
            ),
            result,
        )
    }

    @Test
    fun `maps image only content to token`() {
        val messages = listOf(message(1, isOutgoing = false, MessageContent.VisualOnly(VisualMediaKind.PHOTO)))

        val result = messages.toChatMessages()

        assertEquals(
            listOf(ChatMessage(ChatRole.USER, "(photo)")),
            result,
        )
    }

    @Test
    fun `returns empty list for empty input`() {
        assertEquals(emptyList<ChatMessage>(), emptyList<IncomingMessage>().toChatMessages())
    }
}
