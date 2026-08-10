package pl.diplomat.infrastructure.notification

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import pl.diplomat.domain.model.IncomingMessage
import pl.diplomat.domain.model.MessageContent
import pl.diplomat.domain.model.MessageSourceApp
import pl.diplomat.domain.model.MessageStatus

class IncomingMessageNotifierTest {

    @Test
    fun alertsOnlyAccessibilityWhatsAppAndTelephonySms() {
        assertTrue(
            IncomingMessageNotifier.shouldAlertUser(
                pending(
                    sourceApp = MessageSourceApp.WHATSAPP,
                    notificationKey = "a11y:Alice\u0000hi\u0000false\u00000\u0000false",
                ),
            ),
        )
        assertTrue(
            IncomingMessageNotifier.shouldAlertUser(
                pending(
                    sourceApp = MessageSourceApp.SMS,
                    notificationKey = "sms:42",
                ),
            ),
        )
        assertFalse(
            IncomingMessageNotifier.shouldAlertUser(
                pending(
                    sourceApp = MessageSourceApp.WHATSAPP,
                    notificationKey = "0|com.whatsapp|g:abc|123",
                ),
            ),
        )
        assertFalse(
            IncomingMessageNotifier.shouldAlertUser(
                pending(
                    sourceApp = MessageSourceApp.SMS,
                    notificationKey = "0|com.google.android.apps.messaging|g:abc|123",
                ),
            ),
        )
    }

    @Test
    fun ignoresNonPendingAndOutgoing() {
        assertFalse(
            IncomingMessageNotifier.shouldAlertUser(
                pending(
                    sourceApp = MessageSourceApp.SMS,
                    notificationKey = "sms:1",
                    status = MessageStatus.IGNORED_CONFIRMATION,
                ),
            ),
        )
        assertFalse(
            IncomingMessageNotifier.shouldAlertUser(
                pending(
                    sourceApp = MessageSourceApp.SMS,
                    notificationKey = "sms:1",
                    isOutgoing = true,
                ),
            ),
        )
    }

    private fun pending(
        sourceApp: MessageSourceApp,
        notificationKey: String,
        status: MessageStatus = MessageStatus.PENDING,
        isOutgoing: Boolean = false,
    ) = IncomingMessage(
        id = 1,
        contactId = 1,
        content = MessageContent.TextOnly("hello"),
        timestamp = 1L,
        sourceApp = sourceApp,
        status = status,
        notificationKey = notificationKey,
        isRead = isOutgoing,
        isOutgoing = isOutgoing,
    )
}
