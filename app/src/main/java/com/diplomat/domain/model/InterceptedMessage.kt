package com.diplomat.domain.model

/**
 * A single captured message plus everything Diplomat derives from it.
 */
data class InterceptedMessage(
    val id: Long = 0,
    val sender: String,
    val body: String,
    val packageName: String,
    val timestamp: Long,
    val status: MessageStatus = MessageStatus.PENDING_DECISION,
    val toneAnalysis: String? = null,
    val requiresResponse: Boolean? = null,
    val draftResponse: String? = null,
    val userAgreement: Boolean? = null,
    val userReasoning: String? = null,
) {
    val source: MessageSource get() = MessageSource.fromPackage(packageName)
}
