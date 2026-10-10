package com.dvt.moneylens.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

enum class ThemeMode {
    SYSTEM, LIGHT, DARK
}

enum class ThemeGroup {
    MODERN_M3,
    EXTENDED
}

enum class ThemeAccent(
    val title: String,
    val primaryLight: Color,
    val primaryDark: Color,
    val containerLight: Color,
    val containerDark: Color,
    val previewColor: Color,
    val group: ThemeGroup = ThemeGroup.MODERN_M3,
    val description: String = ""
) {
    // === 10 THEME MỚI (CHUẨN MATERIAL 3 THEO TÀI LIỆU) ===
    MODERN_EMERALD(
        title = "Modern Emerald",
        primaryLight = Color(0xFF059669),
        primaryDark = Color(0xFF34D399),
        containerLight = Color(0xFFD1FAE5),
        containerDark = Color(0xFF064E3B),
        previewColor = Color(0xFF059669),
        group = ThemeGroup.MODERN_M3,
        description = "Xanh ngọc lục bảo hiện đại & thịnh vượng"
    ),
    FINTECH_BLUE(
        title = "Fintech Blue",
        primaryLight = Color(0xFF1D4ED8),
        primaryDark = Color(0xFF60A5FA),
        containerLight = Color(0xFFDBEAFE),
        containerDark = Color(0xFF1E3A8A),
        previewColor = Color(0xFF1D4ED8),
        group = ThemeGroup.MODERN_M3,
        description = "Xanh ngân hàng tin cậy & chuyên nghiệp"
    ),
    MINIMALIST_SLATE(
        title = "Minimalist Slate",
        primaryLight = Color(0xFF18181B),
        primaryDark = Color(0xFFFAFAFA),
        containerLight = Color(0xFFE4E4E7),
        containerDark = Color(0xFF27272A),
        previewColor = Color(0xFF3F3F46),
        group = ThemeGroup.MODERN_M3,
        description = "Đen xám tối giản, tinh tế & sắc nét"
    ),
    SUNSET_WARMTH(
        title = "Sunset Warmth",
        primaryLight = Color(0xFFF97316),
        primaryDark = Color(0xFFFB923C),
        containerLight = Color(0xFFFFEDD5),
        containerDark = Color(0xFF7C2D12),
        previewColor = Color(0xFFF97316),
        group = ThemeGroup.MODERN_M3,
        description = "Cam san hô ấm áp & tràn đầy năng lượng"
    ),
    ROYAL_PURPLE(
        title = "Royal Purple",
        primaryLight = Color(0xFF7C3AED),
        primaryDark = Color(0xFFA78BFA),
        containerLight = Color(0xFFEDE9FE),
        containerDark = Color(0xFF4C1D95),
        previewColor = Color(0xFF7C3AED),
        group = ThemeGroup.MODERN_M3,
        description = "Tím hoàng gia sang trọng & huyền bí"
    ),
    CYBERPUNK_DARK(
        title = "Neon Cyberpunk",
        primaryLight = Color(0xFF06B6D4),
        primaryDark = Color(0xFF22D3EE),
        containerLight = Color(0xFFCFFAFE),
        containerDark = Color(0xFF164E63),
        previewColor = Color(0xFF06B6D4),
        group = ThemeGroup.MODERN_M3,
        description = "Xanh Neon tương lai nổi bật trên nền tối"
    ),
    ELECTRIC_LIME(
        title = "Electric Lime",
        primaryLight = Color(0xFF65A30D),
        primaryDark = Color(0xFFA3E635),
        containerLight = Color(0xFFECFCCB),
        containerDark = Color(0xFF365314),
        previewColor = Color(0xFF84CC16),
        group = ThemeGroup.MODERN_M3,
        description = "Xanh chanh Gen-Z trẻ trung & phá cách"
    ),
    BUBBLEGUM_POP(
        title = "Bubblegum Pop",
        primaryLight = Color(0xFFDB2777),
        primaryDark = Color(0xFFF472B6),
        containerLight = Color(0xFFFCE7F3),
        containerDark = Color(0xFF831843),
        previewColor = Color(0xFFEC4899),
        group = ThemeGroup.MODERN_M3,
        description = "Hồng kẹo ngọt pastel đáng yêu & tươi tắn"
    ),
    SYNTHWAVE_ROSE(
        title = "Synthwave Rose",
        primaryLight = Color(0xFFE11D48),
        primaryDark = Color(0xFFFB7185),
        containerLight = Color(0xFFFFE4E6),
        containerDark = Color(0xFF881337),
        previewColor = Color(0xFFF43F5E),
        group = ThemeGroup.MODERN_M3,
        description = "Cam hồng hoàng hôn mang hơi hướng retro"
    ),
    MINTY_FRESH(
        title = "Minty Fresh",
        primaryLight = Color(0xFF0D9488),
        primaryDark = Color(0xFF2DD4BF),
        containerLight = Color(0xFFCCFBF1),
        containerDark = Color(0xFF134E4A),
        previewColor = Color(0xFF14B8A6),
        group = ThemeGroup.MODERN_M3,
        description = "Xanh bạc hà thanh mát & dịu êm thị giác"
    ),

    // === CÁC THEME CŨ LƯU THÀNH TÙY CHỌN MỞ RỘNG (EXTENDED) ===
    EMERALD(
        title = "Xanh Ngọc Cổ Điển",
        primaryLight = Color(0xFF0A6B5C),
        primaryDark = Color(0xFF4DD0E1),
        containerLight = Color(0xFFC7F0EA),
        containerDark = Color(0xFF004D40),
        previewColor = Color(0xFF0A6B5C),
        group = ThemeGroup.EXTENDED,
        description = "Màu xanh ngọc MoneyLens nguyên bản"
    ),
    OCEAN_BLUE(
        title = "Xanh Biển Sâu",
        primaryLight = Color(0xFF1565C0),
        primaryDark = Color(0xFF64B5F6),
        containerLight = Color(0xFFD0E4FF),
        containerDark = Color(0xFF0D47A1),
        previewColor = Color(0xFF1565C0),
        group = ThemeGroup.EXTENDED,
        description = "Màu xanh nước biển đậm truyền thống"
    ),
    MODERN_PURPLE(
        title = "Tím Thạch Anh",
        primaryLight = Color(0xFF7B1FA2),
        primaryDark = Color(0xFFCE93D8),
        containerLight = Color(0xFFF3E5F5),
        containerDark = Color(0xFF4A148C),
        previewColor = Color(0xFF7B1FA2),
        group = ThemeGroup.EXTENDED,
        description = "Sắc tím thạch anh cổ điển"
    ),
    SUNSET_ORANGE(
        title = "Cam Đất Nung",
        primaryLight = Color(0xFFE65100),
        primaryDark = Color(0xFFFFB74D),
        containerLight = Color(0xFFFFE0B2),
        containerDark = Color(0xFFBF360C),
        previewColor = Color(0xFFE65100),
        group = ThemeGroup.EXTENDED,
        description = "Sắc cam đậm rực rỡ"
    ),
    RUBY_RED(
        title = "Đỏ Ruby Quý Phái",
        primaryLight = Color(0xFFC2185B),
        primaryDark = Color(0xFFF48FB1),
        containerLight = Color(0xFFFCE4EC),
        containerDark = Color(0xFF880E4F),
        previewColor = Color(0xFFC2185B),
        group = ThemeGroup.EXTENDED,
        description = "Đỏ hồng ngọc ấn tượng"
    ),
    FOREST_GREEN(
        title = "Xanh Rừng Nhiệt Đới",
        primaryLight = Color(0xFF2E7D32),
        primaryDark = Color(0xFF81C784),
        containerLight = Color(0xFFE8F5E9),
        containerDark = Color(0xFF1B5E20),
        previewColor = Color(0xFF2E7D32),
        group = ThemeGroup.EXTENDED,
        description = "Màu lá xanh thẫm tự nhiên"
    ),
    CYBERPUNK_NEON(
        title = "Cyberpunk Classic",
        primaryLight = Color(0xFF0097A7),
        primaryDark = Color(0xFF00E5FF),
        containerLight = Color(0xFFE0F7FA),
        containerDark = Color(0xFF10002B),
        previewColor = Color(0xFF00E5FF),
        group = ThemeGroup.EXTENDED,
        description = "Phong cách Neon nguyên bản"
    ),
    PASTEL_CUTE(
        title = "Pastel Dâu Tây",
        primaryLight = Color(0xFFD81B60),
        primaryDark = Color(0xFFFF6584),
        containerLight = Color(0xFFFCE4EC),
        containerDark = Color(0xFF4A0E17),
        previewColor = Color(0xFFFF6584),
        group = ThemeGroup.EXTENDED,
        description = "Hồng phấn ngọt ngào"
    ),
    SUNSET_GLOW(
        title = "Hoàng Hôn Ấm",
        primaryLight = Color(0xFFE64A19),
        primaryDark = Color(0xFFFF7043),
        containerLight = Color(0xFFFFECB3),
        containerDark = Color(0xFFBF360C),
        previewColor = Color(0xFFFF5722),
        group = ThemeGroup.EXTENDED,
        description = "Ánh sáng hoàng hôn ấm cúng"
    ),
    ANIME_MINT(
        title = "Anime Mint Pastel",
        primaryLight = Color(0xFF00897B),
        primaryDark = Color(0xFF00E676),
        containerLight = Color(0xFFE0F2F1),
        containerDark = Color(0xFF00332C),
        previewColor = Color(0xFF00E676),
        group = ThemeGroup.EXTENDED,
        description = "Sắc xanh phong cách Anime"
    )
}

