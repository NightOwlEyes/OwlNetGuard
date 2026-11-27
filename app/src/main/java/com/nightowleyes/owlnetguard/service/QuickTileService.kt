package com.nightowleyes.owlnetguard.service

import android.app.PendingIntent
import android.content.Intent
import android.graphics.drawable.Icon
import android.net.VpnService
import android.os.Build
import android.provider.Settings
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.nightowleyes.owlnetguard.R
import com.nightowleyes.owlnetguard.data.PreferencesManager
import com.nightowleyes.owlnetguard.ui.MainActivity

class QuickTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        refreshTile()
    }

    override fun onClick() {
        super.onClick()
        val isCurrentlyOn = PreferencesManager.isPopupEnabled(this)

        if (isCurrentlyOn) {
            // Popup tắt tương đương VPN tắt
            PreferencesManager.setPopupEnabled(this, false)
            refreshTile()
            return
        }

        // Muốn bật Popup + VPN: kiểm tra đủ 2 quyền VPN & Hiển thị trên ứng dụng khác
        val vpnGranted = VpnService.prepare(this) == null
        val overlayGranted = Settings.canDrawOverlays(this)

        if (!vpnGranted || !overlayGranted) {
            openMainActivityToGrantPermissions()
            return
        }

        // Đủ quyền: bật ngay Popup + VPN mà không cần mở ứng dụng
        PreferencesManager.setPopupEnabled(this, true)
        refreshTile()
    }

    private fun refreshTile() {
        val tile = qsTile ?: return
        val isPopupOn = PreferencesManager.isPopupEnabled(this) &&
            VpnService.prepare(this) == null &&
            Settings.canDrawOverlays(this)
        val activeCount = PreferencesManager.getEffectiveBlockedPackages(this)
            .filter { it != packageName }.size

        tile.state = if (isPopupOn) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.label = "OwlNetGuard"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.subtitle = when {
                !isPopupOn -> "Đã tắt"
                activeCount > 0 -> "Đang cách ly ($activeCount)"
                else -> "Phím nổi đang bật"
            }
        }
        tile.icon = Icon.createWithResource(this, R.drawable.ic_qs_tile_owl)
        tile.updateTile()
    }

    private fun openMainActivityToGrantPermissions() {
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP
            )
            putExtra(MainActivity.EXTRA_ENABLE_POPUP_FROM_TILE, true)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val pendingIntent = PendingIntent.getActivity(
                this,
                300,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            startActivityAndCollapse(pendingIntent)
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(intent)
        }
    }
}
