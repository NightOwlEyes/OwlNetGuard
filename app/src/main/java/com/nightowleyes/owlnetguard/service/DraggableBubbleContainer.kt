package com.nightowleyes.owlnetguard.service

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.widget.LinearLayout
import kotlin.math.hypot

/**
 * Custom container that intercepts touch gestures to provide smooth dragging
 * from ANY point on the floating widget (background or any button), while preserving
 * standard tap/click handling on buttons when not dragged.
 * Also supports long-press to dismiss/turn off the HUD.
 */
class DraggableBubbleContainer @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    interface DragListener {
        fun onDragStart(rawX: Float, rawY: Float)
        fun onDragMove(dx: Float, dy: Float)
        fun onDragEnd()
        fun onLongPressDismiss()
    }

    var dragListener: DragListener? = null

    private var initialTouchX = 0f
    private var initialTouchY = 0f
    private var isDragging = false
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    private val mainHandler = Handler(Looper.getMainLooper())
    private var isLongPressed = false

    private val longPressRunnable = Runnable {
        if (!isDragging) {
            isLongPressed = true
            try {
                performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            } catch (_: Exception) {}
            dragListener?.onLongPressDismiss()
        }
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                initialTouchX = ev.rawX
                initialTouchY = ev.rawY
                isDragging = false
                isLongPressed = false
                mainHandler.removeCallbacks(longPressRunnable)
                mainHandler.postDelayed(longPressRunnable, 750L)
                dragListener?.onDragStart(ev.rawX, ev.rawY)
                return false
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = ev.rawX - initialTouchX
                val dy = ev.rawY - initialTouchY
                if (!isDragging && hypot(dx.toDouble(), dy.toDouble()) > touchSlop) {
                    isDragging = true
                    mainHandler.removeCallbacks(longPressRunnable)
                    return true // Intercept from child buttons!
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                mainHandler.removeCallbacks(longPressRunnable)
                if (isDragging) {
                    isDragging = false
                    dragListener?.onDragEnd()
                    return true
                }
            }
        }
        return isDragging
    }

    override fun onTouchEvent(ev: MotionEvent): Boolean {
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                initialTouchX = ev.rawX
                initialTouchY = ev.rawY
                isDragging = false
                isLongPressed = false
                mainHandler.removeCallbacks(longPressRunnable)
                mainHandler.postDelayed(longPressRunnable, 750L)
                dragListener?.onDragStart(ev.rawX, ev.rawY)
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = ev.rawX - initialTouchX
                val dy = ev.rawY - initialTouchY
                if (!isDragging && hypot(dx.toDouble(), dy.toDouble()) > touchSlop) {
                    isDragging = true
                    mainHandler.removeCallbacks(longPressRunnable)
                }
                if (isDragging) {
                    dragListener?.onDragMove(dx, dy)
                }
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                mainHandler.removeCallbacks(longPressRunnable)
                if (isDragging) {
                    isDragging = false
                    dragListener?.onDragEnd()
                } else if (ev.actionMasked == MotionEvent.ACTION_UP && !isLongPressed) {
                    performClick()
                }
                return true
            }
        }
        return super.onTouchEvent(ev)
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }
}
