package com.diplomat.usecase.whitelist

import com.diplomat.domain.whitelist.ContactRepositoryPort

class RemoveContactFromWhitelistUseCase(
    private val repository: ContactRepositoryPort,
) {
    suspend operator fun invoke(id: Long) = repository.delete(id)
}
