package pl.diplomat.infrastructure.notification

import android.os.Bundle
import org.junit.Test
import pl.diplomat.domain.model.MessageContent
import pl.diplomat.domain.model.MessageSourceApp
import pl.diplomat.domain.model.VisualMediaKind
import pl.diplomat.domain.testsupport.TestConstants
import pl.diplomat.infrastructure.BaseSpec
import pl.diplomat.infrastructure.testsupport.NotificationTestConstants
import pl.diplomat.infrastructure.testsupport.ParsedNotificationAssertion
import pl.diplomat.infrastructure.testsupport.notificationExtras

class NotificationParserTest : BaseSpec() {

    @Test
    fun parsesSmsNotification() {
        val parsed = parseNotification(
            packageName = NotificationTestConstants.SMS_PACKAGE,
            extras = notificationExtras()
                .withTitle(NotificationTestConstants.SMS_SENDER)
                .withText(TestConstants.TEXT_SMS_BODY)
                .build(),
            postedAtMillis = NotificationTestConstants.TIMESTAMP_SMS,
        )

        ParsedNotificationAssertion.assertThat(parsed)
            .isNotNull()
            .hasSourceApp(MessageSourceApp.SMS)
            .hasContent(MessageContent.TextOnly(TestConstants.TEXT_SMS_BODY))
    }

    @Test
    fun parsesWhatsAppNotification() {
        val parsed = parseNotification(
            packageName = NotificationTestConstants.WHATSAPP_PACKAGE,
            extras = notificationExtras()
                .withTitle(NotificationTestConstants.WHATSAPP_SENDER)
                .withText(TestConstants.TEXT_WHATSAPP)
                .withConversationTitle(NotificationTestConstants.WHATSAPP_SENDER)
                .build(),
            postedAtMillis = NotificationTestConstants.TIMESTAMP_WHATSAPP,
        )

        ParsedNotificationAssertion.assertThat(parsed)
            .isNotNull()
            .hasSourceApp(MessageSourceApp.WHATSAPP)
            .hasSenderPhone(NotificationTestConstants.WHATSAPP_SENDER)
            .hasContent(MessageContent.TextOnly(TestConstants.TEXT_WHATSAPP))
    }

    @Test
    fun parsesPolishPhotoPlaceholderFromLocalizedResources() {
        val parsed = parseNotification(
            packageName = NotificationTestConstants.WHATSAPP_PACKAGE,
            extras = notificationExtras()
                .withTitle(NotificationTestConstants.WHATSAPP_SENDER)
                .withText(NotificationTestConstants.NOTIFICATION_TEXT_PHOTO_PL)
                .withConversationTitle(NotificationTestConstants.WHATSAPP_SENDER)
                .build(),
            postedAtMillis = NotificationTestConstants.TIMESTAMP_PHOTO,
        )

        ParsedNotificationAssertion.assertThat(parsed)
            .isNotNull()
            .hasVisualOnly(VisualMediaKind.PHOTO)
    }

    @Test
    fun parsesPhotoNotificationFromPlaceholderText() {
        val parsed = parseNotification(
            packageName = NotificationTestConstants.WHATSAPP_PACKAGE,
            extras = notificationExtras()
                .withTitle(NotificationTestConstants.WHATSAPP_SENDER)
                .withText(NotificationTestConstants.NOTIFICATION_TEXT_PHOTO)
                .withConversationTitle(NotificationTestConstants.WHATSAPP_SENDER)
                .build(),
            postedAtMillis = NotificationTestConstants.TIMESTAMP_PHOTO,
        )

        ParsedNotificationAssertion.assertThat(parsed)
            .isNotNull()
            .hasVisualOnly(VisualMediaKind.PHOTO)
    }

