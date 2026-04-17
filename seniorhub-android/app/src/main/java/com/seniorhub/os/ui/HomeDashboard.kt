package com.seniorhub.os.ui

import android.content.res.Configuration
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.seniorhub.os.BuildConfig
import com.seniorhub.os.R
import com.seniorhub.os.data.Contact
import com.seniorhub.os.data.DeviceConfig
import com.seniorhub.os.data.DeviceMessage
import com.seniorhub.os.data.DeviceSettings
import com.seniorhub.os.ui.theme.SeniorHubTheme
import com.seniorhub.os.util.normalizePhoneForDial
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

/** Nad touto šířkou jsou přehled a kontakty vedle sebe; pod ní jeden sloupec (např. úzký režim / telefon). */
private val DashboardTwoPaneMinWidth: Dp = 840.dp

/**
 * Domovský dashboard (Senior / kiosk): M3 [Surface] / [Card], typografie z tématu, jemné přechody při načtení.
 */
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
    val familyMessagesPreview = remember(state.messages) {
        state.messages
            .filter { it.delivery.isNullOrBlank() }
            .take(5)
            .asReversed()
    }
    val scheme = MaterialTheme.colorScheme
    Surface(
        modifier = modifier.fillMaxSize(),
        color = scheme.background,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AnimatedContent(
                targetState = state.loading,
                transitionSpec = {
                    fadeIn(animationSpec = tween(320)) togetherWith
                        fadeOut(animationSpec = tween(180))
                },
                label = "dashboardLoad",
            ) { loading ->
                if (loading) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(56.dp),
                            color = scheme.primary,
                            trackColor = scheme.surfaceContainerHighest,
                            strokeWidth = 4.dp,
                        )
                    }
                } else {
                    HomeDashboardMain(
                        state = state,
                        familyMessagesPreview = familyMessagesPreview,
                        showKioskLauncherHint = showKioskLauncherHint,
                        onShowPairing = onShowPairing,
                        onContactCall = onContactCall,
                        onContactSms = onContactSms,
                        onContactThread = onContactThread,
                    )
                }
            }

            AnimatedVisibility(
                visible = state.errorMessage != null,
                enter = fadeIn(tween(280)) + slideInVertically(tween(280)) { full -> full / 3 },
                exit = fadeOut(tween(200)) + slideOutVertically(tween(200)) { full -> full / 3 },
                modifier = Modifier.align(Alignment.BottomCenter),
            ) {
                val msg = state.errorMessage ?: return@AnimatedVisibility
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = MaterialTheme.shapes.large,
                    color = scheme.errorContainer,
                    shadowElevation = 6.dp,
                ) {
                    Text(
                        text = stringResource(R.string.dashboard_connection_error, msg),
                        style = MaterialTheme.typography.bodyLarge,
                        color = scheme.onErrorContainer,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
                    )
                }
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
}

