package com.athaya.printer.renderer

import android.graphics.Bitmap
import android.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Validates rotateBitmap90Clockwise() in isolation, with small deterministic
 * bitmaps, before it's trusted inside NotaRenderer. Bagian A of Phase 4.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class BitmapTransformTest {

    @Test
    fun `rotate swaps width and height`() {
        val source = Bitmap.createBitmap(100, 60, Bitmap.Config.ARGB_8888)
        val rotated = rotateBitmap90Clockwise(source)

        assertEquals(60, rotated.width)
        assertEquals(100, rotated.height)
    }

    @Test
    fun `rotate does not lose or invent pixels (no distortion)`() {
        val width = 10
        val height = 6
        val source = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        // Paint the left half black, right half white -- distinct, countable regions.
        for (y in 0 until height) {
            for (x in 0 until width) {
                source.setPixel(x, y, if (x < width / 2) Color.BLACK else Color.WHITE)
            }
        }
        val blackCountBefore = countPixels(source, Color.BLACK)
        val whiteCountBefore = countPixels(source, Color.WHITE)

        val rotated = rotateBitmap90Clockwise(source)

        assertEquals(width * height, rotated.width * rotated.height)
        assertEquals(blackCountBefore, countPixels(rotated, Color.BLACK))
        assertEquals(whiteCountBefore, countPixels(rotated, Color.WHITE))
    }

    @Test
    fun `rotating twice returns to original dimensions`() {
        val source = Bitmap.createBitmap(100, 56, Bitmap.Config.ARGB_8888)
        val rotatedTwice = rotateBitmap90Clockwise(rotateBitmap90Clockwise(source))

        assertEquals(source.width, rotatedTwice.width)
        assertEquals(source.height, rotatedTwice.height)
    }

    private fun countPixels(bitmap: Bitmap, color: Int): Int {
        var count = 0
        for (y in 0 until bitmap.height) {
            for (x in 0 until bitmap.width) {
                if (bitmap.getPixel(x, y) == color) count++
            }
        }
        return count
    }
}
