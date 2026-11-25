package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.AssignmentReturn
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.MoneyOff
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.RequestQuote
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.model.DebtRecord
import com.example.data.model.Expense
import com.example.data.model.Payslip
import com.example.data.model.Product
import com.example.data.model.Purchase
import com.example.data.model.PurchaseReturn
import com.example.data.model.Sale
import com.example.data.model.UiState
import com.example.ui.components.ResponsiveScreenContainer
import com.example.ui.theme.InfoBlue
import com.example.ui.theme.LossRed
import com.example.ui.theme.ProfitGreen
import com.example.ui.theme.WarningAmber
import com.example.util.formatRupiah
import com.example.util.isSameDay

private data class HubMenuEntry(
    val section: String,
    val title: String,
    val subtitle: String,
    val badgeText: String,
    val icon: ImageVector,
    val tint: Color,
    val route: String,
    val tag: String
)

@Composable
fun TransactionsHubScreen(
    salesState: UiState<List<Sale>>,
    purchasesState: UiState<List<Purchase>>,
    returnsState: UiState<List<PurchaseReturn>>,
    expensesState: UiState<List<Expense>>,
    debtsState: UiState<List<DebtRecord>>,
    productsState: UiState<List<Product>>,
    payslipsState: UiState<List<Payslip>>,
    onNavigate: (String) -> Unit
) {
    val sales = (salesState as? UiState.Success)?.data ?: emptyList()
    val purchases = (purchasesState as? UiState.Success)?.data ?: emptyList()
    val returns = (returnsState as? UiState.Success)?.data ?: emptyList()
    val expenses = (expensesState as? UiState.Success)?.data ?: emptyList()
    val debts = (debtsState as? UiState.Success)?.data ?: emptyList()
    val products = (productsState as? UiState.Success)?.data ?: emptyList()
    val payslips = (payslipsState as? UiState.Success)?.data ?: emptyList()

    val openHutang = debts.filter { it.debtType == "HUTANG" && it.remainingAmount > 0 }.sumOf { it.remainingAmount }
    val openPiutang = debts.filter { it.debtType == "PIUTANG" && it.remainingAmount > 0 }.sumOf { it.remainingAmount }
    val unpaidPayroll = payslips.filter { !it.paid }.sumOf { it.totalAmount }
    val lossSales = sales.filter { it.lossAmount > 0.0 || it.items.any { item -> item.sellPrice < item.buyPrice } }
    val totalLoss = lossSales.sumOf { sale ->
        if (sale.lossAmount > 0.0) sale.lossAmount
        else sale.items.filter { it.sellPrice < it.buyPrice }.sumOf { (it.buyPrice - it.sellPrice) * it.quantity }
    }
    val todayMillis = System.currentTimeMillis()
    val movedProductIdsToday = buildSet {
        sales.filter { isSameDay(it.transactionDateMillis, todayMillis) }
            .forEach { s -> s.items.forEach { add(it.productId) } }
        purchases.filter { isSameDay(it.transactionDateMillis, todayMillis) }
            .forEach { p -> p.items.forEach { add(it.productId) } }
        returns.filter { isSameDay(it.transactionDateMillis, todayMillis) }
            .forEach { r -> add(r.productId) }
    }

    val entries = listOf(
        HubMenuEntry(
            section = "Transaksi Barang",
            title = "Penjualan",
            subtitle = "Kasir modern tunai (Uang Pas & kembalian) atau piutang -> masuk Keuangan",
            badgeText = "${sales.size} transaksi • ${formatRupiah(sales.sumOf { it.totalAmount })}",
            icon = Icons.Default.PointOfSale,
            tint = ProfitGreen,
            route = "sales",
            tag = "hub_sales"
        ),
        HubMenuEntry(
            section = "Transaksi Barang",
            title = "Pembelian",
            subtitle = "Kasir pembelian stok tunai/kembalian atau hutang -> masuk Keuangan",
            badgeText = "${purchases.size} transaksi • ${formatRupiah(purchases.sumOf { it.totalAmount })}",
            icon = Icons.Default.AddShoppingCart,
            tint = InfoBlue,
            route = "purchases",
            tag = "hub_purchases"
        ),
        HubMenuEntry(
            section = "Transaksi Barang",
            title = "Retur Pembelian",
            subtitle = "Kasir retur barang dengan waktu/tgl (masuk Keuangan / potong hutang & cetak struk)",
            badgeText = "${returns.size} retur",
            icon = Icons.AutoMirrored.Filled.AssignmentReturn,
            tint = WarningAmber,
            route = "purchase_returns",
            tag = "hub_returns"
        ),
        HubMenuEntry(
            section = "Transaksi Barang",
            title = "Jual Rugi",
            subtitle = "Kasir penjualan di bawah harga modal -> masuk Keuangan & cetak struk",
            badgeText = "${lossSales.size} transaksi • Rugi: ${formatRupiah(totalLoss)}",
            icon = Icons.AutoMirrored.Filled.TrendingDown,
            tint = LossRed,
            route = "loss_sales",
            tag = "hub_loss_sales"
        ),
        HubMenuEntry(
            section = "Transaksi Keuangan & Gaji",
            title = "Transaksi Gaji Karyawan",
            subtitle = "Upah borongan harian, bayar semua per filter nama & cetak slip belum dibayar",
            badgeText = "${payslips.size} slip • Belum bayar: ${formatRupiah(unpaidPayroll)}",
            icon = Icons.Default.Badge,
            tint = MaterialTheme.colorScheme.secondary,
            route = "employee_salary",
            tag = "hub_employee_salary"
        ),
        HubMenuEntry(
            section = "Transaksi Keuangan & Gaji",
            title = "Pengeluaran",
            subtitle = "Pengeluaran Toko & Perusahaan -> riwayat langsung di Keuangan",
            badgeText = "${expenses.size} catatan • ${formatRupiah(expenses.sumOf { it.amount })}",
            icon = Icons.Default.MoneyOff,
            tint = LossRed,
            route = "expenses",
            tag = "hub_expenses"
        ),
        HubMenuEntry(
            section = "Transaksi Keuangan & Gaji",
            title = "Hutang Pembelian",
            subtitle = "Kelola & bayar cicilan hutang ke pemasok",
            badgeText = "Sisa: ${formatRupiah(openHutang)}",
            icon = Icons.Default.CreditCard,
            tint = LossRed,
            route = "debts_hutang",
            tag = "hub_hutang"
        ),
        HubMenuEntry(
            section = "Transaksi Keuangan & Gaji",
            title = "Piutang Pedagang",
            subtitle = "Tagihan dan pelunasan pedagang keliling",
            badgeText = "Sisa: ${formatRupiah(openPiutang)}",
            icon = Icons.Default.RequestQuote,
            tint = InfoBlue,
            route = "debts_piutang",
            tag = "hub_piutang"
        ),
        HubMenuEntry(
            section = "Produk & Stok",
            title = "Data Produk",
            subtitle = "Kelola harga beli (modal), harga jual, satuan & stok",
            badgeText = "${products.size} produk terdaftar",
            icon = Icons.Default.Inventory2,
            tint = MaterialTheme.colorScheme.primary,
            route = "products",
            tag = "hub_products"
        ),
        HubMenuEntry(
            section = "Produk & Stok",
            title = "Data Stok Barang Perhari",
            subtitle = "Pantau stok awal, barang masuk, keluar/retur & stok akhir per tanggal",
            badgeText = if (movedProductIdsToday.isNotEmpty()) {
                "${movedProductIdsToday.size} produk bergerak hari ini • ${products.size} produk"
            } else {
                "${products.size} produk • Lihat per tanggal"
            },
            icon = Icons.Default.EventNote,
            tint = InfoBlue,
            route = "daily_stock",
            tag = "hub_daily_stock"
        )
    )

    val grouped = entries.groupBy { it.section }

    ResponsiveScreenContainer {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    text = "Transaksi & Produk",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            grouped.forEach { (section, itemsInSection) ->
                item {
                    Text(
                        text = section,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                items(itemsInSection, key = { it.route }) { item ->
                    Card(
                        onClick = { onNavigate(item.route) },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = MaterialTheme.shapes.medium,
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag(item.tag)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = item.tint.copy(alpha = 0.13f),
                                modifier = Modifier.size(48.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = item.icon,
                                        contentDescription = item.title,
                                        tint = item.tint,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(item.title, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    item.subtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    item.badgeText,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = item.tint
                                )
                            }
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
