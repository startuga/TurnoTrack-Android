@file:OptIn(ExperimentalMaterial3Api::class)

package com.startuga.turnotrack.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Keyboard
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimeInput
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.startuga.turnotrack.domain.TimeFormat
import java.time.LocalTime

/** Campo de hora: mostra "HH:mm" e abre o relógio do Android ao tocar. */
@Composable
fun TimeField(
    label: String,
    value: LocalTime?,
    onValueChange: (LocalTime?) -> Unit,
    modifier: Modifier = Modifier,
    accent: Color = MaterialTheme.colorScheme.primary,
) {
    var showPicker by remember { mutableStateOf(false) }

    Surface(
        onClick = { showPicker = true },
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        border = BorderStroke(1.dp, if (value != null) accent.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                TimeFormat.formatOrDash(value),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (value != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline,
            )
        }
    }

    if (showPicker) {
        TimePickerDialog(
            title = label,
            initial = value,
            onDismiss = { showPicker = false },
            onConfirm = {
                onValueChange(it)
                showPicker = false
            },
            onClear = {
                onValueChange(null)
                showPicker = false
            },
        )
    }
}

@Composable
private fun TimePickerDialog(
    title: String,
    initial: LocalTime?,
    onDismiss: () -> Unit,
    onConfirm: (LocalTime) -> Unit,
    onClear: () -> Unit,
) {
    val start = initial ?: LocalTime.now().withSecond(0).withNano(0)
    val state = rememberTimePickerState(initialHour = start.hour, initialMinute = start.minute, is24Hour = true)
    var keyboard by rememberSaveable { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            modifier = Modifier.padding(horizontal = 16.dp).widthIn(max = 400.dp),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
        ) {
            Column(
                Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(title, style = MaterialTheme.typography.labelLarge, modifier = Modifier.fillMaxWidth())
                if (keyboard) TimeInput(state = state) else TimePicker(state = state)
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { keyboard = !keyboard }) {
                        Icon(
                            if (keyboard) Icons.Outlined.Schedule else Icons.Outlined.Keyboard,
                            contentDescription = if (keyboard) "Mostrar relógio" else "Escrever hora",
                        )
                    }
                    TextButton(onClick = onClear) { Text("Limpar") }
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = onDismiss) { Text("Cancelar") }
                    Spacer(Modifier.width(4.dp))
                    TextButton(onClick = { onConfirm(LocalTime.of(state.hour, state.minute)) }) { Text("OK") }
                }
            }
        }
    }
}
