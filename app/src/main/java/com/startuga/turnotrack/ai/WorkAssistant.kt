package com.startuga.turnotrack.ai

import com.startuga.turnotrack.data.WorkEntryRepository
import com.startuga.turnotrack.data.backup.BackupCodec
import com.startuga.turnotrack.domain.ShiftType
import com.startuga.turnotrack.domain.TimeFormat
import com.startuga.turnotrack.domain.WorkEntry
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonArray
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * O assistente da web app (geminiService.ts), agora em Kotlin:
 * mesmo prompt, mesmas ferramentas (saveWorkEntry / deleteWorkEntry), mesmas respostas.
 * Melhoria: mantém o contexto da conversa e aplica todas as ações pedidas, não só a primeira.
 */
class WorkAssistant(
    private val repository: WorkEntryRepository,
    private val client: GeminiClient = GeminiClient(),
) {

    suspend fun send(
        message: String,
        history: List<GeminiClient.Turn>,
        apiKey: String,
        model: String,
    ): String {
        if (apiKey.isBlank()) {
            return "A funcionalidade de Inteligência Artificial requer uma chave Gemini API. Adiciona-a nas Definições."
        }

        val entries = repository.getAll()
        val reply = try {
            client.generate(
                apiKey = apiKey,
                model = model,
                systemInstruction = systemPrompt(entries),
                turns = history.takeLast(MAX_HISTORY) + GeminiClient.Turn("user", message),
                functionDeclarations = tools,
            )
        } catch (e: GeminiClient.GeminiException) {
            return "Erro no assistente: ${e.message}"
        } catch (e: Exception) {
            return "Ocorreu um erro ao contactar o assistente inteligente."
        }

        val results = reply.functionCalls.mapNotNull { call ->
            when (call.name) {
                "saveWorkEntry" -> saveFromArgs(call.args)
                "deleteWorkEntry" -> deleteFromArgs(call.args)
                else -> null
            }
        }

        return when {
            results.isNotEmpty() -> results.joinToString("\n")
            reply.text != null -> reply.text
            else -> "Desculpa, não consegui processar a tua resposta."
        }
    }

    private suspend fun saveFromArgs(args: JsonObject): String {
        val date = args.string("date")?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
            ?: return "Não percebi a data do registo."
        val existing = repository.get(date)

        // Tal como na web app: os campos enviados pelo assistente sobrepõem-se ao registo existente.
        val base = existing ?: WorkEntry(id = WorkEntry.newId(), date = date, type = ShiftType.REGULAR)
        val updated = base.copy(
            type = args.string("type")?.let { ShiftType.fromName(it.uppercase(Locale.ROOT)) } ?: base.type,
            isHoliday = (args["isHoliday"] as? JsonPrimitive)?.booleanOrNull ?: base.isHoliday,
            scheduledStart = args.time("scheduledStart", base.scheduledStart),
            scheduledEnd = args.time("scheduledEnd", base.scheduledEnd),
            actualStart = args.time("actualStart", base.actualStart),
            actualEnd = args.time("actualEnd", base.actualEnd),
            overtimeStart = args.time("overtimeStart", base.overtimeStart),
            overtimeEnd = args.time("overtimeEnd", base.overtimeEnd),
            notes = if (args.containsKey("notes")) args.string("notes")?.trim()?.ifEmpty { null } else base.notes,
        )
        repository.save(updated)
        return "Registo para o dia $date guardado com sucesso!"
    }

    private suspend fun deleteFromArgs(args: JsonObject): String {
        val raw = args.string("date")
        val date = raw?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
            ?: return "Não percebi a data do registo a apagar."
        return if (repository.delete(date) != null) {
            "Registo do dia $date apagado com sucesso!"
        } else {
            "Não foi encontrado nenhum registo para o dia $date."
        }
    }

    private fun JsonObject.string(key: String): String? =
        (this[key] as? JsonPrimitive)?.takeIf { it !is JsonNull }?.content

    private fun JsonObject.time(key: String, fallback: java.time.LocalTime?) =
        if (containsKey(key)) TimeFormat.parse(string(key)) ?: fallback else fallback

    private fun systemPrompt(entries: List<WorkEntry>): String {
        val now = ZonedDateTime.now()
        val currentDate = now.format(DateTimeFormatter.ofPattern("EEEE, dd/MM/yyyy, HH:mm:ss", PT))
        val data = BackupCodec.toJsonArray(entries).toString()
        return """
            Tu és um assistente inteligente de gestão de registos de trabalho pessoal chamado "Registo de Trabalho AI".
            O utilizador trabalha por turnos rotativos.
            Tens acesso aos registos de trabalho do utilizador em formato JSON.

            A data e hora atual do sistema é: $currentDate. Usa esta informação sempre que o utilizador perguntar "que dia é hoje", "que horas são" ou fizer perguntas relativas ao dia atual, ontem, amanhã, este mês, este ano, etc.

            DADOS: $data

            REGRAS:
            1. Responde de forma concisa, educada e clara em Português (Portugal).
            2. Podes calcular totais de horas trabalhadas, dias de folga, dias de falta, dias de férias, atrasos e horas extras baseados nos dados fornecidos.
            3. Se o utilizador pedir para adicionar, alterar ou apagar um registo, usa as ferramentas (function calls) 'saveWorkEntry' ou 'deleteWorkEntry'.
            4. Quando o utilizador pedir para registar férias, usa o tipo 'VACATION'.
            5. Quando o utilizador pedir para registar folga, usa o tipo 'DAY_OFF'.
            6. Quando o utilizador pedir para registar falta, usa o tipo 'ABSENT'.
            7. Formata os tempos sempre em "Xh Ym" (ex: 8h 30m).
            8. O teu foco é ajudar o utilizador a acompanhar a sua assiduidade e produtividade.
            9. Responde em texto simples, sem formatação Markdown.
        """.trimIndent()
    }

    private val tools = buildJsonArray {
        add(
            GeminiClient.functionDeclaration(
                name = "saveWorkEntry",
                description = "Adiciona ou atualiza um registo de trabalho para um determinado dia.",
                properties = mapOf(
                    "date" to ("STRING" to "A data do registo no formato YYYY-MM-DD."),
                    "type" to ("STRING" to "O tipo de turno: 'REGULAR', 'DAY_OFF' (folga), 'ABSENT' (falta) ou 'VACATION' (férias)."),
                    "isHoliday" to ("BOOLEAN" to "Se é um dia de feriado."),
                    "scheduledStart" to ("STRING" to "Hora de início planeada no formato HH:mm (ex: 09:00)."),
                    "scheduledEnd" to ("STRING" to "Hora de fim planeada no formato HH:mm (ex: 18:00)."),
                    "actualStart" to ("STRING" to "Hora de início real no formato HH:mm."),
                    "actualEnd" to ("STRING" to "Hora de fim real no formato HH:mm."),
                    "overtimeStart" to ("STRING" to "Hora de início de horas extras no formato HH:mm."),
                    "overtimeEnd" to ("STRING" to "Hora de fim de horas extras no formato HH:mm."),
                    "notes" to ("STRING" to "Notas opcionais sobre o turno."),
                ),
                required = listOf("date", "type"),
            )
        )
        add(
            GeminiClient.functionDeclaration(
                name = "deleteWorkEntry",
                description = "Apaga o registo de trabalho de um determinado dia.",
                properties = mapOf(
                    "date" to ("STRING" to "A data do registo a apagar no formato YYYY-MM-DD."),
                ),
                required = listOf("date"),
            )
        )
    }

    companion object {
        private const val MAX_HISTORY = 20
        private val PT: Locale = Locale.forLanguageTag("pt-PT")
    }
}
