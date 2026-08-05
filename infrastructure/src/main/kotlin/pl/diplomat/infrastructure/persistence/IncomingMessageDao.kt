package pl.diplomat.infrastructure.persistence

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface IncomingMessageDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entity: IncomingMessageEntity): Long

    @Query(
        """
        SELECT m.* FROM incoming_messages m
        INNER JOIN (
            SELECT contactId, MAX(timestamp) AS maxTimestamp
            FROM incoming_messages
            GROUP BY contactId
        ) latest ON m.contactId = latest.contactId AND m.timestamp = latest.maxTimestamp
        ORDER BY m.timestamp DESC
        """,
    )
    fun observeLatestPerContact(): Flow<List<IncomingMessageEntity>>

    @Query("SELECT * FROM incoming_messages WHERE contactId = :contactId ORDER BY timestamp DESC")
    suspend fun findByContactId(contactId: Long): List<IncomingMessageEntity>
}
