package pl.diplomat.usecase

import pl.diplomat.domain.model.PhoneNumber
import pl.diplomat.domain.model.WhitelistedContact
import pl.diplomat.domain.model.normalizeDisplayName
import pl.diplomat.domain.port.ContactRepositoryPort
import kotlinx.coroutines.flow.Flow

class GetWhitelistedContactsUseCase(
    private val repository: ContactRepositoryPort,
) {
    operator fun invoke(): Flow<List<WhitelistedContact>> = repository.observeAll()
}

class AddContactToWhitelistUseCase(
    private val repository: ContactRepositoryPort,
) {
    suspend operator fun invoke(
        displayName: String,
        phoneNumber: PhoneNumber,
        avatarUri: String? = null,
    ): Long {
        val trimmedDisplayName = displayName.normalizeDisplayName()
        require(trimmedDisplayName.isNotBlank()) { "Display name cannot be blank" }

        val existing = repository.findByPhoneNumber(phoneNumber)
        if (existing != null) {
            repository.update(
                existing.copy(
                    displayName = trimmedDisplayName,
                    avatarUri = avatarUri ?: existing.avatarUri,
                ),
            )
            return existing.id
        }

        return repository.add(trimmedDisplayName, phoneNumber, avatarUri)
    }
}

class UpdateWhitelistedContactUseCase(
    private val repository: ContactRepositoryPort,
) {
    suspend operator fun invoke(contact: WhitelistedContact) = repository.update(contact)
}

class RemoveContactFromWhitelistUseCase(
    private val repository: ContactRepositoryPort,
) {
    suspend operator fun invoke(id: Long) = repository.remove(id)
}
