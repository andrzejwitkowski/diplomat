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
)

object NotificationParser {
    private val SMS_PACKAGES = setOf(
        "com.google.android.apps.messaging",
        "com.android.mms",
        "com.samsung.android.messaging",
    )

    private val WHATSAPP_PACKAGES = setOf(
        "com.whatsapp",
        "com.whatsapp.w4b",
    )

    private val VISUAL_PLACEHOLDERS: Map<VisualMediaKind, Set<String>> = mapOf(
        VisualMediaKind.PHOTO to setOf(
            "photo",
            "image",
            "picture",
            "zdjęcie",
            "zdjecie",
            "obraz",
            "picture message",
            "mms",
            "multimedia message",
        ),
        VisualMediaKind.GIF to setOf(
            "gif",
            "animated gif",
            "animowany gif",
        ),
        VisualMediaKind.STICKER to setOf(
            "sticker",
            "naklejka",
        ),
        VisualMediaKind.VIDEO to setOf(
            "video",
            "film",
            "wideo",
        ),
    )

    fun parse(packageName: String, extras: Bundle, postedAtMillis: Long): ParsedNotification? {
        val sourceApp = resolveSourceApp(packageName) ?: return null
        val title = extras.getCharSequence("android.title")?.toString()?.trim().orEmpty()
        val text = extractNotificationText(extras)

        val senderPhone = when (sourceApp) {
            MessageSourceApp.SMS -> extractSmsSender(title, extras) ?: title
            MessageSourceApp.WHATSAPP -> extractWhatsAppSender(title, extras) ?: title
        }

        if (senderPhone.isBlank()) return null

        val content = resolveContent(text, extras) ?: return null

        return ParsedNotification(
            senderPhone = senderPhone,
            content = content,
            timestamp = postedAtMillis,
            sourceApp = sourceApp,
        )
    }

    fun isSupportedPackage(packageName: String): Boolean =
        resolveSourceApp(packageName) != null

    internal fun resolveContent(text: String, extras: Bundle): MessageContent? {
        val trimmedText = text.trim()
        val hasPicture = hasPictureAttachment(extras)
        val placeholderKind = detectVisualPlaceholderKind(trimmedText)

        return when {
            hasPicture && trimmedText.isBlank() -> MessageContent.VisualOnly(VisualMediaKind.PHOTO)
            placeholderKind != null && isPlaceholderOnly(trimmedText, placeholderKind) ->
                MessageContent.VisualOnly(placeholderKind)
            hasPicture -> MessageContent.VisualWithText(VisualMediaKind.PHOTO, trimmedText)
            placeholderKind != null -> MessageContent.VisualOnly(placeholderKind)
            trimmedText.isNotBlank() -> MessageContent.TextOnly(trimmedText)
            else -> null
        }
    }

    internal fun detectVisualPlaceholderKind(text: String): VisualMediaKind? {
        val normalized = text.trim().lowercase()
        if (normalized.isBlank()) return null

        emojiPrefixKind(normalized)?.let { return it }

        return VISUAL_PLACEHOLDERS.entries.firstOrNull { (_, labels) ->
            labels.any { label -> normalized == label || normalized.endsWith(" $label") }
        }?.key
    }

    internal fun isPlaceholderOnly(text: String, kind: VisualMediaKind): Boolean {
        val normalized = text.trim().lowercase()
        if (normalized.isBlank()) return true
        emojiPrefixKind(normalized)?.let { return it == kind }
        val labels = VISUAL_PLACEHOLDERS.getValue(kind)
        return labels.any { label -> normalized == label || normalized.endsWith(" $label") }
    }

    private fun emojiPrefixKind(normalized: String): VisualMediaKind? = when {
        normalized.startsWith("📷") || normalized.startsWith("🖼") -> VisualMediaKind.PHOTO
        normalized.startsWith("🎬") || normalized.startsWith("🎞") -> VisualMediaKind.GIF
        normalized.contains("gif") -> VisualMediaKind.GIF
        normalized.startsWith("🎥") -> VisualMediaKind.VIDEO
        else -> null
    }

    private fun resolveSourceApp(packageName: String): MessageSourceApp? = when {
        packageName in SMS_PACKAGES -> MessageSourceApp.SMS
        packageName in WHATSAPP_PACKAGES -> MessageSourceApp.WHATSAPP
        else -> null
    }

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
        return ""
    }

    private fun hasPictureAttachment(extras: Bundle): Boolean =
        extras.containsKey("android.picture") ||
            extras.containsKey("android.pictureContentDescription") ||
            extras.getString("android.template")?.contains("BigPictureStyle", ignoreCase = true) == true

    private fun extractSmsSender(title: String, extras: Bundle): String? {
        extras.getString("android.subText")?.takeIf { it.isNotBlank() }?.let { return it }
        title.takeIf { it.isNotBlank() }?.let { return it }
        return null
    }

    private fun extractWhatsAppSender(title: String, extras: Bundle): String? {
        extras.getString("android.conversationTitle")?.takeIf { it.isNotBlank() }?.let { return it }
        title.takeIf { it.isNotBlank() }?.let { return it }
        return null
    }

    fun toRaw(parsed: ParsedNotification): RawIncomingMessage =
        RawIncomingMessage(
            senderPhone = parsed.senderPhone,
            content = parsed.content,
            timestamp = parsed.timestamp,
            sourceApp = parsed.sourceApp,
        )
}
