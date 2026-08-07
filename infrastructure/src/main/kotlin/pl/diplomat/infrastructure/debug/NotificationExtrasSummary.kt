package pl.diplomat.infrastructure.debug

import android.os.Bundle

object NotificationExtrasSummary {
    fun format(extras: Bundle): String {
        val title = extras.getCharSequence("android.title")?.toString()?.trim().orEmpty()
        val text = extras.getCharSequence("android.text")?.toString()?.trim().orEmpty()
        val conversationTitle = extras.getString("android.conversationTitle").orEmpty().trim()
        val subText = extras.getString("android.subText").orEmpty().trim()
        @Suppress("DEPRECATION")
        val messageCount = extras.getParcelableArray("android.messages")?.size ?: 0
        return buildString {
            append("title=").append(quote(title))
            append(" conv=").append(quote(conversationTitle))
            append(" sub=").append(quote(subText))
            append(" text=").append(quote(text))
            append(" msgs=").append(messageCount)
        }
    }

    private fun quote(value: String): String =
        if (value.isEmpty()) "(empty)" else value.take(120)
}
