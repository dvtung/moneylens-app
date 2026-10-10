package com.dvt.moneylens.ui.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import com.dvt.moneylens.domain.model.Category
import com.dvt.moneylens.ui.theme.Spacing

@Composable
fun CategoryAvatar(
    category: Category?,
    modifier: Modifier = Modifier,
    size: Dp = Spacing.avatarSize
) {
    val color = runCatching {
        category?.colorHex?.let { Color(android.graphics.Color.parseColor(it)) }
    }.getOrNull() ?: Color(0xFF0A6B5C)

    val iconVector = resolveCategoryIcon(category?.icon)

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(color.copy(alpha = 0.16f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = iconVector,
            contentDescription = category?.name ?: "Danh mục",
            tint = color,
            modifier = Modifier.size(size * 0.55f)
        )
    }
}

fun resolveCategoryIcon(iconName: String?): ImageVector {
    return when (iconName) {
        "restaurant" -> Icons.Rounded.Restaurant
        "local_cafe" -> Icons.Rounded.LocalCafe
        "directions_car" -> Icons.Rounded.DirectionsCar
        "shopping_bag" -> Icons.Rounded.ShoppingBag
        "shopping_cart" -> Icons.Rounded.ShoppingCart
        "receipt_long" -> Icons.Rounded.ReceiptLong
        "medical_services" -> Icons.Rounded.MedicalServices
        "payments" -> Icons.Rounded.Payments
        "redeem" -> Icons.Rounded.Redeem
        "trending_up" -> Icons.Rounded.TrendingUp
        "swap_horiz" -> Icons.Rounded.SwapHoriz
        "credit_card" -> Icons.Rounded.CreditCard
        "account_balance" -> Icons.Rounded.AccountBalance
        else -> Icons.Rounded.AccountBalanceWallet
    }
}
