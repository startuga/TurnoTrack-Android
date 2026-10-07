package com.startuga.turnotrack.domain

import java.time.LocalTime
import java.util.Locale

/** Conversão entre "HH:mm" (formato da web app e dos backups) e [LocalTime]. */
object TimeFormat {
    private val pattern = Regex("""^\s*(\d{1,2}):(\d{1,2})(?::\d{1,2})?\s*$""")

    /**
     * Lê "HH:mm" (aceita "9:05"). "24:00" é tratado como 00:00, o que dá os mesmos
     * resultados que a web app nos cálculos de duração. Valores inválidos dão null.
     */
    fun parse(value: String?): LocalTime? {
        if (value.isNullOrBlank()) return null
        val match = pattern.matchEntire(value) ?: return null
        val hour = match.groupValues[1].toInt()
        val minute = match.groupValues[2].toInt()
        if (hour == 24 && minute == 0) return LocalTime.MIDNIGHT
        if (hour !in 0..23 || minute !in 0..59) return null
        return LocalTime.of(hour, minute)
    }

    fun format(time: LocalTime?): String? =
        time?.let { String.format(Locale.ROOT, "%02d:%02d", it.hour, it.minute) }

    fun formatOrDash(time: LocalTime?): String = format(time) ?: "--:--"
}
