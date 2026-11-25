package com.example.util

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import android.print.pdf.PrintedPdfDocument
import com.example.data.model.Expense
import com.example.data.model.Payslip
import com.example.data.model.StoreSettings
import java.io.FileOutputStream
import java.io.IOException

data class ReportPdfSection(
    val title: String,
    val summaryPairs: List<Pair<String, String>> = emptyList(),
    val rows: List<String> = emptyList()
)

object PayslipPrinter {
    private tailrec fun Context.findActivity(): Activity? = when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }

    private fun sanitizeJobName(raw: String): String =
        raw.replace(Regex("[^A-Za-z0-9._-]"), "_").take(64).ifBlank { "Dokumen_AFI" }

    fun printPayslip(
        context: Context,
        payslip: Payslip,
        storeSettings: StoreSettings
    ) {
        val hostContext = context.findActivity() ?: context
        val printManager = hostContext.getSystemService(Context.PRINT_SERVICE) as? PrintManager ?: return
        val jobName = sanitizeJobName("Slip_Gaji_${payslip.employeeName}_${payslip.periodLabel}")
        printManager.print(
            jobName,
            PayslipPrintAdapter(hostContext, payslip, storeSettings),
            PrintAttributes.Builder()
                .setMediaSize(PrintAttributes.MediaSize.ISO_A5)
                .build()
        )
    }

    fun printExpense(
        context: Context,
        expense: Expense,
        storeSettings: StoreSettings
    ) {
        val hostContext = context.findActivity() ?: context
        val printManager = hostContext.getSystemService(Context.PRINT_SERVICE) as? PrintManager ?: return
        val jobName = sanitizeJobName("Pengeluaran_${expense.title}_${expense.id.take(6)}")
        printManager.print(
            jobName,
            ExpensePrintAdapter(hostContext, expense, storeSettings),
            PrintAttributes.Builder()
                .setMediaSize(PrintAttributes.MediaSize.ISO_A5)
                .build()
        )
    }

    // Ukuran kertas F4 / Folio Indonesia: 215 mm x 330 mm (8465 x 12992 mils)
    val MEDIA_SIZE_F4 = PrintAttributes.MediaSize(
        "F4_FOLIO",
        "F4 (215 x 330 mm)",
        8465,
        12992
    )

    fun printReportPdf(
        context: Context,
        storeSettings: StoreSettings,
        reportTitle: String,
        periodSubtitle: String,
        sections: List<ReportPdfSection>
    ) {
        val hostContext = context.findActivity() ?: context
        val printManager = hostContext.getSystemService(Context.PRINT_SERVICE) as? PrintManager ?: return
        val jobName = sanitizeJobName("Laporan_AFI_F4_${reportTitle}")
        printManager.print(
            jobName,
            ReportPdfPrintAdapter(hostContext, storeSettings, reportTitle, periodSubtitle, sections),
            PrintAttributes.Builder()
                .setMediaSize(MEDIA_SIZE_F4)
                .build()
        )
    }
}

