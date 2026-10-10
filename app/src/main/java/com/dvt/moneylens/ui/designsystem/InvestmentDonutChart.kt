package com.dvt.moneylens.ui.designsystem

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dvt.moneylens.domain.model.Asset
import com.dvt.moneylens.domain.model.AssetType
import com.dvt.moneylens.domain.model.Money
import com.dvt.moneylens.ui.theme.Spacing
import java.util.Locale

data class DonutSliceData(
    val type: AssetType,
    val label: String,
    val valueMinor: Long,
    val percentage: Double,
    val color: Color
)

fun getAssetTypeColor(type: AssetType): Color {
    return when (type) {
        AssetType.STOCK -> Color(0xFF1E88E5)
        AssetType.CRYPTO -> Color(0xFF8E24AA)
        AssetType.GOLD -> Color(0xFFF59E0B)
        AssetType.FUND -> Color(0xFF0A6B5C)
        AssetType.REAL_ESTATE -> Color(0xFFFF7043)
    }
}

fun getAssetTypeLabel(type: AssetType): String {
    return when (type) {
        AssetType.STOCK -> "Cổ phiếu"
        AssetType.CRYPTO -> "Tiền mã hóa"
        AssetType.GOLD -> "Vàng & Kim loại"
        AssetType.FUND -> "Quỹ mở & ETF"
        AssetType.REAL_ESTATE -> "Bất động sản (BĐS)"
    }
}

@Composable
fun InvestmentDonutChart(
    assets: List<Asset>,
    totalValueMinor: Long,
    masked: Boolean = false,
    modifier: Modifier = Modifier
) {
    val slices = remember(assets, totalValueMinor) {
        if (totalValueMinor <= 0L || assets.isEmpty()) {
            emptyList()
        } else {
            AssetType.values().mapNotNull { type ->
                val typeAssets = assets.filter { it.type == type }
                val typeTotal = typeAssets.sumOf { it.currentTotalValueMinor }
                if (typeTotal > 0L) {
                    val pct = (typeTotal.toDouble() / totalValueMinor) * 100.0
                    DonutSliceData(
                        type = type,
                        label = getAssetTypeLabel(type),
                        valueMinor = typeTotal,
                        percentage = pct,
                        color = getAssetTypeColor(type)
                    )
                } else null
            }.sortedByDescending { it.valueMinor }
        }
    }

    val animationProgress = remember { Animatable(0f) }
    LaunchedEffect(slices) {
        animationProgress.snapTo(0f)
        animationProgress.animateTo(1f, animationSpec = tween(durationMillis = 800))
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.md),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Phân bổ danh mục đầu tư",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${slices.size} nhóm tài sản",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(Spacing.md))

            if (slices.isEmpty()) {
                Box(
                    modifier = Modifier
                        .size(160.dp)
                        .padding(Spacing.sm),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawCircle(
                            color = Color.LightGray.copy(alpha = 0.3f),
                            style = Stroke(width = 24.dp.toPx())
                        )
                    }
                    Text(
                        text = "Chưa có tài sản",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                // Donut Chart Graphic with Center Label
                Box(
                    modifier = Modifier.size(175.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                        val strokeWidth = 26.dp.toPx()
                        val arcSize = Size(size.width, size.height)
                        var startAngle = -90f

                        slices.forEach { slice ->
                            val sweepAngle = (slice.percentage.toFloat() / 100f * 360f) * animationProgress.value
                            drawArc(
                                color = slice.color,
                                startAngle = startAngle,
                                sweepAngle = sweepAngle,
                                useCenter = false,
                                style = Stroke(width = strokeWidth, cap = StrokeCap.Butt),
                                size = arcSize,
                                topLeft = Offset.Zero
                            )
                            startAngle += sweepAngle
                        }
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Tổng danh mục",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (masked) "•••••••• đ" else Money(totalValueMinor).formatVnd(),
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Spacer(modifier = Modifier.height(Spacing.md))

                // Breakdown Legend items
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(Spacing.xs)
                ) {
                    slices.forEach { slice ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = Spacing.xs, vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(CircleShape)
                                        .background(slice.color)
                                )
                                Spacer(modifier = Modifier.width(Spacing.sm))
                                Text(
                                    text = slice.label,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.End
                            ) {
                                Text(
                                    text = if (masked) "••• đ" else Money(slice.valueMinor).formatVnd(),
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.width(Spacing.sm))
                                Surface(
                                    shape = RoundedCornerShape(100.dp),
                                    color = slice.color.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = String.format(Locale.US, "%.1f%%", slice.percentage),
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = slice.color
                                        ),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
