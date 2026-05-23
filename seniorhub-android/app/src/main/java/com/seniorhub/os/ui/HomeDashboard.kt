package com.seniorhub.os.ui

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Rect
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.Timestamp
import com.seniorhub.os.R
import com.seniorhub.os.data.Contact
import com.seniorhub.os.data.DeviceConfig
import com.seniorhub.os.data.DeviceMessage
import com.seniorhub.os.data.DeviceSettings
import com.seniorhub.os.ui.dashboard.ConfirmActionDialog
import com.seniorhub.os.ui.dashboard.ConfirmActionKind
import com.seniorhub.os.ui.dashboard.ContactQuickActionDialog
import com.seniorhub.os.ui.dashboard.ContactsRailSelection
import com.seniorhub.os.ui.dashboard.DashboardCallsScreen
import com.seniorhub.os.ui.dashboard.DashboardCenterPanel
import com.seniorhub.os.ui.dashboard.DashboardContactsScreen
import com.seniorhub.os.ui.dashboard.DashboardLeftPanel
import com.seniorhub.os.ui.dashboard.DashboardMenuDestination
import com.seniorhub.os.ui.dashboard.DashboardMessagesScreen
import com.seniorhub.os.ui.dashboard.DashboardPhotosScreen
import com.seniorhub.os.ui.dashboard.DashboardRightPanel
import com.seniorhub.os.ui.dashboard.EditContactDialog
import com.seniorhub.os.ui.dashboard.FamilyPhotoItem
import com.seniorhub.os.ui.dashboard.MessageReplyDialog
import com.seniorhub.os.ui.dashboard.PhotoViewDialog
import com.seniorhub.os.ui.theme.SeniorHubDesign
import com.seniorhub.os.util.CallHistoryEntry
import com.seniorhub.os.util.CallType
import com.seniorhub.os.util.SimCardStatus
import com.seniorhub.os.util.SimCardState
import com.seniorhub.os.util.SimCardStatusReader
import com.seniorhub.os.util.belongsToContactThread
import com.seniorhub.os.util.normalizePhoneForDial
import com.seniorhub.os.util.phonesMatchForThread
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Suppress("unused")
private enum class SeniorTab {
    Home,
    Calls,
    Messages,
}

private sealed interface ContactFilter {
    data object All : ContactFilter
    data class One(val contact: Contact) : ContactFilter
}

private fun formatMessageTime(ts: Timestamp?): String {
    if (ts == null) return "Teď"
    return ts.toDate().toInstant().atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("d.M. HH:mm"))
}

private fun formatCallTime(millis: Long): String =
    java.time.Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("d.M. HH:mm"))

private fun CallType.label(): String = when (this) {
    CallType.Incoming -> "Příchozí"
    CallType.Outgoing -> "Odchozí"
    CallType.Missed -> "Zmeškaný"
    CallType.Other -> "Hovor"
}

private fun CallHistoryEntry.belongsTo(contact: Contact): Boolean =
    phonesMatchForThread(phone, contact.phone)

