package com.example.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
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
import com.example.data.model.CashFlow
import com.example.data.model.UiState
import com.example.ui.components.ConfirmDeleteDialog
import com.example.ui.components.CurrencyText
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.LoadingCenter
import com.example.ui.components.ResponsiveScreenContainer
import com.example.ui.components.StatusBadge
import com.example.ui.theme.InfoBlue
import com.example.ui.theme.InfoBlueBg
import com.example.ui.theme.LossRed
import com.example.ui.theme.LossRedBg
import com.example.ui.theme.ProfitGreen
import com.example.ui.theme.ProfitGreenBg
import com.example.ui.theme.WarningAmber
import com.example.util.formatDateTime
import com.example.util.formatRupiah

@Composable
fun CashAndFlowScreen(
    cashBoxesState: UiState<List<CashBox>>,
    cashFlowsState: UiState<List<CashFlow>>,
    onCreateCashBox: (name: String, description: String, initialBalance: Double) -> Unit,
    onDeleteCashBox: (cashBoxId: String) -> Unit,
    onAdjustBalance: (cashBox: CashBox, isAddition: Boolean, amount: Double, description: String, transactionDateMillis: Long) -> Unit,
    onTransferBalance: (fromBox: CashBox, toBox: CashBox, amount: Double, description: String, transactionDateMillis: Long) -> Unit = { _, _, _, _, _ -> },
    onDeleteCashFlow: (flow: CashFlow) -> Unit
) {
    var showNewBoxDialog by remember { mutableStateOf(false) }
    var adjustMode by remember { mutableStateOf<Boolean?>(null) } // true=add, false=subtract
    var showTransferDialog by remember { mutableStateOf(false) }
    var flowFilter by remember { mutableStateOf("ALL") } // "ALL", "IN", "OUT", "SALE", "PURCHASE", "RETURN", "EXPENSE", "TRANSFER"
    var deleteBoxTarget by remember { mutableStateOf<CashBox?>(null) }
    var deleteFlowTarget by remember { mutableStateOf<CashFlow?>(null) }

    val cashBoxes = (cashBoxesState as? UiState.Success)?.data ?: emptyList()
    val cashFlows = (cashFlowsState as? UiState.Success)?.data ?: emptyList()

    ResponsiveScreenContainer {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Keuangan & Cash Box", style = MaterialTheme.typography.headlineSmall)
                    OutlinedButton(
                        onClick = { showNewBoxDialog = true },
                        modifier = Modifier.testTag("add_cashbox_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("Cash Box")
                    }
                }
            }

            // Total Cash Box Card
            item {
                val totalBalance = cashBoxes.sumOf { it.balance }
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            "Total Saldo Semua Cash Box",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                        )
                        CurrencyText(
                            amount = totalBalance,
                            color = MaterialTheme.colorScheme.onPrimary,
                            style = MaterialTheme.typography.displaySmall
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = { adjustMode = true },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = ProfitGreenBg,
                                    contentColor = ProfitGreen
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("add_balance_button")
                            ) {
                                Icon(Icons.Default.AddCircleOutline, contentDescription = null)
                                Spacer(Modifier.width(6.dp))
                                Text("Tambah Saldo")
                            }
                            Button(
                                onClick = { adjustMode = false },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = LossRedBg,
                                    contentColor = LossRed
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("reduce_balance_button")
                            ) {
                                Icon(Icons.Default.RemoveCircleOutline, contentDescription = null)
                                Spacer(Modifier.width(6.dp))
                                Text("Kurangi Saldo")
                            }
                        }
                        Button(
                            onClick = { showTransferDialog = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = InfoBlueBg,
                                contentColor = InfoBlue
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("transfer_balance_button")
                        ) {
                            Icon(Icons.Default.SwapHoriz, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("Pemindahan Saldo")
                        }
                    }
                }
            }

            // Cash Boxes Horizontal List
            item {
                Text("Daftar Cash Box", style = MaterialTheme.typography.titleMedium)
            }

            item {
                if (cashBoxes.isEmpty()) {
                    EmptyStateCard(
                        title = "Belum ada Cash Box",
                        subtitle = "Tambahkan Cash Box untuk memisahkan kas laci, bank, atau tabungan.",
                        icon = Icons.Default.AccountBalanceWallet
                    )
                } else {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(cashBoxes, key = { it.id }) { box ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                shape = MaterialTheme.shapes.medium,
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                                modifier = Modifier.width(230.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = box.name,
                                            style = MaterialTheme.typography.titleSmall,
                                            modifier = Modifier.weight(1f),
                                            maxLines = 1
                                        )
                                        IconButton(
                                            onClick = { deleteBoxTarget = box },
                                            modifier = Modifier
                                                .size(32.dp)
                                                .testTag("delete_cashbox_${box.id}")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.DeleteOutline,
                                                contentDescription = "Hapus Cash Box ${box.name}",
                                                tint = LossRed,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                    if (box.description.isNotBlank()) {
                                        Text(
                                            box.description,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1
                                        )
                                    }
                                    CurrencyText(amount = box.balance, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                }
            }

            // Arus Uang Section
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Arus Uang & Riwayat Transaksi", style = MaterialTheme.typography.titleMedium)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = flowFilter == "ALL",
                            onClick = { flowFilter = "ALL" },
                            label = { Text("Semua (${cashFlows.size})") },
                            modifier = Modifier.testTag("filter_flow_all")
                        )
                        FilterChip(
                            selected = flowFilter == "IN",
                            onClick = { flowFilter = "IN" },
                            label = { Text("Masuk") },
                            modifier = Modifier.testTag("filter_flow_in")
                        )
                        FilterChip(
                            selected = flowFilter == "OUT",
                            onClick = { flowFilter = "OUT" },
                            label = { Text("Keluar") },
                            modifier = Modifier.testTag("filter_flow_out")
                        )
                        FilterChip(
                            selected = flowFilter == "SALE",
                            onClick = { flowFilter = "SALE" },
                            label = { Text("Penjualan") },
                            modifier = Modifier.testTag("filter_flow_sale")
                        )
                        FilterChip(
                            selected = flowFilter == "PURCHASE",
                            onClick = { flowFilter = "PURCHASE" },
                            label = { Text("Pembelian") },
                            modifier = Modifier.testTag("filter_flow_purchase")
                        )
                        FilterChip(
                            selected = flowFilter == "RETURN",
                            onClick = { flowFilter = "RETURN" },
                            label = { Text("Retur") },
                            modifier = Modifier.testTag("filter_flow_return")
                        )
                        FilterChip(
                            selected = flowFilter == "EXPENSE",
                            onClick = { flowFilter = "EXPENSE" },
                            label = { Text("Pengeluaran") },
                            modifier = Modifier.testTag("filter_flow_expense")
                        )
                        FilterChip(
                            selected = flowFilter == "TRANSFER",
                            onClick = { flowFilter = "TRANSFER" },
                            label = { Text("Pindah Saldo") },
                            modifier = Modifier.testTag("filter_flow_transfer")
                        )
                    }
                }
            }

            when (cashFlowsState) {
                is UiState.Loading -> item { LoadingCenter() }
                is UiState.Error -> item { EmptyStateCard("Gagal memuat arus uang", cashFlowsState.message) }
                is UiState.Success -> {
                    val filtered = cashFlows.filter { flow ->
                        when (flowFilter) {
                            "IN" -> flow.flowType == "IN"
                            "OUT" -> flow.flowType == "OUT"
                            "SALE" -> flow.category == "PENJUALAN"
                            "PURCHASE" -> flow.category == "PEMBELIAN"
                            "RETURN" -> flow.category == "RETUR_PEMBELIAN"
                            "EXPENSE" -> flow.category == "PENGELUARAN"
                            "TRANSFER" -> flow.category == "PINDAH_SALDO"
                            else -> true
                        }
                    }
                    if (filtered.isEmpty()) {
                        item {
                            EmptyStateCard(
                                title = "Belum ada arus uang",
                                subtitle = "Semua transaksi penjualan, pembelian, retur, pengeluaran & mutasi kas tercatat otomatis di sini."
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
                                        .padding(start = 14.dp, top = 12.dp, bottom = 12.dp, end = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    StatusBadge(
                                        text = if (isIn) "MASUK" else "KELUAR",
                                        containerColor = if (isIn) ProfitGreenBg else LossRedBg,
                                        contentColor = if (isIn) ProfitGreen else LossRed
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            flow.description.ifBlank { flow.category },
                                            style = MaterialTheme.typography.titleSmall
                                        )
                                        Text(
                                            "${flow.category.replace("_", " ")} • ${flow.cashBoxName} • ${formatDateTime(flow.transactionDateMillis)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = if (isIn) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                                            contentDescription = null,
                                            tint = if (isIn) ProfitGreen else LossRed,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        CurrencyText(
                                            amount = flow.amount,
                                            color = if (isIn) ProfitGreen else LossRed,
                                            style = MaterialTheme.typography.titleSmall
                                        )
                                    }
                                    IconButton(
                                        onClick = { deleteFlowTarget = flow },
                                        modifier = Modifier
                                            .size(48.dp)
                                            .testTag("delete_cashflow_${flow.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DeleteOutline,
                                            contentDescription = "Hapus riwayat",
                                            tint = LossRed
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showNewBoxDialog) {
        var name by remember { mutableStateOf("") }
        var desc by remember { mutableStateOf("") }
        var balStr by remember { mutableStateOf("0") }
        var error by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showNewBoxDialog = false },
            title = { Text("Tambah Cash Box Baru") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nama Cash Box *") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Keterangan") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(
                        value = balStr,
                        onValueChange = { balStr = it },
                        label = { Text("Saldo Awal (Rp)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (error != null) Text(error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (name.isBlank()) { error = "Nama wajib diisi"; return@Button }
                    val initBal = balStr.toDoubleOrNull() ?: 0.0
                    onCreateCashBox(name.trim(), desc.trim(), initBal)
                    showNewBoxDialog = false
                }) { Text("Buat Cash Box") }
            },
            dismissButton = { TextButton(onClick = { showNewBoxDialog = false }) { Text("Batal") } }
        )
    }

    adjustMode?.let { isAddition ->
        AdjustBalanceDialog(
            isAddition = isAddition,
            cashBoxes = cashBoxes,
            onConfirm = { box, amt, desc, txMillis ->
                onAdjustBalance(box, isAddition, amt, desc, txMillis)
                adjustMode = null
            },
            onDismiss = { adjustMode = null }
        )
    }

    if (showTransferDialog) {
        TransferBalanceDialog(
            cashBoxes = cashBoxes,
            onConfirm = { fromBox, toBox, amt, desc, txMillis ->
                onTransferBalance(fromBox, toBox, amt, desc, txMillis)
                showTransferDialog = false
            },
            onDismiss = { showTransferDialog = false }
        )
    }

    deleteBoxTarget?.let { box ->
        ConfirmDeleteDialog(
            title = "Hapus Cash Box",
            message = "Hapus pilihan Cash Box \"${box.name}\" (saldo saat ini ${formatRupiah(box.balance)})? Riwayat arus uang tetap tersimpan.",
            onConfirm = {
                onDeleteCashBox(box.id)
                deleteBoxTarget = null
            },
            onDismiss = { deleteBoxTarget = null }
        )
    }

    deleteFlowTarget?.let { flow ->
        val extraNote = when (flow.category) {
            "PENGELUARAN" -> " Catatan pengeluaran terkait juga akan dihapus."
            "PINDAH_SALDO" -> " Kedua sisi mutasi pemindahan saldo (asal & tujuan) akan ikut dihapus."
            else -> ""
        }
        ConfirmDeleteDialog(
            title = "Hapus Riwayat Arus Uang",
            message = "Hapus \"${flow.description.ifBlank { flow.category }}\" (${formatRupiah(flow.amount)})? Saldo Cash Box terkait akan otomatis disesuaikan kembali.$extraNote",
            onConfirm = {
                onDeleteCashFlow(flow)
                deleteFlowTarget = null
            },
            onDismiss = { deleteFlowTarget = null }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AdjustBalanceDialog(
    isAddition: Boolean,
    cashBoxes: List<CashBox>,
    onConfirm: (CashBox, Double, String, Long) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedBox by remember { mutableStateOf(cashBoxes.firstOrNull()) }
    LaunchedEffect(cashBoxes) {
        if (selectedBox == null) selectedBox = cashBoxes.firstOrNull()
    }
    var boxMenu by remember { mutableStateOf(false) }
    var amountStr by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var transactionDateMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isAddition) "Penambahan Saldo" else "Pengurangan Saldo") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                DateTimeSelectorSection(
                    title = "Waktu & Tanggal Transaksi",
                    millis = transactionDateMillis,
                    onMillisChange = { transactionDateMillis = it },
                    tagPrefix = if (isAddition) "cash_add" else "cash_sub"
                )
                if (cashBoxes.isEmpty()) {
                    Text("Buat Cash Box terlebih dahulu.", color = LossRed)
                } else {
                    ExposedDropdownMenuBox(expanded = boxMenu, onExpandedChange = { boxMenu = it }) {
                        OutlinedTextField(
                            value = selectedBox?.let { "${it.name} (${formatRupiah(it.balance)})" } ?: "Pilih Cash Box",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Cash Box") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(boxMenu) },
                            modifier = Modifier
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(expanded = boxMenu, onDismissRequest = { boxMenu = false }) {
                            cashBoxes.forEach { box ->
                                DropdownMenuItem(
                                    text = { Text("${box.name} (${formatRupiah(box.balance)})") },
                                    onClick = { selectedBox = box; boxMenu = false }
                                )
                            }
                        }
                    }
                    OutlinedTextField(
                        value = amountStr,
                        onValueChange = { amountStr = it },
                        label = { Text("Nominal (Rp) *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Keterangan / Alasan") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                if (error != null) Text(error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val box = selectedBox
                    val amt = amountStr.toDoubleOrNull() ?: 0.0
                    if (box == null || amt <= 0.0) {
                        error = "Pilih Cash Box dan masukkan nominal > 0"
                        return@Button
                    }
                    onConfirm(box, amt, description, transactionDateMillis)
                },
                modifier = Modifier.testTag("confirm_adjust_balance_button")
            ) { Text("Simpan") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun TransferBalanceDialog(
    cashBoxes: List<CashBox>,
    onConfirm: (fromBox: CashBox, toBox: CashBox, amount: Double, description: String, transactionDateMillis: Long) -> Unit,
    onDismiss: () -> Unit
) {
    var fromBox by remember { mutableStateOf(cashBoxes.firstOrNull()) }
    var toBox by remember { mutableStateOf(cashBoxes.getOrNull(1)) }
    LaunchedEffect(cashBoxes) {
        if (fromBox == null) fromBox = cashBoxes.firstOrNull()
        if (toBox == null || toBox?.id == fromBox?.id) {
            toBox = cashBoxes.firstOrNull { it.id != fromBox?.id }
        }
    }
    var fromMenu by remember { mutableStateOf(false) }
    var toMenu by remember { mutableStateOf(false) }
    var amountStr by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var transactionDateMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var error by remember { mutableStateOf<String?>(null) }

    val parsedAmt = (amountStr.replace(",", ".").toDoubleOrNull() ?: 0.0).coerceAtLeast(0.0)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.SwapHoriz, contentDescription = null, tint = InfoBlue)
                Text("Pemindahan Saldo")
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (cashBoxes.size < 2) {
                    Text(
                        "Minimal diperlukan 2 Cash Box untuk memindahkan saldo. Tambahkan Cash Box baru terlebih dahulu.",
                        color = LossRed,
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else {
                    DateTimeSelectorSection(
                        title = "Waktu & Tanggal Pemindahan",
                        millis = transactionDateMillis,
                        onMillisChange = { transactionDateMillis = it },
                        tagPrefix = "cash_transfer"
                    )

                    ExposedDropdownMenuBox(expanded = fromMenu, onExpandedChange = { fromMenu = it }) {
                        OutlinedTextField(
                            value = fromBox?.let { "${it.name} (${formatRupiah(it.balance)})" } ?: "Pilih Cash Box Asal",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Dari Cash Box (Asal)") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(fromMenu) },
                            modifier = Modifier
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                .fillMaxWidth()
                                .testTag("transfer_from_box")
                        )
                        ExposedDropdownMenu(expanded = fromMenu, onDismissRequest = { fromMenu = false }) {
                            cashBoxes.forEach { box ->
                                DropdownMenuItem(
                                    text = { Text("${box.name} (${formatRupiah(box.balance)})") },
                                    onClick = {
                                        val prevFrom = fromBox
                                        fromBox = box
                                        if (toBox?.id == box.id) {
                                            toBox = prevFrom?.takeIf { it.id != box.id }
                                                ?: cashBoxes.firstOrNull { it.id != box.id }
                                        }
                                        fromMenu = false
                                        error = null
                                    }
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        TextButton(
                            onClick = {
                                val oldFrom = fromBox
                                val oldTo = toBox
                                fromBox = oldTo
                                toBox = oldFrom
                                error = null
                            },
                            modifier = Modifier.testTag("swap_transfer_boxes_button")
                        ) {
                            Icon(Icons.Default.SwapVert, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Tukar Asal & Tujuan")
                        }
                    }

                    ExposedDropdownMenuBox(expanded = toMenu, onExpandedChange = { toMenu = it }) {
                        OutlinedTextField(
                            value = toBox?.let { "${it.name} (${formatRupiah(it.balance)})" } ?: "Pilih Cash Box Tujuan",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Ke Cash Box (Tujuan)") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(toMenu) },
                            modifier = Modifier
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                .fillMaxWidth()
                                .testTag("transfer_to_box")
                        )
                        ExposedDropdownMenu(expanded = toMenu, onDismissRequest = { toMenu = false }) {
                            cashBoxes.forEach { box ->
                                DropdownMenuItem(
                                    text = { Text("${box.name} (${formatRupiah(box.balance)})") },
                                    onClick = {
                                        val prevTo = toBox
                                        toBox = box
                                        if (fromBox?.id == box.id) {
                                            fromBox = prevTo?.takeIf { it.id != box.id }
                                                ?: cashBoxes.firstOrNull { it.id != box.id }
                                        }
                                        toMenu = false
                                        error = null
                                    }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = amountStr,
                        onValueChange = { amountStr = it; error = null },
                        label = { Text("Nominal Pemindahan (Rp) *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("transfer_amount_input")
                    )

                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        val quickNominals = buildList {
                            add("50rb" to 50_000L)
                            add("100rb" to 100_000L)
                            add("500rb" to 500_000L)
                            val fromBal = fromBox?.balance?.toLong() ?: 0L
                            if (fromBal > 0L) {
                                add("Semua" to fromBal)
                            }
                        }
                        quickNominals.forEach { (label, valAmt) ->
                            OutlinedButton(onClick = { amountStr = valAmt.toString(); error = null }) {
                                Text(label)
                            }
                        }
                    }

                    val currentFrom = fromBox
                    val currentTo = toBox
                    if (currentFrom != null && currentTo != null && currentFrom.id != currentTo.id && parsedAmt > 0.0) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    "${currentFrom.name}: ${formatRupiah(currentFrom.balance)} -> ${formatRupiah(currentFrom.balance - parsedAmt)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (currentFrom.balance - parsedAmt < 0.0) WarningAmber else LossRed,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    "${currentTo.name}: ${formatRupiah(currentTo.balance)} -> ${formatRupiah(currentTo.balance + parsedAmt)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = ProfitGreen,
                                    fontWeight = FontWeight.Medium
                                )
                                if (parsedAmt > currentFrom.balance) {
                                    Text(
                                        "Perhatian: nominal melebihi saldo ${currentFrom.name} saat ini.",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = LossRed
                                    )
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Keterangan / Catatan") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("transfer_description_input")
                    )
                }
                if (error != null) {
                    Text(error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            if (cashBoxes.size >= 2) {
                Button(
                    onClick = {
                        val src = fromBox
                        val dst = toBox
                        val amt = amountStr.replace(",", ".").toDoubleOrNull() ?: 0.0
                        if (src == null || dst == null) {
                            error = "Pilih Cash Box asal dan tujuan"
                            return@Button
                        }
                        if (src.id == dst.id) {
                            error = "Cash Box asal dan tujuan harus berbeda"
                            return@Button
                        }
                        if (amt <= 0.0) {
                            error = "Masukkan nominal pemindahan > 0"
                            return@Button
                        }
                        onConfirm(src, dst, amt, description, transactionDateMillis)
                    },
                    modifier = Modifier.testTag("confirm_transfer_balance_button")
                ) {
                    Text("Pindahkan Saldo")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        }
    )
}


