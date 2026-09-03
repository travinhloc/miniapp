@file:Suppress("MagicNumber", "ReturnCount", "ModifierReused")

package com.vault.vanishx.presentation.mailbox.chat

import android.net.Uri
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.rememberAsyncImagePainter
import com.vault.vanishx.R
import com.vault.vanishx.domain.model.AttachmentMeta
import com.vault.vanishx.domain.model.ChatMessage
import java.io.File

/** In-app preview. Save = Pro (E11-11). */
@Composable
internal fun MediaViewerDialog(
    message: ChatMessage,
    isPro: Boolean,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.room_media_viewer_title)) },
        text = {
            when (message.mediaKind) {
                AttachmentMeta.KIND_IMAGE -> Image(
                    painter = rememberAsyncImagePainter(message.mediaLocalPath),
                    contentDescription = stringResource(R.string.room_media_image_cd),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 360.dp),
                )
                AttachmentMeta.KIND_VIDEO -> MediaPathPlayer(
                    path = message.mediaLocalPath,
                    aspectRatio = 16f / 9f,
                )
                AttachmentMeta.KIND_VOICE -> MediaPathPlayer(
                    path = message.mediaLocalPath,
                    aspectRatio = null,
                )
                else -> Text(message.mediaFileName ?: stringResource(R.string.room_media_file))
            }
        },
        confirmButton = {
            TextButton(onClick = onSave) {
                Text(
                    stringResource(
                        if (isPro) R.string.room_media_save else R.string.room_media_save_pro,
                    ),
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_back))
            }
        },
    )
}

/** Resolves absolute file path or `content://` URI string for playback. */
internal fun resolveMediaPlaybackUri(path: String?): Uri? {
    if (path.isNullOrBlank()) return null
    val file = File(path)
    if (file.exists()) return Uri.fromFile(file)
    return runCatching { Uri.parse(path) }.getOrNull()
}

@Composable
internal fun MediaPathPlayer(
    path: String?,
    aspectRatio: Float?,
    modifier: Modifier = Modifier,
    autoPlay: Boolean = true,
) {
    val context = LocalContext.current
    val uri = remember(path) { resolveMediaPlaybackUri(path) }
    if (uri == null) {
        Text(stringResource(R.string.room_media_missing), modifier = modifier)
        return
    }
    val player = remember(uri) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(uri))
            prepare()
            playWhenReady = autoPlay
        }
    }
    DisposableEffect(player) {
        onDispose { player.release() }
    }
    val baseModifier = modifier.fillMaxWidth().then(
        if (aspectRatio != null) {
            Modifier.aspectRatio(aspectRatio)
        } else {
            Modifier.height(72.dp)
        },
    )
    AndroidView(
        factory = { ctx ->
            PlayerView(ctx).apply {
                this.player = player
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                )
                useController = true
            }
        },
        modifier = baseModifier,
    )
}
