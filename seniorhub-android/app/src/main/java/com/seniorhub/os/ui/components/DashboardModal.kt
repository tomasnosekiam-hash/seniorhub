package com.seniorhub.os.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.seniorhub.os.ui.theme.SeniorHubDesign

/** Ztmavení + klik mimo dialog zavře overlay. Při alpha ≈ 0 nezachytává dotyky. */
@Composable
fun DashboardModalScrim(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    alpha: Float = 0.8f,
) {
    val effectiveAlpha = alpha.coerceIn(0f, 1f)
    val interceptsTouches = effectiveAlpha > 0.05f
    Box(
        modifier = modifier
            .fillMaxSize()
            .then(
                if (interceptsTouches) {
                    Modifier
                        .background(Color(0xFF0F0D14).copy(alpha = effectiveAlpha))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onDismiss,
                        )
                } else {
                    Modifier
                },
            ),
    )
}

@Composable
fun DashboardModalCard(
    modifier: Modifier = Modifier,
    backgroundColor: Color = SeniorHubDesign.WeatherSurface,
    cornerRadius: Dp = 16.dp,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(backgroundColor),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}
