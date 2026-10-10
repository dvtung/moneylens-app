package com.dvt.moneylens.ui.designsystem

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dvt.moneylens.domain.model.Account
import com.dvt.moneylens.domain.model.AccountType
import com.dvt.moneylens.domain.model.CardType
import com.dvt.moneylens.domain.model.Category
import com.dvt.moneylens.domain.model.CreditCard
import com.dvt.moneylens.domain.model.Transaction
import com.dvt.moneylens.domain.model.TransactionStatus
import com.dvt.moneylens.domain.model.TransactionType
import com.dvt.moneylens.ui.theme.Spacing
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TransactionRow(
    transaction: Transaction,
    category: Category?,
    account: Account?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    creditCard: CreditCard? = null,
    masked: Boolean = false,
    showDivider: Boolean = true,
    isSelectionMode: Boolean = false,
    isSelected: Boolean = false,
    onToggleSelect: () -> Unit = {},
    onLongClick: (() -> Unit)? = null,
    onQuickDelete: (() -> Unit)? = null
) {
    val timeFormatted = SimpleDateFormat("HH:mm", Locale.getDefault())
        .format(Date(transaction.occurredAt))

    val title = transaction.counterparty?.takeIf { it.isNotBlank() }
        ?: transaction.note.takeIf { it.isNotBlank() }
        ?: category?.name
        ?: "Giao dịch"

    val categoryName = category?.name ?: "Chưa phân loại"
    val subtitle = "$categoryName · $timeFormatted"

    val sourceNickname = creditCard?.nickname
        ?: account?.name
        ?: "Tài khoản"

    val (badgeBgColor, badgeTextColor) = when {
        creditCard?.cardType == CardType.CREDIT || transaction.isCredit -> {
            MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
        }
        creditCard?.cardType == CardType.WALLET || account?.type == AccountType.EWALLET -> {
            MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
        }
        creditCard?.cardType == CardType.DEBIT || creditCard?.cardType == CardType.BANK_ACCOUNT || account?.type == AccountType.BANK -> {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.75f) to MaterialTheme.colorScheme.onPrimaryContainer
        }
        else -> {
            MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
        }
    }

    val rowBackground = if (isSelectionMode && isSelected) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.28f)
    } else {
        MaterialTheme.colorScheme.surface
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(rowBackground)
            .combinedClickable(
                onClick = {
                    if (isSelectionMode) {
                        onToggleSelect()
                    } else {
                        onClick()
                    }
                },
                onLongClick = onLongClick
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = Spacing.transactionRowHeight)
                .padding(horizontal = Spacing.lg, vertical = Spacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AnimatedVisibility(
                visible = isSelectionMode,
                enter = fadeIn() + expandHorizontally(),
                exit = fadeOut() + shrinkHorizontally()
            ) {
                Row {
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = { onToggleSelect() },
                        colors = CheckboxDefaults.colors(
                            checkedColor = MaterialTheme.colorScheme.primary,
                            checkmarkColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                    Spacer(modifier = Modifier.width(Spacing.xs))
                }
            }

            CategoryAvatar(category = category)

            Spacer(modifier = Modifier.width(Spacing.md))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = badgeBgColor,
                        modifier = Modifier.padding(end = 6.dp)
                    ) {
                        Text(
                            text = sourceNickname,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = badgeTextColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .widthIn(max = 130.dp)
                                .padding(horizontal = 5.dp, vertical = 1.5.dp)
                        )
                    }
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(Spacing.sm))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End
            ) {
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    MoneyText(
                        amountMinor = transaction.amountMinor,
                        type = transaction.type,
                        masked = masked,
                        style = MaterialTheme.typography.titleMedium
                    )
                    if (transaction.status == TransactionStatus.PENDING) {
                        Spacer(modifier = Modifier.height(Spacing.xs))
                        StatusPill(status = TransactionStatus.PENDING, type = transaction.type)
                    }
                }

                if (!isSelectionMode && onQuickDelete != null) {
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(
                        onClick = onQuickDelete,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.DeleteOutline,
                            contentDescription = "Xóa giao dịch",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        if (showDivider) {
            val dividerStart = if (isSelectionMode) 116.dp else 72.dp
            HorizontalDivider(
                modifier = Modifier.padding(start = dividerStart, end = Spacing.lg),
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                thickness = 0.8.dp
            )
        }
    }
}
