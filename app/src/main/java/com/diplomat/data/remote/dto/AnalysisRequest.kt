package com.diplomat.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Payload sent to the tone-analysis backend.
 *
 * Field names use snake_case to match the backend contract.
 */
@Serializable
data class AnalysisRequest(
    @SerialName("incoming_message") val incomingMessage: String,
    @SerialName("user_agreement") val userAgreement: Boolean,
    @SerialName("user_reasoning") val userReasoning: String,
)
