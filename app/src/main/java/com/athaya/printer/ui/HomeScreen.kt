package com.athaya.printer.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun HomeScreen(
    onCetakNota: () -> Unit,
    onCetakAlamat: () -> Unit,
    onPengaturanPrinter: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(text = "Athaya Printer")
        Spacer()
        Button(onClick = onCetakNota, modifier = Modifier.fillMaxWidth()) {
            Text("CETAK NOTA")
        }
        Spacer()
        Button(onClick = onCetakAlamat, modifier = Modifier.fillMaxWidth()) {
            Text("CETAK ALAMAT")
        }
        Spacer()
        Button(onClick = onPengaturanPrinter, modifier = Modifier.fillMaxWidth()) {
            Text("PENGATURAN PRINTER")
        }
    }
}

@Composable
private fun Spacer() {
    androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(8.dp))
}
