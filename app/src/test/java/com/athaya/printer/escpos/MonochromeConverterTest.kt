package com.athaya.printer.escpos

import android.graphics.Bitmap
import android.graphics.Color
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class MonochromeConverterTest {

    @Test
    fun `pure black pixel converts to black`() {
        val bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
        bitmap.setPixel(0, 0, Color.BLACK)

        val mono = MonochromeConverter.convert(bitmap)

        assertTrue(mono.blackPixels[0])
    }

    @Test
    fun `pure white pixel converts to white`() {
        val bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
        bitmap.setPixel(0, 0, Color.WHITE)

        val mono = MonochromeConverter.convert(bitmap)

        assertFalse(mono.blackPixels[0])
    }

    @Test
    fun `threshold is configurable and changes the black-white boundary`() {
        val bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
        // Mid-gray: luminance = 128.
        bitmap.setPixel(0, 0, Color.rgb(128, 128, 128))

        val blackUnderLowThreshold = MonochromeConverter.convert(bitmap, threshold = 64).blackPixels[0]
        val blackUnderHighThreshold = MonochromeConverter.convert(bitmap, threshold = 200).blackPixels[0]

        assertFalse("gray(128) should stay white when threshold=64", blackUnderLowThreshold)
        assertTrue("gray(128) should become black when threshold=200", blackUnderHighThreshold)
    }

    @Test
    fun `converted dimensions match the source bitmap`() {
        val bitmap = Bitmap.createBitmap(12, 7, Bitmap.Config.ARGB_8888)
        val mono = MonochromeConverter.convert(bitmap)

        org.junit.Assert.assertEquals(12, mono.width)
        org.junit.Assert.assertEquals(7, mono.height)
        org.junit.Assert.assertEquals(12 * 7, mono.blackPixels.size)
    }
}
