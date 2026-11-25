package com.example.ui

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.Contacts
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.data.local.AfiDatabase
import com.example.data.model.LocalUser
import com.example.data.repository.AfiRepository
import com.example.data.repository.AfiTransactionRepository
import com.example.ui.screens.CashAndFlowScreen
import com.example.ui.screens.ContactsAndPayrollScreen
import com.example.ui.screens.DailyStockScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.DebtsScreen
import com.example.ui.screens.EmployeeSalaryScreen
import com.example.ui.screens.ExpensesScreen
import com.example.ui.screens.LossSalesScreen
import com.example.ui.screens.ProductsScreen
import com.example.ui.screens.PurchaseReturnsScreen
import com.example.ui.screens.PurchasesScreen
import com.example.ui.screens.ReportsScreen
import com.example.ui.screens.SalesScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.TransactionsHubScreen
import com.example.ui.viewmodel.AfiViewModel

object AfiRoutes {
    const val HOME = "home"
    const val TRANSACTIONS = "transactions"
    const val CONTACTS = "contacts"
    const val FINANCE = "finance"
    const val REPORTS = "reports"
    const val SALES = "sales"
    const val PURCHASES = "purchases"
    const val PURCHASE_RETURNS = "purchase_returns"
    const val LOSS_SALES = "loss_sales"
    const val EXPENSES = "expenses"
    const val DEBTS_HUTANG = "debts_hutang"
    const val DEBTS_PIUTANG = "debts_piutang"
    const val PRODUCTS = "products"
    const val DAILY_STOCK = "daily_stock"
    const val EMPLOYEE_SALARY = "employee_salary"
    const val SETTINGS = "settings"
}

private data class TopNavItem(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val tag: String
)

