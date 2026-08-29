@file:Suppress("ReturnCount", "ComplexCondition", "ComplexMethod")

package com.vault.vanishx.domain.model

/**
 * Soft caps for Epic 11 / 16 (E11-4 / E11-9 / E11-10 · E16-1…7). HQ tiers → paywall later.
 */
object MediaLimits {
    const val IMAGE_MAX_BYTES = 3L * 1024 * 1024
    const val IMAGE_MAX_EDGE_PX = 1280
    const val IMAGE_JPEG_QUALITY = 82

    const val FILE_MAX_BYTES = 10L * 1024 * 1024
    const val VIDEO_MAX_BYTES = 25L * 1024 * 1024
    const val VIDEO_MAX_DURATION_MS = 60_000L

    /** E16-5 voice note. */
    const val VOICE_MAX_DURATION_MS = 60_000L
    const val VOICE_MAX_BYTES = 5L * 1024 * 1024

    /** E16-10 gallery multi-select (photos only). */
    const val PHOTO_MULTI_SELECT_MAX = 9

    /** Clamp gallery multi-select to [PHOTO_MULTI_SELECT_MAX]; empty stays empty. */
    fun <T> clampPhotoMultiSelect(items: List<T>): List<T> =
        items.take(PHOTO_MULTI_SELECT_MAX)

    /** Max chars loaded into the in-app text document viewer. */
    const val TEXT_VIEWER_MAX_CHARS = 200_000

    val IMAGE_MIME = setOf("image/jpeg", "image/jpg", "image/png", "image/webp")

    val VOICE_MIME = setOf("audio/mp4", "audio/aac", "audio/m4a")

    /** E16-1: no ZIP, no wildcard picker types. */
    val FILE_MIME = setOf(
        "application/pdf",
        "text/plain",
        "text/markdown",
        "text/x-markdown",
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
        "application/msword",
        "application/vnd.ms-excel",
    )

    val VIDEO_MIME = setOf("video/mp4", "video/3gpp", "video/webm")

    /** MIME list for [androidx.activity.result.contract.ActivityResultContracts.OpenDocument]. */
    val DOCUMENT_PICKER_MIME: Array<String> = arrayOf(
        "application/pdf",
        "text/plain",
        "text/markdown",
        "text/x-markdown",
        "application/msword",
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        "application/vnd.ms-excel",
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
    )

    fun kindForMime(mime: String): String? {
        val normalized = mime.lowercase().substringBefore(';').trim()
        return when {
            normalized in IMAGE_MIME ||
                (normalized.startsWith("image/") &&
                    normalized.removePrefix("image/") in setOf("jpeg", "jpg", "png", "webp")) ->
                AttachmentMeta.KIND_IMAGE
            normalized in VIDEO_MIME || normalized.startsWith("video/") ->
                AttachmentMeta.KIND_VIDEO
            normalized in VOICE_MIME || normalized.startsWith("audio/") ->
                AttachmentMeta.KIND_VOICE
            normalized in FILE_MIME -> AttachmentMeta.KIND_FILE
            else -> null
        }
    }

    /** Prefer content MIME; fall back to filename extension (OEM pickers often return null). */
    fun kindForMimeOrName(mime: String?, fileName: String?): String? {
        kindForMime(mime.orEmpty())?.let { return it }
        val ext = fileName?.substringAfterLast('.', "")?.lowercase()?.takeIf { it.isNotBlank() }
            ?: return null
        return when (ext) {
            "jpg", "jpeg", "png", "webp" -> AttachmentMeta.KIND_IMAGE
            "mp4", "m4v", "webm", "3gp", "3gpp" -> AttachmentMeta.KIND_VIDEO
            "m4a", "aac" -> AttachmentMeta.KIND_VOICE
            "pdf", "txt", "md", "markdown", "doc", "docx", "xls", "xlsx" ->
                AttachmentMeta.KIND_FILE
            else -> null
        }
    }

    fun maxBytesForKind(kind: String): Long = when (kind) {
        AttachmentMeta.KIND_IMAGE -> IMAGE_MAX_BYTES
        AttachmentMeta.KIND_VIDEO -> VIDEO_MAX_BYTES
        AttachmentMeta.KIND_VOICE -> VOICE_MAX_BYTES
        else -> FILE_MAX_BYTES
    }

    fun isPdf(mime: String?, fileName: String?): Boolean {
        val m = mime.orEmpty().lowercase()
        if (m.contains("pdf")) return true
        val ext = fileName?.substringAfterLast('.', "")?.lowercase()
        return ext == "pdf"
    }

    fun isPlainTextDocument(mime: String?, fileName: String?): Boolean {
        val m = mime.orEmpty().lowercase().substringBefore(';').trim()
        val textMime = m == "text/plain" || m == "text/markdown" || m == "text/x-markdown"
        if (textMime) return true
        val ext = fileName?.substringAfterLast('.', "")?.lowercase()
        return ext in setOf("txt", "md", "markdown")
    }

    /** Office docs open via system Intent (E16-3). */
    fun isSystemOpenDocument(mime: String?, fileName: String?): Boolean {
        if (kindForMimeOrName(mime, fileName) != AttachmentMeta.KIND_FILE) return false
        return !isPdf(mime, fileName) && !isPlainTextDocument(mime, fileName)
    }

    fun isInAppDocumentViewer(mime: String?, fileName: String?): Boolean =
        isPdf(mime, fileName) || isPlainTextDocument(mime, fileName)
}
