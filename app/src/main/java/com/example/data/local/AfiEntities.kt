package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.CashBox
import com.example.data.model.CashFlow
import com.example.data.model.DebtRecord
import com.example.data.model.Employee
import com.example.data.model.Expense
import com.example.data.model.JobType
import com.example.data.model.LocalUser
import com.example.data.model.Merchant
import com.example.data.model.Payslip
import com.example.data.model.PayslipItem
import com.example.data.model.Product
import com.example.data.model.Purchase
import com.example.data.model.PurchaseItem
import com.example.data.model.PurchaseReturn
import com.example.data.model.Sale
import com.example.data.model.SaleItem
import com.example.data.model.StoreSettings
import com.example.data.model.Supplier
import org.json.JSONArray
import org.json.JSONObject

@Entity(tableName = "local_users")
data class LocalUserEntity(
    @PrimaryKey val id: String,
    val username: String,
    val displayName: String,
    val passwordHash: String,
    val createdAtMillis: Long
) {
    fun toDomain() = LocalUser(id = id, username = username, displayName = displayName, createdAtMillis = createdAtMillis)
}

@Entity(tableName = "store_settings")
data class StoreSettingsEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val storeName: String,
    val ownerName: String,
    val phone: String,
    val address: String,
    val receiptFooter: String,
    val updatedAtMillis: Long
) {
    fun toDomain() = StoreSettings(id, userId, storeName, ownerName, phone, address, receiptFooter, updatedAtMillis)
}

@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val name: String,
    val sku: String,
    val category: String,
    val unit: String,
    val buyPrice: Double,
    val sellPrice: Double,
    val stock: Double,
    val minStock: Double,
    val updatedAtMillis: Long
) {
    fun toDomain() = Product(id, userId, name, sku, category, unit, buyPrice, sellPrice, stock, minStock, updatedAtMillis)
}

@Entity(tableName = "merchants")
data class MerchantEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val name: String,
    val phone: String,
    val routeArea: String,
    val address: String,
    val notes: String
) {
    fun toDomain() = Merchant(id, userId, name, phone, routeArea, address, notes)
}

@Entity(tableName = "suppliers")
data class SupplierEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val name: String,
    val phone: String,
    val suppliedItems: String,
    val address: String,
    val notes: String
) {
    fun toDomain() = Supplier(id, userId, name, phone, suppliedItems, address, notes)
}

@Entity(tableName = "employees")
data class EmployeeEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val name: String,
    val phone: String,
    val defaultJobTypeId: String,
    val defaultJobName: String,
    val active: Boolean
) {
    fun toDomain() = Employee(id, userId, name, phone, defaultJobTypeId, defaultJobName, active)
}

@Entity(tableName = "job_types")
data class JobTypeEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val code: String,
    val name: String,
    val unit: String,
    val ratePerUnit: Double,
    val sortOrder: Long
) {
    fun toDomain() = JobType(id, userId, code, name, unit, ratePerUnit, sortOrder)
}

@Entity(tableName = "payslips")
data class PayslipEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val employeeId: String,
    val employeeName: String,
    val periodLabel: String,
    val itemsJson: String,
    val bonus: Double,
    val deduction: Double,
    val totalAmount: Double,
    val paidAmount: Double = 0.0,
    val paid: Boolean,
    val cashBoxId: String,
    val notes: String,
    val workDateMillis: Long,
    val salaryDateMillis: Long,
    val paidAtMillis: Long,
    val createdAtMillis: Long
) {
    fun toDomain() = Payslip(
        id = id,
        userId = userId,
        employeeId = employeeId,
        employeeName = employeeName,
        periodLabel = periodLabel,
        items = decodePayslipItems(itemsJson),
        bonus = bonus,
        deduction = deduction,
        totalAmount = totalAmount,
        paidAmount = if (paid && paidAmount <= 0.0) totalAmount else paidAmount,
        paid = paid,
        cashBoxId = cashBoxId,
        notes = notes,
        workDateMillis = if (workDateMillis > 0L) workDateMillis else salaryDateMillis,
        salaryDateMillis = salaryDateMillis,
        paidAtMillis = paidAtMillis,
        createdAtMillis = createdAtMillis
    )
}

@Entity(tableName = "cash_boxes")
data class CashBoxEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val name: String,
    val description: String,
    val balance: Double,
    val primaryBox: Boolean
) {
    fun toDomain() = CashBox(id, userId, name, description, balance, primaryBox)
}

@Entity(tableName = "cash_flows")
data class CashFlowEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val flowType: String,
    val category: String,
    val amount: Double,
    val cashBoxId: String,
    val cashBoxName: String,
    val description: String,
    val referenceId: String,
    val transactionDateMillis: Long
) {
    fun toDomain() = CashFlow(id, userId, flowType, category, amount, cashBoxId, cashBoxName, description, referenceId, transactionDateMillis)
}

@Entity(tableName = "sales")
data class SaleEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val invoiceNo: String,
    val customerName: String,
    val merchantId: String,
    val itemsJson: String,
    val totalAmount: Double,
    val paidAmount: Double,
    val remainingDebt: Double,
    val totalCost: Double,
    val totalProfit: Double,
    val lossAmount: Double,
    val paymentStatus: String,
    val cashBoxId: String,
    val notes: String,
    val transactionDateMillis: Long
) {
    fun toDomain() = Sale(
        id, userId, invoiceNo, customerName, merchantId, decodeSaleItems(itemsJson),
        totalAmount, paidAmount, remainingDebt, totalCost, totalProfit, lossAmount,
        paymentStatus, cashBoxId, notes, transactionDateMillis
    )
}

