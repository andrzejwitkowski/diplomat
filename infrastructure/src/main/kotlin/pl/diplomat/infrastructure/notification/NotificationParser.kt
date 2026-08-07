package pl.diplomat.infrastructure.notification

import android.os.Bundle
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
)

class NotificationParser(
    private val placeholders: VisualPlaceholderCatalog,
) {
    fun parse(packageName: String, extras: Bundle, postedAtMillis: Long, notificationKey: String): ParsedNotification? {
        val sourceApp = resolveSourceApp(packageName) ?: return null
        val title = extras.getCharSequence("android.title")?.toString()?.trim().orEmpty()
        val text = extractNotificationText(extras)

        val senderCandidates = when (sourceApp) {
            MessageSourceApp.SMS -> extractSmsSenderCandidates(title, extras)
            MessageSourceApp.WHATSAPP -> extractWhatsAppSenderCandidates(title, extras)
        }
        val senderPhone = senderCandidates.firstOrNull() ?: title
        if (senderPhone.isBlank()) return null

        val content = resolveContent(text, extras) ?: return null

        return ParsedNotification(
            senderPhone = senderPhone,
            content = content,
            timestamp = postedAtMillis,
            sourceApp = sourceApp,
            notificationKey = notificationKey,
            additionalSenderCandidates = senderCandidates.drop(1),
        )
    }

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

        fun isSupportedPackage(packageName: String): Boolean =
            resolveSourceApp(packageName) != null

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
        extractMessagingStylePayload(extras)?.text?.let { return it }
        return ""
    }

    private fun hasPictureAttachment(extras: Bundle): Boolean =
        extras.containsKey("android.picture") ||
            extras.containsKey("android.pictureContentDescription") ||
            extras.getString("android.template")?.contains("BigPictureStyle", ignoreCase = true) == true

    private fun extractSmsSenderCandidates(title: String, extras: Bundle): List<String> {
        val candidates = mutableListOf<String>()
        extras.getString("android.conversationTitle")?.trim()?.takeIf { it.isNotBlank() }
            ?.let { candidates.add(it) }
        title.trim().takeIf { it.isNotBlank() && !isGenericMessagingAppTitle(it) }
            ?.let { candidates.add(it) }
        extras.getString("android.subText")?.trim()?.takeIf { it.isNotBlank() }
            ?.let { candidates.add(it) }
        extractMessagingStylePayload(extras)?.sender?.let { candidates.add(it) }
        title.trim().takeIf { it.isNotBlank() }?.let { candidates.add(it) }
        return candidates.distinct()
    }

    private fun extractWhatsAppSenderCandidates(title: String, extras: Bundle): List<String> {
        val candidates = mutableListOf<String>()
        extras.getString("android.conversationTitle")?.trim()?.takeIf { it.isNotBlank() }
            ?.let { candidates.add(it) }
        title.trim().takeIf { it.isNotBlank() }?.let { candidates.add(it) }
        return candidates.distinct()
    }

    private fun isGenericMessagingAppTitle(title: String): Boolean =
        title.trim().lowercase() in GENERIC_MESSAGING_TITLES

    private data class MessagingStylePayload(
        val text: String?,
        val sender: String?,
    )

    @Suppress("DEPRECATION")
    private fun extractMessagingStylePayload(extras: Bundle): MessagingStylePayload? {
        val messages = extras.getParcelableArray("android.messages")
        if (messages == null || messages.isEmpty()) return null

        var lastText: String? = null
        var lastSender: String? = null
        for (parcelable in messages) {
            val bundle = parcelable as? Bundle ?: continue
            bundle.getCharSequence("text")?.toString()?.trim()?.takeIf { it.isNotBlank() }
                ?.let { lastText = it }
            bundle.getCharSequence("sender")?.toString()?.trim()?.takeIf { it.isNotBlank() }
                ?.let { lastSender = it }
        }
        if (lastText == null && lastSender == null) return null
        return MessagingStylePayload(text = lastText, sender = lastSender)
    }
}
