package pl.diplomat.infrastructure.adapter

import android.content.ContentResolver
import android.net.Uri
import android.provider.ContactsContract
import pl.diplomat.domain.model.PhoneNumber
import pl.diplomat.domain.port.DeviceContact
import pl.diplomat.domain.port.SystemContactsPort
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AndroidSystemContactsAdapter(
    private val contentResolver: ContentResolver,
) : SystemContactsPort {

    override suspend fun lookupContact(lookupUri: String): DeviceContact? = withContext(Dispatchers.IO) {
        val uri = Uri.parse(lookupUri)
        contentResolver.query(
            uri,
            arrayOf(
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER,
            ),
            null,
            null,
            null,
        )?.use { cursor ->
            if (!cursor.moveToFirst()) return@withContext null
            val displayName = cursor.getString(0) ?: return@withContext null
            val phoneNumber = cursor.getString(1) ?: return@withContext null
            DeviceContact(displayName = displayName, phoneNumber = PhoneNumber(phoneNumber))
        }
    }
}