@Entity(tableName = "purchases")
data class PurchaseEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val invoiceNo: String,
    val supplierName: String,
    val itemsJson: String,
    val totalAmount: Double,
    val paidAmount: Double,
    val remainingDebt: Double,
    val paymentStatus: String,
    val cashBoxId: String,
    val notes: String,
    val transactionDateMillis: Long
) {
    fun toDomain() = Purchase(
        id, userId, invoiceNo, supplierName, decodePurchaseItems(itemsJson),
        totalAmount, paidAmount, remainingDebt, paymentStatus, cashBoxId, notes, transactionDateMillis
    )
}

@Entity(tableName = "purchase_returns")
data class PurchaseReturnEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val purchaseId: String,
    val supplierName: String,
    val productId: String,
    val productName: String,
    val quantity: Double,
    val unitPrice: Double,
    val totalRefund: Double,
    val refundMethod: String,
    val cashBoxId: String,
    val reason: String,
    val transactionDateMillis: Long
) {
    fun toDomain() = PurchaseReturn(
        id, userId, purchaseId, supplierName, productId, productName,
        quantity, unitPrice, totalRefund, refundMethod, cashBoxId, reason, transactionDateMillis
    )
}

@Entity(tableName = "expenses")
data class ExpenseEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val title: String,
    val category: String,
    val amount: Double,
    val cashBoxId: String,
    val cashBoxName: String,
    val notes: String,
    val transactionDateMillis: Long
) {
    fun toDomain() = Expense(id, userId, title, category, amount, cashBoxId, cashBoxName, notes, transactionDateMillis)
}

@Entity(tableName = "debt_records")
data class DebtRecordEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val debtType: String,
    val partyId: String,
    val partyName: String,
    val referenceId: String,
    val invoiceNo: String,
    val totalAmount: Double,
    val paidAmount: Double,
    val remainingAmount: Double,
    val status: String,
    val notes: String,
    val transactionDateMillis: Long,
    val paidAtMillis: Long = 0L
) {
    fun toDomain() = DebtRecord(
        id, userId, debtType, partyId, partyName, referenceId,
        invoiceNo, totalAmount, paidAmount, remainingAmount, status, notes, transactionDateMillis, paidAtMillis
    )
}

// --- JSON Serializers for Nested Line Items ---

fun encodePayslipItems(items: List<PayslipItem>): String {
    val arr = JSONArray()
    items.forEach { item ->
        arr.put(JSONObject().apply {
            put("jobName", item.jobName)
            put("unit", item.unit)
            put("quantity", item.quantity)
            put("ratePerUnit", item.ratePerUnit)
            put("subtotal", item.subtotal)
        })
    }
    return arr.toString()
}

fun decodePayslipItems(json: String): List<PayslipItem> {
    return try {
        val arr = JSONArray(json)
        List(arr.length()) { i ->
            val obj = arr.getJSONObject(i)
            PayslipItem(
                jobName = obj.optString("jobName", ""),
                unit = obj.optString("unit", ""),
                quantity = obj.optDouble("quantity", 0.0),
                ratePerUnit = obj.optDouble("ratePerUnit", 0.0),
                subtotal = obj.optDouble("subtotal", 0.0)
            )
        }
    } catch (_: Exception) {
        emptyList()
    }
}

fun encodeSaleItems(items: List<SaleItem>): String {
    val arr = JSONArray()
    items.forEach { item ->
        arr.put(JSONObject().apply {
            put("productId", item.productId)
            put("productName", item.productName)
            put("unit", item.unit)
            put("quantity", item.quantity)
            put("buyPrice", item.buyPrice)
            put("sellPrice", item.sellPrice)
            put("subtotal", item.subtotal)
            put("profit", item.profit)
        })
    }
    return arr.toString()
}

fun decodeSaleItems(json: String): List<SaleItem> {
    return try {
        val arr = JSONArray(json)
        List(arr.length()) { i ->
            val obj = arr.getJSONObject(i)
            SaleItem(
                productId = obj.optString("productId", ""),
                productName = obj.optString("productName", ""),
                unit = obj.optString("unit", ""),
                quantity = obj.optDouble("quantity", 0.0),
                buyPrice = obj.optDouble("buyPrice", 0.0),
                sellPrice = obj.optDouble("sellPrice", 0.0),
                subtotal = obj.optDouble("subtotal", 0.0),
                profit = obj.optDouble("profit", 0.0)
            )
        }
    } catch (_: Exception) {
        emptyList()
    }
}

fun encodePurchaseItems(items: List<PurchaseItem>): String {
    val arr = JSONArray()
    items.forEach { item ->
        arr.put(JSONObject().apply {
            put("productId", item.productId)
            put("productName", item.productName)
            put("unit", item.unit)
            put("quantity", item.quantity)
            put("buyPrice", item.buyPrice)
            put("subtotal", item.subtotal)
        })
    }
    return arr.toString()
}

fun decodePurchaseItems(json: String): List<PurchaseItem> {
    return try {
        val arr = JSONArray(json)
        List(arr.length()) { i ->
            val obj = arr.getJSONObject(i)
            PurchaseItem(
                productId = obj.optString("productId", ""),
                productName = obj.optString("productName", ""),
                unit = obj.optString("unit", ""),
                quantity = obj.optDouble("quantity", 0.0),
                buyPrice = obj.optDouble("buyPrice", 0.0),
                subtotal = obj.optDouble("subtotal", 0.0)
            )
        }
    } catch (_: Exception) {
        emptyList()
    }
}
