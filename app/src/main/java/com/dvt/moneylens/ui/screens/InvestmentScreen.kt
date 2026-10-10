package com.dvt.moneylens.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dvt.moneylens.domain.model.*
import com.dvt.moneylens.ui.MainUiState
import com.dvt.moneylens.ui.designsystem.FintechScreenTopAppBar
import com.dvt.moneylens.ui.designsystem.InvestmentDonutChart
import com.dvt.moneylens.ui.designsystem.getAssetTypeLabel
import com.dvt.moneylens.ui.designsystem.MoneyText
import com.dvt.moneylens.ui.designsystem.PrivacyToggle
import com.dvt.moneylens.ui.theme.LocalMoneyLensColors
import com.dvt.moneylens.ui.theme.MoneyHero
import com.dvt.moneylens.ui.theme.Spacing
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvestmentScreen(
    uiState: MainUiState,
    onAddInvestmentClick: () -> Unit,
    onAssetClick: (Asset) -> Unit,
    onAssetActionClick: ((Asset, InvestmentAction) -> Unit)? = null,
    onRefreshPrices: () -> Unit,
    onTogglePrivacy: () -> Unit,
    onOpenSettings: () -> Unit = {},
    onUpdateTaxConfig: (InvestmentTaxConfig) -> Unit = {},
    onResetTaxConfig: () -> Unit = {},
    onSaveFundSchedule: (FundExitFeeSchedule) -> Unit = {},
    onResetFundSchedule: (String) -> Unit = {},
    onOpenInvestmentGuide: () -> Unit = {},
    onUpdateAsset: (Asset) -> Unit = {},
    onDeleteAsset: (String) -> Unit = {},
    onDeleteAssetWithOption: (assetId: String, refundAccountId: String?, refundAmountMinor: Long) -> Unit = { id, _, _ -> onDeleteAsset(id) },
    modifier: Modifier = Modifier
) {
    val lensColors = LocalMoneyLensColors.current
    var selectedTypeFilter by remember { mutableStateOf<AssetType?>(null) }
    var showTaxConfigSheet by remember { mutableStateOf(false) }
    var selectedDetailAsset by remember { mutableStateOf<Asset?>(null) }
    var editingAsset by remember { mutableStateOf<Asset?>(null) }
    var assetToDelete by remember { mutableStateOf<Asset?>(null) }

    val filteredAssets = (if (selectedTypeFilter == null) {
        uiState.assets
    } else {
        uiState.assets.filter { it.type == selectedTypeFilter }
    }).sortedByDescending { it.lastUpdated }

    Scaffold(
        topBar = {
            FintechScreenTopAppBar(
                title = "Đầu tư",
                subtitle = null, // Loại bỏ thông tin tỷ suất P&L bên dưới Tiêu đề TopAppBar theo đúng yêu cầu
                icon = Icons.Rounded.TrendingUp,
                onOpenSettings = onOpenSettings,
                actions = {
                    IconButton(
                        onClick = { showTaxConfigSheet = true }
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Tune,
                            contentDescription = "Cài đặt thuế & phí",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(
                        onClick = onOpenInvestmentGuide
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.HelpOutline,
                            contentDescription = "Phương pháp tính đầu tư",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(
                        onClick = onRefreshPrices,
                        enabled = !uiState.isRefreshingPrices
                    ) {
                        if (uiState.isRefreshingPrices) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Rounded.Refresh,
                                contentDescription = "Làm mới giá thị trường",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    PrivacyToggle(isMasked = uiState.isMasked, onToggle = onTogglePrivacy)
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddInvestmentClick,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape
            ) {
                Icon(imageVector = Icons.Rounded.Add, contentDescription = "Thêm tài sản đầu tư")
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
            // Portfolio Summary Card
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
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Tổng giá trị tài sản đầu tư",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            // Tag cấu hình thuế phí
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                modifier = Modifier.clickable { showTaxConfigSheet = true }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Tune,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = "Thuế & phí",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (uiState.isMasked) "••••••" else Money(uiState.totalPortfolioValueMinor).formatVnd(),
                            style = MoneyHero,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(Spacing.sm))

                        // Net Liquidation Value Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Thanh lý ròng (sau thuế & phí):",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = if (uiState.isMasked) "••••••" else Money(uiState.totalNetLiquidationValueMinor).formatVnd(),
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        // Real Value after Inflation Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Thực sau lạm phát (${uiState.investmentTaxConfig.generalInflationRatePercent}%/năm):",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = if (uiState.isMasked) "••••••" else Money(uiState.totalNetRealLiquidationValueMinor).formatVnd(),
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFF9C27B0)
                            )
                        }

                        Spacer(modifier = Modifier.height(Spacing.xs))

                        val isNetGain = uiState.totalNetUnrealizedPnlMinor >= 0
                        val netPnlPrefix = if (isNetGain) "▲ +" else "▼ "
                        val netPnlColor = if (isNetGain) lensColors.income else lensColors.expense

                        val isRealGain = uiState.totalRealUnrealizedPnlMinor >= 0
                        val realPnlPrefix = if (isRealGain) "▲ +" else "▼ "
                        val realPnlColor = if (isRealGain) lensColors.income else lensColors.expense

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Lãi / lỗ ròng (thực tế sau lạm phát)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = if (uiState.isMasked) "••••••" else "$netPnlPrefix${Money(Math.abs(uiState.totalNetUnrealizedPnlMinor)).formatVnd()} (${String.format(Locale.US, "%.1f", uiState.overallNetPnlPercent)}%)",
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                        color = netPnlColor
                                    )
                                    Text(
                                        text = " · ",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                    Text(
                                        text = if (uiState.isMasked) "••••••" else "Thực: $realPnlPrefix${String.format(Locale.US, "%.1f", uiState.overallRealPnlPercent)}%",
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                        color = realPnlColor
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(
                                    text = "Giá vốn: ${if (uiState.isMasked) "••••••" else Money(uiState.totalCostBasisMinor).formatVnd()}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = Spacing.sm, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(Spacing.sm))

                        // Market Price Status Pill
                        val timeFormat = remember { java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()) }
                        val timeStr = timeFormat.format(java.util.Date(uiState.lastPricesRefreshedAt))
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(if (uiState.isOfflineMode) MaterialTheme.colorScheme.onSurfaceVariant else lensColors.income)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (uiState.isOfflineMode) "Chế độ ngoại tuyến (giá cục bộ)" else "Giá thị trường tự động · Cập nhật $timeStr",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Investment Donut Allocation Chart
            item {
                InvestmentDonutChart(
                    assets = uiState.assets,
                    totalValueMinor = uiState.totalPortfolioValueMinor,
                    masked = uiState.isMasked,
                    modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.xs)
                )
            }

            // Asset Type Filter Chips
            item {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = Spacing.xs),
                    contentPadding = PaddingValues(horizontal = Spacing.lg),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
                ) {
                    item {
                        FilterChip(
                            selected = selectedTypeFilter == null,
                            onClick = { selectedTypeFilter = null },
                            label = { Text("Tất cả (${uiState.assets.size})", fontWeight = if (selectedTypeFilter == null) FontWeight.Bold else FontWeight.Normal) }
                        )
                    }
                    items(AssetType.values()) { type ->
                        val count = uiState.assets.count { it.type == type }
                        val label = when (type) {
                            AssetType.STOCK -> "Cổ phiếu"
                            AssetType.REAL_ESTATE -> "BĐS"
                            AssetType.GOLD -> "Vàng"
                            AssetType.CRYPTO -> "Crypto"
                            AssetType.FUND -> "Quỹ mở"
                        }
                        FilterChip(
                            selected = selectedTypeFilter == type,
                            onClick = { selectedTypeFilter = if (selectedTypeFilter == type) null else type },
                            label = { Text("$label ($count)", fontWeight = if (selectedTypeFilter == type) FontWeight.Bold else FontWeight.Normal) }
                        )
                    }
                }
            }

            // Assets List with Enhanced Financial Indicators
            items(filteredAssets, key = { it.id }) { asset ->
                val metrics = remember(asset, uiState.investmentTaxConfig, uiState.fundFeeSchedules) {
                    InvestmentCalculations.calculateAssetMetrics(asset, uiState.investmentTaxConfig, uiState.fundFeeSchedules)
                }

                val isNetGain = metrics.netUnrealizedPnlMinor >= 0
                val assetPnlColor = if (isNetGain) lensColors.income else lensColors.expense
                val assetPnlPrefix = if (isNetGain) "▲ +" else "▼ "

                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.lg, vertical = 4.dp)
                        .clickable { selectedDetailAsset = asset }
                ) {
                    Column(modifier = Modifier.padding(Spacing.md)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(getAssetTypeColor(asset.type).copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = asset.symbol.take(3),
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = getAssetTypeColor(asset.type)
                                    )
                                }

                                Spacer(modifier = Modifier.width(Spacing.sm))

                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = asset.symbol,
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = getAssetTypeColor(asset.type).copy(alpha = 0.12f)
                                        ) {
                                            Text(
                                                text = getAssetTypeLabel(asset.type),
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                                                color = getAssetTypeColor(asset.type),
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = "${asset.name} · SL: ${asset.quantity}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = if (uiState.isMasked) "••••••" else asset.currentTotalValue.formatVnd(),
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Text(
                                        text = if (uiState.isMasked) "••••••" else "$assetPnlPrefix${String.format(Locale.US, "%.1f", metrics.netRoiPercent)}% ròng",
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                        color = assetPnlColor
                                    )
                                }
                                Spacer(modifier = Modifier.width(4.dp))
                                IconButton(
                                    onClick = { editingAsset = asset },
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Edit,
                                        contentDescription = "Chỉnh sửa khoản đầu tư",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                IconButton(
                                    onClick = { assetToDelete = asset },
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.DeleteOutline,
                                        contentDescription = "Xóa khoản đầu tư",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(Spacing.xs))

                        // Market Price & Cost Basis
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Giá TT: ${Money(asset.currentPriceMinor).formatVnd()} / đv",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Giá vốn: ${Money(asset.costBasisMinor).formatVnd()} / đv",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Net Liquidation Value & Tax Notice
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Thanh lý ròng: ${if (uiState.isMasked) "••••••" else Money(metrics.netLiquidationValueMinor).formatVnd()}",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )

                            Text(
                                text = when (asset.type) {
                                    AssetType.GOLD -> "Bid Price tiệm"
                                    AssetType.FUND -> "Biểu phí FIFO"
                                    AssetType.REAL_ESTATE -> "Trừ 3% thuế&MG"
                                    AssetType.STOCK -> "Trừ thuế 0.1% + phí GD"
                                    AssetType.CRYPTO -> "Phí sàn 0.1%"
                                },
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = MaterialTheme.colorScheme.outline
                            )
                        }

                        // Real Value after Inflation
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val realPrefix = if (metrics.realRoiPercent >= 0) "▲ +" else "▼ "
                            val realColor = if (metrics.realRoiPercent >= 0) lensColors.income else lensColors.expense
                            Text(
                                text = "Thực sau lạm phát (${metrics.inflationRatePercent}%/năm):",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${if (uiState.isMasked) "••••••" else Money(metrics.realLiquidationValueMinor).formatVnd()} ($realPrefix${String.format(Locale.US, "%.1f", metrics.realRoiPercent)}%)",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                                color = realColor
                            )
                        }

                        if (!asset.notes.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.EditNote,
                                    contentDescription = null,
                                    modifier = Modifier.size(15.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = asset.notes,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 11.sp,
                                        fontStyle = FontStyle.Italic
                                    ),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            // Legal Disclaimer
            item {
                Text(
                    text = "ⓘ Thông tin và công thức tính toán chỉ mang tính tham khảo quản lý tài chính cá nhân, không cấu thành khuyến nghị đầu tư.",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.lg)
                )
            }
        }
    }

    // Modal Cài đặt Thuế & Phí Đầu tư
    if (showTaxConfigSheet) {
        InvestmentTaxConfigSheet(
            initialConfig = uiState.investmentTaxConfig,
            fundFeeSchedules = uiState.fundFeeSchedules,
            onDismiss = { showTaxConfigSheet = false },
            onSave = { onUpdateTaxConfig(it) },
            onResetToDefault = onResetTaxConfig,
            onSaveFundSchedule = onSaveFundSchedule,
            onResetFundSchedule = onResetFundSchedule
        )
    }

    // Modal Chi tiết Toán học & Thanh lý Tài sản & Lịch sử Giao dịch
    selectedDetailAsset?.let { asset ->
        AssetFinancialDetailSheet(
            asset = asset,
            transactions = uiState.getInvestmentTransactionsForAsset(asset.id),
            config = uiState.investmentTaxConfig,
            fundFeeSchedules = uiState.fundFeeSchedules,
            masked = uiState.isMasked,
            onDismiss = { selectedDetailAsset = null },
            onExecuteAction = { targetAsset, action ->
                if (onAssetActionClick != null) {
                    onAssetActionClick(targetAsset, action)
                } else {
                    onAssetClick(targetAsset)
                }
            },
            onEditAsset = { targetAsset ->
                editingAsset = targetAsset
                selectedDetailAsset = null
            },
            onDeleteAsset = { targetAsset ->
                assetToDelete = targetAsset
            },
            onSaveFundSchedule = onSaveFundSchedule,
            onResetFundSchedule = onResetFundSchedule
        )
    }

    // Modal Chỉnh sửa thông tin khoản đầu tư
    editingAsset?.let { assetToEdit ->
        EditAssetBottomSheet(
            asset = assetToEdit,
            onDismiss = { editingAsset = null },
            onSave = { updated ->
                onUpdateAsset(updated)
                editingAsset = null
            },
            onDelete = { id ->
                onDeleteAsset(id)
                editingAsset = null
                if (selectedDetailAsset?.id == id) {
                    selectedDetailAsset = null
                }
            },
            onDeleteRequest = { targetAsset ->
                assetToDelete = targetAsset
            }
        )
    }

    // Hộp thoại Xác nhận Xóa khoản đầu tư (kèm tùy chọn Hoàn tiền về ví hoặc chỉ xóa danh mục)
    assetToDelete?.let { assetItem ->
        DeleteInvestmentDialog(
            asset = assetItem,
            accounts = uiState.accounts,
            onDismiss = { assetToDelete = null },
            onConfirm = { refundToAccount, refundAccountId, refundAmountMinor ->
                val idToDelete = assetItem.id
                assetToDelete = null
                if (selectedDetailAsset?.id == idToDelete) {
                    selectedDetailAsset = null
                }
                if (editingAsset?.id == idToDelete) {
                    editingAsset = null
                }
                onDeleteAssetWithOption(
                    idToDelete,
                    if (refundToAccount) refundAccountId else null,
                    if (refundToAccount) refundAmountMinor else 0L
                )
            }
        )
    }
}

private fun getAssetTypeColor(type: AssetType): Color {
    return when (type) {
        AssetType.STOCK -> Color(0xFF1E88E5)
        AssetType.GOLD -> Color(0xFFE8A317)
        AssetType.CRYPTO -> Color(0xFF8E24AA)
        AssetType.FUND -> Color(0xFF0A6B5C)
        AssetType.REAL_ESTATE -> Color(0xFFFF7043)
    }
}
