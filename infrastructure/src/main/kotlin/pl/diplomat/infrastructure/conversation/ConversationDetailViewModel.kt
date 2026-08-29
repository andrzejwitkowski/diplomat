package pl.diplomat.infrastructure.conversation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import pl.diplomat.domain.model.ChannelMessageGroup
import pl.diplomat.domain.model.ChatMessage
import pl.diplomat.domain.model.ConversationRange
import pl.diplomat.domain.model.IncomingMessage
import pl.diplomat.domain.model.Sentiment
import pl.diplomat.domain.model.WhitelistedContact
import pl.diplomat.domain.model.toChatMessages
import pl.diplomat.domain.port.LlmCompletionResult
import pl.diplomat.usecase.ApplyRangeMarkResult
import pl.diplomat.usecase.MarkConversationAsReadUseCase
import pl.diplomat.usecase.ObserveContactMessagesUseCase
import pl.diplomat.usecase.ObserveConversationRangeUseCase
import pl.diplomat.usecase.RangeMarkAction
import pl.diplomat.usecase.SendConversationToModelUseCase
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

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

sealed interface SuggestionOutcome {
    data class Success(val text: String) : SuggestionOutcome
    data class Failure(val message: String) : SuggestionOutcome
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
    private val sendConversationToModel: SendConversationToModelUseCase,
) : ViewModel() {

    private val markMode = MutableStateFlow(MarkMode.Idle)
    private var continueToEndAfterStart = true
    private val _events = MutableSharedFlow<ConversationDetailEvent>(extraBufferCapacity = 1)

    val sentiment = MutableStateFlow(Sentiment.POSITIVE)
    var desiredAnswer by mutableStateOf("")
        private set
    var isSubmitting by mutableStateOf(false)
        private set
    var lastOutcome by mutableStateOf<SuggestionOutcome>(SuggestionOutcome.Success(""))
        private set
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

    private fun contentOrNull(): ConversationDetailUiState.Content? =
        uiState.value as? ConversationDetailUiState.Content

    private fun findMessage(id: Long): IncomingMessage? =
        contentOrNull()
            ?.channelGroups
            ?.asSequence()
            ?.flatMap { it.messages.asSequence() }
            ?.find { it.id == id }

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
        val startId = contentOrNull()?.range?.startMessageId
        if (startId == null) {
            continueToEndAfterStart = true
            markMode.value = MarkMode.PickingStart
        } else {
            markMode.value = MarkMode.PickingEnd
        }
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
                val keepEnd = if (!continueToEndAfterStart) {
                    contentOrNull()?.range?.endMessageId?.let(::findMessage)
                } else {
                    null
                }
                updateConversationRange(
                    contact.id,
                    RangeMarkAction.SetStart(message, keepEnd = keepEnd),
                )
                markMode.value =
                    if (continueToEndAfterStart) MarkMode.PickingEnd else MarkMode.Idle
            }
            MarkMode.PickingEnd -> {
                val startId = contentOrNull()?.range?.startMessageId
                val startMessage = startId?.let(::findMessage)
                if (startMessage == null) {
                    markMode.value = MarkMode.Idle
                    return
                }
                when (updateConversationRange(contact.id, RangeMarkAction.SetEnd(message, startMessage))) {
                    ApplyRangeMarkResult.RejectedWrongChannel ->
                        viewModelScope.launch { _events.emit(ConversationDetailEvent.WrongChannel) }
                    is ApplyRangeMarkResult.Applied -> markMode.value = MarkMode.Idle
                }
            }
        }
    }

    fun selectSentiment(sentiment: Sentiment) {
        this.sentiment.value = sentiment
    }

    fun updateDesiredAnswer(text: String) {
        desiredAnswer = text
    }

    fun submitSuggestion() {
        if (isSubmitting) return
        isSubmitting = true
        viewModelScope.launch {
            val result = runCatching {
                sendConversationToModel(
                    systemPrompt = SUGGEST_ANSWER_SYSTEM_PROMPT,
                    sentiment = sentiment.value,
                    desiredAnswer = if (desiredAnswer.isBlank()) null else desiredAnswer,
                    conversation = gatherConversationMessages(),
                )
            }.getOrNull() ?: LlmCompletionResult.Failure("Request failed")

            lastOutcome = when (result) {
                is LlmCompletionResult.Success -> SuggestionOutcome.Success(result.text)
                is LlmCompletionResult.Failure -> SuggestionOutcome.Failure(result.message)
            }
            isSubmitting = false
        }
    }

    private fun gatherConversationMessages(): List<ChatMessage> {
        val content = contentOrNull() ?: return emptyList()
        val range = content.range
        val all = content.channelGroups.flatMap { it.messages }
        val startId = range?.startMessageId
        val endId = range?.endMessageId
        val scoped = if (startId != null && endId != null) {
            all.filter { it.id in startId..endId }
        } else {
            all
        }
        return scoped.toChatMessages()
    }

    private companion object {
        const val SUGGEST_ANSWER_SYSTEM_PROMPT =
            "Zaproponuj odpowiedz na konwersacie zgodnie z sentymentem"
    }
}
