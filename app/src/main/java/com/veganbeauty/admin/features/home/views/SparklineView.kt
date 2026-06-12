package com.veganbeauty.admin.features.home.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View

class SparklineView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 6f
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        color = 0xFF677559.toInt() // Default color secondary
    }

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private var dataPoints: List<Float> = emptyList()
    private val linePath = Path()
    private val fillPath = Path()

    fun setData(points: List<Float>) {
        dataPoints = points
        invalidate()
    }

    fun setLineColor(color: Int) {
        linePaint.color = color
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (dataPoints.size < 2) return

        val width = width.toFloat()
        val height = height.toFloat()

        val paddingLeft = paddingLeft.toFloat()
        val paddingTop = paddingTop.toFloat()
        val paddingRight = paddingRight.toFloat()
        val paddingBottom = paddingBottom.toFloat()

        val usableWidth = width - paddingLeft - paddingRight
        val usableHeight = height - paddingTop - paddingBottom

        val minVal = dataPoints.minOrNull() ?: 0f
        val maxVal = dataPoints.maxOrNull() ?: 100f
        val range = if (maxVal == minVal) 1f else maxVal - minVal

        linePath.reset()
        fillPath.reset()

        val stepX = usableWidth / (dataPoints.size - 1)

        for (i in dataPoints.indices) {
            val x = paddingLeft + i * stepX
            val normY = (dataPoints[i] - minVal) / range
            val y = paddingTop + usableHeight - (normY * usableHeight)

            if (i == 0) {
                linePath.moveTo(x, y)
                fillPath.moveTo(x, paddingTop + usableHeight)
                fillPath.lineTo(x, y)
            } else {
                val prevX = paddingLeft + (i - 1) * stepX
                val prevNormY = (dataPoints[i - 1] - minVal) / range
                val prevY = paddingTop + usableHeight - (prevNormY * usableHeight)

                val cx1 = prevX + stepX / 2f
                val cy1 = prevY
                val cx2 = prevX + stepX / 2f
                val cy2 = y
                linePath.cubicTo(cx1, cy1, cx2, cy2, x, y)
                fillPath.cubicTo(cx1, cy1, cx2, cy2, x, y)
            }

            if (i == dataPoints.size - 1) {
                fillPath.lineTo(x, paddingTop + usableHeight)
                fillPath.close()
            }
        }

        // Setup gradient shader for the fill
        val baseColor = linePaint.color
        val r = (baseColor shr 16) and 0xFF
        val g = (baseColor shr 8) and 0xFF
        val b = baseColor and 0xFF
        val startColor = (0x33 shl 24) or (r shl 16) or (g shl 8) or b
        val endColor = (0x00 shl 24) or (r shl 16) or (g shl 8) or b

        fillPaint.shader = LinearGradient(
            0f, paddingTop, 0f, paddingTop + usableHeight,
            startColor, endColor, Shader.TileMode.CLAMP
        )

        canvas.drawPath(fillPath, fillPaint)
        canvas.drawPath(linePath, linePaint)
    }
}
