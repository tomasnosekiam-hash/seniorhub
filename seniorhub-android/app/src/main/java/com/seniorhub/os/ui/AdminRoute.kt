package com.seniorhub.os.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.seniorhub.os.R
import com.seniorhub.os.data.DeviceSettings
import com.seniorhub.os.data.MvpRepository
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun AdminRoute(
    viewModel: AdminViewModel,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val webClientId = remember {
        context.getString(R.string.default_web_client_id).trim()
    }

    val signInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        val data = result.data ?: return@rememberLauncherForActivityResult
        val task = GoogleSignIn.getSignedInAccountFromIntent(data)
        try {
            val account = task.getResult(ApiException::class.java)
            val idToken = account.idToken
            if (idToken != null) {
                viewModel.onGoogleSignInSuccess(idToken)
            } else {
                viewModel.onGoogleSignInFailed("Google účet nemá id token.")
            }
        } catch (e: ApiException) {
            viewModel.onGoogleSignInFailed(e.message ?: "Google přihlášení selhalo (${e.statusCode}).")
        }
    }

    fun launchGoogleSignIn() {
        if (webClientId.isBlank()) {
            return
        }
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(webClientId)
            .requestEmail()
            .build()
        val client = GoogleSignIn.getClient(context, gso)
        signInLauncher.launch(client.signInIntent)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF0F2F5)),
    ) {
        when {
            webClientId.isBlank() -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = "Chybí Web client ID pro Google přihlášení.",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "V Firebase Console → Project settings → Your apps přidej SHA-1/256 k Android aplikaci, povol Google přihlášení a z konzole zkopíruj OAuth 2.0 Web client ID do seniorhub-android/app/src/main/res/values/strings.xml (default_web_client_id).",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }

            state.authUser == null -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = "Správce SeniorHub",
                        style = MaterialTheme.typography.headlineSmall,
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { launchGoogleSignIn() }) {
                        Text("Přihlásit přes Google")
                    }
                    state.error?.let { err ->
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = err,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }

            else -> {
                AdminSignedInContent(
                    state = state,
                    onSignOut = viewModel::signOut,
                    onSelectDevice = viewModel::selectDevice,
                    onPairDraft = viewModel::setPairDraft,
                    onPair = viewModel::pair,
                    onDismissBanner = viewModel::dismissBanner,
                    onSaveDeviceMeta = viewModel::saveDeviceMeta,
                    onClearAlert = viewModel::clearAlert,
                    onSaveConfig = viewModel::saveConfig,
                    onSaveSenior = viewModel::saveSeniorProfile,
                    onAddContact = viewModel::addContact,
                    onSetContactEmergency = viewModel::setContactEmergency,
                    onMoveContactUp = viewModel::moveContactUp,
                    onMoveContactDown = viewModel::moveContactDown,
                    onDeleteContact = viewModel::deleteContact,
                    onSendMessage = viewModel::sendMessage,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }

        if (state.loading && state.authUser != null && webClientId.isNotBlank()) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AdminSignedInContent(
    state: AdminUiState,
    onSignOut: () -> Unit,
    onSelectDevice: (String) -> Unit,
    onPairDraft: (String) -> Unit,
    onPair: () -> Unit,
    onDismissBanner: () -> Unit,
    onSaveDeviceMeta: (String, Int, String) -> Unit,
    onClearAlert: () -> Unit,
    onSaveConfig: (String, String, String) -> Unit,
    onSaveSenior: (String, String, String) -> Unit,
    onAddContact: (String, String) -> Unit,
    onSetContactEmergency: (String, Boolean) -> Unit,
    onMoveContactUp: (String) -> Unit,
    onMoveContactDown: (String) -> Unit,
    onDeleteContact: (String) -> Unit,
    onSendMessage: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val selectedId = state.selectedDeviceId
    val device = state.device
    val config = state.config

    var labelDraft by remember(selectedId) { mutableStateOf(device?.deviceLabel ?: "") }
    var volumeDraft by remember(selectedId) { mutableIntStateOf(device?.volumePercent ?: 50) }
    var alertDraft by remember(selectedId) { mutableStateOf(device?.alertMessage ?: "") }

    var pinDraft by remember(selectedId) { mutableStateOf(config?.adminPin ?: "") }
    var simDraft by remember(selectedId) { mutableStateOf(config?.simNumber ?: "") }
    var assistantDraft by remember(selectedId) { mutableStateOf(config?.assistantName ?: "") }
    var seniorFirstDraft by remember(selectedId) { mutableStateOf(config?.seniorFirstName ?: "") }
    var seniorLastDraft by remember(selectedId) { mutableStateOf(config?.seniorLastName ?: "") }
    var addressDraft by remember(selectedId) { mutableStateOf(config?.addressLine ?: "") }

    var messageDraft by remember(selectedId) { mutableStateOf("") }
    var newContactName by remember(selectedId) { mutableStateOf("") }
    var newContactPhone by remember(selectedId) { mutableStateOf("") }

    LaunchedEffect(device?.deviceLabel, selectedId) {
        labelDraft = device?.deviceLabel ?: ""
        volumeDraft = device?.volumePercent ?: 50
        alertDraft = device?.alertMessage ?: ""
    }
    LaunchedEffect(config?.adminPin, selectedId) {
        pinDraft = config?.adminPin ?: ""
        simDraft = config?.simNumber ?: ""
        assistantDraft = config?.assistantName ?: ""
        seniorFirstDraft = config?.seniorFirstName ?: ""
        seniorLastDraft = config?.seniorLastName ?: ""
        addressDraft = config?.addressLine ?: ""
    }

    LazyColumn(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = "Přihlášen",
                        style = MaterialTheme.typography.labelMedium,
                    )
                    Text(
                        text = state.authUser?.email ?: state.authUser?.uid ?: "",
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
                TextButton(onClick = onSignOut) {
                    Text("Odhlásit")
                }
            }
        }

        item {
            Text(
                text = "Moje tablety",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            state.devicesError?.let {
                Text(text = it, color = MaterialTheme.colorScheme.error)
            }
        }

        item {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                state.devices.forEach { row ->
                    FilterChip(
                        selected = row.deviceId == selectedId,
                        onClick = { onSelectDevice(row.deviceId) },
                        label = {
                            Text(
                                text = buildString {
                                    append(row.deviceLabel)
                                    row.batteryPercent?.let { append(" · $it %") }
                                    row.networkLabel?.let { append(" · $it") }
                                },
                            )
                        },
                    )
                }
            }
        }

        item {
            OutlinedTextField(
                value = state.pairCodeDraft,
                onValueChange = onPairDraft,
                label = { Text("Párovací kód z tabletu") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onPair) {
                    Text("Spárovat")
                }
            }
            state.pairError?.let {
                Text(text = it, color = MaterialTheme.colorScheme.error)
            }
        }

        item {
            state.banner?.let { b ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(text = b, color = Color(0xFF1B5E20))
                    TextButton(onClick = onDismissBanner) {
                        Text("OK")
                    }
                }
            }
            state.error?.let {
                Text(text = it, color = MaterialTheme.colorScheme.error)
            }
        }

        if (selectedId != null && device != null) {
            item {
                SectionTitle("Zařízení")
                DeviceSummary(device)
                OutlinedTextField(
                    value = labelDraft,
                    onValueChange = { labelDraft = it },
                    label = { Text("Název / popisek") },
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Hlasitost ${volumeDraft} %", modifier = Modifier.weight(1f))
                }
                Slider(
                    value = volumeDraft.toFloat(),
                    onValueChange = { volumeDraft = it.toInt() },
                    valueRange = 0f..100f,
                    steps = 19,
                )
                OutlinedTextField(
                    value = alertDraft,
                    onValueChange = { alertDraft = it },
                    label = { Text("Hláška na celou obrazovku") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            onSaveDeviceMeta(labelDraft, volumeDraft, alertDraft)
                        },
                    ) {
                        Text("Uložit název a hlášku")
                    }
                    OutlinedButton(onClick = onClearAlert) {
                        Text("Vymazat hlášku")
                    }
                }
            }

            item {
                SectionTitle("PIN, SIM, asistent")
                OutlinedTextField(
                    value = pinDraft,
                    onValueChange = { v ->
                        pinDraft = v.filter { ch -> ch.isDigit() }.take(4)
                    },
                    label = { Text("Admin PIN (4 číslice)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                OutlinedTextField(
                    value = simDraft,
                    onValueChange = { simDraft = it },
                    label = { Text("Číslo SIM v tabletu") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                OutlinedTextField(
                    value = assistantDraft,
                    onValueChange = { assistantDraft = it },
                    label = { Text("Jméno asistenta") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                Button(
                    onClick = {
                        onSaveConfig(pinDraft, simDraft, assistantDraft)
                    },
                ) {
                    Text("Uložit PIN a provoz")
                }
            }

            item {
                SectionTitle("Profil seniora")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = seniorFirstDraft,
                        onValueChange = { seniorFirstDraft = it },
                        label = { Text("Jméno") },
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = seniorLastDraft,
                        onValueChange = { seniorLastDraft = it },
                        label = { Text("Příjmení") },
                        modifier = Modifier.weight(1f),
                    )
                }
                OutlinedTextField(
                    value = addressDraft,
                    onValueChange = { addressDraft = it },
                    label = { Text("Adresa (řádek)") },
                    modifier = Modifier.fillMaxWidth(),
                )
                Button(
                    onClick = {
                        onSaveSenior(seniorFirstDraft, seniorLastDraft, addressDraft)
                    },
                ) {
                    Text("Uložit profil seniora")
                }
            }

            item {
                SectionTitle("Kontakty na tabletu")
                Text(
                    text = "Zaškrtni „Nouze“ u kontaktů pro prioritu při hovoru / u Matěje.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.DarkGray,
                )
            }

            itemsIndexed(state.contacts, key = { _, c -> c.id }) { idx, c ->
                val canUp = idx > 0
                val canDown = idx < state.contacts.lastIndex
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Column {
                        IconButton(
                            onClick = { onMoveContactUp(c.id) },
                            enabled = canUp,
                        ) {
                            Icon(Icons.Filled.KeyboardArrowUp, contentDescription = "Výš")
                        }
                        IconButton(
                            onClick = { onMoveContactDown(c.id) },
                            enabled = canDown,
                        ) {
                            Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Níž")
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = c.name.ifEmpty { "(bez jména)" },
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = c.phone.ifEmpty { "—" },
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.DarkGray,
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Nouze",
                            style = MaterialTheme.typography.labelSmall,
                        )
                        Switch(
                            checked = c.isEmergency,
                            onCheckedChange = { onSetContactEmergency(c.id, it) },
                        )
                    }
                    TextButton(
                        onClick = { onDeleteContact(c.id) },
                    ) {
                        Text("Smazat", color = MaterialTheme.colorScheme.error)
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedTextField(
                        value = newContactName,
                        onValueChange = { newContactName = it },
                        label = { Text("Jméno") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = newContactPhone,
                        onValueChange = { newContactPhone = it },
                        label = { Text("Telefon") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                    )
                }
                Button(
                    onClick = {
                        onAddContact(newContactName, newContactPhone)
                        newContactName = ""
                        newContactPhone = ""
                    },
                ) {
                    Text("Přidat kontakt")
                }
            }

            item {
                SectionTitle("Vzkaz na tablet")
                OutlinedTextField(
                    value = messageDraft,
                    onValueChange = { messageDraft = it },
                    label = { Text("Text") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                )
                Button(
                    onClick = {
                        onSendMessage(messageDraft)
                        messageDraft = ""
                    },
                ) {
                    Text("Odeslat na tablet")
                }
            }

            item { SectionTitle("Poslední vzkazy") }

            items(state.messages, key = { it.id }) { m ->
                val fmt = remember(m.id, m.createdAt) {
                    m.createdAt?.toDate()?.let { d ->
                        SimpleDateFormat("d.M. HH:mm", Locale.getDefault()).format(d)
                    } ?: "—"
                }
                val deliveryNote = when (m.delivery) {
                    MvpRepository.VAL_DELIVERY_TABLET_FIRESTORE -> "cloud (tablet)"
                    MvpRepository.VAL_DELIVERY_SMS_CELLULAR -> "SMS zrcadlo"
                    MvpRepository.VAL_DELIVERY_SMS_INBOUND -> "SMS příchozí"
                    null, "" -> "web"
                    else -> m.delivery ?: ""
                }
                Text(
                    text = "${m.body.take(120)}${if (m.body.length > 120) "…" else ""}\n$fmt · ${m.senderDisplayName ?: ""} · $deliveryNote · ${
                        if (m.readAt != null) "přečteno" else "nepřečteno"
                    }",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(vertical = 4.dp),
                )
            }
        }

        item { Spacer(modifier = Modifier.height(32.dp)) }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
    )
}

@Composable
private fun DeviceSummary(device: DeviceSettings) {
    val bits = buildList {
        if (device.paired) add("spárováno") else add("čeká na spárování")
        device.pairingCode?.let { add("kód $it${device.pairingExpiresAtLabel?.let { " do $it" } ?: ""}") }
        device.batteryPercent?.let { add("$it %${if (device.charging) " nabíjení" else ""}") }
        device.lastHeartbeatAtLabel?.let { add("kontakt $it") }
        device.networkLabel?.let { add(it) }
    }
    Text(
        text = "${device.deviceId} · ${bits.joinToString(" · ")}",
        style = MaterialTheme.typography.bodySmall,
        color = Color.DarkGray,
    )
}