@Composable
private fun HomeDashboardMain(
    state: HomeUiState,
    familyMessagesPreview: List<DeviceMessage>,
    showKioskLauncherHint: Boolean,
    onShowPairing: () -> Unit,
    onContactCall: (String) -> Unit,
    onContactSms: (Contact) -> Unit,
    onContactThread: (Contact) -> Unit,
) {
    var clock by remember { mutableStateOf(LocalTime.now()) }
    LaunchedEffect(Unit) {
        while (isActive) {
            clock = LocalTime.now()
            delay(60_000L)
        }
    }
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 20.dp),
    ) {
        val buildFingerprint = remember {
            "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE}) · ${BuildConfig.BUILD_MARK} · ${BuildConfig.BUILD_STAMP}"
        }
        val stacked = maxWidth < DashboardTwoPaneMinWidth
        if (stacked) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                dashboardOverviewItems(
                    clock = clock,
                    device = state.device,
                    deviceConfig = state.deviceConfig,
                    weatherLine = state.weatherLine,
                    familyMessages = familyMessagesPreview,
                    showKioskLauncherHint = showKioskLauncherHint,
                    onShowPairing = onShowPairing,
                )
                item {
                    Spacer(modifier = Modifier.height(6.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = stringResource(R.string.dashboard_contacts),
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }
                contactListItems(
                    contacts = state.contacts,
                    onContactCall = onContactCall,
                    onContactSms = onContactSms,
                    onContactThread = onContactThread,
                )
            }
        } else {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                LazyColumn(
                    modifier = Modifier
                        .weight(0.38f)
                        .fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    dashboardOverviewItems(
                        clock = clock,
                        device = state.device,
                        deviceConfig = state.deviceConfig,
                        weatherLine = state.weatherLine,
                        familyMessages = familyMessagesPreview,
                        showKioskLauncherHint = showKioskLauncherHint,
                        onShowPairing = onShowPairing,
                    )
                }
                LazyColumn(
                    modifier = Modifier
                        .weight(0.62f)
                        .fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item {
                        Text(
                            text = stringResource(R.string.dashboard_contacts),
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                    contactListItems(
                        contacts = state.contacts,
                        onContactCall = onContactCall,
                        onContactSms = onContactSms,
                        onContactThread = onContactThread,
                    )
                }
            }
        }
        Surface(
            modifier = Modifier.align(Alignment.BottomStart),
            shape = MaterialTheme.shapes.extraSmall,
            tonalElevation = 2.dp,
            shadowElevation = 4.dp,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
        ) {
            Text(
                text = buildFingerprint,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
                lineHeight = 16.sp,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private fun LazyListScope.dashboardOverviewItems(
    clock: LocalTime,
    device: DeviceSettings?,
    deviceConfig: DeviceConfig?,
    weatherLine: String?,
    familyMessages: List<DeviceMessage>,
    showKioskLauncherHint: Boolean,
    onShowPairing: () -> Unit,
) {
    val seniorDisplay = listOfNotNull(
        deviceConfig?.seniorFirstName?.trim()?.takeIf { it.isNotEmpty() },
        deviceConfig?.seniorLastName?.trim()?.takeIf { it.isNotEmpty() },
    ).joinToString(" ")
    val address = deviceConfig?.addressLine?.trim().orEmpty()
    item {
        val scheme = MaterialTheme.colorScheme
        Text(
            text = stringResource(R.string.dashboard_section_overview),
            style = MaterialTheme.typography.headlineSmall,
            color = scheme.primary,
        )
    }
    item {
        val scheme = MaterialTheme.colorScheme
        Text(
            text = clock.format(DateTimeFormatter.ofPattern("HH:mm")),
            style = MaterialTheme.typography.displayLarge,
            color = scheme.onBackground,
        )
    }
    item {
        val scheme = MaterialTheme.colorScheme
        Text(
            text = device?.deviceLabel ?: stringResource(R.string.device_label_fallback),
            style = MaterialTheme.typography.headlineMedium,
            color = scheme.primary,
        )
    }
    if (seniorDisplay.isNotBlank()) {
        item {
            val scheme = MaterialTheme.colorScheme
            Text(
                text = stringResource(R.string.dashboard_senior_label, seniorDisplay),
                style = MaterialTheme.typography.titleLarge,
                color = scheme.onBackground,
            )
        }
    }
    if (address.isNotBlank()) {
        item {
            val scheme = MaterialTheme.colorScheme
            Text(
                text = address,
                style = MaterialTheme.typography.bodyLarge,
                color = scheme.onSurfaceVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
    item {
        val scheme = MaterialTheme.colorScheme
        Text(
            text = stringResource(
                R.string.dashboard_device_id,
                device?.deviceId ?: "—",
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = scheme.onSurfaceVariant,
        )
    }
    item {
        val scheme = MaterialTheme.colorScheme
        Text(
            text = stringResource(
                R.string.dashboard_volume_remote,
                device?.volumePercent?.toString() ?: "—",
            ),
            style = MaterialTheme.typography.titleMedium,
            color = scheme.onBackground,
        )
    }
    weatherLine?.let { line ->
        item {
            val scheme = MaterialTheme.colorScheme
            Card(
                colors = CardDefaults.cardColors(containerColor = scheme.surfaceContainer),
                shape = MaterialTheme.shapes.medium,
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            ) {
                Text(
                    text = line,
                    style = MaterialTheme.typography.bodyLarge,
                    color = scheme.onSurface,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                )
            }
        }
    }
    device?.networkLabel?.let { net ->
        item {
            val scheme = MaterialTheme.colorScheme
            Text(
                text = stringResource(R.string.dashboard_network, net),
                style = MaterialTheme.typography.bodyMedium,
                color = scheme.onSurfaceVariant,
            )
        }
    }
    if (showKioskLauncherHint) {
        item {
            val scheme = MaterialTheme.colorScheme
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = scheme.tertiaryContainer,
            ) {
                Text(
                    text = stringResource(R.string.kiosk_home_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.onTertiaryContainer,
                    lineHeight = MaterialTheme.typography.bodyMedium.lineHeight * 1.15f,
                    modifier = Modifier.padding(14.dp),
                )
            }
        }
    }
    if (familyMessages.isNotEmpty()) {
        item {
            val scheme = MaterialTheme.colorScheme
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = stringResource(R.string.dashboard_family_messages_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = scheme.primary,
                )
                Card(
                    colors = CardDefaults.cardColors(containerColor = scheme.surfaceContainer),
                    shape = MaterialTheme.shapes.large,
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        familyMessages.forEach { m ->
                            Text(
                                text = "[Rodina] ${m.senderDisplayName ?: "—"}: ${m.body}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = scheme.onSurface.copy(alpha = 0.92f),
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        HorizontalDivider(color = scheme.outlineVariant)
                        Text(
                            text = stringResource(R.string.dashboard_family_messages_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = scheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
    device?.batteryPercent?.let { pct ->
        item {
            val scheme = MaterialTheme.colorScheme
            val batText = if (device.charging) {
                stringResource(R.string.dashboard_battery_charging, pct)
            } else {
                stringResource(R.string.dashboard_battery, pct)
            }
            Text(
                text = batText,
                style = MaterialTheme.typography.titleMedium,
                color = scheme.onSurface,
            )
        }
    }
    if (device != null) {
        item {
            PairingSummaryCard(
                paired = device.paired,
                pairingCode = device.pairingCode,
                expiresAtLabel = device.pairingExpiresAtLabel,
                onShowPairing = onShowPairing,
            )
        }
    }
}

private fun LazyListScope.contactListItems(
    contacts: List<Contact>,
    onContactCall: (String) -> Unit,
    onContactSms: (Contact) -> Unit,
    onContactThread: (Contact) -> Unit,
) {
    if (contacts.isEmpty()) {
        item {
            Text(
                text = stringResource(R.string.dashboard_no_contacts),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    } else {
        itemsIndexed(
            contacts,
            key = { _, c -> c.id },
        ) { index, c ->
            var revealed by remember(c.id) { mutableStateOf(false) }
            LaunchedEffect(c.id) {
                delay((index * 42L).coerceAtMost(420L))
                revealed = true
            }
            AnimatedVisibility(
                visible = revealed,
                enter = fadeIn(animationSpec = tween(260)),
            ) {
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

@Composable
private fun ContactRow(
    contact: Contact,
    onCall: () -> Unit,
    onSms: () -> Unit,
    onThread: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val canCommunicate = contact.phone.isNotBlank() && normalizePhoneForDial(contact.phone) != null
    val scheme = MaterialTheme.colorScheme
    val cardColors = if (contact.isEmergency) {
        CardDefaults.cardColors(
            containerColor = scheme.errorContainer.copy(alpha = 0.55f),
        )
    } else {
        CardDefaults.cardColors(containerColor = scheme.surfaceContainer)
    }
    val border = if (contact.isEmergency) {
        BorderStroke(1.dp, scheme.error.copy(alpha = 0.65f))
    } else {
        null
    }
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = cardColors,
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp,
            pressedElevation = 4.dp,
        ),
        border = border,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
        ) {
            if (contact.isEmergency) {
                Text(
                    text = stringResource(R.string.contact_badge_emergency).uppercase(),
                    style = MaterialTheme.typography.labelLarge,
                    color = scheme.error,
                )
                Spacer(modifier = Modifier.height(6.dp))
            }
            Text(
                text = contact.name.ifEmpty { stringResource(R.string.contact_name_fallback) },
                style = MaterialTheme.typography.titleLarge,
                color = scheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = contact.phone.ifEmpty { "—" },
                style = MaterialTheme.typography.bodyLarge,
                color = scheme.onSurfaceVariant,
            )
            if (canCommunicate) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    FilledTonalButton(
                        onClick = onCall,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 52.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = scheme.surfaceContainerHigh,
                            contentColor = scheme.primary,
                        ),
                    ) {
                        Icon(
                            Icons.Filled.Call,
                            contentDescription = null,
                            modifier = Modifier.size(22.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            stringResource(R.string.contact_action_call),
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                    FilledTonalButton(
                        onClick = onSms,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 52.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = scheme.surfaceContainerHigh,
                            contentColor = scheme.primary,
                        ),
                    ) {
                        Icon(
                            Icons.Filled.Sms,
                            contentDescription = null,
                            modifier = Modifier.size(22.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            stringResource(R.string.contact_action_sms),
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onThread,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp),
                    border = BorderStroke(1.dp, scheme.outline),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = scheme.onSurface,
                    ),
                ) {
                    Icon(
                        Icons.Filled.Forum,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        stringResource(R.string.contact_action_thread),
                        style = MaterialTheme.typography.bodyLarge,
                    )
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
    val scheme = MaterialTheme.colorScheme
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onShowPairing),
        colors = CardDefaults.cardColors(containerColor = scheme.surfaceContainerHigh),
        shape = MaterialTheme.shapes.large,
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = if (paired) {
                    stringResource(R.string.dashboard_pairing_connected)
                } else {
                    stringResource(R.string.dashboard_pairing_waiting)
                },
                style = MaterialTheme.typography.titleLarge,
                color = scheme.primary,
            )
            Text(
                text = if (paired) {
                    stringResource(R.string.dashboard_pairing_hint_connected)
                } else {
                    stringResource(R.string.dashboard_pairing_hint_waiting)
                },
                style = MaterialTheme.typography.bodyLarge,
                color = scheme.onSurface,
            )
            if (!pairingCode.isNullOrBlank()) {
                Text(
                    text = pairingCode,
                    style = MaterialTheme.typography.displayMedium,
                    color = scheme.onBackground,
                )
            }
            if (!expiresAtLabel.isNullOrBlank()) {
                Text(
                    text = stringResource(R.string.dashboard_pairing_expires, expiresAtLabel),
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Preview(name = "Úzký / jeden sloupec", widthDp = 400, heightDp = 780)
@Composable
private fun HomeScreenNarrowPreview() {
    SeniorHubTheme {
        HomeScreen(
            state = HomeUiState(
                loading = false,
                device = DeviceSettings(
                    deviceId = "narrow",
                    deviceLabel = "Tablet",
                    volumePercent = 80,
                    alertMessage = null,
                    paired = true,
                    pairingCode = null,
                    pairingExpiresAtLabel = null,
                    batteryPercent = 100,
                    charging = true,
                    networkLabel = "5G",
                ),
                deviceConfig = DeviceConfig(
                    adminPin = "",
                    simNumber = "",
                    assistantName = "",
                    seniorFirstName = "Jan",
                    seniorLastName = "",
                    addressLine = "",
                ),
                contacts = listOf(
                    Contact("1", "Dcera", "+420 777 000 111"),
                ),
                weatherLine = "Venku 12 °C",
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
                    batteryPercent = 84,
                    charging = false,
                    networkLabel = "Wi‑Fi",
                ),
                deviceConfig = DeviceConfig(
                    adminPin = "",
                    simNumber = "",
                    assistantName = "",
                    seniorFirstName = "Marie",
                    seniorLastName = "Nováková",
                    addressLine = "Praha 4",
                ),
                contacts = listOf(
                    Contact("1", "Jana", "+420 777 111 222"),
                    Contact("2", "Petr", "+420 603 333 444", isEmergency = true),
                ),
                weatherLine = "Venku 18 °C, polojasno",
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
            showKioskLauncherHint = true,
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
