package pl.diplomat.infrastructure.notification

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

object WhatsAppBubbleTimeParser {

    private val CLOCK_FORMATS = listOf(
        DateTimeFormatter.ofPattern("H:mm"),
        DateTimeFormatter.ofPattern("HH:mm"),
    )

    fun parseClockTime(text: String, referenceMillis: Long, zoneId: ZoneId = ZoneId.systemDefault()): Long? {
        val trimmed = text.trim()
        if (trimmed.isBlank()) return null
        val time = parseLocalTime(trimmed) ?: return null
        val referenceDate = Instant.ofEpochMilli(referenceMillis).atZone(zoneId).toLocalDate()
        return localDateTimeToMillis(referenceDate, time, referenceMillis, zoneId)
    }

    private fun parseLocalTime(text: String): LocalTime? {
        for (pattern in CLOCK_FORMATS) {
            try {
                return LocalTime.parse(text, pattern)
            } catch (_: DateTimeParseException) {
            }
        }
        return null
    }

    private fun localDateTimeToMillis(
        date: LocalDate,
        time: LocalTime,
        referenceMillis: Long,
        zoneId: ZoneId,
    ): Long {
        var candidate = LocalDateTime.of(date, time).atZone(zoneId).toInstant().toEpochMilli()
        if (candidate > referenceMillis) {
            candidate = LocalDateTime.of(date.minusDays(1), time)
                .atZone(zoneId)
                .toInstant()
                .toEpochMilli()
        }
        return candidate
    }

    fun attachBubbleTimestamps(
        candidates: List<WhatsAppNodeMessageExtractor.MessageCandidate>,
        nodes: List<WhatsAppNodeMessageExtractor.NodeTextSnapshot>,
        referenceMillis: Long,
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): List<WhatsAppNodeMessageExtractor.MessageCandidate> {
        if (candidates.isEmpty()) return emptyList()
        val markers = nodes
            .asSequence()
            .filter { WhatsAppSystemTextFilter.isTimestampNode(it) }
            .mapNotNull { node ->
                val parsed = parseClockTime(node.text, referenceMillis, zoneId) ?: return@mapNotNull null
                node.top to parsed
            }
            .toList()
        val sortedIndices = candidates.indices.sortedBy { candidates[it].top }
        val fallbackByIndex = sortedIndices.withIndex().associate { (order, originalIndex) ->
            originalIndex to (referenceMillis - (sortedIndices.size - 1 - order) * FALLBACK_STEP_MS)
        }
        return candidates.mapIndexed { index, candidate ->
            val parsed = markers
                .filter { (top, _) -> top >= candidate.top - 8 && top <= candidate.top + 140 }
                .maxByOrNull { it.first }
                ?.second
            candidate.copy(timestampMillis = parsed ?: fallbackByIndex.getValue(index))
        }
    }

    const val FALLBACK_STEP_MS = 1_000L
}
