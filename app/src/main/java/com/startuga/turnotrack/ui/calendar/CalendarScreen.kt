@file:OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)

package com.startuga.turnotrack.ui.calendar

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CardGiftcard
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.startuga.turnotrack.domain.ShiftType
import com.startuga.turnotrack.domain.TimeFormat
import com.startuga.turnotrack.domain.WorkCalculations
import com.startuga.turnotrack.domain.WorkEntry
import com.startuga.turnotrack.ui.Fmt
import com.startuga.turnotrack.ui.components.MonthSwitcher
import com.startuga.turnotrack.ui.components.SummaryTile
import com.startuga.turnotrack.ui.forEntry
import com.startuga.turnotrack.ui.theme.LocalShiftColors
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit

private const val PAGE_COUNT = 2400 // ±100 anos
private const val START_PAGE = PAGE_COUNT / 2
private val CELL_HEIGHT = 80.dp

@Composable
fun CalendarScreen(
    month: YearMonth,
    entries: List<WorkEntry>,
    entriesByDate: Map<LocalDate, WorkEntry>,
    onMonthChange: (YearMonth) -> Unit,
    onShiftMonths: (Long) -> Unit,
    onToday: () -> Unit,
    onDayClick: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    val baseMonth = remember { YearMonth.now() }
    fun pageFor(m: YearMonth) = START_PAGE + ChronoUnit.MONTHS.between(baseMonth, m).toInt()
    fun monthFor(page: Int): YearMonth = baseMonth.plusMonths((page - START_PAGE).toLong())

    val pagerState = rememberPagerState(initialPage = pageFor(month)) { PAGE_COUNT }
    val currentOnMonthChange by rememberUpdatedState(onMonthChange)

    // Deslizar o calendário → atualiza o mês partilhado.
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }.collect { page -> currentOnMonthChange(monthFor(page)) }
    }
    // Mês mudado por botões / estatísticas → anima o calendário até lá.
    LaunchedEffect(month) {
        val target = pageFor(month)
        if (pagerState.settledPage != target) pagerState.animateScrollToPage(target)
    }

    val stats = remember(entries, month) { WorkCalculations.monthStats(entries, month) }
    val shift = LocalShiftColors.current

    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        MonthSwitcher(
            title = Fmt.month(month),
            onPrevious = { onShiftMonths(-1) },
            onNext = { onShiftMonths(1) },
            onTitleClick = onToday,
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SummaryTile("Horas trab.", WorkCalculations.formatMinutes(stats.totalWorkedMinutes), shift.regular, Modifier.weight(1f))
            SummaryTile("Atraso total", "${stats.totalLateMinutes} min", shift.late, Modifier.weight(1f))
            SummaryTile("Horas extras", WorkCalculations.formatMinutes(stats.totalOvertimeMinutes), shift.overtime, Modifier.weight(1f))
        }

        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .padding(vertical = 8.dp)
            ) {
                Fmt.WEEKDAYS.forEach { day ->
                    Text(
                        day.uppercase(),
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            HorizontalPager(state = pagerState, modifier = Modifier.fillMaxWidth()) { page ->
                MonthGrid(
                    month = monthFor(page),
                    entriesByDate = entriesByDate,
                    onDayClick = onDayClick,
                )
            }
        }

        Legend()
        Spacer(Modifier.height(72.dp)) // espaço para o botão "Registar hoje"
    }
}

