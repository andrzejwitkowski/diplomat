package pl.diplomat.usecase

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import pl.diplomat.domain.model.MessageSourceApp
import pl.diplomat.domain.testsupport.anIncomingMessage

class ApplyRangeMarkTest {

    private val contactId = 1L

    @Test
    fun `set start replaces range and clears end`() {
        val priorStart = anIncomingMessage().withId(10).withTimestamp(100).build()
        val priorEnd = anIncomingMessage().withId(11).withTimestamp(200).build()
        val withEnd = (applyRangeMark(contactId, null, RangeMarkAction.SetStart(priorStart))
            as ApplyRangeMarkResult.Applied).range!!
            .copy(endMessageId = priorEnd.id)

        val newStart = anIncomingMessage()
            .withId(20)
            .withTimestamp(50)
            .withSourceApp(MessageSourceApp.WHATSAPP)
            .build()

        val result = applyRangeMark(contactId, withEnd, RangeMarkAction.SetStart(newStart))
            as ApplyRangeMarkResult.Applied
        assertEquals(20L, result.range!!.startMessageId)
        assertNull(result.range!!.endMessageId)
        assertEquals(MessageSourceApp.WHATSAPP, result.range!!.sourceApp)
    }

    @Test
    fun `set end rejects different channel`() {
        val start = anIncomingMessage().withId(1).withSourceApp(MessageSourceApp.SMS).build()
        val current = (applyRangeMark(contactId, null, RangeMarkAction.SetStart(start))
            as ApplyRangeMarkResult.Applied).range
        val end = anIncomingMessage().withId(2).withSourceApp(MessageSourceApp.WHATSAPP).build()

        val result = applyRangeMark(contactId, current, RangeMarkAction.SetEnd(end, start))
        assertTrue(result is ApplyRangeMarkResult.RejectedWrongChannel)
    }

    @Test
    fun `set end swaps when end is earlier than start`() {
        val start = anIncomingMessage().withId(2).withTimestamp(200).build()
        val end = anIncomingMessage().withId(1).withTimestamp(100).build()
        val current = (applyRangeMark(contactId, null, RangeMarkAction.SetStart(start))
            as ApplyRangeMarkResult.Applied).range

        val result = applyRangeMark(contactId, current, RangeMarkAction.SetEnd(end, start))
            as ApplyRangeMarkResult.Applied
        assertEquals(1L, result.range!!.startMessageId)
        assertEquals(2L, result.range!!.endMessageId)
    }

    @Test
    fun `clear start removes incomplete range`() {
        val start = anIncomingMessage().withId(1).build()
        val current = (applyRangeMark(contactId, null, RangeMarkAction.SetStart(start))
            as ApplyRangeMarkResult.Applied).range

        val result = applyRangeMark(contactId, current, RangeMarkAction.ClearStart)
            as ApplyRangeMarkResult.Applied
        assertNull(result.range)
    }

    @Test
    fun `clear end keeps start only`() {
        val start = anIncomingMessage().withId(1).withTimestamp(100).build()
        val end = anIncomingMessage().withId(2).withTimestamp(200).build()
        var marked = (applyRangeMark(contactId, null, RangeMarkAction.SetStart(start))
            as ApplyRangeMarkResult.Applied).range
        marked = (applyRangeMark(contactId, marked, RangeMarkAction.SetEnd(end, start))
            as ApplyRangeMarkResult.Applied).range

        val after = (applyRangeMark(contactId, marked, RangeMarkAction.ClearEnd)
            as ApplyRangeMarkResult.Applied).range!!
        assertEquals(1L, after.startMessageId)
        assertNull(after.endMessageId)
    }
}
