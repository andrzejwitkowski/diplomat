package pl.diplomat.domain.port

import pl.diplomat.domain.model.PhoneNumber
import pl.diplomat.domain.model.WhitelistedContact
import kotlinx.coroutines.flow.Flow

interface ContactRepositoryPort {
    fun observeAll(): Flow<List<WhitelistedContact>>
    suspend fun add(displayName: String, phoneNumber: PhoneNumber, avatarUri: String? = null): Long
    suspend fun update(contact: WhitelistedContact)
    suspend fun remove(id: Long)
    suspend fun findById(id: Long): WhitelistedContact?
    suspend fun findByPhoneNumber(phoneNumber: PhoneNumber): WhitelistedContact?
    suspend fun findByDisplayName(displayName: String): WhitelistedContact?
}
