package com.athaya.printer.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.athaya.printer.model.NotaItem

/**
 * Nota input screen. Holds all fields purely in Compose remember state
 * (in-memory only, no persistence). Preview + actual bitmap rendering via
 * NotaRenderer, and the CETAK button wiring to PrinterManager, are added
 * in later phases (Phase 2 and Phase 5) per the staged MVP plan.
 */
@Composable
fun NotaScreen(onBack: () -> Unit) {
    var storeName by remember { mutableStateOf("") }
    var invoiceNumber by remember { mutableStateOf("") }
    var date by remember { mutableStateOf("") }
    var customerName by remember { mutableStateOf("") }
    var discount by remember { mutableStateOf("0") }
    var note by remember { mutableStateOf("") }

    var items by remember { mutableStateOf(listOf<NotaItem>()) }
    var itemName by remember { mutableStateOf("") }
    var itemQty by remember { mutableStateOf("1") }
    var itemPrice by remember { mutableStateOf("0") }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
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

        LazyColumn(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
            items(items) { item ->
                Text("${item.name} x${item.quantity} = ${item.subtotal}")
            }
        }

        OutlinedTextField(value = discount, onValueChange = { discount = it }, label = { Text("Diskon") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = note, onValueChange = { note = it }, label = { Text("Catatan") }, modifier = Modifier.fillMaxWidth())

        val subtotal = items.sumOf { it.subtotal }
        val total = (subtotal - (discount.toLongOrNull() ?: 0L)).coerceAtLeast(0L)
        Text("Subtotal: $subtotal")
        Text("Total: $total")

        Button(onClick = { /* Preview: implemented in Phase 2 with NotaRenderer */ }, modifier = Modifier.fillMaxWidth()) {
            Text("Preview")
        }
        Button(onClick = { /* Print: implemented in Phase 5 with PrinterManager */ }, modifier = Modifier.fillMaxWidth()) {
            Text("CETAK")
        }
        Button(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text("Kembali")
        }
    }
}
