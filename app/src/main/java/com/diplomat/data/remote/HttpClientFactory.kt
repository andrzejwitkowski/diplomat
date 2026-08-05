package com.diplomat.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/**
 * Builds the shared Ktor [HttpClient] used to reach the Cloudflare-tunneled
 * backend, wired for kotlinx.serialization JSON.
 */
object HttpClientFactory {

    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    fun create(): HttpClient = HttpClient(OkHttp) {
        expectSuccess = true

        install(ContentNegotiation) {
            json(json)
        }

        install(HttpTimeout) {
            requestTimeoutMillis = 30_000
            connectTimeoutMillis = 15_000
        }

        install(Logging) {
            level = LogLevel.INFO
        }
    }
}
