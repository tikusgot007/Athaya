package com.athaya.printer.renderer

import android.graphics.Bitmap
import com.athaya.printer.printer.PrinterProfile

/**
 * Contract shared by every renderer (NotaRenderer, AddressRenderer, ...).
 *
 * A single render() implementation is used for BOTH the on-screen preview
 * and the bitmap that gets sent to the printer, so preview and print are
 * guaranteed to match. Implementations work in millimeters first and only
 * convert to pixels via mmToPx(profile.dpi) internally — they must never
 * hard-code a pixel size or a paper width.
 *
 * T is the input data type (e.g. NotaData, AddressData).
 */
interface PrintRenderer<T> {
    /**
     * Renders [data] against [profile], returning a bitmap already
     * clipped/laid out to respect profile.printableDots. Any
     * template-specific transforms (e.g. the nota's 90-degree rotation)
     * are applied inside the implementation, not by the caller.
     */
    fun render(data: T, profile: PrinterProfile): Bitmap
}
