package com.example.ui.screens

import android.app.DatePickerDialog
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.CalendarMonth
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.DebtRecord
import com.example.data.model.Payslip
import com.example.ui.theme.LossRed
import com.example.ui.theme.PaidBlue
import com.example.ui.theme.ProfitGreen
import com.example.ui.theme.ProfitGreenBg
import com.example.util.ThermalReceiptDocument
import com.example.util.formatDateSlash
import com.example.util.formatRupiah
import com.example.util.toPaidDebtsThermalReceipt
import com.example.util.toPaidThermalReceipt
import java.util.Calendar

private fun startOfDay(millis: Long): Long {
    return Calendar.getInstance().apply {
        timeInMillis = millis
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}

private fun endOfDay(millis: Long): Long {
    return Calendar.getInstance().apply {
        timeInMillis = millis
        set(Calendar.HOUR_OF_DAY, 23)
        set(Calendar.MINUTE, 59)
        set(Calendar.SECOND, 59)
        set(Calendar.MILLISECOND, 999)
    }.timeInMillis
}

private fun startOfCurrentMonthMillis(): Long {
    return Calendar.getInstance().apply {
        set(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PrintPaidPayslipsFilterDialog(
    allPayslips: List<Payslip>,
    employeeNames: List<String>,
    initialEmployeeFilter: String?,
    onPrintDocument: (ThermalReceiptDocument) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var selectedName by remember(initialEmployeeFilter) { mutableStateOf(initialEmployeeFilter) }
    var nameExpanded by remember { mutableStateOf(false) }
    var startMillis by remember { mutableLongStateOf(startOfCurrentMonthMillis()) }
    var endMillis by remember { mutableLongStateOf(endOfDay(System.currentTimeMillis())) }
    var filterByPaidDate by remember { mutableStateOf(true) }

    val rangeStart = remember(startMillis, endMillis) { startOfDay(minOf(startMillis, endMillis)) }
    val rangeEnd = remember(startMillis, endMillis) { endOfDay(maxOf(startMillis, endMillis)) }

    val matchedSlips = remember(allPayslips, selectedName, rangeStart, rangeEnd, filterByPaidDate) {
        allPayslips.filter { slip ->
            val isFullyPaid = slip.paid || slip.remainingAmount() <= 0.0
            if (!isFullyPaid) return@filter false

            val matchesName = selectedName == null ||
                slip.employeeName.equals(selectedName, ignoreCase = true)
            if (!matchesName) return@filter false

            val workMillis = slip.workDateMillis.takeIf { it > 0L }
                ?: slip.salaryDateMillis.takeIf { it > 0L }
                ?: slip.createdAtMillis
            val targetDateMillis = if (filterByPaidDate) {
                slip.paidAtMillis.takeIf { it > 0L } ?: workMillis
            } else {
                workMillis
            }
            targetDateMillis in rangeStart..rangeEnd
        }
    }
    val totalAmount = remember(matchedSlips) { matchedSlips.sumOf { it.totalAmount } }
    val filterLabel = selectedName ?: "Semua Karyawan"

    fun pickDate(currentMillis: Long, onPicked: (Long) -> Unit) {
        val cal = Calendar.getInstance().apply { timeInMillis = currentMillis }
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val chosen = Calendar.getInstance().apply {
                    set(year, month, dayOfMonth, 12, 0, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
                onPicked(chosen)
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Cetak Gaji Dibayar Penuh (1 Struk)") },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 470.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ExposedDropdownMenuBox(
                    expanded = nameExpanded,
                    onExpandedChange = { nameExpanded = it }
                ) {
                    OutlinedTextField(
                        value = filterLabel,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Filter Nama Karyawan") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(nameExpanded) },
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth()
                            .testTag("paid_payslip_filter_name_dropdown")
                    )
                    ExposedDropdownMenu(
                        expanded = nameExpanded,
                        onDismissRequest = { nameExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Semua Karyawan") },
                            onClick = {
                                selectedName = null
                                nameExpanded = false
                            }
                        )
                        employeeNames.forEach { emp ->
                            DropdownMenuItem(
                                text = { Text(emp) },
                                onClick = {
                                    selectedName = emp
                                    nameExpanded = false
                                }
                            )
                        }
                    }
                }

                Text("Rentang Tanggal", style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { pickDate(startMillis) { startMillis = startOfDay(it) } },
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("paid_payslip_start_date_btn")
                    ) {
                        Icon(Icons.Default.CalendarMonth, contentDescription = "Dari tanggal", modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Column {
                            Text("Dari", style = MaterialTheme.typography.labelSmall)
                            Text(formatDateSlash(rangeStart), maxLines = 1, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                    OutlinedButton(
                        onClick = { pickDate(endMillis) { endMillis = endOfDay(it) } },
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("paid_payslip_end_date_btn")
                    ) {
                        Icon(Icons.Default.CalendarMonth, contentDescription = "Sampai tanggal", modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Column {
                            Text("Sampai", style = MaterialTheme.typography.labelSmall)
                            Text(formatDateSlash(rangeEnd), maxLines = 1, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = filterByPaidDate,
                        onClick = { filterByPaidDate = true },
                        label = { Text("Tgl Dibayar") }
                    )
                    FilterChip(
                        selected = !filterByPaidDate,
                        onClick = { filterByPaidDate = false },
                        label = { Text("Tgl Kerja") }
                    )
                }

                Surface(
                    color = if (matchedSlips.isNotEmpty()) ProfitGreenBg else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "${matchedSlips.size} transaksi gaji dibayar penuh",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (matchedSlips.isNotEmpty()) ProfitGreen else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Total: ${formatRupiah(totalAmount)}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (matchedSlips.isNotEmpty()) ProfitGreen else LossRed
                        )
                    }
                }

                if (matchedSlips.isEmpty()) {
                    Text(
                        text = "Tidak ada transaksi gaji yang sudah dibayar penuh sesuai filter nama dan tanggal tersebut.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    HorizontalDivider()
                    matchedSlips.take(25).forEach { slip ->
                        val workMillis = slip.workDateMillis.takeIf { it > 0L }
                            ?: slip.salaryDateMillis.takeIf { it > 0L }
                            ?: slip.createdAtMillis
                        val paidMillis = slip.paidAtMillis.takeIf { it > 0L } ?: workMillis
                        Surface(
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            shape = MaterialTheme.shapes.small,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = slip.employeeName,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "Kerja ${formatDateSlash(workMillis)} • Lunas ${formatDateSlash(paidMillis)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    text = formatRupiah(slip.totalAmount),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = PaidBlue
                                )
                            }
                        }
                    }
                    if (matchedSlips.size > 25) {
                        Text(
                            text = "+ ${matchedSlips.size - 25} slip lainnya akan ikut tercetak dalam 1 struk.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val doc = matchedSlips.toPaidThermalReceipt(
                        employeeFilterLabel = filterLabel,
                        startMillis = rangeStart,
                        endMillis = rangeEnd,
                        byPaidDate = filterByPaidDate
                    )
                    onPrintDocument(doc)
                },
                enabled = matchedSlips.isNotEmpty(),
                modifier = Modifier.testTag("confirm_print_paid_payslips_btn")
            ) {
                Icon(Icons.Default.Bluetooth, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Cetak 1 Struk")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PrintPaidDebtsFilterDialog(
    allDebts: List<DebtRecord>,
    initialTab: Int, // 0=HUTANG, 1=PIUTANG
    initialPartyFilter: String?,
    onPrintDocument: (ThermalReceiptDocument) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    // "HUTANG", "PIUTANG", "ALL"
    var selectedType by remember(initialTab) {
        mutableStateOf(if (initialTab == 0) "HUTANG" else "PIUTANG")
    }
    var selectedParty by remember(initialPartyFilter) { mutableStateOf(initialPartyFilter) }
    var partyExpanded by remember { mutableStateOf(false) }
    var startMillis by remember { mutableLongStateOf(startOfCurrentMonthMillis()) }
    var endMillis by remember { mutableLongStateOf(endOfDay(System.currentTimeMillis())) }
    var filterByPaidDate by remember { mutableStateOf(true) }

    val availablePartyNames = remember(allDebts, selectedType) {
        allDebts
            .filter { selectedType == "ALL" || it.debtType == selectedType }
            .map { it.partyName.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .sortedBy { it.lowercase() }
    }

    val effectiveParty = if (selectedParty != null && availablePartyNames.any { it.equals(selectedParty, ignoreCase = true) }) {
        selectedParty
    } else {
        null
    }

    val rangeStart = remember(startMillis, endMillis) { startOfDay(minOf(startMillis, endMillis)) }
    val rangeEnd = remember(startMillis, endMillis) { endOfDay(maxOf(startMillis, endMillis)) }

    val matchedDebts = remember(allDebts, selectedType, effectiveParty, rangeStart, rangeEnd, filterByPaidDate) {
        allDebts.filter { debt ->
            val isLunas = debt.status == "LUNAS" || debt.remainingAmount <= 0.0
            if (!isLunas) return@filter false

            val matchesType = selectedType == "ALL" || debt.debtType == selectedType
            if (!matchesType) return@filter false

            val matchesParty = effectiveParty == null ||
                debt.partyName.equals(effectiveParty, ignoreCase = true)
            if (!matchesParty) return@filter false

            val targetDateMillis = if (filterByPaidDate) {
                debt.paidAtMillis.takeIf { it > 0L } ?: debt.transactionDateMillis
            } else {
                debt.transactionDateMillis
            }
            targetDateMillis in rangeStart..rangeEnd
        }
    }

    val totalAmount = remember(matchedDebts) { matchedDebts.sumOf { it.totalAmount } }
    val partyLabel = effectiveParty ?: "Semua Nama"
    val typeLabel = when (selectedType) {
        "HUTANG" -> "Hutang"
        "PIUTANG" -> "Piutang"
        else -> "Hutang & Piutang"
    }

    fun pickDate(currentMillis: Long, onPicked: (Long) -> Unit) {
        val cal = Calendar.getInstance().apply { timeInMillis = currentMillis }
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val chosen = Calendar.getInstance().apply {
                    set(year, month, dayOfMonth, 12, 0, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
                onPicked(chosen)
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Cetak $typeLabel Lunas (1 Struk)") },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 490.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Jenis Transaksi", style = MaterialTheme.typography.labelMedium)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = selectedType == "HUTANG",
                        onClick = { selectedType = "HUTANG" },
                        label = { Text("Hutang") },
                        modifier = Modifier.testTag("paid_debt_type_hutang")
                    )
                    FilterChip(
                        selected = selectedType == "PIUTANG",
                        onClick = { selectedType = "PIUTANG" },
                        label = { Text("Piutang") },
                        modifier = Modifier.testTag("paid_debt_type_piutang")
                    )
                    FilterChip(
                        selected = selectedType == "ALL",
                        onClick = { selectedType = "ALL" },
                        label = { Text("Hutang & Piutang") },
                        modifier = Modifier.testTag("paid_debt_type_all")
                    )
                }

                ExposedDropdownMenuBox(
                    expanded = partyExpanded,
                    onExpandedChange = { partyExpanded = it }
                ) {
                    OutlinedTextField(
                        value = partyLabel,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Filter Nama Pemasok / Pedagang") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(partyExpanded) },
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth()
                            .testTag("paid_debt_filter_name_dropdown")
                    )
                    ExposedDropdownMenu(
                        expanded = partyExpanded,
                        onDismissRequest = { partyExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Semua Nama") },
                            onClick = {
                                selectedParty = null
                                partyExpanded = false
                            }
                        )
                        availablePartyNames.forEach { party ->
                            DropdownMenuItem(
                                text = { Text(party) },
                                onClick = {
                                    selectedParty = party
                                    partyExpanded = false
                                }
                            )
                        }
                    }
                }

                Text("Rentang Tanggal", style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { pickDate(startMillis) { startMillis = startOfDay(it) } },
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("paid_debt_start_date_btn")
                    ) {
                        Icon(Icons.Default.CalendarMonth, contentDescription = "Dari tanggal", modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Column {
                            Text("Dari", style = MaterialTheme.typography.labelSmall)
                            Text(formatDateSlash(rangeStart), maxLines = 1, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                    OutlinedButton(
                        onClick = { pickDate(endMillis) { endMillis = endOfDay(it) } },
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("paid_debt_end_date_btn")
                    ) {
                        Icon(Icons.Default.CalendarMonth, contentDescription = "Sampai tanggal", modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Column {
                            Text("Sampai", style = MaterialTheme.typography.labelSmall)
                            Text(formatDateSlash(rangeEnd), maxLines = 1, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = filterByPaidDate,
                        onClick = { filterByPaidDate = true },
                        label = { Text("Tgl Lunas") }
                    )
                    FilterChip(
                        selected = !filterByPaidDate,
                        onClick = { filterByPaidDate = false },
                        label = { Text("Tgl Transaksi") }
                    )
                }

                Surface(
                    color = if (matchedDebts.isNotEmpty()) ProfitGreenBg else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "${matchedDebts.size} transaksi $typeLabel lunas",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (matchedDebts.isNotEmpty()) ProfitGreen else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Total: ${formatRupiah(totalAmount)}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (matchedDebts.isNotEmpty()) ProfitGreen else LossRed
                        )
                    }
                }

                if (matchedDebts.isEmpty()) {
                    Text(
                        text = "Tidak ada transaksi $typeLabel yang sudah lunas sesuai filter nama dan tanggal tersebut.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    HorizontalDivider()
                    matchedDebts.take(25).forEach { debt ->
                        val paidMillis = debt.paidAtMillis.takeIf { it > 0L } ?: debt.transactionDateMillis
                        Surface(
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            shape = MaterialTheme.shapes.small,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${debt.partyName} (${debt.invoiceNo.ifBlank { debt.debtType }})",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${if (debt.debtType == "HUTANG") "Hutang" else "Piutang"} • Lunas ${formatDateSlash(paidMillis)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    text = formatRupiah(debt.totalAmount),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = PaidBlue
                                )
                            }
                        }
                    }
                    if (matchedDebts.size > 25) {
                        Text(
                            text = "+ ${matchedDebts.size - 25} nota lainnya akan ikut tercetak dalam 1 struk.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val doc = matchedDebts.toPaidDebtsThermalReceipt(
                        typeLabel = typeLabel,
                        partyFilterLabel = partyLabel,
                        startMillis = rangeStart,
                        endMillis = rangeEnd,
                        byPaidDate = filterByPaidDate
                    )
                    onPrintDocument(doc)
                },
                enabled = matchedDebts.isNotEmpty(),
                modifier = Modifier.testTag("confirm_print_paid_debts_btn")
            ) {
                Icon(Icons.Default.Bluetooth, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Cetak 1 Struk")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        }
    )
}
