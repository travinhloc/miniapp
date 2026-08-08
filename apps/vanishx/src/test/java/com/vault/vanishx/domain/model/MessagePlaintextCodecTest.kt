package com.vault.vanishx.domain.model

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.Test

class MessagePlaintextCodecTest {

    @Test
    fun `encode non-sensitive keeps legacy plain text`() {
        MessagePlaintextCodec.encode("hello", sensitive = false) shouldBe "hello"
    }

    @Test
    fun `encode sensitive wraps json envelope`() {
        val wire = MessagePlaintextCodec.encode("secret \"line\"", sensitive = true)
        wire.startsWith("{\"v\":1,") shouldBe true
        val decoded = MessagePlaintextCodec.decode(wire)
        decoded.sensitive shouldBe true
        decoded.text shouldBe "secret \"line\""
    }

    @Test
    fun `decode legacy plaintext is not sensitive`() {
        val decoded = MessagePlaintextCodec.decode("plain body")
        decoded.sensitive shouldBe false
        decoded.text shouldBe "plain body"
    }

    @Test
    fun `round-trip preserves newlines`() {
        val original = "line1\nline2"
        val decoded = MessagePlaintextCodec.decode(
            MessagePlaintextCodec.encode(original, sensitive = true),
        )
        decoded.text shouldBe original
        decoded.sensitive shouldBe true
        MessagePlaintextCodec.encode(original, sensitive = true) shouldNotBe original
    }
}
