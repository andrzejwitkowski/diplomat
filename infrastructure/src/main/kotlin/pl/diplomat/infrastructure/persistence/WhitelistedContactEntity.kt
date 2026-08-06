package pl.diplomat.infrastructure.persistence

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "whitelisted_contacts",
    indices = [
        Index(value = ["normalizedPhoneNumber"], unique = true),
    ],
)
data class WhitelistedContactEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val displayName: String,
    val phoneNumber: String,
    val normalizedPhoneNumber: String,
    val avatarUri: String? = null,
)
