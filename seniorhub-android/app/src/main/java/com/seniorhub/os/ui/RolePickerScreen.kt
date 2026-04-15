package com.seniorhub.os.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * První spuštění: volba režimu stejné APK (tablet u seniora vs. správce).
 * Volba je trvalá v DataStore ([com.seniorhub.os.data.AppRoleStore]).
 */
@Composable
fun RolePickerScreen(
    onChooseSenior: () -> Unit,
    onChooseAdmin: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "SeniorHub",
            style = MaterialTheme.typography.headlineLarge,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Vyber, jak budeš aplikaci používat. Tuto volbu později v aplikaci neměň — pro změnu režimu přeinstaluj aplikaci nebo vymaž její data.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = onChooseSenior,
            modifier = Modifier.padding(vertical = 8.dp),
        ) {
            Text("Tablet u seniora (kiosk, kontakty, vzkazy)")
        }
        Button(
            onClick = onChooseAdmin,
            modifier = Modifier.padding(vertical = 8.dp),
        ) {
            Text("Správce (Google účet, více tabletů)")
        }
    }
}