    @Test
    fun parsesGifNotificationFromPlaceholderText() {
        val parsed = parseNotification(
            packageName = NotificationTestConstants.WHATSAPP_PACKAGE,
            extras = notificationExtras()
                .withTitle(NotificationTestConstants.WHATSAPP_SENDER)
                .withText(NotificationTestConstants.NOTIFICATION_TEXT_GIF)
                .withConversationTitle(NotificationTestConstants.WHATSAPP_SENDER)
                .build(),
            postedAtMillis = NotificationTestConstants.TIMESTAMP_GIF,
        )

        ParsedNotificationAssertion.assertThat(parsed)
            .isNotNull()
            .hasVisualOnly(VisualMediaKind.GIF)
    }

    @Test
    fun parsesGifNotificationFromEmojiPrefix() {
        val parsed = parseNotification(
            packageName = NotificationTestConstants.WHATSAPP_PACKAGE,
            extras = notificationExtras()
                .withTitle(NotificationTestConstants.WHATSAPP_SENDER)
                .withText(NotificationTestConstants.NOTIFICATION_TEXT_GIF_EMOJI)
                .withConversationTitle(NotificationTestConstants.WHATSAPP_SENDER)
                .build(),
            postedAtMillis = NotificationTestConstants.TIMESTAMP_GIF_EMOJI,
        )

        ParsedNotificationAssertion.assertThat(parsed)
            .isNotNull()
            .hasVisualOnly(VisualMediaKind.GIF)
    }

    @Test
    fun parsesStickerNotification() {
        val parsed = parseNotification(
            packageName = NotificationTestConstants.WHATSAPP_PACKAGE,
            extras = notificationExtras()
                .withTitle(NotificationTestConstants.WHATSAPP_SENDER)
                .withText(NotificationTestConstants.NOTIFICATION_TEXT_STICKER)
                .withConversationTitle(NotificationTestConstants.WHATSAPP_SENDER)
                .build(),
            postedAtMillis = NotificationTestConstants.TIMESTAMP_STICKER,
        )

        ParsedNotificationAssertion.assertThat(parsed)
            .isNotNull()
            .hasVisualOnly(VisualMediaKind.STICKER)
    }

    @Test
    fun parsesVideoNotification() {
        val parsed = parseNotification(
            packageName = NotificationTestConstants.WHATSAPP_PACKAGE,
            extras = notificationExtras()
                .withTitle(NotificationTestConstants.WHATSAPP_SENDER)
                .withText(NotificationTestConstants.NOTIFICATION_TEXT_VIDEO)
                .withConversationTitle(NotificationTestConstants.WHATSAPP_SENDER)
                .build(),
            postedAtMillis = NotificationTestConstants.TIMESTAMP_VIDEO,
        )

        ParsedNotificationAssertion.assertThat(parsed)
            .isNotNull()
            .hasVisualOnly(VisualMediaKind.VIDEO)
    }

    @Test
    fun parsesImageWithCaptionNotification() {
        val parsed = parseNotification(
            packageName = NotificationTestConstants.WHATSAPP_PACKAGE,
            extras = notificationExtras()
                .withTitle(NotificationTestConstants.WHATSAPP_SENDER)
                .withText(TestConstants.TEXT_SUNSET_CAPTION)
                .withConversationTitle(NotificationTestConstants.WHATSAPP_SENDER)
                .withPicture()
                .build(),
            postedAtMillis = NotificationTestConstants.TIMESTAMP_IMAGE_CAPTION,
        )

        ParsedNotificationAssertion.assertThat(parsed)
            .isNotNull()
            .hasContent(
                MessageContent.VisualWithText(VisualMediaKind.PHOTO, TestConstants.TEXT_SUNSET_CAPTION),
            )
    }

    @Test
    fun parsesImageOnlyNotificationFromPictureExtra() {
        val parsed = parseNotification(
            packageName = NotificationTestConstants.SMS_PACKAGE,
            extras = notificationExtras()
                .withTitle(NotificationTestConstants.SMS_SENDER)
                .withPicture()
                .build(),
            postedAtMillis = NotificationTestConstants.TIMESTAMP_MMS_PICTURE,
        )

        ParsedNotificationAssertion.assertThat(parsed)
            .isNotNull()
            .hasVisualOnly(VisualMediaKind.PHOTO)
    }

