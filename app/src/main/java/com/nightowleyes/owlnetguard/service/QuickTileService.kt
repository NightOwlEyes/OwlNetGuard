package com.nightowleyes.owlnetguard.service

import android.app.PendingIntent
import android.content.Intent
import android.graphics.drawable.Icon
import android.net.VpnService
import android.os.Build
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
        if (VpnService.prepare(this) != null) {
            openMainActivity(requestVpn = true)
            return
        }

        PreferencesManager.toggleBubbleIsolation(this)
        refreshTile()
    }

    private fun refreshTile() {
        val tile = qsTile ?: return
        val isIsolated = PreferencesManager.isBubbleIsolated(this)
        val activeCount = PreferencesManager.getActivePackages(this).size

        tile.state = if (isIsolated) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.label = if (isIsolated) "Đang cô lập ($activeCount)" else "OwlNetGuard"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.subtitle = if (isIsolated) "VPN Nền đang bật" else "Sẵn sàng ($activeCount ứng dụng)"
        }
        tile.icon = Icon.createWithResource(
            this,
            if (isIsolated) R.drawable.ic_shield_isolated else R.drawable.ic_shield_normal
        )
        tile.updateTile()
    }

    @Suppress("DEPRECATION")
    private fun openMainActivity(requestVpn: Boolean = false) {
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            if (requestVpn) {
                putExtra(MainActivity.EXTRA_REQUEST_VPN_PERMISSION, true)
            }
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
            startActivityAndCollapse(intent)
        }
    }
}
