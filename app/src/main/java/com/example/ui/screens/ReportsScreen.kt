package com.example.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.MoneyOff
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.CashFlow
import com.example.data.model.Expense
import com.example.data.model.Payslip
import com.example.data.model.Purchase
import com.example.data.model.Sale
import com.example.data.model.StoreSettings
import com.example.data.model.UiState
import com.example.ui.components.AdaptiveTwoColumnRow
import com.example.ui.components.CurrencyText
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.ResponsiveScreenContainer
import com.example.ui.components.SimpleBarChart
import com.example.ui.components.StatKpiCard
import com.example.ui.components.StatusBadge
import com.example.ui.theme.InfoBlue
import com.example.ui.theme.LossRed
import com.example.ui.theme.LossRedBg
import com.example.ui.theme.ProfitGreen
import com.example.ui.theme.ProfitGreenBg
import com.example.ui.theme.WarningAmber
import com.example.util.PayslipPrinter
import com.example.util.ReportPdfSection
import com.example.util.formatDate
import com.example.util.formatDateTime
import com.example.util.formatMonthYear
import com.example.util.formatQty
import com.example.util.formatRupiah
import com.example.util.isSameDay
import com.example.util.isSameMonth
import com.example.util.isSameYear
import java.util.Calendar

@Composable
fun ReportsScreen(
    salesState: UiState<List<Sale>>,
    purchasesState: UiState<List<Purchase>>,
    expensesState: UiState<List<Expense>>,
    payslipsState: UiState<List<Payslip>>,
    cashFlowsState: UiState<List<CashFlow>>,
    storeSettingsState: UiState<StoreSettings>
) {
    // 0=Harian, 1=Bulanan, 2=Tahunan, 3=Laba, 4=Jual Rugi, 5=Gaji Bulanan, 6=Arus Uang
    var selectedTab by remember { mutableIntStateOf(0) }
    var showExportDialog by remember { mutableStateOf(false) }

    val calNow = remember { Calendar.getInstance() }
    var dailyDateMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var monthlyYear by remember { mutableIntStateOf(calNow.get(Calendar.YEAR)) }
    var monthlyMonth by remember { mutableIntStateOf(calNow.get(Calendar.MONTH)) }
    var yearlyYear by remember { mutableIntStateOf(calNow.get(Calendar.YEAR)) }
    var profitYear by remember { mutableIntStateOf(calNow.get(Calendar.YEAR)) }
    var profitMonth by remember { mutableIntStateOf(calNow.get(Calendar.MONTH)) }
    var lossYear by remember { mutableIntStateOf(calNow.get(Calendar.YEAR)) }
    var lossMonth by remember { mutableIntStateOf(calNow.get(Calendar.MONTH)) }
    var payrollYear by remember { mutableIntStateOf(calNow.get(Calendar.YEAR)) }
    var payrollMonth by remember { mutableIntStateOf(calNow.get(Calendar.MONTH)) }
    var cashFlowYear by remember { mutableIntStateOf(calNow.get(Calendar.YEAR)) }
    var cashFlowMonth by remember { mutableIntStateOf(calNow.get(Calendar.MONTH)) }
    var cashFlowFilter by remember { mutableStateOf("ALL") } // ALL, IN, OUT, EXPENSE, TRANSFER

    val context = LocalContext.current
    val sales = (salesState as? UiState.Success)?.data ?: emptyList()
    val purchases = (purchasesState as? UiState.Success)?.data ?: emptyList()
    val expenses = (expensesState as? UiState.Success)?.data ?: emptyList()
    val payslips = (payslipsState as? UiState.Success)?.data ?: emptyList()
    val cashFlows = (cashFlowsState as? UiState.Success)?.data ?: emptyList()
    val storeSettings = (storeSettingsState as? UiState.Success)?.data ?: StoreSettings()

    fun buildAllReportSections(): List<ReportPdfSection> {
        val dSales = sales.filter { isSameDay(it.transactionDateMillis, dailyDateMillis) }
        val dPurchases = purchases.filter { isSameDay(it.transactionDateMillis, dailyDateMillis) }
        val dExpenses = expenses.filter { isSameDay(it.transactionDateMillis, dailyDateMillis) }
        val dPayroll = payslips.filter { it.effectivePaidAmount() > 0.0 && isSameDay(it.paidAtMillis, dailyDateMillis) }
        val dNet = dSales.sumOf { it.totalProfit } - dExpenses.sumOf { it.amount } - dPayroll.sumOf { it.effectivePaidAmount() }

        val mSales = sales.filter { isSameMonth(it.transactionDateMillis, monthlyYear, monthlyMonth) }
        val mPurchases = purchases.filter { isSameMonth(it.transactionDateMillis, monthlyYear, monthlyMonth) }
        val mExpenses = expenses.filter { isSameMonth(it.transactionDateMillis, monthlyYear, monthlyMonth) }
        val mPayroll = payslips.filter { it.effectivePaidAmount() > 0.0 && isSameMonth(it.paidAtMillis, monthlyYear, monthlyMonth) }
        val mNet = mSales.sumOf { it.totalProfit } - mExpenses.sumOf { it.amount } - mPayroll.sumOf { it.effectivePaidAmount() }

        val ySales = sales.filter { isSameYear(it.transactionDateMillis, yearlyYear) }
        val yPurchases = purchases.filter { isSameYear(it.transactionDateMillis, yearlyYear) }
        val yExpenses = expenses.filter { isSameYear(it.transactionDateMillis, yearlyYear) }
        val yPayroll = payslips.filter { it.effectivePaidAmount() > 0.0 && isSameYear(it.paidAtMillis, yearlyYear) }
        val yNet = ySales.sumOf { it.totalProfit } - yExpenses.sumOf { it.amount } - yPayroll.sumOf { it.effectivePaidAmount() }

        val profitSales = sales.filter { isSameMonth(it.transactionDateMillis, profitYear, profitMonth) }
        val profitExpenses = expenses.filter { isSameMonth(it.transactionDateMillis, profitYear, profitMonth) }
        val profitPayroll = payslips.filter { it.effectivePaidAmount() > 0.0 && isSameMonth(it.paidAtMillis, profitYear, profitMonth) }
        val totalOmzet = profitSales.sumOf { it.totalAmount }
        val totalHpp = profitSales.sumOf { it.totalCost }
        val grossProfit = profitSales.sumOf { it.totalProfit }
        val totalExp = profitExpenses.sumOf { it.amount }
        val totalPay = profitPayroll.sumOf { it.effectivePaidAmount() }
        val netProfit = grossProfit - totalExp - totalPay

        val lossSales = sales.filter { s ->
            isSameMonth(s.transactionDateMillis, lossYear, lossMonth) &&
                (s.lossAmount > 0.0 || s.items.any { it.sellPrice < it.buyPrice })
        }
        val totalLoss = lossSales.sumOf { s ->
            if (s.lossAmount > 0.0) s.lossAmount
            else s.items.filter { it.sellPrice < it.buyPrice }.sumOf { (it.buyPrice - it.sellPrice) * it.quantity }
        }

        val pSlips = payslips.filter { slip ->
            val workMillis = slip.workDateMillis.takeIf { it > 0L }
                ?: slip.salaryDateMillis.takeIf { it > 0L }
                ?: slip.createdAtMillis
            isSameMonth(workMillis, payrollYear, payrollMonth)
        }
        val pTotal = pSlips.sumOf { it.totalAmount }
        val pPaid = pSlips.sumOf { it.effectivePaidAmount() }
        val pUnpaid = pSlips.sumOf { it.remainingAmount() }
        val pEmpRows = pSlips.groupBy { it.employeeName }
            .entries
            .sortedBy { it.key.lowercase() }
            .map { (empName, list) ->
                val ePaid = list.sumOf { it.effectivePaidAmount() }
                val eUnpaid = list.sumOf { it.remainingAmount() }
                "• $empName (${list.size} hari/slip): Total ${formatRupiah(ePaid + eUnpaid)} | Dibayar ${formatRupiah(ePaid)} | Belum ${formatRupiah(eUnpaid)}"
            }

        val monthCashFlows = cashFlows.filter { isSameMonth(it.transactionDateMillis, cashFlowYear, cashFlowMonth) }
        val filteredCashFlows = monthCashFlows.filter { flow ->
            when (cashFlowFilter) {
                "IN" -> flow.flowType == "IN"
                "OUT" -> flow.flowType == "OUT"
                "EXPENSE" -> flow.category == "PENGELUARAN"
                "TRANSFER" -> flow.category == "PINDAH_SALDO"
                else -> true
            }
        }
        val cashFlowFilterLabel = when (cashFlowFilter) {
            "IN" -> "Kas Masuk"
            "OUT" -> "Kas Keluar"
            "EXPENSE" -> "Pengeluaran"
            "TRANSFER" -> "Pindah Saldo"
            else -> "Semua"
        }
        val totalCashIn = monthCashFlows.filter { it.flowType == "IN" }.sumOf { it.amount }
        val totalCashOut = monthCashFlows.filter { it.flowType == "OUT" }.sumOf { it.amount }
        val totalExpenseFlow = monthCashFlows.filter { it.category == "PENGELUARAN" }.sumOf { it.amount }
        val flowRows = filteredCashFlows.mapIndexed { idx, flow ->
            val sign = if (flow.flowType == "IN") "[MASUK]" else "[KELUAR]"
            "${idx + 1}. $sign ${formatDateTime(flow.transactionDateMillis)} • ${flow.description.ifBlank { flow.category }} (${flow.cashBoxName}) = ${formatRupiah(flow.amount)}"
        }

        val productRows = profitSales.flatMap { it.items }
            .groupBy { it.productName }
            .map { (name, items) ->
                val qty = items.sumOf { it.quantity }
                val prof = items.sumOf { it.profit }
                "• $name (${formatQty(qty)} terjual) : Laba ${formatRupiah(prof)}"
            }

        val profitPeriodLabel = formatMonthYear(profitYear, profitMonth)
        val lossPeriodLabel = formatMonthYear(lossYear, lossMonth)
        val cashFlowPeriodLabel = formatMonthYear(cashFlowYear, cashFlowMonth)

        return listOf(
            ReportPdfSection(
                title = "1. LAPORAN HARIAN (${formatDate(dailyDateMillis)})",
                summaryPairs = listOf(
                    "Penjualan (${dSales.size} nota)" to formatRupiah(dSales.sumOf { it.totalAmount }),
                    "Pembelian (${dPurchases.size} nota)" to formatRupiah(dPurchases.sumOf { it.totalAmount }),
                    "Pengeluaran (${dExpenses.size})" to formatRupiah(dExpenses.sumOf { it.amount }),
                    "Gaji Dibayar (${dPayroll.size})" to formatRupiah(dPayroll.sumOf { it.effectivePaidAmount() }),
                    "Estimasi Laba Bersih Hari Ini" to formatRupiah(dNet)
                ),
                rows = dSales.map { s ->
                    "• ${s.invoiceNo} (${s.customerName}) ${formatDateTime(s.transactionDateMillis)}: ${formatRupiah(s.totalAmount)}"
                }
            ),
            ReportPdfSection(
                title = "2. LAPORAN BULANAN (${formatMonthYear(monthlyYear, monthlyMonth)})",
                summaryPairs = listOf(
                    "Omzet Bulanan (${mSales.size} nota)" to formatRupiah(mSales.sumOf { it.totalAmount }),
                    "Pembelian Bulanan (${mPurchases.size} nota)" to formatRupiah(mPurchases.sumOf { it.totalAmount }),
                    "Pengeluaran Bulanan" to formatRupiah(mExpenses.sumOf { it.amount }),
                    "Gaji Pabrik Bulanan" to formatRupiah(mPayroll.sumOf { it.effectivePaidAmount() }),
                    "Laba Bersih Bulanan" to formatRupiah(mNet)
                )
            ),
            ReportPdfSection(
                title = "3. LAPORAN TAHUNAN ($yearlyYear)",
                summaryPairs = listOf(
                    "Omzet Tahunan (${ySales.size} nota)" to formatRupiah(ySales.sumOf { it.totalAmount }),
                    "Total Pembelian Tahunan" to formatRupiah(yPurchases.sumOf { it.totalAmount }),
                    "Pengeluaran Tahunan" to formatRupiah(yExpenses.sumOf { it.amount }),
                    "Gaji Karyawan Tahunan" to formatRupiah(yPayroll.sumOf { it.effectivePaidAmount() }),
                    "Laba Bersih Tahun $yearlyYear" to formatRupiah(yNet)
                )
            ),
            ReportPdfSection(
                title = "4. LAPORAN LABA RUGI BULAN ${profitPeriodLabel.uppercase()}",
                summaryPairs = listOf(
                    "Total Omzet Penjualan (${profitSales.size} nota)" to formatRupiah(totalOmzet),
                    "Harga Pokok Modal (HPP)" to formatRupiah(totalHpp),
                    "Laba Kotor Penjualan" to formatRupiah(grossProfit),
                    "Beban Pengeluaran" to formatRupiah(totalExp),
                    "Gaji Karyawan Dibayar" to formatRupiah(totalPay),
                    "LABA BERSIH ($profitPeriodLabel)" to formatRupiah(netProfit)
                ),
                rows = if (productRows.isEmpty()) listOf("Belum ada penjualan pada bulan $profitPeriodLabel.") else productRows
            ),
            ReportPdfSection(
                title = "5. LAPORAN JUAL RUGI BULAN ${lossPeriodLabel.uppercase()}",
                summaryPairs = listOf(
                    "Jumlah Transaksi Jual Rugi" to "${lossSales.size} transaksi",
                    "Total Nilai Kerugian ($lossPeriodLabel)" to formatRupiah(totalLoss)
                ),
                rows = if (lossSales.isEmpty()) {
                    listOf("Tidak ada transaksi jual rugi pada bulan $lossPeriodLabel.")
                } else {
                    lossSales.map { s ->
                        val sLoss = if (s.lossAmount > 0.0) s.lossAmount
                        else s.items.filter { it.sellPrice < it.buyPrice }.sumOf { (it.buyPrice - it.sellPrice) * it.quantity }
                        "• ${s.invoiceNo} (${s.customerName}) - ${formatDateTime(s.transactionDateMillis)}: Tagihan ${formatRupiah(s.totalAmount)} | Rugi ${formatRupiah(sLoss)}"
                    }
                }
            ),
            ReportPdfSection(
                title = "6. LAPORAN GAJI KARYAWAN BULANAN (${formatMonthYear(payrollYear, payrollMonth)})",
                summaryPairs = listOf(
                    "Jumlah Transaksi Borongan" to "${pSlips.size} slip",
                    "Total Gaji Bulan Ini" to formatRupiah(pTotal),
                    "Sudah Dibayar" to formatRupiah(pPaid),
                    "Belum Dibayar" to formatRupiah(pUnpaid)
                ),
                rows = if (pEmpRows.isEmpty()) listOf("Belum ada transaksi gaji pada bulan ini.") else pEmpRows
            ),
            ReportPdfSection(
                title = "7. HASIL ARUS UANG BULAN ${cashFlowPeriodLabel.uppercase()} ($cashFlowFilterLabel: ${filteredCashFlows.size} CATATAN)",
                summaryPairs = listOf(
                    "Total Kas Masuk ($cashFlowPeriodLabel)" to formatRupiah(totalCashIn),
                    "Total Kas Keluar ($cashFlowPeriodLabel)" to formatRupiah(totalCashOut),
                    "Khusus Pengeluaran Operasional" to formatRupiah(totalExpenseFlow),
                    "Selisih Arus Kas (Net Cash Flow)" to formatRupiah(totalCashIn - totalCashOut)
                ),
                rows = if (flowRows.isEmpty()) listOf("Belum ada catatan arus uang ($cashFlowFilterLabel) pada bulan $cashFlowPeriodLabel.") else flowRows
            )
        )
    }

    fun printSingleSectionF4(sectionIndex: Int) {
        val sections = buildAllReportSections()
        val chosen = sections[sectionIndex.coerceIn(0, sections.lastIndex)]
        PayslipPrinter.printReportPdf(
            context = context,
            storeSettings = storeSettings,
            reportTitle = chosen.title,
            periodSubtitle = "Ukuran Kertas F4 (215 x 330 mm)",
            sections = listOf(chosen)
        )
    }

    ResponsiveScreenContainer {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Laporan Bisnis AFI",
                    style = MaterialTheme.typography.headlineSmall
                )
                OutlinedButton(
                    onClick = { showExportDialog = true },
                    modifier = Modifier.testTag("export_report_pdf_button")
                ) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("PDF F4")
                }
            }
            ScrollableTabRow(selectedTabIndex = selectedTab, edgePadding = 12.dp) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Harian") }, modifier = Modifier.testTag("tab_report_daily"))
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Bulanan") }, modifier = Modifier.testTag("tab_report_monthly"))
                Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("Tahunan") }, modifier = Modifier.testTag("tab_report_yearly"))
                Tab(selected = selectedTab == 3, onClick = { selectedTab = 3 }, text = { Text("Laba") }, modifier = Modifier.testTag("tab_report_profit"))
                Tab(selected = selectedTab == 4, onClick = { selectedTab = 4 }, text = { Text("Jual Rugi") }, modifier = Modifier.testTag("tab_report_loss"))
                Tab(selected = selectedTab == 5, onClick = { selectedTab = 5 }, text = { Text("Gaji Bulanan") }, modifier = Modifier.testTag("tab_report_payroll"))
                Tab(selected = selectedTab == 6, onClick = { selectedTab = 6 }, text = { Text("Arus Uang") }, modifier = Modifier.testTag("tab_report_cashflow"))
            }

            when (selectedTab) {
                0 -> DailyReportTab(
                    sales = sales,
                    purchases = purchases,
                    expenses = expenses,
                    payslips = payslips,
                    selectedDateMillis = dailyDateMillis,
                    onDateChange = { dailyDateMillis = it },
                    onPrintF4 = { printSingleSectionF4(0) }
                )
                1 -> MonthlyReportTab(
                    sales = sales,
                    purchases = purchases,
                    expenses = expenses,
                    payslips = payslips,
                    year = monthlyYear,
                    month = monthlyMonth,
                    onMonthChange = { y, m -> monthlyYear = y; monthlyMonth = m },
                    onPrintF4 = { printSingleSectionF4(1) }
                )
                2 -> YearlyReportTab(
                    sales = sales,
                    purchases = purchases,
                    expenses = expenses,
                    payslips = payslips,
                    year = yearlyYear,
                    onYearChange = { yearlyYear = it },
                    onPrintF4 = { printSingleSectionF4(2) }
                )
                3 -> ProfitReportTab(
                    sales = sales,
                    expenses = expenses,
                    payslips = payslips,
                    year = profitYear,
                    month = profitMonth,
                    onMonthChange = { y, m -> profitYear = y; profitMonth = m },
                    onPrintF4 = { printSingleSectionF4(3) }
                )
                4 -> LossSalesReportTab(
                    sales = sales,
                    year = lossYear,
                    month = lossMonth,
                    onMonthChange = { y, m -> lossYear = y; lossMonth = m },
                    onPrintF4 = { printSingleSectionF4(4) }
                )
                5 -> MonthlyPayrollReportTab(
                    payslips = payslips,
                    year = payrollYear,
                    month = payrollMonth,
                    onMonthChange = { y, m -> payrollYear = y; payrollMonth = m },
                    onPrintF4 = { printSingleSectionF4(5) }
                )
                6 -> CashFlowReportTab(
                    cashFlows = cashFlows,
                    year = cashFlowYear,
                    month = cashFlowMonth,
                    filter = cashFlowFilter,
                    onMonthChange = { y, m -> cashFlowYear = y; cashFlowMonth = m },
                    onFilterChange = { cashFlowFilter = it },
                    onPrintF4 = { printSingleSectionF4(6) }
                )
            }
        }
    }

    if (showExportDialog) {
        val allSections = remember(
            sales, purchases, expenses, payslips, cashFlows,
            dailyDateMillis, monthlyYear, monthlyMonth, yearlyYear,
            profitYear, profitMonth, lossYear, lossMonth,
            payrollYear, payrollMonth, cashFlowYear, cashFlowMonth, cashFlowFilter
        ) {
            buildAllReportSections()
        }
        val activeSectionIndex = selectedTab.coerceIn(0, allSections.lastIndex)
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            title = { Text("Cetak Laporan PDF (Kertas F4)") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Dokumen PDF diformat ke ukuran kertas F4 (215 x 330 mm). Pilih cakupan cetak:",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Button(
                        onClick = {
                            showExportDialog = false
                            PayslipPrinter.printReportPdf(
                                context = context,
                                storeSettings = storeSettings,
                                reportTitle = "LAPORAN LENGKAP & GAJI KARYAWAN SEMUA SUB-MENU",
                                periodSubtitle = "Kertas F4 (215 x 330 mm) • Harian, Bulanan, Tahunan, Laba, Jual Rugi, Gaji Bulanan & Arus Uang",
                                sections = allSections
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("export_all_reports_pdf_button")
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Semua Sub-Menu (F4)")
                    }
                    OutlinedButton(
                        onClick = {
                            showExportDialog = false
                            printSingleSectionF4(activeSectionIndex)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("export_current_report_pdf_button")
                    ) {
                        Text("Sub-Menu Aktif (${allSections[activeSectionIndex].title.substringAfter(". ").take(24)})")
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showExportDialog = false }) { Text("Batal") }
            }
        )
    }
}

