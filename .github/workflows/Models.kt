
package com.vtm.invoice.data

data class InvoiceItem(
    var name: String = "",
    var qty: Int = 1,
    var price: Int = 0
) { val total: Int get() = qty * price }

enum class Template { MODERN, CLASSIC }

data class InvoiceData(
    val invoiceNo: String,
    val customer: String,
    val items: List<InvoiceItem>,
    val footer: String,
    val language: String = "en",
    val template: Template = Template.MODERN
) {
    val grandTotal: Int get() = items.sumOf { it.total }
}
