package com.vault.vanishx.presentation.mailbox

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import com.vault.vanishx.presentation.composable
import com.vault.vanishx.presentation.navigate

fun NavGraphBuilder.mailboxNavGraph(
    navController: NavHostController,
) {
    composable(MailboxDestination.Create) {
        CreateRoomScreen(navigator = { navController.navigate(it) })
    }
    composable(MailboxDestination.Join) {
        JoinRoomScreen(navigator = { navController.navigate(it) })
    }
    composable(MailboxDestination.Room(roomId = "{roomId}")) {
        RoomScreen(navigator = { navController.navigate(it) })
    }
}
