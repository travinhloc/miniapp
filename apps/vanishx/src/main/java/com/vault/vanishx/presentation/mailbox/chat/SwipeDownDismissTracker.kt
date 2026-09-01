package com.vault.vanishx.presentation.mailbox.chat

import android.content.Context
import android.view.MotionEvent
import android.view.ViewConfiguration
import kotlin.math.abs

/** Shared vertical swipe-down gesture detection for photo/video viewer pages. */
internal class SwipeDownDismissTracker(
    context: Context,
    private val onPull: (deltaY: Float) -> Unit,
    private val onRelease: () -> Unit,
) {
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    private var downRawX = 0f
    private var downRawY = 0f
    private var lastY = 0f
    var dragging = false
        private set

    fun reset() {
        dragging = false
    }

    /**
     * @return true when the event was consumed by an active dismiss drag.
     */
    fun handleTouchEvent(
        event: MotionEvent,
        enabled: Boolean = true,
    ): Boolean {
        if (!enabled || event.pointerCount != 1) {
            if (event.actionMasked == MotionEvent.ACTION_POINTER_DOWN) {
                dragging = false
            }
            return false
        }
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downRawX = event.rawX
                downRawY = event.rawY
                lastY = event.y
                dragging = false
            }
            MotionEvent.ACTION_MOVE -> {
                if (!dragging) {
                    val dx = event.rawX - downRawX
                    val dy = event.rawY - downRawY
                    if (dy > touchSlop && dy > abs(dx) * VERTICAL_BIAS) {
                        dragging = true
                    }
                }
                if (dragging) {
                    val delta = event.y - lastY
                    lastY = event.y
                    onPull(delta)
                    return true
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (dragging) {
                    dragging = false
                    onRelease()
                    return true
                }
            }
        }
        return false
    }

    private companion object {
        const val VERTICAL_BIAS = 1.15f
    }
}

internal const val PHOTO_VIEWER_DISMISS_THRESHOLD_DP = 120f
