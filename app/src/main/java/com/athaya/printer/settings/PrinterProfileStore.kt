package com.athaya.printer.settings

import com.athaya.printer.printer.PrinterProfile

/**
 * Holds the currently selected PrinterProfile in memory (no database).
 * A simple singleton object is enough for MVP: the profile only needs to
 * survive for the lifetime of the app process, selected once in the
 * "PENGATURAN PRINTER" screen and read by renderer/printer code elsewhere.
 *
 * Defaults to the 58 mm test profile since that's the printer available
 * for development right now. Switching to the Iware XS-80 production
 * printer later is just calling setActiveProfile(PrinterProfile.productionIwareXs80()).
 */
object PrinterProfileStore {
    private var activeProfile: PrinterProfile = PrinterProfile.test58mm()

    fun getActiveProfile(): PrinterProfile = activeProfile

    fun setActiveProfile(profile: PrinterProfile) {
        activeProfile = profile
    }

    fun availableProfiles(): List<PrinterProfile> = listOf(
        PrinterProfile.test58mm(),
        PrinterProfile.productionIwareXs80(),
    )
}
