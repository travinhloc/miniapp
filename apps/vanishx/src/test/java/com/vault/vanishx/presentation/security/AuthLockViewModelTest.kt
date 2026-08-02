package com.vault.vanishx.presentation.security

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.vault.vanishx.data.security.AppLockSession
import com.vault.vanishx.data.security.SecurityPinStore
import com.vault.vanishx.domain.usecase.PanicWipeUseCase
import com.vault.vanishx.test.CoroutineTestRule
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class AuthSetupViewModelTest {

    @get:Rule
    val coroutinesRule = CoroutineTestRule()

    private lateinit var pinStore: SecurityPinStore
    private val appLockSession: AppLockSession = mockk(relaxed = true)
    private lateinit var viewModel: AuthSetupViewModel

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = context.getSharedPreferences("auth_setup_vm_pins", Context.MODE_PRIVATE)
        prefs.edit().clear().commit()
        pinStore = SecurityPinStore(prefs)
        viewModel = AuthSetupViewModel(pinStore, appLockSession)
    }

    @Test
    fun `matching pins advance to biometric step`() = runTest {
        enterPin("1234")
        enterPin("1234")
        viewModel.uiState.value.step shouldBe AuthSetupStep.Biometric
        pinStore.hasUnlockPin() shouldBe true
    }

    @Test
    fun `mismatch resets to enter with error`() = runTest {
        enterPin("1234")
        enterPin("9999")
        val state = viewModel.uiState.value
        state.step shouldBe AuthSetupStep.Enter
        state.showMismatch shouldBe true
        pinStore.hasUnlockPin() shouldBe false
    }

    @Test
    fun `finish unlocks session`() = runTest {
        enterPin("1234")
        enterPin("1234")
        viewModel.onAction(AuthSetupAction.EnableBiometric(false))
        viewModel.uiState.value.completed shouldBe true
        verify { appLockSession.unlock() }
        pinStore.isBiometricEnabled() shouldBe false
    }

    private fun enterPin(pin: String) {
        pin.forEach { viewModel.onAction(AuthSetupAction.Digit(it)) }
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class LockViewModelTest {

    @get:Rule
    val coroutinesRule = CoroutineTestRule()

    private lateinit var pinStore: SecurityPinStore
    private val appLockSession: AppLockSession = mockk(relaxed = true)
    private val panicWipe: PanicWipeUseCase = mockk()
    private lateinit var viewModel: LockViewModel

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = context.getSharedPreferences("lock_vm_pins", Context.MODE_PRIVATE)
        prefs.edit().clear().commit()
        pinStore = SecurityPinStore(prefs)
        pinStore.setUnlockPin("1234")
        coEvery { panicWipe() } answers {
            pinStore.clearAll()
        }
        viewModel = LockViewModel(
            securityPinStore = pinStore,
            appLockSession = appLockSession,
            panicWipe = panicWipe,
            dispatchersProvider = coroutinesRule.testDispatcherProvider,
        )
    }

    @Test
    fun `correct pin unlocks after OK`() = runTest {
        enterPinWithoutSubmit("1234")
        viewModel.uiState.value.unlocked shouldBe false
        viewModel.onAction(LockAction.Submit)
        viewModel.uiState.value.unlocked shouldBe true
        verify { appLockSession.unlock() }
    }

    @Test
    fun `wrong pin shows attempts remaining`() = runTest {
        enterPinWithoutSubmit("0000")
        viewModel.onAction(LockAction.Submit)
        val state = viewModel.uiState.value
        state.showWrongPin shouldBe true
        state.attemptsLeft shouldBe 4
        state.unlocked shouldBe false
    }

    @Test
    fun `five wrong pins start cooldown without wipe by default`() = runTest {
        repeat(5) {
            enterPinWithoutSubmit("0000")
            viewModel.onAction(LockAction.Submit)
        }
        val state = viewModel.uiState.value
        state.cooldownRemainingMs shouldBe SecurityPinStore.COOLDOWN_DURATIONS_MS[0]
        state.showBurnOverlay shouldBe false
        state.wiped shouldBe false
        coVerify(exactly = 0) { panicWipe() }
        pinStore.hasUnlockPin() shouldBe true
    }

    @Test
    fun `five wrong pins wipe when auto wipe enabled`() = runTest {
        pinStore.setAutoWipeEnabled(true)
        viewModel = LockViewModel(
            securityPinStore = pinStore,
            appLockSession = appLockSession,
            panicWipe = panicWipe,
            dispatchersProvider = coroutinesRule.testDispatcherProvider,
        )
        repeat(5) {
            enterPinWithoutSubmit("0000")
            viewModel.onAction(LockAction.Submit)
        }
        advanceUntilIdle()
        viewModel.uiState.test {
            val state = expectMostRecentItem()
            state.showBurnOverlay shouldBe true
            state.wiped shouldBe true
        }
        coVerify(exactly = 1) { panicWipe() }
        pinStore.hasUnlockPin() shouldBe false
    }

    @Test
    fun `panic pin wipes silently`() = runTest {
        pinStore.setPanicPin("9999")
        enterPinWithoutSubmit("9999")
        viewModel.onAction(LockAction.Submit)
        advanceUntilIdle()
        val state = viewModel.uiState.value
        state.wiped shouldBe true
        state.showBurnOverlay shouldBe false
        coVerify(exactly = 1) { panicWipe() }
    }

    private fun enterPinWithoutSubmit(pin: String) {
        pin.forEach { viewModel.onAction(LockAction.Digit(it)) }
    }
}
