package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddBox
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.model.CashBox
import com.example.data.model.Employee
import com.example.data.model.JobType
import com.example.data.model.Payslip
import com.example.data.model.StoreSettings
import com.example.data.model.UiState
import com.example.ui.components.ConfirmDeleteDialog
import com.example.ui.components.ResponsiveScreenContainer
import com.example.ui.theme.SlateBarText
import com.example.util.PayslipPrinter
import com.example.util.ThermalReceiptDocument
import com.example.util.toUnpaidThermalReceipt

@Composable
fun EmployeeSalaryScreen(
    employeesState: UiState<List<Employee>>,
    jobTypesState: UiState<List<JobType>>,
    payslipsState: UiState<List<Payslip>>,
    cashBoxesState: UiState<List<CashBox>>,
    storeSettingsState: UiState<StoreSettings>,
    onCreatePayslip: (Payslip, CashBox?) -> Unit,
    onTogglePayslipPaid: (Payslip, Boolean, CashBox?, Long) -> Unit,
    onPayInstallmentSingle: (Payslip, Double, CashBox?, String, Long) -> Unit = { _, _, _, _, _ -> },
    onPayInstallmentPool: (List<Payslip>, Double, CashBox?, String, Long) -> Unit = { _, _, _, _, _ -> },
    onPayAllPayslips: (List<Payslip>, CashBox?, Long) -> Unit,
    onDeletePayslip: (String) -> Unit,
    onNavigateToContactsPayslip: () -> Unit,
    onBack: () -> Unit
) {
    val employees = (employeesState as? UiState.Success)?.data ?: emptyList()
    val jobTypes = (jobTypesState as? UiState.Success)?.data ?: emptyList()
    val cashBoxes = (cashBoxesState as? UiState.Success)?.data ?: emptyList()
    val storeSettings = (storeSettingsState as? UiState.Success)?.data ?: StoreSettings()
    val context = LocalContext.current

    var showCreateDialog by remember { mutableStateOf(false) }
    var payPayslipTarget by remember { mutableStateOf<Payslip?>(null) }
    var previewPayslip by remember { mutableStateOf<Payslip?>(null) }
    var bluetoothPrintTarget by remember { mutableStateOf<Payslip?>(null) }
    var unpaidBluetoothTarget by remember { mutableStateOf<Pair<List<Payslip>, String>?>(null) }
    var paidFilteredBluetoothDoc by remember { mutableStateOf<ThermalReceiptDocument?>(null) }
    var deleteConfirm by remember { mutableStateOf<Payslip?>(null) }

    ResponsiveScreenContainer {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
        ) {
            SlateTopHeaderBar(
                title = "Data Gaji Karyawan",
                onBack = onBack,
                backTag = "employee_salary_back",
                trailingAction = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onNavigateToContactsPayslip,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("salary_to_contacts_payslip")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Badge,
                                contentDescription = "Buka Slip Gaji Kontak",
                                tint = SlateBarText
                            )
                        }
                        IconButton(
                            onClick = { showCreateDialog = true },
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("add_employee_salary_fab")
                        ) {
                            Icon(
                                imageVector = Icons.Default.AddBox,
                                contentDescription = "Catat Transaksi Gaji",
                                tint = SlateBarText,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }
                }
            )

            PayslipsTabContent(
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
                onDelete = { deleteConfirm = it }
            )
        }
    }

    if (showCreateDialog) {
        CreatePayslipDialog(
            employees = employees,
            jobTypes = jobTypes,
            cashBoxes = cashBoxes,
            dialogTitle = "Transaksi Gaji Karyawan",
            onSave = { slip, box ->
                onCreatePayslip(slip, box)
                showCreateDialog = false
            },
            onDismiss = { showCreateDialog = false }
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

    deleteConfirm?.let { slip ->
        ConfirmDeleteDialog(
            title = "Hapus Transaksi Gaji",
            message = "Hapus slip gaji ${slip.employeeName} (${slip.periodLabel})? Riwayat pembayaran gaji di menu Keuangan juga akan ikut terhapus dan saldo Cash Box disesuaikan.",
            onConfirm = {
                onDeletePayslip(slip.id)
                deleteConfirm = null
            },
            onDismiss = { deleteConfirm = null }
        )
    }
}
