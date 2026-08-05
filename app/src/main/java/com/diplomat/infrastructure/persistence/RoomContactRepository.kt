package com.diplomat.infrastructure.persistence

import com.diplomat.domain.whitelist.ContactRepositoryPort
import com.diplomat.domain.whitelist.WhitelistedContact
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomContactRepository(
    private val dao: WhitelistedContactDao,
) : ContactRepositoryPort {
    override fun observeAll(): Flow<List<WhitelistedContact>> =
        dao.observeAll().map { rows -> rows.map { it.toDomain() } }

    override suspend fun getById(id: Long): WhitelistedContact? =
        dao.getById(id)?.toDomain()

    override suspend fun upsert(contact: WhitelistedContact): WhitelistedContact {
        val id = dao.upsert(contact.toEntity())
        // REPLACE returns row id; for updates Room may return existing id
        return contact.copy(id = if (contact.id != 0L) contact.id else id)
    }

    override suspend fun delete(id: Long) = dao.delete(id)
}
