@file:Suppress("ComplexMethod", "NestedBlockDepth", "ReturnCount")

package com.vault.vanishx.presentation.mailbox.chat

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ViewConfiguration
import com.github.chrisbanes.photoview.PhotoView
import kotlin.math.abs

/**
 * PhotoView that swipes down to dismiss at min zoom (before the attacher consumes the stream).
 */
internal class DismissablePhotoView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : PhotoView(context, attrs) {

    var onDismissPull: (deltaY: Float) -> Unit = {}
    var onDismissRelease: () -> Unit = {}
    var onHorizontalSwipe: (pageDelta: Int) -> Unit = {}

    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    private var downX = 0f
    private var downY = 0f
    private var swipeStartedAtBaseScale = false
    private var swipeSinglePointer = true

    private val dismissTracker = SwipeDownDismissTracker(
        context = context,
        onPull = { delta -> onDismissPull(delta) },
        onRelease = { onDismissRelease() },
    )

    fun resetDismissGesture() {
        dismissTracker.reset()
    }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = event.x
                downY = event.y
                swipeStartedAtBaseScale = scale <= MAX_SCALE_FOR_PAGER
                swipeSinglePointer = true
                // ViewPager2's native detector is disabled; retain the complete
                // stream here and decide between zoom/pan, dismiss and page swipe.
                parent?.requestDisallowInterceptTouchEvent(true)
            }
            MotionEvent.ACTION_POINTER_DOWN -> swipeSinglePointer = false
        }
        val dismissEnabled = event.pointerCount == 1 && scale <= MAX_SCALE_FOR_DISMISS
        if (dismissTracker.handleTouchEvent(event, enabled = dismissEnabled)) {
            parent?.requestDisallowInterceptTouchEvent(true)
            return true
        }
        // PhotoViewAttacher installs an OnTouchListener which consumes the
        // stream before View.onTouchEvent. Intercept at dispatch level so our
        // gallery gestures always observe the same events as the attacher.
        val handled = super.dispatchTouchEvent(event)
        if (event.actionMasked == MotionEvent.ACTION_UP) {
            val dx = event.x - downX
            val dy = event.y - downY
            if (
                swipeStartedAtBaseScale &&
                swipeSinglePointer &&
                isHorizontalPagerGesture(dx, dy, touchSlop)
            ) {
                onHorizontalSwipe(if (dx < 0f) 1 else -1)
            }
            parent?.requestDisallowInterceptTouchEvent(false)
        } else if (event.actionMasked == MotionEvent.ACTION_CANCEL) {
            parent?.requestDisallowInterceptTouchEvent(false)
        }
        return handled
    }

    private companion object {
        const val MAX_SCALE_FOR_DISMISS = 1.02f
        const val MAX_SCALE_FOR_PAGER = 1.05f
    }
}

internal fun isHorizontalPagerGesture(dx: Float, dy: Float, touchSlop: Int): Boolean =
    abs(dx) > touchSlop && abs(dx) > abs(dy) * 1.15f
