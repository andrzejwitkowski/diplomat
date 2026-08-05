package com.diplomat.presentation.whitelist

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.diplomat.DiplomatApplication
import com.diplomat.R
import com.diplomat.domain.whitelist.WhitelistedContact

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WhitelistScreen(
    onBack: () -> Unit,
    viewModel: WhitelistViewModel = viewModel(factory = WhitelistViewModel.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val gateway = (context.applicationContext as DiplomatApplication).container.deviceContacts

    val pickContact = rememberLauncherForActivityResult(
        ActivityResultContracts.PickContact(),
    ) { uri: Uri? ->
        uri ?: return@rememberLauncherForActivityResult
        gateway.read(uri)?.let { viewModel.applyPickedContact(it.displayName, it.phoneNumber) }
    }

    val requestContacts = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> if (granted) pickContact.launch(null) }

    fun openPicker() {
        val ok = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) ==
            PackageManager.PERMISSION_GRANTED
        if (ok) pickContact.launch(null) else requestContacts.launch(Manifest.permission.READ_CONTACTS)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.whitelist_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
            )
        },
    ) { padding ->
        when (val ui = state) {
            WhitelistUiState.Loading -> Column(
                Modifier.fillMaxSize().padding(padding),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) { CircularProgressIndicator() }

            is WhitelistUiState.Ready -> Column(
                Modifier.fillMaxSize().padding(padding).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = if (ui.editingId == null) {
                        stringResource(R.string.whitelist_add)
                    } else {
                        stringResource(R.string.whitelist_edit)
                    },
                    style = MaterialTheme.typography.titleMedium,
                )
                OutlinedTextField(
                    value = ui.draftName,
                    onValueChange = viewModel::onNameChange,
                    label = { Text(stringResource(R.string.whitelist_name)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                OutlinedTextField(
                    value = ui.draftPhone,
                    onValueChange = viewModel::onPhoneChange,
                    label = { Text(stringResource(R.string.whitelist_phone)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = viewModel::save) {
                        Text(stringResource(R.string.whitelist_save))
                    }
                    OutlinedButton(onClick = ::openPicker) {
                        Icon(Icons.Filled.Contacts, contentDescription = null)
                        Text(
                            text = stringResource(R.string.whitelist_from_contacts),
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                    if (ui.editingId != null) {
                        OutlinedButton(onClick = viewModel::cancelEdit) {
                            Text(stringResource(R.string.whitelist_cancel))
                        }
                    }
                }
                ui.errorMessage?.let {
                    Text(it, color = MaterialTheme.colorScheme.error)
                }
                HorizontalDivider()
                if (ui.contacts.isEmpty()) {
                    Text(
                        stringResource(R.string.whitelist_empty),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        items(ui.contacts, key = { it.id }) { contact ->
                            ContactRow(
                                contact = contact,
                                onEdit = { viewModel.startEdit(contact) },
                                onDelete = { viewModel.delete(contact.id) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ContactRow(
    contact: WhitelistedContact,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(contact.displayName, style = MaterialTheme.typography.titleSmall)
            Text(contact.phoneNumber.value, style = MaterialTheme.typography.bodyMedium)
        }
        IconButton(onClick = onEdit) {
            Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.whitelist_edit))
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.whitelist_delete))
        }
    }
}
