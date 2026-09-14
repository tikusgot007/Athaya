package com.athaya.printer.printer

import android.graphics.Bitmap
import kotlinx.coroutines.flow.StateFlow

/**
 * Connection status reported by a PrinterManager implementation. Exposed as
 * a StateFlow (see PrinterManager.connectionState) rather than an Android
 * callback (BluetoothAdapter listener, etc.) so UI and tests can observe it
 * the same way regardless of transport.
 */
sealed interface PrinterConnectionState {
    data object Disconnected : PrinterConnectionState
    data object Connecting : PrinterConnectionState
    data class Connected(val deviceName: String) : PrinterConnectionState
    data class Printing(val deviceName: String) : PrinterConnectionState
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
 *
 * Only one print/feed/cut/connect/disconnect operation may be in flight at
 * a time per instance; implementations are responsible for serializing
 * their own I/O (e.g. with a Mutex) so two quick taps on "Print" can never
 * interleave bytes on the same output stream.
 */
interface PrinterManager {
    val connectionType: ConnectionType

    /** Current connection state, observable by UI (collectAsState()) and tests alike. */
    val connectionState: StateFlow<PrinterConnectionState>

    /**
     * Lists printers available to connect to. For BluetoothPrinter (MVP)
     * this returns already-paired/bonded devices — see BluetoothPrinter's
     * KDoc for why active discovery is out of scope for now.
     */
    suspend fun scan(): List<DiscoveredPrinter>

    /**
     * Connects to [target]. If already connected, does NOT open a second
     * socket — it returns the current Connected state as-is; callers that
     * want to switch devices must disconnect() first. Never throws for an
     * expected failure (permission denied, device not found, I/O error);
     * those are reported as PrinterConnectionState.Error with a
     * human-readable message instead.
     */
    suspend fun connect(target: DiscoveredPrinter): PrinterConnectionState

    suspend fun disconnect()

    /**
     * Print a raster bitmap. The bitmap must already respect the active
     * PrinterProfile.printableDots width and be pre-rotated/pre-processed
     * as required by the caller (renderer layer). This function is only
     * responsible for monochrome/ESC-POS encoding and transmission.
     *
     * Throws when not connected or when the transport write fails — see
     * BluetoothPrinterException for the specific, user-readable failure
     * reasons implementations should use.
     */
    suspend fun printBitmap(bitmap: Bitmap, profile: PrinterProfile)

    suspend fun feed(lines: Int = 3)

    /**
     * Sends a cut command ONLY when [profile].supportsCut is true;
     * otherwise this is a silent no-op. [profile] is required here (rather
     * than assumed from the last printBitmap call) so cut() always reflects
     * the printer actually in use, not stale state.
     */
    suspend fun cut(profile: PrinterProfile)
}