@Composable
private fun PrintF4SubmenuButton(label: String, tag: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .testTag(tag)
    ) {
        Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(label)
    }
}

@Composable
private fun MonthlyPayrollReportTab(
    payslips: List<Payslip>,
    year: Int,
    month: Int,
    onMonthChange: (Int, Int) -> Unit,
    onPrintF4: () -> Unit
) {
    val monthSlips = remember(payslips, year, month) {
        payslips.filter { slip ->
            val workMillis = slip.workDateMillis.takeIf { it > 0L }
                ?: slip.salaryDateMillis.takeIf { it > 0L }
                ?: slip.createdAtMillis
            isSameMonth(workMillis, year, month)
        }
    }
    val totalPayroll = monthSlips.sumOf { it.totalAmount }
    val paidPayroll = monthSlips.sumOf { it.effectivePaidAmount() }
    val unpaidPayroll = monthSlips.sumOf { it.remainingAmount() }

    val byEmployee = remember(monthSlips) {
        monthSlips.groupBy { it.employeeName }
            .entries
            .sortedBy { it.key.lowercase() }
            .map { (name, list) ->
                val p = list.sumOf { it.effectivePaidAmount() }
                val u = list.sumOf { it.remainingAmount() }
                Triple(name, list.size, p to u)
            }
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            DateNavigatorCard(
                label = formatMonthYear(year, month),
                onPrev = {
                    if (month == 0) onMonthChange(year - 1, 11) else onMonthChange(year, month - 1)
                },
                onNext = {
                    if (month == 11) onMonthChange(year + 1, 0) else onMonthChange(year, month + 1)
                }
            )
        }
        item {
            PrintF4SubmenuButton(
                label = "Cetak Laporan Gaji Bulanan (PDF F4)",
                tag = "print_f4_payroll_button",
                onClick = onPrintF4
            )
        }
        item {
            AdaptiveTwoColumnRow(
                first = {
                    StatKpiCard("Gaji Dibayar", formatRupiah(paidPayroll), "${monthSlips.count { it.paid }} slip", Icons.Default.Badge, ProfitGreen)
                },
                second = {
                    StatKpiCard("Belum Dibayar", formatRupiah(unpaidPayroll), "${monthSlips.count { !it.paid }} slip", Icons.Default.Badge, LossRed)
                }
            )
        }
        item {
            StatKpiCard(
                title = "Total Gaji Karyawan (${formatMonthYear(year, month)})",
                value = formatRupiah(totalPayroll),
                subtitle = "${byEmployee.size} karyawan • ${monthSlips.size} slip borongan",
                icon = Icons.Default.Badge,
                accentColor = InfoBlue
            )
        }
        if (byEmployee.isEmpty()) {
            item {
                EmptyStateCard(
                    title = "Belum ada gaji bulan ${formatMonthYear(year, month)}",
                    subtitle = "Transaksi gaji karyawan di bulan ini akan direkap otomatis per karyawan.",
                    icon = Icons.Default.Badge
                )
            }
        } else {
            items(byEmployee, key = { it.first }) { (empName, count, amounts) ->
                val (empPaid, empUnpaid) = amounts
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(empName, style = MaterialTheme.typography.titleSmall)
                            Text(
                                "$count hari/slip • Total ${formatRupiah(empPaid + empUnpaid)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "Dibayar: ${formatRupiah(empPaid)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = ProfitGreen
                            )
                        }
                        if (empUnpaid <= 0.0) {
                            StatusBadge("DIBAYAR", ProfitGreenBg, ProfitGreen)
                        } else {
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Belum Bayar", style = MaterialTheme.typography.labelSmall, color = LossRed)
                                Text(
                                    formatRupiah(empUnpaid),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = LossRed
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CashFlowReportTab(
    cashFlows: List<CashFlow>,
    year: Int,
    month: Int,
    filter: String,
    onMonthChange: (Int, Int) -> Unit,
    onFilterChange: (String) -> Unit,
    onPrintF4: () -> Unit
) {
    val periodLabel = formatMonthYear(year, month)
    val monthFlows = remember(cashFlows, year, month) {
        cashFlows.filter { isSameMonth(it.transactionDateMillis, year, month) }
    }
    val totalIn = monthFlows.filter { it.flowType == "IN" }.sumOf { it.amount }
    val totalOut = monthFlows.filter { it.flowType == "OUT" }.sumOf { it.amount }
    val netFlow = totalIn - totalOut

    val filtered = remember(monthFlows, filter) {
        monthFlows.filter { flow ->
            when (filter) {
                "IN" -> flow.flowType == "IN"
                "OUT" -> flow.flowType == "OUT"
                "EXPENSE" -> flow.category == "PENGELUARAN"
                "TRANSFER" -> flow.category == "PINDAH_SALDO"
                else -> true
            }
        }
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            DateNavigatorCard(
                label = periodLabel,
                onPrev = {
                    if (month == 0) onMonthChange(year - 1, 11) else onMonthChange(year, month - 1)
                },
                onNext = {
                    if (month == 11) onMonthChange(year + 1, 0) else onMonthChange(year, month + 1)
                }
            )
        }
        item {
            PrintF4SubmenuButton(
                label = "Cetak Arus Uang $periodLabel (PDF F4)",
                tag = "print_f4_cashflow_button",
                onClick = onPrintF4
            )
        }
        item {
            AdaptiveTwoColumnRow(
                first = { StatKpiCard("Kas Masuk", formatRupiah(totalIn), null, Icons.AutoMirrored.Filled.TrendingUp, ProfitGreen) },
                second = { StatKpiCard("Kas Keluar", formatRupiah(totalOut), null, Icons.AutoMirrored.Filled.TrendingDown, LossRed) }
            )
        }
        item {
            StatKpiCard(
                title = "Selisih Bersih Arus Uang ($periodLabel)",
                value = formatRupiah(netFlow),
                subtitle = "${monthFlows.size} transaksi arus uang bulan ini",
                icon = Icons.Default.AccountBalanceWallet,
                accentColor = if (netFlow >= 0) ProfitGreen else LossRed
            )
        }
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(selected = filter == "ALL", onClick = { onFilterChange("ALL") }, label = { Text("Semua (${monthFlows.size})") })
                FilterChip(selected = filter == "IN", onClick = { onFilterChange("IN") }, label = { Text("Masuk") })
                FilterChip(selected = filter == "OUT", onClick = { onFilterChange("OUT") }, label = { Text("Keluar") })
                FilterChip(selected = filter == "EXPENSE", onClick = { onFilterChange("EXPENSE") }, label = { Text("Pengeluaran") })
                FilterChip(selected = filter == "TRANSFER", onClick = { onFilterChange("TRANSFER") }, label = { Text("Pindah Saldo") })
            }
        }
        if (filtered.isEmpty()) {
            item {
                EmptyStateCard(
                    title = "Belum ada arus uang bulan $periodLabel",
                    subtitle = "Transaksi kas masuk & keluar di bulan ini akan tampil di sini dan siap diekspor ke PDF F4."
                )
            }
        } else {
            items(filtered, key = { it.id }) { flow ->
                val isIn = flow.flowType == "IN"
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                StatusBadge(
                                    text = if (isIn) "MASUK" else "KELUAR",
                                    containerColor = if (isIn) ProfitGreenBg else LossRedBg,
                                    contentColor = if (isIn) ProfitGreen else LossRed
                                )
                                Text(flow.category.replace("_", " "), style = MaterialTheme.typography.labelSmall)
                            }
                            Text(flow.description.ifBlank { flow.category }, style = MaterialTheme.typography.titleSmall)
                            Text(
                                "${flow.cashBoxName} • ${formatDateTime(flow.transactionDateMillis)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        CurrencyText(
                            amount = flow.amount,
                            color = if (isIn) ProfitGreen else LossRed,
                            style = MaterialTheme.typography.titleSmall
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DailyReportTab(
    sales: List<Sale>,
    purchases: List<Purchase>,
    expenses: List<Expense>,
    payslips: List<Payslip>,
    selectedDateMillis: Long,
    onDateChange: (Long) -> Unit,
    onPrintF4: () -> Unit
) {
    val dayMillis = 86_400_000L

    val daySales = sales.filter { isSameDay(it.transactionDateMillis, selectedDateMillis) }
    val dayPurchases = purchases.filter { isSameDay(it.transactionDateMillis, selectedDateMillis) }
    val dayExpenses = expenses.filter { isSameDay(it.transactionDateMillis, selectedDateMillis) }
    val dayPayroll = payslips.filter { it.effectivePaidAmount() > 0.0 && isSameDay(it.paidAtMillis, selectedDateMillis) }

    val omzet = daySales.sumOf { it.totalAmount }
    val profitKotor = daySales.sumOf { it.totalProfit }
    val totalPurchase = dayPurchases.sumOf { it.totalAmount }
    val totalExpense = dayExpenses.sumOf { it.amount }
    val totalPayroll = dayPayroll.sumOf { it.effectivePaidAmount() }
    val labaBersih = profitKotor - totalExpense - totalPayroll

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            DateNavigatorCard(
                label = formatDate(selectedDateMillis),
                onPrev = { onDateChange(selectedDateMillis - dayMillis) },
                onNext = { onDateChange(selectedDateMillis + dayMillis) }
            )
        }
        item {
            PrintF4SubmenuButton(
                label = "Cetak Laporan Harian (PDF F4)",
                tag = "print_f4_daily_button",
                onClick = onPrintF4
            )
        }
        item {
            AdaptiveTwoColumnRow(
                first = {
                    StatKpiCard("Penjualan", formatRupiah(omzet), "${daySales.size} nota", Icons.Default.PointOfSale, ProfitGreen)
                },
                second = {
                    StatKpiCard("Pembelian", formatRupiah(totalPurchase), "${dayPurchases.size} nota", Icons.Default.AddShoppingCart, InfoBlue)
                }
            )
        }
        item {
            AdaptiveTwoColumnRow(
                first = {
                    StatKpiCard("Pengeluaran", formatRupiah(totalExpense), "${dayExpenses.size} catatan", Icons.Default.MoneyOff, LossRed)
                },
                second = {
                    StatKpiCard("Gaji Dibayar", formatRupiah(totalPayroll), "${dayPayroll.size} slip", Icons.Default.Badge, WarningAmber)
                }
            )
        }
        item {
            NetProfitBannerCard("Estimasi Laba Bersih Hari Ini", labaBersih)
        }
    }
}

@Composable
private fun MonthlyReportTab(
    sales: List<Sale>,
    purchases: List<Purchase>,
    expenses: List<Expense>,
    payslips: List<Payslip>,
    year: Int,
    month: Int,
    onMonthChange: (Int, Int) -> Unit,
    onPrintF4: () -> Unit
) {
    val mSales = sales.filter { isSameMonth(it.transactionDateMillis, year, month) }
    val mPurchases = purchases.filter { isSameMonth(it.transactionDateMillis, year, month) }
    val mExpenses = expenses.filter { isSameMonth(it.transactionDateMillis, year, month) }
    val mPayroll = payslips.filter { it.effectivePaidAmount() > 0.0 && isSameMonth(it.paidAtMillis, year, month) }

    val omzet = mSales.sumOf { it.totalAmount }
    val grossProfit = mSales.sumOf { it.totalProfit }
    val totalPurchase = mPurchases.sumOf { it.totalAmount }
    val totalExpense = mExpenses.sumOf { it.amount }
    val totalPayroll = mPayroll.sumOf { it.effectivePaidAmount() }
    val netProfit = grossProfit - totalExpense - totalPayroll

    val weekBuckets = (1..4).map { week ->
        val weekTotal = mSales.filter { s ->
            val c = Calendar.getInstance().apply { timeInMillis = s.transactionDateMillis }
            val day = c.get(Calendar.DAY_OF_MONTH)
            val w = ((day - 1) / 8) + 1
            w.coerceAtMost(4) == week
        }.sumOf { it.totalAmount }
        "Mgg $week" to weekTotal
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            DateNavigatorCard(
                label = formatMonthYear(year, month),
                onPrev = {
                    if (month == 0) onMonthChange(year - 1, 11) else onMonthChange(year, month - 1)
                },
                onNext = {
                    if (month == 11) onMonthChange(year + 1, 0) else onMonthChange(year, month + 1)
                }
            )
        }
        item {
            PrintF4SubmenuButton(
                label = "Cetak Laporan Bulanan (PDF F4)",
                tag = "print_f4_monthly_button",
                onClick = onPrintF4
            )
        }
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = MaterialTheme.shapes.medium
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Grafik Penjualan Mingguan", style = MaterialTheme.typography.titleSmall)
                    SimpleBarChart(bars = weekBuckets, barColor = MaterialTheme.colorScheme.primary)
                }
            }
        }
        item {
            AdaptiveTwoColumnRow(
                first = { StatKpiCard("Omzet Bulanan", formatRupiah(omzet), "${mSales.size} penjualan", Icons.Default.PointOfSale, ProfitGreen) },
                second = { StatKpiCard("Pembelian", formatRupiah(totalPurchase), "${mPurchases.size} pembelian", Icons.Default.AddShoppingCart, InfoBlue) }
            )
        }
        item {
            AdaptiveTwoColumnRow(
                first = { StatKpiCard("Pengeluaran", formatRupiah(totalExpense), null, Icons.Default.MoneyOff, LossRed) },
                second = { StatKpiCard("Gaji Pabrik", formatRupiah(totalPayroll), null, Icons.Default.Badge, WarningAmber) }
            )
        }
        item {
            NetProfitBannerCard("Laba Bersih Bulanan", netProfit)
        }
    }
}

