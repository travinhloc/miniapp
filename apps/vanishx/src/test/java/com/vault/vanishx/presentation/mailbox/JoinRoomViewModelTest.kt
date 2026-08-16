package com.vault.vanishx.presentation.mailbox

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.miniapp.core.mvvm.BaseDestination
import com.vault.vanishx.data.invite.InviteJoinCleanup
import com.vault.vanishx.data.invite.PendingInviteStore
import com.vault.vanishx.data.remote.InMemoryMailboxRemoteDataSource
import com.vault.vanishx.data.remote.RemoteRoomMeta
import com.vault.vanishx.domain.model.MailboxRoom
import com.vault.vanishx.domain.repository.BlockRepository
import com.vault.vanishx.domain.usecase.JoinRoomUseCase
import com.vault.vanishx.domain.usecase.VerifyInviteUseCase
import com.vault.vanishx.test.CoroutineTestRule
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
class JoinRoomViewModelTest {

    @get:Rule
    val coroutinesRule = CoroutineTestRule()

    private val joinRoom: JoinRoomUseCase = mockk()
    private val pendingInviteStore: PendingInviteStore = mockk(relaxed = true)
    private val blockRepository: BlockRepository = mockk(relaxed = true)
    private val remote = InMemoryMailboxRemoteDataSource()
    private val joinCleanup: InviteJoinCleanup = mockk(relaxed = true)

    private fun verifyInvite() = VerifyInviteUseCase(remote, pendingInviteStore, joinCleanup)

    private fun viewModel(savedState: Map<String, String?> = emptyMap()) = JoinRoomViewModel(
        savedStateHandle = SavedStateHandle(savedState),
        joinRoom = joinRoom,
        pendingInviteStore = pendingInviteStore,
        verifyInvite = verifyInvite(),
        blockRepository = blockRepository,
        dispatchersProvider = coroutinesRule.testDispatcherProvider,
    )

    private suspend fun writeMeta(roomId: String, icebreaker: String? = null, creatorPub: String? = "hostPub") {
        remote.writeRoomMeta(
            roomId,
            RemoteRoomMeta(
                createdAt = System.currentTimeMillis(),
                expiresAt = 0L,
                creatorPub = creatorPub,
                icebreaker = icebreaker,
                hostPro = false,
            ),
        )
    }

    @Test
    fun `RequestPreview loads icebreaker and creator pub from remote meta`() = runTest {
        every { pendingInviteStore.peek() } returns null
        writeMeta("r1", icebreaker = "Still interested?")
        val viewModel = viewModel()

        viewModel.onAction(JoinRoomAction.InputChanged("vanishx://r/r1?k=key1"))
        viewModel.onAction(JoinRoomAction.RequestPreview)
        advanceUntilIdle()

        val preview = viewModel.uiState.value.preview
        preview?.icebreaker shouldBe "Still interested?"
        preview?.creatorPub shouldBe "hostPub"
        viewModel.uiState.value.nickname.isBlank() shouldBe false
    }

    @Test
    fun `AcceptAndChat runs the handshake and navigates to Room`() = runTest {
        every { pendingInviteStore.peek() } returns null
        writeMeta("r1")
        coEvery { joinRoom(any<String>(), any()) } returns MailboxRoom(
            id = "r1",
            roomKey = "key1",
            role = MailboxRoom.ROLE_MEMBER,
        )
        val viewModel = viewModel()
        viewModel.onAction(JoinRoomAction.InputChanged("vanishx://r/r1?k=key1"))
        viewModel.onAction(JoinRoomAction.RequestPreview)
        advanceUntilIdle()

        viewModel.navigator.test {
            viewModel.onAction(JoinRoomAction.AcceptAndChat)
            awaitItem() shouldBe MailboxDestination.Room("r1")
        }
        coVerify(exactly = 1) { joinRoom(any<String>(), any()) }
        coVerify { pendingInviteStore.clear() }
    }

    @Test
    fun `Back keeps pending invite`() = runTest {
        every { pendingInviteStore.peek() } returns null
        val viewModel = viewModel()
        viewModel.navigator.test {
            viewModel.onAction(JoinRoomAction.Back)
            awaitItem() shouldBe BaseDestination.Up()
        }
        coVerify(exactly = 0) { pendingInviteStore.clear() }
    }

    @Test
    fun `SaveForLater never runs the handshake and keeps the invite for later`() = runTest {
        every { pendingInviteStore.peek() } returns null
        writeMeta("r1")
        val viewModel = viewModel()
        viewModel.onAction(JoinRoomAction.InputChanged("vanishx://r/r1?k=key1"))
        viewModel.onAction(JoinRoomAction.RequestPreview)
        advanceUntilIdle()

        viewModel.navigator.test {
            viewModel.onAction(JoinRoomAction.SaveForLater)
            awaitItem() shouldBe BaseDestination.Up()
        }
        coVerify(exactly = 0) { joinRoom(any<String>(), any()) }
        coVerify { pendingInviteStore.save("vanishx://r/r1?k=key1") }
    }

    @Test
    fun `ConfirmBlock blocks the host pub key without ever joining`() = runTest {
        every { pendingInviteStore.peek() } returns null
        writeMeta("r1", creatorPub = "hostPub")
        val viewModel = viewModel()
        viewModel.onAction(JoinRoomAction.InputChanged("vanishx://r/r1?k=key1"))
        viewModel.onAction(JoinRoomAction.RequestPreview)
        advanceUntilIdle()

        viewModel.onAction(JoinRoomAction.OpenBlockConfirm)
        viewModel.navigator.test {
            viewModel.onAction(JoinRoomAction.ConfirmBlock)
            awaitItem() shouldBe BaseDestination.Up()
        }
        coVerify(exactly = 0) { joinRoom(any<String>(), any()) }
        coVerify { blockRepository.block("hostPub", any()) }
        coVerify { pendingInviteStore.clear() }
    }

    @Test
    fun `missing remote meta shows JOIN-2 and does not open Request`() = runTest {
        every { pendingInviteStore.peek() } returns null
        val viewModel = viewModel()
        viewModel.onAction(JoinRoomAction.InputChanged("vanishx://r/missing?k=key1"))
        viewModel.onAction(JoinRoomAction.RequestPreview)
        advanceUntilIdle()

        viewModel.uiState.value.preview shouldBe null
        viewModel.uiState.value.errorMessage shouldBe VerifyInviteUseCase.NOT_FOUND
        coVerify { joinCleanup.clearPendingAndClipboard() }
    }

    @Test
    fun `expired remote meta shows JOIN-2`() = runTest {
        every { pendingInviteStore.peek() } returns null
        remote.writeRoomMeta(
            "r1",
            RemoteRoomMeta(
                createdAt = 1L,
                expiresAt = 1L,
                creatorPub = "hostPub",
                hostPro = false,
            ),
        )
        val viewModel = viewModel()
        viewModel.onAction(JoinRoomAction.InputChanged("vanishx://r/r1?k=key1"))
        viewModel.onAction(JoinRoomAction.RequestPreview)
        advanceUntilIdle()

        viewModel.uiState.value.preview shouldBe null
        viewModel.uiState.value.errorMessage shouldBe VerifyInviteUseCase.EXPIRED
        coVerify { joinCleanup.clearPendingAndClipboard() }
    }
}
