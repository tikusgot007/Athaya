package com.athaya.printer.printer

/**
 * Describes the physical/electrical characteristics of a thermal printer.
 *
 * This is the ONLY place that knows about a specific printer's paper width.
 * Renderer and template code must never hard-code 58 or 80 (or any other)
 * millimeter value — they read it from a PrinterProfile instance instead.
 * Swapping from the 58 mm development printer to the Iware XS-80 production
 * printer is done by swapping the active PrinterProfile, not by changing
 * renderer/template logic.
 */
enum class ConnectionType {
    BLUETOOTH,
    USB,
    LAN,
}

data class PrinterProfile(
    /** Human readable name shown in the UI, e.g. "Iware XS-80" or "Test Printer 58mm". */
    val printerName: String,

    /** Physical paper width in millimeters. */
    val paperWidthMm: Float,

    /** Printer resolution in dots per inch. Never assume 203 permanently. */
    val dpi: Int,

    /**
     * Maximum printable width in dots (pixels) for this printer. Usually
     * slightly less than the full paperWidthMm converted to dots because of
     * physical, non-printable margins on either side of the print head.
     */
    val printableDots: Int,

    /** Active transport for this profile. */
    val connectionType: ConnectionType,

    /**
     * Whether this printer accepts an ESC/POS cut command (GS V). Some
     * cheap thermal printers/mechanisms have no cutter at all; sending a
     * cut command to one can be ignored or can error out depending on
     * firmware. EscPosEncoder must never emit a cut command when this is
     * false — it is not something to assume "probably fine" for a given
     * printer without confirming.
     */
    val supportsCut: Boolean,
) {
    companion object {
        /**
         * Development/testing profile: the 58 mm thermal printer currently
         * available. printableDots for a typical 58mm/203dpi head is ~384 dots;
         * kept slightly conservative here and still fully derived, never
         * hard-coded elsewhere.
         */
        fun test58mm(): PrinterProfile = PrinterProfile(
            printerName = "Test Printer 58mm",
            paperWidthMm = 58f,
            dpi = 203,
            printableDots = 384,
            connectionType = ConnectionType.BLUETOOTH,
            supportsCut = true,
        )

        /**
         * Production profile: Iware XS-80, 80 mm thermal printer.
         * Typical printable width for an 80mm/203dpi head is 576 dots.
         */
        fun productionIwareXs80(): PrinterProfile = PrinterProfile(
            printerName = "Iware XS-80",
            paperWidthMm = 80f,
            dpi = 203,
            printableDots = 576,
            connectionType = ConnectionType.BLUETOOTH,
            supportsCut = true,
        )
    }
}
