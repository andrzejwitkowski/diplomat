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
        val contactId = resolveContactId(uri) ?: return@withContext null
        val displayName = resolveDisplayName(contactId) ?: return@withContext null
        val phoneNumber = resolvePhoneNumber(contactId) ?: return@withContext null
        DeviceContact(displayName = displayName, phoneNumber = PhoneNumber(phoneNumber))
    }

    private fun resolveContactId(uri: Uri): String? {
        contentResolver.query(uri, arrayOf(ContactsContract.Contacts._ID), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                return cursor.getString(0)
            }
        }
        return null
    }

    private fun resolveDisplayName(contactId: String): String? {
        val uri = ContactsContract.Contacts.CONTENT_URI
        contentResolver.query(
            uri,
            arrayOf(ContactsContract.Contacts.DISPLAY_NAME),
            "${ContactsContract.Contacts._ID} = ?",
            arrayOf(contactId),
            null,
        )?.use { cursor ->
            if (cursor.moveToFirst()) return cursor.getString(0)
        }
        return null
    }

    private fun resolvePhoneNumber(contactId: String): String? {
        val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
        contentResolver.query(
            uri,
            arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
            "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
            arrayOf(contactId),
            null,
        )?.use { cursor ->
            if (cursor.moveToFirst()) return cursor.getString(0)
        }
        return null
    }
}
