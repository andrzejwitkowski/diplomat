package pl.diplomat.infrastructure.debug

import android.os.Bundle

object NotificationExtrasSummary {
    fun format(extras: Bundle): String {
        val titleLen = extras.getCharSequence("android.title")?.toString()?.trim()?.length ?: 0
        val textLen = extras.getCharSequence("android.text")?.toString()?.trim()?.length ?: 0
        val conversationTitleLen =
            extras.getCharSequence("android.conversationTitle")?.toString()?.trim()?.length ?: 0
        val subTextLen = extras.getCharSequence("android.subText")?.toString()?.trim()?.length ?: 0
        @Suppress("DEPRECATION")
        val messageCount = extras.getParcelableArray("android.messages")?.size ?: 0
        return buildString {
            append("titleLen=").append(titleLen)
            append(" convLen=").append(conversationTitleLen)
            append(" subLen=").append(subTextLen)
            append(" textLen=").append(textLen)
            append(" msgs=").append(messageCount)
        }
    }
}
