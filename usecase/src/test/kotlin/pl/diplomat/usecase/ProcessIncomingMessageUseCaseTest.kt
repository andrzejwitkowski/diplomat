package pl.diplomat.usecase

import kotlinx.coroutines.test.runTest
import org.junit.Test
import pl.diplomat.domain.model.MessageContent
import pl.diplomat.domain.model.MessageSourceApp
import pl.diplomat.domain.model.MessageStatus
import pl.diplomat.domain.model.PhoneNumber
import pl.diplomat.domain.model.VisualMediaKind
import pl.diplomat.domain.testsupport.TestConstants
import pl.diplomat.usecase.testsupport.MessageRepositoryAssertion
import pl.diplomat.usecase.testsupport.ProcessResultAssertion
import pl.diplomat.usecase.testsupport.aRawIncomingMessage

class ProcessIncomingMessageUseCaseTest : BaseProcessIncomingMessageSpec() {

    @Test
    fun `rejects message from non-whitelisted sender`() = runTest {
        val result = useCase(
            aRawIncomingMessage()
                .withSenderPhone(TestConstants.ALICE_PHONE)
                .withText(TestConstants.TEXT_HELLO)
                .withTimestamp(TestConstants.TIMESTAMP_1)
                .build(),
        )

        ProcessResultAssertion.assertThat(result).isRejectedNotWhitelisted()
        MessageRepositoryAssertion.assertThat(messageRepository.snapshot()).isEmpty()
    }

    @Test
    fun `saves pending message for whitelisted sender with longer text`() = runTest {
        contactRepository.add(TestConstants.ALICE_NAME, PhoneNumber(TestConstants.ALICE_PHONE_FORMATTED))

        val result = useCase(
            aRawIncomingMessage()
                .withSenderPhone(TestConstants.ALICE_PHONE)
                .withText(TestConstants.TEXT_LONG)
                .withTimestamp(TestConstants.TIMESTAMP_2)
                .withSourceApp(MessageSourceApp.WHATSAPP)
                .build(),
        )

        ProcessResultAssertion.assertThat(result)
            .isSaved {
                hasStatus(MessageStatus.PENDING)
                hasSourceApp(MessageSourceApp.WHATSAPP)
            }
        MessageRepositoryAssertion.assertThat(messageRepository.snapshot()).hasSize(1)
    }

    @Test
    fun `classifies short message without question as one-liner`() = runTest {
        contactRepository.add(TestConstants.BOB_NAME, PhoneNumber(TestConstants.BOB_PHONE))

        val result = useCase(
            aRawIncomingMessage()
                .withSenderPhone(TestConstants.BOB_PHONE_NORMALIZED)
                .withText(TestConstants.TEXT_OK)
                .withTimestamp(TestConstants.TIMESTAMP_3)
                .build(),
        )

        ProcessResultAssertion.assertThat(result)
            .isSaved { hasStatus(MessageStatus.IGNORED_CONFIRMATION) }
    }

    @Test
    fun `does not classify question as one-liner even when short`() = runTest {
        contactRepository.add(TestConstants.BOB_NAME, PhoneNumber(TestConstants.BOB_PHONE))

        val result = useCase(
            aRawIncomingMessage()
                .withSenderPhone(TestConstants.BOB_PHONE_NORMALIZED)
                .withText(TestConstants.TEXT_READY)
                .withTimestamp(TestConstants.TIMESTAMP_4)
                .build(),
        )

        ProcessResultAssertion.assertThat(result)
            .isSaved { hasStatus(MessageStatus.PENDING) }
    }

    @Test
    fun `visual only message is always pending`() = runTest {
        contactRepository.add(TestConstants.BOB_NAME, PhoneNumber(TestConstants.BOB_PHONE))

        val result = useCase(
            aRawIncomingMessage()
                .withSenderPhone(TestConstants.BOB_PHONE_NORMALIZED)
                .withContent(MessageContent.VisualOnly(VisualMediaKind.GIF))
                .withTimestamp(TestConstants.TIMESTAMP_5)
                .withSourceApp(MessageSourceApp.WHATSAPP)
                .build(),
        )

        ProcessResultAssertion.assertThat(result)
            .isSaved {
                hasStatus(MessageStatus.PENDING)
                hasVisualKind(VisualMediaKind.GIF)
            }
    }

    @Test
    fun `visual with short caption can still be one-liner`() = runTest {
        contactRepository.add(TestConstants.BOB_NAME, PhoneNumber(TestConstants.BOB_PHONE))

        val result = useCase(
            aRawIncomingMessage()
                .withSenderPhone(TestConstants.BOB_PHONE_NORMALIZED)
                .withContent(MessageContent.VisualWithText(VisualMediaKind.PHOTO, TestConstants.TEXT_OK))
                .withTimestamp(TestConstants.TIMESTAMP_6)
                .withSourceApp(MessageSourceApp.WHATSAPP)
                .build(),
        )

        ProcessResultAssertion.assertThat(result)
            .isSaved { hasStatus(MessageStatus.IGNORED_CONFIRMATION) }
    }

    @Test
    fun `rejects invalid phone number`() = runTest {
        contactRepository.add(TestConstants.ALICE_NAME, PhoneNumber(TestConstants.ALICE_PHONE_FORMATTED))

        val result = useCase(
            aRawIncomingMessage()
                .withSenderPhone(TestConstants.INVALID_PHONE)
                .withText(TestConstants.TEXT_HELLO)
                .withTimestamp(TestConstants.TIMESTAMP_7)
                .build(),
        )

        ProcessResultAssertion.assertThat(result).isRejectedNotWhitelisted()
    }

    @Test
    fun `ignores duplicate notification key`() = runTest {
        contactRepository.add(TestConstants.ALICE_NAME, PhoneNumber(TestConstants.ALICE_PHONE_FORMATTED))
        val notificationKey = "notification-key-1"

        useCase(
            aRawIncomingMessage()
                .withSenderPhone(TestConstants.ALICE_PHONE)
                .withText(TestConstants.TEXT_HELLO)
                .withTimestamp(TestConstants.TIMESTAMP_1)
                .withNotificationKey(notificationKey)
                .build(),
        )

        val duplicate = useCase(
            aRawIncomingMessage()
                .withSenderPhone(TestConstants.ALICE_PHONE)
                .withText(TestConstants.TEXT_LONG)
                .withTimestamp(TestConstants.TIMESTAMP_2)
                .withNotificationKey(notificationKey)
                .build(),
        )

        ProcessResultAssertion.assertThat(duplicate).isIgnoredDuplicate()
        MessageRepositoryAssertion.assertThat(messageRepository.snapshot()).hasSize(1)
    }
}
