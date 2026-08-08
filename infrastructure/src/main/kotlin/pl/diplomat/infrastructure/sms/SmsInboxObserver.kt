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

/** # ponytail: no historical backfill — seeds lastSeenId to current max on first run. */
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
            seedLastSeenToMax()
        }
        syncNew()
        context.contentResolver.registerContentObserver(Telephony.Sms.CONTENT_URI, true, observer)
        registered = true
        DevLog.log("SMS", "observer started lastId=${lastSeenId()}")
    }

    private fun hasReadSmsPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) ==
            PackageManager.PERMISSION_GRANTED

    private fun lastSeenId(): Long = prefs.getLong(KEY_LAST_ID, 0L)

    private fun seedLastSeenToMax() {
        val maxId = queryMaxId() ?: 0L
        prefs.edit().putLong(KEY_LAST_ID, maxId).apply()
        DevLog.log("SMS", "seeded lastId=$maxId")
    }

    private fun syncNew() {
        if (!hasReadSmsPermission()) return
        scope.launch {
            val afterId = lastSeenId()
            val rows = queryAfter(afterId)
            var maxId = afterId
            var sawPendingOutbound = false
            for (row in rows) {
                if (SmsRowMapper.isPendingOutbound(row.type)) {
                    sawPendingOutbound = true
                    continue
                }
                val raw = SmsRowMapper.toRaw(row.id, row.address, row.body, row.date, row.type)
                if (raw == null) {
                    if (!sawPendingOutbound && row.id > maxId) maxId = row.id
                    continue
                }
                when (val result = process(raw)) {
                    is ProcessIncomingMessageResult.Saved ->
                        DevLog.log("SMS", "saved outgoing=${raw.isOutgoing} id=${row.id}")
                    ProcessIncomingMessageResult.RejectedNotWhitelisted ->
                        DevLog.log("SMS", "rejected id=${row.id}")
                    ProcessIncomingMessageResult.IgnoredDuplicate ->
                        DevLog.log("SMS", "duplicate id=${row.id}")
                }
                if (!sawPendingOutbound && row.id > maxId) maxId = row.id
            }
            if (maxId > afterId) {
                prefs.edit().putLong(KEY_LAST_ID, maxId).apply()
            }
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

    private fun queryAfter(afterId: Long): List<SmsRow> {
        val rows = mutableListOf<SmsRow>()
        context.contentResolver.query(
            Telephony.Sms.CONTENT_URI,
            arrayOf(
                Telephony.Sms._ID,
                Telephony.Sms.ADDRESS,
                Telephony.Sms.BODY,
                Telephony.Sms.DATE,
                Telephony.Sms.TYPE,
            ),
            "${Telephony.Sms._ID} > ?",
            arrayOf(afterId.toString()),
            "${Telephony.Sms._ID} ASC",
        )?.use { cursor ->
            val idIdx = cursor.getColumnIndexOrThrow(Telephony.Sms._ID)
            val addressIdx = cursor.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
            val bodyIdx = cursor.getColumnIndexOrThrow(Telephony.Sms.BODY)
            val dateIdx = cursor.getColumnIndexOrThrow(Telephony.Sms.DATE)
            val typeIdx = cursor.getColumnIndexOrThrow(Telephony.Sms.TYPE)
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
    }
}