@Composable
private fun YearlyReportTab(
    sales: List<Sale>,
    purchases: List<Purchase>,
    expenses: List<Expense>,
    payslips: List<Payslip>,
    year: Int,
    onYearChange: (Int) -> Unit,
    onPrintF4: () -> Unit
) {
    val ySales = sales.filter { isSameYear(it.transactionDateMillis, year) }
    val yPurchases = purchases.filter { isSameYear(it.transactionDateMillis, year) }
    val yExpenses = expenses.filter { isSameYear(it.transactionDateMillis, year) }
    val yPayroll = payslips.filter { it.effectivePaidAmount() > 0.0 && isSameYear(it.paidAtMillis, year) }

    val shortMonths = listOf("J", "F", "M", "A", "M", "J", "J", "A", "S", "O", "N", "D")
    val monthBars = shortMonths.mapIndexed { mIdx, label ->
        val total = ySales.filter { isSameMonth(it.transactionDateMillis, year, mIdx) }.sumOf { it.totalAmount }
        label to total
    }

    val omzet = ySales.sumOf { it.totalAmount }
    val grossProfit = ySales.sumOf { it.totalProfit }
    val totalExpense = yExpenses.sumOf { it.amount }
    val totalPayroll = yPayroll.sumOf { it.effectivePaidAmount() }
    val netProfit = grossProfit - totalExpense - totalPayroll

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            DateNavigatorCard(
                label = "Tahun $year",
                onPrev = { onYearChange(year - 1) },
                onNext = { onYearChange(year + 1) }
            )
        }
        item {
            PrintF4SubmenuButton(
                label = "Cetak Laporan Tahunan (PDF F4)",
                tag = "print_f4_yearly_button",
                onClick = onPrintF4
            )
        }
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = MaterialTheme.shapes.medium
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Tren Penjualan $year", style = MaterialTheme.typography.titleSmall)
                    SimpleBarChart(bars = monthBars, barColor = MaterialTheme.colorScheme.tertiary)
                }
            }
        }
        item {
            AdaptiveTwoColumnRow(
                first = { StatKpiCard("Omzet Tahunan", formatRupiah(omzet), "${ySales.size} nota", Icons.Default.PointOfSale, ProfitGreen) },
                second = { StatKpiCard("Total Pembelian", formatRupiah(yPurchases.sumOf { it.totalAmount }), null, Icons.Default.AddShoppingCart, InfoBlue) }
            )
        }
        item {
            AdaptiveTwoColumnRow(
                first = { StatKpiCard("Pengeluaran", formatRupiah(totalExpense), null, Icons.Default.MoneyOff, LossRed) },
                second = { StatKpiCard("Gaji Karyawan", formatRupiah(totalPayroll), null, Icons.Default.Badge, WarningAmber) }
            )
        }
        item {
            NetProfitBannerCard("Laba Bersih Tahun $year", netProfit)
        }
    }
}

