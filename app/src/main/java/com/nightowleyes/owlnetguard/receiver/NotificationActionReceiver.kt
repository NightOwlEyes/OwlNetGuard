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
            ACTION_STOP_SERVICE, ACTION_TOGGLE_BUBBLE_VISIBILITY -> {
                PreferencesManager.setPopupEnabled(context, false)
                Toast.makeText(context, "Đã tắt phím nổi và dừng VPN nền", Toast.LENGTH_SHORT).show()
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
