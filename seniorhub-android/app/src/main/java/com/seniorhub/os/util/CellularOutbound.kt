package com.seniorhub.os.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Telephony
import android.util.Log

/**
 * Odchozí mobilní zpráva — SMS přes [SmsManager], RCS přes výchozí aplikaci Zpráv
 * ([Telephony.Sms.Intents.ACTION_RESPOND_VIA_MESSAGE], pokud ji systém nabízí).
 */
object CellularOutbound {
    private const val TAG = "CellularOutbound"

    data class SendOutcome(
        val channelUsed: CellularChannel,
        val fellBackFromRcs: Boolean = false,
    )

    fun send(
        context: Context,
        phone: String,
        body: String,
        preferred: CellularChannel,
    ): Result<SendOutcome> {
        return when (preferred) {
            CellularChannel.Sms -> SmsSender.send(context, phone, body).map {
                SendOutcome(channelUsed = CellularChannel.Sms)
            }
            CellularChannel.Rcs -> sendRcsOrFallbackSms(context, phone, body)
        }
    }

    private fun sendRcsOrFallbackSms(
        context: Context,
        phone: String,
        body: String,
    ): Result<SendOutcome> {
        if (!hasNetworkForRcs(context)) {
            return SmsSender.send(context, phone, body).map {
                SendOutcome(channelUsed = CellularChannel.Sms, fellBackFromRcs = true)
            }
        }
        RcsOutboundSender.send(context, phone, body).fold(
            onSuccess = {
                return Result.success(SendOutcome(channelUsed = CellularChannel.Rcs))
            },
            onFailure = { rcsErr ->
                Log.w(TAG, "RCS send failed, falling back to SMS", rcsErr)
                return SmsSender.send(context, phone, body).map {
                    SendOutcome(channelUsed = CellularChannel.Sms, fellBackFromRcs = true)
                }
            },
        )
    }
}

/**
 * Požádá výchozí SMS aplikaci (typicky Google Zprávy) o odeslání přes její transport (RCS, pokud jde).
 * Není to veřejné RCS API — funguje jen pokud je Zprávy výchozí SMS a služba odpoví.
 */
internal object RcsOutboundSender {
    private const val TAG = "RcsOutboundSender"
    private const val ACTION_RESPOND_VIA_MESSAGE = "android.intent.action.RESPOND_VIA_MESSAGE"

    fun send(context: Context, phone: String, body: String): Result<Unit> {
        val dest = normalizePhoneForDial(phone)
            ?: return Result.failure(IllegalArgumentException("Neplatné telefonní číslo."))
        val text = body.trim()
        if (text.isEmpty()) return Result.failure(IllegalArgumentException("Zpráva je prázdná."))

        val defaultPkg = Telephony.Sms.getDefaultSmsPackage(context)
        if (defaultPkg.isNullOrBlank()) {
            return Result.failure(IllegalStateException("Není nastavena výchozí aplikace pro SMS."))
        }

        val intent = Intent(ACTION_RESPOND_VIA_MESSAGE).apply {
            data = Uri.fromParts("smsto", dest, null)
            putExtra(Intent.EXTRA_TEXT, text)
            putExtra("sms_body", text)
            setPackage(defaultPkg)
            addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)
        }

        return runCatching {
            val component = intent.resolveActivity(context.packageManager)
            if (component == null) {
                throw IllegalStateException("Výchozí aplikace pro zprávy neumí odeslat na pozadí.")
            }
            @Suppress("DEPRECATION")
            val started = context.startService(intent)
            if (started == null) {
                throw IllegalStateException("Výchozí aplikace pro zprávy neodpověděla na odeslání.")
            }
            Unit
        }.onFailure { e ->
            Log.e(TAG, "respond-via-message failed for $defaultPkg", e)
        }
    }
}
