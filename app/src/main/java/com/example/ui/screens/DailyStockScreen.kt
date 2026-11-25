package com.example.ui.screens

import android.app.DatePickerDialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.Product
import com.example.data.model.Purchase
import com.example.data.model.PurchaseReturn
import com.example.data.model.Sale
import com.example.data.model.StoreSettings
import com.example.data.model.UiState
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.LoadingCenter
import com.example.ui.components.ResponsiveScreenContainer
import com.example.ui.components.SearchBarField
import com.example.ui.components.StatusBadge
import com.example.ui.theme.InfoBlue
import com.example.ui.theme.LossRed
import com.example.ui.theme.LossRedBg
import com.example.ui.theme.ProfitGreen
import com.example.ui.theme.ProfitGreenBg
import com.example.ui.theme.SlateBarText
import com.example.ui.theme.WarningAmber
import com.example.ui.theme.WarningAmberBg
import com.example.util.PayslipPrinter
import com.example.util.ReportPdfSection
import com.example.util.formatDate
import com.example.util.formatDateSlash
import com.example.util.formatDateTimeSlash
import com.example.util.formatQty
import com.example.util.formatRupiah
import com.example.util.isSameDay
import java.util.Calendar
import kotlin.math.max

private enum class DailyStockFilter(val label: String, val tag: String) {
    ALL("Semua", "daily_stock_filter_all"),
    MOVED("Ada Mutasi", "daily_stock_filter_moved"),
    LOW("Stok Menipis", "daily_stock_filter_low")
}

private data class DailyStockMovement(
    val timestampMillis: Long,
    val typeLabel: String,
    val reference: String,
    val partyName: String,
    val deltaQty: Double,
    val color: Color
)

private data class DailyStockRow(
    val product: Product,
    val startStock: Double,
    val inQty: Double,
    val grossSaleQty: Double,
    val outSaleQty: Double,
    val outReturnQty: Double,
    val endStock: Double,
    val movements: List<DailyStockMovement>
) {
    val hasMovement: Boolean
        get() = inQty > 0.0 || grossSaleQty > 0.0 || outReturnQty > 0.0
}

