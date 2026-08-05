package com.diplomat.ui.decision

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.diplomat.DiplomatApplication
import com.diplomat.data.repository.MessageRepository
import com.diplomat.domain.model.InterceptedMessage
import com.diplomat.ui.navigation.DiplomatDestinations
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Drives the decision screen: user agreement, reasoning, draft generation and
 * dispatch for a single captured message.
 */
class DecisionViewModel(
    private val repository: MessageRepository,
    private val messageId: Long,
) : ViewModel() {

    val message: StateFlow<InterceptedMessage?> =
        repository.observeMessage(messageId)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = null,
            )

    private val _uiState = MutableStateFlow(DecisionUiState())
    val uiState: StateFlow<DecisionUiState> = _uiState.asStateFlow()

    init {
        // Seed the editable fields from persisted values the first time the
        // message loads.
        message.onEach { loaded ->
            if (loaded != null && !_uiState.value.seeded) {
                _uiState.update {
                    it.copy(
                        agreement = loaded.userAgreement ?: false,
                        reasoning = loaded.userReasoning.orEmpty(),
                        draft = loaded.draftResponse.orEmpty(),
                        seeded = true,
                    )
                }
            }
        }.launchIn(viewModelScope)
    }

    fun setAgreement(value: Boolean) = _uiState.update { it.copy(agreement = value) }

    fun setReasoning(value: String) = _uiState.update { it.copy(reasoning = value) }

    fun editDraft(value: String) = _uiState.update { it.copy(draft = value) }

    fun generateDraft() {
        val state = _uiState.value
        _uiState.update { it.copy(isGenerating = true, error = null) }
        viewModelScope.launch {
            val result = repository.requestDraft(
                id = messageId,
                userAgreement = state.agreement,
                userReasoning = state.reasoning,
            )
            _uiState.update { current ->
                result.fold(
                    onSuccess = { updated ->
                        current.copy(
                            isGenerating = false,
                            draft = updated.draftResponse.orEmpty(),
                        )
                    },
                    onFailure = { throwable ->
                        current.copy(
                            isGenerating = false,
                            error = throwable.message ?: "Failed to generate reply",
                        )
                    },
                )
            }
        }
    }

    fun approveAndSend(onReadyToDispatch: (InterceptedMessage) -> Unit) {
        val current = message.value ?: return
        viewModelScope.launch {
            repository.updateDraft(messageId, _uiState.value.draft)
            repository.markSent(messageId)
            onReadyToDispatch(current.copy(draftResponse = _uiState.value.draft))
        }
    }

    fun ignore() {
        viewModelScope.launch { repository.markIgnored(messageId) }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as DiplomatApplication
                val savedState: SavedStateHandle = createSavedStateHandle()
                val id = savedState.get<String>(DiplomatDestinations.DECISION_ARG_ID)
                    ?.toLongOrNull()
                    ?: error("Missing ${DiplomatDestinations.DECISION_ARG_ID} argument")
                DecisionViewModel(app.container.messageRepository, id)
            }
        }
    }
}

/**
 * UI state for the decision screen.
 */
data class DecisionUiState(
    val agreement: Boolean = false,
    val reasoning: String = "",
    val draft: String = "",
    val isGenerating: Boolean = false,
    val error: String? = null,
    val seeded: Boolean = false,
)
