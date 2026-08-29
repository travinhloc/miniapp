package com.vault.vanishx.presentation.home

import app.cash.turbine.test
import com.vault.vanishx.data.invite.PendingInviteStore
import com.vault.vanishx.domain.model.Identity
import com.vault.vanishx.domain.model.MailboxRoom
import com.vault.vanishx.domain.repository.MailboxRepository
import com.vault.vanishx.domain.repository.ProEntitlementRepository
import com.vault.vanishx.domain.usecase.EnsureIdentityUseCase
import com.vault.vanishx.domain.usecase.SyncActiveMailboxesResult
import com.vault.vanishx.domain.usecase.SyncActiveMailboxesUseCase
import com.vault.vanishx.presentation.mailbox.MailboxDestination
import com.vault.vanishx.presentation.security.SecurityDestination
import com.vault.vanishx.test.CoroutineTestRule
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
class HomeViewModelTest {

    @get:Rule
    val coroutinesRule = CoroutineTestRule()

    private val ensureIdentity: EnsureIdentityUseCase = mockk()
    private val syncActiveMailboxes: SyncActiveMailboxesUseCase = mockk()
    private val mailboxRepository: MailboxRepository = mockk(relaxed = true)
    private val pendingInviteStore: PendingInviteStore = mockk(relaxed = true)
    private val pendingFlow = MutableStateFlow<String?>(null)
    private val proEntitlement: ProEntitlementRepository = mockk(relaxed = true)
    private val proFlow = MutableStateFlow(false)

    private lateinit var viewModel: HomeViewModel

    @Before
    fun setUp() {
        coEvery { ensureIdentity() } returns Identity(
            anonymousId = "vx_home",
            publicKeyBase64 = "pub",
        )
        every { pendingInviteStore.peek() } answers { pendingFlow.value }
        every { pendingInviteStore.pending } returns pendingFlow
        coEvery { mailboxRepository.getAllRooms() } returns emptyList()
        coEvery { syncActiveMailboxes() } returns SyncActiveMailboxesResult(
            activeCount = 0,
            purgedCount = 0,
            syncedCount = 0,
            syncFailures = 0,
        )
        every { proEntitlement.isPro } returns proFlow
        every { proEntitlement.isProNow() } returns false
        pendingFlow.value = null
        viewModel = HomeViewModel(
            ensureIdentity = ensureIdentity,
            syncActiveMailboxes = syncActiveMailboxes,
            mailboxRepository = mailboxRepository,
            pendingInviteStore = pendingInviteStore,
            proEntitlement = proEntitlement,
            dispatchersProvider = coroutinesRule.testDispatcherProvider,
        )
    }

    @Test
    fun `bootstraps anonymous id on init`() = runTest {
        viewModel.uiState.test {
            expectMostRecentItem().anonymousId shouldBe "vx_home"
        }
    }

    @Test
    fun `CreateRoom navigates to create destination`() = runTest {
        viewModel.navigator.test {
            viewModel.onAction(HomeAction.CreateRoom)
            awaitItem() shouldBe MailboxDestination.Create
        }
    }

    @Test
    fun `JoinRoom navigates to join destination`() = runTest {
        viewModel.navigator.test {
            viewModel.onAction(HomeAction.JoinRoom)
            awaitItem() shouldBe MailboxDestination.Join
        }
    }

    @Test
    fun `Resume syncs active mailboxes`() = runTest {
        coEvery { syncActiveMailboxes() } returns SyncActiveMailboxesResult(
            activeCount = 2,
            purgedCount = 1,
            syncedCount = 1,
            syncFailures = 0,
        )

        viewModel.onAction(HomeAction.Resume)
        advanceUntilIdle()

        coVerify(atLeast = 1) { syncActiveMailboxes() }
        coVerify(atLeast = 1) { mailboxRepository.getAllRooms() }
    }

    @Test
    fun `pending https invite opens Join without auto-accept`() = runTest {
        viewModel.navigator.test {
            pendingFlow.value = "https://vanihx-staging.web.app/join?token=abc"
            awaitItem() shouldBe MailboxDestination.Join
        }
    }

    @Test
    fun `OpenSettings navigates to settings`() = runTest {
        viewModel.navigator.test {
            viewModel.onAction(HomeAction.OpenSettings)
            awaitItem() shouldBe SecurityDestination.Settings
        }
    }

    @Test
    fun `Home omits waiting rooms and sorts favorite then newest`() = runTest {
        val waiting = MailboxRoom(
            id = "wait",
            roomKey = "k",
            createdAt = 400L,
            status = MailboxRoom.STATUS_ACTIVE,
            role = MailboxRoom.ROLE_CREATOR,
            activatedAt = 0L,
            favorite = true,
        )
        val favorite = MailboxRoom(
            id = "fav",
            roomKey = "k",
            createdAt = 100L,
            status = MailboxRoom.STATUS_ACTIVE,
            role = MailboxRoom.ROLE_CREATOR,
            activatedAt = 1L,
            favorite = true,
            title = "Fav",
        )
        val liveNew = MailboxRoom(
            id = "new",
            roomKey = "k",
            createdAt = 300L,
            status = MailboxRoom.STATUS_ACTIVE,
            role = MailboxRoom.ROLE_MEMBER,
            activatedAt = 1L,
            title = "New",
        )
        val liveOld = MailboxRoom(
            id = "old",
            roomKey = "k",
            createdAt = 200L,
            status = MailboxRoom.STATUS_ACTIVE,
            role = MailboxRoom.ROLE_MEMBER,
            activatedAt = 1L,
            title = "Old",
        )
        coEvery { mailboxRepository.getAllRooms() } returns listOf(waiting, liveOld, liveNew, favorite)
        viewModel = HomeViewModel(
            ensureIdentity = ensureIdentity,
            syncActiveMailboxes = syncActiveMailboxes,
            mailboxRepository = mailboxRepository,
            pendingInviteStore = pendingInviteStore,
            proEntitlement = proEntitlement,
            dispatchersProvider = coroutinesRule.testDispatcherProvider,
        )
        advanceUntilIdle()

        viewModel.uiState.value.rooms.map { it.id } shouldBe listOf("fav", "new", "old")
        viewModel.uiState.value.visibleRooms.map { it.id } shouldBe listOf("fav", "new", "old")
    }
}