@Composable
private fun ProfitReportTab(
    sales: List<Sale>,
    expenses: List<Expense>,
    payslips: List<Payslip>,
    year: Int,
    month: Int,
    onMonthChange: (Int, Int) -> Unit,
    onPrintF4: () -> Unit
) {
    val periodLabel = formatMonthYear(year, month)
    val monthSales = remember(sales, year, month) {
        sales.filter { isSameMonth(it.transactionDateMillis, year, month) }
    }
    val monthExpenses = remember(expenses, year, month) {
        expenses.filter { isSameMonth(it.transactionDateMillis, year, month) }
    }
    val monthPayroll = remember(payslips, year, month) {
        payslips.filter { it.effectivePaidAmount() > 0.0 && isSameMonth(it.paidAtMillis, year, month) }
    }

    val totalOmzet = monthSales.sumOf { it.totalAmount }
    val totalHpp = monthSales.sumOf { it.totalCost }
    val grossProfit = monthSales.sumOf { it.totalProfit }
    val totalExpense = monthExpenses.sumOf { it.amount }
    val totalPayroll = monthPayroll.sumOf { it.effectivePaidAmount() }
    val netProfit = grossProfit - totalExpense - totalPayroll

    val productProfitMap = remember(monthSales) {
        monthSales.flatMap { it.items }
            .groupBy { it.productName }
            .map { (name, list) ->
                Triple(name, list.sumOf { it.quantity }, list.sumOf { it.profit })
            }
            .sortedByDescending { it.third }
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            DateNavigatorCard(
                label = periodLabel,
                onPrev = {
                    if (month == 0) onMonthChange(year - 1, 11) else onMonthChange(year, month - 1)
                },
                onNext = {
                    if (month == 11) onMonthChange(year + 1, 0) else onMonthChange(year, month + 1)
                }
            )
        }
        item {
            PrintF4SubmenuButton(
                label = "Cetak Laba Rugi $periodLabel (PDF F4)",
                tag = "print_f4_profit_button",
                onClick = onPrintF4
            )
        }
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Ringkasan Laba Rugi ($periodLabel)", style = MaterialTheme.typography.titleMedium)
                    ReportLine("Total Omzet Penjualan", totalOmzet, ProfitGreen)
                    ReportLine("Harga Pokok Modal (HPP)", -totalHpp, MaterialTheme.colorScheme.onSurfaceVariant)
                    HorizontalDivider()
                    ReportLine("Laba Kotor Penjualan", grossProfit, InfoBlue)
                    ReportLine("Beban Pengeluaran", -totalExpense, LossRed)
                    ReportLine("Gaji Karyawan Dibayar", -totalPayroll, WarningAmber)
                    HorizontalDivider()
                    ReportLine("Laba Bersih", netProfit, if (netProfit >= 0) ProfitGreen else LossRed, bold = true)
                }
            }
        }
        item {
            Text("Laba per Produk ($periodLabel)", style = MaterialTheme.typography.titleMedium)
        }
        if (productProfitMap.isEmpty()) {
            item {
                EmptyStateCard(
                    title = "Belum ada data laba bulan $periodLabel",
                    subtitle = "Catat penjualan pada bulan ini untuk melihat analisis laba per produk."
                )
            }
        } else {
            items(productProfitMap, key = { it.first }) { (name, qty, prof) ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(name, style = MaterialTheme.typography.titleSmall)
                            Text("Terjual: ${formatQty(qty)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        CurrencyText(amount = prof, color = if (prof >= 0) ProfitGreen else LossRed)
                    }
                }
            }
        }
    }
}

