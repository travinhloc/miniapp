package com.vault.vanishx.domain.model

/**
 * Media attachment metadata carried in envelope v:2 (no caption — E11-5).
 * Blob bytes live on Firebase Storage; this stays inside RTDB ciphertext.
 */
data class AttachmentMeta(
    val kind: String,
    val mime: String,
    val bytes: Long,
    val attId: String,
    val width: Int? = null,
    val height: Int? = null,
    val fileName: String? = null,
) {
    companion object {
        const val KIND_IMAGE = "image"
        const val KIND_FILE = "file"
        const val KIND_VIDEO = "video"
        const val KIND_VOICE = "voice"
        /** Local UI aggregation of multiple visual media (not a wire envelope kind). */
        const val KIND_ALBUM = "album"
    }
}
