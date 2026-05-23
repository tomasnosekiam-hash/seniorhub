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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.NightsStay
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.sp
import com.seniorhub.os.data.Contact
import com.seniorhub.os.data.DayNightWeather
import com.seniorhub.os.data.DeviceMessage
import com.seniorhub.os.ui.components.DashboardContactAvatar
import com.seniorhub.os.ui.components.DashboardMessageCard
import com.seniorhub.os.ui.theme.SeniorHubDesign
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun DashboardLeftPanel(
    clock: LocalTime,
    activeDestination: DashboardMenuDestination = DashboardMenuDestination.Home,
    onMenuSelect: (DashboardMenuDestination) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val today = LocalDate.now()
    val dateLabel = today.format(DateTimeFormatter.ofPattern("d.M.yyyy"))
    val dayLabel = today.format(DateTimeFormatter.ofPattern("EEEE", Locale("cs", "CZ")))
        .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale("cs", "CZ")) else it.toString() }

    Column(
        modifier = modifier
            .width(220.dp)
            .fillMaxHeight()
            .padding(24.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = clock.format(DateTimeFormatter.ofPattern("HH:mm")),
                fontSize = 64.sp,
                fontWeight = FontWeight.Normal,
                letterSpacing = (-0.16).sp,
                color = SeniorHubDesign.AccentGold,
                maxLines = 1,
            )
            Text(
                text = dateLabel,
                fontSize = 20.sp,
                color = SeniorHubDesign.AccentGold,
                letterSpacing = (-0.16).sp,
            )
            Text(
                text = dayLabel,
                fontSize = 20.sp,
                color = SeniorHubDesign.AccentGold,
                letterSpacing = (-0.16).sp,
            )
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(24.dp),
            horizontalAlignment = Alignment.Start,
        ) {
            DashboardMenuDestination.entries.forEach { dest ->
                DashboardMenuItem(
                    label = dest.label,
                    active = dest == activeDestination,
                    enabled = true,
                    onClick = { onMenuSelect(dest) },
                )
            }
        }

        Text(
            text = "Nastavení",
            fontSize = 16.sp,
            color = SeniorHubDesign.MenuInactive,
            letterSpacing = (-0.16).sp,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun DashboardMenuItem(
    label: String,
    active: Boolean,
    enabled: Boolean = true,
    onClick: () -> Unit = {},
) {
    Text(
        text = label,
        fontSize = 24.sp,
        letterSpacing = (-0.16).sp,
        textAlign = TextAlign.Start,
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (enabled) Modifier.clickable(onClick = onClick) else Modifier,
            ),
        color = when {
            active -> SeniorHubDesign.AccentGold
            enabled -> SeniorHubDesign.MenuInactive
            else -> SeniorHubDesign.MenuInactive.copy(alpha = 0.55f)
        },
    )
}

@Composable
fun DashboardCenterPanel(
    messages: List<DeviceMessage>,
    contacts: List<Contact>,
    onMessageClick: (DeviceMessage) -> Unit,
    onMessageCardBounds: (String, Rect) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .width(383.dp)
            .fillMaxHeight()
            .padding(top = 96.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = "Nové zprávy a volání",
            fontSize = 24.sp,
            letterSpacing = (-0.16).sp,
            color = SeniorHubDesign.MenuInactive,
            textAlign = TextAlign.Start,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
        )
        if (messages.isEmpty()) {
            Text(
                text = "Zatím žádná zpráva.",
                fontSize = 18.sp,
                color = SeniorHubDesign.MenuInactive,
                modifier = Modifier.padding(horizontal = 8.dp),
            )
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 16.dp),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .fillMaxHeight(),
            ) {
                items(messages.take(12), key = { it.id }) { message ->
                    DashboardMessageCard(
                        message = message,
                        contacts = contacts,
                        onClick = { onMessageClick(message) },
                        modifier = Modifier.onGloballyPositioned { coordinates ->
                            onMessageCardBounds(message.id, coordinates.boundsInRoot())
                        },
                    )
                }
            }
        }
    }
}

@Composable
fun DashboardRightPanel(
    weather: DayNightWeather?,
    contacts: List<Contact>,
    onContactClick: (Contact) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .width(383.dp)
            .fillMaxHeight()
            .padding(top = 96.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        DashboardWeatherCard(weather = weather)
        if (contacts.isNotEmpty()) {
            DashboardFrequentContactsCard(
                contacts = contacts,
                onContactClick = onContactClick,
            )
        }
    }
}

@Composable
private fun DashboardWeatherCard(weather: DayNightWeather?) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SeniorHubDesign.WeatherSurface)
            .padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Text(
            text = "Počasí dnes a v noci",
            fontSize = 14.sp,
            color = SeniorHubDesign.WeatherText,
            letterSpacing = (-0.14).sp,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(34.dp),
        ) {
            WeatherTempColumn(
                icon = {
                    Icon(
                        Icons.Outlined.WbSunny,
                        contentDescription = null,
                        tint = SeniorHubDesign.WeatherSun,
                        modifier = Modifier.size(22.dp),
                    )
                },
                temp = weather?.afternoonTempC?.let { "$it°C" } ?: "—",
                label = "Odpoledne",
            )
            WeatherTempColumn(
                icon = {
                    Icon(
                        Icons.Outlined.NightsStay,
                        contentDescription = null,
                        tint = SeniorHubDesign.WeatherMoon,
                        modifier = Modifier.size(22.dp),
                    )
                },
                temp = weather?.nightTempC?.let { "$it°C" } ?: "—",
                label = "V noci",
            )
        }
    }
}

@Composable
private fun WeatherTempColumn(
    icon: @Composable () -> Unit,
    temp: String,
    label: String,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.width(80.dp),
    ) {
        Box(modifier = Modifier.height(25.dp), contentAlignment = Alignment.CenterStart) {
            icon()
        }
        Text(
            text = temp,
            fontSize = 32.sp,
            color = SeniorHubDesign.WeatherText,
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = SeniorHubDesign.WeatherText,
        )
    }
}

@Composable
private fun DashboardFrequentContactsCard(
    contacts: List<Contact>,
    onContactClick: (Contact) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SeniorHubDesign.ContactsSurface)
            .padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "Nejčastější kontakty",
            fontSize = 14.sp,
            color = SeniorHubDesign.ContactsTitle,
            letterSpacing = (-0.14).sp,
        )
        contacts.take(4).forEach { contact ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onContactClick(contact) }
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                DashboardContactAvatar(
                    contact = contact,
                    fallbackLabel = contact.name.ifBlank { contact.phone },
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = contact.name.ifBlank { contact.phone },
                        fontSize = 18.sp,
                        color = SeniorHubDesign.WeatherText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Icon(
                    Icons.Outlined.Call,
                    contentDescription = "Zavolat",
                    tint = SeniorHubDesign.AccentGold,
                    modifier = Modifier.size(28.dp),
                )
            }
        }
    }
}