@Composable
fun DailyStockScreen(
    productsState: UiState<List<Product>>,
    salesState: UiState<List<Sale>>,
    purchasesState: UiState<List<Purchase>>,
    returnsState: UiState<List<PurchaseReturn>>,
    storeSettingsState: UiState<StoreSettings>,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var selectedDateMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var search by remember { mutableStateOf("") }
    var activeFilter by remember { mutableStateOf(DailyStockFilter.ALL) }
    var inspectingRow by remember { mutableStateOf<DailyStockRow?>(null) }

    val products = (productsState as? UiState.Success)?.data ?: emptyList()
    val sales = (salesState as? UiState.Success)?.data ?: emptyList()
    val purchases = (purchasesState as? UiState.Success)?.data ?: emptyList()
    val returns = (returnsState as? UiState.Success)?.data ?: emptyList()
    val storeSettings = (storeSettingsState as? UiState.Success)?.data ?: StoreSettings()

    val (dayStart, dayEnd) = remember(selectedDateMillis) { dayBounds(selectedDateMillis) }
    val isToday = remember(selectedDateMillis) { isSameDay(selectedDateMillis, System.currentTimeMillis()) }

    val dailyRows = remember(products, sales, purchases, returns, dayStart, dayEnd) {
        buildDailyStockRows(
            products = products,
            sales = sales,
            purchases = purchases,
            returns = returns,
            dayStart = dayStart,
            dayEnd = dayEnd
        )
    }

    val filteredRows = remember(dailyRows, search, activeFilter) {
        val q = search.trim()
        dailyRows.filter { row ->
            val matchesSearch = q.isBlank() ||
                row.product.name.contains(q, ignoreCase = true) ||
                row.product.sku.contains(q, ignoreCase = true) ||
                row.product.category.contains(q, ignoreCase = true) ||
                row.product.unit.contains(q, ignoreCase = true)
            val matchesFilter = when (activeFilter) {
                DailyStockFilter.ALL -> true
                DailyStockFilter.MOVED -> row.hasMovement
                DailyStockFilter.LOW -> row.endStock <= row.product.minStock
            }
            matchesSearch && matchesFilter
        }
    }

    val totalIn = remember(dailyRows) { dailyRows.sumOf { it.inQty } }
    val totalOutSale = remember(dailyRows) { dailyRows.sumOf { it.outSaleQty } }
    val totalOutReturn = remember(dailyRows) { dailyRows.sumOf { it.outReturnQty } }
    val movedCount = remember(dailyRows) { dailyRows.count { it.hasMovement } }
    val totalEndStock = remember(dailyRows) { dailyRows.sumOf { it.endStock } }
    val totalEndAssetValue = remember(dailyRows) { dailyRows.sumOf { it.endStock * it.product.buyPrice } }

    fun openDatePicker() {
        val cal = Calendar.getInstance().apply { timeInMillis = selectedDateMillis }
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val picked = Calendar.getInstance().apply {
                    set(year, month, dayOfMonth, 12, 0, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                selectedDateMillis = picked.timeInMillis
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    fun exportDailyStockPdf() {
        val sectionSummary = listOf(
            "Tanggal" to formatDate(selectedDateMillis),
            "Total Produk" to "${filteredRows.size} produk",
            "Produk Bergerak" to "$movedCount produk",
            "Barang Masuk (Pembelian)" to "+${formatQty(totalIn)}",
            "Stok Keluar (Penjualan - Retur)" to "-${formatQty(totalOutSale)}",
            "Retur & Jual Rugi" to "-${formatQty(totalOutReturn)}",
            "Total Stok Akhir" to formatQty(totalEndStock),
            "Estimasi Nilai Modal Stok Akhir" to formatRupiah(totalEndAssetValue)
        )
        val tableRows = if (filteredRows.isEmpty()) {
            listOf("Tidak ada data produk untuk filter/tanggal ini.")
        } else {
            filteredRows.mapIndexed { idx, row ->
                "${idx + 1}. ${row.product.name} (${row.product.unit}) | " +
                    "Awal: ${formatQty(row.startStock)} | " +
                    "Masuk: +${formatQty(row.inQty)} | " +
                    "Keluar: -${formatQty(row.outSaleQty)} | " +
                    "Retur: -${formatQty(row.outReturnQty)} | " +
                    "Akhir: ${formatQty(row.endStock)}"
            }
        }
        PayslipPrinter.printReportPdf(
            context = context,
            storeSettings = storeSettings,
            reportTitle = "DATA STOK BARANG PERHARI",
            periodSubtitle = "Tanggal ${formatDate(selectedDateMillis)} (${formatDateSlash(selectedDateMillis)})",
            sections = listOf(
                ReportPdfSection(
                    title = "Ringkasan & Rincian Stok Harian",
                    summaryPairs = sectionSummary,
                    rows = tableRows
                )
            )
        )
    }

    ResponsiveScreenContainer {
        Column(modifier = Modifier.fillMaxSize()) {
            SlateTopHeaderBar(
                title = "Data Stok Barang Perhari",
                onBack = onBack,
                backTag = "daily_stock_back",
                trailingAction = {
                    IconButton(
                        onClick = { exportDailyStockPdf() },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("daily_stock_print_pdf")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PictureAsPdf,
                            contentDescription = "Cetak PDF F4",
                            tint = SlateBarText
                        )
                    }
                }
            )

            when {
                productsState is UiState.Loading -> LoadingCenter()
                productsState is UiState.Error -> BoxErrorMessage(productsState.message)
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            DateNavigatorCard(
                                dateMillis = selectedDateMillis,
                                isToday = isToday,
                                onPrevDay = { selectedDateMillis = shiftDay(selectedDateMillis, -1) },
                                onNextDay = { selectedDateMillis = shiftDay(selectedDateMillis, 1) },
                                onPickDate = { openDatePicker() },
                                onResetToday = { selectedDateMillis = System.currentTimeMillis() }
                            )
                        }

                        item {
                            DailyStockSummaryRow(
                                totalIn = totalIn,
                                totalOutSale = totalOutSale,
                                totalOutReturn = totalOutReturn,
                                totalEndStock = totalEndStock
                            )
                        }

                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                SearchBarField(
                                    query = search,
                                    onQueryChange = { search = it },
                                    placeholder = "Cari nama produk, kategori, SKU...",
                                    tag = "daily_stock_search"
                                )
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    DailyStockFilter.entries.forEach { filter ->
                                        val countSuffix = when (filter) {
                                            DailyStockFilter.ALL -> " (${dailyRows.size})"
                                            DailyStockFilter.MOVED -> " ($movedCount)"
                                            DailyStockFilter.LOW -> " (${dailyRows.count { it.endStock <= it.product.minStock }})"
                                        }
                                        FilterChip(
                                            selected = activeFilter == filter,
                                            onClick = { activeFilter = filter },
                                            label = { Text("${filter.label}$countSuffix") },
                                            modifier = Modifier.testTag(filter.tag)
                                        )
                                    }
                                }
                            }
                        }

                        if (filteredRows.isEmpty()) {
                            item {
                                EmptyStateCard(
                                    title = "Tidak ada data stok",
                                    subtitle = if (products.isEmpty()) {
                                        "Tambahkan produk di menu Data Produk terlebih dahulu."
                                    } else {
                                        "Tidak ada produk yang sesuai dengan filter atau pencarian pada tanggal ini."
                                    },
                                    icon = Icons.Default.Inventory2
                                )
                            }
                        } else {
                            items(filteredRows, key = { it.product.id }) { row ->
                                DailyStockItemCard(
                                    row = row,
                                    onClick = { inspectingRow = row }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    inspectingRow?.let { row ->
        DailyStockDetailDialog(
            row = row,
            dateMillis = selectedDateMillis,
            onDismiss = { inspectingRow = null }
        )
    }
}

@Composable
private fun BoxErrorMessage(message: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        EmptyStateCard(title = "Gagal memuat data stok", subtitle = message)
    }
}

@Composable
private fun DateNavigatorCard(
    dateMillis: Long,
    isToday: Boolean,
    onPrevDay: () -> Unit,
    onNextDay: () -> Unit,
    onPickDate: () -> Unit,
    onResetToday: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                onClick = onPrevDay,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("daily_stock_prev_day")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = "Hari sebelumnya"
                )
            }

            Row(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .clickable(onClick = onPickDate)
                    .padding(horizontal = 8.dp)
                    .testTag("daily_stock_pick_date"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CalendarMonth,
                    contentDescription = "Pilih tanggal",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = formatDate(dateMillis),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (isToday) "Hari Ini • Ketuk untuk ubah" else "Ketuk untuk pilih tanggal",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (!isToday) {
                IconButton(
                    onClick = onResetToday,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("daily_stock_today_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Today,
                        contentDescription = "Kembali ke hari ini",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            IconButton(
                onClick = onNextDay,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("daily_stock_next_day")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "Hari berikutnya"
                )
            }
        }
    }
}

@Composable
private fun DailyStockSummaryRow(
    totalIn: Double,
    totalOutSale: Double,
    totalOutReturn: Double,
    totalEndStock: Double
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        StockKpiChip(
            label = "Masuk (+)",
            value = "+${formatQty(totalIn)}",
            bgColor = ProfitGreenBg,
            textColor = ProfitGreen,
            modifier = Modifier.weight(1f)
        )
        StockKpiChip(
            label = "Keluar (-)",
            value = "-${formatQty(totalOutSale)}",
            bgColor = InfoBlue.copy(alpha = 0.12f),
            textColor = InfoBlue,
            modifier = Modifier.weight(1f)
        )
        StockKpiChip(
            label = "Retur (-)",
            value = "-${formatQty(totalOutReturn)}",
            bgColor = WarningAmberBg,
            textColor = WarningAmber,
            modifier = Modifier.weight(1f)
        )
        StockKpiChip(
            label = "Stok Akhir",
            value = formatQty(totalEndStock),
            bgColor = MaterialTheme.colorScheme.primaryContainer,
            textColor = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun StockKpiChip(
    label: String,
    value: String,
    bgColor: Color,
    textColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = bgColor,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = textColor.copy(alpha = 0.85f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = textColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun DailyStockItemCard(
    row: DailyStockRow,
    onClick: () -> Unit
) {
    val product = row.product
    val isOut = row.endStock <= 0.0
    val isLow = row.endStock <= product.minStock
    val badgeBg = when {
        isOut -> LossRedBg
        isLow -> WarningAmberBg
        else -> ProfitGreenBg
    }
    val badgeColor = when {
        isOut -> LossRed
        isLow -> WarningAmber
        else -> ProfitGreen
    }
    val badgeLabel = when {
        isOut -> "Habis"
        isLow -> "Menipis"
        else -> "Aman"
    }

    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = if (row.hasMovement) {
            BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
        } else {
            null
        },
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("daily_stock_card_${product.id}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = product.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    val meta = listOfNotNull(
                        product.category.takeIf { it.isNotBlank() },
                        "Satuan: ${product.unit}",
                        if (row.movements.isNotEmpty()) "${row.movements.size} mutasi" else null
                    ).joinToString(" • ")
                    Text(
                        text = meta,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.width(8.dp))
                StatusBadge(
                    text = badgeLabel,
                    containerColor = badgeBg,
                    contentColor = badgeColor
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MetricCol(
                    label = "Awal",
                    value = formatQty(row.startStock),
                    valueColor = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                MetricCol(
                    label = "Masuk",
                    value = if (row.inQty > 0.0) "+${formatQty(row.inQty)}" else "0",
                    valueColor = if (row.inQty > 0.0) ProfitGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                MetricCol(
                    label = "Keluar",
                    value = if (row.outSaleQty > 0.0) "-${formatQty(row.outSaleQty)}" else "0",
                    valueColor = if (row.outSaleQty > 0.0) InfoBlue else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                MetricCol(
                    label = "Retur",
                    value = if (row.outReturnQty > 0.0) "-${formatQty(row.outReturnQty)}" else "0",
                    valueColor = if (row.outReturnQty > 0.0) WarningAmber else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                MetricCol(
                    label = "Akhir",
                    value = formatQty(row.endStock),
                    valueColor = if (isOut || isLow) badgeColor else MaterialTheme.colorScheme.primary,
                    bold = true,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun MetricCol(
    label: String,
    value: String,
    valueColor: Color,
    bold: Boolean = false,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.SemiBold,
            color = valueColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun DailyStockDetailDialog(
    row: DailyStockRow,
    dateMillis: Long,
    onDismiss: () -> Unit
) {
    val product = row.product
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(product.name)
                Text(
                    text = "Mutasi ${formatDate(dateMillis)} • Satuan ${product.unit}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        DetailStatLine("Stok Awal Hari", "${formatQty(row.startStock)} ${product.unit}")
                        DetailStatLine("Masuk (Pembelian)", "+${formatQty(row.inQty)} ${product.unit}", ProfitGreen)
                        if (row.grossSaleQty > 0.0 && row.outReturnQty > 0.0) {
                            DetailStatLine("Penjualan Kotor", "-${formatQty(row.grossSaleQty)} ${product.unit}", MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        DetailStatLine("Keluar (Penjualan - Retur)", "-${formatQty(row.outSaleQty)} ${product.unit}", InfoBlue)
                        DetailStatLine("Retur & Jual Rugi", "-${formatQty(row.outReturnQty)} ${product.unit}", WarningAmber)
                        HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))
                        DetailStatLine(
                            "Stok Akhir Hari",
                            "${formatQty(row.endStock)} ${product.unit}",
                            MaterialTheme.colorScheme.primary
                        )
                        DetailStatLine("Stok Saat Ini (Master)", "${formatQty(product.stock)} ${product.unit}")
                    }
                }

                Text(
                    text = "Riwayat Transaksi Barang Tanggal Ini",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )

                if (row.movements.isEmpty()) {
                    Text(
                        text = "Tidak ada transaksi keluar/masuk untuk barang ini pada ${formatDate(dateMillis)}.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    row.movements.forEach { mv ->
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            shape = MaterialTheme.shapes.small,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${mv.typeLabel} • ${mv.reference}",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "${mv.partyName} • ${formatDateTimeSlash(mv.timestampMillis)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                val sign = if (mv.deltaQty >= 0) "+" else "-"
                                Text(
                                    text = "$sign${formatQty(kotlin.math.abs(mv.deltaQty))} ${product.unit}",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = mv.color
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Tutup")
            }
        }
    )
}

@Composable
private fun DetailStatLine(
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = valueColor)
    }
}

private fun dayBounds(dateMillis: Long): Pair<Long, Long> {
    val start = Calendar.getInstance().apply {
        timeInMillis = dateMillis
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
    val end = Calendar.getInstance().apply {
        timeInMillis = dateMillis
        set(Calendar.HOUR_OF_DAY, 23)
        set(Calendar.MINUTE, 59)
        set(Calendar.SECOND, 59)
        set(Calendar.MILLISECOND, 999)
    }.timeInMillis
    return start to end
}

private fun shiftDay(dateMillis: Long, deltaDays: Int): Long {
    return Calendar.getInstance().apply {
        timeInMillis = dateMillis
        add(Calendar.DAY_OF_MONTH, deltaDays)
    }.timeInMillis
}

private fun buildDailyStockRows(
    products: List<Product>,
    sales: List<Sale>,
    purchases: List<Purchase>,
    returns: List<PurchaseReturn>,
    dayStart: Long,
    dayEnd: Long
): List<DailyStockRow> {
    return products.map { product ->
        var inOnDay = 0.0
        var outSaleOnDay = 0.0
        var outReturnOnDay = 0.0
        var deltaAfterDay = 0.0
        val movements = mutableListOf<DailyStockMovement>()

        fun matchesProduct(itemProductId: String, itemProductName: String): Boolean {
            return itemProductId == product.id ||
                (itemProductId.isBlank() && itemProductName.equals(product.name, ignoreCase = true))
        }

        purchases.forEach { purchase ->
            val tx = purchase.transactionDateMillis
            val matchedQty = purchase.items
                .filter { matchesProduct(it.productId, it.productName) }
                .sumOf { it.quantity }
            if (matchedQty > 0.0) {
                if (tx in dayStart..dayEnd) {
                    inOnDay += matchedQty
                    movements.add(
                        DailyStockMovement(
                            timestampMillis = tx,
                            typeLabel = "Pembelian",
                            reference = purchase.invoiceNo.ifBlank { "PB" },
                            partyName = purchase.supplierName.ifBlank { "Supplier" },
                            deltaQty = matchedQty,
                            color = ProfitGreen
                        )
                    )
                } else if (tx > dayEnd) {
                    deltaAfterDay += matchedQty
                }
            }
        }

        sales.forEach { sale ->
            val tx = sale.transactionDateMillis
            val matchedItems = sale.items.filter { matchesProduct(it.productId, it.productName) }
            val lossQty = matchedItems
                .filter { it.sellPrice < it.buyPrice }
                .sumOf { it.quantity }
            val normalQty = matchedItems
                .filterNot { it.sellPrice < it.buyPrice }
                .sumOf { it.quantity }
            val totalMatchedQty = lossQty + normalQty

            if (totalMatchedQty > 0.0) {
                if (tx in dayStart..dayEnd) {
                    if (normalQty > 0.0) {
                        outSaleOnDay += normalQty
                        movements.add(
                            DailyStockMovement(
                                timestampMillis = tx,
                                typeLabel = "Penjualan",
                                reference = sale.invoiceNo.ifBlank { "PJ" },
                                partyName = sale.customerName.ifBlank { "Pelanggan" },
                                deltaQty = -normalQty,
                                color = InfoBlue
                            )
                        )
                    }
                    if (lossQty > 0.0) {
                        outReturnOnDay += lossQty
                        movements.add(
                            DailyStockMovement(
                                timestampMillis = tx,
                                typeLabel = "Jual Rugi (Masuk Stok Retur)",
                                reference = sale.invoiceNo.ifBlank { "PJ" },
                                partyName = sale.customerName.ifBlank { "Pelanggan" },
                                deltaQty = -lossQty,
                                color = WarningAmber
                            )
                        )
                    }
                } else if (tx > dayEnd) {
                    deltaAfterDay -= totalMatchedQty
                }
            }
        }

        returns.forEach { ret ->
            val tx = ret.transactionDateMillis
            if (matchesProduct(ret.productId, ret.productName) && ret.quantity > 0.0) {
                if (tx in dayStart..dayEnd) {
                    outReturnOnDay += ret.quantity
                    movements.add(
                        DailyStockMovement(
                            timestampMillis = tx,
                            typeLabel = "Retur Beli",
                            reference = "RETUR",
                            partyName = ret.supplierName.ifBlank { "Supplier" },
                            deltaQty = -ret.quantity,
                            color = WarningAmber
                        )
                    )
                } else if (tx > dayEnd) {
                    deltaAfterDay -= ret.quantity
                }
            }
        }

        val actualNetOnDay = inOnDay - outSaleOnDay - outReturnOnDay
        val netOutSaleOnDay = max(0.0, outSaleOnDay - outReturnOnDay)
        val endStock = max(0.0, product.stock - deltaAfterDay)
        val startStock = max(0.0, endStock - actualNetOnDay)

        DailyStockRow(
            product = product,
            startStock = startStock,
            inQty = inOnDay,
            grossSaleQty = outSaleOnDay,
            outSaleQty = netOutSaleOnDay,
            outReturnQty = outReturnOnDay,
            endStock = endStock,
            movements = movements.sortedByDescending { it.timestampMillis }
        )
    }.sortedWith(compareByDescending<DailyStockRow> { it.hasMovement }.thenBy { it.product.name.lowercase() })
}
