package com.startuga.turnotrack.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class ThemeMode(val label: String) {
    SYSTEM("Automático (sistema)"),
    LIGHT("Claro"),
    DARK("Escuro"),
    OLED("OLED (preto puro)"),
}

private val Context.settingsStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/** Ficheiro separado para a chave, excluído do backup do Android (ver res/xml). */
private val Context.secretsStore: DataStore<Preferences> by preferencesDataStore(name = "secrets")

class SettingsRepository(context: Context) {

    private val settings = context.applicationContext.settingsStore
    private val secrets = context.applicationContext.secretsStore

    private object Keys {
        val THEME = stringPreferencesKey("theme_mode")
        val GEMINI_MODEL = stringPreferencesKey("gemini_model")
        val GEMINI_API_KEY = stringPreferencesKey("gemini_api_key")
    }

    val themeMode: Flow<ThemeMode> = settings.data.map { prefs ->
        prefs[Keys.THEME]?.let { name -> ThemeMode.entries.firstOrNull { it.name == name } }
            ?: ThemeMode.SYSTEM
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        settings.edit { it[Keys.THEME] = mode.name }
    }

    val geminiModel: Flow<String> = settings.data.map { prefs ->
        prefs[Keys.GEMINI_MODEL]?.takeIf { it.isNotBlank() } ?: DEFAULT_GEMINI_MODEL
    }

    suspend fun setGeminiModel(model: String) {
        settings.edit { it[Keys.GEMINI_MODEL] = model.trim() }
    }

    val geminiApiKey: Flow<String> = secrets.data.map { prefs -> prefs[Keys.GEMINI_API_KEY].orEmpty() }

    suspend fun setGeminiApiKey(key: String) {
        secrets.edit { it[Keys.GEMINI_API_KEY] = key.trim() }
    }

    companion object {
        /** O mesmo modelo que a web app usa. Pode ser alterado nas Definições. */
        const val DEFAULT_GEMINI_MODEL = "gemini-3.8-flash"
    }
}
