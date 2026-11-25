package com.example.ui.screens

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Factory
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.CashBox
import com.example.data.model.DebtRecord
import com.example.data.model.Employee
import com.example.data.model.JobType
import com.example.data.model.Merchant
import com.example.data.model.Payslip
import com.example.data.model.StoreSettings
import com.example.data.model.Supplier
import com.example.data.model.UiState
import com.example.ui.components.ConfirmDeleteDialog
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.LoadingCenter
import com.example.ui.components.ResponsiveScreenContainer
import com.example.ui.components.SearchBarField
import com.example.ui.components.StatusBadge
import com.example.ui.theme.InfoBlue
import com.example.ui.theme.InfoBlueBg
import com.example.ui.theme.LossRed
import com.example.ui.theme.LossRedBg
import com.example.ui.theme.ProfitGreen
import com.example.ui.theme.ProfitGreenBg
import com.example.ui.theme.WarningAmber
import com.example.util.PayslipPrinter
import com.example.util.ThermalReceiptDocument
import com.example.util.formatRupiah
import com.example.util.toUnpaidThermalReceipt

@Composable
fun ContactsAndPayrollScreen(
    initialTab: Int = 0, // 0=Pedagang, 1=Supplier, 2=Karyawan, 3=Jenis Pekerjaan, 4=Slip Gaji
    tabRequestNonce: Int = 0,
    merchantsState: UiState<List<Merchant>>,
    suppliersState: UiState<List<Supplier>>,
    employeesState: UiState<List<Employee>>,
    jobTypesState: UiState<List<JobType>>,
    payslipsState: UiState<List<Payslip>>,
    debtsState: UiState<List<DebtRecord>>,
    cashBoxesState: UiState<List<CashBox>>,
    storeSettingsState: UiState<StoreSettings>,
    onSaveMerchant: (Merchant) -> Unit,
    onDeleteMerchant: (String) -> Unit,
    onSaveSupplier: (Supplier) -> Unit,
    onDeleteSupplier: (String) -> Unit,
    onSaveEmployee: (Employee) -> Unit,
    onDeleteEmployee: (String) -> Unit,
    onSaveJobType: (JobType) -> Unit,
    onCreatePayslip: (Payslip, CashBox?) -> Unit,
    onTogglePayslipPaid: (Payslip, Boolean, CashBox?, Long) -> Unit,
    onPayInstallmentSingle: (Payslip, Double, CashBox?, String, Long) -> Unit = { _, _, _, _, _ -> },
    onPayInstallmentPool: (List<Payslip>, Double, CashBox?, String, Long) -> Unit = { _, _, _, _, _ -> },
    onPayAllPayslips: (List<Payslip>, CashBox?, Long) -> Unit = { _, _, _ -> },
    onDeletePayslip: (String) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(initialTab) }
    LaunchedEffect(initialTab, tabRequestNonce) {
        selectedTab = initialTab
    }
    var editingMerchant by remember { mutableStateOf<Merchant?>(null) }
    var showMerchantDialog by remember { mutableStateOf(false) }
    var editingSupplier by remember { mutableStateOf<Supplier?>(null) }
    var showSupplierDialog by remember { mutableStateOf(false) }
    var editingEmployee by remember { mutableStateOf<Employee?>(null) }
    var showEmployeeDialog by remember { mutableStateOf(false) }
    var editingJobType by remember { mutableStateOf<JobType?>(null) }
    var showJobTypeDialog by remember { mutableStateOf(false) }
    var showPayslipCreateDialog by remember { mutableStateOf(false) }
    var payPayslipTarget by remember { mutableStateOf<Payslip?>(null) }
    var previewPayslip by remember { mutableStateOf<Payslip?>(null) }
    var bluetoothPrintTarget by remember { mutableStateOf<Payslip?>(null) }
    var unpaidBluetoothTarget by remember { mutableStateOf<Pair<List<Payslip>, String>?>(null) }
    var paidFilteredBluetoothDoc by remember { mutableStateOf<ThermalReceiptDocument?>(null) }
    var deleteConfirm by remember { mutableStateOf<Pair<String, () -> Unit>?>(null) }

    val jobTypes = (jobTypesState as? UiState.Success)?.data ?: emptyList()
    val employees = (employeesState as? UiState.Success)?.data ?: emptyList()
    val cashBoxes = (cashBoxesState as? UiState.Success)?.data ?: emptyList()
    val debts = (debtsState as? UiState.Success)?.data ?: emptyList()
    val storeSettings = (storeSettingsState as? UiState.Success)?.data ?: StoreSettings()
    val context = LocalContext.current

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    when (selectedTab) {
                        0 -> { editingMerchant = null; showMerchantDialog = true }
                        1 -> { editingSupplier = null; showSupplierDialog = true }
                        2 -> { editingEmployee = null; showEmployeeDialog = true }
                        3 -> { editingJobType = null; showJobTypeDialog = true }
                        4 -> { showPayslipCreateDialog = true }
                    }
                },
                modifier = Modifier.testTag("contacts_add_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Tambah Data")
            }
        }
    ) { padding ->
        ResponsiveScreenContainer(modifier = Modifier.padding(padding)) {
            Column(modifier = Modifier.fillMaxSize()) {
                Text(
                    text = "Kontak & Penggajian Pabrik",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                )
                ScrollableTabRow(selectedTabIndex = selectedTab, edgePadding = 12.dp) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Pedagang Keliling") },
                        modifier = Modifier.testTag("tab_merchants")
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Supplier") },
                        modifier = Modifier.testTag("tab_suppliers")
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = { Text("Karyawan Pabrik") },
                        modifier = Modifier.testTag("tab_employees")
                    )
                    Tab(
                        selected = selectedTab == 3,
                        onClick = { selectedTab = 3 },
                        text = { Text("Jenis Pekerjaan (Tarif)") },
                        modifier = Modifier.testTag("tab_job_types")
                    )
                    Tab(
                        selected = selectedTab == 4,
                        onClick = { selectedTab = 4 },
                        text = { Text("Slip Gaji") },
                        modifier = Modifier.testTag("tab_payslips")
                    )
                }

                when (selectedTab) {
                    0 -> MerchantsTabContent(
                        merchantsState = merchantsState,
                        debts = debts,
                        onEdit = { editingMerchant = it; showMerchantDialog = true },
                        onDelete = { m -> deleteConfirm = "Hapus pedagang \"${m.name}\"?" to { onDeleteMerchant(m.id) } }
                    )
                    1 -> SuppliersTabContent(
                        suppliersState = suppliersState,
                        debts = debts,
                        onEdit = { editingSupplier = it; showSupplierDialog = true },
                        onDelete = { s -> deleteConfirm = "Hapus supplier \"${s.name}\"?" to { onDeleteSupplier(s.id) } }
                    )
                    2 -> EmployeesTabContent(
                        employeesState = employeesState,
                        onEdit = { editingEmployee = it; showEmployeeDialog = true },
                        onDelete = { e -> deleteConfirm = "Hapus karyawan \"${e.name}\"?" to { onDeleteEmployee(e.id) } }
                    )
                    3 -> JobTypesTabContent(
                        jobTypesState = jobTypesState,
                        onEdit = { editingJobType = it; showJobTypeDialog = true }
                    )
                    4 -> PayslipsTabContent(
                        payslipsState = payslipsState,
                        employees = employees,
                        cashBoxes = cashBoxes,
                        onTogglePaid = { slip ->
                            if (slip.paid || slip.remainingAmount() <= 0.0) {
                                onTogglePayslipPaid(slip, false, null, 0L)
                            } else {
                                payPayslipTarget = slip
                            }
                        },
                        onPayInstallmentSingle = onPayInstallmentSingle,
                        onPayInstallmentPool = onPayInstallmentPool,
                        onPayAll = onPayAllPayslips,
                        onPreviewAndPrint = { previewPayslip = it },
                        onBluetoothPrint = { bluetoothPrintTarget = it },
                        onPrintAllUnpaidBluetooth = { list, label -> unpaidBluetoothTarget = list to label },
                        onPrintThermalDocument = { paidFilteredBluetoothDoc = it },
                        onDelete = { s ->
                            deleteConfirm = "Hapus slip gaji ${s.employeeName}? Riwayat pembayaran gaji di menu Keuangan juga akan ikut terhapus dan saldo Cash Box disesuaikan." to {
                                onDeletePayslip(s.id)
                            }
                        }
                    )
                }
            }
        }
    }

    if (showMerchantDialog) {
        MerchantFormDialog(
            initial = editingMerchant,
            onSave = { onSaveMerchant(it); showMerchantDialog = false },
            onDismiss = { showMerchantDialog = false }
        )
    }

    if (showSupplierDialog) {
        SupplierFormDialog(
            initial = editingSupplier,
            onSave = { onSaveSupplier(it); showSupplierDialog = false },
            onDismiss = { showSupplierDialog = false }
        )
    }

    if (showEmployeeDialog) {
        EmployeeFormDialog(
            initial = editingEmployee,
            jobTypes = jobTypes,
            onSave = { onSaveEmployee(it); showEmployeeDialog = false },
            onDismiss = { showEmployeeDialog = false }
        )
    }

    if (showJobTypeDialog) {
        JobTypeFormDialog(
            initial = editingJobType,
            nextSortOrder = (jobTypes.maxOfOrNull { it.sortOrder } ?: 0L) + 1L,
            onSave = { onSaveJobType(it); showJobTypeDialog = false },
            onDismiss = { showJobTypeDialog = false }
        )
    }

    if (showPayslipCreateDialog) {
        CreatePayslipDialog(
            employees = employees,
            jobTypes = jobTypes,
            cashBoxes = cashBoxes,
            onSave = { slip, box -> onCreatePayslip(slip, box); showPayslipCreateDialog = false },
            onDismiss = { showPayslipCreateDialog = false }
        )
    }

    payPayslipTarget?.let { slip ->
        MarkPayslipPaidDialog(
            payslip = slip,
            cashBoxes = cashBoxes,
            onConfirm = { box, paidAtMillis ->
                onTogglePayslipPaid(slip, true, box, paidAtMillis)
                payPayslipTarget = null
            },
            onDismiss = { payPayslipTarget = null }
        )
    }

    previewPayslip?.let { slip ->
        PayslipPreviewDialog(
            payslip = slip,
            storeSettings = storeSettings,
            onPrintPdf = { PayslipPrinter.printPayslip(context, slip, storeSettings) },
            onPrintBluetooth = { bluetoothPrintTarget = slip },
            onDismiss = { previewPayslip = null }
        )
    }

    bluetoothPrintTarget?.let { slip ->
        BluetoothPrintDialog(
            payslip = slip,
            storeSettings = storeSettings,
            onDismiss = { bluetoothPrintTarget = null }
        )
    }

    unpaidBluetoothTarget?.let { (unpaidList, empLabel) ->
        BluetoothPrintDialog(
            document = unpaidList.toUnpaidThermalReceipt(empLabel),
            storeSettings = storeSettings,
            onDismiss = { unpaidBluetoothTarget = null }
        )
    }

    paidFilteredBluetoothDoc?.let { doc ->
        BluetoothPrintDialog(
            document = doc,
            storeSettings = storeSettings,
            onDismiss = { paidFilteredBluetoothDoc = null }
        )
    }

    deleteConfirm?.let { (msg, action) ->
        ConfirmDeleteDialog(
            title = "Konfirmasi Hapus",
            message = msg,
            onConfirm = { action(); deleteConfirm = null },
            onDismiss = { deleteConfirm = null }
        )
    }
}

