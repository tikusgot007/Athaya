package com.athaya.printer.renderer

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import com.athaya.printer.printer.PrinterProfile

/**
 * Builds a small, deterministic Bitmap for "Test Print" in PrinterSettingsScreen —
 * used to verify the FULL raster pipeline (Bitmap -> monochrome -> ESC/POS ->
 * Bluetooth) actually works end to end, as opposed to sending a raw text
 * string which would bypass EscPosEncoder/MonochromeConverter entirely and
 * prove nothing about the raster path.
 *
 * Uses the exact same Bitmap/Canvas/Paint + TextRenderUtils/mmToPx approach
 * as NotaRenderer/AddressRenderer, so it exercises the same code path a real
 * print job would.
 */
object TestPrintBitmap {

    fun build(profile: PrinterProfile): Bitmap {
        val dpi = profile.dpi
        // Width is exactly printableDots -- the safe maximum for this
        // profile, never guessed from paperWidthMm.
        val widthPx = profile.printableDots
        val heightPx = mmToPx(20f, dpi)

        val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)

        val centerX = widthPx / 2f
        val titlePaint = TextRenderUtils.paint(sizePx = mmToPx(3.5f, dpi).toFloat(), bold = true, align = TextRenderUtils.Align.CENTER)
        TextRenderUtils.drawLine(canvas, "TEST PRINT", centerX, mmToPx(6f, dpi).toFloat(), titlePaint)

        val infoPaint = TextRenderUtils.paint(sizePx = mmToPx(2.4f, dpi).toFloat(), align = TextRenderUtils.Align.CENTER)
        TextRenderUtils.drawLine(
            canvas,
            "${profile.printerName} | ${profile.paperWidthMm}mm | ${profile.dpi}dpi",
            centerX,
            mmToPx(11f, dpi).toFloat(),
            infoPaint,
        )
        TextRenderUtils.drawLine(
            canvas,
            "printableDots=${profile.printableDots}",
            centerX,
            mmToPx(15f, dpi).toFloat(),
            infoPaint,
        )

        return bitmap
    }
}
