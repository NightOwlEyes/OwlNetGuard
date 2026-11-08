package com.nightowleyes.owlnetguard.ui.components

import androidx.compose.foundation.Canvas as ComposeCanvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path

@Composable
fun FacetedAmbientBackground() {
    ComposeCanvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        val leftPrism = Path().apply {
            moveTo(0f, 0f)
            lineTo(w * 0.58f, 0f)
            lineTo(w * 0.32f, h * 0.18f)
            lineTo(0f, h * 0.12f)
            close()
        }
        drawPath(path = leftPrism, color = Color(0xFF161130))

        val rightPrism = Path().apply {
            moveTo(w * 0.58f, 0f)
            lineTo(w, 0f)
            lineTo(w, h * 0.22f)
            lineTo(w * 0.72f, h * 0.15f)
            lineTo(w * 0.32f, h * 0.18f)
            close()
        }
        drawPath(path = rightPrism, color = Color(0xFF121833))

        val centerFacet = Path().apply {
            moveTo(w * 0.32f, h * 0.18f)
            lineTo(w * 0.72f, h * 0.15f)
            lineTo(w * 0.52f, h * 0.28f)
            close()
        }
        drawPath(path = centerFacet, color = Color(0xFF191438).copy(alpha = 0.65f))
    }
}
