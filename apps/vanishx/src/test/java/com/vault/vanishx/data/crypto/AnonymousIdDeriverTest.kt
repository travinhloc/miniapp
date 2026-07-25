package com.vault.vanishx.data.crypto

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldStartWith
import org.junit.Test

class AnonymousIdDeriverTest {

    @Test
    fun `same public key yields same anonymous id`() {
        val key = ByteArray(32) { it.toByte() }
        val first = AnonymousIdDeriver.fromPublicKeyBytes(key)
        val second = AnonymousIdDeriver.fromPublicKeyBytes(key)

        first shouldBe second
        first.shouldStartWith("vx_")
    }

    @Test
    fun `different public keys yield different ids`() {
        val a = ByteArray(32) { 1 }
        val b = ByteArray(32) { 2 }

        AnonymousIdDeriver.fromPublicKeyBytes(a) shouldNotBe AnonymousIdDeriver.fromPublicKeyBytes(b)
    }
}
