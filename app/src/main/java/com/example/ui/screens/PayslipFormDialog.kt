package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.CashBox
import com.example.data.model.Employee
import com.example.data.model.JobType
import com.example.data.model.Payslip
import com.example.data.model.PayslipItem
import com.example.ui.components.CurrencyText
import com.example.ui.theme.LossRed
import com.example.ui.theme.ProfitGreen
import com.example.util.formatDate
import com.example.util.formatRupiah

private data class EditablePayrollLine(
    val jobId: String,
    val jobName: String,
    val unit: String,
    val qtyStr: String,
    val rateStr: String
) {
    val qty: Double get() = qtyStr.replace(",", ".").toDoubleOrNull() ?: 0.0
    val rate: Double get() = rateStr.replace(",", ".").toDoubleOrNull() ?: 0.0
    val subtotal: Double get() = (qty.coerceAtLeast(0.0)) * (rate.coerceAtLeast(0.0))
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun CreatePayslipDialog(
    employees: List<Employee>,
    jobTypes: List<JobType>,
    cashBoxes: List<CashBox>,
    dialogTitle: String = "Transaksi Gaji Karyawan",
    onSave: (Payslip, CashBox?) -> Unit,
    onDismiss: () -> Unit
) {
    val activeEmployees = remember(employees) {
        employees.filter { it.active }.ifEmpty { employees }
    }
    var selectedEmp by remember { mutableStateOf(activeEmployees.firstOrNull()) }
    var empMenu by remember { mutableStateOf(false) }
    var workDateMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var periodLabel by remember { mutableStateOf("Kerja ${formatDate(workDateMillis)}") }
    var periodEdited by remember { mutableStateOf(false) }

    val lines = remember {
        mutableStateListOf<EditablePayrollLine>().apply {
            val emp = activeEmployees.firstOrNull()
            val assignedIds = emp?.assignedJobIds().orEmpty()
            val initialJobs = if (assignedIds.isNotEmpty()) {
                jobTypes.filter { it.id in assignedIds }
            } else {
                listOfNotNull(jobTypes.firstOrNull())
            }
            initialJobs.forEach { job ->
                add(
                    EditablePayrollLine(
                        jobId = job.id,
                        jobName = job.name,
                        unit = job.unit,
                        qtyStr = "1",
                        rateStr = job.ratePerUnit.toLong().toString()
                    )
                )
            }
        }
    }

    var bonusStr by remember { mutableStateOf("0") }
    var deductionStr by remember { mutableStateOf("0") }
    var payMode by remember { mutableStateOf("UNPAID") } // "UNPAID", "CICILAN", "PAID"
    var initialCicilanStr by remember { mutableStateOf("") }
    var paidAtMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var selectedBox by remember { mutableStateOf(cashBoxes.firstOrNull()) }
    var boxMenu by remember { mutableStateOf(false) }
    var notes by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    val bonus = bonusStr.replace(",", ".").toDoubleOrNull() ?: 0.0
    val deduction = deductionStr.replace(",", ".").toDoubleOrNull() ?: 0.0
    val total = (lines.sumOf { it.subtotal } + bonus - deduction).coerceAtLeast(0.0)

    fun applyEmployeeDefaultJobs(emp: Employee) {
        val assignedIds = emp.assignedJobIds()
        if (assignedIds.isNotEmpty()) {
            lines.clear()
            jobTypes.filter { it.id in assignedIds }.forEach { job ->
                lines.add(
                    EditablePayrollLine(
                        jobId = job.id,
                        jobName = job.name,
                        unit = job.unit,
                        qtyStr = "1",
                        rateStr = job.ratePerUnit.toLong().toString()
                    )
                )
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(dialogTitle) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (activeEmployees.isEmpty()) {
                    Text("Tambah Data Karyawan Pabrik terlebih dahulu di menu Kontak.", color = LossRed)
                } else {
                    ExposedDropdownMenuBox(expanded = empMenu, onExpandedChange = { empMenu = it }) {
                        OutlinedTextField(
                            value = selectedEmp?.name ?: "Pilih Karyawan",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Karyawan *") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(empMenu) },
                            modifier = Modifier
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                .fillMaxWidth()
                                .testTag("salary_employee_dropdown")
                        )
                        ExposedDropdownMenu(expanded = empMenu, onDismissRequest = { empMenu = false }) {
                            activeEmployees.forEach { e ->
                                val jobsDesc = e.assignedJobNames().joinToString(", ")
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(e.name)
                                            if (jobsDesc.isNotBlank()) {
                                                Text(
                                                    jobsDesc,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    },
                                    onClick = {
                                        selectedEmp = e
                                        empMenu = false
                                        applyEmployeeDefaultJobs(e)
                                    }
                                )
                            }
                        }
                    }

                    val empJobs = selectedEmp?.assignedJobNames().orEmpty()
                    if (empJobs.isNotEmpty()) {
                        Text(
                            text = "Pekerjaan karyawan: ${empJobs.joinToString(", ")}",
                            style = MaterialTheme.typography.labelSmall,
                            color = ProfitGreen
                        )
                    }

                    DateTimeSelectorSection(
                        title = "Tanggal & Waktu Bekerja *",
                        millis = workDateMillis,
                        onMillisChange = {
                            workDateMillis = it
                            if (!periodEdited) {
                                periodLabel = "Kerja ${formatDate(it)}"
                            }
                        },
                        tagPrefix = "work_datetime"
                    )

                    OutlinedTextField(
                        value = periodLabel,
                        onValueChange = {
                            periodLabel = it
                            periodEdited = true
                        },
                        label = { Text("Periode / Keterangan *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (jobTypes.isNotEmpty()) {
                        HorizontalDivider()
                        Text(
                            text = "Pilih Pekerjaan (Bisa Lebih dari 1)",
                            style = MaterialTheme.typography.labelLarge
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            jobTypes.forEach { job ->
                                val isSelected = lines.any { it.jobId == job.id }
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        error = null
                                        if (isSelected) {
                                            lines.removeAll { it.jobId == job.id }
                                        } else {
                                            lines.add(
                                                EditablePayrollLine(
                                                    jobId = job.id,
                                                    jobName = job.name,
                                                    unit = job.unit,
                                                    qtyStr = "1",
                                                    rateStr = job.ratePerUnit.toLong().toString()
                                                )
                                            )
                                        }
                                    },
                                    label = { Text("${job.name} (${formatRupiah(job.ratePerUnit)}/${job.unit})") },
                                    modifier = Modifier.testTag("job_chip_${job.code}")
                                )
                            }
                        }

                        lines.forEachIndexed { idx, row ->
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "${row.jobName} (${row.unit})",
                                            style = MaterialTheme.typography.labelLarge,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Text(
                                            text = formatRupiah(row.subtotal),
                                            style = MaterialTheme.typography.labelMedium,
                                            color = ProfitGreen
                                        )
                                        IconButton(onClick = { lines.removeAt(idx) }) {
                                            Icon(Icons.Default.DeleteOutline, contentDescription = "Hapus pekerjaan")
                                        }
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedTextField(
                                            value = row.qtyStr,
                                            onValueChange = { lines[idx] = row.copy(qtyStr = it) },
                                            label = { Text("Jumlah (${row.unit})") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                            singleLine = true,
                                            modifier = Modifier.weight(1f)
                                        )
                                        OutlinedTextField(
                                            value = row.rateStr,
                                            onValueChange = { lines[idx] = row.copy(rateStr = it) },
                                            label = { Text("Upah/Satuan") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            singleLine = true,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = bonusStr,
                            onValueChange = { bonusStr = it },
                            label = { Text("Bonus (Rp)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = deductionStr,
                            onValueChange = { deductionStr = it },
                            label = { Text("Potongan (Rp)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Total Gaji:", style = MaterialTheme.typography.titleSmall)
                        CurrencyText(amount = total)
                    }

                    Text("Status Pembayaran Gaji", style = MaterialTheme.typography.labelMedium)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = payMode == "UNPAID",
                            onClick = { payMode = "UNPAID" },
                            label = { Text("Belum Dibayar") },
                            modifier = Modifier.testTag("create_status_unpaid")
                        )
                        FilterChip(
                            selected = payMode == "CICILAN",
                            onClick = { payMode = "CICILAN" },
                            label = { Text("Cicilan / DP") },
                            modifier = Modifier.testTag("create_status_cicilan")
                        )
                        FilterChip(
                            selected = payMode == "PAID",
                            onClick = { payMode = "PAID" },
                            label = { Text("Sudah Dibayar") },
                            modifier = Modifier.testTag("create_status_paid")
                        )
                    }

                    if (payMode == "CICILAN") {
                        OutlinedTextField(
                            value = initialCicilanStr,
                            onValueChange = { initialCicilanStr = it },
                            label = { Text("Nominal Cicilan / DP Awal (Rp)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("create_cicilan_amount_input")
                        )
                    }

                    if (payMode != "UNPAID") {
                        DateTimeSelectorSection(
                            title = "Tanggal & Waktu Pembayaran Gaji",
                            millis = paidAtMillis,
                            onMillisChange = { paidAtMillis = it },
                            tagPrefix = "create_paid"
                        )
                    }

                    if (payMode != "UNPAID" && cashBoxes.isNotEmpty()) {
                        ExposedDropdownMenuBox(expanded = boxMenu, onExpandedChange = { boxMenu = it }) {
                            OutlinedTextField(
                                value = selectedBox?.name ?: "Pilih Cash Box",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Bayar dari Cash Box") },
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
                }
                if (error != null) Text(error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val emp = selectedEmp
                    val validItems = lines.filter { it.qty > 0.0 }.map {
                        PayslipItem(
                            jobName = it.jobName,
                            unit = it.unit,
                            quantity = it.qty,
                            ratePerUnit = it.rate.coerceAtLeast(0.0),
                            subtotal = it.subtotal
                        )
                    }
                    if (emp == null || periodLabel.isBlank() || validItems.isEmpty()) {
                        error = "Pilih karyawan, isi periode, dan masukkan jumlah > 0 minimal untuk 1 pekerjaan"
                        return@Button
                    }
                    val computedTotal = (validItems.sumOf { it.subtotal } + bonus - deduction).coerceAtLeast(0.0)
                    val cicilanAmt = initialCicilanStr.replace(",", ".").toDoubleOrNull() ?: 0.0
                    if (payMode == "CICILAN" && cicilanAmt <= 0.0) {
                        error = "Masukkan nominal cicilan awal > 0"
                        return@Button
                    }
                    val isFullyPaid = payMode == "PAID" || (payMode == "CICILAN" && cicilanAmt >= computedTotal && computedTotal > 0.0)
                    val resolvedPaidAmt = when {
                        isFullyPaid -> computedTotal
                        payMode == "CICILAN" -> minOf(cicilanAmt, computedTotal)
                        else -> 0.0
                    }
                    onSave(
                        Payslip(
                            employeeId = emp.id,
                            employeeName = emp.name,
                            periodLabel = periodLabel.trim(),
                            items = validItems,
                            bonus = bonus.coerceAtLeast(0.0),
                            deduction = deduction.coerceAtLeast(0.0),
                            totalAmount = computedTotal,
                            paidAmount = resolvedPaidAmt,
                            paid = isFullyPaid,
                            notes = notes.trim(),
                            workDateMillis = workDateMillis,
                            salaryDateMillis = workDateMillis,
                            paidAtMillis = if (resolvedPaidAmt > 0.0) paidAtMillis else 0L
                        ),
                        if (resolvedPaidAmt > 0.0) selectedBox else null
                    )
                },
                modifier = Modifier.testTag("save_salary_transaction_button")
            ) {
                Text("Simpan ke Slip Gaji")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } }
    )
}
