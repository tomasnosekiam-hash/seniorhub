package com.seniorhub.os.util

import android.content.Context
import android.util.Log
import com.seniorhub.os.data.AppRole
import com.seniorhub.os.data.AppRoleStore
import com.seniorhub.os.data.InboundCellularDedupStore
import com.seniorhub.os.data.MvpRepository

private const val TAG = "InboundCellularSync"
private const val LOOKBACK_MS = 7L * 24 * 60 * 60 * 1000

/**
 * Zrcadlí nové příchozí zprávy ze systémové schránky (SMS + MMS/RCS) do Firestore u známého kontaktu.
 * Doplňuje [com.seniorhub.os.IncomingSmsReceiver] — ten zachytí jen klasické SMS broadcasty.
 */
suspend fun syncInboundCellularMessages(
    context: Context,
    repository: MvpRepository,
): Int {
    if (AppRoleStore(context).getRoleOrNull() != AppRole.Senior) return 0
    val since = System.currentTimeMillis() - LOOKBACK_MS
    val inbox = readRecentCellularInbox(context, sinceMillis = since)
    if (inbox.isEmpty()) return 0

    val dedup = InboundCellularDedupStore(context)
    var imported = 0
    for (entry in inbox) {
        if (dedup.contains(entry.dedupKey)) continue
        val contact = repository.findContactForIncomingPhone(entry.address)
        if (contact == null) {
            Log.d(TAG, "skip ${entry.dedupKey}: no contact for sender")
            continue
        }
        runCatching {
            repository.recordInboundCellularSms(
                rawFromAddress = entry.address,
                body = entry.body,
                matchedContact = contact,
                viaRcs = entry.viaRcs,
            )
            dedup.markProcessed(entry.dedupKey)
            imported++
            Log.d(TAG, "imported ${entry.dedupKey} rcs=${entry.viaRcs} contact=${contact.name}")
        }.onFailure { e ->
            Log.w(TAG, "import failed ${entry.dedupKey}", e)
        }
    }
    if (imported > 0) {
        Log.i(TAG, "synced $imported cellular message(s) from SMS/MMS inbox")
    }
    return imported
}
