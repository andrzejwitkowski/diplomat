package com.diplomat.domain.model

/**
 * A sender-correlated conversation shown on the dashboard: the most recent
 * captured message from a contact plus a count of everything from them.
 */
data class ConversationThread(
    val sender: String,
    val source: MessageSource,
    val latestMessage: InterceptedMessage,
    val messageCount: Int,
)
