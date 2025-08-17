
package com.vtm.invoice.pdf

import android.content.Context
import android.graphics.*
import android.graphics.pdf.PdfDocument
import com.vtm.invoice.R
import com.vtm.invoice.data.InvoiceData
import com.vtm.invoice.data.Template
import java.io.File
import java.io.FileOutputStream

class PdfGenerator(private val context: Context) {

    fun generate(data: InvoiceData, signature: Bitmap?): File {
        val doc = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 @ 72dpi
        val page = doc.startPage(pageInfo)
        val c = page.canvas

        val gold = Color.parseColor("#D4AF37")
        val black = Color.BLACK

        // Background white
        c.drawColor(Color.WHITE)

        // Watermark
        val wm = BitmapFactory.decodeResource(context.resources, R.drawable.logo)
        val wmScaled = Bitmap.createScaledBitmap(wm, 260, 260, true)
        val wmPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { alpha = 35 }
        c.drawBitmap(wmScaled, (595-260)/2f, (842-260)/2f, wmPaint)

        // Header
        val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = gold
            textSize = 20f
            typeface = Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD)
        }
        val businessEn = context.getString(R.string.business_name_en)
        val businessKn = context.getString(R.string.business_name_kn)
        val businessMr = context.getString(R.string.business_name_mr)
        val biz = when (data.language) {
            "kn" -> businessKn
            "mr" -> businessMr
            else -> businessEn
        }
        c.drawText(biz, 40f, 60f, headerPaint)

        // Labels by language
        fun s(id: Int) = context.getString(id)
        val tInvoiceNo = s(R.string.invoice_no)
        val tBillTo = s(R.string.bill_to)
        val tItem = s(R.string.item)
        val tQty = s(R.string.qty)
        val tPrice = s(R.string.price)
        val tTotal = s(R.string.total)
        val tGrand = s(R.string.grand_total)

        // Invoice meta
        val text = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 12f; color = black }
        val bold = Paint(text).apply { typeface = Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD) }
        c.drawText("$tInvoiceNo: ${data.invoiceNo}", 40f, 90f, text)
        c.drawText("$tBillTo: ${data.customer}", 40f, 110f, text)

        // Table header
        var y = 140f
        c.drawText(tItem, 40f, y, bold)
        c.drawText(tQty, 330f, y, bold)
        c.drawText(tPrice, 380f, y, bold)
        c.drawText(tTotal, 470f, y, bold)
        y += 12f
        c.drawLine(40f, y, 555f, y, text)
        y += 16f

        data.items.forEach {
            c.drawText(it.name, 40f, y, text)
            c.drawText(it.qty.toString(), 340f, y, text)
            c.drawText("₹${it.price}", 380f, y, text)
            c.drawText("₹${it.total}", 470f, y, text)
            y += 18f
        }

        y += 10f
        c.drawLine(320f, y, 555f, y, text)
        y += 18f
        c.drawText("$tGrand:", 380f, y, bold)
        c.drawText("₹${data.grandTotal}", 470f, y, bold)

        // Footer
        c.drawText(data.footer, 40f, 800f, text)

        // Signature
        if (signature != null) {
            val sc = Bitmap.createScaledBitmap(signature, 140, 40, true)
            c.drawBitmap(sc, 400f, 760f, null)
        }
        c.drawText(context.getString(R.string.signature), 400f, 820f, text)

        // Classic border
        if (data.template == Template.CLASSIC) {
            val border = Paint().apply { color = gold; style = Paint.Style.STROKE; strokeWidth = 3f }
            c.drawRect(20f, 20f, 575f, 822f, border)
        }

        doc.finishPage(page)
        val out = File(context.cacheDir, "VTM_${System.currentTimeMillis()}.pdf")
        doc.writeTo(FileOutputStream(out))
        doc.close()
        return out
    }
}
