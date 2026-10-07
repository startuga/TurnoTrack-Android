package com.startuga.turnotrack.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.startuga.turnotrack.BuildConfig
import com.startuga.turnotrack.data.SettingsRepository
import com.startuga.turnotrack.data.ThemeMode
import com.startuga.turnotrack.data.backup.BackupCodec
import com.startuga.turnotrack.domain.WorkEntry
import com.startuga.turnotrack.ui.MainViewModel
import com.startuga.turnotrack.ui.components.SectionCard
import kotlinx.coroutines.launch
import java.time.LocalDate

@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    snackbar: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val scope = rememberCoroutineScope()

    val theme by viewModel.themeMode.collectAsStateWithLifecycle()
    val entries by viewModel.entries.collectAsStateWithLifecycle()
    val savedKey by viewModel.geminiApiKey.collectAsStateWithLifecycle()
    val savedModel by viewModel.geminiModel.collectAsStateWithLifecycle()

    var pendingImport by remember { mutableStateOf<List<WorkEntry>?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) scope.launch { snackbar.showSnackbar(viewModel.exportTo(context.contentResolver, uri)) }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            val result = viewModel.readBackup(context.contentResolver, uri)
            if (result is BackupCodec.DecodeResult.Success) {
                pendingImport = result.entries
            } else if (result is BackupCodec.DecodeResult.Failure) {
                snackbar.showSnackbar(result.message)
            }
        }
    }

    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        SectionCard(title = "Aparência", icon = Icons.Outlined.Palette) {
            Column(Modifier.selectableGroup()) {
                ThemeMode.entries.forEach { mode ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = theme == mode,
                                onClick = { viewModel.setThemeMode(mode) },
                                role = Role.RadioButton,
                            )
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = theme == mode, onClick = null)
                        Spacer(Modifier.width(12.dp))
                        Text(mode.label, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }

        SectionCard(title = "Dados e backup", icon = Icons.Outlined.Storage) {
            Text(
                "Os registos ficam guardados apenas neste telemóvel. Exporta um backup de vez em quando " +
                    "para não perderes nada ou para passares os dados para outro dispositivo.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text("${entries.size} registos guardados", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            Button(
                onClick = { exportLauncher.launch("registotrabalho_backup_${LocalDate.now()}.json") },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Outlined.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Exportar backup (.json)")
            }
            OutlinedButton(
                onClick = {
                    importLauncher.launch(arrayOf("application/json", "text/json", "text/plain", "application/octet-stream"))
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Outlined.FileUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Importar / restaurar backup")
            }
            Text(
                "Aceita também os backups exportados pela versão web — é assim que passas o teu histórico para a app.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        SectionCard(title = "Assistente IA (Gemini)", icon = Icons.Outlined.AutoAwesome) {
            var keyInput by rememberSaveable(savedKey) { mutableStateOf(savedKey) }
            var modelInput by rememberSaveable(savedModel) { mutableStateOf(savedModel) }
            var showKey by rememberSaveable { mutableStateOf(false) }

            OutlinedTextField(
                value = keyInput,
                onValueChange = { keyInput = it },
                label = { Text("Chave Gemini API") },
                singleLine = true,
                visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, autoCorrectEnabled = false),
                trailingIcon = {
                    IconButton(onClick = { showKey = !showKey }) {
                        Icon(
                            if (showKey) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                            contentDescription = if (showKey) "Esconder chave" else "Mostrar chave",
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = modelInput,
                onValueChange = { modelInput = it },
                label = { Text("Modelo") },
                singleLine = true,
                supportingText = { Text("Predefinido: ${SettingsRepository.DEFAULT_GEMINI_MODEL}") },
                keyboardOptions = KeyboardOptions(autoCorrectEnabled = false),
                modifier = Modifier.fillMaxWidth(),
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = { uriHandler.openUri("https://aistudio.google.com/apikey") }) {
                    Text("Obter chave")
                }
                Spacer(Modifier.weight(1f))
                Button(
                    onClick = {
                        viewModel.saveGeminiSettings(keyInput, modelInput.ifBlank { SettingsRepository.DEFAULT_GEMINI_MODEL })
                        scope.launch { snackbar.showSnackbar("Definições do assistente guardadas.") }
                    },
                    enabled = keyInput.trim() != savedKey || modelInput.trim() != savedModel,
                ) { Text("Guardar") }
            }
            Text(
                "A chave fica guardada só neste telemóvel e não entra nos backups. Os teus registos só são " +
                    "enviados à Google (Gemini) quando fazes uma pergunta ao assistente.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        SectionCard(title = "Sobre", icon = Icons.Outlined.Info) {
            Text(
                "Registo de Trabalho para Android\nVersão ${BuildConfig.VERSION_NAME}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(8.dp))
    }

    pendingImport?.let { list ->
        AlertDialog(
            onDismissRequest = { pendingImport = null },
            title = { Text("Importar backup?") },
            text = {
                Text(
                    "O ficheiro tem ${list.size} registo(s). Ao importar, os ${entries.size} registos atuais " +
                        "são substituídos pelos do ficheiro."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    pendingImport = null
                    scope.launch {
                        viewModel.importEntries(list)
                        snackbar.showSnackbar("${list.size} registo(s) importado(s) com sucesso!")
                    }
                }) { Text("Importar") }
            },
            dismissButton = {
                TextButton(onClick = { pendingImport = null }) { Text("Cancelar") }
            },
        )
    }
}
