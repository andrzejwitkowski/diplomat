package com.diplomat.data.remote

import com.diplomat.data.remote.dto.AnalysisRequest
import com.diplomat.data.remote.dto.AnalysisResponse

/**
 * Abstraction over the remote tone-analysis backend so the repository does
 * not depend on a concrete HTTP client.
 */
interface DiplomatApi {
    suspend fun analyze(request: AnalysisRequest): AnalysisResponse
}
