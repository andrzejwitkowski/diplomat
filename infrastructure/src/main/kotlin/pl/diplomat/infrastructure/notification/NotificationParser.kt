package pl.diplomat.infrastructure.notification

import android.os.Bundle
import pl.diplomat.domain.model.MessageContent
import pl.diplomat.domain.model.MessageSourceApp
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

    private val IMAGE_PLACEHOLDER_TEXT = setOf(
        "photo",
        "image",
        "picture",
        "zdjęcie",
        "zdjecie",
        "obraz",
        "picture message",
        "mms",
        "multimedia message",
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

        return when {
            hasPicture && trimmedText.isBlank() -> MessageContent.ImageOnly
            hasPicture && isImagePlaceholder(trimmedText) -> MessageContent.ImageOnly
            hasPicture -> MessageContent.ImageWithText(trimmedText)
            trimmedText.isNotBlank() && isImagePlaceholder(trimmedText) -> MessageContent.ImageOnly
            trimmedText.isNotBlank() -> MessageContent.TextOnly(trimmedText)
            else -> null
        }
    }

    internal fun isImagePlaceholder(text: String): Boolean {
        val normalized = text.trim().lowercase()
        if (normalized.isBlank()) return false
        if (normalized.startsWith("📷") || normalized.startsWith("🖼")) return true
        return IMAGE_PLACEHOLDER_TEXT.any { normalized == it || normalized.endsWith(" $it") }
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
