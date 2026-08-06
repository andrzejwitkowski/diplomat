package pl.diplomat.infrastructure.testsupport

object NotificationTestConstants {
    const val SMS_PACKAGE = "com.google.android.apps.messaging"
    const val WHATSAPP_PACKAGE = "com.whatsapp"
    const val UNKNOWN_PACKAGE = "com.example.unknown"

    const val WHATSAPP_SENDER = "Alice"
    const val SMS_SENDER = "+48 123 456 789"

    const val NOTIFICATION_TEXT_PHOTO = "📷 Photo"
    const val NOTIFICATION_TEXT_GIF = "GIF"
    const val NOTIFICATION_TEXT_GIF_EMOJI = "🎬 GIF"
    const val NOTIFICATION_TEXT_STICKER = "Sticker"
    const val NOTIFICATION_TEXT_VIDEO = "Video"
    const val NOTIFICATION_TEXT_IGNORED = "Ignored"

    const val TIMESTAMP_SMS = 1_000L
    const val TIMESTAMP_WHATSAPP = 2_000L
    const val TIMESTAMP_PHOTO = 2_500L
    const val TIMESTAMP_GIF = 2_550L
    const val TIMESTAMP_GIF_EMOJI = 2_560L
    const val TIMESTAMP_STICKER = 2_570L
    const val TIMESTAMP_VIDEO = 2_580L
    const val TIMESTAMP_IMAGE_CAPTION = 2_600L
    const val TIMESTAMP_MMS_PICTURE = 2_700L
    const val TIMESTAMP_IGNORED = 3_000L
}
