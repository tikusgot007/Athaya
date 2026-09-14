package com.athaya.printer.escpos

import android.graphics.Bitmap

/**
 * Result of converting a color/grayscale Bitmap to 1-bit black/white.
 * [blackPixels] is row-major (index = y * width + x), true = black (ink),
 * false = white (no ink). Kept as a plain Kotlin structure (no Android
 * Bitmap) so bit-packing logic downstream can be unit-tested without an
 * Android runtime.
 */
data class MonochromeBitmap(
    val width: Int,
    val height: Int,
    val blackPixels: BooleanArray,
) {
    init {
        require(blackPixels.size == width * height) {
            "blackPixels size ${blackPixels.size} must equal width*height (${width * height})"
        }
    }

    // BooleanArray has no structural equals/hashCode by default; provide one
    // so tests can compare MonochromeBitmap instances with assertEquals.
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is MonochromeBitmap) return false
        return width == other.width && height == other.height && blackPixels.contentEquals(other.blackPixels)
    }

    override fun hashCode(): Int {
        var result = width
        result = 31 * result + height
        result = 31 * result + blackPixels.contentHashCode()
        return result
    }
}

/**
 * Converts a Bitmap to 1-bit monochrome using a simple luminance threshold.
 * No dithering (explicitly out of scope for now) — a pixel is either fully
 * black or fully white, decided by comparing its luminance against a
 * configurable threshold so this can be tuned later without touching the
 * encoder or renderers.
 */
object MonochromeConverter {

    /** 0 (all black) .. 255 (all white). Pixels with luminance below this become black. */
    const val DEFAULT_THRESHOLD = 128

    fun convert(bitmap: Bitmap, threshold: Int = DEFAULT_THRESHOLD): MonochromeBitmap {
        require(threshold in 0..255) { "threshold must be in 0..255, was $threshold" }

        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        val black = BooleanArray(width * height)
        for (i in pixels.indices) {
            black[i] = isBlack(pixels[i], threshold)
        }
        return MonochromeBitmap(width, height, black)
    }

    /**
     * A fully (or mostly) transparent source pixel is treated as white —
     * there's no ink to print for an area the renderer never drew on.
     */
    private fun isBlack(argbPixel: Int, threshold: Int): Boolean {
        val alpha = (argbPixel ushr 24) and 0xFF
        if (alpha < 128) return false

        val r = (argbPixel ushr 16) and 0xFF
        val g = (argbPixel ushr 8) and 0xFF
        val b = argbPixel and 0xFF

        // Standard perceptual luminance weights.
        val luminance = 0.299 * r + 0.587 * g + 0.114 * b
        return luminance < threshold
    }
}
