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
    fun rejectsEnglishSystemMetadata() {
        assertTrue(WhatsAppSystemTextFilter.isJunk("online"))
        assertTrue(WhatsAppSystemTextFilter.isJunk("last seen today at 8:59 PM"))
        assertTrue(WhatsAppSystemTextFilter.isJunk("2 unread messages"))
        assertTrue(WhatsAppSystemTextFilter.isJunk("Voice call"))
        assertTrue(WhatsAppSystemTextFilter.isJunk("28 sec."))
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
