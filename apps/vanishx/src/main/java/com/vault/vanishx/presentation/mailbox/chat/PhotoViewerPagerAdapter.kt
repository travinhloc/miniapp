@file:Suppress("ComplexMethod", "NestedBlockDepth", "ReturnCount", "LongParameterList")

package com.vault.vanishx.presentation.mailbox.chat

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.vault.vanishx.domain.model.AttachmentMeta

internal class PhotoViewerPagerAdapter(
    private val pages: List<MediaViewerPage>,
    private val onPhotoTap: () -> Unit,
    private val onScaleChanged: (scale: Float) -> Unit,
    private val onDismissPull: (deltaY: Float) -> Unit,
    private val onDismissRelease: () -> Unit,
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    override fun getItemCount(): Int = pages.size

    override fun getItemViewType(position: Int): Int {
        return if (pages[position].mediaKind == AttachmentMeta.KIND_VIDEO) {
            VIEW_TYPE_VIDEO
        } else {
            VIEW_TYPE_IMAGE
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_VIDEO -> {
                val playerView = PlayerView(parent.context).apply {
                    layoutParams = FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT,
                    )
                    useController = true
                }
                VideoViewHolder(playerView, onDismissPull, onDismissRelease)
            }
            else -> {
                val photoView = DismissablePhotoView(parent.context).apply {
                    layoutParams = FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT,
                    )
                    maximumScale = MAX_ZOOM
                    mediumScale = MEDIUM_ZOOM
                    minimumScale = 1f
                    setAllowParentInterceptOnEdge(true)
                }
                PhotoViewHolder(photoView)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val page = pages[position]
        when (holder) {
            is PhotoViewHolder -> holder.bind(
                path = page.mediaPath,
                onTap = onPhotoTap,
                onScaleChanged = onScaleChanged,
                onDismissPull = onDismissPull,
                onDismissRelease = onDismissRelease,
            )
            is VideoViewHolder -> holder.bind(
                path = page.mediaPath,
                autoPlay = true,
            )
        }
    }

    override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
        if (holder is VideoViewHolder) {
            holder.release()
        }
        super.onViewRecycled(holder)
    }

    private class PhotoViewHolder(
        private val photoView: DismissablePhotoView,
    ) : RecyclerView.ViewHolder(photoView) {
        fun bind(
            path: String?,
            onTap: () -> Unit,
            onScaleChanged: (Float) -> Unit,
            onDismissPull: (Float) -> Unit,
            onDismissRelease: () -> Unit,
        ) {
            photoView.resetDismissGesture()
            photoView.onDismissPull = onDismissPull
            photoView.onDismissRelease = onDismissRelease
            photoView.setOnViewTapListener { _, _, _ -> onTap() }
            photoView.setOnScaleChangeListener { _, _, _ ->
                onScaleChanged(photoView.scale)
            }
            val uri = resolveMediaPlaybackUri(path)
            if (uri == null) {
                photoView.setImageDrawable(null)
            } else {
                photoView.load(uri) {
                    crossfade(true)
                }
            }
        }
    }

    private class VideoViewHolder(
        private val playerView: PlayerView,
        onDismissPull: (Float) -> Unit,
        onDismissRelease: () -> Unit,
    ) : RecyclerView.ViewHolder(playerView) {
        private var player: ExoPlayer? = null
        private val dismissTracker = SwipeDownDismissTracker(
            context = playerView.context,
            onPull = onDismissPull,
            onRelease = onDismissRelease,
        )

        init {
            playerView.setOnTouchListener { _, event ->
                if (dismissTracker.handleTouchEvent(event)) true else false
            }
        }

        fun bind(path: String?, autoPlay: Boolean) {
            dismissTracker.reset()
            release()
            val uri = resolveMediaPlaybackUri(path) ?: return
            player = ExoPlayer.Builder(itemView.context).build().apply {
                setMediaItem(MediaItem.fromUri(uri))
                prepare()
                playWhenReady = autoPlay
            }
            playerView.player = player
        }

        fun release() {
            playerView.player = null
            player?.release()
            player = null
        }
    }

    private companion object {
        const val VIEW_TYPE_IMAGE = 1
        const val VIEW_TYPE_VIDEO = 2
        const val MAX_ZOOM = 4f
        const val MEDIUM_ZOOM = 2.5f
    }
}
