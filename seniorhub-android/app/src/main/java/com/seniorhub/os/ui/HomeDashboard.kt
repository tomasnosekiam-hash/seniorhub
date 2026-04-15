package com.seniorhub.os.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.seniorhub.os.BuildConfig
import com.seniorhub.os.R
import com.seniorhub.os.data.Contact
import com.seniorhub.os.data.DeviceConfig
import com.seniorhub.os.data.DeviceMessage
import com.seniorhub.os.data.DeviceSettings
import com.seniorhub.os.matej.MatejForegroundService
import com.seniorhub.os.ui.theme.SeniorHubTheme
import com.seniorhub.os.util.normalizePhoneForDial
import java.time.LocalTime
import java.time.format.DateTimeFormatter

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
    showKioskLauncherHint: Boolean,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val familyMessagesPreview = remember(state.messages) {
        state.messages
            .filter { it.delivery.isNullOrBlank() }
            .take(5)
            .asReversed()
    }
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        if (state.loading) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = Color(0xFFFFFF00),
            )
        } else {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                StatusColumn(
                    device = state.device,
                    deviceConfig = state.deviceConfig,
                    weatherLine = state.weatherLine,
                    familyMessages = familyMessagesPreview,
                    showKioskLauncherHint = showKioskLauncherHint,
                    onShowPairing = onShowPairing,
                    modifier = Modifier
                        .weight(0.38f)
                        .fillMaxSize(),
                )
                ContactsColumn(
                    contacts = state.contacts,
                    onContactCall = onContactCall,
                    onContactSms = onContactSms,
                    onContactThread = onContactThread,
                    modifier = Modifier
                        .weight(0.62f)
                        .fillMaxSize(),
                )
            }
        }

        state.errorMessage?.let { msg ->
            Text(
                text = "Spojení: $msg",
                color = Color(0xFFFF6666),
                fontSize = 18.sp,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
            )
        }

        val alert = state.device?.alertMessage
        if (!alert.isNullOrBlank()) {
            AlertOverlay(message = alert, onDismiss = onDismissAlert)
        }

        val device = state.device
        if (device != null && state.showPairingSheet) {
            PairingOverlay(
                device = device,
                onRefreshPairing = onRefreshPairing,
                onClose = onHidePairing,
            )
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .size(100.dp)
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

        if (BuildConfig.DEBUG) {
            TextButton(
                onClick = { MatejForegroundService.triggerTestEmergency(context) },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(12.dp),
            ) {
                Text("Test nouze · DBG", color = Color(0xFFFF8888), fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun StatusColumn(
    device: DeviceSettings?,
    deviceConfig: DeviceConfig?,
    weatherLine: String?,
    familyMessages: List<DeviceMessage>,
    showKioskLauncherHint: Boolean,
    onShowPairing: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val seniorDisplay = listOfNotNull(
        deviceConfig?.seniorFirstName?.trim()?.takeIf { it.isNotEmpty() },
        deviceConfig?.seniorLastName?.trim()?.takeIf { it.isNotEmpty() },
    ).joinToString(" ")
    val address = deviceConfig?.addressLine?.trim().orEmpty()
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")),
            color = Color.White,
            fontSize = 72.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = device?.deviceLabel ?: "Tablet",
            color = Color(0xFFFFFF00),
            fontSize = 28.sp,
            fontWeight = FontWeight.Medium,
        )
        if (seniorDisplay.isNotBlank()) {
            Text(
                text = "Senior: $seniorDisplay",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Medium,
            )
        }
        if (address.isNotBlank()) {
            Text(
                text = address,
                color = Color.White.copy(alpha = 0.88f),
                fontSize = 18.sp,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            text = "ID zařízení: ${device?.deviceId ?: "—"}",
            color = Color.White.copy(alpha = 0.82f),
            fontSize = 18.sp,
        )
        Text(
            text = "Hlasitost (nastavení z webu): ${device?.volumePercent ?: "—"} %",
            color = Color.White,
            fontSize = 22.sp,
        )
        weatherLine?.let { line ->
            Text(
                text = line,
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 20.sp,
            )
        }
        device?.networkLabel?.let { net ->
            Text(
                text = "Síť: $net",
                color = Color.White.copy(alpha = 0.88f),
                fontSize = 18.sp,
            )
        }
        if (showKioskLauncherHint) {
            Text(
                text = stringResource(R.string.kiosk_home_hint),
                color = Color(0xFFFFCC66),
                fontSize = 16.sp,
                lineHeight = 22.sp,
            )
        }
        if (familyMessages.isNotEmpty()) {
            Text(
                text = "Vzkazy od rodiny (na celý tablet)",
                color = Color(0xFFFFFF00),
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
            )
            familyMessages.forEach { m ->
                Text(
                    text = "[Rodina] ${m.senderDisplayName ?: "—"}: ${m.body}",
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 16.sp,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                text = "U kontaktu klepni „Vlákno“ — zobrazí se odchozí zprávy (cloud i SMS).",
                color = Color.White.copy(alpha = 0.55f),
                fontSize = 14.sp,
            )
        }
        device?.batteryPercent?.let { pct ->
            Text(
                text = buildString {
                    append("Baterie: $pct %")
                    if (device.charging) append(" · nabíjení")
                },
                color = Color.White.copy(alpha = 0.92f),
                fontSize = 20.sp,
            )
        }
        if (device != null) {
            PairingSummaryCard(
                paired = device.paired,
                pairingCode = device.pairingCode,
                expiresAtLabel = device.pairingExpiresAtLabel,
                onShowPairing = onShowPairing,
            )
        }
    }
}

@Composable
private fun ContactsColumn(
    contacts: List<Contact>,
    onContactCall: (String) -> Unit,
    onContactSms: (Contact) -> Unit,
    onContactThread: (Contact) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = "Kontakty",
            color = Color(0xFFFFFF00),
            fontSize = 26.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(modifier = Modifier.height(12.dp))
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            if (contacts.isEmpty()) {
                item {
                    Text(
                        text = "Zatím žádné — přidej je z webové administrace.",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 20.sp,
                    )
                }
            } else {
                items(contacts, key = { it.id }) { c ->
                    ContactRow(
                        contact = c,
                        onCall = {
                            if (c.phone.isNotBlank()) onContactCall(c.phone)
                        },
                        onSms = { onContactSms(c) },
                        onThread = { onContactThread(c) },
                    )
                }
            }
        }
    }
}

@Composable
private fun ContactRow(
    contact: Contact,
    onCall: () -> Unit,
    onSms: () -> Unit,
    onThread: () -> Unit,
) {
    val canCommunicate = contact.phone.isNotBlank() && normalizePhoneForDial(contact.phone) != null
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (contact.isEmergency) Color(0xFF2A1518) else Color(0xFF111111),
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        if (contact.isEmergency) {
            Text(
                text = "NOUZE",
                color = Color(0xFFFF6666),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(4.dp))
        }
        Text(
            text = contact.name.ifEmpty { "(bez jména)" },
            color = Color.White,
            fontSize = 22.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = contact.phone.ifEmpty { "—" },
            color = Color.White.copy(alpha = 0.9f),
            fontSize = 18.sp,
        )
        if (canCommunicate) {
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Button(
                    onClick = onCall,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF2A2A2A),
                        contentColor = Color(0xFFFFFF00),
                    ),
                ) {
                    Text("Zavolat", fontSize = 18.sp)
                }
                Button(
                    onClick = onSms,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF2A2A2A),
                        contentColor = Color(0xFFFFFF00),
                    ),
                ) {
                    Text("SMS", fontSize = 18.sp)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = onThread,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF1A1A1A),
                    contentColor = Color.White.copy(alpha = 0.92f),
                ),
            ) {
                Text("Vlákno (zprávy s kontaktem)", fontSize = 17.sp)
            }
        }
    }
}
@Composable
private fun PairingSummaryCard(
    paired: Boolean,
    pairingCode: String?,
    expiresAtLabel: String?,
    onShowPairing: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF111111))
            .clickable(onClick = onShowPairing)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = if (paired) "Správci připojeni" else "Čeká na spárování",
            color = Color(0xFFFFFF00),
            fontSize = 22.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = if (paired) {
                "Klepni sem pro přidání dalšího správce."
            } else {
                "Použij kód ve webové administraci."
            },
            color = Color.White,
            fontSize = 18.sp,
        )
        if (!pairingCode.isNullOrBlank()) {
            Text(
                text = pairingCode,
                color = Color.White,
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        if (!expiresAtLabel.isNullOrBlank()) {
            Text(
                text = "Platí do $expiresAtLabel",
                color = Color.White.copy(alpha = 0.78f),
                fontSize = 16.sp,
            )
        }
    }
}

