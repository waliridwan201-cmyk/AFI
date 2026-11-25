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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.BluetoothSearching
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.SettingsBluetooth
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.data.model.LocalUser
import com.example.data.model.StoreSettings
import com.example.data.model.UiState
import com.example.ui.components.ResponsiveScreenContainer
import com.example.ui.components.StatusBadge
import com.example.ui.theme.LossRed
import com.example.ui.theme.LossRedBg
import com.example.ui.theme.ProfitGreen
import com.example.ui.theme.ProfitGreenBg
import com.example.util.BluetoothThermalPrinter
import com.example.util.ThermalPrinterDevice
import kotlinx.coroutines.launch

@SuppressLint("MissingPermission")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    activeUser: LocalUser?,
    storeSettingsState: UiState<StoreSettings>,
    onSaveSettings: (storeName: String, ownerName: String, phone: String, address: String, receiptFooter: String) -> Unit,
    onResetTransactions: (includePayslips: Boolean) -> Unit,
    onSignOut: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val currentSettings = (storeSettingsState as? UiState.Success)?.data ?: StoreSettings()
    var storeName by remember { mutableStateOf(currentSettings.storeName) }
    var ownerName by remember { mutableStateOf(currentSettings.ownerName) }
    var phone by remember { mutableStateOf(currentSettings.phone) }
    var address by remember { mutableStateOf(currentSettings.address) }
    var receiptFooter by remember { mutableStateOf(currentSettings.receiptFooter) }

    LaunchedEffect(currentSettings) {
        if (currentSettings.id.isNotBlank()) {
            storeName = currentSettings.storeName
            ownerName = currentSettings.ownerName
            phone = currentSettings.phone
            address = currentSettings.address
            receiptFooter = currentSettings.receiptFooter
        }
    }

    // Pengaturan Printer Thermal Bluetooth state
    var hasBtPermission by remember { mutableStateOf(BluetoothThermalPrinter.hasConnectPermission(context)) }
    val adapter = remember(context) { BluetoothThermalPrinter.getAdapter(context) }
    var isBtEnabled by remember(adapter) { mutableStateOf(adapter?.isEnabled == true) }
    val pairedPrinters = remember { mutableStateListOf<ThermalPrinterDevice>() }
    val discoveredPrinters = remember { mutableStateListOf<ThermalPrinterDevice>() }
    var savedPrinterAddress by remember { mutableStateOf(BluetoothThermalPrinter.getSavedPrinterAddress(context)) }
    var savedPrinterName by remember { mutableStateOf(BluetoothThermalPrinter.getSavedPrinterName(context)) }
    var lineChars by remember { mutableIntStateOf(BluetoothThermalPrinter.getSavedLineChars(context)) }
    var isScanningPrinter by remember { mutableStateOf(false) }
    var isTestingPrint by remember { mutableStateOf(false) }
    var printerStatusMessage by remember { mutableStateOf<String?>(null) }
    var printerStatusIsError by remember { mutableStateOf(false) }

    // Reset Data Transaksi dialog state
    var showResetDialog by remember { mutableStateOf(false) }

    fun reloadPairedPrinters() {
        hasBtPermission = BluetoothThermalPrinter.hasConnectPermission(context)
        isBtEnabled = adapter?.isEnabled == true
        pairedPrinters.clear()
        if (hasBtPermission && isBtEnabled) {
            val list = BluetoothThermalPrinter.getPairedPrinters(context)
            pairedPrinters.addAll(list)
            if (!savedPrinterAddress.isNullOrBlank() && savedPrinterName.isNullOrBlank()) {
                savedPrinterName = list.firstOrNull { it.address == savedPrinterAddress }?.name
            }
        }
    }

    val btPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        hasBtPermission = grants.values.all { it } || BluetoothThermalPrinter.hasConnectPermission(context)
        if (hasBtPermission) {
            reloadPairedPrinters()
            printerStatusIsError = false
            printerStatusMessage = null
        } else {
            printerStatusIsError = true
            printerStatusMessage = "Izin Bluetooth diperlukan untuk mengelola printer thermal."
        }
    }

    LaunchedEffect(Unit) {
        if (hasBtPermission) {
            reloadPairedPrinters()
        }
    }

    DisposableEffect(context, hasBtPermission) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                when (intent?.action) {
                    BluetoothAdapter.ACTION_STATE_CHANGED -> {
                        val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
                        isBtEnabled = state == BluetoothAdapter.STATE_ON || adapter?.isEnabled == true
                        if (isBtEnabled && hasBtPermission) {
                            reloadPairedPrinters()
                        } else if (!isBtEnabled) {
                            isScanningPrinter = false
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
                            if (pairedPrinters.none { it.address == item.address } &&
                                discoveredPrinters.none { it.address == item.address }
                            ) {
                                discoveredPrinters.add(item)
                            }
                        }
                    }
                    BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                        isScanningPrinter = false
                    }
                }
            }
        }
        val filter = IntentFilter().apply {
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
            addAction(BluetoothDevice.ACTION_FOUND)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        }
        ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_EXPORTED)
        onDispose {
            runCatching {
                if (BluetoothThermalPrinter.hasDiscoveryPermission(context)) {
                    adapter?.cancelDiscovery()
                }
            }
            runCatching { context.unregisterReceiver(receiver) }
        }
    }

    val allPrinters = (pairedPrinters + discoveredPrinters).distinctBy { it.address }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pengaturan Toko") },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("settings_back")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
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
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. Identitas Toko & Pabrik
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.Store, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text("Identitas Toko & Pabrik", style = MaterialTheme.typography.titleMedium)
                        }
                        OutlinedTextField(
                            value = storeName,
                            onValueChange = { storeName = it },
                            label = { Text("Nama Toko / Pabrik *") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = ownerName,
                            onValueChange = { ownerName = it },
                            label = { Text("Nama Pemilik / Penanggung Jawab") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = phone,
                            onValueChange = { phone = it },
                            label = { Text("No. Telepon / WhatsApp") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = address,
                            onValueChange = { address = it },
                            label = { Text("Alamat Lengkap") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = receiptFooter,
                            onValueChange = { receiptFooter = it },
                            label = { Text("Catatan Footer Struk & Slip Gaji") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Button(
                            onClick = {
                                if (storeName.isNotBlank()) {
                                    onSaveSettings(storeName, ownerName, phone, address, receiptFooter)
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("save_store_settings_button")
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Simpan Pengaturan Toko")
                        }
                    }
                }

                // 2. Pengaturan Printer Thermal Bluetooth
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("settings_bluetooth_printer_card")
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.Bluetooth, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Text("Printer Thermal Bluetooth", style = MaterialTheme.typography.titleMedium)
                            }
                            StatusBadge(
                                text = if (!savedPrinterAddress.isNullOrBlank()) "Tersimpan" else "Belum Dipilih",
                                containerColor = if (!savedPrinterAddress.isNullOrBlank()) ProfitGreenBg else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (!savedPrinterAddress.isNullOrBlank()) ProfitGreen else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            shape = MaterialTheme.shapes.small,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = savedPrinterName?.takeIf { it.isNotBlank() }
                                            ?: if (!savedPrinterAddress.isNullOrBlank()) "Printer Thermal Bluetooth" else "Belum ada printer aktif",
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                    Text(
                                        text = savedPrinterAddress ?: "Pilih printer di bawah agar langsung siap saat mencetak struk.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (!savedPrinterAddress.isNullOrBlank()) {
                                    TextButton(
                                        onClick = {
                                            BluetoothThermalPrinter.clearSavedPrinter(context)
                                            savedPrinterAddress = null
                                            savedPrinterName = null
                                            printerStatusIsError = false
                                            printerStatusMessage = "Pilihan printer default dihapus."
                                        }
                                    ) {
                                        Text("Hapus")
                                    }
                                }
                            }
                        }

                        Text("Lebar Kertas Struk Thermal", style = MaterialTheme.typography.labelMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = lineChars == 32,
                                onClick = {
                                    lineChars = 32
                                    BluetoothThermalPrinter.saveLineChars(context, 32)
                                    printerStatusIsError = false
                                    printerStatusMessage = "Lebar kertas disimpan: 58 mm (32 karakter)."
                                },
                                label = { Text("58 mm (32 col)") },
                                modifier = Modifier.testTag("settings_printer_58mm")
                            )
                            FilterChip(
                                selected = lineChars == 48,
                                onClick = {
                                    lineChars = 48
                                    BluetoothThermalPrinter.saveLineChars(context, 48)
                                    printerStatusIsError = false
                                    printerStatusMessage = "Lebar kertas disimpan: 80 mm (48 karakter)."
                                },
                                label = { Text("80 mm (48 col)") },
                                modifier = Modifier.testTag("settings_printer_80mm")
                            )
                        }

                        HorizontalDivider()

                        if (adapter == null) {
                            Text(
                                "Perangkat tidak memiliki adaptor Bluetooth.",
                                style = MaterialTheme.typography.bodySmall,
                                color = LossRed
                            )
                        } else if (!hasBtPermission) {
                            Button(
                                onClick = {
                                    val perms = BluetoothThermalPrinter.requiredPermissions()
                                    if (perms.isNotEmpty()) btPermissionLauncher.launch(perms)
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Izinkan Akses Bluetooth")
                            }
                        } else if (!isBtEnabled) {
                            Text(
                                "Bluetooth belum aktif. Aktifkan untuk memilih & menguji printer.",
                                style = MaterialTheme.typography.bodySmall,
                                color = LossRed
                            )
                            OutlinedButton(
                                onClick = { context.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS)) },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.SettingsBluetooth, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Buka Pengaturan Bluetooth HP")
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { reloadPairedPrinters() },
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
                                            if (perms.isNotEmpty()) btPermissionLauncher.launch(perms)
                                            return@OutlinedButton
                                        }
                                        discoveredPrinters.clear()
                                        printerStatusIsError = false
                                        printerStatusMessage = null
                                        isScanningPrinter = true
                                        val started = runCatching {
                                            adapter.cancelDiscovery()
                                            adapter.startDiscovery()
                                        }.getOrDefault(false)
                                        if (!started) {
                                            isScanningPrinter = false
                                            printerStatusIsError = true
                                            printerStatusMessage = "Gagal mencari perangkat Bluetooth. Pastikan Bluetooth & Lokasi aktif."
                                        }
                                    },
                                    enabled = !isScanningPrinter,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("settings_scan_printer_button")
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.BluetoothSearching, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text(if (isScanningPrinter) "Mencari..." else "Scan Printer", maxLines = 1)
                                }
                            }

                            if (allPrinters.isEmpty()) {
                                Text(
                                    "Belum ada printer Bluetooth terpasang (paired). Pasangkan printer di Pengaturan Bluetooth HP lalu ketuk Muat Ulang.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                TextButton(onClick = { context.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS)) }) {
                                    Text("Buka Pengaturan Bluetooth HP")
                                }
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    allPrinters.forEach { device ->
                                        val selected = savedPrinterAddress == device.address
                                        Card(
                                            onClick = {
                                                savedPrinterAddress = device.address
                                                savedPrinterName = device.name
                                                BluetoothThermalPrinter.savePrinterSelection(context, device.name, device.address)
                                                printerStatusIsError = false
                                                printerStatusMessage = "Printer default dipilih: ${device.name}"
                                            },
                                            colors = CardDefaults.cardColors(
                                                containerColor = if (selected) {
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
                                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                RadioButton(
                                                    selected = selected,
                                                    onClick = {
                                                        savedPrinterAddress = device.address
                                                        savedPrinterName = device.name
                                                        BluetoothThermalPrinter.savePrinterSelection(context, device.name, device.address)
                                                        printerStatusIsError = false
                                                        printerStatusMessage = "Printer default dipilih: ${device.name}"
                                                    }
                                                )
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(device.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                                    Text(device.address, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            Button(
                                onClick = {
                                    val addr = savedPrinterAddress ?: return@Button
                                    isTestingPrint = true
                                    printerStatusIsError = false
                                    printerStatusMessage = null
                                    scope.launch {
                                        val res = BluetoothThermalPrinter.printDocument(
                                            context = context,
                                            deviceAddress = addr,
                                            document = BluetoothThermalPrinter.createTestReceiptDocument(),
                                            storeSettings = currentSettings,
                                            lineChars = lineChars
                                        )
                                        isTestingPrint = false
                                        res.onSuccess {
                                            printerStatusIsError = false
                                            printerStatusMessage = "Test cetak berhasil dikirim ke printer."
                                        }.onFailure { err ->
                                            printerStatusIsError = true
                                            printerStatusMessage = err.message ?: "Gagal mengirim test cetak."
                                        }
                                    }
                                },
                                enabled = !isTestingPrint && !savedPrinterAddress.isNullOrBlank(),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("settings_test_print_button")
                            ) {
                                if (isTestingPrint) {
                                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Menguji Printer...")
                                } else {
                                    Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Test Cetak Printer Thermal")
                                }
                            }
                        }

                        if (printerStatusMessage != null) {
                            Surface(
                                color = if (printerStatusIsError) LossRedBg else ProfitGreenBg,
                                shape = MaterialTheme.shapes.small,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    if (!printerStatusIsError) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = ProfitGreen, modifier = Modifier.size(16.dp))
                                    }
                                    Text(
                                        text = printerStatusMessage!!,
                                        color = if (printerStatusIsError) LossRed else ProfitGreen,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                    }
                }

                // 3. Reset Data Transaksi (Dipindahkan dari Menu Keuangan)
                Card(
                    colors = CardDefaults.cardColors(containerColor = LossRedBg.copy(alpha = 0.65f)),
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.WarningAmber, contentDescription = null, tint = LossRed)
                            Text(
                                "Reset Data Transaksi",
                                style = MaterialTheme.typography.titleMedium,
                                color = LossRed,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Text(
                            "Gunakan jika terjadi kesalahan pencatatan massal. Menghapus riwayat penjualan, pembelian, retur, pengeluaran, hutang/piutang, arus uang & mengembalikan saldo Cash Box ke Rp 0. Data master (produk, kontak, karyawan) tetap aman.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        OutlinedButton(
                            onClick = { showResetDialog = true },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = LossRed),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("reset_transactions_button")
                        ) {
                            Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Reset Data Transaksi")
                        }
                    }
                }

                // 4. Akun Lokal Aktif
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Akun Lokal Aktif", style = MaterialTheme.typography.titleMedium)
                        Text(
                            activeUser?.displayName?.ifBlank { "Pengguna AFI" } ?: "Pengguna AFI",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            "Username: ${activeUser?.username ?: "-"} • Database Lokal",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedButton(
                            onClick = onSignOut,
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("sign_out_button")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Keluar / Ganti Akun")
                        }
                    }
                }
            }
        }
    }

    if (showResetDialog) {
        var includePayslips by remember { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Konfirmasi Reset Data Transaksi") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Tindakan ini akan menghapus semua catatan penjualan, pembelian, retur pembelian, pengeluaran, hutang/piutang, riwayat arus uang, dan mereset saldo semua Cash Box ke Rp 0.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        "Data produk, pedagang, supplier, karyawan, jenis pekerjaan & pengaturan toko tidak dihapus.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Checkbox(
                            checked = includePayslips,
                            onCheckedChange = { includePayslips = it },
                            modifier = Modifier.testTag("reset_include_payslips_checkbox")
                        )
                        Text("Sertakan data Slip Gaji Karyawan", style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onResetTransactions(includePayslips)
                        showResetDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = LossRed),
                    modifier = Modifier.testTag("confirm_reset_transactions_button")
                ) {
                    Text("Ya, Reset Sekarang")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) { Text("Batal") }
            }
        )
    }
}
