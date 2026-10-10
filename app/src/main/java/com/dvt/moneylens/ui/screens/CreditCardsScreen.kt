package com.dvt.moneylens.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dvt.moneylens.domain.model.CreditCard
import com.dvt.moneylens.domain.model.Money
import com.dvt.moneylens.ui.MainUiState
import com.dvt.moneylens.ui.designsystem.CreditCardTile
import com.dvt.moneylens.ui.designsystem.EmptyState
import com.dvt.moneylens.ui.designsystem.PrivacyToggle
import com.dvt.moneylens.ui.theme.MoneyHero
import com.dvt.moneylens.ui.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreditCardsScreen(
    uiState: MainUiState,
    onCardClick: (String) -> Unit,
    onPayCardClick: (String) -> Unit,
    onDeleteCardClick: (String) -> Unit = {},
    onAddCardClick: () -> Unit,
    onOpenSettings: () -> Unit,
    onTogglePrivacy: () -> Unit,
    modifier: Modifier = Modifier
) {
    var cardToDelete by remember { mutableStateOf<CreditCard?>(null) }
    val totalDebt = uiState.creditCards.sumOf { it.currentBalanceOwedMinor }
    val totalLimit = uiState.creditCards.sumOf { it.creditLimitMinor }
    val overallUtilization = if (totalLimit > 0) ((totalDebt.toDouble() / totalLimit) * 100).toInt() else 0

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Thẻ tín dụng",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(
                            imageVector = Icons.Rounded.Settings,
                            contentDescription = "Cài đặt",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    PrivacyToggle(isMasked = uiState.isMasked, onToggle = onTogglePrivacy)
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddCardClick,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = androidx.compose.foundation.shape.CircleShape
            ) {
                Icon(imageVector = Icons.Rounded.Add, contentDescription = "Thêm thẻ mới")
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
            // Overall Cards Summary Card
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.lg, vertical = Spacing.sm)
                ) {
                    Column(modifier = Modifier.padding(Spacing.lg)) {
                        Text(
                            text = "Tổng dư nợ thẻ hiện tại",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (uiState.isMasked) "••••••" else Money(totalDebt).formatVnd(),
                            style = MoneyHero,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(Spacing.md))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Tổng hạn mức: ${if (uiState.isMasked) "••••••" else Money(totalLimit).formatVnd()}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Mức sử dụng: $overallUtilization%",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            // Security note
            item {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.lg, vertical = Spacing.xs)
                ) {
                    Text(
                        text = "🔒 Bảo mật: MoneyLens chỉ lưu 4 số cuối của thẻ để nhắc hạn và tính sao kê.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(Spacing.md)
                    )
                }
            }

            if (uiState.creditCards.isEmpty()) {
                item {
                    EmptyState(
                        icon = Icons.Rounded.CreditCard,
                        title = "Chưa có thẻ hoặc ví",
                        body = "Thêm thẻ để MoneyLens tự động theo dõi số dư, chu kỳ sao kê và hạn thanh toán.",
                        actionLabel = "Thêm thẻ",
                        onActionClick = onAddCardClick
                    )
                }
            } else {
                items(uiState.creditCards, key = { it.id }) { card ->
                    CreditCardTile(
                        card = card,
                        onCardClick = { onCardClick(card.id) },
                        onPayClick = { onPayCardClick(card.id) },
                        onDeleteClick = { cardToDelete = card },
                        masked = uiState.isMasked,
                        accountBalanceMinor = uiState.accounts.find { it.id == card.accountId }?.balanceMinor,
                        modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.xs)
                    )
                }
            }
        }

        cardToDelete?.let { card ->
            AlertDialog(
                onDismissRequest = { cardToDelete = null },
                title = { Text("Xác nhận xóa thẻ") },
                text = { Text("Bạn có chắc chắn muốn xóa thẻ tín dụng ${card.nickname} (${card.issuerBank} ••••${card.last4}) không? Tài khoản liên kết và toàn bộ dữ liệu thẻ này sẽ bị xóa.") },
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
    }
}
