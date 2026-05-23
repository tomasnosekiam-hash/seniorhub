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
    if (isDeviceOutboundDelivery(delivery)) {
        val outbound = outboundPhone ?: return false
        return phonesMatchForThread(outbound, contact.phone)
    }
    if (delivery == MvpRepository.VAL_DELIVERY_SMS_INBOUND) {
        val inbound = inboundFromPhone ?: return false
        return phonesMatchForThread(inbound, contact.phone)
    }
    return false
}
