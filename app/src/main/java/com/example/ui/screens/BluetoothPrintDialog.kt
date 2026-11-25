package com.example.ui.screens

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.automirrored.filled.BluetoothSearching
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SettingsBluetooth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.Payslip
import com.example.data.model.StoreSettings
import com.example.ui.theme.LossRed
import com.example.ui.theme.LossRedBg
import com.example.ui.theme.ProfitGreen
import com.example.ui.theme.ProfitGreenBg
import com.example.util.BluetoothThermalPrinter
import com.example.util.ThermalPrinterDevice
import com.example.util.ThermalReceiptDocument
import com.example.util.toThermalReceipt
import kotlinx.coroutines.launch

@Composable
fun BluetoothPrintDialog(
    payslip: Payslip,
    storeSettings: StoreSettings,
    onDismiss: () -> Unit
) {
    BluetoothPrintDialog(
        document = payslip.toThermalReceipt(),
        storeSettings = storeSettings,
        onDismiss = onDismiss
    )
}

@SuppressLint("MissingPermission")
@Composable
fun BluetoothPrintDialog(
    document: ThermalReceiptDocument,
    storeSettings: StoreSettings,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var hasPermission by remember { mutableStateOf(BluetoothThermalPrinter.hasConnectPermission(context)) }
    val pairedDevices = remember { mutableStateListOf<ThermalPrinterDevice>() }
    val discoveredDevices = remember { mutableStateListOf<ThermalPrinterDevice>() }
    var selectedAddress by remember { mutableStateOf(BluetoothThermalPrinter.getSavedPrinterAddress(context)) }
    var lineChars by remember { mutableIntStateOf(BluetoothThermalPrinter.getSavedLineChars(context)) }
    var isScanning by remember { mutableStateOf(false) }
    var isPrinting by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var isError by remember { mutableStateOf(false) }

    val adapter = remember(context) { BluetoothThermalPrinter.getAdapter(context) }
    var isBluetoothEnabled by remember(adapter) { mutableStateOf(adapter?.isEnabled == true) }

    fun reloadPaired() {
        hasPermission = BluetoothThermalPrinter.hasConnectPermission(context)
        isBluetoothEnabled = adapter?.isEnabled == true
        pairedDevices.clear()
        if (hasPermission && isBluetoothEnabled) {
            val list = BluetoothThermalPrinter.getPairedPrinters(context)
            pairedDevices.addAll(list)
            if (selectedAddress == null || list.none { it.address == selectedAddress }) {
                selectedAddress = list.firstOrNull()?.address ?: selectedAddress
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        hasPermission = grants.values.all { it } || BluetoothThermalPrinter.hasConnectPermission(context)
        if (hasPermission) {
            reloadPaired()
            isError = false
            statusMessage = null
        } else {
            isError = true
            statusMessage = "Izin Bluetooth diperlukan untuk terhubung ke printer thermal."
        }
    }

    LaunchedEffect(Unit) {
        if (hasPermission) {
            reloadPaired()
        } else {
            val perms = BluetoothThermalPrinter.requiredPermissions()
            if (perms.isNotEmpty()) {
                permissionLauncher.launch(perms)
            }
        }
    }

    DisposableEffect(context, hasPermission) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                when (intent?.action) {
                    BluetoothAdapter.ACTION_STATE_CHANGED -> {
                        val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
                        isBluetoothEnabled = state == BluetoothAdapter.STATE_ON || adapter?.isEnabled == true
                        if (isBluetoothEnabled && hasPermission) {
                            reloadPaired()
                        } else if (!isBluetoothEnabled) {
                            isScanning = false
                        }
                    }
                    BluetoothDevice.ACTION_FOUND -> {
                        if (!BluetoothThermalPrinter.hasConnectPermission(context)) return
                        val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                        } else {
                            @Suppress("DEPRECATION")
                            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                        }
                        if (device != null && !device.address.isNullOrBlank()) {
                            val item = ThermalPrinterDevice(
                                name = device.name?.takeIf { it.isNotBlank() } ?: "Printer Bluetooth",
                                address = device.address
                            )
                            if (pairedDevices.none { it.address == item.address } &&
                                discoveredDevices.none { it.address == item.address }
                            ) {
                                discoveredDevices.add(item)
                            }
                        }
                    }
                    BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                        isScanning = false
                    }
                }
            }
        }
        val filter = IntentFilter().apply {
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
            addAction(BluetoothDevice.ACTION_FOUND)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        }
        androidx.core.content.ContextCompat.registerReceiver(
            context,
            receiver,
            filter,
            androidx.core.content.ContextCompat.RECEIVER_EXPORTED
        )
        onDispose {
            runCatching {
                if (BluetoothThermalPrinter.hasDiscoveryPermission(context)) {
                    adapter?.cancelDiscovery()
                }
            }
            runCatching { context.unregisterReceiver(receiver) }
        }
    }

    val allDevices = (pairedDevices + discoveredDevices).distinctBy { it.address }

    AlertDialog(
        onDismissRequest = { if (!isPrinting) onDismiss() },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Bluetooth, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text("Cetak Printer Bluetooth")
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 460.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = document.summaryLabel,
                    style = MaterialTheme.typography.titleSmall
                )

                Text("Ukuran Kertas", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = lineChars == 32,
                        onClick = { lineChars = 32 },
                        label = { Text("58 mm (32 col)") },
                        modifier = Modifier.testTag("thermal_width_58")
                    )
                    FilterChip(
                        selected = lineChars == 48,
                        onClick = { lineChars = 48 },
                        label = { Text("80 mm (48 col)") },
                        modifier = Modifier.testTag("thermal_width_80")
                    )
                }

                HorizontalDivider()

                if (adapter == null) {
                    Surface(color = LossRedBg, shape = MaterialTheme.shapes.small, modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Perangkat ini tidak memiliki modul Bluetooth.",
                            color = LossRed,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                } else if (!hasPermission) {
                    Text(
                        text = "Berikan izin Bluetooth untuk melihat dan mencetak ke printer.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Button(
                        onClick = {
                            val perms = BluetoothThermalPrinter.requiredPermissions()
                            if (perms.isNotEmpty()) permissionLauncher.launch(perms)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Izinkan Akses Bluetooth")
                    }
                } else if (!isBluetoothEnabled) {
                    Surface(color = LossRedBg, shape = MaterialTheme.shapes.small, modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Bluetooth sedang nonaktif. Aktifkan Bluetooth terlebih dahulu.",
                            color = LossRed,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                    OutlinedButton(
                        onClick = {
                            context.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.SettingsBluetooth, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Buka Pengaturan Bluetooth")
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { reloadPaired() },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Muat Ulang", maxLines = 1)
                        }
                        OutlinedButton(
                            onClick = {
                                if (!BluetoothThermalPrinter.hasDiscoveryPermission(context)) {
                                    val perms = BluetoothThermalPrinter.requiredPermissions()
                                    if (perms.isNotEmpty()) permissionLauncher.launch(perms)
                                    return@OutlinedButton
                                }
                                discoveredDevices.clear()
                                isError = false
                                statusMessage = null
                                isScanning = true
                                val started = runCatching {
                                    adapter.cancelDiscovery()
                                    adapter.startDiscovery()
                                }.getOrDefault(false)
                                if (!started) {
                                    isScanning = false
                                    isError = true
                                    statusMessage = "Gagal memulai pencarian printer Bluetooth. Pastikan Bluetooth & Lokasi aktif."
                                }
                            },
                            enabled = !isScanning,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("scan_bluetooth_printer_button")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.BluetoothSearching, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(if (isScanning) "Mencari..." else "Scan Printer", maxLines = 1)
                        }
                    }

                    if (allDevices.isEmpty()) {
                        Text(
                            text = "Belum ada printer Bluetooth yang terpasang (paired). Pasangkan printer di Pengaturan Bluetooth lalu ketuk Muat Ulang.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        TextButton(
                            onClick = {
                                context.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
                            }
                        ) {
                            Text("Buka Pengaturan Bluetooth")
                        }
                    } else {
                        Text("Pilih Printer Bluetooth", style = MaterialTheme.typography.labelMedium)
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            allDevices.forEach { device ->
                                val isSelected = selectedAddress == device.address
                                Card(
                                    onClick = {
                                        selectedAddress = device.address
                                        BluetoothThermalPrinter.savePrinterSelection(context, device.name, device.address)
                                    },
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSelected) {
                                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                                        } else {
                                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                        }
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 10.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        RadioButton(
                                            selected = isSelected,
                                            onClick = {
                                                selectedAddress = device.address
                                                BluetoothThermalPrinter.savePrinterSelection(context, device.name, device.address)
                                            }
                                        )
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = device.name,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Medium
                                            )
                                            Text(
                                                text = device.address,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        OutlinedButton(
                            onClick = {
                                val addr = selectedAddress ?: return@OutlinedButton
                                isPrinting = true
                                isError = false
                                statusMessage = null
                                scope.launch {
                                    val res = BluetoothThermalPrinter.printDocument(
                                        context = context,
                                        deviceAddress = addr,
                                        document = BluetoothThermalPrinter.createTestReceiptDocument(),
                                        storeSettings = storeSettings,
                                        lineChars = lineChars
                                    )
                                    isPrinting = false
                                    res.onSuccess {
                                        isError = false
                                        statusMessage = "Test cetak berhasil dikirim ke printer Bluetooth."
                                    }.onFailure { err ->
                                        isError = true
                                        statusMessage = err.message ?: "Gagal test cetak ke printer."
                                    }
                                }
                            },
                            enabled = !isPrinting && !selectedAddress.isNullOrBlank(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("test_bluetooth_print_button")
                        ) {
                            Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Test Cetak")
                        }
                    }
                }

                if (statusMessage != null) {
                    Surface(
                        color = if (isError) LossRedBg else ProfitGreenBg,
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (!isError) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = ProfitGreen, modifier = Modifier.size(18.dp))
                            }
                            Text(
                                text = statusMessage!!,
                                color = if (isError) LossRed else ProfitGreen,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val addr = selectedAddress
                    if (addr.isNullOrBlank()) {
                        isError = true
                        statusMessage = "Pilih printer Bluetooth terlebih dahulu."
                        return@Button
                    }
                    isPrinting = true
                    isError = false
                    statusMessage = null
                    scope.launch {
                        val res = BluetoothThermalPrinter.printDocument(
                            context = context,
                            deviceAddress = addr,
                            document = document,
                            storeSettings = storeSettings,
                            lineChars = lineChars
                        )
                        isPrinting = false
                        res.onSuccess {
                            isError = false
                            statusMessage = "${document.headerTitle.lowercase().replaceFirstChar { it.uppercase() }} berhasil dicetak ke printer Bluetooth."
                        }.onFailure { err ->
                            isError = true
                            statusMessage = err.message ?: "Gagal mencetak ke printer."
                        }
                    }
                },
                enabled = !isPrinting && hasPermission && isBluetoothEnabled && !selectedAddress.isNullOrBlank(),
                modifier = Modifier.testTag("confirm_bluetooth_print_button")
            ) {
                if (isPrinting) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                    Text("Mencetak...")
                } else {
                    Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Cetak")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = { if (!isPrinting) onDismiss() }) {
                Text("Tutup")
            }
        }
    )
}
