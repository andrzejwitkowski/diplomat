package pl.diplomat.usecase

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import pl.diplomat.domain.model.MessageSourceApp
import pl.diplomat.domain.model.MessageStatus
import pl.diplomat.domain.model.PhoneNumber
import pl.diplomat.usecase.testsupport.InMemoryContactRepository
import pl.diplomat.usecase.testsupport.InMemoryMessageRepository

class ProcessIncomingMessageUseCaseTest {

    private lateinit var contactRepository: InMemoryContactRepository
    private lateinit var messageRepository: InMemoryMessageRepository
    private lateinit var useCase: ProcessIncomingMessageUseCase

    @Before
    fun setUp() {
        contactRepository = InMemoryContactRepository()
        messageRepository = InMemoryMessageRepository(contactRepository)
        useCase = ProcessIncomingMessageUseCase(contactRepository, messageRepository)
    }

    @Test
    fun `rejects message from non-whitelisted sender`() = runTest {
        val result = useCase(
            RawIncomingMessage(
                senderPhone = "+48123456789",
                text = "Hello",
                timestamp = 1_000L,
                sourceApp = MessageSourceApp.SMS,
            ),
        )

        assertEquals(ProcessIncomingMessageResult.RejectedNotWhitelisted, result)
        assertTrue(messageRepository.snapshot().isEmpty())
    }

    @Test
    fun `saves pending message for whitelisted sender with longer text`() = runTest {
        contactRepository.add("Alice", PhoneNumber("+48 123 456 789"))

        val result = useCase(
            RawIncomingMessage(
                senderPhone = "+48123456789",
                text = "Can we meet tomorrow afternoon?",
                timestamp = 2_000L,
                sourceApp = MessageSourceApp.WHATSAPP,
            ),
        )

        val saved = (result as ProcessIncomingMessageResult.Saved).message
        assertEquals(MessageStatus.PENDING, saved.status)
        assertEquals(MessageSourceApp.WHATSAPP, saved.sourceApp)
        assertEquals(1, messageRepository.snapshot().size)
    }

    @Test
    fun `classifies short message without question as one-liner`() = runTest {
        contactRepository.add("Bob", PhoneNumber("555-0100"))

        val result = useCase(
            RawIncomingMessage(
                senderPhone = "5550100",
                text = "OK",
                timestamp = 3_000L,
                sourceApp = MessageSourceApp.SMS,
            ),
        )

        val saved = (result as ProcessIncomingMessageResult.Saved).message
        assertEquals(MessageStatus.IGNORED_CONFIRMATION, saved.status)
    }

    @Test
    fun `does not classify question as one-liner even when short`() = runTest {
        contactRepository.add("Bob", PhoneNumber("555-0100"))

        val result = useCase(
            RawIncomingMessage(
                senderPhone = "5550100",
                text = "Ready?",
                timestamp = 4_000L,
                sourceApp = MessageSourceApp.SMS,
            ),
        )

        val saved = (result as ProcessIncomingMessageResult.Saved).message
        assertEquals(MessageStatus.PENDING, saved.status)
    }

    @Test
    fun `rejects invalid phone number`() = runTest {
        contactRepository.add("Alice", PhoneNumber("+48123456789"))

        val result = useCase(
            RawIncomingMessage(
                senderPhone = "   ",
                text = "Hello",
                timestamp = 5_000L,
                sourceApp = MessageSourceApp.SMS,
            ),
        )

        assertEquals(ProcessIncomingMessageResult.RejectedNotWhitelisted, result)
    }
}
