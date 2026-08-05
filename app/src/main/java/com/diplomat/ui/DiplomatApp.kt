package com.diplomat.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.diplomat.presentation.whitelist.WhitelistScreen
import com.diplomat.ui.dashboard.DashboardScreen
import com.diplomat.ui.dashboard.PermissionBannerState
import com.diplomat.ui.decision.DecisionScreen
import com.diplomat.ui.navigation.DiplomatDestinations
import com.diplomat.util.PermissionUtils

/**
 * Root composable: hosts navigation between the dashboard and the decision
 * screen, and refreshes permission state whenever the app resumes (the user
 * may have toggled settings in another screen).
 */
@Composable
fun DiplomatApp() {
    val navController = rememberNavController()
    val context = LocalContext.current

    var notificationAccess by remember { mutableStateOf(false) }
    var batteryExempt by remember { mutableStateOf(false) }

    LifecycleResumeEffect(Unit) {
        notificationAccess = PermissionUtils.isNotificationListenerEnabled(context)
        batteryExempt = PermissionUtils.isIgnoringBatteryOptimizations(context)
        onPauseOrDispose { }
    }

    NavHost(
        navController = navController,
        startDestination = DiplomatDestinations.DASHBOARD,
    ) {
        composable(DiplomatDestinations.DASHBOARD) {
            DashboardScreen(
                onOpenDecision = { id ->
                    navController.navigate(DiplomatDestinations.decision(id))
                },
                onOpenWhitelist = {
                    navController.navigate(DiplomatDestinations.WHITELIST)
                },
                permissionState = PermissionBannerState(
                    notificationAccessGranted = notificationAccess,
                    batteryOptimizationDisabled = batteryExempt,
                    onGrantNotificationAccess = {
                        PermissionUtils.openNotificationListenerSettings(context)
                    },
                    onDisableBatteryOptimization = {
                        PermissionUtils.requestIgnoreBatteryOptimizations(context)
                    },
                ),
            )
        }

        composable(DiplomatDestinations.WHITELIST) {
            WhitelistScreen(onBack = { navController.popBackStack() })
        }

        composable(
            route = DiplomatDestinations.DECISION_ROUTE,
            arguments = listOf(
                navArgument(DiplomatDestinations.DECISION_ARG_ID) { type = NavType.StringType },
            ),
        ) {
            DecisionScreen(onBack = { navController.popBackStack() })
        }
    }
}
