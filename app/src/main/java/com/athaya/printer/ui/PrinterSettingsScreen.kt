package com.athaya.printer.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.athaya.printer.printer.BluetoothPermissions
import com.athaya.printer.printer.BluetoothPrinter
import com.athaya.printer.printer.DiscoveredPrinter
import com.athaya.printer.printer.PrinterConnectionState
import com.athaya.printer.renderer.TestPrintBitmap
import com.athaya.printer.settings.PrinterProfileStore
import kotlinx.coroutines.launch

/**
 * Printer connection + PrinterProfile selection screen.
 *
 * Phase 5: real Bluetooth Classic (RFCOMM) wiring against BluetoothPrinter.
 * Only paired ("bonded") devices are listed — see BluetoothPrinter's KDoc
 * for why. USB/LAN selection stays a stub (PrinterProfile.connectionType
 * still exists for them, but no UI/implementation yet).
 */
@Composable
fun PrinterSettingsScreen(onBack: () -> Unit) {
    var activeProfile by remember { mutableStateOf(PrinterProfileStore.getActiveProfile()) }

    val context = LocalContext.current
    val bluetoothPrinter = remember { BluetoothPrinter.create(context.applicationContext) }
    val connectionState by bluetoothPrinter.connectionState.collectAsState()
    val scope = rememberCoroutineScope()

    var pairedDevices by remember { mutableStateOf<List<DiscoveredPrinter>>(emptyList()) }
    var selectedDevice by remember { mutableStateOf<DiscoveredPrinter?>(null) }
    var statusMessage by remember { mutableStateOf<String?>(null) }

    fun loadPairedDevices() {
        scope.launch {
            statusMessage = null
            try {
                pairedDevices = bluetoothPrinter.scan()
                if (pairedDevices.isEmpty()) {
                    statusMessage = "Tidak ada printer yang sudah di-pair. Pair printer dari pengaturan Bluetooth Android terlebih dahulu."
                }
            } catch (e: Exception) {
                statusMessage = e.message ?: "Gagal memuat daftar printer."
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            loadPairedDevices()
        } else {
            statusMessage = "Izin Bluetooth ditolak. Aplikasi tidak bisa menghubungkan printer tanpa izin ini."
        }
    }

    fun requestPermissionThenLoadDevices() {
        if (BluetoothPermissions.hasRequiredPermissions(context)) {
            loadPairedDevices()
        } else {
            val required = BluetoothPermissions.requiredRuntimePermissions()
            if (required.isEmpty()) {
                loadPairedDevices()
            } else {
                permissionLauncher.launch(required.first())
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        Text("Pengaturan Printer")
        Text("Profile aktif: ${activeProfile.printerName} (${activeProfile.paperWidthMm} mm, ${activeProfile.dpi} dpi, ${activeProfile.printableDots} dots)")

        PrinterProfileStore.availableProfiles().forEach { profile ->
            Button(
                onClick = {
                    PrinterProfileStore.setActiveProfile(profile)
                    activeProfile = profile
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Gunakan: ${profile.printerName}")
            }
        }

        Text("Koneksi: ${activeProfile.connectionType}")

        Text("Status: ${connectionState.toDisplayText()}")

        Button(onClick = { requestPermissionThenLoadDevices() }, modifier = Modifier.fillMaxWidth()) {
            Text("Muat daftar printer (paired)")
        }

        // Plain forEach, not LazyColumn: this Column is already inside an
        // outer Column.verticalScroll() for the whole settings page, and a
        // LazyColumn nested inside a scrollable Column gets infinite height
        // constraints from its parent, which crashes at runtime (see
        // "PrinterSettingsScreen crash" bug report). The paired-device list
        // is always small, so a lazy list buys nothing here anyway.
        Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
            pairedDevices.forEach { device ->
                Row {
                    RadioButton(selected = selectedDevice == device, onClick = { selectedDevice = device })
                    Text("${device.name} (${device.address})", modifier = Modifier.padding(top = 12.dp))
                }
            }
        }

        Button(
            onClick = {
                val device = selectedDevice
                if (device == null) {
                    statusMessage = "Pilih printer terlebih dahulu."
                } else {
                    scope.launch {
                        statusMessage = null
                        val result = bluetoothPrinter.connect(device)
                        if (result is PrinterConnectionState.Error) {
                            statusMessage = result.message
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Connect")
        }

        Button(
            onClick = { scope.launch { bluetoothPrinter.disconnect() } },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Disconnect")
        }

        Button(
            onClick = {
                scope.launch {
                    statusMessage = null
                    try {
                        val testBitmap = TestPrintBitmap.build(activeProfile)
                        bluetoothPrinter.printBitmap(testBitmap, activeProfile)
                        bluetoothPrinter.feed(3)
                        bluetoothPrinter.cut(activeProfile)
                        statusMessage = "Test print terkirim."
                    } catch (e: Exception) {
                        statusMessage = e.message ?: "Test print gagal."
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Test Print")
        }

        statusMessage?.let { Text(it) }

        Button(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text("Kembali")
        }
    }
}

private fun PrinterConnectionState.toDisplayText(): String = when (this) {
    is PrinterConnectionState.Disconnected -> "Terputus"
    is PrinterConnectionState.Connecting -> "Menghubungkan..."
    is PrinterConnectionState.Connected -> "Terhubung ke $deviceName"
    is PrinterConnectionState.Printing -> "Mencetak ke $deviceName..."
    is PrinterConnectionState.Error -> "Error: $message"
}
