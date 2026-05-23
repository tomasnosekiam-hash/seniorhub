package com.seniorhub.os.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.roundToInt

/** Počasí dle `dashboard-final` — odpolední a noční teplota (denní max / min). */
data class DayNightWeather(
    val afternoonTempC: Int,
    val nightTempC: Int,
)

/**
 * Jednoduchý klient Open-Meteo (bez API klíče). Výchozí souřadnice: Praha.
 */
object OpenMeteoWeather {

    private const val DEFAULT_LAT = 50.0755
    private const val DEFAULT_LON = 14.4378

    suspend fun fetchDayNightTemperatures(
        latitude: Double? = null,
        longitude: Double? = null,
    ): Result<DayNightWeather> = withContext(Dispatchers.IO) {
        runCatching {
            val lat = latitude ?: DEFAULT_LAT
            val lon = longitude ?: DEFAULT_LON
            val u = URL(
                "https://api.open-meteo.com/v1/forecast?" +
                    "latitude=$lat&longitude=$lon" +
                    "&daily=temperature_2m_max,temperature_2m_min" +
                    "&timezone=auto&forecast_days=1",
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
                val daily = JSONObject(text).optJSONObject("daily") ?: error("daily missing")
                val maxArr = daily.optJSONArray("temperature_2m_max") ?: error("max missing")
                val minArr = daily.optJSONArray("temperature_2m_min") ?: error("min missing")
                if (maxArr.length() == 0 || minArr.length() == 0) error("empty forecast")
                val afternoon = maxArr.optDouble(0, Double.NaN)
                val night = minArr.optDouble(0, Double.NaN)
                if (afternoon.isNaN() || night.isNaN()) error("temperature missing")
                DayNightWeather(
                    afternoonTempC = afternoon.roundToInt(),
                    nightTempC = night.roundToInt(),
                )
            } finally {
                conn.disconnect()
            }
        }
    }

    /** @deprecated Prefer [fetchDayNightTemperatures] for dashboard widget. */
    suspend fun fetchCurrentSummary(
        latitude: Double? = null,
        longitude: Double? = null,
    ): Result<String> = fetchDayNightTemperatures(latitude, longitude).map { w ->
        "Odpoledne ${w.afternoonTempC} °C · V noci ${w.nightTempC} °C"
    }
}
