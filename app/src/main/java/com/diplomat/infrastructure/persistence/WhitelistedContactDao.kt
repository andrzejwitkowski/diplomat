package com.diplomat.infrastructure.persistence

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface WhitelistedContactDao {
    @Query("SELECT * FROM whitelisted_contacts ORDER BY displayName COLLATE NOCASE")
    fun observeAll(): Flow<List<WhitelistedContactEntity>>

    @Query("SELECT * FROM whitelisted_contacts WHERE id = :id")
    suspend fun getById(id: Long): WhitelistedContactEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: WhitelistedContactEntity): Long

    @Query("DELETE FROM whitelisted_contacts WHERE id = :id")
    suspend fun delete(id: Long)
}
