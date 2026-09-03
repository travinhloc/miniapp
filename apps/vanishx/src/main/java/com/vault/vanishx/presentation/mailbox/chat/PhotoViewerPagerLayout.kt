@file:Suppress("ReturnCount")

package com.vault.vanishx.presentation.mailbox.chat

import android.annotation.SuppressLint
import android.content.Context
import android.widget.FrameLayout
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.vault.vanishx.domain.model.AttachmentMeta

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
            onScaleChanged = { scale -> onCurrentPageScaleChanged(scale) },
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
        )
        viewPager.offscreenPageLimit = 1
        viewPager.setCurrentItem(targetIndex, false)
        updatePagerInputEnabled()
    }

    private fun onCurrentPageScaleChanged(scale: Float) {
        updatePagerInputEnabled(zoomedOverride = scale > MIN_SCALE + SCALE_EPSILON)
    }

    private fun updatePagerInputEnabled(zoomedOverride: Boolean? = null) {
        val zoomed = zoomedOverride ?: isCurrentPageZoomed()
        viewPager.isUserInputEnabled = !zoomed && dismissOffsetPx <= 0f
    }

    private fun isCurrentPageZoomed(): Boolean {
        val photoView = currentPhotoView() ?: return false
        return photoView.scale > MIN_SCALE + SCALE_EPSILON
    }

    private fun currentPhotoView(): DismissablePhotoView? {
        val page = pages.getOrNull(viewPager.currentItem) ?: return null
        if (page.mediaKind == AttachmentMeta.KIND_VIDEO) return null
        val recycler = viewPager.getChildAt(0) as? RecyclerView ?: return null
        val holder = recycler.findViewHolderForAdapterPosition(viewPager.currentItem) ?: return null
        return holder.itemView as? DismissablePhotoView
    }

    internal data class Callbacks(
        val onDismissOffsetChanged: (Float) -> Unit = {},
        val onDismissRequested: () -> Unit = {},
        val onPageSelected: (Int) -> Unit = {},
        val onPhotoTap: () -> Unit = {},
    )

    private companion object {
        const val MIN_SCALE = 1f
        const val SCALE_EPSILON = 0.01f

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
