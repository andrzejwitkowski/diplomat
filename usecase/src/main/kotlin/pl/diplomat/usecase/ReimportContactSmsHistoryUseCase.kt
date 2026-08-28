package pl.diplomat.usecase

import pl.diplomat.domain.model.MessageSourceApp
import pl.diplomat.domain.model.WhitelistedContact
import pl.diplomat.domain.port.ConversationRangePort
import pl.diplomat.domain.port.MessageRepositoryPort
import pl.diplomat.domain.port.SmsHistoryImportPort
import pl.diplomat.domain.port.SmsHistoryImportResult
import java.time.LocalDate
import java.time.ZoneId

class ReimportContactSmsHistoryUseCase(
    private val messageRepository: MessageRepositoryPort,
    private val conversationRangePort: ConversationRangePort,
    private val smsHistoryImport: SmsHistoryImportPort,
) {
    suspend operator fun invoke(
        contact: WhitelistedContact,
        sinceDate: LocalDate,
    ): SmsHistoryImportResult {
        messageRepository.deleteSmsForContact(contact.id)

        conversationRangePort.get(contact.id)
            ?.takeIf { it.sourceApp == MessageSourceApp.SMS }
            ?.let { conversationRangePort.clear(contact.id) }

        val sinceMillis = sinceDate
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

        return smsHistoryImport.importForContact(contact, sinceMillis)
    }
}
