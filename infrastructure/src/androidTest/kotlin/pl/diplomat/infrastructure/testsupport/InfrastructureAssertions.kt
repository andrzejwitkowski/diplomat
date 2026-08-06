package pl.diplomat.infrastructure.testsupport

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import pl.diplomat.domain.model.MessageContent
import pl.diplomat.domain.model.MessageContentType
import pl.diplomat.domain.model.MessageSourceApp
import pl.diplomat.domain.model.VisualMediaKind
import pl.diplomat.domain.testsupport.MessageAssertion
import pl.diplomat.infrastructure.notification.ParsedNotification
import pl.diplomat.infrastructure.persistence.IncomingMessageEntity

class ParsedNotificationAssertion private constructor(
    private val actual: ParsedNotification?,
) {
    fun isNull(): ParsedNotificationAssertion = apply {
        assertNull(actual)
    }

    fun isNotNull(): ParsedNotificationAssertion = apply {
        assertNotNull(actual)
    }

    fun hasSourceApp(expected: MessageSourceApp): ParsedNotificationAssertion = apply {
        assertEquals(expected, requireNotNull(actual).sourceApp)
    }

    fun hasSenderPhone(expected: String): ParsedNotificationAssertion = apply {
        assertEquals(expected, requireNotNull(actual).senderPhone)
    }

    fun hasContent(expected: MessageContent): ParsedNotificationAssertion = apply {
        assertEquals(expected, requireNotNull(actual).content)
    }

    fun hasVisualOnly(kind: VisualMediaKind): ParsedNotificationAssertion = apply {
        assertEquals(MessageContent.VisualOnly(kind), requireNotNull(actual).content)
    }

    companion object {
        fun assertThat(actual: ParsedNotification?): ParsedNotificationAssertion =
            ParsedNotificationAssertion(actual)
    }
}

class MessageEntityAssertion private constructor(
    private val actual: IncomingMessageEntity,
) {
    fun hasContentType(expected: MessageContentType): MessageEntityAssertion = apply {
        assertEquals(expected.name, actual.contentType)
    }

    fun hasMediaKind(expected: VisualMediaKind): MessageEntityAssertion = apply {
        assertEquals(expected.name, actual.mediaKind)
    }

    fun hasText(expected: String): MessageEntityAssertion = apply {
        assertEquals(expected, actual.text)
    }

    companion object {
        fun assertThat(actual: IncomingMessageEntity): MessageEntityAssertion =
            MessageEntityAssertion(actual)
    }
}

class MessageHistoryAssertion private constructor(
    private val messages: List<pl.diplomat.domain.model.IncomingMessage>,
) {
    fun hasSize(expected: Int): MessageHistoryAssertion = apply {
        assertEquals(expected, messages.size)
    }

    fun first(block: MessageAssertion.() -> Unit): MessageHistoryAssertion = apply {
        MessageAssertion.assertThat(messages.first()).block()
    }

    companion object {
        fun assertThat(
            messages: List<pl.diplomat.domain.model.IncomingMessage>,
        ): MessageHistoryAssertion = MessageHistoryAssertion(messages)
    }
}

class ConversationRepositoryAssertion private constructor(
    private val conversations: List<pl.diplomat.domain.model.ConversationThread>,
) {
    fun hasSize(expected: Int): ConversationRepositoryAssertion = apply {
        assertEquals(expected, conversations.size)
    }

    fun first(block: MessageAssertion.() -> Unit): ConversationRepositoryAssertion = apply {
        MessageAssertion.assertThat(conversations.first().lastMessage).block()
    }

    fun firstContactHasDisplayName(expected: String): ConversationRepositoryAssertion = apply {
        assertEquals(expected, conversations.first().contact.displayName)
    }

    fun savedMessageIdIsPositive(messageId: Long): ConversationRepositoryAssertion = apply {
        assertTrue(messageId > 0)
    }

    companion object {
        fun assertThat(
            conversations: List<pl.diplomat.domain.model.ConversationThread>,
        ): ConversationRepositoryAssertion = ConversationRepositoryAssertion(conversations)
    }
}
