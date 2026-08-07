package pl.diplomat.presentation.conversation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pl.diplomat.domain.model.ChannelMessageGroup
import pl.diplomat.domain.model.ConversationThread
import pl.diplomat.domain.model.IncomingMessage
import pl.diplomat.domain.model.MessageSourceApp
import pl.diplomat.domain.model.MessageStatus
import pl.diplomat.infrastructure.conversation.ConversationDetailUiState
import pl.diplomat.infrastructure.conversation.ConversationDetailViewModel
import pl.diplomat.presentation.R
import pl.diplomat.presentation.dashboard.ChannelBadge
import pl.diplomat.presentation.dashboard.MessageStatusLabel
import pl.diplomat.presentation.message.previewText
import java.text.DateFormat
import java.util.Date

@Composable
fun ConversationDetailRoute(
    thread: ConversationThread,
    viewModel: ConversationDetailViewModel,
    onBack: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

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
                onBack = onBack,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationDetailScreen(
    contactName: String,
    channelGroups: List<ChannelMessageGroup>,
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(contactName) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                        )
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
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                channelGroups.forEach { group ->
                    item(key = "header-${group.sourceApp}") {
                        ChannelSectionHeader(sourceApp = group.sourceApp)
                    }
                    items(group.messages, key = { it.id }) { message ->
                        ConversationMessageCard(message = message)
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
        modifier = Modifier.padding(bottom = 4.dp),
    )
}

@Composable
private fun ConversationMessageCard(message: IncomingMessage) {
    val timeFormatter = DateFormat.getTimeInstance(DateFormat.SHORT)
    val formattedTime = timeFormatter.format(Date(message.timestamp))

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            RowWithBadges(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = formattedTime,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                MessageChannelStatusBadges(
                    status = message.status,
                    sourceApp = message.sourceApp,
                )
            }
            Text(
                text = message.content.previewText(),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun RowWithBadges(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    content: @Composable () -> Unit,
) {
    androidx.compose.foundation.layout.Row(
        modifier = modifier,
        horizontalArrangement = horizontalArrangement,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        content()
    }
}

@Composable
internal fun MessageChannelStatusBadges(
    status: MessageStatus,
    sourceApp: MessageSourceApp,
) {
    androidx.compose.foundation.layout.Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ChannelBadge(sourceApp = sourceApp)
        MessageStatusLabel(status = status)
    }
}
