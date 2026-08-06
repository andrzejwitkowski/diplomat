package pl.diplomat.domain.normalization

import org.junit.Assert.assertEquals
import org.junit.Test

class NormalizationServiceTest {

    private val normalization = NormalizationService.default

    @Test
    fun `normalizes display name with custom strategy pipeline`() {
        val result = normalization.displayName()
            .with(NormalizationStrategyKeys.TRIM)
            .with(NormalizationStrategyKeys.COLLAPSE_WHITESPACE)
            .normalize("  Jan   Kowalski  ")

        assertEquals("Jan Kowalski", result)
    }

    @Test
    fun `normalizes phone formats to the same match key`() {
        val formatted = normalization.normalizePhone("+48 123 456 789")
        val compact = normalization.normalizePhone("48123456789")

        assertEquals("123456789", formatted.matchKey)
        assertEquals(formatted.matchKey, compact.matchKey)
    }

    @Test
    fun `builds phone pipeline fluently from strategy keys`() {
        val result = normalization.phoneNumber()
            .with(NormalizationStrategyKeys.PHONE_TRIM)
            .with(NormalizationStrategyKeys.PHONE_EXTRACT_DIGITS)
            .with(NormalizationStrategyKeys.PHONE_MATCH_KEY_NATIONAL_9)
            .normalize(" 48123456789 ")

        assertEquals("123456789", result.matchKey)
    }
}
