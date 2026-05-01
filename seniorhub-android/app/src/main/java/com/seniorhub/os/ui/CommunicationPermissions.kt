package com.seniorhub.os.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

/**
 * Oprávnění potřebná pro odchozí hovor, odchozí SMS a příjem SMS (vlákno u kontaktu).
 */
data class CommunicationPermissions(
    val callGranted: Boolean,
    val sendSmsGranted: Boolean,
    val receiveSmsGranted: Boolean,
    val callLogGranted: Boolean,
) {
    val allGranted: Boolean
        get() = callGranted && sendSmsGranted && receiveSmsGranted && callLogGranted

    companion object {
        val AllGranted = CommunicationPermissions(true, true, true, true)
    }
}

fun communicationPermissionsOf(context: Context): CommunicationPermissions {
    return CommunicationPermissions(
        callGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CALL_PHONE,
        ) == PackageManager.PERMISSION_GRANTED,
        sendSmsGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.SEND_SMS,
        ) == PackageManager.PERMISSION_GRANTED,
        receiveSmsGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECEIVE_SMS,
        ) == PackageManager.PERMISSION_GRANTED,
        callLogGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CALL_LOG,
        ) == PackageManager.PERMISSION_GRANTED,
    )
}

val communicationPermissionArray: Array<String> = arrayOf(
    Manifest.permission.CALL_PHONE,
    Manifest.permission.SEND_SMS,
    Manifest.permission.RECEIVE_SMS,
    Manifest.permission.READ_CALL_LOG,
)
