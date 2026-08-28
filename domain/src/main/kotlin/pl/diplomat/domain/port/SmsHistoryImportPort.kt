package pl.diplomat.domain.port

import pl.diplomat.domain.model.WhitelistedContact

sealed class SmsHistoryImportResult {
    data class Success(val importedCount: Int) : SmsHistoryImportResult()
    data object PermissionDenied : SmsHistoryImportResult()
}

interface SmsHistoryImportPort {
    suspend fun importForContact(contact: WhitelistedContact, sinceMillis: Long): SmsHistoryImportResult
}
