package pl.diplomat.infrastructure.notification

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WhatsAppSystemTextFilterTest {

    @Test
    fun rejectsPolishSystemMetadata() {
        assertTrue(WhatsAppSystemTextFilter.isJunk("aktywny(-a)"))
        assertTrue(WhatsAppSystemTextFilter.isJunk("widziano dzisiaj o 20:59"))
        assertTrue(WhatsAppSystemTextFilter.isJunk("2 nieprzeczytane wiadomości"))
        assertTrue(WhatsAppSystemTextFilter.isJunk("Rozmowa głosowa"))
        assertTrue(WhatsAppSystemTextFilter.isJunk("28 sek."))
    }

    @Test
    fun rejectsStandaloneClockTimes() {
        assertTrue(WhatsAppSystemTextFilter.isJunk("20:59"))
        assertTrue(WhatsAppSystemTextFilter.isJunk("9:05"))
    }

    @Test
    fun keepsRealMessageText() {
        assertFalse(WhatsAppSystemTextFilter.isJunk("Mandat"))
        assertFalse(WhatsAppSystemTextFilter.isJunk("Ja patrzący na dzika"))
        assertFalse(WhatsAppSystemTextFilter.isJunk("Przecież ty na kenie jestes"))
    }
}
