package com.seniorhub.os.matej

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build

internal fun Context.hasValidatedInternet(): Boolean {
    val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
    @Suppress("DEPRECATION")
    return cm.activeNetworkInfo?.isConnected == true
}
