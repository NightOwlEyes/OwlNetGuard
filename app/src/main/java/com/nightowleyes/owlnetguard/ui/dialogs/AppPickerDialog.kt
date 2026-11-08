package com.nightowleyes.owlnetguard.ui.dialogs

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.nightowleyes.owlnetguard.R
import com.nightowleyes.owlnetguard.data.AppInfo
import com.nightowleyes.owlnetguard.ui.theme.BrandPrimary
import com.nightowleyes.owlnetguard.ui.theme.BrandPrimaryLight
import com.nightowleyes.owlnetguard.ui.theme.CyberCyan
import com.nightowleyes.owlnetguard.ui.theme.CyberCyanLight
import com.nightowleyes.owlnetguard.ui.theme.FacetedBadgeShape
import com.nightowleyes.owlnetguard.ui.theme.FacetedCardShape
import com.nightowleyes.owlnetguard.ui.theme.FacetedTileShape
import com.nightowleyes.owlnetguard.ui.theme.SlateBackground
import com.nightowleyes.owlnetguard.ui.theme.SlateBorder
import com.nightowleyes.owlnetguard.ui.theme.SlateSurface
import com.nightowleyes.owlnetguard.ui.theme.SlateSurfaceHighlight
import com.nightowleyes.owlnetguard.ui.theme.TextMuted
import com.nightowleyes.owlnetguard.ui.theme.TextPrimary
import com.nightowleyes.owlnetguard.ui.theme.TextSecondary
import com.nightowleyes.owlnetguard.ui.utils.toImageBitmap

@Composable
fun AppPickerDialog(
    allApps: List<AppInfo>,
    selectedPackages: Set<String>,
    onToggleSelectApp: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var showSystemApps by remember { mutableStateOf(false) }

    val filteredApps = remember(allApps, selectedPackages, searchQuery, showSystemApps) {
        val base = if (showSystemApps) {
            allApps
        } else {
            allApps.filter { !it.isSystemApp || selectedPackages.contains(it.packageName) }
        }
        val searched = if (searchQuery.isBlank()) {
            base
        } else {
            val q = searchQuery.trim().lowercase()
            base.filter {
                it.name.lowercase().contains(q) || it.packageName.lowercase().contains(q)
            }
        }
        searched.sortedWith(
            compareByDescending<AppInfo> { selectedPackages.contains(it.packageName) }
                .thenBy { it.isSystemApp }
                .thenBy(String.CASE_INSENSITIVE_ORDER) { it.name }
        )
    }

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(if (isLandscape) 0.72f else 0.94f)
                .fillMaxHeight(if (isLandscape) 0.92f else 0.86f),
            shape = FacetedCardShape,
            colors = CardDefaults.cardColors(containerColor = SlateBackground),
            border = CardDefaults.outlinedCardBorder().copy(
                width = 1.3.dp,
                brush = Brush.linearGradient(listOf(BrandPrimary, CyberCyan))
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "◆ Chọn ứng dụng",
                            color = CyberCyanLight,
                            fontSize = 16.5.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "Đã chọn ${selectedPackages.size} ứng dụng vào danh sách",
                            color = TextSecondary,
                            fontSize = 11.5.sp
                        )
                    }
                    TextButton(onClick = onDismiss) {
                        Text(
                            text = "Xong",
                            color = CyberCyan,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Search & System Apps Filter inside Dialog
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.weight(1f),
                        placeholder = {
                            Text("Tìm ứng dụng...", fontSize = 12.sp, color = TextMuted)
                        },
                        singleLine = true,
                        shape = FacetedTileShape,
                        leadingIcon = {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_search),
                                contentDescription = null,
                                tint = CyberCyan,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                TextButton(onClick = { searchQuery = "" }) {
                                    Text("Xóa", fontSize = 11.sp, color = CyberCyan)
                                }
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = SlateSurface,
                            unfocusedContainerColor = SlateSurface,
                            focusedBorderColor = CyberCyan,
                            unfocusedBorderColor = SlateBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )
                    FilterChip(
                        selected = showSystemApps,
                        onClick = { showSystemApps = !showSystemApps },
                        label = { Text("Hệ thống", fontSize = 11.sp) },
                        shape = FacetedBadgeShape,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = BrandPrimary.copy(alpha = 0.25f),
                            selectedLabelColor = BrandPrimaryLight,
                            containerColor = SlateSurface,
                            labelColor = TextMuted
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            borderColor = if (showSystemApps) BrandPrimary else SlateBorder,
                            enabled = true,
                            selected = showSystemApps
                        )
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = SlateBorder)

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(vertical = 6.dp)
                ) {
                    items(
                        items = filteredApps,
                        key = { it.packageName }
                    ) { app ->
                        val isSelected = selectedPackages.contains(app.packageName)
                        val iconBitmap = remember(app.packageName, app.icon) {
                            app.icon?.toImageBitmap(width = 96, height = 96)
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(FacetedTileShape)
                                .clickable { onToggleSelectApp(app.packageName) }
                                .padding(horizontal = 8.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (iconBitmap != null) {
                                Image(
                                    bitmap = iconBitmap,
                                    contentDescription = app.name,
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(FacetedBadgeShape)
                                        .background(BrandPrimary.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = app.name.take(1).uppercase(),
                                        fontWeight = FontWeight.Bold,
                                        color = BrandPrimaryLight,
                                        fontSize = 15.sp
                                    )
                                }
                            }

                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 10.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = app.name,
                                        color = TextPrimary,
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                    if (app.isSystemApp) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Box(
                                            modifier = Modifier
                                                .clip(FacetedBadgeShape)
                                                .background(SlateSurfaceHighlight)
                                                .padding(horizontal = 4.dp, vertical = 1.dp)
                                        ) {
                                            Text(
                                                text = "HỆ THỐNG",
                                                color = BrandPrimaryLight,
                                                fontSize = 8.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                                Text(
                                    text = app.packageName,
                                    color = TextMuted,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Checkbox(
                                checked = isSelected,
                                onCheckedChange = { onToggleSelectApp(app.packageName) },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = BrandPrimary,
                                    checkmarkColor = Color.White,
                                    uncheckedColor = TextMuted
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}
