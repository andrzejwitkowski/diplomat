package pl.diplomat.usecase

import pl.diplomat.domain.model.ConversationRange
import pl.diplomat.domain.model.IncomingMessage
import pl.diplomat.domain.port.ConversationRangePort
import kotlinx.coroutines.flow.Flow

sealed interface RangeMarkAction {
    /** [keepEnd] when editing start — preserves end if same channel (reorders by time if needed). */
    data class SetStart(
        val message: IncomingMessage,
        val keepEnd: IncomingMessage? = null,
    ) : RangeMarkAction

    data class SetEnd(val message: IncomingMessage, val startMessage: IncomingMessage) : RangeMarkAction
    data object ClearStart : RangeMarkAction
    data object ClearEnd : RangeMarkAction
}

sealed interface ApplyRangeMarkResult {
    data class Applied(val range: ConversationRange?) : ApplyRangeMarkResult
    data object RejectedWrongChannel : ApplyRangeMarkResult
}

private fun orderedBounds(a: IncomingMessage, b: IncomingMessage): Pair<Long, Long> {
    val aFirst = a.timestamp < b.timestamp || (a.timestamp == b.timestamp && a.id <= b.id)
    return if (aFirst) a.id to b.id else b.id to a.id
}

fun applyRangeMark(
    contactId: Long,
    current: ConversationRange?,
    action: RangeMarkAction,
): ApplyRangeMarkResult = when (action) {
    is RangeMarkAction.SetStart -> {
        val keep = action.keepEnd?.takeIf { it.sourceApp == action.message.sourceApp }
        if (keep == null) {
            ApplyRangeMarkResult.Applied(
                ConversationRange(
                    contactId = contactId,
                    sourceApp = action.message.sourceApp,
                    startMessageId = action.message.id,
                    endMessageId = null,
                ),
            )
        } else {
            val (startId, endId) = orderedBounds(action.message, keep)
            ApplyRangeMarkResult.Applied(
                ConversationRange(
                    contactId = contactId,
                    sourceApp = action.message.sourceApp,
                    startMessageId = startId,
                    endMessageId = endId,
                ),
            )
        }
    }
    is RangeMarkAction.SetEnd -> {
        if (current?.startMessageId == null) {
            ApplyRangeMarkResult.Applied(current)
        } else if (action.message.sourceApp != current.sourceApp) {
            ApplyRangeMarkResult.RejectedWrongChannel
        } else {
            val (startId, endId) = orderedBounds(action.startMessage, action.message)
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
