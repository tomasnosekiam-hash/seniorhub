package com.seniorhub.os

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.seniorhub.os.data.AppRole
import com.seniorhub.os.data.AppRoleStore
import com.seniorhub.os.data.DeviceIdentityStore
import com.seniorhub.os.data.MvpRepository
import kotlin.concurrent.thread
import kotlinx.coroutines.runBlocking

/**
 * Příjem klasických SMS — pokud číslo odpovídá kontaktu ve Firestore, zrcadlí zprávu do [MvpRepository.SUB_MESSAGES]
 * (stejné vlákno jako odchozí SMS / cloud zprávy).
 */
class IncomingSmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return
        val pendingResult = goAsync()
        val appContext = context.applicationContext
        thread(name = "seniorhub-incoming-sms") {
            try {
                runBlocking {
                    if (AppRoleStore(appContext).getRoleOrNull() != AppRole.Senior) return@runBlocking

                    val pdus = Telephony.Sms.Intents.getMessagesFromIntent(intent)
                    if (pdus.isEmpty()) return@runBlocking
                    val from = pdus[0].originatingAddress?.trim().orEmpty()
                    if (from.isEmpty()) return@runBlocking
                    val fullBody = buildString {
                        for (m in pdus) {
                            append(m.messageBody ?: "")
                        }
                    }
                    if (fullBody.isBlank()) return@runBlocking

                    val deviceId = DeviceIdentityStore(appContext).getOrCreateDeviceId()
                    val repo = MvpRepository(
                        FirebaseFirestore.getInstance(),
                        FirebaseAuth.getInstance(),
                        deviceId,
                    )
                    val contact = repo.findContactForIncomingPhone(from) ?: return@runBlocking
                    repo.recordInboundCellularSms(from, fullBody, contact)
                }
            } catch (_: Throwable) {
            } finally {
                pendingResult.finish()
            }
        }
    }
}
