package com.example.data.repository

import androidx.room.withTransaction
import com.example.data.local.CashBoxEntity
import com.example.data.local.CashFlowEntity
import com.example.data.local.DebtRecordEntity
import com.example.data.local.ExpenseEntity
import com.example.data.local.PurchaseEntity
import com.example.data.local.PurchaseReturnEntity
import com.example.data.local.SaleEntity
import com.example.data.local.encodePurchaseItems
import com.example.data.local.encodeSaleItems
import com.example.data.model.CashBox
import com.example.data.model.CashFlow
import com.example.data.model.DebtRecord
import com.example.data.model.Expense
import com.example.data.model.Product
import com.example.data.model.Purchase
import com.example.data.model.PurchaseItem
import com.example.data.model.PurchaseReturn
import com.example.data.model.Sale
import com.example.data.model.SaleItem
import com.example.util.generateInvoiceNo
import kotlinx.coroutines.CancellationException
import java.util.UUID
import kotlin.math.max

class AfiTransactionRepository(private val base: AfiRepository) {

    private val db get() = base.db
    private val dao get() = base.dao
    private val userId get() = base.userId

    suspend fun createSale(
        customerName: String,
        merchantId: String,
        items: List<SaleItem>,
        productsSnapshot: List<Product>,
        paidAmountInput: Double,
        isPiutang: Boolean,
        cashBox: CashBox?,
        notes: String,
        transactionDateMillis: Long = System.currentTimeMillis()
    ): Result<Sale> {
        return try {
            require(customerName.isNotBlank()) { "Nama pembeli/pedagang wajib diisi" }
            require(items.isNotEmpty()) { "Pilih minimal 1 produk" }

            val txMillis = if (transactionDateMillis > 0L) transactionDateMillis else System.currentTimeMillis()
            val nowMillis = System.currentTimeMillis()
            val invoiceNo = generateInvoiceNo("PJ")
            val saleId = UUID.randomUUID().toString()
            val totalAmount = items.sumOf { it.subtotal }
            val totalCost = items.sumOf { it.buyPrice * it.quantity }
            val totalProfit = items.sumOf { it.profit }
            val lossAmount = items.filter { it.sellPrice < it.buyPrice }
                .sumOf { (it.buyPrice - it.sellPrice) * it.quantity }

            val paidAmount = if (isPiutang) paidAmountInput.coerceIn(0.0, totalAmount) else totalAmount
            val remainingDebt = max(0.0, totalAmount - paidAmount)
            val paymentStatus = if (remainingDebt > 0.0) "PIUTANG" else "LUNAS"

            val entity = SaleEntity(
                id = saleId,
                userId = userId,
                invoiceNo = invoiceNo,
                customerName = customerName.trim().take(120),
                merchantId = merchantId,
                itemsJson = encodeSaleItems(items),
                totalAmount = totalAmount,
                paidAmount = paidAmount,
                remainingDebt = remainingDebt,
                totalCost = totalCost,
                totalProfit = totalProfit,
                lossAmount = lossAmount,
                paymentStatus = paymentStatus,
                cashBoxId = if (cashBox != null && paidAmount > 0.0) cashBox.id else "",
                notes = notes.trim().take(500),
                transactionDateMillis = txMillis
            )

            db.withTransaction {
                dao.insertSale(entity)

                items.forEach { item ->
                    val currentProd = dao.getProductById(item.productId)
                        ?: productsSnapshot.firstOrNull { it.id == item.productId }?.let {
                            com.example.data.local.ProductEntity(
                                it.id, userId, it.name, it.sku, it.category, it.unit,
                                it.buyPrice, it.sellPrice, it.stock, it.minStock, nowMillis
                            )
                        }
                    if (currentProd != null) {
                        dao.upsertProduct(
                            currentProd.copy(
                                stock = max(0.0, currentProd.stock - item.quantity),
                                updatedAtMillis = nowMillis
                            )
                        )
                    }
                }

                if (paidAmount > 0.0 && cashBox != null) {
                    dao.adjustCashBoxBalance(cashBox.id, paidAmount)
                    dao.insertCashFlow(
                        CashFlowEntity(
                            id = UUID.randomUUID().toString(),
                            userId = userId,
                            flowType = "IN",
                            category = "PENJUALAN",
                            amount = paidAmount,
                            cashBoxId = cashBox.id,
                            cashBoxName = cashBox.name,
                            description = "Penjualan $invoiceNo - $customerName",
                            referenceId = saleId,
                            transactionDateMillis = txMillis
                        )
                    )
                }

                if (remainingDebt > 0.0) {
                    dao.upsertDebtRecord(
                        DebtRecordEntity(
                            id = UUID.randomUUID().toString(),
                            userId = userId,
                            debtType = "PIUTANG",
                            partyId = merchantId,
                            partyName = customerName.trim().take(120),
                            referenceId = saleId,
                            invoiceNo = invoiceNo,
                            totalAmount = totalAmount,
                            paidAmount = paidAmount,
                            remainingAmount = remainingDebt,
                            status = if (paidAmount > 0.0) "CICILAN" else "BELUM_LUNAS",
                            notes = notes.trim().take(500),
                            transactionDateMillis = txMillis,
                            paidAtMillis = if (paidAmount > 0.0) txMillis else 0L
                        )
                    )
                }
            }
            Result.success(entity.toDomain())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateSaleDate(saleId: String, newMillis: Long): Result<Unit> {
        return try {
            require(saleId.isNotBlank()) { "ID penjualan tidak valid" }
            require(newMillis > 0L) { "Waktu transaksi tidak valid" }
            db.withTransaction {
                dao.updateSaleDate(saleId, newMillis)
                dao.updateCashFlowDateByReference(saleId, newMillis)
                dao.updateDebtDateByReference(saleId, newMillis)
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createPurchase(
        supplierName: String,
        items: List<PurchaseItem>,
        productsSnapshot: List<Product>,
        paidAmountInput: Double,
        isHutang: Boolean,
        cashBox: CashBox?,
        notes: String,
        transactionDateMillis: Long = System.currentTimeMillis()
    ): Result<Purchase> {
        return try {
            require(supplierName.isNotBlank()) { "Nama pemasok wajib diisi" }
            require(items.isNotEmpty()) { "Pilih minimal 1 produk" }

            val txMillis = if (transactionDateMillis > 0L) transactionDateMillis else System.currentTimeMillis()
            val nowMillis = System.currentTimeMillis()
            val invoiceNo = generateInvoiceNo("PB")
            val purchaseId = UUID.randomUUID().toString()
            val totalAmount = items.sumOf { it.subtotal }
            val paidAmount = if (isHutang) paidAmountInput.coerceIn(0.0, totalAmount) else totalAmount
            val remainingDebt = max(0.0, totalAmount - paidAmount)
            val paymentStatus = if (remainingDebt > 0.0) "HUTANG" else "LUNAS"

            val entity = PurchaseEntity(
                id = purchaseId,
                userId = userId,
                invoiceNo = invoiceNo,
                supplierName = supplierName.trim().take(120),
                itemsJson = encodePurchaseItems(items),
                totalAmount = totalAmount,
                paidAmount = paidAmount,
                remainingDebt = remainingDebt,
                paymentStatus = paymentStatus,
                cashBoxId = if (cashBox != null && paidAmount > 0.0) cashBox.id else "",
                notes = notes.trim().take(500),
                transactionDateMillis = txMillis
            )

            db.withTransaction {
                dao.insertPurchase(entity)

                items.forEach { item ->
                    val currentProd = dao.getProductById(item.productId)
                        ?: productsSnapshot.firstOrNull { it.id == item.productId }?.let {
                            com.example.data.local.ProductEntity(
                                it.id, userId, it.name, it.sku, it.category, it.unit,
                                it.buyPrice, it.sellPrice, it.stock, it.minStock, nowMillis
                            )
                        }
                    if (currentProd != null) {
                        dao.upsertProduct(
                            currentProd.copy(
                                stock = currentProd.stock + item.quantity,
                                buyPrice = item.buyPrice.coerceAtLeast(0.0),
                                updatedAtMillis = nowMillis
                            )
                        )
                    }
                }

                if (paidAmount > 0.0 && cashBox != null) {
                    dao.adjustCashBoxBalance(cashBox.id, -paidAmount)
                    dao.insertCashFlow(
                        CashFlowEntity(
                            id = UUID.randomUUID().toString(),
                            userId = userId,
                            flowType = "OUT",
                            category = "PEMBELIAN",
                            amount = paidAmount,
                            cashBoxId = cashBox.id,
                            cashBoxName = cashBox.name,
                            description = "Pembelian $invoiceNo - $supplierName",
                            referenceId = purchaseId,
                            transactionDateMillis = txMillis
                        )
                    )
                }

                if (remainingDebt > 0.0) {
                    dao.upsertDebtRecord(
                        DebtRecordEntity(
                            id = UUID.randomUUID().toString(),
                            userId = userId,
                            debtType = "HUTANG",
                            partyId = "",
                            partyName = supplierName.trim().take(120),
                            referenceId = purchaseId,
                            invoiceNo = invoiceNo,
                            totalAmount = totalAmount,
                            paidAmount = paidAmount,
                            remainingAmount = remainingDebt,
                            status = if (paidAmount > 0.0) "CICILAN" else "BELUM_LUNAS",
                            notes = notes.trim().take(500),
                            transactionDateMillis = txMillis,
                            paidAtMillis = if (paidAmount > 0.0) txMillis else 0L
                        )
                    )
                }
            }
            Result.success(entity.toDomain())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updatePurchaseDate(purchaseId: String, newMillis: Long): Result<Unit> {
        return try {
            require(purchaseId.isNotBlank()) { "ID pembelian tidak valid" }
            require(newMillis > 0L) { "Waktu transaksi tidak valid" }
            db.withTransaction {
                dao.updatePurchaseDate(purchaseId, newMillis)
                dao.updateCashFlowDateByReference(purchaseId, newMillis)
                dao.updateDebtDateByReference(purchaseId, newMillis)
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createPurchaseReturn(
        supplierName: String,
        product: Product,
        quantity: Double,
        unitPrice: Double,
        refundMethod: String,
        cashBox: CashBox?,
        reason: String,
        matchingHutang: DebtRecord?,
        transactionDateMillis: Long = System.currentTimeMillis()
    ): Result<PurchaseReturn> {
        return try {
            require(supplierName.isNotBlank()) { "Nama pemasok wajib diisi" }
            require(quantity > 0.0) { "Jumlah retur harus lebih dari 0" }

            val txMillis = if (transactionDateMillis > 0L) transactionDateMillis else System.currentTimeMillis()
            val nowMillis = System.currentTimeMillis()
            val returnId = UUID.randomUUID().toString()
            val totalRefund = quantity * unitPrice

            val entity = PurchaseReturnEntity(
                id = returnId,
                userId = userId,
                purchaseId = matchingHutang?.referenceId ?: "",
                supplierName = supplierName.trim().take(120),
                productId = product.id,
                productName = product.name,
                quantity = quantity,
                unitPrice = unitPrice,
                totalRefund = totalRefund,
                refundMethod = refundMethod,
                cashBoxId = if (refundMethod == "KEMBALI_KAS" && cashBox != null) cashBox.id else "",
                reason = reason.trim().take(500),
                transactionDateMillis = txMillis
            )

            db.withTransaction {
                dao.insertPurchaseReturn(entity)

                val currentProd = dao.getProductById(product.id)
                if (currentProd != null) {
                    dao.upsertProduct(
                        currentProd.copy(
                            stock = max(0.0, currentProd.stock - quantity),
                            updatedAtMillis = nowMillis
                        )
                    )
                }

                if (refundMethod == "KEMBALI_KAS" && cashBox != null && totalRefund > 0.0) {
                    dao.adjustCashBoxBalance(cashBox.id, totalRefund)
                    dao.insertCashFlow(
                        CashFlowEntity(
                            id = UUID.randomUUID().toString(),
                            userId = userId,
                            flowType = "IN",
                            category = "RETUR_PEMBELIAN",
                            amount = totalRefund,
                            cashBoxId = cashBox.id,
                            cashBoxName = cashBox.name,
                            description = "Retur ${product.name} ke $supplierName",
                            referenceId = returnId,
                            transactionDateMillis = txMillis
                        )
                    )
                } else if (refundMethod == "POTONG_HUTANG" && matchingHutang != null && totalRefund > 0.0) {
                    val applied = minOf(totalRefund, matchingHutang.remainingAmount)
                    val newPaid = matchingHutang.paidAmount + applied
                    val newRem = max(0.0, matchingHutang.remainingAmount - applied)
                    val newStatus = if (newRem <= 0.0) "LUNAS" else "CICILAN"
                    dao.upsertDebtRecord(
                        DebtRecordEntity(
                            id = matchingHutang.id,
                            userId = userId,
                            debtType = matchingHutang.debtType,
                            partyId = matchingHutang.partyId,
                            partyName = matchingHutang.partyName,
                            referenceId = matchingHutang.referenceId,
                            invoiceNo = matchingHutang.invoiceNo,
                            totalAmount = matchingHutang.totalAmount,
                            paidAmount = newPaid,
                            remainingAmount = newRem,
                            status = newStatus,
                            notes = matchingHutang.notes,
                            transactionDateMillis = matchingHutang.transactionDateMillis,
                            paidAtMillis = txMillis
                        )
                    )
                }
            }
            Result.success(entity.toDomain())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createExpense(
        title: String,
        category: String,
        amount: Double,
        cashBox: CashBox,
        notes: String,
        transactionDateMillis: Long = System.currentTimeMillis()
    ): Result<Expense> {
        return try {
            require(title.isNotBlank()) { "Judul pengeluaran wajib diisi" }
            require(amount > 0.0) { "Nominal pengeluaran harus lebih dari 0" }
            val txMillis = if (transactionDateMillis > 0L) transactionDateMillis else System.currentTimeMillis()
            val expId = UUID.randomUUID().toString()

            val entity = ExpenseEntity(
                id = expId,
                userId = userId,
                title = title.trim().take(120),
                category = category.trim().ifBlank { "Operasional" }.take(60),
                amount = amount,
                cashBoxId = cashBox.id,
                cashBoxName = cashBox.name,
                notes = notes.trim().take(500),
                transactionDateMillis = txMillis
            )

            db.withTransaction {
                dao.insertExpense(entity)
                dao.adjustCashBoxBalance(cashBox.id, -amount)
                dao.insertCashFlow(
                    CashFlowEntity(
                        id = UUID.randomUUID().toString(),
                        userId = userId,
                        flowType = "OUT",
                        category = "PENGELUARAN",
                        amount = amount,
                        cashBoxId = cashBox.id,
                        cashBoxName = cashBox.name,
                        description = "$title ($category)",
                        referenceId = expId,
                        transactionDateMillis = txMillis
                    )
                )
            }
            Result.success(entity.toDomain())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateExpenseDate(expenseId: String, newMillis: Long): Result<Unit> {
        return try {
            require(expenseId.isNotBlank()) { "ID pengeluaran tidak valid" }
            require(newMillis > 0L) { "Waktu transaksi tidak valid" }
            db.withTransaction {
                dao.updateExpenseDate(expenseId, newMillis)
                dao.updateCashFlowDateByReference(expenseId, newMillis)
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun payDebtRecord(
        debt: DebtRecord,
        paymentAmount: Double,
        cashBox: CashBox,
        notes: String,
        paidAtMillis: Long = System.currentTimeMillis()
    ): Result<Unit> {
        return try {
            require(paymentAmount > 0.0) { "Nominal pembayaran harus lebih dari 0" }
            val actualPay = minOf(paymentAmount, debt.remainingAmount)
            val newPaid = debt.paidAmount + actualPay
            val newRem = max(0.0, debt.remainingAmount - actualPay)
            val newStatus = if (newRem <= 0.0) "LUNAS" else "CICILAN"
            val payMillis = if (paidAtMillis > 0L) paidAtMillis else System.currentTimeMillis()
            val isPiutang = debt.debtType == "PIUTANG"
            val cashDelta = if (isPiutang) actualPay else -actualPay

            db.withTransaction {
                dao.upsertDebtRecord(
                    DebtRecordEntity(
                        id = debt.id,
                        userId = userId,
                        debtType = debt.debtType,
                        partyId = debt.partyId,
                        partyName = debt.partyName,
                        referenceId = debt.referenceId,
                        invoiceNo = debt.invoiceNo,
                        totalAmount = debt.totalAmount,
                        paidAmount = newPaid,
                        remainingAmount = newRem,
                        status = newStatus,
                        notes = notes.trim().take(500),
                        transactionDateMillis = debt.transactionDateMillis,
                        paidAtMillis = payMillis
                    )
                )
                dao.adjustCashBoxBalance(cashBox.id, cashDelta)
                dao.insertCashFlow(
                    CashFlowEntity(
                        id = UUID.randomUUID().toString(),
                        userId = userId,
                        flowType = if (isPiutang) "IN" else "OUT",
                        category = if (isPiutang) "TERIMA_PIUTANG" else "BAYAR_HUTANG",
                        amount = actualPay,
                        cashBoxId = cashBox.id,
                        cashBoxName = cashBox.name,
                        description = "${if (isPiutang) "Pelunasan piutang" else "Bayar hutang"} ${debt.partyName}",
                        referenceId = debt.id,
                        transactionDateMillis = payMillis
                    )
                )
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateDebtPaidDate(debtId: String, newPaidMillis: Long): Result<Unit> {
        return try {
            require(debtId.isNotBlank()) { "ID hutang/piutang tidak valid" }
            require(newPaidMillis > 0L) { "Waktu pembayaran tidak valid" }
            db.withTransaction {
                dao.updateDebtPaidAt(debtId, newPaidMillis)
                dao.updateCashFlowDateByReference(debtId, newPaidMillis)
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createManualDebt(
        debtType: String,
        partyId: String,
        partyName: String,
        totalAmount: Double,
        notes: String,
        transactionDateMillis: Long = System.currentTimeMillis()
    ): Result<Unit> {
        return try {
            require(partyName.isNotBlank()) { "Nama pihak wajib diisi" }
            require(totalAmount > 0.0) { "Nominal harus lebih dari 0" }
            val txMillis = if (transactionDateMillis > 0L) transactionDateMillis else System.currentTimeMillis()
            dao.upsertDebtRecord(
                DebtRecordEntity(
                    id = UUID.randomUUID().toString(),
                    userId = userId,
                    debtType = debtType,
                    partyId = partyId,
                    partyName = partyName.trim().take(120),
                    referenceId = "",
                    invoiceNo = generateInvoiceNo(if (debtType == "HUTANG") "HT" else "PT"),
                    totalAmount = totalAmount,
                    paidAmount = 0.0,
                    remainingAmount = totalAmount,
                    status = "BELUM_LUNAS",
                    notes = notes.trim().take(500),
                    transactionDateMillis = txMillis,
                    paidAtMillis = 0L
                )
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun settleAllDebts(
        debts: List<DebtRecord>,
        cashBox: CashBox,
        notes: String,
        paidAtMillis: Long = System.currentTimeMillis()
    ): Result<Int> {
        return try {
            val unpaidList = debts.filter { it.remainingAmount > 0.0 && it.status != "LUNAS" }
            require(unpaidList.isNotEmpty()) { "Tidak ada tagihan yang belum lunas" }
            val payMillis = if (paidAtMillis > 0L) paidAtMillis else System.currentTimeMillis()

            db.withTransaction {
                unpaidList.forEach { debt ->
                    val actualPay = debt.remainingAmount
                    val isPiutang = debt.debtType == "PIUTANG"
                    val cashDelta = if (isPiutang) actualPay else -actualPay
                    val mergedNotes = notes.trim().take(500).ifBlank { debt.notes }

                    dao.upsertDebtRecord(
                        DebtRecordEntity(
                            id = debt.id,
                            userId = userId,
                            debtType = debt.debtType,
                            partyId = debt.partyId,
                            partyName = debt.partyName,
                            referenceId = debt.referenceId,
                            invoiceNo = debt.invoiceNo,
                            totalAmount = debt.totalAmount,
                            paidAmount = debt.totalAmount,
                            remainingAmount = 0.0,
                            status = "LUNAS",
                            notes = mergedNotes,
                            transactionDateMillis = debt.transactionDateMillis,
                            paidAtMillis = payMillis
                        )
                    )
                    dao.adjustCashBoxBalance(cashBox.id, cashDelta)
                    dao.insertCashFlow(
                        CashFlowEntity(
                            id = UUID.randomUUID().toString(),
                            userId = userId,
                            flowType = if (isPiutang) "IN" else "OUT",
                            category = if (isPiutang) "TERIMA_PIUTANG" else "BAYAR_HUTANG",
                            amount = actualPay,
                            cashBoxId = cashBox.id,
                            cashBoxName = cashBox.name,
                            description = "${if (isPiutang) "Pelunasan piutang" else "Pelunasan hutang"} ${debt.partyName} (${debt.invoiceNo})",
                            referenceId = debt.id,
                            transactionDateMillis = payMillis
                        )
                    )
                }
            }
            Result.success(unpaidList.size)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun payDebtsByInstallmentPool(
        debts: List<DebtRecord>,
        totalPaymentAmount: Double,
        cashBox: CashBox,
        notes: String,
        paidAtMillis: Long = System.currentTimeMillis()
    ): Result<Pair<Int, Double>> {
        return try {
            require(totalPaymentAmount > 0.0) { "Nominal uang masuk/bayar harus lebih dari 0" }
            val unpaidList = debts
                .filter { it.remainingAmount > 0.0 && it.status != "LUNAS" }
                .sortedBy { it.transactionDateMillis }
            require(unpaidList.isNotEmpty()) { "Tidak ada tagihan yang belum lunas" }
            val payMillis = if (paidAtMillis > 0L) paidAtMillis else System.currentTimeMillis()

            var poolRemaining = totalPaymentAmount
            var updatedCount = 0
            var appliedTotal = 0.0

            db.withTransaction {
                for (debt in unpaidList) {
                    if (poolRemaining <= 0.001) break
                    val debtRem = debt.remainingAmount
                    if (debtRem <= 0.0) continue

                    val actualPay = minOf(poolRemaining, debtRem)
                    val newPaid = debt.paidAmount + actualPay
                    val newRem = max(0.0, debt.remainingAmount - actualPay)
                    val isLunas = newRem <= 0.001
                    val finalPaid = if (isLunas) debt.totalAmount else newPaid
                    val finalRem = if (isLunas) 0.0 else newRem
                    val newStatus = if (isLunas) "LUNAS" else "CICILAN"
                    val isPiutang = debt.debtType == "PIUTANG"
                    val cashDelta = if (isPiutang) actualPay else -actualPay
                    val mergedNotes = notes.trim().take(500).ifBlank { debt.notes }

                    dao.upsertDebtRecord(
                        DebtRecordEntity(
                            id = debt.id,
                            userId = userId,
                            debtType = debt.debtType,
                            partyId = debt.partyId,
                            partyName = debt.partyName,
                            referenceId = debt.referenceId,
                            invoiceNo = debt.invoiceNo,
                            totalAmount = debt.totalAmount,
                            paidAmount = finalPaid,
                            remainingAmount = finalRem,
                            status = newStatus,
                            notes = mergedNotes,
                            transactionDateMillis = debt.transactionDateMillis,
                            paidAtMillis = payMillis
                        )
                    )
                    dao.adjustCashBoxBalance(cashBox.id, cashDelta)
                    dao.insertCashFlow(
                        CashFlowEntity(
                            id = UUID.randomUUID().toString(),
                            userId = userId,
                            flowType = if (isPiutang) "IN" else "OUT",
                            category = if (isPiutang) "TERIMA_PIUTANG" else "BAYAR_HUTANG",
                            amount = actualPay,
                            cashBoxId = cashBox.id,
                            cashBoxName = cashBox.name,
                            description = "${if (isPiutang) (if (isLunas) "Pelunasan" else "Cicilan") + " piutang" else (if (isLunas) "Pelunasan" else "Cicilan") + " hutang"} ${debt.partyName} (${debt.invoiceNo})",
                            referenceId = debt.id,
                            transactionDateMillis = payMillis
                        )
                    )
                    poolRemaining -= actualPay
                    appliedTotal += actualPay
                    updatedCount++
                }
            }
            Result.success(updatedCount to appliedTotal)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteDebtRecord(debt: DebtRecord): Result<Unit> {
        return try {
            require(debt.id.isNotBlank()) { "ID hutang/piutang tidak valid" }
            db.withTransaction {
                val paymentFlows = dao.getCashFlowsByReference(debt.id)
                val originFlows = if (debt.referenceId.isNotBlank()) {
                    dao.getCashFlowsByReference(debt.referenceId)
                } else {
                    emptyList()
                }
                (paymentFlows + originFlows)
                    .distinctBy { it.id }
                    .forEach { flow ->
                    if (flow.cashBoxId.isNotBlank() && dao.getCashBoxById(flow.cashBoxId) != null) {
                        val reverseDelta = if (flow.flowType == "IN") -flow.amount else flow.amount
                        dao.adjustCashBoxBalance(flow.cashBoxId, reverseDelta)
                    }
                }
                dao.deleteCashFlowsByReference(debt.id)
                if (debt.referenceId.isNotBlank()) {
                    dao.deleteCashFlowsByReference(debt.referenceId)
                    if (debt.debtType == "PIUTANG") {
                        dao.deleteSale(debt.referenceId)
                    } else if (debt.debtType == "HUTANG") {
                        dao.deletePurchase(debt.referenceId)
                    }
                }
                dao.deleteDebtRecord(debt.id)
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createCashBox(name: String, description: String, initialBalance: Double): Result<Unit> {
        return try {
            require(name.isNotBlank()) { "Nama Cash Box wajib diisi" }
            val nowMillis = System.currentTimeMillis()
            val boxId = UUID.randomUUID().toString()
            db.withTransaction {
                dao.upsertCashBox(
                    CashBoxEntity(
                        id = boxId,
                        userId = userId,
                        name = name.trim().take(80),
                        description = description.trim().take(200),
                        balance = initialBalance,
                        primaryBox = false
                    )
                )
                if (initialBalance > 0.0) {
                    dao.insertCashFlow(
                        CashFlowEntity(
                            id = UUID.randomUUID().toString(),
                            userId = userId,
                            flowType = "IN",
                            category = "PENAMBAHAN_SALDO",
                            amount = initialBalance,
                            cashBoxId = boxId,
                            cashBoxName = name.trim().take(80),
                            description = "Saldo awal ${name.trim()}",
                            referenceId = boxId,
                            transactionDateMillis = nowMillis
                        )
                    )
                }
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteCashBox(cashBoxId: String): Result<Unit> {
        return try {
            require(cashBoxId.isNotBlank()) { "ID Cash Box tidak valid" }
            dao.deleteCashBox(cashBoxId)
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun adjustCashBoxBalance(
        cashBox: CashBox,
        isAddition: Boolean,
        amount: Double,
        description: String,
        transactionDateMillis: Long = System.currentTimeMillis()
    ): Result<Unit> {
        return try {
            require(amount > 0.0) { "Nominal harus lebih dari 0" }
            val txMillis = if (transactionDateMillis > 0L) transactionDateMillis else System.currentTimeMillis()
            val delta = if (isAddition) amount else -amount

            db.withTransaction {
                dao.adjustCashBoxBalance(cashBox.id, delta)
                dao.insertCashFlow(
                    CashFlowEntity(
                        id = UUID.randomUUID().toString(),
                        userId = userId,
                        flowType = if (isAddition) "IN" else "OUT",
                        category = if (isAddition) "PENAMBAHAN_SALDO" else "PENGURANGAN_SALDO",
                        amount = amount,
                        cashBoxId = cashBox.id,
                        cashBoxName = cashBox.name,
                        description = description.trim().ifBlank {
                            if (isAddition) "Penambahan saldo ${cashBox.name}" else "Pengurangan saldo ${cashBox.name}"
                        }.take(300),
                        referenceId = cashBox.id,
                        transactionDateMillis = txMillis
                    )
                )
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun transferCashBoxBalance(
        fromBox: CashBox,
        toBox: CashBox,
        amount: Double,
        description: String,
        transactionDateMillis: Long = System.currentTimeMillis()
    ): Result<Unit> {
        return try {
            require(fromBox.id != toBox.id) { "Cash Box asal dan tujuan tidak boleh sama" }
            require(amount > 0.0) { "Nominal pemindahan harus lebih dari 0" }
            val txMillis = if (transactionDateMillis > 0L) transactionDateMillis else System.currentTimeMillis()
            val cleanDesc = description.trim().take(200)
            val transferRefId = UUID.randomUUID().toString()

            db.withTransaction {
                val freshFrom = dao.getCashBoxById(fromBox.id) ?: error("Cash Box asal tidak ditemukan")
                val freshTo = dao.getCashBoxById(toBox.id) ?: error("Cash Box tujuan tidak ditemukan")

                dao.adjustCashBoxBalance(freshFrom.id, -amount)
                dao.adjustCashBoxBalance(freshTo.id, amount)

                val outDesc = buildString {
                    append("Pindah saldo ke ${freshTo.name}")
                    if (cleanDesc.isNotBlank()) append(" ($cleanDesc)")
                }.take(300)
                val inDesc = buildString {
                    append("Terima saldo dari ${freshFrom.name}")
                    if (cleanDesc.isNotBlank()) append(" ($cleanDesc)")
                }.take(300)

                dao.insertCashFlow(
                    CashFlowEntity(
                        id = UUID.randomUUID().toString(),
                        userId = userId,
                        flowType = "OUT",
                        category = "PINDAH_SALDO",
                        amount = amount,
                        cashBoxId = freshFrom.id,
                        cashBoxName = freshFrom.name,
                        description = outDesc,
                        referenceId = transferRefId,
                        transactionDateMillis = txMillis
                    )
                )
                dao.insertCashFlow(
                    CashFlowEntity(
                        id = UUID.randomUUID().toString(),
                        userId = userId,
                        flowType = "IN",
                        category = "PINDAH_SALDO",
                        amount = amount,
                        cashBoxId = freshTo.id,
                        cashBoxName = freshTo.name,
                        description = inDesc,
                        referenceId = transferRefId,
                        transactionDateMillis = txMillis
                    )
                )
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteCashFlowHistory(flow: CashFlow): Result<Unit> {
        return try {
            db.withTransaction {
                if (flow.category == "PINDAH_SALDO" && flow.referenceId.isNotBlank()) {
                    val pairedFlows = dao.getCashFlowsByReference(flow.referenceId)
                    pairedFlows.forEach { paired ->
                        if (paired.cashBoxId.isNotBlank() && dao.getCashBoxById(paired.cashBoxId) != null) {
                            val reverseDelta = if (paired.flowType == "IN") -paired.amount else paired.amount
                            dao.adjustCashBoxBalance(paired.cashBoxId, reverseDelta)
                        }
                    }
                    dao.deleteCashFlowsByReference(flow.referenceId)
                } else {
                    dao.deleteCashFlow(flow.id)
                    if (flow.cashBoxId.isNotBlank() && dao.getCashBoxById(flow.cashBoxId) != null) {
                        val reverseDelta = if (flow.flowType == "IN") -flow.amount else flow.amount
                        dao.adjustCashBoxBalance(flow.cashBoxId, reverseDelta)
                    }
                    if (flow.category == "PENGELUARAN" && flow.referenceId.isNotBlank()) {
                        dao.deleteExpense(flow.referenceId)
                    }
                }
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun resetTransactionData(includePayslips: Boolean = false): Result<Unit> {
        return try {
            db.withTransaction {
                dao.deleteAllSales(userId)
                dao.deleteAllPurchases(userId)
                dao.deleteAllPurchaseReturns(userId)
                dao.deleteAllExpenses(userId)
                dao.deleteAllDebtRecords(userId)
                dao.deleteAllCashFlows(userId)
                dao.resetAllCashBoxBalances(userId)
                if (includePayslips) {
                    dao.deleteAllPayslips(userId)
                }
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
