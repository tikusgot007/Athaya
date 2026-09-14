package com.athaya.printer.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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

/**
 * Address label input screen. Same staged approach as NotaScreen: fields
 * only for now, AddressRenderer preview wired in Phase 2, printing in
 * Phase 5.
 */
@Composable
fun AddressScreen(onBack: () -> Unit) {
    var recipientName by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var postalCode by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Cetak Alamat")

        OutlinedTextField(value = recipientName, onValueChange = { recipientName = it }, label = { Text("Nama Penerima") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Nomor HP") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text("Alamat") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = city, onValueChange = { city = it }, label = { Text("Kota") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = postalCode, onValueChange = { postalCode = it }, label = { Text("Kode Pos") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = note, onValueChange = { note = it }, label = { Text("Catatan") }, modifier = Modifier.fillMaxWidth())

        Button(onClick = { /* Preview: implemented in Phase 2 with AddressRenderer */ }, modifier = Modifier.fillMaxWidth()) {
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
