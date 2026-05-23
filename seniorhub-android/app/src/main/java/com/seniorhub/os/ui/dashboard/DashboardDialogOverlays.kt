package com.seniorhub.os.ui.dashboard

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import com.seniorhub.os.R
import com.seniorhub.os.data.Contact
import com.seniorhub.os.data.DeviceMessage
import com.seniorhub.os.ui.components.DashboardContactAvatar
import com.seniorhub.os.ui.components.DashboardMessageContent
import com.seniorhub.os.ui.components.DashboardModalCard
import com.seniorhub.os.ui.components.DashboardModalScrim
import com.seniorhub.os.ui.components.MessageCardShape
import com.seniorhub.os.ui.components.MessagePaddingBottom
import com.seniorhub.os.ui.components.MessagePaddingHorizontal
import com.seniorhub.os.ui.components.MessagePaddingTop
import com.seniorhub.os.ui.components.MorphingDashboardModal
import com.seniorhub.os.ui.theme.SeniorHubDesign

@Composable
fun ContactQuickActionDialog(
    contact: Contact,
    onDismiss: () -> Unit,
    onCall: () -> Unit,
    onWriteMessage: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        DashboardModalScrim(onDismiss = onDismiss)
        DashboardModalCard(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth(0.55f)
                .padding(32.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                horizontalAlignment = Alignment.Start,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top,
                ) {
                    DashboardContactAvatar(
                        contact = contact,
                        fallbackLabel = contact.name.ifBlank { contact.phone },
                        modifier = Modifier.size(width = 64.dp, height = 76.dp),
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            Icons.Outlined.Close,
                            contentDescription = "Zavřít",
                            tint = SeniorHubDesign.MenuInactive,
                        )
                    }
                }
                Text(
                    text = contact.name.ifBlank { contact.phone },
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Normal,
                    color = SeniorHubDesign.AccentGold,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (contact.name.isNotBlank() && contact.phone.isNotBlank()) {
                    Text(
                        text = contact.phone,
                        fontSize = 18.sp,
                        color = SeniorHubDesign.WeatherText,
                        textAlign = TextAlign.Start,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                Button(
                    onClick = onWriteMessage,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SeniorHubDesign.MessageSurface,
                        contentColor = SeniorHubDesign.Black,
                    ),
                ) {
                    Icon(Icons.Outlined.ChatBubbleOutline, contentDescription = null)
                    Text("Napsat zprávu", fontSize = 20.sp, modifier = Modifier.padding(start = 10.dp))
                }
                OutlinedButton(
                    onClick = onCall,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp),
                    enabled = contact.phone.isNotBlank(),
                ) {
                    Icon(
                        Icons.Outlined.Call,
                        contentDescription = null,
                        tint = SeniorHubDesign.AccentGold,
                    )
                    Text(
                        "Zavolat",
                        fontSize = 20.sp,
                        color = SeniorHubDesign.AccentGold,
                        modifier = Modifier.padding(start = 10.dp),
                    )
                }
            }
        }
    }
}

/** Pencil `dialogue-answer` — tmavý vstup odpovědi vpravo. */
@Composable
private fun DialogueAnswerInput(
    value: String,
    onValueChange: (String) -> Unit,
    focusRequester: FocusRequester,
    modifier: Modifier = Modifier,
) {
    val inputShape = RoundedCornerShape(16.dp)
    val textStyle = TextStyle(
        fontFamily = SeniorHubDesign.messageBodyStyle(read = true).fontFamily,
        fontSize = 22.sp,
        fontWeight = FontWeight.Normal,
        letterSpacing = (-0.24).sp,
        lineHeight = 33.sp,
        color = SeniorHubDesign.ReplyInputText,
    )
    Box(
        modifier = modifier
            .clip(inputShape)
            .background(SeniorHubDesign.ReplyInputBackground)
            .border(1.dp, SeniorHubDesign.ReplyInputBorder, inputShape)
            .padding(24.dp),
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxSize()
                .focusRequester(focusRequester),
            textStyle = textStyle,
            cursorBrush = SolidColor(SeniorHubDesign.AccentGold),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            decorationBox = { inner ->
                Box(Modifier.fillMaxSize()) {
                    if (value.isEmpty()) {
                        Text(
                            text = "Odpověď…",
                            style = textStyle.copy(color = SeniorHubDesign.ReplyInputText.copy(alpha = 0.55f)),
                        )
                    }
                    inner()
                }
            },
        )
    }
}

