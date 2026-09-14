package com.athaya.printer.escpos

/** Text/image alignment as understood by ESC a (alignment) command. */
enum class PrintAlign(val code: Byte) {
    LEFT(0),
    CENTER(1),
    RIGHT(2),
}

/**
 * Raw ESC/POS byte sequences. Kept deliberately dumb — no state, no
 * decisions about *when* to send what (that belongs to EscPosEncoder) —
 * just the well-known command bytes so they exist in exactly one place.
 */
object EscPosCommands {

    /** ESC @ — resets the printer to its default state. */
    val INITIALIZE: ByteArray = byteArrayOf(0x1B, 0x40)

    /** ESC a n — set justification for what follows. */
    fun align(align: PrintAlign): ByteArray = byteArrayOf(0x1B, 0x61, align.code)

    /** ESC d n — feed n lines. */
    fun feed(lines: Int): ByteArray {
        val n = lines.coerceIn(0, 255)
        return byteArrayOf(0x1B, 0x64, n.toByte())
    }

    /** GS V 0 — full cut. Only ever send this when PrinterProfile.supportsCut is true. */
    val CUT_FULL: ByteArray = byteArrayOf(0x1D, 0x56, 0x00)

    /**
     * GS v 0 m xL xH yL yH — raster bit image header.
     * [widthBytes] is the row width in BYTES (i.e. pixel width / 8, rounded up),
     * not pixels. [heightDots] is the image height in pixels/dots.
     * m = 0 (normal, no scaling) is the only mode this app uses.
     */
    fun rasterImageHeader(widthBytes: Int, heightDots: Int): ByteArray {
        require(widthBytes in 1..0xFFFF) { "widthBytes out of range: $widthBytes" }
        require(heightDots in 1..0xFFFF) { "heightDots out of range: $heightDots" }
        val xL = (widthBytes and 0xFF).toByte()
        val xH = ((widthBytes shr 8) and 0xFF).toByte()
        val yL = (heightDots and 0xFF).toByte()
        val yH = ((heightDots shr 8) and 0xFF).toByte()
        return byteArrayOf(0x1D, 0x76, 0x30, 0x00, xL, xH, yL, yH)
    }
}
