package com.seniorhub.os.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.seniorhub.os.data.Contact
import com.seniorhub.os.data.DeviceMessage
import com.seniorhub.os.data.DeviceSettings
import com.seniorhub.os.data.MvpRepository
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
internal fun ContactThreadOverlay(
    contact: Contact,
    messages: List<DeviceMessage>,
    onDismiss: () -> Unit,
    onReply: () -> Unit,
) {
    val fmt = remember {
        SimpleDateFormat("d.M. HH:mm", Locale.getDefault())
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xEE000000)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.88f)
                .background(Color(0xFF111111))
                .border(1.dp, Color(0xFFFFFF00))
                .padding(20.dp),
        ) {
            Text(
                text = "Konverzace",
                color = Color(0xFFFFFF00),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = contact.name.ifEmpty { contact.phone },
                color = Color.White,
                fontSize = 20.sp,
            )
            Text(
                text = contact.phone,
                color = Color.White.copy(alpha = 0.75f),
                fontSize = 16.sp,
            )
            Spacer(modifier = Modifier.height(12.dp))
            if (messages.isEmpty()) {
                Text(
                    text = "Zatím žádná zpráva v tomto vlákně. Po odeslání nebo příjmu SMS (číslo v kontaktech) a po zprávách přes aplikaci se záznam zobrazí tady i u rodiny ve webu.",
                    color = Color.White.copy(alpha = 0.78f),
                    fontSize = 16.sp,
                )
                Spacer(modifier = Modifier.weight(1f))
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                ) {
                    items(messages, key = { it.id }) { m ->
                        val timeLabel = m.createdAt?.toDate()?.let { fmt.format(it) } ?: "—"
                        val tech = when (m.delivery) {
                            MvpRepository.VAL_DELIVERY_TABLET_FIRESTORE -> "Aplikace (cloud)"
                            MvpRepository.VAL_DELIVERY_SMS_CELLULAR -> "SMS (odchozí)"
                            MvpRepository.VAL_DELIVERY_SMS_INBOUND -> "SMS (příchozí)"
                            else -> "Tablet"
                        }
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF1A1A1A))
                                .padding(12.dp),
                        ) {
                            Text(
                                text = "$timeLabel · $tech",
                                color = Color(0xFFFFFF00),
                                fontSize = 14.sp,
                            )
                            Text(
                                text = m.body,
                                color = Color.White,
                                fontSize = 17.sp,
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF2A2A2A),
                        contentColor = Color.White,
                    ),
                ) {
                    Text("Zavřít", fontSize = 18.sp)
                }
                Button(
                    onClick = onReply,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Napsat", fontSize = 18.sp)
                }
            }
        }
    }
}

@Composable
internal fun SmsComposeOverlay(
    contact: Contact,
    useCellularSms: Boolean,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onSend: (String) -> Unit,
) {
    var text by remember(contact.id) { mutableStateOf("") }
    val title = if (useCellularSms) {
        "Zpráva pro ${contact.name.ifEmpty { contact.phone }}"
    } else {
        "Zpráva pro ${contact.name.ifEmpty { contact.phone }} (bez klasické SMS)"
    }
    val techHint = if (useCellularSms) {
        "Technologie: SMS přes mobilní síť. Text se zároveň uloží do cloudu (vlákno u kontaktu a web pro rodinu)."
    } else {
        "Technologie: Firebase Firestore + synchronizace do webové administrace. " +
            "Na telefon příjemce nedorazí jako klasická SMS — uvidí ji přihlášená rodina v aplikaci."
    }
    val sendLabel = if (useCellularSms) "Odeslat SMS" else "Odeslat přes aplikaci"
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
                text = title,
                color = Color(0xFFFFFF00),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = contact.phone,
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 18.sp,
            )
            Text(
                text = techHint,
                color = Color.White.copy(alpha = 0.78f),
                fontSize = 16.sp,
                lineHeight = 22.sp,
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
                    Text(sendLabel, fontSize = 18.sp)
                }
            }
        }
    }
}

@Composable
internal fun MessageOverlay(
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
internal fun AlertOverlay(
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
internal fun KioskUnlockOverlay(
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
                text = "Stejný kód jako ve webové administraci (4 číslice). Po ověření se krátce uvolní kiosk a otevřou systémová nastavení.",
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
internal fun PairingOverlay(
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
