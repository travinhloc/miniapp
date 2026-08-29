package com.vault.vanishx.presentation.history

import app.cash.turbine.test
import com.vault.vanishx.domain.model.MailboxRoom
import com.vault.vanishx.domain.repository.MailboxRepository
import com.vault.vanishx.domain.repository.ProEntitlementRepository
import com.vault.vanishx.test.CoroutineTestRule
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
class HistoryViewModelTest {

    @get:Rule
    val coroutinesRule = CoroutineTestRule()

    private val mailboxRepository: MailboxRepository = mockk(relaxed = true)
    private val proEntitlement: ProEntitlementRepository = mockk(relaxed = true)
    private val proFlow = MutableStateFlow(false)

    @Test
    fun `Waiting filter lists inactive rooms`() = runTest {
        every { proEntitlement.isPro } returns proFlow
        every { proEntitlement.isProNow() } returns false
        coEvery { mailboxRepository.getAllRooms() } returns listOf(
            MailboxRoom(
                id = "wait",
                roomKey = "k",
                createdAt = 10L,
                status = MailboxRoom.STATUS_ACTIVE,
                role = MailboxRoom.ROLE_CREATOR,
                activatedAt = 0L,
                title = "Waiting",
            ),
        )

        val viewModel = HistoryViewModel(
            mailboxRepository = mailboxRepository,
            proEntitlement = proEntitlement,
            dispatchersProvider = coroutinesRule.testDispatcherProvider,
        )
        advanceUntilIdle()
        viewModel.onAction(HistoryAction.SetFilter(HistoryRoomFilter.Waiting))
        advanceUntilIdle()

        viewModel.uiState.test {
            val state = expectMostRecentItem()
            state.filter shouldBe HistoryRoomFilter.Waiting
            state.rooms.map { it.row.id } shouldBe listOf("wait")
            state.rooms.single().meta shouldBe HistoryRoomMeta.Waiting
            state.rooms.single().row.isWaiting shouldBe true
        }
    }

    @Test
    fun `Open filter omits waiting rooms`() = runTest {
        every { proEntitlement.isPro } returns proFlow
        every { proEntitlement.isProNow() } returns false
        coEvery { mailboxRepository.getAllRooms() } returns listOf(
            MailboxRoom(
                id = "wait",
                roomKey = "k",
                createdAt = 10L,
                status = MailboxRoom.STATUS_ACTIVE,
                role = MailboxRoom.ROLE_CREATOR,
                activatedAt = 0L,
                title = "Waiting",
            ),
            MailboxRoom(
                id = "live",
                roomKey = "k",
                createdAt = 20L,
                status = MailboxRoom.STATUS_ACTIVE,
                role = MailboxRoom.ROLE_MEMBER,
                activatedAt = 1L,
                title = "Live",
            ),
        )

        val viewModel = HistoryViewModel(
            mailboxRepository = mailboxRepository,
            proEntitlement = proEntitlement,
            dispatchersProvider = coroutinesRule.testDispatcherProvider,
        )
        advanceUntilIdle()

        viewModel.uiState.test {
            val state = expectMostRecentItem()
            state.filter shouldBe HistoryRoomFilter.Open
            state.rooms.map { it.row.id } shouldBe listOf("live")
        }
    }
}
