package com.nightowleyes.owlnetguard.ui.dialogs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nightowleyes.owlnetguard.R
import com.nightowleyes.owlnetguard.ui.MainActivity
import com.nightowleyes.owlnetguard.ui.theme.BrandPrimary
import com.nightowleyes.owlnetguard.ui.theme.BrandPrimaryLight
import com.nightowleyes.owlnetguard.ui.theme.CyberCyan
import com.nightowleyes.owlnetguard.ui.theme.CyberCyanLight
import com.nightowleyes.owlnetguard.ui.theme.FacetedBadgeShape
import com.nightowleyes.owlnetguard.ui.theme.FacetedCardShape
import com.nightowleyes.owlnetguard.ui.theme.FacetedTileShape
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
    NONE, PRIVACY_POLICY, OPEN_SOURCE_LICENSES, CHECK_UPDATE, USAGE_ACCESS_PROMPT, DONATE
}

@Composable
fun UsageAccessPromptDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = FacetedCardShape,
        modifier = Modifier.border(1.dp, SlateBorder, FacetedCardShape),
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
        shape = FacetedCardShape,
        modifier = Modifier.border(1.dp, SlateBorder, FacetedCardShape),
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
        shape = FacetedCardShape,
        modifier = Modifier.border(1.dp, SlateBorder, FacetedCardShape),
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

/**
 * So sánh 2 chuỗi phiên bản ngữ nghĩa (SemVer), ví dụ: "1.1" và "1.0", "1.2.3" và "1.2.0".
 * Trả về:
 *   > 0 nếu remoteVersion mới hơn currentVersion (cần cập nhật)
 *   < 0 nếu currentVersion mới hơn remoteVersion (bản thử nghiệm/dev mới hơn release)
 *   == 0 nếu 2 phiên bản bằng nhau
 */
fun compareVersions(remoteVersion: String, currentVersion: String): Int {
    val cleanRemote = remoteVersion.trim().removePrefix("v").removePrefix("V")
    val cleanCurrent = currentVersion.trim().removePrefix("v").removePrefix("V")

    val remoteParts = cleanRemote.split(".").mapNotNull { it.filter { c -> c.isDigit() }.toIntOrNull() }
    val currentParts = cleanCurrent.split(".").mapNotNull { it.filter { c -> c.isDigit() }.toIntOrNull() }

    val maxLen = maxOf(remoteParts.size, currentParts.size)
    for (i in 0 until maxLen) {
        val r = remoteParts.getOrElse(i) { 0 }
        val c = currentParts.getOrElse(i) { 0 }
        if (r > c) return 1
        if (r < c) return -1
    }
    return 0
}

/**
 * Làm sạch chuỗi ghi chú phát hành (Release Notes) từ GitHub API:
 * Loại bỏ các thẻ HTML, ảnh/badge markdown, link cú pháp thô để hiển thị văn bản gọn gàng.
 */
fun cleanReleaseNotes(rawBody: String): String {
    if (rawBody.isBlank()) return ""

    var cleaned = rawBody
    // 1. Loại bỏ các thẻ HTML (như <p>, <img>, <div>, ...)
    cleaned = cleaned.replace(Regex("<[^>]*>", RegexOption.DOT_MATCHES_ALL), " ")
    // 2. Loại bỏ các badge markdown lồng nhau dạng [![...](...)](...)
    cleaned = cleaned.replace(Regex("\\[\\s*!\\[[^\\]]*\\]\\([^)]*\\)\\s*\\]\\([^)]*\\)"), " ")
    // 3. Loại bỏ ảnh markdown standalone: ![alt](url)
    cleaned = cleaned.replace(Regex("!\\[[^\\]]*\\]\\([^)]*\\)"), " ")
    // 4. Đổi link markdown [tên](url) -> tên
    cleaned = cleaned.replace(Regex("\\[([^\\]]+)\\]\\([^)]*\\)"), "$1")
    // 5. Loại bỏ ký tự in đậm, in nghiêng (**text**, *text*, __text__)
    cleaned = cleaned.replace(Regex("\\*\\*([^*]+)\\*\\*"), "$1")
    cleaned = cleaned.replace(Regex("__([^_]+)__"), "$1")
    cleaned = cleaned.replace(Regex("\\*([^*]+)\\*"), "$1")

    // 6. Lọc các dòng trống, link thuần hoặc dòng changelog commit tự động
    val lines = cleaned.lines()
        .map { it.trim() }
        .filter { line ->
            line.isNotBlank() &&
                !line.startsWith("http://") &&
                !line.startsWith("https://") &&
                !line.startsWith("Full Changelog", ignoreCase = true)
        }

    val result = lines.joinToString("\n").trim()
    return if (result.isNotBlank()) {
        if (result.length > 350) result.take(350).trimEnd() + "..." else result
    } else {
        ""
    }
}

