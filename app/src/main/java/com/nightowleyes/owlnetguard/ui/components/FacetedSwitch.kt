package com.nightowleyes.owlnetguard.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.nightowleyes.owlnetguard.ui.theme.BrandPrimary
import com.nightowleyes.owlnetguard.ui.theme.FacetedSwitchThumbShape
import com.nightowleyes.owlnetguard.ui.theme.FacetedSwitchTrackShape
import com.nightowleyes.owlnetguard.ui.theme.SlateBorder
import com.nightowleyes.owlnetguard.ui.theme.SlateSurfaceElevated
import com.nightowleyes.owlnetguard.ui.theme.TextMuted

/**
 * Custom faceted/angular toggle switch aligning with the Faceted Apex design system.
 * Replaces standard rounded pill switches with sharp chamfered cut-corner geometry.
 */
@Composable
fun FacetedSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    checkedTrackColor: Color = BrandPrimary,
    uncheckedTrackColor: Color = SlateSurfaceElevated,
    checkedThumbColor: Color = Color.White,
    uncheckedThumbColor: Color = TextMuted,
    trackBorderColor: Color = SlateBorder
) {
    val trackWidth = 48.dp
    val trackHeight = 26.dp
    val thumbSize = 20.dp
    val padding = 3.dp
    val maxOffset = trackWidth - thumbSize - (padding * 2) // 22.dp

    val thumbOffset by animateDpAsState(
        targetValue = if (checked) maxOffset else 0.dp,
        animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
        label = "facetedSwitchThumbOffset"
    )

    val currentTrackColor by animateColorAsState(
        targetValue = if (checked) checkedTrackColor else uncheckedTrackColor,
        animationSpec = tween(durationMillis = 200),
        label = "facetedSwitchTrackColor"
    )

    val currentThumbColor by animateColorAsState(
        targetValue = if (checked) checkedThumbColor else uncheckedThumbColor,
        animationSpec = tween(durationMillis = 200),
        label = "facetedSwitchThumbColor"
    )

    val currentBorderColor by animateColorAsState(
        targetValue = if (checked) checkedTrackColor.copy(alpha = 0.7f) else trackBorderColor,
        animationSpec = tween(durationMillis = 200),
        label = "facetedSwitchBorderColor"
    )

    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .size(width = trackWidth, height = trackHeight)
            .alpha(if (enabled) 1.0f else 0.42f)
            .clip(FacetedSwitchTrackShape)
            .background(currentTrackColor)
            .border(1.dp, currentBorderColor, FacetedSwitchTrackShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled && onCheckedChange != null,
                onClick = { onCheckedChange?.invoke(!checked) }
            )
            .semantics { role = Role.Switch }
            .padding(padding),
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .offset(x = thumbOffset)
                .size(thumbSize)
                .clip(FacetedSwitchThumbShape)
                .background(currentThumbColor)
        )
    }
}
