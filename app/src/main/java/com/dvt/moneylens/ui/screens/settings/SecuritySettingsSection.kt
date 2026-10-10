package com.dvt.moneylens.ui.screens.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dvt.moneylens.ui.AutoLockTimeout
import com.dvt.moneylens.ui.MainUiState
import com.dvt.moneylens.ui.theme.Spacing

@Composable
fun SecuritySettingsSection(
    uiState: MainUiState,
    onTogglePinSecurity: (Boolean, String) -> Unit,
    onToggleBiometric: (Boolean) -> Unit,
    onVerifyPin: (String) -> Boolean,
    onSelectAutoLockTimeout: (AutoLockTimeout) -> Unit,
    onLockApp: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showPinDialog by remember { mutableStateOf(false) }
    var pinDialogMode by remember { mutableStateOf("ENABLE") } // "ENABLE", "DISABLE", "CHANGE"
    var enteredPin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var isConfirming by remember { mutableStateOf(false) }
    var pinError by remember { mutableStateOf("") }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.lg, vertical = Spacing.xs)
    ) {
        Column(modifier = Modifier.padding(Spacing.lg)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Rounded.Security,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(Spacing.md))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Bảo mật ứng dụng",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "Mã PIN 6 số và xác thực vân tay/khuôn mặt",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(Spacing.md))

            // Switch Mã PIN
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Khóa ứng dụng bằng PIN",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                    Text(
                        text = if (uiState.isPinSecurityEnabled) "Đã bật mã PIN 6 số" else "Chưa bật mã PIN",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = uiState.isPinSecurityEnabled,
                    onCheckedChange = { enabled ->
                        if (enabled) {
                            pinDialogMode = "ENABLE"
                            enteredPin = ""
                            confirmPin = ""
                            isConfirming = false
                            pinError = ""
                            showPinDialog = true
                        } else {
                            pinDialogMode = "DISABLE"
                            enteredPin = ""
                            pinError = ""
                            showPinDialog = true
                        }
                    }
                )
            }

            AnimatedVisibility(visible = uiState.isPinSecurityEnabled) {
                Column {
                    Spacer(modifier = Modifier.height(Spacing.sm))
                    Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(Spacing.sm))

                    // Đổi mã PIN
                    OutlinedButton(
                        onClick = {
                            pinDialogMode = "CHANGE"
                            enteredPin = ""
                            confirmPin = ""
                            isConfirming = false
                            pinError = ""
                            showPinDialog = true
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Rounded.Key, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(Spacing.xs))
                        Text("Đổi mã PIN")
                    }

                    Spacer(modifier = Modifier.height(Spacing.sm))

                    // Switch Sinh trắc học
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Mở khóa bằng Vân tay / Khuôn mặt",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                            )
                            Text(
                                text = "Mở khóa nhanh không cần gõ PIN",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = uiState.isBiometricEnabled,
                            onCheckedChange = { onToggleBiometric(it) }
                        )
                    }

                    Spacer(modifier = Modifier.height(Spacing.sm))

                    // Tự động khóa sau thời gian chờ
                    Text(
                        text = "Thời gian tự động khóa khi chạy ngầm",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                    Spacer(modifier = Modifier.height(Spacing.xs))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        AutoLockTimeout.entries.forEach { timeout ->
                            FilterChip(
                                selected = uiState.autoLockTimeout == timeout,
                                onClick = { onSelectAutoLockTimeout(timeout) },
                                label = { Text(timeout.label, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(Spacing.sm))

                    // Nút khóa app ngay
                    Button(
                        onClick = onLockApp,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Rounded.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(Spacing.xs))
                        Text("Khóa ứng dụng ngay")
                    }
                }
            }
        }
    }

    // Modal nhập PIN
    if (showPinDialog) {
        AlertDialog(
            onDismissRequest = { showPinDialog = false },
            title = {
                Text(
                    when (pinDialogMode) {
                        "ENABLE" -> if (!isConfirming) "Tạo mã PIN mới (6 số)" else "Xác nhận lại mã PIN"
                        "DISABLE" -> "Nhập mã PIN hiện tại để tắt"
                        else -> if (!isConfirming) "Nhập mã PIN mới (6 số)" else "Xác nhận mã PIN mới"
                    }
                )
            },
            text = {
                Column {
                    OutlinedTextField(
                        value = if (!isConfirming) enteredPin else confirmPin,
                        onValueChange = { valDigits ->
                            if (valDigits.length <= 6 && valDigits.all { it.isDigit() }) {
                                if (!isConfirming) enteredPin = valDigits else confirmPin = valDigits
                                pinError = ""
                            }
                        },
                        label = { Text("Mã PIN (6 chữ số)") },
                        singleLine = true,
                        isError = pinError.isNotBlank()
                    )
                    if (pinError.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = pinError, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val input = if (!isConfirming) enteredPin else confirmPin
                        if (input.length != 6) {
                            pinError = "Vui lòng nhập đủ 6 chữ số!"
                            return@TextButton
                        }

                        when (pinDialogMode) {
                            "ENABLE", "CHANGE" -> {
                                if (!isConfirming) {
                                    isConfirming = true
                                } else {
                                    if (confirmPin == enteredPin) {
                                        onTogglePinSecurity(true, confirmPin)
                                        showPinDialog = false
                                    } else {
                                        pinError = "Mã PIN xác nhận không khớp!"
                                        confirmPin = ""
                                    }
                                }
                            }
                            "DISABLE" -> {
                                if (onVerifyPin(enteredPin)) {
                                    onTogglePinSecurity(false, "")
                                    showPinDialog = false
                                } else {
                                    pinError = "Mã PIN không chính xác!"
                                }
                            }
                        }
                    }
                ) {
                    Text("Tiếp tục")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPinDialog = false }) {
                    Text("Hủy")
                }
            }
        )
    }
}
