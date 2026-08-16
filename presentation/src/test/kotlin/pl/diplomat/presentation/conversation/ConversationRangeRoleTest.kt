package pl.diplomat.presentation.conversation

import org.junit.Assert.assertEquals
import org.junit.Test
import pl.diplomat.domain.model.ConversationRange
import pl.diplomat.domain.model.MessageSourceApp
import pl.diplomat.domain.testsupport.anIncomingMessage

class ConversationRangeRoleTest {

    private val contactId = 1L

    @Test
    fun `complete range marks start interior and end`() {
        val m1 = anIncomingMessage().withId(1).withTimestamp(100).build()
        val m2 = anIncomingMessage().withId(2).withTimestamp(200).build()
        val m3 = anIncomingMessage().withId(3).withTimestamp(300).build()
        val range = ConversationRange(
            contactId = contactId,
            sourceApp = MessageSourceApp.SMS,
            startMessageId = 1,
            endMessageId = 3,
        )

        assertEquals(
            listOf(RangeRole.Start, RangeRole.Interior, RangeRole.End),
            rangeRoles(range, listOf(m1, m2, m3)),
        )
    }

    @Test
    fun `messages outside span are none`() {
        val m1 = anIncomingMessage().withId(1).withTimestamp(100).build()
        val m2 = anIncomingMessage().withId(2).withTimestamp(200).build()
        val m3 = anIncomingMessage().withId(3).withTimestamp(300).build()
        val m4 = anIncomingMessage().withId(4).withTimestamp(400).build()
        val range = ConversationRange(
            contactId = contactId,
            sourceApp = MessageSourceApp.SMS,
            startMessageId = 2,
            endMessageId = 3,
        )

        assertEquals(
            listOf(RangeRole.None, RangeRole.Start, RangeRole.End, RangeRole.None),
            rangeRoles(range, listOf(m1, m2, m3, m4)),
        )
    }

    @Test
    fun `different sourceApp is never interior`() {
        val smsStart = anIncomingMessage()
            .withId(1)
            .withTimestamp(100)
            .withSourceApp(MessageSourceApp.SMS)
            .build()
        val wa = anIncomingMessage()
            .withId(2)
            .withTimestamp(200)
            .withSourceApp(MessageSourceApp.WHATSAPP)
            .build()
        val smsEnd = anIncomingMessage()
            .withId(3)
            .withTimestamp(300)
            .withSourceApp(MessageSourceApp.SMS)
            .build()
        val range = ConversationRange(
            contactId = contactId,
            sourceApp = MessageSourceApp.SMS,
            startMessageId = 1,
            endMessageId = 3,
        )

        assertEquals(
            listOf(RangeRole.Start, RangeRole.None, RangeRole.End),
            rangeRoles(range, listOf(smsStart, wa, smsEnd)),
        )
    }

    @Test
    fun `incomplete range has start only no interior`() {
        val m1 = anIncomingMessage().withId(1).withTimestamp(100).build()
        val m2 = anIncomingMessage().withId(2).withTimestamp(200).build()
        val range = ConversationRange(
            contactId = contactId,
            sourceApp = MessageSourceApp.SMS,
            startMessageId = 1,
            endMessageId = null,
        )

        assertEquals(
            listOf(RangeRole.Start, RangeRole.None),
            rangeRoles(range, listOf(m1, m2)),
        )
    }

    @Test
    fun `interior still between when start index is after end index`() {
        val m1 = anIncomingMessage().withId(1).withTimestamp(100).build()
        val m2 = anIncomingMessage().withId(2).withTimestamp(200).build()
        val m3 = anIncomingMessage().withId(3).withTimestamp(300).build()
        val range = ConversationRange(
            contactId = contactId,
            sourceApp = MessageSourceApp.SMS,
            startMessageId = 3,
            endMessageId = 1,
        )

        assertEquals(
            listOf(RangeRole.End, RangeRole.Interior, RangeRole.Start),
            rangeRoles(range, listOf(m1, m2, m3)),
        )
    }
}
