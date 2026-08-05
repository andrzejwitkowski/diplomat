package com.diplomat.domain.whitelist

data class WhitelistedContact(
    val id: Long = 0L,
    val displayName: String,
    val phoneNumber: PhoneNumber,
)