@Composable
fun MessageReplyDialog(
    message: DeviceMessage,
    contact: Contact?,
    sourceBoundsInRoot: Rect? = null,
    outboundChannelLabel: String? = null,
    onDismiss: () -> Unit,
    onSendReply: (String, (Result<Unit>) -> Unit) -> Unit,
) {
    var replyText by remember(message.id) { mutableStateOf("") }
    var error by remember(message.id) { mutableStateOf<String?>(null) }
    val read = message.readAt != null
    val messageSurface = if (read) {
        SeniorHubDesign.MessageReadSurface
    } else {
        SeniorHubDesign.MessageSurface
    }
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val pillShape = RoundedCornerShape(64.dp)

    MorphingDashboardModal(
        sourceBoundsInRoot = sourceBoundsInRoot,
        onDismiss = onDismiss,
        widthFraction = 0.72f,
        anchorToTopHalf = true,
        modifier = Modifier.fillMaxSize(),
    ) { dismiss, showReplyControls ->
        LaunchedEffect(showReplyControls) {
            if (!showReplyControls) return@LaunchedEffect
            delay(360)
            focusRequester.requestFocus()
            keyboardController?.show()
        }

        val replyAlpha by animateFloatAsState(
            targetValue = if (showReplyControls) 1f else 0f,
            animationSpec = tween(durationMillis = 220),
            label = "dialogueAnswerReplyAlpha",
        )

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(32.dp),
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(MessageCardShape)
                    .background(messageSurface)
                    .padding(
                        start = MessagePaddingHorizontal,
                        top = MessagePaddingTop,
                        end = MessagePaddingHorizontal,
                        bottom = MessagePaddingBottom,
                    ),
            ) {
                val messageScroll = rememberScrollState()
                DashboardMessageContent(
                    message = message,
                    contact = contact,
                    read = read,
                    bodyPreview = false,
                    fadeColor = messageSurface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(messageScroll),
                )
            }

            if (showReplyControls) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .graphicsLayer { alpha = replyAlpha }
                        .alpha(replyAlpha),
                    verticalArrangement = Arrangement.spacedBy(24.dp),
                ) {
                    DialogueAnswerInput(
                        value = replyText,
                        onValueChange = {
                            if (it.length <= 2000) {
                                replyText = it
                                error = null
                            }
                        },
                        focusRequester = focusRequester,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                    )
                    outboundChannelLabel?.let { ch ->
                        Text(
                            text = "Odeslat přes $ch",
                            fontSize = 14.sp,
                            color = SeniorHubDesign.MenuInactive,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    error?.let {
                        Text(text = it, color = SeniorHubDesign.WeatherSun, fontSize = 14.sp)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(24.dp),
                    ) {
                        OutlinedButton(
                            onClick = dismiss,
                            modifier = Modifier
                                .width(180.dp)
                                .height(56.dp),
                            shape = pillShape,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                SeniorHubDesign.ReplyInputBorder,
                            ),
                        ) {
                            Text(
                                text = "Zavřít",
                                fontSize = 20.sp,
                                color = SeniorHubDesign.ReplyInputText,
                            )
                        }
                        Button(
                            onClick = {
                                val body = replyText.trim()
                                when {
                                    contact == null ->
                                        error = "Kontakt pro odpověď není k dispozici."
                                    body.isEmpty() -> error = "Napište text odpovědi."
                                    else -> onSendReply(body) { result ->
                                        result.fold(
                                            onSuccess = { dismiss() },
                                            onFailure = { e ->
                                                error = e.message ?: "Odeslání se nezdařilo."
                                            },
                                        )
                                    }
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp),
                            enabled = contact != null,
                            shape = pillShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SeniorHubDesign.MessageSurface,
                                contentColor = SeniorHubDesign.Black,
                            ),
                        ) {
                            Text("Odeslat", fontSize = 20.sp)
                        }
                    }
                }
            }
        }
    }
}

enum class ConfirmActionKind { Call, Sms }

