package pl.diplomat.infrastructure.testsupport

import android.app.Person
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.text.SpannableString

class NotificationExtrasBuilder {
    private var title: String? = null
    private var text: String? = null
    private var conversationTitle: CharSequence? = null
    private var picture: Bitmap? = null
    private var messagingMessages: List<Pair<String?, String?>> = emptyList()
    private var messagingPersonMessages: List<Pair<String, Person>> = emptyList()

    fun withTitle(value: String) = apply { title = value }

    fun withText(value: String) = apply { text = value }

    fun withConversationTitle(value: String) = apply { conversationTitle = value }

    fun withSpannableConversationTitle(value: String) = apply {
        conversationTitle = SpannableString(value)
    }

    fun withPicture(bitmap: Bitmap) = apply { picture = bitmap }

    fun withPicture(width: Int = 1, height: Int = 1) = apply {
        picture = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    }

    fun withMessagingMessage(text: String, sender: String? = null) = apply {
        messagingMessages = messagingMessages + (sender to text)
    }

    fun withMessagingOutgoingMessage(text: String) = apply {
        messagingMessages = messagingMessages + (null to text)
    }

    fun withMessagingPersonMessage(text: String, personName: String, tel: String) = apply {
        val person = Person.Builder()
            .setName(personName)
            .setUri(Uri.parse("tel:$tel"))
            .build()
        messagingPersonMessages = messagingPersonMessages + (text to person)
    }

    fun build(): Bundle = Bundle().apply {
        title?.let { putCharSequence("android.title", it) }
        text?.let { putCharSequence("android.text", it) }
        conversationTitle?.let { putCharSequence("android.conversationTitle", it) }
        picture?.let { putParcelable("android.picture", it) }
        val messageBundles = buildList {
            messagingMessages.forEach { (sender, messageText) ->
                add(
                    Bundle().apply {
                        putCharSequence("text", messageText)
                        sender?.let { putCharSequence("sender", it) }
                    },
                )
            }
            messagingPersonMessages.forEach { (messageText, person) ->
                add(
                    Bundle().apply {
                        putCharSequence("text", messageText)
                        putParcelable("sender_person", person)
                    },
                )
            }
        }
        if (messageBundles.isNotEmpty()) {
            putParcelableArray("android.messages", messageBundles.toTypedArray())
        }
    }
}

fun notificationExtras(): NotificationExtrasBuilder = NotificationExtrasBuilder()
