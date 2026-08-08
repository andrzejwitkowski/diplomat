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

class SmsRowMapperTest {

    @Test
    fun sentTypeIsOutgoingInboxIsNot() {
        assertTrue(SmsRowMapper.isOutgoing(Telephony.Sms.MESSAGE_TYPE_SENT))
        assertFalse(SmsRowMapper.isOutgoing(Telephony.Sms.MESSAGE_TYPE_INBOX))
    }

    @Test
    fun toRawMapsSentAndInbox() {
        val sent = SmsRowMapper.toRaw(
            id = 10L,
            address = "+48111111111",
            body = "hello",
            date = 1_700_000_000_000L,
            type = Telephony.Sms.MESSAGE_TYPE_SENT,
        )
        assertNotNull(sent)
        assertTrue(sent!!.isOutgoing)
        assertEquals(MessageSourceApp.SMS, sent.sourceApp)
        assertEquals("sms:10", sent.notificationKey)
        assertEquals(MessageContent.TextOnly("hello"), sent.content)

        val inbox = SmsRowMapper.toRaw(
            id = 11L,
            address = "+48111111111",
            body = "hi",
            date = 1_700_000_000_001L,
            type = Telephony.Sms.MESSAGE_TYPE_INBOX,
        )
        assertNotNull(inbox)
        assertFalse(inbox!!.isOutgoing)
    }

    @Test
    fun toRawSkipsDraftsAndBlank() {
        assertNull(
            SmsRowMapper.toRaw(
                id = 1L,
                address = "+48111111111",
                body = "x",
                date = 1L,
                type = Telephony.Sms.MESSAGE_TYPE_DRAFT,
            ),
        )
        assertNull(
            SmsRowMapper.toRaw(
                id = 1L,
                address = "",
                body = "x",
                date = 1L,
                type = Telephony.Sms.MESSAGE_TYPE_INBOX,
            ),
        )
    }
}
