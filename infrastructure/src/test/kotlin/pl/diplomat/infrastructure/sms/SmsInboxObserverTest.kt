package pl.diplomat.infrastructure.sms

import org.junit.Assert.assertEquals
import org.junit.Test

class SmsInboxObserverTest {

    @Test
    fun advancesCheckpointPastPendingOutboundRow() {
        val afterId = 4L
        val maxId = SmsInboxObserver.advanceCheckpoint(
            rowIds = listOf(5L, 6L),
            pendingIds = setOf(5L),
            afterId = afterId,
        )
        assertEquals(6L, maxId)
    }

    @Test
    fun stopsAtPendingRowWhenNoLaterRows() {
        val afterId = 4L
        val maxId = SmsInboxObserver.advanceCheckpoint(
            rowIds = listOf(5L),
            pendingIds = setOf(5L),
            afterId = afterId,
        )
        assertEquals(5L, maxId)
    }
}
