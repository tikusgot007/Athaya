package com.athaya.printer.renderer

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import com.athaya.printer.model.NotaData
import com.athaya.printer.printer.PrintableWidthExceededException
import com.athaya.printer.printer.PrinterProfile

/**
 * Renders a NotaData into a Bitmap and applies the mandatory 90-degree
 * rotation, per spec: the design is authored at [template] size (production
 * 100x60mm), drawn upright, then rotated so the printed result becomes
 * heightMm x widthMm (production: 60x100mm). The printer never performs
 * this rotation itself.
 *
 * The same render() call backs both the on-screen preview and the bitmap
 * that will later be sent through monochrome + ESC/POS encoding, so
 * preview and print can never drift apart.
 */
class NotaRenderer : PrintRenderer<NotaData> {

    override fun render(data: NotaData, profile: PrinterProfile): Bitmap =
        renderWithTemplate(data, profile, NotaTemplateSpec.PRODUCTION)

    /**
     * Renders against an explicit [template] (e.g. NotaTemplateSpec.TEST_58MM
     * to validate the pipeline on the 58mm development printer) instead of
     * always assuming production. The template's physical size is never
     * altered to "fit" a printer — pick a template already sized correctly.
     */
    fun renderWithTemplate(data: NotaData, profile: PrinterProfile, template: NotaTemplateSpec): Bitmap {
        val dpi = profile.dpi
        val widthPx = mmToPx(template.widthMm, dpi)
        val heightPx = mmToPx(template.heightMm, dpi)

        val upright = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(upright)
        canvas.drawColor(Color.WHITE)

        drawContent(canvas, data, widthPx, dpi)

        val rotated = rotateBitmap90Clockwise(upright)

        if (rotated.width > profile.printableDots) {
            throw PrintableWidthExceededException(
                actualWidthPx = rotated.width,
                printableDots = profile.printableDots,
                printerName = profile.printerName,
            )
        }
        return rotated
    }

    private fun drawContent(canvas: Canvas, data: NotaData, widthPx: Int, dpi: Int) {
        val marginPx = mmToPx(3f, dpi).toFloat()
        val contentWidthPx = (widthPx - 2 * marginPx).toInt()
        val centerX = widthPx / 2f
        var y = marginPx + mmToPx(4f, dpi)

        val titlePaint = TextRenderUtils.paint(sizePx = mmToPx(4.2f, dpi).toFloat(), bold = true, align = TextRenderUtils.Align.CENTER)
        TextRenderUtils.drawLine(canvas, data.storeName, centerX, y, titlePaint)
        y += mmToPx(5f, dpi)

        val labelPaint = TextRenderUtils.paint(sizePx = mmToPx(2.6f, dpi).toFloat(), align = TextRenderUtils.Align.LEFT)
        canvas.drawText("No: ${data.invoiceNumber}   ${data.date}", marginPx, y, labelPaint)
        y += mmToPx(3.6f, dpi)
        canvas.drawText("Customer: ${data.customerName}", marginPx, y, labelPaint)
        y += mmToPx(4.5f, dpi)

        canvas.drawLine(marginPx, y, widthPx - marginPx, y, labelPaint)
        y += mmToPx(3.5f, dpi)

        // Item rows: name (wrapped if long) on its own line, qty x price = subtotal below it.
        val itemPaint = TextRenderUtils.paint(sizePx = mmToPx(2.6f, dpi).toFloat(), align = TextRenderUtils.Align.LEFT)
        for (item in data.items) {
            val nameHeight = TextRenderUtils.drawWrapped(
                canvas = canvas,
                text = item.name,
                x = marginPx,
                top = y - mmToPx(2.6f, dpi),
                maxWidthPx = contentWidthPx,
                paint = itemPaint,
            )
            y += nameHeight.coerceAtLeast(mmToPx(3.2f, dpi).toFloat())
            canvas.drawText(
                "${item.quantity} x ${item.price} = ${item.subtotal}",
                marginPx + mmToPx(2f, dpi),
                y,
                itemPaint,
            )
            y += mmToPx(3.4f, dpi)
        }

        y += mmToPx(1.5f, dpi)
        canvas.drawLine(marginPx, y, widthPx - marginPx, y, labelPaint)
        y += mmToPx(3.6f, dpi)

        val totalsPaint = TextRenderUtils.paint(sizePx = mmToPx(2.8f, dpi).toFloat(), align = TextRenderUtils.Align.LEFT)
        canvas.drawText("Subtotal: ${data.subtotal}", marginPx, y, totalsPaint)
        y += mmToPx(3.6f, dpi)
        canvas.drawText("Diskon: ${data.discount}", marginPx, y, totalsPaint)
        y += mmToPx(3.6f, dpi)

        val totalPaint = TextRenderUtils.paint(sizePx = mmToPx(3.2f, dpi).toFloat(), bold = true, align = TextRenderUtils.Align.LEFT)
        canvas.drawText("TOTAL: ${data.total}", marginPx, y, totalPaint)
        y += mmToPx(4.5f, dpi)

        if (data.note.isNotBlank()) {
            val notePaint = TextRenderUtils.paint(sizePx = mmToPx(2.4f, dpi).toFloat(), align = TextRenderUtils.Align.LEFT)
            TextRenderUtils.drawWrapped(
                canvas = canvas,
                text = data.note,
                x = marginPx,
                top = y - mmToPx(2.4f, dpi),
                maxWidthPx = contentWidthPx,
                paint = notePaint,
            )
        }
    }
}
