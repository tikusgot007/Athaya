package com.athaya.printer.renderer

/**
 * Physical size (in millimeters) of the nota design BEFORE the mandatory
 * 90-degree rotation. Production is fixed at 100x60mm per spec; the test
 * variant exists ONLY to validate the rendering/rotation pipeline against
 * the 58mm printer available for development, and must never replace or
 * silently scale the production template.
 *
 * After rotate 90°, the printed result is heightMm x widthMm — e.g.
 * PRODUCTION (100x60) becomes 60x100 on paper, TEST_58MM (100x56) becomes
 * 56x100 on paper so it fits an <=58mm printable width.
 */
data class NotaTemplateSpec(
    val widthMm: Float,
    val heightMm: Float,
    val label: String,
) {
    /** Physical size AFTER the 90-degree rotation, as printed. */
    val rotatedWidthMm: Float get() = heightMm
    val rotatedHeightMm: Float get() = widthMm

    companion object {
        val PRODUCTION = NotaTemplateSpec(widthMm = 100f, heightMm = 60f, label = "Production 100x60mm")

        /** Rotates to 56x100mm on paper, fitting a 58mm-wide test printer. */
        val TEST_58MM = NotaTemplateSpec(widthMm = 100f, heightMm = 56f, label = "Test 58mm (rotated 56x100mm)")
    }
}

/**
 * Physical size (in millimeters) of an address label. Unlike nota, address
 * labels are not rotated: continuous sticker stock means the actual printed
 * height can grow with content, so heightMm here is a starting/minimum
 * value, not a hard cap.
 */
data class AddressTemplateSpec(
    val widthMm: Float,
    val minHeightMm: Float,
    val label: String,
) {
    companion object {
        val PRODUCTION = AddressTemplateSpec(widthMm = 80f, minHeightMm = 100f, label = "Production 80x100mm")
        val TEST_58MM = AddressTemplateSpec(widthMm = 56f, minHeightMm = 100f, label = "Test 58mm 56x100mm")
    }
}
