package com.vault.vanishx.data.security

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import io.kotest.matchers.shouldBe
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class SecurityPinStoreTest {

    private lateinit var store: SecurityPinStore

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = context.getSharedPreferences("test_security_pins", Context.MODE_PRIVATE)
        prefs.edit().clear().commit()
        store = SecurityPinStore(prefs)
    }

    @Test
    fun `unlock and panic pins verify distinctly`() {
        store.setUnlockPin("1234")
        store.setPanicPin("9999")

        store.verify("1234") shouldBe PinVerifyResult.UNLOCK
        store.verify("9999") shouldBe PinVerifyResult.PANIC
        store.verify("0000") shouldBe PinVerifyResult.INVALID
    }

    @Test
    fun `panic pin cannot match unlock pin`() {
        store.setUnlockPin("1234")
        runCatching { store.setPanicPin("1234") }.isFailure shouldBe true
    }

    @Test
    fun `clearAll removes pins`() {
        store.setUnlockPin("1234")
        store.setPanicPin("5678")
        store.clearAll()
        store.hasUnlockPin() shouldBe false
        store.hasPanicPin() shouldBe false
    }
}
