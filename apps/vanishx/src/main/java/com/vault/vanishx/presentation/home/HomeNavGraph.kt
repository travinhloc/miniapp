package com.vault.vanishx.presentation.home

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.navigation
import com.vault.vanishx.presentation.AppDestination
import com.vault.vanishx.presentation.composable
import com.vault.vanishx.presentation.navigate

fun NavGraphBuilder.homeNavGraph(
    navController: NavHostController,
) {
    navigation(
        route = AppDestination.MainNavGraph.route,
        startDestination = HomeDestination.Home.destination,
    ) {
        composable(HomeDestination.Home) {
            HomeScreen(
                navigator = { destination -> navController.navigate(destination) },
            )
        }
    }
}
