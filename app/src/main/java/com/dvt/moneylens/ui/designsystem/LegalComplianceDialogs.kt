package com.dvt.moneylens.ui.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.OpenInBrowser
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Sms
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.dvt.moneylens.ui.theme.Spacing

enum class DisclosureType {
    NOTIFICATION_LISTENER,
    SMS_PERMISSION
}

/**
 * Hộp thoại giải thích quyền minh bạch (Prominent In-App Disclosure)
 * Tuân thủ yêu cầu bắt buộc của Google Play User Data & Permissions Policy
 * trước khi kích hoạt quyền đọc thông báo hoặc nhận tin nhắn SMS ngân hàng.
 */
@Composable
fun ProminentDisclosureDialog(
    type: DisclosureType,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    onOpenPrivacyPolicy: () -> Unit
) {
    val isNotification = type == DisclosureType.NOTIFICATION_LISTENER
    val icon = if (isNotification) Icons.Rounded.NotificationsActive else Icons.Rounded.Sms
    val title = if (isNotification) "Minh bạch: Quyền đọc thông báo ngân hàng" else "Minh bạch: Quyền nhận SMS ngân hàng"

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .fillMaxWidth(0.92f)
            .clip(RoundedCornerShape(24.dp)),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(Spacing.md))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = if (isNotification) {
                        "Để tự động phát hiện và gợi ý ghi chép biến động số dư mà bạn không cần gõ tay, MoneyLens cần quyền đọc thông báo từ hệ thống Android:"
                    } else {
                        "Để ghi nhận biến động số dư qua tin nhắn SMS Brandname của ngân hàng, ứng dụng cần sự đồng ý truy cập quyền SMS của bạn:"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(Spacing.md))

                // Card tóm tắt cam kết
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(Spacing.md)) {
                        DisclosureBulletItem(
                            icon = Icons.Rounded.CheckCircle,
                            title = "Phạm vi giới hạn",
                            description = if (isNotification) {
                                "Chỉ quét nội dung thông báo từ các ứng dụng ngân hàng và ví điện tử được hỗ trợ."
                            } else {
                                "Chỉ lọc các tin nhắn từ đầu số ngân hàng, bỏ qua toàn bộ tin nhắn cá nhân."
                            }
                        )
                        Spacer(modifier = Modifier.height(Spacing.sm))
                        DisclosureBulletItem(
                            icon = Icons.Rounded.Lock,
                            title = "Bảo mật On-Device tuyệt đối",
                            description = "Thuật toán xử lý tức thì ngay trên thiết bị của bạn. Không gửi dữ liệu giao dịch ra ngoài Internet."
                        )
                        Spacer(modifier = Modifier.height(Spacing.sm))
                        DisclosureBulletItem(
                            icon = Icons.Rounded.Shield,
                            title = "Không đọc mã OTP hay mật khẩu",
                            description = "Ứng dụng tuyệt đối bỏ qua mã OTP, mã xác thực và mật khẩu đăng nhập của bạn."
                        )
                    }
                }

                Spacer(modifier = Modifier.height(Spacing.md))

                TextButton(
                    onClick = onOpenPrivacyPolicy,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Icon(imageVector = Icons.Rounded.Security, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(Spacing.xs))
                    Text(
                        text = "Xem chi tiết Chính sách bảo mật",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Tôi hiểu & Tiếp tục")
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Để sau")
            }
        }
    )
}

@Composable
private fun DisclosureBulletItem(
    icon: ImageVector,
    title: String,
    description: String
) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .size(18.dp)
                .padding(top = 2.dp)
        )
        Spacer(modifier = Modifier.width(Spacing.sm))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Hộp thoại hiển thị Chính sách quyền riêng tư (Privacy Policy)
 * ngay trong ứng dụng, đáp ứng yêu cầu của Google Play.
 */
@Composable
fun PrivacyPolicyDialog(
    onDismiss: () -> Unit,
    onOpenWebUrl: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .fillMaxWidth(0.94f)
            .clip(RoundedCornerShape(24.dp)),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Security,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(Spacing.md))
                Column {
                    Text(
                        text = "Chính sách quyền riêng tư",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "MoneyLens · Local-First Security",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Card tuyên bố cốt lõi
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Cam kết Local-First: Toàn bộ dữ liệu thu chi, tài khoản, thẻ và tài sản được lưu trữ và xử lý trực tiếp trên thiết bị của bạn. MoneyLens không thu thập và không chia sẻ dữ liệu với bất kỳ máy chủ bên thứ ba nào.",
                        style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(Spacing.md)
                    )
                }

                Spacer(modifier = Modifier.height(Spacing.md))

                PolicySection(
                    title = "1. Dữ liệu thu thập & xử lý",
                    content = "• Dữ liệu tài chính: Các khoản giao dịch, danh mục, số dư thẻ do bạn nhập hoặc quét được lưu trong cơ sở dữ liệu nội bộ (Room Database) trên máy.\n• Sinh trắc học: Sử dụng API Android BiometricPrompt chuẩn của hệ thống, không lưu dấu vân tay hay khuôn mặt."
                )

                PolicySection(
                    title = "2. Quyền truy cập thiết bị (Permissions)",
                    content = "• Notification Listener Service: Lắng nghe thông báo từ các app ngân hàng/ví điện tử để tự động gợi ý ghi chép. Xử lý tức thì (ephemerally) nội bộ trên máy, không lưu tin nhắn cá nhân.\n• SMS Permissions (Tùy chọn): Nhận biết SMS biến động số dư. Tuyệt đối không đọc OTP hay mật khẩu.\n• Quyền mạng (INTERNET): Chỉ dùng để cập nhật giá vàng và cổ phiếu công khai. Có thể tắt hoàn toàn qua Chế độ ngoại tuyến."
                )

                PolicySection(
                    title = "3. Quyền xóa dữ liệu (User Data Deletion)",
                    content = "Tuân thủ chính sách Google Play, bạn có toàn quyền xóa vĩnh viễn toàn bộ dữ liệu tài chính bất cứ lúc nào tại Cài đặt > 'Xóa sạch toàn bộ dữ liệu'. Khi gỡ cài đặt ứng dụng, hệ điều hành Android sẽ tự động xóa sạch toàn bộ cơ sở dữ liệu."
                )

                PolicySection(
                    title = "4. Liên hệ hỗ trợ nhà phát triển",
                    content = "Email: dvtung0910@gmail.com\nĐịa chỉ hỗ trợ: Ứng dụng di động MoneyLens"
                )

                Spacer(modifier = Modifier.height(Spacing.sm))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(Spacing.sm))

                OutlinedButton(
                    onClick = onOpenWebUrl,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Rounded.OpenInBrowser, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(Spacing.xs))
                    Text("Mở bản tài liệu trên trình duyệt")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Đã hiểu & Đóng")
            }
        }
    )
}

@Composable
private fun PolicySection(
    title: String,
    content: String
) {
    Column(modifier = Modifier.padding(vertical = Spacing.xs)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = content,
            style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
