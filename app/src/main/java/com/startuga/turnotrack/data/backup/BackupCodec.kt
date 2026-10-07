package com.startuga.turnotrack.data.backup

import com.startuga.turnotrack.domain.ShiftType
import com.startuga.turnotrack.domain.TimeFormat
import com.startuga.turnotrack.domain.WorkEntry
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.put
import java.time.LocalDate

/**
 * Lê e escreve backups no MESMO formato JSON da web app
 * (lista de objetos com id, date, type, isHoliday, scheduledStart, …, notes).
 * Assim um backup exportado da web app importa-se aqui, e vice-versa.
 */
object BackupCodec {

    private val prettyJson = Json { prettyPrint = true }
    private val dateRegex = Regex("""^\d{4}-\d{2}-\d{2}$""")

    sealed interface DecodeResult {
        data class Success(val entries: List<WorkEntry>) : DecodeResult
        data class Failure(val message: String) : DecodeResult
    }

    fun toJsonArray(entries: List<WorkEntry>): JsonArray = buildJsonArray {
        entries.sortedBy { it.date }.forEach { e ->
            addJsonObject {
                put("id", e.id)
                put("date", e.date.toString())
                put("type", e.type.name)
                put("isHoliday", e.isHoliday)
                TimeFormat.format(e.scheduledStart)?.let { put("scheduledStart", it) }
                TimeFormat.format(e.scheduledEnd)?.let { put("scheduledEnd", it) }
                TimeFormat.format(e.actualStart)?.let { put("actualStart", it) }
                TimeFormat.format(e.actualEnd)?.let { put("actualEnd", it) }
                TimeFormat.format(e.overtimeStart)?.let { put("overtimeStart", it) }
                TimeFormat.format(e.overtimeEnd)?.let { put("overtimeEnd", it) }
                e.notes?.let { put("notes", it) }
            }
        }
    }

    fun encode(entries: List<WorkEntry>): String =
        prettyJson.encodeToString(JsonElement.serializer(), toJsonArray(entries))

    /** Mesma validação da web app: ignora itens sem data válida e corrige campos inválidos. */
    fun decode(content: String): DecodeResult {
        val root = try {
            Json.parseToJsonElement(content)
        } catch (e: Exception) {
            return DecodeResult.Failure("Erro ao ler o ficheiro. Verifica se é um backup válido.")
        }
        if (root !is JsonArray) {
            return DecodeResult.Failure("Formato inválido (o ficheiro deve conter uma lista).")
        }

        val valid = root.mapNotNull { item -> parseEntry(item) }
        if (valid.isEmpty() && root.isNotEmpty()) {
            return DecodeResult.Failure("Nenhum registo válido encontrado no ficheiro.")
        }
        return DecodeResult.Success(valid)
    }

    private fun parseEntry(item: JsonElement): WorkEntry? {
        val obj = item as? JsonObject ?: return null
        val dateStr = obj.string("date") ?: return null
        if (!dateRegex.matches(dateStr)) return null
        val date = runCatching { LocalDate.parse(dateStr) }.getOrNull() ?: return null

        return WorkEntry(
            id = obj.string("id") ?: WorkEntry.newId(),
            date = date,
            type = ShiftType.fromName(obj.string("type")) ?: ShiftType.REGULAR,
            isHoliday = isTruthy(obj["isHoliday"]),
            scheduledStart = TimeFormat.parse(obj.string("scheduledStart")),
            scheduledEnd = TimeFormat.parse(obj.string("scheduledEnd")),
            actualStart = TimeFormat.parse(obj.string("actualStart")),
            actualEnd = TimeFormat.parse(obj.string("actualEnd")),
            overtimeStart = TimeFormat.parse(obj.string("overtimeStart")),
            overtimeEnd = TimeFormat.parse(obj.string("overtimeEnd")),
            notes = obj.string("notes"),
        )
    }

    private fun JsonObject.string(key: String): String? {
        val value = this[key] as? JsonPrimitive ?: return null
        return if (value.isString) value.content else null
    }

    /** Equivalente a `Boolean(x)` em JavaScript. */
    private fun isTruthy(element: JsonElement?): Boolean = when (element) {
        null, JsonNull -> false
        is JsonPrimitive -> when {
            element.isString -> element.content.isNotEmpty()
            element.booleanOrNull != null -> element.booleanOrNull == true
            else -> element.doubleOrNull?.let { it != 0.0 && !it.isNaN() } ?: false
        }
        else -> true // objetos e listas
    }
}
