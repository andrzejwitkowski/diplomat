package pl.diplomat.presentation.ota

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pl.diplomat.infrastructure.ota.OtaUiState
import pl.diplomat.infrastructure.ota.OtaUpdateViewModel
import pl.diplomat.presentation.R

@Composable
fun OtaUpdateSection(
    viewModel: OtaUpdateViewModel,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var url by rememberSaveable { mutableStateOf("") }
    val downloading = state is OtaUiState.Downloading

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.resumeInstallIfReady()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        OutlinedTextField(
            value = url,
            onValueChange = { url = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            label = { Text(stringResource(R.string.ota_url_label)) },
            singleLine = true,
            enabled = !downloading,
        )
        Text(
            text = stringResource(R.string.ota_hint),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(
                onClick = { viewModel.startUpdate(url) },
                enabled = !downloading && url.isNotBlank(),
            ) {
                Text(stringResource(R.string.ota_update))
            }
            val pending = state as? OtaUiState.NeedInstallPermission
            if (pending != null) {
                TextButton(onClick = {
                    context.startActivity(viewModel.unknownSourcesSettingsIntent())
                }) {
                    Text(stringResource(R.string.ota_open_install_settings))
                }
                TextButton(onClick = { viewModel.resumeInstall(pending.apkPath) }) {
                    Text(stringResource(R.string.ota_continue_install))
                }
            }
        }
        when (val current = state) {
            OtaUiState.Idle -> Unit
            is OtaUiState.Downloading -> OtaDownloadProgress(current.percent)
            is OtaUiState.NeedInstallPermission -> Text(
                text = stringResource(R.string.ota_need_permission),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error,
            )
            OtaUiState.Installing -> Text(
                text = stringResource(R.string.ota_installing),
                style = MaterialTheme.typography.labelSmall,
            )
            is OtaUiState.Error -> Text(
                text = current.message,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Composable
private fun OtaDownloadProgress(percent: Int?) {
    if (percent != null) {
        LinearProgressIndicator(
            progress = { percent / 100f },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
        )
        Text(
            text = stringResource(R.string.ota_downloading_percent, percent),
            style = MaterialTheme.typography.labelSmall,
        )
    } else {
        LinearProgressIndicator(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
        )
        Text(
            text = stringResource(R.string.ota_downloading),
            style = MaterialTheme.typography.labelSmall,
        )
    }
}
