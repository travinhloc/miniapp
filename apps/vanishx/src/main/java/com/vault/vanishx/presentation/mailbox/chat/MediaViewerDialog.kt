@file:Suppress("MagicNumber")

package com.vault.vanishx.presentation.mailbox.chat

import android.net.Uri
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
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
                AttachmentMeta.KIND_VIDEO -> VideoPlayer(path = message.mediaLocalPath)
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

@Composable
private fun VideoPlayer(path: String?) {
    val context = LocalContext.current
    val file = path?.let { File(it) }?.takeIf { it.exists() }
    if (file == null) {
        Text(stringResource(R.string.room_media_missing))
        return
    }
    val player = remember(file.absolutePath) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(Uri.fromFile(file)))
            prepare()
            playWhenReady = true
        }
    }
    DisposableEffect(player) {
        onDispose { player.release() }
    }
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
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f),
    )
}
