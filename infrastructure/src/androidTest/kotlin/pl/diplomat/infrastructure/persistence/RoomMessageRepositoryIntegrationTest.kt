package pl.diplomat.infrastructure.persistence

import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Test
import pl.diplomat.domain.model.MessageContent
import pl.diplomat.domain.model.MessageContentType
import pl.diplomat.domain.model.MessageSourceApp
import pl.diplomat.domain.model.MessageStatus
import pl.diplomat.domain.model.PhoneNumber
import pl.diplomat.domain.model.VisualMediaKind
import pl.diplomat.domain.testsupport.MessageAssertion
import pl.diplomat.domain.testsupport.TestConstants
import pl.diplomat.domain.testsupport.WhitelistedContactAssertion
import pl.diplomat.domain.testsupport.anIncomingMessage
import pl.diplomat.domain.testsupport.aWhitelistedContact
import pl.diplomat.infrastructure.testsupport.ConversationRepositoryAssertion
import pl.diplomat.infrastructure.testsupport.MessageEntityAssertion
import pl.diplomat.infrastructure.testsupport.MessageHistoryAssertion

class RoomMessageRepositoryIntegrationTest : BaseRoomIntegrationSpec() {

    @Test
    fun saveAndObserveActiveConversations() = runTest {
        val contactId = contactRepository.add(TestConstants.ALICE_NAME, PhoneNumber(TestConstants.ALICE_PHONE_FORMATTED))
        val messageId = messageRepository.save(
            anIncomingMessage()
                .withContactId(contactId)
                .withText(TestConstants.TEXT_HELLO_DIPLOMAT)
                .withTimestamp(TestConstants.TIMESTAMP_ROOM)
                .build(),
        )

        val conversations = messageRepository.observeActiveConversations().first()

        ConversationRepositoryAssertion.assertThat(conversations)
            .savedMessageIdIsPositive(messageId)
            .hasSize(1)
            .firstContactHasDisplayName(TestConstants.ALICE_NAME)
            .first { hasTextBody(TestConstants.TEXT_HELLO_DIPLOMAT) }
    }

    @Test
    fun mapperRoundTripPreservesTextMessage() {
        val domain = anIncomingMessage()
            .withId(TestConstants.MESSAGE_ID)
            .withText(TestConstants.TEXT_MAPPED)
            .withTimestamp(TestConstants.TIMESTAMP_99)
            .withSourceApp(MessageSourceApp.WHATSAPP)
            .withStatus(MessageStatus.IGNORED_CONFIRMATION)
            .build()

        MessageAssertion.assertThat(domain.toEntity().toDomain()).isEqualTo(domain)
    }

    @Test
    fun mapperRoundTripPreservesGifMessage() {
        val domain = anIncomingMessage()
            .withId(43)
            .withVisualOnly(VisualMediaKind.GIF)
            .withTimestamp(TestConstants.TIMESTAMP_100_MEDIA)
            .withSourceApp(MessageSourceApp.WHATSAPP)
            .build()

        val entity = domain.toEntity()

        MessageEntityAssertion.assertThat(entity)
            .hasContentType(MessageContentType.IMAGE)
            .hasMediaKind(VisualMediaKind.GIF)
            .hasText("")

        MessageAssertion.assertThat(entity.toDomain()).isEqualTo(domain)
    }

    @Test
    fun mapperRoundTripPreservesImageWithTextMessage() {
        val domain = anIncomingMessage()
            .withId(44)
            .withVisualAndText(VisualMediaKind.PHOTO, TestConstants.TEXT_SUNSET)
            .withTimestamp(TestConstants.TIMESTAMP_101)
            .build()

        MessageAssertion.assertThat(domain.toEntity().toDomain()).isEqualTo(domain)
    }

    @Test
    fun contactMapperRoundTrip() {
        val domain = aWhitelistedContact()
            .withId(3)
            .withDisplayName(TestConstants.BOB_NAME)
            .withPhoneNumber(TestConstants.BOB_PHONE_FORMATTED)
            .build()

        WhitelistedContactAssertion.assertThat(domain.toEntity().toDomain()).isEqualTo(domain)
    }

    @Test
    fun findMessagesByContactIdReturnsHistory() = runTest {
        val contactId = contactRepository.add(TestConstants.CAROL_NAME, PhoneNumber(TestConstants.CAROL_PHONE))
        messageRepository.save(
            anIncomingMessage()
                .withContactId(contactId)
                .withText(TestConstants.TEXT_OLDER)
                .withTimestamp(TestConstants.TIMESTAMP_100)
                .withStatus(MessageStatus.REPLIED)
                .build(),
        )
        messageRepository.save(
            anIncomingMessage()
                .withContactId(contactId)
                .withVisualAndText(VisualMediaKind.GIF, TestConstants.TEXT_NEWER)
                .withTimestamp(TestConstants.TIMESTAMP_200)
                .build(),
        )

        val history = messageRepository.findMessagesByContactId(contactId)

        MessageHistoryAssertion.assertThat(history)
            .hasSize(2)
            .first {
                hasTextBody(TestConstants.TEXT_NEWER)
                hasVisualKind(VisualMediaKind.GIF)
                hasContent(MessageContent.VisualWithText(VisualMediaKind.GIF, TestConstants.TEXT_NEWER))
            }
    }

