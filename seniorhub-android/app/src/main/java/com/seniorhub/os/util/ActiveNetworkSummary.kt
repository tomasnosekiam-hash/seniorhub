package com.seniorhub.os.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
/**
 * Krátký popis aktivní sítě pro zápis do `devices/.../status` (bez citlivých údajů).
 */
fun readActiveNetworkSummary(context: Context): Pair<String?, String?> {
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        ?: return null to null
    val network = cm.activeNetwork ?: return "offline" to "Bez připojení"
    val caps = cm.getNetworkCapabilities(network) ?: return "unknown" to "Neznámá síť"
    val type = when {
        caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "wifi"
        caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "cellular"
        caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "ethernet"
        else -> "unknown"
    }
    val label = when (type) {
        "wifi" -> "Wi‑Fi"
        "cellular" -> "Mobilní data"
        "ethernet" -> "Ethernet"
        "offline" -> "Bez připojení"
        else -> "Síť"
    }
    return type to label
}
