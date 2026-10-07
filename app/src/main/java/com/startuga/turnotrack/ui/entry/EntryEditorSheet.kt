@file:OptIn(ExperimentalMaterial3Api::class)

package com.startuga.turnotrack.ui.entry

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CardGiftcard
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.MoreTime
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.WorkOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.startuga.turnotrack.domain.ShiftType
import com.startuga.turnotrack.domain.WorkCalculations
import com.startuga.turnotrack.domain.WorkEntry
import com.startuga.turnotrack.ui.Fmt
import com.startuga.turnotrack.ui.components.TimeField
import com.startuga.turnotrack.ui.forType
import com.startuga.turnotrack.ui.theme.LocalShiftColors
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime

private val DEFAULT_START: LocalTime = LocalTime.of(9, 0)
private val DEFAULT_END: LocalTime = LocalTime.of(18, 0)

/** Equivalente ao "Registo Diário" (EntryModal) da web app, como folha inferior nativa. */
@Composable
fun EntryEditorSheet(
    date: LocalDate,
    existing: WorkEntry?,
    onDismiss: () -> Unit,
    onSave: (WorkEntry) -> Unit,
    onDelete: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val shift = LocalShiftColors.current
    val key = date.toString()

    var type by rememberSaveable(key) { mutableStateOf(existing?.type ?: ShiftType.REGULAR) }
    var isHoliday by rememberSaveable(key) { mutableStateOf(existing?.isHoliday ?: false) }
    var scheduledStart by rememberSaveable(key) { mutableStateOf<LocalTime?>(existing?.scheduledStart ?: DEFAULT_START) }
    var scheduledEnd by rememberSaveable(key) { mutableStateOf<LocalTime?>(existing?.scheduledEnd ?: DEFAULT_END) }
    var actualStart by rememberSaveable(key) { mutableStateOf<LocalTime?>(existing?.actualStart) }
    var actualEnd by rememberSaveable(key) { mutableStateOf<LocalTime?>(existing?.actualEnd) }
    var showOvertime by rememberSaveable(key) {
        mutableStateOf(existing?.overtimeStart != null || existing?.overtimeEnd != null)
    }
    var overtimeStart by rememberSaveable(key) { mutableStateOf<LocalTime?>(existing?.overtimeStart) }
    var overtimeEnd by rememberSaveable(key) { mutableStateOf<LocalTime?>(existing?.overtimeEnd) }
    var notes by rememberSaveable(key) { mutableStateOf(existing?.notes.orEmpty()) }

    // Mesmas regras de gravação da web app: horas só em turnos normais, extra só se ativado.
    fun buildEntry(): WorkEntry {
        val regular = type == ShiftType.REGULAR
        return WorkEntry(
            id = existing?.id ?: WorkEntry.newId(),
            date = date,
            type = type,
            isHoliday = regular && isHoliday,
            scheduledStart = if (regular) scheduledStart else null,
            scheduledEnd = if (regular) scheduledEnd else null,
            actualStart = if (regular) actualStart else null,
            actualEnd = if (regular) actualEnd else null,
            overtimeStart = if (regular && showOvertime) overtimeStart else null,
            overtimeEnd = if (regular && showOvertime) overtimeEnd else null,
            notes = notes.trim().ifEmpty { null },
        )
    }

    fun closeThen(action: () -> Unit) {
        scope.launch { sheetState.hide() }.invokeOnCompletion { action() }
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            Modifier
                .fillMaxWidth()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column {
                Text("Registo diário", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(
                    Fmt.longDate(date),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // Tipo de registo
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                val types = ShiftType.entries
                types.forEachIndexed { index, t ->
                    val c = shift.forType(t)
                    SegmentedButton(
                        selected = type == t,
                        onClick = { type = t },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = types.size),
                        colors = SegmentedButtonDefaults.colors(
                            activeContainerColor = c.first,
                            activeContentColor = c.second,
                        ),
                        icon = {},
                    ) {
                        Text(t.label, maxLines = 1)
                    }
                }
            }

            if (type == ShiftType.REGULAR) {
                ToggleRow(
                    icon = Icons.Outlined.CardGiftcard,
                    label = "É dia de feriado?",
                    checked = isHoliday,
                    onCheckedChange = { isHoliday = it },
                    activeColors = shift.holiday,
                )

                FormSection(title = "Horário", icon = Icons.Outlined.WorkOutline) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        TimeField("Entrada", scheduledStart, { scheduledStart = it }, Modifier.weight(1f))
                        TimeField("Saída", scheduledEnd, { scheduledEnd = it }, Modifier.weight(1f))
                    }
                }

                val realColors = if (isHoliday) shift.holiday else shift.regular
                FormSection(
                    title = if (isHoliday) "Horário feriado real" else "Horário real",
                    icon = Icons.Outlined.Schedule,
                    container = realColors.first.copy(alpha = 0.55f),
                    titleColor = realColors.second,
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        TimeField("Entrada real", actualStart, { actualStart = it }, Modifier.weight(1f), accent = realColors.second)
                        TimeField("Saída real", actualEnd, { actualEnd = it }, Modifier.weight(1f), accent = realColors.second)
                    }
                    TextButton(
                        onClick = {
                            actualStart = scheduledStart
                            actualEnd = scheduledEnd
                        },
                        contentPadding = ButtonDefaults.TextButtonContentPadding,
                    ) { Text("Igual ao horário") }
                }

                ToggleRow(
                    icon = Icons.Outlined.MoreTime,
                    label = "Horas extras",
                    checked = showOvertime,
                    onCheckedChange = { showOvertime = it },
                    activeColors = shift.overtime,
                )
                if (showOvertime) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        TimeField("Início extra", overtimeStart, { overtimeStart = it }, Modifier.weight(1f), accent = shift.overtime.second)
                        TimeField("Fim extra", overtimeEnd, { overtimeEnd = it }, Modifier.weight(1f), accent = shift.overtime.second)
                    }
                }

                val preview = WorkCalculations.dailyStats(buildEntry())
                val parts = buildList {
                    val main = preview.workedMinutes + preview.holidayMinutes
                    if (main > 0) add("Turno: ${WorkCalculations.formatMinutes(main)}")
                    if (preview.lateMinutes > 0) add("Atraso: ${preview.lateMinutes} min")
                    if (preview.overtimeMinutes > 0) add("Extra: ${WorkCalculations.formatMinutes(preview.overtimeMinutes)}")
                }
                if (parts.isNotEmpty()) {
                    Text(
                        parts.joinToString("  ·  "),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notas (opcional)") },
                minLines = 2,
                modifier = Modifier.fillMaxWidth(),
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (existing != null) {
                    OutlinedButton(
                        onClick = { closeThen(onDelete) },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    ) {
                        Icon(Icons.Outlined.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Apagar")
                    }
                }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = { closeThen(onDismiss) }) { Text("Cancelar") }
                Spacer(Modifier.width(8.dp))
                Button(onClick = {
                    val entry = buildEntry()
                    closeThen { onSave(entry) }
                }) { Text("Guardar") }
            }
        }
    }
}

@Composable
private fun ToggleRow(
    icon: ImageVector,
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    activeColors: Pair<Color, Color>,
) {
    Surface(
        onClick = { onCheckedChange(!checked) },
        shape = RoundedCornerShape(14.dp),
        color = if (checked) activeColors.first else MaterialTheme.colorScheme.surfaceContainer,
        contentColor = if (checked) activeColors.second else MaterialTheme.colorScheme.onSurfaceVariant,
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
            Text(label, modifier = Modifier.weight(1f), fontWeight = FontWeight.Medium)
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}

@Composable
private fun FormSection(
    title: String,
    icon: ImageVector,
    container: Color = MaterialTheme.colorScheme.surfaceContainer,
    titleColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(shape = RoundedCornerShape(16.dp), color = container) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = titleColor, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(title, style = MaterialTheme.typography.labelLarge, color = titleColor, fontWeight = FontWeight.SemiBold)
            }
            content()
        }
    }
}
