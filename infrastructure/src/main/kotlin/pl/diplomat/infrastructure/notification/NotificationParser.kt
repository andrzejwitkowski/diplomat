package pl.diplomat.infrastructure.notification

import android.os.Bundle
import pl.diplomat.domain.model.MessageSourceApp
import pl.diplomat.usecase.RawIncomingMessage

data class ParsedNotification(
    val senderPhone: String,
    val text: String,
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

    fun parse(packageName: String, extras: Bundle, postedAtMillis: Long): ParsedNotification? {
        val sourceApp = resolveSourceApp(packageName) ?: return null
        val title = extras.getCharSequence("android.title")?.toString()?.trim().orEmpty()
        val text = extras.getCharSequence("android.text")?.toString()?.trim().orEmpty()
        if (text.isBlank()) return null

        val senderPhone = when (sourceApp) {
            MessageSourceApp.SMS -> extractSmsSender(title, extras) ?: title
            MessageSourceApp.WHATSAPP -> extractWhatsAppSender(title, extras) ?: title
        }

        if (senderPhone.isBlank()) return null

        return ParsedNotification(
            senderPhone = senderPhone,
            text = text,
            timestamp = postedAtMillis,
            sourceApp = sourceApp,
        )
    }

    fun isSupportedPackage(packageName: String): Boolean =
        resolveSourceApp(packageName) != null

    private fun resolveSourceApp(packageName: String): MessageSourceApp? = when {
        packageName in SMS_PACKAGES -> MessageSourceApp.SMS
        packageName in WHATSAPP_PACKAGES -> MessageSourceApp.WHATSAPP
        else -> null
    }

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
            text = parsed.text,
            timestamp = parsed.timestamp,
            sourceApp = parsed.sourceApp,
        )
}
