package com.seniorhub.os.ui

import android.content.Intent
import android.content.res.Configuration
import android.provider.Settings
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.seniorhub.os.data.Contact
import com.seniorhub.os.data.DeviceSettings
import com.seniorhub.os.ui.theme.SeniorHubTheme
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@Composable
fun HomeRoute(
    viewModel: HomeViewModel,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    HomeScreen(
        state = state,
        onDismissAlert = viewModel::dismissAlert,
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
        modifier = modifier,
    )
}

@Composable
fun HomeScreen(
    state: HomeUiState,
    onDismissAlert: () -> Unit,
    onShowPairing: () -> Unit,
    onHidePairing: () -> Unit,
    onRefreshPairing: () -> Unit,
    onKioskSecretTap: () -> Unit,
    onDismissKioskUnlock: () -> Unit,
    onSubmitKioskPin: (String) -> Unit,
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
                    onShowPairing = onShowPairing,
                    modifier = Modifier
                        .weight(0.38f)
                        .fillMaxSize(),
                )
                ContactsColumn(
                    contacts = state.contacts,
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
    }
}

@Composable
private fun StatusColumn(
    device: DeviceSettings?,
    onShowPairing: () -> Unit,
    modifier: Modifier = Modifier,
) {
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
                    ContactRow(contact = c)
                }
            }
        }
    }
}

@Composable
private fun ContactRow(contact: Contact) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF111111))
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
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
            onShowPairing = {},
            onHidePairing = {},
            onRefreshPairing = {},
            onKioskSecretTap = {},
            onDismissKioskUnlock = {},
            onSubmitKioskPin = {},
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
            onShowPairing = {},
            onHidePairing = {},
            onRefreshPairing = {},
            onKioskSecretTap = {},
            onDismissKioskUnlock = {},
            onSubmitKioskPin = {},
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
            onShowPairing = {},
            onHidePairing = {},
            onRefreshPairing = {},
            onKioskSecretTap = {},
            onDismissKioskUnlock = {},
            onSubmitKioskPin = {},
        )
    }
}
