package com.athaya.printer.renderer

/**
 * Thrown when a rendered bitmap's printable width would exceed
 * PrinterProfile.printableDots. We deliberately fail loudly here instead of
 * silently auto-scaling the bitmap down to fit — scaling a production
 * template to a smaller printer changes its real-world physical size,
 * which this app must never do implicitly. Callers should pick a template
 * sized for the active profile (e.g. NotaTemplateSpec.TEST_58MM) instead.
 */
class PrintableWidthExceededException(
    val actualWidthPx: Int,
    val printableDots: Int,
    printerName: String,
) : IllegalStateException(
    "Rendered width ${actualWidthPx}px exceeds printableDots=$printableDots for printer " +
        "\"$printerName\". Use a template sized for this printer instead of scaling automatically."
)
