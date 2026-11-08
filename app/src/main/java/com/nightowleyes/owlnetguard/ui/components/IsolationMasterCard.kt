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
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import com.nightowleyes.owlnetguard.ui.theme.BrandPrimary
import com.nightowleyes.owlnetguard.ui.theme.BrandPrimaryDeep
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
    isIsolationActive: Boolean = true,
    activeAppsCount: Int,
    totalSelectedCount: Int,
    onToggleVpn: (Boolean) -> Unit
) {
    val isEffectivelyActive = isVpnEnabled && isIsolationActive
    val accentColor = if (isEffectivelyActive) DangerRed else BrandPrimary
    val borderBrush = remember(isEffectivelyActive) {
        if (isEffectivelyActive) {
            Brush.linearGradient(listOf(Color.White.copy(alpha = 0.85f), DangerRed, BrandPrimaryDeep))
        } else {
            Brush.linearGradient(listOf(BrandPrimary.copy(alpha = 0.7f), CyberCyan.copy(alpha = 0.5f)))
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 4.dp),
        shape = FacetedCardShape,
        colors = CardDefaults.cardColors(
            containerColor = if (isEffectivelyActive) DangerRedMuted.copy(alpha = 0.45f) else SlateSurface
        ),
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
                drawPath(p1, if (isEffectivelyActive) DangerRed.copy(alpha = 0.08f) else BrandPrimary.copy(alpha = 0.08f))

                drawLine(
                    brush = Brush.horizontalGradient(
                        if (isEffectivelyActive) {
                            listOf(Color.White.copy(alpha = 0.6f), DangerRed.copy(alpha = 0.4f), Color.Transparent)
                        } else {
                            listOf(Color.White.copy(alpha = 0.5f), BrandPrimary.copy(alpha = 0.3f), Color.Transparent)
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
                                id = if (isEffectivelyActive) R.drawable.logo_faceted_apex_isolated else R.drawable.logo_faceted_apex
                            ),
                            contentDescription = null,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.size(34.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Cách ly ứng dụng",
                                color = if (isEffectivelyActive) Color.White else TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = when {
                                    !isVpnEnabled ->
                                        "Duy trì kết nối mạng VPN để cách ly ứng dụng đã chọn"
                                    !isIsolationActive ->
                                        "Tạm dừng cách ly (VPN nền đang bật)\nChạm phím nổi để kích hoạt lại"
                                    activeAppsCount > 0 ->
                                        "Đang cách ly $activeAppsCount/$totalSelectedCount ứng dụng\nVPN nền đang bật"
                                    else ->
                                        "Sẵn sàng (chưa chọn ứng dụng)\nVPN nền đang bật"
                                },
                                color = if (isEffectivelyActive) DangerRed else if (isVpnEnabled) CyberCyan else TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }
                    Switch(
                        checked = isVpnEnabled,
                        onCheckedChange = onToggleVpn,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = accentColor,
                            uncheckedThumbColor = TextMuted,
                            uncheckedTrackColor = SlateSurfaceElevated
                        )
                    )
                }
            }
        }
    }
}
