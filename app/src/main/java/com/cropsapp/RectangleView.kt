package com.cropsapp

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import kotlin.math.abs

class RectangleView constructor(context: Context?, attributeSet: AttributeSet?) :
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

    fun draw(boxes: Set<FloatArray>?) {
        boxes?.apply {
            rectangles = map { box ->
                RectF(box[0], box[1], box[2], box[3])
            }
        }
        invalidate()
    }
}