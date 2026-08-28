package pl.diplomat.infrastructure.sms

import android.content.Context
import pl.diplomat.domain.model.WhitelistedContact
import pl.diplomat.domain.normalization.NormalizationService
import pl.diplomat.domain.port.SmsHistoryImportPort
import pl.diplomat.domain.port.SmsHistoryImportResult
import pl.diplomat.infrastructure.debug.DevLog
import pl.diplomat.usecase.ProcessIncomingMessageResult
import pl.diplomat.usecase.RawIncomingMessage

class SmsHistoryImporter(
    private val context: Context,
    private val normalization: NormalizationService,
    private val process: suspend (RawIncomingMessage) -> ProcessIncomingMessageResult,
) : SmsHistoryImportPort {
    private val smsQueries = SmsTelephonyQueries(context.contentResolver)
    private val mmsQueries = MmsTelephonyQueries(context.contentResolver)

    override suspend fun importForContact(
        contact: WhitelistedContact,
        sinceMillis: Long,
    ): SmsHistoryImportResult {
        if (!ReadSmsPermission.isGranted(context)) {
            return SmsHistoryImportResult.PermissionDenied
        }

        var saved = 0
        var scanned = 0

        for (row in smsQueries.querySince(sinceMillis)) {
            scanned++
            if (SmsRowMapper.isPendingOutbound(row.type)) continue
            if (!normalization.phonesMatch(contact.phoneNumber, row.address)) continue
            val raw = SmsRowMapper.toRaw(row.id, row.address, row.body, row.date, row.type) ?: continue
            if (process(raw) is ProcessIncomingMessageResult.Saved) saved++
        }

        for (row in mmsQueries.querySince(sinceMillis / 1_000L)) {
            scanned++
            if (MmsRowMapper.isPendingOutbound(row.messageBox)) continue
            val address = mmsQueries.queryAddress(row.id, row.messageBox) ?: continue
            if (!normalization.phonesMatch(contact.phoneNumber, address)) continue
            val body = mmsQueries.queryText(row.id) ?: continue
            val raw = MmsRowMapper.toRaw(row.id, address, body, row.dateSeconds, row.messageBox) ?: continue
            if (process(raw) is ProcessIncomingMessageResult.Saved) saved++
        }

        DevLog.log(
            "SMS",
            "history import contact=${contact.id} since=$sinceMillis scanned=$scanned saved=$saved",
        )
        return SmsHistoryImportResult.Success(saved)
    }
}
