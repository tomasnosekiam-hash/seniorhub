package com.seniorhub.os.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Typografie pro kiosk / senior tablet: větší řez, čitelnost na dálku, stále v souladu s M3 stupnicí.
 */
val SeniorHubTypography = Typography().let { base ->
    base.copy(
            displayLarge = TextStyle(
                fontWeight = FontWeight.Medium,
                fontSize = 72.sp,
                lineHeight = 80.sp,
                letterSpacing = (-0.5).sp,
            ),
            displayMedium = base.displayMedium.copy(
                fontWeight = FontWeight.Medium,
                fontSize = 52.sp,
                lineHeight = 58.sp,
            ),
            headlineLarge = base.headlineLarge.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 30.sp,
                lineHeight = 36.sp,
            ),
            headlineMedium = base.headlineMedium.copy(
                fontWeight = FontWeight.Medium,
                fontSize = 26.sp,
                lineHeight = 32.sp,
            ),
            headlineSmall = base.headlineSmall.copy(
                fontWeight = FontWeight.Medium,
                fontSize = 22.sp,
                lineHeight = 28.sp,
            ),
            titleLarge = base.titleLarge.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 22.sp,
                lineHeight = 28.sp,
            ),
            titleMedium = base.titleMedium.copy(
                fontWeight = FontWeight.Medium,
                fontSize = 18.sp,
                lineHeight = 24.sp,
            ),
            bodyLarge = base.bodyLarge.copy(
                fontSize = 18.sp,
                lineHeight = 26.sp,
            ),
            bodyMedium = base.bodyMedium.copy(
                fontSize = 16.sp,
                lineHeight = 22.sp,
            ),
            bodySmall = base.bodySmall.copy(
                fontSize = 14.sp,
                lineHeight = 20.sp,
            ),
            labelLarge = base.labelLarge.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                lineHeight = 18.sp,
                letterSpacing = 0.1.sp,
            ),
            labelMedium = base.labelMedium.copy(
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp,
                lineHeight = 16.sp,
            ),
        )
}
