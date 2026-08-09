package pl.diplomat.infrastructure.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import pl.diplomat.domain.model.ConversationThread
import pl.diplomat.infrastructure.appinfo.AppBuildInfo
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
        val isPostNotificationsEnabled: Boolean,
        val isAccessibilityServiceEnabled: Boolean,
        val isBatteryOptimizationIgnored: Boolean,
        val buildInfo: AppBuildInfo,
        val selectedThread: ConversationThread? = null,
    ) : DashboardUiState

    data class Error(val message: String) : DashboardUiState
}

data class DashboardPermissionState(
    val notificationListener: Boolean = false,
    val postNotifications: Boolean = true,
    val accessibility: Boolean = false,
    val batteryIgnored: Boolean = true,
)

class DashboardViewModel(
    getActiveConversations: GetActiveConversationsUseCase,
    buildInfo: AppBuildInfo,
) : ViewModel() {

    private val permissions = MutableStateFlow(DashboardPermissionState())
    private val selectedThread = MutableStateFlow<ConversationThread?>(null)
    private val buildInfoState = MutableStateFlow(buildInfo)

    val uiState: StateFlow<DashboardUiState> = combine(
        getActiveConversations(),
        permissions,
        selectedThread,
        buildInfoState,
    ) { conversations, perms, thread, info ->
        DashboardUiState.Content(
            conversations = conversations,
            isNotificationListenerEnabled = perms.notificationListener,
            isPostNotificationsEnabled = perms.postNotifications,
            isAccessibilityServiceEnabled = perms.accessibility,
            isBatteryOptimizationIgnored = perms.batteryIgnored,
            buildInfo = info,
            selectedThread = thread,
        )
    }
        .catch<DashboardUiState> { emit(DashboardUiState.Error(it.message ?: "Unknown error")) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardUiState.Loading)

    fun refreshPermissions(state: DashboardPermissionState) {
        permissions.value = state
    }

    fun refreshPostNotificationsPermission(isEnabled: Boolean) {
        permissions.update { it.copy(postNotifications = isEnabled) }
    }

    fun onThreadClick(thread: ConversationThread) {
        selectedThread.value = thread
    }

    fun clearSelectedThread() {
        selectedThread.update { null }
    }
}
