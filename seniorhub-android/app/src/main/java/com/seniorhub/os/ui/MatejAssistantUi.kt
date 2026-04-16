package com.seniorhub.os.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.seniorhub.os.R

/** Fáze zobrazení asistenta Matěje — audio řídí [com.seniorhub.os.ui.HomeViewModel] a [com.seniorhub.os.matej.MatejVoicePipeline]. */
enum class MatejPhase {
    /** Krátké přivítání po probuzení (TTS bude doplněno). */
    Greeting,

    /** Aktivní poslech uživatele. */
    Listening,

    /** Zpracování (např. volání modelu / nástroje). */
    Processing,

    /** Čekání na hlasové ano/ne před SMS nebo hovorem. */
    Confirming,
}

/**
 * @param compact Pokud true, zobrazí se malý box v pravém horním rohu (např. při překryvu jiného obsahu).
 */
data class MatejUiSession(
    val phase: MatejPhase,
    val compact: Boolean,
)

@Composable
fun MatejAssistantChrome(
    session: MatejUiSession?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (session == null) return

    if (session.compact) {
        MatejCompactChip(
            phase = session.phase,
            onDismiss = onDismiss,
            modifier = modifier,
        )
    } else {
        MatejExpandedCard(
            phase = session.phase,
            onDismiss = onDismiss,
            modifier = modifier,
        )
    }
}

@Composable
private fun MatejExpandedCard(
    phase: MatejPhase,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    Card(
        modifier = modifier
            .padding(16.dp)
            .width(320.dp),
        colors = CardDefaults.cardColors(
            containerColor = scheme.surfaceContainerHigh,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Filled.SmartToy,
                contentDescription = null,
                tint = scheme.primary,
                modifier = Modifier.size(36.dp),
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.matej_assistant_name),
                    style = MaterialTheme.typography.titleMedium,
                    color = scheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = phaseSubtitle(phase),
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = stringResource(R.string.matej_assistant_dismiss_cd),
                    tint = scheme.onSurfaceVariant,
                )
            }
        }
        if (phase == MatejPhase.Listening || phase == MatejPhase.Confirming) {
            ListeningPulseRow(
                label = if (phase == MatejPhase.Confirming) {
                    stringResource(R.string.matej_confirm_listening_indicator)
                } else {
                    stringResource(R.string.matej_listening_indicator)
                },
            )
        }
    }
}

@Composable
private fun ListeningPulseRow(label: String) {
    val scheme = MaterialTheme.colorScheme
    val transition = rememberInfiniteTransition(label = "matejPulse")
    val alpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulseAlpha",
    )
    Row(
        modifier = Modifier
            .padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .alpha(alpha)
                .background(scheme.tertiary, CircleShape),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Icon(
            imageVector = Icons.Filled.Mic,
            contentDescription = null,
            tint = scheme.tertiary,
            modifier = Modifier.size(18.dp),
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = scheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun MatejCompactChip(
    phase: MatejPhase,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val transition = rememberInfiniteTransition(label = "matejChipPulse")
    val pulseAlpha by transition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "chipPulse",
    )
    Surface(
        modifier = modifier.padding(12.dp),
        shape = MaterialTheme.shapes.large,
        color = scheme.surfaceContainerHighest,
        tonalElevation = 4.dp,
        shadowElevation = 4.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Filled.SmartToy,
                contentDescription = null,
                tint = scheme.primary,
                modifier = Modifier.size(28.dp),
            )
            if (phase == MatejPhase.Listening || phase == MatejPhase.Confirming) {
                Spacer(modifier = Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .alpha(pulseAlpha)
                        .background(scheme.tertiary, CircleShape),
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
            IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = stringResource(R.string.matej_assistant_dismiss_cd),
                    tint = scheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

@Composable
private fun phaseSubtitle(phase: MatejPhase): String {
    return when (phase) {
        MatejPhase.Greeting -> stringResource(R.string.matej_phase_greeting)
        MatejPhase.Listening -> stringResource(R.string.matej_phase_listening)
        MatejPhase.Processing -> stringResource(R.string.matej_phase_processing)
        MatejPhase.Confirming -> stringResource(R.string.matej_phase_confirming)
    }
}
