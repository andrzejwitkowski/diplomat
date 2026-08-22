package pl.diplomat.usecase

import pl.diplomat.domain.model.ChatMessage
import pl.diplomat.domain.model.ChatRole
import pl.diplomat.domain.port.LlmCompletionPort
import pl.diplomat.domain.port.LlmCompletionResult
import pl.diplomat.domain.port.LlmSettingsPort

/**
 * Sends a fixed [systemPrompt] plus a volatile conversation (typically a selected
 * excerpt, later produced by the conversation service as role-tagged messages) to
 * an OpenAI-compatible chat completion API.
 */
class SendConversationToModelUseCase(
    private val settingsPort: LlmSettingsPort,
    private val completionPort: LlmCompletionPort,
) {
    suspend operator fun invoke(
        systemPrompt: String,
        conversation: List<ChatMessage>,
    ): LlmCompletionResult {
        val settings = settingsPort.load()
        if (settings.apiKey.isBlank()) {
            return LlmCompletionResult.Failure("API key is not configured")
        }
        val messages = buildList {
            add(ChatMessage(ChatRole.SYSTEM, systemPrompt))
            addAll(conversation)
        }
        return completionPort.complete(settings, messages)
    }
}
