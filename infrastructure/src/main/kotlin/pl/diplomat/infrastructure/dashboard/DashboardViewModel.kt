package pl.diplomat.infrastructure.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import pl.diplomat.domain.model.ConversationThread
import pl.diplomat.usecase.GetActiveConversationsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

sealed interface DashboardUiState {
    data object Loading : DashboardUiState
    data class Content(
        val conversations: List<ConversationThread>,
        val isNotificationListenerEnabled: Boolean,
        val selectedThread: ConversationThread? = null,
    ) : DashboardUiState

    data class Error(val message: String) : DashboardUiState
}

class DashboardViewModel(
    getActiveConversations: GetActiveConversationsUseCase,
) : ViewModel() {

    private val notificationListenerEnabled = MutableStateFlow(false)
    private val selectedThread = MutableStateFlow<ConversationThread?>(null)

    val uiState: StateFlow<DashboardUiState> = combine(
        getActiveConversations(),
        notificationListenerEnabled,
        selectedThread,
    ) { conversations, listenerEnabled, thread ->
        DashboardUiState.Content(
            conversations = conversations,
            isNotificationListenerEnabled = listenerEnabled,
            selectedThread = thread,
        )
    }
        .catch<DashboardUiState> { emit(DashboardUiState.Error(it.message ?: "Unknown error")) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardUiState.Loading)

    fun refreshNotificationListenerPermission(isEnabled: Boolean) {
        notificationListenerEnabled.value = isEnabled
    }

    fun onThreadClick(thread: ConversationThread) {
        selectedThread.value = thread
    }

    fun clearSelectedThread() {
        selectedThread.update { null }
    }
}
