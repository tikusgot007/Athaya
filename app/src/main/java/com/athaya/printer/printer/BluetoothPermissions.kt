package com.athaya.printer.printer

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

/**
 * Centralizes which Bluetooth runtime permission this app actually needs,
 * per Android version — and nothing more.
 *
 * On Android 12+ (API 31, Build.VERSION_CODES.S) connecting to a bonded
 * device and reading its name requires the runtime-granted
 * BLUETOOTH_CONNECT permission. We deliberately do NOT request
 * BLUETOOTH_SCAN or ACCESS_FINE_LOCATION: this app only ever connects to
 * already-paired devices (see BluetoothPrinter KDoc), it never starts
 * active discovery, so those permissions would be unused and must not be
 * requested.
 *
 * On pre-12 devices, BLUETOOTH and BLUETOOTH_ADMIN are normal
 * (manifest-only, install-time) permissions — no runtime prompt is needed
 * or requested there.
 */
object BluetoothPermissions {

    fun requiredRuntimePermissions(): Array<String> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            emptyArray()
        }

    fun hasRequiredPermissions(context: Context): Boolean =
        requiredRuntimePermissions().all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
}
