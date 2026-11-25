package com.example.util

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.example.data.model.CashFlow
import com.example.data.model.DebtRecord
import com.example.data.model.Expense
import com.example.data.model.Payslip
import com.example.data.model.Purchase
import com.example.data.model.PurchaseReturn
import com.example.data.model.Sale
import com.example.data.model.StoreSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.text.Normalizer
import java.util.UUID

data class ThermalPrinterDevice(
    val name: String,
    val address: String
)

data class ThermalReceiptLine(
    val title: String,
    val detailLeft: String = "",
    val detailRight: String = ""
)

data class ThermalReceiptDocument(
    val headerTitle: String,
    val summaryLabel: String,
    val metadataLines: List<Pair<String, String>>,
    val itemLines: List<ThermalReceiptLine> = emptyList(),
    val extraRows: List<Pair<String, String>> = emptyList(),
    val totalLabel: String,
    val totalAmountText: String,
    val notes: String = ""
)

fun Payslip.toThermalReceipt(): ThermalReceiptDocument {
    val workMillis = workDateMillis.takeIf { it > 0L } ?: salaryDateMillis
    val paidSoFar = effectivePaidAmount()
    val remaining = remainingAmount()
    val statusText = when {
        paid -> "DIBAYAR"
        paidSoFar > 0.0 -> "CICILAN"
        else -> "BELUM DIBAYAR"
    }
    val meta = buildList {
        add("Nama" to employeeName)
        add("Periode" to periodLabel)
        add("Kerja" to formatDateTime(workMillis))
        add("Status" to statusText)
        if (paidAtMillis > 0L && paidSoFar > 0.0) {
            add("Dibayar" to formatDateTime(paidAtMillis))
        }
    }
    val lines = items.map { item ->
        ThermalReceiptLine(
            title = item.jobName,
            detailLeft = "${formatQty(item.quantity)} ${item.unit} x ${formatRupiah(item.ratePerUnit)}",
            detailRight = formatRupiah(item.subtotal)
        )
    }
    val extras = buildList {
        if (bonus > 0.0) add("Bonus" to "+${formatRupiah(bonus)}")
        if (deduction > 0.0) add("Potongan" to "-${formatRupiah(deduction)}")
        if (!paid && paidSoFar > 0.0) {
            add("Sudah Dibayar" to formatRupiah(paidSoFar))
            add("Sisa Gaji" to formatRupiah(remaining))
        }
    }
    return ThermalReceiptDocument(
        headerTitle = "SLIP GAJI KARYAWAN",
        summaryLabel = "Slip: $employeeName - ${formatRupiah(totalAmount)}",
        metadataLines = meta,
        itemLines = lines,
        extraRows = extras,
        totalLabel = if (!paid && paidSoFar > 0.0) "SISA GAJI" else "TOTAL GAJI",
        totalAmountText = formatRupiah(if (!paid && paidSoFar > 0.0) remaining else totalAmount),
        notes = notes
    )
}

fun List<Payslip>.toUnpaidThermalReceipt(employeeFilterLabel: String): ThermalReceiptDocument {
    val unpaid = filter { !it.paid && it.remainingAmount() > 0.0 }
    val grandTotal = unpaid.sumOf { it.remainingAmount() }
    val meta = listOf(
        "Filter" to employeeFilterLabel,
        "Status" to "BELUM DIBAYAR (${unpaid.size} slip)",
        "Dicetak" to formatDateTime(System.currentTimeMillis())
    )
    val lines = unpaid.mapIndexed { idx, slip ->
        val workMillis = slip.workDateMillis.takeIf { it > 0L }
            ?: slip.salaryDateMillis.takeIf { it > 0L }
            ?: slip.createdAtMillis
        val workSummary = slip.items.joinToString(", ") {
            "${it.jobName} ${formatQty(it.quantity)} ${it.unit}"
        }.ifBlank { slip.periodLabel }
        val paidSoFar = slip.effectivePaidAmount()
        val detailSuffix = if (paidSoFar > 0.0) " | Terbayar ${formatRupiah(paidSoFar)}" else ""
        ThermalReceiptLine(
            title = "${idx + 1}. ${slip.employeeName} (${formatDate(workMillis)})",
            detailLeft = "$workSummary$detailSuffix",
            detailRight = formatRupiah(slip.remainingAmount())
        )
    }
    return ThermalReceiptDocument(
        headerTitle = "SLIP GAJI BELUM DIBAYAR",
        summaryLabel = "Gaji Belum Dibayar ($employeeFilterLabel) - ${formatRupiah(grandTotal)}",
        metadataLines = meta,
        itemLines = lines,
        extraRows = listOf("Jumlah Slip" to "${unpaid.size} hari/slip"),
        totalLabel = "TOTAL BELUM BAYAR",
        totalAmountText = formatRupiah(grandTotal),
        notes = "Harap simpan sebagai bukti upah borongan yang belum dibayarkan."
    )
}

