package com.seniorhub.os.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.google.firebase.Timestamp
import com.seniorhub.os.data.Contact
import com.seniorhub.os.data.DeviceMessage
import com.seniorhub.os.ui.theme.SeniorHubDesign
import com.seniorhub.os.util.belongsToContactThread
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

val MessageCardShape = RoundedCornerShape(16.dp)

/** Pencil `message` / `message-read`: padding top / right / bottom / left, gap 24. */
val MessagePaddingTop = 32.dp
val MessagePaddingHorizontal = 32.dp
val MessagePaddingBottom = 24.dp
val MessageContentGap = 24.dp
val MessageFooterGap = 12.dp
val MessageBodyContentMaxWidth = 320.dp

private const val MessageBodyMaxLines = 3

fun messageSenderLabel(message: DeviceMessage, contact: Contact?): String =
    contact?.name?.trim()?.takeIf { it.isNotEmpty() }
        ?: message.senderDisplayName?.trim()?.takeIf { it.isNotEmpty() }
        ?: message.inboundFromName?.trim()?.takeIf { it.isNotEmpty() }
        ?: message.outboundName?.trim()?.takeIf { it.isNotEmpty() }
        ?: "Rodina"

/** Přečteno na tabletu (Firestore `readAt`). */
fun DeviceMessage.isReadOnTablet(): Boolean = readAt != null

fun formatMessageTimeNatural(ts: Timestamp?): String {
    if (ts == null) return "Teď"
    val zdt = ts.toDate().toInstant().atZone(ZoneId.systemDefault())
    val date = zdt.toLocalDate()
    val time = zdt.format(DateTimeFormatter.ofPattern("HH:mm"))
    val today = LocalDate.now()
    return when {
        date == today -> "Dnes $time"
        date == today.minusDays(1) -> "Včera $time"
        date.year == today.year -> {
            val monthDay = zdt.format(DateTimeFormatter.ofPattern("d. M.", Locale("cs", "CZ")))
            "$monthDay $time"
        }
        else -> zdt.format(DateTimeFormatter.ofPattern("d. M. yyyy HH:mm", Locale("cs", "CZ")))
    }
}

/**
 * Karta zprávy — `message` (nepřečtená) / `message-read` (přečtená), Pencil `JXMMA` / `nt3lR`.
 */
@Composable
fun DashboardMessageCard(
    message: DeviceMessage,
    contact: Contact?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    DashboardMessageCard(
        message = message,
        contact = contact,
        read = message.isReadOnTablet(),
        onClick = onClick,
        modifier = modifier,
    )
}

@Composable
fun DashboardMessageCard(
    message: DeviceMessage,
    contact: Contact?,
    read: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val surfaceColor = if (read) SeniorHubDesign.MessageReadSurface else SeniorHubDesign.MessageSurface

    Column(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentWidth(align = Alignment.Start)
            .clip(MessageCardShape)
            .background(surfaceColor)
            .clickable(onClick = onClick)
            .padding(
                start = MessagePaddingHorizontal,
                top = MessagePaddingTop,
                end = MessagePaddingHorizontal,
                bottom = MessagePaddingBottom,
            ),
        verticalArrangement = Arrangement.spacedBy(MessageContentGap),
    ) {
        DashboardMessageContent(
            message = message,
            contact = contact,
            read = read,
            bodyPreview = true,
            fadeColor = surfaceColor,
        )
    }
}

/**
 * Stejná kompozice jako karta na dashboardu: text nahoře, odesílatel + čas dole.
 * V dialogu [bodyPreview] = false — celý text bez zkrácení a bez fade.
 */
@Composable
fun DashboardMessageContent(
    message: DeviceMessage,
    contact: Contact?,
    read: Boolean,
    bodyPreview: Boolean = true,
    fadeColor: Color? = null,
    modifier: Modifier = Modifier,
) {
    val senderName = messageSenderLabel(message, contact)
    val timeLabel = formatMessageTimeNatural(message.createdAt)
    val bodyStyle = SeniorHubDesign.messageBodyStyle(read)
    val metaStyle = SeniorHubDesign.messageMetaStyle(read)
    val surfaceColor = fadeColor
        ?: if (read) SeniorHubDesign.MessageReadSurface else SeniorHubDesign.MessageSurface

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(MessageContentGap),
    ) {
        if (bodyPreview) {
            MessageBodyWithFade(
                text = message.body,
                bodyStyle = bodyStyle,
                fadeColor = surfaceColor,
                modifier = Modifier.widthIn(max = MessageBodyContentMaxWidth),
            )
        } else {
            Text(
                text = message.body,
                style = bodyStyle,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MessageFooterGap),
        ) {
            DashboardContactAvatar(
                contact = contact,
                fallbackLabel = senderName,
            )
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = senderName,
                    style = metaStyle,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = timeLabel,
                    style = metaStyle,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
fun DashboardMessageCard(
    message: DeviceMessage,
    contacts: List<Contact>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val contact = contacts.firstOrNull { message.belongsToContactThread(it) }
    DashboardMessageCard(
        message = message,
        contact = contact,
        onClick = onClick,
        modifier = modifier,
    )
}

@Composable
private fun MessageBodyWithFade(
    text: String,
    bodyStyle: TextStyle,
    fadeColor: Color,
    modifier: Modifier = Modifier,
) {
    var showFade by remember(text) { mutableStateOf(false) }

    val fadeBrush = remember(fadeColor) {
        Brush.verticalGradient(
            0f to Color.Transparent,
            0.55f to fadeColor.copy(alpha = 0.35f),
            1f to fadeColor,
        )
    }
    Text(
        text = text,
        style = bodyStyle,
        maxLines = MessageBodyMaxLines,
        overflow = TextOverflow.Clip,
        onTextLayout = { showFade = it.hasVisualOverflow },
        modifier = modifier
            .wrapContentWidth(align = Alignment.Start)
            .drawBehind {
                if (showFade) {
                    drawRect(brush = fadeBrush)
                }
            },
    )
}

