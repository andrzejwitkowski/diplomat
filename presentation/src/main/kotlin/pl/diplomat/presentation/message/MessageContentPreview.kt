package pl.diplomat.presentation.message

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import pl.diplomat.domain.model.MessageContent
import pl.diplomat.presentation.R

@Composable
fun MessageContent.previewText(): String = when (this) {
    is MessageContent.TextOnly -> body
    is MessageContent.ImageOnly -> stringResource(R.string.message_preview_image)
    is MessageContent.ImageWithText -> stringResource(R.string.message_preview_image_with_text, body)
}

fun MessageContent.hasImage(): Boolean = this is MessageContent.ImageOnly || this is MessageContent.ImageWithText
