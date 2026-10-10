package com.dvt.moneylens.ui

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dvt.moneylens.data.backup.BackupSummary
import com.dvt.moneylens.data.backup.DataBackupDto
import com.dvt.moneylens.data.backup.DataBackupManager
import com.dvt.moneylens.data.local.MoneyLensDatabase
import com.dvt.moneylens.data.repository.MoneyLensRepository
import com.dvt.moneylens.domain.model.*
import com.dvt.moneylens.service.BudgetAlertNotifier
import com.dvt.moneylens.ui.designsystem.MonthlyBarData
import com.dvt.moneylens.ui.designsystem.NetWorthPoint
import com.dvt.moneylens.ui.theme.ThemeAccent
import com.dvt.moneylens.ui.theme.ThemeMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

enum class AutoLockTimeout(val label: String, val millis: Long) {
    IMMEDIATE("Ngay lập tức", 0L),
    SEC_30("30 giây", 30_000L),
    MIN_1("1 phút", 60_000L),
    MIN_5("5 phút", 300_000L),
    MIN_15("15 phút", 900_000L)
}

data class MainUiState(
    val accounts: List<Account> = emptyList(),
    val creditCards: List<CreditCard> = emptyList(),
    val transactions: List<Transaction> = emptyList(),
    val pendingTransactions: List<Transaction> = emptyList(),
    val categories: List<Category> = emptyList(),
    val budgets: List<Budget> = emptyList(),
    val assets: List<Asset> = emptyList(),
    val members: List<Member> = emptyList(),
    val loans: List<Loan> = emptyList(),
    val loanPayments: List<LoanPayment> = emptyList(),
    val isMasked: Boolean = false,
    val selectedMemberId: String? = null,
    val selectedTypeFilter: TransactionType? = null,
    val showOnlyPending: Boolean = false,
    val searchQuery: String = "",
    val lastDeletedTransaction: Transaction? = null,
    val enabledBankCodes: Set<String> = setOf("VCB", "TCB", "MB", "VPB", "BIDV", "ACB", "TPB", "SHB", "VIB", "SHINHAN", "COOPBANK", "HSBC", "AGRIBANK", "VIETINBANK", "MOMO", "ZALOPAY"),
    val isRefreshingPrices: Boolean = false,
    val isOfflineMode: Boolean = false,
    val lastPricesRefreshedAt: Long = System.currentTimeMillis(),
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val themeAccent: ThemeAccent = ThemeAccent.MODERN_EMERALD,
    val isAppLocked: Boolean = false,
    val isPinSecurityEnabled: Boolean = false,
    val isBiometricEnabled: Boolean = false,
    val userName: String = "MoneyLens",
    val userAvatar: String = "💼",
    val hasCompletedOnboarding: Boolean = false,
    val autoLockTimeout: AutoLockTimeout = AutoLockTimeout.MIN_5,
    val selectedChartYear: Int = Calendar.getInstance().get(Calendar.YEAR),
    val isNotificationCaptureEnabled: Boolean = true,
    val investmentTaxConfig: InvestmentTaxConfig = InvestmentTaxConfig(),
    val fundFeeSchedules: Map<String, FundExitFeeSchedule> = emptyMap(),
    val investmentTransactions: List<InvestmentTransaction> = emptyList()
) {
    fun getInvestmentTransactionsForAsset(assetId: String): List<InvestmentTransaction> {
        val list = investmentTransactions.filter { it.assetId == assetId }
        if (list.isNotEmpty()) return list
        val asset = assets.find { it.id == assetId } ?: return emptyList()
        return listOf(
            InvestmentTransaction(
                id = "init_${asset.id}",
                assetId = asset.id,
                symbol = asset.symbol,
                action = InvestmentAction.BUY,
                quantity = asset.quantity,
                priceMinor = asset.costBasisMinor,
                totalAmountMinor = (asset.quantity * asset.costBasisMinor).toLong(),
                accountId = null,
                accountName = "Khởi tạo ban đầu",
                note = asset.notes ?: "Mua ban đầu khi tạo danh mục",
                feeAndTaxMinor = 0L,
                timestamp = asset.lastUpdated
            )
        )
    }

    val totalCashBalanceMinor: Long
        get() = accounts.filter { it.type != AccountType.CREDIT_CARD }.sumOf { it.balanceMinor }

    val totalCreditDebtMinor: Long
        get() = creditCards.sumOf { it.currentBalanceOwedMinor }

    val totalBorrowingDebtMinor: Long
        get() = loans.filter { it.type == LoanType.BORROWING && !it.isPaidOff }.sumOf { it.remainingPrincipalMinor }

    val totalLendingReceivableMinor: Long
        get() = loans.filter { it.type == LoanType.LENDING && !it.isPaidOff }.sumOf { it.remainingPrincipalMinor }

    val totalAllLiabilitiesMinor: Long
        get() = totalCreditDebtMinor + totalBorrowingDebtMinor

    val totalPortfolioValueForNetWorth: Long
        get() = if (assets.isNotEmpty()) totalNetRealLiquidationValueMinor else 0L

    val totalAllAssetsMinor: Long
        get() = totalCashBalanceMinor + totalPortfolioValueForNetWorth + totalLendingReceivableMinor

    val totalNominalAssetsMinor: Long
        get() = totalCashBalanceMinor + totalPortfolioValueMinor + totalLendingReceivableMinor

    val netWorthMinor: Long
        get() = totalAllAssetsMinor - totalAllLiabilitiesMinor

    val monthlyDebtDueMinor: Long
        get() = loans.filter { it.type == LoanType.BORROWING && !it.isPaidOff }.sumOf { it.monthlyPaymentMinor }

    val totalIncomeThisMonthMinor: Long
        get() {
            val cal = Calendar.getInstance()
            val currYear = cal.get(Calendar.YEAR)
            val currMonth = cal.get(Calendar.MONTH) + 1
            return transactions.filter { tx ->
                if (tx.type != TransactionType.INCOME || tx.status != TransactionStatus.CONFIRMED) return@filter false
                val c = Calendar.getInstance()
                c.timeInMillis = tx.occurredAt
                c.get(Calendar.YEAR) == currYear && (c.get(Calendar.MONTH) + 1) == currMonth
            }.sumOf { it.amountMinor }
        }

    val totalExpenseThisMonthMinor: Long
        get() {
            val cal = Calendar.getInstance()
            val currYear = cal.get(Calendar.YEAR)
            val currMonth = cal.get(Calendar.MONTH) + 1
            return transactions.filter { tx ->
                if (tx.type != TransactionType.EXPENSE || tx.status != TransactionStatus.CONFIRMED) return@filter false
                // Tách dòng tiền tích sản và thanh toán nợ thẻ tín dụng ra khỏi chi phí sinh hoạt tiêu dùng
                if (tx.categoryId == "cat_investment" || tx.categoryId == "cat_credit_pay") return@filter false
                val c = Calendar.getInstance()
                c.timeInMillis = tx.occurredAt
                c.get(Calendar.YEAR) == currYear && (c.get(Calendar.MONTH) + 1) == currMonth
            }.sumOf { it.amountMinor }
        }

    val totalInvestmentThisMonthMinor: Long
        get() {
            val cal = Calendar.getInstance()
            val currYear = cal.get(Calendar.YEAR)
            val currMonth = cal.get(Calendar.MONTH) + 1
            return transactions.filter { tx ->
                if (tx.categoryId != "cat_investment" || tx.status != TransactionStatus.CONFIRMED) return@filter false
                val c = Calendar.getInstance()
                c.timeInMillis = tx.occurredAt
                c.get(Calendar.YEAR) == currYear && (c.get(Calendar.MONTH) + 1) == currMonth
            }.sumOf { it.amountMinor }
        }

    val totalBudgetLimitMinor: Long
        get() = budgets.sumOf { it.limitMinor }

    val budgetUsagePercent: Int
        get() = if (totalBudgetLimitMinor > 0) ((totalExpenseThisMonthMinor.toDouble() / totalBudgetLimitMinor) * 100).toInt() else 0

    val isBudgetWarning80: Boolean
        get() = budgetUsagePercent in 80..99

    val isBudgetCritical100: Boolean
        get() = budgetUsagePercent >= 100

    val availableToSpendMinor: Long
        get() = (totalBudgetLimitMinor - totalExpenseThisMonthMinor).coerceAtLeast(0L)

    val totalPortfolioValueMinor: Long
        get() = assets.sumOf { it.currentTotalValueMinor }

    val totalCostBasisMinor: Long
        get() = assets.sumOf { it.totalCostBasisMinor }

    val totalUnrealizedPnlMinor: Long
        get() = totalPortfolioValueMinor - totalCostBasisMinor

    val overallPnlPercent: Double
        get() = if (totalCostBasisMinor > 0) (totalUnrealizedPnlMinor.toDouble() / totalCostBasisMinor) * 100.0 else 0.0

    val totalNetLiquidationValueMinor: Long
        get() = assets.sumOf { InvestmentCalculations.calculateAssetMetrics(it, investmentTaxConfig, fundFeeSchedules).netLiquidationValueMinor }

    val totalNetRealLiquidationValueMinor: Long
        get() = assets.sumOf { InvestmentCalculations.calculateAssetMetrics(it, investmentTaxConfig, fundFeeSchedules).realLiquidationValueMinor }

    val totalNetUnrealizedPnlMinor: Long
        get() = totalNetLiquidationValueMinor - totalCostBasisMinor

    val totalRealUnrealizedPnlMinor: Long
        get() = totalNetRealLiquidationValueMinor - totalCostBasisMinor

    val overallNetPnlPercent: Double
        get() = if (totalCostBasisMinor > 0) (totalNetUnrealizedPnlMinor.toDouble() / totalCostBasisMinor) * 100.0 else 0.0

    val overallRealPnlPercent: Double
        get() = if (totalCostBasisMinor > 0) (totalRealUnrealizedPnlMinor.toDouble() / totalCostBasisMinor) * 100.0 else 0.0

    val availableChartYears: List<Int>
        get() {
            val cal = Calendar.getInstance()
            val currentYr = cal.get(Calendar.YEAR)
            val txYears = transactions.map {
                val c = Calendar.getInstance()
                c.timeInMillis = it.occurredAt
                c.get(Calendar.YEAR)
            }.toSet()
            val allYears = (txYears + currentYr).toList()
            return allYears.sortedDescending()
        }

    val monthlyBarData: List<MonthlyBarData>
        get() {
            val cal = Calendar.getInstance()
            val currentYr = cal.get(Calendar.YEAR)
            val currentMonth = cal.get(Calendar.MONTH) + 1 // 1..12

            val maxMonth = when {
                selectedChartYear < currentYr -> 12
                selectedChartYear == currentYr -> currentMonth
                else -> 12
            }

            val result = mutableListOf<MonthlyBarData>()
            for (m in 1..maxMonth) {
                val incomeForMonth = transactions.filter { tx ->
                    if (tx.status != TransactionStatus.CONFIRMED || tx.type != TransactionType.INCOME) return@filter false
                    val c = Calendar.getInstance()
                    c.timeInMillis = tx.occurredAt
                    c.get(Calendar.YEAR) == selectedChartYear && (c.get(Calendar.MONTH) + 1) == m
                }.sumOf { it.amountMinor }

                val expenseForMonth = transactions.filter { tx ->
                    if (tx.status != TransactionStatus.CONFIRMED || tx.type != TransactionType.EXPENSE) return@filter false
                    if (tx.categoryId == "cat_credit_pay") return@filter false
                    val c = Calendar.getInstance()
                    c.timeInMillis = tx.occurredAt
                    c.get(Calendar.YEAR) == selectedChartYear && (c.get(Calendar.MONTH) + 1) == m
                }.sumOf { it.amountMinor }

                result.add(
                    MonthlyBarData(
                        monthLabel = "Tháng $m",
                        incomeMinor = incomeForMonth,
                        expenseMinor = expenseForMonth
                    )
                )
            }
            return result
        }

    val historicalNetWorthPoints: List<NetWorthPoint>
        get() {
            val cal = Calendar.getInstance()
            val currentMonth = cal.get(Calendar.MONTH) + 1
            val currentNW = netWorthMinor
            val currentAssets = totalAllAssetsMinor
            val currentDebts = totalAllLiabilitiesMinor

            // If completely cleared or empty, return flat 0 points
            if (transactions.isEmpty() && accounts.all { it.balanceMinor == 0L } && assets.isEmpty() && loans.isEmpty()) {
                return (1..currentMonth).map { m ->
                    NetWorthPoint(
                        timestamp = System.currentTimeMillis() - (currentMonth - m) * 30 * 86_400_000L,
                        label = "T$m",
                        netWorthMinor = 0L,
                        totalAssetsMinor = 0L,
                        totalDebtsMinor = 0L
                    )
                }
            }

            // Generate monthly trend points leading to current net worth
            val points = mutableListOf<NetWorthPoint>()
            for (m in 1..currentMonth) {
                val monthsAgo = currentMonth - m
                val growthFactor = 1.0 - (monthsAgo * 0.038)
                val ptNW = (currentNW * growthFactor).toLong()
                val ptAssets = (currentAssets * (1.0 - monthsAgo * 0.026)).toLong()
                val ptDebts = (currentDebts * (1.0 + monthsAgo * 0.015)).toLong()

                points.add(
                    NetWorthPoint(
                        timestamp = System.currentTimeMillis() - monthsAgo * 30 * 86_400_000L,
                        label = "T$m",
                        netWorthMinor = ptNW,
                        totalAssetsMinor = ptAssets,
                        totalDebtsMinor = ptDebts
                    )
                )
            }
            return points
        }

    val filteredTransactions: List<Transaction>
        get() = transactions.filter { tx ->
            val matchMember = selectedMemberId == null || tx.memberId == selectedMemberId
            val matchType = selectedTypeFilter == null || tx.type == selectedTypeFilter
            val matchPending = !showOnlyPending || tx.status == TransactionStatus.PENDING
            val matchSearch = searchQuery.isBlank() ||
                    tx.note.contains(searchQuery, ignoreCase = true) ||
                    (tx.counterparty?.contains(searchQuery, ignoreCase = true) == true)
            matchMember && matchType && matchPending && matchSearch
        }
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: MoneyLensRepository
    private val securityPrefs = application.getSharedPreferences("moneylens_security_prefs", Context.MODE_PRIVATE)
    private val dataPrefs = application.getSharedPreferences("moneylens_prefs", Context.MODE_PRIVATE)

    private val _lastBackupTimestamp = MutableStateFlow<Long?>(
        run {
            val t = dataPrefs.getLong("last_backup_time", 0L)
            if (t > 0L) t else null
        }
    )
    val lastBackupTimestamp: StateFlow<Long?> = _lastBackupTimestamp.asStateFlow()

    private val _isBackupProcessing = MutableStateFlow(false)
    val isBackupProcessing: StateFlow<Boolean> = _isBackupProcessing.asStateFlow()

    private val _isMasked = MutableStateFlow(false)
    private val _selectedMemberId = MutableStateFlow<String?>(null)
    private val _selectedTypeFilter = MutableStateFlow<TransactionType?>(null)
    private val _showOnlyPending = MutableStateFlow(false)
    private val _searchQuery = MutableStateFlow("")
    private val _lastDeletedTransaction = MutableStateFlow<Transaction?>(null)
    private val _selectedChartYear = MutableStateFlow(Calendar.getInstance().get(Calendar.YEAR))
    private val _enabledBankCodes = MutableStateFlow(
        setOf("VCB", "TCB", "MB", "VPB", "BIDV", "ACB", "TPB", "SHB", "VIB", "SHINHAN", "COOPBANK", "HSBC", "AGRIBANK", "VIETINBANK", "MOMO", "ZALOPAY")
    )
    private val _isRefreshingPrices = MutableStateFlow(false)
    private val _isOfflineMode = MutableStateFlow(false)
    private val _lastPricesRefreshedAt = MutableStateFlow(System.currentTimeMillis())

    private val _themeMode = MutableStateFlow(
        runCatching {
            ThemeMode.valueOf(securityPrefs.getString("theme_mode", ThemeMode.SYSTEM.name) ?: ThemeMode.SYSTEM.name)
        }.getOrDefault(ThemeMode.SYSTEM)
    )
    private val _themeAccent = MutableStateFlow(
        runCatching {
            ThemeAccent.valueOf(securityPrefs.getString("theme_accent", ThemeAccent.MODERN_EMERALD.name) ?: ThemeAccent.MODERN_EMERALD.name)
        }.getOrDefault(ThemeAccent.MODERN_EMERALD)
    )
    private val _isPinSecurityEnabled = MutableStateFlow(securityPrefs.getBoolean("pin_security_enabled", false))
    private val _isBiometricEnabled = MutableStateFlow(securityPrefs.getBoolean("biometric_security_enabled", false))
    private val _isAppLocked = MutableStateFlow(securityPrefs.getBoolean("pin_security_enabled", false))
    private val _currentPin = MutableStateFlow(securityPrefs.getString("user_pin_code", "123456") ?: "123456")

    private val _userName = MutableStateFlow(
        run {
            val stored = securityPrefs.getString("user_display_name", null)
            if (stored.isNullOrBlank() || stored == "Tùng Đỗ") {
                securityPrefs.edit().putString("user_display_name", "MoneyLens").apply()
                "MoneyLens"
            } else {
                stored
            }
        }
    )
    private val _userAvatar = MutableStateFlow(securityPrefs.getString("user_avatar_emoji", "💼") ?: "💼")
    private val _hasCompletedOnboarding = MutableStateFlow(securityPrefs.getBoolean("has_completed_onboarding", false))
    private val _autoLockTimeout = MutableStateFlow(
        runCatching {
            AutoLockTimeout.valueOf(securityPrefs.getString("auto_lock_timeout", AutoLockTimeout.MIN_5.name) ?: AutoLockTimeout.MIN_5.name)
        }.getOrDefault(AutoLockTimeout.MIN_5)
    )
    private val _isNotificationCaptureEnabled = MutableStateFlow(
        securityPrefs.getBoolean("notification_capture_enabled", true)
    )

    private fun loadTaxConfig(): InvestmentTaxConfig {
        return InvestmentTaxConfig(
            stockBrokerageFeePercent = securityPrefs.getFloat("tax_stock_brokerage", 0.2f).toDouble(),
            stockSellingTaxPercent = securityPrefs.getFloat("tax_stock_sell", 0.1f).toDouble(),
            cashDividendTaxPercent = securityPrefs.getFloat("tax_cash_div", 5.0f).toDouble(),
            fundBuyingFeePercent = securityPrefs.getFloat("tax_fund_buy", 0.5f).toDouble(),
            fundSellingTaxPercent = securityPrefs.getFloat("tax_fund_sell", 0.1f).toDouble(),
            fundExitFeeTier1Percent = securityPrefs.getFloat("tax_fund_tier1", 1.0f).toDouble(),
            fundExitFeeTier2Percent = securityPrefs.getFloat("tax_fund_tier2", 0.5f).toDouble(),
            fundExitFeeTier3Percent = securityPrefs.getFloat("tax_fund_tier3", 0.0f).toDouble(),
            realEstateTransferTaxPercent = securityPrefs.getFloat("tax_re_transfer", 2.0f).toDouble(),
            realEstateBrokerageFeePercent = securityPrefs.getFloat("tax_re_brokerage", 1.0f).toDouble(),
            generalInflationRatePercent = securityPrefs.getFloat("tax_inflation_general", 4.0f).toDouble(),
            stockInflationRatePercent = securityPrefs.getFloat("tax_inflation_stock", 4.0f).toDouble(),
            realEstateInflationRatePercent = securityPrefs.getFloat("tax_inflation_re", 4.0f).toDouble(),
            goldInflationRatePercent = securityPrefs.getFloat("tax_inflation_gold", 4.0f).toDouble(),
            cryptoInflationRatePercent = securityPrefs.getFloat("tax_inflation_crypto", 4.0f).toDouble(),
            fundInflationRatePercent = securityPrefs.getFloat("tax_inflation_fund", 4.0f).toDouble()
        )
    }

    private val _investmentTaxConfig = MutableStateFlow(loadTaxConfig())

    private fun loadFundFeeSchedules(): Map<String, FundExitFeeSchedule> {
        val result = mutableMapOf<String, FundExitFeeSchedule>()
        // Defaults mẫu: DCDS, VESAF, VEOF, SSISCA, DCBC
        FundExitFeeSchedule.getPredefinedFundTemplates().forEach {
            result[it.symbol.uppercase()] = it
        }

        // Tải các cấu hình tùy chỉnh từ SharedPreferences
        securityPrefs.all.forEach { (key, value) ->
            if (key.startsWith("fund_fee_sched_") && value is String) {
                val symbol = key.removePrefix("fund_fee_sched_").uppercase()
                runCatching {
                    val parts = value.split("|")
                    if (parts.size >= 2) {
                        val name = parts[0]
                        val tiersStr = parts[1]
                        val tiers = tiersStr.split(",").mapNotNull { tierStr ->
                            val sub = tierStr.split(":")
                            if (sub.size == 2) {
                                FundExitFeeTier(maxMonths = sub[0].toInt(), feePercent = sub[1].toDouble())
                            } else null
                        }
                        if (tiers.isNotEmpty()) {
                            result[symbol] = FundExitFeeSchedule(symbol = symbol, fundName = name, tiers = tiers)
                        }
                    }
                }
            }
        }
        return result
    }

    private val _fundFeeSchedules = MutableStateFlow(loadFundFeeSchedules())
    private var appBackgroundedAt: Long = 0L

    private var hasAlerted80 = false
    private var hasAlerted100 = false

    val uiState: StateFlow<MainUiState>

    init {
        val db = MoneyLensDatabase.getInstance(application)
        repository = MoneyLensRepository(db)

        viewModelScope.launch {
            repository.checkAndSeedInitialData(application)
            repository.repairMissingAccountTransactions()
            refreshMarketPrices()
        }

        uiState = combine(
            combine(
                repository.accounts,
                repository.creditCards,
                repository.transactions,
                repository.pendingTransactions,
                repository.categories
            ) { accs, cards, txs, pending, cats ->
                Tuple5(accs, cards, txs, pending, cats)
            },
            combine(
                repository.budgets,
                repository.assets,
                repository.members,
                repository.loans,
                combine(_isMasked, repository.loanPayments, repository.investmentTransactions) { masked, payments, invTxs ->
                    Triple(masked, payments, invTxs)
                }
            ) { b, ast, mem, loans, maskedPaymentsTxs ->
                Tuple5(b, ast, mem, loans, maskedPaymentsTxs)
            },
            combine(
                _selectedMemberId,
                _selectedTypeFilter,
                _showOnlyPending,
                _searchQuery,
                _lastDeletedTransaction
            ) { memberId, typeFilter, pendingOnly, query, lastDel ->
                Tuple5(memberId, typeFilter, pendingOnly, query, lastDel)
            },
            combine(
                _enabledBankCodes,
                _isRefreshingPrices,
                _isOfflineMode,
                _lastPricesRefreshedAt,
                combine(_themeMode, _themeAccent, _selectedChartYear, _isNotificationCaptureEnabled) { tm, ta, yr, nce ->
                    ThemeSettingsTuple(tm, ta, yr, nce)
                }
            ) { banks, refreshing, offline, refreshedAt, themeSettings ->
                Tuple5(banks, refreshing, offline, refreshedAt, themeSettings)
            },
            combine(
                _isAppLocked,
                _isPinSecurityEnabled,
                _isBiometricEnabled,
                _userName,
                combine(_userAvatar, _hasCompletedOnboarding, _autoLockTimeout, _investmentTaxConfig, _fundFeeSchedules) { av, ob, timeout, tax, scheds ->
                    Tuple5(av, ob, timeout, tax, scheds)
                }
            ) { locked, pinEnabled, bioEnabled, uName, quint ->
                SecurityFullTuple(locked, pinEnabled, bioEnabled, uName, quint.a, quint.b, quint.c, quint.d, quint.e)
            }
        ) { t1, t2, t3, t4, sec ->
            val themeSettings = t4.e
            val state = MainUiState(
                accounts = t1.a,
                creditCards = t1.b,
                transactions = t1.c,
                pendingTransactions = t1.d,
                categories = t1.e,
                budgets = t2.a,
                assets = t2.b.sortedByDescending { it.lastUpdated },
                members = t2.c,
                loans = t2.d,
                isMasked = t2.e.first,
                loanPayments = t2.e.second,
                investmentTransactions = t2.e.third,
                selectedMemberId = t3.a,
                selectedTypeFilter = t3.b,
                showOnlyPending = t3.c,
                searchQuery = t3.d,
                lastDeletedTransaction = t3.e,
                enabledBankCodes = t4.a,
                isRefreshingPrices = t4.b,
                isOfflineMode = t4.c,
                lastPricesRefreshedAt = t4.d,
                themeMode = themeSettings.themeMode,
                themeAccent = themeSettings.themeAccent,
                selectedChartYear = themeSettings.selectedChartYear,
                isNotificationCaptureEnabled = themeSettings.isNotificationCaptureEnabled,
                investmentTaxConfig = sec.taxConfig,
                fundFeeSchedules = sec.fundFeeSchedules,
                isAppLocked = sec.isAppLocked,
                isPinSecurityEnabled = sec.pinEnabled,
                isBiometricEnabled = sec.bioEnabled,
                userName = sec.userName,
                userAvatar = sec.userAvatar,
                hasCompletedOnboarding = sec.hasCompletedOnboarding,
                autoLockTimeout = sec.autoLockTimeout
            )
            checkBudgetAlerts(state.totalExpenseThisMonthMinor, state.totalBudgetLimitMinor)
            state
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = MainUiState(
                themeMode = _themeMode.value,
                themeAccent = _themeAccent.value,
                selectedChartYear = _selectedChartYear.value,
                investmentTaxConfig = _investmentTaxConfig.value,
                fundFeeSchedules = _fundFeeSchedules.value,
                isAppLocked = _isAppLocked.value,
                isPinSecurityEnabled = _isPinSecurityEnabled.value,
                isBiometricEnabled = _isBiometricEnabled.value,
                userName = _userName.value,
                userAvatar = _userAvatar.value,
                hasCompletedOnboarding = _hasCompletedOnboarding.value,
                autoLockTimeout = _autoLockTimeout.value
            )
        )
    }

    fun setSelectedChartYear(year: Int) {
        _selectedChartYear.value = year
    }

    fun onAppBackgrounded() {
        appBackgroundedAt = System.currentTimeMillis()
    }

    fun onAppForegrounded() {
        if (_isPinSecurityEnabled.value && appBackgroundedAt > 0L) {
            val elapsed = System.currentTimeMillis() - appBackgroundedAt
            if (elapsed >= _autoLockTimeout.value.millis) {
                _isAppLocked.value = true
            }
        }
    }

    fun setAutoLockTimeout(timeout: AutoLockTimeout) {
        _autoLockTimeout.value = timeout
        securityPrefs.edit().putString("auto_lock_timeout", timeout.name).apply()
    }

    fun setUserInfo(name: String, avatar: String) {
        _userName.value = name
        _userAvatar.value = avatar
        securityPrefs.edit()
            .putString("user_display_name", name)
            .putString("user_avatar_emoji", avatar)
            .apply()
    }

    fun completeOnboarding(useDemoData: Boolean, name: String, avatar: String, pin: String?, enableBio: Boolean) {
        setUserInfo(name, avatar)
        if (pin != null) {
            togglePinSecurity(true, pin)
        } else {
            togglePinSecurity(false)
        }
        toggleBiometricSecurity(enableBio)

        viewModelScope.launch {
            val app = getApplication<Application>()
            if (useDemoData) {
                repository.clearAllData(app)
                repository.seedDemoData(app)
                refreshMarketPrices()
            } else {
                repository.clearAllData(app)
            }
            _hasCompletedOnboarding.value = true
            securityPrefs.edit().putBoolean("has_completed_onboarding", true).apply()
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        _themeMode.value = mode
        securityPrefs.edit().putString("theme_mode", mode.name).apply()
    }

    fun setThemeAccent(accent: ThemeAccent) {
        _themeAccent.value = accent
        securityPrefs.edit().putString("theme_accent", accent.name).apply()
    }

    fun verifyPin(pin: String): Boolean {
        return if (pin == _currentPin.value) {
            _isAppLocked.value = false
            true
        } else {
            false
        }
    }

    fun unlockWithBiometric() {
        _isAppLocked.value = false
    }

    fun lockApp() {
        if (_isPinSecurityEnabled.value) {
            _isAppLocked.value = true
        }
    }

    fun setPin(newPin: String) {
        if (newPin.length == 6) {
            _currentPin.value = newPin
            securityPrefs.edit().putString("user_pin_code", newPin).apply()
        }
    }

    fun togglePinSecurity(enabled: Boolean, defaultPin: String = "123456") {
        _isPinSecurityEnabled.value = enabled
        securityPrefs.edit().putBoolean("pin_security_enabled", enabled).apply()
        if (enabled && securityPrefs.getString("user_pin_code", null).isNullOrBlank()) {
            _currentPin.value = defaultPin
            securityPrefs.edit().putString("user_pin_code", defaultPin).apply()
        }
        if (!enabled) {
            _isAppLocked.value = false
            // Khi tắt mã PIN, tự động tắt luôn xác thực sinh trắc học (vân tay)
            _isBiometricEnabled.value = false
            securityPrefs.edit().putBoolean("biometric_security_enabled", false).apply()
        }
    }

    fun toggleBiometricSecurity(enabled: Boolean) {
        // Chỉ cho phép bật vân tay nếu mã PIN bảo mật đang bật
        if (enabled && !_isPinSecurityEnabled.value) {
            return
        }
        _isBiometricEnabled.value = enabled
        securityPrefs.edit().putBoolean("biometric_security_enabled", enabled).apply()
    }

    private fun checkBudgetAlerts(spentMinor: Long, limitMinor: Long) {
        val app = getApplication<Application>()
        if (limitMinor <= 0 || spentMinor == 0L) {
            hasAlerted80 = false
            hasAlerted100 = false
            BudgetAlertNotifier.cancelAlerts(app)
            return
        }
        val percent = ((spentMinor.toDouble() / limitMinor) * 100).toInt()
        if (percent >= 100) {
            if (!hasAlerted100) {
                BudgetAlertNotifier.sendBudgetAlert(
                    context = app,
                    percent = percent,
                    spentVnd = Money(spentMinor).formatVnd(),
                    limitVnd = Money(limitMinor).formatVnd(),
                    isCritical = true
                )
                hasAlerted100 = true
            }
        } else if (percent >= 80) {
            if (!hasAlerted80) {
                BudgetAlertNotifier.sendBudgetAlert(
                    context = app,
                    percent = percent,
                    spentVnd = Money(spentMinor).formatVnd(),
                    limitVnd = Money(limitMinor).formatVnd(),
                    isCritical = false
                )
                hasAlerted80 = true
            }
        } else {
            hasAlerted80 = false
            hasAlerted100 = false
            BudgetAlertNotifier.cancelAlerts(app)
        }
    }

    fun togglePrivacy() {
        _isMasked.update { !it }
    }

    fun setMemberFilter(memberId: String?) {
        _selectedMemberId.value = memberId
    }

    fun setTypeFilter(type: TransactionType?) {
        _selectedTypeFilter.value = type
    }

    fun setShowOnlyPending(show: Boolean) {
        _showOnlyPending.value = show
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setOfflineMode(enabled: Boolean) {
        _isOfflineMode.value = enabled
    }

    fun toggleBankEnabled(bankCode: String) {
        _enabledBankCodes.update { current ->
            if (current.contains(bankCode)) current - bankCode else current + bankCode
        }
    }

    fun enableAllBanks(bankCodes: List<String>) {
        _enabledBankCodes.value = bankCodes.toSet()
    }

    fun disableAllBanks() {
        _enabledBankCodes.value = emptySet()
    }

    fun refreshMarketPrices(onComplete: ((Int) -> Unit)? = null) {
        viewModelScope.launch {
            if (_isRefreshingPrices.value) return@launch
            _isRefreshingPrices.value = true
            try {
                val updatedCount = repository.refreshAllAssetPrices(_isOfflineMode.value)
                _lastPricesRefreshedAt.value = System.currentTimeMillis()
                onComplete?.invoke(updatedCount)
            } catch (e: Exception) {
                onComplete?.invoke(0)
            } finally {
                _isRefreshingPrices.value = false
            }
        }
    }

    fun confirmTransaction(tx: Transaction) {
        viewModelScope.launch {
            repository.confirmTransaction(tx)
        }
    }

    fun confirmTransaction(transactionId: String, categoryId: String?) {
        viewModelScope.launch {
            repository.confirmTransaction(transactionId, categoryId)
        }
    }

    fun confirmTransactions(transactionIds: List<String>) {
        if (transactionIds.isEmpty()) return
        viewModelScope.launch {
            repository.confirmTransactions(transactionIds)
        }
    }

    fun confirmTransactionsWithCategory(transactionIds: List<String>, categoryId: String?) {
        if (transactionIds.isEmpty()) return
        viewModelScope.launch {
            repository.confirmTransactionsWithCategory(transactionIds, categoryId)
        }
    }

    fun deleteTransaction(transaction: Transaction) {
        viewModelScope.launch {
            _lastDeletedTransaction.value = transaction
            repository.deleteTransaction(transaction.id)
        }
    }

    fun deleteTransactions(transactionIds: List<String>) {
        if (transactionIds.isEmpty()) return
        viewModelScope.launch {
            repository.deleteTransactions(transactionIds)
        }
    }

    fun undoDelete() {
        val tx = _lastDeletedTransaction.value ?: return
        viewModelScope.launch {
            repository.addTransaction(tx)
            _lastDeletedTransaction.value = null
        }
    }

    fun addQuickTransaction(
        amountMinor: Long,
        type: TransactionType,
        categoryId: String?,
        accountId: String,
        note: String,
        paymentSource: PaymentSourceType = PaymentSourceType.DEBIT,
        creditCardId: String? = null,
        occurredAt: Long = System.currentTimeMillis()
    ) {
        viewModelScope.launch {
            val newTx = Transaction(
                id = UUID.randomUUID().toString(),
                accountId = accountId,
                amountMinor = amountMinor,
                type = type,
                categoryId = categoryId,
                note = note,
                status = TransactionStatus.CONFIRMED,
                occurredAt = occurredAt,
                source = "MANUAL",
                memberId = _selectedMemberId.value ?: "mem_self",
                paymentSource = paymentSource,
                creditCardId = creditCardId
            )
            repository.addTransaction(newTx)
            if (type == TransactionType.EXPENSE && categoryId != "cat_investment" && categoryId != "cat_credit_pay") {
                val txCal = java.util.Calendar.getInstance().apply { timeInMillis = occurredAt }
                val nowCal = java.util.Calendar.getInstance()
                val isCurrentMonth = txCal.get(java.util.Calendar.YEAR) == nowCal.get(java.util.Calendar.YEAR) &&
                        txCal.get(java.util.Calendar.MONTH) == nowCal.get(java.util.Calendar.MONTH)

                if (isCurrentMonth) {
                    val newSpent = uiState.value.totalExpenseThisMonthMinor + amountMinor
                    checkBudgetAlerts(newSpent, uiState.value.totalBudgetLimitMinor)
                }
            }
        }
    }

    fun updateTransaction(tx: Transaction) {
        viewModelScope.launch {
            repository.updateTransaction(tx)
        }
    }

    fun convertCreditTxToInstallment(
        txId: String,
        title: String,
        counterparty: String,
        originalAmountMinor: Long,
        termMonths: Int,
        conversionFeeMinor: Long,
        conversionFeeSchedule: ConversionFeeSchedule,
        conversionFeePercent: Double,
        dueDayOfMonth: Int = 15,
        colorHex: String = "#E8A317"
    ) {
        viewModelScope.launch {
            val regularMonthly = if (termMonths > 0) {
                val base = originalAmountMinor / termMonths
                if (conversionFeeSchedule == ConversionFeeSchedule.SPREAD_MONTHLY) {
                    base + (conversionFeeMinor / termMonths)
                } else base
            } else originalAmountMinor

            val loan = Loan(
                id = UUID.randomUUID().toString(),
                title = title,
                counterparty = counterparty,
                type = LoanType.BORROWING,
                originalAmountMinor = originalAmountMinor,
                remainingPrincipalMinor = originalAmountMinor,
                interestRate = 0.0,
                interestType = InterestType.ZERO_PERCENT,
                termMonths = termMonths,
                monthlyPaymentMinor = regularMonthly,
                startDate = System.currentTimeMillis(),
                dueDayOfMonth = dueDayOfMonth,
                status = LoanStatus.ACTIVE,
                notes = "Trả góp 0% chuyển đổi từ giao dịch thẻ tín dụng",
                colorHex = colorHex,
                conversionFeeMinor = conversionFeeMinor,
                conversionFeeSchedule = conversionFeeSchedule,
                conversionFeePercent = conversionFeePercent
            )
            repository.convertCreditTxToInstallment(txId, loan)
        }
    }

    fun setMonthlyTotalBudget(totalBudgetMinor: Long) {
        viewModelScope.launch {
            repository.updateMonthlyTotalBudget(totalBudgetMinor, uiState.value.categories)
            checkBudgetAlerts(uiState.value.totalExpenseThisMonthMinor, totalBudgetMinor)
        }
    }

    fun updateCategoryBudget(categoryId: String, limitMinor: Long) {
        viewModelScope.launch {
            repository.updateBudget(categoryId, limitMinor)
        }
    }

    fun payCreditCard(cardId: String, fromAccountId: String, amountMinor: Long) {
        viewModelScope.launch {
            repository.recordCardPayment(cardId, fromAccountId, amountMinor)
        }
    }

    fun addCreditCard(
        nickname: String,
        issuerBank: String,
        network: CardNetwork,
        last4: String,
        creditLimitMinor: Long,
        statementDay: Int,
        dueDaysAfterStatement: Int,
        colorHex: String,
        cardType: CardType = CardType.CREDIT,
        initialBalanceMinor: Long = 0L
    ) {
        viewModelScope.launch {
            val accId = UUID.randomUUID().toString()
            val card = CreditCard(
                id = UUID.randomUUID().toString(),
                accountId = accId,
                nickname = nickname,
                issuerBank = issuerBank,
                network = network,
                last4 = last4,
                creditLimitMinor = if (cardType == CardType.CREDIT) creditLimitMinor else 0L,
                statementDay = if (cardType == CardType.CREDIT) statementDay else 0,
                dueDaysAfterStatement = if (cardType == CardType.CREDIT) dueDaysAfterStatement else 0,
                minPaymentPercent = 0.05,
                minPaymentFloorMinor = 100_000L,
                currentBalanceOwedMinor = 0L,
                annualFeeMinor = 0L,
                colorHex = colorHex,
                ownerMemberId = _selectedMemberId.value ?: "mem_self",
                cardType = cardType
            )
            repository.addCreditCard(card, initialBalanceMinor)
        }
    }

    fun updateCreditCard(card: CreditCard) {
        viewModelScope.launch {
            repository.updateCreditCard(card)
        }
    }

    fun deleteCreditCard(cardId: String) {
        viewModelScope.launch {
            repository.deleteCreditCard(cardId)
        }
    }

    fun addLoan(
        title: String,
        counterparty: String,
        type: LoanType,
        originalAmountMinor: Long,
        interestRate: Double,
        interestType: InterestType,
        termMonths: Int,
        monthlyPaymentMinor: Long,
        startDate: Long = System.currentTimeMillis(),
        dueDayOfMonth: Int,
        notes: String?,
        linkedAccountId: String?,
        colorHex: String,
        conversionFeeMinor: Long = 0L,
        conversionFeeSchedule: ConversionFeeSchedule = ConversionFeeSchedule.FIRST_MONTH,
        conversionFeePercent: Double = 0.0
    ) {
        viewModelScope.launch {
            val loan = Loan(
                id = UUID.randomUUID().toString(),
                title = title,
                counterparty = counterparty,
                type = type,
                originalAmountMinor = originalAmountMinor,
                remainingPrincipalMinor = originalAmountMinor,
                interestRate = interestRate,
                interestType = interestType,
                termMonths = termMonths,
                monthlyPaymentMinor = monthlyPaymentMinor,
                startDate = startDate,
                dueDayOfMonth = dueDayOfMonth,
                status = LoanStatus.ACTIVE,
                notes = notes,
                linkedAccountId = linkedAccountId,
                colorHex = colorHex,
                conversionFeeMinor = conversionFeeMinor,
                conversionFeeSchedule = conversionFeeSchedule,
                conversionFeePercent = conversionFeePercent
            )
            repository.addOrUpdateLoan(loan)
        }
    }

    fun payLoan(
        loanId: String,
        amountMinor: Long,
        principalMinor: Long,
        interestMinor: Long,
        accountId: String?,
        note: String,
        paidAt: Long = System.currentTimeMillis()
    ) {
        viewModelScope.launch {
            repository.recordLoanPayment(
                loanId = loanId,
                amountMinor = amountMinor,
                principalMinor = principalMinor,
                interestMinor = interestMinor,
                accountId = accountId,
                note = note,
                paidAt = paidAt
            )
        }
    }

    fun updateLoan(loan: Loan) {
        viewModelScope.launch {
            repository.addOrUpdateLoan(loan)
        }
    }

    fun deleteLoanPayment(paymentId: String, loanId: String) {
        viewModelScope.launch {
            repository.deleteLoanPayment(paymentId, loanId)
        }
    }

    fun deleteLoan(loanId: String) {
        viewModelScope.launch {
            repository.deleteLoan(loanId)
        }
    }

    fun resetToDemoData() {
        viewModelScope.launch {
            val app = getApplication<Application>()
            repository.clearAllData(app)
            repository.seedDemoData(app)
            refreshMarketPrices()
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            val app = getApplication<Application>()
            hasAlerted80 = false
            hasAlerted100 = false
            BudgetAlertNotifier.cancelAlerts(app)
            repository.clearAllData(app)
        }
    }

    fun exportBackupToUri(uri: Uri, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            _isBackupProcessing.value = true
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val dto = repository.exportAllDataForBackup()
                    val json = DataBackupManager.toJson(dto)
                    getApplication<Application>().contentResolver.openOutputStream(uri)?.use { os ->
                        os.write(json.toByteArray(Charsets.UTF_8))
                        os.flush()
                    } ?: error("Không thể mở tệp đích để ghi")
                    val now = System.currentTimeMillis()
                    dataPrefs.edit().putLong("last_backup_time", now).apply()
                    now
                }
            }
            _isBackupProcessing.value = false
            result.onSuccess { now ->
                _lastBackupTimestamp.value = now
                onResult(true, null)
            }.onFailure { e ->
                onResult(false, e.localizedMessage ?: "Lỗi khi xuất file sao lưu")
            }
        }
    }

    fun readAndParseBackupFromUri(
        uri: Uri,
        onSuccess: (DataBackupDto, BackupSummary) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            _isBackupProcessing.value = true
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val json = getApplication<Application>().contentResolver.openInputStream(uri)?.use { inputStream ->
                        inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                    } ?: error("Không thể mở tệp được chọn")

                    val dto = DataBackupManager.fromJson(json).getOrThrow()
                    val summary = DataBackupManager.extractSummary(json).getOrThrow()
                    Pair(dto, summary)
                }
            }
            _isBackupProcessing.value = false
            result.onSuccess { (dto, summary) ->
                onSuccess(dto, summary)
            }.onFailure { e ->
                onError(e.localizedMessage ?: "Tệp sao lưu không hợp lệ hoặc lỗi định dạng")
            }
        }
    }

    fun restoreFromBackup(dto: DataBackupDto, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            _isBackupProcessing.value = true
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val app = getApplication<Application>()
                    repository.restoreAllData(dto, app)
                    val now = System.currentTimeMillis()
                    dataPrefs.edit().putLong("last_backup_time", now).apply()
                    now
                }
            }
            _isBackupProcessing.value = false
            result.onSuccess { now ->
                _lastBackupTimestamp.value = now
                refreshMarketPrices()
                onResult(true, null)
            }.onFailure { e ->
                onResult(false, e.localizedMessage ?: "Lỗi khôi phục cơ sở dữ liệu")
            }
        }
    }

    fun shareBackup(context: Context, onError: (String) -> Unit = {}) {
        viewModelScope.launch {
            _isBackupProcessing.value = true
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val dto = repository.exportAllDataForBackup()
                    val json = DataBackupManager.toJson(dto)
                    val cacheDir = context.cacheDir
                    val timeStr = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())
                    val backupFile = java.io.File(cacheDir, "moneylens_backup_$timeStr.json")
                    backupFile.writeText(json, Charsets.UTF_8)

                    val uri = FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        backupFile
                    )
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "application/json"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        putExtra(Intent.EXTRA_SUBJECT, "Bản sao lưu MoneyLens")
                        putExtra(
                            Intent.EXTRA_TEXT,
                            "Bản sao lưu dữ liệu tài chính MoneyLens (${SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())})"
                        )
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    val now = System.currentTimeMillis()
                    dataPrefs.edit().putLong("last_backup_time", now).apply()
                    Pair(now, shareIntent)
                }
            }
            _isBackupProcessing.value = false
            result.onSuccess { (now, shareIntent) ->
                _lastBackupTimestamp.value = now
                context.startActivity(Intent.createChooser(shareIntent, "Chia sẻ bản sao lưu MoneyLens"))
            }.onFailure { e ->
                onError(e.localizedMessage ?: "Lỗi chia sẻ file sao lưu")
            }
        }
    }

    fun executeInvestmentAction(
        action: InvestmentAction,
        assetId: String?,
        symbol: String,
        name: String,
        assetType: AssetType,
        quantity: Double,
        priceMinor: Long,
        accountId: String?,
        note: String,
        occurredAt: Long = System.currentTimeMillis()
    ) {
        viewModelScope.launch {
            repository.executeInvestmentTransaction(
                action = action,
                assetId = assetId,
                symbol = symbol,
                name = name,
                assetType = assetType,
                quantity = quantity,
                priceMinor = priceMinor,
                accountId = accountId,
                note = note,
                occurredAt = occurredAt
            )
        }
    }

    fun updateTaxConfig(config: InvestmentTaxConfig) {
        _investmentTaxConfig.value = config
        securityPrefs.edit()
            .putFloat("tax_stock_brokerage", config.stockBrokerageFeePercent.toFloat())
            .putFloat("tax_stock_sell", config.stockSellingTaxPercent.toFloat())
            .putFloat("tax_cash_div", config.cashDividendTaxPercent.toFloat())
            .putFloat("tax_fund_buy", config.fundBuyingFeePercent.toFloat())
            .putFloat("tax_fund_sell", config.fundSellingTaxPercent.toFloat())
            .putFloat("tax_fund_tier1", config.fundExitFeeTier1Percent.toFloat())
            .putFloat("tax_fund_tier2", config.fundExitFeeTier2Percent.toFloat())
            .putFloat("tax_fund_tier3", config.fundExitFeeTier3Percent.toFloat())
            .putFloat("tax_re_transfer", config.realEstateTransferTaxPercent.toFloat())
            .putFloat("tax_re_brokerage", config.realEstateBrokerageFeePercent.toFloat())
            .putFloat("tax_inflation_general", config.generalInflationRatePercent.toFloat())
            .putFloat("tax_inflation_stock", config.stockInflationRatePercent.toFloat())
            .putFloat("tax_inflation_re", config.realEstateInflationRatePercent.toFloat())
            .putFloat("tax_inflation_gold", config.goldInflationRatePercent.toFloat())
            .putFloat("tax_inflation_crypto", config.cryptoInflationRatePercent.toFloat())
            .putFloat("tax_inflation_fund", config.fundInflationRatePercent.toFloat())
            .apply()
    }

    fun resetTaxConfig() {
        updateTaxConfig(InvestmentTaxConfig())
    }

    fun saveFundFeeSchedule(schedule: FundExitFeeSchedule) {
        val current = _fundFeeSchedules.value.toMutableMap()
        current[schedule.symbol.uppercase()] = schedule
        _fundFeeSchedules.value = current

        val serialized = "${schedule.fundName}|" + schedule.tiers.joinToString(",") { "${it.maxMonths}:${it.feePercent}" }
        securityPrefs.edit().putString("fund_fee_sched_${schedule.symbol.uppercase()}", serialized).apply()
    }

    fun resetFundFeeSchedule(symbol: String) {
        val upper = symbol.uppercase()
        val current = _fundFeeSchedules.value.toMutableMap()
        val def = FundExitFeeSchedule.getDefaultSchedule(upper)
        current[upper] = def
        _fundFeeSchedules.value = current
        securityPrefs.edit().remove("fund_fee_sched_$upper").apply()
    }

    fun updateAsset(asset: Asset) {
        viewModelScope.launch {
            repository.addOrUpdateAsset(asset)
        }
    }

    fun deleteAsset(
        assetId: String,
        refundAccountId: String? = null,
        refundAmountMinor: Long = 0L
    ) {
        viewModelScope.launch {
            repository.deleteAsset(assetId, refundAccountId, refundAmountMinor)
        }
    }

    fun setNotificationCaptureEnabled(enabled: Boolean) {
        _isNotificationCaptureEnabled.value = enabled
        securityPrefs.edit().putBoolean("notification_capture_enabled", enabled).apply()
    }

    fun cleanSpuriousTransactions(onComplete: (Int) -> Unit = {}) {
        viewModelScope.launch {
            val count = repository.cleanSpuriousTransactions()
            onComplete(count)
        }
    }
}

private data class Tuple4<A, B, C, D>(val a: A, val b: B, val c: C, val d: D)
private data class Tuple5<A, B, C, D, E>(val a: A, val b: B, val c: C, val d: D, val e: E)
private data class SecurityFullTuple(
    val isAppLocked: Boolean,
    val pinEnabled: Boolean,
    val bioEnabled: Boolean,
    val userName: String,
    val userAvatar: String,
    val hasCompletedOnboarding: Boolean,
    val autoLockTimeout: AutoLockTimeout,
    val taxConfig: InvestmentTaxConfig,
    val fundFeeSchedules: Map<String, FundExitFeeSchedule>
)

private data class ThemeSettingsTuple(
    val themeMode: ThemeMode,
    val themeAccent: ThemeAccent,
    val selectedChartYear: Int,
    val isNotificationCaptureEnabled: Boolean
)
