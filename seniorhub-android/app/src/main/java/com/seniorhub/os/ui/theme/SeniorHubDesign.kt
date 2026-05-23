package com.seniorhub.os.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Design tokens from `seniorhub-design.pen` — `dashboard-final`, `message`, `message-read`.
 */
object SeniorHubDesign {
    val DashboardBackground = Color(0xFF0F0D14)
    val AccentGold = Color(0xFFEECB96)
    val MenuInactive = Color(0xFF9A95A7)
    val Black = Color(0xFF312631)

    /** Nepřečtená zpráva — pozadí karty (`message`). */
    val MessageSurface = Color(0xFFEECB96)
    /** Přečtená zpráva — pozadí karty (`message-read`). */
    val MessageReadSurface = Color(0xFF35333D)
    /** Text na přečtené kartě (tělo i meta). */
    val MessageReadText = Color(0xFFC2B197)

    val WeatherSurface = Color(0xFF35333D)
    val WeatherText = Color(0xFFE9D8C8)
    val WeatherSun = Color(0xFFFFB52E)
    val WeatherMoon = Color(0xFF1795FF)

    val ContactsSurface = Color(0xFF35333D)
    val ContactsTitle = Color(0xFFE9D8C8)

    /** `dialogue-answer` — obal dialogu, vstup odpovědi, tlačítka. */
    val DialogueAnswerShell = Color(0xFF35333D)
    val ReplyInputBackground = Color(0xFF0F0D14)
    val ReplyInputText = Color(0xFFD8D5E1)
    val ReplyInputBorder = Color(0xFFD8D5E1)

    fun messageBodyStyle(read: Boolean) = TextStyle(
        fontFamily = FontFamily.Serif,
        fontSize = 22.sp,
        fontWeight = FontWeight.Normal,
        letterSpacing = (-0.24).sp,
        lineHeight = 33.sp,
        color = if (read) MessageReadText else Black,
    )

    fun messageMetaStyle(read: Boolean) = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 14.sp,
        fontWeight = FontWeight.Normal,
        color = if (read) MessageReadText else Black,
    )
}
