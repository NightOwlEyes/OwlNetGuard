package com.nightowleyes.owlnetguard.data

import android.graphics.drawable.Drawable

data class AppInfo(
    val name: String,
    val packageName: String,
    val uid: Int,
    val icon: Drawable?,
    val isBlocked: Boolean = false,
    val isInBubble: Boolean = false,
    val isSystemApp: Boolean = false
)
