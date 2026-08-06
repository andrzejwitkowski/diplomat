package pl.diplomat.infrastructure.adapter

import pl.diplomat.domain.model.PhoneNumber
import pl.diplomat.domain.model.WhitelistedContact
import pl.diplomat.domain.model.matchKey
import pl.diplomat.domain.model.normalizeDisplayName
import pl.diplomat.domain.port.ContactRepositoryPort
import pl.diplomat.infrastructure.persistence.WhitelistedContactDao
import pl.diplomat.infrastructure.persistence.toDomain
import pl.diplomat.infrastructure.persistence.toEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomContactRepositoryAdapter(
    private val dao: WhitelistedContactDao,
) : ContactRepositoryPort {

    override fun observeAll(): Flow<List<WhitelistedContact>> =
        dao.observeAll().map { entities -> entities.map { it.toDomain() } }

    override suspend fun add(displayName: String, phoneNumber: PhoneNumber, avatarUri: String?): Long =
        dao.insert(
            WhitelistedContact(
                id = 0,
                displayName = displayName.normalizeDisplayName(),
                phoneNumber = phoneNumber,
                avatarUri = avatarUri,
            ).toEntity(),
        )

    override suspend fun update(contact: WhitelistedContact) {
        dao.update(contact.toEntity())
    }

    override suspend fun remove(id: Long) {
        dao.deleteById(id)
    }

    override suspend fun findById(id: Long): WhitelistedContact? =
        dao.findById(id)?.toDomain()

    override suspend fun findByPhoneNumber(phoneNumber: PhoneNumber): WhitelistedContact? =
        dao.findByPhoneMatchKey(phoneNumber.matchKey())?.toDomain()

    override suspend fun findByDisplayName(displayName: String): WhitelistedContact? =
        dao.findByDisplayName(displayName.normalizeDisplayName())?.toDomain()
}
