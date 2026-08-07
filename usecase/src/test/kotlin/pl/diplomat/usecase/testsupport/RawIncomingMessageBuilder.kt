package pl.diplomat.usecase.testsupport

import pl.diplomat.domain.model.MessageContent
import pl.diplomat.domain.model.MessageSourceApp
import pl.diplomat.domain.testsupport.TestConstants
import pl.diplomat.usecase.RawIncomingMessage

class RawIncomingMessageBuilder {
    private var senderPhone: String = TestConstants.ALICE_PHONE
    private var content: MessageContent = MessageContent.TextOnly(TestConstants.TEXT_HELLO)
    private var timestamp: Long = TestConstants.TIMESTAMP_1
    private var sourceApp: MessageSourceApp = MessageSourceApp.SMS
    private var notificationKey: String? = null
    private var additionalSenderCandidates: List<String> = emptyList()

    fun withSenderPhone(value: String) = apply { senderPhone = value }

    fun withAdditionalSenderCandidates(value: List<String>) = apply { additionalSenderCandidates = value }

    fun withContent(value: MessageContent) = apply { content = value }

    fun withText(body: String) = apply { content = MessageContent.TextOnly(body) }

    fun withTimestamp(value: Long) = apply { timestamp = value }

    fun withSourceApp(value: MessageSourceApp) = apply { sourceApp = value }

    fun withNotificationKey(value: String?) = apply { notificationKey = value }

    fun build(): RawIncomingMessage = RawIncomingMessage(
        senderPhone = senderPhone,
        content = content,
        timestamp = timestamp,
        sourceApp = sourceApp,
        notificationKey = notificationKey,
        additionalSenderCandidates = additionalSenderCandidates,
    )
}

fun aRawIncomingMessage(): RawIncomingMessageBuilder = RawIncomingMessageBuilder()
