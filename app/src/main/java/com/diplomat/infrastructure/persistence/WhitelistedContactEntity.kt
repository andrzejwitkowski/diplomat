package com.diplomat.infrastructure.persistence

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "whitelisted_contacts",
    indices = [Index(value = ["phoneNumber"], unique = true)],
)
data class WhitelistedContactEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val displayName: String,
    val phoneNumber: String,
)
