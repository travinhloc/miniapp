package com.vault.vanishx.presentation.mailbox.chat

import io.kotest.matchers.shouldBe
import org.junit.Test

class AlbumMediaCollageTest {

    @Test
    fun `grid balances four through nine items without blank cells`() {
        albumGridRowSizes(4) shouldBe listOf(3)
        albumGridRowSizes(5) shouldBe listOf(2, 2)
        albumGridRowSizes(6) shouldBe listOf(3, 2)
        albumGridRowSizes(7) shouldBe listOf(3, 3)
        albumGridRowSizes(8) shouldBe listOf(3, 2, 2)
        albumGridRowSizes(9) shouldBe listOf(3, 3, 2)
    }

    @Test
    fun `row sizes account for every item after hero`() {
        (1..9).forEach { count ->
            albumGridRowSizes(count).sum() shouldBe (count - 1).coerceAtLeast(0)
        }
    }

    @Test
    fun `each non-hero tile keeps its original click index`() {
        albumGridIndices(8) shouldBe listOf(
            listOf(1, 2, 3),
            listOf(4, 5),
            listOf(6, 7),
        )
        albumGridIndices(9).flatten() shouldBe (1..8).toList()
    }
}
