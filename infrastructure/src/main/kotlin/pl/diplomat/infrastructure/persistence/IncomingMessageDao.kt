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

    @Query("SELECT EXISTS(SELECT 1 FROM incoming_messages WHERE notificationKey = :notificationKey)")
    suspend fun existsByNotificationKey(notificationKey: String): Boolean

    @Query(
        """
        SELECT m.*
        FROM incoming_messages m
        WHERE m.id = (
            SELECT latest.id
            FROM incoming_messages latest
            WHERE latest.contactId = m.contactId
            ORDER BY latest.timestamp DESC, latest.id DESC
            LIMIT 1
        )
        ORDER BY m.timestamp DESC, m.id DESC
        """,
    )
    fun observeLatestPerContact(): Flow<List<IncomingMessageEntity>>

    @Query("SELECT * FROM incoming_messages WHERE contactId = :contactId ORDER BY timestamp DESC, id DESC")
    suspend fun findByContactId(contactId: Long): List<IncomingMessageEntity>
}
