package pl.diplomat.infrastructure.conversation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import pl.diplomat.domain.model.ChannelMessageGroup
import pl.diplomat.domain.model.WhitelistedContact
import pl.diplomat.usecase.MarkConversationAsReadUseCase
import pl.diplomat.usecase.ObserveContactMessagesUseCase
import pl.diplomat.usecase.groupMessagesByChannel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface ConversationDetailUiState {
    data object Loading : ConversationDetailUiState
    data class Content(
        val contact: WhitelistedContact,
        val channelGroups: List<ChannelMessageGroup>,
    ) : ConversationDetailUiState
}

class ConversationDetailViewModel(
    private val contact: WhitelistedContact,
    observeContactMessages: ObserveContactMessagesUseCase,
    private val markConversationAsRead: MarkConversationAsReadUseCase,
) : ViewModel() {

    val uiState: StateFlow<ConversationDetailUiState> =
        observeContactMessages(contact.id)
            .map { messages ->
                ConversationDetailUiState.Content(
                    contact = contact,
                    channelGroups = groupMessagesByChannel(messages),
                )
            }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                ConversationDetailUiState.Loading,
            )

    init {
        viewModelScope.launch {
            markConversationAsRead(contact.id)
        }
    }
}
