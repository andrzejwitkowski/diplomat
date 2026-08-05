package pl.diplomat.infrastructure.persistence

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "whitelisted_contacts")
data class WhitelistedContactEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val displayName: String,
    val phoneNumber: String,
)
