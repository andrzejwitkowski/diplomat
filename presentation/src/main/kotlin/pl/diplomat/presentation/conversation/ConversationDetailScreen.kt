package pl.diplomat.presentation.conversation

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pl.diplomat.domain.model.ChannelMessageGroup
import pl.diplomat.domain.model.ConversationRange
import pl.diplomat.domain.model.ConversationThread
import pl.diplomat.domain.model.IncomingMessage
import pl.diplomat.domain.model.MessageSourceApp
import pl.diplomat.domain.model.Sentiment
import pl.diplomat.infrastructure.conversation.ConversationDetailEvent
import pl.diplomat.infrastructure.conversation.ConversationDetailUiState
import pl.diplomat.infrastructure.conversation.ConversationDetailViewModel
import pl.diplomat.infrastructure.conversation.MarkMode
import pl.diplomat.infrastructure.conversation.SuggestionOutcome
import pl.diplomat.infrastructure.sms.ReadSmsPermission
import pl.diplomat.presentation.R
import kotlinx.coroutines.flow.collectLatest
import java.time.LocalDate

@Composable
fun ConversationDetailRoute(
    thread: ConversationThread,
    viewModel: ConversationDetailViewModel,
    onBack: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val wrongChannelMessage = stringResource(R.string.conversation_mark_wrong_channel)
    val importPermissionMessage = stringResource(R.string.read_sms_required)
    var isReadSmsGranted by remember { mutableStateOf(ReadSmsPermission.isGranted(context)) }
    val requestReadSms = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        isReadSmsGranted = granted
    }

    LaunchedEffect(thread.contact.id, viewModel) {
        viewModel.events.collectLatest { event ->
            when (event) {
                ConversationDetailEvent.WrongChannel ->
                    snackbarHostState.showSnackbar(wrongChannelMessage)
                is ConversationDetailEvent.ImportSuccess -> {
                    val message = context.getString(R.string.history_import_success, event.count)
                    snackbarHostState.showSnackbar(message)
                }
                ConversationDetailEvent.ImportPermissionDenied ->
                    snackbarHostState.showSnackbar(importPermissionMessage)
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
            val sentimentState by viewModel.sentiment.collectAsStateWithLifecycle()
            ConversationDetailScreen(
                contactName = state.contact.displayName,
                channelGroups = state.channelGroups,
                range = state.range,
                markMode = state.markMode,
                snackbarHostState = snackbarHostState,
                isReadSmsGranted = isReadSmsGranted,
                isImporting = viewModel.isImporting,
                onBack = onBack,
                onEnterMarkMode = viewModel::enterMarkMode,
                onCancelMarkMode = viewModel::cancelMarkMode,
                onMessageClick = viewModel::onMessageClick,
                onEditStart = viewModel::editStart,
                onEditEnd = viewModel::editEnd,
                onDeleteStart = viewModel::deleteStart,
                onDeleteEnd = viewModel::deleteEnd,
                onSentimentSelect = viewModel::selectSentiment,
                onDesiredAnswerChange = viewModel::updateDesiredAnswer,
                onSubmitSuggestion = viewModel::submitSuggestion,
                onRequestReadSms = { requestReadSms.launch(Manifest.permission.READ_SMS) },
                onImportHistory = viewModel::requestImport,
                sentiment = sentimentState,
                desiredAnswer = viewModel.desiredAnswer,
                isSubmitting = viewModel.isSubmitting,
                lastOutcome = viewModel.lastOutcome,
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
    isReadSmsGranted: Boolean,
    isImporting: Boolean,
    onBack: () -> Unit,
    onEnterMarkMode: () -> Unit,
    onCancelMarkMode: () -> Unit,
    onMessageClick: (IncomingMessage) -> Unit,
    onEditStart: () -> Unit,
    onEditEnd: () -> Unit,
    onDeleteStart: () -> Unit,
    onDeleteEnd: () -> Unit,
    onSentimentSelect: (Sentiment) -> Unit,
    onDesiredAnswerChange: (String) -> Unit,
    onSubmitSuggestion: () -> Unit,
    onRequestReadSms: () -> Unit,
    onImportHistory: (LocalDate) -> Unit,
    sentiment: Sentiment,
    desiredAnswer: String,
    isSubmitting: Boolean,
    lastOutcome: SuggestionOutcome,
) {
    val listState = rememberLazyListState()
    var didScrollToLatest by remember { mutableStateOf(false) }

    LaunchedEffect(channelGroups) {
        if (channelGroups.isNotEmpty() && !didScrollToLatest) {
            listState.scrollToItem(latestMessageLazyIndex(channelGroups))
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
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item(key = "history-import") {
                HistoryImportSection(
                    isReadSmsGranted = isReadSmsGranted,
                    isImporting = isImporting,
                    onRequestReadSms = onRequestReadSms,
                    onImportHistory = onImportHistory,
                )
            }

            item(key = "suggestion-composer") {
                SuggestionComposer(
                    sentiment = sentiment,
                    onSentimentSelect = onSentimentSelect,
                    desiredAnswer = desiredAnswer,
                    onDesiredAnswerChange = onDesiredAnswerChange,
                    onSubmit = onSubmitSuggestion,
                    isSubmitting = isSubmitting,
                    lastOutcome = lastOutcome,
                )
            }

            if (channelGroups.isEmpty()) {
                item(key = "empty") {
                    Text(
                        text = stringResource(R.string.conversation_detail_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                    )
                }
            } else {
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

private const val CONTACT_SETUP_LAZY_ITEMS = 2

private fun latestMessageLazyIndex(channelGroups: List<ChannelMessageGroup>): Int =
    CONTACT_SETUP_LAZY_ITEMS + channelGroups.sumOf { 1 + it.messages.size } - 1

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