@Composable
fun ConfirmActionDialog(
    contactName: String,
    kind: ConfirmActionKind,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    val title = when (kind) {
        ConfirmActionKind.Call -> "Zavolat $contactName?"
        ConfirmActionKind.Sms -> "Napsat zprávu $contactName?"
    }
    val subtitle = when (kind) {
        ConfirmActionKind.Call -> "Potvrďte, že chcete zahájit hovor na číslo z adresáře."
        ConfirmActionKind.Sms -> "Otevře se dialog pro novou zprávu."
    }
    val confirmLabel = when (kind) {
        ConfirmActionKind.Call -> "Ano, volat"
        ConfirmActionKind.Sms -> "Ano, psát"
    }
    val pillShape = RoundedCornerShape(64.dp)
    Box(modifier = Modifier.fillMaxSize()) {
        DashboardModalScrim(onDismiss = onDismiss)
        DashboardModalCard(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth(0.42f)
                .padding(24.dp),
            backgroundColor = SeniorHubDesign.DialogueAnswerShell,
            cornerRadius = 32.dp,
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                Text(
                    text = title,
                    fontSize = 28.sp,
                    color = SeniorHubDesign.AccentGold,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = subtitle,
                    fontSize = 18.sp,
                    color = SeniorHubDesign.MenuInactive,
                    textAlign = TextAlign.Center,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .width(160.dp)
                            .height(56.dp),
                        shape = pillShape,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            SeniorHubDesign.ReplyInputBorder,
                        ),
                    ) {
                        Text("Zrušit", fontSize = 20.sp, color = SeniorHubDesign.ReplyInputText)
                    }
                    Button(
                        onClick = onConfirm,
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp),
                        shape = pillShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SeniorHubDesign.MessageSurface,
                            contentColor = SeniorHubDesign.Black,
                        ),
                    ) {
                        Text(confirmLabel, fontSize = 20.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun PhotoViewDialog(
    photo: FamilyPhotoItem,
    onDismiss: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        DashboardModalScrim(onDismiss = onDismiss)
        DashboardModalCard(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth(0.72f)
                .padding(24.dp),
            backgroundColor = SeniorHubDesign.DialogueAnswerShell,
            cornerRadius = 32.dp,
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(360.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(photo.color),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(photo.title, fontSize = 32.sp, color = Color.White.copy(alpha = 0.5f))
                }
                Text(
                    text = photo.description,
                    style = SeniorHubDesign.messageBodyStyle(read = true),
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    Button(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(64.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SeniorHubDesign.MessageSurface,
                            contentColor = SeniorHubDesign.Black,
                        ),
                    ) {
                        Text("Zavřít", fontSize = 20.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun EditContactDialog(
    contact: Contact,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onSave: (name: String, phone: String, avatarUri: String?, note: String) -> Unit,
) {
    val context = LocalContext.current
    var name by remember(contact.id) { mutableStateOf(contact.name) }
    var phone by remember(contact.id) { mutableStateOf(contact.phone) }
    var note by remember(contact.id) { mutableStateOf(contact.note) }
    var avatarUri by remember(contact.id) { mutableStateOf(contact.avatarUri) }
    var error by remember(contact.id) { mutableStateOf(errorMessage) }

    val avatarPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            }
            avatarUri = uri.toString()
        }
    }

    val previewContact = remember(contact.id, name, avatarUri) {
        contact.copy(name = name, avatarUri = avatarUri)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        DashboardModalScrim(onDismiss = onDismiss)
        DashboardModalCard(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth(0.55f)
                .padding(24.dp),
            backgroundColor = SeniorHubDesign.DialogueAnswerShell,
            cornerRadius = 32.dp,
        ) {
            Column(
                modifier = Modifier
                    .padding(32.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = stringResource(R.string.contact_edit_title),
                    fontSize = 28.sp,
                    color = SeniorHubDesign.AccentGold,
                )
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = stringResource(R.string.contact_edit_avatar_section),
                        fontSize = 14.sp,
                        color = SeniorHubDesign.MenuInactive,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        Box(
                            modifier = Modifier.clickable {
                                avatarPicker.launch(arrayOf("image/*"))
                            },
                        ) {
                            DashboardContactAvatar(
                                contact = previewContact,
                                fallbackLabel = name.ifBlank { phone },
                                modifier = Modifier.size(width = 72.dp, height = 84.dp),
                            )
                        }
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Button(
                                onClick = { avatarPicker.launch(arrayOf("image/*")) },
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                shape = RoundedCornerShape(64.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = SeniorHubDesign.MessageSurface,
                                    contentColor = SeniorHubDesign.Black,
                                ),
                            ) {
                                Text(
                                    text = stringResource(
                                        if (avatarUri.isNullOrBlank()) {
                                            R.string.contact_edit_avatar_add
                                        } else {
                                            R.string.contact_edit_avatar_change
                                        },
                                    ),
                                    fontSize = 16.sp,
                                )
                            }
                            if (!avatarUri.isNullOrBlank()) {
                                OutlinedButton(
                                    onClick = { avatarUri = null },
                                    modifier = Modifier.fillMaxWidth().height(48.dp),
                                    shape = RoundedCornerShape(64.dp),
                                ) {
                                    Text(
                                        text = stringResource(R.string.contact_edit_avatar_remove),
                                        color = SeniorHubDesign.ReplyInputText,
                                        fontSize = 16.sp,
                                    )
                                }
                            }
                        }
                    }
                }
                contactField(stringResource(R.string.contact_edit_name_label), name) { name = it }
                contactField(stringResource(R.string.contact_edit_phone_label), phone) { phone = it }
                contactNoteField(
                    label = stringResource(R.string.contact_edit_note_label),
                    placeholder = stringResource(R.string.contact_edit_note_placeholder),
                    value = note,
                    onValueChange = { if (it.length <= 500) note = it },
                )
                error?.let {
                    Text(it, color = SeniorHubDesign.WeatherSun, fontSize = 14.sp)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.width(140.dp).height(52.dp),
                        shape = RoundedCornerShape(64.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.contact_edit_cancel),
                            color = SeniorHubDesign.ReplyInputText,
                        )
                    }
                    Button(
                        onClick = {
                            val n = name.trim()
                            val p = phone.trim()
                            if (n.isEmpty() && p.isEmpty()) {
                                error = "Vyplň jméno nebo telefon."
                            } else {
                                onSave(n, p, avatarUri?.trim()?.takeIf { it.isNotEmpty() }, note.trim())
                            }
                        },
                        modifier = Modifier.weight(1f).height(52.dp),
                        shape = RoundedCornerShape(64.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SeniorHubDesign.MessageSurface,
                            contentColor = SeniorHubDesign.Black,
                        ),
                    ) {
                        Text(stringResource(R.string.contact_edit_save))
                    }
                }
            }
        }
    }
}

