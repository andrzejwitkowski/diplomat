package pl.diplomat.infrastructure.notification

import android.app.Person
import android.os.Build
import android.os.Bundle
import androidx.annotation.RequiresApi
import androidx.core.os.BundleCompat
import pl.diplomat.domain.model.MessageContent
import pl.diplomat.domain.model.MessageSourceApp
import pl.diplomat.domain.model.VisualMediaKind
import pl.diplomat.usecase.RawIncomingMessage

data class ParsedNotification(
    val senderPhone: String,
    val content: MessageContent,
    val timestamp: Long,
    val sourceApp: MessageSourceApp,
    val notificationKey: String,
    val additionalSenderCandidates: List<String> = emptyList(),
    val isOutgoing: Boolean = false,
)

class NotificationParser(
    private val placeholders: VisualPlaceholderCatalog,
) {
    fun parse(packageName: String, extras: Bundle, postedAtMillis: Long, notificationKey: String): List<ParsedNotification> {
        val sourceApp = resolveSourceApp(packageName) ?: return emptyList()
        val title = extras.getCharSequence("android.title")?.toString()?.trim().orEmpty()
        val conversationCandidates = when (sourceApp) {
            MessageSourceApp.SMS -> extractSmsSenderCandidates(title, extras)
            MessageSourceApp.WHATSAPP -> extractWhatsAppSenderCandidates(title, extras)
        }
        val senderPhone = conversationCandidates.firstOrNull() ?: title
        if (senderPhone.isBlank()) return emptyList()

        val threadMessages = extractMessagingStyleMessages(
            extras = extras,
            postedAtMillis = postedAtMillis,
            notificationKey = notificationKey,
            conversationCandidates = conversationCandidates,
            sourceApp = sourceApp,
        )
        if (threadMessages.isNotEmpty()) {
            return threadMessages.mapIndexedNotNull { index, message ->
                val contentExtras = if (index == threadMessages.lastIndex) extras else Bundle.EMPTY
                val content = resolveContent(message.text, contentExtras) ?: return@mapIndexedNotNull null
                ParsedNotification(
                    senderPhone = senderPhone,
                    content = content,
                    timestamp = message.timestamp,
                    sourceApp = sourceApp,
                    notificationKey = notificationKey,
                    additionalSenderCandidates = conversationCandidates.drop(1),
                    isOutgoing = message.isOutgoing,
                )
            }
        }

        val text = extractNotificationText(extras)
        if (sourceApp == MessageSourceApp.WHATSAPP && WhatsAppSystemTextFilter.isJunk(text)) return emptyList()
        val content = resolveContent(text, extras) ?: return emptyList()
        return listOf(
            ParsedNotification(
                senderPhone = senderPhone,
                content = content,
                timestamp = postedAtMillis,
                sourceApp = sourceApp,
                notificationKey = notificationKey,
                additionalSenderCandidates = conversationCandidates.drop(1),
                isOutgoing = false,
            ),
        )
    }

    fun resolveTextContent(text: String): MessageContent? = resolveContent(text, Bundle.EMPTY)

    internal fun resolveContent(text: String, extras: Bundle): MessageContent? {
        val trimmedText = text.trim()
        val hasPicture = hasPictureAttachment(extras)
        val emojiKind = emojiPrefixKind(trimmedText.lowercase())
        val placeholderKind = when {
            emojiKind != null && isPlaceholderOnly(trimmedText, emojiKind) -> emojiKind
            else -> placeholders.detectKind(trimmedText)
        }

        return when {
            hasPicture && trimmedText.isBlank() -> MessageContent.VisualOnly(VisualMediaKind.PHOTO)
            placeholderKind != null && isPlaceholderOnly(trimmedText, placeholderKind) ->
                MessageContent.VisualOnly(placeholderKind)
            hasPicture -> MessageContent.VisualWithText(VisualMediaKind.PHOTO, trimmedText)
            trimmedText.isNotBlank() -> MessageContent.TextOnly(trimmedText)
            else -> null
        }
    }

    private fun isPlaceholderOnly(text: String, kind: VisualMediaKind): Boolean {
        val normalized = text.trim().lowercase()
        if (normalized.isBlank()) return true
        emojiPrefixKind(normalized)?.let { emojiKind ->
            if (emojiKind != kind) return false
            val remainder = stripKnownEmojiPrefix(normalized)
            return remainder.isBlank() || placeholders.isPlaceholderOnly(remainder, kind)
        }
        return placeholders.isPlaceholderOnly(text, kind)
    }

    private fun stripKnownEmojiPrefix(normalized: String): String =
        normalized
            .removePrefix("📷")
            .removePrefix("🖼")
            .removePrefix("🎬")
            .removePrefix("🎞")
            .removePrefix("🎥")
            .replace("\uFE0F", "")
            .trim()

    private fun emojiPrefixKind(normalized: String): VisualMediaKind? = when {
        normalized.startsWith("📷") || normalized.startsWith("🖼") -> VisualMediaKind.PHOTO
        normalized.startsWith("🎬") || normalized.startsWith("🎞") -> VisualMediaKind.GIF
        normalized.startsWith("🎥") -> VisualMediaKind.VIDEO
        else -> null
    }

    fun toRaw(parsed: ParsedNotification): RawIncomingMessage =
        RawIncomingMessage(
            senderPhone = parsed.senderPhone,
            content = parsed.content,
            timestamp = parsed.timestamp,
            sourceApp = parsed.sourceApp,
            notificationKey = parsed.notificationKey,
            additionalSenderCandidates = parsed.additionalSenderCandidates,
            isOutgoing = parsed.isOutgoing,
        )

    companion object {
        private val SMS_PACKAGES = setOf(
            "com.google.android.apps.messaging",
            "com.android.mms",
            "com.samsung.android.messaging",
        )

        private val WHATSAPP_PACKAGES = setOf(
            "com.whatsapp",
            "com.whatsapp.w4b",
        )

        private val GENERIC_MESSAGING_TITLES = setOf(
            "messages",
            "message",
            "messaging",
            "sms",
            "mms",
            "wiadomości",
            "wiadomosc",
        )

        private val SELF_SENDER_LABELS = setOf(
            "you",
            "ty",
            "ja",
        )

        private const val THREAD_MESSAGE_STEP_MS = 1_000L

        internal fun inferredThreadTimestamp(postedAtMillis: Long, index: Int, lastIndex: Int): Long {
            val offset = (lastIndex - index).coerceAtLeast(0)
            val base = if (postedAtMillis > 0L) postedAtMillis else System.currentTimeMillis()
            return base - offset * THREAD_MESSAGE_STEP_MS
        }

        fun isSupportedPackage(packageName: String): Boolean =
            resolveSourceApp(packageName) != null

        fun isWhatsAppPackage(packageName: String): Boolean =
            packageName in WHATSAPP_PACKAGES

        private fun resolveSourceApp(packageName: String): MessageSourceApp? = when {
            packageName in SMS_PACKAGES -> MessageSourceApp.SMS
            packageName in WHATSAPP_PACKAGES -> MessageSourceApp.WHATSAPP
            else -> null
        }
    }

    private fun resolveSourceApp(packageName: String): MessageSourceApp? =
        Companion.resolveSourceApp(packageName)

    private fun extractNotificationText(extras: Bundle): String {
        extras.getCharSequence("android.text")?.toString()?.trim()?.takeIf { it.isNotBlank() }
            ?.let { return it }
        extras.getCharSequence("android.bigText")?.toString()?.trim()?.takeIf { it.isNotBlank() }
            ?.let { return it }
        @Suppress("DEPRECATION")
        extras.getCharSequenceArray("android.textLines")
            ?.mapNotNull { it?.toString()?.trim() }
            ?.filter { it.isNotBlank() }
            ?.joinToString("\n")
            ?.takeIf { it.isNotBlank() }
            ?.let { return it }
        extractMessagingStyleMessages(
            extras = extras,
            postedAtMillis = 0L,
            notificationKey = "",
            conversationCandidates = emptyList(),
        )
            .lastOrNull()
            ?.text
            ?.let { return it }
        return ""
    }

    private fun hasPictureAttachment(extras: Bundle): Boolean =
        extras.containsKey("android.picture") ||
            extras.containsKey("android.pictureContentDescription") ||
            extras.getString("android.template")?.contains("BigPictureStyle", ignoreCase = true) == true

    private fun extractSmsSenderCandidates(title: String, extras: Bundle): List<String> =
        buildSenderCandidates(
            title = title,
            extras = extras,
            skipGenericTitle = true,
        )

    private fun extractWhatsAppSenderCandidates(title: String, extras: Bundle): List<String> {
        val candidates = mutableListOf<String>()
        charSequenceFromExtras(extras, "android.conversationTitle")?.let { candidates.add(it) }
        extractMessagingStyleMessages(extras, postedAtMillis = 0L, notificationKey = "", conversationCandidates = emptyList())
            .flatMap { it.senderCandidates }
            .let { candidates.addAll(it) }
        charSequenceFromExtras(extras, "android.subText")?.let { candidates.add(it) }
        title.trim().takeIf { it.isNotBlank() }?.let { candidates.add(it) }
        return candidates.distinct()
    }

    private fun buildSenderCandidates(
        title: String,
        extras: Bundle,
        skipGenericTitle: Boolean,
    ): List<String> {
        val candidates = mutableListOf<String>()
        charSequenceFromExtras(extras, "android.conversationTitle")?.let { candidates.add(it) }
        if (!skipGenericTitle || !isGenericMessagingAppTitle(title)) {
            title.trim().takeIf { it.isNotBlank() }?.let { candidates.add(it) }
        }
        charSequenceFromExtras(extras, "android.subText")?.let { candidates.add(it) }
        extractMessagingStyleMessages(
            extras = extras,
            postedAtMillis = 0L,
            notificationKey = "",
            conversationCandidates = emptyList(),
        )
            .flatMap { it.senderCandidates }
            .let { candidates.addAll(it) }
        if (skipGenericTitle) {
            title.trim().takeIf { it.isNotBlank() }?.let { candidates.add(it) }
        }
        return candidates.distinct()
    }

    private fun charSequenceFromExtras(extras: Bundle, key: String): String? =
        extras.getCharSequence(key)?.toString()?.trim()?.takeIf { it.isNotBlank() }

    private fun isGenericMessagingAppTitle(title: String): Boolean =
        title.trim().lowercase() in GENERIC_MESSAGING_TITLES

    private data class MessagingStyleMessage(
        val text: String,
        val timestamp: Long,
        val senderCandidates: List<String>,
        val isOutgoing: Boolean,
    )

    @Suppress("DEPRECATION")
    private fun extractMessagingStyleMessages(
        extras: Bundle,
        postedAtMillis: Long,
        notificationKey: String,
        conversationCandidates: List<String>,
        sourceApp: MessageSourceApp? = null,
    ): List<MessagingStyleMessage> {
        val messages = extras.getParcelableArray("android.messages") ?: return emptyList()
        if (messages.isEmpty()) return emptyList()

        val messagingUser = extractMessagingUser(extras)
        val parsed = mutableListOf<MessagingStyleMessage>()
        for ((index, parcelable) in messages.withIndex()) {
            val bundle = parcelable as? Bundle ?: continue
            val text = bundle.getCharSequence("text")?.toString()?.trim()?.takeIf { it.isNotBlank() }
                ?: continue
            if (sourceApp == MessageSourceApp.WHATSAPP && WhatsAppSystemTextFilter.isJunk(text)) continue
            val senderCandidates = buildList {
                bundle.getCharSequence("sender")?.toString()?.trim()?.takeIf { it.isNotBlank() }
                    ?.let { add(it) }
                addAll(extractSenderPersonCandidates(bundle))
            }.distinct()
            val explicitTime = bundle.getLong("time").takeIf { it > 0L }
            val timestamp = explicitTime
                ?: Companion.inferredThreadTimestamp(
                    postedAtMillis = postedAtMillis,
                    index = index,
                    lastIndex = messages.lastIndex,
                )
            val isOutgoing = resolveIsOutgoing(
                senderCandidates = senderCandidates,
                conversationCandidates = conversationCandidates,
                messagingUser = messagingUser,
                senderPerson = extractSenderPerson(bundle),
            )
            parsed.add(
                MessagingStyleMessage(
                    text = text,
                    timestamp = timestamp,
                    senderCandidates = senderCandidates,
                    isOutgoing = isOutgoing,
                ),
            )
        }
        return parsed
    }

    private fun resolveIsOutgoing(
        senderCandidates: List<String>,
        conversationCandidates: List<String>,
        messagingUser: Person?,
        senderPerson: Person?,
    ): Boolean {
        val normalizedConversation = conversationCandidates.map { it.trim().lowercase() }.toSet()
        if (senderCandidates.any { candidate ->
                val normalized = candidate.trim().lowercase()
                normalized in SELF_SENDER_LABELS && normalized !in normalizedConversation
            }
        ) {
            return true
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P &&
            senderPerson != null &&
            messagingUser != null
        ) {
            return personsRepresentSameUser(senderPerson, messagingUser)
        }
        return false
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private fun personsRepresentSameUser(left: Person, right: Person): Boolean {
        val leftUri = left.uri?.toString()?.trim()?.lowercase()
        val rightUri = right.uri?.toString()?.trim()?.lowercase()
        if (!leftUri.isNullOrBlank() && leftUri == rightUri) return true
        val leftName = left.name?.toString()?.trim()?.lowercase()
        val rightName = right.name?.toString()?.trim()?.lowercase()
        return !leftName.isNullOrBlank() && leftName == rightName
    }

    private fun extractMessagingUser(extras: Bundle): Person? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return null
        return BundleCompat.getParcelable(extras, "android.messagingUser", Person::class.java)
    }

    private fun extractSenderPerson(bundle: Bundle): Person? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return null
        return BundleCompat.getParcelable(bundle, "sender_person", Person::class.java)
    }

    private fun extractSenderPersonCandidates(bundle: Bundle): List<String> {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return emptyList()
        val person = BundleCompat.getParcelable(bundle, "sender_person", Person::class.java) ?: return emptyList()
        val candidates = mutableListOf<String>()
        person.uri?.toString()?.trim()?.takeIf { it.isNotBlank() }?.let { uri ->
            if (uri.startsWith("tel:", ignoreCase = true)) {
                candidates.add(uri.substring(4).trim())
            } else {
                candidates.add(uri)
            }
        }
        person.name?.toString()?.trim()?.takeIf { it.isNotBlank() }?.let { candidates.add(it) }
        return candidates
    }
}
