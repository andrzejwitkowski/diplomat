package pl.diplomat.presentation.conversation

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import pl.diplomat.domain.model.ConversationRange
import pl.diplomat.domain.model.IncomingMessage
import pl.diplomat.domain.model.MessageSourceApp
import pl.diplomat.domain.model.MessageStatus
import pl.diplomat.presentation.R
import pl.diplomat.presentation.dashboard.ChannelBadge
import pl.diplomat.presentation.dashboard.MessageStatusLabel
import pl.diplomat.presentation.message.previewText
import java.text.DateFormat
import java.util.Date

internal val StartBorder = Color(0xFF15803D)
internal val EndBorder = Color(0xFFB91C1C)
private val InRangeTint = Color(0xFF1E3A2E)
private val InRangeTintContent = Color(0xFFDCFCE7)

internal enum class RangeRole { None, Start, End, Interior }

internal fun rangeRole(
    message: IncomingMessage,
    range: ConversationRange?,
    allMessages: List<IncomingMessage>,
): RangeRole {
    if (range == null) return RangeRole.None
    if (message.id == range.startMessageId) return RangeRole.Start
    if (message.id == range.endMessageId) return RangeRole.End
    if (!range.isComplete || message.sourceApp != range.sourceApp) return RangeRole.None
    val start = allMessages.find { it.id == range.startMessageId } ?: return RangeRole.None
    val end = allMessages.find { it.id == range.endMessageId } ?: return RangeRole.None
    val afterStart = message.timestamp > start.timestamp ||
        (message.timestamp == start.timestamp && message.id > start.id)
    val beforeEnd = message.timestamp < end.timestamp ||
        (message.timestamp == end.timestamp && message.id < end.id)
    return if (afterStart && beforeEnd) RangeRole.Interior else RangeRole.None
}

@Composable
internal fun ConversationMessageBubble(
    message: IncomingMessage,
    contactName: String,
    role: RangeRole,
    markModeActive: Boolean,
    onMessageClick: () -> Unit,
    onEditStart: () -> Unit,
    onEditEnd: () -> Unit,
    onDeleteStart: () -> Unit,
    onDeleteEnd: () -> Unit,
) {
    val alignment = if (message.isOutgoing) Alignment.CenterEnd else Alignment.CenterStart
    val bubbleColor = when {
        role == RangeRole.Interior -> InRangeTint
        message.isOutgoing -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    val textColor = when {
        role == RangeRole.Interior -> InRangeTintContent
        message.isOutgoing -> MaterialTheme.colorScheme.onPrimaryContainer
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val formattedTime = DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(message.timestamp))
    val directionLabel = if (message.isOutgoing) {
        stringResource(R.string.message_from_you)
    } else {
        stringResource(R.string.message_from_contact, contactName)
    }

    var menuExpanded by remember { mutableStateOf(false) }
    val borderColor = when (role) {
        RangeRole.Start -> StartBorder
        RangeRole.End -> EndBorder
        else -> null
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = directionLabel },
        contentAlignment = alignment,
    ) {
        Surface(
            modifier = Modifier
                .widthIn(max = 300.dp)
                .then(
                    if (borderColor != null) Modifier.border(2.dp, borderColor, RoundedCornerShape(16.dp))
                    else Modifier,
                )
                .then(
                    when {
                        markModeActive -> Modifier.clickable(onClick = onMessageClick)
                        role == RangeRole.Start || role == RangeRole.End ->
                            Modifier.clickable { menuExpanded = true }
                        else -> Modifier
                    },
                ),
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (message.isOutgoing) 16.dp else 4.dp,
                bottomEnd = if (message.isOutgoing) 4.dp else 16.dp,
            ),
            color = bubbleColor,
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                when (role) {
                    RangeRole.Start -> Text(
                        text = stringResource(R.string.conversation_mark_label_start),
                        style = MaterialTheme.typography.labelSmall,
                        color = StartBorder,
                    )
                    RangeRole.End -> Text(
                        text = stringResource(R.string.conversation_mark_label_end),
                        style = MaterialTheme.typography.labelSmall,
                        color = EndBorder,
                    )
                    else -> Unit
                }
                if (!message.isOutgoing) {
                    MessageChannelStatusBadges(
                        status = message.status,
                        sourceApp = message.sourceApp,
                    )
                }
                Text(
                    text = message.content.previewText(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = textColor,
                )
                Text(
                    text = formattedTime,
                    style = MaterialTheme.typography.labelSmall,
                    color = textColor.copy(alpha = 0.7f),
                    modifier = Modifier.align(Alignment.End),
                )
            }
        }

        DropdownMenu(
            expanded = menuExpanded,
            onDismissRequest = { menuExpanded = false },
        ) {
            when (role) {
                RangeRole.Start -> {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.conversation_mark_edit_start)) },
                        onClick = {
                            menuExpanded = false
                            onEditStart()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.conversation_mark_delete_start)) },
                        onClick = {
                            menuExpanded = false
                            onDeleteStart()
                        },
                    )
                }
                RangeRole.End -> {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.conversation_mark_edit_end)) },
                        onClick = {
                            menuExpanded = false
                            onEditEnd()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.conversation_mark_delete_end)) },
                        onClick = {
                            menuExpanded = false
                            onDeleteEnd()
                        },
                    )
                }
                else -> Unit
            }
        }
    }
}

@Composable
internal fun MessageChannelStatusBadges(
    status: MessageStatus,
    sourceApp: MessageSourceApp,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ChannelBadge(sourceApp = sourceApp)
        MessageStatusLabel(status = status)
    }
}
