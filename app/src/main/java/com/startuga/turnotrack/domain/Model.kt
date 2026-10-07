package com.startuga.turnotrack.domain

import java.time.LocalDate
import java.time.LocalTime

/** Tipos de registo — os mesmos nomes da web app, para os backups JSON serem compatíveis. */
enum class ShiftType(val label: String) {
    REGULAR("Normal"),
    DAY_OFF("Folga"),
    ABSENT("Falta"),
    VACATION("Férias");

    companion object {
        fun fromName(name: String?): ShiftType? = entries.firstOrNull { it.name == name }
    }
}

/** Um registo por dia (a data é a chave, tal como na web app). */
data class WorkEntry(
    val id: String,
    val date: LocalDate,
    val type: ShiftType,
    /** Turno normal feito num feriado: as horas contam como "horas de feriado". */
    val isHoliday: Boolean = false,
    val scheduledStart: LocalTime? = null,
    val scheduledEnd: LocalTime? = null,
    val actualStart: LocalTime? = null,
    val actualEnd: LocalTime? = null,
    /** Horas extra explícitas, separadas do turno normal. */
    val overtimeStart: LocalTime? = null,
    val overtimeEnd: LocalTime? = null,
    val notes: String? = null,
) {
    companion object {
        fun newId(): String = System.currentTimeMillis().toString()
    }
}

data class DailyStats(
    val date: LocalDate,
    val workedMinutes: Int,
    val holidayMinutes: Int,
    val overtimeMinutes: Int,
    val lateMinutes: Int,
    val isAbsent: Boolean,
    val isDayOff: Boolean,
    val isVacation: Boolean,
)

/** Totais de um período (mês ou ano). Equivale a `MonthlyStats` da web app. */
data class PeriodStats(
    val totalWorkedMinutes: Int = 0,
    val totalHolidayMinutes: Int = 0,
    val totalOvertimeMinutes: Int = 0,
    val totalLateMinutes: Int = 0,
    val daysAbsent: Int = 0,
    val daysOff: Int = 0,
    val daysWorked: Int = 0,
    val daysHoliday: Int = 0,
    val daysVacation: Int = 0,
)

/** Uma barra do gráfico, em horas. */
data class ChartBar(
    val label: String,
    val workedHours: Float,
    val holidayHours: Float,
    val overtimeHours: Float,
) {
    val totalHours: Float get() = workedHours + holidayHours + overtimeHours
}
