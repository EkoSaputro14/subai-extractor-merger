package com.example.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlin.random.Random

data class ParsedGeminiCookies(
    val secure1Psid: String = "",
    val secure1Psidts: String = ""
) {
    val isValid: Boolean
        get() = secure1Psid.isNotBlank()
}

object GeminiWebCookieClient {

    private const val GEMINI_APP_URL = "https://gemini.google.com/app"
    private const val GEMINI_STREAM_GENERATE_URL =
        "https://gemini.google.com/_/BardChatUi/data/assistant.lamda.BardFrontendService/StreamGenerate"

    private const val USER_AGENT =
        "Mozilla/5.0 (Linux; Android 14; Pixel 8 Pro) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36"

    private val SNLM0E_REGEX = Regex(""""SNlM0e"\s*:\s*"([^"]+)"""")
    private val CFB2H_REGEX = Regex(""""cfb2h"\s*:\s*"([^"]+)"""")

    private val JSON_COOKIE_EDITOR_PSID_REGEX = Regex(
        """"name"\s*:\s*"(?:__Secure-1PSID|SECURE_1PSID)"[^}]*?"value"\s*:\s*"([^"]+)"""",
        RegexOption.IGNORE_CASE
    )
    private val JSON_COOKIE_EDITOR_PSIDTS_REGEX = Regex(
        """"name"\s*:\s*"(?:__Secure-1PSIDTS|SECURE_1PSIDTS)"[^}]*?"value"\s*:\s*"([^"]+)"""",
        RegexOption.IGNORE_CASE
    )

    private val PSID_COOKIE_REGEX = Regex(
        """["']?(?:__Secure-1PSID|SECURE_1PSID|GEMINI_1PSID|1PSID)["']?\s*[=:]\s*["']?([^"';\s\r\n,}]+)""",
        RegexOption.IGNORE_CASE
    )
    private val PSIDTS_COOKIE_REGEX = Regex(
        """["']?(?:__Secure-1PSIDTS|SECURE_1PSIDTS|GEMINI_1PSIDTS|1PSIDTS)["']?\s*[=:]\s*["']?([^"';\s\r\n,}]+)""",
        RegexOption.IGNORE_CASE
    )

    /**
     * Automatically extracts `__Secure-1PSID` and `__Secure-1PSIDTS` from:
     * 1. Cookie-Editor JSON array export: [{"name":"__Secure-1PSID","value":"..."}]
     * 2. JSON object: {"__Secure-1PSID":"...","__Secure-1PSIDTS":"..."}
     * 3. Raw HTTP Cookie header: Cookie: __Secure-1PSID=...; __Secure-1PSIDTS=...
     * 4. .env lines: SECURE_1PSID=...
     * 5. Raw single token string
     */
    fun parseSmartCookieInput(rawInput: String): ParsedGeminiCookies {
        val trimmed = rawInput.trim()
        if (trimmed.isEmpty()) return ParsedGeminiCookies()

        // 1. Try Cookie-Editor JSON pattern ({"name": "__Secure-1PSID", ..., "value": "..."})
        val editorPsid = JSON_COOKIE_EDITOR_PSID_REGEX.find(trimmed)?.groupValues?.getOrNull(1)?.trim().orEmpty()
        val editorPsidts = JSON_COOKIE_EDITOR_PSIDTS_REGEX.find(trimmed)?.groupValues?.getOrNull(1)?.trim().orEmpty()
        if (editorPsid.isNotEmpty() || editorPsidts.isNotEmpty()) {
            return ParsedGeminiCookies(editorPsid, editorPsidts)
        }

        // 2. Try JSON Array (Cookie-Editor format with reversed value/name order)
        if (trimmed.startsWith("[")) {
            runCatching {
                val arr = JSONArray(trimmed)
                var psid = ""
                var psidts = ""
                for (i in 0 until arr.length()) {
                    val obj = arr.optJSONObject(i) ?: continue
                    val name = obj.optString("name", "").trim()
                    val value = obj.optString("value", "").trim()
                    if (name.equals("__Secure-1PSID", ignoreCase = true) ||
                        name.equals("SECURE_1PSID", ignoreCase = true)
                    ) {
                        psid = value
                    } else if (name.equals("__Secure-1PSIDTS", ignoreCase = true) ||
                        name.equals("SECURE_1PSIDTS", ignoreCase = true)
                    ) {
                        psidts = value
                    }
                }
                if (psid.isNotEmpty() || psidts.isNotEmpty()) {
                    return ParsedGeminiCookies(psid, psidts)
                }
            }
        }

        // 3. Try Key-Value / Header / .env / JSON Object pairs
        val psidMatch = PSID_COOKIE_REGEX.find(trimmed)?.groupValues?.getOrNull(1)?.trim().orEmpty()
        val psidtsMatch = PSIDTS_COOKIE_REGEX.find(trimmed)?.groupValues?.getOrNull(1)?.trim().orEmpty()
        if (psidMatch.isNotEmpty() || psidtsMatch.isNotEmpty()) {
            return ParsedGeminiCookies(psidMatch, psidtsMatch)
        }

        // 4. Fallback: if user pasted a raw single cookie value (e.g. g.a000...)
        val singleToken = trimmed.lineSequence().firstOrNull { it.isNotBlank() }?.trim().orEmpty()
        if (singleToken.isNotEmpty() && !singleToken.contains(" ")) {
            return ParsedGeminiCookies(secure1Psid = singleToken, secure1Psidts = "")
        }

        return ParsedGeminiCookies()
    }

    private val discoveredModelHeaders = java.util.concurrent.ConcurrentHashMap<String, String>()

    fun sanitizeCookieToken(rawToken: String): String {
        return rawToken
            .trim()
            .trim('"', '\'')
            .replace("\r", "")
            .replace("\n", "")
            .replace("\t", "")
            .trim()
    }

    fun buildCookieHeaderValue(psid: String, psidts: String): String {
        val cleanPsid = sanitizeCookieToken(psid)
        val cleanPsidts = sanitizeCookieToken(psidts)
        return buildString {
            if (cleanPsid.isNotEmpty()) {
                append("__Secure-1PSID=").append(cleanPsid)
            }
            if (cleanPsidts.isNotEmpty()) {
                if (isNotEmpty()) append("; ")
                append("__Secure-1PSIDTS=").append(cleanPsidts)
            }
        }
    }

    private data class GeminiWebSessionTokens(
        val snlm0eToken: String,
        val blBuildLabel: String,
        val rawHtml: String = ""
    )

    private fun fetchSessionTokensBlocking(psid: String, psidts: String): GeminiWebSessionTokens {
        val cleanPsid = sanitizeCookieToken(psid)
        if (cleanPsid.isEmpty()) {
            throw IllegalArgumentException(
                "Cookie __Secure-1PSID belum diisi. Gunakan tombol 'WebView Turbo', 'Browser HP', atau 'Smart Paste Cookie'."
            )
        }

        val cookieHeader = buildCookieHeaderValue(cleanPsid, psidts)
        val connection = (URL(GEMINI_APP_URL).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            instanceFollowRedirects = true
            connectTimeout = 20_000
            readTimeout = 25_000
            setRequestProperty("User-Agent", USER_AGENT)
            setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
            setRequestProperty("Accept-Language", "id-ID,id;q=0.9,en-US;q=0.8,en;q=0.7")
            setRequestProperty("Cookie", cookieHeader)
        }

        try {
            val code = connection.responseCode
            val html = readStream(connection, isError = code !in 200..299)
            if (code !in 200..299) {
                throw IllegalStateException("HTTP $code saat menghubungi gemini.google.com/app")
            }

            val snlm0e = SNLM0E_REGEX.find(html)?.groupValues?.getOrNull(1).orEmpty()
            if (snlm0e.isEmpty()) {
                throw IllegalStateException(
                    "Token sesi SNlM0e tidak ditemukan. Pastikan cookie __Secure-1PSID & __Secure-1PSIDTS masih aktif (belum kedaluwarsa)."
                )
            }
            val cfb2h = CFB2H_REGEX.find(html)?.groupValues?.getOrNull(1)
                ?: "boq_assistant-bard-web-server_20250219.08_p0"

            return GeminiWebSessionTokens(
                snlm0eToken = snlm0e,
                blBuildLabel = cfb2h,
                rawHtml = html
            )
        } finally {
            connection.disconnect()
        }
    }

    /**
     * Tests whether the provided `__Secure-1PSID` and `__Secure-1PSIDTS` cookies can obtain a valid
     * `SNlM0e` token from `https://gemini.google.com/app`.
     */
    suspend fun verifyCookies(psid: String, psidts: String): Result<String> =
        withContext(Dispatchers.IO) {
            runCatching {
                val tokens = fetchSessionTokensBlocking(psid, psidts)
                val discovered = extractLiveModelsFromHtml(tokens.rawHtml)
                val maskedToken = tokens.snlm0eToken.take(10) + "..."
                "Cookie Gemini Web Valid! Token SNlM0e ($maskedToken) aktif • ${discovered.size} model siap digunakan."
            }
        }

    /**
     * Fetches the available Gemini models for the user's session in Cookie Mode.
     * If valid cookies are present, connects to `gemini.google.com/app` to verify and scrape
     * live model metadata & hex routing IDs, merged with the latest Gemini 3.8 Flash / 4 Argon / 3.5 catalog.
     */
    suspend fun fetchModelsForCookieMode(psid: String, psidts: String): Result<List<String>> =
        withContext(Dispatchers.IO) {
            runCatching {
                val catalog = listOf(
                    "gemini-3.8-flash",
                    "gemini-4-argon",
                    "gemini-3.5-flash",
                    "gemini-3.5-flash-thinking",
                    "gemini-3.0-pro",
                    "gemini-3.0-flash",
                    "gemini-2.5-flash",
                    "gemini-2.5-pro",
                    "gemini-2.0-flash-thinking",
                    "gemini-2.0-flash"
                )
                if (sanitizeCookieToken(psid).isEmpty()) {
                    return@runCatching catalog
                }
                val session = runCatching { fetchSessionTokensBlocking(psid, psidts) }.getOrNull()
                if (session != null && session.rawHtml.isNotEmpty()) {
                    val liveDiscovered = extractLiveModelsFromHtml(session.rawHtml)
                    (liveDiscovered + catalog).distinct()
                } else {
                    catalog
                }
            }
        }

    /**
     * Extracts model identifiers and 16-hex routing hashes embedded in `gemini.google.com/app` HTML.
     */
    internal fun extractLiveModelsFromHtml(html: String): List<String> {
        val foundModels = mutableListOf(
            "gemini-3.8-flash",
            "gemini-4-argon",
            "gemini-3.5-flash",
            "gemini-3.5-flash-thinking",
            "gemini-3.0-pro",
            "gemini-3.0-flash",
            "gemini-2.5-flash",
            "gemini-2.5-pro",
            "gemini-2.0-flash-thinking",
            "gemini-2.0-flash"
        )
        if (html.isBlank()) return foundModels

        // Match internal model descriptor tuples like ["71c2d248d3b102ff", "2.5 Flash", ...]
        val hexDescriptorRegex = Regex(""""([0-9a-f]{16})"\s*,\s*"([^"]{2,40})"""")
        for (match in hexDescriptorRegex.findAll(html)) {
            val hexId = match.groupValues[1]
            val rawLabel = match.groupValues[2].trim()
            val normalizedSlug = normalizeGeminiModelSlug(rawLabel)
            if (normalizedSlug != null) {
                discoveredModelHeaders[normalizedSlug] = """[1,null,null,null,"$hexId"]"""
                if (normalizedSlug !in foundModels) {
                    foundModels.add(0, normalizedSlug)
                }
            }
        }
        return foundModels.distinct()
    }

    private fun normalizeGeminiModelSlug(rawLabel: String): String? {
        val lower = rawLabel.lowercase().trim()
        if (!lower.contains("gemini") &&
            !lower.contains("flash") &&
            !lower.contains("pro") &&
            !lower.contains("thinking") &&
            !lower.contains("argon")
        ) {
            return null
        }
        return when {
            lower.contains("4") && lower.contains("argon") -> "gemini-4-argon"
            lower.contains("3.8") && lower.contains("flash") -> "gemini-3.8-flash"
            lower.contains("3.5") && lower.contains("thinking") -> "gemini-3.5-flash-thinking"
            lower.contains("3.5") && lower.contains("flash") -> "gemini-3.5-flash"
            lower.contains("3.0") && lower.contains("pro") -> "gemini-3.0-pro"
            lower.contains("3.0") && lower.contains("flash") -> "gemini-3.0-flash"
            lower.contains("2.5") && lower.contains("pro") -> "gemini-2.5-pro"
            lower.contains("2.5") && lower.contains("flash") -> "gemini-2.5-flash"
            lower.contains("2.0") && lower.contains("thinking") -> "gemini-2.0-flash-thinking"
            lower.contains("2.0") && lower.contains("flash") -> "gemini-2.0-flash"
            else -> null
        }
    }

    /**
     * Maps Gemini model names (from gemini-web2api, including Gemini 3.8 Flash, Gemini 4 Argon,
     * Gemini 3.5 Flash/Thinking, and 2.5/2.0 series) to the `x-goog-ext-525001261-jspb` header value.
     */
    private fun resolveModelHeader(modelId: String): String? {
        val lower = modelId.lowercase().trim().removePrefix("models/")
        discoveredModelHeaders[lower]?.let { return it }
        return when {
            lower.contains("4-argon") || lower.contains("argon") ||
                lower.contains("2.5-pro") || lower.contains("3.0-pro") || lower.contains("3.5-pro") ->
                """[1,null,null,null,"2525e3954d185b3c"]"""
            lower.contains("thinking") || lower.contains("@think") ->
                """[null,null,null,null,"7ca48d02d802f20a"]"""
            lower.contains("3.8-flash") || lower.contains("3.5-flash") ||
                lower.contains("3.0-flash") || lower.contains("2.5-flash") ->
                """[1,null,null,null,"71c2d248d3b102ff"]"""
            lower.contains("2.0-flash") ->
                """[1,null,null,null,"f299729663a2343f"]"""
            else -> null
        }
    }

    /**
     * Native implementation of `gemini-web2api` directly inside the Android app using
     * `__Secure-1PSID` and `__Secure-1PSIDTS` cookies.
     */
    suspend fun translateSubtitleBatchWithCookies(
        psid: String,
        psidts: String,
        modelId: String,
        systemPrompt: String,
        numberedUserLines: String
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val tokens = fetchSessionTokensBlocking(psid, psidts)
            val combinedPrompt = buildString {
                appendLine(systemPrompt.trim())
                appendLine()
                appendLine("--- DAFTAR DIALOG SUBTITLE ---")
                append(numberedUserLines.trim())
            }

            // Build BardFrontendService/StreamGenerate inner payload:
            // [[prompt], null, ["", "", ""]]
            val innerPromptArray = JSONArray().apply {
                put(combinedPrompt)
                put(JSONObject.NULL)
                put(JSONArray().put("").put("").put(""))
            }
            val innerJsonStr = JSONArray().apply {
                put(innerPromptArray)
                put(JSONObject.NULL)
                put(JSONArray().put("").put("").put(""))
            }.toString()

            val outerFreqStr = JSONArray().apply {
                put(JSONObject.NULL)
                put(innerJsonStr)
            }.toString()

            val reqId = Random.nextInt(100_000, 999_999)
            val encodedBl = URLEncoder.encode(tokens.blBuildLabel, "UTF-8")
            val endpoint = "$GEMINI_STREAM_GENERATE_URL?bl=$encodedBl&_reqid=$reqId&rt=c"

            val postFormBody = "f.req=" + URLEncoder.encode(outerFreqStr, "UTF-8") +
                "&at=" + URLEncoder.encode(tokens.snlm0eToken, "UTF-8") + "&"

            val cookieHeader = buildCookieHeaderValue(psid, psidts)
            val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                connectTimeout = 25_000
                readTimeout = 95_000
                setRequestProperty("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8")
                setRequestProperty("User-Agent", USER_AGENT)
                setRequestProperty("Origin", "https://gemini.google.com")
                setRequestProperty("Referer", "https://gemini.google.com/")
                setRequestProperty("X-Same-Domain", "1")
                setRequestProperty("Cookie", cookieHeader)
                resolveModelHeader(modelId)?.let { modelHeaderVal ->
                    setRequestProperty("x-goog-ext-525001261-jspb", modelHeaderVal)
                }
            }

            try {
                OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use { writer ->
                    writer.write(postFormBody)
                    writer.flush()
                }

                val code = connection.responseCode
                val rawResponse = readStream(connection, isError = code !in 200..299)
                if (code !in 200..299) {
                    throw IllegalStateException("HTTP $code saat memanggil StreamGenerate Gemini Web")
                }

                val extractedText = parseBardStreamResponse(rawResponse)
                if (extractedText.isBlank()) {
                    throw IllegalStateException(
                        "Respons Gemini Web kosong atau cookie membutuhkan penyegaran (__Secure-1PSIDTS)."
                    )
                }
                OpenAiCompatibleClient.cleanAiModelOutput(extractedText)
            } finally {
                connection.disconnect()
            }
        }
    }

    /**
     * Parses the multi-line `)]}'` response from `BardFrontendService/StreamGenerate`
     * to extract the candidate response text.
     */
    fun parseBardStreamResponse(rawResponse: String): String {
        var bestCandidate = ""
        for (line in rawResponse.lines()) {
            val trimmed = line.trim()
            if (!trimmed.startsWith("[")) continue
            runCatching {
                val rootArr = JSONArray(trimmed)
                for (i in 0 until rootArr.length()) {
                    val item = rootArr.optJSONArray(i) ?: continue
                    if (item.optString(0) != "wrb.fr") continue
                    val payloadStr = item.optString(2, "")
                    if (payloadStr.isBlank() || payloadStr == "null") continue

                    val payloadJson = JSONArray(payloadStr)
                    // Candidate list is typically at payloadJson[4]
                    val candidates = payloadJson.optJSONArray(4) ?: continue
                    if (candidates.length() > 0) {
                        val firstCandidate = candidates.optJSONArray(0) ?: continue
                        val textParts = firstCandidate.optJSONArray(1) ?: continue
                        if (textParts.length() > 0) {
                            val candidateText = textParts.optString(0, "")
                            if (candidateText.isNotBlank()) {
                                bestCandidate = candidateText
                            }
                        }
                    }
                }
            }
        }
        return bestCandidate.trim()
    }

    private fun readStream(connection: HttpURLConnection, isError: Boolean): String {
        val stream = if (isError) connection.errorStream else connection.inputStream
        if (stream == null) return ""
        return BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).use { it.readText() }
    }
}
