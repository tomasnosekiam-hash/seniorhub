package com.seniorhub.os.util

import com.seniorhub.os.data.Contact
import com.seniorhub.os.data.DeviceMessage
import com.seniorhub.os.data.MvpRepository

/** Odchozí zprávy z tabletu (cloud náhrada nebo zrcadlo SMS). */
fun isDeviceOutboundDelivery(delivery: String?): Boolean {
    return delivery == MvpRepository.VAL_DELIVERY_TABLET_FIRESTORE ||
        delivery == MvpRepository.VAL_DELIVERY_SMS_CELLULAR
}

/** Vlákno s kontaktem = odchozí záznamy nebo příchozí SMS na stejné normalizované číslo. */
fun DeviceMessage.belongsToContactThread(contact: Contact): Boolean {
    val cNorm = normalizePhoneForDial(contact.phone) ?: return false
    if (isDeviceOutboundDelivery(delivery)) {
        val oNorm = outboundPhone?.let { normalizePhoneForDial(it) } ?: return false
        return cNorm == oNorm
    }
    if (delivery == MvpRepository.VAL_DELIVERY_SMS_INBOUND) {
        val iNorm = inboundFromPhone?.let { normalizePhoneForDial(it) } ?: return false
        return cNorm == iNorm
    }
    return false
}
