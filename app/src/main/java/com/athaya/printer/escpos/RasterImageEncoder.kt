package com.athaya.printer.escpos

/**
 * Produces a complete raster-image command (header + packed pixel data)
 * for one [MonochromeBitmap]. Exists as its own interface so EscPosEncoder
 * is not hard-wired to a single raster command: GS v 0 is the only
 * implementation for now, but a future alternative — e.g. GS ( L for
 * larger images — can be added as another implementation of this
 * interface without changing EscPosEncoder's public API or the bit
 * packing logic it shares.
 */
interface RasterImageEncoder {
    fun encode(mono: MonochromeBitmap): ByteArray
}

/**
 * GS v 0 raster bit image command: the standard ESC/POS raster mode this
 * app uses today. Delegates its header/packing to EscPosEncoder so both
 * stay in one place.
 */
object GsV0RasterImageEncoder : RasterImageEncoder {
    override fun encode(mono: MonochromeBitmap): ByteArray = EscPosEncoder.buildRasterCommand(mono)
}
