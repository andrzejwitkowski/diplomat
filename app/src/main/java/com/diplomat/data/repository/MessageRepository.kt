package com.diplomat.data.repository

import com.diplomat.domain.model.ConversationThread
import com.diplomat.domain.model.InterceptedMessage
import kotlinx.coroutines.flow.Flow

/**
 * Single source of truth for captured messages and their Grey Rock lifecycle.
 */
interface MessageRepository {

    /** Sender-correlated conversation list for the dashboard. */
    fun observeThreads(): Flow<List<ConversationThread>>

    /** Observe a single message (decision screen). */
    fun observeMessage(id: Long): Flow<InterceptedMessage?>

    /** Persist a freshly captured message; returns its new id. */
    suspend fun recordIncoming(
        sender: String,
        body: String,
        packageName: String,
        timestamp: Long,
    ): Long

    /**
     * Ask the backend to analyze tone and draft a toned-down reply, storing
     * the result against the message.
     */
    suspend fun requestDraft(
        id: Long,
        userAgreement: Boolean,
        userReasoning: String,
    ): Result<InterceptedMessage>

    /** Persist a manually edited draft. */
    suspend fun updateDraft(id: Long, draft: String)

    /** Mark a message as sent after the user approves it. */
    suspend fun markSent(id: Long)

    /** Mark a message as ignored/acknowledged (Grey Rock: no reply). */
    suspend fun markIgnored(id: Long)
}
