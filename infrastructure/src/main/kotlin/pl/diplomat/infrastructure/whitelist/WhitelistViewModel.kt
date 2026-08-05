package pl.diplomat.infrastructure.whitelist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import pl.diplomat.domain.model.PhoneNumber
import pl.diplomat.domain.model.WhitelistedContact
import pl.diplomat.domain.port.AvatarStoragePort
import pl.diplomat.domain.port.SystemContactsPort
import pl.diplomat.usecase.AddContactToWhitelistUseCase
import pl.diplomat.usecase.GetWhitelistedContactsUseCase
import pl.diplomat.usecase.RemoveContactFromWhitelistUseCase
import pl.diplomat.usecase.UpdateWhitelistedContactUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface WhitelistUiState {
    data object Loading : WhitelistUiState
    data class Content(
        val contacts: List<WhitelistedContact>,
        val editor: EditorState? = null,
        val message: String? = null,
    ) : WhitelistUiState

    data class Error(val message: String) : WhitelistUiState
}

data class EditorState(
    val id: Long? = null,
    val displayName: String = "",
    val phoneNumber: String = "",
    val avatarUri: String? = null,
)

class WhitelistViewModel(
    getWhitelistedContacts: GetWhitelistedContactsUseCase,
    private val addContact: AddContactToWhitelistUseCase,
    private val updateContact: UpdateWhitelistedContactUseCase,
    private val removeContactFromWhitelist: RemoveContactFromWhitelistUseCase,
    private val systemContacts: SystemContactsPort,
    private val avatarStorage: AvatarStoragePort,
) : ViewModel() {

    private val editor = MutableStateFlow<EditorState?>(null)
    private val message = MutableStateFlow<String?>(null)

    val uiState: StateFlow<WhitelistUiState> = combine(
        getWhitelistedContacts(),
        editor,
        message,
    ) { contacts, editorState, snackbar ->
        WhitelistUiState.Content(contacts, editorState, snackbar)
    }
        .catch<WhitelistUiState> { emit(WhitelistUiState.Error(it.message ?: "Unknown error")) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WhitelistUiState.Loading)

    fun openAddEditor() {
        editor.value = EditorState()
        message.value = null
    }

    fun openEditEditor(contact: WhitelistedContact) {
        editor.value = EditorState(
            id = contact.id,
            displayName = contact.displayName,
            phoneNumber = contact.phoneNumber.value,
            avatarUri = contact.avatarUri,
        )
        message.value = null
    }

    fun dismissEditor() {
        editor.value = null
    }

    fun updateEditorDisplayName(value: String) {
        editor.update { it?.copy(displayName = value) }
    }

    fun updateEditorPhoneNumber(value: String) {
        editor.update { it?.copy(phoneNumber = value) }
    }

    fun saveEditor() {
        val current = editor.value ?: return
        viewModelScope.launch {
            runCatching {
                val phone = PhoneNumber(current.phoneNumber.trim())
                if (current.id == null) {
                    addContact(current.displayName.trim(), phone, current.avatarUri)
                } else {
                    updateContact(
                        WhitelistedContact(
                            id = current.id,
                            displayName = current.displayName.trim(),
                            phoneNumber = phone,
                            avatarUri = current.avatarUri,
                        ),
                    )
                }
            }.onSuccess {
                editor.value = null
                message.value = null
            }.onFailure {
                message.value = it.message ?: "Save failed"
            }
        }
    }

    fun removeContact(id: Long) {
        viewModelScope.launch {
            runCatching { removeContactFromWhitelist(id) }
                .onFailure { message.value = it.message ?: "Delete failed" }
        }
    }

    fun importFromSystemContact(lookupUri: String) {
        viewModelScope.launch {
            runCatching {
                val deviceContact = systemContacts.lookupContact(lookupUri)
                    ?: error("Could not read selected contact")
                val avatarUri = deviceContact.avatarUri?.let { avatarStorage.saveFromUri(it) }
                editor.value = EditorState(
                    id = editor.value?.id,
                    displayName = deviceContact.displayName,
                    phoneNumber = deviceContact.phoneNumber.value,
                    avatarUri = avatarUri,
                )
            }.onFailure {
                message.value = it.message ?: "Import failed"
            }
        }
    }

    fun setEditorAvatarFromUri(sourceUri: String) {
        viewModelScope.launch {
            runCatching {
                val localUri = avatarStorage.saveFromUri(sourceUri)
                editor.update { it?.copy(avatarUri = localUri) }
            }.onFailure {
                message.value = it.message ?: "Could not save avatar"
            }
        }
    }

    fun clearMessage() {
        message.value = null
    }
}
