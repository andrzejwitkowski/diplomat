package pl.diplomat.presentation.conversation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.background
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.TextField
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
import androidx.compose.material3.OutlinedTextField
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
import pl.diplomat.domain.model.Sentiment
import pl.diplomat.infrastructure.conversation.ConversationDetailEvent
import pl.diplomat.infrastructure.conversation.ConversationDetailUiState
import pl.diplomat.infrastructure.conversation.ConversationDetailViewModel
import pl.diplomat.infrastructure.conversation.MarkMode
import pl.diplomat.infrastructure.conversation.SuggestionOutcome
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
            val sentimentState by viewModel.sentiment.collectAsStateWithLifecycle()
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
                onSentimentSelect = viewModel::selectSentiment,
                onDesiredAnswerChange = viewModel::updateDesiredAnswer,
                onSubmitSuggestion = viewModel::submitSuggestion,
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
    sentiment: Sentiment,
    desiredAnswer: String,
    isSubmitting: Boolean,
    lastOutcome: SuggestionOutcome,
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
            }
        }
    }
}

@Composable
private fun SuggestionComposer(
    sentiment: Sentiment,
    onSentimentSelect: (Sentiment) -> Unit,
    desiredAnswer: String,
    onDesiredAnswerChange: (String) -> Unit,
    onSubmit: () -> Unit,
    isSubmitting: Boolean,
    lastOutcome: SuggestionOutcome,
) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
    ) {
        Text(
            text = stringResource(R.string.suggestion_composer_title),
            style = MaterialTheme.typography.titleMedium,
            color = colors.primary,
            modifier = Modifier.padding(bottom = 8.dp),
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = stringResource(R.string.suggestion_sentiment_label),
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(8.dp))
            Sentiment.entries.forEach { option ->
                val active = option == sentiment
                IconButton(
                    onClick = { onSentimentSelect(option) },
                    modifier = Modifier
                        .background(
                            colors.secondaryContainer.copy(alpha = if (active) 0.5f else 0.1f),
                            shape = MaterialTheme.shapes.small,
                        )
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                ) {
                    Text(
                        text = option.label,
                        style = MaterialTheme.typography.labelMedium,
                        color = when (option) {
                            Sentiment.POSITIVE -> colors.primary
                            Sentiment.NEUTRAL -> colors.onSurfaceVariant
                            Sentiment.NEGATIVE -> colors.error
                        },
                    )
                }
            }
        }

        TextField(
            value = desiredAnswer,
            onValueChange = onDesiredAnswerChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.suggestion_desired_answer)) },
            minLines = 3,
        )

        Button(
            onClick = onSubmit,
            enabled = !isSubmitting,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = if (isSubmitting) stringResource(R.string.suggestion_submitting)
                else stringResource(R.string.suggestion_submit),
            )
        }

        if (lastOutcome is SuggestionOutcome.Failure) {
            Text(
                text = stringResource(R.string.suggestion_failed, lastOutcome.message),
                style = MaterialTheme.typography.bodySmall,
                color = colors.error,
                modifier = Modifier.padding(top = 4.dp),
            )
        } else if (lastOutcome is SuggestionOutcome.Success && lastOutcome.text.isNotBlank()) {
            Text(
                text = stringResource(R.string.suggestion_result),
                style = MaterialTheme.typography.labelMedium,
                color = colors.primary,
                modifier = Modifier.padding(top = 8.dp),
            )
            OutlinedTextField(
                value = lastOutcome.text,
                onValueChange = {},
                modifier = Modifier.fillMaxWidth(),
                enabled = false,
            )
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