@Composable
private fun LossSalesReportTab(
    sales: List<Sale>,
    year: Int,
    month: Int,
    onMonthChange: (Int, Int) -> Unit,
    onPrintF4: () -> Unit
) {
    val periodLabel = formatMonthYear(year, month)
    val lossSales = remember(sales, year, month) {
        sales.filter { sale ->
            isSameMonth(sale.transactionDateMillis, year, month) &&
                (sale.lossAmount > 0.0 || sale.items.any { it.sellPrice < it.buyPrice })
        }
    }
    val totalLoss = lossSales.sumOf { sale ->
        if (sale.lossAmount > 0.0) sale.lossAmount
        else sale.items.filter { it.sellPrice < it.buyPrice }.sumOf { (it.buyPrice - it.sellPrice) * it.quantity }
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            DateNavigatorCard(
                label = periodLabel,
                onPrev = {
                    if (month == 0) onMonthChange(year - 1, 11) else onMonthChange(year, month - 1)
                },
                onNext = {
                    if (month == 11) onMonthChange(year + 1, 0) else onMonthChange(year, month + 1)
                }
            )
        }
        item {
            PrintF4SubmenuButton(
                label = "Cetak Jual Rugi $periodLabel (PDF F4)",
                tag = "print_f4_loss_button",
                onClick = onPrintF4
            )
        }
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = LossRedBg),
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Total Jual Rugi ($periodLabel)", style = MaterialTheme.typography.titleSmall, color = LossRed)
                        Text("${lossSales.size} transaksi terdeteksi jual rugi", style = MaterialTheme.typography.bodySmall, color = LossRed)
                    }
                    CurrencyText(amount = totalLoss, color = LossRed)
                }
            }
        }
        if (lossSales.isEmpty()) {
            item {
                EmptyStateCard(
                    title = "Tidak ada Jual Rugi bulan $periodLabel",
                    subtitle = "Semua transaksi penjualan pada bulan ini berada pada harga modal atau menghasilkan laba.",
                    icon = Icons.AutoMirrored.Filled.TrendingUp
                )
            }
        } else {
            items(lossSales, key = { it.id }) { sale ->
                val lossItems = sale.items.filter { it.sellPrice < it.buyPrice }
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(sale.customerName, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    "${sale.invoiceNo} • ${formatDateTime(sale.transactionDateMillis)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            StatusBadge("RUGI", LossRedBg, LossRed)
                        }
                        lossItems.forEach { item ->
                            val itemLoss = (item.buyPrice - item.sellPrice) * item.quantity
                            Text(
                                "• ${item.productName}: Jual ${formatRupiah(item.sellPrice)} < Modal ${formatRupiah(item.buyPrice)} (${formatQty(item.quantity)} ${item.unit}) -> Rugi ${formatRupiah(itemLoss)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = LossRed
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DateNavigatorCard(label: String, onPrev: () -> Unit, onNext: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onPrev) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Sebelumnya")
            }
            Text(label, style = MaterialTheme.typography.titleMedium)
            IconButton(onClick = onNext) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Berikutnya")
            }
        }
    }
}

@Composable
private fun NetProfitBannerCard(title: String, amount: Double) {
    val isProfit = amount >= 0
    StatKpiCard(
        title = title,
        value = formatRupiah(amount),
        subtitle = if (isProfit) "Bisnis menghasilkan keuntungan" else "Pengeluaran & gaji melebihi laba kotor",
        icon = if (isProfit) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown,
        accentColor = if (isProfit) ProfitGreen else LossRed
    )
}

@Composable
private fun ReportLine(label: String, amount: Double, color: androidx.compose.ui.graphics.Color, bold: Boolean = false) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(
            text = label,
            style = if (bold) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodyMedium
        )
        CurrencyText(
            amount = amount,
            color = color,
            style = if (bold) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium
        )
    }
}
