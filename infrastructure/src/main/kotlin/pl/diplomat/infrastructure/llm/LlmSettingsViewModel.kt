package pl.diplomat.infrastructure.llm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import pl.diplomat.domain.model.LlmSettings
import pl.diplomat.domain.port.LlmSettingsPort
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LlmSettingsUiState(
    val baseUrl: String = "",
    val apiKey: String = "",
    val model: String = "",
    val message: String? = null,
)

class LlmSettingsViewModel(
    private val settingsPort: LlmSettingsPort,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        settingsPort.load().let {
            LlmSettingsUiState(baseUrl = it.baseUrl, apiKey = it.apiKey, model = it.model)
        },
    )
    val uiState: StateFlow<LlmSettingsUiState> = _uiState.asStateFlow()

    fun updateBaseUrl(value: String) = _uiState.update { it.copy(baseUrl = value, message = null) }
    fun updateApiKey(value: String) = _uiState.update { it.copy(apiKey = value, message = null) }
    fun updateModel(value: String) = _uiState.update { it.copy(model = value, message = null) }

    fun save() {
        val current = _uiState.value
        viewModelScope.launch {
            runCatching {
                settingsPort.save(
                    LlmSettings(
                        baseUrl = current.baseUrl.trim(),
                        apiKey = current.apiKey.trim(),
                        model = current.model.trim(),
                    ),
                )
            }.onSuccess {
                _uiState.update { it.copy(message = "Settings saved") }
            }.onFailure {
                _uiState.update { it.copy(message = it.message ?: "Save failed") }
            }
        }
    }

    fun clearMessage() = _uiState.update { it.copy(message = null) }
}
