package com.nightowleyes.owlnetguard.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxState
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nightowleyes.owlnetguard.R
import com.nightowleyes.owlnetguard.data.AppInfo
import com.nightowleyes.owlnetguard.ui.theme.BrandPrimary
import com.nightowleyes.owlnetguard.ui.theme.BrandPrimaryDeep
import com.nightowleyes.owlnetguard.ui.theme.BrandPrimaryLight
import com.nightowleyes.owlnetguard.ui.theme.CyberCyan
import com.nightowleyes.owlnetguard.ui.theme.CyberCyanLight
import com.nightowleyes.owlnetguard.ui.theme.DangerRed
import com.nightowleyes.owlnetguard.ui.theme.DangerRedMuted
import com.nightowleyes.owlnetguard.ui.theme.FacetedBadgeShape
import com.nightowleyes.owlnetguard.ui.theme.FacetedTileShape
import com.nightowleyes.owlnetguard.ui.theme.SlateBorder
import com.nightowleyes.owlnetguard.ui.theme.SlateSurface
import com.nightowleyes.owlnetguard.ui.theme.SlateSurfaceElevated
import com.nightowleyes.owlnetguard.ui.theme.TextMuted
import com.nightowleyes.owlnetguard.ui.theme.TextPrimary
import com.nightowleyes.owlnetguard.ui.utils.toImageBitmap

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeableSelectedAppItem(
    app: AppInfo,
    isVpnIsolated: Boolean,
    onToggleActive: () -> Unit,
    onRemoveFromList: () -> Unit
) {
    var itemWidthPx by remember { mutableFloatStateOf(0f) }
    var dismissStateRef: SwipeToDismissBoxState? = null

    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.StartToEnd ||
                value == SwipeToDismissBoxValue.EndToStart
            ) {
                val currentOffset = dismissStateRef?.let {
                    try {
                        kotlin.math.abs(it.requireOffset())
                    } catch (_: Exception) {
                        0f
                    }
                } ?: 0f
                // Bắt buộc hành trình vuốt phải thực sự vượt qua 60% chiều rộng thì mới xóa.
                // Nếu vuốt nhanh (fling) nhưng mới đi được < 60% thì chặn lại và đàn hồi về chỗ cũ.
                val minThreshold = if (itemWidthPx > 0f) itemWidthPx * 0.50f else Float.MAX_VALUE
                if (currentOffset >= minThreshold) {
                    onRemoveFromList()
                    true
                } else {
                    false
                }
            } else {
                false
            }
        },
        positionalThreshold = { totalDistance -> totalDistance * 0.50f }
    )
    dismissStateRef = dismissState

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = true,
        enableDismissFromEndToStart = true,
        modifier = Modifier.onSizeChanged { size ->
            if (size.width > 0) itemWidthPx = size.width.toFloat()
        },
        backgroundContent = {
            val isSwiping = dismissState.targetValue != SwipeToDismissBoxValue.Settled
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp, vertical = 3.5.dp)
                    .clip(FacetedTileShape)
                    .background(if (isSwiping) DangerRed.copy(alpha = 0.28f) else Color.Transparent)
                    .border(
                        1.dp,
                        if (isSwiping) DangerRed.copy(alpha = 0.5f) else Color.Transparent,
                        FacetedTileShape
                    )
                    .padding(horizontal = 18.dp),
                contentAlignment = if (dismissState.dismissDirection == SwipeToDismissBoxValue.StartToEnd) {
                    Alignment.CenterStart
                } else {
                    Alignment.CenterEnd
                }
            ) {
                if (isSwiping) {
                    Text(
                        text = "Bỏ khỏi danh sách",
                        color = DangerRed,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 12.sp
                    )
                }
            }
        }
    ) {
        SelectedAppRowCard(
            app = app,
            isVpnIsolated = isVpnIsolated,
            onToggleActive = onToggleActive
        )
    }
}

@Composable
fun SelectedAppRowCard(
    app: AppInfo,
    isVpnIsolated: Boolean,
    onToggleActive: () -> Unit
) {
    val isIsolated = app.isBlocked // true = ĐANG CÁCH LY; false = ĐANG TẠM DỪNG
    val iconBitmap = remember(app.packageName, app.icon) {
        app.icon?.toImageBitmap(width = 112, height = 112)
    }

    val cardBorderBrush = remember(isIsolated) {
        if (isIsolated) {
            Brush.horizontalGradient(listOf(DangerRed, BrandPrimaryDeep))
        } else {
            Brush.horizontalGradient(listOf(BrandPrimary.copy(alpha = 0.8f), CyberCyan.copy(alpha = 0.55f)))
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 3.5.dp)
            .clip(FacetedTileShape)
            .clickable { onToggleActive() },
        shape = FacetedTileShape,
        colors = CardDefaults.cardColors(
            containerColor = if (isIsolated) DangerRedMuted.copy(alpha = 0.45f) else SlateSurface
        ),
        border = CardDefaults.outlinedCardBorder().copy(brush = cardBorderBrush)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (iconBitmap != null) {
                Image(
                    bitmap = iconBitmap,
                    contentDescription = app.name,
                    colorFilter = null, // Giữ nguyên màu sắc ứng dụng, không làm xám
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(10.dp))
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(FacetedBadgeShape)
                        .background(if (isIsolated) DangerRedMuted else BrandPrimary.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = app.name.take(1).uppercase(),
                        fontWeight = FontWeight.Bold,
                        color = if (isIsolated) DangerRed else BrandPrimaryLight,
                        fontSize = 17.sp
                    )
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp, end = 6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = app.name,
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(FacetedBadgeShape)
                            .background(if (isIsolated) DangerRedMuted else BrandPrimary.copy(alpha = 0.2f))
                            .padding(horizontal = 6.dp, vertical = 1.5.dp)
                    ) {
                        Text(
                            text = if (isIsolated) "ĐANG CÁCH LY" else "ĐANG TẠM DỪNG",
                            color = if (isIsolated) DangerRed else CyberCyanLight,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
                Text(
                    text = app.packageName,
                    fontSize = 11.5.sp,
                    color = TextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Nút lăng kính lục giác: chuyển đổi giữa Đang cách ly và Đang tạm dừng
            IconButton(
                onClick = onToggleActive,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    painter = painterResource(
                        id = if (isIsolated) R.drawable.ic_bubble_active else R.drawable.ic_bubble_inactive
                    ),
                    contentDescription = if (isIsolated) "Chuyển sang Đang tạm dừng" else "Chuyển sang Đang cách ly",
                    tint = Color.Unspecified,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}
