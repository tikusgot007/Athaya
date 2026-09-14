package com.athaya.printer.printer

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import java.io.OutputStream
import java.util.UUID

/** Standard Serial Port Profile UUID used by virtually every Bluetooth Classic thermal printer. */
private val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

/**
 * Real android.bluetooth-backed implementation of BluetoothTransport.
 * Bluetooth Classic / RFCOMM only — BLE is intentionally not used, since
 * these thermal printers are SPP devices.
 *
 * This class is the one piece of Phase 5 that genuinely cannot be unit
 * tested in this environment (or any environment without a real Bluetooth
 * adapter + paired printer) — see BluetoothPrinter/README for the manual
 * hardware test plan.
 */
class AndroidBluetoothTransport(private val context: Context) : BluetoothTransport {

    private val adapter: BluetoothAdapter?
        get() = (context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter

    override fun hasConnectPermission(): Boolean = BluetoothPermissions.hasRequiredPermissions(context)

    override fun isBluetoothSupported(): Boolean = adapter != null

    override fun isBluetoothEnabled(): Boolean = adapter?.isEnabled == true

    override fun listBondedDevices(): List<BondedDeviceHandle> {
        val bt = adapter ?: return emptyList()
        val devices = try {
            bt.bondedDevices
        } catch (e: SecurityException) {
            throw BluetoothPrinterException.PermissionDenied()
        }
        return devices.orEmpty().map { it.toHandle() }
    }

    override fun findBondedDevice(address: String): BondedDeviceHandle? =
        listBondedDevices().find { it.address == address }

    override fun openRfcommSocket(device: BondedDeviceHandle): PrinterSocketHandle {
        val bt = adapter ?: throw BluetoothPrinterException.BluetoothUnavailable()
        val rawDevice: BluetoothDevice = try {
            bt.bondedDevices.orEmpty().find { it.address == device.address }
        } catch (e: SecurityException) {
            throw BluetoothPrinterException.PermissionDenied()
        } ?: throw BluetoothPrinterException.DeviceNotFound(device.address)

        val socket: BluetoothSocket = try {
            rawDevice.createRfcommSocketToServiceRecord(SPP_UUID)
        } catch (e: SecurityException) {
            throw BluetoothPrinterException.PermissionDenied()
        } catch (e: java.io.IOException) {
            throw BluetoothPrinterException.ConnectionFailed(e.message ?: "tidak dapat membuat socket")
        }

        // Cancelling discovery before connecting is standard SPP practice
        // (a discovery in progress can slow down/derail the connect
        // attempt). This is best-effort: we never START discovery
        // ourselves, so a permission failure here is not fatal.
        try {
            bt.cancelDiscovery()
        } catch (_: SecurityException) {
            // ignore -- see comment above
        }

        try {
            socket.connect()
        } catch (e: java.io.IOException) {
            runCatching { socket.close() }
            throw BluetoothPrinterException.ConnectionFailed(e.message ?: "tidak dapat membuka koneksi ke printer")
        }

        return object : PrinterSocketHandle {
            override val outputStream: OutputStream = socket.outputStream
            override fun close() {
                runCatching { socket.outputStream.flush() }
                runCatching { socket.close() }
            }
        }
    }

    private fun BluetoothDevice.toHandle(): BondedDeviceHandle {
        val deviceName = try {
            name
        } catch (e: SecurityException) {
            null
        }
        return BondedDeviceHandle(name = deviceName ?: address, address = address)
    }
}
