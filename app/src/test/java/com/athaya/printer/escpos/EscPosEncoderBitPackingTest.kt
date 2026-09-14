package com.athaya.printer.escpos

import com.athaya.printer.printer.ConnectionType
import com.athaya.printer.printer.PrintableWidthExceededException
import com.athaya.printer.printer.PrinterProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

/**
 * Pure JVM tests for bit-packing and command assembly. MonochromeBitmap is
 * hand-built here (no android.graphics.Bitmap involved), which keeps these
 * deterministic and runnable without an Android runtime/Robolectric.
 */
class EscPosEncoderBitPackingTest {

    private fun testProfile(printableDots: Int = 384) = PrinterProfile(
        printerName = "Unit Test Printer",
        paperWidthMm = 58f,
        dpi = 203,
        printableDots = printableDots,
        connectionType = ConnectionType.BLUETOOTH,
        supportsCut = true,
    )

    @Test
    fun `width multiple of 8 packs into exactly one byte per row`() {
        // 8x1, all black.
        val mono = MonochromeBitmap(width = 8, height = 1, blackPixels = BooleanArray(8) { true })
        val packed = EscPosEncoder.packBits(mono)

        assertEquals(1, packed.size)
        assertEquals(0xFF.toByte(), packed[0])
    }

    @Test
    fun `width not a multiple of 8 pads the trailing bits with white`() {
        // 5x1: first 5 pixels black, byte must still be emitted (1 byte covers up to 8 px).
        val mono = MonochromeBitmap(width = 5, height = 1, blackPixels = booleanArrayOf(true, true, true, true, true))
        val packed = EscPosEncoder.packBits(mono)

        assertEquals(1, packed.size)
        // bit7..bit3 = 1 (5 black pixels), bit2..bit0 = 0 (padding) = 0b11111000 = 0xF8
        assertEquals(0xF8.toByte(), packed[0])
    }

    @Test
    fun `black pixel produces bit 1 and white pixel produces bit 0`() {
        // 8x1: alternating black/white starting black -> 0b10101010 = 0xAA
        val pattern = booleanArrayOf(true, false, true, false, true, false, true, false)
        val mono = MonochromeBitmap(width = 8, height = 1, blackPixels = pattern)
        val packed = EscPosEncoder.packBits(mono)

        assertEquals(0xAA.toByte(), packed[0])
    }

    @Test
    fun `bit 7 is the leftmost pixel and bit 0 is the rightmost pixel in a byte`() {
        // Only the leftmost pixel (x=0) is black -> only bit7 set -> 0b10000000 = 0x80
        val leftOnly = MonochromeBitmap(width = 8, height = 1, blackPixels = booleanArrayOf(true, false, false, false, false, false, false, false))
        assertEquals(0x80.toByte(), EscPosEncoder.packBits(leftOnly)[0])

        // Only the rightmost pixel (x=7) is black -> only bit0 set -> 0b00000001 = 0x01
        val rightOnly = MonochromeBitmap(width = 8, height = 1, blackPixels = booleanArrayOf(false, false, false, false, false, false, false, true))
        assertEquals(0x01.toByte(), EscPosEncoder.packBits(rightOnly)[0])
    }

    @Test
    fun `multi-row bitmap packs each row independently with correct widthBytes stride`() {
        // 9px wide -> 2 bytes/row. Row0 all black, row1 all white.
        val width = 9
        val row0 = BooleanArray(width) { true }
        val row1 = BooleanArray(width) { false }
        val mono = MonochromeBitmap(width = width, height = 2, blackPixels = row0 + row1)

        val packed = EscPosEncoder.packBits(mono)
        val widthBytes = 2
        assertEquals(widthBytes * 2, packed.size)

        // Row 0: 9 black pixels -> byte0 = 0xFF (8 bits), byte1 = leftmost bit of remaining 1 px = 0x80.
        assertEquals(0xFF.toByte(), packed[0])
        assertEquals(0x80.toByte(), packed[1])
        // Row 1: all white -> both bytes 0x00.
        assertEquals(0x00.toByte(), packed[2])
        assertEquals(0x00.toByte(), packed[3])
    }

    @Test
    fun `raster command header encodes widthBytes and height correctly`() {
        // 20px wide -> ceil(20/8) = 3 bytes/row; height = 4.
        val width = 20
        val height = 4
        val mono = MonochromeBitmap(width = width, height = height, blackPixels = BooleanArray(width * height))
        val command = EscPosEncoder.buildRasterCommand(mono)

        // GS v 0 m xL xH yL yH <data...>
        assertEquals(0x1D.toByte(), command[0])
        assertEquals(0x76.toByte(), command[1])
        assertEquals(0x30.toByte(), command[2])
        assertEquals(0x00.toByte(), command[3]) // m = normal
        val widthBytes = (command[4].toInt() and 0xFF) or ((command[5].toInt() and 0xFF) shl 8)
        val heightDots = (command[6].toInt() and 0xFF) or ((command[7].toInt() and 0xFF) shl 8)

        assertEquals(3, widthBytes)
        assertEquals(height, heightDots)
        assertEquals(8 + widthBytes * height, command.size)
    }

    @Test
    fun `encodeMonochrome throws instead of scaling when width exceeds printableDots`() {
        val mono = MonochromeBitmap(width = 400, height = 10, blackPixels = BooleanArray(400 * 10))
        val profile = testProfile(printableDots = 384)

        val ex = assertThrows(PrintableWidthExceededException::class.java) {
            EscPosEncoder.encodeMonochrome(mono, profile)
        }
        assertEquals(400, ex.actualWidthPx)
        assertEquals(384, ex.printableDots)
    }

    @Test
    fun `encodeMonochrome omits cut command when profile does not support it`() {
        val mono = MonochromeBitmap(width = 8, height = 1, blackPixels = BooleanArray(8))
        val noCutProfile = testProfile().copy(supportsCut = false)

        val bytes = EscPosEncoder.encodeMonochrome(mono, noCutProfile, cut = true)

        assertFalse(containsSubsequence(bytes, EscPosCommands.CUT_FULL))
    }

    @Test
    fun `encodeMonochrome includes cut command when profile supports it and cut is requested`() {
        val mono = MonochromeBitmap(width = 8, height = 1, blackPixels = BooleanArray(8))
        val bytes = EscPosEncoder.encodeMonochrome(mono, testProfile(), cut = true)

        assertTrue(containsSubsequence(bytes, EscPosCommands.CUT_FULL))
    }

    private fun containsSubsequence(haystack: ByteArray, needle: ByteArray): Boolean {
        if (needle.isEmpty() || needle.size > haystack.size) return false
        outer@ for (start in 0..haystack.size - needle.size) {
            for (i in needle.indices) {
                if (haystack[start + i] != needle[i]) continue@outer
            }
            return true
        }
        return false
    }

    private fun assertTrue(condition: Boolean) = org.junit.Assert.assertTrue(condition)
    private fun assertFalse(condition: Boolean) = org.junit.Assert.assertFalse(condition)
}