private class PayslipPrintAdapter(
    private val context: Context,
    private val payslip: Payslip,
    private val storeSettings: StoreSettings
) : PrintDocumentAdapter() {

    private var pdfDocument: PrintedPdfDocument? = null

    override fun onLayout(
        oldAttributes: PrintAttributes?,
        newAttributes: PrintAttributes,
        cancellationSignal: CancellationSignal?,
        callback: LayoutResultCallback,
        extras: Bundle?
    ) {
        pdfDocument = PrintedPdfDocument(context, newAttributes)
        if (cancellationSignal?.isCanceled == true) {
            callback.onLayoutCancelled()
            return
        }
        val info = PrintDocumentInfo.Builder("slip_gaji_${payslip.id}.pdf")
            .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
            .setPageCount(1)
            .build()
        callback.onLayoutFinished(info, true)
    }

    override fun onWrite(
        pages: Array<out PageRange>,
        destination: ParcelFileDescriptor,
        cancellationSignal: CancellationSignal?,
        callback: WriteResultCallback
    ) {
        val doc = pdfDocument ?: run {
            callback.onWriteFailed("Dokumen PDF tidak tersedia")
            return
        }
        val page = doc.startPage(0)
        if (cancellationSignal?.isCanceled == true) {
            callback.onWriteCancelled()
            doc.close()
            pdfDocument = null
            return
        }
        drawPayslipPage(page)
        doc.finishPage(page)

        try {
            FileOutputStream(destination.fileDescriptor).use { out ->
                doc.writeTo(out)
            }
            callback.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
        } catch (e: IOException) {
            callback.onWriteFailed(e.message)
        } finally {
            doc.close()
            pdfDocument = null
        }
    }

    override fun onFinish() {
        pdfDocument?.close()
        pdfDocument = null
        super.onFinish()
    }

    private fun drawPayslipPage(page: PdfDocument.Page) {
        val canvas = page.canvas
        val width = page.info.pageWidth.toFloat()
        val margin = 28f
        var y = 40f

        val titlePaint = Paint().apply {
            color = Color.parseColor("#0F5132")
            textSize = 18f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val headerPaint = Paint().apply {
            color = Color.BLACK
            textSize = 13f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val bodyPaint = Paint().apply {
            color = Color.DKGRAY
            textSize = 11f
        }
        val boldBodyPaint = Paint().apply {
            color = Color.BLACK
            textSize = 11f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val linePaint = Paint().apply {
            color = Color.LTGRAY
            strokeWidth = 1f
        }

        // Store Header
        canvas.drawText(storeSettings.storeName.ifBlank { "AFI" }, margin, y, titlePaint)
        y += 16f
        if (storeSettings.address.isNotBlank()) {
            canvas.drawText(storeSettings.address, margin, y, bodyPaint)
            y += 14f
        }
        if (storeSettings.phone.isNotBlank()) {
            canvas.drawText("Telp: ${storeSettings.phone}", margin, y, bodyPaint)
            y += 14f
        }

        y += 6f
        canvas.drawLine(margin, y, width - margin, y, linePaint)
        y += 20f

        canvas.drawText("SLIP GAJI KARYAWAN PABRIK", margin, y, headerPaint)
        y += 18f
        canvas.drawText("Nama Karyawan : ${payslip.employeeName}", margin, y, bodyPaint)
        y += 15f
        canvas.drawText("Periode       : ${payslip.periodLabel}", margin, y, bodyPaint)
        y += 15f
        canvas.drawText("Tgl/Waktu Kerja: ${formatDateTime(payslip.workDateMillis.takeIf { it > 0L } ?: payslip.salaryDateMillis)}", margin, y, bodyPaint)
        y += 15f
        val paidSoFar = payslip.effectivePaidAmount()
        val remaining = payslip.remainingAmount()
        val statusText = when {
            payslip.paid -> "SUDAH DIBAYAR"
            paidSoFar > 0.0 -> "CICILAN"
            else -> "BELUM DIBAYAR"
        }
        canvas.drawText("Status        : $statusText", margin, y, boldBodyPaint)
        if (paidSoFar > 0.0 && payslip.paidAtMillis > 0L) {
            y += 15f
            canvas.drawText("Tgl Gajian Dibayar: ${formatDateTime(payslip.paidAtMillis)}", margin, y, bodyPaint)
        }

        y += 14f
        canvas.drawLine(margin, y, width - margin, y, linePaint)
        y += 16f

        canvas.drawText("Rincian Pekerjaan", margin, y, boldBodyPaint)
        y += 16f
        payslip.items.forEach { item ->
            val left = "${item.jobName} (${formatQty(item.quantity)} ${item.unit} x ${formatRupiah(item.ratePerUnit)})"
            val right = formatRupiah(item.subtotal)
            canvas.drawText(left, margin, y, bodyPaint)
            val rightW = bodyPaint.measureText(right)
            canvas.drawText(right, width - margin - rightW, y, bodyPaint)
            y += 15f
        }

        if (payslip.bonus > 0) {
            canvas.drawText("Bonus / Tambahan", margin, y, bodyPaint)
            val right = formatRupiah(payslip.bonus)
            canvas.drawText(right, width - margin - bodyPaint.measureText(right), y, bodyPaint)
            y += 15f
        }
        if (payslip.deduction > 0) {
            canvas.drawText("Potongan", margin, y, bodyPaint)
            val right = "-${formatRupiah(payslip.deduction)}"
            canvas.drawText(right, width - margin - bodyPaint.measureText(right), y, bodyPaint)
            y += 15f
        }

        y += 6f
        canvas.drawLine(margin, y, width - margin, y, linePaint)
        y += 18f

        canvas.drawText("TOTAL GAJI", margin, y, headerPaint)
        val totalText = formatRupiah(payslip.totalAmount)
        canvas.drawText(totalText, width - margin - headerPaint.measureText(totalText), y, headerPaint)

        if (!payslip.paid && paidSoFar > 0.0) {
            y += 16f
            canvas.drawText("Sudah Dibayar (Cicilan)", margin, y, bodyPaint)
            val paidText = formatRupiah(paidSoFar)
            canvas.drawText(paidText, width - margin - bodyPaint.measureText(paidText), y, bodyPaint)
            y += 16f
            canvas.drawText("SISA GAJI BELUM DIBAYAR", margin, y, boldBodyPaint)
            val remText = formatRupiah(remaining)
            canvas.drawText(remText, width - margin - boldBodyPaint.measureText(remText), y, boldBodyPaint)
        }

        if (payslip.notes.isNotBlank()) {
            y += 22f
            canvas.drawText("Catatan: ${payslip.notes}", margin, y, bodyPaint)
        }

        y += 38f
        canvas.drawText("Penerima,", margin, y, bodyPaint)
        val rightLabel = "Pimpinan / Admin,"
        canvas.drawText(rightLabel, width - margin - bodyPaint.measureText(rightLabel), y, bodyPaint)
        y += 38f
        canvas.drawText("(${payslip.employeeName})", margin, y, bodyPaint)
        val ownerLabel = "(${storeSettings.ownerName.ifBlank { "AFI" }})"
        canvas.drawText(ownerLabel, width - margin - bodyPaint.measureText(ownerLabel), y, bodyPaint)
    }
}

private class ExpensePrintAdapter(
    private val context: Context,
    private val expense: Expense,
    private val storeSettings: StoreSettings
) : PrintDocumentAdapter() {

    private var pdfDocument: PrintedPdfDocument? = null

    override fun onLayout(
        oldAttributes: PrintAttributes?,
        newAttributes: PrintAttributes,
        cancellationSignal: CancellationSignal?,
        callback: LayoutResultCallback,
        extras: Bundle?
    ) {
        pdfDocument = PrintedPdfDocument(context, newAttributes)
        if (cancellationSignal?.isCanceled == true) {
            callback.onLayoutCancelled()
            return
        }
        val info = PrintDocumentInfo.Builder("pengeluaran_${expense.id}.pdf")
            .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
            .setPageCount(1)
            .build()
        callback.onLayoutFinished(info, true)
    }

    override fun onWrite(
        pages: Array<out PageRange>,
        destination: ParcelFileDescriptor,
        cancellationSignal: CancellationSignal?,
        callback: WriteResultCallback
    ) {
        val doc = pdfDocument ?: run {
            callback.onWriteFailed("Dokumen PDF tidak tersedia")
            return
        }
        val page = doc.startPage(0)
        if (cancellationSignal?.isCanceled == true) {
            callback.onWriteCancelled()
            doc.close()
            pdfDocument = null
            return
        }
        drawExpensePage(page)
        doc.finishPage(page)

        try {
            FileOutputStream(destination.fileDescriptor).use { out ->
                doc.writeTo(out)
            }
            callback.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
        } catch (e: IOException) {
            callback.onWriteFailed(e.message)
        } finally {
            doc.close()
            pdfDocument = null
        }
    }

    override fun onFinish() {
        pdfDocument?.close()
        pdfDocument = null
        super.onFinish()
    }

    private fun drawExpensePage(page: PdfDocument.Page) {
        val canvas = page.canvas
        val width = page.info.pageWidth.toFloat()
        val margin = 28f
        var y = 40f

        val titlePaint = Paint().apply {
            color = Color.parseColor("#0F5132")
            textSize = 18f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val headerPaint = Paint().apply {
            color = Color.BLACK
            textSize = 13f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val bodyPaint = Paint().apply {
            color = Color.DKGRAY
            textSize = 11f
        }
        val linePaint = Paint().apply {
            color = Color.LTGRAY
            strokeWidth = 1f
        }

        canvas.drawText(storeSettings.storeName.ifBlank { "TOKO & PABRIK AFI" }, margin, y, titlePaint)
        y += 16f
        if (storeSettings.address.isNotBlank()) {
            canvas.drawText(storeSettings.address, margin, y, bodyPaint)
            y += 14f
        }
        if (storeSettings.phone.isNotBlank()) {
            canvas.drawText("Telp: ${storeSettings.phone}", margin, y, bodyPaint)
            y += 14f
        }

        y += 6f
        canvas.drawLine(margin, y, width - margin, y, linePaint)
        y += 20f

        canvas.drawText("BUKTI TRANSAKSI PENGELUARAN", margin, y, headerPaint)
        y += 18f
        canvas.drawText("Judul         : ${expense.title}", margin, y, bodyPaint)
        y += 15f
        canvas.drawText("Kategori      : ${expense.category}", margin, y, bodyPaint)
        y += 15f
        canvas.drawText("Sumber Kas    : ${expense.cashBoxName.ifBlank { "-" }}", margin, y, bodyPaint)
        y += 15f
        canvas.drawText("Waktu & Tgl   : ${formatDateTime(expense.transactionDateMillis)}", margin, y, bodyPaint)
        y += 15f

        y += 10f
        canvas.drawLine(margin, y, width - margin, y, linePaint)
        y += 18f

        canvas.drawText("TOTAL PENGELUARAN", margin, y, headerPaint)
        val totalText = formatRupiah(expense.amount)
        canvas.drawText(totalText, width - margin - headerPaint.measureText(totalText), y, headerPaint)

        if (expense.notes.isNotBlank()) {
            y += 22f
            canvas.drawText("Catatan: ${expense.notes}", margin, y, bodyPaint)
        }

        y += 40f
        val adminLabel = "Dibuat Oleh,"
        canvas.drawText(adminLabel, width - margin - bodyPaint.measureText(adminLabel), y, bodyPaint)
        y += 38f
        val ownerLabel = "(${storeSettings.ownerName.ifBlank { "Admin AFI" }})"
        canvas.drawText(ownerLabel, width - margin - bodyPaint.measureText(ownerLabel), y, bodyPaint)
    }
}

private sealed interface PdfLineItem {
    data class SectionTitle(val text: String) : PdfLineItem
    data class KeyValue(val label: String, val value: String) : PdfLineItem
    data class RowText(val text: String) : PdfLineItem
    data object Divider : PdfLineItem
    data object Spacer : PdfLineItem
}

private class ReportPdfPrintAdapter(
    private val context: Context,
    private val storeSettings: StoreSettings,
    private val reportTitle: String,
    private val periodSubtitle: String,
    private val sections: List<ReportPdfSection>
) : PrintDocumentAdapter() {

    private var pdfDocument: PrintedPdfDocument? = null
    private var paginatedPages: List<List<PdfLineItem>> = emptyList()

    private val sectionPaint = Paint().apply {
        color = Color.parseColor("#0F5132")
        textSize = 12f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    private val bodyPaint = Paint().apply {
        color = Color.DKGRAY
        textSize = 10f
    }
    private val boldPaint = Paint().apply {
        color = Color.BLACK
        textSize = 10f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }

    private fun wrapText(text: String, maxWidth: Float, paint: Paint): List<String> {
        if (paint.measureText(text) <= maxWidth) return listOf(text)
        val words = text.split(" ")
        val lines = mutableListOf<String>()
        var current = StringBuilder()
        for (word in words) {
            val candidate = if (current.isEmpty()) word else "$current $word"
            if (paint.measureText(candidate) <= maxWidth) {
                current = StringBuilder(candidate)
            } else {
                if (current.isNotEmpty()) lines.add(current.toString())
                current = StringBuilder(word)
            }
        }
        if (current.isNotEmpty()) lines.add(current.toString())
        return if (lines.isEmpty()) listOf(text) else lines
    }

    private fun buildPaginatedPages(pageWidth: Float, pageHeight: Float): List<List<PdfLineItem>> {
        val margin = 36f
        val maxTextWidth = (pageWidth - margin * 2).coerceAtLeast(180f)
        val allItems = mutableListOf<PdfLineItem>()

        sections.forEach { section ->
            allItems.add(PdfLineItem.SectionTitle(section.title))
            section.summaryPairs.forEach { (k, v) ->
                allItems.add(PdfLineItem.KeyValue(k, v))
            }
            section.rows.forEach { row ->
                wrapText(row, maxTextWidth, bodyPaint).forEach { wrapped ->
                    allItems.add(PdfLineItem.RowText(wrapped))
                }
            }
            allItems.add(PdfLineItem.Divider)
            allItems.add(PdfLineItem.Spacer)
        }

        val pages = mutableListOf<MutableList<PdfLineItem>>()
        var currentPage = mutableListOf<PdfLineItem>()
        var y = 132f
        val maxY = pageHeight - 48f

        fun itemHeight(item: PdfLineItem): Float = when (item) {
            is PdfLineItem.SectionTitle -> 20f
            is PdfLineItem.KeyValue -> 15f
            is PdfLineItem.RowText -> 14f
            PdfLineItem.Divider -> 10f
            PdfLineItem.Spacer -> 8f
        }

        allItems.forEach { item ->
            val h = itemHeight(item)
            if (y + h > maxY && currentPage.isNotEmpty()) {
                pages.add(currentPage)
                currentPage = mutableListOf()
                y = 66f
            }
            currentPage.add(item)
            y += h
        }
        if (currentPage.isNotEmpty() || pages.isEmpty()) {
            pages.add(currentPage)
        }
        return pages
    }

    override fun onLayout(
        oldAttributes: PrintAttributes?,
        newAttributes: PrintAttributes,
        cancellationSignal: CancellationSignal?,
        callback: LayoutResultCallback,
        extras: Bundle?
    ) {
        val doc = PrintedPdfDocument(context, newAttributes)
        pdfDocument = doc
        if (cancellationSignal?.isCanceled == true) {
            callback.onLayoutCancelled()
            return
        }
        val contentWidth = (newAttributes.mediaSize?.widthMils ?: 8465) * 72f / 1000f
        val contentHeight = (newAttributes.mediaSize?.heightMils ?: 12992) * 72f / 1000f
        paginatedPages = buildPaginatedPages(contentWidth, contentHeight)

        val info = PrintDocumentInfo.Builder("laporan_afi_f4.pdf")
            .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
            .setPageCount(paginatedPages.size.coerceAtLeast(1))
            .build()
        callback.onLayoutFinished(info, true)
    }

    override fun onWrite(
        pages: Array<out PageRange>,
        destination: ParcelFileDescriptor,
        cancellationSignal: CancellationSignal?,
        callback: WriteResultCallback
    ) {
        val doc = pdfDocument ?: run {
            callback.onWriteFailed("Dokumen PDF tidak tersedia")
            return
        }
        val titlePaint = Paint().apply {
            color = Color.parseColor("#0F5132")
            textSize = 16f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val subtitlePaint = Paint().apply {
            color = Color.DKGRAY
            textSize = 10f
        }
        val headerPaint = Paint().apply {
            color = Color.BLACK
            textSize = 13f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val linePaint = Paint().apply {
            color = Color.LTGRAY
            strokeWidth = 1f
        }

        val totalPages = paginatedPages.size.coerceAtLeast(1)
        for (pageIndex in 0 until totalPages) {
            if (cancellationSignal?.isCanceled == true) {
                callback.onWriteCancelled()
                doc.close()
                pdfDocument = null
                return
            }
            val page = doc.startPage(pageIndex)
            val canvas = page.canvas
            val width = page.info.pageWidth.toFloat()
            val height = page.info.pageHeight.toFloat()
            val margin = 36f
            var y = 40f

            if (pageIndex == 0) {
                canvas.drawText(storeSettings.storeName.ifBlank { "TOKO & PABRIK AFI" }, margin, y, titlePaint)
                y += 15f
                if (storeSettings.address.isNotBlank()) {
                    canvas.drawText(storeSettings.address, margin, y, subtitlePaint)
                    y += 13f
                }
                canvas.drawText(reportTitle, margin, y, headerPaint)
                y += 14f
                canvas.drawText("$periodSubtitle • Dicetak: ${formatDateTime(System.currentTimeMillis())}", margin, y, subtitlePaint)
                y += 10f
                canvas.drawLine(margin, y, width - margin, y, linePaint)
                y += 16f
            } else {
                canvas.drawText("$reportTitle (Hal. ${pageIndex + 1}/$totalPages)", margin, y, boldPaint)
                y += 10f
                canvas.drawLine(margin, y, width - margin, y, linePaint)
                y += 14f
            }

            paginatedPages.getOrElse(pageIndex) { emptyList() }.forEach { item ->
                when (item) {
                    is PdfLineItem.SectionTitle -> {
                        canvas.drawText(item.text, margin, y, sectionPaint)
                        y += 20f
                    }
                    is PdfLineItem.KeyValue -> {
                        canvas.drawText(item.label, margin, y, bodyPaint)
                        val valW = boldPaint.measureText(item.value)
                        canvas.drawText(item.value, width - margin - valW, y, boldPaint)
                        y += 15f
                    }
                    is PdfLineItem.RowText -> {
                        canvas.drawText(item.text, margin, y, bodyPaint)
                        y += 14f
                    }
                    PdfLineItem.Divider -> {
                        canvas.drawLine(margin, y - 4f, width - margin, y - 4f, linePaint)
                        y += 10f
                    }
                    PdfLineItem.Spacer -> {
                        y += 8f
                    }
                }
            }

            val footerText = "Kertas F4 (215 x 330 mm) • Halaman ${pageIndex + 1} dari $totalPages"
            canvas.drawText(footerText, width - margin - subtitlePaint.measureText(footerText), height - 20f, subtitlePaint)
            doc.finishPage(page)
        }

        try {
            FileOutputStream(destination.fileDescriptor).use { out ->
                doc.writeTo(out)
            }
            callback.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
        } catch (e: IOException) {
            callback.onWriteFailed(e.message)
        } finally {
            doc.close()
            pdfDocument = null
        }
    }

    override fun onFinish() {
        pdfDocument?.close()
        pdfDocument = null
        super.onFinish()
    }
}
