package com.athaya.printer.printer

/**
 * Thrown whenever a bitmap's printable width would exceed
 * PrinterProfile.printableDots — checked both right after rendering
 * (NotaRenderer/AddressRenderer) and again right before ESC/POS encoding
 * (EscPosEncoder), since the encoder must not trust that every bitmap it
 * is handed necessarily came from our own renderers.
 *
 * We deliberately fail loudly here instead of silently auto-scaling the
 * bitmap down to fit — scaling a production template to a smaller printer
 * changes its real-world physical size, which this app must never do
 * implicitly. Callers should pick a template/profile combination that
 * already fits (e.g. NotaTemplateSpec.TEST_58MM) instead.
 */
class PrintableWidthExceededException(
    val actualWidthPx: Int,
    val printableDots: Int,
    printerName: String,
) : IllegalStateException(
    "Rendered width ${actualWidthPx}px exceeds printableDots=$printableDots for printer " +
        "\"$printerName\". Use a template sized for this printer instead of scaling automatically."
)
