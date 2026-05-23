package com.seniorhub.os.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import com.seniorhub.os.ui.theme.SeniorHubDesign
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Modal s animací z [sourceBoundsInRoot] (karta zprávy) do středu / horní poloviny při klávesnici.
 */
@Composable
fun MorphingDashboardModal(
    sourceBoundsInRoot: Rect?,
    onDismiss: () -> Unit,
    widthFraction: Float = 0.62f,
    /** Dialog v horní polovině — místo pro klávesnici pod ním (`dialogue-answer`). */
    anchorToTopHalf: Boolean = false,
    modifier: Modifier = Modifier,
    content: @Composable (dismiss: () -> Unit, showReplyControls: Boolean) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val progress = remember(sourceBoundsInRoot) {
        Animatable(if (sourceBoundsInRoot == null) 1f else 0f)
    }
    LaunchedEffect(sourceBoundsInRoot) {
        if (sourceBoundsInRoot != null) {
            progress.snapTo(0f)
            progress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 340, easing = FastOutSlowInEasing),
            )
        } else {
            progress.snapTo(1f)
        }
    }

    fun dismissAnimated() {
        scope.launch {
            if (sourceBoundsInRoot != null && progress.value > 0.01f) {
                progress.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing),
                )
            }
            onDismiss()
        }
    }

    val density = LocalDensity.current
    val imeBottomPx = WindowInsets.ime.getBottom(density)
    val keyboardOpen = imeBottomPx > 0
    BoxWithConstraints(modifier.fillMaxSize()) {
        val screenW = constraints.maxWidth.toFloat()
        val screenH = constraints.maxHeight.toFloat()
        val targetW = screenW * widthFraction
        val targetLeft = (screenW - targetW) / 2f
        val topInsetPx = with(density) { 16.dp.toPx() }
        val bottomMarginPx = with(density) { 8.dp.toPx() }
        val availableAboveKeyboardPx = (screenH - imeBottomPx - topInsetPx - bottomMarginPx)
            .coerceAtLeast(screenH * 0.35f)
        val maxDialogHeightPx = when {
            anchorToTopHalf && keyboardOpen -> availableAboveKeyboardPx
            anchorToTopHalf -> screenH * 0.48f
            keyboardOpen -> availableAboveKeyboardPx
            else -> screenH * 0.72f
        }
        val centeredTop = ((screenH - maxDialogHeightPx) * 0.5f).coerceAtLeast(topInsetPx)
        val targetTop = if (anchorToTopHalf || keyboardOpen) topInsetPx else centeredTop

        val src = sourceBoundsInRoot
        val t = progress.value
        val left = if (src != null) lerp(src.left, targetLeft, t) else targetLeft
        val top = if (src != null) lerp(src.top, targetTop, t) else targetTop
        val width = if (src != null) lerp(src.width, targetW, t) else targetW
        val height = if (src != null) {
            lerp(src.height, maxDialogHeightPx, t).coerceIn(src.height, maxDialogHeightPx)
        } else {
            maxDialogHeightPx
        }

        val scrimAlpha = 0.8f * t.coerceIn(0f, 1f)
        val showReplyControls = t > 0.52f

        DashboardModalScrim(
            onDismiss = { dismissAnimated() },
            alpha = scrimAlpha,
        )
        val modalShape = if (anchorToTopHalf) 32.dp else 16.dp
        val modalBg = if (anchorToTopHalf) SeniorHubDesign.DialogueAnswerShell else SeniorHubDesign.WeatherSurface
        DashboardModalCard(
            modifier = Modifier
                .offset { IntOffset(left.roundToInt(), top.roundToInt()) }
                .size(
                    width = with(density) { width.toDp() },
                    height = with(density) { height.toDp() },
                ),
            backgroundColor = modalBg,
            cornerRadius = modalShape,
        ) {
            Box(Modifier.fillMaxSize()) {
                content({ dismissAnimated() }, showReplyControls)
            }
        }
    }
}

private fun lerp(start: Float, end: Float, fraction: Float): Float =
    start + (end - start) * fraction
