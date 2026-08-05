package com.diplomat.data.local

import androidx.room.TypeConverter
import com.diplomat.domain.model.MessageStatus

/**
 * Persists enums as their stable names.
 */
class Converters {
    @TypeConverter
    fun fromStatus(status: MessageStatus): String = status.name

    @TypeConverter
    fun toStatus(value: String): MessageStatus = MessageStatus.valueOf(value)
}
