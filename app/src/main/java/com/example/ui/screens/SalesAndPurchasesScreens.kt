package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.material.icons.automirrored.filled.AssignmentReturn
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.CashBox
import com.example.data.model.DebtRecord
import com.example.data.model.Merchant
import com.example.data.model.Product
import com.example.data.model.Purchase
import com.example.data.model.PurchaseItem
import com.example.data.model.PurchaseReturn
import com.example.data.model.Sale
import com.example.data.model.SaleItem
import com.example.data.model.StoreSettings
import com.example.data.model.Supplier
import com.example.data.model.UiState
import com.example.ui.components.CurrencyText
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.ResponsiveScreenContainer
import com.example.ui.components.SearchBarField
import com.example.ui.theme.LossRed
import com.example.ui.theme.LossRedBg
import com.example.ui.theme.ProfitGreen
import com.example.ui.theme.WarningAmber
import com.example.util.formatDateTime
import com.example.util.formatDateTimeSlash
import com.example.util.formatQty
import com.example.util.formatRupiah
import com.example.util.generateInvoiceNo
import com.example.util.toThermalReceipt
import kotlin.math.max

// ==========================================
// KOMPONEN SHARED (DIPAKAI OLEH PENGELUARAN, GAJI & PICKER DIALOG)
// ==========================================
@Composable
internal fun PosRegisterDateHeader(
    registerTitle: String,
    subtitle: String,
    transactionDateMillis: Long,
    icon: ImageVector,
    accentColor: Color = MaterialTheme.colorScheme.primary
) {
    Surface(
        color = accentColor.copy(alpha = 0.10f),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.25f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                color = accentColor.copy(alpha = 0.16f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(24.dp))
                }
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(registerTitle, style = MaterialTheme.typography.titleSmall, color = accentColor, fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Default.Schedule, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(14.dp))
                    Text(
                        text = formatDateTime(transactionDateMillis),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
internal fun PosProductTileGrid(
    products: List<Product>,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    selectedProductId: String?,
    cartQtyByProductId: Map<String, Double>,
    priceProvider: (Product) -> Double,
    priceLabelPrefix: String = "",
    tagPrefix: String,
    onTileTap: (Product) -> Unit
) {
    val filtered = remember(products, searchQuery) {
        if (searchQuery.isBlank()) products
        else {
            val q = searchQuery.trim()
            products.filter {
                it.name.contains(q, ignoreCase = true) ||
                    it.category.contains(q, ignoreCase = true) ||
                    it.unit.contains(q, ignoreCase = true)
            }
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SearchBarField(
            query = searchQuery,
            onQueryChange = onSearchChange,
            placeholder = "Cari nama produk / kategori...",
            tag = "${tagPrefix}_product_search"
        )
        if (filtered.isEmpty()) {
            Text(
                "Produk tidak ditemukan.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val columns = if (maxWidth >= 560.dp) 3 else 2
                val rows = remember(filtered, columns) { filtered.chunked(columns) }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    rows.forEach { rowItems ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowItems.forEach { prod ->
                                val qtyInCart = cartQtyByProductId[prod.id] ?: 0.0
                                val isSelected = prod.id == selectedProductId || qtyInCart > 0.0
                                val tilePrice = priceProvider(prod)
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = if (qtyInCart > 0.0) {
                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                                    } else if (isSelected) {
                                        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f)
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                                    },
                                    border = BorderStroke(
                                        width = if (isSelected) 1.5.dp else 1.dp,
                                        color = if (qtyInCart > 0.0) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.outlineVariant
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 96.dp)
                                        .clickable { onTileTap(prod) }
                                        .testTag("${tagPrefix}_tile_${prod.id}")
                                ) {
                                    Column(
                                        modifier = Modifier.padding(10.dp),
                                        verticalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.Top
                                        ) {
                                            Text(
                                                text = prod.name,
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = FontWeight.SemiBold,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f)
                                            )
                                            if (qtyInCart > 0.0) {
                                                Surface(
                                                    color = MaterialTheme.colorScheme.primary,
                                                    shape = RoundedCornerShape(8.dp)
                                                ) {
                                                    Text(
                                                        text = "x${formatQty(qtyInCart)}",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.onPrimary,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                        }
                                        Spacer(Modifier.size(4.dp))
                                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                            Text(
                                                text = "$priceLabelPrefix${formatRupiah(tilePrice)}",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Text(
                                                text = "Stok ${formatQty(prod.stock)} ${prod.unit}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (prod.stock <= 0.0) LossRed else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                            repeat(columns - rowItems.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 1. PENJUALAN (IMAGE 1 LAYOUT, KASIR CASH PAS + KEMBALIAN, LANGSUNG KE KEUANGAN)
// ==========================================
@Composable
fun SalesScreen(
    productsState: UiState<List<Product>>,
    merchantsState: UiState<List<Merchant>>,
    cashBoxesState: UiState<List<CashBox>>,
    storeSettingsState: UiState<StoreSettings>,
    onCreateSale: (
        customerName: String,
        merchantId: String,
        items: List<SaleItem>,
        productsSnapshot: List<Product>,
        paidAmountInput: Double,
        isPiutang: Boolean,
        cashBox: CashBox?,
        notes: String,
        transactionDateMillis: Long,
        onCreated: (Sale) -> Unit
    ) -> Unit,
    onNavigateToFinance: () -> Unit,
    onBack: () -> Unit
) {
    val products = (productsState as? UiState.Success)?.data ?: emptyList()
    val merchants = (merchantsState as? UiState.Success)?.data ?: emptyList()
    val cashBoxes = (cashBoxesState as? UiState.Success)?.data ?: emptyList()
    val storeSettings = (storeSettingsState as? UiState.Success)?.data ?: StoreSettings()

    var txNonce by remember { mutableIntStateOf(0) }
    val draftId = remember(txNonce) { "TRS${System.currentTimeMillis()}" }
    val draftInvoiceNo = remember(txNonce) { generateInvoiceNo("INV") }

    var selectedMerchant by remember { mutableStateOf<Merchant?>(null) }
    var customerName by remember { mutableStateOf("Pelanggan Umum") }
    var isPiutang by remember { mutableStateOf(false) }
    var cashTenderedStr by remember { mutableStateOf("") }
    var dpAmountStr by remember { mutableStateOf("0") }
    var selectedCashBox by remember { mutableStateOf(cashBoxes.firstOrNull()) }
    LaunchedEffect(cashBoxes) {
        if (selectedCashBox == null) selectedCashBox = cashBoxes.firstOrNull()
    }
    var notes by remember { mutableStateOf("") }
    var transactionDateMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }

    val cart = remember { mutableStateListOf<SaleItem>() }
    var error by remember { mutableStateOf<String?>(null) }
    var showPartyDialog by remember { mutableStateOf(false) }
    var showProductDialog by remember { mutableStateOf(false) }
    var showPaymentDialog by remember { mutableStateOf(false) }
    var showDateDialog by remember { mutableStateOf(false) }
    var editingItemPair by remember { mutableStateOf<Pair<Int, SaleItem>?>(null) }

    var savedSaleReceipt by remember { mutableStateOf<Triple<Sale, Double?, Double?>?>(null) }
    var printingReceipt by remember { mutableStateOf<Triple<Sale, Double?, Double?>?>(null) }

    val totalAmount = cart.sumOf { it.subtotal }
    val cashTendered = cashTenderedStr.replace(",", ".").toDoubleOrNull() ?: 0.0
    val changeAmount = max(0.0, cashTendered - totalAmount)
    val dpAmount = (dpAmountStr.replace(",", ".").toDoubleOrNull() ?: 0.0).coerceIn(0.0, totalAmount)
    val cartQtyMap = remember(cart.toList()) {
        cart.groupBy { it.productId }.mapValues { (_, list) -> list.sumOf { it.quantity } }
    }

    fun addProductToCart(prod: Product, qty: Double, sellPrice: Double) {
        val existingIdx = cart.indexOfFirst { it.productId == prod.id && it.sellPrice == sellPrice }
        if (existingIdx >= 0) {
            val old = cart[existingIdx]
            val newQty = old.quantity + qty
            cart[existingIdx] = old.copy(
                quantity = newQty,
                subtotal = newQty * sellPrice,
                profit = (sellPrice - old.buyPrice) * newQty
            )
        } else {
            cart.add(
                SaleItem(
                    productId = prod.id,
                    productName = prod.name,
                    unit = prod.unit,
                    quantity = qty,
                    buyPrice = prod.buyPrice,
                    sellPrice = sellPrice,
                    subtotal = qty * sellPrice,
                    profit = (sellPrice - prod.buyPrice) * qty
                )
            )
        }
    }

    fun resetCashierForm() {
        cart.clear()
        cashTenderedStr = ""
        dpAmountStr = "0"
        notes = ""
        error = null
        transactionDateMillis = System.currentTimeMillis()
        txNonce++
    }

    fun submitSale() {
        val cust = customerName.trim().ifBlank { selectedMerchant?.name ?: "" }
        if (cust.isBlank()) {
            error = "Nama pembeli / pedagang wajib diisi"
            return
        }
        if (cart.isEmpty()) {
            error = "Tambahkan minimal 1 produk ke keranjang"
            return
        }
        if (!isPiutang && cashTendered < totalAmount) {
            error = null
            showPaymentDialog = true
            return
        }
        error = null
        val paidInput = if (isPiutang) dpAmount else totalAmount
        val recordedCash = if (!isPiutang) cashTendered else null
        val recordedChange = if (!isPiutang) changeAmount else null
        onCreateSale(
            cust,
            selectedMerchant?.id ?: "",
            cart.toList(),
            products,
            paidInput,
            isPiutang,
            selectedCashBox,
            notes,
            transactionDateMillis
        ) { created ->
            savedSaleReceipt = Triple(created, recordedCash, recordedChange)
            resetCashierForm()
        }
    }

    ResponsiveScreenContainer {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
        ) {
            SlateTopHeaderBar(
                title = "Penjualan",
                onBack = onBack,
                backTag = "sales_back"
            )
            PosSlateActionToolbar(
                tagPrefix = "sales",
                partyContentDescription = "Pilih Customer / Pedagang",
                onSelectPartyClick = { showPartyDialog = true },
                onAddProductClick = { showProductDialog = true },
                onOptionsClick = { showPaymentDialog = true },
                onSharePrintClick = {
                    val existing = savedSaleReceipt
                    if (existing != null) {
                        printingReceipt = existing
                    } else if (cart.isNotEmpty()) {
                        val previewSale = Sale(
                            invoiceNo = draftInvoiceNo,
                            customerName = customerName.ifBlank { "Pelanggan Umum" },
                            items = cart.toList(),
                            totalAmount = totalAmount,
                            paidAmount = if (isPiutang) dpAmount else cashTendered.coerceAtLeast(totalAmount),
                            remainingDebt = if (isPiutang) max(0.0, totalAmount - dpAmount) else 0.0,
                            transactionDateMillis = transactionDateMillis
                        )
                        printingReceipt = Triple(
                            previewSale,
                            if (!isPiutang) cashTendered.takeIf { it > 0 } else null,
                            if (!isPiutang && cashTendered > totalAmount) changeAmount else null
                        )
                    } else {
                        error = "Tambahkan produk terlebih dahulu untuk cetak struk"
                    }
                },
                onSaveClick = { submitSale() }
            )

            PosMetadataRow(label = "ID", value = draftId)
            PosMetadataRow(label = "Nomor", value = draftInvoiceNo)
            PosMetadataRow(
                label = "Tanggal",
                value = formatDateTimeSlash(transactionDateMillis),
                onClick = { showDateDialog = true },
                tag = "sale_date_row"
            )
            PosMetadataRow(
                label = "Customer",
                value = customerName,
                onClick = { showPartyDialog = true },
                tag = "sale_customer_row"
            )

            PosLedgerTableHeader()

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(Color.White)
            ) {
                if (cart.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable { showProductDialog = true }
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddShoppingCart,
                            contentDescription = null,
                            tint = Color(0xFF9CA3AF),
                            modifier = Modifier.size(36.dp)
                        )
                        Text(
                            text = "Ketuk ikon keranjang di atas atau di sini untuk menambah produk",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF6B7280),
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        itemsIndexed(cart, key = { idx, item -> "${item.productId}_$idx" }) { idx, item ->
                            val lossWarning = item.sellPrice < item.buyPrice
                            PosLedgerTableRow(
                                index = idx,
                                productName = item.productName,
                                subtitle = "@${formatRupiah(item.sellPrice)}" + if (lossWarning) " (Rugi)" else "",
                                qtyText = formatQty(item.quantity),
                                amount = item.subtotal,
                                subtitleColor = if (lossWarning) LossRed else Color(0xFF6B7280),
                                onClick = { editingItemPair = idx to item },
                                tag = "sale_cart_row_$idx"
                            )
                        }
                    }
                }

                if (error != null) {
                    Surface(
                        color = LossRedBg,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .clickable { error = null }
                    ) {
                        Text(
                            text = error!!,
                            color = LossRed,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            }

            PosTotalFooterBar(
                totalAmount = totalAmount,
                statusBadgeText = when {
                    totalAmount <= 0.0 -> null
                    isPiutang -> "Piutang"
                    cashTendered >= totalAmount && changeAmount > 0.0 -> "Kembali ${formatRupiah(changeAmount)}"
                    cashTendered == totalAmount -> "Uang Pas"
                    else -> "Cash"
                },
                tag = "save_sale_button",
                onClick = { submitSale() }
            )
        }
    }

    if (showPartyDialog) {
        PosPartyPickerDialog(
            title = "Pilih Customer / Pedagang",
            fieldLabel = "Nama Pembeli / Pedagang *",
            dropdownLabel = "Pedagang Keliling Terdaftar",
            partyOptions = merchants.map { it.id to "${it.name} (${it.routeArea})" },
            currentName = customerName,
            allowDefaultGeneral = true,
            onConfirm = { pickedId, pickedName ->
                selectedMerchant = merchants.firstOrNull { it.id == pickedId }
                customerName = pickedName
                showPartyDialog = false
            },
            onDismiss = { showPartyDialog = false }
        )
    }

    if (showProductDialog) {
        PosProductPickerDialog(
            title = "Tambah Produk Penjualan",
            products = products,
            cartQtyByProductId = cartQtyMap,
            defaultPriceProvider = { it.sellPrice },
            priceLabel = "Harga Jual (Rp)",
            tapTileAddsDirectly = false,
            tagPrefix = "quick_add_sale",
            onAddProduct = { prod, qty, sell ->
                error = null
                addProductToCart(prod, qty, sell)
            },
            onDismiss = { showProductDialog = false }
        )
    }

    editingItemPair?.let { (idx, item) ->
        PosEditItemDialog(
            productName = item.productName,
            unit = item.unit,
            initialQty = item.quantity,
            initialPrice = item.sellPrice,
            priceLabel = "Harga Jual (Rp)",
            onUpdate = { newQty, newPrice ->
                cart[idx] = item.copy(
                    quantity = newQty,
                    sellPrice = newPrice,
                    subtotal = newQty * newPrice,
                    profit = (newPrice - item.buyPrice) * newQty
                )
                editingItemPair = null
            },
            onDelete = {
                cart.removeAt(idx)
                editingItemPair = null
            },
            onDismiss = { editingItemPair = null }
        )
    }

    if (showDateDialog) {
        EditTransactionDateDialog(
            title = "Atur Tanggal & Waktu Penjualan",
            subtitle = customerName,
            initialMillis = transactionDateMillis,
            tagPrefix = "sale_create",
            onConfirm = { newMillis ->
                transactionDateMillis = newMillis
                showDateDialog = false
            },
            onDismiss = { showDateDialog = false }
        )
    }

    if (showPaymentDialog) {
        PosPaymentOptionsDialog(
            title = "Pembayaran & Opsi Penjualan",
            totalAmount = totalAmount,
            tempoLabel = "Piutang / Tempo",
            cashInputLabel = "Uang Diterima / Tunai (Rp)",
            cashBoxLabel = "Masuk ke Cash Box",
            isTempo = isPiutang,
            onIsTempoChange = { isPiutang = it },
            cashTenderedStr = cashTenderedStr,
            onCashTenderedChange = { cashTenderedStr = it },
            dpAmountStr = dpAmountStr,
            onDpAmountChange = { dpAmountStr = it },
            cashBoxes = cashBoxes,
            selectedCashBox = selectedCashBox,
            onSelectCashBox = { selectedCashBox = it },
            notes = notes,
            onNotesChange = { notes = it },
            transactionDateMillis = transactionDateMillis,
            onDateMillisChange = { transactionDateMillis = it },
            onNavigateToFinance = onNavigateToFinance,
            onConfirmPayAndSave = {
                showPaymentDialog = false
                submitSale()
            },
            onDismiss = { showPaymentDialog = false }
        )
    }

    savedSaleReceipt?.let { receipt ->
        val (sale, cashIn, changeOut) = receipt
        AlertDialog(
            onDismissRequest = { savedSaleReceipt = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = ProfitGreen)
                    Text("Penjualan Tersimpan")
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("No. Nota: ${sale.invoiceNo}", fontWeight = FontWeight.SemiBold)
                    Text("Pembeli: ${sale.customerName}")
                    Text("Waktu: ${formatDateTime(sale.transactionDateMillis)}", style = MaterialTheme.typography.bodySmall)
                    HorizontalDivider()
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Total Tagihan")
                        CurrencyText(amount = sale.totalAmount)
                    }
                    if (cashIn != null && cashIn > 0.0 && sale.remainingDebt <= 0.0) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Tunai")
                            Text(formatRupiah(cashIn))
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Kembalian", color = ProfitGreen, fontWeight = FontWeight.SemiBold)
                            Text(formatRupiah(changeOut ?: 0.0), color = ProfitGreen, fontWeight = FontWeight.SemiBold)
                        }
                    } else if (sale.remainingDebt > 0.0) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Sisa Piutang", color = WarningAmber)
                            Text(formatRupiah(sale.remainingDebt), color = WarningAmber)
                        }
                    }
                    Text(
                        "Transaksi tercatat otomatis ke menu Keuangan.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        printingReceipt = receipt
                        savedSaleReceipt = null
                    },
                    modifier = Modifier.testTag("print_saved_sale_button")
                ) {
                    Icon(Icons.Default.Bluetooth, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Cetak Struk")
                }
            },
            dismissButton = {
                TextButton(onClick = { savedSaleReceipt = null }) { Text("Transaksi Baru") }
            }
        )
    }

    printingReceipt?.let { (sale, cashIn, changeOut) ->
        BluetoothPrintDialog(
            document = sale.toThermalReceipt(cashTendered = cashIn, changeAmount = changeOut),
            storeSettings = storeSettings,
            onDismiss = { printingReceipt = null }
        )
    }
}

// ==========================================
// 2. PEMBELIAN (IMAGE 1 LAYOUT, KASIR CASH PAS + KEMBALIAN, LANGSUNG KE KEUANGAN)
// ==========================================
@Composable
fun PurchasesScreen(
    productsState: UiState<List<Product>>,
    suppliersState: UiState<List<Supplier>>,
    cashBoxesState: UiState<List<CashBox>>,
    storeSettingsState: UiState<StoreSettings>,
    onCreatePurchase: (
        supplierName: String,
        items: List<PurchaseItem>,
        productsSnapshot: List<Product>,
        paidAmountInput: Double,
        isHutang: Boolean,
        cashBox: CashBox?,
        notes: String,
        transactionDateMillis: Long,
        onCreated: (Purchase) -> Unit
    ) -> Unit,
    onNavigateToFinance: () -> Unit,
    onBack: () -> Unit
) {
    val products = (productsState as? UiState.Success)?.data ?: emptyList()
    val suppliers = (suppliersState as? UiState.Success)?.data ?: emptyList()
    val cashBoxes = (cashBoxesState as? UiState.Success)?.data ?: emptyList()
    val storeSettings = (storeSettingsState as? UiState.Success)?.data ?: StoreSettings()

    var txNonce by remember { mutableIntStateOf(0) }
    val draftId = remember(txNonce) { "TRS${System.currentTimeMillis()}" }
    val draftInvoiceNo = remember(txNonce) { generateInvoiceNo("BEL") }

    var supplierName by remember { mutableStateOf(suppliers.firstOrNull()?.name ?: "") }
    LaunchedEffect(suppliers) {
        if (supplierName.isBlank() && suppliers.isNotEmpty()) {
            supplierName = suppliers.first().name
        }
    }
    var isHutang by remember { mutableStateOf(false) }
    var cashPaidStr by remember { mutableStateOf("") }
    var dpAmountStr by remember { mutableStateOf("0") }
    var selectedCashBox by remember { mutableStateOf(cashBoxes.firstOrNull()) }
    LaunchedEffect(cashBoxes) {
        if (selectedCashBox == null) selectedCashBox = cashBoxes.firstOrNull()
    }
    var notes by remember { mutableStateOf("") }
    var transactionDateMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }

    val cart = remember { mutableStateListOf<PurchaseItem>() }
    var error by remember { mutableStateOf<String?>(null) }
    var showPartyDialog by remember { mutableStateOf(false) }
    var showProductDialog by remember { mutableStateOf(false) }
    var showPaymentDialog by remember { mutableStateOf(false) }
    var showDateDialog by remember { mutableStateOf(false) }
    var editingItemPair by remember { mutableStateOf<Pair<Int, PurchaseItem>?>(null) }

    var savedPurchaseReceipt by remember { mutableStateOf<Triple<Purchase, Double?, Double?>?>(null) }
    var printingReceipt by remember { mutableStateOf<Triple<Purchase, Double?, Double?>?>(null) }

    val totalAmount = cart.sumOf { it.subtotal }
    val cashPaidInput = cashPaidStr.replace(",", ".").toDoubleOrNull() ?: 0.0
    val changeAmount = max(0.0, cashPaidInput - totalAmount)
    val dpAmount = (dpAmountStr.replace(",", ".").toDoubleOrNull() ?: 0.0).coerceIn(0.0, totalAmount)
    val cartQtyMap = remember(cart.toList()) {
        cart.groupBy { it.productId }.mapValues { (_, list) -> list.sumOf { it.quantity } }
    }

    fun addProductToCart(prod: Product, qty: Double, buyPrice: Double) {
        val existingIdx = cart.indexOfFirst { it.productId == prod.id && it.buyPrice == buyPrice }
        if (existingIdx >= 0) {
            val old = cart[existingIdx]
            val newQty = old.quantity + qty
            cart[existingIdx] = old.copy(quantity = newQty, subtotal = newQty * buyPrice)
        } else {
            cart.add(
                PurchaseItem(
                    productId = prod.id,
                    productName = prod.name,
                    unit = prod.unit,
                    quantity = qty,
                    buyPrice = buyPrice,
                    subtotal = qty * buyPrice
                )
            )
        }
    }

    fun resetCashierForm() {
        cart.clear()
        cashPaidStr = ""
        dpAmountStr = "0"
        notes = ""
        error = null
        transactionDateMillis = System.currentTimeMillis()
        txNonce++
    }

    fun submitPurchase() {
        val supName = supplierName.trim()
        if (supName.isBlank()) {
            error = "Nama pemasok / supplier wajib diisi"
            showPartyDialog = true
            return
        }
        if (cart.isEmpty()) {
            error = "Tambahkan minimal 1 barang pembelian"
            return
        }
        if (!isHutang && cashPaidInput < totalAmount) {
            error = null
            showPaymentDialog = true
            return
        }
        error = null
        val paidInput = if (isHutang) dpAmount else totalAmount
        val recordedCash = if (!isHutang) cashPaidInput else null
        val recordedChange = if (!isHutang) changeAmount else null
        onCreatePurchase(
            supName,
            cart.toList(),
            products,
            paidInput,
            isHutang,
            selectedCashBox,
            notes,
            transactionDateMillis
        ) { created ->
            savedPurchaseReceipt = Triple(created, recordedCash, recordedChange)
            resetCashierForm()
        }
    }

    ResponsiveScreenContainer {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
        ) {
            SlateTopHeaderBar(
                title = "Pembelian",
                onBack = onBack,
                backTag = "purchases_back"
            )
            PosSlateActionToolbar(
                tagPrefix = "purchases",
                partyContentDescription = "Pilih Supplier",
                onSelectPartyClick = { showPartyDialog = true },
                onAddProductClick = { showProductDialog = true },
                onOptionsClick = { showPaymentDialog = true },
                onSharePrintClick = {
                    val existing = savedPurchaseReceipt
                    if (existing != null) {
                        printingReceipt = existing
                    } else if (cart.isNotEmpty()) {
                        val previewPurchase = Purchase(
                            invoiceNo = draftInvoiceNo,
                            supplierName = supplierName.ifBlank { "Supplier" },
                            items = cart.toList(),
                            totalAmount = totalAmount,
                            paidAmount = if (isHutang) dpAmount else cashPaidInput.coerceAtLeast(totalAmount),
                            remainingDebt = if (isHutang) max(0.0, totalAmount - dpAmount) else 0.0,
                            transactionDateMillis = transactionDateMillis
                        )
                        printingReceipt = Triple(
                            previewPurchase,
                            if (!isHutang) cashPaidInput.takeIf { it > 0 } else null,
                            if (!isHutang && cashPaidInput > totalAmount) changeAmount else null
                        )
                    } else {
                        error = "Tambahkan barang terlebih dahulu untuk cetak struk"
                    }
                },
                onSaveClick = { submitPurchase() }
            )

            PosMetadataRow(label = "ID", value = draftId)
            PosMetadataRow(label = "Nomor", value = draftInvoiceNo)
            PosMetadataRow(
                label = "Tanggal",
                value = formatDateTimeSlash(transactionDateMillis),
                onClick = { showDateDialog = true },
                tag = "purchase_date_row"
            )
            PosMetadataRow(
                label = "Supplier",
                value = supplierName.ifBlank { "Ketuk untuk pilih supplier" },
                onClick = { showPartyDialog = true },
                tag = "purchase_supplier_row"
            )

            PosLedgerTableHeader()

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(Color.White)
            ) {
                if (cart.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable { showProductDialog = true }
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddShoppingCart,
                            contentDescription = null,
                            tint = Color(0xFF9CA3AF),
                            modifier = Modifier.size(36.dp)
                        )
                        Text(
                            text = "Ketuk ikon keranjang di atas atau di sini untuk menambah barang masuk",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF6B7280),
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        itemsIndexed(cart, key = { idx, item -> "${item.productId}_$idx" }) { idx, item ->
                            PosLedgerTableRow(
                                index = idx,
                                productName = item.productName,
                                subtitle = "@${formatRupiah(item.buyPrice)}",
                                qtyText = formatQty(item.quantity),
                                amount = item.subtotal,
                                onClick = { editingItemPair = idx to item },
                                tag = "purchase_cart_row_$idx"
                            )
                        }
                    }
                }

                if (error != null) {
                    Surface(
                        color = LossRedBg,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .clickable { error = null }
                    ) {
                        Text(
                            text = error!!,
                            color = LossRed,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            }

            PosTotalFooterBar(
                totalAmount = totalAmount,
                statusBadgeText = when {
                    totalAmount <= 0.0 -> null
                    isHutang -> "Hutang"
                    cashPaidInput >= totalAmount && changeAmount > 0.0 -> "Kembali ${formatRupiah(changeAmount)}"
                    cashPaidInput == totalAmount -> "Uang Pas"
                    else -> "Cash"
                },
                tag = "save_purchase_button",
                onClick = { submitPurchase() }
            )
        }
    }

    if (showPartyDialog) {
        PosPartyPickerDialog(
            title = "Pilih Supplier / Pemasok",
            fieldLabel = "Nama Pemasok / Supplier *",
            dropdownLabel = "Supplier Tersimpan",
            partyOptions = suppliers.map { sup ->
                sup.id to if (sup.suppliedItems.isNotBlank()) "${sup.name} (${sup.suppliedItems})" else sup.name
            },
            currentName = supplierName,
            allowDefaultGeneral = false,
            onConfirm = { _, pickedName ->
                supplierName = pickedName
                error = null
                showPartyDialog = false
            },
            onDismiss = { showPartyDialog = false }
        )
    }

    if (showProductDialog) {
        PosProductPickerDialog(
            title = "Tambah Barang Pembelian",
            products = products,
            cartQtyByProductId = cartQtyMap,
            defaultPriceProvider = { it.buyPrice },
            priceLabel = "Harga Beli (Rp)",
            pricePrefixInGrid = "Modal ",
            tapTileAddsDirectly = false,
            tagPrefix = "quick_add_purchase",
            onAddProduct = { prod, qty, buy ->
                error = null
                addProductToCart(prod, qty, buy)
            },
            onDismiss = { showProductDialog = false }
        )
    }

    editingItemPair?.let { (idx, item) ->
        PosEditItemDialog(
            productName = item.productName,
            unit = item.unit,
            initialQty = item.quantity,
            initialPrice = item.buyPrice,
            priceLabel = "Harga Beli (Rp)",
            onUpdate = { newQty, newPrice ->
                cart[idx] = item.copy(
                    quantity = newQty,
                    buyPrice = newPrice,
                    subtotal = newQty * newPrice
                )
                editingItemPair = null
            },
            onDelete = {
                cart.removeAt(idx)
                editingItemPair = null
            },
            onDismiss = { editingItemPair = null }
        )
    }

    if (showDateDialog) {
        EditTransactionDateDialog(
            title = "Atur Tanggal & Waktu Pembelian",
            subtitle = supplierName.ifBlank { "Pembelian Stok" },
            initialMillis = transactionDateMillis,
            tagPrefix = "purchase_create",
            onConfirm = { newMillis ->
                transactionDateMillis = newMillis
                showDateDialog = false
            },
            onDismiss = { showDateDialog = false }
        )
    }

    if (showPaymentDialog) {
        PosPaymentOptionsDialog(
            title = "Pembayaran & Opsi Pembelian",
            totalAmount = totalAmount,
            tempoLabel = "Hutang Tempo",
            cashInputLabel = "Uang Dibayar / Tunai (Rp)",
            cashBoxLabel = "Sumber Kas",
            isTempo = isHutang,
            onIsTempoChange = { isHutang = it },
            cashTenderedStr = cashPaidStr,
            onCashTenderedChange = { cashPaidStr = it },
            dpAmountStr = dpAmountStr,
            onDpAmountChange = { dpAmountStr = it },
            cashBoxes = cashBoxes,
            selectedCashBox = selectedCashBox,
            onSelectCashBox = { selectedCashBox = it },
            notes = notes,
            onNotesChange = { notes = it },
            transactionDateMillis = transactionDateMillis,
            onDateMillisChange = { transactionDateMillis = it },
            onNavigateToFinance = onNavigateToFinance,
            onConfirmPayAndSave = {
                showPaymentDialog = false
                submitPurchase()
            },
            onDismiss = { showPaymentDialog = false }
        )
    }

    savedPurchaseReceipt?.let { receipt ->
        val (purchase, cashOut, changeBack) = receipt
        AlertDialog(
            onDismissRequest = { savedPurchaseReceipt = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = ProfitGreen)
                    Text("Pembelian Tersimpan")
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("No. Nota: ${purchase.invoiceNo}", fontWeight = FontWeight.SemiBold)
                    Text("Pemasok: ${purchase.supplierName}")
                    Text("Waktu: ${formatDateTime(purchase.transactionDateMillis)}", style = MaterialTheme.typography.bodySmall)
                    HorizontalDivider()
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Total Pembelian")
                        CurrencyText(amount = purchase.totalAmount)
                    }
                    if (cashOut != null && cashOut > 0.0 && purchase.remainingDebt <= 0.0) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Tunai")
                            Text(formatRupiah(cashOut))
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Kembalian", color = ProfitGreen, fontWeight = FontWeight.SemiBold)
                            Text(formatRupiah(changeBack ?: 0.0), color = ProfitGreen, fontWeight = FontWeight.SemiBold)
                        }
                    } else if (purchase.remainingDebt > 0.0) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Sisa Hutang", color = LossRed)
                            Text(formatRupiah(purchase.remainingDebt), color = LossRed)
                        }
                    }
                    Text(
                        "Transaksi tercatat otomatis ke menu Keuangan.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        printingReceipt = receipt
                        savedPurchaseReceipt = null
                    },
                    modifier = Modifier.testTag("print_saved_purchase_button")
                ) {
                    Icon(Icons.Default.Bluetooth, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Cetak Struk")
                }
            },
            dismissButton = {
                TextButton(onClick = { savedPurchaseReceipt = null }) { Text("Transaksi Baru") }
            }
        )
    }

    printingReceipt?.let { (purchase, cashOut, changeBack) ->
        BluetoothPrintDialog(
            document = purchase.toThermalReceipt(cashPaidInput = cashOut, changeAmount = changeBack),
            storeSettings = storeSettings,
            onDismiss = { printingReceipt = null }
        )
    }
}

