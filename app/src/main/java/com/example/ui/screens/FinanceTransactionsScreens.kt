package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AddBox
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoneyOff
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.RequestQuote
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CashBox
import com.example.data.model.DebtRecord
import com.example.data.model.Expense
import com.example.data.model.Merchant
import com.example.data.model.StoreSettings
import com.example.data.model.Supplier
import com.example.data.model.UiState
import com.example.ui.components.ConfirmDeleteDialog
import com.example.ui.components.CurrencyText
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.LoadingCenter
import com.example.ui.components.ResponsiveScreenContainer
import com.example.ui.theme.LedgerCellBorder
import com.example.ui.theme.LedgerDateText
import com.example.ui.theme.LossRed
import com.example.ui.theme.LossRedBg
import com.example.ui.theme.PaidBlue
import com.example.ui.theme.ProfitGreen
import com.example.ui.theme.ProfitGreenBg
import com.example.ui.theme.SlateBarBg
import com.example.ui.theme.SlateBarText
import com.example.ui.theme.SlateDivider
import com.example.ui.theme.SlateToolbarBg
import com.example.ui.theme.WarningAmber
import com.example.ui.theme.WarningAmberBg
import com.example.util.PayslipPrinter
import com.example.util.ReportPdfSection
import com.example.util.ThermalReceiptDocument
import com.example.util.formatDateTime
import com.example.util.formatDateTimeSlash
import com.example.util.formatLedgerNumber
import com.example.util.formatRupiah
import com.example.util.toThermalReceipt

