package com.vault.vanishx.presentation.home

import app.cash.turbine.test
import com.vault.vanishx.domain.model.Identity
import com.vault.vanishx.domain.usecase.ConsumePendingInviteUseCase
import com.vault.vanishx.domain.usecase.EnsureIdentityUseCase
import com.vault.vanishx.domain.usecase.SmokeMailboxRemoteUseCase
import com.vault.vanishx.domain.usecase.SyncActiveMailboxesResult
import com.vault.vanishx.domain.usecase.SyncActiveMailboxesUseCase
import com.vault.vanishx.presentation.mailbox.MailboxDestination
import com.vault.vanishx.test.CoroutineTestRule
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
    private val consumePendingInvite: ConsumePendingInviteUseCase = mockk()
    private val smokeMailboxRemote: SmokeMailboxRemoteUseCase = mockk()

    private lateinit var viewModel: HomeViewModel

    @Before
    fun setUp() {
        coEvery { ensureIdentity() } returns Identity(
            anonymousId = "vx_home",
            publicKeyBase64 = "pub",
        )
        coEvery { consumePendingInvite() } returns null
        coEvery { syncActiveMailboxes() } returns SyncActiveMailboxesResult(
            activeCount = 0,
            purgedCount = 0,
            syncedCount = 0,
            syncFailures = 0,
        )
        viewModel = HomeViewModel(
            ensureIdentity = ensureIdentity,
            syncActiveMailboxes = syncActiveMailboxes,
            consumePendingInvite = consumePendingInvite,
            smokeMailboxRemote = smokeMailboxRemote,
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
    fun `Resume syncs active mailboxes and updates count`() = runTest {
        coEvery { syncActiveMailboxes() } returns SyncActiveMailboxesResult(
            activeCount = 2,
            purgedCount = 1,
            syncedCount = 1,
            syncFailures = 0,
        )

        viewModel.onAction(HomeAction.Resume)
        advanceUntilIdle()

        viewModel.uiState.test {
            expectMostRecentItem().activeRoomCount shouldBe 2
        }
        coVerify(atLeast = 1) { syncActiveMailboxes() }
    }

    @Test
    fun `bootstraps consumes pending invite`() = runTest {
        advanceUntilIdle()
        coVerify(exactly = 1) { consumePendingInvite() }
    }
}
