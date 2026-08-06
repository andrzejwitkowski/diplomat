package pl.diplomat.usecase

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import pl.diplomat.domain.model.IncomingMessage
import pl.diplomat.domain.model.MessageContent
import pl.diplomat.domain.model.MessageSourceApp
import pl.diplomat.domain.model.MessageStatus
import pl.diplomat.domain.model.PhoneNumber
import pl.diplomat.domain.model.bodyText
import pl.diplomat.usecase.testsupport.InMemoryContactRepository
import pl.diplomat.usecase.testsupport.InMemoryMessageRepository

class GetActiveConversationsUseCaseTest {

    private lateinit var contactRepository: InMemoryContactRepository
    private lateinit var messageRepository: InMemoryMessageRepository
    private lateinit var useCase: GetActiveConversationsUseCase

    @Before
    fun setUp() {
        contactRepository = InMemoryContactRepository()
        messageRepository = InMemoryMessageRepository(contactRepository)
        useCase = GetActiveConversationsUseCase(messageRepository)
    }

    @Test
    fun `returns latest message per contact ordered by timestamp`() = runTest {
        val aliceId = contactRepository.add("Alice", PhoneNumber("+48111111111"))
        val bobId = contactRepository.add("Bob", PhoneNumber("+48222222222"))

        messageRepository.save(
            IncomingMessage(
                0,
                aliceId,
                MessageContent.TextOnly("First"),
                100L,
                MessageSourceApp.SMS,
                MessageStatus.PENDING,
            ),
        )
        messageRepository.save(
            IncomingMessage(
                0,
                aliceId,
                MessageContent.TextOnly("Latest from Alice"),
                300L,
                MessageSourceApp.SMS,
                MessageStatus.PENDING,
            ),
        )
        messageRepository.save(
            IncomingMessage(
                0,
                bobId,
                MessageContent.ImageOnly,
                200L,
                MessageSourceApp.WHATSAPP,
                MessageStatus.REPLIED,
            ),
        )

        val conversations = useCase().first()

        assertEquals(2, conversations.size)
        assertEquals("Latest from Alice", conversations[0].lastMessage.content.bodyText())
        assertEquals(MessageContent.ImageOnly, conversations[1].lastMessage.content)
    }
}
