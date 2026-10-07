package com.startuga.turnotrack.ui.assistant

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

private val SUGGESTIONS = listOf(
    "Quantas horas extras fiz este mês?",
    "Quantos minutos de atraso tenho este ano?",
    "Regista folga amanhã",
    "Quantos dias de férias já gozei?",
)

@Composable
fun AssistantScreen(
    viewModel: AssistantViewModel,
    hasApiKey: Boolean,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val loading by viewModel.loading.collectAsStateWithLifecycle()
    var input by rememberSaveable { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size, loading) {
        val count = listState.layoutInfo.totalItemsCount
        if (count > 0) listState.animateScrollToItem(count - 1)
    }

    fun send(text: String) {
        viewModel.send(text)
        input = ""
    }

    Column(modifier.fillMaxSize().imePadding()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (!hasApiKey) {
                item { NoKeyCard(onOpenSettings) }
            }
            items(messages, key = { it.id }) { message -> MessageBubble(message) }
            if (loading) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(10.dp))
                        Text("A pensar…", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            if (messages.size == 1 && !loading && hasApiKey) {
                item { Suggestions(onPick = ::send) }
            }
        }

        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Escreve uma mensagem…") },
                enabled = hasApiKey,
                maxLines = 4,
                shape = RoundedCornerShape(24.dp),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Send,
                ),
                keyboardActions = KeyboardActions(onSend = { if (input.isNotBlank()) send(input) }),
            )
            Spacer(Modifier.width(8.dp))
            FilledIconButton(
                onClick = { send(input) },
                enabled = hasApiKey && !loading && input.isNotBlank(),
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Enviar")
            }
        }
    }
}

@Composable
private fun MessageBubble(message: ChatMessage) {
    val fromUser = message.fromUser
    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (fromUser) Arrangement.End else Arrangement.Start) {
        Surface(
            modifier = Modifier.widthIn(max = 320.dp),
            shape = RoundedCornerShape(
                topStart = 18.dp,
                topEnd = 18.dp,
                bottomStart = if (fromUser) 18.dp else 4.dp,
                bottomEnd = if (fromUser) 4.dp else 18.dp,
            ),
            color = if (fromUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = if (fromUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
        ) {
            Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                if (!fromUser) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.AutoAwesome, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(4.dp))
                        Text("Assistente", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    }
                    Spacer(Modifier.size(4.dp))
                }
                SelectionContainer {
                    Text(message.text, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Suggestions(onPick: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Experimenta perguntar:", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SUGGESTIONS.forEach { text ->
                SuggestionChip(onClick = { onPick(text) }, label = { Text(text) })
            }
        }
    }
}

@Composable
private fun NoKeyCard(onOpenSettings: () -> Unit) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Key, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                Spacer(Modifier.width(8.dp))
                Text(
                    "Falta a chave Gemini API",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
            Text(
                "Para usar o assistente, cola a tua chave Gemini API nas Definições. Fica guardada só neste telemóvel.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Button(onClick = onOpenSettings) { Text("Abrir Definições") }
        }
    }
}
