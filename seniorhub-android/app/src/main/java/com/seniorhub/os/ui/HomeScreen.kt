package com.seniorhub.os.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.seniorhub.os.BuildConfig
import com.seniorhub.os.R
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.seniorhub.os.data.Contact
import com.seniorhub.os.util.CellularSmsCapability
import com.seniorhub.os.util.KioskMode
import com.seniorhub.os.util.SmsSender
import com.seniorhub.os.util.belongsToContactThread
import com.seniorhub.os.util.normalizePhoneForDial
import com.seniorhub.os.util.openDialPad
import com.seniorhub.os.util.startOutgoingCall

@Composable
fun HomeRoute(
    viewModel: HomeViewModel,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val matejSession by viewModel.matejSession.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val useCellularSms = remember(context) {
        CellularSmsCapability.canSendCellularSms(context)
    }
    var pendingCallPhone by remember { mutableStateOf<String?>(null) }
    var smsTarget by remember { mutableStateOf<Contact?>(null) }
    var threadContact by remember { mutableStateOf<Contact?>(null) }
    var smsSendError by remember { mutableStateOf<String?>(null) }
    var pendingSms by remember { mutableStateOf<Pair<String, String>?>(null) }
    val overlayBlocksDashboard = threadContact != null || smsTarget != null
    val matejDisplayed = matejSession?.let { s ->
        if (overlayBlocksDashboard) s.copy(compact = true) else s
    }
    var isDefaultHomeApp by remember {
        mutableStateOf(KioskMode.isOurPackageDefaultHome(context))
    }
    val showKioskLauncherHint = state.device?.paired == true && !isDefaultHomeApp
    val pairedForMatej = state.device?.paired == true
    // Dříve jen první složení + ON_RESUME: po načtení Firestore (paired=true) se sync nevolal znovu
    // a FGS s Porcupine nikdy nenaběhla — mikrofon v systému bez nového přístupu.
    LaunchedEffect(state.loading, pairedForMatej) {
        viewModel.syncMatejWakeService()
    }
    DisposableEffect(lifecycleOwner) {
        val obs = LifecycleEventObserver { _, e ->
            if (e == Lifecycle.Event.ON_RESUME) {
                viewModel.syncMatejWakeService(force = true)
            }
        }
        lifecycleOwner.lifecycle.addObserver(obs)
        onDispose { lifecycleOwner.lifecycle.removeObserver(obs) }
    }
    KioskPinningEffect(
        paired = state.device?.paired == true,
        onHomeStateRefresh = { isDefaultHomeApp = KioskMode.isOurPackageDefaultHome(context) },
    )
    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            viewModel.startMatejAssistant()
        } else {
            Toast.makeText(
                context,
                context.getString(R.string.matej_mic_denied),
                Toast.LENGTH_LONG,
            ).show()
        }
    }
    fun requestMatejStart() {
        if (state.device?.paired != true) {
            Toast.makeText(
                context,
                context.getString(R.string.matej_start_need_pairing),
                Toast.LENGTH_SHORT,
            ).show()
            return
        }
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            viewModel.startMatejAssistant()
        } else {
            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }
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
            val phone = pending.first
            val body = pending.second
            val targetContact = smsTarget
            SmsSender.send(context, phone, body).fold(
                onSuccess = {
                    if (targetContact != null) {
                        viewModel.recordOutboundCellularSms(targetContact, body) { result ->
                            result.fold(
                                onSuccess = {
                                    smsTarget = null
                                    smsSendError = null
                                },
                                onFailure = { e ->
                                    smsSendError =
                                        "SMS odeslána. Zápis do cloudu se nepodařil: ${e.message ?: e}"
                                    smsTarget = null
                                },
                            )
                        }
                    } else {
                        smsTarget = null
                        smsSendError = null
                    }
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
                    val act = context as? ComponentActivity
                    if (act != null) {
                        KioskMode.tryStopPinning(act)
                    }
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
            onContactThread = { contact ->
                threadContact = contact
            },
            onStartMatej = { requestMatejStart() },
            showKioskLauncherHint = showKioskLauncherHint,
            modifier = Modifier.fillMaxSize(),
        )
        threadContact?.let { tc ->
            val threadMsgs = state.messages
                .filter { it.belongsToContactThread(tc) }
                .sortedBy { it.createdAt?.toDate()?.time ?: 0L }
            ContactThreadOverlay(
                contact = tc,
                messages = threadMsgs,
                onDismiss = { threadContact = null },
                onReply = {
                    threadContact = null
                    smsSendError = null
                    smsTarget = tc
                },
            )
        }
        smsTarget?.let { contact ->
            SmsComposeOverlay(
                contact = contact,
                useCellularSms = useCellularSms,
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
                        !useCellularSms -> {
                            viewModel.sendTabletFirestoreMessage(contact, body) { result ->
                                result.fold(
                                    onSuccess = {
                                        smsTarget = null
                                        smsSendError = null
                                    },
                                    onFailure = { e ->
                                        smsSendError = e.message ?: "Odeslání se nezdařilo."
                                    },
                                )
                            }
                        }
                        ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) ==
                            PackageManager.PERMISSION_GRANTED -> {
                            SmsSender.send(context, contact.phone, body).fold(
                                onSuccess = {
                                    viewModel.recordOutboundCellularSms(contact, body) { result ->
                                        result.fold(
                                            onSuccess = {
                                                smsTarget = null
                                                smsSendError = null
                                            },
                                            onFailure = { e ->
                                                smsSendError =
                                                    "SMS odeslána. Zápis do cloudu se nepodařil: ${e.message ?: e}"
                                                smsTarget = null
                                            },
                                        )
                                    }
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
        matejDisplayed?.let { ms ->
            MatejAssistantChrome(
                session = ms,
                onDismiss = viewModel::dismissMatej,
                modifier = Modifier.align(
                    if (ms.compact) Alignment.TopEnd else Alignment.TopStart,
                ),
            )
        }
        if (BuildConfig.DEBUG) {
            FloatingActionButton(
                onClick = { requestMatejStart() },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(20.dp)
                    .semantics {
                        contentDescription = context.getString(R.string.matej_debug_fab_cd)
                    },
            ) {
                Text(stringResource(R.string.matej_debug_fab_label))
            }
        }
    }
}

@Composable
private fun KioskPinningEffect(
    paired: Boolean,
    onHomeStateRefresh: () -> Unit,
) {
    val context = LocalContext.current
    val activity = context as? ComponentActivity ?: return
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(paired, lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                onHomeStateRefresh()
                if (paired) {
                    KioskMode.tryStartPinning(activity)
                } else {
                    KioskMode.tryStopPinning(activity)
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onHomeStateRefresh()
        if (paired) {
            KioskMode.tryStartPinning(activity)
        } else {
            KioskMode.tryStopPinning(activity)
        }
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
}
