package pl.diplomat.infrastructure.persistence

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
abstract class IncomingMessageDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insert(entity: IncomingMessageEntity): Long

    @Query(
        """
        SELECT COUNT(*) > 0 FROM incoming_messages
        WHERE notificationKey = :notificationKey
          AND text = :text
          AND contentType = :contentType
          AND mediaKind = :mediaKind
          AND timestamp BETWEEN :sinceTimestamp AND :untilTimestamp
        """,
    )
    protected abstract suspend fun hasRecentDuplicateByNotificationKey(
        notificationKey: String,
        text: String,
        contentType: String,
        mediaKind: String,
        sinceTimestamp: Long,
        untilTimestamp: Long,
    ): Boolean

    @Query(
        """
        SELECT COUNT(*) > 0 FROM incoming_messages
        WHERE contactId = :contactId
          AND notificationKey IS NULL
          AND text = :text
          AND contentType = :contentType
          AND mediaKind = :mediaKind
          AND timestamp BETWEEN :sinceTimestamp AND :untilTimestamp
        """,
    )
    protected abstract suspend fun hasRecentDuplicateByContact(
        contactId: Long,
        text: String,
        contentType: String,
        mediaKind: String,
        sinceTimestamp: Long,
        untilTimestamp: Long,
    ): Boolean

    @Transaction
    open suspend fun insertIgnoringRecentDuplicate(
        entity: IncomingMessageEntity,
        windowMs: Long,
    ): Long {
        val sinceTimestamp = entity.timestamp - windowMs
        val untilTimestamp = entity.timestamp + windowMs
        val isDuplicate = when (val notificationKey = entity.notificationKey) {
            null -> hasRecentDuplicateByContact(
                contactId = entity.contactId,
                text = entity.text,
                contentType = entity.contentType,
                mediaKind = entity.mediaKind,
                sinceTimestamp = sinceTimestamp,
                untilTimestamp = untilTimestamp,
            )
            else -> hasRecentDuplicateByNotificationKey(
                notificationKey = notificationKey,
                text = entity.text,
                contentType = entity.contentType,
                mediaKind = entity.mediaKind,
                sinceTimestamp = sinceTimestamp,
                untilTimestamp = untilTimestamp,
            )
        }
        if (isDuplicate) return DUPLICATE_ID
        return insert(entity)
    }

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
    abstract fun observeLatestPerContact(): Flow<List<IncomingMessageEntity>>

    @Query("SELECT * FROM incoming_messages WHERE contactId = :contactId ORDER BY timestamp DESC, id DESC")
    abstract suspend fun findByContactId(contactId: Long): List<IncomingMessageEntity>

    @Query("SELECT * FROM incoming_messages WHERE contactId = :contactId ORDER BY timestamp DESC, id DESC")
    abstract fun observeByContactId(contactId: Long): Flow<List<IncomingMessageEntity>>

    @Query(
        """
        SELECT contactId, COUNT(*) AS unreadCount
        FROM incoming_messages
        WHERE isRead = 0
        GROUP BY contactId
        """,
    )
    abstract fun observeUnreadCountsByContact(): Flow<List<ContactUnreadCountEntity>>

    @Query("UPDATE incoming_messages SET isRead = 1 WHERE contactId = :contactId AND isRead = 0")
    abstract suspend fun markAllAsReadForContact(contactId: Long)

    companion object {
        const val DUPLICATE_ID = -1L
    }
}
