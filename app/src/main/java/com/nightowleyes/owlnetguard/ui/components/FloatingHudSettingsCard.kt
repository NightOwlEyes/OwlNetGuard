package com.nightowleyes.owlnetguard.ui.components

import androidx.compose.foundation.Canvas as ComposeCanvas
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nightowleyes.owlnetguard.R
import com.nightowleyes.owlnetguard.data.PreferencesManager
import com.nightowleyes.owlnetguard.ui.dialogs.AvailableUpdateInfo
import com.nightowleyes.owlnetguard.ui.theme.Alata
import com.nightowleyes.owlnetguard.ui.theme.BrandPrimary
import com.nightowleyes.owlnetguard.ui.theme.BrandPrimaryLight
import com.nightowleyes.owlnetguard.ui.theme.CyberCyan
import com.nightowleyes.owlnetguard.ui.theme.CyberCyanLight
import com.nightowleyes.owlnetguard.ui.theme.DangerRed
import com.nightowleyes.owlnetguard.ui.theme.DangerRedMuted
import com.nightowleyes.owlnetguard.ui.theme.FacetedBadgeShape
import com.nightowleyes.owlnetguard.ui.theme.FacetedCardShape
import com.nightowleyes.owlnetguard.ui.theme.FacetedTileShape
import com.nightowleyes.owlnetguard.ui.theme.JosefinSans
import com.nightowleyes.owlnetguard.ui.theme.SlateBorder
import com.nightowleyes.owlnetguard.ui.theme.SlateSurface
import com.nightowleyes.owlnetguard.ui.theme.SlateSurfaceElevated
import com.nightowleyes.owlnetguard.ui.theme.TextMuted
import com.nightowleyes.owlnetguard.ui.theme.TextPrimary
import com.nightowleyes.owlnetguard.ui.theme.TextSecondary

/**
 * Banner thông báo bản cập nhật mới (xuất hiện tự động ở đầu app khi có phiên bản mới trên GitHub).
 */
@Composable
fun UpdateAvailableBanner(
    updateInfo: AvailableUpdateInfo,
    onOpenUpdate: () -> Unit,
    onDismiss: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 4.dp)
            .clip(FacetedTileShape)
            .background(CyberCyan.copy(alpha = 0.14f))
            .border(
                width = 1.dp,
                brush = Brush.horizontalGradient(listOf(CyberCyan, BrandPrimary)),
                shape = FacetedTileShape
            )
            .clickable { onOpenUpdate() }
            .padding(horizontal = 11.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "✦ Cập nhật mới v${updateInfo.version}",
                color = CyberCyanLight,
                fontFamily = JosefinSans,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "Nhấn để xem và tải về",
                color = TextSecondary,
                fontSize = 10.5.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(modifier = Modifier.width(6.dp))
        Box(
            modifier = Modifier
                .clip(FacetedBadgeShape)
                .background(CyberCyan.copy(alpha = 0.22f))
                .border(1.dp, CyberCyan.copy(alpha = 0.65f), FacetedBadgeShape)
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                text = "Cập nhật",
                color = CyberCyanLight,
                fontSize = 10.5.sp
            )
        }
        Spacer(modifier = Modifier.width(6.dp))
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .clickable { onDismiss() },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "✕",
                color = TextMuted,
                style = TextStyle(
                    fontFamily = Alata,
                    fontSize = 12.sp,
                    lineHeight = 12.sp,
                    platformStyle = PlatformTextStyle(includeFontPadding = false)
                )
            )
        }
    }
}

/**
 * Thẻ đề xuất thêm nút bật/tắt nhanh lên thanh Cài đặt nhanh (có thể xóa/đóng đi, xuất hiện ở đầu ứng dụng).
 */
@Composable
fun QuickTileSuggestionBanner(
    onAddTile: () -> Unit,
    onDismiss: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 4.dp)
            .clip(FacetedTileShape)
            .background(SlateSurfaceElevated)
            .border(
                width = 1.dp,
                brush = Brush.horizontalGradient(
                    listOf(BrandPrimary.copy(alpha = 0.7f), CyberCyan.copy(alpha = 0.45f))
                ),
                shape = FacetedTileShape
            )
            .clickable { onAddTile() }
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(FacetedBadgeShape)
                .background(BrandPrimary.copy(alpha = 0.25f))
                .border(1.dp, CyberCyan.copy(alpha = 0.4f), FacetedBadgeShape),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_qs_tile_owl),
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                contentScale = ContentScale.Fit
            )
        }
        Spacer(modifier = Modifier.width(9.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Ô Cài đặt nhanh",
                color = TextPrimary,
                fontFamily = JosefinSans,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "Bật/tắt nhanh từ thanh thông báo",
                color = TextSecondary,
                fontSize = 10.5.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(modifier = Modifier.width(6.dp))
        Box(
            modifier = Modifier
                .clip(FacetedBadgeShape)
                .background(BrandPrimary.copy(alpha = 0.28f))
                .border(1.dp, CyberCyan.copy(alpha = 0.55f), FacetedBadgeShape)
                .clickable { onAddTile() }
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                text = "+ Thêm",
                color = CyberCyanLight,
                fontSize = 10.5.sp
            )
        }
        Spacer(modifier = Modifier.width(4.dp))
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .clickable { onDismiss() },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "✕",
                color = TextMuted,
                style = TextStyle(
                    fontFamily = Alata,
                    fontSize = 12.sp,
                    lineHeight = 12.sp,
                    platformStyle = PlatformTextStyle(includeFontPadding = false)
                )
            )
        }
    }
}

