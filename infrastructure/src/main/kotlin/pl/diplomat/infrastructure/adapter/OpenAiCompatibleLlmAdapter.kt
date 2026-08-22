package pl.diplomat.infrastructure.adapter

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import pl.diplomat.domain.model.ChatMessage
import pl.diplomat.domain.model.ChatRole
import pl.diplomat.domain.model.LlmSettings
import pl.diplomat.domain.port.LlmCompletionPort
import pl.diplomat.domain.port.LlmCompletionResult
import java.net.HttpURLConnection
import java.net.URL

/**
 * OpenAI-compatible `/chat/completions` client. Plain [HttpURLConnection] + platform
 * org.json, mirroring [pl.diplomat.infrastructure.ota.OtaUpdateManager].
 */
class OpenAiCompatibleLlmAdapter : LlmCompletionPort {

    private companion object {
        const val CONNECT_TIMEOUT_MS = 30_000
        const val READ_TIMEOUT_MS = 60_000
        const val BEARER_PREFIX = "Bearer "
    }

    override suspend fun complete(
        settings: LlmSettings,
        messages: List<ChatMessage>,
    ): LlmCompletionResult = withContext(Dispatchers.IO) {
        val connection = (URL(endpoint(settings.baseUrl)).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = CONNECT_TIMEOUT_MS
            readTimeout = READ_TIMEOUT_MS
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Authorization", BEARER_PREFIX + settings.apiKey)
        }
        try {
            connection.outputStream.use { output ->
                output.write(buildRequestBody(settings.model, messages).toByteArray(Charsets.UTF_8))
            }
            val code = connection.responseCode
            val responseBody = readBody(connection, code)
            if (code !in 200..299) {
                LlmCompletionResult.Failure("HTTP $code: ${extractErrorMessage(responseBody)}")
            } else {
                parseAssistantText(responseBody)
                    ?.let { LlmCompletionResult.Success(it) }
                    ?: LlmCompletionResult.Failure("Unexpected response: ${responseBody.take(200)}")
            }
        } catch (error: Throwable) {
            LlmCompletionResult.Failure(error.message ?: "Request failed")
        } finally {
            connection.disconnect()
        }
    }

    private fun endpoint(baseUrl: String): String =
        "${baseUrl.trimEnd('/')}/chat/completions"

    private fun buildRequestBody(model: String, messages: List<ChatMessage>): String {
        val jsonMessages = JSONArray()
        messages.forEach { message ->
            jsonMessages.put(
                JSONObject()
                    .put("role", message.role.wireName())
                    .put("content", message.content),
            )
        }
        return JSONObject()
            .put("model", model)
            .put("messages", jsonMessages)
            .toString()
    }

    private fun readBody(connection: HttpURLConnection, code: Int): String {
        val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            ?: return ""
        return stream.bufferedReader(Charsets.UTF_8).use { it.readText() }
    }

    private fun parseAssistantText(responseBody: String): String? = runCatching {
        JSONObject(responseBody)
            .getJSONArray("choices")
            .getJSONObject(0)
            .getJSONObject("message")
            .getString("content")
    }.getOrNull()

    private fun extractErrorMessage(responseBody: String): String = runCatching {
        JSONObject(responseBody).getJSONObject("error").getString("message")
    }.getOrDefault(responseBody.take(200))

    private fun ChatRole.wireName(): String = when (this) {
        ChatRole.SYSTEM -> "system"
        ChatRole.USER -> "user"
        ChatRole.ASSISTANT -> "assistant"
    }
}
