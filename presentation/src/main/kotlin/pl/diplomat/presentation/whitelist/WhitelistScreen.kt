package pl.diplomat.presentation.whitelist

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.provider.ContactsContract
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import pl.diplomat.domain.model.WhitelistedContact
import pl.diplomat.infrastructure.whitelist.EditorState
import pl.diplomat.infrastructure.whitelist.WhitelistEvent
import pl.diplomat.infrastructure.whitelist.WhitelistUiState
import pl.diplomat.infrastructure.whitelist.WhitelistViewModel
import pl.diplomat.infrastructure.sms.ReadSmsPermission
import pl.diplomat.presentation.R
import kotlinx.coroutines.flow.collectLatest
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WhitelistRoute(
    viewModel: WhitelistViewModel,
    onBack: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val importPermissionMessage = stringResource(R.string.read_sms_required)
    var isReadSmsGranted by remember { mutableStateOf(ReadSmsPermission.isGranted(context)) }
    val requestReadSms = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        isReadSmsGranted = granted
    }

    val pickContactLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                viewModel.importFromSystemContact(uri.toString())
            }
        }
    }

    val pickImageLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent(),
    ) { uri ->
        uri?.let { viewModel.setEditorAvatarFromUri(it.toString()) }
    }

    LaunchedEffect(uiState) {
        val message = (uiState as? WhitelistUiState.Content)?.message ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        viewModel.clearMessage()
    }

    LaunchedEffect(viewModel) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is WhitelistEvent.ImportSuccess -> {
                    val message = context.getString(R.string.history_import_success, event.count)
                    snackbarHostState.showSnackbar(message)
                }
                WhitelistEvent.ImportPermissionDenied ->
                    snackbarHostState.showSnackbar(importPermissionMessage)
            }
        }
    }

    WhitelistScreen(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        isReadSmsGranted = isReadSmsGranted,
        importingContactId = viewModel.importingContactId,
        onBack = onBack,
        onAddClick = viewModel::openAddEditor,
        onEditClick = viewModel::openEditEditor,
        onDeleteClick = viewModel::removeContact,
        onDismissEditor = viewModel::dismissEditor,
        onSaveEditor = viewModel::saveEditor,
        onDisplayNameChange = viewModel::updateEditorDisplayName,
        onPhoneNumberChange = viewModel::updateEditorPhoneNumber,
        onPickFromContacts = {
            pickContactLauncher.launch(
                Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI),
            )
        },
        onPickAvatar = { pickImageLauncher.launch("image/*") },
        onRequestReadSms = { requestReadSms.launch(Manifest.permission.READ_SMS) },
        onImportHistory = viewModel::requestImport,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WhitelistScreen(
    uiState: WhitelistUiState,
    snackbarHostState: SnackbarHostState,
    isReadSmsGranted: Boolean,
    importingContactId: Long?,
    onBack: () -> Unit,
    onAddClick: () -> Unit,
    onEditClick: (WhitelistedContact) -> Unit,
    onDeleteClick: (Long) -> Unit,
    onDismissEditor: () -> Unit,
    onSaveEditor: () -> Unit,
    onDisplayNameChange: (String) -> Unit,
    onPhoneNumberChange: (String) -> Unit,
    onPickFromContacts: () -> Unit,
    onPickAvatar: () -> Unit,
    onRequestReadSms: () -> Unit,
    onImportHistory: (WhitelistedContact, LocalDate) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.whitelist_title)) },
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
        floatingActionButton = {
            FloatingActionButton(onClick = onAddClick) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_contact))
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        when (uiState) {
            WhitelistUiState.Loading -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(stringResource(R.string.loading))
                }
            }

            is WhitelistUiState.Error -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(uiState.message, color = MaterialTheme.colorScheme.error)
                }
            }

            is WhitelistUiState.Content -> {
                if (uiState.contacts.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .padding(16.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(stringResource(R.string.empty_whitelist))
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(uiState.contacts, key = { it.id }) { contact ->
                            ContactCard(
                                contact = contact,
                                isReadSmsGranted = isReadSmsGranted,
                                isImporting = importingContactId == contact.id,
                                onEdit = { onEditClick(contact) },
                                onDelete = { onDeleteClick(contact.id) },
                                onRequestReadSms = onRequestReadSms,
                                onImportHistory = { date -> onImportHistory(contact, date) },
                            )
                        }
                    }
                }

                uiState.editor?.let { editor ->
                    ContactEditorDialog(
                        editor = editor,
                        onDismiss = onDismissEditor,
                        onSave = onSaveEditor,
                        onDisplayNameChange = onDisplayNameChange,
                        onPhoneNumberChange = onPhoneNumberChange,
                        onPickFromContacts = onPickFromContacts,
                        onPickAvatar = onPickAvatar,
                    )
                }
            }
        }
    }
}

@Composable
private fun ContactAvatar(
    avatarUri: String?,
    size: Dp,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val clickableModifier = if (onClick != null) {
        modifier.clickable(onClick = onClick)
    } else {
        modifier
    }

    Surface(
        modifier = clickableModifier
            .size(size)
            .clip(CircleShape),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        if (avatarUri != null) {
            AsyncImage(
                model = avatarUri,
                contentDescription = stringResource(R.string.contact_avatar),
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } else {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = stringResource(R.string.contact_avatar),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ContactCard(
    contact: WhitelistedContact,
    isReadSmsGranted: Boolean,
    isImporting: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onRequestReadSms: () -> Unit,
    onImportHistory: (LocalDate) -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ContactAvatar(avatarUri = contact.avatarUri, size = 48.dp)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                ) {
                    Text(contact.displayName, style = MaterialTheme.typography.titleMedium)
                    Text(contact.phoneNumber.value, style = MaterialTheme.typography.bodyMedium)
                }
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.edit_contact))
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete_contact))
                }
            }
            Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)) {
                ContactSmsImportSection(
                    isReadSmsGranted = isReadSmsGranted,
                    isImporting = isImporting,
                    onRequestReadSms = onRequestReadSms,
                    onImportHistory = onImportHistory,
                )
            }
        }
    }
}

@Composable
private fun ContactEditorDialog(
    editor: EditorState,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
    onDisplayNameChange: (String) -> Unit,
    onPhoneNumberChange: (String) -> Unit,
    onPickFromContacts: () -> Unit,
    onPickAvatar: () -> Unit,
) {
    val title = if (editor.id == null) {
        stringResource(R.string.add_contact)
    } else {
        stringResource(R.string.edit_contact)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                ContactAvatar(
                    avatarUri = editor.avatarUri,
                    size = 96.dp,
                    onClick = onPickAvatar,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.change_avatar),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = editor.displayName,
                    onValueChange = onDisplayNameChange,
                    label = { Text(stringResource(R.string.display_name)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = editor.phoneNumber,
                    onValueChange = onPhoneNumberChange,
                    label = { Text(stringResource(R.string.phone_number)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(onClick = onPickFromContacts) {
                    Text(stringResource(R.string.pick_from_contacts))
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onSave,
                enabled = editor.displayName.isNotBlank() && editor.phoneNumber.isNotBlank(),
            ) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}
