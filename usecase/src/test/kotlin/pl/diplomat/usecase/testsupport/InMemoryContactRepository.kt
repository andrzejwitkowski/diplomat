package pl.diplomat.usecase.testsupport

import pl.diplomat.domain.model.PhoneNumber
import pl.diplomat.domain.model.WhitelistedContact
import pl.diplomat.domain.port.ContactRepositoryPort
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class InMemoryContactRepository : ContactRepositoryPort {
    private val contacts = MutableStateFlow<List<WhitelistedContact>>(emptyList())
    private var nextId = 1L

    override fun observeAll(): Flow<List<WhitelistedContact>> = contacts.asStateFlow()

    override suspend fun add(displayName: String, phoneNumber: PhoneNumber, avatarUri: String?): Long {
        val id = nextId++
        contacts.update { current ->
            current + WhitelistedContact(id, displayName.trim(), phoneNumber, avatarUri)
        }
        return id
    }

    override suspend fun update(contact: WhitelistedContact) {
        contacts.update { current ->
            current.map { if (it.id == contact.id) contact.copy(displayName = contact.displayName.trim()) else it }
        }
    }

    override suspend fun remove(id: Long) {
        contacts.update { current -> current.filterNot { it.id == id } }
    }

    override suspend fun findById(id: Long): WhitelistedContact? =
        contacts.value.firstOrNull { it.id == id }

    override suspend fun findByPhoneNumber(phoneNumber: PhoneNumber): WhitelistedContact? {
        val target = phoneNumber.normalized()
        return contacts.value.firstOrNull { it.phoneNumber.normalized() == target }
    }

    override suspend fun findByDisplayName(displayName: String): WhitelistedContact? =
        contacts.value.firstOrNull { it.displayName.equals(displayName.trim(), ignoreCase = true) }
}
