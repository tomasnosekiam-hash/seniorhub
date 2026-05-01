package com.seniorhub.os.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.telecom.TelecomManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.seniorhub.os.data.Contact
import com.seniorhub.os.util.CellularSmsCapability
import com.seniorhub.os.util.KioskMode
import com.seniorhub.os.util.RemoteAudioVolume
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
    var addContactOpen by remember { mutableStateOf(false) }
    var addContactError by remember { mutableStateOf<String?>(null) }
    var isDefaultHomeApp by remember {
        mutableStateOf(KioskMode.isOurPackageDefaultHome(context))
    }
    val showKioskLauncherHint = state.device?.paired == true && !isDefaultHomeApp
    var permissionEpoch by remember { mutableIntStateOf(0) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                permissionEpoch++
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    val communicationPermissions = remember(permissionEpoch) {
        communicationPermissionsOf(context)
    }
    val communicationPermissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) {
        permissionEpoch++
    }
    fun placeOutgoingCall(rawPhone: String) {
        val act = context as? ComponentActivity
        val telecom = context.getSystemService(TelecomManager::class.java)
        val seniorHubIsDialer = telecom?.defaultDialerPackage == context.packageName
        if (act != null && !seniorHubIsDialer) {
            KioskMode.tryStopPinning(act)
        }
        if (!context.startOutgoingCall(rawPhone)) {
            context.openDialPad(rawPhone)
        }
    }
    LaunchedEffect(state.device?.volumePercent) {
        state.device?.volumePercent?.let { pct ->
            RemoteAudioVolume.apply(context.applicationContext, pct)
        }
    }
    KioskPinningEffect(
        paired = state.device?.paired == true,
        isDefaultHomeApp = isDefaultHomeApp,
        onHomeStateRefresh = { isDefaultHomeApp = KioskMode.isOurPackageDefaultHome(context) },
    )
    val callPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        val raw = pendingCallPhone
        pendingCallPhone = null
        if (raw == null) return@rememberLauncherForActivityResult
        if (granted) {
            placeOutgoingCall(raw)
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
                        placeOutgoingCall(rawPhone)
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
            onSendContactMessage = { contact, body, onDone ->
                sendMessageToContact(
                    context = context,
                    viewModel = viewModel,
                    contact = contact,
                    body = body,
                    useCellularSms = useCellularSms,
                    onNeedPermission = {
                        pendingSms = contact.phone to body
                        smsTarget = contact
                        smsPermissionLauncher.launch(Manifest.permission.SEND_SMS)
                    },
                    onDone = onDone,
                )
            },
            showKioskLauncherHint = showKioskLauncherHint,
            communicationPermissions = communicationPermissions,
            onRequestCommunicationPermissions = {
                communicationPermissionsLauncher.launch(communicationPermissionArray)
            },
            onOpenAppSettings = {
                context.startActivity(
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    },
                )
            },
            onAddContact = {
                addContactError = null
                addContactOpen = true
            },
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
                    sendMessageToContact(
                        context = context,
                        viewModel = viewModel,
                        contact = contact,
                        body = body,
                        useCellularSms = useCellularSms,
                        onNeedPermission = {
                            pendingSms = contact.phone to body
                            smsPermissionLauncher.launch(Manifest.permission.SEND_SMS)
                        },
                    ) { result ->
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
                },
            )
        }
        if (addContactOpen) {
            AddContactOverlay(
                errorMessage = addContactError,
                onDismiss = {
                    addContactOpen = false
                    addContactError = null
                },
                onSave = { name, phone, avatarUri ->
                    viewModel.addContact(name, phone, avatarUri) { result ->
                        result.fold(
                            onSuccess = {
                                addContactOpen = false
                                addContactError = null
                            },
                            onFailure = { e ->
                                addContactError = e.message ?: e.toString()
                            },
                        )
                    }
                },
            )
        }
    }
}

private fun sendMessageToContact(
    context: android.content.Context,
    viewModel: HomeViewModel,
    contact: Contact,
    body: String,
    useCellularSms: Boolean,
    onNeedPermission: () -> Unit,
    onDone: (Result<Unit>) -> Unit,
) {
    when {
        normalizePhoneForDial(contact.phone) == null -> {
            onDone(Result.failure(IllegalArgumentException("Neplatné telefonní číslo.")))
        }
        !useCellularSms -> {
            viewModel.sendTabletFirestoreMessage(contact, body, onDone)
        }
        ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) ==
            PackageManager.PERMISSION_GRANTED -> {
            SmsSender.send(context, contact.phone, body).fold(
                onSuccess = {
                    viewModel.recordOutboundCellularSms(contact, body) { result ->
                        result.fold(
                            onSuccess = { onDone(Result.success(Unit)) },
                            onFailure = { e ->
                                onDone(
                                    Result.failure(
                                        IllegalStateException(
                                            "SMS odeslána. Zápis do cloudu se nepodařil: ${e.message ?: e}",
                                        ),
                                    ),
                                )
                            },
                        )
                    }
                },
                onFailure = { e -> onDone(Result.failure(e)) },
            )
        }
        else -> onNeedPermission()
    }
}

@Composable
private fun KioskPinningEffect(
    paired: Boolean,
    isDefaultHomeApp: Boolean,
    onHomeStateRefresh: () -> Unit,
) {
    val context = LocalContext.current
    val activity = context as? ComponentActivity ?: return
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(paired, lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                onHomeStateRefresh()
                if (paired && !isDefaultHomeApp) {
                    KioskMode.tryStartPinning(activity)
                } else {
                    KioskMode.tryStopPinning(activity)
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onHomeStateRefresh()
        if (paired && !isDefaultHomeApp) {
            KioskMode.tryStartPinning(activity)
        } else {
            KioskMode.tryStopPinning(activity)
        }
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
}