val LocalCardTranslucency = staticCompositionLocalOf { 1.0f }

private fun createLightColorScheme(accent: ThemeAccent) = lightColorScheme(
    primary = accent.primaryLight,
    onPrimary = Color.White,
    primaryContainer = accent.containerLight,
    onPrimaryContainer = accent.primaryLight,
    secondary = TransferLight,
    onSecondary = Color.White,
    tertiary = FocusAmberLight,
    onTertiary = OnFocusContainerLight,
    tertiaryContainer = FocusContainerLight,
    onTertiaryContainer = OnFocusContainerLight,
    background = BackgroundLight,
    onBackground = OnBackgroundLight,
    surface = SurfaceLight,
    onSurface = OnSurfaceLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = OnSurfaceVariantLight,
    outline = OutlineLight,
    error = ErrorLight
)

private fun createDarkColorScheme(accent: ThemeAccent) = darkColorScheme(
    primary = accent.primaryDark,
    onPrimary = Color(0xFF003731),
    primaryContainer = accent.containerDark,
    onPrimaryContainer = accent.primaryDark,
    secondary = TransferDark,
    onSecondary = Color.White,
    tertiary = FocusAmberDark,
    onTertiary = OnFocusContainerDark,
    tertiaryContainer = FocusContainerDark,
    onTertiaryContainer = OnFocusContainerDark,
    background = BackgroundDark,
    onBackground = OnBackgroundDark,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceVariantDark,
    outline = OutlineDark,
    error = ErrorDark
)

