package com.vault.vanishx.domain.model

import io.kotest.matchers.shouldBe
import org.junit.Test

class MediaLimitsTest {

    @Test
    fun `ZIP is not an allowed file mime`() {
        MediaLimits.kindForMime("application/zip") shouldBe null
        MediaLimits.kindForMimeOrName("application/zip", "a.zip") shouldBe null
        MediaLimits.kindForMimeOrName(null, "notes.zip") shouldBe null
    }

    @Test
    fun `document picker mime list has no wildcard or zip`() {
        MediaLimits.DOCUMENT_PICKER_MIME.contains("*/*") shouldBe false
        MediaLimits.DOCUMENT_PICKER_MIME.any { it.contains("zip") } shouldBe false
        MediaLimits.DOCUMENT_PICKER_MIME.contains("application/pdf") shouldBe true
    }

    @Test
    fun `PDF and text use in-app viewer`() {
        MediaLimits.isInAppDocumentViewer("application/pdf", "a.pdf") shouldBe true
        MediaLimits.isInAppDocumentViewer("text/plain", "a.txt") shouldBe true
        MediaLimits.isInAppDocumentViewer("text/markdown", "a.md") shouldBe true
        MediaLimits.isPdf("application/pdf", null) shouldBe true
    }

    @Test
    fun `Office docs open via system`() {
        MediaLimits.isSystemOpenDocument(
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "a.docx",
        ) shouldBe true
        MediaLimits.isSystemOpenDocument("application/msword", "a.doc") shouldBe true
        MediaLimits.isSystemOpenDocument(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "a.xlsx",
        ) shouldBe true
        MediaLimits.isInAppDocumentViewer(
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "a.docx",
        ) shouldBe false
    }

    @Test
    fun `allowed document mimes map to KIND_FILE`() {
        MediaLimits.kindForMime("application/pdf") shouldBe AttachmentMeta.KIND_FILE
        MediaLimits.kindForMimeOrName(null, "readme.md") shouldBe AttachmentMeta.KIND_FILE
        MediaLimits.FILE_MIME.contains("application/zip") shouldBe false
    }

    @Test
    fun `file size cap remains 10 MB`() {
        MediaLimits.maxBytesForKind(AttachmentMeta.KIND_FILE) shouldBe MediaLimits.FILE_MAX_BYTES
        MediaLimits.FILE_MAX_BYTES shouldBe 10L * 1024 * 1024
    }

    @Test
    fun `voice caps are 60s and 5 MB`() {
        MediaLimits.VOICE_MAX_DURATION_MS shouldBe 60_000L
        MediaLimits.VOICE_MAX_BYTES shouldBe 5L * 1024 * 1024
        MediaLimits.maxBytesForKind(AttachmentMeta.KIND_VOICE) shouldBe MediaLimits.VOICE_MAX_BYTES
    }

    @Test
    fun `audio mp4 maps to KIND_VOICE`() {
        MediaLimits.kindForMime("audio/mp4") shouldBe AttachmentMeta.KIND_VOICE
        MediaLimits.kindForMime("audio/aac") shouldBe AttachmentMeta.KIND_VOICE
        MediaLimits.kindForMimeOrName(null, "note.m4a") shouldBe AttachmentMeta.KIND_VOICE
        MediaLimits.VOICE_MIME.contains("audio/mp4") shouldBe true
    }

    @Test
    fun `video caps are 60s and 25 MB`() {
        MediaLimits.VIDEO_MAX_DURATION_MS shouldBe 60_000L
        MediaLimits.VIDEO_MAX_BYTES shouldBe 25L * 1024 * 1024
        MediaLimits.maxBytesForKind(AttachmentMeta.KIND_VIDEO) shouldBe MediaLimits.VIDEO_MAX_BYTES
    }

    @Test
    fun `video mp4 maps to KIND_VIDEO`() {
        MediaLimits.kindForMime("video/mp4") shouldBe AttachmentMeta.KIND_VIDEO
        MediaLimits.kindForMimeOrName(null, "clip.mp4") shouldBe AttachmentMeta.KIND_VIDEO
        MediaLimits.VIDEO_MIME.contains("video/mp4") shouldBe true
    }

    @Test
    fun `photo multi select max is nine`() {
        MediaLimits.PHOTO_MULTI_SELECT_MAX shouldBe 9
    }

    @Test
    fun `clamp photo multi select empty is no-op`() {
        MediaLimits.clampPhotoMultiSelect(emptyList<String>()) shouldBe emptyList()
    }

    @Test
    fun `clamp photo multi select truncates above max`() {
        val input = (1..12).toList()
        MediaLimits.clampPhotoMultiSelect(input) shouldBe (1..9).toList()
    }

    @Test
    fun `clamp photo multi select keeps at most nine`() {
        val input = (1..9).toList()
        MediaLimits.clampPhotoMultiSelect(input) shouldBe input
    }
}
