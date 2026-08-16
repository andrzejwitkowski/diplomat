package pl.diplomat.presentation.conversation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pl.diplomat.domain.model.ChannelMessageGroup
import pl.diplomat.domain.model.ConversationRange
import pl.diplomat.domain.model.ConversationThread
import pl.diplomat.domain.model.IncomingMessage
import pl.diplomat.domain.model.MessageSourceApp
import pl.diplomat.infrastructure.conversation.ConversationDetailEvent
import pl.diplomat.infrastructure.conversation.ConversationDetailUiState
import pl.diplomat.infrastructure.conversation.ConversationDetailViewModel
import pl.diplomat.infrastructure.conversation.MarkMode
import pl.diplomat.presentation.R
import kotlinx.coroutines.flow.collectLatest

@Composable
fun ConversationDetailRoute(
    thread: ConversationThread,
    viewModel: ConversationDetailViewModel,
    onBack: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val wrongChannelMessage = stringResource(R.string.conversation_mark_wrong_channel)

    LaunchedEffect(thread.contact.id, viewModel) {
        viewModel.events.collectLatest { event ->
            when (event) {
                ConversationDetailEvent.WrongChannel ->
                    snackbarHostState.showSnackbar(wrongChannelMessage)
            }
        }
    }

    when (val state = uiState) {
        ConversationDetailUiState.Loading -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.dashboard_loading))
            }
        }

        is ConversationDetailUiState.Content -> {
            ConversationDetailScreen(
                contactName = state.contact.displayName,
                channelGroups = state.channelGroups,
                range = state.range,
                markMode = state.markMode,
                snackbarHostState = snackbarHostState,
                onBack = onBack,
                onEnterMarkMode = viewModel::enterMarkMode,
                onCancelMarkMode = viewModel::cancelMarkMode,
                onMessageClick = viewModel::onMessageClick,
                onEditStart = viewModel::editStart,
                onEditEnd = viewModel::editEnd,
                onDeleteStart = viewModel::deleteStart,
                onDeleteEnd = viewModel::deleteEnd,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationDetailScreen(
    contactName: String,
    channelGroups: List<ChannelMessageGroup>,
    range: ConversationRange?,
    markMode: MarkMode,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onEnterMarkMode: () -> Unit,
    onCancelMarkMode: () -> Unit,
    onMessageClick: (IncomingMessage) -> Unit,
    onEditStart: () -> Unit,
    onEditEnd: () -> Unit,
    onDeleteStart: () -> Unit,
    onDeleteEnd: () -> Unit,
) {
    val listState = rememberLazyListState()
    val itemCount = channelGroups.sumOf { 1 + it.messages.size }
    var didScrollToLatest by remember { mutableStateOf(false) }

    LaunchedEffect(itemCount) {
        if (itemCount > 0 && !didScrollToLatest) {
            listState.scrollToItem(itemCount - 1)
            didScrollToLatest = true
        }
    }

    val markModeActive = markMode != MarkMode.Idle
    val markHint = when (markMode) {
        MarkMode.Idle -> null
        MarkMode.PickingStart -> stringResource(R.string.conversation_mark_pick_start)
        MarkMode.PickingEnd -> stringResource(R.string.conversation_mark_pick_end)
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(contactName)
                        if (markHint != null) {
                            Text(
                                text = markHint,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                        )
                    }
                },
                actions = {
                    if (markModeActive) {
                        IconButton(onClick = onCancelMarkMode) {
                            Icon(
                                Icons.Filled.Close,
                                contentDescription = stringResource(R.string.conversation_mark_cancel),
                            )
                        }
                    } else {
                        IconButton(onClick = onEnterMarkMode) {
                            Icon(
                                Icons.Filled.Flag,
                                contentDescription = stringResource(R.string.conversation_mark_start),
                            )
                        }
                    }
                },
            )
        },
    ) { padding ->
        if (channelGroups.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.conversation_detail_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                channelGroups.forEach { group ->
                    item(key = "header-${group.sourceApp}") {
                        ChannelSectionHeader(sourceApp = group.sourceApp)
                    }
                    val roles = rangeRoles(range, group.messages)
                    itemsIndexed(group.messages, key = { _, message -> message.id }) { index, message ->
                        ConversationMessageBubble(
                            message = message,
                            contactName = contactName,
                            role = roles[index],
                            range = range,
                            markModeActive = markModeActive,
                            onMessageClick = { onMessageClick(message) },
                            onEditStart = onEditStart,
                            onEditEnd = onEditEnd,
                            onDeleteStart = onDeleteStart,
                            onDeleteEnd = onDeleteEnd,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChannelSectionHeader(sourceApp: MessageSourceApp) {
    Text(
        text = when (sourceApp) {
            MessageSourceApp.SMS -> stringResource(R.string.channel_sms)
            MessageSourceApp.WHATSAPP -> stringResource(R.string.channel_whatsapp)
        },
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 4.dp),
    )
}
