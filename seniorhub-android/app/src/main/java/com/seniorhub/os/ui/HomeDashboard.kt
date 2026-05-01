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
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.seniorhub.os.util.CallHistoryEntry
import com.seniorhub.os.util.CallType
import com.seniorhub.os.util.belongsToContactThread
import com.seniorhub.os.util.normalizePhoneForDial
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

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

private fun CallHistoryEntry.belongsTo(contact: Contact): Boolean {
    val call = normalizePhoneForDial(phone) ?: return false
    val other = normalizePhoneForDial(contact.phone) ?: return false
    return call == other
}

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
    onSendContactMessage: (Contact, String, (Result<Unit>) -> Unit) -> Unit,
    showKioskLauncherHint: Boolean,
    communicationPermissions: CommunicationPermissions = CommunicationPermissions.AllGranted,
    onRequestCommunicationPermissions: () -> Unit = {},
    onOpenAppSettings: () -> Unit = {},
    onAddContact: () -> Unit = {},
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
                        communicationPermissions = communicationPermissions,
                        onRequestCommunicationPermissions = onRequestCommunicationPermissions,
                        onOpenAppSettings = onOpenAppSettings,
                        onShowPairing = onShowPairing,
                        onContactCall = onContactCall,
                        onContactSms = onContactSms,
                        onContactThread = onContactThread,
                        onSendContactMessage = onSendContactMessage,
                        onAddContact = onAddContact,
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
                    .clickable(onClick = onKioskSecretTap),
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
    communicationPermissions: CommunicationPermissions,
    onRequestCommunicationPermissions: () -> Unit,
    onOpenAppSettings: () -> Unit,
    onShowPairing: () -> Unit,
    onContactCall: (String) -> Unit,
    onContactSms: (Contact) -> Unit,
    onContactThread: (Contact) -> Unit,
    onSendContactMessage: (Contact, String, (Result<Unit>) -> Unit) -> Unit,
    onAddContact: () -> Unit,
) {
    var selectedTab by remember { mutableStateOf(SeniorTab.Home) }
    var selectedMessage by remember { mutableStateOf<DeviceMessage?>(null) }
    var callFilter by remember { mutableStateOf<ContactFilter>(ContactFilter.All) }
    var messageFilter by remember { mutableStateOf<ContactFilter>(ContactFilter.All) }
    var clock by remember { mutableStateOf(LocalTime.now()) }
    LaunchedEffect(Unit) {
        while (isActive) {
            clock = LocalTime.now()
            delay(60_000L)
        }
    }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(18.dp),
        horizontalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        SeniorMenu(
            selected = selectedTab,
            onSelected = { selectedTab = it },
            device = state.device,
            config = state.deviceConfig,
            clock = clock,
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(start = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (!communicationPermissions.allGranted) {
                PermissionStrip(
                    permissions = communicationPermissions,
                    onRequestPermissions = onRequestCommunicationPermissions,
                    onOpenSettings = onOpenAppSettings,
                )
            }
            if (showKioskLauncherHint) {
                SystemHintStrip(text = stringResource(R.string.kiosk_home_hint), onClick = onShowPairing)
            }
            when (selectedTab) {
                SeniorTab.Home -> HomeTab(
                    state = state,
                    onMessageClick = { selectedMessage = it },
                    onContactCall = onContactCall,
                    onContactSms = onContactSms,
                )
                SeniorTab.Calls -> CallsTab(
                    contacts = state.contacts,
                    callHistory = state.callHistory,
                    selected = callFilter,
                    onSelected = { callFilter = it },
                    onAddContact = onAddContact,
                    onCall = onContactCall,
                )
                SeniorTab.Messages -> MessagesTab(
                    contacts = state.contacts,
                    messages = state.messages,
                    selected = messageFilter,
                    onSelected = { messageFilter = it },
                    onAddContact = onAddContact,
                    onOpenThread = onContactThread,
                    onSendContactMessage = onSendContactMessage,
                )
            }
        }
    }

    selectedMessage?.let { msg ->
        MessageDetailOverlay(
            message = msg,
            replyContact = state.contacts.firstOrNull { msg.belongsToContactThread(it) },
            onDismiss = { selectedMessage = null },
            onReply = { contact ->
                selectedMessage = null
                onContactSms(contact)
            },
        )
    }
}

