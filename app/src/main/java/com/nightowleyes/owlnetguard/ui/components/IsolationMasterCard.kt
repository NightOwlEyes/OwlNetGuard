package com.nightowleyes.owlnetguard.ui.components

import androidx.compose.foundation.Canvas as ComposeCanvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nightowleyes.owlnetguard.R
import com.nightowleyes.owlnetguard.data.PreferencesManager
import com.nightowleyes.owlnetguard.ui.theme.BrandPrimary
import com.nightowleyes.owlnetguard.ui.theme.BrandPrimaryDeep
import com.nightowleyes.owlnetguard.ui.theme.BrandPrimaryLight
import com.nightowleyes.owlnetguard.ui.theme.CyberCyan
import com.nightowleyes.owlnetguard.ui.theme.DangerRed
import com.nightowleyes.owlnetguard.ui.theme.DangerRedMuted
import com.nightowleyes.owlnetguard.ui.theme.FacetedCardShape
import com.nightowleyes.owlnetguard.ui.theme.SlateSurface
import com.nightowleyes.owlnetguard.ui.theme.SlateSurfaceElevated
import com.nightowleyes.owlnetguard.ui.theme.TextMuted
import com.nightowleyes.owlnetguard.ui.theme.TextPrimary
import com.nightowleyes.owlnetguard.ui.theme.TextSecondary

@Composable
fun IsolationMasterCard(
    isVpnEnabled: Boolean,
    isBubbleOverlayEnabled: Boolean,
    isolationMode: Int,
    isIsolationActive: Boolean = true,
    activeAppsCount: Int,
    totalSelectedCount: Int,
    onToggleVpn: (Boolean) -> Unit
) {
    // Xác định trạng thái (1) Violet (Đang tạm dừng cách ly) hay (2) Scarlet (Đang cách ly X/Y ứng dụng)
    val isState2: Boolean = when {
        !isVpnEnabled -> false
        !isBubbleOverlayEnabled -> {
            // Phím nổi: Tắt
            // - Nếu: "Ứng dụng đã chọn" toàn bộ "Đang tạm dừng" -> (1)
            // - Nếu: "Ứng dụng đã chọn" một hoặc toàn bộ "Đang cách ly" -> (2)
            activeAppsCount > 0
        }
        isolationMode == PreferencesManager.ISOLATION_MODE_FOREGROUND -> {
            // Phím nổi: Bật | Mode: "Chỉ app đang mở"
            // - Nếu đang ở trong app hoặc trên các ứng dụng không có trong danh sách -> (1)
            // - Nếu ở trong app có trong danh sách -> (2)
            activeAppsCount > 0
        }
        else -> {
            // Phím nổi: Bật | Mode: "Tất cả app đã chọn"
            // Bấm vào một ứng dụng thì toàn bộ app chuyển thành "Đang tạm dừng" -> (1) / "Đang cách ly" -> (2)
            // Bấm vào phím nổi thì toàn bộ app chuyển thành "Đang tạm dừng" -> (1) / "Đang cách ly" -> (2)
            activeAppsCount > 0 && isIsolationActive
        }
    }

    val accentColor = when {
        !isVpnEnabled -> BrandPrimary
        isState2 -> DangerRed // (2) Scarlet
        else -> BrandPrimary // (1) Violet
    }

    val borderBrush = remember(isVpnEnabled, isState2) {
        when {
            !isVpnEnabled ->
                Brush.linearGradient(listOf(BrandPrimary.copy(alpha = 0.5f), CyberCyan.copy(alpha = 0.3f)))
            isState2 ->
                Brush.linearGradient(listOf(Color.White.copy(alpha = 0.85f), DangerRed, BrandPrimaryDeep))
            else ->
                Brush.linearGradient(listOf(BrandPrimary.copy(alpha = 0.9f), BrandPrimaryDeep, CyberCyan.copy(alpha = 0.45f)))
        }
    }

    val containerColor = when {
        !isVpnEnabled -> SlateSurface
        isState2 -> DangerRedMuted.copy(alpha = 0.45f)
        else -> Color(0xFF1E1338).copy(alpha = 0.70f) // Deep Violet
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 4.dp),
        shape = FacetedCardShape,
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = CardDefaults.outlinedCardBorder().copy(width = 1.2.dp, brush = borderBrush)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            ComposeCanvas(modifier = Modifier.matchParentSize()) {
                val w = size.width
                val h = size.height
                val p1 = Path().apply {
                    moveTo(w * 0.72f, 0f)
                    lineTo(w, 0f)
                    lineTo(w, h * 0.5f)
                    lineTo(w * 0.85f, h * 0.35f)
                    close()
                }
                drawPath(
                    p1,
                    when {
                        !isVpnEnabled -> BrandPrimary.copy(alpha = 0.05f)
                        isState2 -> DangerRed.copy(alpha = 0.08f)
                        else -> BrandPrimary.copy(alpha = 0.09f)
                    }
                )

                drawLine(
                    brush = Brush.horizontalGradient(
                        when {
                            !isVpnEnabled -> listOf(BrandPrimary.copy(alpha = 0.3f), Color.Transparent)
                            isState2 -> listOf(Color.White.copy(alpha = 0.6f), DangerRed.copy(alpha = 0.4f), Color.Transparent)
                            else -> listOf(Color.White.copy(alpha = 0.5f), BrandPrimary.copy(alpha = 0.4f), Color.Transparent)
                        }
                    ),
                    start = Offset(0f, 1.5f),
                    end = Offset(w * 0.6f, 1.5f),
                    strokeWidth = 3f
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                // Main Switch Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Image(
                            painter = painterResource(
                                id = if (isVpnEnabled && isState2) R.drawable.logo_faceted_apex_isolated else R.drawable.logo_faceted_apex
                            ),
                            contentDescription = null,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.size(34.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Cách ly ứng dụng",
                                color = if (isVpnEnabled) Color.White else TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = when {
                                    !isVpnEnabled ->
                                        "Duy trì kết nối mạng VPN để cách ly ứng dụng đã chọn"
                                    isState2 ->
                                        "VPN nền đang bật\nĐang cách ly $activeAppsCount/$totalSelectedCount ứng dụng"
                                    else ->
                                        "VPN nền đang bật\nĐang tạm dừng cách ly"
                                },
                                color = when {
                                    !isVpnEnabled -> TextSecondary
                                    isState2 -> DangerRed
                                    else -> BrandPrimaryLight
                                },
                                fontSize = 11.sp
                            )
                        }
                    }
                    FacetedSwitch(
                        checked = isVpnEnabled,
                        onCheckedChange = onToggleVpn,
                        checkedTrackColor = accentColor,
                        uncheckedTrackColor = SlateSurfaceElevated,
                        checkedThumbColor = Color.White,
                        uncheckedThumbColor = TextMuted
                    )
                }
            }
        }
    }
}
