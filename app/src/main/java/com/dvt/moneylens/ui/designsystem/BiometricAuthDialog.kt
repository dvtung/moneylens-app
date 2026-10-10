package com.dvt.moneylens.ui.designsystem

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.dvt.moneylens.ui.theme.LocalMoneyLensColors
import com.dvt.moneylens.ui.theme.Spacing
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class BiometricScanState {
    IDLE, SCANNING, SUCCESS, ERROR
}

/**
 * Hộp thoại và cơ chế xác thực sinh trắc học chuẩn Android BiometricPrompt.
 * Kiểm tra phần cứng và vân tay thực tế của thiết bị, chỉ mở khóa khi người dùng xác thực đúng.
 */
@Composable
fun FintechBiometricPromptDialog(
    onSuccess: () -> Unit,
    onDismiss: () -> Unit,
    userName: String = "MoneyLens",
    modifier: Modifier = Modifier
) {
    val lensColors = LocalMoneyLensColors.current
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var scanState by remember { mutableStateOf(BiometricScanState.IDLE) }
    var statusText by remember { mutableStateOf("Chạm vào biểu tượng vân tay để mở xác thực") }

    // Pulsing animation for idle state
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    fun startScanning() {
        val fragmentActivity = context as? FragmentActivity
        if (fragmentActivity == null) {
            statusText = "Không thể khởi tạo xác thực sinh trắc học"
            scanState = BiometricScanState.ERROR
            return
        }

        val biometricManager = BiometricManager.from(context)
        val authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK
        val canAuth = biometricManager.canAuthenticate(authenticators)

        if (canAuth != BiometricManager.BIOMETRIC_SUCCESS) {
            scanState = BiometricScanState.ERROR
            statusText = when (canAuth) {
                BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> "Chưa đăng ký vân tay trên máy. Vui lòng dùng mã PIN."
                BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> "Thiết bị không hỗ trợ phần cứng sinh trắc học."
                BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> "Cảm biến sinh trắc học hiện không khả dụng."
                else -> "Không thể xác thực sinh trắc học trên thiết bị."
            }
            return
        }

        scanState = BiometricScanState.SCANNING
        statusText = "Vui lòng đặt ngón tay vào cảm biến..."

        val executor = ContextCompat.getMainExecutor(context)
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Xác thực sinh trắc học")
            .setSubtitle("Xác nhận vân tay đã đăng ký để mở khóa MoneyLens")
            .setNegativeButtonText("Sử dụng mã PIN")
            .setAllowedAuthenticators(authenticators)
            .build()

        val biometricPrompt = BiometricPrompt(
            fragmentActivity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    scanState = BiometricScanState.SUCCESS
                    statusText = "Xác thực vân tay thành công!"
                    coroutineScope.launch {
                        delay(350)
                        onSuccess()
                    }
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    if (errorCode == BiometricPrompt.ERROR_USER_CANCELED || errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                        onDismiss()
                    } else {
                        scanState = BiometricScanState.ERROR
                        statusText = errString.toString()
                    }
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    scanState = BiometricScanState.ERROR
                    statusText = "Vân tay không khớp! Vui lòng chạm lại đúng vân tay."
                }
            }
        )

        biometricPrompt.authenticate(promptInfo)
    }

    LaunchedEffect(Unit) {
        startScanning()
    }

    Dialog(
        onDismissRequest = {
            if (scanState != BiometricScanState.SUCCESS) onDismiss()
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight()
                .padding(Spacing.md)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Spacing.xl),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Security Header Badge
                Surface(
                    shape = RoundedCornerShape(100.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                    )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Shield,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Xác thực bảo mật MoneyLens",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 11.sp
                            ),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(Spacing.lg))

                // Title & Description
                Text(
                    text = "Xác thực sinh trắc học",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(Spacing.xs))

                Text(
                    text = "Xin chào $userName, vui lòng xác nhận danh tính để truy cập dữ liệu tài chính",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = Spacing.sm)
                )

                Spacer(modifier = Modifier.height(Spacing.xl))

                // Interactive Biometric Sensor Target with Glow & Pulse Effect
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(130.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { startScanning() }
                        )
                ) {
                    // Pulsing Outer Ring (When Idle)
                    if (scanState == BiometricScanState.IDLE) {
                        Box(
                            modifier = Modifier
                                .size(110.dp * pulseScale)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = pulseAlpha))
                        )
                    }

                    // Rotating / Glow Circle
                    Box(
                        modifier = Modifier
                            .size(96.dp)
                            .clip(CircleShape)
                            .background(
                                when (scanState) {
                                    BiometricScanState.SUCCESS -> Brush.radialGradient(
                                        listOf(lensColors.income.copy(alpha = 0.35f), Color.Transparent)
                                    )
                                    BiometricScanState.SCANNING -> Brush.radialGradient(
                                        listOf(MaterialTheme.colorScheme.primary.copy(alpha = 0.35f), Color.Transparent)
                                    )
                                    BiometricScanState.ERROR -> Brush.radialGradient(
                                        listOf(MaterialTheme.colorScheme.error.copy(alpha = 0.35f), Color.Transparent)
                                    )
                                    else -> Brush.radialGradient(
                                        listOf(MaterialTheme.colorScheme.primaryContainer, Color.Transparent)
                                    )
                                }
                            )
                            .border(
                                width = 2.dp,
                                color = when (scanState) {
                                    BiometricScanState.SUCCESS -> lensColors.income
                                    BiometricScanState.SCANNING -> MaterialTheme.colorScheme.primary
                                    BiometricScanState.ERROR -> MaterialTheme.colorScheme.error
                                    else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                                },
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        when (scanState) {
                            BiometricScanState.SUCCESS -> {
                                Icon(
                                    imageVector = Icons.Rounded.CheckCircle,
                                    contentDescription = "Thành công",
                                    tint = lensColors.income,
                                    modifier = Modifier.size(52.dp)
                                )
                            }
                            BiometricScanState.SCANNING -> {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(76.dp),
                                    strokeWidth = 3.dp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Icon(
                                    imageVector = Icons.Rounded.Fingerprint,
                                    contentDescription = "Đang quét",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(46.dp)
                                )
                            }
                            BiometricScanState.ERROR -> {
                                Icon(
                                    imageVector = Icons.Rounded.ErrorOutline,
                                    contentDescription = "Lỗi xác thực",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(48.dp)
                                )
                            }
                            else -> {
                                Icon(
                                    imageVector = Icons.Rounded.Fingerprint,
                                    contentDescription = "Chạm để quét vân tay",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(48.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(Spacing.md))

                // Dynamic Status Helper Text
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = if (scanState == BiometricScanState.SUCCESS) FontWeight.Bold else FontWeight.Medium
                    ),
                    color = when (scanState) {
                        BiometricScanState.SUCCESS -> lensColors.income
                        BiometricScanState.SCANNING -> MaterialTheme.colorScheme.primary
                        BiometricScanState.ERROR -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(Spacing.xl))

                // Bottom Action Options: Switch to PIN or Cancel
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                    ) {
                        Text("Dùng mã PIN", style = MaterialTheme.typography.labelLarge)
                    }

                    Button(
                        onClick = { startScanning() },
                        enabled = scanState != BiometricScanState.SCANNING,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                    ) {
                        Text("Quét lại", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    }
}
