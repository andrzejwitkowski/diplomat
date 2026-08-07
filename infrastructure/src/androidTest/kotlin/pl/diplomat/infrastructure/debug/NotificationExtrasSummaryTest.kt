package pl.diplomat.infrastructure.debug

import android.os.Bundle
import android.text.SpannableString
import org.junit.Assert.assertTrue
import org.junit.Test
import pl.diplomat.infrastructure.BaseSpec

class NotificationExtrasSummaryTest : BaseSpec() {

    @Test
    fun countsSpannableConversationMetadata() {
        val extras = Bundle().apply {
            putCharSequence("android.conversationTitle", SpannableString("Alice"))
            putCharSequence("android.subText", SpannableString("via SMS"))
        }

        val formatted = NotificationExtrasSummary.format(extras)
        assertTrue(formatted.contains("convLen=5"))
        assertTrue(formatted.contains("subLen=7"))
    }
}
