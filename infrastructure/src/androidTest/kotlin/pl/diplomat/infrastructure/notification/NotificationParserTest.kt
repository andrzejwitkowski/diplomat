package pl.diplomat.infrastructure.notification

import android.graphics.Bitmap
import android.os.Bundle
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import pl.diplomat.domain.model.MessageContent
import pl.diplomat.domain.model.MessageSourceApp
import pl.diplomat.domain.model.VisualMediaKind
import pl.diplomat.domain.model.visualKind

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
        assertEquals(MessageContent.TextOnly("Test SMS body"), parsed.content)
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
        assertEquals(MessageContent.TextOnly("WhatsApp message"), parsed.content)
    }

    @Test
    fun parsesPhotoNotificationFromPlaceholderText() {
        val extras = Bundle().apply {
            putCharSequence("android.title", "Alice")
            putCharSequence("android.text", "📷 Photo")
            putString("android.conversationTitle", "Alice")
        }

        val parsed = NotificationParser.parse(
            packageName = "com.whatsapp",
            extras = extras,
            postedAtMillis = 2_500L,
        )

        assertNotNull(parsed)
        assertEquals(MessageContent.VisualOnly(VisualMediaKind.PHOTO), parsed!!.content)
    }

    @Test
    fun parsesGifNotificationFromPlaceholderText() {
        val extras = Bundle().apply {
            putCharSequence("android.title", "Alice")
            putCharSequence("android.text", "GIF")
            putString("android.conversationTitle", "Alice")
        }

        val parsed = NotificationParser.parse(
            packageName = "com.whatsapp",
            extras = extras,
            postedAtMillis = 2_550L,
        )

        assertNotNull(parsed)
        assertEquals(MessageContent.VisualOnly(VisualMediaKind.GIF), parsed!!.content)
    }

    @Test
    fun parsesGifNotificationFromEmojiPrefix() {
        val extras = Bundle().apply {
            putCharSequence("android.title", "Alice")
            putCharSequence("android.text", "🎬 GIF")
            putString("android.conversationTitle", "Alice")
        }

        val parsed = NotificationParser.parse(
            packageName = "com.whatsapp",
            extras = extras,
            postedAtMillis = 2_560L,
        )

        assertNotNull(parsed)
        assertEquals(MessageContent.VisualOnly(VisualMediaKind.GIF), parsed!!.content)
    }

    @Test
    fun parsesStickerNotification() {
        val extras = Bundle().apply {
            putCharSequence("android.title", "Alice")
            putCharSequence("android.text", "Sticker")
            putString("android.conversationTitle", "Alice")
        }

        val parsed = NotificationParser.parse(
            packageName = "com.whatsapp",
            extras = extras,
            postedAtMillis = 2_570L,
        )

        assertNotNull(parsed)
        assertEquals(MessageContent.VisualOnly(VisualMediaKind.STICKER), parsed!!.content)
    }

    @Test
    fun parsesVideoNotification() {
        val extras = Bundle().apply {
            putCharSequence("android.title", "Alice")
            putCharSequence("android.text", "Video")
            putString("android.conversationTitle", "Alice")
        }

        val parsed = NotificationParser.parse(
            packageName = "com.whatsapp",
            extras = extras,
            postedAtMillis = 2_580L,
        )

        assertNotNull(parsed)
        assertEquals(MessageContent.VisualOnly(VisualMediaKind.VIDEO), parsed!!.content)
    }

    @Test
    fun parsesImageWithCaptionNotification() {
        val extras = Bundle().apply {
            putCharSequence("android.title", "Alice")
            putCharSequence("android.text", "Look at this sunset")
            putParcelable("android.picture", Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888))
            putString("android.conversationTitle", "Alice")
        }

        val parsed = NotificationParser.parse(
            packageName = "com.whatsapp",
            extras = extras,
            postedAtMillis = 2_600L,
        )

        assertNotNull(parsed)
        assertEquals(
            MessageContent.VisualWithText(VisualMediaKind.PHOTO, "Look at this sunset"),
            parsed!!.content,
        )
    }

    @Test
    fun parsesImageOnlyNotificationFromPictureExtra() {
        val extras = Bundle().apply {
            putCharSequence("android.title", "+48 123 456 789")
            putParcelable("android.picture", Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888))
        }

        val parsed = NotificationParser.parse(
            packageName = "com.google.android.apps.messaging",
            extras = extras,
            postedAtMillis = 2_700L,
        )

        assertNotNull(parsed)
        assertTrue(parsed!!.content is MessageContent.VisualOnly)
        assertEquals(VisualMediaKind.PHOTO, parsed.content.visualKind())
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
