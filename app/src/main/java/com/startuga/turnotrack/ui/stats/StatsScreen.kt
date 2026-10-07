@file:OptIn(ExperimentalMaterial3Api::class)

package com.startuga.turnotrack.ui.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.BeachAccess
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.CardGiftcard
import androidx.compose.material.icons.outlined.Coffee
import androidx.compose.material.icons.outlined.MoreTime
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material.icons.outlined.WorkOutline
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.startuga.turnotrack.domain.WorkCalculations
import com.startuga.turnotrack.domain.WorkEntry
import com.startuga.turnotrack.ui.Fmt
import com.startuga.turnotrack.ui.components.MonthSwitcher
import com.startuga.turnotrack.ui.theme.LocalShiftColors
import java.time.YearMonth

private enum class StatsPeriod(val label: String) { MONTH("Mensal"), YEAR("Anual") }

@Composable
fun StatsScreen(
    month: YearMonth,
    entries: List<WorkEntry>,
    onShiftMonths: (Long) -> Unit,
    onToday: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var period by rememberSaveable { mutableStateOf(StatsPeriod.MONTH) }
    var selectedBar by remember { mutableStateOf<Int?>(null) }
    LaunchedEffect(period, month) { selectedBar = null }

    val isMonth = period == StatsPeriod.MONTH
    val stats = remember(entries, month, period) {
        if (isMonth) WorkCalculations.monthStats(entries, month) else WorkCalculations.yearStats(entries, month.year)
    }
    val bars = remember(entries, month, period) {
        if (isMonth) WorkCalculations.dailyChart(entries, month)
        else WorkCalculations.monthlyChart(entries, month.year, Fmt.MONTHS_SHORT)
    }
    val shift = LocalShiftColors.current

    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        MonthSwitcher(
            title = if (isMonth) Fmt.month(month) else month.year.toString(),
            onPrevious = { onShiftMonths(if (isMonth) -1 else -12) },
            onNext = { onShiftMonths(if (isMonth) 1 else 12) },
            onTitleClick = onToday,
            previousLabel = if (isMonth) "Mês anterior" else "Ano anterior",
            nextLabel = if (isMonth) "Próximo mês" else "Próximo ano",
        )

        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            StatsPeriod.entries.forEachIndexed { index, p ->
                SegmentedButton(
                    selected = period == p,
                    onClick = { period = p },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = StatsPeriod.entries.size),
                ) {
                    Text(p.label)
                }
            }
        }

        // Destaque: horas trabalhadas
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
        ) {
            Column(Modifier.fillMaxWidth().padding(20.dp)) {
                Text("Horas trabalhadas", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f))
                Text(
                    WorkCalculations.formatMinutes(stats.totalWorkedMinutes),
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
                Text(
                    "+ ${WorkCalculations.formatMinutes(stats.totalHolidayMinutes)} em feriados  ·  + ${WorkCalculations.formatMinutes(stats.totalOvertimeMinutes)} extra",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f),
                )
            }
        }

        val totalShiftMinutes = stats.totalWorkedMinutes + stats.totalHolidayMinutes
        val average = if (stats.daysWorked > 0) totalShiftMinutes / stats.daysWorked else 0
        val tiles = listOf(
            Tile("Dias trabalhados", stats.daysWorked.toString(), null, Icons.Outlined.WorkOutline, shift.regular),
            Tile("Média por turno", WorkCalculations.formatMinutes(average), null, Icons.Outlined.Speed, shift.regular),
            Tile("Horas extras", WorkCalculations.formatMinutes(stats.totalOvertimeMinutes), if (isMonth) "Este mês" else "Este ano", Icons.Outlined.MoreTime, shift.overtime),
            Tile("Feriados trabalhados", "${stats.daysHoliday} dias", WorkCalculations.formatMinutes(stats.totalHolidayMinutes), Icons.Outlined.CardGiftcard, shift.holiday),
            Tile("Total atrasos", "${stats.totalLateMinutes} min", if (isMonth) "Este mês" else "Este ano", Icons.Outlined.WarningAmber, shift.late),
            Tile("Dias de folga", stats.daysOff.toString(), null, Icons.Outlined.Coffee, shift.dayOff),
            Tile("Dias de falta", stats.daysAbsent.toString(), null, Icons.Outlined.Cancel, shift.absent),
            Tile("Dias de férias", stats.daysVacation.toString(), null, Icons.Outlined.BeachAccess, shift.vacation),
        )
        tiles.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { tile -> StatTile(tile, Modifier.weight(1f)) }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }

        // Gráfico
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        ) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.BarChart, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Distribuição de horas (${if (isMonth) "diário" else "mensal"})",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                }

                val hasData = bars.any { it.totalHours > 0f }
                if (hasData) {
                    StackedBarChart(
                        bars = bars,
                        selectedIndex = selectedBar,
                        onSelect = { selectedBar = it },
                        labelEvery = if (isMonth) 5 else 1,
                    )
                    val selected = selectedBar?.let { bars.getOrNull(it) }
                    Text(
                        if (selected != null) {
                            val name = if (isMonth) "Dia ${selected.label}" else Fmt.MONTHS[selectedBar!!]
                            "$name: ${Fmt.hoursLabel(selected.workedHours)} normais · " +
                                "${Fmt.hoursLabel(selected.holidayHours)} feriado · ${Fmt.hoursLabel(selected.overtimeHours)} extra"
                        } else {
                            "Toca numa barra para ver os detalhes."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        LegendDot("Normais", shift.barWorked)
                        LegendDot("Feriado", shift.barHoliday)
                        LegendDot("Extras", shift.barOvertime)
                    }
                } else {
                    Box(Modifier.fillMaxWidth().height(160.dp), contentAlignment = Alignment.Center) {
                        Text(
                            "Ainda não existem horas registadas para " +
                                (if (isMonth) "${Fmt.MONTHS[month.monthValue - 1]} de ${month.year}" else "${month.year}") +
                                ".\nAdiciona registos no calendário para ver o gráfico.",
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

private data class Tile(
    val title: String,
    val value: String,
    val subtitle: String?,
    val icon: ImageVector,
    val colors: Pair<Color, Color>,
)

@Composable
private fun StatTile(tile: Tile, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(
                Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(tile.colors.first),
                contentAlignment = Alignment.Center,
            ) {
                Icon(tile.icon, contentDescription = null, tint = tile.colors.second, modifier = Modifier.size(18.dp))
            }
            Text(tile.title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            Text(tile.value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, maxLines = 1)
            if (tile.subtitle != null) {
                Text(tile.subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun LegendDot(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(Modifier.width(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
