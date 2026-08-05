package com.diplomat.usecase.whitelist

import com.diplomat.domain.whitelist.ContactRepositoryPort
import com.diplomat.domain.whitelist.PhoneNumber
import com.diplomat.domain.whitelist.WhitelistedContact

class AddContactToWhitelistUseCase(
    private val repository: ContactRepositoryPort,
) {
    suspend operator fun invoke(displayName: String, phoneRaw: String): Result<WhitelistedContact> {
        val phone = PhoneNumber.parse(phoneRaw)
            ?: return Result.failure(IllegalArgumentException("Invalid phone number"))
        val name = displayName.trim().ifBlank { phone.value }
        return Result.success(
            repository.upsert(WhitelistedContact(displayName = name, phoneNumber = phone)),
        )
    }
}
