package com.seniorhub.os.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.seniorhub.os.data.Contact
import com.seniorhub.os.data.DeviceConfig
import com.seniorhub.os.data.DeviceMessage
import com.seniorhub.os.data.DeviceSettings
import com.seniorhub.os.ui.theme.SeniorHubTheme
import com.seniorhub.os.util.SmsSender
import com.seniorhub.os.util.normalizePhoneForDial
import com.seniorhub.os.util.openDialPad
import com.seniorhub.os.util.startOutgoingCall
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@Composable
fun HomeRoute(
    viewModel: HomeViewModel,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var pendingCallPhone by remember { mutableStateOf<String?>(null) }
    var smsTarget by remember { mutableStateOf<Contact?>(null) }
    var smsSendError by remember { mutableStateOf<String?>(null) }
    var pendingSms by remember { mutableStateOf<Pair<String, String>?>(null) }
    val callPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        val raw = pendingCallPhone
        pendingCallPhone = null
        if (raw == null) return@rememberLauncherForActivityResult
        if (granted) {
            context.startOutgoingCall(raw)
        } else {
            context.openDialPad(raw)
        }
    }
    val smsPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        val pending = pendingSms
        pendingSms = null
        if (pending == null) return@rememberLauncherForActivityResult
        if (granted) {
            SmsSender.send(context, pending.first, pending.second).fold(
                onSuccess = {
                    smsTarget = null
                    smsSendError = null
                },
                onFailure = { e ->
                    smsSendError = e.message ?: "Odeslání se nezdařilo."
                },
            )
        } else {
            smsSendError = "Bez oprávnění k SMS nelze odeslat."
        }
    }
    Box(modifier = modifier) {
        HomeScreen(
            state = state,
            onDismissAlert = viewModel::dismissAlert,
            onDismissUnreadMessage = viewModel::dismissUnreadMessage,
            onShowPairing = viewModel::showPairingSheet,
            onHidePairing = viewModel::hidePairingSheet,
            onRefreshPairing = { viewModel.refreshPairingCode(force = true) },
            onKioskSecretTap = viewModel::onKioskSecretTap,
            onDismissKioskUnlock = viewModel::dismissKioskUnlock,
            onSubmitKioskPin = { pin ->
                if (viewModel.tryUnlockWithPin(pin)) {
                    context.startActivity(
                        Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                    )
                }
            },
            onContactCall = { rawPhone ->
                if (normalizePhoneForDial(rawPhone) != null) {
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) ==
                        PackageManager.PERMISSION_GRANTED
                    ) {
                        context.startOutgoingCall(rawPhone)
                    } else {
                        pendingCallPhone = rawPhone
                        callPermissionLauncher.launch(Manifest.permission.CALL_PHONE)
                    }
                }
            },
            onContactSms = { contact ->
                smsSendError = null
                smsTarget = contact
            },
            modifier = Modifier.fillMaxSize(),
        )
        smsTarget?.let { contact ->
            SmsComposeOverlay(
                contact = contact,
                errorMessage = smsSendError,
                onDismiss = {
                    smsTarget = null
                    smsSendError = null
                    pendingSms = null
                },
                onSend = { body ->
                    smsSendError = null
                    when {
                        normalizePhoneForDial(contact.phone) == null -> {
                            smsSendError = "Neplatné telefonní číslo."
                        }
                        ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) ==
                            PackageManager.PERMISSION_GRANTED -> {
                            SmsSender.send(context, contact.phone, body).fold(
                                onSuccess = {
                                    smsTarget = null
                                },
                                onFailure = { e ->
                                    smsSendError = e.message ?: "Odeslání se nezdařilo."
                                },
                            )
                        }
                        else -> {
                            pendingSms = contact.phone to body
                            smsPermissionLauncher.launch(Manifest.permission.SEND_SMS)
                        }
                    }
                },
            )
        }
    }
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
    modifier: Modifier = Modifier,
) {
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
                    onShowPairing = onShowPairing,
                    modifier = Modifier
                        .weight(0.38f)
                        .fillMaxSize(),
                )
                ContactsColumn(
                    contacts = state.contacts,
                    onContactCall = onContactCall,
                    onContactSms = onContactSms,
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
    }
}

@Composable
private fun StatusColumn(
    device: DeviceSettings?,
    deviceConfig: DeviceConfig?,
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
        }
    }
}

@Composable
private fun SmsComposeOverlay(
    contact: Contact,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onSend: (String) -> Unit,
) {
    var text by remember(contact.id) { mutableStateOf("") }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xEE000000)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .background(Color(0xFF111111))
                .border(1.dp, Color(0xFFFFFF00))
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = "SMS pro ${contact.name.ifEmpty { contact.phone }}",
                color = Color(0xFFFFFF00),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = contact.phone,
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 18.sp,
            )
            OutlinedTextField(
                value = text,
                onValueChange = { if (it.length <= 2000) text = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 140.dp),
                minLines = 4,
                placeholder = {
                    Text("Text zprávy…", color = Color.White.copy(alpha = 0.45f))
                },
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    cursorColor = Color(0xFFFFFF00),
                    focusedBorderColor = Color(0xFFFFFF00),
                    unfocusedBorderColor = Color.White.copy(alpha = 0.35f),
                ),
            )
            errorMessage?.let { err ->
                Text(
                    text = err,
                    color = Color(0xFFFF6666),
                    fontSize = 16.sp,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF333333),
                        contentColor = Color.White,
                    ),
                ) {
                    Text("Zrušit", fontSize = 18.sp)
                }
                Button(
                    onClick = { onSend(text) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFFFF00),
                        contentColor = Color.Black,
                    ),
                ) {
                    Text("Odeslat", fontSize = 18.sp)
                }
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

