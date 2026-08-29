package pl.diplomat.presentation.dashboard

import android.Manifest
import android.content.Context
import android.os.Build
import android.os.Build.VERSION_CODES
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import pl.diplomat.domain.model.ConversationThread
import pl.diplomat.domain.model.MessageSourceApp
import pl.diplomat.domain.model.MessageStatus
import pl.diplomat.presentation.conversation.MessageChannelStatusBadges
import pl.diplomat.infrastructure.appinfo.AppBuildInfo
import pl.diplomat.infrastructure.debug.DevLog
import pl.diplomat.infrastructure.dashboard.DashboardPermissionState
import pl.diplomat.infrastructure.dashboard.DashboardUiState
import pl.diplomat.infrastructure.dashboard.DashboardViewModel
import pl.diplomat.infrastructure.ota.OtaUpdateViewModel
import pl.diplomat.presentation.ota.OtaUpdateSection
import pl.diplomat.infrastructure.notification.AccessibilityServicePermission
import pl.diplomat.infrastructure.notification.BatteryOptimizationPermission
import pl.diplomat.infrastructure.notification.NotificationListenerPermission
import pl.diplomat.infrastructure.notification.PostNotificationsPermission
import pl.diplomat.infrastructure.sms.ReadSmsPermission
import pl.diplomat.presentation.R
import pl.diplomat.presentation.copyPlainTextToClipboard
import pl.diplomat.presentation.message.previewText
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardRoute(
    viewModel: DashboardViewModel,
    otaUpdateViewModel: OtaUpdateViewModel,
    onOpenWhitelist: () -> Unit,
    onOpenLlmSettings: () -> Unit,
    onThreadClick: (ConversationThread) -> Unit,
    onSmsPermissionGranted: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val requestPostNotifications = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        viewModel.refreshPostNotificationsPermission(granted)
    }
    val requestReadSms = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            onSmsPermissionGranted()
        }
        viewModel.refreshPermissions(
            DashboardPermissionState(
                notificationListener = NotificationListenerPermission.isGranted(context),
                postNotifications = PostNotificationsPermission.isGranted(context),
                accessibility = AccessibilityServicePermission.isGranted(context),
                batteryIgnored = BatteryOptimizationPermission.isIgnoring(context),
                readSms = ReadSmsPermission.isGranted(context),
            ),
        )
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshPermissions(
                    DashboardPermissionState(
                        notificationListener = NotificationListenerPermission.isGranted(context),
                        postNotifications = PostNotificationsPermission.isGranted(context),
                        accessibility = AccessibilityServicePermission.isGranted(context),
                        batteryIgnored = BatteryOptimizationPermission.isIgnoring(context),
                        readSms = ReadSmsPermission.isGranted(context),
                    ),
                )
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    when (val state = uiState) {
        DashboardUiState.Loading -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.dashboard_loading))
            }
        }

        is DashboardUiState.Error -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(state.message)
            }
        }

        is DashboardUiState.Content -> {
            DashboardScreen(
                conversations = state.conversations,
                isNotificationListenerEnabled = state.isNotificationListenerEnabled,
                isPostNotificationsEnabled = state.isPostNotificationsEnabled,
                isAccessibilityServiceEnabled = state.isAccessibilityServiceEnabled,
                isBatteryOptimizationIgnored = state.isBatteryOptimizationIgnored,
                isReadSmsGranted = state.isReadSmsGranted,
                buildInfo = state.buildInfo,
                otaUpdateViewModel = otaUpdateViewModel,
                onOpenNotificationSettings = {
                    context.startActivity(NotificationListenerPermission.settingsIntent())
                },
                onRequestPostNotifications = {
                    if (Build.VERSION.SDK_INT >= VERSION_CODES.TIRAMISU) {
                        requestPostNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                },
                onOpenAccessibilitySettings = {
                    context.startActivity(AccessibilityServicePermission.settingsIntent())
                },
                onRequestBatteryOptimization = {
                    runCatching {
                        context.startActivity(BatteryOptimizationPermission.requestIntent(context))
                    }.onFailure {
                        context.startActivity(BatteryOptimizationPermission.settingsIntent())
                    }
                },
                onRequestReadSms = {
                    requestReadSms.launch(Manifest.permission.READ_SMS)
                },
                onOpenWhitelist = onOpenWhitelist,
                onOpenLlmSettings = onOpenLlmSettings,
                onThreadClick = { thread ->
                    viewModel.onThreadClick(thread)
                    onThreadClick(thread)
                },
                onCopyDebugLogs = {
                    copyDebugLogsToClipboard(context, state.buildInfo)
                },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    conversations: List<ConversationThread>,
    isNotificationListenerEnabled: Boolean,
    isPostNotificationsEnabled: Boolean,
    isAccessibilityServiceEnabled: Boolean,
    isBatteryOptimizationIgnored: Boolean,
    isReadSmsGranted: Boolean,
    buildInfo: AppBuildInfo,
    otaUpdateViewModel: OtaUpdateViewModel,
    onOpenNotificationSettings: () -> Unit,
    onRequestPostNotifications: () -> Unit,
    onOpenAccessibilitySettings: () -> Unit,
    onRequestBatteryOptimization: () -> Unit,
    onRequestReadSms: () -> Unit,
    onOpenWhitelist: () -> Unit,
    onOpenLlmSettings: () -> Unit,
    onThreadClick: (ConversationThread) -> Unit,
    onCopyDebugLogs: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.dashboard_title)) },
                actions = {
                    IconButton(onClick = onOpenLlmSettings) {
                        Icon(
                            Icons.Default.SmartToy,
                            contentDescription = stringResource(R.string.open_llm_settings),
                        )
                    }
                    IconButton(onClick = onOpenWhitelist) {
                        Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.open_whitelist))
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            if (!isNotificationListenerEnabled) {
                NotificationPermissionBanner(
                    message = stringResource(R.string.notification_listener_required),
                    actionLabel = stringResource(R.string.open_settings),
                    onAction = onOpenNotificationSettings,
                )
            }

            if (!isAccessibilityServiceEnabled) {
                NotificationPermissionBanner(
                    message = stringResource(R.string.accessibility_service_required),
                    actionLabel = stringResource(R.string.open_settings),
                    onAction = onOpenAccessibilitySettings,
                )
            }

            if (!isBatteryOptimizationIgnored) {
                NotificationPermissionBanner(
                    message = stringResource(R.string.battery_optimization_required),
                    actionLabel = stringResource(R.string.grant_permission),
                    onAction = onRequestBatteryOptimization,
                )
            }

            if (!isReadSmsGranted) {
                NotificationPermissionBanner(
                    message = stringResource(R.string.read_sms_required),
                    actionLabel = stringResource(R.string.grant_permission),
                    onAction = onRequestReadSms,
                )
            }

            if (isNotificationListenerEnabled && !isPostNotificationsEnabled) {
                NotificationPermissionBanner(
                    message = stringResource(R.string.post_notifications_required),
                    actionLabel = stringResource(R.string.grant_permission),
                    onAction = onRequestPostNotifications,
                )
            }

            BuildInfoFooter(
                buildInfo = buildInfo,
                otaUpdateViewModel = otaUpdateViewModel,
                onCopyDebugLogs = onCopyDebugLogs,
            )

            if (conversations.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(R.string.dashboard_empty),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(conversations, key = { it.contact.id }) { thread ->
                        ConversationThreadCard(
                            thread = thread,
                            onClick = { onThreadClick(thread) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BuildInfoFooter(
    buildInfo: AppBuildInfo,
    otaUpdateViewModel: OtaUpdateViewModel,
    onCopyDebugLogs: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = stringResource(R.string.build_info_version, buildInfo.versionName),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(R.string.build_info_commit, buildInfo.gitCommitHash),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(R.string.build_info_apk_built_at, buildInfo.apkBuiltAt),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TextButton(
                onClick = onCopyDebugLogs,
                modifier = Modifier.padding(top = 4.dp),
            ) {
                Text(
                    text = stringResource(R.string.copy_debug_logs),
                    style = MaterialTheme.typography.labelSmall,
                )
            }
            OtaUpdateSection(viewModel = otaUpdateViewModel)
        }
    }
}

private fun copyDebugLogsToClipboard(context: Context, buildInfo: AppBuildInfo) {
    copyPlainTextToClipboard(
        context,
        "Diplomat debug logs",
        DevLog.dumpForExport(buildInfo),
        R.string.debug_logs_copied,
    )
}

@Composable
private fun NotificationPermissionBanner(
    message: String,
    actionLabel: String,
    onAction: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.errorContainer,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onAction) {
                Text(actionLabel)
            }
        }
    }
}

