package pl.diplomat.usecase

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test
import pl.diplomat.domain.model.MessageSourceApp
import pl.diplomat.domain.model.MessageStatus
import pl.diplomat.domain.model.PhoneNumber
import pl.diplomat.domain.model.VisualMediaKind
import pl.diplomat.domain.testsupport.TestConstants
import pl.diplomat.domain.testsupport.anIncomingMessage
import pl.diplomat.usecase.testsupport.ConversationListAssertion

class GetActiveConversationsUseCaseTest : BaseGetActiveConversationsSpec() {

    @Test
    fun `returns latest message per contact ordered by timestamp`() = runTest {
        val aliceId = contactRepository.add(TestConstants.ALICE_NAME, PhoneNumber(TestConstants.ALICE_PHONE_ALT))
        val bobId = contactRepository.add(TestConstants.BOB_NAME, PhoneNumber(TestConstants.BOB_PHONE_ALT))

        messageRepository.save(
            anIncomingMessage()
                .withContactId(aliceId)
                .withText(TestConstants.TEXT_FIRST)
                .withTimestamp(TestConstants.TIMESTAMP_100)
                .build(),
        )
        messageRepository.save(
            anIncomingMessage()
                .withContactId(aliceId)
                .withText(TestConstants.TEXT_LATEST_ALICE)
                .withTimestamp(TestConstants.TIMESTAMP_300)
                .build(),
        )
        messageRepository.save(
            anIncomingMessage()
                .withContactId(bobId)
                .withVisualOnly(VisualMediaKind.GIF)
                .withTimestamp(TestConstants.TIMESTAMP_200)
                .withSourceApp(MessageSourceApp.WHATSAPP)
                .withStatus(MessageStatus.REPLIED)
                .build(),
        )

        val conversations = useCase().first()

        ConversationListAssertion.assertThat(conversations)
            .hasSize(2)
            .firstMessageHasTextBody(TestConstants.TEXT_LATEST_ALICE)
            .lastMessageHasVisualKind(VisualMediaKind.GIF)
    }
}
