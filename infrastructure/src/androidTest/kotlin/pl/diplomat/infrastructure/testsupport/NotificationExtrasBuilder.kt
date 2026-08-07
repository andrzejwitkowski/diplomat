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
    private var messagingEntries: List<MessagingEntry> = emptyList()

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
        messagingEntries = messagingEntries + MessagingEntry.Plain(sender, text)
    }

    fun withMessagingOutgoingMessage(text: String) = apply {
        messagingEntries = messagingEntries + MessagingEntry.Plain(sender = null, text = text)
    }

    fun withMessagingPersonMessage(text: String, personName: String, tel: String) = apply {
        val person = Person.Builder()
            .setName(personName)
            .setUri(Uri.parse("tel:$tel"))
            .build()
        messagingEntries = messagingEntries + MessagingEntry.WithPerson(text, person)
    }

    fun build(): Bundle = Bundle().apply {
        title?.let { putCharSequence("android.title", it) }
        text?.let { putCharSequence("android.text", it) }
        conversationTitle?.let { putCharSequence("android.conversationTitle", it) }
        picture?.let { putParcelable("android.picture", it) }
        val messageBundles = messagingEntries.map { entry ->
            when (entry) {
                is MessagingEntry.Plain -> Bundle().apply {
                    putCharSequence("text", entry.text)
                    entry.sender?.let { putCharSequence("sender", it) }
                }
                is MessagingEntry.WithPerson -> Bundle().apply {
                    putCharSequence("text", entry.text)
                    putParcelable("sender_person", entry.person)
                }
            }
        }
        if (messageBundles.isNotEmpty()) {
            putParcelableArray("android.messages", messageBundles.toTypedArray())
        }
    }

    private sealed interface MessagingEntry {
        data class Plain(val sender: String?, val text: String) : MessagingEntry
        data class WithPerson(val text: String, val person: Person) : MessagingEntry
    }
}

fun notificationExtras(): NotificationExtrasBuilder = NotificationExtrasBuilder()
