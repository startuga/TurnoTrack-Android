package com.startuga.turnotrack.ui.stats

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.startuga.turnotrack.domain.ChartBar
import com.startuga.turnotrack.ui.theme.LocalShiftColors
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.min
import kotlin.math.pow

/**
 * Gráfico de barras empilhadas (normais / feriado / extra), desenhado à mão em Canvas
 * para não depender de bibliotecas externas. Tocar numa barra seleciona-a.
 */
@Composable
fun StackedBarChart(
    bars: List<ChartBar>,
    selectedIndex: Int?,
    onSelect: (Int?) -> Unit,
    modifier: Modifier = Modifier,
    labelEvery: Int = 1,
) {
    val shift = LocalShiftColors.current
    val textMeasurer = rememberTextMeasurer()
    val axisColor = MaterialTheme.colorScheme.onSurfaceVariant
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val labelStyle = TextStyle(fontSize = 10.sp, color = axisColor)
    val maxValue = niceMax(bars.maxOfOrNull { it.totalHours } ?: 0f)
    val currentSelected by rememberUpdatedState(selectedIndex)
    val currentOnSelect by rememberUpdatedState(onSelect)

    Canvas(
        modifier
            .fillMaxWidth()
            .height(220.dp)
            .pointerInput(bars.size) {
                detectTapGestures { offset ->
                    val left = 34.dp.toPx()
                    val slot = (size.width - left) / bars.size
                    if (offset.x < left || bars.isEmpty()) {
                        currentOnSelect(null)
                    } else {
                        val index = ((offset.x - left) / slot).toInt().coerceIn(0, bars.lastIndex)
                        currentOnSelect(if (index == currentSelected) null else index)
                    }
                }
            }
    ) {
        val left = 34.dp.toPx()
        val top = 8.dp.toPx()
        val bottom = 20.dp.toPx()
        val chartWidth = size.width - left
        val chartHeight = size.height - top - bottom
        if (bars.isEmpty() || chartWidth <= 0f || chartHeight <= 0f) return@Canvas

        // Linhas de grelha e eixo Y
        for (i in 0..4) {
            val value = maxValue * i / 4f
            val y = top + chartHeight * (1f - i / 4f)
            drawLine(gridColor, Offset(left, y), Offset(size.width, y), strokeWidth = 1.dp.toPx())
            val layout = textMeasurer.measure(formatAxis(value), labelStyle)
            drawText(layout, topLeft = Offset(left - layout.size.width - 6.dp.toPx(), y - layout.size.height / 2f))
        }

        val slot = chartWidth / bars.size
        val barWidth = min(slot * 0.65f, 18.dp.toPx())
        val radius = CornerRadius(min(4.dp.toPx(), barWidth / 2f))

        bars.forEachIndexed { index, bar ->
            val centerX = left + slot * index + slot / 2f
            val alpha = if (selectedIndex == null || selectedIndex == index) 1f else 0.35f
            val segments = listOf(
                bar.workedHours to shift.barWorked,
                bar.holidayHours to shift.barHoliday,
                bar.overtimeHours to shift.barOvertime,
            ).filter { it.first > 0f }

            var segmentBottom = top + chartHeight
            segments.forEachIndexed { segIndex, (value, color) ->
                val height = chartHeight * (value / maxValue)
                val segmentTop = segmentBottom - height
                val rect = Rect(centerX - barWidth / 2f, segmentTop, centerX + barWidth / 2f, segmentBottom)
                if (segIndex == segments.lastIndex) {
                    val path = Path().apply {
                        addRoundRect(RoundRect(rect, topLeft = radius, topRight = radius))
                    }
                    drawPath(path, color.copy(alpha = alpha))
                } else {
                    drawRect(color.copy(alpha = alpha), topLeft = rect.topLeft, size = Size(rect.width, rect.height))
                }
                segmentBottom = segmentTop
            }

            val showLabel = index % labelEvery == 0 || index == selectedIndex
            if (showLabel) {
                val layout = textMeasurer.measure(bar.label, labelStyle)
                drawText(
                    layout,
                    topLeft = Offset(centerX - layout.size.width / 2f, top + chartHeight + 4.dp.toPx()),
                )
            }
        }
    }
}

/** Arredonda o máximo para um valor "bonito" (4, 10, 50, 200…) divisível em 4 linhas. */
private fun niceMax(max: Float): Float {
    if (max <= 0f) return 4f
    val rawStep = max / 4f
    val magnitude = 10.0.pow(floor(log10(rawStep.toDouble()))).toFloat()
    val normalized = rawStep / magnitude
    val niceStep = when {
        normalized <= 1f -> 1f
        normalized <= 2f -> 2f
        normalized <= 2.5f -> 2.5f
        normalized <= 5f -> 5f
        else -> 10f
    } * magnitude
    return ceil(max / niceStep) * niceStep
}

private fun formatAxis(value: Float): String =
    if (value == value.toInt().toFloat()) "${value.toInt()}h" else "%.1fh".format(value)
