package pl.diplomat.usecase

import android.util.Log
import pl.diplomat.domain.model.ChatMessage
import pl.diplomat.domain.model.ChatRole
import pl.diplomat.domain.model.LlmSettings
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
        Log.d("SendConversationUseCase", "Invoked with systemPrompt: $systemPrompt, conversation size: ${conversation.size}")
        val settings = settingsPort.load()
        Log.d("SendConversationUseCase", "Loaded LLM settings: $settings")
        if (settings.apiKey.isBlank()) {
            Log.e("SendConversationUseCase", "API key is not configured")
            return LlmCompletionResult.Failure("API key is not configured")
        }
        val messages = buildList {
            add(ChatMessage(ChatRole.SYSTEM, systemPrompt))
            sentiment?.let { 
                val sentimentMessage = ChatMessage(ChatRole.USER, "Feedback sentiment: ${it.label}")
                add(sentimentMessage)
                Log.d("SendConversationUseCase", "Added sentiment message: $sentimentMessage")
            }
            desiredAnswer
                ?.takeIf { it.isNotBlank() }
                ?.let { 
                    val desiredAnswerMessage = ChatMessage(ChatRole.SYSTEM, "Desired answer based on the conversation and sentiment:\n$it")
                    add(desiredAnswerMessage)
                    Log.d("SendConversationUseCase", "Added desired answer message: $desiredAnswerMessage")
                }
            addAll(conversation)
            Log.d("SendConversationUseCase", "Final message list built with ${size} messages")
        }
        Log.d("SendConversationUseCase", "Calling completion port with ${messages.size} messages")
        return completionPort.complete(settings, messages)
    }
}
