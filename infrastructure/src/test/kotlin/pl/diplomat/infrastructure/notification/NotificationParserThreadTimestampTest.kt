package pl.diplomat.infrastructure.notification

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationParserThreadTimestampTest {

    @Test
    fun ordersThreadMessagesRelativeToPostedTime() {
        val postedAt = 1_700_000_000_000L
        val first = NotificationParser.inferredThreadTimestamp(postedAt, index = 0, lastIndex = 2)
        val second = NotificationParser.inferredThreadTimestamp(postedAt, index = 1, lastIndex = 2)
        val third = NotificationParser.inferredThreadTimestamp(postedAt, index = 2, lastIndex = 2)
        assertTrue(first < second)
        assertTrue(second < third)
        assertEquals(postedAt, third)
        assertEquals(postedAt - 2_000L, first)
    }
}
