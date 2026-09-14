package com.athaya.printer.escpos

import android.graphics.Bitmap
import com.athaya.printer.printer.PrintableWidthExceededException
import com.athaya.printer.printer.PrinterProfile
import java.io.ByteArrayOutputStream

/**
 * Turns a rendered Bitmap into a ready-to-send ESC/POS byte stream:
 *
 *   Bitmap -> monochrome -> bit-packed raster -> GS v 0 command -> feed -> cut
 *
 * This is the ONLY place that talks ESC/POS. PrinterManager implementations
 * (BluetoothPrinter etc., Phase 5) call [encode] and write the resulting
 * bytes to their transport — they must not build ESC/POS commands
 * themselves, and NotaRenderer/AddressRenderer must never import from this
 * package at all.
 */
object EscPosEncoder {

    /**
     * Encodes [bitmap] for [profile]. Throws [PrintableWidthExceededException]
     * if bitmap.width exceeds profile.printableDots — this is checked again
     * here (renderers already check it) because the encoder cannot assume
     * every bitmap it is ever handed came from our own renderers.
     *
     * A cut command is appended only when [cut] is true AND
     * profile.supportsCut is true; otherwise it is silently omitted rather
     * than sent blindly.
     */
    fun encode(
        bitmap: Bitmap,
        profile: PrinterProfile,
        threshold: Int = MonochromeConverter.DEFAULT_THRESHOLD,
        align: PrintAlign = PrintAlign.CENTER,
        feedLines: Int = 3,
        cut: Boolean = true,
    ): ByteArray {
        if (bitmap.width > profile.printableDots) {
            throw PrintableWidthExceededException(
                actualWidthPx = bitmap.width,
                printableDots = profile.printableDots,
                printerName = profile.printerName,
            )
        }

        val mono = MonochromeConverter.convert(bitmap, threshold)
        return encodeMonochrome(mono, profile, align, feedLines, cut)
    }

    /**
     * Same as [encode] but starting from an already-converted
     * [MonochromeBitmap] — split out so bit-packing/command assembly can be
     * unit-tested with a small, deterministic, hand-built MonochromeBitmap
     * without needing a real android.graphics.Bitmap.
     */
    fun encodeMonochrome(
        mono: MonochromeBitmap,
        profile: PrinterProfile,
        align: PrintAlign = PrintAlign.CENTER,
        feedLines: Int = 3,
        cut: Boolean = true,
    ): ByteArray {
        if (mono.width > profile.printableDots) {
            throw PrintableWidthExceededException(
                actualWidthPx = mono.width,
                printableDots = profile.printableDots,
                printerName = profile.printerName,
            )
        }

        val out = ByteArrayOutputStream()
        out.write(EscPosCommands.INITIALIZE)
        out.write(EscPosCommands.align(align))
        out.write(buildRasterCommand(mono))
        out.write(EscPosCommands.feed(feedLines))
        if (cut && profile.supportsCut) {
            out.write(EscPosCommands.CUT_FULL)
        }
        return out.toByteArray()
    }

    /** GS v 0 header + packed raster data for [mono], with no other commands around it. */
    fun buildRasterCommand(mono: MonochromeBitmap): ByteArray {
        val widthBytes = (mono.width + 7) / 8
        val header = EscPosCommands.rasterImageHeader(widthBytes, mono.height)
        return header + packBits(mono)
    }

    /**
     * Packs [mono] into ESC/POS raster row data: 8 horizontal pixels per
     * byte, bit 7 = leftmost pixel, bit 0 = rightmost pixel within that
     * byte. If width is not a multiple of 8, the trailing bits of the last
     * byte in each row are padded with WHITE (0) — the physical image size
     * itself is never cropped or altered to make it byte-aligned.
     */
    fun packBits(mono: MonochromeBitmap): ByteArray {
        val widthBytes = (mono.width + 7) / 8
        val out = ByteArray(widthBytes * mono.height)

        for (row in 0 until mono.height) {
            val rowOffset = row * mono.width
            val outRowOffset = row * widthBytes
            for (byteIndex in 0 until widthBytes) {
                var packed = 0
                for (bitInByte in 0 until 8) {
                    val x = byteIndex * 8 + bitInByte
                    val isBlack = x < mono.width && mono.blackPixels[rowOffset + x]
                    if (isBlack) {
                        packed = packed or (1 shl (7 - bitInByte))
                    }
                }
                out[outRowOffset + byteIndex] = packed.toByte()
            }
        }
        return out
    }
}