fun List<Payslip>.toPaidThermalReceipt(
    employeeFilterLabel: String,
    startMillis: Long,
    endMillis: Long,
    byPaidDate: Boolean
): ThermalReceiptDocument {
    val paidSlips = filter { it.paid || it.remainingAmount() <= 0.0 }
        .sortedBy { slip ->
            val workMillis = slip.workDateMillis.takeIf { it > 0L }
                ?: slip.salaryDateMillis.takeIf { it > 0L }
                ?: slip.createdAtMillis
            if (byPaidDate && slip.paidAtMillis > 0L) slip.paidAtMillis else workMillis
        }
    val grandTotal = paidSlips.sumOf { it.totalAmount }
    val meta = listOf(
        "Nama" to employeeFilterLabel,
        "Periode" to "${formatDateSlash(startMillis)} - ${formatDateSlash(endMillis)}",
        "Dasar" to (if (byPaidDate) "Tgl Dibayar" else "Tgl Kerja"),
        "Status" to "DIBAYAR PENUH (${paidSlips.size} slip)",
        "Dicetak" to formatDateTime(System.currentTimeMillis())
    )
    val lines = paidSlips.mapIndexed { idx, slip ->
        val workMillis = slip.workDateMillis.takeIf { it > 0L }
            ?: slip.salaryDateMillis.takeIf { it > 0L }
            ?: slip.createdAtMillis
        val paidMillis = slip.paidAtMillis.takeIf { it > 0L } ?: workMillis
        val workSummary = slip.items.joinToString(", ") {
            "${it.jobName} ${formatQty(it.quantity)} ${it.unit}"
        }.ifBlank { slip.periodLabel }
        ThermalReceiptLine(
            title = "${idx + 1}. ${slip.employeeName} (${formatDate(workMillis)})",
            detailLeft = "$workSummary | Lunas ${formatDateSlash(paidMillis)}",
            detailRight = formatRupiah(slip.totalAmount)
        )
    }
    return ThermalReceiptDocument(
        headerTitle = "SLIP GAJI DIBAYAR PENUH",
        summaryLabel = "Gaji Dibayar Penuh ($employeeFilterLabel) - ${formatRupiah(grandTotal)}",
        metadataLines = meta,
        itemLines = lines,
        extraRows = listOf("Jumlah Slip" to "${paidSlips.size} transaksi"),
        totalLabel = "TOTAL GAJI DIBAYAR",
        totalAmountText = formatRupiah(grandTotal),
        notes = "Rekap slip gaji yang telah dibayar penuh."
    )
}