@Composable
private fun ConversationThreadCard(
    thread: ConversationThread,
    onClick: () -> Unit,
) {
    val timeFormatter = DateFormat.getTimeInstance(DateFormat.SHORT)
    val formattedTime = timeFormatter.format(Date(thread.lastMessage.timestamp))

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ContactAvatar(
                avatarUri = thread.contact.avatarUri,
                displayName = thread.contact.displayName,
                unreadCount = thread.unreadCount,
            )

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = thread.contact.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = formattedTime,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = thread.lastMessage.content.previewText(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(8.dp))
                    MessageChannelStatusBadges(
                        status = thread.lastMessage.status,
                        sourceApp = thread.lastMessage.sourceApp,
                    )
                }
            }
        }
    }
}

@Composable
private fun ContactAvatar(
    avatarUri: String?,
    displayName: String,
    unreadCount: Int = 0,
    size: androidx.compose.ui.unit.Dp = 48.dp,
) {
    BadgedBox(
        badge = {
            if (unreadCount > 0) {
                Badge {
                    Text(
                        text = unreadCount.toString(),
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
        },
    ) {
        if (avatarUri != null) {
            AsyncImage(
                model = avatarUri,
                contentDescription = displayName,
                modifier = Modifier
                    .size(size)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop,
            )
        } else {
            Surface(
                modifier = Modifier.size(size),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
        }
    }
}

@Composable
internal fun ChannelBadge(sourceApp: MessageSourceApp) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.secondaryContainer,
    ) {
        Text(
            text = when (sourceApp) {
                MessageSourceApp.SMS -> stringResource(R.string.channel_sms_badge)
                MessageSourceApp.WHATSAPP -> stringResource(R.string.channel_whatsapp_badge)
            },
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        )
    }
}

@Composable
internal fun MessageStatusLabel(status: MessageStatus) {
    val (label, containerColor) = when (status) {
        MessageStatus.PENDING -> stringResource(R.string.status_pending) to MaterialTheme.colorScheme.primary
        MessageStatus.IGNORED_CONFIRMATION -> stringResource(R.string.status_ignored) to MaterialTheme.colorScheme.tertiary
        MessageStatus.REPLIED -> stringResource(R.string.status_replied) to MaterialTheme.colorScheme.secondary
    }

    Surface(
        shape = CircleShape,
        color = containerColor.copy(alpha = 0.15f),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = containerColor,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        )
    }
}
