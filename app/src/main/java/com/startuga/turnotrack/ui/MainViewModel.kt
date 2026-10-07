package com.startuga.turnotrack.ui

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.startuga.turnotrack.data.SettingsRepository
import com.startuga.turnotrack.data.ThemeMode
import com.startuga.turnotrack.data.WorkEntryRepository
import com.startuga.turnotrack.data.backup.BackupCodec
import com.startuga.turnotrack.domain.WorkEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.YearMonth

class MainViewModel(
    private val repository: WorkEntryRepository,
    private val settings: SettingsRepository,
) : ViewModel() {

    val entries: StateFlow<List<WorkEntry>> = repository.entries
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val entriesByDate: StateFlow<Map<LocalDate, WorkEntry>> = repository.entries
        .map { list -> list.associateBy { it.date } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    /** Mês visível — partilhado entre o calendário e as estatísticas, como na web app. */
    private val _month = MutableStateFlow(YearMonth.now())
    val month: StateFlow<YearMonth> = _month.asStateFlow()

    fun setMonth(month: YearMonth) {
        _month.value = month
    }

    fun shiftMonths(delta: Long) {
        _month.value = _month.value.plusMonths(delta)
    }

    fun goToToday() {
        _month.value = YearMonth.now()
    }

    fun save(entry: WorkEntry) {
        viewModelScope.launch { repository.save(entry) }
    }

    /** Apaga e devolve o registo apagado, para o botão "Anular". */
    suspend fun delete(date: LocalDate): WorkEntry? = repository.delete(date)

    // --- Definições ---

    val themeMode: StateFlow<ThemeMode> = settings.themeMode
        .stateIn(viewModelScope, SharingStarted.Eagerly, ThemeMode.SYSTEM)

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { settings.setThemeMode(mode) }
    }

    val geminiApiKey: StateFlow<String> = settings.geminiApiKey
        .stateIn(viewModelScope, SharingStarted.Eagerly, "")

    val geminiModel: StateFlow<String> = settings.geminiModel
        .stateIn(viewModelScope, SharingStarted.Eagerly, SettingsRepository.DEFAULT_GEMINI_MODEL)

    fun saveGeminiSettings(apiKey: String, model: String) {
        viewModelScope.launch {
            settings.setGeminiApiKey(apiKey)
            settings.setGeminiModel(model)
        }
    }

    // --- Backup ---

    suspend fun exportTo(resolver: ContentResolver, uri: Uri): String = withContext(Dispatchers.IO) {
        try {
            val all = repository.getAll()
            val json = BackupCodec.encode(all)
            resolver.openOutputStream(uri, "wt")?.use { it.write(json.toByteArray(Charsets.UTF_8)) }
                ?: return@withContext "Não foi possível criar o ficheiro."
            "Backup guardado com sucesso! (${all.size} registos)"
        } catch (e: Exception) {
            "Erro ao criar backup."
        }
    }

    suspend fun readBackup(resolver: ContentResolver, uri: Uri): BackupCodec.DecodeResult =
        withContext(Dispatchers.IO) {
            try {
                val text = resolver.openInputStream(uri)?.use { it.readBytes().toString(Charsets.UTF_8) }
                    ?: return@withContext BackupCodec.DecodeResult.Failure("Não foi possível abrir o ficheiro.")
                BackupCodec.decode(text)
            } catch (e: Exception) {
                BackupCodec.DecodeResult.Failure("Erro ao ler o ficheiro. Verifica se é um backup válido.")
            }
        }

    suspend fun importEntries(entries: List<WorkEntry>) {
        repository.replaceAll(entries)
    }
}