@Composable
fun FloatingHudSettingsCard(
    isPopupActive: Boolean,
    canDrawOverlays: Boolean,
    isVpnGranted: Boolean,
    activeAppsCount: Int,
    totalSelectedCount: Int,
    showIsolate: Boolean,
    showDisrupt: Boolean,
    isolationMode: Int,
    hasUsageStatsPermission: Boolean,
    isAutoUnisolate: Boolean,
    excludedAppsCount: Int = 0,
    onToggleMasterPopup: (Boolean) -> Unit,
    onToggleIsolate: (Boolean) -> Unit,
    onToggleDisrupt: (Boolean) -> Unit,
    onShowDisruptInfo: () -> Unit = {},
    onToggleAutoUnisolate: (Boolean) -> Unit,
    onSelectIsolationMode: (Int) -> Unit,
    onOpenExcludedAppsDialog: () -> Unit = {},
    onRequestOverlayPermission: () -> Unit,
    onRequestUsageStatsPermission: () -> Unit
) {
    val isActivelyIsolating = isPopupActive && activeAppsCount > 0 &&
        isolationMode == PreferencesManager.ISOLATION_MODE_ALL

    val isScarletState = isPopupActive && isActivelyIsolating

    val borderBrush = remember(isPopupActive, isScarletState) {
        when {
            isScarletState -> Brush.linearGradient(
                listOf(Color.White.copy(alpha = 0.7f), DangerRed, BrandPrimary)
            )
            isPopupActive -> Brush.linearGradient(
                listOf(Color.White.copy(alpha = 0.7f), BrandPrimary, CyberCyan.copy(alpha = 0.7f))
            )
            else -> SolidColor(SlateBorder)
        }
    }

    val containerColor = if (isScarletState) {
        DangerRedMuted.copy(alpha = 0.32f)
    } else {
        SlateSurface
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 5.dp),
        shape = FacetedCardShape,
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = CardDefaults.outlinedCardBorder().copy(width = 1.2.dp, brush = borderBrush)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            ComposeCanvas(modifier = Modifier.matchParentSize()) {
                val w = size.width
                val h = size.height
                val accent = if (isScarletState) DangerRed else BrandPrimary
                val p1 = Path().apply {
                    moveTo(w * 0.68f, 0f)
                    lineTo(w, 0f)
                    lineTo(w, h * 0.55f)
                    lineTo(w * 0.82f, h * 0.34f)
                    close()
                }
                drawPath(p1, accent.copy(alpha = if (isPopupActive) 0.12f else 0.06f))

                drawLine(
                    brush = Brush.horizontalGradient(
                        listOf(Color.White.copy(alpha = 0.5f), accent.copy(alpha = 0.25f), Color.Transparent)
                    ),
                    start = Offset(0f, 1.5f),
                    end = Offset(w * 0.6f, 1.5f),
                    strokeWidth = 3f
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 12.dp)
            ) {
                // Header row: Không lặp logo cạnh chữ "Phím nổi (HUD)"
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Phím nổi (HUD)",
                            color = TextPrimary,
                            fontFamily = JosefinSans,
                            fontSize = 15.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                        val statusText = when {
                            !isPopupActive -> "Chưa kích hoạt"
                            isScarletState -> "VPN bật • Đang cách ly $activeAppsCount/$totalSelectedCount ứng dụng"
                            else -> "VPN bật • Đang tạm dừng"
                        }
                        val statusColor = when {
                            !isPopupActive -> TextMuted
                            isScarletState -> DangerRed
                            else -> CyberCyanLight
                        }
                        Text(
                            text = statusText,
                            color = statusColor,
                            fontSize = 11.5.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    FacetedSwitch(
                        checked = isPopupActive,
                        onCheckedChange = onToggleMasterPopup,
                        enabled = true,
                        checkedTrackColor = if (isScarletState) DangerRed else BrandPrimary,
                        uncheckedTrackColor = SlateSurfaceElevated,
                        checkedThumbColor = Color.White,
                        uncheckedThumbColor = TextMuted
                    )
                }

                if (!canDrawOverlays && isVpnGranted && isPopupActive) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = onRequestOverlayPermission,
                        colors = ButtonDefaults.buttonColors(containerColor = DangerRed),
                        shape = FacetedBadgeShape,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "Cấp quyền Hiển thị trên ứng dụng khác",
                            fontSize = 11.5.sp
                        )
                    }
                }

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = SlateBorder
                )

                // Section 1: Nút hiển thị (gọn gàng, dấu ? căn giữa hoàn hảo)
                Text(
                    text = "Nút hiển thị",
                    color = TextPrimary,
                    fontFamily = JosefinSans,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    BubbleConfigCheckbox(
                        label = "Cách ly",
                        checked = showIsolate,
                        enabled = true,
                        onCheckedChange = onToggleIsolate
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        BubbleConfigCheckbox(
                            label = "Làm gián đoạn mạng",
                            checked = showDisrupt,
                            enabled = true,
                            onCheckedChange = onToggleDisrupt
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .clip(CircleShape)
                                .background(SlateSurfaceElevated)
                                .border(1.dp, CyberCyan.copy(alpha = 0.6f), CircleShape)
                                .clickable { onShowDisruptInfo() },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "?",
                                color = CyberCyanLight,
                                textAlign = TextAlign.Center,
                                style = TextStyle(
                                    fontFamily = Alata,
                                    fontSize = 11.sp,
                                    lineHeight = 11.sp,
                                    platformStyle = PlatformTextStyle(includeFontPadding = false)
                                )
                            )
                        }
                    }
                }

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = SlateBorder
                )

                // Section 2: Phạm vi cách ly (tinh gọn bỏ dòng chú thích thừa)
                Text(
                    text = "Phạm vi cách ly",
                    color = TextPrimary,
                    fontFamily = JosefinSans,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IsolationModeChip(
                        modifier = Modifier.weight(1f),
                        title = "Tất cả app đã chọn",
                        selected = isolationMode == PreferencesManager.ISOLATION_MODE_ALL,
                        enabled = true,
                        onClick = { onSelectIsolationMode(PreferencesManager.ISOLATION_MODE_ALL) }
                    )
                    IsolationModeChip(
                        modifier = Modifier.weight(1f),
                        title = "Chỉ app đang mở",
                        selected = isolationMode == PreferencesManager.ISOLATION_MODE_FOREGROUND,
                        enabled = true,
                        onClick = { onSelectIsolationMode(PreferencesManager.ISOLATION_MODE_FOREGROUND) }
                    )
                }

                if (isolationMode == PreferencesManager.ISOLATION_MODE_FOREGROUND) {
                    Spacer(modifier = Modifier.height(6.dp))

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
                        Text(
                            text = "Ứng dụng ngoại lệ (Lớp phủ)",
                            color = TextPrimary,
                            fontSize = 11.5.sp,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(FacetedBadgeShape)
                                .background(BrandPrimary.copy(alpha = 0.25f))
                                .border(1.dp, CyberCyan.copy(alpha = 0.5f), FacetedBadgeShape)
                                .padding(horizontal = 8.dp, vertical = 2.5.dp)
                        ) {
                            Text(
                                text = "$excludedAppsCount app",
                                color = CyberCyanLight,
                                fontSize = 10.5.sp
                            )
                        }
                    }

                    if (!hasUsageStatsPermission) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Button(
                            onClick = onRequestUsageStatsPermission,
                            colors = ButtonDefaults.buttonColors(containerColor = DangerRed),
                            shape = FacetedBadgeShape,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                "⚠ Cấp quyền Truy cập dữ liệu sử dụng",
                                fontSize = 11.5.sp
                            )
                        }
                    }
                }

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = SlateBorder
                )

                // Section 3: Tự hủy cách ly (ngắn gọn, rõ ý)
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
                            text = "Tự hủy cách ly",
                            color = TextPrimary,
                            fontFamily = JosefinSans,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Khi bật lại mạng hoặc làm gián đoạn",
                            color = TextSecondary,
                            fontSize = 10.5.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    FacetedSwitch(
                        checked = isAutoUnisolate,
                        onCheckedChange = onToggleAutoUnisolate,
                        enabled = true,
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
            fontSize = 11.5.sp,
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
        modifier = Modifier
            .alpha(if (enabled) 1f else 0.5f)
            .clickable(enabled = enabled) { onCheckedChange(!checked) }
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
            modifier = Modifier.size(26.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            color = if (enabled) TextPrimary else TextMuted,
            style = TextStyle(
                fontFamily = Alata,
                fontSize = 12.sp,
                platformStyle = PlatformTextStyle(includeFontPadding = false)
            )
        )
    }
}
