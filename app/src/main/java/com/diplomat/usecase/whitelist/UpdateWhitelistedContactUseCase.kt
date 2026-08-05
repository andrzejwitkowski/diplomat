package com.diplomat.usecase.whitelist

import com.diplomat.domain.whitelist.ContactRepositoryPort
import com.diplomat.domain.whitelist.PhoneNumber
import com.diplomat.domain.whitelist.WhitelistedContact

class UpdateWhitelistedContactUseCase(
    private val repository: ContactRepositoryPort,
) {
    suspend operator fun invoke(
        id: Long,
        displayName: String,
        phoneRaw: String,
    ): Result<WhitelistedContact> {
        if (repository.getById(id) == null) {
            return Result.failure(NoSuchElementException("Contact $id not found"))
        }
        val phone = PhoneNumber.parse(phoneRaw)
            ?: return Result.failure(IllegalArgumentException("Invalid phone number"))
        val name = displayName.trim().ifBlank { phone.value }
        return Result.success(
            repository.upsert(WhitelistedContact(id = id, displayName = name, phoneNumber = phone)),
        )
    }
}
