package com.startuga.turnotrack.domain

import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth

/**
 * Cálculos de horas, atrasos e totais.
 * Tradução direta de `utils.ts` da web app — os testes unitários confirmam que os
 * resultados são idênticos (incluindo turnos que passam a meia-noite).
 */
object WorkCalculations {

    private const val MINUTES_PER_DAY = 24 * 60

    private fun LocalTime.minuteOfDay(): Int = hour * 60 + minute

    /** Minutos entre duas horas. Se o fim for antes do início, o turno passou a meia-noite. */
    fun minutesBetween(start: LocalTime?, end: LocalTime?): Int {
        if (start == null || end == null) return 0
        val s = start.minuteOfDay()
        val e = end.minuteOfDay()
        return if (e < s) e + MINUTES_PER_DAY - s else e - s
    }

    /**
     * Minutos de atraso face ao horário previsto.
     * Diferenças de mais de 12h são interpretadas como mudança de dia
     * (ex.: previsto 23:30, entrada 00:10 → 40 min de atraso).
     */
    fun lateness(scheduled: LocalTime?, actual: LocalTime?): Int {
        if (scheduled == null || actual == null) return 0
        var diff = actual.minuteOfDay() - scheduled.minuteOfDay()
        if (diff > 720) {
            diff -= MINUTES_PER_DAY
        } else if (diff < -720) {
            diff += MINUTES_PER_DAY
        }
        return if (diff > 0) diff else 0
    }

    fun dailyStats(entry: WorkEntry): DailyStats {
        var worked = 0
        var holiday = 0
        var overtime = 0
        var late = 0

        if (entry.type == ShiftType.REGULAR) {
            val duration = if (entry.actualStart != null && entry.actualEnd != null) {
                minutesBetween(entry.actualStart, entry.actualEnd)
            } else 0

            if (entry.isHoliday) holiday = duration else worked = duration

            if (entry.scheduledStart != null && entry.actualStart != null) {
                late = lateness(entry.scheduledStart, entry.actualStart)
            }
            if (entry.overtimeStart != null && entry.overtimeEnd != null) {
                overtime += minutesBetween(entry.overtimeStart, entry.overtimeEnd)
            }
        }

        return DailyStats(
            date = entry.date,
            workedMinutes = worked,
            holidayMinutes = holiday,
            overtimeMinutes = overtime,
            lateMinutes = late,
            isAbsent = entry.type == ShiftType.ABSENT,
            isDayOff = entry.type == ShiftType.DAY_OFF,
            isVacation = entry.type == ShiftType.VACATION,
        )
    }

    fun aggregate(entries: Iterable<WorkEntry>, filter: (WorkEntry) -> Boolean): PeriodStats {
        var stats = PeriodStats()
        for (entry in entries) {
            if (!filter(entry)) continue
            val daily = dailyStats(entry)
            val worked = !daily.isAbsent && !daily.isDayOff && !daily.isVacation
            stats = stats.copy(
                totalWorkedMinutes = stats.totalWorkedMinutes + daily.workedMinutes,
                totalHolidayMinutes = stats.totalHolidayMinutes + daily.holidayMinutes,
                totalOvertimeMinutes = stats.totalOvertimeMinutes + daily.overtimeMinutes,
                totalLateMinutes = stats.totalLateMinutes + daily.lateMinutes,
                daysAbsent = stats.daysAbsent + if (daily.isAbsent) 1 else 0,
                daysOff = stats.daysOff + if (daily.isDayOff) 1 else 0,
                daysVacation = stats.daysVacation + if (daily.isVacation) 1 else 0,
                daysWorked = stats.daysWorked + if (worked) 1 else 0,
                daysHoliday = stats.daysHoliday + if (worked && entry.isHoliday) 1 else 0,
            )
        }
        return stats
    }

    fun monthStats(entries: Iterable<WorkEntry>, month: YearMonth): PeriodStats =
        aggregate(entries) { YearMonth.from(it.date) == month }

    fun yearStats(entries: Iterable<WorkEntry>, year: Int): PeriodStats =
        aggregate(entries) { it.date.year == year }

    /** Uma barra por dia do mês (dias sem registo ficam a zero). */
    fun dailyChart(entries: Iterable<WorkEntry>, month: YearMonth): List<ChartBar> {
        val byDate = entries.filter { YearMonth.from(it.date) == month }.associateBy { it.date }
        return (1..month.lengthOfMonth()).map { day ->
            val entry = byDate[LocalDate.of(month.year, month.monthValue, day)]
            val stats = entry?.let { dailyStats(it) }
            ChartBar(
                label = day.toString(),
                workedHours = (stats?.workedMinutes ?: 0) / 60f,
                holidayHours = (stats?.holidayMinutes ?: 0) / 60f,
                overtimeHours = (stats?.overtimeMinutes ?: 0) / 60f,
            )
        }
    }

    /** Uma barra por mês do ano. */
    fun monthlyChart(entries: Iterable<WorkEntry>, year: Int, monthLabels: List<String>): List<ChartBar> {
        val worked = IntArray(12)
        val holiday = IntArray(12)
        val overtime = IntArray(12)
        for (entry in entries) {
            if (entry.date.year != year) continue
            val i = entry.date.monthValue - 1
            val stats = dailyStats(entry)
            worked[i] += stats.workedMinutes
            holiday[i] += stats.holidayMinutes
            overtime[i] += stats.overtimeMinutes
        }
        return (0 until 12).map { i ->
            ChartBar(
                label = monthLabels[i],
                workedHours = worked[i] / 60f,
                holidayHours = holiday[i] / 60f,
                overtimeHours = overtime[i] / 60f,
            )
        }
    }

    /** "8h 30m" — igual a `minutesToHours` da web app (valores ≤ 0 dão "0h 0m"). */
    fun formatMinutes(minutes: Int): String {
        if (minutes <= 0) return "0h 0m"
        return "${minutes / 60}h ${minutes % 60}m"
    }
}
