package pl.diplomat.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import pl.diplomat.infrastructure.dashboard.DashboardViewModel
import pl.diplomat.infrastructure.whitelist.WhitelistViewModel
import pl.diplomat.presentation.conversation.ConversationDetailScreen
import pl.diplomat.presentation.dashboard.DashboardRoute
import pl.diplomat.presentation.navigation.DiplomatDestination
import pl.diplomat.presentation.whitelist.WhitelistRoute

@Composable
fun DiplomatApp(
    dashboardViewModel: DashboardViewModel,
    whitelistViewModel: WhitelistViewModel,
) {
    var destination by remember { mutableStateOf<DiplomatDestination>(DiplomatDestination.Dashboard) }

    when (val current = destination) {
        DiplomatDestination.Dashboard -> {
            DashboardRoute(
                viewModel = dashboardViewModel,
                onOpenWhitelist = { destination = DiplomatDestination.Whitelist },
                onThreadClick = { thread ->
                    destination = DiplomatDestination.ConversationDetail(thread)
                },
            )
        }

        DiplomatDestination.Whitelist -> {
            WhitelistRoute(
                viewModel = whitelistViewModel,
                onBack = { destination = DiplomatDestination.Dashboard },
            )
        }

        is DiplomatDestination.ConversationDetail -> {
            ConversationDetailScreen(
                thread = current.thread,
                onBack = {
                    dashboardViewModel.clearSelectedThread()
                    destination = DiplomatDestination.Dashboard
                },
            )
        }
    }
}
