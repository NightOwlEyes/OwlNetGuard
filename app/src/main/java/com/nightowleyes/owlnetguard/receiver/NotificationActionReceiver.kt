package com.nightowleyes.owlnetguard.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.nightowleyes.owlnetguard.data.PreferencesManager
import com.nightowleyes.owlnetguard.service.LocalVpnService

class NotificationActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return
        when (intent.action) {
            ACTION_STOP_SERVICE -> {
                PreferencesManager.setBubbleOverlayEnabled(context, false)
                PreferencesManager.setVpnEnabled(context, false)
                Toast.makeText(context, "Đã dừng dịch vụ", Toast.LENGTH_SHORT).show()
            }
            ACTION_TOGGLE_ISOLATE -> {
                val isolated = PreferencesManager.toggleBubbleIsolation(context)
                val msg = if (isolated) "Đã bật cô lập mạng (VPN nền)" else "Đã tắt cô lập mạng"
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            }
            ACTION_TOGGLE_BUBBLE_VISIBILITY -> {
                val newVisible = !PreferencesManager.isBubbleOverlayEnabled(context)
                PreferencesManager.setBubbleOverlayEnabled(context, newVisible)
                val msg = if (newVisible) "Đã bật phím nổi" else "Đã tắt phím nổi"
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                LocalVpnService.updateNotification(context)
            }
        }
    }

    companion object {
        const val ACTION_STOP_SERVICE = "com.nightowleyes.owlnetguard.ACTION_STOP_SERVICE"
        const val ACTION_TOGGLE_ISOLATE = "com.nightowleyes.owlnetguard.ACTION_TOGGLE_ISOLATE"
        const val ACTION_TOGGLE_BUBBLE_VISIBILITY =
            "com.nightowleyes.owlnetguard.ACTION_TOGGLE_BUBBLE_VISIBILITY"
    }
}
