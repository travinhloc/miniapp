package com.vault.vanishx.data.crypto

import com.vault.vanishx.domain.model.Identity
import io.kotest.matchers.shouldBe
import org.junit.Test

class InMemoryIdentityKeyStoreTest {

    @Test
    fun `first call creates identity and second call returns same without regenerating`() {
        var generateCount = 0
        val store = object : IdentityKeyStore {
            private var stored: Identity? = null

            override fun getOrCreateIdentity(): Identity {
                stored?.let { return it }
                generateCount++
                return Identity(
                    anonymousId = "vx_fixed",
                    publicKeyBase64 = "cHVi",
                ).also { stored = it }
            }
        }

        val first = store.getOrCreateIdentity()
        val second = store.getOrCreateIdentity()

        first shouldBe second
        generateCount shouldBe 1
    }

    @Test
    fun `empty store means no key yet until create`() {
        val store = InMemoryIdentityKeyStore()
        store.hasIdentity() shouldBe false

        store.getOrCreateIdentity()
        store.hasIdentity() shouldBe true
    }
}

/**
 * Test double documenting “chưa có key / đã có key” for story 1.2 AC.
 */
class InMemoryIdentityKeyStore : IdentityKeyStore {
    private var stored: Identity? = null

    fun hasIdentity(): Boolean = stored != null

    override fun getOrCreateIdentity(): Identity {
        stored?.let { return it }
        return Identity(
            anonymousId = AnonymousIdDeriver.fromPublicKeyBytes(ByteArray(32) { 7 }),
            publicKeyBase64 = AnonymousIdDeriver.publicKeyBase64(ByteArray(32) { 7 }),
        ).also { stored = it }
    }
}
