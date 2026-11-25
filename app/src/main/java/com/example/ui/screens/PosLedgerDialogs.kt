package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.CashBox
import com.example.data.model.Product
import com.example.ui.components.CurrencyText
import com.example.ui.components.EmptyStateCard
import com.example.ui.theme.LossRed
import com.example.ui.theme.LossRedBg
import com.example.ui.theme.ProfitGreen
import com.example.ui.theme.ProfitGreenBg
import com.example.util.formatQty
import com.example.util.formatRupiah
import kotlin.math.max

@Composable
fun PosPartyPickerDialog(
    title: String,
    fieldLabel: String,
    dropdownLabel: String,
    partyOptions: List<Pair<String, String>>, // id to display label
    currentName: String,
    allowDefaultGeneral: Boolean,
    onConfirm: (selectedId: String?, partyName: String) -> Unit,
    onDismiss: () -> Unit
) {
    var nameInput by remember(currentName) { mutableStateOf(currentName) }
    var selectedId by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = nameInput,
                    onValueChange = {
                        nameInput = it
                        selectedId = null
                    },
                    label = { Text(fieldLabel) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("pos_party_name_input")
                )
                if (allowDefaultGeneral) {
                    FilterChip(
                        selected = nameInput.equals("Pelanggan Umum", ignoreCase = true),
                        onClick = {
                            selectedId = null
                            nameInput = "Pelanggan Umum"
                        },
                        label = { Text("Pelanggan Umum") }
                    )
                }
                if (partyOptions.isNotEmpty()) {
                    Text(dropdownLabel, style = MaterialTheme.typography.labelMedium)
                    partyOptions.forEach { (id, label) ->
                        val pureName = label.substringBefore(" (")
                        val isSelected = selectedId == id || nameInput.equals(pureName, ignoreCase = true)
                        OutlinedButton(
                            onClick = {
                                selectedId = id
                                nameInput = pureName
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = label,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
                if (error != null) {
                    Text(error!!, color = LossRed, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val trimmed = nameInput.trim()
                    if (trimmed.isBlank()) {
                        error = "Nama wajib diisi"
                        return@Button
                    }
                    onConfirm(selectedId, trimmed)
                },
                modifier = Modifier.testTag("pos_party_confirm")
            ) {
                Text("Pilih")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        }
    )
}

@Composable
fun PosProductPickerDialog(
    title: String,
    products: List<Product>,
    cartQtyByProductId: Map<String, Double>,
    defaultPriceProvider: (Product) -> Double,
    priceLabel: String,
    pricePrefixInGrid: String = "",
    tapTileAddsDirectly: Boolean,
    isLossSaleMode: Boolean = false,
    tagPrefix: String,
    onAddProduct: (Product, Double, Double) -> Unit,
    onDismiss: () -> Unit
) {
    var search by remember { mutableStateOf("") }
    var pickingProduct by remember(products) { mutableStateOf(products.firstOrNull()) }
    var qtyStr by remember { mutableStateOf("1") }
    var priceStr by remember(pickingProduct) {
        mutableStateOf(
            pickingProduct?.let { defaultPriceProvider(it).toLong().toString() } ?: ""
        )
    }
    var localInfo by remember { mutableStateOf<String?>(null) }
    var localError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 480.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (products.isEmpty()) {
                    EmptyStateCard(
                        title = "Belum ada Data Produk",
                        subtitle = "Tambahkan produk di menu Data Produk terlebih dahulu."
                    )
                } else {
                    Text(
                        text = if (tapTileAddsDirectly) {
                            "Ketuk produk untuk +1 langsung, atau atur jumlah di bawah:"
                        } else {
                            "Ketuk produk untuk memilih, lalu atur jumlah & harga di bawah:"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    PosProductTileGrid(
                        products = products,
                        searchQuery = search,
                        onSearchChange = { search = it },
                        selectedProductId = pickingProduct?.id,
                        cartQtyByProductId = cartQtyByProductId,
                        priceProvider = defaultPriceProvider,
                        priceLabelPrefix = pricePrefixInGrid,
                        tagPrefix = tagPrefix,
                        onTileTap = { prod ->
                            pickingProduct = prod
                            val defPrice = defaultPriceProvider(prod)
                            priceStr = defPrice.toLong().toString()
                            localError = null
                            if (tapTileAddsDirectly) {
                                onAddProduct(prod, 1.0, defPrice)
                                localInfo = "+1 ${prod.name} ditambahkan"
                            } else {
                                localInfo = null
                            }
                        }
                    )

                    if (localInfo != null) {
                        Surface(color = ProfitGreenBg, shape = MaterialTheme.shapes.small, modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = localInfo!!,
                                color = ProfitGreen,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }

                    HorizontalDivider()
                    Text(
                        text = pickingProduct?.let { "Atur: ${it.name} (Modal ${formatRupiah(it.buyPrice)})" } ?: "Atur Jumlah & Harga",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = qtyStr,
                            onValueChange = { qtyStr = it },
                            label = { Text("Jumlah") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = priceStr,
                            onValueChange = { priceStr = it },
                            label = { Text(priceLabel) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (localError != null) {
                        Text(localError!!, color = LossRed, style = MaterialTheme.typography.labelSmall)
                    }
                    OutlinedButton(
                        onClick = {
                            val prod = pickingProduct ?: return@OutlinedButton
                            val qty = qtyStr.replace(",", ".").toDoubleOrNull() ?: 0.0
                            val price = priceStr.replace(",", ".").toDoubleOrNull() ?: defaultPriceProvider(prod)
                            if (qty <= 0.0) {
                                localError = "Jumlah harus > 0"
                                return@OutlinedButton
                            }
                            if (isLossSaleMode && price >= prod.buyPrice) {
                                localError = "Harga jual rugi harus di bawah modal (${formatRupiah(prod.buyPrice)})"
                                return@OutlinedButton
                            }
                            localError = null
                            onAddProduct(prod, qty, price)
                            localInfo = "${formatQty(qty)} ${prod.unit} ${prod.name} ditambahkan"
                            if (!tapTileAddsDirectly) {
                                onDismiss()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("${tagPrefix}_add_custom")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(if (tapTileAddsDirectly) "Tambah ke Daftar" else "Pilih Produk Ini")
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss, modifier = Modifier.testTag("${tagPrefix}_done")) {
                Text("Selesai")
            }
        }
    )
}

@Composable
fun PosEditItemDialog(
    productName: String,
    unit: String,
    initialQty: Double,
    initialPrice: Double,
    priceLabel: String,
    buyPriceFloorCheck: Double? = null,
    onUpdate: (newQty: Double, newPrice: Double) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    var qtyStr by remember(initialQty) { mutableStateOf(formatQty(initialQty).replace(",", ".")) }
    var priceStr by remember(initialPrice) { mutableStateOf(initialPrice.toLong().toString()) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(productName, maxLines = 2) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Sesuaikan Jumlah ($unit)", style = MaterialTheme.typography.labelMedium)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = {
                                val cur = qtyStr.replace(",", ".").toDoubleOrNull() ?: 1.0
                                if (cur > 1.0) qtyStr = formatQty(cur - 1.0).replace(",", ".")
                            }
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = "Kurangi")
                        }
                        IconButton(
                            onClick = {
                                val cur = qtyStr.replace(",", ".").toDoubleOrNull() ?: 0.0
                                qtyStr = formatQty(cur + 1.0).replace(",", ".")
                            }
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Tambah")
                        }
                    }
                }
                OutlinedTextField(
                    value = qtyStr,
                    onValueChange = { qtyStr = it },
                    label = { Text("Jumlah ($unit)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = priceStr,
                    onValueChange = { priceStr = it },
                    label = { Text(priceLabel) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (error != null) {
                    Text(error!!, color = LossRed, style = MaterialTheme.typography.labelSmall)
                }
                TextButton(
                    onClick = onDelete,
                    colors = ButtonDefaults.textButtonColors(contentColor = LossRed),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Hapus Item dari Daftar")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val q = qtyStr.replace(",", ".").toDoubleOrNull() ?: 0.0
                    val p = priceStr.replace(",", ".").toDoubleOrNull() ?: -1.0
                    if (q <= 0.0 || p < 0.0) {
                        error = "Jumlah harus > 0 dan harga valid"
                        return@Button
                    }
                    if (buyPriceFloorCheck != null && p >= buyPriceFloorCheck) {
                        error = "Harga jual rugi harus di bawah modal (${formatRupiah(buyPriceFloorCheck)})"
                        return@Button
                    }
                    onUpdate(q, p)
                }
            ) {
                Text("Simpan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PosPaymentOptionsDialog(
    title: String,
    totalAmount: Double,
    tempoLabel: String, // e.g., "Piutang / Tempo" or "Hutang Tempo"
    cashInputLabel: String, // e.g., "Uang Diterima / Tunai (Rp)" or "Uang Dibayar / Tunai (Rp)"
    cashBoxLabel: String,
    isTempo: Boolean,
    onIsTempoChange: (Boolean) -> Unit,
    cashTenderedStr: String,
    onCashTenderedChange: (String) -> Unit,
    dpAmountStr: String,
    onDpAmountChange: (String) -> Unit,
    cashBoxes: List<CashBox>,
    selectedCashBox: CashBox?,
    onSelectCashBox: (CashBox) -> Unit,
    notes: String,
    onNotesChange: (String) -> Unit,
    transactionDateMillis: Long,
    onDateMillisChange: (Long) -> Unit,
    onNavigateToFinance: () -> Unit,
    onConfirmPayAndSave: () -> Unit,
    onDismiss: () -> Unit
) {
    var cashBoxDropdown by remember { mutableStateOf(false) }
    val cashNum = cashTenderedStr.replace(",", ".").toDoubleOrNull() ?: 0.0
    val change = max(0.0, cashNum - totalAmount)
    val shortfall = max(0.0, totalAmount - cashNum)
    var localError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 490.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
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
                        Text("Total Tagihan", style = MaterialTheme.typography.labelLarge)
                        CurrencyText(amount = totalAmount, style = MaterialTheme.typography.titleMedium)
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = !isTempo,
                        onClick = { onIsTempoChange(false); localError = null },
                        label = { Text("Bayar Cash") }
                    )
                    FilterChip(
                        selected = isTempo,
                        onClick = { onIsTempoChange(true); localError = null },
                        label = { Text(tempoLabel) }
                    )
                }

                if (!isTempo) {
                    OutlinedTextField(
                        value = cashTenderedStr,
                        onValueChange = { onCashTenderedChange(it); localError = null },
                        label = { Text(cashInputLabel) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("pos_dialog_cash_input")
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = totalAmount > 0.0 && cashNum == totalAmount,
                            onClick = {
                                onCashTenderedChange(totalAmount.toLong().toString())
                                localError = null
                            },
                            label = { Text("Uang Pas") },
                            modifier = Modifier.testTag("pos_dialog_exact_cash")
                        )
                        listOf(10_000L to "+10rb", 50_000L to "+50rb", 100_000L to "+100rb").forEach { (nom, lbl) ->
                            OutlinedButton(onClick = {
                                onCashTenderedChange((cashNum.toLong() + nom).toString())
                                localError = null
                            }) {
                                Text(lbl)
                            }
                        }
                    }
                    if (totalAmount > 0.0) {
                        Surface(
                            color = if (cashNum >= totalAmount) ProfitGreenBg else LossRedBg,
                            shape = MaterialTheme.shapes.small,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = when {
                                        cashNum > totalAmount -> "Kembalian"
                                        cashNum == totalAmount -> "Uang Pas (Lunas)"
                                        else -> "Kurang Bayar"
                                    },
                                    color = if (cashNum >= totalAmount) ProfitGreen else LossRed,
                                    fontWeight = FontWeight.SemiBold
                                )
                                CurrencyText(
                                    amount = if (cashNum >= totalAmount) change else shortfall,
                                    color = if (cashNum >= totalAmount) ProfitGreen else LossRed
                                )
                            }
                        }
                    }
                } else {
                    OutlinedTextField(
                        value = dpAmountStr,
                        onValueChange = onDpAmountChange,
                        label = { Text("Uang Muka / DP (Rp)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (cashBoxes.isNotEmpty()) {
                    ExposedDropdownMenuBox(
                        expanded = cashBoxDropdown,
                        onExpandedChange = { cashBoxDropdown = it }
                    ) {
                        OutlinedTextField(
                            value = selectedCashBox?.let { "${it.name} (${formatRupiah(it.balance)})" } ?: "Pilih Cash Box",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(cashBoxLabel) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(cashBoxDropdown) },
                            modifier = Modifier
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = cashBoxDropdown,
                            onDismissRequest = { cashBoxDropdown = false }
                        ) {
                            cashBoxes.forEach { box ->
                                DropdownMenuItem(
                                    text = { Text("${box.name} (${formatRupiah(box.balance)})") },
                                    onClick = {
                                        onSelectCashBox(box)
                                        cashBoxDropdown = false
                                    }
                                )
                            }
                        }
                    }
                }

                DateTimeSelectorSection(
                    title = "Tanggal & Waktu Transaksi",
                    millis = transactionDateMillis,
                    onMillisChange = onDateMillisChange,
                    tagPrefix = "pos_dialog_date"
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = onNotesChange,
                    label = { Text("Catatan") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                TextButton(
                    onClick = {
                        onDismiss()
                        onNavigateToFinance()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Lihat Riwayat di Menu Keuangan")
                }

                if (localError != null) {
                    Text(localError!!, color = LossRed, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (totalAmount <= 0.0) {
                        onDismiss()
                        return@Button
                    }
                    if (!isTempo && cashNum < totalAmount) {
                        localError = "Uang tunai kurang dari total tagihan. Ketuk 'Uang Pas' atau masukkan nominal yang sesuai."
                        return@Button
                    }
                    onConfirmPayAndSave()
                },
                modifier = Modifier.testTag("pos_dialog_save_button")
            ) {
                Text(if (totalAmount > 0.0) "Bayar & Simpan" else "Tutup")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Kembali") }
        }
    )
}
