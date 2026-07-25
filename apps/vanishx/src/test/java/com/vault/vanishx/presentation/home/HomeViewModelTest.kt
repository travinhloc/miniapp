package com.vault.vanishx.presentation.home

import app.cash.turbine.test
import com.vault.vanishx.domain.model.Identity
import com.vault.vanishx.domain.model.MailboxRoom
import com.vault.vanishx.domain.repository.MailboxRepository
import com.vault.vanishx.domain.usecase.EnsureIdentityUseCase
import com.vault.vanishx.domain.usecase.SmokeMailboxRemoteUseCase
import com.vault.vanishx.test.CoroutineTestRule
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
class HomeViewModelTest {

    @get:Rule
    val coroutinesRule = CoroutineTestRule()

    private val mailboxRepository: MailboxRepository = mockk()
    private val ensureIdentity: EnsureIdentityUseCase = mockk()
    private val smokeMailboxRemote: SmokeMailboxRemoteUseCase = mockk()

    private lateinit var viewModel: HomeViewModel

    @Before
    fun setUp() {
        coEvery { mailboxRepository.getActiveRooms() } returns emptyList()
        coEvery { ensureIdentity() } returns Identity(
            anonymousId = "vx_home",
            publicKeyBase64 = "pub",
        )
        viewModel = HomeViewModel(
            mailboxRepository = mailboxRepository,
            ensureIdentity = ensureIdentity,
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
    fun `CreateRoom shows placeholder then can be cleared`() = runTest {
        viewModel.uiState.test {
            awaitItem()

            viewModel.onAction(HomeAction.CreateRoom)
            awaitItem().showPlaceholder shouldBe true

            viewModel.onAction(HomeAction.ClearPlaceholder)
            awaitItem().showPlaceholder shouldBe false
        }
    }

    @Test
    fun `JoinRoom shows placeholder`() = runTest {
        viewModel.uiState.test {
            awaitItem()

            viewModel.onAction(HomeAction.JoinRoom)
            awaitItem().showPlaceholder shouldBe true
        }
    }

    @Test
    fun `Loads active room count on init`() = runTest {
        coEvery { mailboxRepository.getActiveRooms() } returns listOf(MailboxRoom(id = "r1"))
        viewModel = HomeViewModel(
            mailboxRepository = mailboxRepository,
            ensureIdentity = ensureIdentity,
            smokeMailboxRemote = smokeMailboxRemote,
            dispatchersProvider = coroutinesRule.testDispatcherProvider,
        )

        viewModel.uiState.test {
            expectMostRecentItem().activeRoomCount shouldBe 1
        }
    }
}
