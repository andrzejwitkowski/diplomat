package pl.diplomat.domain.port

import pl.diplomat.domain.model.PhoneNumber

data class DeviceContact(
    val displayName: String,
    val phoneNumber: PhoneNumber,
)

interface SystemContactsPort {
    suspend fun lookupContact(lookupUri: String): DeviceContact?
}
