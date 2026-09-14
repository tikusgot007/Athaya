package com.athaya.printer.renderer

import android.graphics.Canvas
import android.graphics.Paint
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint

/**
 * Small set of text-drawing helpers shared by every renderer. Kept
 * deliberately thin: renderers own layout decisions (where things go),
 * this file only owns how a piece of text gets measured/drawn/wrapped.
 */
object TextRenderUtils {

    enum class Align { LEFT, CENTER, RIGHT }

    fun paint(
        sizePx: Float,
        bold: Boolean = false,
        align: Align = Align.LEFT,
        color: Int = android.graphics.Color.BLACK,
    ): TextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = sizePx
        isFakeBoldText = bold
        this.color = color
        textAlign = when (align) {
            Align.LEFT -> Paint.Align.LEFT
            Align.CENTER -> Paint.Align.CENTER
            Align.RIGHT -> Paint.Align.RIGHT
        }
    }

    /**
     * Draws a single line of text at [x], [y] (baseline) using [paint].
     * [x] must already match paint.textAlign (e.g. center x for CENTER align).
     */
    fun drawLine(canvas: Canvas, text: String, x: Float, y: Float, paint: TextPaint) {
        canvas.drawText(text, x, y, paint)
    }

    /**
     * Draws [text] wrapped to [maxWidthPx] starting at ([x], [top]), using
     * StaticLayout so multi-line wrapping matches Android's own text engine
     * (used for address lines and long item names). Returns the height in
     * pixels actually consumed, so callers can advance their cursor.
     */
    fun drawWrapped(
        canvas: Canvas,
        text: String,
        x: Float,
        top: Float,
        maxWidthPx: Int,
        paint: TextPaint,
        lineSpacingExtraPx: Float = 0f,
        lineSpacingMultiplier: Float = 1f,
    ): Float {
        if (text.isEmpty()) return 0f
        val layoutAlign = when (paint.textAlign) {
            Paint.Align.CENTER -> Layout.Alignment.ALIGN_CENTER
            Paint.Align.RIGHT -> Layout.Alignment.ALIGN_OPPOSITE
            else -> Layout.Alignment.ALIGN_NORMAL
        }
        // StaticLayout always draws left-aligned internally relative to its own
        // origin; canvas.drawText respects paint.textAlign, but StaticLayout
        // instead needs Layout.Alignment and a plain LEFT paint for correct x
        // math, so force LEFT on a copy to avoid double-applying alignment.
        val layoutPaint = TextPaint(paint).apply { textAlign = Paint.Align.LEFT }
        val layout = StaticLayout.Builder
            .obtain(text, 0, text.length, layoutPaint, maxWidthPx.coerceAtLeast(1))
            .setAlignment(layoutAlign)
            .setLineSpacing(lineSpacingExtraPx, lineSpacingMultiplier)
            .setIncludePad(false)
            .build()

        canvas.save()
        canvas.translate(x, top)
        layout.draw(canvas)
        canvas.restore()
        return layout.height.toFloat()
    }

    /** Measures the height wrapped text would take, without drawing it. */
    fun measureWrappedHeight(
        text: String,
        maxWidthPx: Int,
        paint: TextPaint,
        lineSpacingExtraPx: Float = 0f,
        lineSpacingMultiplier: Float = 1f,
    ): Int {
        if (text.isEmpty()) return 0
        val layoutPaint = TextPaint(paint).apply { textAlign = Paint.Align.LEFT }
        val layout = StaticLayout.Builder
            .obtain(text, 0, text.length, layoutPaint, maxWidthPx.coerceAtLeast(1))
            .setLineSpacing(lineSpacingExtraPx, lineSpacingMultiplier)
            .setIncludePad(false)
            .build()
        return layout.height
    }
}
