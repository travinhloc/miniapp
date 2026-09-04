@file:Suppress("ComplexMethod", "NestedBlockDepth", "ReturnCount", "LongParameterList")

package com.vault.vanishx.presentation.mailbox.chat

import android.annotation.SuppressLint
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.vault.vanishx.domain.model.AttachmentMeta

@SuppressLint("UnsafeOptInUsageError")
internal class PhotoViewerPagerAdapter(
    private val pages: List<MediaViewerPage>,
    private val onPhotoTap: () -> Unit,
    private val onVideoControlsVisible: (Boolean) -> Unit,
    private val onScaleChanged: (scale: Float) -> Unit,
    private val onDismissPull: (deltaY: Float) -> Unit,
    private val onDismissRelease: () -> Unit,
    private val onHorizontalSwipe: (pageDelta: Int) -> Unit,
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var chromeVisible: Boolean = true
    private val attachedVideoHolders = mutableSetOf<VideoViewHolder>()

    fun setChromeVisible(visible: Boolean) {
        if (chromeVisible == visible) return
        chromeVisible = visible
        attachedVideoHolders.forEach { it.applyChromeVisible(visible) }
    }

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
                    // Tap uses PlayerView's own controller toggle; we sync Compose chrome.
                    controllerAutoShow = false
                    controllerHideOnTouch = true
                    controllerShowTimeoutMs = 0
                }
                VideoViewHolder(
                    playerView = playerView,
                    onDismissPull = onDismissPull,
                    onDismissRelease = onDismissRelease,
                    onControlsVisible = onVideoControlsVisible,
                )
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
                    // This view owns horizontal gallery gestures. Allowing the
                    // attacher to hand the stream back at an edge cancels it
                    // before ACTION_UP while ViewPager2 input is disabled.
                    setAllowParentInterceptOnEdge(false)
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
                onHorizontalSwipe = onHorizontalSwipe,
            )
            is VideoViewHolder -> holder.bind(
                path = page.mediaPath,
                autoPlay = true,
                chromeVisible = chromeVisible,
            )
        }
    }

    override fun onViewAttachedToWindow(holder: RecyclerView.ViewHolder) {
        super.onViewAttachedToWindow(holder)
        if (holder is VideoViewHolder) {
            attachedVideoHolders += holder
            holder.applyChromeVisible(chromeVisible)
        }
    }

    override fun onViewDetachedFromWindow(holder: RecyclerView.ViewHolder) {
        if (holder is VideoViewHolder) {
            attachedVideoHolders -= holder
        }
        super.onViewDetachedFromWindow(holder)
    }

    override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
        if (holder is VideoViewHolder) {
            attachedVideoHolders -= holder
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
            onHorizontalSwipe: (Int) -> Unit,
        ) {
            photoView.resetDismissGesture()
            photoView.onDismissPull = onDismissPull
            photoView.onDismissRelease = onDismissRelease
            photoView.onHorizontalSwipe = onHorizontalSwipe
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

    @SuppressLint("ClickableViewAccessibility")
    private class VideoViewHolder(
        private val playerView: PlayerView,
        onDismissPull: (Float) -> Unit,
        onDismissRelease: () -> Unit,
        onControlsVisible: (Boolean) -> Unit,
    ) : RecyclerView.ViewHolder(playerView) {
        private var player: ExoPlayer? = null
        private var suppressControlsCallback = false
        private val dismissTracker = SwipeDownDismissTracker(
            context = playerView.context,
            onPull = onDismissPull,
            onRelease = onDismissRelease,
        )

        init {
            playerView.setControllerVisibilityListener(
                PlayerView.ControllerVisibilityListener { visibility ->
                    if (!suppressControlsCallback) {
                        onControlsVisible(visibility == View.VISIBLE)
                    }
                },
            )
            playerView.setOnTouchListener { _, event ->
                dismissTracker.handleTouchEvent(event)
            }
        }

        fun bind(path: String?, autoPlay: Boolean, chromeVisible: Boolean) {
            dismissTracker.reset()
            release()
            val uri = resolveMediaPlaybackUri(path) ?: return
            player = ExoPlayer.Builder(itemView.context).build().apply {
                setMediaItem(MediaItem.fromUri(uri))
                prepare()
                playWhenReady = autoPlay
            }
            playerView.player = player
            applyChromeVisible(chromeVisible)
        }

        fun applyChromeVisible(visible: Boolean) {
            suppressControlsCallback = true
            try {
                if (visible) {
                    playerView.hideController()
                } else {
                    playerView.showController()
                }
            } finally {
                // Post so PlayerView finishes visibility dispatch first.
                playerView.post { suppressControlsCallback = false }
            }
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
