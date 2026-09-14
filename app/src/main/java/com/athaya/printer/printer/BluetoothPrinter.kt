package com.athaya.printer.printer

import android.content.Context
import android.graphics.Bitmap
import com.athaya.printer.escpos.EscPosCommands
import com.athaya.printer.escpos.EscPosEncoder
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Bluetooth Classic (RFCOMM/SPP) implementation of PrinterManager.
 *
 * MVP scope: only already-paired ("bonded") devices are listed — no active
 * discovery. Pairing a thermal printer is a one-time setup step done once
 * from Android's own Bluetooth settings, and bonded-only keeps this
 * implementation simple and reliable (no BroadcastReceiver/discovery
 * lifecycle to manage) which matches what the MVP actually needs. Active
 * discovery can be added later behind the same PrinterManager.scan()
 * contract without any caller-visible change.
 *
 * This class is ONLY a transport: it never builds ESC/POS bytes itself
 * (that's EscPosEncoder's job, called from printBitmap()) and it never
 * touches rendering/layout. All actual android.bluetooth calls go through
 * [transport], which is what makes this class unit-testable without real
 * Bluetooth hardware (see BluetoothPrinterTest, which uses a fake
 * transport) — production code should construct this via
 * [BluetoothPrinter.create], not the primary constructor directly.
 *
 * Every public suspend function runs on Dispatchers.IO and is serialized
 * through a single Mutex, so: (a) UI never blocks on Bluetooth I/O, and
 * (b) two quick taps on Print/Feed/Cut/Connect/Disconnect can never
 * interleave bytes on the same output stream or open a second socket.
 */
class BluetoothPrinter(private val transport: BluetoothTransport) : PrinterManager {

    override val connectionType: ConnectionType = ConnectionType.BLUETOOTH

    private val _connectionState = MutableStateFlow<PrinterConnectionState>(PrinterConnectionState.Disconnected)
    override val connectionState: StateFlow<PrinterConnectionState> = _connectionState.asStateFlow()

    private val ioMutex = Mutex()

    private var socketHandle: PrinterSocketHandle? = null

    override suspend fun scan(): List<DiscoveredPrinter> = withContext(Dispatchers.IO) {
        requireAdapterReady()
        transport.listBondedDevices().map { DiscoveredPrinter(name = it.name, address = it.address) }
    }

    override suspend fun connect(target: DiscoveredPrinter): PrinterConnectionState = withContext(Dispatchers.IO) {
        ioMutex.withLock {
            val current = _connectionState.value
            if (current is PrinterConnectionState.Connected) {
                // Already connected: never open a second socket. Caller
                // must disconnect() first to switch to a different device.
                return@withLock current
            }

            _connectionState.value = PrinterConnectionState.Connecting
            try {
                requireAdapterReady()
                val device = transport.findBondedDevice(target.address)
                    ?: throw BluetoothPrinterException.DeviceNotFound(target.address)

                closeSocketQuietly() // safety: never leave a stale socket open before opening a new one

                val handle = transport.openRfcommSocket(device)
                socketHandle = handle

                val connected = PrinterConnectionState.Connected(device.name)
                _connectionState.value = connected
                connected
            } catch (e: BluetoothPrinterException) {
                closeSocketQuietly()
                val error = PrinterConnectionState.Error(e.message ?: "Gagal terhubung ke printer")
                _connectionState.value = error
                error
            } catch (e: IOException) {
                closeSocketQuietly()
                val error = PrinterConnectionState.Error(
                    humanMessage(BluetoothPrinterException.ConnectionFailed(e.message ?: "kesalahan tidak diketahui")),
                )
                _connectionState.value = error
                error
            }
        }
    }

    override suspend fun disconnect() = withContext(Dispatchers.IO) {
        ioMutex.withLock {
            closeSocketQuietly()
            _connectionState.value = PrinterConnectionState.Disconnected
        }
    }

    override suspend fun printBitmap(bitmap: Bitmap, profile: PrinterProfile) = withContext(Dispatchers.IO) {
        ioMutex.withLock {
            val handle = socketHandle ?: throw BluetoothPrinterException.NotConnected()
            val deviceName = (_connectionState.value as? PrinterConnectionState.Connected)?.deviceName ?: profile.printerName
            _connectionState.value = PrinterConnectionState.Printing(deviceName)

            // Bitmap -> monochrome -> ESC/POS bytes happens ONLY in
            // EscPosEncoder. This function is not allowed to build/alter
            // ESC/POS commands itself; it just writes the resulting bytes.
            val bytes = EscPosEncoder.encode(bitmap, profile)

            try {
                handle.outputStream.write(bytes)
                handle.outputStream.flush()
                _connectionState.value = PrinterConnectionState.Connected(deviceName)
            } catch (e: IOException) {
                closeSocketQuietly()
                _connectionState.value = PrinterConnectionState.Error(
                    humanMessage(BluetoothPrinterException.WriteFailed(e.message ?: "koneksi terputus saat mencetak")),
                )
                throw BluetoothPrinterException.WriteFailed(e.message ?: "koneksi terputus saat mencetak")
            }
        }
    }

    override suspend fun feed(lines: Int) = withContext(Dispatchers.IO) {
        ioMutex.withLock {
            val handle = socketHandle ?: throw BluetoothPrinterException.NotConnected()
            writeOrFail(handle, EscPosCommands.feed(lines))
        }
    }

    override suspend fun cut(profile: PrinterProfile) = withContext(Dispatchers.IO) {
        ioMutex.withLock {
            if (!profile.supportsCut) {
                // Silent no-op by design: never send a cut command to a
                // printer that has no cutter.
                return@withLock
            }
            val handle = socketHandle ?: throw BluetoothPrinterException.NotConnected()
            writeOrFail(handle, EscPosCommands.CUT_FULL)
        }
    }

    /** Caller must already hold ioMutex. */
    private fun writeOrFail(handle: PrinterSocketHandle, bytes: ByteArray) {
        try {
            handle.outputStream.write(bytes)
            handle.outputStream.flush()
        } catch (e: IOException) {
            closeSocketQuietly()
            _connectionState.value = PrinterConnectionState.Error(
                humanMessage(BluetoothPrinterException.WriteFailed(e.message ?: "gagal mengirim data")),
            )
            throw BluetoothPrinterException.WriteFailed(e.message ?: "gagal mengirim data")
        }
    }

    private fun humanMessage(e: BluetoothPrinterException): String = e.message.orEmpty()

    private fun requireAdapterReady() {
        if (!transport.hasConnectPermission()) throw BluetoothPrinterException.PermissionDenied()
        if (!transport.isBluetoothSupported()) throw BluetoothPrinterException.BluetoothUnavailable()
        if (!transport.isBluetoothEnabled()) throw BluetoothPrinterException.BluetoothDisabled()
    }

    /** Never throws. Safe to call even when nothing is connected. */
    private fun closeSocketQuietly() {
        runCatching { socketHandle?.close() }
        socketHandle = null
    }

    companion object {
        fun create(context: Context): BluetoothPrinter = BluetoothPrinter(AndroidBluetoothTransport(context))
    }
}
