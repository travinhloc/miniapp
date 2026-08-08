package com.vault.vanishx.presentation.mailbox

import androidx.lifecycle.SavedStateHandle
import com.vault.vanishx.data.remote.MailboxRemoteDataSource
import com.vault.vanishx.domain.model.Identity
import com.vault.vanishx.domain.model.MailboxRoom
import com.vault.vanishx.domain.repository.IdentityRepository
import com.vault.vanishx.domain.repository.MailboxRepository
import com.vault.vanishx.domain.repository.ProEntitlementRepository
import com.vault.vanishx.domain.usecase.BlockPeerUseCase
import com.vault.vanishx.domain.usecase.GetRoomUseCase
import com.vault.vanishx.domain.usecase.PingPeerResult
import com.vault.vanishx.domain.usecase.PingPeerUseCase
import com.vault.vanishx.domain.usecase.PingRoomUseCase
import com.vault.vanishx.domain.usecase.PurgeExpiredRoomUseCase
import com.vault.vanishx.domain.usecase.RecallRoomMessageUseCase
import com.vault.vanishx.domain.usecase.ReportRoomUseCase
import com.vault.vanishx.domain.usecase.SendRoomMessageUseCase
import com.vault.vanishx.domain.usecase.SyncMailboxResult
import com.vault.vanishx.domain.usecase.SyncRoomMailboxUseCase
import com.vault.vanishx.test.CoroutineTestRule
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
class RoomViewModelTest {

    @get:Rule
    val coroutinesRule = CoroutineTestRule()

    private val getRoom: GetRoomUseCase = mockk()
    private val sendRoomMessage: SendRoomMessageUseCase = mockk(relaxed = true)
    private val syncRoomMailbox: SyncRoomMailboxUseCase = mockk()
    private val purgeExpiredRoom: PurgeExpiredRoomUseCase = mockk(relaxed = true)
    private val pingRoom: PingRoomUseCase = mockk(relaxed = true)
    private val pingPeer: PingPeerUseCase = mockk()
    private val blockPeer: BlockPeerUseCase = mockk(relaxed = true)
    private val reportRoom: ReportRoomUseCase = mockk(relaxed = true)
    private val recallRoomMessage: RecallRoomMessageUseCase = mockk(relaxed = true)
    private val renameRoom: com.vault.vanishx.domain.usecase.RenameRoomUseCase = mockk(relaxed = true)
    private val deleteLocalMessage: com.vault.vanishx.domain.usecase.DeleteLocalMessageUseCase =
        mockk(relaxed = true)
    private val mailboxRepository: MailboxRepository = mockk(relaxed = true)
    private val identityRepository: IdentityRepository = mockk()
    private val proEntitlement: ProEntitlementRepository = mockk(relaxed = true)
    private val remote: MailboxRemoteDataSource = mockk(relaxed = true)

    private fun waitingRoom(role: String = MailboxRoom.ROLE_CREATOR) = MailboxRoom(
        id = "room1",
        roomKey = "key1",
        createdAt = 0L,
        expiresAt = 0L,
        status = MailboxRoom.STATUS_ACTIVE,
        role = role,
        peerPub = null,
    )

    private fun liveRoom() = waitingRoom().copy(peerPub = "peerPubKey")

    private fun viewModel(room: MailboxRoom?): RoomViewModel {
        coEvery { getRoom("room1") } returns room
        coEvery { syncRoomMailbox("room1") } returns SyncMailboxResult(
            messages = emptyList(),
            ingested = 0,
            removedRemote = 0,
            decryptFailures = 0,
        )
        coEvery { identityRepository.ensureIdentity() } returns Identity("vx_me", "pubMe+/=")
        every { proEntitlement.isPro } returns MutableStateFlow(false)
        every { proEntitlement.isProNow() } returns false
        every { remote.observeMessages("room1") } returns emptyFlow()
        every { remote.observePresence("room1") } returns emptyFlow()
        every { remote.observeReadWatermarks("room1") } returns emptyFlow()
        every { remote.observeTyping("room1") } returns emptyFlow()
        every { remote.observeReactions("room1") } returns emptyFlow()

        val refreshRoomMeta: com.vault.vanishx.domain.usecase.RefreshRoomMetaUseCase = mockk(relaxed = true)
        return RoomViewModel(
            savedStateHandle = SavedStateHandle(mapOf("roomId" to "room1")),
            getRoom = getRoom,
            refreshRoomMeta = refreshRoomMeta,
            sendRoomMessage = sendRoomMessage,
            syncRoomMailbox = syncRoomMailbox,
            purgeExpiredRoom = purgeExpiredRoom,
            pingRoom = pingRoom,
            pingPeerUseCase = pingPeer,
            blockPeer = blockPeer,
            reportRoom = reportRoom,
            recallRoomMessage = recallRoomMessage,
            renameRoom = renameRoom,
            deleteLocalMessage = deleteLocalMessage,
            mailboxRepository = mailboxRepository,
            identityRepository = identityRepository,
            proEntitlement = proEntitlement,
            remote = remote,
            dispatchersProvider = coroutinesRule.testDispatcherProvider,
        )
    }

    @Test
    fun `PingPeer while waiting emits Sent event`() = runTest {
        every { pingPeer(any(), any()) } returns PingPeerResult(sent = true, cooldownRemainingMs = 0L)
        val viewModel = viewModel(waitingRoom())

        viewModel.onAction(RoomAction.PingPeer)

        viewModel.uiState.value.pingPeerEvent shouldBe PingPeerEvent.Sent
    }

    @Test
    fun `PingPeer within cooldown emits Cooldown event with rounded seconds`() = runTest {
        every { pingPeer(any(), any()) } returns PingPeerResult(sent = false, cooldownRemainingMs = 4_500L)
        val viewModel = viewModel(waitingRoom())

        viewModel.onAction(RoomAction.PingPeer)

        viewModel.uiState.value.pingPeerEvent shouldBe PingPeerEvent.Cooldown(secondsRemaining = 5)
    }

    @Test
    fun `PingPeer is ignored once room is Live`() = runTest {
        val viewModel = viewModel(liveRoom())

        viewModel.onAction(RoomAction.PingPeer)

        viewModel.uiState.value.pingPeerEvent shouldBe null
        verify(exactly = 0) { pingPeer(any(), any()) }
    }

    @Test
    fun `ConsumePingPeerEvent clears the event`() = runTest {
        every { pingPeer(any(), any()) } returns PingPeerResult(sent = true, cooldownRemainingMs = 0L)
        val viewModel = viewModel(waitingRoom())
        viewModel.onAction(RoomAction.PingPeer)

        viewModel.onAction(RoomAction.ConsumePingPeerEvent)

        viewModel.uiState.value.pingPeerEvent shouldBe null
    }

    @Test
    fun `member role waiting room also allows PingPeer`() = runTest {
        every { pingPeer(any(), any()) } returns PingPeerResult(sent = true, cooldownRemainingMs = 0L)
        val viewModel = viewModel(waitingRoom(role = MailboxRoom.ROLE_MEMBER))

        viewModel.onAction(RoomAction.PingPeer)

        viewModel.uiState.value.pingPeerEvent shouldBe PingPeerEvent.Sent
    }
}
