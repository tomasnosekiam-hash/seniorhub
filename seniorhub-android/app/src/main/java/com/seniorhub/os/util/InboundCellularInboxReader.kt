package com.seniorhub.os.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Telephony
import androidx.core.content.ContextCompat

data class CellularInboxEntry(
    val dedupKey: String,
    val address: String,
    val body: String,
    val receivedAtMillis: Long,
    val viaRcs: Boolean,
)

/**
 * SMS schránka + MMS (Google RCS/Chat často zapisuje jen do [Telephony.Mms], ne do [Telephony.Sms]).
 */
fun readRecentCellularInbox(
    context: Context,
    sinceMillis: Long,
    limitPerChannel: Int = 40,
): List<CellularInboxEntry> {
    val sms = readRecentInboxSms(context, sinceMillis, limitPerChannel).map { sms ->
        CellularInboxEntry(
            dedupKey = "sms:${sms.telephonyId}",
            address = sms.address,
            body = sms.body,
            receivedAtMillis = sms.receivedAtMillis,
            viaRcs = false,
        )
    }
    val mms = readRecentInboxMms(context, sinceMillis, limitPerChannel)
    return (sms + mms)
        .sortedByDescending { it.receivedAtMillis }
        .take(limitPerChannel * 2)
}

/** Klasické SMS ze schránky — interní pro [readRecentCellularInbox]. */
internal data class InboxSmsEntry(
    val telephonyId: Long,
    val address: String,
    val body: String,
    val receivedAtMillis: Long,
)

internal fun readRecentInboxSms(
    context: Context,
    sinceMillis: Long,
    limit: Int = 40,
): List<InboxSmsEntry> {
    if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) !=
        PackageManager.PERMISSION_GRANTED
    ) {
        return emptyList()
    }
    val projection = arrayOf(
        Telephony.Sms._ID,
        Telephony.Sms.ADDRESS,
        Telephony.Sms.BODY,
        Telephony.Sms.DATE,
        Telephony.Sms.TYPE,
    )
    val selection = "${Telephony.Sms.TYPE} = ? AND ${Telephony.Sms.DATE} >= ?"
    val selectionArgs = arrayOf(
        Telephony.Sms.MESSAGE_TYPE_INBOX.toString(),
        sinceMillis.toString(),
    )
    val sort = "${Telephony.Sms.DATE} DESC LIMIT ${limit.coerceIn(1, 100)}"
    return runCatching {
        context.contentResolver.query(
            Telephony.Sms.CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            sort,
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(Telephony.Sms._ID)
            val addressCol = cursor.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
            val bodyCol = cursor.getColumnIndexOrThrow(Telephony.Sms.BODY)
            val dateCol = cursor.getColumnIndexOrThrow(Telephony.Sms.DATE)
            buildList {
                while (cursor.moveToNext()) {
                    val body = cursor.getString(bodyCol)?.trim().orEmpty()
                    if (body.isEmpty()) continue
                    val address = cursor.getString(addressCol)?.trim().orEmpty()
                    if (address.isEmpty()) continue
                    add(
                        InboxSmsEntry(
                            telephonyId = cursor.getLong(idCol),
                            address = address,
                            body = body,
                            receivedAtMillis = cursor.getLong(dateCol),
                        ),
                    )
                }
            }
        } ?: emptyList()
    }.getOrDefault(emptyList())
}

