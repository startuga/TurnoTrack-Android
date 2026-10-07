package com.startuga.turnotrack.ui.assistant

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.startuga.turnotrack.ai.GeminiClient
import com.startuga.turnotrack.ai.WorkAssistant
import com.startuga.turnotrack.data.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ChatMessage(val id: Long, val text: String, val fromUser: Boolean)

class AssistantViewModel(
    private val assistant: WorkAssistant,
    private val settings: SettingsRepository,
) : ViewModel() {

    private var nextId = 1L

    private val greeting = ChatMessage(
        id = 0,
        text = "Olá! Eu sou o teu assistente de registo de trabalho. Pergunta-me sobre as tuas horas extras, " +
            "atrasos, feriados, folgas ou férias. Também podes pedir-me para adicionar ou apagar registos!",
        fromUser = false,
    )

    private val _messages = MutableStateFlow(listOf(greeting))
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    fun send(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty() || _loading.value) return

        // Histórico sem a saudação inicial.
        val history = _messages.value.drop(1).map {
            GeminiClient.Turn(role = if (it.fromUser) "user" else "model", text = it.text)
        }
        _messages.update { it + ChatMessage(nextId++, trimmed, fromUser = true) }
        _loading.value = true

        viewModelScope.launch {
            val reply = try {
                assistant.send(
                    message = trimmed,
                    history = history,
                    apiKey = settings.geminiApiKey.first(),
                    model = settings.geminiModel.first(),
                )
            } catch (e: Exception) {
                "Ocorreu um erro ao contactar o assistente inteligente."
            }
            _messages.update { it + ChatMessage(nextId++, reply, fromUser = false) }
            _loading.value = false
        }
    }

    fun clear() {
        _messages.value = listOf(greeting)
    }
}
