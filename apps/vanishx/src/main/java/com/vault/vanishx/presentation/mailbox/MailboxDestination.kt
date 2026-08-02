package com.vault.vanishx.presentation.mailbox

import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.miniapp.core.mvvm.BaseDestination

sealed class MailboxDestination {

    object Create : BaseDestination("mailbox/create")

    object Join : BaseDestination("mailbox/join?roomId={roomId}&roomKey={roomKey}&expiresAt={expiresAt}") {
        override var destination: String = "mailbox/join"

        override val arguments = listOf(
            navArgument("roomId") {
                type = NavType.StringType
                nullable = true
                defaultValue = null
            },
            navArgument("roomKey") {
                type = NavType.StringType
                nullable = true
                defaultValue = null
            },
            navArgument("expiresAt") {
                type = NavType.StringType
                nullable = true
                defaultValue = null
            },
        )

        override val deepLinks = listOf(
            "vanishx://r/{roomId}?k={roomKey}&e={expiresAt}",
            "vanishx://r/{roomId}?k={roomKey}",
        )
    }

    data class Room(
        val roomId: String,
        /** True right after Instant create, so Room auto-opens the invite sheet (story 7.5). */
        val openInvite: Boolean = false,
    ) : BaseDestination("mailbox/room/{roomId}?openInvite={openInvite}") {
        override val arguments = listOf(
            navArgument("roomId") { type = NavType.StringType },
            navArgument("openInvite") {
                type = NavType.BoolType
                defaultValue = false
            },
        )

        override var destination: String = "mailbox/room/$roomId?openInvite=$openInvite"

        override val deepLinks = listOf(
            "vanishx://open/{roomId}",
        )
    }
}
