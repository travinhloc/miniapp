@file:Suppress("ReturnCount")

package com.vault.vanishx.presentation.mailbox.chat

import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Size
import kotlin.math.max

internal data class GalleryMediaItem(
    val uri: Uri,
    val isVideo: Boolean,
    val durationMs: Long = 0L,
    val dateAddedSec: Long,
    val mediaId: Long = 0L,
)

internal fun loadRecentGalleryMedia(context: Context, limit: Int): List<GalleryMediaItem> {
    val merged = buildList {
        addAll(queryImages(context, limit))
        addAll(queryVideos(context, limit))
    }
    return merged
        .sortedByDescending { it.dateAddedSec }
        .distinctBy { it.uri.toString() }
        .take(limit)
}

private fun queryImages(context: Context, limit: Int): List<GalleryMediaItem> {
    val collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
    val projection = arrayOf(
        MediaStore.Images.Media._ID,
        MediaStore.Images.Media.DATE_ADDED,
    )
    val sort = "${MediaStore.Images.Media.DATE_ADDED} DESC"
    return runCatching {
        context.contentResolver.query(collection, projection, null, null, sort)?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
            buildList {
                var count = 0
                while (cursor.moveToNext() && count < limit) {
                    val id = cursor.getLong(idCol)
                    add(
                        GalleryMediaItem(
                            uri = ContentUris.withAppendedId(collection, id),
                            isVideo = false,
                            dateAddedSec = cursor.getLong(dateCol),
                            mediaId = id,
                        ),
                    )
                    count++
                }
            }
        }.orEmpty()
    }.getOrDefault(emptyList())
}

private fun queryVideos(context: Context, limit: Int): List<GalleryMediaItem> {
    val collection = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
    val projection = arrayOf(
        MediaStore.Video.Media._ID,
        MediaStore.Video.Media.DATE_ADDED,
        MediaStore.Video.Media.DURATION,
    )
    val sort = "${MediaStore.Video.Media.DATE_ADDED} DESC"
    return runCatching {
        context.contentResolver.query(collection, projection, null, null, sort)?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)
            val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
            buildList {
                var count = 0
                while (cursor.moveToNext() && count < limit) {
                    val id = cursor.getLong(idCol)
                    add(
                        GalleryMediaItem(
                            uri = ContentUris.withAppendedId(collection, id),
                            isVideo = true,
                            durationMs = cursor.getLong(durationCol).coerceAtLeast(0L),
                            dateAddedSec = cursor.getLong(dateCol),
                            mediaId = id,
                        ),
                    )
                    count++
                }
            }
        }.orEmpty()
    }.getOrDefault(emptyList())
}

internal sealed interface GallerySelectionToggle {
    val items: List<Uri>

    data class Changed(override val items: List<Uri>) : GallerySelectionToggle
    data object MaxReached : GallerySelectionToggle {
        override val items: List<Uri> get() = emptyList()
    }
}

internal fun toggleGallerySelection(current: List<Uri>, uri: Uri, max: Int): GallerySelectionToggle {
    val index = current.indexOf(uri)
    if (index >= 0) {
        return GallerySelectionToggle.Changed(current.toMutableList().also { it.removeAt(index) })
    }
    if (current.size >= max) {
        return GallerySelectionToggle.MaxReached
    }
    return GallerySelectionToggle.Changed(current + uri)
}

internal const val GALLERY_RECENT_LIMIT = 120

private const val THUMB_MAX_EDGE = 512

/** Frame/thumbnail for gallery video tiles — Coil cannot decode `content://` video URIs. */
internal fun loadGalleryVideoThumbnail(context: Context, item: GalleryMediaItem): Bitmap? {
    if (!item.isVideo) return null
    return runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            context.contentResolver.loadThumbnail(
                item.uri,
                Size(THUMB_MAX_EDGE, THUMB_MAX_EDGE),
                null,
            )
        } else {
            loadLegacyVideoThumbnail(context, item)
        }
    }.getOrNull()
}

@Suppress("DEPRECATION")
private fun loadLegacyVideoThumbnail(context: Context, item: GalleryMediaItem): Bitmap? {
    if (item.mediaId > 0L) {
        MediaStore.Video.Thumbnails.getThumbnail(
            context.contentResolver,
            item.mediaId,
            MediaStore.Video.Thumbnails.MINI_KIND,
            null,
        )?.let { return it }
    }
    val retriever = MediaMetadataRetriever()
    return try {
        retriever.setDataSource(context, item.uri)
        retriever.getFrameAtTime(0L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
            ?.let { scaleDownBitmap(it, THUMB_MAX_EDGE) }
    } catch (_: Exception) {
        null
    } finally {
        retriever.release()
    }
}

private fun scaleDownBitmap(source: Bitmap, maxEdge: Int): Bitmap {
    val longest = max(source.width, source.height)
    if (longest <= maxEdge) return source
    val scale = maxEdge.toFloat() / longest.toFloat()
    val w = (source.width * scale).toInt().coerceAtLeast(1)
    val h = (source.height * scale).toInt().coerceAtLeast(1)
    return Bitmap.createScaledBitmap(source, w, h, true).also {
        if (it !== source) source.recycle()
    }
}