// ==========================================
// 3. RETUR PEMBELIAN (IMAGE 1 LAYOUT, WAKTU & TANGGAL, LANGSUNG KE KEUANGAN)
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PurchaseReturnsScreen(
    productsState: UiState<List<Product>>,
    suppliersState: UiState<List<Supplier>>,
    debtsState: UiState<List<DebtRecord>>,
    cashBoxesState: UiState<List<CashBox>>,
    storeSettingsState: UiState<StoreSettings>,
    onCreateReturn: (
        supplierName: String,
        product: Product,
        quantity: Double,
        unitPrice: Double,
        refundMethod: String,
        cashBox: CashBox?,
        reason: String,
        matchingHutang: DebtRecord?,
        transactionDateMillis: Long,
        onCreated: (PurchaseReturn) -> Unit
    ) -> Unit,
    onNavigateToFinance: () -> Unit,
    onBack: () -> Unit
) {
    val products = (productsState as? UiState.Success)?.data ?: emptyList()
    val suppliers = (suppliersState as? UiState.Success)?.data ?: emptyList()
    val openHutangList = (debtsState as? UiState.Success)?.data?.filter {
        it.debtType == "HUTANG" && it.remainingAmount > 0
    } ?: emptyList()
    val cashBoxes = (cashBoxesState as? UiState.Success)?.data ?: emptyList()
    val storeSettings = (storeSettingsState as? UiState.Success)?.data ?: StoreSettings()

    var txNonce by remember { mutableIntStateOf(0) }
    val draftId = remember(txNonce) { "TRS${System.currentTimeMillis()}" }
    val draftReturnNo = remember(txNonce) { generateInvoiceNo("RTR") }

    var selectedProduct by remember { mutableStateOf<Product?>(null) }
    var supplierName by remember { mutableStateOf(suppliers.firstOrNull()?.name ?: "") }
    LaunchedEffect(suppliers) {
        if (supplierName.isBlank() && suppliers.isNotEmpty()) {
            supplierName = suppliers.first().name
        }
    }
    var qtyStr by remember { mutableStateOf("1") }
    var priceStr by remember { mutableStateOf("0") }
    var refundMethod by remember { mutableStateOf("KEMBALI_KAS") }
    var selectedHutang by remember { mutableStateOf(openHutangList.firstOrNull()) }
    var selectedCashBox by remember { mutableStateOf(cashBoxes.firstOrNull()) }
    LaunchedEffect(cashBoxes) {
        if (selectedCashBox == null) selectedCashBox = cashBoxes.firstOrNull()
    }
    var reason by remember { mutableStateOf("") }
    var transactionDateMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var error by remember { mutableStateOf<String?>(null) }

    var showPartyDialog by remember { mutableStateOf(false) }
    var showProductDialog by remember { mutableStateOf(false) }
    var showOptionsDialog by remember { mutableStateOf(false) }
    var showDateDialog by remember { mutableStateOf(false) }
    var showEditRowDialog by remember { mutableStateOf(false) }

    var savedReturn by remember { mutableStateOf<PurchaseReturn?>(null) }
    var printingReturn by remember { mutableStateOf<PurchaseReturn?>(null) }

    val qtyNum = qtyStr.replace(",", ".").toDoubleOrNull() ?: 0.0
    val priceNum = priceStr.replace(",", ".").toDoubleOrNull() ?: 0.0
    val totalRefundPreview = if (selectedProduct != null) qtyNum * priceNum else 0.0

    fun submitReturn() {
        val prod = selectedProduct
        if (supplierName.isBlank()) {
            error = "Nama pemasok wajib diisi"
            showPartyDialog = true
            return
        }
        if (prod == null || qtyNum <= 0.0) {
            error = "Pilih produk diretur dan pastikan jumlah > 0"
            showProductDialog = true
            return
        }
        error = null
        onCreateReturn(
            supplierName.trim(),
            prod,
            qtyNum,
            priceNum,
            refundMethod,
            selectedCashBox,
            reason,
            if (refundMethod == "POTONG_HUTANG") selectedHutang else null,
            transactionDateMillis
        ) { created ->
            savedReturn = created
            selectedProduct = null
            qtyStr = "1"
            priceStr = "0"
            reason = ""
            transactionDateMillis = System.currentTimeMillis()
            txNonce++
        }
    }

    ResponsiveScreenContainer {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
        ) {
            SlateTopHeaderBar(
                title = "Retur Pembelian",
                onBack = onBack,
                backTag = "returns_back"
            )
            PosSlateActionToolbar(
                tagPrefix = "returns",
                partyContentDescription = "Pilih Pemasok",
                onSelectPartyClick = { showPartyDialog = true },
                onAddProductClick = { showProductDialog = true },
                onOptionsClick = { showOptionsDialog = true },
                onSharePrintClick = {
                    val existing = savedReturn
                    val prod = selectedProduct
                    if (existing != null) {
                        printingReturn = existing
                    } else if (prod != null && qtyNum > 0.0) {
                        printingReturn = PurchaseReturn(
                            id = draftReturnNo,
                            supplierName = supplierName.ifBlank { "Supplier" },
                            productId = prod.id,
                            productName = prod.name,
                            quantity = qtyNum,
                            unitPrice = priceNum,
                            totalRefund = totalRefundPreview,
                            refundMethod = refundMethod,
                            cashBoxId = selectedCashBox?.id ?: "",
                            reason = reason,
                            transactionDateMillis = transactionDateMillis
                        )
                    } else {
                        error = "Pilih produk diretur terlebih dahulu untuk cetak struk"
                    }
                },
                onSaveClick = { submitReturn() }
            )

            PosMetadataRow(label = "ID", value = draftId)
            PosMetadataRow(label = "Nomor", value = draftReturnNo)
            PosMetadataRow(
                label = "Tanggal",
                value = formatDateTimeSlash(transactionDateMillis),
                onClick = { showDateDialog = true },
                tag = "return_date_row"
            )
            PosMetadataRow(
                label = "Supplier",
                value = supplierName.ifBlank { "Ketuk untuk pilih pemasok" },
                onClick = { showPartyDialog = true },
                tag = "return_supplier_row"
            )

            PosLedgerTableHeader()

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(Color.White)
            ) {
                val prod = selectedProduct
                if (prod == null) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable { showProductDialog = true }
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.AssignmentReturn,
                            contentDescription = null,
                            tint = Color(0xFF9CA3AF),
                            modifier = Modifier.size(36.dp)
                        )
                        Text(
                            text = "Ketuk ikon keranjang di atas atau di sini untuk memilih produk diretur",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF6B7280),
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        item {
                            PosLedgerTableRow(
                                index = 0,
                                productName = prod.name,
                                subtitle = "@${formatRupiah(priceNum)} • ${if (refundMethod == "KEMBALI_KAS") "Kembali ke Kas" else "Potong Hutang"}",
                                qtyText = formatQty(qtyNum),
                                amount = totalRefundPreview,
                                onClick = { showEditRowDialog = true },
                                tag = "return_item_row_0"
                            )
                        }
                    }
                }

                if (error != null) {
                    Surface(
                        color = LossRedBg,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .clickable { error = null }
                    ) {
                        Text(
                            text = error!!,
                            color = LossRed,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            }

            PosTotalFooterBar(
                totalAmount = totalRefundPreview,
                statusBadgeText = if (refundMethod == "KEMBALI_KAS") "Masuk Kas" else "Potong Hutang",
                tag = "save_return_button",
                onClick = { submitReturn() }
            )
        }
    }

    if (showPartyDialog) {
        PosPartyPickerDialog(
            title = "Pilih Pemasok Retur",
            fieldLabel = "Nama Pemasok *",
            dropdownLabel = "Supplier Tersimpan",
            partyOptions = suppliers.map { it.id to it.name },
            currentName = supplierName,
            allowDefaultGeneral = false,
            onConfirm = { _, pickedName ->
                supplierName = pickedName
                error = null
                showPartyDialog = false
            },
            onDismiss = { showPartyDialog = false }
        )
    }

    if (showProductDialog) {
        PosProductPickerDialog(
            title = "Pilih Produk Diretur",
            products = products,
            cartQtyByProductId = selectedProduct?.let { mapOf(it.id to qtyNum) } ?: emptyMap(),
            defaultPriceProvider = { it.buyPrice },
            priceLabel = "Harga/Unit (Rp)",
            pricePrefixInGrid = "Modal ",
            tapTileAddsDirectly = false,
            tagPrefix = "quick_select_return",
            onAddProduct = { prod, qty, unitPrice ->
                selectedProduct = prod
                qtyStr = formatQty(qty).replace(",", ".")
                priceStr = unitPrice.toLong().toString()
                error = null
                showProductDialog = false
            },
            onDismiss = { showProductDialog = false }
        )
    }

    if (showEditRowDialog && selectedProduct != null) {
        val prod = selectedProduct!!
        PosEditItemDialog(
            productName = prod.name,
            unit = prod.unit,
            initialQty = qtyNum.coerceAtLeast(1.0),
            initialPrice = priceNum,
            priceLabel = "Harga/Unit (Rp)",
            onUpdate = { newQty, newPrice ->
                qtyStr = formatQty(newQty).replace(",", ".")
                priceStr = newPrice.toLong().toString()
                showEditRowDialog = false
            },
            onDelete = {
                selectedProduct = null
                showEditRowDialog = false
            },
            onDismiss = { showEditRowDialog = false }
        )
    }

    if (showDateDialog) {
        EditTransactionDateDialog(
            title = "Atur Tanggal & Waktu Retur",
            subtitle = supplierName.ifBlank { "Retur Pembelian" },
            initialMillis = transactionDateMillis,
            tagPrefix = "return_create",
            onConfirm = { newMillis ->
                transactionDateMillis = newMillis
                showDateDialog = false
            },
            onDismiss = { showDateDialog = false }
        )
    }

    if (showOptionsDialog) {
        var cashMenu by remember { mutableStateOf(false) }
        var hutangMenu by remember { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { showOptionsDialog = false },
            title = { Text("Opsi Retur & Keuangan") },
            text = {
                Column(
                    modifier = Modifier
                        .heightIn(max = 460.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = refundMethod == "KEMBALI_KAS",
                            onClick = { refundMethod = "KEMBALI_KAS" },
                            label = { Text("Kembali ke Kas") }
                        )
                        FilterChip(
                            selected = refundMethod == "POTONG_HUTANG",
                            onClick = { refundMethod = "POTONG_HUTANG" },
                            label = { Text("Potong Hutang") }
                        )
                    }
                    if (refundMethod == "KEMBALI_KAS" && cashBoxes.isNotEmpty()) {
                        ExposedDropdownMenuBox(expanded = cashMenu, onExpandedChange = { cashMenu = it }) {
                            OutlinedTextField(
                                value = selectedCashBox?.name ?: "Pilih Cash Box",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Cash Box Penerima") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(cashMenu) },
                                modifier = Modifier
                                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                    .fillMaxWidth()
                            )
                            ExposedDropdownMenu(expanded = cashMenu, onDismissRequest = { cashMenu = false }) {
                                cashBoxes.forEach { box ->
                                    DropdownMenuItem(
                                        text = { Text("${box.name} (${formatRupiah(box.balance)})") },
                                        onClick = {
                                            selectedCashBox = box
                                            cashMenu = false
                                        }
                                    )
                                }
                            }
                        }
                    } else if (refundMethod == "POTONG_HUTANG" && openHutangList.isNotEmpty()) {
                        ExposedDropdownMenuBox(expanded = hutangMenu, onExpandedChange = { hutangMenu = it }) {
                            OutlinedTextField(
                                value = selectedHutang?.let { "${it.partyName} (${formatRupiah(it.remainingAmount)})" } ?: "Pilih Hutang",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Potong Hutang Pemasok") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(hutangMenu) },
                                modifier = Modifier
                                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                    .fillMaxWidth()
                            )
                            ExposedDropdownMenu(expanded = hutangMenu, onDismissRequest = { hutangMenu = false }) {
                                openHutangList.forEach { debt ->
                                    DropdownMenuItem(
                                        text = { Text("${debt.partyName} • ${formatRupiah(debt.remainingAmount)}") },
                                        onClick = {
                                            selectedHutang = debt
                                            if (supplierName.isBlank()) supplierName = debt.partyName
                                            hutangMenu = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                    DateTimeSelectorSection(
                        title = "Tanggal & Waktu Transaksi",
                        millis = transactionDateMillis,
                        onMillisChange = { transactionDateMillis = it },
                        tagPrefix = "return_opt_date"
                    )
                    OutlinedTextField(
                        value = reason,
                        onValueChange = { reason = it },
                        label = { Text("Alasan Retur") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    TextButton(
                        onClick = {
                            showOptionsDialog = false
                            onNavigateToFinance()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Lihat Riwayat di Menu Keuangan")
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showOptionsDialog = false }) {
                    Text("Terapkan")
                }
            }
        )
    }

    savedReturn?.let { ret ->
        AlertDialog(
            onDismissRequest = { savedReturn = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = ProfitGreen)
                    Text("Retur Tersimpan")
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("${ret.productName} • ${formatQty(ret.quantity)} unit", fontWeight = FontWeight.SemiBold)
                    Text("Pemasok: ${ret.supplierName}")
                    Text("Waktu: ${formatDateTime(ret.transactionDateMillis)}", style = MaterialTheme.typography.bodySmall)
                    HorizontalDivider()
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Nilai Retur")
                        CurrencyText(amount = ret.totalRefund, color = ProfitGreen)
                    }
                    Text(
                        "Transaksi tercatat otomatis ke menu Keuangan.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        printingReturn = ret
                        savedReturn = null
                    },
                    modifier = Modifier.testTag("print_saved_return_button")
                ) {
                    Icon(Icons.Default.Bluetooth, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Cetak Struk")
                }
            },
            dismissButton = {
                TextButton(onClick = { savedReturn = null }) { Text("Selesai") }
            }
        )
    }

    printingReturn?.let { target ->
        BluetoothPrintDialog(
            document = target.toThermalReceipt(),
            storeSettings = storeSettings,
            onDismiss = { printingReturn = null }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun EditTransactionDateDialog(
    title: String,
    subtitle: String,
    initialMillis: Long,
    tagPrefix: String,
    onConfirm: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedMillis by remember(initialMillis) {
        mutableLongStateOf(if (initialMillis > 0L) initialMillis else System.currentTimeMillis())
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                DateTimeSelectorSection(
                    title = "Tanggal & Waktu Transaksi",
                    millis = selectedMillis,
                    onMillisChange = { selectedMillis = it },
                    tagPrefix = tagPrefix
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(selectedMillis) },
                modifier = Modifier.testTag("${tagPrefix}_confirm_button")
            ) {
                Text("Simpan Waktu")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        }
    )
}

@Composable
internal fun BoxPaddingEmpty(
    title: String,
    subtitle: String,
    icon: ImageVector
) {
    Column(modifier = Modifier.padding(16.dp)) {
        EmptyStateCard(title = title, subtitle = subtitle, icon = icon)
    }
}
