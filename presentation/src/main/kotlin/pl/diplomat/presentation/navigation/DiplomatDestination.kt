package pl.diplomat.presentation.navigation

import pl.diplomat.domain.model.ConversationThread

sealed interface DiplomatDestination {
    data object Dashboard : DiplomatDestination
    data object Whitelist : DiplomatDestination
    data object LlmSettings : DiplomatDestination
    data class ConversationDetail(val thread: ConversationThread) : DiplomatDestination
}
