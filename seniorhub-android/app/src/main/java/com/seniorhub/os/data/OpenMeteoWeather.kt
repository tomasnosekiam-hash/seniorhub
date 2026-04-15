package com.seniorhub.os.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

/**
 * Jednoduchý klient Open-Meteo (bez API klíče). Výchozí souřadnice: Praha.
 */
object OpenMeteoWeather {

    private const val DEFAULT_LAT = 50.0755
    private const val DEFAULT_LON = 14.4378

    suspend fun fetchCurrentSummary(
        latitude: Double? = null,
        longitude: Double? = null,
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val lat = latitude ?: DEFAULT_LAT
            val lon = longitude ?: DEFAULT_LON
            val u = URL(
                "https://api.open-meteo.com/v1/forecast?" +
                    "latitude=$lat&longitude=$lon&current=temperature_2m,weather_code&timezone=auto",
            )
            val conn = (u.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 12_000
                readTimeout = 12_000
            }
            try {
                val code = conn.responseCode
                if (code !in 200..299) {
                    error("HTTP $code")
                }
                val text = conn.inputStream.bufferedReader().use { it.readText() }
                val root = JSONObject(text)
                val current = root.optJSONObject("current") ?: error("current missing")
                val temp = current.optDouble("temperature_2m", Double.NaN)
                if (temp.isNaN()) error("temperature missing")
                val tStr = String.format(Locale.US, "%.0f", temp)
                "Venku cca $tStr °C (Open-Meteo)"
            } finally {
                conn.disconnect()
            }
        }
    }
}
