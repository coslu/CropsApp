package com.cropsapp

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

class RectangleView(context: Context?, attributeSet: AttributeSet?) :
    View(context, attributeSet) {

    private val paint = Paint().apply {
        color = Color.YELLOW
        strokeWidth = 5f
        style = Paint.Style.STROKE
    }
    private var rectangles = listOf<RectF>()

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        rectangles.forEach {
            canvas.drawRect(it, paint)
        }
    }

    /**
     * Draws the given set of boxes onto the canvas
     */
    fun draw(boxes: Set<FloatArray>?) {
        boxes?.run {
            rectangles = map { box ->
                RectF(box[0], box[1], box[2], box[3])
            }
        }
        invalidate()
    }
}