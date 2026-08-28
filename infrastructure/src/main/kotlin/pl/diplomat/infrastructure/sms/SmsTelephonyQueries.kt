package pl.diplomat.infrastructure.sms

import android.content.ContentResolver
import android.provider.Telephony

internal class SmsTelephonyQueries(
    private val resolver: ContentResolver,
) {
    fun queryMaxId(): Long? {
        resolver.query(
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

    fun querySince(sinceMillis: Long): List<SmsRow> = query(
        selection = "${Telephony.Sms.DATE} >= ?",
        args = arrayOf(sinceMillis.toString()),
    )

    fun queryAfter(afterId: Long): List<SmsRow> = query(
        selection = "${Telephony.Sms._ID} > ?",
        args = arrayOf(afterId.toString()),
    )

    private fun query(selection: String, args: Array<String>): List<SmsRow> {
        val rows = mutableListOf<SmsRow>()
        resolver.query(
            Telephony.Sms.CONTENT_URI,
            SMS_COLUMNS,
            selection,
            args,
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

    companion object {
        private val SMS_COLUMNS = arrayOf(
            Telephony.Sms._ID,
            Telephony.Sms.ADDRESS,
            Telephony.Sms.BODY,
            Telephony.Sms.DATE,
            Telephony.Sms.TYPE,
        )
    }
}

internal data class SmsRow(
    val id: Long,
    val address: String,
    val body: String,
    val date: Long,
    val type: Int,
)
