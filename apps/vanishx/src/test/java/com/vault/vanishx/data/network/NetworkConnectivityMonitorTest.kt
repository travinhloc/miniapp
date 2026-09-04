package com.vault.vanishx.data.network

import android.net.NetworkCapabilities
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.junit.Test

class NetworkConnectivityMonitorTest {

    @Test
    fun `network must have Internet and validation capabilities`() {
        val capabilities = mockk<NetworkCapabilities>()
        every { capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) } returns true
        every { capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) } returns true

        capabilities.hasValidatedInternet() shouldBe true
    }

    @Test
    fun `unvalidated or missing network is offline`() {
        val captivePortal = mockk<NetworkCapabilities>()
        every { captivePortal.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) } returns true
        every { captivePortal.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) } returns false

        captivePortal.hasValidatedInternet() shouldBe false
        null.hasValidatedInternet() shouldBe false
    }
}
