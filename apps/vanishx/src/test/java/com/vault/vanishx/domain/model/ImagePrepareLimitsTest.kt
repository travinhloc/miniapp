package com.vault.vanishx.domain.model

import io.kotest.matchers.longs.shouldBeLessThanOrEqual
import io.kotest.matchers.shouldBe
import org.junit.Test

class ImagePrepareLimitsTest {

    @Test
    fun `image caps match E16-4 LQ`() {
        MediaLimits.IMAGE_MAX_EDGE_PX shouldBe 1280
        MediaLimits.IMAGE_MAX_BYTES shouldBeLessThanOrEqual 3L * 1024 * 1024
        MediaLimits.IMAGE_JPEG_QUALITY shouldBe 82
    }

    @Test
    fun `photo multi select max is nine for 16_7`() {
        MediaLimits.PHOTO_MULTI_SELECT_MAX shouldBe 9
    }
}
