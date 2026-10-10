package com.dvt.moneylens.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dvt.moneylens.domain.model.Account
import com.dvt.moneylens.domain.model.Asset
import com.dvt.moneylens.domain.model.Money
import com.dvt.moneylens.ui.designsystem.VietnameseCurrencyFormatter
import com.dvt.moneylens.ui.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeleteInvestmentDialog(
    asset: Asset,
    accounts: List<Account>,
    onDismiss: () -> Unit,
    onConfirm: (refundToAccount: Boolean, refundAccountId: String?, refundAmountMinor: Long) -> Unit
) {
    val eligibleAccounts = remember(accounts) {
        accounts.filter { it.type.name != "CREDIT_CARD" && !it.isArchived }
    }

    var refundToWallet by remember { mutableStateOf(true) }
    var selectedAccountId by remember {
        mutableStateOf<String?>(eligibleAccounts.firstOrNull()?.id)
    }
    var refundAmountStr by remember {
        mutableStateOf(VietnameseCurrencyFormatter.formatMinor(asset.totalCostBasisMinor))
    }
    var accountDropdownExpanded by remember { mutableStateOf(false) }

    val parsedRefundAmount = remember(refundAmountStr) {
        VietnameseCurrencyFormatter.parseToMinor(refundAmountStr)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.errorContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.DeleteForever,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Column {
                    Text(
                        text = "Xóa khoản đầu tư",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "${asset.symbol} · ${asset.name}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {
                // Thẻ thông tin tóm tắt tài sản hiện tại
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Số lượng đang nắm giữ:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${asset.quantity} đv", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold))
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Tổng vốn ban đầu:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(Money(asset.totalCostBasisMinor).formatVnd(), style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold))
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Giá trị thị trường:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(asset.currentTotalValue.formatVnd(), style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary))
                        }
                    }
                }

                Text(
                    text = "Lựa chọn xử lý số tiền và danh mục:",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                )

                // Lựa chọn 1: Hoàn tiền về ví
                val isOpt1Selected = refundToWallet
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isOpt1Selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f) else MaterialTheme.colorScheme.surface,
                    border = androidx.compose.foundation.BorderStroke(
                        width = if (isOpt1Selected) 1.5.dp else 1.dp,
                        color = if (isOpt1Selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { refundToWallet = true }
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        RadioButton(
                            selected = isOpt1Selected,
                            onClick = { refundToWallet = true }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Hoàn tiền về ví / tài khoản",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "Cộng lại số tiền thu hồi vào tài khoản thanh toán và ghi nhận giao dịch thu hồi vốn.",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Chi tiết hoàn tiền khi chọn Tùy chọn 1
                if (refundToWallet) {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 8.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Chọn ví nhận hoàn tiền
                            Text("Ví / Tài khoản nhận tiền:", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold))
                            ExposedDropdownMenuBox(
                                expanded = accountDropdownExpanded,
                                onExpandedChange = { accountDropdownExpanded = !accountDropdownExpanded },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                val selectedAccount = eligibleAccounts.find { it.id == selectedAccountId }
                                OutlinedTextField(
                                    value = selectedAccount?.let { "${it.name} (${Money(it.balanceMinor).formatVnd()})" } ?: "Chọn tài khoản",
                                    onValueChange = {},
                                    readOnly = true,
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = accountDropdownExpanded) },
                                    modifier = Modifier
                                        .menuAnchor()
                                        .fillMaxWidth(),
                                    textStyle = MaterialTheme.typography.bodySmall
                                )
                                ExposedDropdownMenu(
                                    expanded = accountDropdownExpanded,
                                    onDismissRequest = { accountDropdownExpanded = false }
                                ) {
                                    eligibleAccounts.forEach { acc ->
                                        DropdownMenuItem(
                                            text = {
                                                Column {
                                                    Text(acc.name, style = MaterialTheme.typography.bodyMedium)
                                                    Text("Số dư: ${Money(acc.balanceMinor).formatVnd()}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                }
                                            },
                                            onClick = {
                                                selectedAccountId = acc.id
                                                accountDropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }

                            // Số tiền hoàn lại
                            Text("Số tiền hoàn lại (đ):", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold))
                            OutlinedTextField(
                                value = refundAmountStr,
                                onValueChange = { refundAmountStr = it },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                trailingIcon = {
                                    Text("đ", modifier = Modifier.padding(end = 12.dp), style = MaterialTheme.typography.bodySmall)
                                },
                                supportingText = {
                                    Text(
                                        text = "Mặc định bằng tổng vốn gốc ban đầu (${Money(asset.totalCostBasisMinor).formatVnd()})",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp)
                                    )
                                },
                                textStyle = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }

                // Lựa chọn 2: Chỉ xóa tài sản
                val isOpt2Selected = !refundToWallet
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isOpt2Selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f) else MaterialTheme.colorScheme.surface,
                    border = androidx.compose.foundation.BorderStroke(
                        width = if (isOpt2Selected) 1.5.dp else 1.dp,
                        color = if (isOpt2Selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { refundToWallet = false }
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        RadioButton(
                            selected = isOpt2Selected,
                            onClick = { refundToWallet = false }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Chỉ xóa tài sản khỏi danh mục",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "Giữ nguyên số dư hiện tại của tất cả các ví. Chỉ loại bỏ tài sản khỏi danh sách theo dõi đầu tư.",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Chú thích tác động Tài sản ròng
                Card(
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Tài sản ròng sẽ tự động tính toán lại ngay sau khi xóa để phản ánh chính xác số dư và danh mục.",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, lineHeight = 15.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(
                        refundToWallet,
                        if (refundToWallet) selectedAccountId else null,
                        if (refundToWallet) parsedRefundAmount else 0L
                    )
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Icon(imageVector = Icons.Rounded.DeleteForever, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Xác nhận xóa", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Hủy")
            }
        }
    )
}
