package com.startuga.turnotrack.ui

import androidx.compose.ui.graphics.Color
import com.startuga.turnotrack.domain.ShiftType
import com.startuga.turnotrack.domain.WorkEntry
import com.startuga.turnotrack.ui.theme.ShiftColors
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

object Fmt {
    val PT: Locale = Locale.forLanguageTag("pt-PT")

    val MONTHS = listOf(
        "Janeiro", "Fevereiro", "Março", "Abril", "Maio", "Junho",
        "Julho", "Agosto", "Setembro", "Outubro", "Novembro", "Dezembro",
    )
    val MONTHS_SHORT = MONTHS.map { it.take(3) }

    /** Domingo primeiro, como na web app. */
    val WEEKDAYS = listOf("Dom", "Seg", "Ter", "Qua", "Qui", "Sex", "Sáb")

    fun month(month: YearMonth): String = "${MONTHS[month.monthValue - 1]} ${month.year}"

    private val longDate = DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM 'de' yyyy", PT)

    /** "Terça-feira, 7 de outubro de 2026" */
    fun longDate(date: LocalDate): String =
        date.format(longDate).replaceFirstChar { it.titlecase(PT) }

    fun hoursLabel(hours: Float): String {
        val minutes = Math.round(hours * 60)
        val h = minutes / 60
        val m = minutes % 60
        return if (m == 0) "${h}h" else "${h}h ${m}m"
    }
}

/** (fundo, texto) para um registo. */
fun ShiftColors.forEntry(entry: WorkEntry): Pair<Color, Color> = when (entry.type) {
    ShiftType.REGULAR -> if (entry.isHoliday) holiday else regular
    ShiftType.DAY_OFF -> dayOff
    ShiftType.ABSENT -> absent
    ShiftType.VACATION -> vacation
}

fun ShiftColors.forType(type: ShiftType): Pair<Color, Color> = when (type) {
    ShiftType.REGULAR -> regular
    ShiftType.DAY_OFF -> dayOff
    ShiftType.ABSENT -> absent
    ShiftType.VACATION -> vacation
}
