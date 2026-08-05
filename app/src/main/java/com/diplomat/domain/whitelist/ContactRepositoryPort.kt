package com.diplomat.domain.whitelist

import kotlinx.coroutines.flow.Flow

/** Outbound port: persist and observe whitelisted contacts. */
interface ContactRepositoryPort {
    fun observeAll(): Flow<List<WhitelistedContact>>
    suspend fun getById(id: Long): WhitelistedContact?
    suspend fun upsert(contact: WhitelistedContact): WhitelistedContact
    suspend fun delete(id: Long)
}
