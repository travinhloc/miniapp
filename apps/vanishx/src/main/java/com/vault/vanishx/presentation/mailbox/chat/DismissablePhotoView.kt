@file:Suppress("ComplexMethod", "NestedBlockDepth", "ReturnCount")

package com.vault.vanishx.presentation.mailbox.chat

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import com.github.chrisbanes.photoview.PhotoView

/**
 * PhotoView that swipes down to dismiss at min zoom (before the attacher consumes the stream).
 */
internal class DismissablePhotoView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : PhotoView(context, attrs) {

    var onDismissPull: (deltaY: Float) -> Unit = {}
    var onDismissRelease: () -> Unit = {}

    private val dismissTracker = SwipeDownDismissTracker(
        context = context,
        onPull = { delta -> onDismissPull(delta) },
        onRelease = { onDismissRelease() },
    )

    fun resetDismissGesture() {
        dismissTracker.reset()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val dismissEnabled = event.pointerCount == 1 && scale <= MAX_SCALE_FOR_DISMISS
        if (dismissTracker.handleTouchEvent(event, enabled = dismissEnabled)) {
            parent?.requestDisallowInterceptTouchEvent(true)
            return true
        }
        return super.onTouchEvent(event)
    }

    private companion object {
        const val MAX_SCALE_FOR_DISMISS = 1.02f
    }
}
