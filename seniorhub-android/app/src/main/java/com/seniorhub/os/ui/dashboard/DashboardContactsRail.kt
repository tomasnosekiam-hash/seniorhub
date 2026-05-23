package com.seniorhub.os.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.seniorhub.os.data.Contact
import com.seniorhub.os.ui.theme.SeniorHubDesign

sealed interface ContactsRailSelection {
    data object All : ContactsRailSelection
    data class One(val contact: Contact) : ContactsRailSelection
}

@Composable
fun DashboardContactsRail(
    allItemsLabel: String,
    selected: ContactsRailSelection,
    contacts: List<Contact>,
    onSelect: (ContactsRailSelection) -> Unit,
    modifier: Modifier = Modifier,
) {
    val sorted = remember(contacts) {
        contacts.sortedBy { it.name.ifBlank { it.phone }.lowercase() }
    }
    Column(
        modifier = modifier
            .fillMaxHeight()
            .padding(top = 96.dp, start = 8.dp, end = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = "Kontakty",
            fontSize = 24.sp,
            letterSpacing = (-0.16).sp,
            color = SeniorHubDesign.MenuInactive,
            textAlign = TextAlign.Start,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
        )
        val allSelected = selected is ContactsRailSelection.All
        RailRow(
            label = allItemsLabel,
            selected = allSelected,
            onClick = { onSelect(ContactsRailSelection.All) },
        )
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.weight(1f),
        ) {
            items(sorted, key = { it.id }) { contact ->
                val label = contactSortLabel(contact)
                val isSelected = (selected as? ContactsRailSelection.One)?.contact?.id == contact.id
                RailRow(
                    label = label,
                    selected = isSelected,
                    onClick = { onSelect(ContactsRailSelection.One(contact)) },
                )
            }
        }
    }
}

@Composable
private fun RailRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val bg = if (selected) SeniorHubDesign.MessageReadSurface else androidx.compose.ui.graphics.Color.Transparent
    val color = if (selected) SeniorHubDesign.AccentGold else SeniorHubDesign.MenuInactive
    Text(
        text = label,
        fontSize = if (selected) 18.sp else 18.sp,
        letterSpacing = (-0.16).sp,
        color = color,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    )
}

private fun contactSortLabel(contact: Contact): String {
    val name = contact.name.ifBlank { contact.phone }
    val letter = name.firstOrNull()?.uppercaseChar() ?: '?'
    return "$letter  $name"
}
