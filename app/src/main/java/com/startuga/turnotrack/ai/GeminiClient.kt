package com.startuga.turnotrack.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.add
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URI

/**
 * Cliente mínimo da Gemini API (REST `generateContent`), sem SDK.
 * A chave é passada em cada pedido e nunca fica no código nem no APK —
 * é introduzida pelo utilizador nas Definições e guardada só no telemóvel.
 */
class GeminiClient {

    data class Turn(val role: String, val text: String) // role: "user" ou "model"
    data class FunctionCall(val name: String, val args: JsonObject)
    data class Reply(val text: String?, val functionCalls: List<FunctionCall>)

    class GeminiException(message: String) : Exception(message)

    private val json = Json { ignoreUnknownKeys = true }
    private val modelPattern = Regex("""^[A-Za-z0-9._-]+$""")

    suspend fun generate(
        apiKey: String,
        model: String,
        systemInstruction: String,
        turns: List<Turn>,
        functionDeclarations: JsonArray,
    ): Reply = withContext(Dispatchers.IO) {
        val modelName = model.trim().removePrefix("models/")
        if (!modelPattern.matches(modelName)) throw GeminiException("Nome de modelo inválido: $model")

        val body = buildJsonObject {
            putJsonObject("systemInstruction") {
                putJsonArray("parts") { addJsonObject { put("text", systemInstruction) } }
            }
            putJsonArray("contents") {
                turns.forEach { turn ->
                    addJsonObject {
                        put("role", turn.role)
                        putJsonArray("parts") { addJsonObject { put("text", turn.text) } }
                    }
                }
            }
            putJsonArray("tools") {
                addJsonObject { put("functionDeclarations", functionDeclarations) }
            }
        }

        val url = URI.create("https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent").toURL()
        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 15_000
            readTimeout = 90_000
            doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
            setRequestProperty("x-goog-api-key", apiKey)
        }

        try {
            connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val text = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            if (code !in 200..299) throw GeminiException(errorMessage(code, text))
            parseReply(text)
        } catch (e: IOException) {
            throw GeminiException("Sem ligação à internet ou o servidor não respondeu (${e.javaClass.simpleName}).")
        } finally {
            connection.disconnect()
        }
    }

    private fun errorMessage(code: Int, body: String): String {
        val apiMessage = runCatching {
            val root = json.parseToJsonElement(body) as JsonObject
            val error = root["error"] as JsonObject
            (error["message"] as JsonPrimitive).content
        }.getOrNull()
        return when (code) {
            400, 403 -> if (apiMessage?.contains("API key", ignoreCase = true) == true) {
                "A chave Gemini API não é válida. Confirma-a nas Definições."
            } else apiMessage ?: "Pedido recusado ($code)."
            404 -> "Modelo não encontrado. Verifica o nome do modelo nas Definições. ${apiMessage.orEmpty()}".trim()
            429 -> "Limite de pedidos atingido. Tenta novamente daqui a pouco."
            else -> apiMessage ?: "Erro do servidor ($code)."
        }
    }

    private fun parseReply(body: String): Reply {
        val root = runCatching { json.parseToJsonElement(body) as JsonObject }.getOrNull()
            ?: throw GeminiException("Resposta inválida do servidor.")

        val candidates = root["candidates"] as? JsonArray
        val first = candidates?.firstOrNull() as? JsonObject
        if (first == null) {
            val reason = ((root["promptFeedback"] as? JsonObject)?.get("blockReason") as? JsonPrimitive)?.content
            throw GeminiException(if (reason != null) "Pedido bloqueado ($reason)." else "O assistente não devolveu resposta.")
        }

        val parts = ((first["content"] as? JsonObject)?.get("parts") as? JsonArray).orEmpty()
        val texts = mutableListOf<String>()
        val calls = mutableListOf<FunctionCall>()
        for (part in parts) {
            val obj = part as? JsonObject ?: continue
            val isThought = (obj["thought"] as? JsonPrimitive)?.content == "true"
            val text = (obj["text"] as? JsonPrimitive)?.content
            if (text != null && !isThought) texts += text
            val call = obj["functionCall"] as? JsonObject
            if (call != null) {
                val name = (call["name"] as? JsonPrimitive)?.content ?: continue
                calls += FunctionCall(name, call["args"] as? JsonObject ?: JsonObject(emptyMap()))
            }
        }
        return Reply(texts.joinToString("").trim().ifEmpty { null }, calls)
    }

    companion object {
        /** Ajuda a construir o schema das ferramentas no formato da API. */
        fun functionDeclaration(
            name: String,
            description: String,
            properties: Map<String, Pair<String, String>>, // nome -> (TIPO, descrição)
            required: List<String>,
        ): JsonObject = buildJsonObject {
            put("name", name)
            put("description", description)
            putJsonObject("parameters") {
                put("type", "OBJECT")
                putJsonObject("properties") {
                    properties.forEach { (prop, spec) ->
                        putJsonObject(prop) {
                            put("type", spec.first)
                            put("description", spec.second)
                        }
                    }
                }
                putJsonArray("required") { required.forEach { add(it) } }
            }
        }
    }
}
