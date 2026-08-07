package com.vault.vanishx.presentation

import androidx.compose.runtime.Composable
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navDeepLink
import com.miniapp.core.mvvm.BaseDestination
import com.vault.vanishx.presentation.home.homeNavGraph
import com.vault.vanishx.presentation.mailbox.MailboxDestination

@Composable
fun AppNavGraph(
    navController: NavHostController,
) {
    NavHost(
        navController = navController,
        route = AppDestination.RootNavGraph.route,
        startDestination = AppDestination.MainNavGraph.destination,
    ) {
        homeNavGraph(navController = navController)
    }
}

fun NavGraphBuilder.composable(
    destination: BaseDestination,
    content: @Composable (NavBackStackEntry) -> Unit,
) {
    composable(
        route = destination.route,
        arguments = destination.arguments,
        deepLinks = destination.deepLinks.map {
            navDeepLink {
                uriPattern = it
            }
        },
        content = content,
    )
}

fun NavHostController.navigate(destination: BaseDestination) {
    when (destination) {
        is BaseDestination.Up -> {
            destination.results.forEach { (key, value) ->
                previousBackStackEntry?.savedStateHandle?.set(key, value)
            }
            navigateUp()
        }
        is MailboxDestination.Room -> {
            navigate(route = destination.destination) {
                popUpTo(MailboxDestination.Join.destination) {
                    inclusive = true
                }
                launchSingleTop = true
            }
        }
        else -> navigate(route = destination.destination)
    }
}
