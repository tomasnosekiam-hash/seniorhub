package com.seniorhub.os.ui

import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.seniorhub.os.ui.theme.SeniorHubDesign
import com.seniorhub.os.util.KioskMode

/** Pruh v layoutu dashboardu — ne overlay, neblokuje dotyky pod sebou. */
@Composable
fun KioskPinBanner(
    modifier: Modifier = Modifier,
) {
    val activity = LocalContext.current as? ComponentActivity ?: return
    val lifecycleOwner = LocalLifecycleOwner.current
    var pinned by remember { mutableStateOf(KioskMode.isInLockTask(activity)) }
    var pinFailed by remember { mutableStateOf(false) }
    var pinBusy by remember { mutableStateOf(false) }

    DisposableEffect(lifecycleOwner, activity) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                pinned = KioskMode.isInLockTask(activity)
                if (pinned) {
                    pinFailed = false
                    pinBusy = false
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        pinned = KioskMode.isInLockTask(activity)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    if (pinned) return

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(SeniorHubDesign.DialogueAnswerShell)
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = if (pinFailed) {
                "Tablet neukázal dialog „Připnout“. Nejdřív v Nastavení zapněte „Připnutí obrazovky“ " +
                    "(Zabezpečení → Pokročilé), pak klepněte znovu na Připnout."
            } else {
                "Pro kiosk režim klepněte na Připnout a v systémovém dialogu potvrďte."
            },
            fontSize = 16.sp,
            color = if (pinFailed) SeniorHubDesign.WeatherSun else SeniorHubDesign.WeatherText,
            textAlign = TextAlign.Center,
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            OutlinedButton(
                onClick = { KioskMode.openPinningSettings(activity) },
                modifier = Modifier.weight(1f),
            ) {
                Text("Nastavení", fontSize = 16.sp)
            }
            Button(
                onClick = {
                    pinBusy = true
                    pinFailed = false
                    KioskMode.tryStartPinning(activity) { success ->
                        pinBusy = false
                        if (success) {
                            pinned = true
                            pinFailed = false
                        } else {
                            pinFailed = true
                        }
                    }
                },
                enabled = !pinBusy,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SeniorHubDesign.MessageSurface,
                    contentColor = SeniorHubDesign.Black,
                ),
            ) {
                Text(
                    if (pinBusy) "Čekám…" else "Připnout SeniorHub",
                    fontSize = 16.sp,
                )
            }
        }
    }
}
