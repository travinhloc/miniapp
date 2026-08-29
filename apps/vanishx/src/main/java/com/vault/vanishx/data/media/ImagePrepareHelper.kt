@file:Suppress("ComplexMethod", "NestedBlockDepth", "MagicNumber")

package com.vault.vanishx.data.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.core.content.FileProvider
import com.vault.vanishx.domain.model.MediaLimits
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Pre-send still-image edits (E16-4): rotate · simple center square crop · JPEG write.
 * Final size gate remains [ImageCompressor] on send.
 */
@Singleton
class ImagePrepareHelper @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun createCaptureFile(): File {
        val dir = File(context.cacheDir, "capture").apply { mkdirs() }
        return File(dir, "still_${System.currentTimeMillis()}.jpg")
    }

    fun uriForFile(file: File): Uri =
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

    /**
     * Apply rotation (degrees, multiples of 90) and optional center-square crop, then write JPEG.
     * Output is scaled so longest edge ≤ [MediaLimits.IMAGE_MAX_EDGE_PX].
     */
    fun prepare(
        source: File,
        rotationDegrees: Int,
        squareCrop: Boolean,
    ): File {
        val decoded = BitmapFactory.decodeFile(source.absolutePath)
            ?: error("Unable to decode captured image")
        var working = applyRotation(decoded, rotationDegrees)
        if (working !== decoded) decoded.recycle()
        if (squareCrop) {
            val cropped = centerSquare(working)
            if (cropped !== working) {
                working.recycle()
                working = cropped
            }
        }
        val scaled = scaleDown(working, MediaLimits.IMAGE_MAX_EDGE_PX)
        if (scaled !== working) working.recycle()
        val out = File(context.cacheDir, "prepare/prep_${System.currentTimeMillis()}.jpg").apply {
            parentFile?.mkdirs()
        }
        FileOutputStream(out).use { stream ->
            var quality = MediaLimits.IMAGE_JPEG_QUALITY
            scaled.compress(Bitmap.CompressFormat.JPEG, quality, stream)
            while (out.length() > MediaLimits.IMAGE_MAX_BYTES && quality > MIN_JPEG_QUALITY) {
                quality -= JPEG_QUALITY_STEP
                FileOutputStream(out).use { retry ->
                    scaled.compress(Bitmap.CompressFormat.JPEG, quality, retry)
                }
            }
        }
        scaled.recycle()
        require(out.length() <= MediaLimits.IMAGE_MAX_BYTES) {
            "Image still exceeds ${MediaLimits.IMAGE_MAX_BYTES} bytes after prepare"
        }
        return out
    }

    private fun applyRotation(source: Bitmap, degrees: Int): Bitmap {
        val normalized = ((degrees % 360) + 360) % 360
        if (normalized == 0) return source
        val matrix = Matrix().apply { postRotate(normalized.toFloat()) }
        return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
    }

    private fun centerSquare(source: Bitmap): Bitmap {
        val size = minOf(source.width, source.height)
        val left = (source.width - size) / 2
        val top = (source.height - size) / 2
        return Bitmap.createBitmap(source, left, top, size, size)
    }

    private fun scaleDown(source: Bitmap, maxEdge: Int): Bitmap {
        val longest = maxOf(source.width, source.height)
        if (longest <= maxEdge) return source
        val scale = maxEdge.toFloat() / longest.toFloat()
        val nw = (source.width * scale).toInt().coerceAtLeast(1)
        val nh = (source.height * scale).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(source, nw, nh, true)
    }

    private companion object {
        const val MIN_JPEG_QUALITY = 40
        const val JPEG_QUALITY_STEP = 8
    }
}
