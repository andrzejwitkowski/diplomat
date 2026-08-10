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

class MmsRowMapperTest {

    @Test
    fun toRawMapsInboxAndSentWithMillisTimestamp() {
        val inbox = MmsRowMapper.toRaw(
            id = 42L,
            address = "+48506021751",
            body = "He, he, he",
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
            body = "Teraz to ja też chcę",
            dateSeconds = 1_700_000_001L,
            messageBox = Telephony.Mms.MESSAGE_BOX_SENT,
        )
        assertNotNull(sent)
        assertTrue(sent!!.isOutgoing)
        assertEquals("mms:43", sent.notificationKey)
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
                body = "x",
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
                body = "x",
                dateSeconds = 1L,
                messageBox = Telephony.Mms.MESSAGE_BOX_DRAFTS,
            ),
        )
        assertNull(
            MmsRowMapper.toRaw(
                id = 1L,
                address = "",
                body = "x",
                dateSeconds = 1L,
                messageBox = Telephony.Mms.MESSAGE_BOX_INBOX,
            ),
        )
        assertNull(
            MmsRowMapper.toRaw(
                id = 1L,
                address = "+48111111111",
                body = "   ",
                dateSeconds = 1L,
                messageBox = Telephony.Mms.MESSAGE_BOX_INBOX,
            ),
        )
    }
}
