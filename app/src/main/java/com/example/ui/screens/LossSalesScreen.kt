package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.CashBox
import com.example.data.model.Merchant
import com.example.data.model.Product
import com.example.data.model.Sale
import com.example.data.model.SaleItem
import com.example.data.model.StoreSettings
import com.example.data.model.UiState
import com.example.ui.components.CurrencyText
import com.example.ui.components.ResponsiveScreenContainer
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

@Composable
fun LossSalesScreen(
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
    val draftInvoiceNo = remember(txNonce) { generateInvoiceNo("RUG") }

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
    val totalLoss = cart.filter { it.sellPrice < it.buyPrice }.sumOf { (it.buyPrice - it.sellPrice) * it.quantity }
    val cashTendered = cashTenderedStr.replace(",", ".").toDoubleOrNull() ?: 0.0
    val changeAmount = max(0.0, cashTendered - totalAmount)
    val dpAmount = (dpAmountStr.replace(",", ".").toDoubleOrNull() ?: 0.0).coerceIn(0.0, totalAmount)
    val cartQtyMap = remember(cart.toList()) {
        cart.groupBy { it.productId }.mapValues { (_, list) -> list.sumOf { it.quantity } }
    }

    fun resetForm() {
        cart.clear()
        cashTenderedStr = ""
        dpAmountStr = "0"
        notes = ""
        error = null
        transactionDateMillis = System.currentTimeMillis()
        txNonce++
    }

    fun submitLossSale() {
        val cust = customerName.trim().ifBlank { selectedMerchant?.name ?: "" }
        if (cust.isBlank()) {
            error = "Nama pembeli wajib diisi"
            return
        }
        if (cart.isEmpty()) {
            error = "Tambahkan minimal 1 item jual rugi"
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
            resetForm()
        }
    }

    ResponsiveScreenContainer {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
        ) {
            SlateTopHeaderBar(
                title = "Jual Rugi",
                onBack = onBack,
                backTag = "loss_sales_back"
            )
            PosSlateActionToolbar(
                tagPrefix = "loss_sale",
                partyContentDescription = "Pilih Pembeli",
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
                        error = "Tambahkan item jual rugi terlebih dahulu untuk cetak struk"
                    }
                },
                onSaveClick = { submitLossSale() }
            )

            PosMetadataRow(label = "ID", value = draftId)
            PosMetadataRow(label = "Nomor", value = draftInvoiceNo)
            PosMetadataRow(
                label = "Tanggal",
                value = formatDateTimeSlash(transactionDateMillis),
                onClick = { showDateDialog = true },
                tag = "loss_sale_date_row"
            )
            PosMetadataRow(
                label = "Customer",
                value = customerName,
                onClick = { showPartyDialog = true },
                tag = "loss_sale_customer_row"
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
                            text = "Ketuk ikon keranjang di atas atau di sini untuk memilih produk jual rugi",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF6B7280),
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        itemsIndexed(cart, key = { idx, item -> "${item.productId}_$idx" }) { idx, item ->
                            val itemLoss = max(0.0, (item.buyPrice - item.sellPrice) * item.quantity)
                            PosLedgerTableRow(
                                index = idx,
                                productName = item.productName,
                                subtitle = "@${formatRupiah(item.sellPrice)} • Rugi ${formatRupiah(itemLoss)}",
                                qtyText = formatQty(item.quantity),
                                amount = item.subtotal,
                                subtitleColor = LossRed,
                                onClick = { editingItemPair = idx to item },
                                tag = "loss_cart_row_$idx"
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
                    totalLoss > 0.0 -> "Rugi ${formatRupiah(totalLoss)}"
                    isPiutang -> "Tempo"
                    else -> null
                },
                tag = "save_loss_sale_button",
                onClick = { submitLossSale() }
            )
        }
    }

    if (showPartyDialog) {
        PosPartyPickerDialog(
            title = "Pilih Pembeli / Pedagang",
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
            title = "Pilih Produk Jual Rugi",
            products = products,
            cartQtyByProductId = cartQtyMap,
            defaultPriceProvider = { max(0.0, it.buyPrice * 0.9) },
            priceLabel = "Harga Jual Rugi (Rp)",
            pricePrefixInGrid = "Est. ",
            tapTileAddsDirectly = false,
            isLossSaleMode = true,
            tagPrefix = "quick_select_loss",
            onAddProduct = { prod, qty, sellPrice ->
                error = null
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
            priceLabel = "Harga Jual Rugi (Rp)",
            buyPriceFloorCheck = item.buyPrice,
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
            title = "Atur Tanggal & Waktu Jual Rugi",
            subtitle = customerName,
            initialMillis = transactionDateMillis,
            tagPrefix = "loss_sale_create",
            onConfirm = { newMillis ->
                transactionDateMillis = newMillis
                showDateDialog = false
            },
            onDismiss = { showDateDialog = false }
        )
    }

    if (showPaymentDialog) {
        PosPaymentOptionsDialog(
            title = "Pembayaran & Opsi Jual Rugi",
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
                submitLossSale()
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
                    Text("Jual Rugi Tersimpan")
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
                            Text("Kembalian", color = ProfitGreen)
                            Text(formatRupiah(changeOut ?: 0.0), color = ProfitGreen)
                        }
                    } else if (sale.remainingDebt > 0.0) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Sisa Piutang", color = WarningAmber)
                            Text(formatRupiah(sale.remainingDebt), color = WarningAmber)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        printingReceipt = receipt
                        savedSaleReceipt = null
                    }
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
            document = sale.toThermalReceipt(isLossSale = true, cashTendered = cashIn, changeAmount = changeOut),
            storeSettings = storeSettings,
            onDismiss = { printingReceipt = null }
        )
    }
}
