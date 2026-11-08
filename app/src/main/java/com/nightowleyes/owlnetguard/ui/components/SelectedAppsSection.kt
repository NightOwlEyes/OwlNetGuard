package com.nightowleyes.owlnetguard.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nightowleyes.owlnetguard.R
import com.nightowleyes.owlnetguard.ui.theme.BrandPrimary
import com.nightowleyes.owlnetguard.ui.theme.CyberCyan
import com.nightowleyes.owlnetguard.ui.theme.FacetedBadgeShape
import com.nightowleyes.owlnetguard.ui.theme.FacetedTileShape
import com.nightowleyes.owlnetguard.ui.theme.TextMuted
import com.nightowleyes.owlnetguard.ui.theme.TextSecondary

@Composable
fun SelectedAppsHeaderBar(
    selectedCount: Int,
    activeCount: Int,
    onOpenAppPicker: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 8.dp)
        ) {
            Text(
                text = "◆ Ứng dụng đã chọn ($selectedCount)",
                color = CyberCyan,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.ExtraBold
            )
            if (selectedCount > 0) {
                Text(
                    text = "Đang kích hoạt: $activeCount • Vuốt ngang để xóa",
                    color = TextMuted,
                    fontSize = 10.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Button(
            onClick = onOpenAppPicker,
            colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
            shape = FacetedBadgeShape,
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
            modifier = Modifier.height(34.dp)
        ) {
            Text(
                text = "+ Chọn ứng dụng",
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        }
    }
}

@Composable
fun LoadingStateView() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator(color = CyberCyan, strokeWidth = 3.dp)
        Spacer(modifier = Modifier.height(12.dp))
        Text("Đang tải danh sách ứng dụng...", fontSize = 13.5.sp, color = TextSecondary)
    }
}

@Composable
fun EmptySelectedAppsView(
    onOpenAppPicker: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(28.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(id = R.drawable.logo_faceted_apex),
            contentDescription = null,
            modifier = Modifier.size(62.dp),
            contentScale = ContentScale.Fit
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Chưa có ứng dụng nào trong danh sách đã chọn.\nNhấn \"Chọn ứng dụng ngay\" để mở bảng tất cả ứng dụng và thêm vào danh sách!",
            fontSize = 13.sp,
            color = TextSecondary,
            textAlign = TextAlign.Center,
            lineHeight = 18.sp
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onOpenAppPicker,
            colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
            shape = FacetedTileShape
        ) {
            Text("◆ Chọn ứng dụng ngay ◆", fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
        }
    }
}