fun List<DebtRecord>.toPaidDebtsThermalReceipt(
    typeLabel: String,
    partyFilterLabel: String,
    startMillis: Long,
    endMillis: Long,
    byPaidDate: Boolean
): ThermalReceiptDocument {
    val paidDebts = filter { it.status == "LUNAS" || it.remainingAmount <= 0.0 }
        .sortedBy { debt ->
            if (byPaidDate && debt.paidAtMillis > 0L) debt.paidAtMillis else debt.transactionDateMillis
        }
    val grandTotal = paidDebts.sumOf { it.totalAmount }
    val hutangTotal = paidDebts.filter { it.debtType == "HUTANG" }.sumOf { it.totalAmount }
    val piutangTotal = paidDebts.filter { it.debtType == "PIUTANG" }.sumOf { it.totalAmount }
    val meta = listOf(
        "Jenis" to typeLabel,
        "Nama" to partyFilterLabel,
        "Periode" to "${formatDateSlash(startMillis)} - ${formatDateSlash(endMillis)}",
        "Dasar" to (if (byPaidDate) "Tgl Lunas" else "Tgl Nota"),
        "Status" to "LUNAS (${paidDebts.size} nota)",
        "Dicetak" to formatDateTime(System.currentTimeMillis())
    )
    val lines = paidDebts.mapIndexed { idx, debt ->
        val paidMillis = debt.paidAtMillis.takeIf { it > 0L } ?: debt.transactionDateMillis
        val kind = if (debt.debtType == "HUTANG") "Hutang" else "Piutang"
        ThermalReceiptLine(
            title = "${idx + 1}. ${debt.partyName} (${debt.invoiceNo.ifBlank { kind }})",
            detailLeft = "$kind • Nota ${formatDateSlash(debt.transactionDateMillis)} • Lunas ${formatDateSlash(paidMillis)}",
            detailRight = formatRupiah(debt.totalAmount)
        )
    }
    val extras = buildList {
        if (hutangTotal > 0.0 && piutangTotal > 0.0) {
            add("Hutang Lunas" to formatRupiah(hutangTotal))
            add("Piutang Lunas" to formatRupiah(piutangTotal))
        }
        add("Jumlah Nota" to "${paidDebts.size} transaksi")
    }
    return ThermalReceiptDocument(
        headerTitle = "REKAP ${typeLabel.uppercase()} LUNAS",
        summaryLabel = "$typeLabel Lunas ($partyFilterLabel) - ${formatRupiah(grandTotal)}",
        metadataLines = meta,
        itemLines = lines,
        extraRows = extras,
        totalLabel = "TOTAL DIBAYAR PENUH",
        totalAmountText = formatRupiah(grandTotal),
        notes = "Rekap transaksi $typeLabel yang telah dibayar penuh."
    )
}

fun Sale.toThermalReceipt(
    isLossSale: Boolean = lossAmount > 0.0 || items.any { it.sellPrice < it.buyPrice },
    cashTendered: Double? = null,
    changeAmount: Double? = null
): ThermalReceiptDocument {
    val computedLoss = if (lossAmount > 0.0) {
        lossAmount
    } else {
        items.filter { it.sellPrice < it.buyPrice }.sumOf { (it.buyPrice - it.sellPrice) * it.quantity }
    }
    val meta = buildList {
        add("No. Nota" to invoiceNo)
        add("Pembeli" to customerName)
        add("Waktu" to formatDateTime(transactionDateMillis))
        add("Status" to paymentStatus)
    }
    val lines = items.map { item ->
        val extraDetail = if (isLossSale && item.sellPrice < item.buyPrice) {
            " (Modal ${formatRupiah(item.buyPrice)})"
        } else {
            ""
        }
        ThermalReceiptLine(
            title = item.productName,
            detailLeft = "${formatQty(item.quantity)} ${item.unit} x ${formatRupiah(item.sellPrice)}$extraDetail",
            detailRight = formatRupiah(item.subtotal)
        )
    }
    val extras = buildList {
        if (remainingDebt <= 0.0 && cashTendered != null && cashTendered > 0.0) {
            val kembalian = (changeAmount ?: (cashTendered - totalAmount)).coerceAtLeast(0.0)
            add("Tunai" to formatRupiah(cashTendered))
            add("Kembalian" to formatRupiah(kembalian))
        } else if (paidAmount > 0.0 && remainingDebt > 0.0) {
            add("Dibayar" to formatRupiah(paidAmount))
            add("Piutang" to formatRupiah(remainingDebt))
        } else if (remainingDebt > 0.0) {
            add("Sisa Piutang" to formatRupiah(remainingDebt))
        }
        if (isLossSale && computedLoss > 0.0) {
            add("Total Rugi" to formatRupiah(computedLoss))
        }
    }
    return ThermalReceiptDocument(
        headerTitle = if (isLossSale) "NOTA JUAL RUGI" else "NOTA PENJUALAN",
        summaryLabel = "${if (isLossSale) "Jual Rugi" else "Penjualan"}: $invoiceNo - ${formatRupiah(totalAmount)}",
        metadataLines = meta,
        itemLines = lines,
        extraRows = extras,
        totalLabel = "TOTAL TAGIHAN",
        totalAmountText = formatRupiah(totalAmount),
        notes = notes
    )
}

