package pl.diplomat.infrastructure.ota

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

sealed interface OtaUiState {
    data object Idle : OtaUiState
    data class Downloading(val percent: Int?) : OtaUiState
    data class NeedInstallPermission(val apkPath: String) : OtaUiState
    data object Installing : OtaUiState
    data class Error(val message: String) : OtaUiState
}

class OtaUpdateViewModel(
    private val otaUpdateManager: OtaUpdateManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow<OtaUiState>(OtaUiState.Idle)
    val uiState: StateFlow<OtaUiState> = _uiState.asStateFlow()

    fun startUpdate(url: String) {
        if (url.isBlank()) {
            _uiState.value = OtaUiState.Error("Paste an APK or ZIP URL first")
            return
        }
        viewModelScope.launch {
            _uiState.value = OtaUiState.Downloading(null)
            runCatching {
                otaUpdateManager.downloadAndValidate(url) { percent ->
                    _uiState.value = OtaUiState.Downloading(percent)
                }
            }.onSuccess { apk ->
                if (!otaUpdateManager.canRequestPackageInstalls()) {
                    _uiState.value = OtaUiState.NeedInstallPermission(apk.absolutePath)
                } else {
                    launchInstaller(apk)
                }
            }.onFailure { error ->
                _uiState.value = OtaUiState.Error(error.message ?: "Update failed")
            }
        }
    }

    fun unknownSourcesSettingsIntent() = otaUpdateManager.unknownSourcesSettingsIntent()

    fun resumeInstallIfReady() {
        val pending = _uiState.value as? OtaUiState.NeedInstallPermission ?: return
        resumeInstall(pending.apkPath)
    }

    fun resumeInstall(apkPath: String) {
        val apk = File(apkPath)
        if (!apk.isFile) {
            _uiState.value = OtaUiState.Error("Downloaded APK missing; try again")
            return
        }
        if (!otaUpdateManager.canRequestPackageInstalls()) {
            _uiState.value = OtaUiState.NeedInstallPermission(apkPath)
            return
        }
        launchInstaller(apk)
    }

    private fun launchInstaller(apk: File) {
        runCatching {
            otaUpdateManager.install(apk)
            _uiState.value = OtaUiState.Installing
        }.onFailure { error ->
            _uiState.value = OtaUiState.Error(error.message ?: "Install failed")
        }
    }
}
