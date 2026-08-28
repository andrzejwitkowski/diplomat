package pl.diplomat.infrastructure.adapter

import org.junit.Assert.assertEquals
import org.junit.Test

class OpenAiCompatibleLlmAdapterTest {

    private val adapter = OpenAiCompatibleLlmAdapter()

    @Test
    fun appendsChatCompletionsToOpenAiStyleBase() {
        assertEquals(
            "https://openrouter.ai/api/v1/chat/completions",
            adapter.endpoint("https://openrouter.ai/api/v1"),
        )
    }

    @Test
    fun doesNotDoubleAppendWhenFullEndpointIsPasted() {
        assertEquals(
            "https://openrouter.ai/api/v1/chat/completions",
            adapter.endpoint("https://openrouter.ai/api/v1/chat/completions"),
        )
    }

    @Test
    fun trimsTrailingSlashBeforeAppending() {
        assertEquals(
            "https://openrouter.ai/api/v1/chat/completions",
            adapter.endpoint("https://openrouter.ai/api/v1/"),
        )
    }
}
