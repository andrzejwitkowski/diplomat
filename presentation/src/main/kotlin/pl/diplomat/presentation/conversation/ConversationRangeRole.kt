package pl.diplomat.presentation.conversation

import pl.diplomat.domain.model.ConversationRange
import pl.diplomat.domain.model.IncomingMessage

internal enum class RangeRole { None, Start, End, Interior }

internal fun rangeRoles(
    range: ConversationRange?,
    channelMessages: List<IncomingMessage>,
): List<RangeRole> {
    if (range == null || channelMessages.isEmpty()) {
        return List(channelMessages.size) { RangeRole.None }
    }

    val startIndex = range.startMessageId?.let { id -> channelMessages.indexOfFirst { it.id == id } } ?: -1
    val endIndex = range.endMessageId?.let { id -> channelMessages.indexOfFirst { it.id == id } } ?: -1

    if (!range.isComplete || startIndex < 0 || endIndex < 0) {
        return channelMessages.map { message ->
            when (message.id) {
                range.startMessageId -> RangeRole.Start
                range.endMessageId -> RangeRole.End
                else -> RangeRole.None
            }
        }
    }

    val lo = minOf(startIndex, endIndex)
    val hi = maxOf(startIndex, endIndex)
    return channelMessages.mapIndexed { index, message ->
        when {
            message.id == range.startMessageId -> RangeRole.Start
            message.id == range.endMessageId -> RangeRole.End
            message.sourceApp != range.sourceApp -> RangeRole.None
            index in (lo + 1) until hi -> RangeRole.Interior
            else -> RangeRole.None
        }
    }
}
