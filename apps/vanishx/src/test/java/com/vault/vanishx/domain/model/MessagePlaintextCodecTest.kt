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

    @Test
    fun `encode reply without sensitive wraps envelope with r`() {
        val wire = MessagePlaintextCodec.encode("hi", sensitive = false, replyToId = "msg-42")
        wire.startsWith("{\"v\":1,") shouldBe true
        val decoded = MessagePlaintextCodec.decode(wire)
        decoded.sensitive shouldBe false
        decoded.text shouldBe "hi"
        decoded.replyToId shouldBe "msg-42"
    }

    @Test
    fun `encode sensitive with reply round-trips both flags`() {
        val wire = MessagePlaintextCodec.encode(
            text = "secret",
            sensitive = true,
            replyToId = "parent-1",
        )
        val decoded = MessagePlaintextCodec.decode(wire)
        decoded.sensitive shouldBe true
        decoded.text shouldBe "secret"
        decoded.replyToId shouldBe "parent-1"
    }

    @Test
    fun `blank replyToId is omitted from envelope`() {
        MessagePlaintextCodec.encode("plain", sensitive = false, replyToId = "  ") shouldBe "plain"
        val decoded = MessagePlaintextCodec.decode(
            MessagePlaintextCodec.encode("x", sensitive = true, replyToId = ""),
        )
        decoded.replyToId shouldBe null
        decoded.sensitive shouldBe true
    }

    @Test
    fun `encode attachment v2 round-trips meta`() {
        val meta = AttachmentMeta(
            kind = AttachmentMeta.KIND_IMAGE,
            mime = "image/jpeg",
            bytes = 4200,
            attId = "a_abc",
            width = 1280,
            height = 720,
            fileName = "shot.jpg",
            albumId = "album_batch_1",
        )
        val wire = MessagePlaintextCodec.encodeAttachment(meta)
        wire.startsWith("{\"v\":2,") shouldBe true
        val decoded = MessagePlaintextCodec.decode(wire)
        decoded.attachment shouldBe meta
        decoded.text shouldBe ""
        decoded.sensitive shouldBe false
        decoded.replyToId shouldBe null
    }
}
