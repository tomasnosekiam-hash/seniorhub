package com.seniorhub.os.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CallLog
import androidx.core.content.ContextCompat

data class CallHistoryEntry(
    val id: String,
    val phone: String,
    val cachedName: String?,
    val type: CallType,
    val startedAtMillis: Long,
    val durationSeconds: Long,
)

enum class CallType {
    Incoming,
    Outgoing,
    Missed,
    Other,
}

fun readRecentCallHistory(context: Context, limit: Int = 80): List<CallHistoryEntry> {
    if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALL_LOG) !=
        PackageManager.PERMISSION_GRANTED
    ) {
        return emptyList()
    }
    val projection = arrayOf(
        CallLog.Calls._ID,
        CallLog.Calls.NUMBER,
        CallLog.Calls.CACHED_NAME,
        CallLog.Calls.TYPE,
        CallLog.Calls.DATE,
        CallLog.Calls.DURATION,
    )
    val sort = "${CallLog.Calls.DATE} DESC LIMIT ${limit.coerceIn(1, 200)}"
    return runCatching {
        context.contentResolver.query(
            CallLog.Calls.CONTENT_URI,
            projection,
            null,
            null,
            sort,
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(CallLog.Calls._ID)
            val numberCol = cursor.getColumnIndexOrThrow(CallLog.Calls.NUMBER)
            val nameCol = cursor.getColumnIndexOrThrow(CallLog.Calls.CACHED_NAME)
            val typeCol = cursor.getColumnIndexOrThrow(CallLog.Calls.TYPE)
            val dateCol = cursor.getColumnIndexOrThrow(CallLog.Calls.DATE)
            val durationCol = cursor.getColumnIndexOrThrow(CallLog.Calls.DURATION)
            buildList {
                while (cursor.moveToNext()) {
                    val type = when (cursor.getInt(typeCol)) {
                        CallLog.Calls.INCOMING_TYPE -> CallType.Incoming
                        CallLog.Calls.OUTGOING_TYPE -> CallType.Outgoing
                        CallLog.Calls.MISSED_TYPE -> CallType.Missed
                        else -> CallType.Other
                    }
                    add(
                        CallHistoryEntry(
                            id = cursor.getString(idCol),
                            phone = cursor.getString(numberCol).orEmpty(),
                            cachedName = cursor.getString(nameCol)?.trim()?.takeIf { it.isNotEmpty() },
                            type = type,
                            startedAtMillis = cursor.getLong(dateCol),
                            durationSeconds = cursor.getLong(durationCol),
                        ),
                    )
                }
            }
        } ?: emptyList()
    }.getOrDefault(emptyList())
}
