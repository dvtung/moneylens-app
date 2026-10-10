package com.dvt.moneylens.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dvt.moneylens.domain.model.CreditCard
import com.dvt.moneylens.domain.model.Loan
import com.dvt.moneylens.domain.model.LoanType
import com.dvt.moneylens.domain.model.Money
import com.dvt.moneylens.ui.MainUiState
import com.dvt.moneylens.ui.designsystem.CreditCardTile
import com.dvt.moneylens.ui.designsystem.EmptyState
import com.dvt.moneylens.ui.designsystem.FintechScreenTopAppBar
import com.dvt.moneylens.ui.designsystem.LoanCardTile
import com.dvt.moneylens.ui.designsystem.PrivacyToggle
import com.dvt.moneylens.ui.theme.LocalMoneyLensColors
import com.dvt.moneylens.ui.theme.MoneyHero
import com.dvt.moneylens.ui.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreditAndDebtsScreen(
    uiState: MainUiState,
    onCardClick: (String) -> Unit,
    onPayCardClick: (String) -> Unit,
    onEditCardClick: (CreditCard) -> Unit = {},
    onDeleteCardClick: (String) -> Unit = {},
    onAddCardClick: () -> Unit,
    onLoanClick: (Loan) -> Unit = {},
    onEditLoanClick: (Loan) -> Unit = {},
    onPayLoanClick: (Loan) -> Unit,
    onDeleteLoanClick: (String) -> Unit = {},
    onAddLoanClick: (LoanType) -> Unit,
    onOpenSettings: () -> Unit,
    onTogglePrivacy: () -> Unit,
    initialTab: Int = 0,
    modifier: Modifier = Modifier
) {
    val lensColors = LocalMoneyLensColors.current
    var selectedTab by remember { mutableIntStateOf(initialTab) }
    var cardToDelete by remember { mutableStateOf<CreditCard?>(null) }
    var loanToDelete by remember { mutableStateOf<Loan?>(null) }

    val borrowingLoans = remember(uiState.loans) {
        uiState.loans.filter { it.type == LoanType.BORROWING }
    }
    val lendingLoans = remember(uiState.loans) {
        uiState.loans.filter { it.type == LoanType.LENDING }
    }

    Scaffold(
        topBar = {
            val totalLiabilities = uiState.totalAllLiabilitiesMinor
            val totalCount = uiState.creditCards.size + uiState.loans.size
            FintechScreenTopAppBar(
                title = "Tín dụng & Nợ",
                subtitle = "• Nợ phải trả: ${if (uiState.isMasked) "••••••" else Money(totalLiabilities).formatVnd()} · $totalCount danh mục",
                subtitleColor = lensColors.expense,
                icon = Icons.Rounded.AccountBalanceWallet,
                onOpenSettings = onOpenSettings,
                actions = {
                    PrivacyToggle(isMasked = uiState.isMasked, onToggle = onTogglePrivacy)
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    when (selectedTab) {
                        0 -> onAddCardClick()
                        1 -> onAddLoanClick(LoanType.BORROWING)
                        2 -> onAddLoanClick(LoanType.LENDING)
                    }
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape
            ) {
                Icon(imageVector = Icons.Rounded.Add, contentDescription = "Thêm mới")
            }
        },
        modifier = modifier
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(bottom = 96.dp)
        ) {
            // Header: Tổng quan Tín dụng & Công nợ
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.lg, vertical = Spacing.xs)
                ) {
                    Column(modifier = Modifier.padding(Spacing.lg)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Tổng công nợ phải trả",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (uiState.isMasked) "•••••••• đ" else Money(uiState.totalAllLiabilitiesMinor).formatVnd(),
                                    style = MoneyHero,
                                    color = lensColors.expense
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "Tổng tiền cho vay",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (uiState.isMasked) "•••••••• đ" else Money(uiState.totalLendingReceivableMinor).formatVnd(),
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = lensColors.income
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(Spacing.md))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                        Spacer(modifier = Modifier.height(Spacing.sm))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Rounded.CreditCard,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Nợ thẻ: ${if (uiState.isMasked) "•••" else Money(uiState.totalCreditDebtMinor).formatVnd()}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Rounded.AccountBalance,
                                    contentDescription = null,
                                    tint = lensColors.expense,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Nợ vay: ${if (uiState.isMasked) "•••" else Money(uiState.totalBorrowingDebtMinor).formatVnd()}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Primary Tabs: Thẻ tín dụng | Đi vay & Trả góp | Cho vay & Thu nợ
            item {
                PrimaryTabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.background,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.lg, vertical = Spacing.xs)
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = {
                            Text(
                                text = "Thẻ & TK (${uiState.creditCards.size})",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal
                                )
                            )
                        }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = {
                            Text(
                                text = "Đi vay (${borrowingLoans.size})",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal
                                )
                            )
                        }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = {
                            Text(
                                text = "Cho vay (${lendingLoans.size})",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Normal
                                )
                            )
                        }
                    )
                }
            }

            // Tab 0: Thẻ tín dụng, Thẻ Debit, Ví, Tài khoản
            if (selectedTab == 0) {
                if (uiState.creditCards.isEmpty()) {
                    item {
                        EmptyState(
                            icon = Icons.Rounded.CreditCard,
                            title = "Chưa có thẻ, ví hay tài khoản",
                            body = "Thêm thẻ tín dụng, thẻ ghi nợ (Debit), ví điện tử hoặc tài khoản ngân hàng để tự động quản lý nguồn tiền và chu kỳ chi tiêu.",
                            actionLabel = "Thêm thẻ / tài khoản",
                            onActionClick = onAddCardClick
                        )
                    }
                } else {
                    items(uiState.creditCards, key = { it.id }) { card ->
                        CreditCardTile(
                            card = card,
                            onCardClick = { onCardClick(card.id) },
                            onPayClick = { onPayCardClick(card.id) },
                            onEditClick = { onEditCardClick(card) },
                            onDeleteClick = { cardToDelete = card },
                            masked = uiState.isMasked,
                            accountBalanceMinor = uiState.accounts.find { it.id == card.accountId }?.balanceMinor,
                            modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.xs)
                        )
                    }
                }
            }

            // Tab 1: Đi vay & Trả góp
            if (selectedTab == 1) {
                if (borrowingLoans.isEmpty()) {
                    item {
                        EmptyState(
                            icon = Icons.Rounded.RequestQuote,
                            title = "Chưa có khoản đi vay hay trả góp",
                            body = "Theo dõi các khoản vay mua nhà, mua xe, vay tiêu dùng hoặc mua sắm trả góp 0%.",
                            actionLabel = "Thêm khoản vay / trả góp",
                            onActionClick = { onAddLoanClick(LoanType.BORROWING) }
                        )
                    }
                } else {
                    items(borrowingLoans, key = { it.id }) { loan ->
                        LoanCardTile(
                            loan = loan,
                            onCardClick = { onLoanClick(loan) },
                            onEditClick = { onEditLoanClick(loan) },
                            onPayClick = { onPayLoanClick(loan) },
                            onDeleteClick = { loanToDelete = loan },
                            masked = uiState.isMasked
                        )
                    }
                }
            }

            // Tab 2: Cho vay & Thu nợ
            if (selectedTab == 2) {
                if (lendingLoans.isEmpty()) {
                    item {
                        EmptyState(
                            icon = Icons.Rounded.Handshake,
                            title = "Chưa có khoản cho vay",
                            body = "Ghi chép các khoản tiền cho người thân, bạn bè hoặc đối tác vay để nhắc lịch thu hồi nợ đúng hẹn.",
                            actionLabel = "Thêm khoản cho vay",
                            onActionClick = { onAddLoanClick(LoanType.LENDING) }
                        )
                    }
                } else {
                    items(lendingLoans, key = { it.id }) { loan ->
                        LoanCardTile(
                            loan = loan,
                            onCardClick = { onLoanClick(loan) },
                            onEditClick = { onEditLoanClick(loan) },
                            onPayClick = { onPayLoanClick(loan) },
                            onDeleteClick = { loanToDelete = loan },
                            masked = uiState.isMasked
                        )
                    }
                }
            }
        }

        // Delete Card Confirmation Dialog
        cardToDelete?.let { card ->
            AlertDialog(
                onDismissRequest = { cardToDelete = null },
                title = { Text("Xác nhận xóa thẻ") },
                text = { Text("Bạn có chắc chắn muốn xóa thẻ tín dụng ${card.nickname} (${card.issuerBank} ••••${card.last4}) không? Toàn bộ dữ liệu gắn với thẻ sẽ bị gỡ.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val id = card.id
                            cardToDelete = null
                            onDeleteCardClick(id)
                        }
                    ) {
                        Text("Xóa thẻ", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { cardToDelete = null }) {
                        Text("Hủy")
                    }
                }
            )
        }

        // Delete Loan Confirmation Dialog
        loanToDelete?.let { loan ->
            AlertDialog(
                onDismissRequest = { loanToDelete = null },
                title = { Text("Xác nhận xóa khoản ${if (loan.type == LoanType.BORROWING) "vay" else "cho vay"}") },
                text = { Text("Bạn có chắc chắn muốn xóa hợp đồng \"${loan.title}\" với ${loan.counterparty} không?") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val id = loan.id
                            loanToDelete = null
                            onDeleteLoanClick(id)
                        }
                    ) {
                        Text("Xóa", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { loanToDelete = null }) {
                        Text("Hủy")
                    }
                }
            )
        }
    }
}
