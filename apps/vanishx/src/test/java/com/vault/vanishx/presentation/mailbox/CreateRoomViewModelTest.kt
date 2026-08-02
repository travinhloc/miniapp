package com.vault.vanishx.presentation.mailbox

import app.cash.turbine.test
import com.miniapp.core.mvvm.BaseDestination
import com.vault.vanishx.domain.model.MailboxRoom
import com.vault.vanishx.domain.model.RoomInvite
import com.vault.vanishx.domain.model.RoomTtlOption
import com.vault.vanishx.domain.usecase.CreateRoomUseCase
import com.vault.vanishx.domain.usecase.CreatedRoom
import com.vault.vanishx.test.CoroutineTestRule
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
class CreateRoomViewModelTest {

    @get:Rule
    val coroutinesRule = CoroutineTestRule()

    private val createRoom: CreateRoomUseCase = mockk()

    private fun viewModel() = CreateRoomViewModel(
        createRoom = createRoom,
        dispatchersProvider = coroutinesRule.testDispatcherProvider,
    )

    private fun createdRoom() = CreatedRoom(
        room = MailboxRoom(
            id = "room1",
            roomKey = "key1",
            createdAt = 0L,
            expiresAt = 1_000L,
            role = MailboxRoom.ROLE_CREATOR,
        ),
        invite = RoomInvite(roomId = "room1", roomKey = "key1", expiresAt = 1_000L),
    )

    @Test
    fun `defaults to Instant mode with 24h TTL`() = runTest {
        val viewModel = viewModel()
        viewModel.uiState.value.mode shouldBe CreateRoomMode.INSTANT
    }

    @Test
    fun `Instant create opens Room with invite sheet`() = runTest {
        coEvery { createRoom(ttl = RoomTtlOption.ONE_DAY, icebreaker = null) } returns createdRoom()
        val viewModel = viewModel()

        viewModel.navigator.test {
            viewModel.onAction(CreateRoomAction.Create)
            awaitItem() shouldBe MailboxDestination.Room(roomId = "room1", openInvite = true)
        }
        coVerify { createRoom(ttl = RoomTtlOption.ONE_DAY, icebreaker = null) }
    }

    @Test
    fun `Later create pops back Home with invite uri result`() = runTest {
        coEvery { createRoom(ttl = RoomTtlOption.ONE_DAY, icebreaker = null) } returns createdRoom()
        val viewModel = viewModel()

        viewModel.onAction(CreateRoomAction.SelectMode(CreateRoomMode.LATER))
        viewModel.navigator.test {
            viewModel.onAction(CreateRoomAction.Create)
            val destination = awaitItem()
            destination shouldBe BaseDestination.Up(
                results = hashMapOf("inviteUri" to createdRoom().invite.toUriString()),
            )
        }
    }

    @Test
    fun `icebreaker is passed through and trimmed to max length`() = runTest {
        coEvery { createRoom(ttl = RoomTtlOption.ONE_DAY, icebreaker = "Hi there") } returns createdRoom()
        val viewModel = viewModel()

        viewModel.onAction(CreateRoomAction.IcebreakerChanged("Hi there"))
        viewModel.navigator.test {
            viewModel.onAction(CreateRoomAction.Create)
            awaitItem()
        }
        coVerify { createRoom(ttl = RoomTtlOption.ONE_DAY, icebreaker = "Hi there") }
    }

    @Test
    fun `Back emits Up`() = runTest {
        val viewModel = viewModel()
        viewModel.navigator.test {
            viewModel.onAction(CreateRoomAction.Back)
            awaitItem() shouldBe BaseDestination.Up()
        }
    }
}
