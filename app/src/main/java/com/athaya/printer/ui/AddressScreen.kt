package com.athaya.printer.ui

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import com.athaya.printer.model.AddressData
import com.athaya.printer.renderer.AddressRenderer
import com.athaya.printer.renderer.AddressTemplateSpec
import com.athaya.printer.renderer.mmToPx
import com.athaya.printer.settings.PrinterProfileStore

/**
 * Address label input screen. Same approach as NotaScreen: Preview renders
 * a real Bitmap via AddressRenderer (the same renderer used for printing
 * later), CETAK stays a placeholder until Phase 5.
 */
@Composable
fun AddressScreen(onBack: () -> Unit) {
    var recipientName by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var postalCode by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }

    var useTestTemplate by remember { mutableStateOf(true) }
    var previewBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var previewError by remember { mutableStateOf<String?>(null) }

    val addressRenderer = remember { AddressRenderer() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        Text("Cetak Alamat")

        OutlinedTextField(value = recipientName, onValueChange = { recipientName = it }, label = { Text("Nama Penerima") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Nomor HP") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text("Alamat") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = city, onValueChange = { city = it }, label = { Text("Kota") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = postalCode, onValueChange = { postalCode = it }, label = { Text("Kode Pos") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = note, onValueChange = { note = it }, label = { Text("Catatan") }, modifier = Modifier.fillMaxWidth())

        Text("Template rendering:")
        Row {
            RadioButton(selected = useTestTemplate, onClick = { useTestTemplate = true })
            Text("Test 58mm (46x100mm)", modifier = Modifier.padding(top = 12.dp))
        }
        Row {
            RadioButton(selected = !useTestTemplate, onClick = { useTestTemplate = false })
            Text("Production (80x100mm)", modifier = Modifier.padding(top = 12.dp))
        }

        // Same informational check as NotaScreen: re-evaluated every
        // recomposition so a mismatch between the selected template and the
        // currently active printer profile is visible before Preview is
        // even tapped. Never auto-scales anything -- renderWithTemplate()
        // below still does the real, authoritative check.
        val activeProfile = PrinterProfileStore.getActiveProfile()
        val selectedTemplate = if (useTestTemplate) AddressTemplateSpec.TEST_58MM else AddressTemplateSpec.PRODUCTION
        val requiredWidthPx = mmToPx(selectedTemplate.widthMm, activeProfile.dpi)
        if (requiredWidthPx > activeProfile.printableDots) {
            Text(
                "⚠ Template \"${selectedTemplate.label}\" butuh ${requiredWidthPx}px, " +
                    "melebihi printableDots=${activeProfile.printableDots} pada profile aktif " +
                    "\"${activeProfile.printerName}\". Preview/Cetak akan gagal kecuali Anda ganti " +
                    "profile printer di Pengaturan Printer, atau pilih template yang sesuai.",
            )
        }

        Button(
            onClick = {
                val data = AddressData(
                    recipientName = recipientName,
                    phone = phone,
                    address = address,
                    city = city,
                    postalCode = postalCode,
                    note = note,
                )
                previewError = null
                previewBitmap = try {
                    addressRenderer.renderWithTemplate(data, activeProfile, selectedTemplate)
                } catch (e: IllegalStateException) {
                    previewError = e.message
                    null
                }
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Preview")
        }

        previewError?.let { Text("Error: $it") }
        previewBitmap?.let { bmp ->
            Text("Preview (${bmp.width}x${bmp.height}px):")
            Image(bitmap = bmp.asImageBitmap(), contentDescription = "Preview alamat")
        }

        Button(onClick = { /* Print: implemented in Phase 5 with PrinterManager */ }, modifier = Modifier.fillMaxWidth()) {
            Text("CETAK")
        }
        Button(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text("Kembali")
        }
    }
}
