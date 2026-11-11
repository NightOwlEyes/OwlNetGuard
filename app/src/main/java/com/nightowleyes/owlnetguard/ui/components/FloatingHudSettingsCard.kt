package com.nightowleyes.owlnetguard.ui.components

import androidx.compose.foundation.Canvas as ComposeCanvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nightowleyes.owlnetguard.data.PreferencesManager
import com.nightowleyes.owlnetguard.ui.theme.BrandPrimary
import com.nightowleyes.owlnetguard.ui.theme.CyberCyan
import com.nightowleyes.owlnetguard.ui.theme.CyberCyanLight
import com.nightowleyes.owlnetguard.ui.theme.DangerRed
import com.nightowleyes.owlnetguard.ui.theme.FacetedBadgeShape
import com.nightowleyes.owlnetguard.ui.theme.FacetedCardShape
import com.nightowleyes.owlnetguard.ui.theme.SlateBorder
import com.nightowleyes.owlnetguard.ui.theme.SlateSurface
import com.nightowleyes.owlnetguard.ui.theme.SlateSurfaceElevated
import com.nightowleyes.owlnetguard.ui.theme.TextMuted
import com.nightowleyes.owlnetguard.ui.theme.TextPrimary
import com.nightowleyes.owlnetguard.ui.theme.TextSecondary

