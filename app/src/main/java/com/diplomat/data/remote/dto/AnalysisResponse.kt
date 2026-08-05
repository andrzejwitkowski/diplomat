package com.diplomat.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Response returned by the tone-analysis backend.
 */
@Serializable
data class AnalysisResponse(
    @SerialName("tone_analysis") val toneAnalysis: String,
    @SerialName("requires_response") val requiresResponse: Boolean,
    @SerialName("draft_response") val draftResponse: String,
)
