package pl.diplomat.infrastructure.conversation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import pl.diplomat.domain.model.ChannelMessageGroup
import pl.diplomat.domain.model.ConversationRange
import pl.diplomat.domain.model.IncomingMessage
import pl.diplomat.domain.model.WhitelistedContact
import pl.diplomat.usecase.ApplyRangeMarkResult
import pl.diplomat.usecase.MarkConversationAsReadUseCase
import pl.diplomat.usecase.ObserveContactMessagesUseCase
import pl.diplomat.usecase.ObserveConversationRangeUseCase
import pl.diplomat.usecase.RangeMarkAction
import pl.diplomat.usecase.UpdateConversationRangeUseCase
import pl.diplomat.usecase.groupMessagesByChannel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class MarkMode {
    Idle,
    PickingStart,
    PickingEnd,
}

sealed interface ConversationDetailUiState {
    data object Loading : ConversationDetailUiState
    data class Content(
        val contact: WhitelistedContact,
        val channelGroups: List<ChannelMessageGroup>,
        val range: ConversationRange?,
        val markMode: MarkMode,
    ) : ConversationDetailUiState
}

sealed interface ConversationDetailEvent {
    data object WrongChannel : ConversationDetailEvent
}

class ConversationDetailViewModel(
    private val contact: WhitelistedContact,
    observeContactMessages: ObserveContactMessagesUseCase,
    private val markConversationAsRead: MarkConversationAsReadUseCase,
    observeConversationRange: ObserveConversationRangeUseCase,
    private val updateConversationRange: UpdateConversationRangeUseCase,
) : ViewModel() {

    private val markMode = MutableStateFlow(MarkMode.Idle)
    private var continueToEndAfterStart = true
    private val _events = MutableSharedFlow<ConversationDetailEvent>(extraBufferCapacity = 1)
    val events = _events.asSharedFlow()

    val uiState: StateFlow<ConversationDetailUiState> =
        combine(
            observeContactMessages(contact.id),
            observeConversationRange(contact.id),
            markMode,
        ) { messageList, range, mode ->
            ConversationDetailUiState.Content(
                contact = contact,
                channelGroups = groupMessagesByChannel(messageList),
                range = range,
                markMode = mode,
            )
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            ConversationDetailUiState.Loading,
        )

    init {
        viewModelScope.launch {
            markConversationAsRead(contact.id)
        }
    }

    fun enterMarkMode() {
        continueToEndAfterStart = true
        markMode.value = MarkMode.PickingStart
    }

    fun cancelMarkMode() {
        markMode.value = MarkMode.Idle
    }

    fun editStart() {
        continueToEndAfterStart = false
        markMode.value = MarkMode.PickingStart
    }

    fun editEnd() {
        markMode.value = MarkMode.PickingEnd
    }

    fun deleteStart() {
        updateConversationRange(contact.id, RangeMarkAction.ClearStart)
        markMode.value = MarkMode.Idle
    }

    fun deleteEnd() {
        updateConversationRange(contact.id, RangeMarkAction.ClearEnd)
        markMode.value = MarkMode.Idle
    }

    fun onMessageClick(message: IncomingMessage) {
        when (markMode.value) {
            MarkMode.Idle -> Unit
            MarkMode.PickingStart -> {
                updateConversationRange(contact.id, RangeMarkAction.SetStart(message))
                markMode.value =
                    if (continueToEndAfterStart) MarkMode.PickingEnd else MarkMode.Idle
            }
            MarkMode.PickingEnd -> {
                val content = uiState.value as? ConversationDetailUiState.Content ?: return
                val startId = content.range?.startMessageId ?: return
                val startMessage = content.channelGroups
                    .asSequence()
                    .flatMap { it.messages.asSequence() }
                    .find { it.id == startId }
                    ?: return
                when (updateConversationRange(contact.id, RangeMarkAction.SetEnd(message, startMessage))) {
                    ApplyRangeMarkResult.RejectedWrongChannel ->
                        viewModelScope.launch { _events.emit(ConversationDetailEvent.WrongChannel) }
                    is ApplyRangeMarkResult.Applied -> markMode.value = MarkMode.Idle
                }
            }
        }
    }
}
