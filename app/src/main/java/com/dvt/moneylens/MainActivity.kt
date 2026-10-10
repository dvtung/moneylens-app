package com.dvt.moneylens

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dvt.moneylens.domain.model.*
import com.dvt.moneylens.ui.MainViewModel
import com.dvt.moneylens.ui.screens.*
import com.dvt.moneylens.ui.theme.*
import kotlinx.coroutines.launch

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    object Overview : Screen("overview", "Tổng quan", Icons.Rounded.Dashboard)
    object Transactions : Screen("transactions", "Giao dịch", Icons.Rounded.ReceiptLong)
    object Budget : Screen("budget", "Ngân sách", Icons.Rounded.PieChart)
    object Investment : Screen("investment", "Đầu tư", Icons.Rounded.TrendingUp)
    object Debts : Screen("debts", "Tín dụng", Icons.Rounded.AccountBalanceWallet)
    object Settings : Screen("settings", "Cài đặt", Icons.Rounded.Settings)
    object InvestmentGuide : Screen("investment_guide", "Phương pháp tính", Icons.Rounded.MenuBook)
}

class MainActivity : FragmentActivity() {

    private val viewModel: MainViewModel by viewModels()

    fun showBiometricPrompt(
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        try {
            val executor = ContextCompat.getMainExecutor(this)
            val biometricManager = BiometricManager.from(this)
            val authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK
            val canAuth = biometricManager.canAuthenticate(authenticators)

            if (canAuth != BiometricManager.BIOMETRIC_SUCCESS) {
                val message = when (canAuth) {
                    BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> "Chưa đăng ký vân tay trên máy. Vui lòng sử dụng mã PIN (Mặc định: 123456)."
                    BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> "Thiết bị không hỗ trợ cảm biến sinh trắc học. Vui lòng nhập mã PIN."
                    BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> "Cảm biến sinh trắc học hiện không khả dụng. Vui lòng nhập mã PIN."
                    else -> "Sinh trắc học không khả dụng. Vui lòng nhập mã PIN."
                }
                onError(message)
                return
            }

            val promptInfo = BiometricPrompt.PromptInfo.Builder()
                .setTitle("Xác thực sinh trắc học")
                .setSubtitle("Xác nhận vân tay để mở khóa MoneyLens")
                .setNegativeButtonText("Sử dụng mã PIN")
                .setAllowedAuthenticators(authenticators)
                .build()

            val biometricPrompt = BiometricPrompt(
                this,
                executor,
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        super.onAuthenticationSucceeded(result)
                        onSuccess()
                    }

                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                        super.onAuthenticationError(errorCode, errString)
                        if (errorCode != BiometricPrompt.ERROR_USER_CANCELED && errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                            onError(errString.toString())
                        }
                    }

                    override fun onAuthenticationFailed() {
                        super.onAuthenticationFailed()
                        // System prompt UI handles "Not recognized, try again"
                    }
                }
            )

            biometricPrompt.authenticate(promptInfo)
        } catch (e: Throwable) {
            onError("Không thể kích hoạt sinh trắc học trên môi trường hiện tại. Vui lòng nhập mã PIN.")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val lastBackupTimestamp by viewModel.lastBackupTimestamp.collectAsStateWithLifecycle()
            val isBackupProcessing by viewModel.isBackupProcessing.collectAsStateWithLifecycle()
            val lifecycleOwner = LocalLifecycleOwner.current
            val context = androidx.compose.ui.platform.LocalContext.current

            var activeConfirmTx by remember { mutableStateOf<Transaction?>(null) }

            // Yêu cầu quyền thông báo tự động trên Android 13+ khi mở ứng dụng
            val requestNotificationLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
                androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
            ) { /* Kết quả xử lý trong state */ }

            LaunchedEffect(Unit) {
                // Tự động dọn dẹp các giao dịch rác sinh nhầm do lỗi vòng lặp trước đó
                viewModel.cleanSpuriousTransactions()

                val reviewTxId = intent?.getStringExtra(EXTRA_REVIEW_TX_ID)
                if (!reviewTxId.isNullOrBlank()) {
                    val target = uiState.transactions.find { it.id == reviewTxId }
                        ?: uiState.pendingTransactions.find { it.id == reviewTxId }
                    if (target != null) {
                        activeConfirmTx = target
                    }
                }

                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                    if (androidx.core.content.ContextCompat.checkSelfPermission(
                            context,
                            android.Manifest.permission.POST_NOTIFICATIONS
                        ) != android.content.pm.PackageManager.PERMISSION_GRANTED
                    ) {
                        requestNotificationLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
            }

            // Auto-lock lifecycle observer
            DisposableEffect(lifecycleOwner) {
                val observer = LifecycleEventObserver { _, event ->
                    when (event) {
                        Lifecycle.Event.ON_STOP -> viewModel.onAppBackgrounded()
                        Lifecycle.Event.ON_START -> viewModel.onAppForegrounded()
                        else -> {}
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose {
                    lifecycleOwner.lifecycle.removeObserver(observer)
                }
            }

            MoneyLensTheme(themeMode = uiState.themeMode, themeAccent = uiState.themeAccent) {
                if (uiState.isAppLocked) {
                    LockScreen(
                        userName = uiState.userName,
                        userAvatar = uiState.userAvatar,
                        onUnlockSuccess = { viewModel.unlockWithBiometric() },
                        onVerifyPin = { pin -> viewModel.verifyPin(pin) },
                        isBiometricAvailable = uiState.isBiometricEnabled,
                        onBiometricClick = {
                            showBiometricPrompt(
                                onSuccess = { viewModel.unlockWithBiometric() },
                                onError = { msg ->
                                    android.widget.Toast.makeText(this@MainActivity, msg, android.widget.Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    )
                } else {
                    val snackbarHostState = remember { SnackbarHostState() }
                    val coroutineScope = rememberCoroutineScope()

                    // Danh sách màn hình chính hỗ trợ vuốt ngang
                    val mainScreens = remember {
                        listOf(
                            Screen.Overview,
                            Screen.Transactions,
                            Screen.Budget,
                            Screen.Investment,
                            Screen.Debts
                        )
                    }
                    val pagerState = rememberPagerState(initialPage = 0, pageCount = { mainScreens.size })
                    val screenBackstack = remember { mutableStateListOf<Screen>(Screen.Overview) }
                    val currentScreen = screenBackstack.lastOrNull() ?: Screen.Overview
                    val isMainScreen = currentScreen in mainScreens

                    // Đồng bộ chuyển đổi khi vuốt pager sang màn hình chính tương ứng
                    LaunchedEffect(pagerState.currentPage) {
                        val targetScreen = mainScreens[pagerState.currentPage]
                        if (currentScreen in mainScreens && currentScreen != targetScreen) {
                            if (screenBackstack.isNotEmpty()) {
                                screenBackstack[screenBackstack.lastIndex] = targetScreen
                            }
                        }
                    }

                    fun navigateTo(screen: Screen) {
                        if (screen in mainScreens) {
                            val targetIndex = mainScreens.indexOf(screen)
                            if (targetIndex >= 0) {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(targetIndex)
                                }
                            }
                            if (screenBackstack.lastOrNull() in mainScreens) {
                                screenBackstack[screenBackstack.lastIndex] = screen
                            } else {
                                screenBackstack.add(screen)
                            }
                        } else {
                            if (screenBackstack.lastOrNull() != screen) {
                                screenBackstack.add(screen)
                            }
                        }
                    }

                    // Android system back key handling
                    BackHandler(enabled = screenBackstack.size > 1) {
                        screenBackstack.removeAt(screenBackstack.lastIndex)
                        val newCurrent = screenBackstack.lastOrNull()
                        if (newCurrent in mainScreens) {
                            val targetIndex = mainScreens.indexOf(newCurrent)
                            if (targetIndex >= 0 && targetIndex != pagerState.currentPage) {
                                coroutineScope.launch {
                                    pagerState.scrollToPage(targetIndex)
                                }
                            }
                        }
                    }

                    var showQuickAddSheet by remember { mutableStateOf(false) }
                    var showInvestmentSheet by remember { mutableStateOf(false) }
                    var showAddCardSheet by remember { mutableStateOf(false) }
                    var showAddLoanType by remember { mutableStateOf<LoanType?>(null) }
                    var showBudgetSheet by remember { mutableStateOf(false) }
                    var selectedInvestmentAsset by remember { mutableStateOf<com.dvt.moneylens.domain.model.Asset?>(null) }
                    var selectedInvestmentAction by remember { mutableStateOf(com.dvt.moneylens.domain.model.InvestmentAction.BUY) }
                    var activePayCard by remember { mutableStateOf<CreditCard?>(null) }
                    var activeEditCard by remember { mutableStateOf<CreditCard?>(null) }
                    var activePayLoan by remember { mutableStateOf<Loan?>(null) }
                    var activeDetailLoan by remember { mutableStateOf<Loan?>(null) }
                    var activeEditLoan by remember { mutableStateOf<Loan?>(null) }


                    Scaffold(
                        snackbarHost = { SnackbarHost(snackbarHostState) },
                        bottomBar = {
                            NavigationBar(
                                containerColor = MaterialTheme.colorScheme.surface,
                                tonalElevation = 3.dp
                            ) {
                                val activeMainIndex = if (isMainScreen) pagerState.currentPage else -1

                                mainScreens.forEachIndexed { index, screen ->
                                    val selected = if (isMainScreen) activeMainIndex == index else currentScreen == screen
                                    NavigationBarItem(
                                        selected = selected,
                                        onClick = { navigateTo(screen) },
                                        icon = {
                                            Icon(
                                                imageVector = screen.icon,
                                                contentDescription = screen.label
                                            )
                                        },
                                        label = { Text(screen.label) },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = MaterialTheme.colorScheme.primary,
                                            selectedTextColor = MaterialTheme.colorScheme.primary,
                                            indicatorColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                                        )
                                    )
                                }
                            }
                        },
                        floatingActionButton = {
                            val isTxActive = if (isMainScreen) pagerState.currentPage == 1 else currentScreen == Screen.Transactions
                            AnimatedVisibility(
                                visible = isTxActive
                            ) {
                                FloatingActionButton(
                                    onClick = { showQuickAddSheet = true },
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary,
                                    shape = CircleShape
                                ) {
                                    Icon(imageVector = Icons.Rounded.Add, contentDescription = "Thêm giao dịch")
                                }
                            }
                        }
                    ) { innerPadding ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            if (isMainScreen) {
                                HorizontalPager(
                                    state = pagerState,
                                    modifier = Modifier.fillMaxSize()
                                ) { pageIndex ->
                                    when (mainScreens[pageIndex]) {
                                        Screen.Overview -> OverviewScreen(
                                            uiState = uiState,
                                            onQuickConfirmCategory = { tx, catId ->
                                                viewModel.confirmTransaction(tx.id, catId)
                                                coroutineScope.launch {
                                                    snackbarHostState.showSnackbar("Đã xác nhận giao dịch")
                                                }
                                            },
                                            onOpenConfirmSheet = { tx -> activeConfirmTx = tx },
                                            onViewAllTransactions = {
                                                viewModel.setTypeFilter(null)
                                                navigateTo(Screen.Transactions)
                                            },
                                            onNavigateToBudget = { navigateTo(Screen.Budget) },
                                            onNavigateToInvestment = { navigateTo(Screen.Investment) },
                                            onNavigateToCard = { cardId ->
                                                navigateTo(Screen.Debts)
                                            },
                                            onPayCardClick = { cardId ->
                                                activePayCard = uiState.creditCards.find { it.id == cardId }
                                            },
                                            onTogglePrivacy = { viewModel.togglePrivacy() },
                                            onOpenBudgetSettings = { showBudgetSheet = true },
                                            onOpenSettings = { navigateTo(Screen.Settings) },
                                            onNavigateToDebts = { navigateTo(Screen.Debts) },
                                            onSelectYear = { viewModel.setSelectedChartYear(it) }
                                        )

                                        Screen.Transactions -> TransactionsScreen(
                                            uiState = uiState,
                                            onSearchQueryChange = { viewModel.setSearchQuery(it) },
                                            onSelectTypeFilter = { viewModel.setTypeFilter(it) },
                                            onTogglePendingOnly = { viewModel.setShowOnlyPending(it) },
                                            onSelectMemberFilter = { viewModel.setMemberFilter(it) },
                                            onTransactionClick = { tx -> activeConfirmTx = tx },
                                            onTogglePrivacy = { viewModel.togglePrivacy() },
                                            onOpenSettings = { navigateTo(Screen.Settings) },
                                            onSelectYear = { viewModel.setSelectedChartYear(it) },
                                            onBatchConfirm = { ids, categoryId ->
                                                viewModel.confirmTransactionsWithCategory(ids, categoryId)
                                                coroutineScope.launch {
                                                    snackbarHostState.showSnackbar("Đã xác nhận ${ids.size} giao dịch")
                                                }
                                            },
                                            onBatchDelete = { ids ->
                                                viewModel.deleteTransactions(ids)
                                                coroutineScope.launch {
                                                    snackbarHostState.showSnackbar("Đã xóa ${ids.size} giao dịch")
                                                }
                                            },
                                            onDeleteTransaction = { tx ->
                                                viewModel.deleteTransaction(tx)
                                                coroutineScope.launch {
                                                    val result = snackbarHostState.showSnackbar(
                                                        message = "Đã xóa giao dịch",
                                                        actionLabel = "Hoàn tác",
                                                        duration = SnackbarDuration.Short
                                                    )
                                                    if (result == SnackbarResult.ActionPerformed) {
                                                        viewModel.undoDelete()
                                                    }
                                                }
                                            }
                                        )

                                        Screen.Budget -> BudgetScreen(
                                            uiState = uiState,
                                            onTogglePrivacy = { viewModel.togglePrivacy() },
                                            onOpenBudgetSettings = { showBudgetSheet = true },
                                            onOpenSettings = { navigateTo(Screen.Settings) }
                                        )

                                        Screen.Investment -> InvestmentScreen(
                                            uiState = uiState,
                                            onAddInvestmentClick = {
                                                selectedInvestmentAsset = null
                                                selectedInvestmentAction = com.dvt.moneylens.domain.model.InvestmentAction.BUY
                                                showInvestmentSheet = true
                                            },
                                            onAssetClick = { asset ->
                                                selectedInvestmentAsset = asset
                                                selectedInvestmentAction = com.dvt.moneylens.domain.model.InvestmentAction.BUY
                                                showInvestmentSheet = true
                                            },
                                            onAssetActionClick = { asset, action ->
                                                selectedInvestmentAsset = asset
                                                selectedInvestmentAction = action
                                                showInvestmentSheet = true
                                            },
                                            onRefreshPrices = {
                                                viewModel.refreshMarketPrices { count ->
                                                    coroutineScope.launch {
                                                        snackbarHostState.showSnackbar("Đã cập nhật giá thị trường ($count tài sản)")
                                                    }
                                                }
                                            },
                                            onTogglePrivacy = { viewModel.togglePrivacy() },
                                            onOpenSettings = { navigateTo(Screen.Settings) },
                                            onUpdateTaxConfig = { viewModel.updateTaxConfig(it) },
                                            onResetTaxConfig = { viewModel.resetTaxConfig() },
                                            onSaveFundSchedule = { viewModel.saveFundFeeSchedule(it) },
                                            onResetFundSchedule = { viewModel.resetFundFeeSchedule(it) },
                                            onOpenInvestmentGuide = { navigateTo(Screen.InvestmentGuide) },
                                            onUpdateAsset = { updatedAsset ->
                                                viewModel.updateAsset(updatedAsset)
                                                coroutineScope.launch {
                                                    snackbarHostState.showSnackbar("Đã cập nhật thông tin tài sản ${updatedAsset.symbol} thành công")
                                                }
                                            },
                                            onDeleteAsset = { assetId ->
                                                viewModel.deleteAsset(assetId)
                                                coroutineScope.launch {
                                                    snackbarHostState.showSnackbar("Đã xóa tài sản khỏi danh mục đầu tư")
                                                }
                                            },
                                            onDeleteAssetWithOption = { assetId, refundAccountId, refundAmountMinor ->
                                                viewModel.deleteAsset(assetId, refundAccountId, refundAmountMinor)
                                                coroutineScope.launch {
                                                    val msg = if (!refundAccountId.isNullOrBlank() && refundAmountMinor > 0)
                                                        "Đã xóa tài sản và hoàn tiền ${com.dvt.moneylens.domain.model.Money(refundAmountMinor).formatVnd()} về ví thành công"
                                                    else
                                                        "Đã xóa tài sản khỏi danh mục đầu tư"
                                                    snackbarHostState.showSnackbar(msg)
                                                }
                                            }
                                        )

                                        Screen.Debts -> CreditAndDebtsScreen(
                                            uiState = uiState,
                                            onCardClick = { cardId ->
                                                val c = uiState.creditCards.find { it.id == cardId }
                                                if (c != null && c.cardType == CardType.CREDIT) {
                                                    activePayCard = c
                                                } else if (c != null) {
                                                    activeEditCard = c
                                                }
                                            },
                                            onPayCardClick = { cardId ->
                                                activePayCard = uiState.creditCards.find { it.id == cardId }
                                            },
                                            onEditCardClick = { card ->
                                                activeEditCard = card
                                            },
                                            onDeleteCardClick = { cardId ->
                                                viewModel.deleteCreditCard(cardId)
                                                coroutineScope.launch {
                                                    snackbarHostState.showSnackbar("Đã xóa thẻ thành công")
                                                }
                                            },
                                            onAddCardClick = {
                                                showAddCardSheet = true
                                            },
                                            onLoanClick = { loan ->
                                                activeDetailLoan = loan
                                            },
                                            onEditLoanClick = { loan ->
                                                activeEditLoan = loan
                                            },
                                            onPayLoanClick = { loan ->
                                                activePayLoan = loan
                                            },
                                            onDeleteLoanClick = { loanId ->
                                                viewModel.deleteLoan(loanId)
                                                coroutineScope.launch {
                                                    snackbarHostState.showSnackbar("Đã xóa khoản nợ thành công")
                                                }
                                            },
                                            onAddLoanClick = { loanType ->
                                                showAddLoanType = loanType
                                            },
                                            onOpenSettings = { navigateTo(Screen.Settings) },
                                            onTogglePrivacy = { viewModel.togglePrivacy() }
                                        )

                                        else -> Unit
                                    }
                                }
                            } else {
                                when (currentScreen) {
                                    Screen.Settings -> SettingsScreen(
                                    uiState = uiState,
                                    onResetDemoData = {
                                        viewModel.resetToDemoData()
                                        coroutineScope.launch {
                                            snackbarHostState.showSnackbar("Đã nạp lại dữ liệu mẫu thành công")
                                        }
                                    },
                                    onClearAllData = {
                                        viewModel.clearAllData()
                                        coroutineScope.launch {
                                            snackbarHostState.showSnackbar("Đã xóa sạch toàn bộ dữ liệu")
                                        }
                                    },
                                    onToggleBank = { bankCode -> viewModel.toggleBankEnabled(bankCode) },
                                    onEnableAllBanks = { codes ->
                                        viewModel.enableAllBanks(codes)
                                        coroutineScope.launch {
                                            snackbarHostState.showSnackbar("Đã kích hoạt nhận diện tất cả ngân hàng")
                                        }
                                    },
                                    onDisableAllBanks = {
                                        viewModel.disableAllBanks()
                                        coroutineScope.launch {
                                            snackbarHostState.showSnackbar("Đã tạm tắt nhận diện tất cả ngân hàng")
                                        }
                                    },
                                    onSelectThemeMode = { viewModel.setThemeMode(it) },
                                    onSelectThemeAccent = { viewModel.setThemeAccent(it) },
                                    onTogglePinSecurity = { enabled, pin ->
                                        viewModel.togglePinSecurity(enabled, pin)
                                        coroutineScope.launch {
                                            snackbarHostState.showSnackbar(if (enabled) "Đã bật bảo mật mã PIN 6 số" else "Đã tắt bảo mật mã PIN")
                                        }
                                    },
                                    onToggleBiometric = { enabled ->
                                        viewModel.toggleBiometricSecurity(enabled)
                                        coroutineScope.launch {
                                            snackbarHostState.showSnackbar(if (enabled) "Đã bật mở khóa bằng sinh trắc học" else "Đã tắt mở khóa sinh trắc học")
                                        }
                                    },
                                    onVerifyPin = { viewModel.verifyPin(it) },
                                    onSelectAutoLockTimeout = { timeout ->
                                        viewModel.setAutoLockTimeout(timeout)
                                        coroutineScope.launch {
                                            snackbarHostState.showSnackbar("Đã cập nhật tự động khóa: ${timeout.label}")
                                        }
                                    },
                                    onUpdateUserInfo = { name, avatar ->
                                        viewModel.setUserInfo(name, avatar)
                                        coroutineScope.launch {
                                            snackbarHostState.showSnackbar("Đã lưu thông tin cá nhân: $avatar $name")
                                        }
                                    },
                                    onLockApp = { viewModel.lockApp() },
                                    onOpenInvestmentGuide = { navigateTo(Screen.InvestmentGuide) },
                                    onToggleNotificationCapture = { enabled ->
                                        viewModel.setNotificationCaptureEnabled(enabled)
                                        coroutineScope.launch {
                                            snackbarHostState.showSnackbar(if (enabled) "Đã bật tự động quét biến động số dư" else "Đã tạm dừng quét biến động số dư")
                                        }
                                    },
                                    onCleanSpuriousTransactions = {
                                        viewModel.cleanSpuriousTransactions { count ->
                                            coroutineScope.launch {
                                                snackbarHostState.showSnackbar("Đã dọn dẹp sạch $count giao dịch rác / lặp lại")
                                            }
                                        }
                                    },
                                    lastBackupTimestamp = lastBackupTimestamp,
                                    isBackupProcessing = isBackupProcessing,
                                    onExportBackup = { uri, onResult ->
                                        viewModel.exportBackupToUri(uri) { success, msg ->
                                            onResult(success, msg)
                                            if (success) {
                                                coroutineScope.launch {
                                                    snackbarHostState.showSnackbar("Đã xuất file sao lưu JSON thành công!")
                                                }
                                            }
                                        }
                                    },
                                    onReadBackupFile = { uri, onSuccess, onError ->
                                        viewModel.readAndParseBackupFromUri(uri, onSuccess, onError)
                                    },
                                    onRestoreBackup = { dto, onResult ->
                                        viewModel.restoreFromBackup(dto) { success, msg ->
                                            onResult(success, msg)
                                            if (success) {
                                                coroutineScope.launch {
                                                    snackbarHostState.showSnackbar("Đã khôi phục toàn bộ dữ liệu thành công!")
                                                }
                                            }
                                        }
                                    },
                                    onShareBackup = {
                                        viewModel.shareBackup(this@MainActivity) { errorMsg ->
                                            coroutineScope.launch {
                                                snackbarHostState.showSnackbar(errorMsg)
                                            }
                                        }
                                    },
                                    onNavigateBack = {
                                        if (screenBackstack.size > 1) {
                                            screenBackstack.removeAt(screenBackstack.lastIndex)
                                        } else {
                                            navigateTo(Screen.Overview)
                                        }
                                    }
                                )

                                Screen.InvestmentGuide -> InvestmentGuideScreen(
                                    onNavigateBack = {
                                        if (screenBackstack.size > 1) {
                                            screenBackstack.removeAt(screenBackstack.lastIndex)
                                        } else {
                                            navigateTo(Screen.Investment)
                                        }
                                    }
                                )

                                else -> Unit
                                }
                            }
                        }
                    }

                    // First-Time User Onboarding Dialog
                    if (!uiState.hasCompletedOnboarding) {
                        OnboardingDialog(
                            initialUserName = uiState.userName,
                            onComplete = { useDemoData, userName, userAvatar, pin, bio ->
                                viewModel.completeOnboarding(useDemoData, userName, userAvatar, pin, bio)
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("Chào mừng $userName đến với MoneyLens!")
                                }
                            }
                        )
                    }

                    // Quick Add Modal Bottom Sheet
                    if (showQuickAddSheet) {
                        QuickAddBottomSheet(
                            accounts = uiState.accounts,
                            categories = uiState.categories,
                            creditCards = uiState.creditCards,
                            onDismiss = { showQuickAddSheet = false },
                            onSave = { amount, type, catId, accId, note, paymentSource, creditCardId, occurredAt ->
                                viewModel.addQuickTransaction(amount, type, catId, accId, note, paymentSource, creditCardId, occurredAt)
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("Đã ghi nhận giao dịch thành công")
                                }
                            }
                        )
                    }

                    // Transaction Review / Confirmation Sheet
                    activeConfirmTx?.let { tx ->
                        ConfirmCategorySheet(
                            transaction = tx,
                            allCategories = uiState.categories,
                            creditCards = uiState.creditCards,
                            accounts = uiState.accounts,
                            onDismiss = { activeConfirmTx = null },
                            onConfirm = { confirmedTx ->
                                viewModel.confirmTransaction(confirmedTx)
                                activeConfirmTx = null
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("Đã duyệt giao dịch vào sổ cái và cập nhật tài sản ròng")
                                }
                            },
                            onDelete = { txToDelete ->
                                viewModel.deleteTransaction(txToDelete)
                                activeConfirmTx = null
                                coroutineScope.launch {
                                    val result = snackbarHostState.showSnackbar(
                                        message = "Đã bỏ qua giao dịch",
                                        actionLabel = "Hoàn tác",
                                        duration = SnackbarDuration.Short
                                    )
                                    if (result == SnackbarResult.ActionPerformed) {
                                        viewModel.undoDelete()
                                    }
                                }
                            },
                            onUpdate = { updatedTx ->
                                viewModel.updateTransaction(updatedTx)
                                activeConfirmTx = null
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("Đã cập nhật giao dịch thành công")
                                }
                            },
                            onConvertToInstallment = { title, counterparty, amount, term, feeMinor, feeSched, feePct ->
                                viewModel.convertCreditTxToInstallment(
                                    txId = tx.id,
                                    title = title,
                                    counterparty = counterparty,
                                    originalAmountMinor = amount,
                                    termMonths = term,
                                    conversionFeeMinor = feeMinor,
                                    conversionFeeSchedule = feeSched,
                                    conversionFeePercent = feePct
                                )
                                activeConfirmTx = null
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("Đã chuyển đổi sang khoản trả góp 0% và giảm dư nợ thẻ")
                                }
                            }
                        )
                    }

                    // Pay Credit Card Modal Bottom Sheet
                    activePayCard?.let { card ->
                        PayCreditCardSheet(
                            card = card,
                            accounts = uiState.accounts,
                            onDismiss = { activePayCard = null },
                            onConfirmPayment = { fromAccId, amount ->
                                viewModel.payCreditCard(card.id, fromAccId, amount)
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("Đã thanh toán thẻ thành công (chuyển khoản nội bộ)")
                                }
                            },
                            onDeleteCard = { cardId ->
                                viewModel.deleteCreditCard(cardId)
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("Đã xóa thẻ tín dụng thành công")
                                }
                            }
                        )
                    }

                    // Edit Credit Card Modal Bottom Sheet
                    activeEditCard?.let { card ->
                        EditCreditCardSheet(
                            card = card,
                            onDismiss = { activeEditCard = null },
                            onSave = { updatedCard ->
                                viewModel.updateCreditCard(updatedCard)
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("Đã cập nhật thông tin thẻ ${updatedCard.nickname} thành công")
                                }
                            }
                        )
                    }

                    // Add Investment Modal Bottom Sheet
                    if (showInvestmentSheet) {
                        AddInvestmentBottomSheet(
                            existingAssets = uiState.assets,
                            initialAsset = selectedInvestmentAsset,
                            initialAction = selectedInvestmentAction,
                            accounts = uiState.accounts,
                            onDismiss = {
                                showInvestmentSheet = false
                                selectedInvestmentAsset = null
                            },
                            onSave = { action, assetId, symbol, name, type, quantity, priceMinor, accountId, note, occurredAt ->
                                viewModel.executeInvestmentAction(
                                    action = action,
                                    assetId = assetId,
                                    symbol = symbol,
                                    name = name,
                                    assetType = type,
                                    quantity = quantity,
                                    priceMinor = priceMinor,
                                    accountId = accountId,
                                    note = note,
                                    occurredAt = occurredAt
                                )
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("Đã ghi nhận giao dịch đầu tư thành công")
                                }
                            }
                        )
                    }

                    // Add Credit Card Modal Bottom Sheet
                    if (showAddCardSheet) {
                        AddCreditCardSheet(
                            onDismiss = { showAddCardSheet = false },
                            onSave = { nickname, issuerBank, network, last4, limit, stmtDay, dueDays, colorHex, cardType, initBalance ->
                                viewModel.addCreditCard(
                                    nickname = nickname,
                                    issuerBank = issuerBank,
                                    network = network,
                                    last4 = last4,
                                    creditLimitMinor = limit,
                                    statementDay = stmtDay,
                                    dueDaysAfterStatement = dueDays,
                                    colorHex = colorHex,
                                    cardType = cardType,
                                    initialBalanceMinor = initBalance
                                )
                                coroutineScope.launch {
                                    val typeLabel = when (cardType) {
                                        CardType.CREDIT -> "thẻ tín dụng"
                                        CardType.DEBIT -> "thẻ ghi nợ (Debit)"
                                        CardType.WALLET -> "ví điện tử"
                                        CardType.BANK_ACCOUNT -> "tài khoản ngân hàng"
                                    }
                                    snackbarHostState.showSnackbar("Đã thêm $typeLabel $nickname ($issuerBank) thành công")
                                }
                            }
                        )
                    }

                    // Add Loan Modal Bottom Sheet
                    showAddLoanType?.let { loanType ->
                        AddLoanBottomSheet(
                            initialType = loanType,
                            accounts = uiState.accounts,
                            onDismiss = { showAddLoanType = null },
                            onSave = { title, counterparty, type, origAmount, rate, interestType, term, monthly, startDate, dueDay, notes, accId, color, feeMinor, feeSched, feePct ->
                                viewModel.addLoan(
                                    title = title,
                                    counterparty = counterparty,
                                    type = type,
                                    originalAmountMinor = origAmount,
                                    interestRate = rate,
                                    interestType = interestType,
                                    termMonths = term,
                                    monthlyPaymentMinor = monthly,
                                    startDate = startDate,
                                    dueDayOfMonth = dueDay,
                                    notes = notes,
                                    linkedAccountId = accId,
                                    colorHex = color,
                                    conversionFeeMinor = feeMinor,
                                    conversionFeeSchedule = feeSched,
                                    conversionFeePercent = feePct
                                )
                                coroutineScope.launch {
                                    val prefix = if (type == LoanType.BORROWING) "khoản vay/trả góp" else "khoản cho vay"
                                    snackbarHostState.showSnackbar("Đã thêm $prefix \"$title\" thành công")
                                }
                            }
                        )
                    }

                    // Active Detail Loan Sheet (Lịch sử các lần thu / trả nợ & Chi tiết hợp đồng)
                    activeDetailLoan?.let { loan ->
                        val liveLoan = uiState.loans.find { it.id == loan.id } ?: loan
                        val loanPayments = uiState.loanPayments.filter { it.loanId == loan.id }
                        LoanDetailBottomSheet(
                            loan = liveLoan,
                            payments = loanPayments,
                            accounts = uiState.accounts,
                            onDismiss = { activeDetailLoan = null },
                            onPayClick = {
                                activeDetailLoan = null
                                activePayLoan = liveLoan
                            },
                            onEditClick = {
                                activeDetailLoan = null
                                activeEditLoan = liveLoan
                            },
                            onDeleteClick = {
                                viewModel.deleteLoan(liveLoan.id)
                                activeDetailLoan = null
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("Đã xóa khoản nợ \"${liveLoan.title}\"")
                                }
                            },
                            onDeletePaymentClick = { paymentId ->
                                viewModel.deleteLoanPayment(paymentId, liveLoan.id)
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("Đã xóa lượt ghi nhận thu/trả nợ")
                                }
                            }
                        )
                    }

                    // Active Edit Loan Sheet (Cập nhật thông tin khoản vay/nợ/trả góp)
                    activeEditLoan?.let { loan ->
                        val liveLoan = uiState.loans.find { it.id == loan.id } ?: loan
                        EditLoanBottomSheet(
                            loan = liveLoan,
                            accounts = uiState.accounts,
                            onDismiss = { activeEditLoan = null },
                            onSave = { updated ->
                                viewModel.updateLoan(updated)
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("Đã cập nhật thông tin \"${updated.title}\" thành công")
                                }
                            }
                        )
                    }

                    // Pay / Collect Loan Modal Bottom Sheet
                    activePayLoan?.let { loan ->
                        val liveLoan = uiState.loans.find { it.id == loan.id } ?: loan
                        PayLoanBottomSheet(
                            loan = liveLoan,
                            accounts = uiState.accounts,
                            onDismiss = { activePayLoan = null },
                            onConfirmPayment = { amount, principal, interest, accId, note, paidAt ->
                                viewModel.payLoan(
                                    loanId = liveLoan.id,
                                    amountMinor = amount,
                                    principalMinor = principal,
                                    interestMinor = interest,
                                    accountId = accId,
                                    note = note,
                                    paidAt = paidAt
                                )
                                coroutineScope.launch {
                                    val actionName = if (liveLoan.type == LoanType.BORROWING) "trả nợ" else "thu hồi nợ"
                                    snackbarHostState.showSnackbar("Đã ghi nhận $actionName ${Money(amount).formatVnd()}")
                                }
                            }
                        )
                    }

                    // Set Budget Modal Bottom Sheet
                    if (showBudgetSheet) {
                        SetBudgetBottomSheet(
                            currentTotalBudgetMinor = uiState.totalBudgetLimitMinor,
                            categories = uiState.categories,
                            budgets = uiState.budgets,
                            onDismiss = { showBudgetSheet = false },
                            onSaveTotalBudget = { newTotal ->
                                viewModel.setMonthlyTotalBudget(newTotal)
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("Đã lưu hạn mức ngân sách: ${Money(newTotal).formatVnd()}")
                                }
                            },
                            onSaveCategoryBudget = { catId, limit ->
                                viewModel.updateCategoryBudget(catId, limit)
                            }
                        )
                    }
                }
            }
        }
    }

    companion object {
        const val EXTRA_REVIEW_TX_ID = "extra_review_tx_id"
    }
}
