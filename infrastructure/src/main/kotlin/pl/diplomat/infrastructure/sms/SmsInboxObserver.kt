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
    private var registered = false

    private val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) = syncNew()
        override fun onChange(selfChange: Boolean, uri: Uri?) = syncNew()
    }

    fun start() {
        if (registered) return
        if (!hasReadSmsPermission()) return
        if (!prefs.contains(KEY_LAST_ID)) {
            prefs.edit().putLong(KEY_LAST_ID, 0L).apply()
        }
        scope.launch {
            if (!prefs.getBoolean(KEY_BACKFILL_TODAY_DONE, false)) {
                backfillToday()
                prefs.edit().putBoolean(KEY_BACKFILL_TODAY_DONE, true).apply()
                val maxId = queryMaxId() ?: lastSeenId()
                if (maxId > lastSeenId()) {
                    prefs.edit().putLong(KEY_LAST_ID, maxId).apply()
                }
            }
            syncNewInternal()
        }
        context.contentResolver.registerContentObserver(Telephony.Sms.CONTENT_URI, true, observer)
        registered = true
        DevLog.log("SMS", "observer started lastId=${lastSeenId()}")
    }

    private fun hasReadSmsPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) ==
            PackageManager.PERMISSION_GRANTED

    private fun lastSeenId(): Long = prefs.getLong(KEY_LAST_ID, 0L)

    private fun syncNew() {
        if (!hasReadSmsPermission()) return
        scope.launch { syncNewInternal() }
    }

    private suspend fun backfillToday() {
        val todayStart = LocalDate.now()
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
        val rows = querySinceDate(todayStart)
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
        DevLog.log("SMS", "backfill today rows=${rows.size} saved=$saved")
    }

    private suspend fun syncNewInternal() {
        val afterId = lastSeenId()
        val rows = queryAfter(afterId)
        var maxId = afterId
        for (row in rows) {
            if (SmsRowMapper.isPendingOutbound(row.type)) {
                break
            }
            val raw = SmsRowMapper.toRaw(row.id, row.address, row.body, row.date, row.type)
            if (raw != null) {
                when (val result = process(raw)) {
                    is ProcessIncomingMessageResult.Saved ->
                        DevLog.log("SMS", "saved outgoing=${raw.isOutgoing} id=${row.id}")
                    ProcessIncomingMessageResult.RejectedNotWhitelisted ->
                        DevLog.log("SMS", "rejected addr=${row.address} id=${row.id}")
                    ProcessIncomingMessageResult.IgnoredDuplicate ->
                        DevLog.log("SMS", "duplicate id=${row.id}")
                }
            }
            maxId = row.id
        }
        if (maxId > afterId) {
            prefs.edit().putLong(KEY_LAST_ID, maxId).apply()
        }
    }

    private fun queryMaxId(): Long? {
        context.contentResolver.query(
            Telephony.Sms.CONTENT_URI,
            arrayOf(Telephony.Sms._ID),
            null,
            null,
            "${Telephony.Sms._ID} DESC",
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                return cursor.getLong(0)
            }
        }
        return null
    }

    private fun querySinceDate(sinceMillis: Long): List<SmsRow> {
        val rows = mutableListOf<SmsRow>()
        context.contentResolver.query(
            Telephony.Sms.CONTENT_URI,
            SMS_COLUMNS,
            "${Telephony.Sms.DATE} >= ?",
            arrayOf(sinceMillis.toString()),
            "${Telephony.Sms._ID} ASC",
        )?.use { cursor ->
            rows.addAll(readRows(cursor))
        }
        return rows
    }

    private fun queryAfter(afterId: Long): List<SmsRow> {
        val rows = mutableListOf<SmsRow>()
        context.contentResolver.query(
            Telephony.Sms.CONTENT_URI,
            SMS_COLUMNS,
            "${Telephony.Sms._ID} > ?",
            arrayOf(afterId.toString()),
            "${Telephony.Sms._ID} ASC",
        )?.use { cursor ->
            rows.addAll(readRows(cursor))
        }
        return rows
    }

    private fun readRows(cursor: android.database.Cursor): List<SmsRow> {
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
        private const val KEY_LAST_ID = "last_seen_id"
        private const val KEY_BACKFILL_TODAY_DONE = "backfill_today_done"

        private val SMS_COLUMNS = arrayOf(
            Telephony.Sms._ID,
            Telephony.Sms.ADDRESS,
            Telephony.Sms.BODY,
            Telephony.Sms.DATE,
            Telephony.Sms.TYPE,
        )
    }
}
