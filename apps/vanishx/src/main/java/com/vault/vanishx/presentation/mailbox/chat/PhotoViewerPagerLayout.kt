@file:Suppress("ReturnCount")

package com.vault.vanishx.presentation.mailbox.chat

import android.annotation.SuppressLint
import android.content.Context
import android.widget.FrameLayout
import androidx.viewpager2.widget.ViewPager2

/**
 * ViewPager2 + PhotoView gallery: pager · pinch-zoom · swipe-down dismiss.
 */
@SuppressLint("ClickableViewAccessibility")
internal class PhotoViewerPagerLayout(
    context: Context,
) : FrameLayout(context) {

    private val viewPager = ViewPager2(context)
    private val dismissThresholdPx = PHOTO_VIEWER_DISMISS_THRESHOLD_DP * resources.displayMetrics.density

    private var pages: List<MediaViewerPage> = emptyList()
    private var callbacks = Callbacks()
    private var dismissOffsetPx = 0f
    private var chromeVisible: Boolean = true

    init {
        addView(
            viewPager,
            LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT),
        )
        viewPager.registerOnPageChangeCallback(
            object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    dismissOffsetPx = 0f
                    callbacks.onDismissOffsetChanged(0f)
                    callbacks.onPageSelected(position)
                    updatePagerInputEnabled()
                }
            },
        )
    }

    fun setCallbacks(callbacks: Callbacks) {
        this.callbacks = callbacks
    }

    fun setChromeVisible(visible: Boolean) {
        chromeVisible = visible
        (viewPager.adapter as? PhotoViewerPagerAdapter)?.setChromeVisible(visible)
    }

    fun setContent(pages: List<MediaViewerPage>, initialIndex: Int) {
        if (pagesContentEquals(this.pages, pages) && viewPager.adapter != null) return
        this.pages = pages
        val targetIndex = if (viewPager.adapter == null) {
            initialIndex.coerceIn(0, pages.lastIndex.coerceAtLeast(0))
        } else {
            viewPager.currentItem.coerceIn(0, pages.lastIndex.coerceAtLeast(0))
        }
        viewPager.adapter = PhotoViewerPagerAdapter(
            pages = pages,
            onPhotoTap = { callbacks.onPhotoTap() },
            onVideoControlsVisible = { controlsVisible ->
                callbacks.onVideoControlsVisible(controlsVisible)
            },
            onScaleChanged = { updatePagerInputEnabled() },
            onDismissPull = { deltaY ->
                dismissOffsetPx = (dismissOffsetPx + deltaY).coerceAtLeast(0f)
                callbacks.onDismissOffsetChanged(dismissOffsetPx)
                updatePagerInputEnabled()
            },
            onDismissRelease = {
                if (dismissOffsetPx >= dismissThresholdPx) {
                    callbacks.onDismissRequested()
                } else {
                    dismissOffsetPx = 0f
                    callbacks.onDismissOffsetChanged(0f)
                }
                updatePagerInputEnabled()
            },
            onHorizontalSwipe = { delta -> movePage(delta) },
        ).also { adapter ->
            adapter.setChromeVisible(chromeVisible)
        }
        viewPager.offscreenPageLimit = 1
        viewPager.setCurrentItem(targetIndex, false)
        updatePagerInputEnabled()
    }

    private fun updatePagerInputEnabled() {
        // PhotoView and ViewPager2 both claim the same touch stream on several
        // Android versions. DismissablePhotoView owns the complete gesture and
        // requests a page change explicitly, so keep the native detector off.
        viewPager.isUserInputEnabled = false
    }

    private fun movePage(delta: Int) {
        if (pages.isEmpty() || dismissOffsetPx > 0f) return
        val target = (viewPager.currentItem + delta).coerceIn(0, pages.lastIndex)
        if (target != viewPager.currentItem) viewPager.setCurrentItem(target, true)
    }

    internal data class Callbacks(
        val onDismissOffsetChanged: (Float) -> Unit = {},
        val onDismissRequested: () -> Unit = {},
        val onPageSelected: (Int) -> Unit = {},
        val onPhotoTap: () -> Unit = {},
        val onVideoControlsVisible: (Boolean) -> Unit = {},
    )

    private companion object {
        private fun pagesContentEquals(
            left: List<MediaViewerPage>,
            right: List<MediaViewerPage>,
        ): Boolean {
            if (left.size != right.size) return false
            return left.indices.all { index ->
                val a = left[index]
                val b = right[index]
                a.messageId == b.messageId &&
                    a.mediaPath == b.mediaPath &&
                    a.mediaKind == b.mediaKind
            }
        }
    }
}
