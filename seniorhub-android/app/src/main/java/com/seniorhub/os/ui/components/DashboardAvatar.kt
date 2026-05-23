package com.seniorhub.os.ui.components

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.seniorhub.os.data.Contact
import com.seniorhub.os.ui.theme.SeniorHubDesign

/** Pencil `avatar` — 44×52, corner 16. */
val DashboardAvatarShape = RoundedCornerShape(16.dp)
val DashboardAvatarWidth = 44.dp
val DashboardAvatarHeight = 52.dp

@Composable
private fun rememberDefaultAvatarBitmapOrNull(): androidx.compose.ui.graphics.ImageBitmap? {
    val context = LocalContext.current
    return remember(context) {
        runCatching {
            context.assets.open("avatar.png").use { input ->
                BitmapFactory.decodeStream(input)?.asImageBitmap()
            }
        }.getOrNull()
    }
}

@Composable
fun DashboardContactAvatar(
    contact: Contact?,
    fallbackLabel: String,
    modifier: Modifier = Modifier.size(width = DashboardAvatarWidth, height = DashboardAvatarHeight),
) {
    val context = LocalContext.current
    val defaultBitmap = rememberDefaultAvatarBitmapOrNull()
    val contactBitmap = remember(contact?.avatarUri) {
        contact?.avatarUri?.let { raw ->
            runCatching {
                context.contentResolver.openInputStream(Uri.parse(raw))?.use { input ->
                    BitmapFactory.decodeStream(input)?.asImageBitmap()
                }
            }.getOrNull()
        }
    }
    val bitmap = contactBitmap ?: defaultBitmap
    val initial = contact?.name?.trim()?.firstOrNull()?.uppercaseChar()?.toString()
        ?: fallbackLabel.trim().firstOrNull()?.uppercaseChar()?.toString()

    Box(
        modifier = modifier
            .clip(DashboardAvatarShape)
            .background(SeniorHubDesign.MessageReadSurface),
        contentAlignment = Alignment.Center,
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } else if (initial != null) {
            Text(
                text = initial,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = SeniorHubDesign.AccentGold,
            )
        }
    }
}
