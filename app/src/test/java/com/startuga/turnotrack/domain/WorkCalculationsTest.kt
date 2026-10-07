package com.startuga.turnotrack.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth

/**
 * Os valores esperados foram obtidos a correr o `utils.ts` original da web app
 * com os mesmos dados — a versão Android tem de dar exatamente os mesmos resultados.
 */
class WorkCalculationsTest {

    private fun t(s: String): LocalTime? = TimeFormat.parse(s)

    @Test
    fun minutesBetween_matchesWebApp() {
        assertEquals(540, WorkCalculations.minutesBetween(t("09:00"), t("18:00")))
        assertEquals(480, WorkCalculations.minutesBetween(t("22:00"), t("06:00")))
        assertEquals(0, WorkCalculations.minutesBetween(t("08:00"), t("08:00")))
        assertEquals(510, WorkCalculations.minutesBetween(t("9:05"), t("17:35")))
        assertEquals(480, WorkCalculations.minutesBetween(t("16:00"), t("24:00")))
        assertEquals(45, WorkCalculations.minutesBetween(t("23:30"), t("00:15")))
        assertEquals(0, WorkCalculations.minutesBetween(t(""), t("18:00")))
        assertEquals(0, WorkCalculations.minutesBetween(t("abc"), t("18:00")))
    }

    @Test
    fun lateness_matchesWebApp() {
        assertEquals(15, WorkCalculations.lateness(t("09:00"), t("09:15")))
        assertEquals(0, WorkCalculations.lateness(t("09:00"), t("08:50")))
        assertEquals(0, WorkCalculations.lateness(t("00:00"), t("23:50")))
        assertEquals(40, WorkCalculations.lateness(t("23:30"), t("00:10")))
        assertEquals(0, WorkCalculations.lateness(t("22:00"), t("22:00")))
        assertEquals(0, WorkCalculations.lateness(t("08:00"), t("20:30")))
        assertEquals(0, WorkCalculations.lateness(t("08:00"), t("21:00")))
        assertEquals(0, WorkCalculations.lateness(t("20:00"), t("08:00")))
        assertEquals(0, WorkCalculations.lateness(null, t("08:00")))
    }

    private val sample = listOf(
        entry("1", "2026-10-01", ShiftType.REGULAR, scheduledStart = "09:00", scheduledEnd = "18:00", actualStart = "09:10", actualEnd = "18:00"),
        entry("2", "2026-10-02", ShiftType.REGULAR, isHoliday = true, scheduledStart = "09:00", actualStart = "09:00", actualEnd = "17:00", overtimeStart = "17:00", overtimeEnd = "19:30"),
        entry("3", "2026-10-03", ShiftType.DAY_OFF, actualStart = "09:00", actualEnd = "18:00"),
        entry("4", "2026-10-04", ShiftType.ABSENT),
        entry("5", "2026-10-05", ShiftType.VACATION),
        entry("6", "2026-10-06", ShiftType.REGULAR, scheduledStart = "22:00", actualStart = "22:20", actualEnd = "06:00"),
        entry("7", "2026-09-30", ShiftType.REGULAR, actualStart = "08:00", actualEnd = "16:00", overtimeStart = "16:00", overtimeEnd = "17:00"),
        entry("8", "2025-10-01", ShiftType.REGULAR, actualStart = "08:00", actualEnd = "16:00"),
    )

    @Test
    fun dailyStats_matchesWebApp() {
        val byId = sample.associateBy { it.id }.mapValues { WorkCalculations.dailyStats(it.value) }

        byId.getValue("1").let {
            assertEquals(530, it.workedMinutes); assertEquals(0, it.holidayMinutes)
            assertEquals(0, it.overtimeMinutes); assertEquals(10, it.lateMinutes)
        }
        byId.getValue("2").let {
            assertEquals(0, it.workedMinutes); assertEquals(480, it.holidayMinutes)
            assertEquals(150, it.overtimeMinutes); assertEquals(0, it.lateMinutes)
        }
        byId.getValue("3").let {
            // Folga com horas preenchidas: as horas são ignoradas, como na web app.
            assertEquals(0, it.workedMinutes); assertEquals(true, it.isDayOff)
        }
        assertEquals(true, byId.getValue("4").isAbsent)
        assertEquals(true, byId.getValue("5").isVacation)
        byId.getValue("6").let {
            assertEquals(460, it.workedMinutes); assertEquals(20, it.lateMinutes)
        }
        byId.getValue("7").let {
            assertEquals(480, it.workedMinutes); assertEquals(60, it.overtimeMinutes)
        }
    }

