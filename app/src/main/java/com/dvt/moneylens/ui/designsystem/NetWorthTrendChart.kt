package com.dvt.moneylens.ui.designsystem

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dvt.moneylens.domain.model.Money
import com.dvt.moneylens.ui.theme.LocalMoneyLensColors
import com.dvt.moneylens.ui.theme.Spacing
import java.util.Locale

data class NetWorthPoint(
    val timestamp: Long,
    val label: String,
    val netWorthMinor: Long,
    val totalAssetsMinor: Long = 0L,
    val totalDebtsMinor: Long = 0L
)

enum class TrendPeriod(val label: String, val pointCount: Int) {
    MONTH_3("3 Tháng", 3),
    MONTH_6("6 Tháng", 6),
    YEAR_1("1 Năm", 12),
    ALL("Tất cả", Int.MAX_VALUE)
}

@Composable
fun NetWorthTrendChart(
    allPoints: List<NetWorthPoint>,
    currentNetWorthMinor: Long,
    modifier: Modifier = Modifier,
    masked: Boolean = false
) {
    if (allPoints.isEmpty()) return

    val lensColors = LocalMoneyLensColors.current
    var selectedPeriod by remember { mutableStateOf(TrendPeriod.MONTH_6) }

    val filteredPoints = remember(allPoints, selectedPeriod) {
        if (selectedPeriod == TrendPeriod.ALL || allPoints.size <= selectedPeriod.pointCount) {
            allPoints
        } else {
            allPoints.takeLast(selectedPeriod.pointCount)
        }
    }

    var activeTouchIndex by remember(filteredPoints) { mutableStateOf<Int?>(null) }
    val displayPoint = remember(activeTouchIndex, filteredPoints) {
        val idx = activeTouchIndex
        if (idx != null && idx in filteredPoints.indices) {
            filteredPoints[idx]
        } else {
            filteredPoints.lastOrNull()
        }
    }

    // Calculate growth over period
    val firstPoint = filteredPoints.firstOrNull()?.netWorthMinor ?: 1L
    val activeVal = displayPoint?.netWorthMinor ?: currentNetWorthMinor
    val diffMinor = activeVal - firstPoint
    val growthPercent = if (firstPoint != 0L) (diffMinor.toDouble() / Math.abs(firstPoint)) * 100.0 else 0.0
    val isPositive = diffMinor >= 0

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(Spacing.lg)) {
            // Header: Title & Time Period Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Xu hướng Tài sản ròng",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Chạm/kéo đường cong để soi mốc thời gian",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Growth badge
                Surface(
                    shape = RoundedCornerShape(100.dp),
                    color = (if (isPositive) lensColors.income else lensColors.expense).copy(alpha = 0.15f)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = Spacing.sm, vertical = 3.dp)
                    ) {
                        Text(
                            text = if (isPositive) "▲ +${String.format(Locale.US, "%.1f", growthPercent)}%" else "▼ ${String.format(Locale.US, "%.1f", growthPercent)}%",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (isPositive) lensColors.income else lensColors.expense
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(Spacing.md))

            // Period Filter Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
            ) {
                TrendPeriod.entries.forEach { period ->
                    FilterChip(
                        selected = selectedPeriod == period,
                        onClick = {
                            selectedPeriod = period
                            activeTouchIndex = null
                        },
                        label = { Text(period.label, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(Spacing.md))

            // Active Point Value Display
            displayPoint?.let { pt ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column {
                        Text(
                            text = "Tài sản ròng (${pt.label})",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (masked) "•••••••• đ" else Money(pt.netWorthMinor).formatVnd(),
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    if (pt.totalAssetsMinor > 0L) {
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "Tổng tài sản: ${if (masked) "•••" else Money(pt.totalAssetsMinor).formatVnd()}",
                                style = MaterialTheme.typography.labelSmall,
                                color = lensColors.income
                            )
                            if (pt.totalDebtsMinor > 0L) {
                                Text(
                                    text = "Công nợ: ${if (masked) "•••" else Money(pt.totalDebtsMinor).formatVnd()}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = lensColors.expense
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(Spacing.lg))

            // Canvas Chart Area
            val chartPrimaryColor = MaterialTheme.colorScheme.primary
            val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
            val pointCount = filteredPoints.size

            val minVal = (filteredPoints.minOfOrNull { it.netWorthMinor } ?: 0L).toDouble()
            val maxVal = (filteredPoints.maxOfOrNull { it.netWorthMinor } ?: 100_000_000L).toDouble()
            val valRange = (maxVal - minVal).coerceAtLeast(1.0)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .pointerInput(filteredPoints) {
                        detectTapGestures { offset ->
                            val width = size.width
                            val step = if (pointCount > 1) width / (pointCount - 1) else width
                            val tappedIdx = (offset.x / step).toInt().coerceIn(0, pointCount - 1)
                            activeTouchIndex = tappedIdx
                        }
                    }
                    .pointerInput(filteredPoints) {
                        detectDragGestures(
                            onDrag = { change, _ ->
                                val width = size.width
                                val step = if (pointCount > 1) width / (pointCount - 1) else width
                                val dragIdx = (change.position.x / step).toInt().coerceIn(0, pointCount - 1)
                                activeTouchIndex = dragIdx
                            },
                            onDragEnd = { /* keep last touched point */ }
                        )
                    }
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    val bottomPadding = 24.dp.toPx()
                    val topPadding = 16.dp.toPx()
                    val effectiveHeight = h - topPadding - bottomPadding

                    // 1. Draw horizontal gridlines (3 lines: Top, Mid, Bottom)
                    for (i in 0..2) {
                        val y = topPadding + (effectiveHeight / 2) * i
                        drawLine(
                            color = gridColor,
                            start = Offset(0f, y),
                            end = Offset(w, y),
                            strokeWidth = 1.dp.toPx()
                        )
                    }

                    if (pointCount < 2) return@Canvas

                    val stepX = w / (pointCount - 1)
                    val pointsOffsets = filteredPoints.mapIndexed { i, pt ->
                        val norm = ((pt.netWorthMinor - minVal) / valRange).toFloat().coerceIn(0f, 1f)
                        val x = i * stepX
                        val y = topPadding + effectiveHeight * (1f - norm)
                        Offset(x, y)
                    }

                    // 2. Build Smooth Cubic Bezier Path
                    val curvePath = Path().apply {
                        moveTo(pointsOffsets.first().x, pointsOffsets.first().y)
                        for (i in 0 until pointsOffsets.size - 1) {
                            val p0 = pointsOffsets[i]
                            val p1 = pointsOffsets[i + 1]
                            val controlX1 = p0.x + (p1.x - p0.x) / 2f
                            val controlY1 = p0.y
                            val controlX2 = p0.x + (p1.x - p0.x) / 2f
                            val controlY2 = p1.y
                            cubicTo(controlX1, controlY1, controlX2, controlY2, p1.x, p1.y)
                        }
                    }

                    // 3. Fill Gradient Area Under Curve
                    val fillPath = Path().apply {
                        addPath(curvePath)
                        lineTo(w, h - bottomPadding)
                        lineTo(0f, h - bottomPadding)
                        close()
                    }

                    drawPath(
                        path = fillPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                chartPrimaryColor.copy(alpha = 0.35f),
                                chartPrimaryColor.copy(alpha = 0.08f),
                                Color.Transparent
                            ),
                            startY = topPadding,
                            endY = h - bottomPadding
                        )
                    )

                    // 4. Draw Smooth Stroke Curve
                    drawPath(
                        path = curvePath,
                        color = chartPrimaryColor,
                        style = Stroke(
                            width = 3.dp.toPx(),
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )

                    // 5. Draw Active / Touched Point Indicator
                    val targetIdx = activeTouchIndex ?: (pointCount - 1)
                    if (targetIdx in pointsOffsets.indices) {
                        val activeOffset = pointsOffsets[targetIdx]

                        // Vertical guide line
                        drawLine(
                            color = chartPrimaryColor.copy(alpha = 0.5f),
                            start = Offset(activeOffset.x, topPadding),
                            end = Offset(activeOffset.x, h - bottomPadding),
                            strokeWidth = 1.5.dp.toPx()
                        )

                        // Glowing Outer Circle
                        drawCircle(
                            color = chartPrimaryColor.copy(alpha = 0.25f),
                            radius = 10.dp.toPx(),
                            center = activeOffset
                        )

                        // Solid Inner Circle
                        drawCircle(
                            color = Color.White,
                            radius = 5.dp.toPx(),
                            center = activeOffset
                        )
                        drawCircle(
                            color = chartPrimaryColor,
                            radius = 3.5.dp.toPx(),
                            center = activeOffset
                        )
                    }
                }

                // X-Axis Labels Row below Canvas
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    filteredPoints.forEachIndexed { idx, pt ->
                        val isHighlighted = idx == (activeTouchIndex ?: (pointCount - 1))
                        Text(
                            text = pt.label,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                fontWeight = if (isHighlighted) FontWeight.Bold else FontWeight.Normal
                            ),
                            color = if (isHighlighted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
