package com.athaya.printer.renderer

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import com.athaya.printer.model.AddressData
import com.athaya.printer.printer.PrintableWidthExceededException
import com.athaya.printer.printer.PrinterProfile

/**
 * Renders AddressData onto a Bitmap. No rotation is applied (unlike nota):
 * an address sticker is printed upright, on continuous stock, so its
 * height grows with content rather than being fixed. Width, however, is
 * always bounded by the template/profile.
 */
class AddressRenderer : PrintRenderer<AddressData> {

    override fun render(data: AddressData, profile: PrinterProfile): Bitmap =
        renderWithTemplate(data, profile, AddressTemplateSpec.PRODUCTION)

    /**
     * Renders against an explicit [template] (e.g. AddressTemplateSpec.TEST_58MM
     * for the 58mm development printer). The production template's width is
     * never silently scaled to fit a smaller printer — pick a template that
     * already matches the target printer's printable width.
     */
    fun renderWithTemplate(data: AddressData, profile: PrinterProfile, template: AddressTemplateSpec): Bitmap {
        val dpi = profile.dpi
        val widthPx = mmToPx(template.widthMm, dpi)

        if (widthPx > profile.printableDots) {
            throw PrintableWidthExceededException(
                actualWidthPx = widthPx,
                printableDots = profile.printableDots,
                printerName = profile.printerName,
            )
        }

        val marginPx = mmToPx(3f, dpi).toFloat()
        val contentWidthPx = (widthPx - 2 * marginPx).toInt()

        val minHeightPx = mmToPx(template.minHeightMm, dpi)
        val measuredHeightPx = measureContentHeight(data, contentWidthPx, dpi)
        val heightPx = maxOf(minHeightPx, measuredHeightPx)

        val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)
        drawContent(canvas, data, widthPx, contentWidthPx, dpi)
        return bitmap
    }

    private fun measureContentHeight(data: AddressData, contentWidthPx: Int, dpi: Int): Int {
        val marginPx = mmToPx(3f, dpi)
        var height = marginPx * 2

        val namePaint = TextRenderUtils.paint(sizePx = mmToPx(4f, dpi).toFloat(), bold = true)
        height += TextRenderUtils.measureWrappedHeight(data.recipientName, contentWidthPx, namePaint).coerceAtLeast(mmToPx(4.5f, dpi))

        val bodyPaint = TextRenderUtils.paint(sizePx = mmToPx(3f, dpi).toFloat())
        height += mmToPx(1.5f, dpi)
        height += TextRenderUtils.measureWrappedHeight(data.phone, contentWidthPx, bodyPaint).coerceAtLeast(mmToPx(3.5f, dpi))
        height += mmToPx(1.5f, dpi)
        height += TextRenderUtils.measureWrappedHeight(data.address, contentWidthPx, bodyPaint)
        height += mmToPx(1.5f, dpi)
        height += TextRenderUtils.measureWrappedHeight("${data.city} ${data.postalCode}", contentWidthPx, bodyPaint)

        if (data.note.isNotBlank()) {
            height += mmToPx(2f, dpi)
            height += TextRenderUtils.measureWrappedHeight(data.note, contentWidthPx, bodyPaint)
        }
        if (data.barcodeData != null || data.qrData != null) {
            // Reserve space for barcode/QR; actual drawing implemented once
            // barcode/QR generation is added (kept out of scope for Phase 2).
            height += mmToPx(20f, dpi)
        }
        return height
    }

    private fun drawContent(canvas: Canvas, data: AddressData, widthPx: Int, contentWidthPx: Int, dpi: Int) {
        val marginPx = mmToPx(3f, dpi).toFloat()
        var y = marginPx

        val namePaint = TextRenderUtils.paint(sizePx = mmToPx(4f, dpi).toFloat(), bold = true)
        y += TextRenderUtils.drawWrapped(canvas, data.recipientName, marginPx, y, contentWidthPx, namePaint)
            .coerceAtLeast(mmToPx(4.5f, dpi).toFloat())

        val bodyPaint = TextRenderUtils.paint(sizePx = mmToPx(3f, dpi).toFloat())
        y += mmToPx(1.5f, dpi)
        y += TextRenderUtils.drawWrapped(canvas, data.phone, marginPx, y, contentWidthPx, bodyPaint)
            .coerceAtLeast(mmToPx(3.5f, dpi).toFloat())

        y += mmToPx(1.5f, dpi)
        y += TextRenderUtils.drawWrapped(canvas, data.address, marginPx, y, contentWidthPx, bodyPaint)

        y += mmToPx(1.5f, dpi)
        y += TextRenderUtils.drawWrapped(canvas, "${data.city} ${data.postalCode}", marginPx, y, contentWidthPx, bodyPaint)

        if (data.note.isNotBlank()) {
            y += mmToPx(2f, dpi)
            TextRenderUtils.drawWrapped(canvas, "Catatan: ${data.note}", marginPx, y, contentWidthPx, bodyPaint)
        }
    }
}
