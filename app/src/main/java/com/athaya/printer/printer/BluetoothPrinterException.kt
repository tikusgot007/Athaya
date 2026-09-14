package com.athaya.printer.printer

/**
 * User-facing Bluetooth transport errors. Every subclass carries a message
 * meant to be shown directly in the UI (Indonesian, matching the rest of
 * the app's UI copy) — technical detail (the original IOException/
 * SecurityException message) can still be logged separately by the caller,
 * but must never be what the user sees as the primary message.
 */
sealed class BluetoothPrinterException(message: String) : Exception(message) {

    class BluetoothUnavailable :
        BluetoothPrinterException("Bluetooth tidak tersedia di perangkat ini.")

    class BluetoothDisabled :
        BluetoothPrinterException("Bluetooth tidak aktif. Aktifkan Bluetooth terlebih dahulu.")

    class PermissionDenied :
        BluetoothPrinterException("Izin Bluetooth belum diberikan. Berikan izin untuk menghubungkan printer.")

    class DeviceNotFound(address: String) :
        BluetoothPrinterException("Printer dengan alamat $address tidak ditemukan di daftar perangkat yang sudah di-pair.")

    class NotConnected :
        BluetoothPrinterException("Printer belum terhubung.")

    class ConnectionFailed(reason: String) :
        BluetoothPrinterException("Gagal terhubung ke printer: $reason")

    class WriteFailed(reason: String) :
        BluetoothPrinterException("Gagal mengirim data ke printer: $reason")
}
