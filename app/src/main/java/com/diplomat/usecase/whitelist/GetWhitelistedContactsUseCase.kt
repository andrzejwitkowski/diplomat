package com.diplomat.usecase.whitelist

import com.diplomat.domain.whitelist.ContactRepositoryPort
import com.diplomat.domain.whitelist.WhitelistedContact
import kotlinx.coroutines.flow.Flow

class GetWhitelistedContactsUseCase(
    private val repository: ContactRepositoryPort,
) {
    operator fun invoke(): Flow<List<WhitelistedContact>> = repository.observeAll()
}
