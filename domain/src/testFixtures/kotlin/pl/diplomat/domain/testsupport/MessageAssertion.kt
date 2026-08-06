package pl.diplomat.domain.testsupport

import org.junit.Assert.assertEquals
import pl.diplomat.domain.model.IncomingMessage
import pl.diplomat.domain.model.MessageContent
import pl.diplomat.domain.model.MessageSourceApp
import pl.diplomat.domain.model.MessageStatus
import pl.diplomat.domain.model.VisualMediaKind
import pl.diplomat.domain.model.bodyText
import pl.diplomat.domain.model.visualKind

class MessageAssertion private constructor(
    private val actual: IncomingMessage,
) {
    fun hasStatus(expected: MessageStatus): MessageAssertion = apply {
        assertEquals(expected, actual.status)
    }

    fun hasSourceApp(expected: MessageSourceApp): MessageAssertion = apply {
        assertEquals(expected, actual.sourceApp)
    }

    fun hasContent(expected: MessageContent): MessageAssertion = apply {
        assertEquals(expected, actual.content)
    }

    fun hasTextBody(expected: String): MessageAssertion = apply {
        assertEquals(expected, actual.content.bodyText())
    }

    fun hasVisualKind(expected: VisualMediaKind): MessageAssertion = apply {
        assertEquals(expected, actual.content.visualKind())
    }

    fun isEqualTo(expected: IncomingMessage): MessageAssertion = apply {
        assertEquals(expected, actual)
    }

    companion object {
        fun assertThat(actual: IncomingMessage): MessageAssertion = MessageAssertion(actual)
    }
}
