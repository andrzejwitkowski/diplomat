package pl.diplomat.infrastructure.adapter

import pl.diplomat.domain.model.PhoneNumber
import pl.diplomat.domain.model.WhitelistedContact
import pl.diplomat.domain.normalization.NormalizationService
import pl.diplomat.domain.port.ContactRepositoryPort
import pl.diplomat.infrastructure.persistence.WhitelistedContactDao
import pl.diplomat.infrastructure.persistence.toDomain
import pl.diplomat.infrastructure.persistence.toEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomContactRepositoryAdapter(
    private val dao: WhitelistedContactDao,
    private val normalization: NormalizationService,
) : ContactRepositoryPort {

    override fun observeAll(): Flow<List<WhitelistedContact>> =
        dao.observeAll().map { entities -> entities.map { it.toDomain() } }

    override suspend fun add(displayName: String, phoneNumber: PhoneNumber, avatarUri: String?): Long =
        dao.insert(
            WhitelistedContact(
                id = 0,
                displayName = normalization.normalizeDisplayName(displayName).value,
                phoneNumber = phoneNumber,
                avatarUri = avatarUri,
            ).toEntity(normalization),
        )

    override suspend fun update(contact: WhitelistedContact) {
        dao.update(contact.toEntity(normalization))
    }

    override suspend fun remove(id: Long) {
        dao.deleteById(id)
    }

    override suspend fun findById(id: Long): WhitelistedContact? =
        dao.findById(id)?.toDomain()

    override suspend fun findByPhoneNumber(phoneNumber: PhoneNumber): WhitelistedContact? =
        dao.findByPhoneMatchKey(normalization.normalizePhone(phoneNumber.value).matchKey)?.toDomain()

    override suspend fun findByDisplayName(displayName: String): WhitelistedContact? =
        dao.findByDisplayName(normalization.normalizeDisplayName(displayName).value)?.toDomain()
}
