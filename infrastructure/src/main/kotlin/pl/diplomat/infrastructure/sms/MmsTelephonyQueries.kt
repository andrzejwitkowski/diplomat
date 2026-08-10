package pl.diplomat.infrastructure.sms

import android.content.ContentResolver
import android.net.Uri
import android.provider.Telephony

internal class MmsTelephonyQueries(
    private val resolver: ContentResolver,
) {
    fun queryMaxId(): Long? {
        resolver.query(
            Telephony.Mms.CONTENT_URI,
            arrayOf(Telephony.Mms._ID),
            null,
            null,
            "${Telephony.Mms._ID} DESC",
        )?.use { cursor ->
            return if (cursor.moveToFirst()) cursor.getLong(0) else 0L
        }
        return null
    }

    fun querySince(sinceSeconds: Long): List<MmsRow> = query(
        selection = "${Telephony.Mms.DATE} >= ?",
        args = arrayOf(sinceSeconds.toString()),
    )

    fun queryAfter(afterId: Long): List<MmsRow> = query(
        selection = "${Telephony.Mms._ID} > ?",
        args = arrayOf(afterId.toString()),
    )

    fun queryAddress(mmsId: Long, messageBox: Int): String? {
        resolver.query(
            Uri.parse("content://mms/$mmsId/addr"),
            arrayOf("address", "type"),
            null,
            null,
            null,
        )?.use { cursor ->
            val addressIdx = cursor.getColumnIndex("address")
            val typeIdx = cursor.getColumnIndex("type")
            if (addressIdx < 0 || typeIdx < 0) return null
            val wantedType = if (messageBox == Telephony.Mms.MESSAGE_BOX_INBOX) {
                ADDR_TYPE_FROM
            } else {
                ADDR_TYPE_TO
            }
            var fallback: String? = null
            while (cursor.moveToNext()) {
                val address = cursor.getString(addressIdx)?.trim().orEmpty()
                if (address.isEmpty() || address.equals("insert-address-token", ignoreCase = true)) continue
                val type = cursor.getInt(typeIdx)
                if (type == wantedType) return address
                if (fallback == null) fallback = address
            }
            return fallback
        }
        return null
    }

    fun queryText(mmsId: Long): String? {
        resolver.query(
            Uri.parse("content://mms/$mmsId/part"),
            arrayOf("ct", "text"),
            null,
            null,
            null,
        )?.use { cursor ->
            val ctIdx = cursor.getColumnIndex("ct")
            val textIdx = cursor.getColumnIndex("text")
            if (ctIdx < 0 || textIdx < 0) return null
            val chunks = mutableListOf<String>()
            while (cursor.moveToNext()) {
                val contentType = cursor.getString(ctIdx).orEmpty()
                if (!contentType.startsWith("text/", ignoreCase = true)) continue
                cursor.getString(textIdx)?.trim()?.takeIf { it.isNotEmpty() }?.let { chunks.add(it) }
            }
            return chunks.joinToString("\n").takeIf { it.isNotBlank() }
        }
        return null
    }

    private fun query(selection: String, args: Array<String>): List<MmsRow> {
        val rows = mutableListOf<MmsRow>()
        resolver.query(
            Telephony.Mms.CONTENT_URI,
            COLUMNS,
            selection,
            args,
            "${Telephony.Mms._ID} ASC",
        )?.use { cursor ->
            val idIdx = cursor.getColumnIndexOrThrow(Telephony.Mms._ID)
            val dateIdx = cursor.getColumnIndexOrThrow(Telephony.Mms.DATE)
            val boxIdx = cursor.getColumnIndexOrThrow(Telephony.Mms.MESSAGE_BOX)
            while (cursor.moveToNext()) {
                rows.add(
                    MmsRow(
                        id = cursor.getLong(idIdx),
                        dateSeconds = cursor.getLong(dateIdx),
                        messageBox = cursor.getInt(boxIdx),
                    ),
                )
            }
        }
        return rows
    }

    data class MmsRow(
        val id: Long,
        val dateSeconds: Long,
        val messageBox: Int,
    )

    companion object {
        private const val ADDR_TYPE_FROM = 137
        private const val ADDR_TYPE_TO = 151
        private val COLUMNS = arrayOf(
            Telephony.Mms._ID,
            Telephony.Mms.DATE,
            Telephony.Mms.MESSAGE_BOX,
        )
    }
}
