package pl.diplomat.domain.port

import pl.diplomat.domain.model.PhoneNumber

data class DeviceContact(
    val displayName: String,
    val phoneNumber: PhoneNumber,
    val avatarUri: String? = null,
)

interface SystemContactsPort {
    suspend fun lookupContact(lookupUri: String): DeviceContact?

    suspend fun findPhoneNumbersByDisplayName(displayName: String): List<PhoneNumber>
}
