package com.dvt.moneylens.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.dvt.moneylens.data.backup.BackupSummary
import com.dvt.moneylens.data.backup.DataBackupDto
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.dvt.moneylens.domain.calc.BankPackageRegistry
import com.dvt.moneylens.domain.calc.BankParserEngine
import com.dvt.moneylens.domain.calc.SupportedBank
import com.dvt.moneylens.ui.AutoLockTimeout
import com.dvt.moneylens.ui.MainUiState
import com.dvt.moneylens.ui.designsystem.FintechScreenTopAppBar
import com.dvt.moneylens.ui.screens.settings.BankCaptureSettingsSection
import com.dvt.moneylens.ui.screens.settings.BackupSettingsSection
import com.dvt.moneylens.ui.screens.settings.SecuritySettingsSection
import com.dvt.moneylens.ui.screens.settings.ThemeSettingsSection
import com.dvt.moneylens.ui.theme.LocalMoneyLensColors
import com.dvt.moneylens.ui.theme.Spacing
import com.dvt.moneylens.ui.theme.ThemeAccent
import com.dvt.moneylens.ui.theme.ThemeGroup
import com.dvt.moneylens.ui.theme.ThemeMode

enum class BankFilterStatus {
    ALL, ENABLED, DISABLED
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    uiState: MainUiState,
    onResetDemoData: () -> Unit,
    onClearAllData: () -> Unit,
    onToggleBank: (String) -> Unit,
    onEnableAllBanks: ((List<String>) -> Unit)? = null,
    onDisableAllBanks: (() -> Unit)? = null,
    onSelectThemeMode: (ThemeMode) -> Unit = {},
    onSelectThemeAccent: (ThemeAccent) -> Unit = {},
    onTogglePinSecurity: (Boolean, String) -> Unit = { _, _ -> },
    onToggleBiometric: (Boolean) -> Unit = {},
    onVerifyPin: (String) -> Boolean = { true },
    onSelectAutoLockTimeout: (AutoLockTimeout) -> Unit = {},
    onUpdateUserInfo: (String, String) -> Unit = { _, _ -> },
    onLockApp: () -> Unit = {},
    onOpenInvestmentGuide: () -> Unit = {},
    onToggleNotificationCapture: (Boolean) -> Unit = {},
    onCleanSpuriousTransactions: () -> Unit = {},
    lastBackupTimestamp: Long? = null,
    isBackupProcessing: Boolean = false,
    onExportBackup: (Uri, onResult: (Boolean, String?) -> Unit) -> Unit = { _, _ -> },
    onReadBackupFile: (Uri, onSuccess: (DataBackupDto, BackupSummary) -> Unit, onError: (String) -> Unit) -> Unit = { _, _, _ -> },
    onRestoreBackup: (DataBackupDto, onResult: (Boolean, String?) -> Unit) -> Unit = { _, _ -> },
    onShareBackup: () -> Unit = {},
    onNavigateBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lensColors = LocalMoneyLensColors.current
    var isOfflineModeOnly by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }
    var showClearDialog by remember { mutableStateOf(false) }
    var bankFilter by remember { mutableStateOf(BankFilterStatus.ALL) }
    var isBankListExpanded by remember { mutableStateOf(false) }

    var showRestoreConfirmDialog by remember { mutableStateOf(false) }
    var pendingRestoreDto by remember { mutableStateOf<DataBackupDto?>(null) }
    var pendingRestoreSummary by remember { mutableStateOf<BackupSummary?>(null) }
    var backupFeedbackMessage by remember { mutableStateOf<String?>(null) }

    val exportDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri != null) {
            onExportBackup(uri) { success, errorMsg ->
                if (success) {
                    backupFeedbackMessage = "Đã xuất file sao lưu thành công!"
                } else {
                    backupFeedbackMessage = errorMsg ?: "Xuất file thất bại"
                }
            }
        }
    }

    val importDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            onReadBackupFile(
                uri,
                { dto, summary ->
                    pendingRestoreDto = dto
                    pendingRestoreSummary = summary
                    showRestoreConfirmDialog = true
                },
                { errorMsg ->
                    backupFeedbackMessage = errorMsg
                }
            )
        }
    }

    val biometricManager = remember { androidx.biometric.BiometricManager.from(context) }
    val canAuthenticateBiometric = remember {
        biometricManager.canAuthenticate(
            androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG or androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
        ) == androidx.biometric.BiometricManager.BIOMETRIC_SUCCESS
    }
    var showSecurityRequirementWarningDialog by remember { mutableStateOf(false) }
    var showBiometricPromptForDelete by remember { mutableStateOf(false) }
    var showDeletePinConfirmDialog by remember { mutableStateOf(false) }
    var deletePinInput by remember { mutableStateOf("") }
    var deletePinError by remember { mutableStateOf("") }
    var isDeletePinVisible by remember { mutableStateOf(false) }

    var showPinDialog by remember { mutableStateOf(false) }
    var newPinInput by remember { mutableStateOf("") }
    var confirmPinInput by remember { mutableStateOf("") }
    var pinDialogError by remember { mutableStateOf("") }
    var isNewPinVisible by remember { mutableStateOf(false) }
    var isConfirmPinVisible by remember { mutableStateOf(false) }

    var showEditProfileDialog by remember { mutableStateOf(false) }
    var editNameInput by remember(uiState.userName) { mutableStateOf(uiState.userName) }
    var editAvatarInput by remember(uiState.userAvatar) { mutableStateOf(uiState.userAvatar) }

    var showSmsGuideDialog by remember { mutableStateOf(false) }
    var showPrivacyPolicyDialog by remember { mutableStateOf(false) }
    var pendingDisclosureType by remember { mutableStateOf<com.dvt.moneylens.ui.designsystem.DisclosureType?>(null) }

    // Kiểm tra & xin quyền thông báo hệ thống (POST_NOTIFICATIONS)
    var hasNotificationPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            } else {
                NotificationManagerCompat.from(context).areNotificationsEnabled()
            }
        )
    }

    // Kiểm tra & xin quyền đọc/nhận SMS ngân hàng
    var hasSmsPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED
        )
    }

    // Kiểm tra quyền Notification Listener Service
    var isNotificationListenerEnabled by remember {
        mutableStateOf(
            NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)
        )
    }

    val requestNotificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasNotificationPermission = isGranted
    }

    val requestSmsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.RECEIVE_SMS] == true ||
                permissions[Manifest.permission.READ_SMS] == true
        hasSmsPermission = granted
        if (!granted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            showSmsGuideDialog = true
        }
    }

    // Tự động làm mới trạng thái quyền ngay khi người dùng quay lại ứng dụng (ON_RESUME)
    val lifecycleOwner = LocalLifecycleOwner.current
    fun refreshPermissionStates() {
        hasNotificationPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else {
            NotificationManagerCompat.from(context).areNotificationsEnabled()
        }
        hasSmsPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED
        isNotificationListenerEnabled = NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refreshPermissionStates()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Scaffold(
        topBar = {
            FintechScreenTopAppBar(
                title = "Cài đặt & Bảo mật",
                subtitle = "• MoneyLens Pro v2.0 · Lưu trữ cục bộ an toàn",
                subtitleColor = MaterialTheme.colorScheme.onSurfaceVariant,
                icon = Icons.Rounded.Settings,
                onBackClick = onNavigateBack
            )
        },
        modifier = modifier
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(bottom = 96.dp)
        ) {
            // 1. Chế độ Giao diện (Dark Mode)
            item {
                SettingsSectionTitle("Giao diện & Trải nghiệm")
            }

            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.lg, vertical = Spacing.xs)
                ) {
                    Column(modifier = Modifier.padding(Spacing.md)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.DarkMode,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(Spacing.sm))
                            Column {
                                Text(
                                    text = "Chế độ nền tối (Dark Mode)",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                                )
                                Text(
                                    text = "Giảm mỏi mắt và tiết kiệm pin khi kiểm tra tài chính ban đêm",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(Spacing.md))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
                        ) {
                            FilterChip(
                                selected = uiState.themeMode == ThemeMode.SYSTEM,
                                onClick = { onSelectThemeMode(ThemeMode.SYSTEM) },
                                label = { Text("Theo hệ thống") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Rounded.BrightnessAuto,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                modifier = Modifier.weight(1f)
                            )

                            FilterChip(
                                selected = uiState.themeMode == ThemeMode.LIGHT,
                                onClick = { onSelectThemeMode(ThemeMode.LIGHT) },
                                label = { Text("Luôn sáng") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Rounded.LightMode,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                modifier = Modifier.weight(1f)
                            )

                            FilterChip(
                                selected = uiState.themeMode == ThemeMode.DARK,
                                onClick = { onSelectThemeMode(ThemeMode.DARK) },
                                label = { Text("Luôn tối") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Rounded.DarkMode,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // Tùy biến Màu chủ đạo (Theme Color Accent - 6 màu)
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.lg, vertical = Spacing.xs)
                ) {
                    Column(modifier = Modifier.padding(Spacing.md)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.Palette,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(Spacing.sm))
                            Column {
                                Text(
                                    text = "Màu chủ đạo ứng dụng (Theme Color)",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                                )
                                Text(
                                    text = "Đang chọn: ${uiState.themeAccent.title}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(Spacing.sm))

                        val modernThemes = remember {
                            ThemeAccent.values().filter { it.group == ThemeGroup.MODERN_M3 }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                        ) {
                            modernThemes.forEach { accent ->
                                val isSelected = uiState.themeAccent == accent
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isSelected) accent.containerLight.copy(alpha = 0.6f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                    border = if (isSelected) {
                                        androidx.compose.foundation.BorderStroke(2.dp, accent.primaryLight)
                                    } else {
                                        androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                                    },
                                    modifier = Modifier
                                        .clickable { onSelectThemeAccent(accent) }
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = Spacing.md, vertical = 10.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(24.dp)
                                                .clip(CircleShape)
                                                .background(accent.previewColor),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (isSelected) {
                                                Icon(
                                                    imageVector = Icons.Rounded.Check,
                                                    contentDescription = "Đã chọn",
                                                    tint = Color.White,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(Spacing.sm))
                                        Column {
                                            Text(
                                                text = accent.title,
                                                style = MaterialTheme.typography.labelMedium.copy(
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold
                                                ),
                                                color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            if (accent.description.isNotBlank()) {
                                                Text(
                                                    text = accent.description,
                                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
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



            // 2. Tự động hóa & Quyền hệ thống
            item {
                SettingsSectionTitle("Tự động hóa & Quyền hệ thống")
            }

            // A. Quyền thông báo ứng dụng (Push Notifications / POST_NOTIFICATIONS)
            item {
                SettingsRowItem(
                    icon = Icons.Rounded.Notifications,
                    title = "Quyền gửi thông báo ứng dụng",
                    subtitle = "Nhận thông báo nhắc nhở chi tiêu, cảnh báo hạn mức ngân sách",
                    statusText = if (hasNotificationPermission) "Đã cho phép" else "Chưa cấp quyền",
                    statusColor = if (hasNotificationPermission) lensColors.income else lensColors.expense,
                    statusContainerColor = if (hasNotificationPermission) lensColors.income.copy(alpha = 0.12f) else lensColors.expense.copy(alpha = 0.12f),
                    onClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            requestNotificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                            }
                            runCatching { context.startActivity(intent) }
                        }
                    }
                )
            }

            // B. Quyền truy cập thông báo ngân hàng (Notification Listener Service)
            item {
                SettingsRowItem(
                    icon = Icons.Rounded.NotificationsActive,
                    title = "Quyền đọc thông báo ngân hàng",
                    subtitle = "Tự động phân tích biến động số dư từ app ngân hàng & ví",
                    statusText = if (isNotificationListenerEnabled) "Đang hoạt động" else "Chưa bật",
                    statusColor = if (isNotificationListenerEnabled) lensColors.income else Color(0xFFE65100),
                    statusContainerColor = if (isNotificationListenerEnabled) lensColors.income.copy(alpha = 0.12f) else Color(0xFFFFF3E0),
                    onClick = {
                        if (!isNotificationListenerEnabled) {
                            pendingDisclosureType = com.dvt.moneylens.ui.designsystem.DisclosureType.NOTIFICATION_LISTENER
                        } else {
                            val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                            context.startActivity(intent)
                        }
                    }
                )
            }

            // Công tắc Bật/Tắt quét thông báo & SMS nội bộ trong app
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.lg, vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(Spacing.md),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (uiState.isNotificationCaptureEnabled) MaterialTheme.colorScheme.primaryContainer
                                        else MaterialTheme.colorScheme.surfaceVariant
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (uiState.isNotificationCaptureEnabled) Icons.Rounded.Sync else Icons.Rounded.SyncDisabled,
                                    contentDescription = null,
                                    tint = if (uiState.isNotificationCaptureEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(Spacing.md))
                            Column {
                                Text(
                                    text = "Tự động ghi nhận thông báo & SMS",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                                )
                                Text(
                                    text = if (uiState.isNotificationCaptureEnabled) "Đang bật quét biến động số dư" else "Đang tạm tắt quét biến động",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (uiState.isNotificationCaptureEnabled) lensColors.income else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Switch(
                            checked = uiState.isNotificationCaptureEnabled,
                            onCheckedChange = { onToggleNotificationCapture(it) }
                        )
                    }
                }
            }

            // Nút Dọn dẹp giao dịch rác / lặp lại
            item {
                SettingsRowItem(
                    icon = Icons.Rounded.CleaningServices,
                    title = "Dọn dẹp giao dịch rác / lặp lại",
                    subtitle = "Xóa nhanh các giao dịch không hợp lệ sinh ra do lỗi vòng lặp (-54 đ)",
                    statusText = "Dọn dẹp ngay",
                    statusColor = lensColors.expense,
                    statusContainerColor = lensColors.expense.copy(alpha = 0.12f),
                    onClick = onCleanSpuriousTransactions
                )
            }

            // C. Quyền nhận & đọc SMS ngân hàng (SMS Permissions)
            item {
                SettingsRowItem(
                    icon = Icons.Rounded.Sms,
                    title = "Nhận & đọc SMS ngân hàng (Full Flavor)",
                    subtitle = "Tự động ghi nhận giao dịch từ tin nhắn SMS biến động",
                    statusText = if (hasSmsPermission) "Đã cho phép" else "Chưa cấp quyền",
                    statusColor = if (hasSmsPermission) lensColors.income else lensColors.expense,
                    statusContainerColor = if (hasSmsPermission) lensColors.income.copy(alpha = 0.12f) else lensColors.expense.copy(alpha = 0.12f),
                    onClick = {
                        if (!hasSmsPermission) {
                            pendingDisclosureType = com.dvt.moneylens.ui.designsystem.DisclosureType.SMS_PERMISSION
                        }
                    }
                )
            }

            // D. Thẻ ghi chú hướng dẫn quyền SMS trên Android 13+ (Chỉ hiển thị khi CHƯA được cấp quyền)
            if (!hasSmsPermission) {
                item {
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Spacing.lg, vertical = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(Spacing.md)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Rounded.Info,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(Spacing.sm))
                                Text(
                                    text = "Lưu ý bật quyền SMS trên Android 13+",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Đối với file APK cài đặt ngoài Google Play, Android sẽ chặn quyền SMS dưới dạng 'Cài đặt bị hạn chế'. Bạn cần vào cài đặt ứng dụng để mở khóa.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(Spacing.sm))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                FilledTonalButton(
                                    onClick = { showSmsGuideDialog = true },
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text("Xem hướng dẫn bật quyền", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                                }
                            }
                        }
                    }
                }
            }

            // E. Chạy nền không bị giới hạn (Battery Optimization)
            item {
                SettingsRowItem(
                    icon = Icons.Rounded.BatteryChargingFull,
                    title = "Chạy nền không bị giới hạn",
                    subtitle = "Tắt tối ưu pin để không bị Android ngắt nhận thông báo",
                    statusText = "Mở cài đặt pin",
                    onClick = {
                        val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                        runCatching { context.startActivity(intent) }
                    }
                )
            }

            // 3. Danh mục các ngân hàng hỗ trợ parse thông báo (Expose & Collapse)
            item {
                SettingsSectionTitle("Ngân hàng & Ví hỗ trợ parse tự động")
            }

            val allBanks = BankParserEngine.allSupportedBanks
            val enabledCount = allBanks.count { uiState.enabledBankCodes.contains(it.code) }
            val disabledCount = allBanks.size - enabledCount

            // Thẻ tóm tắt ngân hàng (Hiển thị thông tin cơ bản)
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.lg, vertical = 4.dp)
                        .clickable { isBankListExpanded = !isBankListExpanded }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(Spacing.md),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.AccountBalance,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(Spacing.md))
                            Column {
                                Text(
                                    text = "Danh sách ngân hàng hỗ trợ",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                                )
                                Text(
                                    text = "Đang theo dõi: $enabledCount / ${allBanks.size} ngân hàng & ví",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (enabledCount > 0) lensColors.income else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        IconButton(onClick = { isBankListExpanded = !isBankListExpanded }) {
                            Icon(
                                imageVector = if (isBankListExpanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                                contentDescription = if (isBankListExpanded) "Thu gọn" else "Mở rộng",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Phần mở rộng chi tiết (Chỉ hiển thị khi người dùng bấm mở rộng)
            if (isBankListExpanded) {
                val filteredBanks = when (bankFilter) {
                    BankFilterStatus.ALL -> allBanks
                    BankFilterStatus.ENABLED -> allBanks.filter { uiState.enabledBankCodes.contains(it.code) }
                    BankFilterStatus.DISABLED -> allBanks.filter { !uiState.enabledBankCodes.contains(it.code) }
                }

                // Thanh Chip lọc trạng thái
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Spacing.lg, vertical = Spacing.xs),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
                    ) {
                        FilterChip(
                            selected = bankFilter == BankFilterStatus.ALL,
                            onClick = { bankFilter = BankFilterStatus.ALL },
                            label = { Text("Tất cả (${allBanks.size})") }
                        )
                        FilterChip(
                            selected = bankFilter == BankFilterStatus.ENABLED,
                            onClick = { bankFilter = BankFilterStatus.ENABLED },
                            label = { Text("Đang bật ($enabledCount)") }
                        )
                        FilterChip(
                            selected = bankFilter == BankFilterStatus.DISABLED,
                            onClick = { bankFilter = BankFilterStatus.DISABLED },
                            label = { Text("Đang tắt ($disabledCount)") }
                        )
                    }
                }

                // Thao tác hàng loạt: Bật tất cả / Tắt tất cả
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Spacing.lg, vertical = Spacing.xs),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Hiển thị ${filteredBanks.size} ngân hàng",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                            TextButton(
                                onClick = { onEnableAllBanks?.invoke(allBanks.map { it.code }) },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text("Bật tất cả", style = MaterialTheme.typography.labelMedium)
                            }
                            TextButton(
                                onClick = { onDisableAllBanks?.invoke() },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text("Tắt tất cả", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }

                if (filteredBanks.isEmpty()) {
                    item {
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = Spacing.lg, vertical = Spacing.md)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(Spacing.lg),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (bankFilter == BankFilterStatus.ENABLED)
                                        "Không có ngân hàng nào đang được bật."
                                    else
                                        "Tất cả ngân hàng đều đang được kích hoạt nhận diện.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    items(filteredBanks, key = { it.code }) { bank ->
                        val isEnabled = uiState.enabledBankCodes.contains(bank.code)
                        val bankColor = runCatching { Color(android.graphics.Color.parseColor(bank.colorHex)) }
                            .getOrDefault(MaterialTheme.colorScheme.primary)

                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = Spacing.lg, vertical = 3.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = Spacing.md, vertical = Spacing.sm),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(bankColor.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = bank.shortName.take(3),
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = bankColor
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(Spacing.md))

                                    val primaryPkg = remember(bank.code) { BankPackageRegistry.getPrimaryPackage(bank.code) }
                                    val installedPkg = remember(bank.code) { BankPackageRegistry.getInstalledPackage(context, bank.code) }
                                    val isInstalled = installedPkg != null
                                    val displayPkg = installedPkg ?: primaryPkg

                                    Column(modifier = Modifier.padding(end = Spacing.xs)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = bank.name,
                                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                                color = MaterialTheme.colorScheme.onSurface,
                                                modifier = Modifier.weight(1f, fill = false)
                                            )
                                            if (isInstalled) {
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = lensColors.income.copy(alpha = 0.15f)
                                                ) {
                                                    Text(
                                                        text = "Đã cài trên máy",
                                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                                                        color = lensColors.income,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                        }
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(top = 2.dp)
                                        ) {
                                            Text(
                                                text = if (isEnabled) "● Đang theo dõi" else "○ Đã tắt",
                                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                                color = if (isEnabled) lensColors.income else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Spacer(modifier = Modifier.width(Spacing.xs))
                                            Text(
                                                text = "· $displayPkg",
                                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace),
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }

                                Switch(
                                    checked = isEnabled,
                                    onCheckedChange = { onToggleBank(bank.code) }
                                )
                            }
                        }
                    }
                }

                // Nút thu gọn ở cuối danh sách
                item {
                    TextButton(
                        onClick = { isBankListExpanded = false },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = Spacing.xs)
                    ) {
                        Icon(imageVector = Icons.Rounded.KeyboardArrowUp, contentDescription = null)
                        Spacer(modifier = Modifier.width(Spacing.xs))
                        Text("Thu gọn danh sách ngân hàng")
                    }
                }
            }

            // 4. Bảo mật & Dữ liệu
            item {
                SettingsSectionTitle("Bảo mật & Dữ liệu")
            }

            // Khóa PIN 6 số
            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.lg, vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(Spacing.md),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    if (uiState.isPinSecurityEnabled) {
                                        newPinInput = ""
                                        confirmPinInput = ""
                                        pinDialogError = ""
                                        isNewPinVisible = false
                                        isConfirmPinVisible = false
                                        showPinDialog = true
                                    }
                                }
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Lock,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(Spacing.md))
                            Column {
                                Text(
                                    text = "Bảo vệ bằng mã PIN 6 số",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                                )
                                Text(
                                    text = if (uiState.isPinSecurityEnabled) "Đã bật · Chạm để đổi mã PIN" else "Khóa ứng dụng tự động khi rời màn hình",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (uiState.isPinSecurityEnabled) lensColors.income else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Switch(
                            checked = uiState.isPinSecurityEnabled,
                            onCheckedChange = { checked ->
                                if (checked) {
                                    newPinInput = ""
                                    confirmPinInput = ""
                                    pinDialogError = ""
                                    isNewPinVisible = false
                                    isConfirmPinVisible = false
                                    showPinDialog = true
                                } else {
                                    onTogglePinSecurity(false, "")
                                }
                            }
                        )
                    }
                }
            }

            // Sinh trắc học (Vân tay / Khuôn mặt)
            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.lg, vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(Spacing.md),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(
                                imageVector = Icons.Rounded.Fingerprint,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(Spacing.md))
                            Column {
                                Text(
                                    text = "Mở khóa bằng Sinh trắc học",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                                )
                                Text(
                                    text = if (!uiState.isPinSecurityEnabled) "Vui lòng bật mã PIN 6 số trước khi kích hoạt vân tay"
                                           else "Dùng cảm biến vân tay hoặc khuôn mặt để mở khóa nhanh",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (!uiState.isPinSecurityEnabled) MaterialTheme.colorScheme.error.copy(alpha = 0.85f) else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Switch(
                            checked = uiState.isBiometricEnabled && uiState.isPinSecurityEnabled,
                            enabled = uiState.isPinSecurityEnabled,
                            onCheckedChange = { onToggleBiometric(it) }
                        )
                    }
                }
            }

            // Hồ sơ người dùng & Lời chào cá nhân hóa
            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.lg, vertical = 4.dp)
                        .clickable { showEditProfileDialog = true }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(Spacing.md),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(text = uiState.userAvatar, fontSize = 22.sp)
                                }
                            }
                            Spacer(modifier = Modifier.width(Spacing.md))
                            Column {
                                Text(
                                    text = "Xin chào, ${uiState.userName}",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                                )
                                Text(
                                    text = "Chạm để đổi tên hiển thị và avatar màn hình khóa",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Icon(
                            imageVector = Icons.Rounded.Edit,
                            contentDescription = "Chỉnh sửa",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Thời gian tự động khóa an toàn
            if (uiState.isPinSecurityEnabled) {
                item {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Spacing.lg, vertical = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(Spacing.md)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Rounded.Timer,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(Spacing.sm))
                                Column {
                                    Text(
                                        text = "Tự động khóa sau khi rời ứng dụng",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                                    )
                                    Text(
                                        text = "Yêu cầu mã PIN hoặc vân tay khi quay lại app",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(Spacing.sm))

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                            ) {
                                AutoLockTimeout.values().forEach { timeout ->
                                    FilterChip(
                                        selected = uiState.autoLockTimeout == timeout,
                                        onClick = { onSelectAutoLockTimeout(timeout) },
                                        label = {
                                            Text(
                                                text = timeout.label,
                                                style = MaterialTheme.typography.labelMedium,
                                                maxLines = 1,
                                                softWrap = false
                                            )
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Nút Khóa ngay để thử nghiệm
            if (uiState.isPinSecurityEnabled) {
                item {
                    SettingsRowItem(
                        icon = Icons.Rounded.LockClock,
                        title = "Khóa ứng dụng ngay",
                        subtitle = "Yêu cầu nhập mã PIN hoặc vân tay ngay lập tức",
                        statusText = "Khóa ngay",
                        onClick = onLockApp
                    )
                }
            }

            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.lg, vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(Spacing.md),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(
                                imageVector = Icons.Rounded.CloudOff,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(Spacing.md))
                            Column {
                                Text(
                                    text = "Chế độ ngoại tuyến hoàn toàn",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                                )
                                Text(
                                    text = "Không gọi mạng tải giá thị trường, an toàn tuyệt đối",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Switch(
                            checked = isOfflineModeOnly,
                            onCheckedChange = { isOfflineModeOnly = it }
                        )
                    }
                }
            }

            // Chuyên mục Thông tin & Pháp lý
            item {
                SettingsSectionTitle("Thông tin & Pháp lý")
            }

            item {
                SettingsRowItem(
                    icon = Icons.Rounded.Security,
                    title = "Chính sách quyền riêng tư (Privacy Policy)",
                    subtitle = "Cam kết bảo mật Local-First, minh bạch dữ liệu & quyền hạn",
                    statusText = "Xem chính sách",
                    statusColor = MaterialTheme.colorScheme.primary,
                    statusContainerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    onClick = { showPrivacyPolicyDialog = true }
                )
            }

            item {
                SettingsRowItem(
                    icon = Icons.Rounded.MenuBook,
                    title = "Phương pháp tính đầu tư",
                    subtitle = "Tiêu chuẩn tính lãi/lỗ và giá trị đầu tư dài hạn (Cổ phiếu, Quỹ, Vàng, BĐS)",
                    statusText = "Xem tài liệu",
                    statusColor = MaterialTheme.colorScheme.primary,
                    statusContainerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    onClick = onOpenInvestmentGuide
                )
            }

            // Quản lý dữ liệu
            item {
                SettingsSectionTitle("Quản lý dữ liệu")
            }

            // Sao lưu & Khôi phục dữ liệu (JSON)
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.lg, vertical = 6.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(Spacing.md)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Rounded.Storage,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(Spacing.md))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Sao lưu & Khôi phục (JSON)",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = "Bảo vệ toàn vẹn dữ liệu cá nhân khi nâng cấp ứng dụng",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(Spacing.sm))

                        // Trạng thái lần sao lưu gần nhất
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = Spacing.sm, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Rounded.Schedule,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Lần sao lưu gần nhất:",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    text = if (lastBackupTimestamp != null && lastBackupTimestamp > 0L) {
                                        SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(lastBackupTimestamp))
                                    } else {
                                        "Chưa thực hiện"
                                    },
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = if (lastBackupTimestamp != null && lastBackupTimestamp > 0L) lensColors.income else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        if (isBackupProcessing) {
                            Spacer(modifier = Modifier.height(Spacing.sm))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(Spacing.sm))
                                Text(
                                    text = "Đang xử lý dữ liệu...",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(Spacing.md))

                        // Nút Xuất và Khôi phục
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                        ) {
                            Button(
                                onClick = {
                                    val now = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())
                                    exportDocumentLauncher.launch("moneylens_backup_$now.json")
                                },
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.UploadFile,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Xuất JSON",
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                    maxLines = 1
                                )
                            }

                            FilledTonalButton(
                                onClick = {
                                    importDocumentLauncher.launch(arrayOf("application/json", "text/*", "*/*"))
                                },
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.FileDownload,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Khôi phục",
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                    maxLines = 1
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Nút chia sẻ nhanh
                        OutlinedButton(
                            onClick = onShareBackup,
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Share,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Chia sẻ file sao lưu (Drive, Zalo, Email...)",
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                }
            }

            item {
                SettingsRowItem(
                    icon = Icons.Rounded.Restore,
                    title = "Nạp lại dữ liệu mẫu (Demo Data)",
                    subtitle = "Đặt lại 10 giao dịch mẫu, 2 thẻ và 4 tài sản",
                    onClick = { showResetDialog = true }
                )
            }

            item {
                SettingsRowItem(
                    icon = Icons.Rounded.DeleteForever,
                    title = "Xóa sạch toàn bộ dữ liệu (Data Deletion)",
                    subtitle = "Yêu cầu xác thực Vân tay & Mã PIN để xóa vĩnh viễn dữ liệu",
                    titleColor = MaterialTheme.colorScheme.error,
                    onClick = {
                        val meetsRequirement = if (canAuthenticateBiometric) {
                            uiState.isPinSecurityEnabled && uiState.isBiometricEnabled
                        } else {
                            uiState.isPinSecurityEnabled
                        }
                        if (!meetsRequirement) {
                            showSecurityRequirementWarningDialog = true
                        } else {
                            deletePinInput = ""
                            deletePinError = ""
                            if (canAuthenticateBiometric) {
                                showBiometricPromptForDelete = true
                            } else {
                                showDeletePinConfirmDialog = true
                            }
                        }
                    }
                )
            }

            item {
                Spacer(modifier = Modifier.height(Spacing.lg))
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "MoneyLens v1.0.0 · Local-First Personal Finance",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }

    // Dialog đặt / đổi mã PIN 6 số
    if (showPinDialog) {
        AlertDialog(
            onDismissRequest = { showPinDialog = false },
            title = { Text("Thiết lập mã PIN 6 số") },
            text = {
                Column {
                    Text(
                        text = "Nhập mã PIN gồm đúng 6 chữ số và xác nhận lại để bảo vệ ứng dụng:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(Spacing.md))
                    OutlinedTextField(
                        value = newPinInput,
                        onValueChange = {
                            if (it.length <= 6 && it.all { char -> char.isDigit() }) {
                                newPinInput = it
                                pinDialogError = ""
                            }
                        },
                        label = { Text("Mã PIN mới (6 chữ số)") },
                        placeholder = { Text("••••••") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        visualTransformation = if (isNewPinVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { isNewPinVisible = !isNewPinVisible }) {
                                Icon(
                                    imageVector = if (isNewPinVisible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                    contentDescription = if (isNewPinVisible) "Ẩn mã PIN" else "Hiện mã PIN",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        isError = pinDialogError.isNotBlank(),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(Spacing.sm))

                    OutlinedTextField(
                        value = confirmPinInput,
                        onValueChange = {
                            if (it.length <= 6 && it.all { char -> char.isDigit() }) {
                                confirmPinInput = it
                                pinDialogError = ""
                            }
                        },
                        label = { Text("Nhập lại mã PIN để xác nhận") },
                        placeholder = { Text("••••••") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        visualTransformation = if (isConfirmPinVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { isConfirmPinVisible = !isConfirmPinVisible }) {
                                Icon(
                                    imageVector = if (isConfirmPinVisible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                    contentDescription = if (isConfirmPinVisible) "Ẩn mã PIN" else "Hiện mã PIN",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        isError = pinDialogError.isNotBlank(),
                        supportingText = if (pinDialogError.isNotBlank()) {
                            { Text(pinDialogError, color = MaterialTheme.colorScheme.error) }
                        } else null,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newPinInput.length != 6) {
                            pinDialogError = "Vui lòng nhập đủ 6 chữ số!"
                        } else if (confirmPinInput != newPinInput) {
                            pinDialogError = "Mã PIN xác nhận không khớp! Vui lòng nhập lại."
                        } else {
                            onTogglePinSecurity(true, newPinInput)
                            showPinDialog = false
                        }
                    }
                ) {
                    Text("Lưu mã PIN")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPinDialog = false }) {
                    Text("Hủy")
                }
            }
        )
    }

    // Dialog xác nhận nạp lại demo data
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Nạp lại dữ liệu mẫu?") },
            text = { Text("Thao tác này sẽ ghi đè các giao dịch hiện tại bằng bộ dữ liệu tài chính mẫu để bạn trải nghiệm tính năng.") },
            confirmButton = {
                Button(
                    onClick = {
                        showResetDialog = false
                        onResetDemoData()
                    }
                ) {
                    Text("Xác nhận nạp")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Hủy")
                }
            }
        )
    }

    // 1. Hộp thoại Cảnh báo nếu chưa bật đủ điều kiện bảo mật (Mã PIN & Vân tay)
    if (showSecurityRequirementWarningDialog) {
        AlertDialog(
            onDismissRequest = { showSecurityRequirementWarningDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Rounded.Security,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = "Yêu cầu bảo mật cấp cao",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Text(
                    text = if (canAuthenticateBiometric) {
                        "Để bảo vệ dữ liệu tài chính tối đa và chống thao tác xóa nhầm, chức năng 'Xóa sạch toàn bộ dữ liệu' CHỈ được phép thực hiện khi bạn đã cài đặt và BẬT cả Mã PIN 6 số VÀ Mở khóa bằng Sinh trắc học (Vân tay).\n\nVui lòng thiết lập và bật đầy đủ cả hai trong phần 'Bảo mật & Dữ liệu' trước khi thực hiện thao tác này."
                    } else {
                        "Để bảo vệ dữ liệu tài chính tối đa và chống thao tác xóa nhầm, chức năng 'Xóa sạch toàn bộ dữ liệu' CHỈ được phép thực hiện khi bạn đã cài đặt và BẬT Mã PIN 6 số.\n\nVui lòng thiết lập mã PIN trong phần 'Bảo mật & Dữ liệu' trước khi thực hiện thao tác này."
                    },
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(onClick = { showSecurityRequirementWarningDialog = false }) {
                    Text("Đã hiểu")
                }
            }
        )
    }

    // 2. Bước 1: Quét vân tay (BiometricPrompt)
    if (showBiometricPromptForDelete) {
        com.dvt.moneylens.ui.designsystem.FintechBiometricPromptDialog(
            userName = uiState.userName.ifBlank { "MoneyLens" },
            onSuccess = {
                showBiometricPromptForDelete = false
                showDeletePinConfirmDialog = true
            },
            onDismiss = {
                showBiometricPromptForDelete = false
            }
        )
    }

    // 3. Bước 2: Nhập mã PIN 6 số để chốt lệnh xóa vĩnh viễn
    if (showDeletePinConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeletePinConfirmDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Rounded.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = "Xác nhận mã PIN để xóa dữ liệu",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Column {
                    Text(
                        text = if (canAuthenticateBiometric) "Vân tay đã xác thực thành công. Vui lòng nhập đúng mã PIN 6 số của bạn để hoàn tất lệnh xóa toàn bộ dữ liệu:"
                               else "Vui lòng nhập đúng mã PIN 6 số của bạn để xác nhận xóa sạch toàn bộ dữ liệu:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(Spacing.md))
                    OutlinedTextField(
                        value = deletePinInput,
                        onValueChange = {
                            if (it.length <= 6 && it.all { char -> char.isDigit() }) {
                                deletePinInput = it
                                deletePinError = ""
                            }
                        },
                        label = { Text("Mã PIN (6 chữ số)") },
                        placeholder = { Text("••••••") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        visualTransformation = if (isDeletePinVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { isDeletePinVisible = !isDeletePinVisible }) {
                                Icon(
                                    imageVector = if (isDeletePinVisible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                    contentDescription = if (isDeletePinVisible) "Ẩn mã PIN" else "Hiện mã PIN",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        isError = deletePinError.isNotBlank(),
                        supportingText = if (deletePinError.isNotBlank()) {
                            { Text(deletePinError, color = MaterialTheme.colorScheme.error) }
                        } else null,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (deletePinInput.length != 6) {
                            deletePinError = "Vui lòng nhập đủ 6 chữ số!"
                        } else if (!onVerifyPin(deletePinInput)) {
                            deletePinError = "Mã PIN không chính xác! Vui lòng thử lại."
                        } else {
                            showDeletePinConfirmDialog = false
                            onClearAllData()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Xác nhận xóa vĩnh viễn")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeletePinConfirmDialog = false }) {
                    Text("Hủy")
                }
            }
        )
    }

    // Dialog chỉnh sửa hồ sơ cá nhân
    if (showEditProfileDialog) {
        val avatarChoices = listOf("💼", "🪙", "📊", "🌟", "🚀")
        AlertDialog(
            onDismissRequest = { showEditProfileDialog = false },
            title = { Text("Chỉnh sửa thông tin cá nhân") },
            text = {
                Column {
                    OutlinedTextField(
                        value = editNameInput,
                        onValueChange = { editNameInput = it },
                        label = { Text("Tên người dùng") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(Spacing.md))
                    Text("Biểu tượng đại diện:", style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(Spacing.xs))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        avatarChoices.forEach { av ->
                            val isSel = editAvatarInput == av
                            Surface(
                                shape = CircleShape,
                                color = if (isSel) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .size(44.dp)
                                    .clickable { editAvatarInput = av }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(text = av, fontSize = 22.sp)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onUpdateUserInfo(editNameInput.ifBlank { "MoneyLens" }, editAvatarInput)
                        showEditProfileDialog = false
                    }
                ) {
                    Text("Lưu thay đổi")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditProfileDialog = false }) {
                    Text("Hủy")
                }
            }
        )
    }

    // Dialog hướng dẫn mở khóa quyền SMS trong Cài đặt bị hạn chế (Android 13+)
    if (showSmsGuideDialog) {
        AlertDialog(
            onDismissRequest = { showSmsGuideDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Rounded.Sms,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = "Hướng dẫn bật quyền SMS trên Android 13+",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Do chính sách bảo mật của Android đối với file APK cài đặt ngoài Google Play, quyền đọc SMS bị khóa trong mục 'Cài đặt bị hạn chế'.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(Spacing.md))
                    Text(
                        text = "CÁC BƯỚC MỞ KHÓA:",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    )
                    Spacer(modifier = Modifier.height(Spacing.xs))
                    Text(
                        text = "1️⃣ Nhấn 'Mở Cài đặt ứng dụng' bên dưới.\n2️⃣ Nhấn biểu tượng dấu 3 chấm (⋮) ở góc trên bên phải.\n3️⃣ Chọn 'Cho phép cài đặt bị hạn chế' (Allow restricted settings).\n4️⃣ Quay lại mục Quyền (Permissions) > Bật quyền Tin nhắn SMS.",
                        style = MaterialTheme.typography.bodySmall.copy(lineHeight = 20.sp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", context.packageName, null)
                        }
                        runCatching { context.startActivity(intent) }
                        showSmsGuideDialog = false
                    }
                ) {
                    Text("Mở Cài đặt ứng dụng")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSmsGuideDialog = false }) {
                    Text("Đã hiểu")
                }
            }
        )
    }

    // Hộp thoại Prominent In-App Disclosure (Tuân thủ chính sách Google Play)
    pendingDisclosureType?.let { type ->
        com.dvt.moneylens.ui.designsystem.ProminentDisclosureDialog(
            type = type,
            onConfirm = {
                val currentType = type
                pendingDisclosureType = null
                if (currentType == com.dvt.moneylens.ui.designsystem.DisclosureType.NOTIFICATION_LISTENER) {
                    val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                    runCatching { context.startActivity(intent) }
                } else {
                    requestSmsLauncher.launch(arrayOf(Manifest.permission.RECEIVE_SMS, Manifest.permission.READ_SMS))
                }
            },
            onDismiss = { pendingDisclosureType = null },
            onOpenPrivacyPolicy = {
                pendingDisclosureType = null
                showPrivacyPolicyDialog = true
            }
        )
    }

    // Hộp thoại Chính sách quyền riêng tư (Privacy Policy)
    if (showPrivacyPolicyDialog) {
        com.dvt.moneylens.ui.designsystem.PrivacyPolicyDialog(
            onDismiss = { showPrivacyPolicyDialog = false },
            onOpenWebUrl = {
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://ais-dev-25yzxzuyffwbhxnr5v7bjm-289708794324.asia-east1.run.app/privacy_policy.html"))
                runCatching { context.startActivity(browserIntent) }
            }
        )
    }

    // Hộp thoại xem trước & xác nhận khôi phục từ tệp sao lưu JSON
    if (showRestoreConfirmDialog && pendingRestoreSummary != null && pendingRestoreDto != null) {
        val summary = pendingRestoreSummary!!
        val dto = pendingRestoreDto!!
        AlertDialog(
            onDismissRequest = {
                showRestoreConfirmDialog = false
                pendingRestoreDto = null
                pendingRestoreSummary = null
            },
            icon = {
                Icon(
                    imageVector = Icons.Rounded.SettingsBackupRestore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = "Xác nhận khôi phục dữ liệu",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = "Ứng dụng đã đọc thành công tệp sao lưu JSON:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(Spacing.sm))

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(Spacing.sm)) {
                            Text(
                                text = "📅 Xuất lúc: ${SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(summary.exportedAt))}",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium)
                            )
                            Text(
                                text = "📱 Thiết bị: ${summary.deviceModel}",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(Spacing.sm))
                    Text(
                        text = "Dữ liệu sẽ được khôi phục:",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "• ${summary.accountCount} Tài khoản ngân hàng / ví", style = MaterialTheme.typography.bodySmall)
                    Text(text = "• ${summary.creditCardCount} Thẻ tín dụng & thẻ ghi nợ", style = MaterialTheme.typography.bodySmall)
                    Text(text = "• ${summary.transactionCount} Giao dịch thu chi", style = MaterialTheme.typography.bodySmall)
                    Text(text = "• ${summary.categoryCount} Danh mục chi tiêu", style = MaterialTheme.typography.bodySmall)
                    Text(text = "• ${summary.budgetCount} Hạn mức ngân sách", style = MaterialTheme.typography.bodySmall)
                    Text(text = "• ${summary.assetCount} Tài sản đầu tư & tích sản", style = MaterialTheme.typography.bodySmall)
                    Text(text = "• ${summary.loanCount} Khoản vay & nợ", style = MaterialTheme.typography.bodySmall)
                    Text(text = "• ${summary.memberCount} Thành viên", style = MaterialTheme.typography.bodySmall)

                    Spacer(modifier = Modifier.height(Spacing.md))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(Spacing.sm),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(Spacing.sm))
                            Text(
                                text = "Cảnh báo: Toàn bộ dữ liệu hiện tại trên thiết bị sẽ được thay thế bằng dữ liệu từ tệp sao lưu này.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showRestoreConfirmDialog = false
                        onRestoreBackup(dto) { success, errorMsg ->
                            if (success) {
                                backupFeedbackMessage = "Đã khôi phục thành công toàn bộ dữ liệu!"
                            } else {
                                backupFeedbackMessage = errorMsg ?: "Khôi phục dữ liệu thất bại"
                            }
                        }
                        pendingRestoreDto = null
                        pendingRestoreSummary = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Khôi phục ngay")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showRestoreConfirmDialog = false
                    pendingRestoreDto = null
                    pendingRestoreSummary = null
                }) {
                    Text("Hủy bỏ")
                }
            }
        )
    }

    // Hộp thoại thông báo kết quả sao lưu / khôi phục
    if (backupFeedbackMessage != null) {
        AlertDialog(
            onDismissRequest = { backupFeedbackMessage = null },
            icon = {
                Icon(
                    imageVector = Icons.Rounded.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = { Text("Thông báo") },
            text = { Text(backupFeedbackMessage ?: "") },
            confirmButton = {
                TextButton(onClick = { backupFeedbackMessage = null }) {
                    Text("Đã hiểu")
                }
            }
        )
    }
}

@Composable
fun SettingsSectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge.copy(
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        ),
        modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.sm)
    )
}

@Composable
fun SettingsRowItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    statusText: String? = null,
    statusColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    statusContainerColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    titleColor: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.lg, vertical = 4.dp)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.md),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (titleColor != MaterialTheme.colorScheme.onSurface) titleColor else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(Spacing.md))
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = titleColor
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (statusText != null) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = statusContainerColor
                ) {
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = statusColor,
                        modifier = Modifier.padding(horizontal = Spacing.sm, vertical = 2.dp)
                    )
                }
            }
        }
    }
}