private val LightExtraColors = MoneyLensColors(
    income = IncomeLight,
    expense = ExpenseLight,
    transfer = TransferLight,
    focus = FocusAmberLight,
    focusContainer = FocusContainerLight,
    onFocusContainer = OnFocusContainerLight
)

private val DarkExtraColors = MoneyLensColors(
    income = IncomeDark,
    expense = ExpenseDark,
    transfer = TransferDark,
    focus = FocusAmberDark,
    focusContainer = FocusContainerDark,
    onFocusContainer = OnFocusContainerDark
)

@Composable
fun MoneyLensTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    themeAccent: ThemeAccent = ThemeAccent.MODERN_EMERALD,
    darkTheme: Boolean? = null,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val isDark = darkTheme ?: when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        isDark -> createDarkColorScheme(themeAccent)
        else -> createLightColorScheme(themeAccent)
    }

    val extraColors = if (isDark) DarkExtraColors else LightExtraColors

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = !isDark
            insetsController.isAppearanceLightNavigationBars = !isDark
        }
    }

    CompositionLocalProvider(
        LocalMoneyLensColors provides extraColors,
        LocalCardTranslucency provides 1.0f
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AppTypography,
            content = content
        )
    }
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    MoneyLensTheme(darkTheme = darkTheme, dynamicColor = dynamicColor, content = content)
}
