package com.diplomat.data.repository

import com.diplomat.data.local.MessageDao
import com.diplomat.data.local.MessageEntity
import com.diplomat.data.remote.DiplomatApi
import com.diplomat.data.remote.dto.AnalysisRequest
import com.diplomat.data.remote.dto.AnalysisResponse
import com.diplomat.domain.model.MessageStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DiplomatMessageRepositoryTest {

    private val dao = FakeMessageDao()

    @Test
    fun `threads are grouped by sender with latest message and count`() = runTest {
        val api = FakeApi()
        val repository = DiplomatMessageRepository(dao, api)

        repository.recordIncoming("Alice", "hi", "com.whatsapp", timestamp = 10)
        repository.recordIncoming("Alice", "you there?", "com.whatsapp", timestamp = 30)
        repository.recordIncoming("Bob", "hello", "com.google.android.apps.messaging", timestamp = 20)

        val threads = repository.observeThreads().first()

        assertEquals(2, threads.size)
        // Sorted by latest timestamp desc -> Alice (30) first.
        assertEquals("Alice", threads[0].sender)
        assertEquals("you there?", threads[0].latestMessage.body)
        assertEquals(2, threads[0].messageCount)
        assertEquals("Bob", threads[1].sender)
        assertEquals(1, threads[1].messageCount)
    }

    @Test
    fun `requestDraft stores tone and draft and marks message drafted`() = runTest {
        val api = FakeApi(
            AnalysisResponse(
                toneAnalysis = "passive-aggressive",
                requiresResponse = true,
                draftResponse = "Understood, thank you.",
            ),
        )
        val repository = DiplomatMessageRepository(dao, api)
        val id = repository.recordIncoming("Carol", "why didn't you reply?!", "com.whatsapp", 5)

        val result = repository.requestDraft(id, userAgreement = true, userReasoning = "stay calm")

        assertTrue(result.isSuccess)
        val stored = dao.getById(id)!!
        assertEquals(MessageStatus.DRAFTED, stored.status)
        assertEquals("passive-aggressive", stored.toneAnalysis)
        assertEquals("Understood, thank you.", stored.draftResponse)
        assertEquals(true, stored.userAgreement)
        assertEquals("stay calm", stored.userReasoning)
    }

    @Test
    fun `requestDraft marks message as error when the backend fails`() = runTest {
        val repository = DiplomatMessageRepository(dao, FailingApi())
        val id = repository.recordIncoming("Dan", "ping", "com.whatsapp", 1)

        val result = repository.requestDraft(id, userAgreement = false, userReasoning = "")

        assertTrue(result.isFailure)
        assertEquals(MessageStatus.ERROR, dao.getById(id)!!.status)
    }

    private class FakeApi(
        private val response: AnalysisResponse = AnalysisResponse("neutral", false, ""),
    ) : DiplomatApi {
        override suspend fun analyze(request: AnalysisRequest): AnalysisResponse = response
    }

    private class FailingApi : DiplomatApi {
        override suspend fun analyze(request: AnalysisRequest): AnalysisResponse =
            throw RuntimeException("network down")
    }

    private class FakeMessageDao : MessageDao {
        private val rows = MutableStateFlow<List<MessageEntity>>(emptyList())
        private var nextId = 1L

        override suspend fun insert(message: MessageEntity): Long {
            val id = nextId++
            rows.update { it + message.copy(id = id) }
            return id
        }

        override suspend fun update(message: MessageEntity) {
            rows.update { list -> list.map { if (it.id == message.id) message else it } }
        }

        override fun observeById(id: Long): Flow<MessageEntity?> =
            rows.map { list -> list.firstOrNull { it.id == id } }

        override suspend fun getById(id: Long): MessageEntity? =
            rows.value.firstOrNull { it.id == id }

        override fun observeAll(): Flow<List<MessageEntity>> = rows
    }
}
