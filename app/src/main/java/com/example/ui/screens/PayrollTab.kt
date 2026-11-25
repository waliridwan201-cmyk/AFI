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
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CashBox
import com.example.data.model.Employee
import com.example.data.model.Payslip
import com.example.data.model.StoreSettings
import com.example.data.model.UiState
import com.example.ui.components.CurrencyText
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.LoadingCenter
import com.example.ui.theme.LedgerCellBorder
import com.example.ui.theme.LedgerDateText
import com.example.ui.theme.LossRed
import com.example.ui.theme.PaidBlue
import com.example.ui.theme.ProfitGreen
import com.example.ui.theme.SlateBarBg
import com.example.ui.theme.SlateBarText
import com.example.ui.theme.SlateDivider
import com.example.ui.theme.SlateToolbarBg
import com.example.ui.theme.WarningAmber
import com.example.util.ThermalReceiptDocument
import com.example.util.formatDate
import com.example.util.formatDateTime
import com.example.util.formatDateTimeSlash
import com.example.util.formatLedgerNumber
import com.example.util.formatQty
import com.example.util.formatRupiah
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

private enum class PayslipStatusFilter {
    ALL, PAID, UNPAID
}

@Composable
internal fun PayslipsTabContent(
    payslipsState: UiState<List<Payslip>>,
    employees: List<Employee>,
    cashBoxes: List<CashBox> = emptyList(),
    onTogglePaid: (Payslip) -> Unit,
    onPayInstallmentSingle: (Payslip, Double, CashBox?, String, Long) -> Unit = { _, _, _, _, _ -> },
    onPayInstallmentPool: (List<Payslip>, Double, CashBox?, String, Long) -> Unit = { _, _, _, _, _ -> },
    onPayAll: (List<Payslip>, CashBox?, Long) -> Unit = { _, _, _ -> },
    onPreviewAndPrint: (Payslip) -> Unit,
    onBluetoothPrint: (Payslip) -> Unit,
    onPrintAllUnpaidBluetooth: (List<Payslip>, String) -> Unit = { _, _ -> },
    onPrintThermalDocument: (ThermalReceiptDocument) -> Unit = {},
    onDelete: (Payslip) -> Unit
) {
    var nameSearchQuery by remember { mutableStateOf("") }
    var selectedEmployeeFilter by remember { mutableStateOf<String?>(null) }
    var statusFilter by remember { mutableStateOf(PayslipStatusFilter.ALL) }

    var showEmployeeSelectorDialog by remember { mutableStateOf(false) }
    var showFloatingMenu by remember { mutableStateOf(false) }
    var showPayAllDialog by remember { mutableStateOf(false) }
    var showPoolInstallmentDialog by remember { mutableStateOf(false) }
    var showPrintPaidFilteredDialog by remember { mutableStateOf(false) }
    var actionTarget by remember { mutableStateOf<Payslip?>(null) }
    var singleInstallmentTarget by remember { mutableStateOf<Payslip?>(null) }

    when (payslipsState) {
        is UiState.Loading -> LoadingCenter()
        is UiState.Error -> EmptyStateCard("Gagal memuat slip gaji", payslipsState.message)
        is UiState.Success -> {
            val allSlips = payslipsState.data
            val employeeNames = remember(allSlips, employees) {
                (allSlips.map { it.employeeName } + employees.map { it.name })
                    .map { it.trim() }
                    .filter { it.isNotBlank() }
                    .distinct()
                    .sortedBy { it.lowercase() }
            }

            val filteredSlips = remember(allSlips, nameSearchQuery, selectedEmployeeFilter, statusFilter) {
                val q = nameSearchQuery.trim()
                allSlips.filter { slip ->
                    val matchesQuery = q.isBlank() ||
                        slip.employeeName.contains(q, ignoreCase = true) ||
                        slip.periodLabel.contains(q, ignoreCase = true) ||
                        slip.items.any { it.jobName.contains(q, ignoreCase = true) }
                    val matchesChip = selectedEmployeeFilter == null ||
                        slip.employeeName.equals(selectedEmployeeFilter, ignoreCase = true)
                    val isPaid = slip.paid || slip.remainingAmount() <= 0.0
                    val matchesStatus = when (statusFilter) {
                        PayslipStatusFilter.ALL -> true
                        PayslipStatusFilter.PAID -> isPaid
                        PayslipStatusFilter.UNPAID -> !isPaid
                    }
                    matchesQuery && matchesChip && matchesStatus
                }.sortedByDescending { slip ->
                    slip.workDateMillis.takeIf { it > 0L }
                        ?: slip.salaryDateMillis.takeIf { it > 0L }
                        ?: slip.createdAtMillis
                }
            }

            val unpaidByNameFilter = remember(allSlips, nameSearchQuery, selectedEmployeeFilter) {
                val q = nameSearchQuery.trim()
                allSlips.filter { slip ->
                    val matchesQuery = q.isBlank() ||
                        slip.employeeName.contains(q, ignoreCase = true) ||
                        slip.periodLabel.contains(q, ignoreCase = true) ||
                        slip.items.any { it.jobName.contains(q, ignoreCase = true) }
                    val matchesChip = selectedEmployeeFilter == null ||
                        slip.employeeName.equals(selectedEmployeeFilter, ignoreCase = true)
                    !slip.paid && slip.remainingAmount() > 0.0 && matchesQuery && matchesChip
                }
            }

            val totalRemainingUnpaid = filteredSlips.sumOf { it.remainingAmount() }
            val activeEmployeeLabel = selectedEmployeeFilter
                ?: nameSearchQuery.trim().takeIf { it.isNotBlank() }
                ?: "Semua Karyawan"

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White)
            ) {
                // Centered Employee Name Selector Strip (matches Image 1)
                Surface(
                    color = Color.White,
                    shadowElevation = 1.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showEmployeeSelectorDialog = true }
                        .testTag("payroll_employee_selector")
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
                                text = activeEmployeeLabel,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF111827),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = "Pilih Nama Karyawan",
                                tint = Color(0xFF4B5563),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        HorizontalDivider(thickness = 1.dp, color = LedgerCellBorder)
                    }
                }

                // Search Bar Strip (matches "cari id transaksi" strip in Image 1)
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
                                    text = "cari nama / periode gaji",
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
                                    .testTag("payslip_search_name")
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

                // Dark Slate Table Header: TANGGAL | NILAI | HAPUS
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

                // Ledger List + Bottom-Left Floating Action Button
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(Color.White)
                ) {
                    if (allSlips.isEmpty()) {
                        BoxPaddingEmpty(
                            title = "Belum ada Transaksi Gaji",
                            subtitle = "Tekan + di kanan atas untuk mencatat upah borongan harian karyawan.",
                            icon = Icons.Default.Badge
                        )
                    } else if (filteredSlips.isEmpty()) {
                        BoxPaddingEmpty(
                            title = "Tidak ada data gaji yang cocok",
                            subtitle = "Ubah filter karyawan atau status di menu kiri bawah.",
                            icon = Icons.Default.Badge
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("payroll_ledger_list"),
                            contentPadding = PaddingValues(bottom = 84.dp)
                        ) {
                            itemsIndexed(filteredSlips, key = { _, s -> s.id }) { _, slip ->
                                val workMillis = slip.workDateMillis.takeIf { it > 0L }
                                    ?: slip.salaryDateMillis.takeIf { it > 0L }
                                    ?: slip.createdAtMillis
                                val paidSoFar = slip.effectivePaidAmount()
                                val remaining = slip.remainingAmount()
                                val isPaid = slip.paid || remaining <= 0.0

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
                                                .clickable { actionTarget = slip }
                                                .testTag("payslip_row_${slip.id}")
                                        ) {
                                            // Subrow 1: Work DateTime | Total Salary | Remaining Salary
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(IntrinsicSize.Min),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = formatDateTimeSlash(workMillis),
                                                    fontSize = 12.sp,
                                                    color = LedgerDateText,
                                                    textAlign = TextAlign.Center,
                                                    modifier = Modifier
                                                        .weight(0.43f)
                                                        .padding(horizontal = 6.dp, vertical = 6.dp)
                                                )
                                                VerticalDivider(
                                                    thickness = 1.dp,
                                                    color = LedgerCellBorder,
                                                    modifier = Modifier.fillMaxHeight()
                                                )
                                                Row(
                                                    modifier = Modifier
                                                        .weight(0.57f)
                                                        .height(IntrinsicSize.Min),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = formatLedgerNumber(slip.totalAmount),
                                                        fontSize = 12.sp,
                                                        color = Color(0xFF1F2937),
                                                        textAlign = TextAlign.End,
                                                        modifier = Modifier
                                                            .weight(1f)
                                                            .padding(horizontal = 6.dp, vertical = 6.dp)
                                                    )
                                                    VerticalDivider(
                                                        thickness = 1.dp,
                                                        color = LedgerCellBorder,
                                                        modifier = Modifier.fillMaxHeight()
                                                    )
                                                    Text(
                                                        text = if (isPaid) "0" else formatLedgerNumber(remaining),
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
                                            HorizontalDivider(
                                                thickness = 0.7.dp,
                                                color = LedgerCellBorder.copy(alpha = 0.7f)
                                            )

                                            // Subrow 2: Paid DateTime (+ Employee Name if All) | Red "Total - Paid" OR Blue "DIBAYAR"
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
                                                        text = if (slip.paidAtMillis > 0L) formatDateTimeSlash(slip.paidAtMillis) else "-",
                                                        fontSize = 12.sp,
                                                        color = LedgerDateText,
                                                        textAlign = TextAlign.Center
                                                    )
                                                    if (selectedEmployeeFilter == null) {
                                                        Text(
                                                            text = slip.employeeName,
                                                            fontSize = 10.sp,
                                                            color = Color(0xFF6B7280),
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                    }
                                                }
                                                VerticalDivider(
                                                    thickness = 1.dp,
                                                    color = LedgerCellBorder,
                                                    modifier = Modifier.fillMaxHeight()
                                                )
                                                Box(
                                                    modifier = Modifier
                                                        .weight(0.57f)
                                                        .padding(horizontal = 6.dp, vertical = 6.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    if (isPaid) {
                                                        Text(
                                                            text = "DIBAYAR",
                                                            fontSize = 12.sp,
                                                            fontWeight = FontWeight.SemiBold,
                                                            color = PaidBlue
                                                        )
                                                    } else {
                                                        Text(
                                                            text = "${formatLedgerNumber(slip.totalAmount)} - ${formatLedgerNumber(paidSoFar)}",
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
                                                onClick = { onDelete(slip) },
                                                modifier = Modifier
                                                    .size(48.dp)
                                                    .testTag("delete_payslip_${slip.id}")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.DeleteOutline,
                                                    contentDescription = "Hapus slip gaji ${slip.employeeName}",
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

                    // Bottom-Left Circular Action Menu Button (≡) matching Image 1
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
                                .testTag("payslip_floating_menu_button")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Menu,
                                    contentDescription = "Menu Aksi Gaji Karyawan",
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
                                        "Bayar Cicilan Gaji ($activeEmployeeLabel)",
                                        fontWeight = FontWeight.Medium
                                    )
                                },
                                enabled = unpaidByNameFilter.isNotEmpty(),
                                onClick = {
                                    showFloatingMenu = false
                                    showPoolInstallmentDialog = true
                                },
                                modifier = Modifier.testTag("pay_installment_payslip_menu")
                            )
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        "Bayar Semua Gaji ($activeEmployeeLabel)",
                                        color = if (unpaidByNameFilter.isNotEmpty()) ProfitGreen else Color(0xFF9CA3AF),
                                        fontWeight = FontWeight.SemiBold
                                    )
                                },
                                enabled = unpaidByNameFilter.isNotEmpty(),
                                onClick = {
                                    showFloatingMenu = false
                                    showPayAllDialog = true
                                },
                                modifier = Modifier.testTag("pay_all_payslips_button")
                            )
                            DropdownMenuItem(
                                text = {
                                    val nextLabel = when (statusFilter) {
                                        PayslipStatusFilter.ALL -> "Semua -> Belum Dibayar"
                                        PayslipStatusFilter.UNPAID -> "Belum Dibayar -> Dibayar"
                                        PayslipStatusFilter.PAID -> "Dibayar -> Semua"
                                    }
                                    Text("Filter Status ($nextLabel)")
                                },
                                onClick = {
                                    statusFilter = when (statusFilter) {
                                        PayslipStatusFilter.ALL -> PayslipStatusFilter.UNPAID
                                        PayslipStatusFilter.UNPAID -> PayslipStatusFilter.PAID
                                        PayslipStatusFilter.PAID -> PayslipStatusFilter.ALL
                                    }
                                    showFloatingMenu = false
                                },
                                modifier = Modifier.testTag("filter_payslip_status_menu")
                            )
                            DropdownMenuItem(
                                text = { Text("Cetak Belum Dibayar (Thermal)") },
                                enabled = unpaidByNameFilter.isNotEmpty(),
                                onClick = {
                                    showFloatingMenu = false
                                    onPrintAllUnpaidBluetooth(unpaidByNameFilter, activeEmployeeLabel)
                                },
                                modifier = Modifier.testTag("print_unpaid_payslips_bluetooth_button")
                            )
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        "Cetak Dibayar Penuh (Filter Tgl & Nama)",
                                        color = if (allSlips.any { it.paid || it.remainingAmount() <= 0.0 }) ProfitGreen else Color(0xFF9CA3AF),
                                        fontWeight = FontWeight.SemiBold
                                    )
                                },
                                enabled = allSlips.any { it.paid || it.remainingAmount() <= 0.0 },
                                onClick = {
                                    showFloatingMenu = false
                                    showPrintPaidFilteredDialog = true
                                },
                                modifier = Modifier.testTag("print_paid_payslips_filtered_button")
                            )
                        }
                    }
                }

                // Sticky Dark Slate Bottom Footer: TOTAL | <totalRemainingUnpaid>
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
                                text = formatLedgerNumber(totalRemainingUnpaid),
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

            if (showEmployeeSelectorDialog) {
                AlertDialog(
                    onDismissRequest = { showEmployeeSelectorDialog = false },
                    title = { Text("Filter Nama Karyawan") },
                    text = {
                        Column(
                            modifier = Modifier
                                .heightIn(max = 400.dp)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    selectedEmployeeFilter = null
                                    showEmployeeSelectorDialog = false
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("filter_employee_all")
                            ) {
                                Text(
                                    text = "Semua Karyawan (${allSlips.size})",
                                    fontWeight = if (selectedEmployeeFilter == null) FontWeight.Bold else FontWeight.Normal,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            employeeNames.forEach { emp ->
                                val unpaidSum = allSlips
                                    .filter { it.employeeName.equals(emp, ignoreCase = true) }
                                    .sumOf { it.remainingAmount() }
                                OutlinedButton(
                                    onClick = {
                                        selectedEmployeeFilter = emp
                                        showEmployeeSelectorDialog = false
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = emp,
                                            fontWeight = if (selectedEmployeeFilter == emp) FontWeight.Bold else FontWeight.Normal,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Text(
                                            text = if (unpaidSum <= 0.0) "DIBAYAR" else formatRupiah(unpaidSum),
                                            color = if (unpaidSum <= 0.0) PaidBlue else LossRed,
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showEmployeeSelectorDialog = false }) { Text("Tutup") }
                    }
                )
            }

            actionTarget?.let { slip ->
                val workMillis = slip.workDateMillis.takeIf { it > 0L }
                    ?: slip.salaryDateMillis.takeIf { it > 0L }
                    ?: slip.createdAtMillis
                val paidSoFar = slip.effectivePaidAmount()
                val remaining = slip.remainingAmount()
                val isPaid = slip.paid || remaining <= 0.0

                AlertDialog(
                    onDismissRequest = { actionTarget = null },
                    title = { Text("${slip.employeeName} • ${slip.periodLabel}") },
                    text = {
                        Column(
                            modifier = Modifier.verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text("Waktu Kerja: ${formatDateTimeSlash(workMillis)}")
                            if (slip.items.isNotEmpty()) {
                                HorizontalDivider()
                                slip.items.forEach { row ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            "${row.jobName} (${formatQty(row.quantity)} ${row.unit})",
                                            style = MaterialTheme.typography.bodySmall,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Text(formatRupiah(row.subtotal), style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                                HorizontalDivider()
                            }
                            Text("Total Gaji: ${formatRupiah(slip.totalAmount)}")
                            Text("Terbayar: ${formatRupiah(paidSoFar)}", color = ProfitGreen)
                            Text(
                                text = if (isPaid) "Status: DIBAYAR PENUH" else "Sisa Belum Dibayar: ${formatRupiah(remaining)}",
                                color = if (isPaid) PaidBlue else LossRed,
                                fontWeight = FontWeight.Bold
                            )
                            if (slip.paidAtMillis > 0L) {
                                Text("Tgl Bayar Terakhir: ${formatDateTimeSlash(slip.paidAtMillis)}")
                            }
                            Spacer(Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        onBluetoothPrint(slip)
                                        actionTarget = null
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("print_bluetooth_button_${slip.id}")
                                ) {
                                    Icon(Icons.Default.Bluetooth, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Cetak")
                                }
                                OutlinedButton(
                                    onClick = {
                                        onPreviewAndPrint(slip)
                                        actionTarget = null
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("PDF")
                                }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (slip.paid || paidSoFar > 0.0) {
                                    OutlinedButton(
                                        onClick = {
                                            onTogglePaid(slip.copy(paid = true))
                                            actionTarget = null
                                        },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Batalkan Bayar", color = LossRed, maxLines = 1)
                                    }
                                }
                                OutlinedButton(
                                    onClick = {
                                        onDelete(slip)
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
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Button(
                                    onClick = {
                                        singleInstallmentTarget = slip
                                        actionTarget = null
                                    },
                                    modifier = Modifier.testTag("pay_cicilan_payslip_${slip.id}")
                                ) {
                                    Text("Bayar Cicilan")
                                }
                                Button(
                                    onClick = {
                                        onTogglePaid(slip)
                                        actionTarget = null
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = ProfitGreen)
                                ) {
                                    Text("Lunasi")
                                }
                            }
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { actionTarget = null }) { Text("Tutup") }
                    }
                )
            }

            singleInstallmentTarget?.let { target ->
                PaySinglePayslipInstallmentDialog(
                    payslip = target,
                    cashBoxes = cashBoxes,
                    onConfirm = { amt, box, notes, paidMillis ->
                        onPayInstallmentSingle(target, amt, box, notes, paidMillis)
                        singleInstallmentTarget = null
                    },
                    onDismiss = { singleInstallmentTarget = null }
                )
            }

            if (showPoolInstallmentDialog) {
                PayPoolPayslipInstallmentDialog(
                    employeeLabel = activeEmployeeLabel,
                    unpaidPayslips = unpaidByNameFilter,
                    cashBoxes = cashBoxes,
                    onConfirmPool = { amt, box, notes, paidMillis ->
                        onPayInstallmentPool(unpaidByNameFilter, amt, box, notes, paidMillis)
                        showPoolInstallmentDialog = false
                    },
                    onConfirmSingle = { slip, amt, box, notes, paidMillis ->
                        onPayInstallmentSingle(slip, amt, box, notes, paidMillis)
                        showPoolInstallmentDialog = false
                    },
                    onDismiss = { showPoolInstallmentDialog = false }
                )
            }

            if (showPayAllDialog) {
                PayAllPayslipsDialog(
                    employeeLabel = activeEmployeeLabel,
                    unpaidPayslips = unpaidByNameFilter,
                    cashBoxes = cashBoxes,
                    onConfirm = { box, paidAtMillis ->
                        onPayAll(unpaidByNameFilter, box, paidAtMillis)
                        showPayAllDialog = false
                    },
                    onDismiss = { showPayAllDialog = false }
                )
            }

            if (showPrintPaidFilteredDialog) {
                PrintPaidPayslipsFilterDialog(
                    allPayslips = allSlips,
                    employeeNames = employeeNames,
                    initialEmployeeFilter = selectedEmployeeFilter,
                    onPrintDocument = { doc ->
                        showPrintPaidFilteredDialog = false
                        onPrintThermalDocument(doc)
                    },
                    onDismiss = { showPrintPaidFilteredDialog = false }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun PaySinglePayslipInstallmentDialog(
    payslip: Payslip,
    cashBoxes: List<CashBox>,
    onConfirm: (Double, CashBox?, String, Long) -> Unit,
    onDismiss: () -> Unit
) {
    val remaining = payslip.remainingAmount()
    var amountStr by remember { mutableStateOf(remaining.toLong().toString()) }
    var selectedBox by remember { mutableStateOf(cashBoxes.firstOrNull()) }
    LaunchedEffect(cashBoxes) {
        if (selectedBox == null) selectedBox = cashBoxes.firstOrNull()
    }
    var boxMenu by remember { mutableStateOf(false) }
    var notes by remember { mutableStateOf("") }
    var paidAtMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var error by remember { mutableStateOf<String?>(null) }

    val parsedAmt = (amountStr.replace(",", ".").toDoubleOrNull() ?: 0.0).coerceAtLeast(0.0)
    val previewApplied = minOf(parsedAmt, remaining)
    val previewLeft = (remaining - previewApplied).coerceAtLeast(0.0)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Bayar Cicilan Gaji") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    "${payslip.employeeName} (${payslip.periodLabel})",
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    "Total: ${formatRupiah(payslip.totalAmount)} • Sudah Dibayar: ${formatRupiah(payslip.effectivePaidAmount())}",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    "Sisa Belum Dibayar: ${formatRupiah(remaining)}",
                    color = LossRed,
                    fontWeight = FontWeight.Bold
                )

                DateTimeSelectorSection(
                    title = "Tanggal & Waktu Pembayaran",
                    millis = paidAtMillis,
                    onMillisChange = { paidAtMillis = it },
                    tagPrefix = "payslip_cicilan_single"
                )

                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it; error = null },
                    label = { Text("Nominal Cicilan / Bayar (Rp) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("single_payslip_installment_input")
                )

                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(
                        "25%" to (remaining * 0.25).toLong(),
                        "50%" to (remaining * 0.50).toLong(),
                        "100rb" to 100_000L,
                        "Lunas" to remaining.toLong()
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
                        text = if (previewLeft <= 0.001) "Status setelah bayar: DIBAYAR PENUH"
                        else "Sisa gaji menjadi: ${formatRupiah(previewLeft)}",
                        color = if (previewLeft <= 0.001) ProfitGreen else PaidBlue,
                        style = MaterialTheme.typography.labelMedium
                    )
                }

                if (cashBoxes.isNotEmpty()) {
                    ExposedDropdownMenuBox(expanded = boxMenu, onExpandedChange = { boxMenu = it }) {
                        OutlinedTextField(
                            value = selectedBox?.let { "${it.name} (${formatRupiah(it.balance)})" } ?: "Pilih Cash Box",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Ambil dana dari Cash Box") },
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
                    if (amt <= 0.0) {
                        error = "Masukkan nominal cicilan > 0"
                        return@Button
                    }
                    onConfirm(amt, selectedBox, notes, paidAtMillis)
                },
                modifier = Modifier.testTag("confirm_single_payslip_installment_button")
            ) {
                Text("Simpan Cicilan")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun PayPoolPayslipInstallmentDialog(
    employeeLabel: String,
    unpaidPayslips: List<Payslip>,
    cashBoxes: List<CashBox>,
    onConfirmPool: (Double, CashBox?, String, Long) -> Unit,
    onConfirmSingle: (Payslip, Double, CashBox?, String, Long) -> Unit,
    onDismiss: () -> Unit
) {
    val sortedUnpaid = remember(unpaidPayslips) {
        unpaidPayslips.sortedBy {
            it.workDateMillis.takeIf { m -> m > 0L }
                ?: it.salaryDateMillis.takeIf { m -> m > 0L }
                ?: it.createdAtMillis
        }
    }
    val totalUnpaid = sortedUnpaid.sumOf { it.remainingAmount() }
    var usePoolMode by remember { mutableStateOf(true) }
    var chosenSlip by remember { mutableStateOf(sortedUnpaid.firstOrNull()) }
    var slipMenu by remember { mutableStateOf(false) }

    val activeTargetTotal = if (usePoolMode) totalUnpaid else (chosenSlip?.remainingAmount() ?: totalUnpaid)
    var amountStr by remember { mutableStateOf("") }
    var selectedBox by remember { mutableStateOf(cashBoxes.firstOrNull()) }
    LaunchedEffect(cashBoxes) {
        if (selectedBox == null) selectedBox = cashBoxes.firstOrNull()
    }
    var boxMenu by remember { mutableStateOf(false) }
    var notes by remember { mutableStateOf("") }
    var paidAtMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var error by remember { mutableStateOf<String?>(null) }

    val parsedAmt = (amountStr.replace(",", ".").toDoubleOrNull() ?: 0.0).coerceAtLeast(0.0)
    val previewLeft = (activeTargetTotal - minOf(parsedAmt, activeTargetTotal)).coerceAtLeast(0.0)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Bayar Cicilan Gaji") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    "$employeeLabel • ${sortedUnpaid.size} slip belum lunas",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    "Total Sisa Gaji: ${formatRupiah(totalUnpaid)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = LossRed
                )

                if (sortedUnpaid.size > 1) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = usePoolMode,
                            onClick = { usePoolMode = true },
                            label = { Text("Otomatis Berurutan") }
                        )
                        FilterChip(
                            selected = !usePoolMode,
                            onClick = { usePoolMode = false },
                            label = { Text("Pilih 1 Slip") }
                        )
                    }
                }

                if (!usePoolMode && sortedUnpaid.isNotEmpty()) {
                    ExposedDropdownMenuBox(expanded = slipMenu, onExpandedChange = { slipMenu = it }) {
                        OutlinedTextField(
                            value = chosenSlip?.let { "${it.employeeName} (${formatRupiah(it.remainingAmount())})" }
                                ?: "Pilih Slip Gaji",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Slip Gaji") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(slipMenu) },
                            modifier = Modifier
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(expanded = slipMenu, onDismissRequest = { slipMenu = false }) {
                            sortedUnpaid.forEach { slip ->
                                DropdownMenuItem(
                                    text = {
                                        Text("${slip.employeeName} • ${slip.periodLabel} (Sisa ${formatRupiah(slip.remainingAmount())})")
                                    },
                                    onClick = { chosenSlip = slip; slipMenu = false }
                                )
                            }
                        }
                    }
                }

                DateTimeSelectorSection(
                    title = "Tanggal & Waktu Pembayaran",
                    millis = paidAtMillis,
                    onMillisChange = { paidAtMillis = it },
                    tagPrefix = "payslip_cicilan_pool"
                )

                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it; error = null },
                    label = { Text("Nominal Cicilan Dibayarkan (Rp) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("pool_payslip_installment_input")
                )

                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(
                        "50rb" to 50_000L,
                        "100rb" to 100_000L,
                        "50%" to (activeTargetTotal * 0.5).toLong(),
                        "Penuh" to activeTargetTotal.toLong()
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
                        text = if (previewLeft <= 0.001) "Sisa tagihan gaji menjadi: Rp 0 (DIBAYAR)"
                        else "Sisa tagihan gaji menjadi: ${formatRupiah(previewLeft)}",
                        color = if (previewLeft <= 0.001) ProfitGreen else PaidBlue,
                        style = MaterialTheme.typography.labelMedium
                    )
                }

                if (cashBoxes.isNotEmpty()) {
                    ExposedDropdownMenuBox(expanded = boxMenu, onExpandedChange = { boxMenu = it }) {
                        OutlinedTextField(
                            value = selectedBox?.let { "${it.name} (${formatRupiah(it.balance)})" } ?: "Pilih Cash Box",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Ambil dana dari Cash Box") },
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
                    label = { Text("Catatan Cicilan") },
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
                    if (amt <= 0.0) {
                        error = "Masukkan nominal cicilan > 0"
                        return@Button
                    }
                    if (usePoolMode) {
                        onConfirmPool(amt, selectedBox, notes, paidAtMillis)
                    } else {
                        val slip = chosenSlip
                        if (slip == null) {
                            error = "Pilih slip gaji terlebih dahulu"
                            return@Button
                        }
                        onConfirmSingle(slip, amt, selectedBox, notes, paidAtMillis)
                    }
                },
                modifier = Modifier.testTag("confirm_pool_payslip_installment_button")
            ) {
                Text("Bayar Cicilan")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PayAllPayslipsDialog(
    employeeLabel: String,
    unpaidPayslips: List<Payslip>,
    cashBoxes: List<CashBox>,
    onConfirm: (CashBox?, Long) -> Unit,
    onDismiss: () -> Unit
) {
    val totalToPay = unpaidPayslips.sumOf { it.remainingAmount() }
    var selectedBox by remember { mutableStateOf(cashBoxes.firstOrNull()) }
    LaunchedEffect(cashBoxes) {
        if (selectedBox == null) selectedBox = cashBoxes.firstOrNull()
    }
    var boxMenu by remember { mutableStateOf(false) }
    var paidAtMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Bayar Semua Gaji") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    "Melunasi ${unpaidPayslips.size} slip gaji untuk \"$employeeLabel\" dengan total sisa:",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = formatRupiah(totalToPay),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = LossRed
                )

                DateTimeSelectorSection(
                    title = "Tanggal & Waktu Gajian Dibayar",
                    millis = paidAtMillis,
                    onMillisChange = { paidAtMillis = it },
                    tagPrefix = "pay_all_slips"
                )

                if (cashBoxes.isNotEmpty()) {
                    ExposedDropdownMenuBox(expanded = boxMenu, onExpandedChange = { boxMenu = it }) {
                        OutlinedTextField(
                            value = selectedBox?.let { "${it.name} (${formatRupiah(it.balance)})" } ?: "Pilih Cash Box",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Ambil dana dari Cash Box") },
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
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(selectedBox, paidAtMillis) },
                colors = ButtonDefaults.buttonColors(containerColor = ProfitGreen),
                modifier = Modifier.testTag("confirm_pay_all_payslips_button")
            ) {
                Text("Bayar Semua")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MarkPayslipPaidDialog(
    payslip: Payslip,
    cashBoxes: List<CashBox>,
    onConfirm: (CashBox?, Long) -> Unit,
    onDismiss: () -> Unit
) {
    val remaining = payslip.remainingAmount()
    var selectedBox by remember { mutableStateOf(cashBoxes.firstOrNull()) }
    LaunchedEffect(cashBoxes) {
        if (selectedBox == null) selectedBox = cashBoxes.firstOrNull()
    }
    var boxMenu by remember { mutableStateOf(false) }
    var paidAtMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Lunasi Slip Gaji") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Lunasi sisa gaji ${payslip.employeeName} (${formatRupiah(remaining)}) menjadi DIBAYAR PENUH?")

                DateTimeSelectorSection(
                    title = "Tanggal & Waktu Gajian Dibayar",
                    millis = paidAtMillis,
                    onMillisChange = { paidAtMillis = it },
                    tagPrefix = "mark_paid"
                )

                if (cashBoxes.isNotEmpty()) {
                    ExposedDropdownMenuBox(expanded = boxMenu, onExpandedChange = { boxMenu = it }) {
                        OutlinedTextField(
                            value = selectedBox?.name ?: "Pilih Cash Box",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Ambil dana dari Cash Box") },
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
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(selectedBox, paidAtMillis) },
                modifier = Modifier.testTag("confirm_mark_paid_button")
            ) {
                Text("Konfirmasi Bayar")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } }
    )
}

@Composable
internal fun PayslipPreviewDialog(
    payslip: Payslip,
    storeSettings: StoreSettings,
    onPrintPdf: () -> Unit,
    onPrintBluetooth: () -> Unit,
    onDismiss: () -> Unit
) {
    val workMillis = payslip.workDateMillis.takeIf { it > 0L }
        ?: payslip.salaryDateMillis.takeIf { it > 0L }
        ?: payslip.createdAtMillis
    val paidSoFar = payslip.effectivePaidAmount()
    val remaining = payslip.remainingAmount()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pratinjau Slip Gaji") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(storeSettings.storeName.ifBlank { "Toko & Pabrik AFI" }, style = MaterialTheme.typography.titleMedium)
                if (storeSettings.address.isNotBlank()) {
                    Text(storeSettings.address, style = MaterialTheme.typography.bodySmall)
                }
                HorizontalDivider()
                Text("Karyawan: ${payslip.employeeName}", style = MaterialTheme.typography.titleSmall)
                Text("Periode: ${payslip.periodLabel}", style = MaterialTheme.typography.bodySmall)
                Text(
                    "Waktu & Tanggal Bekerja: ${formatDateTime(workMillis)}",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    "Status: ${if (payslip.paid) "SUDAH DIBAYAR" else if (paidSoFar > 0.0) "CICILAN" else "BELUM DIBAYAR"}",
                    color = if (payslip.paid) ProfitGreen else WarningAmber,
                    style = MaterialTheme.typography.labelMedium
                )
                if (paidSoFar > 0.0 && payslip.paidAtMillis > 0L) {
                    Text(
                        "Tanggal Gajian Dibayar: ${formatDateTime(payslip.paidAtMillis)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = ProfitGreen
                    )
                }
                HorizontalDivider()
                payslip.items.forEach { row ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("${row.jobName} (${formatQty(row.quantity)} ${row.unit})", style = MaterialTheme.typography.bodySmall)
                        Text(formatRupiah(row.subtotal), style = MaterialTheme.typography.bodySmall)
                    }
                }
                if (payslip.bonus > 0) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Bonus", style = MaterialTheme.typography.bodySmall)
                        Text("+${formatRupiah(payslip.bonus)}", style = MaterialTheme.typography.bodySmall)
                    }
                }
                if (payslip.deduction > 0) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Potongan", style = MaterialTheme.typography.bodySmall)
                        Text("-${formatRupiah(payslip.deduction)}", style = MaterialTheme.typography.bodySmall)
                    }
                }
                HorizontalDivider()
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("TOTAL GAJI", style = MaterialTheme.typography.titleSmall)
                    CurrencyText(amount = payslip.totalAmount)
                }
                if (!payslip.paid && paidSoFar > 0.0) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Terbayar", style = MaterialTheme.typography.bodySmall, color = ProfitGreen)
                        Text(formatRupiah(paidSoFar), style = MaterialTheme.typography.bodySmall, color = ProfitGreen)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("SISA GAJI", style = MaterialTheme.typography.titleSmall, color = LossRed)
                        CurrencyText(amount = remaining, color = LossRed)
                    }
                }
                Spacer(modifier = Modifier.padding(top = 4.dp))
                OutlinedButton(
                    onClick = {
                        onDismiss()
                        onPrintBluetooth()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Bluetooth, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Cetak")
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                onPrintPdf()
                onDismiss()
            }) {
                Icon(Icons.Default.Print, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("Cetak PDF")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Tutup") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DateTimeSelectorSection(
    title: String,
    millis: Long,
    onMillisChange: (Long) -> Unit,
    tagPrefix: String
) {
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    val timeText = remember(millis) {
        SimpleDateFormat("HH:mm", Locale.forLanguageTag("id-ID")).format(Date(millis))
    }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, style = MaterialTheme.typography.labelMedium)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = { showDatePicker = true },
                modifier = Modifier
                    .weight(1f)
                    .testTag("${tagPrefix}_date_button")
            ) {
                Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(formatDate(millis), maxLines = 1)
            }
            OutlinedButton(
                onClick = { showTimePicker = true },
                modifier = Modifier
                    .weight(0.75f)
                    .testTag("${tagPrefix}_time_button")
            ) {
                Icon(Icons.Default.AccessTime, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(timeText, maxLines = 1)
            }
        }
    }

    if (showDatePicker) {
        val localCal = Calendar.getInstance().apply { timeInMillis = millis }
        val initialUtcMillis = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            clear()
            set(localCal.get(Calendar.YEAR), localCal.get(Calendar.MONTH), localCal.get(Calendar.DAY_OF_MONTH))
        }.timeInMillis
        val dateState = rememberDatePickerState(initialSelectedDateMillis = initialUtcMillis)

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val pickedUtc = dateState.selectedDateMillis
                    if (pickedUtc != null) {
                        val utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                            timeInMillis = pickedUtc
                        }
                        val updated = Calendar.getInstance().apply {
                            timeInMillis = millis
                            set(Calendar.YEAR, utcCal.get(Calendar.YEAR))
                            set(Calendar.MONTH, utcCal.get(Calendar.MONTH))
                            set(Calendar.DAY_OF_MONTH, utcCal.get(Calendar.DAY_OF_MONTH))
                        }
                        onMillisChange(updated.timeInMillis)
                    }
                    showDatePicker = false
                }) {
                    Text("Pilih")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Batal") }
            }
        ) {
            DatePicker(state = dateState)
        }
    }

    if (showTimePicker) {
        val cal = Calendar.getInstance().apply { timeInMillis = millis }
        val timeState = rememberTimePickerState(
            initialHour = cal.get(Calendar.HOUR_OF_DAY),
            initialMinute = cal.get(Calendar.MINUTE),
            is24Hour = true
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text("Pilih Jam") },
            text = { TimePicker(state = timeState) },
            confirmButton = {
                TextButton(onClick = {
                    val updated = Calendar.getInstance().apply {
                        timeInMillis = millis
                        set(Calendar.HOUR_OF_DAY, timeState.hour)
                        set(Calendar.MINUTE, timeState.minute)
                        set(Calendar.SECOND, 0)
                    }
                    onMillisChange(updated.timeInMillis)
                    showTimePicker = false
                }) {
                    Text("Pilih")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("Batal") }
            }
        )
    }
}
