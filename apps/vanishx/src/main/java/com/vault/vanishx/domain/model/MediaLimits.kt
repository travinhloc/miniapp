@file:Suppress("ReturnCount")

package com.vault.vanishx.domain.model

/**
 * Soft caps for Epic 11 (E11-4 / E11-9 / E11-10). HQ tiers → paywall later.
 */
object MediaLimits {
    const val IMAGE_MAX_BYTES = 3L * 1024 * 1024
    const val IMAGE_MAX_EDGE_PX = 1280
    const val IMAGE_JPEG_QUALITY = 82

    const val FILE_MAX_BYTES = 10L * 1024 * 1024
    const val VIDEO_MAX_BYTES = 25L * 1024 * 1024
    const val VIDEO_MAX_DURATION_MS = 60_000L

    val IMAGE_MIME = setOf("image/jpeg", "image/jpg", "image/png", "image/webp")
    val FILE_MIME = setOf(
        "application/pdf",
        "text/plain",
        "text/markdown",
        "text/x-markdown",
        "application/zip",
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
        "application/msword",
        "application/vnd.ms-excel",
    )
    val VIDEO_MIME = setOf("video/mp4", "video/3gpp", "video/webm")

    fun kindForMime(mime: String): String? {
        val normalized = mime.lowercase().substringBefore(';').trim()
        return when {
            normalized in IMAGE_MIME ||
                (normalized.startsWith("image/") &&
                    normalized.removePrefix("image/") in setOf("jpeg", "jpg", "png", "webp")) ->
                AttachmentMeta.KIND_IMAGE
            normalized in VIDEO_MIME || normalized.startsWith("video/") ->
                AttachmentMeta.KIND_VIDEO
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
            "pdf", "txt", "md", "markdown", "zip", "doc", "docx", "xls", "xlsx" ->
                AttachmentMeta.KIND_FILE
            else -> null
        }
    }

    fun maxBytesForKind(kind: String): Long = when (kind) {
        AttachmentMeta.KIND_IMAGE -> IMAGE_MAX_BYTES
        AttachmentMeta.KIND_VIDEO -> VIDEO_MAX_BYTES
        else -> FILE_MAX_BYTES
    }
}
