package com.vault.vanishx.data.crypto

import android.util.Base64
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldStartWith
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.security.SecureRandom

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class RoomMessageCipherTest {

    private val cipher = RoomMessageCipher()

    @Test
    fun `encrypt decrypt round trip`() {
        val roomKey = randomRoomKey()
        val wire = cipher.encrypt("roomA", roomKey, "hello mũi tên")
        wire shouldStartWith RoomMessageCipher.PREFIX
        cipher.decrypt("roomA", roomKey, wire) shouldBe "hello mũi tên"
    }

    @Test
    fun `different room id aad fails decrypt`() {
        val roomKey = randomRoomKey()
        val wire = cipher.encrypt("roomA", roomKey, "secret")
        runCatching { cipher.decrypt("roomB", roomKey, wire) }.isFailure shouldBe true
    }

    @Test
    fun `ciphertext is not plaintext`() {
        val roomKey = randomRoomKey()
        val wire = cipher.encrypt("roomA", roomKey, "visible-secret")
        wire shouldNotBe "visible-secret"
        wire.contains("visible-secret") shouldBe false
    }

    private fun randomRoomKey(): String {
        val bytes = ByteArray(32).also { SecureRandom().nextBytes(it) }
        return Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
    }
}
