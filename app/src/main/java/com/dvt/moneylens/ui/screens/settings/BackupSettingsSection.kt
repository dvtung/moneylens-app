package com.dvt.moneylens.ui.screens.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import com.dvt.moneylens.data.backup.BackupSummary
import com.dvt.moneylens.data.backup.DataBackupDto
import com.dvt.moneylens.ui.theme.Spacing
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun BackupSettingsSection(
    lastBackupTimestamp: Long? = null,
    isBackupProcessing: Boolean = false,
    onExportBackup: (Uri, onResult: (Boolean, String?) -> Unit) -> Unit,
    onReadBackupFile: (Uri, onSuccess: (DataBackupDto, BackupSummary) -> Unit, onError: (String) -> Unit) -> Unit,
    onRestoreBackup: (DataBackupDto, onResult: (Boolean, String?) -> Unit) -> Unit,
    onShareBackup: () -> Unit,
    modifier: Modifier = Modifier
) {
    var backupStatusMessage by remember { mutableStateOf<String?>(null) }
    var pendingRestoreDto by remember { mutableStateOf<DataBackupDto?>(null) }
    var pendingRestoreSummary by remember { mutableStateOf<BackupSummary?>(null) }
    var showRestoreConfirmDialog by remember { mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            onExportBackup(uri) { success, msg ->
                backupStatusMessage = if (success) "Đã xuất dữ liệu thành công!" else "Lỗi: $msg"
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            onReadBackupFile(
                uri,
                { dto, summary ->
                    pendingRestoreDto = dto
                    pendingRestoreSummary = summary
                    showRestoreConfirmDialog = true
                },
                { err ->
                    backupStatusMessage = "File không hợp lệ: $err"
                }
            )
        }
    }

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
                    imageVector = Icons.Rounded.Backup,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(Spacing.md))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Sao lưu & Phục hồi dữ liệu",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = if (lastBackupTimestamp != null && lastBackupTimestamp > 0) {
                            "Lần sao lưu gần nhất: ${SimpleDateFormat("HH:mm, dd/MM/yyyy", Locale.getDefault()).format(Date(lastBackupTimestamp))}"
                        } else "Chưa sao lưu dữ liệu lần nào",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(Spacing.md))

            if (backupStatusMessage != null) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = backupStatusMessage!!,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(Spacing.sm)
                    )
                }
                Spacer(modifier = Modifier.height(Spacing.sm))
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = {
                        val fileName = "MoneyLens_Backup_${SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())}.json"
                        exportLauncher.launch(fileName)
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Rounded.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(Spacing.xs))
                    Text("Xuất JSON")
                }

                OutlinedButton(
                    onClick = { importLauncher.launch(arrayOf("application/json", "*/*")) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Rounded.FileUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(Spacing.xs))
                    Text("Phục hồi")
                }
            }

            Spacer(modifier = Modifier.height(Spacing.xs))

            IconButton(onClick = onShareBackup, modifier = Modifier.align(Alignment.End)) {
                Icon(imageVector = Icons.Rounded.Share, contentDescription = "Chia sẻ bản sao lưu", tint = MaterialTheme.colorScheme.primary)
            }
        }
    }

    if (showRestoreConfirmDialog && pendingRestoreSummary != null && pendingRestoreDto != null) {
        val summary = pendingRestoreSummary!!
        AlertDialog(
            onDismissRequest = { showRestoreConfirmDialog = false },
            title = { Text("Xác nhận khôi phục dữ liệu?") },
            text = {
                Column {
                    Text("Dữ liệu hiện tại sẽ bị thay thế bằng bản sao lưu:")
                    Spacer(modifier = Modifier.height(Spacing.sm))
                    Text("• Phiên bản: Schema v${summary.schemaVersion}")
                    Text("• Tổng số bản ghi: ${summary.totalRecordCount}")
                    Text("• Giao dịch: ${summary.transactionCount} | Tài khoản: ${summary.accountCount}")
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onRestoreBackup(pendingRestoreDto!!) { success, msg ->
                            backupStatusMessage = if (success) "Đã khôi phục dữ liệu thành công!" else "Lỗi: $msg"
                            showRestoreConfirmDialog = false
                        }
                    }
                ) {
                    Text("Khôi phục ngay")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreConfirmDialog = false }) {
                    Text("Hủy")
                }
            }
        )
    }
}
