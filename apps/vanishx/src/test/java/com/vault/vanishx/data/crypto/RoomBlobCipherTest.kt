package com.vault.vanishx.data.crypto

import android.util.Base64
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RoomBlobCipherTest {

    private val cipher = RoomBlobCipher()

    @Test
    fun `encrypt decrypt round-trip`() {
        val key = Base64.encodeToString(ByteArray(32) { it.toByte() }, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
        val plain = "hello-media".toByteArray()
        val wire = cipher.encrypt("room1", "att1", key, plain)
        wire shouldNotBe plain
        cipher.decrypt("room1", "att1", key, wire) shouldBe plain
    }
}
