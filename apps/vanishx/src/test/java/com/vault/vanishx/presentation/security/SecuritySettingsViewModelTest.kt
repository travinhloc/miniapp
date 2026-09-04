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

    private fun viewModel(
        hasUnlockPinInitially: Boolean = true,
        hasPanicPinInitially: Boolean = false,
    ): SecuritySettingsViewModel {
        var hasUnlockPin = hasUnlockPinInitially
        var hasPanicPin = hasPanicPinInitially
        every { pinStore.hasUnlockPin() } answers { hasUnlockPin }
        every { pinStore.clearUnlockPin() } answers { hasUnlockPin = false }
        every { pinStore.hasPanicPin() } answers { hasPanicPin }
        every { pinStore.clearPanicPin() } answers { hasPanicPin = false }
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
    fun `SetBiometric off disables immediately`() = runTest {
        every { pinStore.isBiometricEnabled() } returns true
        val vm = viewModel()
        vm.onAction(SecuritySettingsAction.SetBiometric(false))
        verify { pinStore.setBiometricEnabled(false) }
        vm.uiState.value.biometricEnabled shouldBe false
        vm.uiState.value.promptBiometricEnable shouldBe false
    }

    @Test
    fun `SetBiometric on requests biometric confirmation first`() = runTest {
        val vm = viewModel()
        vm.onAction(SecuritySettingsAction.SetBiometric(true))

        vm.uiState.value.promptBiometricEnable shouldBe true
        vm.uiState.value.biometricEnabled shouldBe false
        verify(exactly = 0) { pinStore.setBiometricEnabled(true) }

        vm.onAction(SecuritySettingsAction.BiometricEnableSuccess)
        verify(exactly = 1) { pinStore.setBiometricEnabled(true) }
        vm.uiState.value.biometricEnabled shouldBe true
        vm.uiState.value.promptBiometricEnable shouldBe false
    }

    @Test
    fun `BiometricEnableFailed keeps biometric disabled`() = runTest {
        val vm = viewModel()
        vm.onAction(SecuritySettingsAction.SetBiometric(true))
        vm.onAction(SecuritySettingsAction.BiometricEnableFailed("denied"))

        verify(exactly = 0) { pinStore.setBiometricEnabled(true) }
        vm.uiState.value.biometricEnabled shouldBe false
        vm.uiState.value.promptBiometricEnable shouldBe false
        vm.uiState.value.errorMessage shouldBe "denied"
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

    @Test
    fun `SaveUnlockPin mismatch shows pin form error`() = runTest {
        every { pinStore.setUnlockPin(any()) } returns Unit
        val vm = viewModel(hasUnlockPinInitially = false)
        vm.onAction(SecuritySettingsAction.UnlockPinChanged("1234"))
        vm.onAction(SecuritySettingsAction.UnlockPinConfirmChanged("9999"))
        vm.onAction(SecuritySettingsAction.SaveUnlockPin)

        vm.uiState.value.pinFormErrorRes shouldBe com.vault.vanishx.R.string.security_pin_mismatch_error
        verify(exactly = 0) { pinStore.setUnlockPin(any()) }
    }

    @Test
    fun `SaveUnlockPin incomplete length shows pin form error`() = runTest {
        val vm = viewModel(hasUnlockPinInitially = false)
        vm.onAction(SecuritySettingsAction.UnlockPinChanged("12"))
        vm.onAction(SecuritySettingsAction.UnlockPinConfirmChanged("12"))
        vm.onAction(SecuritySettingsAction.SaveUnlockPin)

        vm.uiState.value.pinFormErrorRes shouldBe com.vault.vanishx.R.string.security_pin_length_error
        verify(exactly = 0) { pinStore.setUnlockPin(any()) }
    }

    @Test
    fun `SaveUnlockPin change requires current PIN`() = runTest {
        every { pinStore.matchesUnlockPin("1111") } returns false
        every { pinStore.matchesUnlockPin("1234") } returns true
        every { pinStore.setUnlockPin("5678") } returns Unit
        val vm = viewModel()
        vm.onAction(SecuritySettingsAction.UnlockPinChanged("5678"))
        vm.onAction(SecuritySettingsAction.UnlockPinConfirmChanged("5678"))
        vm.onAction(SecuritySettingsAction.SaveUnlockPin)
        verify(exactly = 0) { pinStore.setUnlockPin(any()) }
        vm.uiState.value.pinFormErrorRes shouldBe com.vault.vanishx.R.string.security_pin_length_error

        vm.onAction(SecuritySettingsAction.UnlockCurrentPinChanged("1111"))
        vm.onAction(SecuritySettingsAction.SaveUnlockPin)
        verify(exactly = 0) { pinStore.setUnlockPin(any()) }
        vm.uiState.value.pinFormErrorRes shouldBe com.vault.vanishx.R.string.security_clear_pin_wrong

        vm.onAction(SecuritySettingsAction.UnlockCurrentPinChanged("1234"))
        vm.onAction(SecuritySettingsAction.SaveUnlockPin)
        verify(exactly = 1) { pinStore.setUnlockPin("5678") }
    }

    @Test
    fun `ClearUnlockPin requires current PIN before removing`() = runTest {
        every { pinStore.matchesUnlockPin("1234") } returns true
        every { pinStore.matchesUnlockPin("0000") } returns false
        val vm = viewModel()
        vm.onAction(SecuritySettingsAction.ClearUnlockPin)
        vm.uiState.value.pendingClearPin shouldBe PendingClearPin.Unlock
        verify(exactly = 0) { pinStore.clearUnlockPin() }

        vm.onAction(SecuritySettingsAction.ClearPinDraftChanged("0000"))
        vm.onAction(SecuritySettingsAction.ConfirmClearPin)
        verify(exactly = 0) { pinStore.clearUnlockPin() }
        vm.uiState.value.clearPinErrorRes shouldBe com.vault.vanishx.R.string.security_clear_pin_wrong
        vm.uiState.value.pendingClearPin shouldBe PendingClearPin.Unlock

        vm.onAction(SecuritySettingsAction.ClearPinDraftChanged("1234"))
        vm.onAction(SecuritySettingsAction.ConfirmClearPin)
        verify(exactly = 1) { pinStore.clearUnlockPin() }
        vm.uiState.value.pendingClearPin shouldBe null
        vm.uiState.value.hasUnlockPin shouldBe false
    }

    @Test
    fun `DismissClearPin keeps the unlock PIN`() = runTest {
        val vm = viewModel()
        vm.onAction(SecuritySettingsAction.ClearUnlockPin)
        vm.onAction(SecuritySettingsAction.ClearPinDraftChanged("1234"))
        vm.onAction(SecuritySettingsAction.DismissClearPin)

        vm.uiState.value.pendingClearPin shouldBe null
        vm.uiState.value.clearPinDraft shouldBe ""
        verify(exactly = 0) { pinStore.clearUnlockPin() }
    }

    @Test
    fun `SavePanicPin change requires current Panic PIN`() = runTest {
        every { pinStore.matchesPanicPin("1111") } returns false
        every { pinStore.matchesPanicPin("4321") } returns true
        every { pinStore.setPanicPin("9876") } returns Unit
        val vm = viewModel(hasPanicPinInitially = true)

        vm.onAction(SecuritySettingsAction.PanicPinChanged("9876"))
        vm.onAction(SecuritySettingsAction.PanicPinConfirmChanged("9876"))
        vm.onAction(SecuritySettingsAction.SavePanicPin)
        verify(exactly = 0) { pinStore.setPanicPin(any()) }
        vm.uiState.value.pinFormErrorRes shouldBe com.vault.vanishx.R.string.security_pin_length_error

        vm.onAction(SecuritySettingsAction.PanicCurrentPinChanged("1111"))
        vm.onAction(SecuritySettingsAction.SavePanicPin)
        verify(exactly = 0) { pinStore.setPanicPin(any()) }
        vm.uiState.value.pinFormErrorRes shouldBe com.vault.vanishx.R.string.security_clear_pin_wrong

        vm.onAction(SecuritySettingsAction.PanicCurrentPinChanged("4321"))
        vm.onAction(SecuritySettingsAction.SavePanicPin)
        verify(exactly = 1) { pinStore.setPanicPin("9876") }
    }

    @Test
    fun `ClearPanicPin requires current Panic PIN before removing`() = runTest {
        every { pinStore.matchesPanicPin("4321") } returns true
        every { pinStore.matchesPanicPin("0000") } returns false
        val vm = viewModel(hasPanicPinInitially = true)

        vm.onAction(SecuritySettingsAction.ClearPanicPin)
        vm.uiState.value.pendingClearPin shouldBe PendingClearPin.Panic
        verify(exactly = 0) { pinStore.clearPanicPin() }

        vm.onAction(SecuritySettingsAction.ClearPinDraftChanged("0000"))
        vm.onAction(SecuritySettingsAction.ConfirmClearPin)
        verify(exactly = 0) { pinStore.clearPanicPin() }
        vm.uiState.value.clearPinErrorRes shouldBe com.vault.vanishx.R.string.security_clear_pin_wrong

        vm.onAction(SecuritySettingsAction.ClearPinDraftChanged("4321"))
        vm.onAction(SecuritySettingsAction.ConfirmClearPin)
        verify(exactly = 1) { pinStore.clearPanicPin() }
        vm.uiState.value.pendingClearPin shouldBe null
        vm.uiState.value.hasPanicPin shouldBe false
    }

    @Test
    fun `first time SavePanicPin does not require current PIN`() = runTest {
        every { pinStore.setPanicPin("9876") } returns Unit
        val vm = viewModel(hasPanicPinInitially = false)

        vm.onAction(SecuritySettingsAction.PanicPinChanged("9876"))
        vm.onAction(SecuritySettingsAction.PanicPinConfirmChanged("9876"))
        vm.onAction(SecuritySettingsAction.SavePanicPin)

        verify(exactly = 1) { pinStore.setPanicPin("9876") }
        verify(exactly = 0) { pinStore.matchesPanicPin(any()) }
    }

    @Test
    fun `SavePanicPin same as unlock shows non revealing error`() = runTest {
        every { pinStore.setPanicPin("1234") } throws IllegalStateException(
            com.vault.vanishx.data.security.SecurityPinStore.PANIC_SAME_AS_UNLOCK,
        )
        val vm = viewModel(hasPanicPinInitially = false)

        vm.onAction(SecuritySettingsAction.PanicPinChanged("1234"))
        vm.onAction(SecuritySettingsAction.PanicPinConfirmChanged("1234"))
        vm.onAction(SecuritySettingsAction.SavePanicPin)

        vm.uiState.value.pinFormErrorRes shouldBe com.vault.vanishx.R.string.security_panic_pin_rejected
        vm.uiState.value.errorMessage shouldBe null
    }
}
