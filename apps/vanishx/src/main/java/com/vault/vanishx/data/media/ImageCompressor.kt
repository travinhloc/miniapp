package com.vault.vanishx.data.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.vault.vanishx.domain.model.MediaLimits
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import javax.inject.Singleton

data class CompressedImage(
    val bytes: ByteArray,
    val mime: String,
    val width: Int,
    val height: Int,
)

@Singleton
class ImageCompressor @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun compress(uri: Uri): CompressedImage {
        val original = context.contentResolver.openInputStream(uri)?.use { input ->
            BitmapFactory.decodeStream(input)
        } ?: error("Unable to decode image")
        val scaled = scaleDown(original, MediaLimits.IMAGE_MAX_EDGE_PX)
        if (scaled !== original) original.recycle()
        val width = scaled.width
        val height = scaled.height
        val out = ByteArrayOutputStream()
        var quality = MediaLimits.IMAGE_JPEG_QUALITY
        scaled.compress(Bitmap.CompressFormat.JPEG, quality, out)
        while (out.size() > MediaLimits.IMAGE_MAX_BYTES && quality > MIN_JPEG_QUALITY) {
            out.reset()
            quality -= JPEG_QUALITY_STEP
            scaled.compress(Bitmap.CompressFormat.JPEG, quality, out)
        }
        scaled.recycle()
        val bytes = out.toByteArray()
        require(bytes.size <= MediaLimits.IMAGE_MAX_BYTES) {
            "Image still exceeds ${MediaLimits.IMAGE_MAX_BYTES} bytes after compress"
        }
        return CompressedImage(
            bytes = bytes,
            mime = "image/jpeg",
            width = width,
            height = height,
        )
    }

    private fun scaleDown(source: Bitmap, maxEdge: Int): Bitmap {
        val w = source.width
        val h = source.height
        val longest = maxOf(w, h)
        if (longest <= maxEdge) return source
        val scale = maxEdge.toFloat() / longest.toFloat()
        val nw = (w * scale).toInt().coerceAtLeast(1)
        val nh = (h * scale).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(source, nw, nh, true)
    }

    private companion object {
        const val MIN_JPEG_QUALITY = 40
        const val JPEG_QUALITY_STEP = 8
    }
}
