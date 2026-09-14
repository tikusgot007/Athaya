package com.athaya.printer.renderer

/**
 * Physical size (in millimeters) of the nota design BEFORE the mandatory
 * 90-degree rotation. Production is fixed at 100x60mm per spec; the test
 * variant exists ONLY to validate the rendering/rotation pipeline against
 * the 58mm printer available for development, and must never replace or
 * silently scale the production template.
 *
 * After rotate 90°, the printed result is heightMm x widthMm — e.g.
 * PRODUCTION (100x60) becomes 60x100 on paper, TEST_58MM (100x46) becomes
 * 46x100 on paper.
 *
 * IMPORTANT: the test template's rotated width must fit
 * PrinterProfile.test58mm().printableDots (384 dots @ 203dpi ≈ 48mm
 * printable area — the real printable width of a 58mm-paper printer, which
 * is narrower than the 58mm paper itself because of non-printable margins
 * on either side of the print head). 46mm was chosen with a small safety
 * margin under that 48mm ceiling; do NOT raise this back to something
 * close to 56/58mm without checking mmToPx(rotatedWidthMm, 203) against
 * PrinterProfile.test58mm().printableDots first — an earlier version of
 * this template used 56mm, which is a valid paper width but renders to
 * 447px, exceeding printableDots=384 and making the test template
 * un-printable on the very printer it exists to test.
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

        /** Rotates to 46x100mm on paper — fits PrinterProfile.test58mm().printableDots (384 dots ≈ 48mm). */
        val TEST_58MM = NotaTemplateSpec(widthMm = 100f, heightMm = 46f, label = "Test 58mm (rotated 46x100mm)")
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

        /**
         * 46mm, not 58mm/56mm: must fit PrinterProfile.test58mm().printableDots
         * (384 dots @ 203dpi ≈ 48mm printable area) — see NotaTemplateSpec's
         * TEST_58MM KDoc above for why this can't just use the paper width.
         */
        val TEST_58MM = AddressTemplateSpec(widthMm = 46f, minHeightMm = 100f, label = "Test 58mm 46x100mm")
    }
}
