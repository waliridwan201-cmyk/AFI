package com.example.data.model

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

data class LocalUser(
    val id: String = "",
    val username: String = "",
    val displayName: String = "",
    val createdAtMillis: Long = 0L
)

data class StoreSettings(
    val id: String = "default",
    val userId: String = "",
    val storeName: String = "Toko & Pabrik AFI",
    val ownerName: String = "",
    val phone: String = "",
    val address: String = "",
    val receiptFooter: String = "Terima kasih atas kerja sama Anda.",
    val updatedAtMillis: Long = 0L
)

data class Product(
    val id: String = "",
    val userId: String = "",
    val name: String = "",
    val sku: String = "",
    val category: String = "Umum",
    val unit: String = "Pcs",
    val buyPrice: Double = 0.0,
    val sellPrice: Double = 0.0,
    val stock: Double = 0.0,
    val minStock: Double = 5.0,
    val updatedAtMillis: Long = 0L
)

data class Merchant(
    val id: String = "",
    val userId: String = "",
    val name: String = "",
    val phone: String = "",
    val routeArea: String = "",
    val address: String = "",
    val notes: String = ""
)

data class Supplier(
    val id: String = "",
    val userId: String = "",
    val name: String = "",
    val phone: String = "",
    val suppliedItems: String = "",
    val address: String = "",
    val notes: String = ""
)

data class Employee(
    val id: String = "",
    val userId: String = "",
    val name: String = "",
    val phone: String = "",
    val defaultJobTypeId: String = "",
    val defaultJobName: String = "",
    val active: Boolean = true
) {
    fun assignedJobIds(): List<String> =
        defaultJobTypeId.split(",").map { it.trim() }.filter { it.isNotBlank() }

    fun assignedJobNames(): List<String> =
        defaultJobName.split(",").map { it.trim() }.filter { it.isNotBlank() }
}

data class JobType(
    val id: String = "",
    val userId: String = "",
    val code: String = "",
    val name: String = "",
    val unit: String = "Satuan",
    val ratePerUnit: Double = 0.0,
    val sortOrder: Long = 0L
)

data class PayslipItem(
    val jobName: String = "",
    val unit: String = "",
    val quantity: Double = 0.0,
    val ratePerUnit: Double = 0.0,
    val subtotal: Double = 0.0
)

data class Payslip(
    val id: String = "",
    val userId: String = "",
    val employeeId: String = "",
    val employeeName: String = "",
    val periodLabel: String = "",
    val items: List<PayslipItem> = emptyList(),
    val bonus: Double = 0.0,
    val deduction: Double = 0.0,
    val totalAmount: Double = 0.0,
    val paidAmount: Double = 0.0,
    val paid: Boolean = false,
    val cashBoxId: String = "",
    val notes: String = "",
    val workDateMillis: Long = System.currentTimeMillis(),
    val salaryDateMillis: Long = System.currentTimeMillis(),
    val paidAtMillis: Long = 0L,
    val createdAtMillis: Long = System.currentTimeMillis()
) {
    fun effectivePaidAmount(): Double =
        if (paid) totalAmount.coerceAtLeast(paidAmount) else paidAmount.coerceIn(0.0, totalAmount)

    fun remainingAmount(): Double =
        if (paid) 0.0 else kotlin.math.max(0.0, totalAmount - effectivePaidAmount())
}

data class CashBox(
    val id: String = "",
    val userId: String = "",
    val name: String = "",
    val description: String = "",
    val balance: Double = 0.0,
    val primaryBox: Boolean = false
)

data class CashFlow(
    val id: String = "",
    val userId: String = "",
    val flowType: String = "IN", // "IN" or "OUT"
    val category: String = "",
    val amount: Double = 0.0,
    val cashBoxId: String = "",
    val cashBoxName: String = "",
    val description: String = "",
    val referenceId: String = "",
    val transactionDateMillis: Long = 0L
)

data class SaleItem(
    val productId: String = "",
    val productName: String = "",
    val unit: String = "",
    val quantity: Double = 0.0,
    val buyPrice: Double = 0.0,
    val sellPrice: Double = 0.0,
    val subtotal: Double = 0.0,
    val profit: Double = 0.0
)

data class Sale(
    val id: String = "",
    val userId: String = "",
    val invoiceNo: String = "",
    val customerName: String = "",
    val merchantId: String = "",
    val items: List<SaleItem> = emptyList(),
    val totalAmount: Double = 0.0,
    val paidAmount: Double = 0.0,
    val remainingDebt: Double = 0.0,
    val totalCost: Double = 0.0,
    val totalProfit: Double = 0.0,
    val lossAmount: Double = 0.0,
    val paymentStatus: String = "LUNAS", // "LUNAS" or "PIUTANG"
    val cashBoxId: String = "",
    val notes: String = "",
    val transactionDateMillis: Long = 0L
)

data class PurchaseItem(
    val productId: String = "",
    val productName: String = "",
    val unit: String = "",
    val quantity: Double = 0.0,
    val buyPrice: Double = 0.0,
    val subtotal: Double = 0.0
)

data class Purchase(
    val id: String = "",
    val userId: String = "",
    val invoiceNo: String = "",
    val supplierName: String = "",
    val items: List<PurchaseItem> = emptyList(),
    val totalAmount: Double = 0.0,
    val paidAmount: Double = 0.0,
    val remainingDebt: Double = 0.0,
    val paymentStatus: String = "LUNAS", // "LUNAS" or "HUTANG"
    val cashBoxId: String = "",
    val notes: String = "",
    val transactionDateMillis: Long = 0L
)

data class PurchaseReturn(
    val id: String = "",
    val userId: String = "",
    val purchaseId: String = "",
    val supplierName: String = "",
    val productId: String = "",
    val productName: String = "",
    val quantity: Double = 0.0,
    val unitPrice: Double = 0.0,
    val totalRefund: Double = 0.0,
    val refundMethod: String = "POTONG_HUTANG", // "POTONG_HUTANG" or "KEMBALI_KAS"
    val cashBoxId: String = "",
    val reason: String = "",
    val transactionDateMillis: Long = 0L
)

data class Expense(
    val id: String = "",
    val userId: String = "",
    val title: String = "",
    val category: String = "Operasional",
    val amount: Double = 0.0,
    val cashBoxId: String = "",
    val cashBoxName: String = "",
    val notes: String = "",
    val transactionDateMillis: Long = 0L
)

data class DebtRecord(
    val id: String = "",
    val userId: String = "",
    val debtType: String = "PIUTANG", // "HUTANG" or "PIUTANG"
    val partyId: String = "",
    val partyName: String = "",
    val referenceId: String = "",
    val invoiceNo: String = "",
    val totalAmount: Double = 0.0,
    val paidAmount: Double = 0.0,
    val remainingAmount: Double = 0.0,
    val status: String = "BELUM_LUNAS", // "BELUM_LUNAS", "CICILAN", "LUNAS"
    val notes: String = "",
    val transactionDateMillis: Long = 0L,
    val paidAtMillis: Long = 0L
)
