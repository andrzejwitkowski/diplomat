package pl.diplomat.presentation.conversation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import pl.diplomat.domain.model.Sentiment
import pl.diplomat.infrastructure.conversation.SuggestionOutcome
import pl.diplomat.presentation.R
import pl.diplomat.presentation.copyPlainTextToClipboard

@Composable
internal fun SuggestionComposer(
    sentiment: Sentiment,
    onSentimentSelect: (Sentiment) -> Unit,
    desiredAnswer: String,
    onDesiredAnswerChange: (String) -> Unit,
    onSubmit: () -> Unit,
    isSubmitting: Boolean,
    lastOutcome: SuggestionOutcome,
) {
    val context = LocalContext.current
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp, bottom = 8.dp),
    ) {
        Text(
            text = stringResource(R.string.suggestion_composer_title),
            style = MaterialTheme.typography.titleMedium,
            color = colors.primary,
            modifier = Modifier.padding(bottom = 8.dp),
        )

        Text(
            text = stringResource(R.string.suggestion_sentiment_label),
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(bottom = 6.dp),
        )
        SentimentSelector(
            selected = sentiment,
            onSelect = onSentimentSelect,
            modifier = Modifier.padding(bottom = 8.dp),
        )

        TextField(
            value = desiredAnswer,
            onValueChange = onDesiredAnswerChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.suggestion_desired_answer)) },
            minLines = 3,
        )

        Button(
            onClick = onSubmit,
            enabled = !isSubmitting,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = if (isSubmitting) stringResource(R.string.suggestion_submitting)
                else stringResource(R.string.suggestion_submit),
            )
        }

        when (lastOutcome) {
            is SuggestionOutcome.Failure -> {
                Text(
                    text = stringResource(R.string.suggestion_failed, lastOutcome.message),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.error,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            is SuggestionOutcome.Success -> {
                val suggestedText = lastOutcome.text
                if (suggestedText.isNotBlank()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(R.string.suggestion_result),
                            style = MaterialTheme.typography.labelMedium,
                            color = colors.primary,
                        )
                        Row {
                            IconButton(
                                onClick = {
                                    copyPlainTextToClipboard(
                                        context,
                                        context.getString(R.string.suggestion_result),
                                        suggestedText,
                                        R.string.suggestion_copied,
                                    )
                                },
                            ) {
                                Icon(
                                    Icons.Filled.ContentCopy,
                                    contentDescription = stringResource(R.string.suggestion_copy),
                                )
                            }
                            IconButton(
                                onClick = onSubmit,
                                enabled = !isSubmitting,
                            ) {
                                Icon(
                                    Icons.Filled.Refresh,
                                    contentDescription = stringResource(R.string.suggestion_regenerate),
                                )
                            }
                        }
                    }
                    OutlinedTextField(
                        value = suggestedText,
                        onValueChange = {},
                        modifier = Modifier.fillMaxWidth(),
                        enabled = false,
                    )
                }
            }
        }
    }
}
