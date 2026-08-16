package com.vault.vanishx.presentation.security

import com.vault.vanishx.domain.model.BlockedPeer
import com.vault.vanishx.domain.repository.BlockRepository
import com.vault.vanishx.domain.repository.ProEntitlementRepository
import com.vault.vanishx.domain.usecase.EnsureIdentityUseCase
import com.vault.vanishx.test.CoroutineTestRule
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
class SecuritySettingsViewModelTest {

    @get:Rule
    val coroutinesRule = CoroutineTestRule()

    private val pinStore = mockk<com.vault.vanishx.data.security.SecurityPinStore>(relaxed = true)
    private val ensureIdentity: EnsureIdentityUseCase = mockk()
    private val pro: ProEntitlementRepository = mockk(relaxed = true)
    private val blocks: BlockRepository = mockk(relaxed = true)

    private fun viewModel(): SecuritySettingsViewModel {
        every { pinStore.hasUnlockPin() } returns true
        every { pinStore.hasPanicPin() } returns false
        every { pinStore.isFlagSecureEnabled() } returns true
        every { pinStore.isAutoWipeEnabled() } returns false
        every { pinStore.isBiometricEnabled() } returns false
        every { pro.isProNow() } returns false
        every { pro.isPro } returns kotlinx.coroutines.flow.MutableStateFlow(false)
        coEvery { ensureIdentity() } returns com.vault.vanishx.domain.model.Identity("vx_a", "pub")
        coEvery { blocks.listBlocked() } returns listOf(BlockedPeer("peerPubABCDEF", 1L))
        return SecuritySettingsViewModel(
            securityPinStore = pinStore,
            ensureIdentity = ensureIdentity,
            proEntitlement = pro,
            blockRepository = blocks,
            dispatchersProvider = coroutinesRule.testDispatcherProvider,
        )
    }

    @Test
    fun `loads blocked peers on start`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()
        vm.uiState.value.blockedPeers.map { it.peerPub } shouldBe listOf("peerPubABCDEF")
    }

    @Test
    fun `SetBiometric persists preference`() = runTest {
        val vm = viewModel()
        vm.onAction(SecuritySettingsAction.SetBiometric(true))
        verify { pinStore.setBiometricEnabled(true) }
        vm.uiState.value.biometricEnabled shouldBe true
    }

    @Test
    fun `UnblockPeer refreshes the list`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()
        coEvery { blocks.listBlocked() } returns emptyList()
        vm.onAction(SecuritySettingsAction.UnblockPeer("peerPubABCDEF"))
        advanceUntilIdle()
        coVerify { blocks.unblock("peerPubABCDEF") }
        vm.uiState.value.blockedPeers shouldBe emptyList()
    }
}
