package pl.diplomat.presentation

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import pl.diplomat.domain.model.WhitelistedContact
import pl.diplomat.infrastructure.conversation.ConversationDetailViewModel
import pl.diplomat.infrastructure.dashboard.DashboardViewModel
import pl.diplomat.infrastructure.llm.LlmSettingsViewModel
import pl.diplomat.infrastructure.ota.OtaUpdateViewModel
import pl.diplomat.infrastructure.whitelist.WhitelistViewModel
import pl.diplomat.presentation.conversation.ConversationDetailRoute
import pl.diplomat.presentation.dashboard.DashboardRoute
import pl.diplomat.presentation.llm.LlmSettingsRoute
import pl.diplomat.presentation.navigation.DiplomatDestination
import pl.diplomat.presentation.whitelist.WhitelistRoute

@Composable
fun DiplomatApp(
    dashboardViewModel: DashboardViewModel,
    otaUpdateViewModel: OtaUpdateViewModel,
    whitelistViewModel: WhitelistViewModel,
    llmSettingsViewModel: LlmSettingsViewModel,
    conversationDetailViewModelFactory: (WhitelistedContact) -> ConversationDetailViewModel,
    onSmsPermissionGranted: () -> Unit = {},
) {
    var destination by remember { mutableStateOf<DiplomatDestination>(DiplomatDestination.Dashboard) }

    BackHandler(enabled = destination != DiplomatDestination.Dashboard) {
        when (val current = destination) {
            DiplomatDestination.Whitelist -> destination = DiplomatDestination.Dashboard
            DiplomatDestination.LlmSettings -> destination = DiplomatDestination.Dashboard
            is DiplomatDestination.ConversationDetail -> {
                dashboardViewModel.clearSelectedThread()
                destination = DiplomatDestination.Dashboard
            }
            DiplomatDestination.Dashboard -> Unit
        }
    }

    when (val current = destination) {
        DiplomatDestination.Dashboard -> {
            DashboardRoute(
                viewModel = dashboardViewModel,
                otaUpdateViewModel = otaUpdateViewModel,
                onOpenWhitelist = { destination = DiplomatDestination.Whitelist },
                onOpenLlmSettings = { destination = DiplomatDestination.LlmSettings },
                onThreadClick = { thread ->
                    destination = DiplomatDestination.ConversationDetail(thread)
                },
                onSmsPermissionGranted = onSmsPermissionGranted,
            )
        }

        DiplomatDestination.Whitelist -> {
            WhitelistRoute(
                viewModel = whitelistViewModel,
                onBack = { destination = DiplomatDestination.Dashboard },
            )
        }

        DiplomatDestination.LlmSettings -> {
            LlmSettingsRoute(
                viewModel = llmSettingsViewModel,
                onBack = { destination = DiplomatDestination.Dashboard },
            )
        }

        is DiplomatDestination.ConversationDetail -> {
            val detailViewModel = remember(current.thread.contact.id) {
                conversationDetailViewModelFactory(current.thread.contact)
            }
            ConversationDetailRoute(
                thread = current.thread,
                viewModel = detailViewModel,
                onBack = {
                    dashboardViewModel.clearSelectedThread()
                    destination = DiplomatDestination.Dashboard
                },
            )
        }
    }
}
