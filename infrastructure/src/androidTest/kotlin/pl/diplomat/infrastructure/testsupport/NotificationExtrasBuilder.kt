package pl.diplomat.infrastructure.testsupport

import android.graphics.Bitmap
import android.os.Bundle

class NotificationExtrasBuilder {
    private var title: String? = null
    private var text: String? = null
    private var conversationTitle: String? = null
    private var picture: Bitmap? = null

    fun withTitle(value: String) = apply { title = value }

    fun withText(value: String) = apply { text = value }

    fun withConversationTitle(value: String) = apply { conversationTitle = value }

    fun withPicture(bitmap: Bitmap) = apply { picture = bitmap }

    fun withPicture(width: Int = 1, height: Int = 1) = apply {
        picture = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    }

    fun build(): Bundle = Bundle().apply {
        title?.let { putCharSequence("android.title", it) }
        text?.let { putCharSequence("android.text", it) }
        conversationTitle?.let { putString("android.conversationTitle", it) }
        picture?.let { putParcelable("android.picture", it) }
    }
}

fun notificationExtras(): NotificationExtrasBuilder = NotificationExtrasBuilder()
