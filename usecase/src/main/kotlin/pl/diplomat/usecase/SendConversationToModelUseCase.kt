package pl.diplomat.usecase

import pl.diplomat.domain.model.ChatMessage
import pl.diplomat.domain.model.ChatRole
import pl.diplomat.domain.model.Sentiment
import pl.diplomat.domain.port.LlmCompletionPort
import pl.diplomat.domain.port.LlmCompletionResult
import pl.diplomat.domain.port.LlmSettingsPort

/**
 * Sends a fixed [systemPrompt] plus a volatile conversation (typically a selected
 * excerpt, later produced by the conversation service as role-tagged messages) to
 * an OpenAI-compatible chat completion API. Sentiment and an optional desired-answer
 * hint are injected as extra messages so the model can shape its suggestion.
 */
class SendConversationToModelUseCase(
    private val settingsPort: LlmSettingsPort,
    private val completionPort: LlmCompletionPort,
) {
    suspend operator fun invoke(
        systemPrompt: String,
        conversation: List<ChatMessage>,
        sentiment: Sentiment? = null,
        desiredAnswer: String? = null,
    ): LlmCompletionResult {
        val settings = settingsPort.load()
        if (settings.apiKey.isBlank()) {
            return LlmCompletionResult.Failure("API key is not configured")
        }
        val messages = buildList {
            add(ChatMessage(ChatRole.SYSTEM, systemPrompt))
            sentiment?.let {
                add(ChatMessage(ChatRole.USER, "Feedback sentiment: ${it.label}"))
            }
            desiredAnswer
                ?.takeIf { it.isNotBlank() }
                ?.let {
                    add(ChatMessage(ChatRole.SYSTEM, "Desired answer based on the conversation and sentiment:\n$it"))
                }
            addAll(conversation)
        }
        return completionPort.complete(settings, messages)
    }
}
