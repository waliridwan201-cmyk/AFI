package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
import com.example.data.model.PurchaseItem
import com.example.data.model.PurchaseReturn
import com.example.data.model.Sale
import com.example.data.model.SaleItem
import com.example.data.model.StoreSettings
import com.example.data.model.Supplier
import com.example.data.model.UiState
import com.example.data.repository.AfiRepository
import com.example.data.repository.AfiTransactionRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AfiViewModel(
    private val repo: AfiRepository,
    private val txRepo: AfiTransactionRepository,
    val currentUserId: String
) : ViewModel() {

    private val _events = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val events: SharedFlow<String> = _events.asSharedFlow()

    val storeSettings: StateFlow<UiState<StoreSettings>> =
        repo.observeStoreSettings().toUiState("store_settings")

    val products: StateFlow<UiState<List<Product>>> =
        repo.observeProducts().toUiState("products")

    val merchants: StateFlow<UiState<List<Merchant>>> =
        repo.observeMerchants().toUiState("merchants")

    val suppliers: StateFlow<UiState<List<Supplier>>> =
        repo.observeSuppliers().toUiState("suppliers")

    val employees: StateFlow<UiState<List<Employee>>> =
        repo.observeEmployees().toUiState("employees")

    val jobTypes: StateFlow<UiState<List<JobType>>> =
        repo.observeJobTypes().toUiState("job_types")

    val payslips: StateFlow<UiState<List<Payslip>>> =
        repo.observePayslips().toUiState("payslips")

    val cashBoxes: StateFlow<UiState<List<CashBox>>> =
        repo.observeCashBoxes().toUiState("cash_boxes")

    val cashFlows: StateFlow<UiState<List<CashFlow>>> =
        repo.observeCashFlows().toUiState("cash_flows")

    val sales: StateFlow<UiState<List<Sale>>> =
        repo.observeSales().toUiState("sales")

    val purchases: StateFlow<UiState<List<Purchase>>> =
        repo.observePurchases().toUiState("purchases")

    val purchaseReturns: StateFlow<UiState<List<PurchaseReturn>>> =
        repo.observePurchaseReturns().toUiState("purchase_returns")

    val expenses: StateFlow<UiState<List<Expense>>> =
        repo.observeExpenses().toUiState("expenses")

    val debtRecords: StateFlow<UiState<List<DebtRecord>>> =
        repo.observeDebtRecords().toUiState("debt_records")

    init {
        viewModelScope.launch {
            repo.ensureInitialDefaults().onFailure { err ->
                Log.w(TAG, "Gagal inisialisasi default awal", err)
            }
        }
    }

    private fun <T> Flow<T>.toUiState(label: String): StateFlow<UiState<T>> {
        return map<T, UiState<T>> { UiState.Success(it) }
            .catch { error ->
                if (error is CancellationException) throw error
                Log.w(TAG, "Error observing $label", error)
                emit(UiState.Error(error.localizedMessage ?: "Gagal memuat $label"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000L),
                initialValue = UiState.Loading
            )
    }

    private fun runAction(successMessage: String, block: suspend () -> Result<Unit>) {
        viewModelScope.launch {
            block()
                .onSuccess { _events.tryEmit(successMessage) }
                .onFailure { err ->
                    _events.tryEmit(err.localizedMessage ?: "Terjadi kesalahan saat menyimpan data")
                }
        }
    }

    private fun <T> runActionWithResult(
        successMessage: String,
        onSuccessValue: (T) -> Unit,
        block: suspend () -> Result<T>
    ) {
        viewModelScope.launch {
            block()
                .onSuccess { value ->
                    _events.tryEmit(successMessage)
                    onSuccessValue(value)
                }
                .onFailure { err ->
                    _events.tryEmit(err.localizedMessage ?: "Terjadi kesalahan saat menyimpan data")
                }
        }
    }

    fun saveStoreSettings(
        storeName: String,
        ownerName: String,
        phone: String,
        address: String,
        receiptFooter: String
    ) = runAction("Pengaturan toko berhasil disimpan") {
        repo.saveStoreSettings(storeName, ownerName, phone, address, receiptFooter)
    }

    fun saveProduct(product: Product) = runAction(
        if (product.id.isBlank()) "Produk baru ditambahkan" else "Data produk diperbarui"
    ) {
        repo.saveProduct(product)
    }

    fun deleteProduct(productId: String) = runAction("Produk dihapus") {
        repo.deleteProduct(productId)
    }

    fun saveMerchant(merchant: Merchant) = runAction(
        if (merchant.id.isBlank()) "Pedagang keliling ditambahkan" else "Data pedagang diperbarui"
    ) {
        repo.saveMerchant(merchant)
    }

    fun deleteMerchant(merchantId: String) = runAction("Data pedagang dihapus") {
        repo.deleteMerchant(merchantId)
    }

    fun saveSupplier(supplier: Supplier) = runAction(
        if (supplier.id.isBlank()) "Data supplier ditambahkan" else "Data supplier diperbarui"
    ) {
        repo.saveSupplier(supplier)
    }

    fun deleteSupplier(supplierId: String) = runAction("Data supplier dihapus") {
        repo.deleteSupplier(supplierId)
    }

    fun saveEmployee(employee: Employee) = runAction(
        if (employee.id.isBlank()) "Karyawan pabrik ditambahkan" else "Data karyawan diperbarui"
    ) {
        repo.saveEmployee(employee)
    }

    fun deleteEmployee(employeeId: String) = runAction("Karyawan dihapus") {
        repo.deleteEmployee(employeeId)
    }

    fun saveJobType(jobType: JobType) = runAction("Tarif & jenis pekerjaan disimpan") {
        repo.saveJobType(jobType)
    }

    fun createPayslip(payslip: Payslip, cashBox: CashBox?) = runAction("Slip gaji berhasil dibuat") {
        repo.createPayslip(payslip, cashBox)
    }

    fun togglePayslipPaid(
        payslip: Payslip,
        markPaid: Boolean,
        cashBox: CashBox?,
        paidAtMillis: Long = System.currentTimeMillis()
    ) = runAction(
        if (markPaid) "Slip gaji ditandai sudah dibayar" else "Status slip gaji diubah menjadi belum dibayar"
    ) {
        repo.togglePayslipPaid(payslip, markPaid, cashBox, paidAtMillis)
    }

    fun payPayslipInstallment(
        payslip: Payslip,
        paymentAmount: Double,
        cashBox: CashBox?,
        notes: String = "",
        paidAtMillis: Long = System.currentTimeMillis()
    ) = runAction("Cicilan gaji berhasil dicatat") {
        repo.payPayslipInstallment(payslip, paymentAmount, cashBox, notes, paidAtMillis)
    }

    fun payPayslipsByInstallmentPool(
        payslips: List<Payslip>,
        totalPaymentAmount: Double,
        cashBox: CashBox?,
        notes: String = "",
        paidAtMillis: Long = System.currentTimeMillis()
    ) {
        viewModelScope.launch {
            repo.payPayslipsByInstallmentPool(payslips, totalPaymentAmount, cashBox, notes, paidAtMillis)
                .onSuccess { (count, applied) ->
                    _events.tryEmit("Cicilan Rp ${applied.toLong()} dicatat untuk $count slip gaji")
                }
                .onFailure { err ->
                    _events.tryEmit(err.localizedMessage ?: "Gagal mencatat cicilan gaji")
                }
        }
    }

    fun payAllPayslips(
        payslips: List<Payslip>,
        cashBox: CashBox?,
        paidAtMillis: Long = System.currentTimeMillis()
    ) {
        viewModelScope.launch {
            repo.payAllPayslips(payslips, cashBox, paidAtMillis)
                .onSuccess { count -> _events.tryEmit("$count slip gaji berhasil dibayar") }
                .onFailure { err ->
                    _events.tryEmit(err.localizedMessage ?: "Gagal membayar semua slip gaji")
                }
        }
    }

    fun deletePayslip(payslipId: String) = runAction("Slip gaji dihapus") {
        repo.deletePayslip(payslipId)
    }

    fun createSale(
        customerName: String,
        merchantId: String,
        items: List<SaleItem>,
        productsSnapshot: List<Product>,
        paidAmountInput: Double,
        isPiutang: Boolean,
        cashBox: CashBox?,
        notes: String,
        transactionDateMillis: Long = System.currentTimeMillis(),
        onCreated: (Sale) -> Unit = {}
    ) = runActionWithResult("Transaksi penjualan berhasil dicatat", onCreated) {
        txRepo.createSale(
            customerName, merchantId, items, productsSnapshot,
            paidAmountInput, isPiutang, cashBox, notes, transactionDateMillis
        )
    }

    fun updateSaleDate(saleId: String, newMillis: Long) =
        runAction("Waktu transaksi penjualan diperbarui") {
            txRepo.updateSaleDate(saleId, newMillis)
        }

    fun createPurchase(
        supplierName: String,
        items: List<PurchaseItem>,
        productsSnapshot: List<Product>,
        paidAmountInput: Double,
        isHutang: Boolean,
        cashBox: CashBox?,
        notes: String,
        transactionDateMillis: Long = System.currentTimeMillis(),
        onCreated: (Purchase) -> Unit = {}
    ) = runActionWithResult("Transaksi pembelian berhasil dicatat", onCreated) {
        txRepo.createPurchase(
            supplierName, items, productsSnapshot,
            paidAmountInput, isHutang, cashBox, notes, transactionDateMillis
        )
    }

    fun updatePurchaseDate(purchaseId: String, newMillis: Long) =
        runAction("Waktu transaksi pembelian diperbarui") {
            txRepo.updatePurchaseDate(purchaseId, newMillis)
        }

    fun createPurchaseReturn(
        supplierName: String,
        product: Product,
        quantity: Double,
        unitPrice: Double,
        refundMethod: String,
        cashBox: CashBox?,
        reason: String,
        matchingHutang: DebtRecord?,
        transactionDateMillis: Long = System.currentTimeMillis(),
        onCreated: (PurchaseReturn) -> Unit = {}
    ) = runActionWithResult("Retur pembelian berhasil dicatat", onCreated) {
        txRepo.createPurchaseReturn(
            supplierName, product, quantity, unitPrice,
            refundMethod, cashBox, reason, matchingHutang, transactionDateMillis
        )
    }

    fun createExpense(
        title: String,
        category: String,
        amount: Double,
        cashBox: CashBox,
        notes: String,
        transactionDateMillis: Long = System.currentTimeMillis(),
        onCreated: (Expense) -> Unit = {}
    ) = runActionWithResult("Pengeluaran berhasil dicatat", onCreated) {
        txRepo.createExpense(title, category, amount, cashBox, notes, transactionDateMillis)
    }

    fun updateExpenseDate(expenseId: String, newMillis: Long) =
        runAction("Waktu transaksi pengeluaran diperbarui") {
            txRepo.updateExpenseDate(expenseId, newMillis)
        }

    fun payDebtRecord(
        debt: DebtRecord,
        paymentAmount: Double,
        cashBox: CashBox,
        notes: String,
        paidAtMillis: Long = System.currentTimeMillis()
    ) = runAction("Pembayaran berhasil dicatat") {
        txRepo.payDebtRecord(debt, paymentAmount, cashBox, notes, paidAtMillis)
    }

    fun updateDebtPaidDate(debtId: String, newPaidMillis: Long) =
        runAction("Waktu & tanggal bayar diperbarui") {
            txRepo.updateDebtPaidDate(debtId, newPaidMillis)
        }

    fun createManualDebt(
        debtType: String,
        partyId: String,
        partyName: String,
        totalAmount: Double,
        notes: String,
        transactionDateMillis: Long = System.currentTimeMillis()
    ) = runAction("Catatan berhasil ditambahkan") {
        txRepo.createManualDebt(debtType, partyId, partyName, totalAmount, notes, transactionDateMillis)
    }

    fun settleAllDebts(
        debts: List<DebtRecord>,
        cashBox: CashBox,
        notes: String,
        paidAtMillis: Long = System.currentTimeMillis()
    ) {
        viewModelScope.launch {
            txRepo.settleAllDebts(debts, cashBox, notes, paidAtMillis)
                .onSuccess { count -> _events.tryEmit("$count catatan berhasil dilunasi") }
                .onFailure { err ->
                    _events.tryEmit(err.localizedMessage ?: "Gagal melunasi semua catatan")
                }
        }
    }

    fun payDebtsByInstallmentPool(
        debts: List<DebtRecord>,
        totalPaymentAmount: Double,
        cashBox: CashBox,
        notes: String,
        paidAtMillis: Long = System.currentTimeMillis()
    ) {
        viewModelScope.launch {
            txRepo.payDebtsByInstallmentPool(debts, totalPaymentAmount, cashBox, notes, paidAtMillis)
                .onSuccess { (count, applied) ->
                    _events.tryEmit("Cicilan Rp ${applied.toLong()} dicatat untuk $count tagihan")
                }
                .onFailure { err ->
                    _events.tryEmit(err.localizedMessage ?: "Gagal mencatat cicilan")
                }
        }
    }

    fun deleteDebtRecord(debt: DebtRecord) =
        runAction("Transaksi ${if (debt.debtType == "HUTANG") "hutang" else "piutang"} & riwayat keuangan terkait dihapus") {
            txRepo.deleteDebtRecord(debt)
        }

    fun createCashBox(name: String, description: String, initialBalance: Double) =
        runAction("Cash Box baru dibuat") {
            txRepo.createCashBox(name, description, initialBalance)
        }

    fun deleteCashBox(cashBoxId: String) =
        runAction("Cash Box dihapus") {
            txRepo.deleteCashBox(cashBoxId)
        }

    fun adjustCashBoxBalance(
        cashBox: CashBox,
        isAddition: Boolean,
        amount: Double,
        description: String,
        transactionDateMillis: Long = System.currentTimeMillis()
    ) = runAction(
        if (isAddition) "Saldo Cash Box berhasil ditambah" else "Saldo Cash Box berhasil dikurangi"
    ) {
        txRepo.adjustCashBoxBalance(cashBox, isAddition, amount, description, transactionDateMillis)
    }

    fun transferCashBoxBalance(
        fromBox: CashBox,
        toBox: CashBox,
        amount: Double,
        description: String,
        transactionDateMillis: Long = System.currentTimeMillis()
    ) = runAction("Saldo berhasil dipindahkan ke ${toBox.name}") {
        txRepo.transferCashBoxBalance(fromBox, toBox, amount, description, transactionDateMillis)
    }

    fun deleteCashFlow(flow: CashFlow) =
        runAction("Riwayat arus uang dihapus") {
            txRepo.deleteCashFlowHistory(flow)
        }

    fun resetTransactionData(includePayslips: Boolean = false) =
        runAction("Data transaksi berhasil direset") {
            txRepo.resetTransactionData(includePayslips)
        }

    private companion object {
        const val TAG = "AfiViewModel"
    }
}