private fun readRecentInboxMms(
    context: Context,
    sinceMillis: Long,
    limit: Int,
): List<CellularInboxEntry> {
    if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) !=
        PackageManager.PERMISSION_GRANTED
    ) {
        return emptyList()
    }
    val sinceSeconds = sinceMillis / 1000L
    val projection = arrayOf(
        Telephony.Mms._ID,
        Telephony.Mms.DATE,
        Telephony.Mms.MESSAGE_BOX,
    )
    val selection = "${Telephony.Mms.MESSAGE_BOX} = ? AND ${Telephony.Mms.DATE} >= ?"
    val selectionArgs = arrayOf(
        Telephony.Mms.MESSAGE_BOX_INBOX.toString(),
        sinceSeconds.toString(),
    )
    val sort = "${Telephony.Mms.DATE} DESC LIMIT ${limit.coerceIn(1, 100)}"
    return runCatching {
        context.contentResolver.query(
            Telephony.Mms.CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            sort,
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(Telephony.Mms._ID)
            val dateCol = cursor.getColumnIndexOrThrow(Telephony.Mms.DATE)
            buildList {
                while (cursor.moveToNext()) {
                    val mmsId = cursor.getLong(idCol)
                    val body = readMmsText(context, mmsId)
                    if (body.isBlank()) continue
                    val address = readMmsSenderAddress(context, mmsId) ?: continue
                    val dateRaw = cursor.getLong(dateCol)
                    val receivedAtMillis = normalizeProviderDateToMillis(dateRaw)
                    add(
                        CellularInboxEntry(
                            dedupKey = "mms:$mmsId",
                            address = address,
                            body = body,
                            receivedAtMillis = receivedAtMillis,
                            viaRcs = true,
                        ),
                    )
                }
            }
        } ?: emptyList()
    }.getOrDefault(emptyList())
}

/** MMS [Telephony.Mms.DATE] bývá v sekundách, SMS v milisekundách. */
private fun normalizeProviderDateToMillis(raw: Long): Long =
    if (raw < 10_000_000_000L) raw * 1000L else raw

private fun readMmsText(context: Context, mmsId: Long): String {
    val partUri = Uri.parse("content://mms/part")
    val selection = "${Telephony.Mms.Part.MSG_ID} = ?"
    val selectionArgs = arrayOf(mmsId.toString())
    return runCatching {
        context.contentResolver.query(
            partUri,
            arrayOf(Telephony.Mms.Part.TEXT, Telephony.Mms.Part.CONTENT_TYPE),
            selection,
            selectionArgs,
            null,
        )?.use { cursor ->
            val textCol = cursor.getColumnIndex(Telephony.Mms.Part.TEXT)
            val typeCol = cursor.getColumnIndex(Telephony.Mms.Part.CONTENT_TYPE)
            buildString {
                while (cursor.moveToNext()) {
                    val ct = if (typeCol >= 0) cursor.getString(typeCol).orEmpty() else "text/plain"
                    if (!ct.startsWith("text/", ignoreCase = true)) continue
                    if (textCol < 0) continue
                    append(cursor.getString(textCol).orEmpty())
                }
            }.trim()
        }.orEmpty()
    }.getOrDefault("")
}

private fun readMmsSenderAddress(context: Context, mmsId: Long): String? {
    val addrUri = Uri.parse("content://mms/$mmsId/addr")
    return runCatching {
        context.contentResolver.query(
            addrUri,
            arrayOf(Telephony.Mms.Addr.ADDRESS, Telephony.Mms.Addr.TYPE),
            null,
            null,
            null,
        )?.use { cursor ->
            val addressCol = cursor.getColumnIndex(Telephony.Mms.Addr.ADDRESS)
            val typeCol = cursor.getColumnIndex(Telephony.Mms.Addr.TYPE)
            var fromAddress: String? = null
            var fallback: String? = null
            while (cursor.moveToNext()) {
                if (addressCol < 0) continue
                val address = cursor.getString(addressCol)?.trim().orEmpty()
                if (address.isEmpty()) continue
                val type = if (typeCol >= 0) cursor.getInt(typeCol) else -1
                when (type) {
                    MMS_ADDR_TYPE_FROM -> {
                        fromAddress = address
                        break
                    }
                    MMS_ADDR_TYPE_TO -> Unit
                    else -> if (fallback == null) fallback = address
                }
            }
            fromAddress ?: fallback
        }
    }.getOrNull()?.trim()?.takeIf { it.isNotEmpty() }
}

/** [Telephony.Mms.Addr] type constants (API 19+). */
private const val MMS_ADDR_TYPE_FROM = 137
private const val MMS_ADDR_TYPE_TO = 151
