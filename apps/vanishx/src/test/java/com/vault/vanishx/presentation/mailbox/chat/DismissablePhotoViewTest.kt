package com.vault.vanishx.presentation.mailbox.chat

import android.content.Context
import android.view.MotionEvent
import androidx.test.core.app.ApplicationProvider
import io.kotest.matchers.shouldBe
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class DismissablePhotoViewTest {

    @Test
    fun `dispatch observes horizontal swipe before PhotoView touch listener`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        var pageDelta: Int? = null
        val view = DismissablePhotoView(context).apply {
            layout(0, 0, 1080, 2000)
            onHorizontalSwipe = { pageDelta = it }
        }
        val downTime = 1_000L
        val down = MotionEvent.obtain(
            downTime,
            downTime,
            MotionEvent.ACTION_DOWN,
            800f,
            900f,
            0,
        )
        val up = MotionEvent.obtain(
            downTime,
            downTime + 250L,
            MotionEvent.ACTION_UP,
            200f,
            900f,
            0,
        )

        try {
            view.dispatchTouchEvent(down)
            view.dispatchTouchEvent(up)
        } finally {
            down.recycle()
            up.recycle()
        }

        pageDelta shouldBe 1
    }

    @Test
    fun `horizontal drag is handed to pager`() {
        isHorizontalPagerGesture(dx = 40f, dy = 5f, touchSlop = 8) shouldBe true
        isHorizontalPagerGesture(dx = -40f, dy = 5f, touchSlop = 8) shouldBe true
    }

    @Test
    fun `vertical diagonal and tiny drags stay with photo`() {
        isHorizontalPagerGesture(dx = 5f, dy = 40f, touchSlop = 8) shouldBe false
        isHorizontalPagerGesture(dx = 6f, dy = 1f, touchSlop = 8) shouldBe false
        isHorizontalPagerGesture(dx = 20f, dy = 20f, touchSlop = 8) shouldBe false
    }
}
