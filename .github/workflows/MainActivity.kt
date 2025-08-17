
package com.vtm.invoice

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.vtm.invoice.data.InvoiceData
import com.vtm.invoice.data.InvoiceItem
import com.vtm.invoice.data.Template
import com.vtm.invoice.pdf.PdfGenerator
import java.io.File

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { App() }
    }
}

@Composable
fun App() {
    var language by remember { mutableStateOf("en") }
    var template by remember { mutableStateOf(Template.MODERN) }
    var invoiceNo by remember { mutableStateOf(loadNextInvoiceNo()) }
    var customer by remember { mutableStateOf(getString(R.string.customer_sample)) }
    val footer = getString(R.string.footer_contact)

    val items = remember {
        mutableStateListOf(
            InvoiceItem("Stage Decoration", 1, 5000),
            InvoiceItem("Sound System Setup", 1, 3000),
            InvoiceItem("Lighting Arrangement", 1, 2000),
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(title = {
                Text(
                    when (language) {
                        "kn" -> getString(R.string.business_name_kn)
                        "mr" -> getString(R.string.business_name_mr)
                        else -> getString(R.string.business_name_en)
                    }, fontWeight = FontWeight.Bold
                )
            })
        }
    ) { padding ->
        Column(Modifier.padding(padding).padding(16.dp)) {
            // Invoice meta
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(getString(R.string.invoice_no) + ": ")
                Spacer(Modifier.width(8.dp))
                OutlinedTextField(value = invoiceNo, onValueChange = { invoiceNo = it }, modifier = Modifier.weight(1f))
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(getString(R.string.bill_to) + ": ")
                Spacer(Modifier.width(8.dp))
                OutlinedTextField(value = customer, onValueChange = { customer = it }, modifier = Modifier.weight(1f))
            }
            Spacer(Modifier.height(8.dp))

            // Language & template
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(getString(R.string.language) + ": ")
                Spacer(Modifier.width(8.dp))
                FilterChip(selected = language=="en", onClick = { language="en" }, label = { Text(getString(R.string.english)) })
                Spacer(Modifier.width(8.dp))
                FilterChip(selected = language=="kn", onClick = { language="kn" }, label = { Text(getString(R.string.kannada)) })
                Spacer(Modifier.width(8.dp))
                FilterChip(selected = language=="mr", onClick = { language="mr" }, label = { Text(getString(R.string.marathi)) })
                Spacer(Modifier.width(16.dp))
                FilterChip(selected = template==Template.MODERN, onClick = { template=Template.MODERN }, label = { Text(getString(R.string.modern)) })
                Spacer(Modifier.width(8.dp))
                FilterChip(selected = template==Template.CLASSIC, onClick = { template=Template.CLASSIC }, label = { Text(getString(R.string.classic)) })
            }

            Spacer(Modifier.height(12.dp))
            Text(getString(R.string.item), style = MaterialTheme.typography.titleMedium)

            // Items list
            LazyColumn(modifier = Modifier.weight(1f, false)) {
                itemsIndexed(items) { index, it ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(value = it.name, onValueChange = { v -> items[index] = it.copy(name = v) }, label = { Text(getString(R.string.item)) }, modifier = Modifier.weight(1.6f))
                    }
                    Row(Modifier.fillMaxWidth()) {
                        OutlinedTextField(value = it.qty.toString(), onValueChange = { v ->
                            val q = v.toIntOrNull() ?: 0; items[index] = it.copy(qty = q)
                        }, label = { Text(getString(R.string.qty)) }, modifier = Modifier.weight(1f))
                        Spacer(Modifier.width(8.dp))
                        OutlinedTextField(value = it.price.toString(), onValueChange = { v ->
                            val p = v.toIntOrNull() ?: 0; items[index] = it.copy(price = p)
                        }, label = { Text(getString(R.string.price)) }, modifier = Modifier.weight(1f))
                        Spacer(Modifier.width(8.dp))
                        Text("₹${it.total}", modifier = Modifier.align(Alignment.CenterVertically))
                        Spacer(Modifier.width(8.dp))
                        TextButton(onClick = { items.removeAt(index) }) { Text(getString(R.string.delete)) }
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }

            Row {
                Button(onClick = { items.add(InvoiceItem("",1,0)) }) { Text(getString(R.string.add_item)) }
                Spacer(Modifier.width(12.dp))
                val ctx = androidx.compose.ui.platform.LocalContext.current
                Button(onClick = {
                    val file = com.vtm.invoice.pdf.PdfGenerator(ctx).generate(
                        InvoiceData(invoiceNo, customer, items.toList(),
                            footer, language, template),
                        null
                    )
                    // Auto-increment invoice number
                    invoiceNo = incrementAndSave(invoiceNo)
                    // Share
                    val uri = androidx.core.content.FileProvider.getUriForFile(ctx, "com.vtm.invoice.fileprovider", file)
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "application/pdf"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    ctx.startActivity(Intent.createChooser(intent, getString(R.string.share_pdf)))
                }) { Text(getString(R.string.share_pdf)) }
            }
        }
    }
}

// Simple persistence using SharedPreferences
fun ComponentActivity.loadNextInvoiceNo(): String {
    val sp = getSharedPreferences("vtm", MODE_PRIVATE)
    val last = sp.getString("inv", "VTM-2025-001")!!
    return last
}

fun ComponentActivity.incrementAndSave(current: String): String {
    // Expect format VTM-YYYY-NNN
    val parts = current.split("-")
    var n = 1
    var prefix = "VTM-2025"
    if (parts.size >= 3) {
        prefix = parts[0] + "-" + parts[1]
        n = parts[2].toIntOrNull() ?: 1
    }
    n += 1
    val next = "%s-%03d".format(prefix, n)
    val sp = getSharedPreferences("vtm", MODE_PRIVATE)
    sp.edit().putString("inv", next).apply()
    return next
}
