package pl.diplomat.usecase.testsupport

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import pl.diplomat.domain.model.IncomingMessage
import pl.diplomat.domain.model.MessageStatus
import pl.diplomat.domain.model.VisualMediaKind
import pl.diplomat.domain.testsupport.MessageAssertion
import pl.diplomat.usecase.ProcessIncomingMessageResult

class ProcessResultAssertion private constructor(
    private val actual: ProcessIncomingMessageResult,
) {
    fun isRejectedNotWhitelisted(): ProcessResultAssertion = apply {
        assertEquals(ProcessIncomingMessageResult.RejectedNotWhitelisted, actual)
    }

    fun isIgnoredDuplicate(): ProcessResultAssertion = apply {
        assertEquals(ProcessIncomingMessageResult.IgnoredDuplicate, actual)
    }

    fun isSaved(
        block: MessageAssertion.() -> Unit = {},
    ): ProcessResultAssertion = apply {
        val saved = actual as? ProcessIncomingMessageResult.Saved
            ?: error("Expected Saved but was $actual")
        assertEquals(saved.contact.id, saved.message.contactId)
        MessageAssertion.assertThat(saved.message).block()
    }

    fun savedMessage(): IncomingMessage =
        (actual as ProcessIncomingMessageResult.Saved).message

    companion object {
        fun assertThat(actual: ProcessIncomingMessageResult): ProcessResultAssertion =
            ProcessResultAssertion(actual)
    }
}

class MessageRepositoryAssertion private constructor(
    private val messages: List<IncomingMessage>,
) {
    fun isEmpty(): MessageRepositoryAssertion = apply {
        assertTrue(messages.isEmpty())
    }

    fun hasSize(expected: Int): MessageRepositoryAssertion = apply {
        assertEquals(expected, messages.size)
    }

    companion object {
        fun assertThat(messages: List<IncomingMessage>): MessageRepositoryAssertion =
            MessageRepositoryAssertion(messages)
    }
}

class ConversationListAssertion private constructor(
    private val conversations: List<pl.diplomat.domain.model.ConversationThread>,
) {
    fun hasSize(expected: Int): ConversationListAssertion = apply {
        assertEquals(expected, conversations.size)
    }

    fun firstMessageHasTextBody(expected: String): ConversationListAssertion = apply {
        MessageAssertion.assertThat(conversations.first().lastMessage).hasTextBody(expected)
    }

    fun lastMessageHasVisualKind(expected: VisualMediaKind): ConversationListAssertion = apply {
        MessageAssertion.assertThat(conversations.last().lastMessage).hasVisualKind(expected)
    }

    fun firstContactHasDisplayName(expected: String): ConversationListAssertion = apply {
        assertEquals(expected, conversations.first().contact.displayName)
    }

    companion object {
        fun assertThat(
            conversations: List<pl.diplomat.domain.model.ConversationThread>,
        ): ConversationListAssertion = ConversationListAssertion(conversations)
    }
}
