package com.dvt.moneylens.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Backspace
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dvt.moneylens.ui.theme.Spacing

@Composable
fun LockScreen(
    userName: String = "MoneyLens",
    userAvatar: String = "💼",
    onUnlockSuccess: () -> Unit,
    onVerifyPin: (String) -> Boolean,
    isBiometricAvailable: Boolean = true,
    onBiometricClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var enteredPin by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var showBiometricDialog by remember { mutableStateOf(false) }

    fun handleDigit(digit: String) {
        if (enteredPin.length < 6) {
            val newPin = enteredPin + digit
            enteredPin = newPin
            isError = false
            errorMessage = ""

            if (newPin.length == 6) {
                if (onVerifyPin(newPin)) {
                    onUnlockSuccess()
                } else {
                    isError = true
                    errorMessage = "Mã PIN không chính xác. Vui lòng thử lại!"
                    enteredPin = ""
                }
            }
        }
    }

    fun handleBackspace() {
        if (enteredPin.isNotEmpty()) {
            enteredPin = enteredPin.dropLast(1)
            isError = false
            errorMessage = ""
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = Spacing.xl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.height(Spacing.md))

            // Header Lock Branding & Personalized Greeting
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    tonalElevation = 3.dp,
                    modifier = Modifier.size(68.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(text = userAvatar, fontSize = 34.sp)
                    }
                }

                Spacer(modifier = Modifier.height(Spacing.sm))

                Text(
                    text = "Xin chào, $userName 👋",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = "Nhập mã PIN 6 số hoặc sinh trắc học để mở khóa",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(Spacing.lg))

                // 6 PIN Dots
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (i in 0 until 6) {
                        val isFilled = i < enteredPin.length
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        isError -> MaterialTheme.colorScheme.error
                                        isFilled -> MaterialTheme.colorScheme.primary
                                        else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                                    }
                                )
                        )
                    }
                }

                AnimatedVisibility(visible = isError) {
                    Text(
                        text = errorMessage,
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = Spacing.md)
                    )
                }
            }

            // Keypad (3x4 Grid)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = Spacing.xl),
                verticalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {
                val rows = listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9"),
                    listOf("BIOMETRIC", "0", "BACKSPACE")
                )

                for (row in rows) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        for (key in row) {
                            when (key) {
                                "BIOMETRIC" -> {
                                    if (isBiometricAvailable) {
                                        IconButton(
                                            onClick = {
                                                if (onBiometricClick != null) {
                                                    onBiometricClick()
                                                } else {
                                                    showBiometricDialog = true
                                                }
                                            },
                                            modifier = Modifier.size(68.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Fingerprint,
                                                contentDescription = "Mở khóa sinh trắc học",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(34.dp)
                                            )
                                        }
                                    } else {
                                        Spacer(modifier = Modifier.size(68.dp))
                                    }
                                }
                                "BACKSPACE" -> {
                                    IconButton(
                                        onClick = { handleBackspace() },
                                        modifier = Modifier.size(68.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Backspace,
                                            contentDescription = "Xóa",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                }
                                else -> {
                                    Box(
                                        modifier = Modifier
                                            .size(68.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                                            .clickable { handleDigit(key) },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = key,
                                            style = MaterialTheme.typography.titleLarge.copy(
                                                fontSize = 24.sp,
                                                fontWeight = FontWeight.SemiBold
                                            ),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Popup Xác thực vân tay chuẩn Fintech (Fintech Biometric Authentication Dialog)
        if (showBiometricDialog) {
            com.dvt.moneylens.ui.designsystem.FintechBiometricPromptDialog(
                userName = userName,
                onSuccess = {
                    showBiometricDialog = false
                    onUnlockSuccess()
                },
                onDismiss = {
                    showBiometricDialog = false
                }
            )
        }
    }
}
