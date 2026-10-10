package com.dvt.moneylens.ui.designsystem

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PieChart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dvt.moneylens.domain.model.Category
import com.dvt.moneylens.domain.model.Money
import com.dvt.moneylens.domain.model.Transaction
import com.dvt.moneylens.domain.model.TransactionStatus
import com.dvt.moneylens.domain.model.TransactionType
import com.dvt.moneylens.ui.theme.Spacing
import java.util.Locale

data class CategoryExpenseSlice(
    val categoryId: String,
    val categoryName: String,
    val color: Color,
    val amountMinor: Long,
    val percentage: Double
)

@Composable
fun ExpenseAllocationPieChart(
    title: String,
    transactions: List<Transaction>,
    categories: List<Category>,
    masked: Boolean = false,
    modifier: Modifier = Modifier
) {
    // 1. Filter expense transactions
    val slices = remember(transactions, categories) {
        val expenseTx = transactions.filter {
            it.type == TransactionType.EXPENSE && it.status == TransactionStatus.CONFIRMED
        }
        val totalExpense = expenseTx.sumOf { it.amountMinor }

        if (totalExpense <= 0L) {
            emptyList()
        } else {
            val catMap = categories.associateBy { it.id }
            val defaultColorList = listOf(
                Color(0xFFFF5252),
                Color(0xFFFF7A00),
                Color(0xFFFFB300),
                Color(0xFF00C853),
                Color(0xFF00B0FF),
                Color(0xFF7C4DFF),
                Color(0xFFE040FB),
                Color(0xFF00BFA5)
            )

            // Group by categoryId
            val grouped = expenseTx.groupBy { it.categoryId ?: "unknown" }
            var colorIdx = 0

            grouped.map { (catId, txList) ->
                val sum = txList.sumOf { it.amountMinor }
                val cat = catMap[catId]
                val catName = cat?.name ?: if (catId == "unknown") "Khác" else "Chưa phân loại"
                val rawColor = cat?.colorHex?.let { hex ->
                    runCatching { Color(android.graphics.Color.parseColor(hex)) }.getOrNull()
                } ?: defaultColorList[colorIdx % defaultColorList.size].also { colorIdx++ }

                val pct = (sum.toDouble() / totalExpense) * 100.0
                CategoryExpenseSlice(
                    categoryId = catId,
                    categoryName = catName,
                    color = rawColor,
                    amountMinor = sum,
                    percentage = pct
                )
            }.sortedByDescending { it.amountMinor }
        }
    }

    val totalExpenseMinor = remember(slices) {
        slices.sumOf { it.amountMinor }
    }

    val animationProgress = remember { Animatable(0f) }
    LaunchedEffect(slices) {
        animationProgress.snapTo(0f)
        animationProgress.animateTo(1f, animationSpec = tween(durationMillis = 750))
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.md)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (totalExpenseMinor > 0L) {
                    Text(
                        text = if (masked) "••••••" else Money(totalExpenseMinor).formatVnd(),
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFFFF5252)
                    )
                }
            }

            Spacer(modifier = Modifier.height(Spacing.sm))

            if (slices.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Rounded.PieChart,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Chưa có dữ liệu chi tiêu trong kỳ này",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = Spacing.xs),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Donut Chart Graphic
                    Box(
                        modifier = Modifier
                            .size(130.dp)
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val strokeWidth = 22.dp.toPx()
                            val radius = (size.minDimension - strokeWidth) / 2
                            val center = Offset(size.width / 2, size.height / 2)
                            val arcSize = Size(radius * 2, radius * 2)
                            val arcTopLeft = Offset(center.x - radius, center.y - radius)

                            var currentStartAngle = -90f

                            slices.forEach { slice ->
                                val sweepAngle = (slice.percentage.toFloat() / 100f * 360f) * animationProgress.value
                                if (sweepAngle > 0.5f) {
                                    drawArc(
                                        color = slice.color,
                                        startAngle = currentStartAngle,
                                        sweepAngle = sweepAngle - 1.5f, // slight gap
                                        useCenter = false,
                                        topLeft = arcTopLeft,
                                        size = arcSize,
                                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                                    )
                                }
                                currentStartAngle += sweepAngle
                            }
                        }

                        // Center Label
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${slices.size}",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Danh mục",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(Spacing.md))

                    // Top Categories Breakdown List
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = Spacing.xs),
                        verticalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        slices.take(4).forEach { slice ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(slice.color)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = slice.categoryName,
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Text(
                                    text = String.format(Locale.US, "%.1f%%", slice.percentage),
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        if (slices.size > 4) {
                            val remainingPct = slices.drop(4).sumOf { it.percentage }
                            val remainingCount = slices.size - 4
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.outline)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Khác ($remainingCount danh mục)",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1
                                    )
                                }
                                Text(
                                    text = String.format(Locale.US, "%.1f%%", remainingPct),
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
