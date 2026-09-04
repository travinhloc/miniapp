package com.vault.vanishx.data.local

import io.kotest.matchers.shouldBe
import org.junit.Test

class ComposerDraftStoreTest {

    @Test
    fun `meta key is scoped per room`() {
        ComposerDraftStore.metaKey("room-a") shouldBe "composer_draft:room-a"
    }
}
