package pl.diplomat.infrastructure.notification

import android.os.Bundle
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import pl.diplomat.domain.model.MessageSourceApp

@RunWith(AndroidJUnit4::class)
class NotificationParserTest {

    @Test
    fun parsesSmsNotification() {
        val extras = Bundle().apply {
            putCharSequence("android.title", "+48 123 456 789")
            putCharSequence("android.text", "Test SMS body")
        }

        val parsed = NotificationParser.parse(
            packageName = "com.google.android.apps.messaging",
            extras = extras,
            postedAtMillis = 1_000L,
        )

        assertNotNull(parsed)
        assertEquals(MessageSourceApp.SMS, parsed!!.sourceApp)
        assertEquals("Test SMS body", parsed.text)
    }

    @Test
    fun parsesWhatsAppNotification() {
        val extras = Bundle().apply {
            putCharSequence("android.title", "Alice")
            putCharSequence("android.text", "WhatsApp message")
            putString("android.conversationTitle", "Alice")
        }

        val parsed = NotificationParser.parse(
            packageName = "com.whatsapp",
            extras = extras,
            postedAtMillis = 2_000L,
        )

        assertNotNull(parsed)
        assertEquals(MessageSourceApp.WHATSAPP, parsed!!.sourceApp)
        assertEquals("Alice", parsed.senderPhone)
    }

    @Test
    fun ignoresUnsupportedPackage() {
        val extras = Bundle().apply {
            putCharSequence("android.text", "Ignored")
        }

        val parsed = NotificationParser.parse(
            packageName = "com.example.unknown",
            extras = extras,
            postedAtMillis = 3_000L,
        )

        assertNull(parsed)
    }
}
