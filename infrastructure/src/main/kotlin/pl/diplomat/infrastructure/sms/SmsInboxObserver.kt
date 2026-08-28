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
    private val sms = SmsTelephonyQueries(context.contentResolver)
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

            val snapshotSmsMaxId = sms.queryMaxId()
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
        val rows = sms.querySince(todayStartMillis()).filter { it.id <= upToId }
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
        val rows = sms.queryAfter(afterId)
        val pendingIds = mutableSetOf<Long>()
        for (row in rows) {
            if (SmsRowMapper.isPendingOutbound(row.type)) {
                pendingIds.add(row.id)
                continue
            }
            val raw = SmsRowMapper.toRaw(row.id, row.address, row.body, row.date, row.type)
            if (raw != null) logProcess("SMS", row.id, row.address, raw, process(raw))
        }
        val maxId = advanceCheckpoint(rows.map { it.id }, pendingIds, afterId)
        if (maxId > afterId) {
            prefs.edit().putLong(KEY_LAST_SMS_ID, maxId).apply()
        }
    }

    private suspend fun syncNewMms() {
        val afterId = lastSeenMmsId()
        val rows = mms.queryAfter(afterId)
        val pendingIds = mutableSetOf<Long>()
        for (row in rows) {
            if (MmsRowMapper.isPendingOutbound(row.messageBox)) {
                pendingIds.add(row.id)
                continue
            }
            val address = mms.queryAddress(row.id, row.messageBox)
            val body = mms.queryText(row.id)
            if (address != null && body != null) {
                val raw = MmsRowMapper.toRaw(row.id, address, body, row.dateSeconds, row.messageBox)
                if (raw != null) logProcess("SMS", row.id, address, raw, process(raw), prefix = "mms ")
            }
        }
        val maxId = advanceCheckpoint(rows.map { it.id }, pendingIds, afterId)
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

    companion object {
        private const val PREFS = "sms_sync"
        private const val KEY_LAST_SMS_ID = "last_seen_sms_id"
        private const val KEY_LAST_MMS_ID = "last_seen_mms_id"
        private const val KEY_LAST_ID_LEGACY = "last_seen_id"
        private const val KEY_BACKFILL_TODAY_DONE_LEGACY = "backfill_today_done"

        internal fun advanceCheckpoint(
            rowIds: List<Long>,
            pendingIds: Set<Long>,
            afterId: Long,
        ): Long {
            var maxId = afterId
            for (id in rowIds) {
                // Hold before the first pending outbox/queued id so the same `_id` is
                // re-queried when it becomes sent. Later non-pending rows are still
                // processed by the caller; they will be deduped on the next pass.
                if (id in pendingIds) return maxId
                maxId = id
            }
            return maxId
        }
    }
}
