package com.dvt.moneylens.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dvt.moneylens.domain.model.Money
import com.dvt.moneylens.domain.model.Transaction
import com.dvt.moneylens.domain.model.TransactionStatus
import com.dvt.moneylens.domain.model.TransactionType
import com.dvt.moneylens.ui.MainUiState
import com.dvt.moneylens.ui.designsystem.EmptyState
import com.dvt.moneylens.ui.designsystem.ExpenseAllocationPieChart
import com.dvt.moneylens.ui.designsystem.FintechScreenTopAppBar
import com.dvt.moneylens.ui.designsystem.MonthlyBarChart
import com.dvt.moneylens.ui.designsystem.PrivacyToggle
import com.dvt.moneylens.ui.designsystem.TransactionRow
import com.dvt.moneylens.ui.theme.LocalMoneyLensColors
import com.dvt.moneylens.ui.theme.Spacing
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(
    uiState: MainUiState,
    onSearchQueryChange: (String) -> Unit,
    onSelectTypeFilter: (TransactionType?) -> Unit,
    onTogglePendingOnly: (Boolean) -> Unit,
    onSelectMemberFilter: (String?) -> Unit,
    onTransactionClick: (Transaction) -> Unit,
    onTogglePrivacy: () -> Unit,
    onOpenSettings: () -> Unit = {},
    onSelectYear: (Int) -> Unit = {},
    onBatchConfirm: (List<String>, String?) -> Unit = { _, _ -> },
    onBatchDelete: (List<String>) -> Unit = {},
    onDeleteTransaction: (Transaction) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val lensColors = LocalMoneyLensColors.current
    var isSelectionMode by remember { mutableStateOf(false) }
    var selectedTxIds by remember { mutableStateOf(setOf<String>()) }
    var showBatchDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showBatchConfirmDialog by remember { mutableStateOf(false) }
    var selectedBatchCategoryId by remember { mutableStateOf<String?>(null) }
    var showTypeSelectMenu by remember { mutableStateOf(false) }

    val transactions = uiState.filteredTransactions

    // Clean selection if items no longer exist in the filtered list
    val currentFilteredIds = remember(transactions) { transactions.map { it.id }.toSet() }
    val validSelectedIds = remember(selectedTxIds, currentFilteredIds) {
        selectedTxIds.filter { currentFilteredIds.contains(it) }.toSet()
    }

    // Handle back button when in selection mode
    BackHandler(enabled = isSelectionMode) {
        isSelectionMode = false
        selectedTxIds = emptySet()
    }

    // Helper functions for selecting by type
    fun selectAll() {
        selectedTxIds = transactions.map { it.id }.toSet()
    }

    fun selectByType(type: TransactionType) {
        selectedTxIds = transactions.filter { it.type == type }.map { it.id }.toSet()
    }

    fun selectPendingOnly() {
        selectedTxIds = transactions.filter { it.status == TransactionStatus.PENDING }.map { it.id }.toSet()
    }

    fun clearSelection() {
        selectedTxIds = emptySet()
    }

    Scaffold(
        topBar = {
            if (isSelectionMode) {
                // Contextual Selection Top Bar
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shadowElevation = 3.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = Spacing.md, vertical = Spacing.sm),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = {
                                isSelectionMode = false
                                selectedTxIds = emptySet()
                            }) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = "Thoát chế độ chọn"
                                )
                            }
                            Spacer(modifier = Modifier.width(Spacing.xs))
                            Column {
                                Text(
                                    text = if (validSelectedIds.isEmpty()) "Chọn giao dịch" else "Đã chọn: ${validSelectedIds.size}",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = "Tổng ${transactions.size} giao dịch hiển thị",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Quick type dropdown selector
                            Box {
                                IconButton(onClick = { showTypeSelectMenu = true }) {
                                    Icon(
                                        imageVector = Icons.Rounded.FilterList,
                                        contentDescription = "Chọn theo loại"
                                    )
                                }
                                DropdownMenu(
                                    expanded = showTypeSelectMenu,
                                    onDismissRequest = { showTypeSelectMenu = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Chọn tất cả (${transactions.size})") },
                                        leadingIcon = { Icon(Icons.Rounded.SelectAll, contentDescription = null) },
                                        onClick = {
                                            selectAll()
                                            showTypeSelectMenu = false
                                        }
                                    )
                                    val pendingList = transactions.filter { it.status == TransactionStatus.PENDING }
                                    if (pendingList.isNotEmpty()) {
                                        DropdownMenuItem(
                                            text = { Text("Tất cả Chờ xác nhận (${pendingList.size})") },
                                            leadingIcon = { Icon(Icons.Rounded.HourglassTop, contentDescription = null) },
                                            onClick = {
                                                selectPendingOnly()
                                                showTypeSelectMenu = false
                                            }
                                        )
                                    }
                                    DropdownMenuItem(
                                        text = { Text("Tất cả Khoản chi (${transactions.count { it.type == TransactionType.EXPENSE }})") },
                                        leadingIcon = { Icon(Icons.Rounded.ArrowDownward, contentDescription = null, tint = lensColors.expense) },
                                        onClick = {
                                            selectByType(TransactionType.EXPENSE)
                                            showTypeSelectMenu = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Tất cả Khoản thu (${transactions.count { it.type == TransactionType.INCOME }})") },
                                        leadingIcon = { Icon(Icons.Rounded.ArrowUpward, contentDescription = null, tint = lensColors.income) },
                                        onClick = {
                                            selectByType(TransactionType.INCOME)
                                            showTypeSelectMenu = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Tất cả Chuyển khoản (${transactions.count { it.type == TransactionType.TRANSFER }})") },
                                        leadingIcon = { Icon(Icons.Rounded.SwapHoriz, contentDescription = null) },
                                        onClick = {
                                            selectByType(TransactionType.TRANSFER)
                                            showTypeSelectMenu = false
                                        }
                                    )
                                    HorizontalDivider()
                                    DropdownMenuItem(
                                        text = { Text("Bỏ chọn tất cả", color = MaterialTheme.colorScheme.error) },
                                        leadingIcon = { Icon(Icons.Rounded.Deselect, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                                        onClick = {
                                            clearSelection()
                                            showTypeSelectMenu = false
                                        }
                                    )
                                }
                            }

                            // Toggle Select All
                            val allSelected = transactions.isNotEmpty() && validSelectedIds.size == transactions.size
                            IconButton(onClick = {
                                if (allSelected) clearSelection() else selectAll()
                            }) {
                                Icon(
                                    imageVector = if (allSelected) Icons.Rounded.Deselect else Icons.Rounded.SelectAll,
                                    contentDescription = if (allSelected) "Bỏ chọn tất cả" else "Chọn tất cả"
                                )
                            }
                        }
                    }
                }
            } else {
                FintechScreenTopAppBar(
                    title = "Sổ giao dịch",
                    subtitle = "• ${uiState.transactions.size} giao dịch đã ghi nhận",
                    subtitleColor = MaterialTheme.colorScheme.primary,
                    icon = Icons.Rounded.ReceiptLong,
                    onOpenSettings = onOpenSettings,
                    actions = {
                        IconButton(
                            onClick = {
                                isSelectionMode = true
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Checklist,
                                contentDescription = "Chọn nhiều giao dịch",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        PrivacyToggle(isMasked = uiState.isMasked, onToggle = onTogglePrivacy)
                    }
                )
            }
        },
        bottomBar = {
            // Floating Batch Actions Bottom Bar when in selection mode
            AnimatedVisibility(
                visible = isSelectionMode,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 6.dp,
                    shadowElevation = 8.dp,
                    shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = Spacing.lg, vertical = Spacing.md)
                    ) {
                        val selectedCount = validSelectedIds.size
                        val selectedItems = remember(validSelectedIds, transactions) {
                            transactions.filter { validSelectedIds.contains(it.id) }
                        }
                        val totalSelectedAmount = remember(selectedItems) {
                            selectedItems.sumOf { it.amountMinor }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (selectedCount == 0) "Chưa chọn mục nào" else "Đã chọn: $selectedCount mục (${Money(totalSelectedAmount).formatVnd()})",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = if (selectedCount > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            TextButton(
                                onClick = {
                                    isSelectionMode = false
                                    selectedTxIds = emptySet()
                                }
                            ) {
                                Text("Thoát chọn")
                            }
                        }

                        Spacer(modifier = Modifier.height(Spacing.xs))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(Spacing.md)
                        ) {
                            // Button 1: Xác nhận hàng loạt (Batch Confirm)
                            Button(
                                onClick = {
                                    if (validSelectedIds.isNotEmpty()) {
                                        selectedBatchCategoryId = null
                                        showBatchConfirmDialog = true
                                    }
                                },
                                enabled = selectedCount > 0,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = lensColors.income,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.CheckCircle,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(Spacing.xs))
                                Text(
                                    text = if (selectedCount > 0) "Xác nhận ($selectedCount)" else "Xác nhận",
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // Button 2: Xóa hàng loạt (Batch Delete)
                            Button(
                                onClick = {
                                    if (validSelectedIds.isNotEmpty()) {
                                        showBatchDeleteConfirmDialog = true
                                    }
                                },
                                enabled = selectedCount > 0,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error,
                                    contentColor = MaterialTheme.colorScheme.onError
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Delete,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(Spacing.xs))
                                Text(
                                    text = if (selectedCount > 0) "Xóa ($selectedCount)" else "Xóa",
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // 1. Hai sub-tab: Biểu đồ & Phân bổ (mặc định) và Chi tiết GD
            var currentSubTab by remember { mutableIntStateOf(0) }

            if (!isSelectionMode) {
                TabRow(
                    selectedTabIndex = currentSubTab,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.lg, vertical = 2.dp)
                ) {
                    Tab(
                        selected = currentSubTab == 0,
                        onClick = { currentSubTab = 0 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Rounded.Insights,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Biểu đồ & Phân bổ",
                                    fontWeight = if (currentSubTab == 0) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    )
                    Tab(
                        selected = currentSubTab == 1,
                        onClick = { currentSubTab = 1 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Rounded.ReceiptLong,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Chi tiết GD (${transactions.size})",
                                    fontWeight = if (currentSubTab == 1) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    )
                }
            }

            if (currentSubTab == 0 && !isSelectionMode) {
                // TAB 0: BIỂU ĐỒ & PHÂN BỔ (MẶC ĐỊNH HIỂN THỊ)
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(Spacing.md)
                ) {
                    val cal = remember { Calendar.getInstance() }
                    val curMonth = remember { cal.get(Calendar.MONTH) + 1 }
                    val curYear = remember { cal.get(Calendar.YEAR) }

                    val curMonthTxs = remember(uiState.transactions) {
                        uiState.transactions.filter {
                            val c = Calendar.getInstance()
                            c.timeInMillis = it.occurredAt
                            c.get(Calendar.YEAR) == curYear && (c.get(Calendar.MONTH) + 1) == curMonth
                        }
                    }
                    val curMonthIncomeMinor = remember(curMonthTxs) {
                        curMonthTxs.filter { it.type == TransactionType.INCOME && it.status == TransactionStatus.CONFIRMED }.sumOf { it.amountMinor }
                    }
                    val curMonthExpenseMinor = remember(curMonthTxs) {
                        curMonthTxs.filter { it.type == TransactionType.EXPENSE && it.status == TransactionStatus.CONFIRMED && it.categoryId != "cat_investment" }.sumOf { it.amountMinor }
                    }
                    val curMonthSurplusMinor = curMonthIncomeMinor - curMonthExpenseMinor
                    val isSurplusPositive = curMonthSurplusMinor >= 0

                    // Thẻ Hero Tổng quan Tháng hiện tại: Thặng dư / Thu chi (đưa lên đầu tiên, cỡ chữ to, bố cục chống che số)
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Spacing.lg, vertical = Spacing.xs)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(Spacing.lg)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(if (isSurplusPositive) lensColors.income.copy(alpha = 0.15f) else lensColors.expense.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (isSurplusPositive) Icons.Rounded.TrendingUp else Icons.Rounded.TrendingDown,
                                        contentDescription = null,
                                        tint = if (isSurplusPositive) lensColors.income else lensColors.expense,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(Spacing.sm))
                                Column {
                                    Text(
                                        text = "Thặng dư ròng Tháng $curMonth/$curYear",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = if (isSurplusPositive) "Thu lớn hơn Chi trong tháng" else "Chi tiêu vượt mức Thu nhập",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                        color = if (isSurplusPositive) lensColors.income else lensColors.expense
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(Spacing.md))

                            // Số tiền thặng dư cực lớn chống che số
                            Text(
                                text = if (uiState.isMasked) "••••••••" else {
                                    val sign = if (isSurplusPositive) "+ " else "- "
                                    sign + Money(kotlin.math.abs(curMonthSurplusMinor)).formatVnd()
                                },
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 28.sp
                                ),
                                color = if (isSurplusPositive) lensColors.income else lensColors.expense,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            Spacer(modifier = Modifier.height(Spacing.md))

                            // 2 cột Thu nhập và Chi tiêu bên dưới
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = lensColors.income.copy(alpha = 0.08f),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(Spacing.md)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Rounded.ArrowDownward,
                                                contentDescription = null,
                                                tint = lensColors.income,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "Tổng thu",
                                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = if (uiState.isMasked) "••••••" else "+ ${Money(curMonthIncomeMinor).formatVnd()}",
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp
                                            ),
                                            color = lensColors.income,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = lensColors.expense.copy(alpha = 0.08f),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(Spacing.md)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Rounded.ArrowUpward,
                                                contentDescription = null,
                                                tint = lensColors.expense,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "Tổng chi",
                                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = if (uiState.isMasked) "••••••" else "- ${Money(curMonthExpenseMinor).formatVnd()}",
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp
                                            ),
                                            color = lensColors.expense,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Biểu đồ cột Thu Chi các tháng (tương tự tab Tổng quan)
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Spacing.lg)
                    ) {
                        MonthlyBarChart(
                            monthlyData = uiState.monthlyBarData,
                            selectedYear = uiState.selectedChartYear,
                            availableYears = uiState.availableChartYears,
                            onSelectYear = onSelectYear,
                            masked = uiState.isMasked,
                            modifier = Modifier.padding(vertical = Spacing.sm)
                        )
                    }

                    // Biểu đồ Tròn phân bổ chi tiêu (Toàn thời gian & Tháng này)
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Spacing.lg)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(Spacing.md)
                        ) {
                            var selectedPieScopeTab by remember { mutableIntStateOf(0) }

                            TabRow(
                                selectedTabIndex = selectedPieScopeTab,
                                containerColor = Color.Transparent,
                                contentColor = MaterialTheme.colorScheme.primary,
                                divider = {}
                            ) {
                                Tab(
                                    selected = selectedPieScopeTab == 0,
                                    onClick = { selectedPieScopeTab = 0 },
                                    text = {
                                        Text(
                                            text = "Toàn thời gian",
                                            fontSize = 12.sp,
                                            fontWeight = if (selectedPieScopeTab == 0) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                )
                                Tab(
                                    selected = selectedPieScopeTab == 1,
                                    onClick = { selectedPieScopeTab = 1 },
                                    text = {
                                        Text(
                                            text = "Tháng $curMonth/$curYear",
                                            fontSize = 12.sp,
                                            fontWeight = if (selectedPieScopeTab == 1) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                )
                            }

                            Spacer(modifier = Modifier.height(Spacing.xs))

                            if (selectedPieScopeTab == 0) {
                                ExpenseAllocationPieChart(
                                    title = "Tỷ trọng chi tiêu toàn thời gian",
                                    transactions = uiState.transactions,
                                    categories = uiState.categories,
                                    masked = uiState.isMasked
                                )
                            } else {
                                ExpenseAllocationPieChart(
                                    title = "Tỷ trọng chi tiêu tháng $curMonth/$curYear",
                                    transactions = curMonthTxs,
                                    categories = uiState.categories,
                                    masked = uiState.isMasked
                                )
                            }
                        }
                    }
                }
            } else {
                // TAB 1: CHI TIẾT GD (Hoặc khi đang chọn nhiều giao dịch)
                Column(modifier = Modifier.fillMaxSize()) {

            // Search Input
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = { Text("Tìm theo tên, đối tác, nội dung…") },
                leadingIcon = {
                    Icon(imageVector = Icons.Rounded.Search, contentDescription = null)
                },
                trailingIcon = {
                    if (uiState.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(imageVector = Icons.Rounded.Close, contentDescription = "Xóa tìm kiếm")
                        }
                    }
                },
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.lg, vertical = Spacing.xs)
            )

            // Selection Mode: Quick Type Selection Bar
            if (isSelectionMode) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = Spacing.lg, vertical = Spacing.xs),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
                ) {
                    val allCount = transactions.size
                    val pendingCount = transactions.count { it.status == TransactionStatus.PENDING }
                    val expenseCount = transactions.count { it.type == TransactionType.EXPENSE }
                    val incomeCount = transactions.count { it.type == TransactionType.INCOME }
                    val transferCount = transactions.count { it.type == TransactionType.TRANSFER }

                    AssistChip(
                        onClick = { selectAll() },
                        label = { Text("Tất cả ($allCount)", fontSize = 12.sp) },
                        leadingIcon = { Icon(Icons.Rounded.SelectAll, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        )
                    )

                    if (pendingCount > 0) {
                        AssistChip(
                            onClick = { selectPendingOnly() },
                            label = { Text("Chờ xác nhận ($pendingCount)", fontSize = 12.sp) },
                            leadingIcon = { Icon(Icons.Rounded.HourglassTop, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        )
                    }

                    AssistChip(
                        onClick = { selectByType(TransactionType.EXPENSE) },
                        label = { Text("Khoản chi ($expenseCount)", fontSize = 12.sp) },
                        leadingIcon = { Icon(Icons.Rounded.ArrowDownward, contentDescription = null, tint = lensColors.expense, modifier = Modifier.size(16.dp)) }
                    )

                    AssistChip(
                        onClick = { selectByType(TransactionType.INCOME) },
                        label = { Text("Khoản thu ($incomeCount)", fontSize = 12.sp) },
                        leadingIcon = { Icon(Icons.Rounded.ArrowUpward, contentDescription = null, tint = lensColors.income, modifier = Modifier.size(16.dp)) }
                    )

                    if (transferCount > 0) {
                        AssistChip(
                            onClick = { selectByType(TransactionType.TRANSFER) },
                            label = { Text("Chuyển khoản ($transferCount)", fontSize = 12.sp) },
                            leadingIcon = { Icon(Icons.Rounded.SwapHoriz, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        )
                    }

                    AssistChip(
                        onClick = { clearSelection() },
                        label = { Text("Bỏ chọn", fontSize = 12.sp, color = MaterialTheme.colorScheme.error) },
                        leadingIcon = { Icon(Icons.Rounded.Deselect, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp)) }
                    )
                }
            } else {
                // Standard Filter Chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.lg, vertical = Spacing.xs),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    FilterChip(
                        selected = uiState.selectedTypeFilter == null && !uiState.showOnlyPending,
                        onClick = {
                            onSelectTypeFilter(null)
                            onTogglePendingOnly(false)
                        },
                        label = { Text("Tất cả") }
                    )

                    FilterChip(
                        selected = uiState.selectedTypeFilter == TransactionType.EXPENSE,
                        onClick = {
                            onTogglePendingOnly(false)
                            onSelectTypeFilter(if (uiState.selectedTypeFilter == TransactionType.EXPENSE) null else TransactionType.EXPENSE)
                        },
                        label = { Text("Khoản chi") }
                    )

                    FilterChip(
                        selected = uiState.selectedTypeFilter == TransactionType.INCOME,
                        onClick = {
                            onTogglePendingOnly(false)
                            onSelectTypeFilter(if (uiState.selectedTypeFilter == TransactionType.INCOME) null else TransactionType.INCOME)
                        },
                        label = { Text("Khoản thu") }
                    )

                    val pendingCount = uiState.pendingTransactions.size
                    FilterChip(
                        selected = uiState.showOnlyPending,
                        onClick = {
                            onTogglePendingOnly(!uiState.showOnlyPending)
                            onSelectTypeFilter(null)
                        },
                        label = {
                            Text(if (pendingCount > 0) "Chờ xác nhận ($pendingCount)" else "Chờ xác nhận")
                        }
                    )
                }

                // Member filter chips if multiple members
                if (uiState.members.size > 1) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Spacing.lg, vertical = Spacing.xs),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
                    ) {
                        AssistChip(
                            onClick = { onSelectMemberFilter(null) },
                            label = { Text("Tất cả thành viên", style = MaterialTheme.typography.labelSmall) },
                            colors = if (uiState.selectedMemberId == null) {
                                AssistChipDefaults.assistChipColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                            } else AssistChipDefaults.assistChipColors()
                        )
                        uiState.members.forEach { member ->
                            AssistChip(
                                onClick = {
                                    onSelectMemberFilter(if (uiState.selectedMemberId == member.id) null else member.id)
                                },
                                label = { Text(member.displayName, style = MaterialTheme.typography.labelSmall) },
                                colors = if (uiState.selectedMemberId == member.id) {
                                    AssistChipDefaults.assistChipColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                                } else AssistChipDefaults.assistChipColors()
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(Spacing.xs))

            if (transactions.isEmpty()) {
                EmptyState(
                    icon = Icons.Rounded.ReceiptLong,
                    title = "Không tìm thấy giao dịch",
                    body = "Thử điều chỉnh bộ lọc hoặc từ khóa tìm kiếm để xem các giao dịch khác.",
                    actionLabel = "Xóa bộ lọc",
                    onActionClick = {
                        onSearchQueryChange("")
                        onSelectTypeFilter(null)
                        onTogglePendingOnly(false)
                        onSelectMemberFilter(null)
                    }
                )
            } else {
                // Group by Day (Sticky Header)
                val dayFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val displayDateFormat = SimpleDateFormat("EEEE, dd/MM", Locale.getDefault())
                val grouped = transactions.groupBy { dayFormat.format(Date(it.occurredAt)) }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = if (isSelectionMode) 120.dp else 96.dp)
                ) {
                    grouped.forEach { (dateKey, txList) ->
                        val sampleDate = Date(txList.first().occurredAt)
                        val headerTitle = when {
                            isToday(sampleDate) -> "Hôm nay · ${SimpleDateFormat("dd/MM", Locale.getDefault()).format(sampleDate)}"
                            isYesterday(sampleDate) -> "Hôm qua · ${SimpleDateFormat("dd/MM", Locale.getDefault()).format(sampleDate)}"
                            else -> displayDateFormat.format(sampleDate)
                        }

                        val dayTotalExpense = txList.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amountMinor }

                        item(key = "header_$dateKey") {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.background)
                                    .padding(horizontal = Spacing.lg, vertical = Spacing.sm),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = headerTitle,
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                if (dayTotalExpense > 0) {
                                    Text(
                                        text = "−${if (uiState.isMasked) "••••••" else Money(dayTotalExpense).formatVnd()}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        items(txList, key = { it.id }) { tx ->
                            val cat = uiState.categories.find { it.id == tx.categoryId }
                            val acc = uiState.accounts.find { it.id == tx.accountId }
                            val card = tx.creditCardId?.let { id -> uiState.creditCards.find { it.id == id } }
                                ?: uiState.creditCards.find { it.accountId == tx.accountId }
                            val isSelected = validSelectedIds.contains(tx.id)

                            TransactionRow(
                                transaction = tx,
                                category = cat,
                                account = acc,
                                creditCard = card,
                                onClick = { onTransactionClick(tx) },
                                masked = uiState.isMasked,
                                isSelectionMode = isSelectionMode,
                                isSelected = isSelected,
                                onToggleSelect = {
                                    selectedTxIds = if (isSelected) {
                                        selectedTxIds - tx.id
                                    } else {
                                        selectedTxIds + tx.id
                                    }
                                },
                                onLongClick = {
                                    if (!isSelectionMode) {
                                        isSelectionMode = true
                                        selectedTxIds = setOf(tx.id)
                                    }
                                },
                                onQuickDelete = { onDeleteTransaction(tx) }
                            )
                        }
                    }
                }
            }
        }
    }
    }
    }

    // Dialog chọn loại & danh mục khi xác nhận hàng loạt
    if (showBatchConfirmDialog) {
        val count = validSelectedIds.size
        val selectedItems = transactions.filter { validSelectedIds.contains(it.id) }
        val sumAmount = selectedItems.sumOf { it.amountMinor }

        AlertDialog(
            onDismissRequest = { showBatchConfirmDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Rounded.CheckCircle,
                    contentDescription = null,
                    tint = lensColors.income,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = "Xác nhận $count giao dịch",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Column {
                    Text(
                        text = "Bạn đang chuẩn bị xác nhận $count giao dịch đã chọn với tổng giá trị ${Money(sumAmount).formatVnd()}.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(Spacing.md))
                    Text(
                        text = "Tùy chọn gán danh mục đồng loạt:",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                    Spacer(modifier = Modifier.height(Spacing.xs))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
                    ) {
                        FilterChip(
                            selected = selectedBatchCategoryId == null,
                            onClick = { selectedBatchCategoryId = null },
                            label = { Text("Giữ nguyên danh mục") }
                        )

                        uiState.categories.forEach { cat ->
                            FilterChip(
                                selected = selectedBatchCategoryId == cat.id,
                                onClick = { selectedBatchCategoryId = cat.id },
                                label = { Text(cat.name) }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showBatchConfirmDialog = false
                        onBatchConfirm(validSelectedIds.toList(), selectedBatchCategoryId)
                        isSelectionMode = false
                        selectedTxIds = emptySet()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = lensColors.income)
                ) {
                    Text("Xác nhận $count mục")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBatchConfirmDialog = false }) {
                    Text("Hủy")
                }
            }
        )
    }

    // Dialog xác nhận xóa hàng loạt
    if (showBatchDeleteConfirmDialog) {
        val count = validSelectedIds.size
        val selectedItems = transactions.filter { validSelectedIds.contains(it.id) }
        val sumAmount = selectedItems.sumOf { it.amountMinor }

        AlertDialog(
            onDismissRequest = { showBatchDeleteConfirmDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Rounded.DeleteForever,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = "Xóa $count giao dịch đã chọn?",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Column {
                    Text(
                        text = "Toàn bộ $count giao dịch này (tổng giá trị ${Money(sumAmount).formatVnd()}) sẽ bị xóa vĩnh viễn khỏi sổ giao dịch và dữ liệu tài chính.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(Spacing.sm))
                    Text(
                        text = "Thao tác này không thể hoàn tác.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showBatchDeleteConfirmDialog = false
                        onBatchDelete(validSelectedIds.toList())
                        isSelectionMode = false
                        selectedTxIds = emptySet()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Xác nhận xóa $count mục")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBatchDeleteConfirmDialog = false }) {
                    Text("Hủy")
                }
            }
        )
    }
}

private fun isToday(date: Date): Boolean {
    val fmt = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
    return fmt.format(date) == fmt.format(Date())
}

private fun isYesterday(date: Date): Boolean {
    val fmt = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
    val cal = java.util.Calendar.getInstance()
    cal.add(java.util.Calendar.DAY_OF_YEAR, -1)
    return fmt.format(date) == fmt.format(cal.time)
}
