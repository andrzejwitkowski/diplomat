package pl.diplomat.infrastructure.persistence

import pl.diplomat.domain.model.PhoneNumber
import pl.diplomat.domain.model.WhitelistedContact
import pl.diplomat.domain.normalization.NormalizationService

internal fun WhitelistedContactEntity.toDomain(): WhitelistedContact =
    WhitelistedContact(
        id = id,
        displayName = displayName,
        phoneNumber = PhoneNumber(phoneNumber),
        avatarUri = avatarUri,
    )

internal fun WhitelistedContact.toEntity(normalization: NormalizationService): WhitelistedContactEntity =
    WhitelistedContactEntity(
        id = id,
        displayName = normalization.normalizeDisplayName(displayName).value,
        phoneNumber = phoneNumber.value,
        normalizedPhoneNumber = normalization.normalizePhone(phoneNumber.value).normalized,
        phoneMatchKey = normalization.normalizePhone(phoneNumber.value).matchKey,
        avatarUri = avatarUri,
    )
