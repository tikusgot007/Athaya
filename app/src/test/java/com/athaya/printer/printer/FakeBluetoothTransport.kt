package com.athaya.printer.printer

import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.OutputStream

/**
 * In-memory BluetoothTransport used only by tests: lets BluetoothPrinter's
 * connection-lifecycle/error-mapping/serialization logic be exercised
 * without any real android.bluetooth API or physical printer. See
 * BluetoothTransport's KDoc for why this seam exists.
 */
class FakeBluetoothTransport(
    private var connectPermission: Boolean = true,
    private var supported: Boolean = true,
    private var enabled: Boolean = true,
    bondedDevices: List<BondedDeviceHandle> = emptyList(),
) : BluetoothTransport {

    private val devices = bondedDevices.toMutableList()

    /** Set to make the next openRfcommSocket() call throw this instead of succeeding. */
    var openSocketError: Throwable? = null

    /** Every socket handle ever opened, in order -- lets tests assert exactly one was opened for "double connect" scenarios. */
    val openedSockets = mutableListOf<FakePrinterSocketHandle>()

    fun setConnectPermission(granted: Boolean) {
        connectPermission = granted
    }

    fun setBluetoothSupported(value: Boolean) {
        supported = value
    }

    fun setBluetoothEnabled(value: Boolean) {
        enabled = value
    }

    override fun hasConnectPermission(): Boolean = connectPermission

    override fun isBluetoothSupported(): Boolean = supported

    override fun isBluetoothEnabled(): Boolean = enabled

    override fun listBondedDevices(): List<BondedDeviceHandle> = devices.toList()

    override fun findBondedDevice(address: String): BondedDeviceHandle? = devices.find { it.address == address }

    override fun openRfcommSocket(device: BondedDeviceHandle): PrinterSocketHandle {
        openSocketError?.let { throw it }
        val handle = FakePrinterSocketHandle()
        openedSockets += handle
        return handle
    }
}

/** In-memory PrinterSocketHandle: writes go to a ByteArrayOutputStream so tests can inspect exactly what bytes were sent. */
class FakePrinterSocketHandle : PrinterSocketHandle {
    val buffer = ByteArrayOutputStream()
    var closed = false
        private set

    /** Set to make every subsequent write on this handle throw this instead of succeeding. */
    var failWritesWith: IOException? = null

    override val outputStream: OutputStream = object : OutputStream() {
        override fun write(b: Int) {
            failWritesWith?.let { throw it }
            buffer.write(b)
        }

        override fun write(b: ByteArray, off: Int, len: Int) {
            failWritesWith?.let { throw it }
            buffer.write(b, off, len)
        }
    }

    override fun close() {
        closed = true
    }
}
