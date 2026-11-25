package com.example.data.repository

import androidx.room.withTransaction
import com.example.data.local.AfiDao
import com.example.data.local.AfiDatabase
import com.example.data.local.CashBoxEntity
import com.example.data.local.CashFlowEntity
import com.example.data.local.EmployeeEntity
import com.example.data.local.JobTypeEntity
import com.example.data.local.MerchantEntity
import com.example.data.local.PayslipEntity
import com.example.data.local.ProductEntity
import com.example.data.local.StoreSettingsEntity
import com.example.data.local.SupplierEntity
import com.example.data.local.encodePayslipItems
import com.example.data.model.CashBox
import com.example.data.model.CashFlow
import com.example.data.model.DebtRecord
import com.example.data.model.Employee
import com.example.data.model.Expense
import com.example.data.model.JobType
import com.example.data.model.Merchant
import com.example.data.model.Payslip
import com.example.data.model.Product
import com.example.data.model.Purchase
import com.example.data.model.PurchaseReturn
import com.example.data.model.Sale
import com.example.data.model.StoreSettings
import com.example.data.model.Supplier
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

class AfiRepository(
    internal val db: AfiDatabase,
    internal val userId: String,
    private val defaultOwnerName: String = ""
) {
    internal val dao: AfiDao = db.afiDao()

    fun observeStoreSettings(): Flow<StoreSettings> =
        dao.observeStoreSettings(userId).map { entity ->
            entity?.toDomain() ?: StoreSettings(id = "default_${userId}", userId = userId)
        }

    fun observeProducts(): Flow<List<Product>> =
        dao.observeProducts(userId).map { list -> list.map { it.toDomain() } }

    fun observeMerchants(): Flow<List<Merchant>> =
        dao.observeMerchants(userId).map { list -> list.map { it.toDomain() } }

    fun observeSuppliers(): Flow<List<Supplier>> =
        dao.observeSuppliers(userId).map { list -> list.map { it.toDomain() } }

    fun observeEmployees(): Flow<List<Employee>> =
        dao.observeEmployees(userId).map { list -> list.map { it.toDomain() } }

    fun observeJobTypes(): Flow<List<JobType>> =
        dao.observeJobTypes(userId).map { list -> list.map { it.toDomain() } }

    fun observePayslips(): Flow<List<Payslip>> =
        dao.observePayslips(userId).map { list -> list.map { it.toDomain() } }

    fun observeCashBoxes(): Flow<List<CashBox>> =
        dao.observeCashBoxes(userId).map { list -> list.map { it.toDomain() } }

    fun observeCashFlows(): Flow<List<CashFlow>> =
        dao.observeCashFlows(userId).map { list -> list.map { it.toDomain() } }

    fun observeSales(): Flow<List<Sale>> =
        dao.observeSales(userId).map { list -> list.map { it.toDomain() } }

    fun observePurchases(): Flow<List<Purchase>> =
        dao.observePurchases(userId).map { list -> list.map { it.toDomain() } }

    fun observePurchaseReturns(): Flow<List<PurchaseReturn>> =
        dao.observePurchaseReturns(userId).map { list -> list.map { it.toDomain() } }

    fun observeExpenses(): Flow<List<Expense>> =
        dao.observeExpenses(userId).map { list -> list.map { it.toDomain() } }

    fun observeDebtRecords(): Flow<List<DebtRecord>> =
        dao.observeDebtRecords(userId).map { list -> list.map { it.toDomain() } }

    suspend fun ensureInitialDefaults(): Result<Unit> {
        return try {
            val nowMillis = System.currentTimeMillis()
            db.withTransaction {
                if (dao.getStoreSettings(userId) == null) {
                    dao.upsertStoreSettings(
                        StoreSettingsEntity(
                            id = "default_$userId",
                            userId = userId,
                            storeName = "Toko & Pabrik AFI",
                            ownerName = defaultOwnerName,
                            phone = "",
                            address = "",
                            receiptFooter = "Terima kasih atas kerja sama Anda.",
                            updatedAtMillis = nowMillis
                        )
                    )
                }

                if (dao.countCashBoxes(userId) == 0) {
                    dao.upsertCashBox(
                        CashBoxEntity(
                            id = "cash_main_$userId",
                            userId = userId,
                            name = "Cash Box Utama",
                            description = "Laci kas tunai toko & pabrik",
                            balance = 0.0,
                            primaryBox = true
                        )
                    )
                }

                if (dao.countJobTypes(userId) == 0) {
                    val defaultJobs = listOf(
                        Triple("penakar", "PENAKAR", Triple("Penakar", "Takar", 500.0)),
                        Triple("pengkilo", "PENGKILO", Triple("Pengkilo", "Kg", 1000.0)),
                        Triple("sealer", "SEALER", Triple("Sealer", "Pak", 300.0)),
                        Triple("packing", "PACKING", Triple("Packing", "Bal", 1500.0)),
                        Triple("pencuci", "PENCUCI", Triple("Pencuci", "Batch", 5000.0)),
                        Triple("pembolong", "PEMBOLONG", Triple("Pembolong", "Pak", 250.0))
                    )
                    defaultJobs.forEachIndexed { index, (slug, code, details) ->
                        dao.upsertJobType(
                            JobTypeEntity(
                                id = "job_${slug}_$userId",
                                userId = userId,
                                code = code,
                                name = details.first,
                                unit = details.second,
                                ratePerUnit = details.third,
                                sortOrder = (index + 1).toLong()
                            )
                        )
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

    suspend fun saveStoreSettings(
        storeName: String,
        ownerName: String,
        phone: String,
        address: String,
        receiptFooter: String
    ): Result<Unit> {
        return try {
            require(storeName.isNotBlank()) { "Nama toko wajib diisi" }
            dao.upsertStoreSettings(
                StoreSettingsEntity(
                    id = "default_$userId",
                    userId = userId,
                    storeName = storeName.trim().take(100),
                    ownerName = ownerName.trim().take(100),
                    phone = phone.trim().take(40),
                    address = address.trim().take(300),
                    receiptFooter = receiptFooter.trim().take(300),
                    updatedAtMillis = System.currentTimeMillis()
                )
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun saveProduct(product: Product): Result<Unit> {
        return try {
            require(product.name.isNotBlank()) { "Nama produk wajib diisi" }
            require(product.unit.isNotBlank()) { "Satuan wajib diisi" }
            val id = product.id.ifBlank { UUID.randomUUID().toString() }
            dao.upsertProduct(
                ProductEntity(
                    id = id,
                    userId = userId,
                    name = product.name.trim().take(120),
                    sku = product.sku.trim().take(60),
                    category = product.category.trim().ifBlank { "Umum" }.take(60),
                    unit = product.unit.trim().take(30),
                    buyPrice = product.buyPrice.coerceAtLeast(0.0),
                    sellPrice = product.sellPrice.coerceAtLeast(0.0),
                    stock = product.stock.coerceAtLeast(0.0),
                    minStock = product.minStock.coerceAtLeast(0.0),
                    updatedAtMillis = System.currentTimeMillis()
                )
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteProduct(productId: String): Result<Unit> {
        return try {
            require(productId.isNotBlank())
            dao.deleteProduct(productId)
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun saveMerchant(merchant: Merchant): Result<Unit> {
        return try {
            require(merchant.name.isNotBlank()) { "Nama pedagang wajib diisi" }
            val id = merchant.id.ifBlank { UUID.randomUUID().toString() }
            dao.upsertMerchant(
                MerchantEntity(
                    id = id,
                    userId = userId,
                    name = merchant.name.trim().take(100),
                    phone = merchant.phone.trim().take(40),
                    routeArea = merchant.routeArea.trim().take(120),
                    address = merchant.address.trim().take(300),
                    notes = merchant.notes.trim().take(500)
                )
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteMerchant(merchantId: String): Result<Unit> {
        return try {
            require(merchantId.isNotBlank())
            dao.deleteMerchant(merchantId)
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun saveSupplier(supplier: Supplier): Result<Unit> {
        return try {
            require(supplier.name.isNotBlank()) { "Nama supplier wajib diisi" }
            val id = supplier.id.ifBlank { UUID.randomUUID().toString() }
            dao.upsertSupplier(
                SupplierEntity(
                    id = id,
                    userId = userId,
                    name = supplier.name.trim().take(100),
                    phone = supplier.phone.trim().take(40),
                    suppliedItems = supplier.suppliedItems.trim().take(200),
                    address = supplier.address.trim().take(300),
                    notes = supplier.notes.trim().take(500)
                )
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteSupplier(supplierId: String): Result<Unit> {
        return try {
            require(supplierId.isNotBlank())
            dao.deleteSupplier(supplierId)
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun saveEmployee(employee: Employee): Result<Unit> {
        return try {
            require(employee.name.isNotBlank()) { "Nama karyawan wajib diisi" }
            val id = employee.id.ifBlank { UUID.randomUUID().toString() }
            dao.upsertEmployee(
                EmployeeEntity(
                    id = id,
                    userId = userId,
                    name = employee.name.trim().take(100),
                    phone = employee.phone.trim().take(40),
                    defaultJobTypeId = employee.defaultJobTypeId.trim().take(1000),
                    defaultJobName = employee.defaultJobName.trim().take(500),
                    active = employee.active
                )
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteEmployee(employeeId: String): Result<Unit> {
        return try {
            require(employeeId.isNotBlank())
            dao.deleteEmployee(employeeId)
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun saveJobType(jobType: JobType): Result<Unit> {
        return try {
            require(jobType.name.isNotBlank()) { "Nama pekerjaan wajib diisi" }
            val id = jobType.id.ifBlank { UUID.randomUUID().toString() }
            val code = jobType.code.ifBlank {
                jobType.name.uppercase().replace(Regex("[^A-Z0-9_]"), "_").take(40)
            }
            dao.upsertJobType(
                JobTypeEntity(
                    id = id,
                    userId = userId,
                    code = code,
                    name = jobType.name.trim().take(80),
                    unit = jobType.unit.trim().ifBlank { "Satuan" }.take(30),
                    ratePerUnit = jobType.ratePerUnit.coerceAtLeast(0.0),
                    sortOrder = jobType.sortOrder.coerceAtLeast(0L)
                )
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createPayslip(
        payslip: Payslip,
        cashBox: CashBox?
    ): Result<Unit> {
        return try {
            require(payslip.employeeName.isNotBlank()) { "Karyawan wajib dipilih" }
            require(payslip.periodLabel.isNotBlank()) { "Periode gaji wajib diisi" }
            val nowMillis = System.currentTimeMillis()
            val workDate = if (payslip.workDateMillis > 0L) payslip.workDateMillis else nowMillis
            val salaryDate = if (payslip.salaryDateMillis > 0L) payslip.salaryDateMillis else workDate
            val cleanTotal = payslip.totalAmount.coerceAtLeast(0.0)
            val initialPaidAmount = when {
                payslip.paid -> cleanTotal
                payslip.paidAmount > 0.0 -> minOf(payslip.paidAmount, cleanTotal)
                else -> 0.0
            }
            val isFullyPaid = payslip.paid || (cleanTotal > 0.0 && initialPaidAmount >= cleanTotal - 0.001)
            val storedPaidAmount = if (isFullyPaid) cleanTotal else initialPaidAmount
            val paidMillis = if (storedPaidAmount > 0.0) {
                if (payslip.paidAtMillis > 0L) payslip.paidAtMillis else salaryDate
            } else 0L
            val payslipId = UUID.randomUUID().toString()

            db.withTransaction {
                dao.upsertPayslip(
                    PayslipEntity(
                        id = payslipId,
                        userId = userId,
                        employeeId = payslip.employeeId,
                        employeeName = payslip.employeeName.trim().take(100),
                        periodLabel = payslip.periodLabel.trim().take(80),
                        itemsJson = encodePayslipItems(payslip.items),
                        bonus = payslip.bonus.coerceAtLeast(0.0),
                        deduction = payslip.deduction.coerceAtLeast(0.0),
                        totalAmount = cleanTotal,
                        paidAmount = storedPaidAmount,
                        paid = isFullyPaid,
                        cashBoxId = if (storedPaidAmount > 0.0 && cashBox != null) cashBox.id else "",
                        notes = payslip.notes.trim().take(500),
                        workDateMillis = workDate,
                        salaryDateMillis = salaryDate,
                        paidAtMillis = paidMillis,
                        createdAtMillis = nowMillis
                    )
                )

                if (storedPaidAmount > 0.0 && cashBox != null) {
                    dao.adjustCashBoxBalance(cashBox.id, -storedPaidAmount)
                    dao.insertCashFlow(
                        CashFlowEntity(
                            id = UUID.randomUUID().toString(),
                            userId = userId,
                            flowType = "OUT",
                            category = "GAJI_KARYAWAN",
                            amount = storedPaidAmount,
                            cashBoxId = cashBox.id,
                            cashBoxName = cashBox.name,
                            description = "${if (isFullyPaid) "Gaji" else "Cicilan gaji"} ${payslip.employeeName} (${payslip.periodLabel})",
                            referenceId = payslipId,
                            transactionDateMillis = paidMillis
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

    suspend fun togglePayslipPaid(
        payslip: Payslip,
        markPaid: Boolean,
        cashBox: CashBox?,
        paidAtMillis: Long = System.currentTimeMillis()
    ): Result<Unit> {
        return try {
            db.withTransaction {
                val prevPaid = payslip.effectivePaidAmount()
                val effectivePaidAt = if (markPaid) paidAtMillis else 0L
                val effectiveBoxId = if (markPaid) (cashBox?.id ?: payslip.cashBoxId) else ""
                val newPaidAmount = if (markPaid) payslip.totalAmount else 0.0

                dao.upsertPayslip(
                    PayslipEntity(
                        id = payslip.id,
                        userId = userId,
                        employeeId = payslip.employeeId,
                        employeeName = payslip.employeeName,
                        periodLabel = payslip.periodLabel,
                        itemsJson = encodePayslipItems(payslip.items),
                        bonus = payslip.bonus,
                        deduction = payslip.deduction,
                        totalAmount = payslip.totalAmount,
                        paidAmount = newPaidAmount,
                        paid = markPaid,
                        cashBoxId = effectiveBoxId,
                        notes = payslip.notes,
                        workDateMillis = payslip.workDateMillis,
                        salaryDateMillis = payslip.salaryDateMillis,
                        paidAtMillis = effectivePaidAt,
                        createdAtMillis = payslip.createdAtMillis
                    )
                )

                val remainingDelta = (payslip.totalAmount - prevPaid).coerceAtLeast(0.0)
                if (markPaid && !payslip.paid && cashBox != null && remainingDelta > 0.0) {
                    dao.adjustCashBoxBalance(cashBox.id, -remainingDelta)
                    dao.insertCashFlow(
                        CashFlowEntity(
                            id = UUID.randomUUID().toString(),
                            userId = userId,
                            flowType = "OUT",
                            category = "GAJI_KARYAWAN",
                            amount = remainingDelta,
                            cashBoxId = cashBox.id,
                            cashBoxName = cashBox.name,
                            description = "Pelunasan gaji ${payslip.employeeName} (${payslip.periodLabel})",
                            referenceId = payslip.id,
                            transactionDateMillis = effectivePaidAt
                        )
                    )
                } else if (!markPaid && prevPaid > 0.0 && payslip.cashBoxId.isNotBlank()) {
                    val oldBox = dao.getCashBoxById(payslip.cashBoxId)
                    if (oldBox != null) {
                        dao.adjustCashBoxBalance(oldBox.id, prevPaid)
                        dao.insertCashFlow(
                            CashFlowEntity(
                                id = UUID.randomUUID().toString(),
                                userId = userId,
                                flowType = "IN",
                                category = "KOREKSI_GAJI",
                                amount = prevPaid,
                                cashBoxId = oldBox.id,
                                cashBoxName = oldBox.name,
                                description = "Pembatalan pembayaran gaji ${payslip.employeeName}",
                                referenceId = payslip.id,
                                transactionDateMillis = System.currentTimeMillis()
                            )
                        )
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

    suspend fun payPayslipInstallment(
        payslip: Payslip,
        paymentAmount: Double,
        cashBox: CashBox?,
        notes: String = "",
        paidAtMillis: Long = System.currentTimeMillis()
    ): Result<Unit> {
        return try {
            val remaining = payslip.remainingAmount()
            require(remaining > 0.0) { "Slip gaji ini sudah dibayar penuh" }
            require(paymentAmount > 0.0) { "Nominal cicilan harus lebih dari 0" }
            val actualPay = minOf(paymentAmount, remaining)
            val newPaid = payslip.effectivePaidAmount() + actualPay
            val newRem = (payslip.totalAmount - newPaid).coerceAtLeast(0.0)
            val isNowPaid = newRem <= 0.001
            val finalPaidAmount = if (isNowPaid) payslip.totalAmount else newPaid
            val payMillis = if (paidAtMillis > 0L) paidAtMillis else System.currentTimeMillis()
            val mergedNotes = notes.trim().take(500).ifBlank { payslip.notes }

            db.withTransaction {
                dao.upsertPayslip(
                    PayslipEntity(
                        id = payslip.id,
                        userId = userId,
                        employeeId = payslip.employeeId,
                        employeeName = payslip.employeeName,
                        periodLabel = payslip.periodLabel,
                        itemsJson = encodePayslipItems(payslip.items),
                        bonus = payslip.bonus,
                        deduction = payslip.deduction,
                        totalAmount = payslip.totalAmount,
                        paidAmount = finalPaidAmount,
                        paid = isNowPaid,
                        cashBoxId = cashBox?.id ?: payslip.cashBoxId,
                        notes = mergedNotes,
                        workDateMillis = payslip.workDateMillis,
                        salaryDateMillis = payslip.salaryDateMillis,
                        paidAtMillis = payMillis,
                        createdAtMillis = payslip.createdAtMillis
                    )
                )

                if (cashBox != null && actualPay > 0.0) {
                    dao.adjustCashBoxBalance(cashBox.id, -actualPay)
                    dao.insertCashFlow(
                        CashFlowEntity(
                            id = UUID.randomUUID().toString(),
                            userId = userId,
                            flowType = "OUT",
                            category = "GAJI_KARYAWAN",
                            amount = actualPay,
                            cashBoxId = cashBox.id,
                            cashBoxName = cashBox.name,
                            description = "${if (isNowPaid) "Pelunasan" else "Cicilan"} gaji ${payslip.employeeName} (${payslip.periodLabel})",
                            referenceId = payslip.id,
                            transactionDateMillis = payMillis
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

    suspend fun payPayslipsByInstallmentPool(
        payslips: List<Payslip>,
        totalPaymentAmount: Double,
        cashBox: CashBox?,
        notes: String = "",
        paidAtMillis: Long = System.currentTimeMillis()
    ): Result<Pair<Int, Double>> {
        return try {
            require(totalPaymentAmount > 0.0) { "Nominal cicilan harus lebih dari 0" }
            val unpaid = payslips
                .filter { !it.paid && it.remainingAmount() > 0.0 }
                .sortedBy {
                    it.workDateMillis.takeIf { m -> m > 0L }
                        ?: it.salaryDateMillis.takeIf { m -> m > 0L }
                        ?: it.createdAtMillis
                }
            require(unpaid.isNotEmpty()) { "Tidak ada slip gaji yang belum dibayar" }
            val payMillis = if (paidAtMillis > 0L) paidAtMillis else System.currentTimeMillis()

            var poolRemaining = totalPaymentAmount
            var updatedCount = 0
            var appliedTotal = 0.0

            db.withTransaction {
                for (slip in unpaid) {
                    if (poolRemaining <= 0.001) break
                    val slipRem = slip.remainingAmount()
                    if (slipRem <= 0.0) continue

                    val payForSlip = minOf(poolRemaining, slipRem)
                    val newPaid = slip.effectivePaidAmount() + payForSlip
                    val newRem = (slip.totalAmount - newPaid).coerceAtLeast(0.0)
                    val isNowPaid = newRem <= 0.001
                    val finalPaidAmount = if (isNowPaid) slip.totalAmount else newPaid
                    val mergedNotes = notes.trim().take(500).ifBlank { slip.notes }

                    dao.upsertPayslip(
                        PayslipEntity(
                            id = slip.id,
                            userId = userId,
                            employeeId = slip.employeeId,
                            employeeName = slip.employeeName,
                            periodLabel = slip.periodLabel,
                            itemsJson = encodePayslipItems(slip.items),
                            bonus = slip.bonus,
                            deduction = slip.deduction,
                            totalAmount = slip.totalAmount,
                            paidAmount = finalPaidAmount,
                            paid = isNowPaid,
                            cashBoxId = cashBox?.id ?: slip.cashBoxId,
                            notes = mergedNotes,
                            workDateMillis = slip.workDateMillis,
                            salaryDateMillis = slip.salaryDateMillis,
                            paidAtMillis = payMillis,
                            createdAtMillis = slip.createdAtMillis
                        )
                    )
                    if (cashBox != null && payForSlip > 0.0) {
                        dao.adjustCashBoxBalance(cashBox.id, -payForSlip)
                        dao.insertCashFlow(
                            CashFlowEntity(
                                id = UUID.randomUUID().toString(),
                                userId = userId,
                                flowType = "OUT",
                                category = "GAJI_KARYAWAN",
                                amount = payForSlip,
                                cashBoxId = cashBox.id,
                                cashBoxName = cashBox.name,
                                description = "${if (isNowPaid) "Pelunasan" else "Cicilan"} gaji ${slip.employeeName} (${slip.periodLabel})",
                                referenceId = slip.id,
                                transactionDateMillis = payMillis
                            )
                        )
                    }
                    poolRemaining -= payForSlip
                    appliedTotal += payForSlip
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

    suspend fun payAllPayslips(
        payslips: List<Payslip>,
        cashBox: CashBox?,
        paidAtMillis: Long = System.currentTimeMillis()
    ): Result<Int> {
        return try {
            val unpaid = payslips.filter { !it.paid }
            require(unpaid.isNotEmpty()) { "Tidak ada slip gaji yang belum dibayar" }
            val effectivePaidAt = if (paidAtMillis > 0L) paidAtMillis else System.currentTimeMillis()

            db.withTransaction {
                unpaid.forEach { slip ->
                    val remainingToPay = slip.remainingAmount()
                    val effectiveBoxId = cashBox?.id ?: slip.cashBoxId
                    dao.upsertPayslip(
                        PayslipEntity(
                            id = slip.id,
                            userId = userId,
                            employeeId = slip.employeeId,
                            employeeName = slip.employeeName,
                            periodLabel = slip.periodLabel,
                            itemsJson = encodePayslipItems(slip.items),
                            bonus = slip.bonus,
                            deduction = slip.deduction,
                            totalAmount = slip.totalAmount,
                            paidAmount = slip.totalAmount,
                            paid = true,
                            cashBoxId = effectiveBoxId,
                            notes = slip.notes,
                            workDateMillis = slip.workDateMillis,
                            salaryDateMillis = slip.salaryDateMillis,
                            paidAtMillis = effectivePaidAt,
                            createdAtMillis = slip.createdAtMillis
                        )
                    )
                    if (cashBox != null && remainingToPay > 0.0) {
                        dao.adjustCashBoxBalance(cashBox.id, -remainingToPay)
                        dao.insertCashFlow(
                            CashFlowEntity(
                                id = UUID.randomUUID().toString(),
                                userId = userId,
                                flowType = "OUT",
                                category = "GAJI_KARYAWAN",
                                amount = remainingToPay,
                                cashBoxId = cashBox.id,
                                cashBoxName = cashBox.name,
                                description = "Pelunasan gaji ${slip.employeeName} (${slip.periodLabel})",
                                referenceId = slip.id,
                                transactionDateMillis = effectivePaidAt
                            )
                        )
                    }
                }
            }
            Result.success(unpaid.size)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deletePayslip(payslipId: String): Result<Unit> {
        return try {
            require(payslipId.isNotBlank())
            db.withTransaction {
                val relatedFlows = dao.getCashFlowsByReference(payslipId)
                relatedFlows.forEach { flow ->
                    if (flow.cashBoxId.isNotBlank() && dao.getCashBoxById(flow.cashBoxId) != null) {
                        val reverseDelta = if (flow.flowType == "IN") -flow.amount else flow.amount
                        dao.adjustCashBoxBalance(flow.cashBoxId, reverseDelta)
                    }
                }
                dao.deleteCashFlowsByReference(payslipId)
                dao.deletePayslip(payslipId)
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
