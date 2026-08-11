package pl.diplomat.usecase

import pl.diplomat.domain.model.ConversationRange
import pl.diplomat.domain.model.IncomingMessage
import pl.diplomat.domain.port.ConversationRangePort
import kotlinx.coroutines.flow.Flow

sealed interface RangeMarkAction {
    data class SetStart(val message: IncomingMessage) : RangeMarkAction
    data class SetEnd(val message: IncomingMessage, val startMessage: IncomingMessage) : RangeMarkAction
    data object ClearStart : RangeMarkAction
    data object ClearEnd : RangeMarkAction
}

sealed interface ApplyRangeMarkResult {
    data class Applied(val range: ConversationRange?) : ApplyRangeMarkResult
    data object RejectedWrongChannel : ApplyRangeMarkResult
}

fun applyRangeMark(
    contactId: Long,
    current: ConversationRange?,
    action: RangeMarkAction,
): ApplyRangeMarkResult = when (action) {
    is RangeMarkAction.SetStart -> ApplyRangeMarkResult.Applied(
        ConversationRange(
            contactId = contactId,
            sourceApp = action.message.sourceApp,
            startMessageId = action.message.id,
            endMessageId = null,
        ),
    )
    is RangeMarkAction.SetEnd -> {
        if (current?.startMessageId == null) {
            ApplyRangeMarkResult.Applied(current)
        } else if (action.message.sourceApp != current.sourceApp) {
            ApplyRangeMarkResult.RejectedWrongChannel
        } else {
            val start = action.startMessage
            val end = action.message
            val orderedEarlier = end.timestamp < start.timestamp ||
                (end.timestamp == start.timestamp && end.id < start.id)
            val (startId, endId) = if (orderedEarlier) end.id to start.id else start.id to end.id
            ApplyRangeMarkResult.Applied(
                current.copy(startMessageId = startId, endMessageId = endId),
            )
        }
    }
    RangeMarkAction.ClearStart -> ApplyRangeMarkResult.Applied(
        current?.copy(startMessageId = null)?.takeIf { it.endMessageId != null },
    )
    RangeMarkAction.ClearEnd -> ApplyRangeMarkResult.Applied(
        current?.copy(endMessageId = null)?.takeIf { it.startMessageId != null },
    )
}

class ObserveConversationRangeUseCase(
    private val rangePort: ConversationRangePort,
) {
    operator fun invoke(contactId: Long): Flow<ConversationRange?> =
        rangePort.observe(contactId)
}

class UpdateConversationRangeUseCase(
    private val rangePort: ConversationRangePort,
) {
    operator fun invoke(contactId: Long, action: RangeMarkAction): ApplyRangeMarkResult {
        val result = applyRangeMark(contactId, rangePort.get(contactId), action)
        if (result is ApplyRangeMarkResult.Applied) {
            val range = result.range
            if (range == null) rangePort.clear(contactId) else rangePort.set(range)
        }
        return result
    }
}
