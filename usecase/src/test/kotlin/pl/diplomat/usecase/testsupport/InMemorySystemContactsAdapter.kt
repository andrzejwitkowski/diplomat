package pl.diplomat.usecase.testsupport

import pl.diplomat.domain.model.PhoneNumber
import pl.diplomat.domain.model.normalizeDisplayName
import pl.diplomat.domain.port.SystemContactsPort

class InMemorySystemContactsAdapter : SystemContactsPort {
    private val phonesByDisplayName = mutableMapOf<String, List<PhoneNumber>>()

    fun register(displayName: String, vararg phones: String) {
        phonesByDisplayName[displayName.normalizeDisplayName()] = phones.map { PhoneNumber(it) }
    }

    override suspend fun lookupContact(lookupUri: String) = null

    override suspend fun findPhoneNumbersByDisplayName(displayName: String): List<PhoneNumber> =
        phonesByDisplayName[displayName.normalizeDisplayName()].orEmpty()
}
