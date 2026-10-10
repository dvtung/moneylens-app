package com.dvt.moneylens.ui.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dvt.moneylens.domain.model.TransactionStatus
import com.dvt.moneylens.domain.model.TransactionType
import com.dvt.moneylens.ui.theme.LocalMoneyLensColors
import com.dvt.moneylens.ui.theme.Spacing

@Composable
fun StatusPill(
    status: TransactionStatus,
    type: TransactionType = TransactionType.EXPENSE,
    modifier: Modifier = Modifier
) {
    val lensColors = LocalMoneyLensColors.current

    if (type == TransactionType.TRANSFER) {
        Row(
            modifier = modifier
                .clip(RoundedCornerShape(100.dp))
                .background(lensColors.transfer.copy(alpha = 0.15f))
                .padding(horizontal = Spacing.sm, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Rounded.SwapHoriz,
                contentDescription = null,
                tint = lensColors.transfer,
                modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(Spacing.xs))
            Text(
                text = "Chuyển nội bộ",
                style = MaterialTheme.typography.labelSmall,
                color = lensColors.transfer
            )
        }
        return
    }

    when (status) {
        TransactionStatus.PENDING -> {
            Row(
                modifier = modifier
                    .clip(RoundedCornerShape(100.dp))
                    .background(lensColors.focusContainer)
                    .padding(horizontal = Spacing.sm, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.Info,
                    contentDescription = null,
                    tint = lensColors.onFocusContainer,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(Spacing.xs))
                Text(
                    text = "Chờ xác nhận",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                    color = lensColors.onFocusContainer
                )
            }
        }
        TransactionStatus.CONFIRMED -> {
            Row(
                modifier = modifier
                    .clip(RoundedCornerShape(100.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(horizontal = Spacing.sm, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(Spacing.xs))
                Text(
                    text = "Đã xác nhận",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