@Composable
private fun contactField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, fontSize = 14.sp, color = SeniorHubDesign.MenuInactive)
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = SeniorHubDesign.WeatherText,
                unfocusedTextColor = SeniorHubDesign.WeatherText,
                focusedBorderColor = SeniorHubDesign.AccentGold,
                unfocusedBorderColor = SeniorHubDesign.MenuInactive,
                focusedContainerColor = SeniorHubDesign.ReplyInputBackground,
                unfocusedContainerColor = SeniorHubDesign.ReplyInputBackground,
            ),
        )
    }
}

@Composable
private fun contactNoteField(
    label: String,
    placeholder: String,
    value: String,
    onValueChange: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, fontSize = 14.sp, color = SeniorHubDesign.MenuInactive)
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 120.dp),
            placeholder = {
                Text(
                    text = placeholder,
                    color = SeniorHubDesign.MenuInactive.copy(alpha = 0.7f),
                    fontSize = 14.sp,
                )
            },
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
            ),
            minLines = 3,
            maxLines = 6,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = SeniorHubDesign.WeatherText,
                unfocusedTextColor = SeniorHubDesign.WeatherText,
                focusedBorderColor = SeniorHubDesign.AccentGold,
                unfocusedBorderColor = SeniorHubDesign.MenuInactive,
                focusedContainerColor = SeniorHubDesign.ReplyInputBackground,
                unfocusedContainerColor = SeniorHubDesign.ReplyInputBackground,
            ),
        )
    }
}
