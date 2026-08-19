package com.vault.vanishx.presentation

import androidx.compose.runtime.Composable
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navDeepLink
import com.miniapp.core.mvvm.BaseDestination
import com.vault.vanishx.presentation.extensions.collectAsEffect
import com.vault.vanishx.presentation.home.homeNavGraph
import com.vault.vanishx.presentation.mailbox.MailboxDestination
import com.vault.vanishx.presentation.mailbox.PendingOpenRoomStore

@Composable
fun AppNavGraph(
    navController: NavHostController,
    pendingOpenRoom: PendingOpenRoomStore,
) {
    pendingOpenRoom.roomId.collectAsEffect { roomId ->
        if (roomId.isNullOrBlank()) return@collectAsEffect
        pendingOpenRoom.consume()
        navController.navigate(MailboxDestination.Room(roomId))
    }
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
            if (!popBackStack(MailboxDestination.Create.destination, inclusive = true)) {
                popBackStack(MailboxDestination.Join.destination, inclusive = true)
            }
            navigate(route = destination.destination) {
                launchSingleTop = true
            }
        }
        else -> navigate(route = destination.destination)
    }
}