@Composable
fun CheckUpdateDialog(
    onDismiss: () -> Unit,
    onOpenReleases: (String) -> Unit
) {
    var isChecking by remember { mutableStateOf(true) }
    var statusTitle by remember { mutableStateOf("Đang kiểm tra bản cập nhật...") }
    var statusMessage by remember { mutableStateOf("Đang kết nối tới GitHub NightOwlEyes/OwlNetGuard...") }
    var remoteVersion by remember { mutableStateOf("") }
    var releaseNotes by remember { mutableStateOf("") }
    var comparison by remember { mutableStateOf(0) }
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
                    val rawNotes = json.optString("body", "")
                    val cleanedNotes = cleanReleaseNotes(rawNotes)
                    remoteVersion = tagName
                    releaseNotes = cleanedNotes
                    targetUrl = htmlUrl

                    val comp = compareVersions(tagName, MainActivity.CURRENT_APP_VERSION)
                    comparison = comp

                    if (comp > 0) {
                        statusTitle = "Có bản cập nhật mới: v$tagName"
                        statusMessage = "Đã có phiên bản mới hơn trên GitHub."
                    } else if (comp == 0) {
                        statusTitle = "Bạn đang dùng bản mới nhất"
                        statusMessage = "Phiên bản hiện tại (v${MainActivity.CURRENT_APP_VERSION}) đã là bản mới nhất trên GitHub."
                    } else {
                        statusTitle = "Bạn đang dùng bản mới nhất"
                        statusMessage = "Phiên bản hiện tại (v${MainActivity.CURRENT_APP_VERSION}) mới hơn bản phát hành trên GitHub (v$tagName)."
                    }
                } else if (code == 404) {
                    statusTitle = "Phiên bản hiện tại: v${MainActivity.CURRENT_APP_VERSION}"
                    statusMessage = "Kho lưu trữ GitHub (NightOwlEyes/OwlNetGuard) hiện chưa có bản phát hành (Release) nào mới hơn.\nBạn có thể truy cập trực tiếp GitHub để theo dõi."
                    targetUrl = MainActivity.GITHUB_REPO_URL
                } else {
                    statusTitle = "Không thể kiểm tra tự động"
                    statusMessage = "Máy chủ GitHub trả về mã $code. Bạn có thể mở trực tiếp trang phát hành trên GitHub."
                }
            } catch (e: Exception) {
                statusTitle = "Lỗi kết nối kiểm tra cập nhật"
                statusMessage = "Không thể kiểm tra bản phát hành mới từ GitHub (${e.localizedMessage ?: "Mất kết nối mạng"})."
            } finally {
                isChecking = false
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = FacetedCardShape,
        modifier = Modifier.border(1.dp, SlateBorder, FacetedCardShape),
        containerColor = SlateSurfaceElevated,
        titleContentColor = TextPrimary,
        textContentColor = TextSecondary,
        title = {
            Text(
                text = statusTitle,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 17.sp,
                color = if (comparison > 0) CyberCyanLight else TextPrimary
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (isChecking) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp)
                    ) {
                        CircularProgressIndicator(
                            color = CyberCyan,
                            strokeWidth = 2.5.dp,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(statusMessage, fontSize = 12.5.sp, color = TextSecondary)
                    }
                } else {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Phiên bản hiện tại:", fontSize = 12.5.sp, color = TextMuted)
                            Text(
                                "v${MainActivity.CURRENT_APP_VERSION}",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }
                        if (remoteVersion.isNotBlank()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Bản phát hành GitHub:", fontSize = 12.5.sp, color = TextMuted)
                                Text(
                                    "v$remoteVersion",
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (comparison > 0) CyberCyan else TextSecondary
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = SlateBorder)

                    Text(statusMessage, fontSize = 12.5.sp, color = TextSecondary)

                    if (releaseNotes.isNotBlank()) {
                        Text(
                            text = "Chi tiết cập nhật:",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                        Text(
                            text = releaseNotes,
                            fontSize = 12.sp,
                            color = TextSecondary,
                            lineHeight = 17.sp
                        )
                    }
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

@Composable
fun DonateDialog(
    onDismiss: () -> Unit,
    onOpenSponsors: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = FacetedCardShape,
        modifier = Modifier.border(1.dp, SlateBorder, FacetedCardShape),
        containerColor = SlateSurfaceElevated,
        titleContentColor = TextPrimary,
        textContentColor = TextSecondary,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_diamond_heart),
                    contentDescription = null,
                    tint = Color.Unspecified,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Ủng hộ nhà phát triển",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 17.sp,
                    color = CyberCyanLight
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Nếu bạn thấy OwlNetGuard hữu ích và yêu thích sự tiện lợi của ứng dụng, hãy ủng hộ nhà phát triển một ly cà phê để tiếp thêm động lực duy trì và phát triển thêm các tính năng tuyệt vời tiếp theo nhé! Cảm ơn bạn rất nhiều! 💖",
                    fontSize = 12.5.sp,
                    color = TextPrimary,
                    lineHeight = 18.sp
                )

                // QR Code Card
                Card(
                    shape = FacetedTileShape,
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF160926)),
                    border = BorderStroke(1.dp, BrandPrimary.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .padding(vertical = 4.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(8.dp)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_donate_qr),
                            contentDescription = "VietQR TPBank - DO HUY TRUC",
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(0.82f),
                            contentScale = ContentScale.Fit
                        )
                    }
                }

                HorizontalDivider(color = SlateBorder)

                // GitHub Sponsors Button
                Button(
                    onClick = onOpenSponsors,
                    colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
                    shape = FacetedBadgeShape,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_diamond_heart),
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Ủng hộ qua GitHub Sponsors",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Đóng", color = CyberCyan, fontWeight = FontWeight.Bold)
            }
        }
    )
}