// ==========================================
// 1. PENGELUARAN PERUSAHAAN / TOKO SCREEN (LANGSUNG KE KEUANGAN)
// ==========================================
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ExpensesScreen(
    cashBoxesState: UiState<List<CashBox>>,
    storeSettingsState: UiState<StoreSettings>,
    onCreateExpense: (
        title: String,
        category: String,
        amount: Double,
        cashBox: CashBox,
        notes: String,
        transactionDateMillis: Long,
        onCreated: (Expense) -> Unit
    ) -> Unit,
    onNavigateToFinance: () -> Unit,
    onBack: () -> Unit
) {
    val cashBoxes = (cashBoxesState as? UiState.Success)?.data ?: emptyList()
    val storeSettings = (storeSettingsState as? UiState.Success)?.data ?: StoreSettings()
    val context = LocalContext.current

    val scopes = listOf("Toko", "Perusahaan / Pabrik")
    val categories = listOf("Operasional", "Listrik & Air", "Kemasan", "Transportasi", "Bahan Penolong", "Sewa", "Lainnya")
    var selectedScope by remember { mutableStateOf("Toko") }
    var title by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Operasional") }
    var amountStr by remember { mutableStateOf("") }
    var selectedBox by remember { mutableStateOf(cashBoxes.firstOrNull()) }
    LaunchedEffect(cashBoxes) {
        if (selectedBox == null) selectedBox = cashBoxes.firstOrNull()
    }
    var boxMenu by remember { mutableStateOf(false) }
    var notes by remember { mutableStateOf("") }
    var transactionDateMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var error by remember { mutableStateOf<String?>(null) }

    var savedExpense by remember { mutableStateOf<Expense?>(null) }
    var printingExpense by remember { mutableStateOf<Expense?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pengeluaran Perusahaan & Toko") },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("expenses_back")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                },
                actions = {
                    TextButton(
                        onClick = onNavigateToFinance,
                        modifier = Modifier.testTag("expenses_to_finance_history")
                    ) {
                        Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Keuangan")
                    }
                }
            )
        }
    ) { padding ->
        ResponsiveScreenContainer(modifier = Modifier.padding(padding)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                PosRegisterDateHeader(
                    registerTitle = "PENGELUARAN ${selectedScope.uppercase()}",
                    subtitle = "Riwayat langsung masuk ke menu Keuangan & Arus Uang (dapat dicetak/dihapus dari Keuangan)",
                    transactionDateMillis = transactionDateMillis,
                    icon = Icons.Default.MoneyOff,
                    accentColor = LossRed
                )

                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        DateTimeSelectorSection(
                            title = "Tanggal & Waktu Transaksi",
                            millis = transactionDateMillis,
                            onMillisChange = { transactionDateMillis = it },
                            tagPrefix = "expense_create"
                        )
                        Text("Lingkup Pengeluaran", style = MaterialTheme.typography.labelMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            scopes.forEach { scope ->
                                FilterChip(
                                    selected = selectedScope == scope,
                                    onClick = { selectedScope = scope },
                                    label = { Text(scope) },
                                    modifier = Modifier.testTag("expense_scope_${scope.lowercase().replace(" ", "_").replace("/", "_")}")
                                )
                            }
                        }
                        OutlinedTextField(
                            value = title,
                            onValueChange = { title = it },
                            label = { Text("Nama / Keterangan Pengeluaran *") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text("Kategori", style = MaterialTheme.typography.labelMedium)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            categories.forEach { cat ->
                                FilterChip(
                                    selected = category == cat,
                                    onClick = { category = cat },
                                    label = { Text(cat) }
                                )
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
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(
                                20_000L to "20rb",
                                50_000L to "50rb",
                                100_000L to "100rb",
                                250_000L to "250rb",
                                500_000L to "500rb"
                            ).forEach { (nom, lbl) ->
                                OutlinedButton(onClick = { amountStr = nom.toString() }) {
                                    Text(lbl)
                                }
                            }
                        }
                        if (cashBoxes.isEmpty()) {
                            Text(
                                "Belum ada Cash Box. Buat Cash Box terlebih dahulu di menu Keuangan.",
                                color = LossRed,
                                style = MaterialTheme.typography.bodySmall
                            )
                        } else {
                            ExposedDropdownMenuBox(expanded = boxMenu, onExpandedChange = { boxMenu = it }) {
                                OutlinedTextField(
                                    value = selectedBox?.let { "${it.name} (${formatRupiah(it.balance)})" } ?: "Pilih Cash Box",
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Ambil dari Cash Box") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(boxMenu) },
                                    modifier = Modifier
                                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                        .fillMaxWidth()
                                )
                                ExposedDropdownMenu(expanded = boxMenu, onDismissRequest = { boxMenu = false }) {
                                    cashBoxes.forEach { box ->
                                        DropdownMenuItem(
                                            text = { Text("${box.name} (${formatRupiah(box.balance)})") },
                                            onClick = {
                                                selectedBox = box
                                                boxMenu = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                        OutlinedTextField(
                            value = notes,
                            onValueChange = { notes = it },
                            label = { Text("Catatan") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (error != null) {
                            Text(error!!, color = LossRed, style = MaterialTheme.typography.bodySmall)
                        }
                        Button(
                            onClick = {
                                val amt = amountStr.replace(",", ".").toDoubleOrNull() ?: 0.0
                                val box = selectedBox
                                if (title.isBlank() || amt <= 0.0 || box == null) {
                                    error = "Lengkapi nama, nominal > 0, dan Cash Box"
                                    return@Button
                                }
                                error = null
                                val scopedCategory = "$selectedScope • $category"
                                onCreateExpense(title.trim(), scopedCategory, amt, box, notes, transactionDateMillis) { created ->
                                    savedExpense = created
                                    title = ""
                                    amountStr = ""
                                    notes = ""
                                    transactionDateMillis = System.currentTimeMillis()
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("save_expense_button")
                        ) {
                            Text("Simpan Pengeluaran")
                        }
                    }
                }
            }
        }
    }

    savedExpense?.let { exp ->
        AlertDialog(
            onDismissRequest = { savedExpense = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = ProfitGreen)
                    Text("Pengeluaran Tersimpan")
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(exp.title, fontWeight = FontWeight.SemiBold)
                    Text("${exp.category} • ${exp.cashBoxName}", style = MaterialTheme.typography.bodySmall)
                    Text("Waktu: ${formatDateTime(exp.transactionDateMillis)}", style = MaterialTheme.typography.bodySmall)
                    CurrencyText(amount = exp.amount, color = LossRed)
                    Spacer(Modifier.padding(top = 4.dp))
                    OutlinedButton(
                        onClick = { PayslipPrinter.printExpense(context, exp, storeSettings) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Cetak PDF")
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        printingExpense = exp
                        savedExpense = null
                    },
                    modifier = Modifier.testTag("print_saved_expense_button")
                ) {
                    Icon(Icons.Default.Bluetooth, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Cetak")
                }
            },
            dismissButton = {
                TextButton(onClick = { savedExpense = null }) { Text("Transaksi Baru") }
            }
        )
    }

    printingExpense?.let { target ->
        BluetoothPrintDialog(
            document = target.toThermalReceipt(),
            storeSettings = storeSettings,
            onDismiss = { printingExpense = null }
        )
    }
}

private enum class DebtStatusFilter {
    ALL, PAID, UNPAID
}

// ==========================================
// 2. HUTANG PEMBELIAN & PIUTANG PEDAGANG SCREEN (TAMPILAN IMAGE 2)
// ==========================================
@Composable
fun DebtsScreen(
    initialTab: Int, // 0 = Hutang Pembelian, 1 = Piutang Pedagang
    debtsState: UiState<List<DebtRecord>>,
    merchantsState: UiState<List<Merchant>>,
    suppliersState: UiState<List<Supplier>>,
    cashBoxesState: UiState<List<CashBox>>,
    storeSettingsState: UiState<StoreSettings>,
    onPayDebt: (debt: DebtRecord, amount: Double, cashBox: CashBox, notes: String, paidAtMillis: Long) -> Unit,
    onPayDebtsByInstallmentPool: (debts: List<DebtRecord>, amount: Double, cashBox: CashBox, notes: String, paidAtMillis: Long) -> Unit = { _, _, _, _, _ -> },
    onSettleAllDebts: (debts: List<DebtRecord>, cashBox: CashBox, notes: String, paidAtMillis: Long) -> Unit,
    onUpdateDebtPaidDate: (debtId: String, newPaidMillis: Long) -> Unit,
    onCreateManualDebt: (debtType: String, partyId: String, partyName: String, amount: Double, notes: String, transactionDateMillis: Long) -> Unit,
    onDeleteDebt: (DebtRecord) -> Unit = {},
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var selectedTab by remember(initialTab) { mutableIntStateOf(initialTab) }
    var actionTarget by remember { mutableStateOf<DebtRecord?>(null) }
    var deleteTarget by remember { mutableStateOf<DebtRecord?>(null) }
    var payTarget by remember { mutableStateOf<DebtRecord?>(null) }
    var editPaidDateTarget by remember { mutableStateOf<DebtRecord?>(null) }
    var showAddManual by remember { mutableStateOf(false) }
    var showInstallmentPoolDialog by remember { mutableStateOf(false) }
    var showSettleAllDialog by remember { mutableStateOf(false) }
    var showPartySelectorDialog by remember { mutableStateOf(false) }
    var showUnpaidPickDialog by remember { mutableStateOf(false) }
    var showFloatingMenu by remember { mutableStateOf(false) }
    var showPrintPaidDebtsDialog by remember { mutableStateOf(false) }
    var printingDebt by remember { mutableStateOf<DebtRecord?>(null) }
    var printingBatchDoc by remember { mutableStateOf<ThermalReceiptDocument?>(null) }
    var nameSearchQuery by remember { mutableStateOf("") }
    var selectedPartyFilter by remember { mutableStateOf<String?>(null) }
    var statusFilter by remember { mutableStateOf(DebtStatusFilter.ALL) }

    val cashBoxes = (cashBoxesState as? UiState.Success)?.data ?: emptyList()
    val merchants = (merchantsState as? UiState.Success)?.data ?: emptyList()
    val suppliers = (suppliersState as? UiState.Success)?.data ?: emptyList()
    val storeSettings = (storeSettingsState as? UiState.Success)?.data ?: StoreSettings()
    val currentType = if (selectedTab == 0) "HUTANG" else "PIUTANG"
    val allDebts = (debtsState as? UiState.Success)?.data ?: emptyList()

    val allTabDebts = remember(allDebts, currentType) {
        allDebts.filter { it.debtType == currentType }
    }
    val partyNames = remember(allTabDebts) {
        allTabDebts.map { it.partyName.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .sortedBy { it.lowercase() }
    }
    val filteredList = remember(allTabDebts, nameSearchQuery, selectedPartyFilter, statusFilter) {
        allTabDebts.filter { debt ->
            val q = nameSearchQuery.trim()
            val matchesQuery = q.isBlank() ||
                debt.partyName.contains(q, ignoreCase = true) ||
                debt.invoiceNo.contains(q, ignoreCase = true) ||
                debt.id.contains(q, ignoreCase = true)
            val matchesChip = selectedPartyFilter == null ||
                debt.partyName.equals(selectedPartyFilter, ignoreCase = true)
            val isPaid = debt.status == "LUNAS" || debt.remainingAmount <= 0.0
            val matchesStatus = when (statusFilter) {
                DebtStatusFilter.ALL -> true
                DebtStatusFilter.PAID -> isPaid
                DebtStatusFilter.UNPAID -> !isPaid
            }
            matchesQuery && matchesChip && matchesStatus
        }
    }
    val unpaidFilteredList = remember(filteredList) {
        filteredList.filter { it.remainingAmount > 0.0 && it.status != "LUNAS" }
    }
    val totalRemaining = filteredList.sumOf { it.remainingAmount }
    val activePartyLabel = selectedPartyFilter
        ?: nameSearchQuery.trim().takeIf { it.isNotBlank() }
        ?: "Semua Nama"

    ResponsiveScreenContainer {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
        ) {
            SlateTopHeaderBar(
                title = if (selectedTab == 0) "Data Hutang" else "Data Piutang",
                onBack = onBack,
                backTag = "debts_back",
                trailingAction = {
                    IconButton(
                        onClick = { showAddManual = true },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("add_debt_fab")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddBox,
                            contentDescription = "Tambah Hutang atau Piutang",
                            tint = SlateBarText,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
            )

            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = SlateToolbarBg,
                contentColor = SlateBarText
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = {
                        selectedTab = 0
                        selectedPartyFilter = null
                    },
                    text = { Text("Hutang Pembelian", color = SlateBarText) },
                    modifier = Modifier.testTag("tab_hutang")
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = {
                        selectedTab = 1
                        selectedPartyFilter = null
                    },
                    text = { Text("Piutang Pedagang", color = SlateBarText) },
                    modifier = Modifier.testTag("tab_piutang")
                )
            }

            // Centered Party Name Selector Strip (matches "01. INOH" row in Image 2)
            Surface(
                color = Color.White,
                shadowElevation = 1.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showPartySelectorDialog = true }
                    .testTag("debt_party_selector")
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 44.dp)
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = activePartyLabel,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF111827),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Pilih Nama",
                            tint = Color(0xFF4B5563),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    HorizontalDivider(thickness = 1.dp, color = LedgerCellBorder)
                }
            }

            // Search Bar Strip (matches "cari id transaksi" in Image 2)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 44.dp)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = Color(0xFF9CA3AF),
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                        if (nameSearchQuery.isEmpty()) {
                            Text(
                                text = "cari id transaksi",
                                fontSize = 15.sp,
                                color = Color(0xFF6B7280)
                            )
                        }
                        BasicTextField(
                            value = nameSearchQuery,
                            onValueChange = { nameSearchQuery = it },
                            singleLine = true,
                            textStyle = TextStyle(fontSize = 15.sp, color = Color(0xFF111827)),
                            cursorBrush = SolidColor(SlateBarBg),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("debt_search_name")
                        )
                    }
                    if (nameSearchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = { nameSearchQuery = "" },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Hapus pencarian",
                                tint = Color(0xFF6B7280),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
                HorizontalDivider(thickness = 1.dp, color = LedgerCellBorder)
            }

            // Table Header: TANGGAL | NILAI | HAPUS
            Surface(
                color = SlateToolbarBg,
                contentColor = SlateBarText,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(34.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "TANGGAL",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = SlateBarText,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(0.43f)
                        )
                        VerticalDivider(thickness = 1.dp, color = Color.White.copy(alpha = 0.7f))
                        Text(
                            text = "NILAI",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = SlateBarText,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(0.57f)
                        )
                    }
                    VerticalDivider(thickness = 1.dp, color = Color.White.copy(alpha = 0.7f))
                    Text(
                        text = "HAPUS",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = SlateBarText,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.width(48.dp)
                    )
                }
            }

            // Main Ledger Content + Bottom-Left Floating Action Button
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(Color.White)
            ) {
                when (debtsState) {
                    is UiState.Loading -> LoadingCenter()
                    is UiState.Error -> BoxPaddingEmpty(
                        title = "Gagal memuat data",
                        subtitle = debtsState.message,
                        icon = Icons.Default.CreditCard
                    )
                    is UiState.Success -> {
                        if (filteredList.isEmpty()) {
                            BoxPaddingEmpty(
                                title = if (selectedTab == 0) "Tidak ada data hutang" else "Tidak ada data piutang",
                                subtitle = "Transaksi tempo atau catatan manual akan tampil di tabel ini.",
                                icon = if (selectedTab == 0) Icons.Default.CreditCard else Icons.Default.RequestQuote
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .testTag("debt_ledger_list"),
                                contentPadding = PaddingValues(bottom = 84.dp)
                            ) {
                                itemsIndexed(filteredList, key = { _, d -> d.id }) { _, debt ->
                                    val isPaid = debt.status == "LUNAS" || debt.remainingAmount <= 0.0
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(Color.White)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(IntrinsicSize.Min),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clickable { actionTarget = debt }
                                                    .testTag("debt_row_${debt.id}")
                                            ) {
                                                // Subrow 1: Transaction Date | Total Amount | Remaining Amount
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(IntrinsicSize.Min),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = formatDateTimeSlash(debt.transactionDateMillis),
                                                        fontSize = 12.sp,
                                                        color = LedgerDateText,
                                                        textAlign = TextAlign.Center,
                                                        modifier = Modifier
                                                            .weight(0.43f)
                                                            .padding(horizontal = 6.dp, vertical = 6.dp)
                                                    )
                                                    VerticalDivider(thickness = 1.dp, color = LedgerCellBorder, modifier = Modifier.fillMaxHeight())
                                                    Row(
                                                        modifier = Modifier
                                                            .weight(0.57f)
                                                            .height(IntrinsicSize.Min),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text(
                                                            text = formatLedgerNumber(debt.totalAmount),
                                                            fontSize = 12.sp,
                                                            color = Color(0xFF1F2937),
                                                            textAlign = TextAlign.End,
                                                            modifier = Modifier
                                                                .weight(1f)
                                                                .padding(horizontal = 6.dp, vertical = 6.dp)
                                                        )
                                                        VerticalDivider(thickness = 1.dp, color = LedgerCellBorder, modifier = Modifier.fillMaxHeight())
                                                        Text(
                                                            text = if (isPaid) "0" else formatLedgerNumber(debt.remainingAmount),
                                                            fontSize = 12.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = Color(0xFF111827),
                                                            textAlign = TextAlign.End,
                                                            modifier = Modifier
                                                                .weight(0.95f)
                                                                .padding(horizontal = 6.dp, vertical = 6.dp)
                                                        )
                                                    }
                                                }
                                                HorizontalDivider(thickness = 0.7.dp, color = LedgerCellBorder.copy(alpha = 0.7f))

                                                // Subrow 2: Paid/Due Date (+ party name if All) | Red "Total - Paid" OR Blue "LUNAS"
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(IntrinsicSize.Min),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Column(
                                                        modifier = Modifier
                                                            .weight(0.43f)
                                                            .padding(horizontal = 6.dp, vertical = 5.dp),
                                                        horizontalAlignment = Alignment.CenterHorizontally
                                                    ) {
                                                        Text(
                                                            text = if (debt.paidAtMillis > 0L) formatDateTimeSlash(debt.paidAtMillis) else "-",
                                                            fontSize = 12.sp,
                                                            color = LedgerDateText,
                                                            textAlign = TextAlign.Center
                                                        )
                                                        if (selectedPartyFilter == null) {
                                                            Text(
                                                                text = debt.partyName,
                                                                fontSize = 10.sp,
                                                                color = Color(0xFF6B7280),
                                                                maxLines = 1,
                                                                overflow = TextOverflow.Ellipsis
                                                            )
                                                        }
                                                    }
                                                    VerticalDivider(thickness = 1.dp, color = LedgerCellBorder, modifier = Modifier.fillMaxHeight())
                                                    Box(
                                                        modifier = Modifier
                                                            .weight(0.57f)
                                                            .padding(horizontal = 6.dp, vertical = 6.dp),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        if (isPaid) {
                                                            Text(
                                                                text = "LUNAS",
                                                                fontSize = 12.sp,
                                                                fontWeight = FontWeight.SemiBold,
                                                                color = PaidBlue
                                                            )
                                                        } else {
                                                            Text(
                                                                text = "${formatLedgerNumber(debt.totalAmount)} - ${formatLedgerNumber(debt.paidAmount)}",
                                                                fontSize = 12.sp,
                                                                fontWeight = FontWeight.SemiBold,
                                                                color = LossRed
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                            VerticalDivider(
                                                thickness = 1.dp,
                                                color = LedgerCellBorder,
                                                modifier = Modifier.fillMaxHeight()
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .width(48.dp)
                                                    .fillMaxHeight(),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                IconButton(
                                                    onClick = { deleteTarget = debt },
                                                    modifier = Modifier
                                                        .size(48.dp)
                                                        .testTag("delete_debt_${debt.id}")
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.DeleteOutline,
                                                        contentDescription = "Hapus transaksi ${debt.partyName}",
                                                        tint = LossRed,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }
                                            }
                                        }
                                        HorizontalDivider(thickness = 1.dp, color = LedgerCellBorder)
                                    }
                                }
                            }
                        }
                    }
                }

                // Bottom-Left Circular Action Menu Button (≡) matching Image 2
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 16.dp, bottom = 14.dp)
                ) {
                    Surface(
                        onClick = { showFloatingMenu = true },
                        shape = CircleShape,
                        color = SlateBarBg,
                        contentColor = SlateBarText,
                        shadowElevation = 6.dp,
                        modifier = Modifier
                            .size(56.dp)
                            .testTag("debt_floating_menu_button")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "Menu Aksi Hutang Piutang",
                                tint = SlateBarText,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = showFloatingMenu,
                        onDismissRequest = { showFloatingMenu = false },
                        containerColor = Color.White
                    ) {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = if (selectedTab == 0) "Bayar Cicilan (Uang Keluar)"
                                    else "Bayar Cicilan (Uang Masuk)",
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (unpaidFilteredList.isNotEmpty()) PaidBlue else Color(0xFF9CA3AF)
                                )
                            },
                            enabled = unpaidFilteredList.isNotEmpty(),
                            onClick = {
                                showFloatingMenu = false
                                showInstallmentPoolDialog = true
                            },
                            modifier = Modifier.testTag("debt_installment_menu_item")
                        )
                        DropdownMenuItem(
                            text = { Text(if (selectedTab == 0) "Bayar 1 Nota Hutang" else "Terima 1 Nota Piutang") },
                            enabled = unpaidFilteredList.isNotEmpty(),
                            onClick = {
                                showFloatingMenu = false
                                if (unpaidFilteredList.size == 1) {
                                    payTarget = unpaidFilteredList.first()
                                } else if (unpaidFilteredList.isNotEmpty()) {
                                    showUnpaidPickDialog = true
                                }
                            }
                        )
                        DropdownMenuItem(
                            text = {
                                Text(
                                    "Lunasi Semua ($activePartyLabel)",
                                    color = if (unpaidFilteredList.isNotEmpty()) ProfitGreen else Color(0xFF9CA3AF),
                                    fontWeight = FontWeight.SemiBold
                                )
                            },
                            enabled = unpaidFilteredList.isNotEmpty(),
                            onClick = {
                                showFloatingMenu = false
                                showSettleAllDialog = true
                            },
                            modifier = Modifier.testTag("settle_all_debts_button")
                        )
                        DropdownMenuItem(
                            text = {
                                val nextLabel = when (statusFilter) {
                                    DebtStatusFilter.ALL -> "Semua -> Belum Lunas"
                                    DebtStatusFilter.UNPAID -> "Belum Lunas -> Lunas"
                                    DebtStatusFilter.PAID -> "Lunas -> Semua"
                                }
                                Text("Filter Status ($nextLabel)")
                            },
                            onClick = {
                                statusFilter = when (statusFilter) {
                                    DebtStatusFilter.ALL -> DebtStatusFilter.UNPAID
                                    DebtStatusFilter.UNPAID -> DebtStatusFilter.PAID
                                    DebtStatusFilter.PAID -> DebtStatusFilter.ALL
                                }
                                showFloatingMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Export PDF") },
                            enabled = filteredList.isNotEmpty(),
                            onClick = {
                                showFloatingMenu = false
                                val title = if (selectedTab == 0) "Laporan Hutang Pembelian" else "Laporan Piutang Pedagang"
                                val rows = filteredList.map { d ->
                                    val st = if (d.status == "LUNAS" || d.remainingAmount <= 0.0) "LUNAS"
                                    else "Sisa ${formatRupiah(d.remainingAmount)}"
                                    "${formatDateTimeSlash(d.transactionDateMillis)} • ${d.partyName} • Total ${formatRupiah(d.totalAmount)} • Terbayar ${formatRupiah(d.paidAmount)} • $st"
                                }
                                PayslipPrinter.printReportPdf(
                                    context = context,
                                    storeSettings = storeSettings,
                                    reportTitle = title,
                                    periodSubtitle = "Filter: $activePartyLabel",
                                    sections = listOf(
                                        ReportPdfSection(
                                            title = "$title ($activePartyLabel)",
                                            summaryPairs = listOf(
                                                "Jumlah Catatan" to "${filteredList.size}",
                                                "Total Sisa" to formatRupiah(totalRemaining)
                                            ),
                                            rows = rows
                                        )
                                    )
                                )
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Cetak Thermal") },
                            enabled = filteredList.isNotEmpty(),
                            onClick = {
                                showFloatingMenu = false
                                printingDebt = filteredList.firstOrNull()
                            }
                        )
                        DropdownMenuItem(
                            text = {
                                Text(
                                    "Cetak Lunas (Filter Tgl & Nama)",
                                    color = if (allDebts.any { it.status == "LUNAS" || it.remainingAmount <= 0.0 }) ProfitGreen else Color(0xFF9CA3AF),
                                    fontWeight = FontWeight.SemiBold
                                )
                            },
                            enabled = allDebts.any { it.status == "LUNAS" || it.remainingAmount <= 0.0 },
                            onClick = {
                                showFloatingMenu = false
                                showPrintPaidDebtsDialog = true
                            },
                            modifier = Modifier.testTag("print_paid_debts_filtered_button")
                        )
                    }
                }
            }

            // Sticky Dark Slate Bottom Footer: TOTAL | <totalRemaining>
            Surface(
                color = SlateBarBg,
                contentColor = SlateBarText,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "TOTAL",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = SlateBarText,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(0.43f)
                        )
                        VerticalDivider(
                            thickness = 1.dp,
                            color = SlateDivider,
                            modifier = Modifier.height(24.dp)
                        )
                        Text(
                            text = formatLedgerNumber(totalRemaining),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = SlateBarText,
                            textAlign = TextAlign.End,
                            modifier = Modifier
                                .weight(0.57f)
                                .padding(horizontal = 12.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(49.dp))
                }
            }
        }
    }

    if (showPartySelectorDialog) {
        AlertDialog(
            onDismissRequest = { showPartySelectorDialog = false },
            title = { Text(if (selectedTab == 0) "Filter Nama Pemasok" else "Filter Nama Pedagang") },
            text = {
                Column(
                    modifier = Modifier
                        .heightIn(max = 400.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            selectedPartyFilter = null
                            showPartySelectorDialog = false
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("filter_debt_party_all")
                    ) {
                        Text(
                            text = "Semua Nama (${allTabDebts.size})",
                            fontWeight = if (selectedPartyFilter == null) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    partyNames.forEach { party ->
                        val unpaidSum = allTabDebts
                            .filter { it.partyName.equals(party, ignoreCase = true) }
                            .sumOf { it.remainingAmount }
                        OutlinedButton(
                            onClick = {
                                selectedPartyFilter = party
                                showPartySelectorDialog = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = party,
                                    fontWeight = if (selectedPartyFilter == party) FontWeight.Bold else FontWeight.Normal,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = if (unpaidSum <= 0.0) "LUNAS" else formatRupiah(unpaidSum),
                                    color = if (unpaidSum <= 0.0) PaidBlue else LossRed,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showPartySelectorDialog = false }) { Text("Tutup") }
            }
        )
    }

    if (showUnpaidPickDialog) {
        AlertDialog(
            onDismissRequest = { showUnpaidPickDialog = false },
            title = { Text(if (selectedTab == 0) "Pilih Hutang untuk Dibayar" else "Pilih Piutang untuk Diterima") },
            text = {
                Column(
                    modifier = Modifier
                        .heightIn(max = 380.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    unpaidFilteredList.forEach { debt ->
                        OutlinedButton(
                            onClick = {
                                showUnpaidPickDialog = false
                                payTarget = debt
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(debt.partyName, fontWeight = FontWeight.SemiBold)
                                    Text(formatDateTimeSlash(debt.transactionDateMillis), style = MaterialTheme.typography.labelSmall)
                                }
                                Text(formatRupiah(debt.remainingAmount), color = LossRed, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showUnpaidPickDialog = false }) { Text("Batal") }
            }
        )
    }

    actionTarget?.let { debt ->
        val isPaid = debt.status == "LUNAS" || debt.remainingAmount <= 0.0
        AlertDialog(
            onDismissRequest = { actionTarget = null },
            title = { Text(debt.partyName) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("No. Ref: ${debt.invoiceNo.ifBlank { debt.id }}")
                    Text("Tgl Transaksi: ${formatDateTimeSlash(debt.transactionDateMillis)}")
                    Text("Total: ${formatRupiah(debt.totalAmount)}")
                    Text("Terbayar: ${formatRupiah(debt.paidAmount)}", color = ProfitGreen)
                    Text(
                        text = if (isPaid) "Status: LUNAS" else "Sisa: ${formatRupiah(debt.remainingAmount)}",
                        color = if (isPaid) PaidBlue else LossRed,
                        fontWeight = FontWeight.Bold
                    )
                    if (debt.paidAtMillis > 0L) {
                        Text("Tgl Bayar: ${formatDateTimeSlash(debt.paidAtMillis)}")
                    }
                    Spacer(Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                printingDebt = debt
                                actionTarget = null
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cetak")
                        }
                        OutlinedButton(
                            onClick = {
                                deleteTarget = debt
                                actionTarget = null
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = LossRed, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Hapus", color = LossRed)
                        }
                    }
                }
            },
            confirmButton = {
                if (!isPaid) {
                    Button(
                        onClick = {
                            payTarget = debt
                            actionTarget = null
                        },
                        modifier = Modifier.testTag("pay_debt_${debt.id}")
                    ) {
                        Text(if (selectedTab == 0) "Bayar / Cicil" else "Cicil / Terima")
                    }
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (debt.paidAtMillis > 0L) {
                        TextButton(
                            onClick = {
                                editPaidDateTarget = debt
                                actionTarget = null
                            }
                        ) {
                            Text("Ubah Tgl Bayar")
                        }
                    }
                    TextButton(onClick = { actionTarget = null }) { Text("Tutup") }
                }
            }
        )
    }

    payTarget?.let { target ->
        PayDebtDialog(
            debt = target,
            cashBoxes = cashBoxes,
            onConfirm = { amt, box, notes, paidAtMillis ->
                onPayDebt(target, amt, box, notes, paidAtMillis)
                payTarget = null
            },
            onDismiss = { payTarget = null }
        )
    }

    if (showInstallmentPoolDialog) {
        DebtInstallmentPoolDialog(
            isHutang = selectedTab == 0,
            partyLabel = activePartyLabel,
            unpaidDebts = unpaidFilteredList,
            cashBoxes = cashBoxes,
            onConfirmPool = { amt, box, notes, paidAtMillis ->
                onPayDebtsByInstallmentPool(unpaidFilteredList, amt, box, notes, paidAtMillis)
                showInstallmentPoolDialog = false
            },
            onConfirmSingle = { target, amt, box, notes, paidAtMillis ->
                onPayDebt(target, amt, box, notes, paidAtMillis)
                showInstallmentPoolDialog = false
            },
            onDismiss = { showInstallmentPoolDialog = false }
        )
    }

    if (showSettleAllDialog) {
        SettleAllDebtsDialog(
            isHutang = selectedTab == 0,
            partyLabel = activePartyLabel,
            unpaidDebts = unpaidFilteredList,
            cashBoxes = cashBoxes,
            onConfirm = { box, notes, paidAtMillis ->
                onSettleAllDebts(unpaidFilteredList, box, notes, paidAtMillis)
                showSettleAllDialog = false
            },
            onDismiss = { showSettleAllDialog = false }
        )
    }

    editPaidDateTarget?.let { target ->
        EditTransactionDateDialog(
            title = "Ubah Waktu & Tanggal Bayar",
            subtitle = "${target.invoiceNo} • ${target.partyName}",
            initialMillis = target.paidAtMillis,
            tagPrefix = "debt_paid_edit",
            onConfirm = { newMillis ->
                onUpdateDebtPaidDate(target.id, newMillis)
                editPaidDateTarget = null
            },
            onDismiss = { editPaidDateTarget = null }
        )
    }

    if (showAddManual) {
        AddManualDebtDialog(
            debtType = currentType,
            merchants = merchants,
            suppliers = suppliers,
            onSave = { partyId, partyName, amt, notes, txMillis ->
                onCreateManualDebt(currentType, partyId, partyName, amt, notes, txMillis)
                showAddManual = false
            },
            onDismiss = { showAddManual = false }
        )
    }

    printingDebt?.let { target ->
        BluetoothPrintDialog(
            document = target.toThermalReceipt(),
            storeSettings = storeSettings,
            onDismiss = { printingDebt = null }
        )
    }

    if (showPrintPaidDebtsDialog) {
        PrintPaidDebtsFilterDialog(
            allDebts = allDebts,
            initialTab = selectedTab,
            initialPartyFilter = selectedPartyFilter,
            onPrintDocument = { doc ->
                showPrintPaidDebtsDialog = false
                printingBatchDoc = doc
            },
            onDismiss = { showPrintPaidDebtsDialog = false }
        )
    }

    printingBatchDoc?.let { doc ->
        BluetoothPrintDialog(
            document = doc,
            storeSettings = storeSettings,
            onDismiss = { printingBatchDoc = null }
        )
    }

    deleteTarget?.let { debt ->
        val label = if (debt.debtType == "HUTANG") "Hutang" else "Piutang"
        ConfirmDeleteDialog(
            title = "Hapus Transaksi $label",
            message = "Hapus transaksi ${debt.partyName} (${formatRupiah(debt.totalAmount)})? Riwayat terkait di menu Keuangan juga akan ikut terhapus dan saldo Cash Box disesuaikan.",
            onConfirm = {
                onDeleteDebt(debt)
                deleteTarget = null
            },
            onDismiss = { deleteTarget = null }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun DebtInstallmentPoolDialog(
    isHutang: Boolean,
    partyLabel: String,
    unpaidDebts: List<DebtRecord>,
    cashBoxes: List<CashBox>,
    onConfirmPool: (Double, CashBox, String, Long) -> Unit,
    onConfirmSingle: (DebtRecord, Double, CashBox, String, Long) -> Unit,
    onDismiss: () -> Unit
) {
    val sortedUnpaid = remember(unpaidDebts) {
        unpaidDebts.sortedBy { it.transactionDateMillis }
    }
    val totalUnpaid = sortedUnpaid.sumOf { it.remainingAmount }
    var usePoolMode by remember { mutableStateOf(true) }
    var chosenDebt by remember { mutableStateOf(sortedUnpaid.firstOrNull()) }
    var debtMenu by remember { mutableStateOf(false) }

    val activeTargetTotal = if (usePoolMode) totalUnpaid else (chosenDebt?.remainingAmount ?: totalUnpaid)
    var amountStr by remember { mutableStateOf("") }
    var selectedBox by remember { mutableStateOf(cashBoxes.firstOrNull()) }
    LaunchedEffect(cashBoxes) {
        if (selectedBox == null) selectedBox = cashBoxes.firstOrNull()
    }
    var boxMenu by remember { mutableStateOf(false) }
    var notes by remember {
        mutableStateOf(
            if (isHutang) "Cicilan hutang ($partyLabel)" else "Cicilan uang masuk ($partyLabel)"
        )
    }
    var paidAtMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var error by remember { mutableStateOf<String?>(null) }

    val parsedAmt = (amountStr.replace(",", ".").toDoubleOrNull() ?: 0.0).coerceAtLeast(0.0)
    val appliedAmt = minOf(parsedAmt, activeTargetTotal)
    val previewRemaining = (activeTargetTotal - appliedAmt).coerceAtLeast(0.0)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (isHutang) "Bayar Hutang Secara Cicilan"
                else "Terima Cicilan (Sesuai Uang Masuk)"
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    "$partyLabel • ${sortedUnpaid.size} tagihan belum lunas",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    "Total Sisa: ${formatRupiah(totalUnpaid)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = LossRed
                )

                if (sortedUnpaid.size > 1) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = usePoolMode,
                            onClick = { usePoolMode = true },
                            label = { Text("Otomatis Sesuai Uang Masuk") }
                        )
                        FilterChip(
                            selected = !usePoolMode,
                            onClick = { usePoolMode = false },
                            label = { Text("Pilih 1 Nota") }
                        )
                    }
                }

                if (!usePoolMode && sortedUnpaid.isNotEmpty()) {
                    ExposedDropdownMenuBox(expanded = debtMenu, onExpandedChange = { debtMenu = it }) {
                        OutlinedTextField(
                            value = chosenDebt?.let { "${it.partyName} • ${formatRupiah(it.remainingAmount)}" }
                                ?: "Pilih Nota",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Nota Tagihan") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(debtMenu) },
                            modifier = Modifier
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(expanded = debtMenu, onDismissRequest = { debtMenu = false }) {
                            sortedUnpaid.forEach { d ->
                                DropdownMenuItem(
                                    text = {
                                        Text("${d.partyName} (${formatDateTimeSlash(d.transactionDateMillis)}) - Sisa ${formatRupiah(d.remainingAmount)}")
                                    },
                                    onClick = { chosenDebt = d; debtMenu = false }
                                )
                            }
                        }
                    }
                }

                DateTimeSelectorSection(
                    title = if (isHutang) "Waktu & Tanggal Pembayaran" else "Waktu & Tanggal Uang Masuk",
                    millis = paidAtMillis,
                    onMillisChange = { paidAtMillis = it },
                    tagPrefix = "debt_cicilan_pool"
                )

                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it; error = null },
                    label = {
                        Text(
                            if (isHutang) "Nominal Uang Dibayarkan (Rp) *"
                            else "Nominal Uang Masuk Diterima (Rp) *"
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("debt_installment_pool_amount_input")
                )

                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(
                        "50rb" to 50_000L,
                        "100rb" to 100_000L,
                        "50%" to (activeTargetTotal * 0.5).toLong(),
                        "Lunas" to activeTargetTotal.toLong()
                    ).forEach { (label, valAmt) ->
                        if (valAmt > 0L) {
                            OutlinedButton(onClick = { amountStr = valAmt.toString(); error = null }) {
                                Text(label)
                            }
                        }
                    }
                }

                if (parsedAmt > 0.0) {
                    Text(
                        text = if (previewRemaining <= 0.001) "Sisa tagihan menjadi: Rp 0 (LUNAS)"
                        else "Diterapkan: ${formatRupiah(appliedAmt)} • Sisa menjadi: ${formatRupiah(previewRemaining)}",
                        color = if (previewRemaining <= 0.001) ProfitGreen else PaidBlue,
                        style = MaterialTheme.typography.labelMedium
                    )
                }

                if (cashBoxes.isEmpty()) {
                    Text(
                        "Belum ada Cash Box. Buat Cash Box terlebih dahulu di menu Keuangan.",
                        color = LossRed,
                        style = MaterialTheme.typography.bodySmall
                    )
                } else {
                    ExposedDropdownMenuBox(expanded = boxMenu, onExpandedChange = { boxMenu = it }) {
                        OutlinedTextField(
                            value = selectedBox?.let { "${it.name} (${formatRupiah(it.balance)})" } ?: "Pilih Cash Box",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(if (isHutang) "Ambil dari Cash Box" else "Masuk ke Cash Box") },
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
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Catatan") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (error != null) {
                    Text(error!!, color = LossRed, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amountStr.replace(",", ".").toDoubleOrNull() ?: 0.0
                    val box = selectedBox
                    if (amt <= 0.0 || box == null) {
                        error = "Masukkan nominal > 0 dan pilih Cash Box"
                        return@Button
                    }
                    if (usePoolMode) {
                        onConfirmPool(amt, box, notes, paidAtMillis)
                    } else {
                        val target = chosenDebt
                        if (target == null) {
                            error = "Pilih nota tagihan terlebih dahulu"
                            return@Button
                        }
                        onConfirmSingle(target, amt, box, notes, paidAtMillis)
                    }
                },
                modifier = Modifier.testTag("confirm_debt_installment_pool_button")
            ) {
                Text("Simpan Cicilan")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettleAllDebtsDialog(
    isHutang: Boolean,
    partyLabel: String,
    unpaidDebts: List<DebtRecord>,
    cashBoxes: List<CashBox>,
    onConfirm: (CashBox, String, Long) -> Unit,
    onDismiss: () -> Unit
) {
    val totalToSettle = unpaidDebts.sumOf { it.remainingAmount }
    var selectedBox by remember { mutableStateOf(cashBoxes.firstOrNull()) }
    LaunchedEffect(cashBoxes) {
        if (selectedBox == null) selectedBox = cashBoxes.firstOrNull()
    }
    var boxMenu by remember { mutableStateOf(false) }
    var notes by remember { mutableStateOf("Pelunasan sekaligus ($partyLabel)") }
    var paidAtMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Lunasi Semua ${if (isHutang) "Hutang" else "Piutang"}") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    "Melunasi ${unpaidDebts.size} tagihan untuk \"$partyLabel\" dengan total:",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    formatRupiah(totalToSettle),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = LossRed
                )
                DateTimeSelectorSection(
                    title = "Waktu & Tanggal Pelunasan",
                    millis = paidAtMillis,
                    onMillisChange = { paidAtMillis = it },
                    tagPrefix = "settle_all"
                )
                if (cashBoxes.isEmpty()) {
                    Text(
                        "Belum ada Cash Box. Buat Cash Box terlebih dahulu di menu Keuangan.",
                        color = LossRed,
                        style = MaterialTheme.typography.bodySmall
                    )
                } else {
                    ExposedDropdownMenuBox(expanded = boxMenu, onExpandedChange = { boxMenu = it }) {
                        OutlinedTextField(
                            value = selectedBox?.let { "${it.name} (${formatRupiah(it.balance)})" } ?: "Pilih Cash Box",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(if (isHutang) "Ambil dari Cash Box" else "Masuk ke Cash Box") },
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
                }
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Catatan") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (error != null) {
                    Text(error!!, color = LossRed, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val box = selectedBox
                    if (box == null) {
                        error = "Pilih Cash Box terlebih dahulu"
                        return@Button
                    }
                    onConfirm(box, notes, paidAtMillis)
                },
                colors = ButtonDefaults.buttonColors(containerColor = ProfitGreen),
                modifier = Modifier.testTag("confirm_settle_all_button")
            ) {
                Text("Lunasi Semua")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun PayDebtDialog(
    debt: DebtRecord,
    cashBoxes: List<CashBox>,
    onConfirm: (Double, CashBox, String, Long) -> Unit,
    onDismiss: () -> Unit
) {
    var amountStr by remember { mutableStateOf(debt.remainingAmount.toLong().toString()) }
    var selectedBox by remember { mutableStateOf(cashBoxes.firstOrNull()) }
    LaunchedEffect(cashBoxes) {
        if (selectedBox == null) selectedBox = cashBoxes.firstOrNull()
    }
    var boxMenu by remember { mutableStateOf(false) }
    var notes by remember { mutableStateOf("") }
    var paidAtMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var error by remember { mutableStateOf<String?>(null) }

    val parsedAmt = (amountStr.replace(",", ".").toDoubleOrNull() ?: 0.0).coerceAtLeast(0.0)
    val appliedAmt = minOf(parsedAmt, debt.remainingAmount)
    val previewRem = (debt.remainingAmount - appliedAmt).coerceAtLeast(0.0)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (debt.debtType == "HUTANG") "Bayar Cicilan / Lunasi Hutang"
                else "Terima Cicilan Uang Masuk Piutang"
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("${debt.partyName} • Total ${formatRupiah(debt.totalAmount)}")
                Text(
                    "Sudah Terbayar: ${formatRupiah(debt.paidAmount)} • Sisa: ${formatRupiah(debt.remainingAmount)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = LossRed
                )
                DateTimeSelectorSection(
                    title = if (debt.debtType == "HUTANG") "Waktu & Tanggal Bayar" else "Waktu & Tanggal Uang Masuk",
                    millis = paidAtMillis,
                    onMillisChange = { paidAtMillis = it },
                    tagPrefix = "debt_pay"
                )
                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it; error = null },
                    label = {
                        Text(
                            if (debt.debtType == "HUTANG") "Nominal Pembayaran / Cicilan (Rp)"
                            else "Uang Masuk Diterima / Cicilan (Rp)"
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(
                        "25%" to (debt.remainingAmount * 0.25).toLong(),
                        "50%" to (debt.remainingAmount * 0.5).toLong(),
                        "100rb" to 100_000L,
                        "Lunas" to debt.remainingAmount.toLong()
                    ).forEach { (lbl, nominal) ->
                        if (nominal > 0L) {
                            OutlinedButton(onClick = { amountStr = nominal.toString(); error = null }) {
                                Text(lbl)
                            }
                        }
                    }
                }
                if (parsedAmt > 0.0) {
                    Text(
                        text = if (previewRem <= 0.001) "Status setelah simpan: LUNAS"
                        else "Sisa tagihan menjadi: ${formatRupiah(previewRem)} (CICILAN)",
                        color = if (previewRem <= 0.001) ProfitGreen else PaidBlue,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
                if (cashBoxes.isEmpty()) {
                    Text(
                        "Belum ada Cash Box. Buat Cash Box terlebih dahulu di menu Keuangan.",
                        color = LossRed,
                        style = MaterialTheme.typography.bodySmall
                    )
                } else {
                    ExposedDropdownMenuBox(expanded = boxMenu, onExpandedChange = { boxMenu = it }) {
                        OutlinedTextField(
                            value = selectedBox?.let { "${it.name} (${formatRupiah(it.balance)})" } ?: "Pilih Cash Box",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(if (debt.debtType == "HUTANG") "Ambil dari Cash Box" else "Masuk ke Cash Box") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(boxMenu) },
                            modifier = Modifier
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(expanded = boxMenu, onDismissRequest = { boxMenu = false }) {
                            cashBoxes.forEach { box ->
                                DropdownMenuItem(
                                    text = { Text("${box.name} (${formatRupiah(box.balance)})") },
                                    onClick = {
                                        selectedBox = box
                                        boxMenu = false
                                    }
                                )
                            }
                        }
                    }
                }
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Catatan") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (error != null) {
                    Text(error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amountStr.replace(",", ".").toDoubleOrNull() ?: 0.0
                    val box = selectedBox
                    if (amt <= 0.0 || box == null) {
                        error = "Masukkan nominal > 0 dan pilih Cash Box"
                        return@Button
                    }
                    onConfirm(amt, box, notes, paidAtMillis)
                },
                modifier = Modifier.testTag("confirm_pay_debt_button")
            ) {
                Text("Simpan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddManualDebtDialog(
    debtType: String,
    merchants: List<Merchant>,
    suppliers: List<Supplier>,
    onSave: (String, String, Double, String, Long) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedMerchant by remember { mutableStateOf<Merchant?>(null) }
    var merchantMenu by remember { mutableStateOf(false) }
    var selectedSupplier by remember { mutableStateOf<Supplier?>(null) }
    var supplierMenu by remember { mutableStateOf(false) }
    var partyName by remember { mutableStateOf("") }
    var amountStr by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var transactionDateMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (debtType == "HUTANG") "Catat Hutang Pembelian" else "Catat Piutang Pedagang") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                DateTimeSelectorSection(
                    title = "Waktu & Tanggal Transaksi",
                    millis = transactionDateMillis,
                    onMillisChange = { transactionDateMillis = it },
                    tagPrefix = "debt_manual"
                )
                if (debtType == "HUTANG" && suppliers.isNotEmpty()) {
                    ExposedDropdownMenuBox(expanded = supplierMenu, onExpandedChange = { supplierMenu = it }) {
                        OutlinedTextField(
                            value = selectedSupplier?.name ?: "Pilih Supplier Terdaftar (Opsional)",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Supplier") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(supplierMenu) },
                            modifier = Modifier
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(expanded = supplierMenu, onDismissRequest = { supplierMenu = false }) {
                            suppliers.forEach { sup ->
                                DropdownMenuItem(
                                    text = { Text(sup.name) },
                                    onClick = {
                                        selectedSupplier = sup
                                        partyName = sup.name
                                        supplierMenu = false
                                    }
                                )
                            }
                        }
                    }
                } else if (debtType == "PIUTANG" && merchants.isNotEmpty()) {
                    ExposedDropdownMenuBox(expanded = merchantMenu, onExpandedChange = { merchantMenu = it }) {
                        OutlinedTextField(
                            value = selectedMerchant?.name ?: "Pilih Pedagang Keliling",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Pedagang Keliling") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(merchantMenu) },
                            modifier = Modifier
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(expanded = merchantMenu, onDismissRequest = { merchantMenu = false }) {
                            merchants.forEach { m ->
                                DropdownMenuItem(
                                    text = { Text(m.name) },
                                    onClick = {
                                        selectedMerchant = m
                                        partyName = m.name
                                        merchantMenu = false
                                    }
                                )
                            }
                        }
                    }
                }
                OutlinedTextField(
                    value = partyName,
                    onValueChange = { partyName = it },
                    label = { Text(if (debtType == "HUTANG") "Nama Pemasok / Supplier *" else "Nama Pedagang *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = { Text("Nominal (Rp) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Catatan") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (error != null) {
                    Text(error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val amt = amountStr.replace(",", ".").toDoubleOrNull() ?: 0.0
                if (partyName.isBlank() || amt <= 0.0) {
                    error = "Nama dan nominal > 0 wajib diisi"
                    return@Button
                }
                val resolvedPartyId = if (debtType == "HUTANG") {
                    selectedSupplier?.id ?: ""
                } else {
                    selectedMerchant?.id ?: ""
                }
                onSave(resolvedPartyId, partyName.trim(), amt, notes, transactionDateMillis)
            }) {
                Text("Simpan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        }
    )
}
