package com.diplomat.infrastructure.contacts

import android.content.ContentResolver
import android.net.Uri
import android.provider.ContactsContract

/**
 * Reads display name + phone from a contact [Uri] via Contacts Provider.
 */
class DeviceContactsGateway(private val resolver: ContentResolver) {
    data class PickedContact(val displayName: String, val phoneNumber: String)

    fun read(uri: Uri): PickedContact? {
        val contactId = resolver.query(uri, arrayOf(ContactsContract.Contacts._ID), null, null, null)
            ?.use { c -> if (c.moveToFirst()) c.getLong(0) else return null }
            ?: return null

        val name = resolver.query(
            uri,
            arrayOf(ContactsContract.Contacts.DISPLAY_NAME),
            null,
            null,
            null,
        )?.use { c -> if (c.moveToFirst()) c.getString(0).orEmpty() else "" }.orEmpty()

        val phone = resolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
            "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID}=?",
            arrayOf(contactId.toString()),
            null,
        )?.use { c -> if (c.moveToFirst()) c.getString(0) else null }

        return phone?.let { PickedContact(displayName = name.ifBlank { it }, phoneNumber = it) }
    }
}
