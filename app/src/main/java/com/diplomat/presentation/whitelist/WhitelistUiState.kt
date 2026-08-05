package com.diplomat.presentation.whitelist

import com.diplomat.domain.whitelist.WhitelistedContact

sealed interface WhitelistUiState {
    data object Loading : WhitelistUiState

    data class Ready(
        val contacts: List<WhitelistedContact>,
        val draftName: String = "",
        val draftPhone: String = "",
        val editingId: Long? = null,
        val errorMessage: String? = null,
    ) : WhitelistUiState
}
