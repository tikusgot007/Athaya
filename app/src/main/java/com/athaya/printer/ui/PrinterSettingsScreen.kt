package com.athaya.printer.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.athaya.printer.printer.PrinterProfile
import com.athaya.printer.settings.PrinterProfileStore

/**
 * Printer connection + PrinterProfile selection screen.
 *
 * Phase 1: only profile selection (58mm test / 80mm production) so the
 * rest of the app can already depend on PrinterProfileStore. Bluetooth
 * scan/connect/test-print/status wiring against a real PrinterManager
 * implementation is added in Phase 5. USB/LAN selection is stubbed for now.
 */
@Composable
fun PrinterSettingsScreen(onBack: () -> Unit) {
    var activeProfile by remember { mutableStateOf(PrinterProfileStore.getActiveProfile()) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
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
        Text("Scan / Connect / Disconnect / Test Print: diimplementasikan pada Phase 5 (Bluetooth).")

        Button(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text("Kembali")
        }
    }
}
