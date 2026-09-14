package com.athaya.printer.model

/**
 * All data required to render a shipping address label.
 * barcodeData / qrData are optional and left null when not used.
 */
data class AddressData(
    val recipientName: String,
    val phone: String,
    val address: String,
    val city: String,
    val postalCode: String,
    val note: String = "",
    val barcodeData: String? = null,
    val qrData: String? = null,
)