@Composable
fun HomeScreen(
    state: HomeUiState,
    onDismissAlert: () -> Unit,
    onDismissUnreadMessage: () -> Unit,
    onShowPairing: () -> Unit,
    onHidePairing: () -> Unit,
    onRefreshPairing: () -> Unit,
    onKioskSecretTap: () -> Unit,
    onDismissKioskUnlock: () -> Unit,
    onSubmitKioskPin: (String) -> Unit,
    onContactCall: (String) -> Unit,
    onContactSms: (Contact) -> Unit,
    onContactThread: (Contact) -> Unit,
    onSendContactMessage: (Contact, String, DeviceMessage?, (Result<Unit>) -> Unit) -> Unit,
    onMessageRead: (DeviceMessage) -> Unit = {},
    showKioskLauncherHint: Boolean,
    simCardStatus: SimCardStatus = SimCardStatus(SimCardState.Ready),
    onOpenSimSettings: () -> Unit = {},
    communicationPermissions: CommunicationPermissions = CommunicationPermissions.AllGranted,
    onRequestCommunicationPermissions: () -> Unit = {},
    onOpenAppSettings: () -> Unit = {},
    onAddContact: () -> Unit = {},
    onUpdateContact: (Contact, String, String, String?, String, (Result<Unit>) -> Unit) -> Unit =
        { _, _, _, _, _, onDone -> onDone(Result.success(Unit)) },
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    Surface(modifier = modifier.fillMaxSize(), color = scheme.background) {
        Box(Modifier.fillMaxSize()) {
            AnimatedContent(
                targetState = state.loading,
                transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(160)) },
                label = "seniorHomeLoading",
            ) { loading ->
                if (loading) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = scheme.primary)
                    }
                } else {
                    SeniorDashboard(
                        state = state,
                        showKioskLauncherHint = showKioskLauncherHint,
                        simCardStatus = simCardStatus,
                        onOpenSimSettings = onOpenSimSettings,
                        communicationPermissions = communicationPermissions,
                        onRequestCommunicationPermissions = onRequestCommunicationPermissions,
                        onOpenAppSettings = onOpenAppSettings,
                        onShowPairing = onShowPairing,
                        onContactCall = onContactCall,
                        onContactSms = onContactSms,
                        onContactThread = onContactThread,
                        onSendContactMessage = onSendContactMessage,
                        onMessageRead = onMessageRead,
                        onAddContact = onAddContact,
                        onUpdateContact = onUpdateContact,
                    )
                }
            }

            state.errorMessage?.let { msg ->
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(18.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = scheme.errorContainer,
                ) {
                    Text(
                        text = stringResource(R.string.dashboard_connection_error, msg),
                        modifier = Modifier.padding(18.dp),
                        style = MaterialTheme.typography.bodyLarge,
                        color = scheme.onErrorContainer,
                    )
                }
            }

            state.device?.alertMessage?.takeIf { it.isNotBlank() }?.let { alert ->
                AlertOverlay(message = alert, onDismiss = onDismissAlert)
            }

            val device = state.device
            if (device != null && state.showPairingSheet) {
                PairingOverlay(device = device, onRefreshPairing = onRefreshPairing, onClose = onHidePairing)
            }

            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .size(96.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onKioskSecretTap,
                    ),
            )

            if (state.showKioskUnlock) {
                KioskUnlockOverlay(
                    errorMessage = state.kioskUnlockError,
                    onDismiss = onDismissKioskUnlock,
                    onSubmit = onSubmitKioskPin,
                )
            }

            state.unreadMessage?.let { msg ->
                MessageOverlay(message = msg, onDismiss = onDismissUnreadMessage)
            }
        }
    }
}

