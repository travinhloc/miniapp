package com.vault.vanishx.data.security

import app.cash.turbine.test
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import org.junit.Test

class AppLockSessionTest {

    @Test
    fun `lock and unlock emit on flow`() = runTest {
        val session = AppLockSession()
        session.isUnlockedFlow.test {
            awaitItem() shouldBe false
            session.unlock()
            awaitItem() shouldBe true
            session.lock()
            awaitItem() shouldBe false
            cancelAndIgnoreRemainingEvents()
        }
    }
}
