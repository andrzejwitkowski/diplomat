package pl.diplomat.infrastructure.persistence

import pl.diplomat.domain.model.PhoneNumber
import pl.diplomat.domain.model.WhitelistedContact

internal fun WhitelistedContactEntity.toDomain(): WhitelistedContact =
    WhitelistedContact(
        id = id,
        displayName = displayName,
        phoneNumber = PhoneNumber(phoneNumber),
    )

internal fun WhitelistedContact.toEntity(): WhitelistedContactEntity =
    WhitelistedContactEntity(
        id = id,
        displayName = displayName,
        phoneNumber = phoneNumber.value,
    )
