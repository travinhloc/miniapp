package com.miniapp.ui.screens.main

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.navigation
import com.miniapp.ui.AppDestination
import com.miniapp.ui.composable
import com.miniapp.ui.navigate
import com.miniapp.ui.screens.main.home.HomeScreen

fun NavGraphBuilder.mainNavGraph(
    navController: NavHostController,
) {
    navigation(
        route = AppDestination.MainNavGraph.route,
        startDestination = MainDestination.Home.destination
    ) {
        composable(MainDestination.Home) {
            HomeScreen(
                navigator = { destination -> navController.navigate(destination) }
            )
        }
    }
}
