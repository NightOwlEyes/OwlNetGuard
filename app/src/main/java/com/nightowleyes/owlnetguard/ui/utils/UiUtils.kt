package com.nightowleyes.owlnetguard.ui.utils

import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.createBitmap

fun Drawable.toImageBitmap(width: Int = 112, height: Int = 112): ImageBitmap? {
    return try {
        if (this is BitmapDrawable && bitmap != null) {
            return bitmap.asImageBitmap()
        }
        val w = intrinsicWidth.takeIf { it > 0 } ?: width
        val h = intrinsicHeight.takeIf { it > 0 } ?: height
        val bmp = createBitmap(w, h)
        val canvas = Canvas(bmp)
        setBounds(0, 0, canvas.width, canvas.height)
        draw(canvas)
        bmp.asImageBitmap()
    } catch (_: Exception) {
        null
    }
}