fun Purchase.toThermalReceipt(
    cashPaidInput: Double? = null,
    changeAmount: Double? = null
): ThermalReceiptDocument {
    val meta = buildList {
        add("No. Nota" to invoiceNo)
        add("Pemasok" to supplierName)
        add("Waktu" to formatDateTime(transactionDateMillis))
        add("Status" to paymentStatus)
    }
    val lines = items.map { item ->
        ThermalReceiptLine(
            title = item.productName,
            detailLeft = "${formatQty(item.quantity)} ${item.unit} x ${formatRupiah(item.buyPrice)}",
            detailRight = formatRupiah(item.subtotal)
        )
    }
    val extras = buildList {
        if (remainingDebt <= 0.0 && cashPaidInput != null && cashPaidInput > 0.0) {
            val kembalian = (changeAmount ?: (cashPaidInput - totalAmount)).coerceAtLeast(0.0)
            add("Tunai" to formatRupiah(cashPaidInput))
            add("Kembalian" to formatRupiah(kembalian))
        } else if (paidAmount > 0.0 && remainingDebt > 0.0) {
            add("Dibayar" to formatRupiah(paidAmount))
            add("Sisa Hutang" to formatRupiah(remainingDebt))
        } else if (remainingDebt > 0.0) {
            add("Sisa Hutang" to formatRupiah(remainingDebt))
        }
    }
    return ThermalReceiptDocument(
        headerTitle = "NOTA PEMBELIAN",
        summaryLabel = "Pembelian: $invoiceNo - ${formatRupiah(totalAmount)}",
        metadataLines = meta,
        itemLines = lines,
        extraRows = extras,
        totalLabel = "TOTAL PEMBELIAN",
        totalAmountText = formatRupiah(totalAmount),
        notes = notes
    )
}

fun PurchaseReturn.toThermalReceipt(): ThermalReceiptDocument {
    val methodText = if (refundMethod == "POTONG_HUTANG") "Potong Hutang" else "Kembali ke Kas"
    val meta = listOf(
        "Pemasok" to supplierName,
        "Waktu" to formatDateTime(transactionDateMillis),
        "Metode" to methodText
    )
    val lines = listOf(
        ThermalReceiptLine(
            title = productName,
            detailLeft = "${formatQty(quantity)} x ${formatRupiah(unitPrice)}",
            detailRight = formatRupiah(totalRefund)
        )
    )
    return ThermalReceiptDocument(
        headerTitle = "NOTA RETUR PEMBELIAN",
        summaryLabel = "Retur: $productName - ${formatRupiah(totalRefund)}",
        metadataLines = meta,
        itemLines = lines,
        totalLabel = "NILAI RETUR",
        totalAmountText = formatRupiah(totalRefund),
        notes = reason
    )
}

fun Expense.toThermalReceipt(): ThermalReceiptDocument {
    val meta = listOf(
        "Judul" to title,
        "Kategori" to category,
        "Kas" to cashBoxName.ifBlank { "-" },
        "Waktu" to formatDateTime(transactionDateMillis)
    )
    return ThermalReceiptDocument(
        headerTitle = "BUKTI PENGELUARAN",
        summaryLabel = "Pengeluaran: $title - ${formatRupiah(amount)}",
        metadataLines = meta,
        itemLines = listOf(
            ThermalReceiptLine(
                title = title,
                detailLeft = category,
                detailRight = formatRupiah(amount)
            )
        ),
        totalLabel = "TOTAL PENGELUARAN",
        totalAmountText = formatRupiah(amount),
        notes = notes
    )
}

fun DebtRecord.toThermalReceipt(): ThermalReceiptDocument {
    val isHutang = debtType == "HUTANG"
    val meta = buildList {
        add("No. Ref" to invoiceNo.ifBlank { "-" })
        add((if (isHutang) "Pemasok" else "Pedagang") to partyName)
        add("Waktu" to formatDateTime(transactionDateMillis))
        if (paidAtMillis > 0L) {
            add("Bayar" to formatDateTime(paidAtMillis))
        } else {
            add("Bayar" to "Belum dibayar")
        }
        add("Status" to status.replace("_", " "))
    }
    val extras = listOf(
        "Total Awal" to formatRupiah(totalAmount),
        "Terbayar" to formatRupiah(paidAmount)
    )
    return ThermalReceiptDocument(
        headerTitle = if (isHutang) "NOTA HUTANG PEMBELIAN" else "NOTA PIUTANG PEDAGANG",
        summaryLabel = "${if (isHutang) "Hutang" else "Piutang"}: $partyName - Sisa ${formatRupiah(remainingAmount)}",
        metadataLines = meta,
        extraRows = extras,
        totalLabel = "SISA TAGIHAN",
        totalAmountText = formatRupiah(remainingAmount),
        notes = notes
    )
}

