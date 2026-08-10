package pl.diplomat.infrastructure.sms

import android.provider.Telephony
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import pl.diplomat.domain.model.MessageContent
import pl.diplomat.domain.model.MessageSourceApp
import pl.diplomat.domain.model.VisualMediaKind

class MmsRowMapperTest {

    @Test
    fun toRawMapsInboxAndSentWithMillisTimestamp() {
        val inbox = MmsRowMapper.toRaw(
            id = 42L,
            address = "+48506021751",
            parts = MmsPartsContent("He, he, he", null),
            dateSeconds = 1_700_000_000L,
            messageBox = Telephony.Mms.MESSAGE_BOX_INBOX,
        )
        assertNotNull(inbox)
        assertFalse(inbox!!.isOutgoing)
        assertEquals(MessageSourceApp.SMS, inbox.sourceApp)
        assertEquals("mms:42", inbox.notificationKey)
        assertEquals(MessageContent.TextOnly("He, he, he"), inbox.content)
        assertEquals(1_700_000_000_000L, inbox.timestamp)

        val sent = MmsRowMapper.toRaw(
            id = 43L,
            address = "506021751",
            parts = MmsPartsContent("Teraz to ja też chcę", null),
            dateSeconds = 1_700_000_001L,
            messageBox = Telephony.Mms.MESSAGE_BOX_SENT,
        )
        assertNotNull(sent)
        assertTrue(sent!!.isOutgoing)
        assertEquals("mms:43", sent.notificationKey)
    }

    @Test
    fun toRawMapsVisualOnlyWhenMediaWithoutText() {
        val photo = MmsRowMapper.toRaw(
            id = 10L,
            address = "+48111111111",
            parts = MmsPartsContent(null, VisualMediaKind.PHOTO),
            dateSeconds = 1L,
            messageBox = Telephony.Mms.MESSAGE_BOX_INBOX,
        )
        assertEquals(MessageContent.VisualOnly(VisualMediaKind.PHOTO), photo!!.content)

        val gif = MmsRowMapper.toRaw(
            id = 11L,
            address = "+48111111111",
            parts = MmsPartsContent("   ", VisualMediaKind.GIF),
            dateSeconds = 1L,
            messageBox = Telephony.Mms.MESSAGE_BOX_SENT,
        )
        assertEquals(MessageContent.VisualOnly(VisualMediaKind.GIF), gif!!.content)
        assertTrue(gif.isOutgoing)
    }

    @Test
    fun toRawMapsVisualWithTextWhenMediaAndCaption() {
        val raw = MmsRowMapper.toRaw(
            id = 12L,
            address = "+48111111111",
            parts = MmsPartsContent("patrz", VisualMediaKind.VIDEO),
            dateSeconds = 1L,
            messageBox = Telephony.Mms.MESSAGE_BOX_INBOX,
        )
        assertEquals(
            MessageContent.VisualWithText(VisualMediaKind.VIDEO, "patrz"),
            raw!!.content,
        )
    }

    @Test
    fun mediaKindFromContentTypeMapsMime() {
        assertEquals(VisualMediaKind.PHOTO, MmsRowMapper.mediaKindFromContentType("image/jpeg"))
        assertEquals(VisualMediaKind.PHOTO, MmsRowMapper.mediaKindFromContentType("IMAGE/PNG"))
        assertEquals(VisualMediaKind.GIF, MmsRowMapper.mediaKindFromContentType("image/gif"))
        assertEquals(VisualMediaKind.VIDEO, MmsRowMapper.mediaKindFromContentType("video/mp4"))
        assertNull(MmsRowMapper.mediaKindFromContentType("application/smil"))
        assertNull(MmsRowMapper.mediaKindFromContentType("text/plain"))
    }

    @Test
    fun resolvePartsPrefersTextAndFirstVisualMime() {
        val photoOnly = MmsRowMapper.resolveParts(
            listOf(
                "application/smil" to null,
                "image/jpeg" to null,
            ),
        )
        assertEquals(null, photoOnly.text)
        assertEquals(VisualMediaKind.PHOTO, photoOnly.mediaKind)

        val captionedGif = MmsRowMapper.resolveParts(
            listOf(
                "text/plain" to "hi",
                "image/gif" to null,
                "image/jpeg" to null,
            ),
        )
        assertEquals("hi", captionedGif.text)
        assertEquals(VisualMediaKind.GIF, captionedGif.mediaKind)
    }

    @Test
    fun pendingOutboundTypesAreHeld() {
        assertTrue(MmsRowMapper.isPendingOutbound(Telephony.Mms.MESSAGE_BOX_OUTBOX))
        assertFalse(MmsRowMapper.isPendingOutbound(Telephony.Mms.MESSAGE_BOX_SENT))
        assertFalse(MmsRowMapper.isPendingOutbound(Telephony.Mms.MESSAGE_BOX_INBOX))
        assertNull(
            MmsRowMapper.toRaw(
                id = 1L,
                address = "+48111111111",
                parts = MmsPartsContent("x", null),
                dateSeconds = 1L,
                messageBox = Telephony.Mms.MESSAGE_BOX_OUTBOX,
            ),
        )
    }

    @Test
    fun toRawSkipsDraftBlankAndNonPositive() {
        assertNull(
            MmsRowMapper.toRaw(
                id = 1L,
                address = "+48111111111",
                parts = MmsPartsContent("x", null),
                dateSeconds = 1L,
                messageBox = Telephony.Mms.MESSAGE_BOX_DRAFTS,
            ),
        )
        assertNull(
            MmsRowMapper.toRaw(
                id = 1L,
                address = "",
                parts = MmsPartsContent("x", null),
                dateSeconds = 1L,
                messageBox = Telephony.Mms.MESSAGE_BOX_INBOX,
            ),
        )
        assertNull(
            MmsRowMapper.toRaw(
                id = 1L,
                address = "+48111111111",
                parts = MmsPartsContent("   ", null),
                dateSeconds = 1L,
                messageBox = Telephony.Mms.MESSAGE_BOX_INBOX,
            ),
        )
        assertNull(
            MmsRowMapper.toRaw(
                id = 1L,
                address = "+48111111111",
                parts = MmsPartsContent(null, null),
                dateSeconds = 1L,
                messageBox = Telephony.Mms.MESSAGE_BOX_INBOX,
            ),
        )
    }
}