@Composable
private fun MessageOverlay(
    message: DeviceMessage,
    onDismiss: () -> Unit,
) {
    val title = message.senderDisplayName?.takeIf { it.isNotBlank() } ?: "Vzkaz od rodiny"
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xE6000000)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth(0.88f)
                .background(Color(0xFF0D3320))
                .border(1.dp, Color(0xFF22C55E))
                .padding(24.dp),
        ) {
            Text(
                text = title,
                color = Color(0xFF86EFAC),
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = message.body,
                color = Color.White,
                fontSize = 26.sp,
                fontWeight = FontWeight.Medium,
            )
            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF22C55E),
                    contentColor = Color.Black,
                ),
            ) {
                Text("Přečetl jsem", fontSize = 20.sp)
            }
        }
    }
}

@Composable
private fun AlertOverlay(
    message: String,
    onDismiss: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xCC000000)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .background(Color(0xFF330000))
                .padding(24.dp),
        ) {
            Text(
                text = message,
                color = Color.White,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = Color.Black,
                ),
            ) {
                Text("Rozumím", fontSize = 20.sp)
            }
        }
    }
}

@Composable
private fun KioskUnlockOverlay(
    errorMessage: String?,
    onDismiss: () -> Unit,
    onSubmit: (String) -> Unit,
) {
    var pin by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        pin = ""
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xEE000000)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.88f)
                .background(Color(0xFF111111))
                .border(1.dp, Color(0xFFFFFF00))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = "Správcovský PIN",
                color = Color(0xFFFFFF00),
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "Stejný kód jako ve webové administraci (4 číslice).",
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 18.sp,
            )
            Text(
                text = pin.padEnd(4, '·'),
                color = Color.White,
                fontSize = 42.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 8.sp,
            )
            errorMessage?.let { err ->
                Text(
                    text = err,
                    color = Color(0xFFFF6666),
                    fontSize = 18.sp,
                )
            }
            val rows = listOf(
                listOf("1", "2", "3"),
                listOf("4", "5", "6"),
                listOf("7", "8", "9"),
            )
            rows.forEach { row ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    row.forEach { d ->
                        Button(
                            onClick = {
                                if (pin.length < 4) pin += d
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF222222),
                                contentColor = Color.White,
                            ),
                            modifier = Modifier.size(width = 72.dp, height = 56.dp),
                        ) {
                            Text(d, fontSize = 22.sp)
                        }
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = {
                        if (pin.isNotEmpty()) pin = pin.dropLast(1)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF222222),
                        contentColor = Color.White,
                    ),
                    modifier = Modifier.size(width = 154.dp, height = 56.dp),
                ) {
                    Text("⌫", fontSize = 22.sp)
                }
                Button(
                    onClick = {
                        if (pin.length < 4) pin += "0"
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF222222),
                        contentColor = Color.White,
                    ),
                    modifier = Modifier.size(width = 72.dp, height = 56.dp),
                ) {
                    Text("0", fontSize = 22.sp)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color.Black,
                    ),
                ) {
                    Text("Zrušit", fontSize = 20.sp)
                }
                Button(
                    onClick = { onSubmit(pin) },
                    enabled = pin.length == 4,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFFFF00),
                        contentColor = Color.Black,
                    ),
                ) {
                    Text("Otevřít nastavení", fontSize = 20.sp)
                }
            }
        }
    }
}

@Composable
private fun PairingOverlay(
    device: DeviceSettings,
    onRefreshPairing: () -> Unit,
    onClose: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xDD000000)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.88f)
                .fillMaxHeight(0.82f)
                .background(Color.Black)
                .border(width = 1.dp, color = Color(0xFFFFFF00))
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Text(
                text = if (device.paired) "Přidat dalšího správce" else "Spáruj tablet s webem",
                color = Color(0xFFFFFF00),
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "Ve webu se přihlas Google účtem a zadej tento kód.",
                color = Color.White,
                fontSize = 21.sp,
            )
            Text(
                text = device.deviceId,
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 18.sp,
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF111111))
                    .padding(vertical = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = device.pairingCode ?: "------",
                    color = Color.White,
                    fontSize = 48.sp,
                    fontWeight = FontWeight.ExtraBold,
                )
                if (!device.pairingExpiresAtLabel.isNullOrBlank()) {
                    Text(
                        text = "Platí do ${device.pairingExpiresAtLabel}",
                        color = Color.White.copy(alpha = 0.82f),
                        fontSize = 18.sp,
                    )
                }
            }
            Text(
                text = "Po úspěšném spárování se obrazovka sama zavře. Kód můžeš kdykoliv obnovit.",
                color = Color.White.copy(alpha = 0.82f),
                fontSize = 18.sp,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = onRefreshPairing,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFFFF00),
                        contentColor = Color.Black,
                    ),
                ) {
                    Text("Obnovit kód", fontSize = 20.sp)
                }
                if (device.paired) {
                    Button(
                        onClick = onClose,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = Color.Black,
                        ),
                    ) {
                        Text("Zavřít", fontSize = 20.sp)
                    }
                }
            }
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
        )
    }
}