@Composable
private fun MonthGrid(
    month: YearMonth,
    entriesByDate: Map<LocalDate, WorkEntry>,
    onDayClick: (LocalDate) -> Unit,
) {
    val today = LocalDate.now()
    val firstDay = month.atDay(1)
    val offset = firstDay.dayOfWeek.value % 7 // domingo = 0
    val days = month.lengthOfMonth()

    Column(Modifier.padding(4.dp)) {
        for (week in 0 until 6) {
            Row(Modifier.fillMaxWidth()) {
                for (weekday in 0 until 7) {
                    val dayNumber = week * 7 + weekday - offset + 1
                    Box(
                        Modifier
                            .weight(1f)
                            .height(CELL_HEIGHT)
                            .padding(2.dp)
                    ) {
                        if (dayNumber in 1..days) {
                            val date = month.atDay(dayNumber)
                            DayCell(
                                day = dayNumber,
                                entry = entriesByDate[date],
                                isToday = date == today,
                                onClick = { onDayClick(date) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCell(day: Int, entry: WorkEntry?, isToday: Boolean, onClick: () -> Unit) {
    val shift = LocalShiftColors.current
    val colors = entry?.let { shift.forEntry(it) }
    val container = colors?.first ?: Color.Transparent
    val content = colors?.second ?: MaterialTheme.colorScheme.onSurface
    val stats = remember(entry) { entry?.let { WorkCalculations.dailyStats(it) } }

    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxSize(),
        shape = RoundedCornerShape(10.dp),
        color = container,
        contentColor = content,
        border = if (isToday) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
    ) {
        Column(Modifier.padding(horizontal = 4.dp, vertical = 3.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    day.toString(),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (isToday || entry != null) FontWeight.Bold else FontWeight.Normal,
                    color = if (isToday) MaterialTheme.colorScheme.primary else content,
                )
                Spacer(Modifier.weight(1f))
                if (entry != null && entry.isHoliday && entry.type == ShiftType.REGULAR) {
                    Icon(Icons.Outlined.CardGiftcard, contentDescription = "Feriado", modifier = Modifier.size(11.dp))
                }
                if (!entry?.notes.isNullOrBlank()) {
                    Box(
                        Modifier
                            .padding(start = 2.dp)
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(shift.vacation.second)
                    )
                }
            }

            if (entry != null) {
                Spacer(Modifier.height(2.dp))
                if (entry.type == ShiftType.REGULAR) {
                    if (entry.actualStart != null || entry.actualEnd != null) {
                        TinyText(TimeFormat.formatOrDash(entry.actualStart))
                        TinyText(TimeFormat.formatOrDash(entry.actualEnd))
                    } else {
                        TinyText("Turno", bold = true)
                    }
                } else {
                    TinyText(entry.type.label, bold = true)
                }
                Spacer(Modifier.weight(1f))
                if (stats != null && stats.lateMinutes > 0) {
                    TinyText("+${compact(stats.lateMinutes, minutesOnly = true)}", color = shift.late.second, bold = true)
                }
                if (stats != null && stats.overtimeMinutes > 0) {
                    TinyText("+${compact(stats.overtimeMinutes)}", color = shift.overtime.second, bold = true)
                }
            }
        }
    }
}

@Composable
private fun TinyText(text: String, bold: Boolean = false, color: Color = Color.Unspecified) {
    Text(
        text,
        fontSize = 10.sp,
        lineHeight = 11.sp,
        fontWeight = if (bold) FontWeight.Bold else FontWeight.Medium,
        color = color,
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Clip,
    )
}

/** Formato curto para caber na célula: "15m", "2h", "1h30". */
private fun compact(minutes: Int, minutesOnly: Boolean = false): String {
    if (minutesOnly && minutes < 60) return "${minutes}m"
    val h = minutes / 60
    val m = minutes % 60
    return when {
        h == 0 -> "${m}m"
        m == 0 -> "${h}h"
        else -> "${h}h" + m.toString().padStart(2, '0')
    }
}

@Composable
private fun Legend() {
    val shift = LocalShiftColors.current
    val items = listOf(
        "Normal" to shift.regular,
        "Feriado" to shift.holiday,
        "Folga" to shift.dayOff,
        "Falta" to shift.absent,
        "Férias" to shift.vacation,
    )
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.padding(horizontal = 4.dp),
    ) {
        items.forEach { (label, colors) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(colors.second)
                )
                Spacer(Modifier.width(4.dp))
                Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
