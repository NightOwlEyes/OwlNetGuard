package com.nightowleyes.owlnetguard.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.nightowleyes.owlnetguard.R

val JosefinSans = FontFamily(
    Font(R.font.josefin_sans_light, FontWeight.Light),
    Font(R.font.josefin_sans_regular, FontWeight.Normal),
    Font(R.font.josefin_sans_medium, FontWeight.Medium),
    Font(R.font.josefin_sans_semibold, FontWeight.SemiBold),
    Font(R.font.josefin_sans_bold, FontWeight.Bold)
)

private val defaultTypography = Typography()

val Typography = Typography(
    displayLarge = defaultTypography.displayLarge.copy(fontFamily = JosefinSans),
    displayMedium = defaultTypography.displayMedium.copy(fontFamily = JosefinSans),
    displaySmall = defaultTypography.displaySmall.copy(fontFamily = JosefinSans),
    headlineLarge = defaultTypography.headlineLarge.copy(fontFamily = JosefinSans),
    headlineMedium = defaultTypography.headlineMedium.copy(fontFamily = JosefinSans),
    headlineSmall = defaultTypography.headlineSmall.copy(fontFamily = JosefinSans),
    titleLarge = defaultTypography.titleLarge.copy(fontFamily = JosefinSans),
    titleMedium = defaultTypography.titleMedium.copy(fontFamily = JosefinSans),
    titleSmall = defaultTypography.titleSmall.copy(fontFamily = JosefinSans),
    bodyLarge = defaultTypography.bodyLarge.copy(fontFamily = JosefinSans),
    bodyMedium = defaultTypography.bodyMedium.copy(fontFamily = JosefinSans),
    bodySmall = defaultTypography.bodySmall.copy(fontFamily = JosefinSans),
    labelLarge = defaultTypography.labelLarge.copy(fontFamily = JosefinSans),
    labelMedium = defaultTypography.labelMedium.copy(fontFamily = JosefinSans),
    labelSmall = defaultTypography.labelSmall.copy(fontFamily = JosefinSans)
)
