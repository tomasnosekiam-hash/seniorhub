package com.seniorhub.os.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.seniorhub.os.data.Contact
import com.seniorhub.os.data.DeviceMessage
import com.seniorhub.os.ui.components.DashboardContactAvatar
import com.seniorhub.os.ui.components.DashboardMessageCard
import com.seniorhub.os.ui.theme.SeniorHubDesign
import com.seniorhub.os.util.CallHistoryEntry
import com.seniorhub.os.util.CallType
import com.seniorhub.os.util.belongsToContactThread
import com.seniorhub.os.util.isDeviceOutboundDelivery
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun DashboardMessagesScreen(
    messages: List<DeviceMessage>,
    contacts: List<Contact>,
    selected: ContactsRailSelection,
    onSelect: (ContactsRailSelection) -> Unit,
    onMessageClick: (DeviceMessage) -> Unit,
    onMessageCardBounds: (String, Rect) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(32.dp),
    ) {
        DashboardContactsRail(
            allItemsLabel = "Všechny zprávy",
            selected = selected,
            contacts = contacts,
            onSelect = onSelect,
            modifier = Modifier.width(240.dp),
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(top = 96.dp, bottom = 16.dp),
        ) {
            val title = when (selected) {
                ContactsRailSelection.All -> "Všechny zprávy"
                is ContactsRailSelection.One -> selected.contact.name.ifBlank { selected.contact.phone }
            }
            Text(
                text = title,
                fontSize = 24.sp,
                letterSpacing = (-0.16).sp,
                color = SeniorHubDesign.MenuInactive,
                modifier = Modifier.padding(bottom = 24.dp),
            )
            val visible = remember(messages, selected) {
                when (selected) {
                    ContactsRailSelection.All -> messages
                    is ContactsRailSelection.One ->
                        messages.filter { it.belongsToContactThread(selected.contact) }
                }
            }
            if (selected is ContactsRailSelection.One) {
                ConversationColumn(
                    messages = visible,
                    contact = selected.contact,
                    onMessageClick = onMessageClick,
                    onMessageCardBounds = onMessageCardBounds,
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    if (visible.isEmpty()) {
                        item {
                            Text(
                                "Zatím žádná zpráva.",
                                color = SeniorHubDesign.MenuInactive,
                                fontSize = 18.sp,
                            )
                        }
                    } else {
                        items(visible.take(20), key = { it.id }) { message ->
                            DashboardMessageCard(
                                message = message,
                                contacts = contacts,
                                onClick = { onMessageClick(message) },
                                modifier = Modifier.onGloballyPositioned { c ->
                                    onMessageCardBounds(message.id, c.boundsInRoot())
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ConversationColumn(
    messages: List<DeviceMessage>,
    contact: Contact,
    onMessageClick: (DeviceMessage) -> Unit,
    onMessageCardBounds: (String, Rect) -> Unit,
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        if (messages.isEmpty()) {
            item {
                Text(
                    "Zatím žádná zpráva s tímto kontaktem.",
                    color = SeniorHubDesign.MenuInactive,
                    fontSize = 18.sp,
                )
            }
        } else {
            items(messages, key = { it.id }) { message ->
                val outbound = isDeviceOutboundDelivery(message.delivery)
                if (outbound) {
                    OutboundBubble(message = message)
                } else {
                    DashboardMessageCard(
                        message = message,
                        contact = contact,
                        onClick = { onMessageClick(message) },
                        modifier = Modifier
                            .widthIn(max = 520.dp)
                            .onGloballyPositioned { c ->
                                onMessageCardBounds(message.id, c.boundsInRoot())
                            },
                    )
                }
            }
        }
    }
}

@Composable
private fun OutboundBubble(message: DeviceMessage) {
    Column(
        modifier = Modifier
            .fillMaxWidth(0.85f)
            .clip(RoundedCornerShape(16.dp))
            .background(SeniorHubDesign.MessageReadSurface)
            .padding(horizontal = 24.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.End,
    ) {
        Text(
            text = message.body,
            style = SeniorHubDesign.messageBodyStyle(read = true),
        )
        Text(
            text = "Ty · ${formatShortTime(message.createdAt)}",
            fontSize = 13.sp,
            color = SeniorHubDesign.MenuInactive,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

@Composable
fun DashboardCallsScreen(
    callHistory: List<CallHistoryEntry>,
    contacts: List<Contact>,
    selected: ContactsRailSelection,
    onSelect: (ContactsRailSelection) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(32.dp),
    ) {
        DashboardContactsRail(
            allItemsLabel = "Všechna volání",
            selected = selected,
            contacts = contacts,
            onSelect = onSelect,
            modifier = Modifier.width(240.dp),
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(top = 96.dp, bottom = 16.dp),
        ) {
            val title = when (selected) {
                ContactsRailSelection.All -> "Všechna volání"
                is ContactsRailSelection.One -> selected.contact.name.ifBlank { selected.contact.phone }
            }
            Text(
                text = title,
                fontSize = 24.sp,
                color = SeniorHubDesign.MenuInactive,
                modifier = Modifier.padding(bottom = 24.dp),
            )
            val visible = remember(callHistory, selected) {
                when (selected) {
                    ContactsRailSelection.All -> callHistory
                    is ContactsRailSelection.One ->
                        callHistory.filter { it.belongsTo(selected.contact) }
                }
            }
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (visible.isEmpty()) {
                    item {
                        Text("Zatím žádné hovory.", color = SeniorHubDesign.MenuInactive, fontSize = 18.sp)
                    }
                } else {
                    items(visible.take(30), key = { it.id }) { call ->
                        CallLogCard(call = call, contacts = contacts, highlight = call.type == CallType.Incoming)
                    }
                }
            }
        }
    }
}

@Composable
private fun CallLogCard(
    call: CallHistoryEntry,
    contacts: List<Contact>,
    highlight: Boolean,
) {
    val contact = contacts.firstOrNull { call.belongsTo(it) }
    val name = contact?.name ?: call.cachedName ?: call.phone.ifBlank { "Neznámé" }
    val surface = if (highlight) SeniorHubDesign.MessageSurface else SeniorHubDesign.MessageReadSurface
    val titleColor = if (highlight) SeniorHubDesign.Black else SeniorHubDesign.WeatherText
    val subColor = if (highlight) SeniorHubDesign.Black.copy(alpha = 0.75f) else SeniorHubDesign.MessageReadText
    val duration = if (call.durationSeconds > 0) {
        val sec = call.durationSeconds
        if (sec >= 60) "${sec / 60} min" else "$sec s"
    } else {
        "—"
    }
    val time = formatCallMillis(call.startedAtMillis)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(surface)
            .padding(horizontal = 24.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = "${call.type.label()} · $name",
            fontSize = 20.sp,
            color = titleColor,
        )
        Text(
            text = "$time · $duration",
            fontSize = 14.sp,
            color = subColor,
        )
    }
}

private fun CallType.label(): String = when (this) {
    CallType.Incoming -> "Příchozí"
    CallType.Outgoing -> "Odchozí"
    CallType.Missed -> "Zmeškaný"
    CallType.Other -> "Hovor"
}

@Composable
fun DashboardContactsScreen(
    contacts: List<Contact>,
    onEdit: (Contact) -> Unit,
    onSms: (Contact) -> Unit,
    onCall: (Contact) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(top = 96.dp, start = 8.dp, end = 8.dp, bottom = 16.dp),
    ) {
        Text(
            text = "Kontakty v adresáři",
            fontSize = 24.sp,
            color = SeniorHubDesign.MenuInactive,
            modifier = Modifier.padding(bottom = 24.dp),
        )
        val rows = remember(contacts) { contacts.chunked(3) }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(20.dp)) {
            items(rows) { row ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    row.forEach { contact ->
                        ContactDirectoryCard(
                            contact = contact,
                            onEdit = { onEdit(contact) },
                            onSms = { onSms(contact) },
                            onCall = { onCall(contact) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    repeat(3 - row.size) {
                        Box(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun ContactDirectoryCard(
    contact: Contact,
    onEdit: () -> Unit,
    onSms: () -> Unit,
    onCall: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val pill = RoundedCornerShape(64.dp)
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(SeniorHubDesign.MessageReadSurface)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        DashboardContactAvatar(
            contact = contact,
            fallbackLabel = contact.name.ifBlank { contact.phone },
            modifier = Modifier
                .width(64.dp)
                .height(76.dp),
        )
        Text(
            text = contact.name.ifBlank { contact.phone },
            fontSize = 22.sp,
            color = SeniorHubDesign.WeatherText,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            ContactActionChip("Editovat", onEdit, modifier = Modifier.weight(1f))
            ContactActionChip("SMS", onSms, modifier = Modifier.weight(1f))
            ContactActionChip("Volat", onCall, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun ContactActionChip(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = 40.dp),
        shape = RoundedCornerShape(64.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, SeniorHubDesign.ReplyInputBorder),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
    ) {
        Text(label, fontSize = 13.sp, color = SeniorHubDesign.ReplyInputText, maxLines = 1)
    }
}

data class FamilyPhotoItem(
    val id: String,
    val title: String,
    val description: String,
    val color: Color,
    val heightDp: Int,
)

private val demoFamilyPhotos = listOf(
    FamilyPhotoItem("1", "Zahrada", "Adéla s vnoučaty na zahradě — léto 2024.", Color(0xFF4A6741), 280),
    FamilyPhotoItem("2", "Narozeniny", "Oslava narozenin v obýváku.", Color(0xFF6B5344), 220),
    FamilyPhotoItem("3", "Hřiště", "Vnoučata na hřišti u domu.", Color(0xFF3D5C6E), 320),
    FamilyPhotoItem("4", "Čaj", "Čaj o páté u babičky.", Color(0xFF5C4A3A), 240),
    FamilyPhotoItem("5", "Svatba", "Stará fotografie ze svatby.", Color(0xFF4A4A5C), 260),
    FamilyPhotoItem("6", "Vánoce", "Stromeček a dárky.", Color(0xFF2E4A3E), 200),
)

@Composable
fun DashboardPhotosScreen(
    onPhotoClick: (FamilyPhotoItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(top = 96.dp, bottom = 16.dp),
    ) {
        Text(
            text = "Rodinné fotky",
            fontSize = 24.sp,
            color = SeniorHubDesign.MenuInactive,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp),
        )
        LazyVerticalStaggeredGrid(
            columns = StaggeredGridCells.Fixed(3),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalItemSpacing = 16.dp,
            modifier = Modifier.fillMaxSize(),
        ) {
            items(demoFamilyPhotos, key = { it.id }) { photo ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(photo.heightDp.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(photo.color)
                        .clickable { onPhotoClick(photo) },
                    contentAlignment = Alignment.BottomStart,
                ) {
                    Text(
                        text = photo.title,
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }
        }
    }
}

private fun formatShortTime(createdAt: com.google.firebase.Timestamp?): String {
    if (createdAt == null) return "Teď"
    return Instant.ofEpochMilli(createdAt.toDate().time)
        .atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("d.M. HH:mm"))
}

private fun formatCallMillis(millis: Long): String {
    val zdt = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault())
    val today = java.time.LocalDate.now()
    val date = zdt.toLocalDate()
    val time = zdt.format(DateTimeFormatter.ofPattern("HH:mm"))
    return when {
        date == today -> "Dnes $time"
        date == today.minusDays(1) -> "Včera $time"
        else -> zdt.format(DateTimeFormatter.ofPattern("d.M. HH:mm"))
    }
}

private fun CallHistoryEntry.belongsTo(contact: Contact): Boolean =
    com.seniorhub.os.util.phonesMatchForThread(phone, contact.phone)
