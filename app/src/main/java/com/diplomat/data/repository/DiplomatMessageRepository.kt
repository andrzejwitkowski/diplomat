package com.diplomat.data.repository

import com.diplomat.data.local.MessageDao
import com.diplomat.data.local.MessageEntity
import com.diplomat.data.local.toDomain
import com.diplomat.data.remote.DiplomatApi
import com.diplomat.data.remote.dto.AnalysisRequest
import com.diplomat.domain.model.ConversationThread
import com.diplomat.domain.model.InterceptedMessage
import com.diplomat.domain.model.MessageSource
import com.diplomat.domain.model.MessageStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Default [MessageRepository] backed by Room (local history) and the Ktor
 * [DiplomatApi] (tone analysis + draft generation).
 */
class DiplomatMessageRepository(
    private val dao: MessageDao,
    private val api: DiplomatApi,
) : MessageRepository {

    override fun observeThreads(): Flow<List<ConversationThread>> =
        dao.observeAll().map { entities ->
            entities
                .map(MessageEntity::toDomain)
                .groupBy(InterceptedMessage::sender)
                .map { (sender, messages) ->
                    val latest = messages.maxBy(InterceptedMessage::timestamp)
                    ConversationThread(
                        sender = sender,
                        source = MessageSource.fromPackage(latest.packageName),
                        latestMessage = latest,
                        messageCount = messages.size,
                    )
                }
                .sortedByDescending { it.latestMessage.timestamp }
        }

    override fun observeMessage(id: Long): Flow<InterceptedMessage?> =
        dao.observeById(id).map { it?.toDomain() }

    override suspend fun recordIncoming(
        sender: String,
        body: String,
        packageName: String,
        timestamp: Long,
    ): Long = dao.insert(
        MessageEntity(
            sender = sender,
            body = body,
            packageName = packageName,
            timestamp = timestamp,
            status = MessageStatus.PENDING_DECISION,
        ),
    )

    override suspend fun requestDraft(
        id: Long,
        userAgreement: Boolean,
        userReasoning: String,
    ): Result<InterceptedMessage> {
        val entity = dao.getById(id)
            ?: return Result.failure(IllegalArgumentException("Message $id not found"))

        dao.update(
            entity.copy(
                status = MessageStatus.DRAFTING,
                userAgreement = userAgreement,
                userReasoning = userReasoning,
            ),
        )

        return runCatching {
            val response = api.analyze(
                AnalysisRequest(
                    incomingMessage = entity.body,
                    userAgreement = userAgreement,
                    userReasoning = userReasoning,
                ),
            )
            val updated = entity.copy(
                status = MessageStatus.DRAFTED,
                userAgreement = userAgreement,
                userReasoning = userReasoning,
                toneAnalysis = response.toneAnalysis,
                requiresResponse = response.requiresResponse,
                draftResponse = response.draftResponse,
            )
            dao.update(updated)
            updated.toDomain()
        }.onFailure {
            dao.update(entity.copy(status = MessageStatus.ERROR))
        }
    }

    override suspend fun updateDraft(id: Long, draft: String) {
        dao.getById(id)?.let { dao.update(it.copy(draftResponse = draft)) }
    }

    override suspend fun markSent(id: Long) {
        dao.getById(id)?.let { dao.update(it.copy(status = MessageStatus.SENT)) }
    }

    override suspend fun markIgnored(id: Long) {
        dao.getById(id)?.let { dao.update(it.copy(status = MessageStatus.IGNORED_ACK)) }
    }
}
