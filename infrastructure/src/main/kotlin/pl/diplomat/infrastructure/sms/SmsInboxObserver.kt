package pl.diplomat.infrastructure.sms

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.Telephony
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import pl.diplomat.infrastructure.debug.DevLog
import pl.diplomat.usecase.ProcessIncomingMessageResult
import pl.diplomat.usecase.RawIncomingMessage
import java.time.LocalDate
import java.time.ZoneId

class SmsInboxObserver(
    private val context: Context,
    private val scope: CoroutineScope,
    private val process: suspend (RawIncomingMessage) -> ProcessIncomingMessageResult,
) {
    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val mms = MmsTelephonyQueries(context.contentResolver)
    private val syncMutex = Mutex()
    private var registered = false

    private val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) = syncNew()
        override fun onChange(selfChange: Boolean, uri: Uri?) = syncNew()
    }

    fun start() {
        scope.launch { ensureStarted() }
    }

    fun resyncToday() = start()

    private suspend fun ensureStarted() {
        if (!hasReadSmsPermission()) return
        syncMutex.withLock {
            migrateCheckpointPrefs()

            val snapshotSmsMaxId = queryMaxSmsId()
            if (snapshotSmsMaxId == null) {
                DevLog.log("SMS", "max sms id query failed, will retry on next start")
                return@withLock
            }
            val snapshotMmsMaxId = mms.queryMaxId() ?: 0L

            backfillTodaySms(snapshotSmsMaxId)
            backfillTodayMms(snapshotMmsMaxId)
            prefs.edit()
                .putLong(KEY_LAST_SMS_ID, maxOf(lastSeenSmsId(), snapshotSmsMaxId))
                .putLong(KEY_LAST_MMS_ID, maxOf(lastSeenMmsId(), snapshotMmsMaxId))
                .remove(KEY_BACKFILL_TODAY_DONE_LEGACY)
                .apply()

            syncNewInternal()
            if (!registered) {
                context.contentResolver.registerContentObserver(Telephony.Sms.CONTENT_URI, true, observer)
                context.contentResolver.registerContentObserver(Telephony.Mms.CONTENT_URI, true, observer)
                registered = true
                syncNewInternal()
            }
            DevLog.log(
                "SMS",
                "observer ready lastSmsId=${lastSeenSmsId()} lastMmsId=${lastSeenMmsId()}",
            )
        }
    }

    private fun migrateCheckpointPrefs() {
        if (!prefs.contains(KEY_LAST_SMS_ID)) {
            prefs.edit()
                .putLong(KEY_LAST_SMS_ID, prefs.getLong(KEY_LAST_ID_LEGACY, 0L))
                .apply()
        }
        if (!prefs.contains(KEY_LAST_MMS_ID)) {
            prefs.edit().putLong(KEY_LAST_MMS_ID, 0L).apply()
        }
    }

    private fun hasReadSmsPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) ==
            PackageManager.PERMISSION_GRANTED

    private fun lastSeenSmsId(): Long = prefs.getLong(KEY_LAST_SMS_ID, prefs.getLong(KEY_LAST_ID_LEGACY, 0L))

    private fun lastSeenMmsId(): Long = prefs.getLong(KEY_LAST_MMS_ID, 0L)

    private fun syncNew() {
        if (!hasReadSmsPermission()) return
        scope.launch {
            syncMutex.withLock {
                if (!registered) return@withLock
                syncNewInternal()
            }
        }
    }

    private suspend fun backfillTodaySms(upToId: Long) {
        val rows = querySmsSinceDate(todayStartMillis()).filter { it.id <= upToId }
        var saved = 0
        for (row in rows) {
            if (SmsRowMapper.isPendingOutbound(row.type)) continue
            val raw = SmsRowMapper.toRaw(row.id, row.address, row.body, row.date, row.type) ?: continue
            when (process(raw)) {
                is ProcessIncomingMessageResult.Saved -> saved++
                ProcessIncomingMessageResult.RejectedNotWhitelisted ->
                    DevLog.log("SMS", "backfill rejected addr=${row.address} id=${row.id}")
                ProcessIncomingMessageResult.IgnoredDuplicate -> Unit
            }
        }
        DevLog.log("SMS", "backfill today sms rows=${rows.size} saved=$saved upToId=$upToId")
    }

    private suspend fun backfillTodayMms(upToId: Long) {
        val rows = mms.querySince(todayStartMillis() / 1_000L).filter { it.id <= upToId }
        var saved = 0
        for (row in rows) {
            val address = mms.queryAddress(row.id, row.messageBox) ?: continue
            val body = mms.queryText(row.id) ?: continue
            val raw = MmsRowMapper.toRaw(row.id, address, body, row.dateSeconds, row.messageBox) ?: continue
            when (process(raw)) {
                is ProcessIncomingMessageResult.Saved -> saved++
                ProcessIncomingMessageResult.RejectedNotWhitelisted ->
                    DevLog.log("SMS", "mms backfill rejected addr=$address id=${row.id}")
                ProcessIncomingMessageResult.IgnoredDuplicate -> Unit
            }
        }
        DevLog.log("SMS", "backfill today mms rows=${rows.size} saved=$saved upToId=$upToId")
    }

    private suspend fun syncNewInternal() {
        syncNewSms()
        syncNewMms()
    }

    private suspend fun syncNewSms() {
        val afterId = lastSeenSmsId()
        val rows = querySmsAfter(afterId)
        var maxId = afterId
        for (row in rows) {
            if (SmsRowMapper.isPendingOutbound(row.type)) {
                maxId = row.id
                continue
            }
            val raw = SmsRowMapper.toRaw(row.id, row.address, row.body, row.date, row.type)
            if (raw != null) logProcess("SMS", row.id, row.address, raw, process(raw))
            maxId = row.id
        }
        if (maxId > afterId) {
            prefs.edit().putLong(KEY_LAST_SMS_ID, maxId).apply()
        }
    }

    private suspend fun syncNewMms() {
        val afterId = lastSeenMmsId()
        val rows = mms.queryAfter(afterId)
        var maxId = afterId
        for (row in rows) {
            val address = mms.queryAddress(row.id, row.messageBox)
            val body = mms.queryText(row.id)
            if (address != null && body != null) {
                val raw = MmsRowMapper.toRaw(row.id, address, body, row.dateSeconds, row.messageBox)
                if (raw != null) logProcess("SMS", row.id, address, raw, process(raw), prefix = "mms ")
            }
            maxId = row.id
        }
        if (maxId > afterId) {
            prefs.edit().putLong(KEY_LAST_MMS_ID, maxId).apply()
        }
    }

    private fun logProcess(
        tag: String,
        id: Long,
        address: String,
        raw: RawIncomingMessage,
        result: ProcessIncomingMessageResult,
        prefix: String = "",
    ) {
        when (result) {
            is ProcessIncomingMessageResult.Saved ->
                DevLog.log(tag, "${prefix}saved outgoing=${raw.isOutgoing} id=$id")
            ProcessIncomingMessageResult.RejectedNotWhitelisted ->
                DevLog.log(tag, "${prefix}rejected addr=$address id=$id")
            ProcessIncomingMessageResult.IgnoredDuplicate ->
                DevLog.log(tag, "${prefix}duplicate id=$id")
        }
    }

    private fun todayStartMillis(): Long =
        LocalDate.now()
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

    private fun queryMaxSmsId(): Long? {
        context.contentResolver.query(
            Telephony.Sms.CONTENT_URI,
            arrayOf(Telephony.Sms._ID),
            null,
            null,
            "${Telephony.Sms._ID} DESC",
        )?.use { cursor ->
            return if (cursor.moveToFirst()) cursor.getLong(0) else 0L
        }
        return null
    }

    private fun querySmsSinceDate(sinceMillis: Long): List<SmsRow> {
        val rows = mutableListOf<SmsRow>()
        context.contentResolver.query(
            Telephony.Sms.CONTENT_URI,
            SMS_COLUMNS,
            "${Telephony.Sms.DATE} >= ?",
            arrayOf(sinceMillis.toString()),
            "${Telephony.Sms._ID} ASC",
        )?.use { cursor ->
            rows.addAll(readSmsRows(cursor))
        }
        return rows
    }

    private fun querySmsAfter(afterId: Long): List<SmsRow> {
        val rows = mutableListOf<SmsRow>()
        context.contentResolver.query(
            Telephony.Sms.CONTENT_URI,
            SMS_COLUMNS,
            "${Telephony.Sms._ID} > ?",
            arrayOf(afterId.toString()),
            "${Telephony.Sms._ID} ASC",
        )?.use { cursor ->
            rows.addAll(readSmsRows(cursor))
        }
        return rows
    }

    private fun readSmsRows(cursor: android.database.Cursor): List<SmsRow> {
        val idIdx = cursor.getColumnIndexOrThrow(Telephony.Sms._ID)
        val addressIdx = cursor.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
        val bodyIdx = cursor.getColumnIndexOrThrow(Telephony.Sms.BODY)
        val dateIdx = cursor.getColumnIndexOrThrow(Telephony.Sms.DATE)
        val typeIdx = cursor.getColumnIndexOrThrow(Telephony.Sms.TYPE)
        val rows = mutableListOf<SmsRow>()
        while (cursor.moveToNext()) {
            rows.add(
                SmsRow(
                    id = cursor.getLong(idIdx),
                    address = cursor.getString(addressIdx).orEmpty(),
                    body = cursor.getString(bodyIdx).orEmpty(),
                    date = cursor.getLong(dateIdx),
                    type = cursor.getInt(typeIdx),
                ),
            )
        }
        return rows
    }

    private data class SmsRow(
        val id: Long,
        val address: String,
        val body: String,
        val date: Long,
        val type: Int,
    )

    companion object {
        private const val PREFS = "sms_sync"
        private const val KEY_LAST_SMS_ID = "last_seen_sms_id"
        private const val KEY_LAST_MMS_ID = "last_seen_mms_id"
        private const val KEY_LAST_ID_LEGACY = "last_seen_id"
        private const val KEY_BACKFILL_TODAY_DONE_LEGACY = "backfill_today_done"

        private val SMS_COLUMNS = arrayOf(
            Telephony.Sms._ID,
            Telephony.Sms.ADDRESS,
            Telephony.Sms.BODY,
            Telephony.Sms.DATE,
            Telephony.Sms.TYPE,
        )

        internal fun advanceCheckpoint(
            rowIds: List<Long>,
            pendingIds: Set<Long>,
            afterId: Long,
        ): Long {
            var maxId = afterId
            for (id in rowIds) {
                if (id in pendingIds) {
                    maxId = id
                    continue
                }
                maxId = id
            }
            return maxId
        }
    }
}
