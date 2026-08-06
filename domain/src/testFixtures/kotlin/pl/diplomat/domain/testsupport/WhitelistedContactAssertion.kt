package pl.diplomat.domain.testsupport

import org.junit.Assert.assertEquals
import pl.diplomat.domain.model.WhitelistedContact

class WhitelistedContactAssertion private constructor(
    private val actual: WhitelistedContact,
) {
    fun hasDisplayName(expected: String): WhitelistedContactAssertion = apply {
        assertEquals(expected, actual.displayName)
    }

    fun isEqualTo(expected: WhitelistedContact): WhitelistedContactAssertion = apply {
        assertEquals(expected, actual)
    }

    companion object {
        fun assertThat(actual: WhitelistedContact): WhitelistedContactAssertion =
            WhitelistedContactAssertion(actual)
    }
}
