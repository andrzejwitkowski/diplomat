package pl.diplomat.infrastructure.notification

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

class WhatsAppBubbleTimeParserTest {

    private val zone = ZoneId.of("Europe/Warsaw")
    private val referenceMillis = LocalDate.of(2026, 8, 9)
        .atTime(LocalTime.of(21, 5))
        .atZone(zone)
        .toInstant()
        .toEpochMilli()

    @Test
    fun parsesClockTimeOnReferenceDay() {
        val parsed = WhatsAppBubbleTimeParser.parseClockTime("20:59", referenceMillis, zone)
        val expected = LocalDate.of(2026, 8, 9)
            .atTime(LocalTime.of(20, 59))
            .atZone(zone)
            .toInstant()
            .toEpochMilli()
        assertEquals(expected, parsed)
    }

    @Test
    fun rollsBackToPreviousDayWhenBubbleTimeIsAheadOfScan() {
        val zone = ZoneId.of("Europe/Warsaw")
        val referenceMillis = LocalDate.of(2026, 8, 10)
            .atTime(LocalTime.of(0, 10))
            .atZone(zone)
            .toInstant()
            .toEpochMilli()
        val parsed = WhatsAppBubbleTimeParser.parseClockTime("23:50", referenceMillis, zone)
        val expected = LocalDate.of(2026, 8, 9)
            .atTime(LocalTime.of(23, 50))
            .atZone(zone)
            .toInstant()
            .toEpochMilli()
        assertEquals(expected, parsed)
    }

    @Test
    fun attachesParsedTimestampToNearestBubble() {
        val candidates = listOf(
            WhatsAppNodeMessageExtractor.MessageCandidate("ok", false, top = 400),
            WhatsAppNodeMessageExtractor.MessageCandidate("later", true, top = 520),
        )
        val nodes = listOf(
            node("20:55", top = 430, viewId = "com.whatsapp:id/date"),
            node("20:56", top = 550, viewId = "com.whatsapp:id/date"),
        )
        val attached = WhatsAppBubbleTimeParser.attachBubbleTimestamps(
            candidates = candidates,
            nodes = nodes,
            referenceMillis = referenceMillis,
            zoneId = zone,
        )
        assertEquals(
            LocalDate.of(2026, 8, 9).atTime(20, 55).atZone(zone).toInstant().toEpochMilli(),
            attached[0].timestampMillis,
        )
        assertEquals(
            LocalDate.of(2026, 8, 9).atTime(20, 56).atZone(zone).toInstant().toEpochMilli(),
            attached[1].timestampMillis,
        )
    }

    @Test
    fun assignsEachMarkerAtMostOnceWithOverlappingWindows() {
        val candidates = listOf(
            WhatsAppNodeMessageExtractor.MessageCandidate("first", false, top = 400),
            WhatsAppNodeMessageExtractor.MessageCandidate("second", true, top = 500),
        )
        val nodes = listOf(
            node("20:55", top = 430, viewId = "com.whatsapp:id/date"),
            node("20:56", top = 530, viewId = "com.whatsapp:id/date"),
        )
        val attached = WhatsAppBubbleTimeParser.attachBubbleTimestamps(
            candidates = candidates,
            nodes = nodes,
            referenceMillis = referenceMillis,
            zoneId = zone,
        )
        val firstTs = LocalDate.of(2026, 8, 9).atTime(20, 55).atZone(zone).toInstant().toEpochMilli()
        val secondTs = LocalDate.of(2026, 8, 9).atTime(20, 56).atZone(zone).toInstant().toEpochMilli()
        assertEquals(firstTs, attached[0].timestampMillis)
        assertEquals(secondTs, attached[1].timestampMillis)
        assertTrue(attached[0].timestampMillis!! < attached[1].timestampMillis!!)
    }

    @Test
    fun fallsBackToOrderedTimestampsWhenMarkersMissing() {
        val candidates = listOf(
            WhatsAppNodeMessageExtractor.MessageCandidate("b", true, top = 200),
            WhatsAppNodeMessageExtractor.MessageCandidate("a", false, top = 100),
        )
        val attached = WhatsAppBubbleTimeParser.attachBubbleTimestamps(
            candidates = candidates,
            nodes = emptyList(),
            referenceMillis = referenceMillis,
            zoneId = zone,
        )
        val earlier = attached.single { it.top == 100 }
        val later = attached.single { it.top == 200 }
        assertEquals(referenceMillis - WhatsAppBubbleTimeParser.FALLBACK_STEP_MS, earlier.timestampMillis)
        assertEquals(referenceMillis, later.timestampMillis)
    }

    private fun node(
        text: String,
        top: Int,
        viewId: String,
    ) = WhatsAppNodeMessageExtractor.NodeTextSnapshot(
        text = text,
        viewId = viewId,
        top = top,
        bottom = top + 24,
    )
}
