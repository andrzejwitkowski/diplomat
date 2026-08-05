package com.diplomat.infrastructure.persistence

import com.diplomat.domain.whitelist.PhoneNumber
import com.diplomat.domain.whitelist.WhitelistedContact

fun WhitelistedContactEntity.toDomain() = WhitelistedContact(
    id = id,
    displayName = displayName,
    phoneNumber = PhoneNumber.ofNormalized(phoneNumber),
)

fun WhitelistedContact.toEntity() = WhitelistedContactEntity(
    id = id,
    displayName = displayName,
    phoneNumber = phoneNumber.value,
)
