package com.athaya.printer.printer

import android.graphics.Bitmap

/** Connection status reported by a PrinterManager implementation. */
sealed interface PrinterConnectionState {
    data object Disconnected : PrinterConnectionState
    data object Connecting : PrinterConnectionState
    data class Connected(val deviceName: String) : PrinterConnectionState
    data class Error(val message: String) : PrinterConnectionState
}

/** A printer discovered/available for the active connection type. */
data class DiscoveredPrinter(
    val name: String,
    val address: String,
)

/**
 * Transport-agnostic contract for sending print jobs to a thermal printer.
 *
 * Implementations (BluetoothPrinter, UsbPrinter, LanPrinter) own the
 * connection details only. They never contain rendering or layout logic —
 * they receive an already-rendered, already-rotated Bitmap straight from a
 * PrintRenderer. Internally, printBitmap() is expected to call
 * com.athaya.printer.escpos.EscPosEncoder.encode(bitmap, profile) to get the
 * monochrome/ESC-POS byte stream, then write those bytes to the transport —
 * renderers must never import escpos, and PrinterManager implementations
 * must never build ESC/POS commands by hand.
 */
interface PrinterManager {
    val connectionType: ConnectionType

    suspend fun scan(): List<DiscoveredPrinter>

    suspend fun connect(target: DiscoveredPrinter): PrinterConnectionState

    suspend fun disconnect()

    fun connectionState(): PrinterConnectionState

    /**
     * Print a raster bitmap. The bitmap must already respect the active
     * PrinterProfile.printableDots width and be pre-rotated/pre-processed
     * as required by the caller (renderer layer). This function is only
     * responsible for monochrome/ESC-POS encoding and transmission.
     */
    suspend fun printBitmap(bitmap: Bitmap, profile: PrinterProfile)

    suspend fun feed(lines: Int = 3)

    suspend fun cut()
}