@Preview(name = "Tablet landscape", widthDp = 900, heightDp = 600)
@Composable
private fun HomeScreenPreview() {
    SeniorHubTheme {
        HomeScreen(
            state = HomeUiState(
                loading = false,
                device = DeviceSettings(
                    deviceId = "tablet-preview",
                    deviceLabel = "Babička — obývák",
                    volumePercent = 72,
                    alertMessage = null,
                    paired = true,
                    pairingCode = "R5K8P2",
                    pairingExpiresAtLabel = "18:40",
                ),
                contacts = listOf(
                    Contact("1", "Jana", "+420 777 111 222"),
                    Contact("2", "Petr", "+420 603 333 444"),
                ),
                errorMessage = null,
            ),
            onDismissAlert = {},
            onDismissUnreadMessage = {},
            onShowPairing = {},
            onHidePairing = {},
            onRefreshPairing = {},
            onKioskSecretTap = {},
            onDismissKioskUnlock = {},
            onSubmitKioskPin = {},
            onContactCall = {},
            onContactSms = {},
            onContactThread = {},
            showKioskLauncherHint = false,
        )
    }
}

@Preview(name = "Alert", widthDp = 900, heightDp = 600)
@Composable
private fun HomeScreenAlertPreview() {
    SeniorHubTheme {
        HomeScreen(
            state = HomeUiState(
                loading = false,
                device = DeviceSettings(
                    deviceId = "tablet-alert",
                    deviceLabel = "Tablet",
                    volumePercent = 40,
                    alertMessage = "Nezapomeň na léky.",
                    paired = true,
                    pairingCode = "A1B2C3",
                    pairingExpiresAtLabel = "19:10",
                ),
                contacts = emptyList(),
                errorMessage = null,
            ),
            onDismissAlert = {},
            onDismissUnreadMessage = {},
            onShowPairing = {},
            onHidePairing = {},
            onRefreshPairing = {},
            onKioskSecretTap = {},
            onDismissKioskUnlock = {},
            onSubmitKioskPin = {},
            onContactCall = {},
            onContactSms = {},
            onContactThread = {},
            showKioskLauncherHint = false,
        )
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun HomeScreenLoadingPreview() {
    SeniorHubTheme {
        HomeScreen(
            state = HomeUiState(loading = true),
            onDismissAlert = {},
            onDismissUnreadMessage = {},
            onShowPairing = {},
            onHidePairing = {},
            onRefreshPairing = {},
            onKioskSecretTap = {},
            onDismissKioskUnlock = {},
            onSubmitKioskPin = {},
            onContactCall = {},
            onContactSms = {},
            onContactThread = {},
            showKioskLauncherHint = false,
        )
    }
}
