package pl.diplomat.infrastructure.persistence

import pl.diplomat.domain.model.PhoneNumber
import pl.diplomat.domain.model.WhitelistedContact

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
        displayName = displayName.trim(),
        phoneNumber = phoneNumber.value,
        normalizedPhoneNumber = phoneNumber.normalized(),
        avatarUri = avatarUri,
    )
