package pl.diplomat.presentation.conversation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import pl.diplomat.domain.model.Sentiment
import pl.diplomat.presentation.R

private val SentimentPositiveAccent = Color(0xFF43A047)
private val SentimentNeutralAccent = Color(0xFF9E9E9E)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SentimentSelector(
    selected: Sentiment,
    onSelect: (Sentiment) -> Unit,
    modifier: Modifier = Modifier,
) {
    val errorAccent = MaterialTheme.colorScheme.error
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Sentiment.entries.forEach { option ->
            val (labelRes, a11yRes, accent) = when (option) {
                Sentiment.POSITIVE -> Triple(
                    R.string.sentiment_positive,
                    R.string.sentiment_positive_a11y,
                    SentimentPositiveAccent,
                )
                Sentiment.NEUTRAL -> Triple(
                    R.string.sentiment_neutral,
                    R.string.sentiment_neutral_a11y,
                    SentimentNeutralAccent,
                )
                Sentiment.NEGATIVE -> Triple(
                    R.string.sentiment_negative,
                    R.string.sentiment_negative_a11y,
                    errorAccent,
                )
            }
            val isSelected = option == selected
            val contentDesc = stringResource(a11yRes)
            Surface(
                onClick = { onSelect(option) },
                modifier = Modifier
                    .weight(1f)
                    .semantics { contentDescription = contentDesc },
                shape = RoundedCornerShape(12.dp),
                color = accent.copy(alpha = if (isSelected) 0.22f else 0.10f),
                border = if (isSelected) BorderStroke(1.dp, accent.copy(alpha = 0.40f)) else null,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp, horizontal = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(accent, CircleShape),
                    )
                    Text(
                        text = stringResource(labelRes),
                        style = MaterialTheme.typography.labelSmall,
                        color = accent,
                    )
                }
            }
        }
    }
}
