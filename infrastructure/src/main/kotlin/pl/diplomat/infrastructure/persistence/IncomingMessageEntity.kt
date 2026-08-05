package pl.diplomat.infrastructure.persistence

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "incoming_messages",
    foreignKeys = [
        ForeignKey(
            entity = WhitelistedContactEntity::class,
            parentColumns = ["id"],
            childColumns = ["contactId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("contactId"), Index("timestamp")],
)
data class IncomingMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val contactId: Long,
    val text: String,
    val timestamp: Long,
    val sourceApp: String,
    val status: String,
)
