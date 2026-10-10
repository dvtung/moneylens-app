package com.dvt.moneylens.ui.designsystem

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dvt.moneylens.domain.model.Money
import com.dvt.moneylens.ui.theme.LocalMoneyLensColors
import com.dvt.moneylens.ui.theme.Spacing

@Composable
fun BudgetBar(
    spentMinor: Long,
    limitMinor: Long,
    modifier: Modifier = Modifier,
    masked: Boolean = false
) {
    val lensColors = LocalMoneyLensColors.current
    val progress = if (limitMinor > 0) (spentMinor.toFloat() / limitMinor).coerceIn(0f, 1.5f) else 0f
    val percentInt = if (limitMinor > 0) ((spentMinor.toDouble() / limitMinor) * 100).toInt() else 0

    val barColor = when {
        percentInt > 100 -> lensColors.expense
        percentInt >= 80 -> lensColors.focus
        else -> MaterialTheme.colorScheme.primary
    }

    val remainingMinor = limitMinor - spentMinor

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val statusLabel = if (remainingMinor >= 0) {
                "Còn ${if (masked) "••••••" else Money(remainingMinor).formatVnd()}"
            } else {
                "Vượt ${if (masked) "••••••" else Money(-remainingMinor).formatVnd()}"
            }

            Text(
                text = "$percentInt% đã dùng",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = statusLabel,
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                color = if (percentInt > 100) lensColors.expense else MaterialTheme.colorScheme.onSurface
            )
        }

        Spacer(modifier = Modifier.height(Spacing.xs))

        LinearProgressIndicator(
            progress = { progress.coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = barColor,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}
