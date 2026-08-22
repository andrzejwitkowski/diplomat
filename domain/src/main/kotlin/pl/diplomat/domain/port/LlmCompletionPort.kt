package pl.diplomat.domain.port

import pl.diplomat.domain.model.ChatMessage
import pl.diplomat.domain.model.LlmSettings

sealed interface LlmCompletionResult {
    data class Success(val text: String) : LlmCompletionResult
    data class Failure(val message: String) : LlmCompletionResult
}

interface LlmCompletionPort {
    suspend fun complete(settings: LlmSettings, messages: List<ChatMessage>): LlmCompletionResult
}
