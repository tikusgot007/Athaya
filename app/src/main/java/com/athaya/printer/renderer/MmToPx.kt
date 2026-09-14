package com.athaya.printer.renderer

/**
 * Converts a physical size in millimeters to pixels/dots for a given DPI.
 * This is the single source of truth for mm -> px conversion; nothing in
 * the renderer or template code should hard-code pixel sizes directly.
 */
fun mmToPx(mm: Float, dpi: Int): Int {
    val inches = mm / 25.4f
    return (inches * dpi).toInt()
}
