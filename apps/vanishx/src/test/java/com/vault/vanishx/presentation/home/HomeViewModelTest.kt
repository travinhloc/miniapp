package com.vault.vanishx.presentation.home

import app.cash.turbine.test
import com.miniapp.core.common.DispatchersProvider
import com.vault.vanishx.data.invite.PendingInviteStore
import com.vault.vanishx.domain.model.Identity
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
import kotlinx.coroutines.test.StandardTestDispatcher
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
    private val proEntitlement: ProEntitlementRepository = mockk(relaxed = true)
    private val proFlow = MutableStateFlow(false)

    private lateinit var viewModel: HomeViewModel

    @Before
    fun setUp() {
        coEvery { ensureIdentity() } returns Identity(
            anonymousId = "vx_home",
            publicKeyBase64 = "pub",
        )
        every { pendingInviteStore.peek() } returns null
        coEvery { mailboxRepository.getAllRooms() } returns emptyList()
        coEvery { syncActiveMailboxes() } returns SyncActiveMailboxesResult(
            activeCount = 0,
            purgedCount = 0,
            syncedCount = 0,
            syncFailures = 0,
        )
        every { proEntitlement.isPro } returns proFlow
        every { proEntitlement.isProNow() } returns false
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
    fun `bootstrap routes a pending invite to the Join message request instead of auto-accepting`() =
        runTest(coroutinesRule.testDispatcher) {
            every { pendingInviteStore.peek() } returns "vanishx://r/room1?k=key1"
            // bootstrapIdentity() flows through flowOn(io); queuing io on a Standard dispatcher
            // (sharing the same scheduler) keeps it from racing ahead of navigator subscription,
            // matching how a real (suspending) identity lookup behaves versus this test's mock.
            val queuedProvider = object : DispatchersProvider {
                override val io = StandardTestDispatcher(coroutinesRule.testDispatcher.scheduler)
                override val main = coroutinesRule.testDispatcher
                override val default = io
            }
            val vm = HomeViewModel(
                ensureIdentity = ensureIdentity,
                syncActiveMailboxes = syncActiveMailboxes,
                mailboxRepository = mailboxRepository,
                pendingInviteStore = pendingInviteStore,
                proEntitlement = proEntitlement,
                dispatchersProvider = queuedProvider,
            )

            vm.navigator.test {
                advanceUntilIdle()
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
}
