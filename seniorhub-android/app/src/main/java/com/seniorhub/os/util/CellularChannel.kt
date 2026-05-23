package com.seniorhub.os.util

import com.seniorhub.os.data.Contact
import com.seniorhub.os.data.DeviceMessage
import com.seniorhub.os.data.MvpRepository

/** Mobilní kanál zprávy — `sms` = klasická SMS; `rcs` = chat přes data (Google RCS / MMS schránka). */
enum class CellularChannel(val wireValue: String) {
    Sms("sms"),
    Rcs("rcs"),
    ;

    companion object {
        fun fromWire(value: String?): CellularChannel? = when (value?.trim()?.lowercase()) {
            "sms" -> Sms
            "rcs" -> Rcs
            else -> null
        }
    }
}

/** Má aktivní síť vhodnou pro RCS (Wi‑Fi nebo mobilní data). */
fun hasNetworkForRcs(context: android.content.Context): Boolean {
    val (type, _) = readActiveNetworkSummary(context)
    return type == "wifi" || type == "cellular" || type == "ethernet"
}

/**
 * Jakým kanálem odeslat odpověď / novou zprávu kontaktu.
 *
 * - Odpověď na příchozí SMS → vždy SMS.
 * - Odpověď na příchozí RCS → RCS pokud je síť, jinak SMS (úspora vs. neodeslání).
 * - Nová zpráva bez historie → RCS při síti (šetří placené SMS), jinak SMS.
 */
fun resolveOutboundCellularChannel(
    context: android.content.Context,
    contact: Contact,
    threadMessages: List<DeviceMessage>,
    replyTo: DeviceMessage? = null,
): CellularChannel {
    replyTo?.let { msg ->
        if (msg.delivery == MvpRepository.VAL_DELIVERY_SMS_INBOUND) {
            return CellularChannel.fromWire(msg.cellularChannel)
                ?: inferInboundChannelFromLabel(msg.senderDisplayName)
        }
    }
    val lastInbound = threadMessages
        .filter { it.delivery == MvpRepository.VAL_DELIVERY_SMS_INBOUND }
        .maxByOrNull { it.createdAt?.seconds ?: 0L }
    if (lastInbound != null) {
        val ch = CellularChannel.fromWire(lastInbound.cellularChannel)
            ?: inferInboundChannelFromLabel(lastInbound.senderDisplayName)
        if (ch == CellularChannel.Sms) return CellularChannel.Sms
        return if (hasNetworkForRcs(context)) CellularChannel.Rcs else CellularChannel.Sms
    }
    return if (hasNetworkForRcs(context)) CellularChannel.Rcs else CellularChannel.Sms
}

private fun inferInboundChannelFromLabel(senderDisplayName: String?): CellularChannel {
    val label = senderDisplayName?.lowercase().orEmpty()
    return if (label.contains("rcs")) CellularChannel.Rcs else CellularChannel.Sms
}

fun cellularChannelLabel(channel: CellularChannel): String = when (channel) {
    CellularChannel.Sms -> "SMS"
    CellularChannel.Rcs -> "RCS"
}

/** Popisek kanálu u záznamu ve vlákně (Firestore + starší záznamy bez pole). */
fun messageDeliveryTechLabel(message: DeviceMessage): String {
    val ch = CellularChannel.fromWire(message.cellularChannel)
        ?: when (message.delivery) {
            MvpRepository.VAL_DELIVERY_SMS_INBOUND ->
                inferInboundChannelFromLabel(message.senderDisplayName)
            else -> null
        }
    return when (message.delivery) {
        MvpRepository.VAL_DELIVERY_TABLET_FIRESTORE -> "Aplikace (cloud)"
        MvpRepository.VAL_DELIVERY_SMS_CELLULAR ->
            if (ch == CellularChannel.Rcs) "RCS (odchozí)" else "SMS (odchozí)"
        MvpRepository.VAL_DELIVERY_SMS_INBOUND ->
            if (ch == CellularChannel.Rcs) "RCS (příchozí)" else "SMS (příchozí)"
        else -> "Tablet"
    }
}
