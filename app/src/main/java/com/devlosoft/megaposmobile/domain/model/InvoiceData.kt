package com.devlosoft.megaposmobile.domain.model

data class InvoiceData(
    val totals: InvoiceTotals = InvoiceTotals(),
    val items: List<InvoiceItem> = emptyList()
)

data class InvoiceTotals(
    val total: Double = 0.0,
    val subTotal: Double = 0.0,
    val tax: Double = 0.0,
    val balanceDue: Double = 0.0,
    val totalItems: Double = 0.0,
    val totalSavings: Double = 0.0,
    val sponsors: Double = 0.0
)

data class InvoiceItem(
    val lineItemSequence: Int = 0,
    val itemId: String = "",
    val itemName: String = "",
    val discountPercentage: Double = 0.0,
    val unitPrice: Double = 0.0,
    val quantity: Double = 0.0,
    val total: Double = 0.0,
    val hasDiscount: Boolean = false,
    val isDeleted: Boolean = false
)
