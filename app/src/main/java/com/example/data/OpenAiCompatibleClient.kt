package com.example.data

import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

object OpenAiCompatibleClient {

    private val THINK_BLOCK_REGEX = Regex("""<think>[\s\S]*?</think>""", RegexOption.IGNORE_CASE)
    private val CODE_FENCE_REGEX = Regex("""^```[a-zA-Z0-9_-]*\s*|\s*```$""", RegexOption.MULTILINE)

    fun resolveEffectiveApiKey(userEnteredKey: String): String {
        val trimmed = userEnteredKey.trim()
        if (trimmed.isNotEmpty()) return trimmed
        val buildConfigKey = runCatching { BuildConfig.OPENAI_COMPATIBLE_API_KEY }.getOrDefault("").trim()
        if (buildConfigKey.isNotEmpty() && buildConfigKey != "MY_OPENAI_COMPATIBLE_API_KEY") {
            return buildConfigKey
        }
        return ""
    }

    private fun cleanBaseUrl(baseUrl: String): String {
        return baseUrl
            .trim()
            .trimEnd('/')
            .removeSuffix("/chat/completions")
            .removeSuffix("/models")
            .trimEnd('/')
    }

    private fun normalizeEndpointUrl(baseUrl: String, pathSuffix: String): String {
        val cleanBase = cleanBaseUrl(baseUrl)
        val cleanSuffix = pathSuffix.trimStart('/')
        return "$cleanBase/$cleanSuffix"
    }

    internal fun buildCandidateModelEndpoints(baseUrl: String): List<String> {
        val cleanBase = cleanBaseUrl(baseUrl)
        if (cleanBase.isEmpty()) return emptyList()
        return if (cleanBase.endsWith("/v1", ignoreCase = true) ||
            cleanBase.endsWith("/v1beta", ignoreCase = true)
        ) {
            listOf(
                "$cleanBase/models",
                "${cleanBase.removeSuffix("/v1").removeSuffix("/V1")}/models"
            ).distinct()
        } else {
            listOf(
                "$cleanBase/v1/models",
                "$cleanBase/models"
            ).distinct()
        }
    }

    private val MODEL_ID_JSON_FIELD_REGEX = Regex(
        """"(?:id|name|model)"\s*:\s*"([^"]+)"""",
        RegexOption.IGNORE_CASE
    )

    /**
     * Parses model IDs from OpenAI-compatible `{"data": [{"id": "..."}]}`,
     * Google/Ollama `{"models": [{"id": "...", "name": "models/..."}]}`, or direct JSONArray responses.
     */
    fun parseModelsFromJsonBody(rawBody: String): List<String> {
        val trimmed = rawBody.trim()
        if (trimmed.isEmpty()) return emptyList()
        val extracted = mutableListOf<String>()

        runCatching {
            if (trimmed.startsWith("[")) {
                val arr = JSONArray(trimmed)
                extractModelIdsFromArray(arr, extracted)
            } else {
                val root = JSONObject(trimmed)
                root.optJSONArray("data")?.let { extractModelIdsFromArray(it, extracted) }
                if (extracted.isEmpty()) {
                    root.optJSONArray("models")?.let { extractModelIdsFromArray(it, extracted) }
                }
            }
        }

        if (extracted.isEmpty()) {
            for (match in MODEL_ID_JSON_FIELD_REGEX.findAll(trimmed)) {
                val rawVal = match.groupValues.getOrNull(1).orEmpty().trim()
                val normalized = normalizeModelIdentifier(rawVal)
                if (normalized.isNotEmpty() && !normalized.equals("list", ignoreCase = true)) {
                    extracted.add(normalized)
                }
            }
        }

        return rankAndDeduplicateModels(extracted)
    }

    private fun extractModelIdsFromArray(arr: JSONArray, out: MutableList<String>) {
        for (i in 0 until arr.length()) {
            val obj = arr.optJSONObject(i)
            if (obj != null) {
                val rawId = obj.optString("id", "")
                    .ifBlank { obj.optString("name", "") }
                    .ifBlank { obj.optString("model", "") }
                    .trim()
                val normalized = normalizeModelIdentifier(rawId)
                if (normalized.isNotEmpty()) {
                    out.add(normalized)
                }
            } else {
                val str = arr.optString(i, "").trim()
                val normalized = normalizeModelIdentifier(str)
                if (normalized.isNotEmpty()) {
                    out.add(normalized)
                }
            }
        }
    }

    private fun normalizeModelIdentifier(rawId: String): String {
        val clean = rawId.trim()
        return if (clean.startsWith("models/", ignoreCase = true)) {
            clean.substringAfter("/")
        } else {
            clean
        }
    }

