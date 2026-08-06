package pl.diplomat.infrastructure.adapter

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.ContactsContract
import pl.diplomat.domain.model.PhoneNumber
import pl.diplomat.domain.model.normalizeDisplayName
import pl.diplomat.domain.port.AvatarStoragePort
import pl.diplomat.domain.port.DeviceContact
import pl.diplomat.domain.port.SystemContactsPort
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

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
                ContactsContract.CommonDataKinds.Phone.PHOTO_URI,
                ContactsContract.CommonDataKinds.Phone.PHOTO_THUMBNAIL_URI,
            ),
            null,
            null,
            null,
        )?.use { cursor ->
            if (!cursor.moveToFirst()) return@withContext null
            val displayName = cursor.getString(0) ?: return@withContext null
            val phoneNumber = cursor.getString(1) ?: return@withContext null
            val photoUri = cursor.getString(2) ?: cursor.getString(3)
            DeviceContact(
                displayName = displayName.normalizeDisplayName(),
                phoneNumber = PhoneNumber(phoneNumber),
                avatarUri = photoUri,
            )
        }
    }

    override suspend fun findPhoneNumbersByDisplayName(displayName: String): List<PhoneNumber> =
        withContext(Dispatchers.IO) {
            val normalizedName = displayName.normalizeDisplayName()
            if (normalizedName.isBlank()) return@withContext emptyList()

            contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
                "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} = ? COLLATE NOCASE",
                arrayOf(normalizedName),
                null,
            )?.use { cursor ->
                buildList {
                    while (cursor.moveToNext()) {
                        val number = cursor.getString(0) ?: continue
                        runCatching { add(PhoneNumber(number)) }
                    }
                }
            } ?: emptyList()
        }
}

class LocalAvatarStorageAdapter(
    private val context: Context,
) : AvatarStoragePort {

    override suspend fun saveFromUri(sourceUri: String): String = withContext(Dispatchers.IO) {
        val source = Uri.parse(sourceUri)
        val avatarsDir = File(context.filesDir, "avatars").apply { mkdirs() }
        val destination = File(avatarsDir, "${UUID.randomUUID()}.jpg")
        contentResolver.openInputStream(source)?.use { input ->
            destination.outputStream().use { output -> input.copyTo(output) }
        } ?: error("Could not read image from $sourceUri")
        Uri.fromFile(destination).toString()
    }

    private val contentResolver: ContentResolver
        get() = context.contentResolver
}
