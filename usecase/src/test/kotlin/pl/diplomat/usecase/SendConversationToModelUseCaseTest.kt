package pl.diplomat.usecase

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import pl.diplomat.domain.model.ChatMessage
import pl.diplomat.domain.model.ChatRole
import pl.diplomat.domain.model.LlmSettings
import pl.diplomat.domain.model.Sentiment
import pl.diplomat.domain.port.LlmCompletionPort
import pl.diplomat.domain.port.LlmCompletionResult
import pl.diplomat.domain.port.LlmSettingsPort

class SendConversationToModelUseCaseTest {

    private val settingsPort = InMemorySettingsPort()
    private val completionPort = RecordingCompletionPort()
    private val useCase = SendConversationToModelUseCase(settingsPort, completionPort)

    @Test
    fun `sends system prompt before conversation messages`() = runTest {
        settingsPort.saved = LlmSettings(apiKey = "key")
        val conversation = listOf(
            ChatMessage(ChatRole.USER, "hello"),
            ChatMessage(ChatRole.ASSISTANT, "hi"),
        )

        useCase("You are a diplomat", conversation)

        assertEquals(
            listOf(
                ChatMessage(ChatRole.SYSTEM, "You are a diplomat"),
                ChatMessage(ChatRole.USER, "hello"),
                ChatMessage(ChatRole.ASSISTANT, "hi"),
            ),
            completionPort.lastMessages,
        )
    }

    @Test
    fun `passes loaded settings to completion port`() = runTest {
        settingsPort.saved = LlmSettings(
            baseUrl = "http://localhost:11434/v1",
            apiKey = "secret",
            model = "llama3",
        )

        useCase("prompt", listOf(ChatMessage(ChatRole.USER, "hi")))

        assertEquals(
            LlmSettings("http://localhost:11434/v1", "secret", "llama3"),
            completionPort.lastSettings,
        )
    }

    @Test
    fun `fails without calling port when api key is blank`() = runTest {
        settingsPort.saved = LlmSettings(apiKey = "  ")

        val result = useCase("prompt", listOf(ChatMessage(ChatRole.USER, "hi")))

        assertTrue(result is LlmCompletionResult.Failure)
        assertEquals(0, completionPort.callCount)
    }

    @Test
    fun `returns port result`() = runTest {
        settingsPort.saved = LlmSettings(apiKey = "key")
        completionPort.result = LlmCompletionResult.Success("answer")

        val result = useCase("prompt", listOf(ChatMessage(ChatRole.USER, "hi")))

        assertEquals(LlmCompletionResult.Success("answer"), result)
    }

    @Test
    fun `injects sentiment and desired answer between prompt and conversation`() = runTest {
        settingsPort.saved = LlmSettings(apiKey = "key")

        useCase(
            systemPrompt = "Suggest an answer",
            sentiment = Sentiment.NEGATIVE,
            desiredAnswer = "Keep it friendly",
            conversation = listOf(ChatMessage(ChatRole.USER, "hi")),
        )

        assertEquals(
            listOf(
                ChatMessage(ChatRole.SYSTEM, "Suggest an answer"),
                ChatMessage(ChatRole.USER, "Feedback sentiment: Negative"),
                ChatMessage(ChatRole.SYSTEM, "Desired answer based on the conversation and sentiment:\nKeep it friendly"),
                ChatMessage(ChatRole.USER, "hi"),
            ),
            completionPort.lastMessages,
        )
    }

    @Test
    fun `omits desired answer message when blank`() = runTest {
        settingsPort.saved = LlmSettings(apiKey = "key")

        useCase(
            systemPrompt = "Suggest",
            sentiment = Sentiment.NEUTRAL,
            desiredAnswer = "",
            conversation = listOf(ChatMessage(ChatRole.USER, "hi")),
        )

        assertEquals(
            listOf(
                ChatMessage(ChatRole.SYSTEM, "Suggest"),
                ChatMessage(ChatRole.USER, "Feedback sentiment: Neutral"),
                ChatMessage(ChatRole.USER, "hi"),
            ),
            completionPort.lastMessages,
        )
    }

    private class InMemorySettingsPort : LlmSettingsPort {
        var saved: LlmSettings = LlmSettings()

        override fun load(): LlmSettings = saved

        override suspend fun save(settings: LlmSettings) {
            saved = settings
        }
    }

    private class RecordingCompletionPort : LlmCompletionPort {
        var lastSettings: LlmSettings? = null
        var lastMessages: List<ChatMessage>? = null
        var callCount = 0
        var result: LlmCompletionResult = LlmCompletionResult.Success("")

        override suspend fun complete(
            settings: LlmSettings,
            messages: List<ChatMessage>,
        ): LlmCompletionResult {
            callCount++
            lastSettings = settings
            lastMessages = messages
            return result
        }
    }
}
