package com.vault.vanishx.domain.usecase

import com.vault.vanishx.data.crypto.IdentityKeyStore
import com.vault.vanishx.data.invite.PendingInviteStore
import com.vault.vanishx.data.security.AppLockSession
import com.vault.vanishx.data.security.SecurityPinStore
import com.vault.vanishx.domain.model.Identity
import com.vault.vanishx.domain.repository.LocalDatabaseWiper
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Test

class PanicWipeUseCaseTest {

    @Test
    fun `wipe clears db identity pending pins and recreates identity`() = runTest {
        val wiper: LocalDatabaseWiper = mockk(relaxed = true)
        val identityKeyStore: IdentityKeyStore = mockk(relaxed = true)
        val pending: PendingInviteStore = mockk(relaxed = true)
        val pins: SecurityPinStore = mockk(relaxed = true)
        val session = AppLockSession()
        session.lock()
        val ensureIdentity: EnsureIdentityUseCase = mockk()
        coEvery { ensureIdentity() } returns Identity("vx_new", "pub")

        PanicWipeUseCase(
            localDatabaseWiper = wiper,
            identityKeyStore = identityKeyStore,
            pendingInviteStore = pending,
            securityPinStore = pins,
            appLockSession = session,
            ensureIdentity = ensureIdentity,
        ).invoke()

        coVerify { wiper.wipe() }
        verify { identityKeyStore.clear() }
        verify { pending.clear() }
        verify { pins.clearAll() }
        coVerify { ensureIdentity() }
        session.isUnlocked shouldBe true
    }
}
