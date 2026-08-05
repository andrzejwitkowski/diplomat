package com.diplomat.data.remote

import com.diplomat.data.remote.dto.AnalysisRequest
import com.diplomat.data.remote.dto.AnalysisResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

/**
 * Ktor-backed [DiplomatApi]. Posts to `<baseUrl>/analyze`.
 */
class KtorDiplomatApi(
    private val client: HttpClient,
    private val baseUrl: String,
) : DiplomatApi {

    override suspend fun analyze(request: AnalysisRequest): AnalysisResponse =
        client.post("${baseUrl.trimEnd('/')}/analyze") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
}
