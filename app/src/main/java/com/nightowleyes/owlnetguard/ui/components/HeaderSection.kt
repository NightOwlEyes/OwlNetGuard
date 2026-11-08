package com.nightowleyes.owlnetguard.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nightowleyes.owlnetguard.R
import com.nightowleyes.owlnetguard.ui.MainActivity
import com.nightowleyes.owlnetguard.ui.dialogs.InfoDialogType
import com.nightowleyes.owlnetguard.ui.theme.BrandPrimary
import com.nightowleyes.owlnetguard.ui.theme.BrandPrimaryLight
import com.nightowleyes.owlnetguard.ui.theme.CyberCyan
import com.nightowleyes.owlnetguard.ui.theme.FacetedBadgeShape
import com.nightowleyes.owlnetguard.ui.theme.SlateBorder
import com.nightowleyes.owlnetguard.ui.theme.SlateSurfaceElevated
import com.nightowleyes.owlnetguard.ui.theme.TextMuted
import com.nightowleyes.owlnetguard.ui.theme.TextPrimary

@Composable
fun HeaderSection(
    isIsolated: Boolean,
    showInfoMenu: Boolean,
    onToggleInfoMenu: () -> Unit,
    onDismissInfoMenu: () -> Unit,
    onSelectDialog: (InfoDialogType) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = painterResource(
                    id = if (isIsolated) R.drawable.logo_faceted_apex_isolated else R.drawable.logo_faceted_apex
                ),
                contentDescription = "OwlNetGuard Logo",
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(44.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "OWL ",
                    color = TextPrimary,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.8.sp
                )
                Text(
                    text = "NETGUARD",
                    color = BrandPrimaryLight,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.8.sp
                )
            }
        }

        Box {
            IconButton(
                onClick = onToggleInfoMenu,
                modifier = Modifier
                    .size(38.dp)
                    .clip(FacetedBadgeShape)
                    .background(if (showInfoMenu) BrandPrimary.copy(alpha = 0.25f) else SlateSurfaceElevated)
                    .border(
                        width = 1.dp,
                        color = if (showInfoMenu) CyberCyan else SlateBorder,
                        shape = FacetedBadgeShape
                    )
            ) {
                Text(
                    text = "⋮",
                    color = if (showInfoMenu) CyberCyan else BrandPrimaryLight,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            DropdownMenu(
                expanded = showInfoMenu,
                onDismissRequest = onDismissInfoMenu,
                modifier = Modifier
                    .background(SlateSurfaceElevated)
                    .border(1.dp, SlateBorder, RoundedCornerShape(8.dp))
            ) {
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(
                                "OwlNetGuard",
                                color = BrandPrimaryLight,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Phiên bản v${MainActivity.CURRENT_APP_VERSION}",
                                color = TextMuted,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    },
                    onClick = { onSelectDialog(InfoDialogType.CHECK_UPDATE) }
                )
                HorizontalDivider(color = SlateBorder)
                DropdownMenuItem(
                    text = {
                        Text(
                            "Chính sách bảo mật",
                            color = TextPrimary,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Medium
                        )
                    },
                    onClick = { onSelectDialog(InfoDialogType.PRIVACY_POLICY) }
                )
                DropdownMenuItem(
                    text = {
                        Text(
                            "Giấy phép mã nguồn mở",
                            color = TextPrimary,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Medium
                        )
                    },
                    onClick = { onSelectDialog(InfoDialogType.OPEN_SOURCE_LICENSES) }
                )
                HorizontalDivider(color = SlateBorder)
                DropdownMenuItem(
                    text = {
                        Text(
                            "Kiểm tra bản cập nhật",
                            color = CyberCyan,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    onClick = { onSelectDialog(InfoDialogType.CHECK_UPDATE) }
                )
            }
        }
    }
}
