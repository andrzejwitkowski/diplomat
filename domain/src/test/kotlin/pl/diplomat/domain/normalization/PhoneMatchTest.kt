package pl.diplomat.domain.normalization

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import pl.diplomat.domain.model.PhoneNumber

class PhoneMatchTest {

    private val normalization = NormalizationService.default

    @Test
    fun `matches formatted contact phone to telephony address`() {
        assertTrue(
            normalization.phonesMatch(
                contactPhone = PhoneNumber("+48 123 456 789"),
                telephonyAddress = "+48123456789",
            ),
        )
    }

    @Test
    fun `does not match different numbers`() {
        assertFalse(
            normalization.phonesMatch(
                contactPhone = PhoneNumber("+48 123 456 789"),
                telephonyAddress = "+48999888777",
            ),
        )
    }
}
