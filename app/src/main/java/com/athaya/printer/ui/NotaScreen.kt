package com.athaya.printer.ui

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
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
import com.athaya.printer.model.NotaData
import com.athaya.printer.model.NotaItem
import com.athaya.printer.renderer.NotaRenderer
import com.athaya.printer.renderer.NotaTemplateSpec
import com.athaya.printer.settings.PrinterProfileStore

/**
 * Nota input screen. Fields are held purely in Compose remember state
 * (in-memory only, no persistence). Preview renders a real Bitmap via
 * NotaRenderer — the exact same renderer that will later feed the
 * monochrome/ESC-POS pipeline — so preview and print never drift apart.
 * The CETAK button stays a placeholder until Phase 5 wires PrinterManager.
 */
@Composable
fun NotaScreen(onBack: () -> Unit) {
    var storeName by remember { mutableStateOf("Toko Athaya") }
    var invoiceNumber by remember { mutableStateOf("") }
    var date by remember { mutableStateOf("") }
    var customerName by remember { mutableStateOf("") }
    var discount by remember { mutableStateOf("0") }
    var note by remember { mutableStateOf("") }

    var items by remember { mutableStateOf(listOf<NotaItem>()) }
    var itemName by remember { mutableStateOf("") }
    var itemQty by remember { mutableStateOf("1") }
    var itemPrice by remember { mutableStateOf("0") }

    var useTestTemplate by remember { mutableStateOf(true) }
    var previewBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var previewError by remember { mutableStateOf<String?>(null) }

    val notaRenderer = remember { NotaRenderer() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        Text("Cetak Nota")

        OutlinedTextField(value = storeName, onValueChange = { storeName = it }, label = { Text("Nama Toko") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = invoiceNumber, onValueChange = { invoiceNumber = it }, label = { Text("Nomor Nota") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = date, onValueChange = { date = it }, label = { Text("Tanggal") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = customerName, onValueChange = { customerName = it }, label = { Text("Nama Customer") }, modifier = Modifier.fillMaxWidth())

        Text("Tambah Item")
        OutlinedTextField(value = itemName, onValueChange = { itemName = it }, label = { Text("Nama Item") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = itemQty, onValueChange = { itemQty = it }, label = { Text("Qty") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = itemPrice, onValueChange = { itemPrice = it }, label = { Text("Harga") }, modifier = Modifier.fillMaxWidth())
        Button(onClick = {
            val qty = itemQty.toIntOrNull() ?: 1
            val price = itemPrice.toLongOrNull() ?: 0L
            if (itemName.isNotBlank()) {
                items = items + NotaItem(name = itemName, quantity = qty, price = price)
                itemName = ""
                itemQty = "1"
                itemPrice = "0"
            }
        }, modifier = Modifier.fillMaxWidth()) {
            Text("Tambah Item")
        }

        // Plain forEach, not LazyColumn: this Column is already inside an
        // outer Column.verticalScroll() for the whole nota form, and a
        // LazyColumn nested inside a scrollable Column gets infinite height
        // constraints from its parent, which crashes at composition time
        // (same root cause as the PrinterSettingsScreen crash fix). The
        // item list here is always small, so a lazy list buys nothing.
        Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
            items.forEach { item ->
                Text("${item.name} x${item.quantity} = ${item.subtotal}")
            }
        }

        OutlinedTextField(value = discount, onValueChange = { discount = it }, label = { Text("Diskon") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = note, onValueChange = { note = it }, label = { Text("Catatan") }, modifier = Modifier.fillMaxWidth())

        val subtotal = items.sumOf { it.subtotal }
        val total = (subtotal - (discount.toLongOrNull() ?: 0L)).coerceAtLeast(0L)
        Text("Subtotal: $subtotal")
        Text("Total: $total")

        Text("Template rendering:")
        Row(checked = useTestTemplate, label = "Test 58mm (rotate -> 56x100mm)") { useTestTemplate = true }
        Row(checked = !useTestTemplate, label = "Production (rotate -> 60x100mm)") { useTestTemplate = false }

        Button(
            onClick = {
                val data = NotaData(
                    storeName = storeName,
                    invoiceNumber = invoiceNumber,
                    date = date,
                    customerName = customerName,
                    items = items,
                    discount = discount.toLongOrNull() ?: 0L,
                    note = note,
                )
                val template = if (useTestTemplate) NotaTemplateSpec.TEST_58MM else NotaTemplateSpec.PRODUCTION
                val profile = PrinterProfileStore.getActiveProfile()
                previewError = null
                previewBitmap = try {
                    notaRenderer.renderWithTemplate(data, profile, template)
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
            Image(bitmap = bmp.asImageBitmap(), contentDescription = "Preview nota")
        }

        Button(onClick = { /* Print: implemented in Phase 5 with PrinterManager */ }, modifier = Modifier.fillMaxWidth()) {
            Text("CETAK")
        }
        Button(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text("Kembali")
        }
    }
}

@Composable
private fun Row(checked: Boolean, label: String, onClick: () -> Unit) {
    androidx.compose.foundation.layout.Row {
        RadioButton(selected = checked, onClick = onClick)
        Text(label, modifier = Modifier.padding(top = 12.dp))
    }
}