    private fun rankAndDeduplicateModels(models: List<String>): List<String> {
        return models
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
            .sortedWith(
                compareBy<String> { model ->
                    val lower = model.lowercase()
                    when {
                        lower.contains("3.8-flash") -> 0
                        lower.contains("4-argon") || lower.contains("argon") -> 1
                        lower.contains("3.5-flash") && !lower.contains("thinking") -> 2
                        lower.contains("3.5-flash-thinking") -> 3
                        lower.contains("3.0-pro") -> 4
                        lower.contains("3.0-flash") -> 5
                        lower.contains("2.5-flash") -> 6
                        lower.contains("2.5-pro") -> 7
                        lower.contains("2.0-flash-thinking") -> 8
                        lower.contains("gemini") -> 9
                        lower.contains("deepseek") -> 10
                        else -> 20
                    }
                }.thenBy { it.lowercase() }
            )
    }

    /**
     * Calls GET {baseUrl}/v1/models or {baseUrl}/models on an OpenAI-compatible provider
     * (including gemini-web2api) to retrieve available model IDs accurately.
     */
    suspend fun fetchModels(
        baseUrl: String,
        apiKey: String,
        geminiSecure1Psid: String = "",
        geminiSecure1Psidts: String = ""
    ): Result<List<String>> = withContext(Dispatchers.IO) {
        runCatching {
            val effectiveKey = resolveEffectiveApiKey(apiKey)
            val cleanPsid = GeminiWebCookieClient.sanitizeCookieToken(geminiSecure1Psid)
            val cleanPsidts = GeminiWebCookieClient.sanitizeCookieToken(geminiSecure1Psidts)
            val endpoints = buildCandidateModelEndpoints(baseUrl)
            if (endpoints.isEmpty()) {
                throw IllegalArgumentException("Base URL kosong. Masukkan URL server terlebih dahulu.")
            }

            var lastError: String? = null
            for (endpoint in endpoints) {
                val attemptResult = runCatching {
                    val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                        requestMethod = "GET"
                        connectTimeout = 15_000
                        readTimeout = 20_000
                        setRequestProperty("Accept", "application/json")
                        if (effectiveKey.isNotEmpty()) {
                            setRequestProperty("Authorization", "Bearer $effectiveKey")
                        } else if (cleanPsid.isNotEmpty()) {
                            setRequestProperty("Authorization", "Bearer $cleanPsid")
                        }
                        if (cleanPsid.isNotEmpty()) {
                            setRequestProperty(
                                "Cookie",
                                GeminiWebCookieClient.buildCookieHeaderValue(cleanPsid, cleanPsidts)
                            )
                            setRequestProperty("X-Gemini-PSID", cleanPsid)
                            if (cleanPsidts.isNotEmpty()) {
                                setRequestProperty("X-Gemini-PSIDTS", cleanPsidts)
                            }
                        }
                    }

                    try {
                        val code = connection.responseCode
                        val body = readResponseStream(connection, isError = code !in 200..299)
                        if (code !in 200..299) {
                            val errMsg = extractApiErrorMessage(body) ?: "HTTP $code dari $endpoint"
                            throw IllegalStateException(errMsg)
                        }
                        val parsedModels = parseModelsFromJsonBody(body)
                        if (parsedModels.isEmpty()) {
                            throw IllegalStateException("Respons dari $endpoint tidak berisi daftar model yang valid.")
                        }
                        parsedModels
                    } finally {
                        connection.disconnect()
                    }
                }

                if (attemptResult.isSuccess) {
                    return@runCatching attemptResult.getOrThrow()
                } else {
                    lastError = attemptResult.exceptionOrNull()?.localizedMessage
                }
            }

            throw IllegalStateException(lastError ?: "Gagal mengambil daftar model dari server.")
        }
    }

    /**
     * Calls POST {baseUrl}/chat/completions with a model-adapted System Prompt and numbered dialogue lines.
     * Supports combining System Prompt + User Lines into a single user message for 100% compatibility
     * with `gemini-web2api` servers.
     */
    suspend fun translateSubtitleBatch(
        baseUrl: String,
        apiKey: String,
        modelId: String,
        systemPrompt: String,
        numberedUserLines: String,
        temperature: Float = 0.3f,
        combineSystemIntoUserPrompt: Boolean = false,
        geminiSecure1Psid: String = "",
        geminiSecure1Psidts: String = "",
        allowNoApiKey: Boolean = false
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val effectiveKey = resolveEffectiveApiKey(apiKey)
            val cleanPsid = GeminiWebCookieClient.sanitizeCookieToken(geminiSecure1Psid)
            val cleanPsidts = GeminiWebCookieClient.sanitizeCookieToken(geminiSecure1Psidts)
            val isLocalOrGeminiServer = allowNoApiKey ||
                cleanPsid.isNotBlank() ||
                baseUrl.contains("localhost", ignoreCase = true) ||
                baseUrl.contains("127.0.0.1") ||
                baseUrl.contains("10.0.2.2") ||
                baseUrl.contains("192.168.") ||
                baseUrl.startsWith("http://", ignoreCase = true)

            if (effectiveKey.isEmpty() && !isLocalOrGeminiServer) {
                throw IllegalArgumentException(
                    "API Key atau Cookie belum diisi. Masukkan Cookie __Secure-1PSID / API Key provider terlebih dahulu."
                )
            }

            val cleanBase = cleanBaseUrl(baseUrl)
            val endpoint = if (cleanBase.endsWith("/v1", ignoreCase = true) ||
                cleanBase.endsWith("/v1beta", ignoreCase = true) ||
                cleanBase.contains("/api/", ignoreCase = true)
            ) {
                "$cleanBase/chat/completions"
            } else {
                "$cleanBase/v1/chat/completions"
            }

            val lowerModel = modelId.lowercase().trim()
            val isOpenAiReasoningSeries = lowerModel.startsWith("o1") ||
                lowerModel.startsWith("o3") ||
                lowerModel.contains("/o1") ||
                lowerModel.contains("/o3")

            val messagesArray = JSONArray().apply {
                if (combineSystemIntoUserPrompt) {
                    val combinedContent = buildString {
                        appendLine(systemPrompt.trim())
                        appendLine()
                        appendLine("--- DAFTAR DIALOG SUBTITLE ---")
                        append(numberedUserLines.trim())
                    }
                    put(
                        JSONObject().apply {
                            put("role", "user")
                            put("content", combinedContent)
                        }
                    )
                } else {
                    put(
                        JSONObject().apply {
                            put("role", "system")
                            put("content", systemPrompt)
                        }
                    )
                    put(
                        JSONObject().apply {
                            put("role", "user")
                            put("content", numberedUserLines)
                        }
                    )
                }
            }

            val requestJson = JSONObject().apply {
                put("model", modelId.trim())
                put("messages", messagesArray)
                put("stream", false)
                if (!isOpenAiReasoningSeries) {
                    put("temperature", temperature.toDouble())
                }
            }

            val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                connectTimeout = 20_000
                readTimeout = 90_000
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                setRequestProperty("Accept", "application/json")
                if (effectiveKey.isNotEmpty()) {
                    setRequestProperty("Authorization", "Bearer $effectiveKey")
                } else if (cleanPsid.isNotEmpty()) {
                    setRequestProperty("Authorization", "Bearer $cleanPsid")
                }
                if (cleanPsid.isNotEmpty()) {
                    setRequestProperty(
                        "Cookie",
                        GeminiWebCookieClient.buildCookieHeaderValue(cleanPsid, cleanPsidts)
                    )
                    setRequestProperty("X-Gemini-PSID", cleanPsid)
                    if (cleanPsidts.isNotEmpty()) {
                        setRequestProperty("X-Gemini-PSIDTS", cleanPsidts)
                    }
                }
            }

            try {
                OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use { writer ->
                    writer.write(requestJson.toString())
                    writer.flush()
                }

                val code = connection.responseCode
                val responseBody = readResponseStream(connection, isError = code !in 200..299)
                if (code !in 200..299) {
                    val errDetail = extractApiErrorMessage(responseBody) ?: "HTTP $code dari provider AI"
                    throw IllegalStateException(errDetail)
                }

                val root = JSONObject(responseBody)
                val choices = root.optJSONArray("choices")
                if (choices == null || choices.length() == 0) {
                    throw IllegalStateException("Respons AI tidak memiliki choices[].")
                }
                val firstChoice = choices.getJSONObject(0)
                val messageObj = firstChoice.optJSONObject("message")
                val rawContent = messageObj?.optString("content", "")
                    ?: firstChoice.optString("text", "")

                cleanAiModelOutput(rawContent)
            } finally {
                connection.disconnect()
            }
        }
    }

    /**
     * Cleans <think>...</think> reasoning blocks and markdown code fences from AI output
     * so only the numbered subtitle lines remain.
     */
    fun cleanAiModelOutput(rawContent: String): String {
        val withoutThink = THINK_BLOCK_REGEX.replace(rawContent, "")
        val withoutFences = CODE_FENCE_REGEX.replace(withoutThink, "")
        return withoutFences.trim()
    }

    private fun readResponseStream(connection: HttpURLConnection, isError: Boolean): String {
        val stream = if (isError) connection.errorStream else connection.inputStream
        if (stream == null) return ""
        return BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).use { it.readText() }
    }

    private fun extractApiErrorMessage(rawJson: String): String? {
        if (rawJson.isBlank()) return null
        return try {
            val obj = JSONObject(rawJson)
            val err = obj.opt("error")
            when (err) {
                is JSONObject -> err.optString("message").takeIf { it.isNotBlank() }
                is String -> err.takeIf { it.isNotBlank() }
                else -> obj.optString("message").takeIf { it.isNotBlank() }
            }
        } catch (_: Exception) {
            rawJson.take(180)
        }
    }
}