@Composable
private fun MerchantsTabContent(
    merchantsState: UiState<List<Merchant>>,
    debts: List<DebtRecord>,
    onEdit: (Merchant) -> Unit,
    onDelete: (Merchant) -> Unit
) {
    when (merchantsState) {
        is UiState.Loading -> LoadingCenter()
        is UiState.Error -> EmptyStateCard("Gagal memuat pedagang", merchantsState.message)
        is UiState.Success -> {
            if (merchantsState.data.isEmpty()) {
                BoxPaddingEmpty(
                    title = "Belum ada Pedagang Keliling",
                    subtitle = "Tekan + untuk mendaftarkan pedagang keliling dan memantau piutangnya.",
                    icon = Icons.Default.LocalShipping
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp, 12.dp, 16.dp, 88.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(merchantsState.data, key = { it.id }) { m ->
                        val activePiutang = debts.filter {
                            it.debtType == "PIUTANG" && it.remainingAmount > 0 &&
                                (it.partyId == m.id || it.partyName.equals(m.name, ignoreCase = true))
                        }.sumOf { it.remainingAmount }

                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                    Text(m.name, style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        listOfNotNull(
                                            m.routeArea.takeIf { it.isNotBlank() }?.let { "Rute: $it" },
                                            m.phone.takeIf { it.isNotBlank() }
                                        ).joinToString(" • ").ifBlank { "Tidak ada info rute/HP" },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (activePiutang > 0.0) {
                                        Text(
                                            "Piutang Aktif: ${formatRupiah(activePiutang)}",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = WarningAmber
                                        )
                                    }
                                }
                                IconButton(onClick = { onEdit(m) }) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit pedagang")
                                }
                                IconButton(onClick = { onDelete(m) }) {
                                    Icon(Icons.Default.DeleteOutline, contentDescription = "Hapus pedagang", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SuppliersTabContent(
    suppliersState: UiState<List<Supplier>>,
    debts: List<DebtRecord>,
    onEdit: (Supplier) -> Unit,
    onDelete: (Supplier) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }

    when (suppliersState) {
        is UiState.Loading -> LoadingCenter()
        is UiState.Error -> EmptyStateCard("Gagal memuat data supplier", suppliersState.message)
        is UiState.Success -> {
            val allSuppliers = suppliersState.data
            if (allSuppliers.isEmpty()) {
                BoxPaddingEmpty(
                    title = "Belum ada Data Supplier",
                    subtitle = "Tekan tombol + untuk menambahkan pemasok bahan baku atau barang dagangan beserta kontak & alamatnya.",
                    icon = Icons.Default.Storefront
                )
            } else {
                val filteredSuppliers = remember(allSuppliers, searchQuery) {
                    if (searchQuery.isBlank()) allSuppliers
                    else {
                        val q = searchQuery.trim()
                        allSuppliers.filter {
                            it.name.contains(q, ignoreCase = true) ||
                                it.suppliedItems.contains(q, ignoreCase = true) ||
                                it.phone.contains(q, ignoreCase = true)
                        }
                    }
                }
                val totalHutangSuppliers = remember(allSuppliers, debts) {
                    debts.filter { d ->
                        d.debtType == "HUTANG" && d.remainingAmount > 0 &&
                            allSuppliers.any { s -> d.partyId == s.id || d.partyName.equals(s.name, ignoreCase = true) }
                    }.sumOf { it.remainingAmount }
                }

                Column(modifier = Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        SearchBarField(
                            query = searchQuery,
                            onQueryChange = { searchQuery = it },
                            placeholder = "Cari nama supplier / barang pasokan...",
                            tag = "supplier_search_input"
                        )
                    }

                    LazyColumn(
                        contentPadding = PaddingValues(16.dp, 4.dp, 16.dp, 88.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = InfoBlueBg),
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
                                    Column {
                                        Text(
                                            text = "Total Supplier Terdaftar",
                                            style = MaterialTheme.typography.titleSmall,
                                            color = InfoBlue
                                        )
                                        Text(
                                            text = "${allSuppliers.size} pemasok • Hutang aktif: ${formatRupiah(totalHutangSuppliers)}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = InfoBlue
                                        )
                                    }
                                    StatusBadge(
                                        text = "${filteredSuppliers.size} tampil",
                                        containerColor = MaterialTheme.colorScheme.surface,
                                        contentColor = InfoBlue
                                    )
                                }
                            }
                        }

                        if (filteredSuppliers.isEmpty()) {
                            item {
                                EmptyStateCard(
                                    title = "Supplier tidak ditemukan",
                                    subtitle = "Coba kata kunci pencarian lain atau tekan + untuk menambah supplier baru.",
                                    icon = Icons.Default.Storefront
                                )
                            }
                        } else {
                            items(filteredSuppliers, key = { it.id }) { s ->
                                val activeHutang = debts.filter {
                                    it.debtType == "HUTANG" && it.remainingAmount > 0 &&
                                        (it.partyId == s.id || it.partyName.equals(s.name, ignoreCase = true))
                                }.sumOf { it.remainingAmount }

                                Card(
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    shape = MaterialTheme.shapes.medium,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("supplier_card_${s.id}")
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(
                                            modifier = Modifier.weight(1f),
                                            verticalArrangement = Arrangement.spacedBy(3.dp)
                                        ) {
                                            Text(s.name, style = MaterialTheme.typography.titleMedium)
                                            if (s.suppliedItems.isNotBlank()) {
                                                Text(
                                                    text = "Pasokan: ${s.suppliedItems}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                            Text(
                                                text = listOfNotNull(
                                                    s.phone.takeIf { it.isNotBlank() },
                                                    s.address.takeIf { it.isNotBlank() }
                                                ).joinToString(" • ").ifBlank { "Tidak ada info kontak/alamat" },
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            if (s.notes.isNotBlank()) {
                                                Text(
                                                    text = s.notes,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            if (activeHutang > 0.0) {
                                                Text(
                                                    text = "Hutang Aktif: ${formatRupiah(activeHutang)}",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = LossRed
                                                )
                                            }
                                        }
                                        IconButton(
                                            onClick = { onEdit(s) },
                                            modifier = Modifier.testTag("edit_supplier_${s.id}")
                                        ) {
                                            Icon(Icons.Default.Edit, contentDescription = "Edit supplier")
                                        }
                                        IconButton(
                                            onClick = { onDelete(s) },
                                            modifier = Modifier.testTag("delete_supplier_${s.id}")
                                        ) {
                                            Icon(
                                                Icons.Default.DeleteOutline,
                                                contentDescription = "Hapus supplier",
                                                tint = MaterialTheme.colorScheme.error
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
    }
}

@Composable
private fun EmployeesTabContent(
    employeesState: UiState<List<Employee>>,
    onEdit: (Employee) -> Unit,
    onDelete: (Employee) -> Unit
) {
    when (employeesState) {
        is UiState.Loading -> LoadingCenter()
        is UiState.Error -> EmptyStateCard("Gagal memuat karyawan", employeesState.message)
        is UiState.Success -> {
            if (employeesState.data.isEmpty()) {
                BoxPaddingEmpty(
                    title = "Belum ada Karyawan Pabrik",
                    subtitle = "Tekan + untuk menambah data karyawan pabrik beserta bagian kerjanya.",
                    icon = Icons.Default.Factory
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp, 12.dp, 16.dp, 88.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(employeesState.data, key = { it.id }) { emp ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text(emp.name, style = MaterialTheme.typography.titleMedium)
                                        StatusBadge(
                                            text = if (emp.active) "Aktif" else "Nonaktif",
                                            containerColor = if (emp.active) ProfitGreenBg else LossRedBg,
                                            contentColor = if (emp.active) ProfitGreen else LossRed
                                        )
                                    }
                                    Text(
                                        listOfNotNull(
                                            emp.defaultJobName.takeIf { it.isNotBlank() }?.let { "Bagian: $it" },
                                            emp.phone.takeIf { it.isNotBlank() }
                                        ).joinToString(" • ").ifBlank { "Karyawan pabrik" },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(onClick = { onEdit(emp) }) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit karyawan")
                                }
                                IconButton(onClick = { onDelete(emp) }) {
                                    Icon(Icons.Default.DeleteOutline, contentDescription = "Hapus karyawan", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun JobTypesTabContent(
    jobTypesState: UiState<List<JobType>>,
    onEdit: (JobType) -> Unit
) {
    when (jobTypesState) {
        is UiState.Loading -> LoadingCenter()
        is UiState.Error -> EmptyStateCard("Gagal memuat jenis pekerjaan", jobTypesState.message)
        is UiState.Success -> {
            val jobs = jobTypesState.data
            if (jobs.isEmpty()) {
                BoxPaddingEmpty(
                    title = "Belum ada jenis pekerjaan",
                    subtitle = "Penakar, Pengkilo, Sealer, Packing, Pencuci, Pembolong akan disiapkan.",
                    icon = Icons.Default.Work
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp, 12.dp, 16.dp, 88.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        Text(
                            text = "Ketuk \"Ubah Tarif\" untuk mengubah tarif upah (Rp) per satuan pekerjaan.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    items(jobs, key = { it.id }) { job ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(job.name, style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        "Tarif: ${formatRupiah(job.ratePerUnit)} / ${job.unit}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = ProfitGreen
                                    )
                                }
                                OutlinedButton(
                                    onClick = { onEdit(job) },
                                    modifier = Modifier.testTag("edit_job_rate_${job.code}")
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = null)
                                    Spacer(Modifier.width(6.dp))
                                    Text("Ubah Tarif")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MerchantFormDialog(
    initial: Merchant?,
    onSave: (Merchant) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var phone by remember { mutableStateOf(initial?.phone ?: "") }
    var routeArea by remember { mutableStateOf(initial?.routeArea ?: "") }
    var address by remember { mutableStateOf(initial?.address ?: "") }
    var notes by remember { mutableStateOf(initial?.notes ?: "") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Tambah Pedagang Keliling" else "Edit Pedagang Keliling") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nama Pedagang *") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("No. Telepon / WA") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = routeArea, onValueChange = { routeArea = it }, label = { Text("Wilayah / Rute Keliling") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text("Alamat") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Catatan") }, modifier = Modifier.fillMaxWidth())
                if (error != null) Text(error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            Button(onClick = {
                if (name.isBlank()) { error = "Nama pedagang wajib diisi"; return@Button }
                onSave((initial ?: Merchant()).copy(name = name, phone = phone, routeArea = routeArea, address = address, notes = notes))
            }) { Text("Simpan") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } }
    )
}

@Composable
private fun SupplierFormDialog(
    initial: Supplier?,
    onSave: (Supplier) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var phone by remember { mutableStateOf(initial?.phone ?: "") }
    var suppliedItems by remember { mutableStateOf(initial?.suppliedItems ?: "") }
    var address by remember { mutableStateOf(initial?.address ?: "") }
    var notes by remember { mutableStateOf(initial?.notes ?: "") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Tambah Data Supplier" else "Edit Data Supplier") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; error = null },
                    label = { Text("Nama Supplier / Pemasok *") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("supplier_name_input")
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("No. Telepon / WA") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("supplier_phone_input")
                )
                OutlinedTextField(
                    value = suppliedItems,
                    onValueChange = { suppliedItems = it },
                    label = { Text("Barang / Bahan yang Dipasok") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("supplier_items_input")
                )
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Alamat Supplier") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("supplier_address_input")
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Catatan") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("supplier_notes_input")
                )
                if (error != null) {
                    Text(error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank()) {
                        error = "Nama supplier wajib diisi"
                        return@Button
                    }
                    onSave(
                        (initial ?: Supplier()).copy(
                            name = name,
                            phone = phone,
                            suppliedItems = suppliedItems,
                            address = address,
                            notes = notes
                        )
                    )
                },
                modifier = Modifier.testTag("save_supplier_button")
            ) {
                Text("Simpan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EmployeeFormDialog(
    initial: Employee?,
    jobTypes: List<JobType>,
    onSave: (Employee) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var phone by remember { mutableStateOf(initial?.phone ?: "") }
    val selectedJobIds = remember {
        mutableStateListOf<String>().apply {
            val initialIds = initial?.assignedJobIds().orEmpty()
            if (initialIds.isNotEmpty()) {
                addAll(initialIds)
            } else {
                jobTypes.firstOrNull()?.let { add(it.id) }
            }
        }
    }
    var active by remember { mutableStateOf(initial?.active ?: true) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Tambah Karyawan Pabrik" else "Edit Karyawan Pabrik") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nama Karyawan *") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("No. Telepon") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), singleLine = true, modifier = Modifier.fillMaxWidth())
                if (jobTypes.isNotEmpty()) {
                    Text(
                        text = "Pekerjaan yang Dipegang (Bisa Lebih dari 1)",
                        style = MaterialTheme.typography.labelMedium
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        jobTypes.forEach { j ->
                            val selected = j.id in selectedJobIds
                            FilterChip(
                                selected = selected,
                                onClick = {
                                    if (selected) selectedJobIds.remove(j.id) else selectedJobIds.add(j.id)
                                },
                                label = { Text(j.name) }
                            )
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Status Karyawan Aktif")
                    Switch(checked = active, onCheckedChange = { active = it })
                }
                if (error != null) Text(error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            Button(onClick = {
                if (name.isBlank()) { error = "Nama karyawan wajib diisi"; return@Button }
                val chosenJobs = jobTypes.filter { it.id in selectedJobIds }
                onSave(
                    (initial ?: Employee()).copy(
                        name = name,
                        phone = phone,
                        defaultJobTypeId = chosenJobs.joinToString(",") { it.id },
                        defaultJobName = chosenJobs.joinToString(", ") { it.name },
                        active = active
                    )
                )
            }) { Text("Simpan") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } }
    )
}

@Composable
private fun JobTypeFormDialog(
    initial: JobType?,
    nextSortOrder: Long,
    onSave: (JobType) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var unit by remember { mutableStateOf(initial?.unit ?: "Satuan") }
    var rateStr by remember { mutableStateOf(initial?.ratePerUnit?.toLong()?.toString() ?: "") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Tambah Jenis Pekerjaan" else "Ubah Tarif ${initial.name}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nama Pekerjaan *") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = unit, onValueChange = { unit = it }, label = { Text("Satuan (Takar/Kg/Pak/Bal) *") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(
                    value = rateStr,
                    onValueChange = { rateStr = it },
                    label = { Text("Tarif Upah (Rp) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (error != null) Text(error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            Button(onClick = {
                val rate = rateStr.replace(",", ".").toDoubleOrNull() ?: -1.0
                if (name.isBlank() || unit.isBlank() || rate < 0.0) {
                    error = "Lengkapi nama, satuan, dan tarif >= 0"
                    return@Button
                }
                onSave(
                    (initial ?: JobType(sortOrder = nextSortOrder)).copy(
                        name = name.trim(),
                        unit = unit.trim(),
                        ratePerUnit = rate
                    )
                )
            }) { Text("Simpan Tarif") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } }
    )
}
