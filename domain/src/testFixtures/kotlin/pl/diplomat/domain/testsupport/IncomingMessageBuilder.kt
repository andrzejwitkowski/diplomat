package pl.diplomat.domain.testsupport

import pl.diplomat.domain.model.IncomingMessage
import pl.diplomat.domain.model.MessageContent
import pl.diplomat.domain.model.MessageSourceApp
import pl.diplomat.domain.model.MessageStatus
import pl.diplomat.domain.model.VisualMediaKind

class IncomingMessageBuilder {
    private var id: Long = TestConstants.UNSAVED_ID
    private var contactId: Long = TestConstants.CONTACT_ID
    private var content: MessageContent = MessageContent.TextOnly(TestConstants.TEXT_HELLO)
    private var timestamp: Long = TestConstants.TIMESTAMP_1
    private var sourceApp: MessageSourceApp = MessageSourceApp.SMS
    private var status: MessageStatus = MessageStatus.PENDING
    private var notificationKey: String? = null
    private var isRead: Boolean = false
    private var isOutgoing: Boolean = false

    fun withId(value: Long) = apply { id = value }

    fun withContactId(value: Long) = apply { contactId = value }

    fun withContent(value: MessageContent) = apply { content = value }

    fun withText(body: String) = apply { content = MessageContent.TextOnly(body) }

    fun withVisualOnly(kind: VisualMediaKind = VisualMediaKind.PHOTO) =
        apply { content = MessageContent.VisualOnly(kind) }

    fun withVisualAndText(kind: VisualMediaKind, body: String) =
        apply { content = MessageContent.VisualWithText(kind, body) }

    fun withTimestamp(value: Long) = apply { timestamp = value }

    fun withSourceApp(value: MessageSourceApp) = apply { sourceApp = value }

    fun withStatus(value: MessageStatus) = apply { status = value }

    fun withNotificationKey(value: String?) = apply { notificationKey = value }

    fun withIsRead(value: Boolean) = apply { isRead = value }

    fun withIsOutgoing(value: Boolean) = apply { isOutgoing = value }

    fun build(): IncomingMessage = IncomingMessage(
        id = id,
        contactId = contactId,
        content = content,
        timestamp = timestamp,
        sourceApp = sourceApp,
        status = status,
        notificationKey = notificationKey,
        isRead = isRead,
        isOutgoing = isOutgoing,
    )
}

fun anIncomingMessage(): IncomingMessageBuilder = IncomingMessageBuilder()