@Composable
fun AfiMainApp(
    currentUser: LocalUser,
    onSignOut: () -> Unit
) {
    val viewModel: AfiViewModel = viewModel(
        key = currentUser.id,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val db = AfiDatabase.getInstance(app)
                val repo = AfiRepository(
                    db = db,
                    userId = currentUser.id,
                    defaultOwnerName = currentUser.displayName
                )
                val txRepo = AfiTransactionRepository(repo)
                AfiViewModel(repo, txRepo, currentUser.id)
            }
        }
    )

    val storeSettings by viewModel.storeSettings.collectAsStateWithLifecycle()
    val products by viewModel.products.collectAsStateWithLifecycle()
    val merchants by viewModel.merchants.collectAsStateWithLifecycle()
    val suppliers by viewModel.suppliers.collectAsStateWithLifecycle()
    val employees by viewModel.employees.collectAsStateWithLifecycle()
    val jobTypes by viewModel.jobTypes.collectAsStateWithLifecycle()
    val payslips by viewModel.payslips.collectAsStateWithLifecycle()
    val cashBoxes by viewModel.cashBoxes.collectAsStateWithLifecycle()
    val cashFlows by viewModel.cashFlows.collectAsStateWithLifecycle()
    val sales by viewModel.sales.collectAsStateWithLifecycle()
    val purchases by viewModel.purchases.collectAsStateWithLifecycle()
    val purchaseReturns by viewModel.purchaseReturns.collectAsStateWithLifecycle()
    val expenses by viewModel.expenses.collectAsStateWithLifecycle()
    val debtRecords by viewModel.debtRecords.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(viewModel) {
        viewModel.events.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    var contactsInitialTab by remember { mutableIntStateOf(0) }
    var contactsTabNonce by remember { mutableIntStateOf(0) }

    val topItems = listOf(
        TopNavItem(AfiRoutes.HOME, "Beranda", Icons.Filled.Home, Icons.Outlined.Home, "nav_home"),
        TopNavItem(AfiRoutes.TRANSACTIONS, "Transaksi", Icons.AutoMirrored.Filled.ReceiptLong, Icons.AutoMirrored.Outlined.ReceiptLong, "nav_transactions"),
        TopNavItem(AfiRoutes.CONTACTS, "Kontak", Icons.Filled.Contacts, Icons.Outlined.Contacts, "nav_contacts"),
        TopNavItem(AfiRoutes.FINANCE, "Keuangan", Icons.Filled.AccountBalanceWallet, Icons.Outlined.AccountBalanceWallet, "nav_finance"),
        TopNavItem(AfiRoutes.REPORTS, "Laporan", Icons.Filled.Assessment, Icons.Outlined.Assessment, "nav_reports")
    )

    val isTopLevel = topItems.any { it.route == currentRoute }

    val navigateTo: (String) -> Unit = { target ->
        if (target == "tab_contacts_payslip") {
            contactsInitialTab = 4
            contactsTabNonce++
            navController.navigate(AfiRoutes.CONTACTS) {
                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        } else if (topItems.any { it.route == target }) {
            if (target == AfiRoutes.CONTACTS) {
                contactsInitialTab = 0
                contactsTabNonce++
            }
            navController.navigate(target) {
                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        } else {
            navController.navigate(target) { launchSingleTop = true }
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 600.dp

        Scaffold(
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            contentWindowInsets = WindowInsets.safeDrawing,
            bottomBar = {
                if (!useRail && isTopLevel) {
                    NavigationBar {
                        topItems.forEach { item ->
                            val selected = currentRoute == item.route
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateTo(item.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                        contentDescription = item.label
                                    )
                                },
                                label = { Text(item.label) },
                                modifier = Modifier.testTag(item.tag)
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useRail && isTopLevel) {
                    NavigationRail {
                        topItems.forEach { item ->
                            val selected = currentRoute == item.route
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateTo(item.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                        contentDescription = item.label
                                    )
                                },
                                label = { Text(item.label) },
                                modifier = Modifier.testTag(item.tag)
                            )
                        }
                    }
                }

                NavHost(
                    navController = navController,
                    startDestination = AfiRoutes.HOME,
                    modifier = Modifier.weight(1f)
                ) {
                    composable(AfiRoutes.HOME) {
                        DashboardScreen(
                            storeSettingsState = storeSettings,
                            cashBoxesState = cashBoxes,
                            salesState = sales,
                            expensesState = expenses,
                            debtsState = debtRecords,
                            productsState = products,
                            payslipsState = payslips,
                            onNavigate = navigateTo
                        )
                    }
                    composable(AfiRoutes.TRANSACTIONS) {
                        TransactionsHubScreen(
                            salesState = sales,
                            purchasesState = purchases,
                            returnsState = purchaseReturns,
                            expensesState = expenses,
                            debtsState = debtRecords,
                            productsState = products,
                            payslipsState = payslips,
                            onNavigate = navigateTo
                        )
                    }
                    composable(AfiRoutes.CONTACTS) {
                        ContactsAndPayrollScreen(
                            initialTab = contactsInitialTab,
                            tabRequestNonce = contactsTabNonce,
                            merchantsState = merchants,
                            suppliersState = suppliers,
                            employeesState = employees,
                            jobTypesState = jobTypes,
                            payslipsState = payslips,
                            debtsState = debtRecords,
                            cashBoxesState = cashBoxes,
                            storeSettingsState = storeSettings,
                            onSaveMerchant = viewModel::saveMerchant,
                            onDeleteMerchant = viewModel::deleteMerchant,
                            onSaveSupplier = viewModel::saveSupplier,
                            onDeleteSupplier = viewModel::deleteSupplier,
                            onSaveEmployee = viewModel::saveEmployee,
                            onDeleteEmployee = viewModel::deleteEmployee,
                            onSaveJobType = viewModel::saveJobType,
                            onCreatePayslip = viewModel::createPayslip,
                            onTogglePayslipPaid = viewModel::togglePayslipPaid,
                            onPayInstallmentSingle = viewModel::payPayslipInstallment,
                            onPayInstallmentPool = viewModel::payPayslipsByInstallmentPool,
                            onPayAllPayslips = viewModel::payAllPayslips,
                            onDeletePayslip = viewModel::deletePayslip
                        )
                    }
                    composable(AfiRoutes.FINANCE) {
                        CashAndFlowScreen(
                            cashBoxesState = cashBoxes,
                            cashFlowsState = cashFlows,
                            onCreateCashBox = viewModel::createCashBox,
                            onDeleteCashBox = viewModel::deleteCashBox,
                            onAdjustBalance = viewModel::adjustCashBoxBalance,
                            onTransferBalance = viewModel::transferCashBoxBalance,
                            onDeleteCashFlow = viewModel::deleteCashFlow
                        )
                    }
                    composable(AfiRoutes.REPORTS) {
                        ReportsScreen(
                            salesState = sales,
                            purchasesState = purchases,
                            expensesState = expenses,
                            payslipsState = payslips,
                            cashFlowsState = cashFlows,
                            storeSettingsState = storeSettings
                        )
                    }
                    composable(AfiRoutes.PRODUCTS) {
                        ProductsScreen(
                            productsState = products,
                            onSaveProduct = viewModel::saveProduct,
                            onDeleteProduct = viewModel::deleteProduct,
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable(AfiRoutes.DAILY_STOCK) {
                        DailyStockScreen(
                            productsState = products,
                            salesState = sales,
                            purchasesState = purchases,
                            returnsState = purchaseReturns,
                            storeSettingsState = storeSettings,
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable(AfiRoutes.SALES) {
                        SalesScreen(
                            productsState = products,
                            merchantsState = merchants,
                            cashBoxesState = cashBoxes,
                            storeSettingsState = storeSettings,
                            onCreateSale = viewModel::createSale,
                            onNavigateToFinance = { navigateTo(AfiRoutes.FINANCE) },
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable(AfiRoutes.PURCHASES) {
                        PurchasesScreen(
                            productsState = products,
                            suppliersState = suppliers,
                            cashBoxesState = cashBoxes,
                            storeSettingsState = storeSettings,
                            onCreatePurchase = viewModel::createPurchase,
                            onNavigateToFinance = { navigateTo(AfiRoutes.FINANCE) },
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable(AfiRoutes.PURCHASE_RETURNS) {
                        PurchaseReturnsScreen(
                            productsState = products,
                            suppliersState = suppliers,
                            debtsState = debtRecords,
                            cashBoxesState = cashBoxes,
                            storeSettingsState = storeSettings,
                            onCreateReturn = viewModel::createPurchaseReturn,
                            onNavigateToFinance = { navigateTo(AfiRoutes.FINANCE) },
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable(AfiRoutes.LOSS_SALES) {
                        LossSalesScreen(
                            productsState = products,
                            merchantsState = merchants,
                            cashBoxesState = cashBoxes,
                            storeSettingsState = storeSettings,
                            onCreateSale = viewModel::createSale,
                            onNavigateToFinance = { navigateTo(AfiRoutes.FINANCE) },
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable(AfiRoutes.EXPENSES) {
                        ExpensesScreen(
                            cashBoxesState = cashBoxes,
                            storeSettingsState = storeSettings,
                            onCreateExpense = viewModel::createExpense,
                            onNavigateToFinance = { navigateTo(AfiRoutes.FINANCE) },
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable(AfiRoutes.EMPLOYEE_SALARY) {
                        EmployeeSalaryScreen(
                            employeesState = employees,
                            jobTypesState = jobTypes,
                            payslipsState = payslips,
                            cashBoxesState = cashBoxes,
                            storeSettingsState = storeSettings,
                            onCreatePayslip = viewModel::createPayslip,
                            onTogglePayslipPaid = viewModel::togglePayslipPaid,
                            onPayInstallmentSingle = viewModel::payPayslipInstallment,
                            onPayInstallmentPool = viewModel::payPayslipsByInstallmentPool,
                            onPayAllPayslips = viewModel::payAllPayslips,
                            onDeletePayslip = viewModel::deletePayslip,
                            onNavigateToContactsPayslip = { navigateTo("tab_contacts_payslip") },
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable(AfiRoutes.DEBTS_HUTANG) {
                        DebtsScreen(
                            initialTab = 0,
                            debtsState = debtRecords,
                            merchantsState = merchants,
                            suppliersState = suppliers,
                            cashBoxesState = cashBoxes,
                            storeSettingsState = storeSettings,
                            onPayDebt = viewModel::payDebtRecord,
                            onPayDebtsByInstallmentPool = viewModel::payDebtsByInstallmentPool,
                            onSettleAllDebts = viewModel::settleAllDebts,
                            onUpdateDebtPaidDate = viewModel::updateDebtPaidDate,
                            onCreateManualDebt = viewModel::createManualDebt,
                            onDeleteDebt = viewModel::deleteDebtRecord,
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable(AfiRoutes.DEBTS_PIUTANG) {
                        DebtsScreen(
                            initialTab = 1,
                            debtsState = debtRecords,
                            merchantsState = merchants,
                            suppliersState = suppliers,
                            cashBoxesState = cashBoxes,
                            storeSettingsState = storeSettings,
                            onPayDebt = viewModel::payDebtRecord,
                            onPayDebtsByInstallmentPool = viewModel::payDebtsByInstallmentPool,
                            onSettleAllDebts = viewModel::settleAllDebts,
                            onUpdateDebtPaidDate = viewModel::updateDebtPaidDate,
                            onCreateManualDebt = viewModel::createManualDebt,
                            onDeleteDebt = viewModel::deleteDebtRecord,
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable(AfiRoutes.SETTINGS) {
                        SettingsScreen(
                            activeUser = currentUser,
                            storeSettingsState = storeSettings,
                            onSaveSettings = viewModel::saveStoreSettings,
                            onResetTransactions = viewModel::resetTransactionData,
                            onSignOut = onSignOut,
                            onBack = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }
}
