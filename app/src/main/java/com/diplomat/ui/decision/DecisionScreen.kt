package com.diplomat.ui.decision

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.diplomat.R
import com.diplomat.domain.model.InterceptedMessage
import com.diplomat.ui.label
import com.diplomat.util.OutgoingMessageIntents

/**
 * Decision + drafting screen for a single captured message.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DecisionScreen(
    onBack: () -> Unit,
    viewModel: DecisionViewModel = viewModel(factory = DecisionViewModel.Factory),
) {
    val message by viewModel.message.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.decision_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        val current = message
        if (current == null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            IncomingCard(current)
            ToneCard(current)

            // Agreement toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.decision_agreement),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f),
                )
                Switch(checked = uiState.agreement, onCheckedChange = viewModel::setAgreement)
            }

            OutlinedTextField(
                value = uiState.reasoning,
                onValueChange = viewModel::setReasoning,
                label = { Text(stringResource(R.string.decision_reasoning_label)) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
            )

            Button(
                onClick = viewModel::generateDraft,
                enabled = !uiState.isGenerating,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (uiState.isGenerating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                    Spacer(Modifier.size(8.dp))
                }
                Text(stringResource(R.string.decision_generate))
            }

            if (uiState.error != null) {
                Text(
                    text = uiState.error!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            OutlinedTextField(
                value = uiState.draft,
                onValueChange = viewModel::editDraft,
                label = { Text(stringResource(R.string.decision_draft_label)) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedButton(
                    onClick = {
                        viewModel.ignore()
                        onBack()
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Text(stringResource(R.string.decision_ignore))
                }
                Button(
                    onClick = {
                        viewModel.approveAndSend { dispatched ->
                            OutgoingMessageIntents.send(
                                context = context,
                                source = dispatched.source,
                                recipient = dispatched.sender,
                                body = dispatched.draftResponse.orEmpty(),
                            )
                            onBack()
                        }
                    },
                    enabled = uiState.draft.isNotBlank(),
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null)
                    Spacer(Modifier.size(8.dp))
                    Text(stringResource(R.string.decision_send))
                }
            }
        }
    }
}

@Composable
private fun IncomingCard(message: InterceptedMessage) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.decision_incoming),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.secondary,
            )
            Spacer(Modifier.size(4.dp))
            Text(text = message.sender, style = MaterialTheme.typography.titleMedium)
            Text(
                text = "${message.source.label()} · ${message.status.label()}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.size(8.dp))
            Text(text = message.body, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
private fun ToneCard(message: InterceptedMessage) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.decision_tone),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.secondary,
            )
            Spacer(Modifier.size(4.dp))
            Text(
                text = message.toneAnalysis ?: stringResource(R.string.decision_tone_pending),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}
