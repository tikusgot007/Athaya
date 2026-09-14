package com.athaya.printer.printer

import java.io.OutputStream

/**
 * A bonded (paired) Bluetooth Classic device, reduced to the two fields
 * BluetoothPrinter actually needs. Kept separate from android.bluetooth.
 * BluetoothDevice so BluetoothPrinter's own logic never touches an Android
 * framework class directly and can be unit-tested with a fake transport.
 */
data class BondedDeviceHandle(val name: String, val address: String)

/** An open RFCOMM connection to a printer. */
interface PrinterSocketHandle {
    val outputStream: OutputStream

    /** Must never throw — swallow/log I/O errors on close, same as closeQuietly semantics. */
    fun close()
}

/**
 * Thin seam between BluetoothPrinter and the real android.bluetooth APIs.
 *
 * BluetoothPrinter contains ALL of the connection-lifecycle, error-mapping,
 * and serialization logic that Phase 5 needs to get right; this interface
 * exists purely so that logic can be exercised in a plain JVM/Robolectric
 * unit test against a fake implementation, instead of requiring a real
 * Bluetooth adapter and a physical printer. AndroidBluetoothTransport is
 * the only real implementation and is NOT covered by unit tests — see the
 * README's Phase 5 section for what still needs a hardware test pass.
 */
interface BluetoothTransport {
    fun hasConnectPermission(): Boolean
    fun isBluetoothSupported(): Boolean
    fun isBluetoothEnabled(): Boolean

    /** Bonded/paired devices only — see BluetoothPrinter's KDoc on why active discovery is out of scope for the MVP. */
    fun listBondedDevices(): List<BondedDeviceHandle>

    fun findBondedDevice(address: String): BondedDeviceHandle?

    /** Opens a blocking RFCOMM connection to [device] using the standard SPP UUID. Throws IOException/SecurityException on failure. */
    fun openRfcommSocket(device: BondedDeviceHandle): PrinterSocketHandle
}