    @Test
    fun observeLatestPerContactReturnsOneMessageWhenTimestampsTie() = runTest {
        val contactId = contactRepository.add(TestConstants.ALICE_NAME, PhoneNumber(TestConstants.ALICE_PHONE_FORMATTED))
        val sharedTimestamp = TestConstants.TIMESTAMP_300

        messageRepository.save(
            anIncomingMessage()
                .withContactId(contactId)
                .withText(TestConstants.TEXT_OLDER)
                .withTimestamp(sharedTimestamp)
                .build(),
        )
        messageRepository.save(
            anIncomingMessage()
                .withContactId(contactId)
                .withText(TestConstants.TEXT_NEWER)
                .withTimestamp(sharedTimestamp)
                .build(),
        )

        val conversations = messageRepository.observeActiveConversations().first()

        ConversationRepositoryAssertion.assertThat(conversations)
            .hasSize(1)
            .first { hasTextBody(TestConstants.TEXT_NEWER) }
    }

    @Test
    fun observeActiveConversationsReflectsContactRename() = runTest {
        val contactId = contactRepository.add(TestConstants.ALICE_NAME, PhoneNumber(TestConstants.ALICE_PHONE_FORMATTED))
        messageRepository.save(
            anIncomingMessage()
                .withContactId(contactId)
                .withText(TestConstants.TEXT_HELLO_DIPLOMAT)
                .withTimestamp(TestConstants.TIMESTAMP_ROOM)
                .build(),
        )

        val snapshots = async {
            messageRepository.observeActiveConversations().take(2).toList()
        }

        val renamedContact = aWhitelistedContact()
            .withId(contactId)
            .withDisplayName(TestConstants.BOB_NAME)
            .withPhoneNumber(TestConstants.ALICE_PHONE_FORMATTED)
            .build()
        contactRepository.update(renamedContact)

        ConversationRepositoryAssertion.assertThat(snapshots.await().last())
            .hasSize(1)
            .firstContactHasDisplayName(TestConstants.BOB_NAME)
    }

    @Test
    fun observeActiveConversationsDropsRemovedContact() = runTest {
        val contactId = contactRepository.add(TestConstants.ALICE_NAME, PhoneNumber(TestConstants.ALICE_PHONE_FORMATTED))
        messageRepository.save(
            anIncomingMessage()
                .withContactId(contactId)
                .withText(TestConstants.TEXT_HELLO_DIPLOMAT)
                .withTimestamp(TestConstants.TIMESTAMP_ROOM)
                .build(),
        )

        val snapshots = async {
            messageRepository.observeActiveConversations().take(2).toList()
        }
        contactRepository.remove(contactId)

        ConversationRepositoryAssertion.assertThat(snapshots.await().last()).hasSize(0)
    }

    @Test
    fun ignoresIdenticalNotificationFingerprint() = runTest {
        val contactId = contactRepository.add(TestConstants.ALICE_NAME, PhoneNumber(TestConstants.ALICE_PHONE_FORMATTED))
        val message = anIncomingMessage()
            .withContactId(contactId)
            .withText(TestConstants.TEXT_HELLO)
            .withTimestamp(TestConstants.TIMESTAMP_1)
            .withNotificationKey("shared-notification-key")
            .build()

        val firstId = messageRepository.save(message)
        val duplicateId = messageRepository.save(message)

        org.junit.Assert.assertTrue(firstId > 0)
        org.junit.Assert.assertEquals(-1L, duplicateId)
    }

    @Test
    fun savesDistinctMessagesSharingNotificationKey() = runTest {
        val contactId = contactRepository.add(TestConstants.ALICE_NAME, PhoneNumber(TestConstants.ALICE_PHONE_FORMATTED))
        val notificationKey = "shared-notification-key"

        messageRepository.save(
            anIncomingMessage()
                .withContactId(contactId)
                .withText(TestConstants.TEXT_OLDER)
                .withTimestamp(TestConstants.TIMESTAMP_100)
                .withNotificationKey(notificationKey)
                .build(),
        )
        val secondId = messageRepository.save(
            anIncomingMessage()
                .withContactId(contactId)
                .withText(TestConstants.TEXT_NEWER)
                .withTimestamp(TestConstants.TIMESTAMP_200)
                .withNotificationKey(notificationKey)
                .build(),
        )

        org.junit.Assert.assertTrue(secondId > 0)
        MessageHistoryAssertion.assertThat(messageRepository.findMessagesByContactId(contactId))
            .hasSize(2)
    }
}