    @Test
    fun parsesPhotoNotificationWithEmojiVariationSelector() {
        val parsed = parseNotification(
            packageName = NotificationTestConstants.WHATSAPP_PACKAGE,
            extras = notificationExtras()
                .withTitle(NotificationTestConstants.WHATSAPP_SENDER)
                .withText(NotificationTestConstants.NOTIFICATION_TEXT_PHOTO_VARIATION)
                .withConversationTitle(NotificationTestConstants.WHATSAPP_SENDER)
                .build(),
            postedAtMillis = NotificationTestConstants.TIMESTAMP_PHOTO,
        )

        ParsedNotificationAssertion.assertThat(parsed)
            .isNotNull()
            .hasVisualOnly(VisualMediaKind.PHOTO)
    }

    @Test
    fun ignoresUnsupportedPackage() {
        val parsed = parseNotification(
            packageName = NotificationTestConstants.UNKNOWN_PACKAGE,
            extras = notificationExtras()
                .withText(NotificationTestConstants.NOTIFICATION_TEXT_IGNORED)
                .build(),
            postedAtMillis = NotificationTestConstants.TIMESTAMP_IGNORED,
        )

        ParsedNotificationAssertion.assertThat(parsed).isNull()
    }

    @Test
    fun keepsEmojiPrefixedCaptionAsText() {
        val parsed = parseNotification(
            packageName = NotificationTestConstants.WHATSAPP_PACKAGE,
            extras = notificationExtras()
                .withTitle(NotificationTestConstants.WHATSAPP_SENDER)
                .withText(NotificationTestConstants.NOTIFICATION_TEXT_EMOJI_CAPTION)
                .withConversationTitle(NotificationTestConstants.WHATSAPP_SENDER)
                .build(),
            postedAtMillis = NotificationTestConstants.TIMESTAMP_PHOTO,
        )

        ParsedNotificationAssertion.assertThat(parsed)
            .isNotNull()
            .hasContent(MessageContent.TextOnly(NotificationTestConstants.NOTIFICATION_TEXT_EMOJI_CAPTION))
    }

    @Test
    fun doesNotTreatGiftTextAsGifPlaceholder() {
        val parsed = parseNotification(
            packageName = NotificationTestConstants.WHATSAPP_PACKAGE,
            extras = notificationExtras()
                .withTitle(NotificationTestConstants.WHATSAPP_SENDER)
                .withText(NotificationTestConstants.NOTIFICATION_TEXT_GIFT)
                .withConversationTitle(NotificationTestConstants.WHATSAPP_SENDER)
                .build(),
            postedAtMillis = NotificationTestConstants.TIMESTAMP_GIF,
        )

        ParsedNotificationAssertion.assertThat(parsed)
            .isNotNull()
            .hasContent(MessageContent.TextOnly(NotificationTestConstants.NOTIFICATION_TEXT_GIFT))
    }

    @Test
    fun doesNotTreatSentPhotoPhraseAsVisualPlaceholder() {
        val parsed = parseNotification(
            packageName = NotificationTestConstants.WHATSAPP_PACKAGE,
            extras = notificationExtras()
                .withTitle(NotificationTestConstants.WHATSAPP_SENDER)
                .withText(NotificationTestConstants.NOTIFICATION_TEXT_SENT_PHOTO)
                .withConversationTitle(NotificationTestConstants.WHATSAPP_SENDER)
                .build(),
            postedAtMillis = NotificationTestConstants.TIMESTAMP_PHOTO,
        )

        ParsedNotificationAssertion.assertThat(parsed)
            .isNotNull()
            .hasContent(MessageContent.TextOnly(NotificationTestConstants.NOTIFICATION_TEXT_SENT_PHOTO))
    }
}
