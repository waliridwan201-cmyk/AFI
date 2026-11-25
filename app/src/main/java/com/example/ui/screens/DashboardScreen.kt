package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.AssignmentReturn
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.MoneyOff
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.RequestQuote
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.CashBox
import com.example.data.model.DebtRecord
import com.example.data.model.Expense
import com.example.data.model.Payslip
import com.example.data.model.Product
import com.example.data.model.Sale
import com.example.data.model.StoreSettings
import com.example.data.model.UiState
import com.example.ui.components.AdaptiveTwoColumnRow
import com.example.ui.components.ResponsiveScreenContainer
import com.example.ui.components.StatKpiCard
import com.example.ui.components.StatusBadge
import com.example.ui.theme.InfoBlue
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.LossRed
import com.example.ui.theme.LossRedBg
import com.example.ui.theme.ProfitGreen
import com.example.ui.theme.WarningAmber
import com.example.ui.theme.WarningAmberBg
import com.example.util.formatQty
import com.example.util.formatRupiah
import com.example.util.isSameDay

data class QuickMenuAction(
    val label: String,
    val icon: ImageVector,
    val tint: Color,
    val route: String,
    val tag: String
)

@Composable
fun DashboardScreen(
    storeSettingsState: UiState<StoreSettings>,
    cashBoxesState: UiState<List<CashBox>>,
    salesState: UiState<List<Sale>>,
    expensesState: UiState<List<Expense>>,
    debtsState: UiState<List<DebtRecord>>,
    productsState: UiState<List<Product>>,
    payslipsState: UiState<List<Payslip>>,
    onNavigate: (String) -> Unit
) {
    val store = (storeSettingsState as? UiState.Success)?.data ?: StoreSettings()
    val cashBoxes = (cashBoxesState as? UiState.Success)?.data ?: emptyList()
    val sales = (salesState as? UiState.Success)?.data ?: emptyList()
    val expenses = (expensesState as? UiState.Success)?.data ?: emptyList()
    val debts = (debtsState as? UiState.Success)?.data ?: emptyList()
    val products = (productsState as? UiState.Success)?.data ?: emptyList()
    val payslips = (payslipsState as? UiState.Success)?.data ?: emptyList()

    val now = System.currentTimeMillis()
    val totalCash = cashBoxes.sumOf { it.balance }
    val todaySales = sales.filter { isSameDay(it.transactionDateMillis, now) }
    val todayOmzet = todaySales.sumOf { it.totalAmount }
    val todayProfit = todaySales.sumOf { it.totalProfit }
    val todayExpense = expenses.filter { isSameDay(it.transactionDateMillis, now) }.sumOf { it.amount }
    val totalHutang = debts.filter { it.debtType == "HUTANG" && it.remainingAmount > 0 }.sumOf { it.remainingAmount }
    val totalPiutang = debts.filter { it.debtType == "PIUTANG" && it.remainingAmount > 0 }.sumOf { it.remainingAmount }
    val lowStockProducts = products.filter { it.stock <= it.minStock }
    val unpaidPayslips = payslips.filter { !it.paid }

    val quickActions = listOf(
        QuickMenuAction("Penjualan", Icons.Default.PointOfSale, ProfitGreen, "sales", "qa_sales"),
        QuickMenuAction("Pembelian", Icons.Default.AddShoppingCart, InfoBlue, "purchases", "qa_purchases"),
        QuickMenuAction("Retur Beli", Icons.AutoMirrored.Filled.AssignmentReturn, WarningAmber, "purchase_returns", "qa_returns"),
        QuickMenuAction("Pengeluaran", Icons.Default.MoneyOff, LossRed, "expenses", "qa_expenses"),
        QuickMenuAction("Hutang", Icons.Default.CreditCard, LossRed, "debts_hutang", "qa_hutang"),
        QuickMenuAction("Piutang", Icons.Default.RequestQuote, InfoBlue, "debts_piutang", "qa_piutang"),
        QuickMenuAction("Data Produk", Icons.Default.Inventory2, MaterialTheme.colorScheme.primary, "products", "qa_products"),
        QuickMenuAction("Slip Gaji", Icons.Default.Badge, MaterialTheme.colorScheme.secondary, "tab_contacts_payslip", "qa_payslips")
    )

    ResponsiveScreenContainer {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Hero Banner Card
            item {
                Card(
                    shape = MaterialTheme.shapes.large,
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(185.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_afi_hero),
                            contentDescription = "Banner bisnis AFI",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(
                                            Color(0xE60B3B24),
                                            Color(0xB30F5132),
                                            Color(0x660B3B24)
                                        )
                                    )
                                )
                        )
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(18.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = store.storeName.ifBlank { "Toko & Pabrik AFI" },
                                        style = MaterialTheme.typography.titleLarge,
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (store.ownerName.isNotBlank()) {
                                        Text(
                                            text = "Halo, ${store.ownerName}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color.White.copy(alpha = 0.85f)
                                        )
                                    }
                                }
                                IconButton(
                                    onClick = { onNavigate("settings") },
                                    modifier = Modifier.testTag("dashboard_settings_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Settings,
                                        contentDescription = "Pengaturan Toko",
                                        tint = Color.White
                                    )
                                }
                            }

                            Column {
                                Text(
                                    text = "Total Saldo Cash Box (${cashBoxes.size} Kas)",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color(0xFFD1FAE5)
                                )
                                Text(
                                    text = formatRupiah(totalCash),
                                    style = MaterialTheme.typography.displaySmall.copy(
                                        fontFamily = JetBrainsMonoFamily,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }

            // Today's KPI Grid
            item {
                AdaptiveTwoColumnRow(
                    first = {
                        StatKpiCard(
                            title = "Penjualan Hari Ini",
                            value = formatRupiah(todayOmzet),
                            subtitle = "${todaySales.size} transaksi • Laba ${formatRupiah(todayProfit)}",
                            icon = Icons.AutoMirrored.Filled.TrendingUp,
                            accentColor = ProfitGreen,
                            onClick = { onNavigate("sales") }
                        )
                    },
                    second = {
                        StatKpiCard(
                            title = "Pengeluaran Hari Ini",
                            value = formatRupiah(todayExpense),
                            subtitle = "Total piutang ${formatRupiah(totalPiutang)}",
                            icon = Icons.Default.AccountBalanceWallet,
                            accentColor = WarningAmber,
                            onClick = { onNavigate("expenses") }
                        )
                    }
                )
            }

            item {
                AdaptiveTwoColumnRow(
                    first = {
                        StatKpiCard(
                            title = "Hutang Pembelian",
                            value = formatRupiah(totalHutang),
                            subtitle = "Kewajiban ke pemasok",
                            icon = Icons.Default.CreditCard,
                            accentColor = LossRed,
                            onClick = { onNavigate("debts_hutang") }
                        )
                    },
                    second = {
                        StatKpiCard(
                            title = "Piutang Pedagang",
                            value = formatRupiah(totalPiutang),
                            subtitle = "Tagihan pedagang keliling",
                            icon = Icons.Default.RequestQuote,
                            accentColor = InfoBlue,
                            onClick = { onNavigate("debts_piutang") }
                        )
                    }
                )
            }

            // Quick Actions Grid
            item {
                Text(
                    text = "Menu Cepat AFI",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            item {
                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    val columns = if (maxWidth >= 580.dp) 4 else 2
                    val rows = quickActions.chunked(columns)
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        rows.forEach { rowItems ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                rowItems.forEach { action ->
                                    Card(
                                        onClick = { onNavigate(action.route) },
                                        shape = MaterialTheme.shapes.medium,
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.surface
                                        ),
                                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag(action.tag)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Surface(
                                                shape = RoundedCornerShape(12.dp),
                                                color = action.tint.copy(alpha = 0.13f),
                                                modifier = Modifier.size(38.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(
                                                        imageVector = action.icon,
                                                        contentDescription = action.label,
                                                        tint = action.tint,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }
                                            }
                                            Text(
                                                text = action.label,
                                                style = MaterialTheme.typography.titleSmall,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                                if (rowItems.size < columns) {
                                    repeat(columns - rowItems.size) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Unpaid Payslips Reminder
            if (unpaidPayslips.isNotEmpty()) {
                item {
                    Card(
                        onClick = { onNavigate("tab_contacts_payslip") },
                        colors = CardDefaults.cardColors(containerColor = WarningAmberBg),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ReceiptLong,
                                contentDescription = null,
                                tint = WarningAmber
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${unpaidPayslips.size} Slip Gaji Belum Dibayar",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = Color(0xFF78350F)
                                )
                                Text(
                                    text = "Total upah tertunda ${formatRupiah(unpaidPayslips.sumOf { it.totalAmount })}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF92400E)
                                )
                            }
                            StatusBadge(
                                text = "Lihat",
                                containerColor = WarningAmber,
                                contentColor = Color.White
                            )
                        }
                    }
                }
            }

            // Low Stock Alerts
            if (lowStockProducts.isNotEmpty()) {
                item {
                    Text(
                        text = "Peringatan Stok Menipis (${lowStockProducts.size})",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
                items(lowStockProducts.take(5), key = { it.id }) { prod ->
                    Card(
                        onClick = { onNavigate("products") },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.WarningAmber,
                                contentDescription = "Stok menipis",
                                tint = LossRed
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(prod.name, style = MaterialTheme.typography.titleSmall)
                                Text(
                                    "Sisa stok: ${formatQty(prod.stock)} ${prod.unit} (Min: ${formatQty(prod.minStock)})",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            StatusBadge(
                                text = "Stok Rendah",
                                containerColor = LossRedBg,
                                contentColor = LossRed
                            )
                        }
                    }
                }
            }

            // App Creator & Version Footer
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                    ),
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dashboard_creator_footer")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Verified,
                                        contentDescription = "Pembuat Aplikasi",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = "Pembuat Aplikasi",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Ridwan Al Namasy",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Aplikasi ini berbayar, dibayar dengan doa terbaik dan pahala bagi pembuat aplikasi.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        StatusBadge(
                            text = "V.1.2",
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }
    }
}