@Composable
private fun SeniorMenu(
    selected: SeniorTab,
    onSelected: (SeniorTab) -> Unit,
    device: DeviceSettings?,
    config: DeviceConfig?,
    clock: LocalTime,
) {
    val scheme = MaterialTheme.colorScheme
    val seniorName = listOfNotNull(
        config?.seniorFirstName?.trim()?.takeIf { it.isNotEmpty() },
        config?.seniorLastName?.trim()?.takeIf { it.isNotEmpty() },
    ).joinToString(" ").ifBlank { device?.deviceLabel ?: stringResource(R.string.device_label_fallback) }
    NavigationRail(
        modifier = Modifier
            .width(170.dp)
            .fillMaxHeight(),
        containerColor = scheme.surfaceContainer,
        contentColor = scheme.onSurface,
        header = {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = clock.format(DateTimeFormatter.ofPattern("HH:mm")),
                style = MaterialTheme.typography.headlineLarge,
                color = scheme.onSurface,
                maxLines = 1,
            )
            Text(
                text = seniorName,
                style = MaterialTheme.typography.labelLarge,
                color = scheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        },
    ) {
            Spacer(Modifier.height(12.dp))
            MenuButton(
                label = "Domů",
                icon = { Icon(Icons.Outlined.Home, contentDescription = null) },
                selected = selected == SeniorTab.Home,
                onClick = { onSelected(SeniorTab.Home) },
            )
            MenuButton(
                label = "Volání",
                icon = { Icon(Icons.Outlined.Call, contentDescription = null) },
                selected = selected == SeniorTab.Calls,
                onClick = { onSelected(SeniorTab.Calls) },
            )
            MenuButton(
                label = "Zprávy",
                icon = { Icon(Icons.Outlined.ChatBubbleOutline, contentDescription = null) },
                selected = selected == SeniorTab.Messages,
                onClick = { onSelected(SeniorTab.Messages) },
            )
            Spacer(Modifier.weight(1f))
            Column(
                modifier = Modifier.padding(bottom = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
            device?.batteryPercent?.let { pct ->
                Text(
                    text = if (device.charging) "$pct % · nabíjí" else "$pct %",
                    style = MaterialTheme.typography.labelLarge,
                    color = scheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun MenuButton(
    label: String,
    icon: @Composable () -> Unit,
    selected: Boolean,
    onClick: () -> Unit,
) {
    NavigationRailItem(
        selected = selected,
        onClick = onClick,
        icon = icon,
        label = { Text(label) },
        alwaysShowLabel = true,
    )
}

@Composable
private fun HomeTab(
    state: HomeUiState,
    onMessageClick: (DeviceMessage) -> Unit,
    onContactCall: (String) -> Unit,
    onContactSms: (Contact) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        RecentActivityColumn(
            messages = state.messages,
            contacts = state.contacts,
            onMessageClick = onMessageClick,
            modifier = Modifier.weight(1.08f),
        )
        HomeSideColumn(
            weatherLine = state.weatherLine,
            contacts = state.contacts,
            onContactCall = onContactCall,
            onContactSms = onContactSms,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun RecentActivityColumn(
    messages: List<DeviceMessage>,
    contacts: List<Contact>,
    onMessageClick: (DeviceMessage) -> Unit,
    modifier: Modifier = Modifier,
) {
    val recent = messages.take(8)
    SectionSurface(modifier = modifier.fillMaxHeight()) {
        Text("Co je nového", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(12.dp))
        if (recent.isEmpty()) {
            EmptyText("Zatím žádná zpráva.")
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(recent, key = { it.id }) { message ->
                    LargeMessagePreview(
                        message = message,
                        contacts = contacts,
                        onClick = { onMessageClick(message) },
                    )
                }
                item { HorizontalDivider(color = MaterialTheme.colorScheme.surfaceContainerHigh) }
                items(contacts.take(3), key = { "call-${it.id}" }) { contact ->
                    MiniCallRow(contact)
                }
            }
        }
    }
}

@Composable
private fun LargeMessagePreview(
    message: DeviceMessage,
    contacts: List<Contact>,
    onClick: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val contact = contacts.firstOrNull { message.belongsToContactThread(it) }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        color = scheme.surfaceContainerHigh,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            if (contact != null) {
                ContactAvatar(contact = contact, selected = false, modifier = Modifier.size(48.dp))
            } else {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("R", style = MaterialTheme.typography.titleLarge, color = Color.White)
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = message.senderDisplayName
                        ?: message.inboundFromName
                        ?: message.outboundName
                        ?: "Rodina",
                    style = MaterialTheme.typography.labelLarge,
                    color = scheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = message.body,
                    style = MaterialTheme.typography.titleLarge.copy(fontSize = 24.sp, lineHeight = 31.sp),
                    color = scheme.onSurface,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = formatMessageTime(message.createdAt),
                    style = MaterialTheme.typography.labelMedium,
                    color = scheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun MiniCallRow(contact: Contact) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = Color.Transparent,
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ContactAvatar(contact = contact, selected = false)
            Column {
                Text(contact.name.ifBlank { contact.phone }, style = MaterialTheme.typography.titleMedium)
                Text("Kontakt pro volání", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun HomeSideColumn(
    weatherLine: String?,
    contacts: List<Contact>,
    onContactCall: (String) -> Unit,
    onContactSms: (Contact) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        SectionSurface(modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Icons.Outlined.WbSunny, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                Text("Počasí", style = MaterialTheme.typography.titleLarge)
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = weatherLine ?: "Počasí se načítá.",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = LocalDate.now().format(DateTimeFormatter.ofPattern("d.M.yyyy")),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        SectionSurface(modifier = Modifier.fillMaxWidth()) {
            Text("Nejčastější kontakty", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(12.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                items(contacts.take(6), key = { it.id }) { contact ->
                    QuickContact(contact = contact, onCall = onContactCall, onSms = onContactSms)
                }
            }
        }
        SectionSurface(modifier = Modifier.weight(1f).fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Icons.Outlined.Image, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text("Nejnovější fotky", style = MaterialTheme.typography.titleLarge)
            }
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                repeat(3) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White.copy(alpha = 0.07f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Outlined.Image, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f))
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickContact(
    contact: Contact,
    onCall: (String) -> Unit,
    onSms: (Contact) -> Unit,
) {
    Column(
        modifier = Modifier.width(108.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ContactAvatar(
            contact = contact,
            selected = false,
            modifier = Modifier
                .size(70.dp)
                .clickable { if (contact.phone.isNotBlank()) onCall(contact.phone) },
        )
        Text(
            text = contact.name.ifBlank { contact.phone },
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        OutlinedButton(
            onClick = { onSms(contact) },
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
            Icon(Icons.Outlined.ChatBubbleOutline, contentDescription = null, modifier = Modifier.size(16.dp))
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
    onSendContactMessage: (Contact, String, (Result<Unit>) -> Unit) -> Unit,
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
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
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
}

@Composable
private fun MessageComposer(
    contact: Contact,
    onOpenThread: () -> Unit,
    onSend: (Contact, String, (Result<Unit>) -> Unit) -> Unit,
) {
    var body by remember(contact.id) { mutableStateOf("") }
    var error by remember(contact.id) { mutableStateOf<String?>(null) }
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
                            onSend(contact, text) { result ->
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
}

@Composable
private fun SectionSurface(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            content = content,
        )
    }
}

@Composable
private fun ContentSurface(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            content = content,
        )
    }
}

@Composable
private fun PeopleSurface(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Row(modifier = modifier.background(MaterialTheme.colorScheme.background)) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(14.dp),
            content = content,
        )
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(1.dp)
                .background(MaterialTheme.colorScheme.outlineVariant),
        )
    }
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
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(14.dp),
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
private fun PermissionStrip(
    permissions: CommunicationPermissions,
    onRequestPermissions: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val missing = buildList {
        if (!permissions.callGranted) add("hovory")
        if (!permissions.sendSmsGranted) add("SMS")
        if (!permissions.receiveSmsGranted) add("příjem SMS")
        if (!permissions.callLogGranted) add("historie hovorů")
    }.joinToString(", ")
    Surface(
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
private fun SystemHintStrip(text: String, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(12.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun MessageDetailOverlay(
    message: DeviceMessage,
    replyContact: Contact?,
    onDismiss: () -> Unit,
    onReply: (Contact) -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xDD000000)),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.72f),
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
            Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = message.senderDisplayName ?: message.inboundFromName ?: "Zpráva",
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Outlined.Close, contentDescription = "Zavřít")
                    }
                }
                Text(
                    text = message.body,
                    style = MaterialTheme.typography.headlineMedium.copy(lineHeight = 40.sp),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = formatMessageTime(message.createdAt),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("Zavřít")
                    }
                    Button(
                        enabled = replyContact != null,
                        onClick = { replyContact?.let(onReply) },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Odpovědět")
                    }
                }
            }
        }
    }
}