fun CashFlow.toThermalReceipt(): ThermalReceiptDocument {
    val isIn = flowType == "IN"
    val meta = listOf(
        "Jenis" to (if (isIn) "KAS MASUK" else "KAS KELUAR"),
        "Kategori" to category.replace("_", " "),
        "Cash Box" to cashBoxName.ifBlank { "-" },
        "Waktu" to formatDateTime(transactionDateMillis)
    )
    return ThermalReceiptDocument(
        headerTitle = "BUKTI ARUS UANG",
        summaryLabel = "${if (isIn) "Masuk" else "Keluar"}: ${description.ifBlank { category }} - ${formatRupiah(amount)}",
        metadataLines = meta,
        totalLabel = if (isIn) "TOTAL KAS MASUK" else "TOTAL KAS KELUAR",
        totalAmountText = formatRupiah(amount),
        notes = description
    )
}

object BluetoothThermalPrinter {
    private val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    private const val PREFS_NAME = "afi_printer_prefs"
    private const val KEY_LAST_ADDRESS = "last_printer_address"
    private const val KEY_LAST_NAME = "last_printer_name"
    private const val KEY_LINE_CHARS = "last_printer_line_chars"

    fun createTestReceiptDocument(): ThermalReceiptDocument = ThermalReceiptDocument(
        headerTitle = "TEST CETAK PRINTER",
        summaryLabel = "Tes Koneksi Printer Bluetooth",
        metadataLines = listOf(
            "Status" to "TERHUBUNG",
            "Waktu" to formatDateTime(System.currentTimeMillis())
        ),
        itemLines = listOf(
            ThermalReceiptLine(
                title = "Contoh Item Produk",
                detailLeft = "2 kg x Rp 10.000",
                detailRight = "Rp 20.000"
            )
        ),
        totalLabel = "TOTAL TEST",
        totalAmountText = "Rp 20.000",
        notes = "Printer siap digunakan untuk mencetak nota."
    )

