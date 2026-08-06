package pl.diplomat.presentation.message

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import pl.diplomat.domain.model.MessageContent
import pl.diplomat.domain.model.VisualMediaKind
import pl.diplomat.presentation.R

@Composable
fun MessageContent.previewText(): String = when (this) {
    is MessageContent.TextOnly -> body
    is MessageContent.VisualOnly -> kind.previewLabel()
    is MessageContent.VisualWithText -> kind.previewWithTextLabel(body)
}

fun MessageContent.hasVisualMedia(): Boolean =
    this is MessageContent.VisualOnly || this is MessageContent.VisualWithText

@Composable
private fun VisualMediaKind.previewLabel(): String = stringResource(previewLabelRes())

@Composable
private fun VisualMediaKind.previewWithTextLabel(caption: String): String =
    stringResource(previewWithTextLabelRes(), caption)

private fun VisualMediaKind.previewLabelRes(): Int = when (this) {
    VisualMediaKind.PHOTO -> R.string.message_preview_image
    VisualMediaKind.GIF -> R.string.message_preview_gif
    VisualMediaKind.STICKER -> R.string.message_preview_sticker
    VisualMediaKind.VIDEO -> R.string.message_preview_video
}

private fun VisualMediaKind.previewWithTextLabelRes(): Int = when (this) {
    VisualMediaKind.PHOTO -> R.string.message_preview_image_with_text
    VisualMediaKind.GIF -> R.string.message_preview_gif_with_text
    VisualMediaKind.STICKER -> R.string.message_preview_sticker_with_text
    VisualMediaKind.VIDEO -> R.string.message_preview_video_with_text
}