@Composable
fun FloatingHudSettingsCard(
    isVpnEnabled: Boolean,
    canDrawOverlays: Boolean,
    isOverlayEnabled: Boolean,
    showIsolate: Boolean,
    showWifi: Boolean,
    showData: Boolean,
    showAirplane: Boolean,
    isolationMode: Int,
    hasUsageStatsPermission: Boolean,
    isAutoUnisolate: Boolean,
    excludedAppsCount: Int = 0,
    onToggleMasterOverlay: (Boolean) -> Unit,
    onToggleIsolate: (Boolean) -> Unit,
    onToggleWifi: (Boolean) -> Unit,
    onToggleData: (Boolean) -> Unit,
    onToggleAirplane: (Boolean) -> Unit,
    onToggleAutoUnisolate: (Boolean) -> Unit,
    onSelectIsolationMode: (Int) -> Unit,
    onOpenExcludedAppsDialog: () -> Unit = {},
    onRequestOverlayPermission: () -> Unit,
    onRequestUsageStatsPermission: () -> Unit
) {
    val isHudActive = isVpnEnabled && isOverlayEnabled && canDrawOverlays
    val borderBrush = remember(isHudActive) {
        if (isHudActive) {
            Brush.linearGradient(
                listOf(Color.White.copy(alpha = 0.7f), BrandPrimary, CyberCyan.copy(alpha = 0.7f))
            )
        } else {
            SolidColor(SlateBorder)
        }
    }

    val cardAlpha = if (isVpnEnabled) 1.0f else 0.45f

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 4.dp)
            .alpha(cardAlpha),
        shape = FacetedCardShape,
        colors = CardDefaults.cardColors(containerColor = SlateSurface),
        border = CardDefaults.outlinedCardBorder().copy(width = 1.2.dp, brush = borderBrush)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            ComposeCanvas(modifier = Modifier.matchParentSize()) {
                val w = size.width
                val h = size.height
                val p1 = Path().apply {
                    moveTo(w * 0.68f, 0f)
                    lineTo(w, 0f)
                    lineTo(w, h * 0.6f)
                    lineTo(w * 0.82f, h * 0.38f)
                    close()
                }
                drawPath(p1, BrandPrimary.copy(alpha = 0.08f))

                drawLine(
                    brush = Brush.horizontalGradient(
                        listOf(Color.White.copy(alpha = 0.5f), BrandPrimary.copy(alpha = 0.2f), Color.Transparent)
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
                // Header row of the HUD Card
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "◆ Phím nổi (HUD)",
                            color = CyberCyanLight,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isVpnEnabled) "Phím nổi luôn hiển thị để điều khiển nhanh"
                            else "Cần bật Cách ly ứng dụng ở trên để kích hoạt phím nổi",
                            color = if (isVpnEnabled) TextSecondary else TextMuted,
                            fontSize = 11.sp
                        )
                    }
                    FacetedSwitch(
                        checked = isOverlayEnabled && canDrawOverlays && isVpnEnabled,
                        onCheckedChange = { if (isVpnEnabled) onToggleMasterOverlay(it) },
                        enabled = isVpnEnabled,
                        checkedTrackColor = BrandPrimary,
                        uncheckedTrackColor = SlateSurfaceElevated,
                        checkedThumbColor = Color.White,
                        uncheckedThumbColor = TextMuted
                    )
                }

                if (isOverlayEnabled && !canDrawOverlays && isVpnEnabled) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Button(
                        onClick = onRequestOverlayPermission,
                        colors = ButtonDefaults.buttonColors(containerColor = DangerRed),
                        shape = FacetedBadgeShape,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "Cấp quyền Hiển thị trên ứng dụng khác",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 7.dp),
                    color = SlateBorder
                )

                val isHudConfigEnabled = isHudActive
                val hudConfigAlpha = if (isHudConfigEnabled) 1.0f else 0.42f

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .alpha(hudConfigAlpha)
                ) {
                    Text(
                        text = "Các nút trong phím nổi (chọn nút hiển thị):",
                        color = if (isHudConfigEnabled) TextSecondary else TextMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        BubbleConfigCheckbox(
                            label = "Cách ly",
                            checked = showIsolate,
                            enabled = isHudConfigEnabled,
                            onCheckedChange = onToggleIsolate
                        )
                        BubbleConfigCheckbox(
                            label = "Wi-Fi",
                            checked = showWifi,
                            enabled = isHudConfigEnabled,
                            onCheckedChange = onToggleWifi
                        )
                        BubbleConfigCheckbox(
                            label = "Dữ liệu",
                            checked = showData,
                            enabled = isHudConfigEnabled,
                            onCheckedChange = onToggleData
                        )
                        BubbleConfigCheckbox(
                            label = "Máy bay",
                            checked = showAirplane,
                            enabled = isHudConfigEnabled,
                            onCheckedChange = onToggleAirplane
                        )
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 7.dp),
                        color = SlateBorder
                    )

                    Text(
                        text = "Khi bấm nút Cách ly trên phím nổi:",
                        color = if (isHudConfigEnabled) TextPrimary else TextMuted,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (!isHudConfigEnabled) {
                            "Bật công tắc Phím nổi ở trên để tùy chỉnh các chế độ này"
                        } else if (isolationMode == PreferencesManager.ISOLATION_MODE_FOREGROUND) {
                            "Chỉ cách ly ứng dụng đang mở trên màn hình"
                        } else {
                            "Cách ly cho toàn bộ danh sách ứng dụng đã chọn"
                        },
                        color = if (isHudConfigEnabled) CyberCyanLight else TextMuted,
                        fontSize = 10.5.sp,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IsolationModeChip(
                            modifier = Modifier.weight(1f),
                            title = "Tất cả app đã chọn",
                            selected = isolationMode == PreferencesManager.ISOLATION_MODE_ALL,
                            enabled = isHudConfigEnabled,
                            onClick = { onSelectIsolationMode(PreferencesManager.ISOLATION_MODE_ALL) }
                        )
                        IsolationModeChip(
                            modifier = Modifier.weight(1f),
                            title = "Chỉ app đang mở",
                            selected = isolationMode == PreferencesManager.ISOLATION_MODE_FOREGROUND,
                            enabled = isHudConfigEnabled,
                            onClick = { onSelectIsolationMode(PreferencesManager.ISOLATION_MODE_FOREGROUND) }
                        )
                    }

                    if (isHudConfigEnabled && isolationMode == PreferencesManager.ISOLATION_MODE_FOREGROUND) {
                        Spacer(modifier = Modifier.height(6.dp))

                        // Nút cấu hình Ứng dụng ngoại lệ (Bong bóng chat, ứng dụng nổi - Cách B)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(FacetedBadgeShape)
                                .background(SlateSurfaceElevated)
                                .border(1.dp, SlateBorder, FacetedBadgeShape)
                                .clickable { onOpenExcludedAppsDialog() }
                                .padding(horizontal = 10.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Ứng dụng ngoại lệ khi mở đè (Lớp phủ)",
                                    color = TextPrimary,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Thêm ứng dụng có thể hiển thị đè lên ứng dụng bị cách ly (auto click, messenger,...)",
                                    color = TextSecondary,
                                    fontSize = 10.sp
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(FacetedBadgeShape)
                                    .background(BrandPrimary.copy(alpha = 0.25f))
                                    .border(1.dp, CyberCyan.copy(alpha = 0.5f), FacetedBadgeShape)
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = "$excludedAppsCount app",
                                    color = CyberCyanLight,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        if (!hasUsageStatsPermission && isVpnEnabled) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Button(
                                onClick = onRequestUsageStatsPermission,
                                colors = ButtonDefaults.buttonColors(containerColor = DangerRed),
                                shape = FacetedBadgeShape,
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    "⚠ Chưa cấp quyền Truy cập dữ liệu sử dụng • Nhấn để cấp",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 7.dp),
                        color = SlateBorder
                    )

                    // Row: Tự hủy cách ly khi bật lại mạng (được làm mờ khi Phím nổi chưa bật)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 8.dp)
                        ) {
                            Text(
                                text = "Tự hủy cách ly khi bật lại mạng",
                                color = if (isHudConfigEnabled) TextPrimary else TextMuted,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (!isHudConfigEnabled) {
                                    "Cần bật Phím nổi ở trên để sử dụng tính năng này"
                                } else {
                                    "Tự động khôi phục mạng cho ứng dụng khi bật lại Wi-Fi hoặc Dữ liệu di động"
                                },
                                color = if (isHudConfigEnabled) TextSecondary else TextMuted,
                                fontSize = 10.sp,
                                lineHeight = 13.sp
                            )
                        }
                        FacetedSwitch(
                            checked = isAutoUnisolate && isHudConfigEnabled,
                            onCheckedChange = { if (isHudConfigEnabled) onToggleAutoUnisolate(it) },
                            enabled = isHudConfigEnabled,
                            checkedTrackColor = BrandPrimary,
                            uncheckedTrackColor = SlateSurfaceElevated,
                            checkedThumbColor = Color.White,
                            uncheckedThumbColor = TextMuted
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun IsolationModeChip(
    modifier: Modifier = Modifier,
    title: String,
    selected: Boolean,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier
            .clip(FacetedBadgeShape)
            .background(if (selected) BrandPrimary.copy(alpha = 0.22f) else SlateSurfaceElevated)
            .border(
                width = 1.dp,
                color = if (selected) CyberCyan else SlateBorder,
                shape = FacetedBadgeShape
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = null,
            enabled = enabled,
            colors = RadioButtonDefaults.colors(
                selectedColor = CyberCyan,
                unselectedColor = TextMuted
            ),
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = title,
            color = if (selected) TextPrimary else TextSecondary,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun BubbleConfigCheckbox(
    label: String,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.clickable(enabled = enabled) { onCheckedChange(!checked) }
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = if (enabled) onCheckedChange else null,
            enabled = enabled,
            colors = CheckboxDefaults.colors(
                checkedColor = BrandPrimary,
                checkmarkColor = Color.White,
                uncheckedColor = TextMuted
            ),
            modifier = Modifier.size(28.dp)
        )
        Spacer(modifier = Modifier.width(2.dp))
        Text(text = label, color = if (enabled) TextPrimary else TextMuted, fontSize = 11.5.sp)
    }
}
