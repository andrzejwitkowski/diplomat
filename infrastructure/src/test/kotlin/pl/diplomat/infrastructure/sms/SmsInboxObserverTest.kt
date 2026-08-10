package pl.diplomat.infrastructure.sms

import org.junit.Assert.assertEquals
import org.junit.Test

class SmsInboxObserverTest {

    @Test
    fun holdsCheckpointBeforePendingOutboundRow() {
        val maxId = SmsInboxObserver.advanceCheckpoint(
            rowIds = listOf(5L, 6L),
            pendingIds = setOf(5L),
            afterId = 4L,
        )
        assertEquals(4L, maxId)
    }

    @Test
    fun holdsCheckpointWhenOnlyPendingRowExists() {
        val maxId = SmsInboxObserver.advanceCheckpoint(
            rowIds = listOf(5L),
            pendingIds = setOf(5L),
            afterId = 4L,
        )
        assertEquals(4L, maxId)
    }

    @Test
    fun advancesThroughContiguousNonPendingPrefix() {
        val maxId = SmsInboxObserver.advanceCheckpoint(
            rowIds = listOf(5L, 6L, 7L),
            pendingIds = setOf(6L),
            afterId = 4L,
        )
        assertEquals(5L, maxId)
    }

    @Test
    fun advancesFullyWhenNoPendingRows() {
        val maxId = SmsInboxObserver.advanceCheckpoint(
            rowIds = listOf(5L, 6L),
            pendingIds = emptySet(),
            afterId = 4L,
        )
        assertEquals(6L, maxId)
    }
}