    fun requiredPermissions(): Array<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_SCAN
            )
        } else {
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    fun hasConnectPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun hasScanPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_SCAN
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun hasDiscoveryPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            hasScanPermission(context)
        } else {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun getAdapter(context: Context): BluetoothAdapter? {
        val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        return manager?.adapter
    }

    @SuppressLint("MissingPermission")
    fun getPairedPrinters(context: Context): List<ThermalPrinterDevice> {
        if (!hasConnectPermission(context)) return emptyList()
        val adapter = getAdapter(context) ?: return emptyList()
        return runCatching {
            adapter.bondedDevices
                ?.map { device ->
                    ThermalPrinterDevice(
                        name = device.name?.takeIf { it.isNotBlank() } ?: "Printer Bluetooth",
                        address = device.address
                    )
                }
                ?.sortedBy { it.name.lowercase() }
                ?: emptyList()
        }.getOrDefault(emptyList())
    }

    fun getSavedPrinterAddress(context: Context): String? =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_LAST_ADDRESS, null)

    fun getSavedPrinterName(context: Context): String? =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_LAST_NAME, null)

    fun savePrinterAddress(context: Context, address: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_LAST_ADDRESS, address)
            .apply()
    }

    fun savePrinterSelection(context: Context, name: String, address: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_LAST_NAME, name)
            .putString(KEY_LAST_ADDRESS, address)
            .apply()
    }

    fun clearSavedPrinter(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_LAST_ADDRESS)
            .remove(KEY_LAST_NAME)
            .apply()
    }

    fun getSavedLineChars(context: Context): Int =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getInt(KEY_LINE_CHARS, 32)

    fun saveLineChars(context: Context, lineChars: Int) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putInt(KEY_LINE_CHARS, lineChars)
            .apply()
    }

    suspend fun printPayslip(
        context: Context,
        deviceAddress: String,
        payslip: Payslip,
        storeSettings: StoreSettings,
        lineChars: Int = 32
    ): Result<Unit> = printDocument(
        context = context,
        deviceAddress = deviceAddress,
        document = payslip.toThermalReceipt(),
        storeSettings = storeSettings,
        lineChars = lineChars
    )

    @SuppressLint("MissingPermission")
    private fun connectSocketWithFallback(device: BluetoothDevice): BluetoothSocket {
        val attempts: List<() -> BluetoothSocket> = listOf(
            { device.createRfcommSocketToServiceRecord(SPP_UUID) },
            { device.createInsecureRfcommSocketToServiceRecord(SPP_UUID) },
            {
                val method = device.javaClass.getMethod("createRfcommSocket", Int::class.javaPrimitiveType)
                method.invoke(device, 1) as BluetoothSocket
            }
        )

        var lastError: Exception? = null
        for (factory in attempts) {
            var candidate: BluetoothSocket? = null
            try {
                candidate = factory()
                candidate.connect()
                return candidate
            } catch (e: Exception) {
                lastError = e
                runCatching { candidate?.close() }
            }
        }
        throw lastError ?: IOException("Tidak dapat membuat koneksi RFCOMM ke printer")
    }

    @SuppressLint("MissingPermission")
    suspend fun printDocument(
        context: Context,
        deviceAddress: String,
        document: ThermalReceiptDocument,
        storeSettings: StoreSettings,
        lineChars: Int = 32
    ): Result<Unit> = withContext(Dispatchers.IO) {
        if (!hasConnectPermission(context)) {
            return@withContext Result.failure(IllegalStateException("Izin Bluetooth belum diberikan."))
        }
        val adapter = getAdapter(context)
            ?: return@withContext Result.failure(IllegalStateException("Perangkat tidak mendukung Bluetooth."))
        if (!adapter.isEnabled) {
            return@withContext Result.failure(IllegalStateException("Bluetooth belum aktif. Aktifkan Bluetooth terlebih dahulu."))
        }

        val device: BluetoothDevice = try {
            adapter.getRemoteDevice(deviceAddress)
        } catch (_: IllegalArgumentException) {
            return@withContext Result.failure(IllegalStateException("Alamat printer Bluetooth tidak valid."))
        }

        if (hasDiscoveryPermission(context)) {
            runCatching { adapter.cancelDiscovery() }
            delay(180L)
        }

        var lastFailure: Exception? = null
        for (attempt in 1..2) {
            var socket: BluetoothSocket? = null
            try {
                socket = withTimeoutOrNull(10_000L) {
                    connectSocketWithFallback(device)
                } ?: throw IOException("Koneksi printer melewati batas waktu (timeout)")

                val payload = buildEscPosDocumentBytes(document, storeSettings, lineChars)
                val out = socket.outputStream
                val chunkSize = 256
                var offset = 0
                while (offset < payload.size) {
                    val count = minOf(chunkSize, payload.size - offset)
                    out.write(payload, offset, count)
                    out.flush()
                    offset += count
                    if (offset < payload.size) {
                        delay(20L)
                    }
                }
                // Tunggu buffer Bluetooth SPP printer selesai dikonsumsi sebelum socket ditutup
                delay(450L)

                savePrinterAddress(context, deviceAddress)
                saveLineChars(context, lineChars)
                return@withContext Result.success(Unit)
            } catch (e: Exception) {
                lastFailure = e
                if (attempt < 2) {
                    delay(350L)
                }
            } finally {
                runCatching { socket?.close() }
            }
        }

        Result.failure(
            IllegalStateException(
                "Gagal mencetak ke printer (${lastFailure?.localizedMessage ?: "koneksi terputus"}). Pastikan printer menyala, baterai cukup & sudah dipasangkan (paired)."
            )
        )
    }

    fun buildEscPosPayslipBytes(
        payslip: Payslip,
        storeSettings: StoreSettings,
        lineChars: Int = 32
    ): ByteArray = buildEscPosDocumentBytes(payslip.toThermalReceipt(), storeSettings, lineChars)

    fun buildEscPosDocumentBytes(
        document: ThermalReceiptDocument,
        storeSettings: StoreSettings,
        lineChars: Int = 32
    ): ByteArray {
        val cols = lineChars.coerceIn(28, 48)
        val out = ByteArrayOutputStream()
        val sep = "-".repeat(cols)
        val eqSep = "=".repeat(cols)

        fun writeBytes(vararg bytes: Int) {
            bytes.forEach { out.write(it) }
        }

        fun sanitizeAscii(raw: String): String {
            val normalized = Normalizer.normalize(raw, Normalizer.Form.NFD)
                .replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
            val sb = StringBuilder(normalized.length)
            for (ch in normalized) {
                when (ch) {
                    '–', '—' -> sb.append('-')
                    '•' -> sb.append('*')
                    '“', '”' -> sb.append('"')
                    '‘', '’' -> sb.append('\'')
                    else -> if (ch.code in 32..126) sb.append(ch) else sb.append(' ')
                }
            }
            return sb.toString().trimEnd()
        }

        fun wrapLines(text: String): List<String> {
            val clean = sanitizeAscii(text)
            if (clean.isEmpty()) return listOf("")
            if (clean.length <= cols) return listOf(clean)
            val result = mutableListOf<String>()
            var remaining = clean
            while (remaining.length > cols) {
                val breakIdx = remaining.lastIndexOf(' ', cols).let { if (it <= 0) cols else it }
                result.add(remaining.substring(0, breakIdx).trimEnd())
                remaining = remaining.substring(breakIdx).trimStart()
            }
            if (remaining.isNotEmpty()) result.add(remaining)
            return result
        }

        fun writeLine(text: String = "") {
            wrapLines(text).forEach { line ->
                out.write((line + "\n").toByteArray(Charsets.US_ASCII))
            }
        }

        fun twoColLine(left: String, right: String): String {
            val cleanRight = sanitizeAscii(right)
            val cleanLeft = sanitizeAscii(left)
            val maxLeft = (cols - cleanRight.length - 1).coerceAtLeast(1)
            val trimmedLeft = if (cleanLeft.length > maxLeft) cleanLeft.take(maxLeft) else cleanLeft
            val spaces = (cols - trimmedLeft.length - cleanRight.length).coerceAtLeast(1)
            return trimmedLeft + " ".repeat(spaces) + cleanRight
        }

        // ESC @ Initialize
        writeBytes(0x1B, 0x40)

        // Center Align + Bold Header
        writeBytes(0x1B, 0x61, 0x01)
        writeBytes(0x1B, 0x45, 0x01)
        writeLine(storeSettings.storeName.ifBlank { "TOKO & PABRIK AFI" }.uppercase())
        writeBytes(0x1B, 0x45, 0x00)
        if (storeSettings.address.isNotBlank()) writeLine(storeSettings.address)
        if (storeSettings.phone.isNotBlank()) writeLine("Telp: ${storeSettings.phone}")
        writeLine(eqSep)
        writeBytes(0x1B, 0x45, 0x01)
        writeLine(document.headerTitle.uppercase())
        writeBytes(0x1B, 0x45, 0x00)
        writeLine(sep)

        // Left Align Body
        writeBytes(0x1B, 0x61, 0x00)
        document.metadataLines.forEach { (label, value) ->
            val padded = sanitizeAscii(label).padEnd(8).take(8)
            writeLine("$padded: $value")
        }
        writeLine(sep)

        if (document.itemLines.isNotEmpty()) {
            document.itemLines.forEach { item ->
                writeLine(item.title)
                if (item.detailLeft.isNotBlank() || item.detailRight.isNotBlank()) {
                    writeLine(twoColLine(item.detailLeft, item.detailRight))
                }
            }
        }

        document.extraRows.forEach { (label, value) ->
            writeLine(twoColLine(label, value))
        }

        if (document.itemLines.isNotEmpty() || document.extraRows.isNotEmpty()) {
            writeLine(sep)
        }
        writeBytes(0x1B, 0x45, 0x01)
        writeLine(twoColLine(document.totalLabel, document.totalAmountText))
        writeBytes(0x1B, 0x45, 0x00)
        writeLine(eqSep)

        if (document.notes.isNotBlank()) {
            writeLine("Catatan: ${document.notes}")
        }

        writeBytes(0x1B, 0x61, 0x01)
        writeLine(storeSettings.receiptFooter.ifBlank { "Terima kasih atas kerja sama Anda." })
        writeLine("")
        // Feed 5 lines agar kertas mudah disobek tanpa byte cutter yang menyebabkan teks acak
        writeBytes(0x1B, 0x64, 0x05)

        return out.toByteArray()
    }
}

