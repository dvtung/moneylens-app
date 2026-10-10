package com.dvt.moneylens.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Light Scheme Colors (Tối ưu tương phản cao, nền xám bạc dịu mắt chuẩn Fintech)
val LensLight = Color(0xFF0A6B5C)
val OnLensLight = Color(0xFFFFFFFF)
val LensContainerLight = Color(0xFFD8F3EC)
val OnLensContainerLight = Color(0xFF00382E)

val FocusAmberLight = Color(0xFFD97706)
val FocusContainerLight = Color(0xFFFEF3C7)
val OnFocusContainerLight = Color(0xFF78350F)

val BackgroundLight = Color(0xFFF0F3F6) // Nền xám bạc dịu mắt, làm nổi bật tuyệt đối thẻ Surface #FFFFFF
val OnBackgroundLight = Color(0xFF1A202C) // Màu chữ và icon đậm nét, chống lóa 100%
val SurfaceLight = Color(0xFFFFFFFF) // Màu thẻ trắng tinh khôi, tạo khối nổi 3D
val OnSurfaceLight = Color(0xFF1A202C)
val SurfaceVariantLight = Color(0xFFE2E8F0) // Viền và khay nút tương phản rõ
val OnSurfaceVariantLight = Color(0xFF475569)
val OutlineLight = Color(0xFFCBD5E1)

val IncomeLight = Color(0xFF0D8253)
val ExpenseLight = Color(0xFFD32F2F)
val TransferLight = Color(0xFF2563EB)
val ErrorLight = Color(0xFFDC2626)

// Dark Scheme Colors
val LensDark = Color(0xFF5FD3BD)
val OnLensDark = Color(0xFF00382F)
val LensContainerDark = Color(0xFF005144)
val OnLensContainerDark = Color(0xFFCFEDE5)

val FocusAmberDark = Color(0xFFFFC857)
val FocusContainerDark = Color(0xFF543E00)
val OnFocusContainerDark = Color(0xFFFFE9B8)

val BackgroundDark = Color(0xFF0E1514)
val OnBackgroundDark = Color(0xFFE1EAE7)
val SurfaceDark = Color(0xFF151E1C)
val OnSurfaceDark = Color(0xFFE1EAE7)
val SurfaceVariantDark = Color(0xFF1F2B28)
val OnSurfaceVariantDark = Color(0xFFA3B3AE)
val OutlineDark = Color(0xFF33433F)

val IncomeDark = Color(0xFF5BD39A)
val ExpenseDark = Color(0xFFFF8A80)
val TransferDark = Color(0xFF9DAACB)
val ErrorDark = Color(0xFFF2B8B5)

@Immutable
data class MoneyLensColors(
    val income: Color,
    val expense: Color,
    val transfer: Color,
    val focus: Color,
    val focusContainer: Color,
    val onFocusContainer: Color,
)

val LocalMoneyLensColors = staticCompositionLocalOf<MoneyLensColors> {
    error("MoneyLensColors not provided")
}
