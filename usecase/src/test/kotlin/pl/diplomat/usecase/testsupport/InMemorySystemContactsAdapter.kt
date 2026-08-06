package pl.diplomat.usecase.testsupport

import pl.diplomat.domain.model.PhoneNumber
import pl.diplomat.domain.normalization.NormalizationService
import pl.diplomat.domain.port.SystemContactsPort

class InMemorySystemContactsAdapter(
    private val normalization: NormalizationService = NormalizationService.default,
) : SystemContactsPort {
    private val phonesByDisplayName = mutableMapOf<String, List<PhoneNumber>>()

    fun register(displayName: String, vararg phones: String) {
        phonesByDisplayName[normalization.normalizeDisplayName(displayName).value] =
            phones.map { PhoneNumber(it) }
    }

    override suspend fun lookupContact(lookupUri: String) = null

    override suspend fun findPhoneNumbersByDisplayName(displayName: String): List<PhoneNumber> =
        phonesByDisplayName[normalization.normalizeDisplayName(displayName).value].orEmpty()
}
