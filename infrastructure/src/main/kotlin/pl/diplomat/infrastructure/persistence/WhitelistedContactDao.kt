package pl.diplomat.infrastructure.persistence

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface WhitelistedContactDao {
    @Query("SELECT * FROM whitelisted_contacts ORDER BY displayName COLLATE NOCASE")
    fun observeAll(): Flow<List<WhitelistedContactEntity>>

    @Query("SELECT * FROM whitelisted_contacts WHERE id = :id")
    suspend fun findById(id: Long): WhitelistedContactEntity?

    @Query("SELECT * FROM whitelisted_contacts WHERE normalizedPhoneNumber = :normalizedPhoneNumber LIMIT 1")
    suspend fun findByNormalizedPhoneNumber(normalizedPhoneNumber: String): WhitelistedContactEntity?

    @Query("SELECT * FROM whitelisted_contacts WHERE phoneMatchKey = :phoneMatchKey LIMIT 1")
    suspend fun findByPhoneMatchKey(phoneMatchKey: String): WhitelistedContactEntity?

    @Query("SELECT * FROM whitelisted_contacts WHERE displayName = :displayName COLLATE NOCASE LIMIT 1")
    suspend fun findByDisplayName(displayName: String): WhitelistedContactEntity?

    @Query("SELECT * FROM whitelisted_contacts")
    suspend fun getAll(): List<WhitelistedContactEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entity: WhitelistedContactEntity): Long

    @Update
    suspend fun update(entity: WhitelistedContactEntity)

    @Query("DELETE FROM whitelisted_contacts WHERE id = :id")
    suspend fun deleteById(id: Long)
}
