package com.diplomat.ui.dashboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.diplomat.R
import com.diplomat.domain.model.ConversationThread
import com.diplomat.ui.label

/**
 * Dashboard: the list of sender-correlated conversations with their status,
 * plus banners prompting for the permissions capture depends on.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onOpenDecision: (Long) -> Unit,
    permissionState: PermissionBannerState,
    viewModel: DashboardViewModel = viewModel(factory = DashboardViewModel.Factory),
) {
    val threads by viewModel.threads.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.dashboard_title)) }) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            PermissionBanners(state = permissionState)

            if (threads.isEmpty()) {
                EmptyState()
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(threads, key = { it.latestMessage.id }) { thread ->
                        ConversationRow(thread = thread, onClick = { onOpenDecision(thread.latestMessage.id) })
                    }
                }
            }
        }
    }
}

@Composable
private fun ConversationRow(thread: ConversationThread, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = thread.sender,
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = thread.source.label(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
            Spacer(Modifier.size(4.dp))
            Text(
                text = thread.latestMessage.body,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.size(8.dp))
            Text(
                text = thread.latestMessage.status.label(),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun EmptyState() {
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = stringResource(R.string.dashboard_empty),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun PermissionBanners(state: PermissionBannerState) {
    if (!state.notificationAccessGranted) {
        Banner(
            title = stringResource(R.string.permission_banner_title),
            text = stringResource(R.string.permission_banner_text),
            actionLabel = stringResource(R.string.permission_banner_action),
            onAction = state.onGrantNotificationAccess,
        )
    }
    if (!state.batteryOptimizationDisabled) {
        Banner(
            title = null,
            text = stringResource(R.string.battery_banner_text),
            actionLabel = stringResource(R.string.battery_banner_action),
            onAction = state.onDisableBatteryOptimization,
        )
    }
}

@Composable
private fun Banner(title: String?, text: String, actionLabel: String, onAction: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
            )
            Column(modifier = Modifier.weight(1f)) {
                if (title != null) {
                    Text(text = title, style = MaterialTheme.typography.titleSmall)
                }
                Text(text = text, style = MaterialTheme.typography.bodyMedium)
            }
            OutlinedButton(onClick = onAction) { Text(actionLabel) }
        }
    }
}

/**
 * Everything the dashboard needs to render + resolve permission banners,
 * hoisted so the screen stays stateless and previewable.
 */
data class PermissionBannerState(
    val notificationAccessGranted: Boolean,
    val batteryOptimizationDisabled: Boolean,
    val onGrantNotificationAccess: () -> Unit,
    val onDisableBatteryOptimization: () -> Unit,
)
