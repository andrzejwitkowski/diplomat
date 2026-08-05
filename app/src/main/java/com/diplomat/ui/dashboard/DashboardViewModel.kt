package com.diplomat.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.diplomat.DiplomatApplication
import com.diplomat.data.repository.MessageRepository
import com.diplomat.domain.model.ConversationThread
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

/**
 * Exposes the sender-correlated conversation list for the dashboard.
 */
class DashboardViewModel(
    repository: MessageRepository,
) : ViewModel() {

    val threads: StateFlow<List<ConversationThread>> =
        repository.observeThreads()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList(),
            )

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as DiplomatApplication
                DashboardViewModel(app.container.messageRepository)
            }
        }
    }
}
