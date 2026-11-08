package com.nightowleyes.owlnetguard.ui.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nightowleyes.owlnetguard.ui.MainActivity
import com.nightowleyes.owlnetguard.ui.theme.BrandPrimary
import com.nightowleyes.owlnetguard.ui.theme.BrandPrimaryLight
import com.nightowleyes.owlnetguard.ui.theme.CyberCyan
import com.nightowleyes.owlnetguard.ui.theme.CyberCyanLight
import com.nightowleyes.owlnetguard.ui.theme.FacetedBadgeShape
import com.nightowleyes.owlnetguard.ui.theme.SlateBorder
import com.nightowleyes.owlnetguard.ui.theme.SlateSurfaceElevated
import com.nightowleyes.owlnetguard.ui.theme.TextMuted
import com.nightowleyes.owlnetguard.ui.theme.TextPrimary
import com.nightowleyes.owlnetguard.ui.theme.TextSecondary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

enum class InfoDialogType {
    NONE, PRIVACY_POLICY, OPEN_SOURCE_LICENSES, CHECK_UPDATE, USAGE_ACCESS_PROMPT
}

@Composable
fun UsageAccessPromptDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SlateSurfaceElevated,
        titleContentColor = TextPrimary,
        textContentColor = TextSecondary,
        title = {
            Text(
                text = "Cấp quyền nhận diện ứng dụng đang mở",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 16.5.sp,
                color = CyberCyanLight
            )
        },
        text = {
            Text(
                text = "Để chế độ \"Chỉ cách ly ứng dụng đang mở\" hoạt động, OwlNetGuard cần quyền Truy cập dữ liệu sử dụng (Usage Access) chuẩn của Android nhằm biết ứng dụng nào đang hiển thị trên màn hình.\n\nNhấn \"Mở cài đặt\" và bật công tắc cho OwlNetGuard.",
                fontSize = 13.sp
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Để sau", color = TextMuted)
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
                shape = FacetedBadgeShape
            ) {
                Text("Mở cài đặt", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
fun PrivacyPolicyDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SlateSurfaceElevated,
        titleContentColor = TextPrimary,
        textContentColor = TextSecondary,
        title = {
            Text(
                text = "Chính sách bảo mật",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 17.sp,
                color = CyberCyanLight
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "OwlNetGuard cam kết bảo vệ quyền riêng tư tuyệt đối của người dùng:",
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    fontSize = 13.sp
                )
                Text(
                    text = "1. Xử lý 100% cục bộ trên thiết bị: OwlNetGuard sử dụng Android VpnService tiêu chuẩn để tạo giao diện mạng cục bộ (Sinkhole) ngay trên điện thoại nhằm cách ly kết nối mạng của các ứng dụng được bạn chỉ định. Không có bất kỳ lưu lượng mạng nào được gửi ra máy chủ bên ngoài.",
                    fontSize = 12.5.sp
                )
                Text(
                    text = "2. Không thu thập dữ liệu cá nhân: Ứng dụng không thu thập, không lưu trữ và không chia sẻ thông tin cá nhân, lịch sử duyệt web hay nội dung gói tin của bạn cho bất kỳ bên thứ ba nào.",
                    fontSize = 12.5.sp
                )
                Text(
                    text = "3. Quyền ứng dụng minh bạch:\n• Quyền VPN (VpnService): Chỉ dùng để ngắt kết nối mạng cục bộ cho các ứng dụng được chọn.\n• Quyền Hiển thị trên ứng dụng khác: Dùng để hiển thị phím nổi điều khiển nhanh trên màn hình.\n• Quyền Truy cập dữ liệu sử dụng (tùy chọn): Chỉ dùng để nhận diện ứng dụng đang mở trên màn hình khi bạn bật chế độ \"Chỉ ứng dụng đang mở\".",
                    fontSize = 12.5.sp
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Đóng", color = CyberCyan, fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
fun OpenSourceLicensesDialog(
    onDismiss: () -> Unit,
    onOpenGitHub: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SlateSurfaceElevated,
        titleContentColor = TextPrimary,
        textContentColor = TextSecondary,
        title = {
            Text(
                text = "Giấy phép mã nguồn mở",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 17.sp,
                color = CyberCyanLight
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "OwlNetGuard là dự án mã nguồn mở phát hành theo giấy phép GNU General Public License v3.0 (GPL-3.0) tại:\nhttps://github.com/NightOwlEyes/OwlNetGuard",
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    fontSize = 12.5.sp
                )
                Text(
                    text = "Giấy phép GPL-3.0 bảo đảm quyền tự do sử dụng, nghiên cứu, chia sẻ và sửa đổi phần mềm cho toàn bộ cộng đồng.",
                    color = TextSecondary,
                    fontSize = 11.5.sp
                )
                HorizontalDivider(color = SlateBorder)
                Text(
                    text = "Các thư viện mã nguồn mở được sử dụng:",
                    fontWeight = FontWeight.SemiBold,
                    color = BrandPrimaryLight,
                    fontSize = 12.5.sp
                )
                Text(
                    text = "• Android Jetpack Compose (UI, Material 3, Foundation) - Apache-2.0\n" +
                        "• AndroidX Core KTX & Activity Compose - Apache-2.0\n" +
                        "• AndroidX Lifecycle Runtime - Apache-2.0\n" +
                        "• Kotlin Standard Library & Kotlinx Coroutines - Apache-2.0\n" +
                        "• Josefin Sans Font - SIL Open Font License (OFL 1.1)\n\n" +
                        "Bản quyền dự án © NightOwlEyes. Phát hành theo giấy phép GNU General Public License v3.0 (GPL-3.0).",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onOpenGitHub) {
                Text("Mở GitHub", color = BrandPrimaryLight, fontWeight = FontWeight.Bold)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Đóng", color = CyberCyan, fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
fun CheckUpdateDialog(
    onDismiss: () -> Unit,
    onOpenReleases: (String) -> Unit
) {
    var isChecking by remember { mutableStateOf(true) }
    var statusTitle by remember { mutableStateOf("Đang kiểm tra bản cập nhật...") }
    var statusMessage by remember { mutableStateOf("Đang kết nối tới GitHub NightOwlEyes/OwlNetGuard...") }
    var targetUrl by remember { mutableStateOf(MainActivity.GITHUB_RELEASES_URL) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            try {
                val url = URL(MainActivity.GITHUB_LATEST_API)
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    setRequestProperty("Accept", "application/vnd.github+json")
                    connectTimeout = 6000
                    readTimeout = 6000
                }
                val code = conn.responseCode
                if (code == 200) {
                    val body = conn.inputStream.bufferedReader().use { it.readText() }
                    val json = JSONObject(body)
                    val tagName = json.optString("tag_name", "").removePrefix("v").removePrefix("V")
                    val htmlUrl = json.optString("html_url", MainActivity.GITHUB_RELEASES_URL)
                    val releaseNotes = json.optString("body", "").take(300)
                    targetUrl = htmlUrl

                    if (tagName.isNotBlank() && tagName != MainActivity.CURRENT_APP_VERSION) {
                        statusTitle = "Có bản cập nhật mới: v$tagName"
                        statusMessage = "Phiên bản hiện tại: v${MainActivity.CURRENT_APP_VERSION}\n" +
                            "Phiên bản mới nhất: v$tagName\n\n" +
                            if (releaseNotes.isNotBlank()) "Chi tiết:\n$releaseNotes" else "Nhấn \"Mở GitHub\" để cập nhật."
                    } else {
                        statusTitle = "Bạn đang dùng bản mới nhất"
                        statusMessage = "Phiên bản hiện tại (v${MainActivity.CURRENT_APP_VERSION}) là phiên bản mới nhất trên GitHub."
                    }
                } else if (code == 404) {
                    statusTitle = "Phiên bản hiện tại: v${MainActivity.CURRENT_APP_VERSION}"
                    statusMessage = "Kho lưu trữ GitHub (NightOwlEyes/OwlNetGuard) hiện chưa có bản phát hành (Release) mới hơn.\nBạn có thể truy cập trực tiếp GitHub để theo dõi."
                    targetUrl = MainActivity.GITHUB_REPO_URL
                } else {
                    statusTitle = "Không thể kiểm tra tự động"
                    statusMessage = "Máy chủ GitHub trả về mã $code. Bạn có thể mở trực tiếp trang phát hành trên GitHub."
                }
            } catch (_: Exception) {
                statusTitle = "Không thể kết nối GitHub"
                statusMessage = "Vui lòng kiểm tra kết nối mạng hoặc mở trực tiếp kho lưu trữ tại:\n${MainActivity.GITHUB_REPO_URL}"
                targetUrl = MainActivity.GITHUB_REPO_URL
            } finally {
                isChecking = false
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SlateSurfaceElevated,
        titleContentColor = TextPrimary,
        textContentColor = TextSecondary,
        title = {
            Text(
                text = statusTitle,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 16.5.sp,
                color = CyberCyanLight
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Phiên bản hiện tại: v${MainActivity.CURRENT_APP_VERSION}",
                    fontWeight = FontWeight.Bold,
                    color = BrandPrimaryLight,
                    fontSize = 12.sp
                )
                HorizontalDivider(color = SlateBorder)
                if (isChecking) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(
                            color = CyberCyan,
                            strokeWidth = 2.5.dp,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(statusMessage, fontSize = 12.5.sp)
                    }
                } else {
                    Text(statusMessage, fontSize = 12.5.sp)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = { onOpenReleases(targetUrl) }) {
                Text("Mở GitHub", color = BrandPrimaryLight, fontWeight = FontWeight.Bold)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Đóng", color = CyberCyan, fontWeight = FontWeight.Bold)
            }
        }
    )
}
