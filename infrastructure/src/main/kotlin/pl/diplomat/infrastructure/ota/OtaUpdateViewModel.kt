package pl.diplomat.infrastructure.ota

import android.util.Log
import pl.diplomat.domain.port.LatestReleaseUrlResolver
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
    data object LoadingLatest : OtaUiState
    data class NeedInstallPermission(val apkPath: String) : OtaUiState
    data object Installing : OtaUiState
    data class Error(val message: String) : OtaUiState
}

class OtaUpdateViewModel(
    private val otaUpdateManager: OtaUpdateManager,
    private val latestUrlResolver: LatestReleaseUrlResolver? = null,
) : ViewModel() {

    private val _uiState = MutableStateFlow<OtaUiState>(OtaUiState.Idle)
    val uiState: StateFlow<OtaUiState> = _uiState.asStateFlow()

    fun updateLatest() {
        Log.d("OtaUpdateViewModel", "Attempting to update to latest")
        val resolver = latestUrlResolver
        if (resolver == null) {
            Log.e("OtaUpdateViewModel", "No latest resolver configured")
            _uiState.value = OtaUiState.Error("No latest resolver configured")
            return
        }
        _uiState.value = OtaUiState.LoadingLatest
        viewModelScope.launch {
            Log.d("OtaUpdateViewModel", "Resolving latest URL")
            val resolvedUrl = resolver.resolveLatestUrl().getOrElse {
                Log.e("OtaUpdateViewModel", "Cannot resolve latest URL: ${it.message}")
                _uiState.value = OtaUiState.Error("Cannot resolve latest URL: ${it.message}")
                return@launch
            }
            Log.d("OtaUpdateViewModel", "Resolved latest URL: $resolvedUrl")
            startUpdate(resolvedUrl)
        }
    }

    fun startUpdate(url: String) {
        Log.d("OtaUpdateViewModel", "Starting update from URL: $url")
        if (url.isBlank()) {
            Log.e("OtaUpdateViewModel", "Paste an APK or ZIP URL first")
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
                Log.d("OtaUpdateViewModel", "Download successful, checking install permissions")
                if (!otaUpdateManager.canRequestPackageInstalls()) {
                    Log.d("OtaUpdateViewModel", "Need install permission, setting state")
                    _uiState.value = OtaUiState.NeedInstallPermission(apk.absolutePath)
                } else {
                    Log.d("OtaUpdateViewModel", "Installing APK")
                    launchInstaller(apk)
                }
            }.onFailure { error ->
                Log.e("OtaUpdateViewModel", "Failed to download/update", error)
                _uiState.value = OtaUiState.Error(error.message ?: "Update failed")
            }
        }
    }

    fun unknownSourcesSettingsIntent() = otaUpdateManager.unknownSourcesSettingsIntent()

    fun resumeInstallIfReady() {
        Log.d("OtaUpdateViewModel", "Resuming install if ready")
        val pending = _uiState.value as? OtaUiState.NeedInstallPermission ?: return
        resumeInstall(pending.apkPath)
    }

    fun resumeInstall(apkPath: String) {
        Log.d("OtaUpdateViewModel", "Resuming install for APK: $apkPath")
        val apk = File(apkPath)
        if (!apk.isFile) {
            Log.e("OtaUpdateViewModel", "Downloaded APK missing")
            _uiState.value = OtaUiState.Error("Downloaded APK missing; try again")
            return
        }
        if (!otaUpdateManager.canRequestPackageInstalls()) {
            Log.d("OtaUpdateViewModel", "Still need install permission")
            _uiState.value = OtaUiState.NeedInstallPermission(apkPath)
            return
        }
        Log.d("OtaUpdateViewModel", "Performing actual install")
        launchInstaller(apk)
    }

    private fun launchInstaller(apk: File) {
        runCatching {
            Log.d("OtaUpdateViewModel", "Launching installer for APK")
            otaUpdateManager.install(apk)
            _uiState.value = OtaUiState.Installing
        }.onFailure { error ->
            Log.e("OtaUpdateViewModel", "Install failed", error)
            _uiState.value = OtaUiState.Error(error.message ?: "Install failed")
        }
    }
}
