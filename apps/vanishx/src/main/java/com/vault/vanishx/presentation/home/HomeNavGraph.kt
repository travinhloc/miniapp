package com.vault.vanishx.presentation.home

import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.navigation
import com.vault.vanishx.presentation.AppDestination
import com.vault.vanishx.presentation.composable
import com.vault.vanishx.presentation.history.HistoryDestination
import com.vault.vanishx.presentation.history.HistoryScreen
import com.vault.vanishx.presentation.mailbox.mailboxNavGraph
import com.vault.vanishx.presentation.navigate
import com.vault.vanishx.presentation.paywall.PaywallDestination
import com.vault.vanishx.presentation.paywall.PaywallScreen
import com.vault.vanishx.presentation.security.SecurityDestination
import com.vault.vanishx.presentation.security.SecuritySettingsScreen

fun NavGraphBuilder.homeNavGraph(
    navController: NavHostController,
) {
    navigation(
        route = AppDestination.MainNavGraph.route,
        startDestination = HomeDestination.Home.destination,
    ) {
        composable(HomeDestination.Home) { backStackEntry ->
            val inviteUri = backStackEntry.savedStateHandle.get<String>("inviteUri")
            LaunchedEffect(inviteUri) {
                if (inviteUri != null) {
                    backStackEntry.savedStateHandle.remove<String>("inviteUri")
                }
            }
            HomeScreen(
                navigator = { destination -> navController.navigate(destination) },
                createdInviteUri = inviteUri,
            )
        }
        composable(SecurityDestination.Settings) {
            SecuritySettingsScreen(
                navigator = { destination -> navController.navigate(destination) },
            )
        }
        composable(HistoryDestination.History) {
            HistoryScreen(
                navigator = { destination -> navController.navigate(destination) },
            )
        }
        composable(PaywallDestination.Paywall) {
            PaywallScreen(
                navigator = { destination -> navController.navigate(destination) },
            )
        }
        mailboxNavGraph(navController = navController)
    }
}
