package com.athaya.printer.renderer

import android.graphics.Bitmap
import android.graphics.Matrix

/**
 * Rotates [source] 90 degrees clockwise. The resulting bitmap's width is
 * the source's height and vice versa — this is the ONLY place rotation
 * happens; printers never rotate anything themselves.
 */
fun rotateBitmap90Clockwise(source: Bitmap): Bitmap {
    val matrix = Matrix().apply { postRotate(90f) }
    return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
}
