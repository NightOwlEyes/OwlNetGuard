package com.nightowleyes.owlnetguard.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.nightowleyes.owlnetguard.data.PreferencesManager

class NotificationActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return
        when (intent.action) {
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
            }
        }
    }

    companion object {
        const val ACTION_TOGGLE_ISOLATE = "com.nightowleyes.owlnetguard.ACTION_TOGGLE_ISOLATE"
        const val ACTION_TOGGLE_BUBBLE_VISIBILITY =
            "com.nightowleyes.owlnetguard.ACTION_TOGGLE_BUBBLE_VISIBILITY"
    }
}
