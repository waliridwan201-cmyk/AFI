package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AfiDao {
    // Local Users
    @Query("SELECT * FROM local_users WHERE username = :username LIMIT 1")
    suspend fun getUserByUsername(username: String): LocalUserEntity?

    @Query("SELECT * FROM local_users WHERE id = :userId LIMIT 1")
    suspend fun getUserById(userId: String): LocalUserEntity?

    @Query("SELECT COUNT(*) FROM local_users")
    suspend fun countUsers(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: LocalUserEntity)

    // Store Settings
    @Query("SELECT * FROM store_settings WHERE userId = :userId LIMIT 1")
    fun observeStoreSettings(userId: String): Flow<StoreSettingsEntity?>

    @Query("SELECT * FROM store_settings WHERE userId = :userId LIMIT 1")
    suspend fun getStoreSettings(userId: String): StoreSettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertStoreSettings(settings: StoreSettingsEntity)

    // Products
    @Query("SELECT * FROM products WHERE userId = :userId ORDER BY name COLLATE NOCASE ASC")
    fun observeProducts(userId: String): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE id = :id LIMIT 1")
    suspend fun getProductById(id: String): ProductEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProduct(product: ProductEntity)

    @Query("DELETE FROM products WHERE id = :id")
    suspend fun deleteProduct(id: String)

    // Merchants
    @Query("SELECT * FROM merchants WHERE userId = :userId ORDER BY name COLLATE NOCASE ASC")
    fun observeMerchants(userId: String): Flow<List<MerchantEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMerchant(merchant: MerchantEntity)

    @Query("DELETE FROM merchants WHERE id = :id")
    suspend fun deleteMerchant(id: String)

    // Suppliers
    @Query("SELECT * FROM suppliers WHERE userId = :userId ORDER BY name COLLATE NOCASE ASC")
    fun observeSuppliers(userId: String): Flow<List<SupplierEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSupplier(supplier: SupplierEntity)

    @Query("DELETE FROM suppliers WHERE id = :id")
    suspend fun deleteSupplier(id: String)

    // Employees
    @Query("SELECT * FROM employees WHERE userId = :userId ORDER BY name COLLATE NOCASE ASC")
    fun observeEmployees(userId: String): Flow<List<EmployeeEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertEmployee(employee: EmployeeEntity)

    @Query("DELETE FROM employees WHERE id = :id")
    suspend fun deleteEmployee(id: String)

    // Job Types
    @Query("SELECT * FROM job_types WHERE userId = :userId ORDER BY sortOrder ASC")
    fun observeJobTypes(userId: String): Flow<List<JobTypeEntity>>

    @Query("SELECT COUNT(*) FROM job_types WHERE userId = :userId")
    suspend fun countJobTypes(userId: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertJobType(jobType: JobTypeEntity)

    // Payslips
    @Query("SELECT * FROM payslips WHERE userId = :userId ORDER BY workDateMillis DESC, salaryDateMillis DESC, createdAtMillis DESC")
    fun observePayslips(userId: String): Flow<List<PayslipEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPayslip(payslip: PayslipEntity)

    @Query("DELETE FROM payslips WHERE id = :id")
    suspend fun deletePayslip(id: String)

    // Cash Boxes
    @Query("SELECT * FROM cash_boxes WHERE userId = :userId ORDER BY primaryBox DESC, name ASC")
    fun observeCashBoxes(userId: String): Flow<List<CashBoxEntity>>

    @Query("SELECT * FROM cash_boxes WHERE id = :id LIMIT 1")
    suspend fun getCashBoxById(id: String): CashBoxEntity?

    @Query("SELECT COUNT(*) FROM cash_boxes WHERE userId = :userId")
    suspend fun countCashBoxes(userId: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCashBox(cashBox: CashBoxEntity)

    @Query("UPDATE cash_boxes SET balance = balance + :delta WHERE id = :cashBoxId")
    suspend fun adjustCashBoxBalance(cashBoxId: String, delta: Double)

    @Query("UPDATE cash_boxes SET balance = 0.0 WHERE userId = :userId")
    suspend fun resetAllCashBoxBalances(userId: String)

    @Query("DELETE FROM cash_boxes WHERE id = :cashBoxId")
    suspend fun deleteCashBox(cashBoxId: String)

    // Cash Flows
    @Query("SELECT * FROM cash_flows WHERE userId = :userId ORDER BY transactionDateMillis DESC")
    fun observeCashFlows(userId: String): Flow<List<CashFlowEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCashFlow(flow: CashFlowEntity)

    @Query("UPDATE cash_flows SET transactionDateMillis = :newMillis WHERE referenceId = :referenceId")
    suspend fun updateCashFlowDateByReference(referenceId: String, newMillis: Long)

    @Query("SELECT * FROM cash_flows WHERE referenceId = :referenceId")
    suspend fun getCashFlowsByReference(referenceId: String): List<CashFlowEntity>

    @Query("DELETE FROM cash_flows WHERE id = :flowId")
    suspend fun deleteCashFlow(flowId: String)

    @Query("DELETE FROM cash_flows WHERE referenceId = :referenceId")
    suspend fun deleteCashFlowsByReference(referenceId: String)

    @Query("DELETE FROM cash_flows WHERE userId = :userId")
    suspend fun deleteAllCashFlows(userId: String)

    // Sales
    @Query("SELECT * FROM sales WHERE userId = :userId ORDER BY transactionDateMillis DESC")
    fun observeSales(userId: String): Flow<List<SaleEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSale(sale: SaleEntity)

    @Query("UPDATE sales SET transactionDateMillis = :newMillis WHERE id = :saleId")
    suspend fun updateSaleDate(saleId: String, newMillis: Long)

    @Query("DELETE FROM sales WHERE id = :saleId")
    suspend fun deleteSale(saleId: String)

    @Query("DELETE FROM sales WHERE userId = :userId")
    suspend fun deleteAllSales(userId: String)

    // Purchases
    @Query("SELECT * FROM purchases WHERE userId = :userId ORDER BY transactionDateMillis DESC")
    fun observePurchases(userId: String): Flow<List<PurchaseEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPurchase(purchase: PurchaseEntity)

    @Query("UPDATE purchases SET transactionDateMillis = :newMillis WHERE id = :purchaseId")
    suspend fun updatePurchaseDate(purchaseId: String, newMillis: Long)

    @Query("DELETE FROM purchases WHERE id = :purchaseId")
    suspend fun deletePurchase(purchaseId: String)

    @Query("DELETE FROM purchases WHERE userId = :userId")
    suspend fun deleteAllPurchases(userId: String)

    // Purchase Returns
    @Query("SELECT * FROM purchase_returns WHERE userId = :userId ORDER BY transactionDateMillis DESC")
    fun observePurchaseReturns(userId: String): Flow<List<PurchaseReturnEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPurchaseReturn(ret: PurchaseReturnEntity)

    @Query("DELETE FROM purchase_returns WHERE userId = :userId")
    suspend fun deleteAllPurchaseReturns(userId: String)

    // Expenses
    @Query("SELECT * FROM expenses WHERE userId = :userId ORDER BY transactionDateMillis DESC")
    fun observeExpenses(userId: String): Flow<List<ExpenseEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: ExpenseEntity)

    @Query("UPDATE expenses SET transactionDateMillis = :newMillis WHERE id = :expenseId")
    suspend fun updateExpenseDate(expenseId: String, newMillis: Long)

    @Query("DELETE FROM expenses WHERE id = :expenseId")
    suspend fun deleteExpense(expenseId: String)

    @Query("DELETE FROM expenses WHERE userId = :userId")
    suspend fun deleteAllExpenses(userId: String)

    // Debt Records
    @Query("SELECT * FROM debt_records WHERE userId = :userId ORDER BY transactionDateMillis DESC")
    fun observeDebtRecords(userId: String): Flow<List<DebtRecordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDebtRecord(debt: DebtRecordEntity)

    @Query("UPDATE debt_records SET transactionDateMillis = :newMillis WHERE referenceId = :referenceId")
    suspend fun updateDebtDateByReference(referenceId: String, newMillis: Long)

    @Query("UPDATE debt_records SET paidAtMillis = :paidAtMillis WHERE id = :debtId")
    suspend fun updateDebtPaidAt(debtId: String, paidAtMillis: Long)

    @Query("DELETE FROM debt_records WHERE id = :debtId")
    suspend fun deleteDebtRecord(debtId: String)

    @Query("DELETE FROM debt_records WHERE userId = :userId")
    suspend fun deleteAllDebtRecords(userId: String)

    @Query("DELETE FROM payslips WHERE userId = :userId")
    suspend fun deleteAllPayslips(userId: String)
}
