package com.athaya.printer.model

/**
 * A single line item on a nota (receipt/invoice).
 */
data class NotaItem(
    val name: String,
    val quantity: Int,
    val price: Long,
) {
    val subtotal: Long
        get() = quantity.toLong() * price
}

/**
 * All data required to render a nota. Held only in memory for the
 * duration of input -> preview -> print; nothing is persisted.
 */
data class NotaData(
    val storeName: String,
    val invoiceNumber: String,
    val date: String,
    val customerName: String,
    val items: List<NotaItem>,
    val discount: Long = 0L,
    val note: String = "",
) {
    val subtotal: Long
        get() = items.sumOf { it.subtotal }

    val total: Long
        get() = (subtotal - discount).coerceAtLeast(0L)
}
