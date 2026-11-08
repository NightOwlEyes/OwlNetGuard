package com.nightowleyes.owlnetguard.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.nightowleyes.owlnetguard.data.PreferencesManager

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null) return
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED) {
            PreferencesManager.syncAllServices(context)
        }
    }
}
