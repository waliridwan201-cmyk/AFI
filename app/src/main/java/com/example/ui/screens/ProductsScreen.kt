package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.Product
import com.example.data.model.UiState
import com.example.ui.components.ConfirmDeleteDialog
import com.example.ui.components.CurrencyText
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.LoadingCenter
import com.example.ui.components.ResponsiveScreenContainer
import com.example.ui.components.SearchBarField
import com.example.ui.components.StatusBadge
import com.example.ui.theme.LossRed
import com.example.ui.theme.LossRedBg
import com.example.ui.theme.ProfitGreen
import com.example.ui.theme.ProfitGreenBg
import com.example.util.formatQty
import com.example.util.formatRupiah

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductsScreen(
    productsState: UiState<List<Product>>,
    onSaveProduct: (Product) -> Unit,
    onDeleteProduct: (String) -> Unit,
    onBack: () -> Unit
) {
    var search by remember { mutableStateOf("") }
    var editingProduct by remember { mutableStateOf<Product?>(null) }
    var showFormDialog by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<Product?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Data Produk") },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("products_back")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingProduct = null
                    showFormDialog = true
                },
                modifier = Modifier.testTag("add_product_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Tambah Produk")
            }
        }
    ) { padding ->
        ResponsiveScreenContainer(modifier = Modifier.padding(padding)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                SearchBarField(
                    query = search,
                    onQueryChange = { search = it },
                    placeholder = "Cari nama produk, SKU, atau kategori...",
                    tag = "product_search_field"
                )
                Spacer(modifier = Modifier.height(12.dp))

                when (productsState) {
                    is UiState.Loading -> LoadingCenter()
                    is UiState.Error -> EmptyStateCard("Gagal memuat produk", productsState.message)
                    is UiState.Success -> {
                        val filtered = productsState.data.filter {
                            search.isBlank() ||
                                it.name.contains(search, ignoreCase = true) ||
                                it.sku.contains(search, ignoreCase = true) ||
                                it.category.contains(search, ignoreCase = true)
                        }
                        if (filtered.isEmpty()) {
                            EmptyStateCard(
                                title = "Belum ada produk",
                                subtitle = "Tekan tombol + untuk menambahkan produk beserta harga beli, harga jual, dan stok.",
                                icon = Icons.Default.Inventory2
                            )
                        } else {
                            LazyColumn(
                                contentPadding = PaddingValues(bottom = 88.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(filtered, key = { it.id }) { product ->
                                    ProductCard(
                                        product = product,
                                        onEdit = {
                                            editingProduct = product
                                            showFormDialog = true
                                        },
                                        onDelete = { deleteTarget = product }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showFormDialog) {
        ProductFormDialog(
            initial = editingProduct,
            onSave = {
                onSaveProduct(it)
                showFormDialog = false
            },
            onDismiss = { showFormDialog = false }
        )
    }

    deleteTarget?.let { target ->
        ConfirmDeleteDialog(
            title = "Hapus Produk?",
            message = "Produk \"${target.name}\" akan dihapus dari daftar master.",
            onConfirm = {
                onDeleteProduct(target.id)
                deleteTarget = null
            },
            onDismiss = { deleteTarget = null }
        )
    }
}

@Composable
private fun ProductCard(
    product: Product,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val isLowStock = product.stock <= product.minStock
    val margin = product.sellPrice - product.buyPrice
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = MaterialTheme.shapes.medium,
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(product.name, style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = listOfNotNull(
                            product.category.takeIf { it.isNotBlank() },
                            product.sku.takeIf { it.isNotBlank() }?.let { "SKU: $it" }
                        ).joinToString(" • "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row {
                    IconButton(onClick = onEdit, modifier = Modifier.testTag("edit_product_${product.id}")) {
                        Icon(Icons.Default.Edit, contentDescription = "Ubah produk")
                    }
                    IconButton(onClick = onDelete) {
                        Icon(
                            Icons.Default.DeleteOutline,
                            contentDescription = "Hapus produk",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Modal: ${formatRupiah(product.buyPrice)}", style = MaterialTheme.typography.bodySmall)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Jual:", style = MaterialTheme.typography.bodySmall)
                        CurrencyText(
                            amount = product.sellPrice,
                            color = if (margin >= 0) ProfitGreen else LossRed,
                            style = MaterialTheme.typography.titleSmall
                        )
                    }
                }
                StatusBadge(
                    text = "Stok: ${formatQty(product.stock)} ${product.unit}",
                    containerColor = if (isLowStock) LossRedBg else ProfitGreenBg,
                    contentColor = if (isLowStock) LossRed else ProfitGreen
                )
            }
        }
    }
}

@Composable
private fun ProductFormDialog(
    initial: Product?,
    onSave: (Product) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var sku by remember { mutableStateOf(initial?.sku ?: "") }
    var category by remember { mutableStateOf(initial?.category ?: "Umum") }
    var unit by remember { mutableStateOf(initial?.unit ?: "Pak") }
    var buyPriceStr by remember { mutableStateOf(initial?.buyPrice?.toLong()?.toString() ?: "") }
    var sellPriceStr by remember { mutableStateOf(initial?.sellPrice?.toLong()?.toString() ?: "") }
    var stockStr by remember { mutableStateOf(initial?.stock?.let { formatQty(it) } ?: "0") }
    var minStockStr by remember { mutableStateOf(initial?.minStock?.let { formatQty(it) } ?: "5") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Tambah Produk" else "Edit Produk") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nama Produk *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = sku,
                        onValueChange = { sku = it },
                        label = { Text("Kode/SKU") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = unit,
                        onValueChange = { unit = it },
                        label = { Text("Satuan *") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Kategori") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = buyPriceStr,
                        onValueChange = { buyPriceStr = it },
                        label = { Text("Harga Beli (Rp)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = sellPriceStr,
                        onValueChange = { sellPriceStr = it },
                        label = { Text("Harga Jual (Rp)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = stockStr,
                        onValueChange = { stockStr = it },
                        label = { Text("Stok Awal") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = minStockStr,
                        onValueChange = { minStockStr = it },
                        label = { Text("Stok Min") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                if (error != null) {
                    Text(error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val buy = buyPriceStr.replace(",", ".").toDoubleOrNull() ?: 0.0
                    val sell = sellPriceStr.replace(",", ".").toDoubleOrNull() ?: 0.0
                    val stock = stockStr.replace(",", ".").toDoubleOrNull() ?: 0.0
                    val minStock = minStockStr.replace(",", ".").toDoubleOrNull() ?: 5.0
                    if (name.isBlank() || unit.isBlank()) {
                        error = "Nama produk dan satuan wajib diisi"
                        return@Button
                    }
                    onSave(
                        (initial ?: Product()).copy(
                            name = name.trim(),
                            sku = sku.trim(),
                            category = category.trim(),
                            unit = unit.trim(),
                            buyPrice = buy,
                            sellPrice = sell,
                            stock = stock,
                            minStock = minStock
                        )
                    )
                },
                modifier = Modifier.testTag("save_product_button")
            ) {
                Text("Simpan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        }
    )
}