@Composable
private fun SeniorDashboard(
    state: HomeUiState,
    showKioskLauncherHint: Boolean,
    simCardStatus: SimCardStatus,
    onOpenSimSettings: () -> Unit,
    communicationPermissions: CommunicationPermissions,
    onRequestCommunicationPermissions: () -> Unit,
    onOpenAppSettings: () -> Unit,
    onShowPairing: () -> Unit,
    onContactCall: (String) -> Unit,
    onContactSms: (Contact) -> Unit,
    onContactThread: (Contact) -> Unit,
    onSendContactMessage: (Contact, String, DeviceMessage?, (Result<Unit>) -> Unit) -> Unit,
    onMessageRead: (DeviceMessage) -> Unit,
    onAddContact: () -> Unit,
    onUpdateContact: (Contact, String, String, String?, String, (Result<Unit>) -> Unit) -> Unit,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var menuDestination by remember { mutableStateOf(DashboardMenuDestination.Home) }
    var messagesRail by remember { mutableStateOf<ContactsRailSelection>(ContactsRailSelection.All) }
    var callsRail by remember { mutableStateOf<ContactsRailSelection>(ContactsRailSelection.All) }
    var selectedMessage by remember { mutableStateOf<DeviceMessage?>(null) }
    var messageExpandFromBounds by remember { mutableStateOf<Rect?>(null) }
    val messageCardBounds = remember { mutableStateMapOf<String, Rect>() }
    var selectedContact by remember { mutableStateOf<Contact?>(null) }
    var confirmAction by remember { mutableStateOf<Pair<Contact, ConfirmActionKind>?>(null) }
    var editContact by remember { mutableStateOf<Contact?>(null) }
    var editContactError by remember { mutableStateOf<String?>(null) }
    var selectedPhoto by remember { mutableStateOf<FamilyPhotoItem?>(null) }
    var clock by remember { mutableStateOf(LocalTime.now()) }
    LaunchedEffect(Unit) {
        while (isActive) {
            clock = LocalTime.now()
            delay(60_000L)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars.only(WindowInsetsSides.Top))
                .background(SeniorHubDesign.DashboardBackground),
        ) {
            if (simCardStatus.blocksCellular) {
                SimUnlockStrip(
                    message = SimCardStatusReader.bannerMessage(simCardStatus),
                    onOpenSimSettings = onOpenSimSettings,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                )
            }
            if (!communicationPermissions.allGranted) {
                PermissionStrip(
                    permissions = communicationPermissions,
                    onRequestPermissions = onRequestCommunicationPermissions,
                    onOpenSettings = onOpenAppSettings,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                )
            }
            if (showKioskLauncherHint) {
                SystemHintStrip(
                    text = stringResource(R.string.kiosk_home_hint),
                    onClick = onShowPairing,
                    modifier = Modifier.padding(horizontal = 24.dp),
                )
            }
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(
                    if (menuDestination == DashboardMenuDestination.Home) 96.dp else 32.dp,
                    Alignment.Start,
                ),
                verticalAlignment = Alignment.Top,
            ) {
                DashboardLeftPanel(
                    clock = clock,
                    activeDestination = menuDestination,
                    onMenuSelect = { dest ->
                        menuDestination = dest
                        if (dest == DashboardMenuDestination.Messages) {
                            messagesRail = ContactsRailSelection.All
                        }
                        if (dest == DashboardMenuDestination.Calls) {
                            callsRail = ContactsRailSelection.All
                        }
                    },
                )
                when (menuDestination) {
                    DashboardMenuDestination.Home -> {
                        DashboardCenterPanel(
                            messages = state.messages,
                            contacts = state.contacts,
                            onMessageCardBounds = { id, rect -> messageCardBounds[id] = rect },
                            onMessageClick = { msg ->
                                onMessageRead(msg)
                                messageExpandFromBounds = messageCardBounds[msg.id]
                                selectedMessage = msg
                            },
                        )
                        DashboardRightPanel(
                            weather = state.weather,
                            contacts = state.contacts,
                            onContactClick = { selectedContact = it },
                        )
                    }
                    DashboardMenuDestination.Messages -> {
                        DashboardMessagesScreen(
                            messages = state.messages,
                            contacts = state.contacts,
                            selected = messagesRail,
                            onSelect = { messagesRail = it },
                            onMessageClick = { msg ->
                                onMessageRead(msg)
                                messageExpandFromBounds = messageCardBounds[msg.id]
                                selectedMessage = msg
                            },
                            onMessageCardBounds = { id, rect -> messageCardBounds[id] = rect },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    DashboardMenuDestination.Calls -> {
                        DashboardCallsScreen(
                            callHistory = state.callHistory,
                            contacts = state.contacts,
                            selected = callsRail,
                            onSelect = { callsRail = it },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    DashboardMenuDestination.Contacts -> {
                        DashboardContactsScreen(
                            contacts = state.contacts,
                            onEdit = {
                                editContactError = null
                                editContact = it
                            },
                            onSms = { confirmAction = it to ConfirmActionKind.Sms },
                            onCall = { confirmAction = it to ConfirmActionKind.Call },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    DashboardMenuDestination.Photos -> {
                        DashboardPhotosScreen(
                            onPhotoClick = { selectedPhoto = it },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }

        selectedContact?.let { contact ->
            ContactQuickActionDialog(
                contact = contact,
                onDismiss = { selectedContact = null },
                onCall = {
                    if (contact.phone.isNotBlank()) {
                        onContactCall(contact.phone)
                    }
                    selectedContact = null
                },
                onWriteMessage = {
                    selectedContact = null
                    onContactSms(contact)
                },
            )
        }

        selectedMessage?.let { msg ->
            val replyContact = state.contacts.firstOrNull { msg.belongsToContactThread(it) }
            val threadForReply = replyContact?.let { c ->
                state.messages.filter { it.belongsToContactThread(c) }
            }.orEmpty()
            val outboundLabel = replyContact?.let { c ->
                com.seniorhub.os.util.cellularChannelLabel(
                    com.seniorhub.os.util.resolveOutboundCellularChannel(
                        context,
                        c,
                        threadForReply,
                        replyTo = msg,
                    ),
                )
            }
            MessageReplyDialog(
                message = msg,
                contact = replyContact,
                sourceBoundsInRoot = messageExpandFromBounds,
                outboundChannelLabel = outboundLabel,
                onDismiss = {
                    onMessageRead(msg)
                    selectedMessage = null
                    messageExpandFromBounds = null
                },
                onSendReply = { body, onDone ->
                    val contact = replyContact
                    if (contact == null) {
                        onDone(Result.failure(IllegalStateException("Kontakt pro odpověď není k dispozici.")))
                    } else {
                        onSendContactMessage(contact, body, msg, onDone)
                    }
                },
            )
        }

        confirmAction?.let { (contact, kind) ->
            ConfirmActionDialog(
                contactName = contact.name.ifBlank { contact.phone },
                kind = kind,
                onDismiss = { confirmAction = null },
                onConfirm = {
                    confirmAction = null
                    when (kind) {
                        ConfirmActionKind.Call -> {
                            if (contact.phone.isNotBlank()) onContactCall(contact.phone)
                        }
                        ConfirmActionKind.Sms -> onContactSms(contact)
                    }
                },
            )
        }

        editContact?.let { contact ->
            EditContactDialog(
                contact = contact,
                errorMessage = editContactError,
                onDismiss = {
                    editContact = null
                    editContactError = null
                },
                onSave = { name, phone, avatarUri, note ->
                    onUpdateContact(contact, name, phone, avatarUri, note) { result ->
                        result.fold(
                            onSuccess = {
                                editContact = null
                                editContactError = null
                            },
                            onFailure = { e ->
                                editContactError = e.message ?: e.toString()
                            },
                        )
                    }
                },
            )
        }

        selectedPhoto?.let { photo ->
            PhotoViewDialog(photo = photo, onDismiss = { selectedPhoto = null })
        }
    }
}

@Composable
private fun CallsTab(
    contacts: List<Contact>,
    callHistory: List<CallHistoryEntry>,
    selected: ContactFilter,
    onSelected: (ContactFilter) -> Unit,
    onAddContact: () -> Unit,
    onCall: (String) -> Unit,
) {
    Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(0.dp)) {
        PeopleRail(
            contacts = contacts,
            selected = selected,
            onSelected = onSelected,
            onAddContact = onAddContact,
            modifier = Modifier.width(190.dp),
        )
        ContentSurface(Modifier.weight(1f).fillMaxHeight()) {
            val selectedContact = (selected as? ContactFilter.One)?.contact
            val visibleCalls = remember(callHistory, selected) {
                selectedContact?.let { c -> callHistory.filter { it.belongsTo(c) } } ?: callHistory
            }
            Text(
                text = selectedContact?.name ?: "Všechny kontakty",
                style = MaterialTheme.typography.headlineSmall,
            )
            Spacer(Modifier.height(14.dp))
            if (selectedContact != null) {
                Button(
                    onClick = { if (selectedContact.phone.isNotBlank()) onCall(selectedContact.phone) },
                    modifier = Modifier.heightIn(min = 64.dp),
                ) {
                    Icon(Icons.Outlined.Call, contentDescription = null)
                    Spacer(Modifier.width(10.dp))
                    Text("Zavolat", style = MaterialTheme.typography.titleLarge)
                }
            }
            Spacer(Modifier.height(18.dp))
            Text("Historie hovorů", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(10.dp))
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (visibleCalls.isEmpty()) {
                    item { EmptyText("Zatím žádné hovory pro tento výběr.") }
                } else {
                    items(visibleCalls, key = { it.id }) { call ->
                        CallHistoryRow(call = call, contacts = contacts)
                    }
                }
            }
        }
    }
}

@Composable
private fun MessagesTab(
    contacts: List<Contact>,
    messages: List<DeviceMessage>,
    selected: ContactFilter,
    onSelected: (ContactFilter) -> Unit,
    onAddContact: () -> Unit,
    onOpenThread: (Contact) -> Unit,
    onSendContactMessage: (Contact, String, DeviceMessage?, (Result<Unit>) -> Unit) -> Unit,
) {
    Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(0.dp)) {
        PeopleRail(
            contacts = contacts,
            selected = selected,
            onSelected = onSelected,
            onAddContact = onAddContact,
            modifier = Modifier.width(190.dp),
        )
        ContentSurface(Modifier.weight(1f).fillMaxHeight()) {
            val selectedContact = (selected as? ContactFilter.One)?.contact
            Text(
                text = selectedContact?.name ?: "Všechny zprávy",
                style = MaterialTheme.typography.headlineSmall,
            )
            Spacer(Modifier.height(12.dp))
            val visibleMessages = remember(messages, selected) {
                selectedContact?.let { c -> messages.filter { it.belongsToContactThread(c) } } ?: messages
            }
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (visibleMessages.isEmpty()) {
                    item { EmptyText("Zatím žádné zprávy.") }
                } else {
                    items(visibleMessages.asReversed(), key = { it.id }) { msg ->
                        CompactMessageRow(message = msg)
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            if (selectedContact != null) {
                MessageComposer(
                    contact = selectedContact,
                    onOpenThread = { onOpenThread(selectedContact) },
                    onSend = onSendContactMessage,
                )
            } else {
                EmptyText("Vyber kontakt vlevo a můžeš mu napsat novou zprávu.")
            }
        }
    }
}

@Composable
private fun PeopleRail(
    contacts: List<Contact>,
    selected: ContactFilter,
    onSelected: (ContactFilter) -> Unit,
    onAddContact: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PeopleSurface(modifier = modifier.fillMaxHeight()) {
        OutlinedButton(
            onClick = onAddContact,
            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
        ) {
            Icon(Icons.Outlined.PersonAdd, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Nový")
        }
        Spacer(Modifier.height(12.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                PersonFilterItem(
                    label = "Všechny kontakty",
                    selected = selected is ContactFilter.All,
                    icon = { Icon(Icons.Outlined.Groups, contentDescription = null) },
                    onClick = { onSelected(ContactFilter.All) },
                )
            }
            items(contacts, key = { it.id }) { contact ->
                PersonFilterItem(
                    label = contact.name.ifBlank { contact.phone },
                    selected = (selected as? ContactFilter.One)?.contact?.id == contact.id,
                    avatar = { ContactAvatar(contact = contact, selected = false) },
                    onClick = { onSelected(ContactFilter.One(contact)) },
                )
            }
        }
    }
}

@Composable
private fun PersonFilterItem(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    icon: (@Composable () -> Unit)? = null,
    avatar: (@Composable () -> Unit)? = null,
) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        color = if (selected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            when {
                avatar != null -> avatar()
                icon != null -> Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) { icon() }
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = scheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun ContactAvatar(
    contact: Contact,
    selected: Boolean,
    modifier: Modifier = Modifier.size(54.dp),
) {
    val scheme = MaterialTheme.colorScheme
    val context = LocalContext.current
    val bitmap = remember(contact.avatarUri) {
        contact.avatarUri?.let { raw ->
            runCatching {
                context.contentResolver.openInputStream(Uri.parse(raw))?.use { input ->
                    BitmapFactory.decodeStream(input)
                }
            }.getOrNull()
        }
    }
    val initial = contact.name.trim().firstOrNull()?.uppercaseChar()?.toString()
        ?: contact.phone.trim().firstOrNull()?.toString()
        ?: "?"
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest),
        contentAlignment = Alignment.Center,
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } else {
            Text(
                text = initial,
                style = MaterialTheme.typography.titleLarge,
                color = if (selected) scheme.onPrimary else scheme.onSurface,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun CompactMessageRow(message: DeviceMessage) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
            Text(
                text = message.senderDisplayName
                    ?: message.inboundFromName
                    ?: message.outboundName
                    ?: "Zpráva",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = message.body,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = formatMessageTime(message.createdAt),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
    }
}

@Composable
private fun MessageComposer(
    contact: Contact,
    onOpenThread: () -> Unit,
    onSend: (Contact, String, DeviceMessage?, (Result<Unit>) -> Unit) -> Unit,
) {
    var body by remember(contact.id) { mutableStateOf("") }
    var error by remember(contact.id) { mutableStateOf<String?>(null) }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(
                value = body,
                onValueChange = { if (it.length <= 2000) body = it },
                modifier = Modifier.fillMaxWidth().heightIn(min = 108.dp),
                minLines = 3,
                placeholder = { Text("Napsat zprávu...") },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                ),
            )
            error?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = onOpenThread, modifier = Modifier.weight(1f)) {
                    Text("Celé vlákno")
                }
                Button(
                    onClick = {
                        val text = body.trim()
                        if (text.isEmpty()) {
                            error = "Napiš text zprávy."
                        } else {
                            onSend(contact, text, null) { result ->
                                result.fold(
                                    onSuccess = {
                                        body = ""
                                        error = null
                                    },
                                    onFailure = { e -> error = e.message ?: "Odeslání se nezdařilo." },
                                )
                            }
                        }
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.AutoMirrored.Outlined.Send, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Odeslat")
                }
            }
    }
}

@Composable
private fun SectionSurface(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier.padding(18.dp),
        content = content,
    )
}

@Composable
private fun ContentSurface(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier.padding(18.dp),
        content = content,
    )
}

@Composable
private fun PeopleSurface(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier.padding(14.dp),
        content = content,
    )
}

@Composable
private fun CallHistoryRow(
    call: CallHistoryEntry,
    contacts: List<Contact>,
) {
    val contact = contacts.firstOrNull { call.belongsTo(it) }
    val title = contact?.name ?: call.cachedName ?: call.phone.ifBlank { "Neznámé číslo" }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(
                    if (call.type == CallType.Missed) {
                        MaterialTheme.colorScheme.errorContainer
                    } else {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.20f)
                    },
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Outlined.Call,
                contentDescription = null,
                tint = if (call.type == CallType.Missed) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.primary
                },
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            Text(
                text = "${call.type.label()} · ${formatCallTime(call.startedAtMillis)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (call.durationSeconds > 0) {
            Text(
                text = "${call.durationSeconds / 60}:${(call.durationSeconds % 60).toString().padStart(2, '0')}",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun EmptyText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        lineHeight = 26.sp,
    )
}

@Composable
private fun SimUnlockStrip(
    message: String,
    onOpenSimSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.errorContainer,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = message,
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.onErrorContainer,
                style = MaterialTheme.typography.bodyLarge,
            )
            FilledTonalButton(onClick = onOpenSimSettings) {
                Text(stringResource(R.string.sim_unlock_action))
            }
        }
    }
}

@Composable
private fun PermissionStrip(
    permissions: CommunicationPermissions,
    onRequestPermissions: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val missing = buildList {
        if (!permissions.callGranted) add("hovory")
        if (!permissions.sendSmsGranted) add("SMS")
        if (!permissions.receiveSmsGranted) add("příjem SMS")
        if (!permissions.readSmsGranted) add("čtení schránky (RCS)")
        if (!permissions.callLogGranted) add("historie hovorů")
    }.joinToString(", ")
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.tertiaryContainer,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Chybí oprávnění: $missing",
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.onTertiaryContainer,
                style = MaterialTheme.typography.bodyLarge,
            )
            FilledTonalButton(onClick = onRequestPermissions) { Text("Povolit") }
            OutlinedButton(onClick = onOpenSettings) { Text("Nastavení") }
        }
    }
}

@Composable
private fun SystemHintStrip(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(12.dp),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.bodyMedium,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
    )
}

