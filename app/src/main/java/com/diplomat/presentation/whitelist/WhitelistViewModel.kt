package com.diplomat.presentation.whitelist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.diplomat.DiplomatApplication
import com.diplomat.domain.whitelist.WhitelistedContact
import com.diplomat.usecase.whitelist.AddContactToWhitelistUseCase
import com.diplomat.usecase.whitelist.GetWhitelistedContactsUseCase
import com.diplomat.usecase.whitelist.RemoveContactFromWhitelistUseCase
import com.diplomat.usecase.whitelist.UpdateWhitelistedContactUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class WhitelistViewModel(
    getContacts: GetWhitelistedContactsUseCase,
    private val addContact: AddContactToWhitelistUseCase,
    private val updateContact: UpdateWhitelistedContactUseCase,
    private val removeContact: RemoveContactFromWhitelistUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow<WhitelistUiState>(WhitelistUiState.Loading)
    val state: StateFlow<WhitelistUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            getContacts().collect { contacts ->
                _state.update { prev ->
                    val draft = prev as? WhitelistUiState.Ready
                    WhitelistUiState.Ready(
                        contacts = contacts,
                        draftName = draft?.draftName.orEmpty(),
                        draftPhone = draft?.draftPhone.orEmpty(),
                        editingId = draft?.editingId,
                        errorMessage = draft?.errorMessage,
                    )
                }
            }
        }
    }

    fun onNameChange(value: String) = updateReady { it.copy(draftName = value, errorMessage = null) }
    fun onPhoneChange(value: String) = updateReady { it.copy(draftPhone = value, errorMessage = null) }

    fun startEdit(contact: WhitelistedContact) = updateReady {
        it.copy(
            editingId = contact.id,
            draftName = contact.displayName,
            draftPhone = contact.phoneNumber.value,
            errorMessage = null,
        )
    }

    fun cancelEdit() = updateReady {
        it.copy(editingId = null, draftName = "", draftPhone = "", errorMessage = null)
    }

    fun applyPickedContact(name: String, phone: String) = updateReady {
        it.copy(draftName = name, draftPhone = phone, errorMessage = null)
    }

    fun save() {
        val ready = _state.value as? WhitelistUiState.Ready ?: return
        viewModelScope.launch {
            val result = if (ready.editingId == null) {
                addContact(ready.draftName, ready.draftPhone)
            } else {
                updateContact(ready.editingId, ready.draftName, ready.draftPhone)
            }
            result.fold(
                onSuccess = { cancelEdit() },
                onFailure = { e ->
                    updateReady { it.copy(errorMessage = e.message ?: "Save failed") }
                },
            )
        }
    }

    fun delete(id: Long) {
        viewModelScope.launch { removeContact(id) }
    }

    private fun updateReady(block: (WhitelistUiState.Ready) -> WhitelistUiState.Ready) {
        _state.update { state ->
            (state as? WhitelistUiState.Ready)?.let(block) ?: state
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val c = (this[APPLICATION_KEY] as DiplomatApplication).container
                WhitelistViewModel(
                    getContacts = c.getWhitelistedContacts,
                    addContact = c.addContactToWhitelist,
                    updateContact = c.updateWhitelistedContact,
                    removeContact = c.removeContactFromWhitelist,
                )
            }
        }
    }
}
