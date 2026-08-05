package com.diplomat.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.diplomat.domain.model.MessageStatus

/**
 * Room row for a captured message.
 */
@Entity(
    tableName = "messages",
    indices = [Index("sender"), Index("timestamp")],
)
data class MessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sender: String,
    val body: String,
    @ColumnInfo(name = "package_name") val packageName: String,
    val timestamp: Long,
    val status: MessageStatus,
    @ColumnInfo(name = "tone_analysis") val toneAnalysis: String? = null,
    @ColumnInfo(name = "requires_response") val requiresResponse: Boolean? = null,
    @ColumnInfo(name = "draft_response") val draftResponse: String? = null,
    @ColumnInfo(name = "user_agreement") val userAgreement: Boolean? = null,
    @ColumnInfo(name = "user_reasoning") val userReasoning: String? = null,
)
