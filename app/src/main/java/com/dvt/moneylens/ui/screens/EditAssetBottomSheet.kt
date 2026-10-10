package com.dvt.moneylens.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dvt.moneylens.domain.model.Asset
import com.dvt.moneylens.domain.model.AssetType
import com.dvt.moneylens.domain.model.Money
import com.dvt.moneylens.ui.designsystem.CurrencyAmountTextField
import com.dvt.moneylens.ui.designsystem.DateTimePickerField
import com.dvt.moneylens.ui.designsystem.VietnameseCurrencyFormatter
import com.dvt.moneylens.ui.theme.LocalMoneyLensColors
import com.dvt.moneylens.ui.theme.Spacing
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditAssetBottomSheet(
    asset: Asset,
    onDismiss: () -> Unit,
    onSave: (Asset) -> Unit,
    onDelete: ((String) -> Unit)? = null,
    onDeleteRequest: ((Asset) -> Unit)? = null
) {
    val lensColors = LocalMoneyLensColors.current

    var selectedType by remember { mutableStateOf(asset.type) }
    var symbol by remember { mutableStateOf(asset.symbol) }
    var name by remember { mutableStateOf(asset.name) }
    var quantityStr by remember {
        mutableStateOf(
            if (asset.quantity % 1.0 == 0.0) asset.quantity.toLong().toString()
            else String.format(Locale.US, "%.4f", asset.quantity).trimEnd('0').trimEnd('.')
        )
    }
    var costBasisStr by remember {
        mutableStateOf(VietnameseCurrencyFormatter.formatMinor(asset.costBasisMinor))
    }
    var currentPriceStr by remember {
        mutableStateOf(VietnameseCurrencyFormatter.formatMinor(asset.currentPriceMinor))
    }
    var notes by remember { mutableStateOf(asset.notes.orEmpty()) }
    var lastUpdatedTimestamp by remember(asset) { mutableStateOf(asset.lastUpdated) }

    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    val parsedQty = VietnameseCurrencyFormatter.parseToDouble(quantityStr)
    val parsedCostBasis = VietnameseCurrencyFormatter.parseToMinor(costBasisStr)
    val parsedCurrentPrice = VietnameseCurrencyFormatter.parseToMinor(currentPriceStr)

    val totalCost = (parsedQty * parsedCostBasis).toLong()
    val totalMarketValue = (parsedQty * parsedCurrentPrice).toLong()
    val pnlMinor = totalMarketValue - totalCost
    val isGain = pnlMinor >= 0
    val pnlPercent = if (totalCost > 0) (pnlMinor.toDouble() / totalCost) * 100.0 else 0.0

    val isFormValid = symbol.isNotBlank() && name.isNotBlank() && parsedQty > 0 && parsedCurrentPrice >= 0
    val focusManager = LocalFocusManager.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.lg, vertical = Spacing.xs),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Edit,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(Spacing.sm))
                    Column {
                        Text(
                            text = "Chỉnh sửa khoản đầu tư",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Cập nhật mã, tên, giá vốn, thị giá và ghi chú",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Rounded.Close, contentDescription = "Đóng")
                }
            }

            HorizontalDivider(modifier = Modifier.padding(top = Spacing.xs))

            // Scrollable Content
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Spacing.lg, vertical = Spacing.md),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                // Asset Type selector
                Text(
                    text = "Loại tài sản:",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
                ) {
                    items(AssetType.values()) { type ->
                        val label = when (type) {
                            AssetType.STOCK -> "Cổ phiếu"
                            AssetType.REAL_ESTATE -> "Bất động sản (BĐS)"
                            AssetType.GOLD -> "Vàng"
                            AssetType.CRYPTO -> "Crypto"
                            AssetType.FUND -> "Quỹ mở CCQ"
                        }
                        FilterChip(
                            selected = selectedType == type,
                            onClick = { selectedType = type },
                            label = { Text(label, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                // Symbol & Name
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    OutlinedTextField(
                        value = symbol,
                        onValueChange = { symbol = it.uppercase() },
                        label = { Text("Mã tài sản") },
                        placeholder = { Text("VD: VNM, SJC") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Right) }),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    )
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Tên tài sản") },
                        placeholder = { Text("VD: Vinamilk") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                        modifier = Modifier.weight(1.6f),
                        shape = RoundedCornerShape(10.dp)
                    )
                }

                // Quantity & Cost Basis
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    OutlinedTextField(
                        value = quantityStr,
                        onValueChange = { quantityStr = it },
                        label = { Text("Số lượng nắm giữ") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Right) }),
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    )

                    CurrencyAmountTextField(
                        value = costBasisStr,
                        onValueChange = { costBasisStr = it },
                        label = "Giá vốn / đv (VND)",
                        allowDecimals = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                        modifier = Modifier.weight(1.4f)
                    )
                }

                // Current Market Price
                CurrencyAmountTextField(
                    value = currentPriceStr,
                    onValueChange = { currentPriceStr = it },
                    label = "Giá thị trường hiện tại / đv (VND)",
                    allowDecimals = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                    supportingText = if (parsedCurrentPrice > 0L) {
                        { Text("Bằng chữ: ${Money(parsedCurrentPrice).formatVnd()} / đv") }
                    } else null,
                    modifier = Modifier.fillMaxWidth()
                )

                // Notes Field
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Ghi chú đầu tư") },
                    placeholder = { Text("Ghi chú mục tiêu đầu tư...") },
                    minLines = 2,
                    maxLines = 4,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.EditNote,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                // Date & Time Picker
                DateTimePickerField(
                    timestamp = lastUpdatedTimestamp,
                    onTimestampChange = { lastUpdatedTimestamp = it },
                    label = "Thời gian cập nhật / Ngày ghi nhận (Chính xác đến Ngày & Giờ)"
                )

                // Live Preview Summary Card
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(Spacing.md),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Tổng vốn đầu tư:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(Money(totalCost).formatVnd(), style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold))
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Tổng giá trị thị trường:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(Money(totalMarketValue).formatVnd(), style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary))
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Lãi/lỗ dự tính:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            val pnlColor = if (isGain) lensColors.income else lensColors.expense
                            val prefix = if (isGain) "+" else ""
                            Text(
                                text = "$prefix${Money(pnlMinor).formatVnd()} ($prefix${String.format(Locale.US, "%.1f", pnlPercent)}%)",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = pnlColor)
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Thời gian ghi nhận:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            val dtFormat = remember { SimpleDateFormat("HH:mm · dd/MM/yyyy", Locale.getDefault()) }
                            Text(dtFormat.format(Date(lastUpdatedTimestamp)), style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium))
                        }
                    }
                }
            }

            // Pinned Bottom Action Buttons
            Surface(
                tonalElevation = 2.dp,
                shadowElevation = 6.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.lg, vertical = Spacing.sm),
                    verticalArrangement = Arrangement.spacedBy(Spacing.xs)
                ) {
                    Button(
                        onClick = {
                            if (isFormValid) {
                                val updated = asset.copy(
                                    type = selectedType,
                                    symbol = symbol.trim().uppercase(),
                                    name = name.trim().ifBlank { symbol.trim().uppercase() },
                                    quantity = parsedQty,
                                    costBasisMinor = parsedCostBasis,
                                    currentPriceMinor = parsedCurrentPrice,
                                    notes = notes.trim().ifBlank { null },
                                    lastUpdated = lastUpdatedTimestamp
                                )
                                onSave(updated)
                                onDismiss()
                            }
                        },
                        enabled = isFormValid,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        Icon(imageVector = Icons.Rounded.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Lưu thay đổi", fontWeight = FontWeight.Bold)
                    }

                    if (onDeleteRequest != null || onDelete != null) {
                        TextButton(
                            onClick = {
                                if (onDeleteRequest != null) {
                                    onDeleteRequest(asset)
                                    onDismiss()
                                } else {
                                    showDeleteConfirmDialog = true
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.DeleteOutline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Xóa tài sản khỏi danh mục", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }

    if (showDeleteConfirmDialog && onDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Xác nhận xóa tài sản") },
            text = { Text("Bạn có chắc chắn muốn xóa tài sản ${asset.symbol} (${asset.name}) khỏi danh mục đầu tư? Thao tác này không thể hoàn tác.") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirmDialog = false
                        onDelete(asset.id)
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Xóa tài sản")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Hủy")
                }
            }
        )
    }
}
