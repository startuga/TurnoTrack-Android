@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.startuga.turnotrack.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.startuga.turnotrack.AppContainer
import com.startuga.turnotrack.ui.assistant.AssistantScreen
import com.startuga.turnotrack.ui.assistant.AssistantViewModel
import com.startuga.turnotrack.ui.calendar.CalendarScreen
import com.startuga.turnotrack.ui.entry.EntryEditorSheet
import com.startuga.turnotrack.ui.settings.SettingsScreen
import com.startuga.turnotrack.ui.stats.StatsScreen
import kotlinx.coroutines.launch
import java.time.LocalDate

private enum class AppTab(val label: String, val icon: ImageVector) {
    CALENDAR("Calendário", Icons.Outlined.CalendarMonth),
    STATS("Estatísticas", Icons.Outlined.BarChart),
    ASSISTANT("Assistente", Icons.Outlined.AutoAwesome),
    SETTINGS("Definições", Icons.Outlined.Settings),
}

@Composable
fun TurnoTrackRoot(container: AppContainer) {
    val mainViewModel: MainViewModel = viewModel(
        factory = viewModelFactory { initializer { MainViewModel(container.repository, container.settings) } }
    )
    val assistantViewModel: AssistantViewModel = viewModel(
        factory = viewModelFactory { initializer { AssistantViewModel(container.assistant, container.settings) } }
    )

    val entries by mainViewModel.entries.collectAsStateWithLifecycle()
    val entriesByDate by mainViewModel.entriesByDate.collectAsStateWithLifecycle()
    val month by mainViewModel.month.collectAsStateWithLifecycle()
    val apiKey by mainViewModel.geminiApiKey.collectAsStateWithLifecycle()

    var tab by rememberSaveable { mutableStateOf(AppTab.CALENDAR) }
    var editingDate by rememberSaveable { mutableStateOf<LocalDate?>(null) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(9.dp))
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.Outlined.Schedule,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        Text(
                            if (tab == AppTab.CALENDAR) "Registo de Trabalho" else tab.label,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                },
                actions = {
                    when (tab) {
                        AppTab.CALENDAR, AppTab.STATS -> IconButton(onClick = { mainViewModel.goToToday() }) {
                            Icon(Icons.Outlined.Today, contentDescription = "Ir para hoje")
                        }
                        AppTab.ASSISTANT -> IconButton(onClick = { assistantViewModel.clear() }) {
                            Icon(Icons.Outlined.DeleteSweep, contentDescription = "Limpar conversa")
                        }
                        AppTab.SETTINGS -> Unit
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
            )
        },
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainerLow) {
                AppTab.entries.forEach { item ->
                    NavigationBarItem(
                        selected = tab == item,
                        onClick = { tab = item },
                        icon = { Icon(item.icon, contentDescription = null) },
                        label = { Text(item.label, maxLines = 1) },
                    )
                }
            }
        },
        floatingActionButton = {
            if (tab == AppTab.CALENDAR) {
                ExtendedFloatingActionButton(
                    onClick = { editingDate = LocalDate.now() },
                    icon = { Icon(Icons.Outlined.Add, contentDescription = null) },
                    text = { Text("Registar hoje") },
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbar) },
        containerColor = MaterialTheme.colorScheme.surface,
    ) { padding ->
        val contentModifier = Modifier
            .padding(padding)
            .consumeWindowInsets(padding)

        when (tab) {
            AppTab.CALENDAR -> CalendarScreen(
                month = month,
                entries = entries,
                entriesByDate = entriesByDate,
                onMonthChange = mainViewModel::setMonth,
                onShiftMonths = mainViewModel::shiftMonths,
                onToday = mainViewModel::goToToday,
                onDayClick = { editingDate = it },
                modifier = contentModifier,
            )
            AppTab.STATS -> StatsScreen(
                month = month,
                entries = entries,
                onShiftMonths = mainViewModel::shiftMonths,
                onToday = mainViewModel::goToToday,
                modifier = contentModifier,
            )
            AppTab.ASSISTANT -> AssistantScreen(
                viewModel = assistantViewModel,
                hasApiKey = apiKey.isNotBlank(),
                onOpenSettings = { tab = AppTab.SETTINGS },
                modifier = contentModifier,
            )
            AppTab.SETTINGS -> SettingsScreen(
                viewModel = mainViewModel,
                snackbar = snackbar,
                modifier = contentModifier,
            )
        }
    }

    editingDate?.let { date ->
        EntryEditorSheet(
            date = date,
            existing = entriesByDate[date],
            onDismiss = { editingDate = null },
            onSave = { entry ->
                mainViewModel.save(entry)
                editingDate = null
            },
            onDelete = {
                editingDate = null
                scope.launch {
                    val deleted = mainViewModel.delete(date) ?: return@launch
                    val result = snackbar.showSnackbar(
                        message = "Registo apagado",
                        actionLabel = "Anular",
                        duration = SnackbarDuration.Short,
                    )
                    if (result == SnackbarResult.ActionPerformed) mainViewModel.save(deleted)
                }
            },
        )
    }
}
