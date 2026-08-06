package pl.diplomat.infrastructure.persistence

import pl.diplomat.domain.model.PhoneNumber
import pl.diplomat.domain.model.WhitelistedContact
import pl.diplomat.domain.model.matchKey
import pl.diplomat.domain.model.normalizeDisplayName

internal fun WhitelistedContactEntity.toDomain(): WhitelistedContact =
    WhitelistedContact(
        id = id,
        displayName = displayName,
        phoneNumber = PhoneNumber(phoneNumber),
        avatarUri = avatarUri,
    )

internal fun WhitelistedContact.toEntity(): WhitelistedContactEntity =
    WhitelistedContactEntity(
        id = id,
        displayName = displayName.normalizeDisplayName(),
        phoneNumber = phoneNumber.value,
        normalizedPhoneNumber = phoneNumber.normalized(),
        phoneMatchKey = phoneNumber.matchKey(),
        avatarUri = avatarUri,
    )
