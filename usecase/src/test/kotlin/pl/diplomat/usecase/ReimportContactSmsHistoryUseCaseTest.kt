package pl.diplomat.usecase

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import pl.diplomat.domain.model.ConversationRange
import pl.diplomat.domain.model.MessageSourceApp
import pl.diplomat.domain.model.PhoneNumber
import pl.diplomat.domain.model.WhitelistedContact
import pl.diplomat.domain.port.ConversationRangePort
import pl.diplomat.domain.port.SmsHistoryImportPort
import pl.diplomat.domain.port.SmsHistoryImportResult
import pl.diplomat.domain.testsupport.TestConstants
import pl.diplomat.domain.testsupport.anIncomingMessage
import pl.diplomat.usecase.testsupport.InMemoryContactRepository
import pl.diplomat.usecase.testsupport.InMemoryMessageRepository
import java.time.LocalDate

class ReimportContactSmsHistoryUseCaseTest {
    private lateinit var contactRepository: InMemoryContactRepository
    private lateinit var messageRepository: InMemoryMessageRepository
    private lateinit var rangePort: RecordingConversationRangePort
    private lateinit var smsHistoryImport: RecordingSmsHistoryImportPort
    private lateinit var useCase: ReimportContactSmsHistoryUseCase
    private lateinit var contact: WhitelistedContact

    @Before
    fun setUp() = runBlocking {
        contactRepository = InMemoryContactRepository()
        messageRepository = InMemoryMessageRepository(contactRepository)
        rangePort = RecordingConversationRangePort()
        smsHistoryImport = RecordingSmsHistoryImportPort()
        useCase = ReimportContactSmsHistoryUseCase(
            messageRepository = messageRepository,
            conversationRangePort = rangePort,
            smsHistoryImport = smsHistoryImport,
        )
        val contactId = contactRepository.add(TestConstants.ALICE_NAME, PhoneNumber(TestConstants.ALICE_PHONE))
        contact = contactRepository.findById(contactId)!!
    }

    @Test
    fun `deletes sms only and imports history`() = runTest {
        messageRepository.save(
            anIncomingMessage()
                .withContactId(contact.id)
                .withSourceApp(MessageSourceApp.SMS)
                .withText("old sms")
                .build(),
        )
        messageRepository.save(
            anIncomingMessage()
                .withContactId(contact.id)
                .withSourceApp(MessageSourceApp.WHATSAPP)
                .withText(TestConstants.TEXT_WHATSAPP)
                .build(),
        )
        smsHistoryImport.result = SmsHistoryImportResult.Success(importedCount = 3)

        val result = useCase(contact, LocalDate.of(2024, 1, 1))

        assertEquals(SmsHistoryImportResult.Success(3), result)
        assertEquals(1, messageRepository.snapshot().size)
        assertEquals(MessageSourceApp.WHATSAPP, messageRepository.snapshot().single().sourceApp)
        assertEquals(contact.id, smsHistoryImport.lastContact?.id)
    }

    @Test
    fun `clears sms range markers only`() = runTest {
        rangePort.range = ConversationRange(
            contactId = contact.id,
            sourceApp = MessageSourceApp.SMS,
            startMessageId = 1L,
            endMessageId = 2L,
        )
        smsHistoryImport.result = SmsHistoryImportResult.Success(0)

        useCase(contact, LocalDate.of(2024, 1, 1))

        assertTrue(rangePort.clearedContactIds.contains(contact.id))
    }

    @Test
    fun `preserves whatsapp range markers`() = runTest {
        rangePort.range = ConversationRange(
            contactId = contact.id,
            sourceApp = MessageSourceApp.WHATSAPP,
            startMessageId = 1L,
            endMessageId = 2L,
        )
        smsHistoryImport.result = SmsHistoryImportResult.Success(0)

        useCase(contact, LocalDate.of(2024, 1, 1))

        assertTrue(rangePort.clearedContactIds.isEmpty())
    }

    private class RecordingSmsHistoryImportPort : SmsHistoryImportPort {
        var result: SmsHistoryImportResult = SmsHistoryImportResult.Success(0)
        var lastContact: WhitelistedContact? = null

        override suspend fun importForContact(
            contact: WhitelistedContact,
            sinceMillis: Long,
        ): SmsHistoryImportResult {
            lastContact = contact
            return result
        }
    }

    private class RecordingConversationRangePort : ConversationRangePort {
        var range: ConversationRange? = null
        val clearedContactIds = mutableListOf<Long>()

        override fun observe(contactId: Long) = kotlinx.coroutines.flow.flow {
            emit(if (range?.contactId == contactId) range else null)
        }

        override fun get(contactId: Long): ConversationRange? =
            range?.takeIf { it.contactId == contactId }

        override fun set(range: ConversationRange) {
            this.range = range
        }

        override fun clear(contactId: Long) {
            clearedContactIds += contactId
            if (this.range?.contactId == contactId) {
                this.range = null
            }
        }
    }
}