    @Test
    fun monthStats_matchesWebApp() {
        val stats = WorkCalculations.monthStats(sample, YearMonth.of(2026, 10))
        assertEquals(
            PeriodStats(
                totalWorkedMinutes = 990,
                totalHolidayMinutes = 480,
                totalOvertimeMinutes = 150,
                totalLateMinutes = 30,
                daysAbsent = 1,
                daysOff = 1,
                daysWorked = 3,
                daysHoliday = 1,
                daysVacation = 1,
            ),
            stats
        )
    }

    @Test
    fun yearStats_matchesWebApp() {
        val stats = WorkCalculations.yearStats(sample, 2026)
        assertEquals(
            PeriodStats(
                totalWorkedMinutes = 1470,
                totalHolidayMinutes = 480,
                totalOvertimeMinutes = 210,
                totalLateMinutes = 30,
                daysAbsent = 1,
                daysOff = 1,
                daysWorked = 4,
                daysHoliday = 1,
                daysVacation = 1,
            ),
            stats
        )
    }

    @Test
    fun formatMinutes_matchesWebApp() {
        assertEquals("0h 0m", WorkCalculations.formatMinutes(0))
        assertEquals("0h 59m", WorkCalculations.formatMinutes(59))
        assertEquals("1h 0m", WorkCalculations.formatMinutes(60))
        assertEquals("8h 30m", WorkCalculations.formatMinutes(510))
        assertEquals("24h 1m", WorkCalculations.formatMinutes(1441))
        assertEquals("0h 0m", WorkCalculations.formatMinutes(-5))
    }

    @Test
    fun charts_coverWholeMonthAndYear() {
        val daily = WorkCalculations.dailyChart(sample, YearMonth.of(2026, 10))
        assertEquals(31, daily.size)
        assertEquals(530 / 60f, daily[0].workedHours, 0.001f)
        assertEquals(2.5f, daily[1].overtimeHours, 0.001f)
        assertEquals(0f, daily[30].totalHours, 0.001f)

        val labels = listOf("Jan", "Fev", "Mar", "Abr", "Mai", "Jun", "Jul", "Ago", "Set", "Out", "Nov", "Dez")
        val monthly = WorkCalculations.monthlyChart(sample, 2026, labels)
        assertEquals(12, monthly.size)
        assertEquals(1f, monthly[8].overtimeHours, 0.001f) // setembro
        assertEquals(990 / 60f, monthly[9].workedHours, 0.001f) // outubro
    }

    @Test
    fun timeFormat_parsesLikeWebInputs() {
        assertEquals(LocalTime.of(9, 5), TimeFormat.parse("9:05"))
        assertEquals(LocalTime.MIDNIGHT, TimeFormat.parse("24:00"))
        assertEquals(LocalTime.of(7, 30), TimeFormat.parse("07:30:00"))
        assertNull(TimeFormat.parse("25:00"))
        assertNull(TimeFormat.parse("abc"))
        assertNull(TimeFormat.parse(null))
        assertEquals("07:05", TimeFormat.format(LocalTime.of(7, 5)))
    }

    private fun entry(
        id: String,
        date: String,
        type: ShiftType,
        isHoliday: Boolean = false,
        scheduledStart: String? = null,
        scheduledEnd: String? = null,
        actualStart: String? = null,
        actualEnd: String? = null,
        overtimeStart: String? = null,
        overtimeEnd: String? = null,
    ) = WorkEntry(
        id = id,
        date = LocalDate.parse(date),
        type = type,
        isHoliday = isHoliday,
        scheduledStart = TimeFormat.parse(scheduledStart),
        scheduledEnd = TimeFormat.parse(scheduledEnd),
        actualStart = TimeFormat.parse(actualStart),
        actualEnd = TimeFormat.parse(actualEnd),
        overtimeStart = TimeFormat.parse(overtimeStart),
        overtimeEnd = TimeFormat.parse(overtimeEnd),
    )
}
