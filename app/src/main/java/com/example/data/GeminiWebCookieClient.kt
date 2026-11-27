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
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"

    private const val DEFAULT_BL_BUILD_LABEL = "boq_assistant-bard-web-server_20250507.06_p0"

    private val SNLM0E_REGEX = Regex("""\\?"SNlM0e\\?"\s*:\s*\\?"([^"\\]+)\\?"""")
    private val CFB2H_REGEX = Regex("""\\?"cfb2h\\?"\s*:\s*\\?"([^"\\]+)\\?"""")
    private val FDRFJE_REGEX = Regex("""\\?"FdrFJe\\?"\s*:\s*\\?"([^"\\]+)\\?"""")

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

    private val NUMBERED_LINE_REGEX = Regex(
        """^(?:[*_`~>\-\s•]*)(?:\[\s*\d+\s*\]|\d+\s*[.|:\-)]|#\d+).*"""
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
    private val rejectedModelHeaders = java.util.concurrent.ConcurrentHashMap.newKeySet<String>()

    internal fun clearRuntimeCachesForTesting() {
        discoveredModelHeaders.clear()
        rejectedModelHeaders.clear()
    }

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

    /**
     * Merges user-supplied `__Secure-1PSID` / `__Secure-1PSIDTS` with any rotated cookies returned
     * via `Set-Cookie` headers from `GET https://gemini.google.com/app` (e.g. refreshed
     * `__Secure-1PSIDTS`, `__Secure-1PSIDCC`, `NID`, `AEC`) without duplicating keys.
     */
    internal fun mergeCookieHeaders(
        psid: String,
        psidts: String,
        setCookieHeaders: List<String> = emptyList()
    ): String {
        val cookies = LinkedHashMap<String, String>()
        val cleanPsid = sanitizeCookieToken(psid)
        val cleanPsidts = sanitizeCookieToken(psidts)
        if (cleanPsid.isNotEmpty()) {
            cookies["__Secure-1PSID"] = cleanPsid
        }
        if (cleanPsidts.isNotEmpty()) {
            cookies["__Secure-1PSIDTS"] = cleanPsidts
        }

        for (rawHeader in setCookieHeaders) {
            val pair = rawHeader.substringBefore(';').trim()
            val eqIndex = pair.indexOf('=')
            if (eqIndex <= 0) continue
            val name = pair.substring(0, eqIndex).trim()
            val value = sanitizeCookieToken(pair.substring(eqIndex + 1))
            if (name.isEmpty() || value.isEmpty() || value.equals("deleted", ignoreCase = true)) {
                continue
            }
            // Never overwrite a non-empty __Secure-1PSID with a malformed short value
            if (name.equals("__Secure-1PSID", ignoreCase = true) && cleanPsid.isNotEmpty() && value.length < 16) {
                continue
            }
            cookies[name] = value
        }

        return cookies.entries.joinToString("; ") { (k, v) -> "$k=$v" }
    }

    internal enum class FreqPayloadVariant(val label: String) {
        STANDARD_NULL_METADATA("standard_null_meta"),
        STANDARD_EMPTY_METADATA("standard_empty_meta"),
        EXTENDED_NULL_METADATA("extended_null_meta")
    }

    internal data class GeminiWebSessionTokens(
        val snlm0eToken: String,
        val blBuildLabel: String,
        val blWasExtractedFromHtml: Boolean = true,
        val fSid: String = "",
        val setCookieHeaders: List<String> = emptyList(),
        val finalAppUrl: String = GEMINI_APP_URL,
        val rawHtml: String = ""
    )

    internal class StreamGenerateHttpException(
        val statusCode: Int,
        val responseSnippet: String,
        message: String
    ) : IllegalStateException(message)

    internal data class StreamGenerateAttemptSpec(
        val attemptLabel: String,
        val outerFreqStr: String,
        val payloadVariant: FreqPayloadVariant,
        val modelHeaderVal: String?,
        val includeQueryParams: Boolean,
        val includeFSid: Boolean,
        val useExtendedPromptTuple: Boolean,
        val refreshSessionBeforeAttempt: Boolean = false
    )

    internal fun parseSessionTokensFromHtml(
        html: String,
        setCookieHeaders: List<String> = emptyList(),
        finalAppUrl: String = GEMINI_APP_URL
    ): GeminiWebSessionTokens {
        val snlm0e = SNLM0E_REGEX.find(html)?.groupValues?.getOrNull(1).orEmpty()
        if (snlm0e.isEmpty()) {
            AppDiagnosticLogger.error(
                "GeminiWeb",
                "SNlM0e tidak ditemukan pada HTML (${html.length} chars, finalUrl=$finalAppUrl)"
            )
            throw IllegalStateException(
                "Token sesi SNlM0e tidak ditemukan. Pastikan cookie __Secure-1PSID & __Secure-1PSIDTS masih aktif (belum kedaluwarsa)."
            )
        }
        val extractedCfb2h = CFB2H_REGEX.find(html)?.groupValues?.getOrNull(1)?.takeIf { it.isNotBlank() }
        val fSid = FDRFJE_REGEX.find(html)?.groupValues?.getOrNull(1).orEmpty()

        return GeminiWebSessionTokens(
            snlm0eToken = snlm0e,
            blBuildLabel = extractedCfb2h ?: DEFAULT_BL_BUILD_LABEL,
            blWasExtractedFromHtml = extractedCfb2h != null,
            fSid = fSid,
            setCookieHeaders = setCookieHeaders,
            finalAppUrl = finalAppUrl,
            rawHtml = html
        )
    }

    /**
     * Builds valid JSPB `f.req` payloads for `BardFrontendService/StreamGenerate`:
     * - `STANDARD_NULL_METADATA`:  `[null, "[[\"PROMPT\"],null,null]"]` (matches HanaokaYuzu/Gemini-API new chat)
     * - `STANDARD_EMPTY_METADATA`: `[null, "[[\"PROMPT\"],null,[\"\",\"\",\"\"]]"]`
     * - `EXTENDED_NULL_METADATA`:  `[null, "[[\"PROMPT\",0,null,null],null,null]"]`
     */
    internal fun buildStreamGenerateFreqPayload(
        combinedPrompt: String,
        variant: FreqPayloadVariant
    ): String {
        val innerPromptArray = JSONArray().apply {
            put(combinedPrompt)
            if (variant == FreqPayloadVariant.EXTENDED_NULL_METADATA) {
                put(0)
                put(JSONObject.NULL)
                put(JSONObject.NULL)
            }
        }
        val innerJsonStr = JSONArray().apply {
            put(innerPromptArray)
            put(JSONObject.NULL)
            when (variant) {
                FreqPayloadVariant.STANDARD_EMPTY_METADATA ->
                    put(JSONArray().put("").put("").put(""))
                FreqPayloadVariant.STANDARD_NULL_METADATA,
                FreqPayloadVariant.EXTENDED_NULL_METADATA ->
                    put(JSONObject.NULL)
            }
        }.toString()

        return JSONArray().apply {
            put(JSONObject.NULL)
            put(innerJsonStr)
        }.toString()
    }

    internal fun buildStreamGenerateFreqPayload(
        combinedPrompt: String,
        extendedPromptTuple: Boolean = false
    ): String = buildStreamGenerateFreqPayload(
        combinedPrompt = combinedPrompt,
        variant = if (extendedPromptTuple) {
            FreqPayloadVariant.EXTENDED_NULL_METADATA
        } else {
            FreqPayloadVariant.STANDARD_NULL_METADATA
        }
    )

    internal fun buildStreamGenerateEndpoint(
        blBuildLabel: String,
        fSid: String,
        reqId: Int,
        includeQueryParams: Boolean = true,
        includeFSid: Boolean = true,
        blWasExtractedFromHtml: Boolean = true
    ): String {
        if (!includeQueryParams) {
            return GEMINI_STREAM_GENERATE_URL
        }
        val params = mutableListOf<String>()
        if (blWasExtractedFromHtml && blBuildLabel.isNotBlank()) {
            params.add("bl=${URLEncoder.encode(blBuildLabel, "UTF-8")}")
        }
        if (includeFSid && fSid.isNotBlank()) {
            params.add("f.sid=${URLEncoder.encode(fSid, "UTF-8")}")
        }
        params.add("_reqid=$reqId")
        params.add("rt=c")
        return "$GEMINI_STREAM_GENERATE_URL?${params.joinToString("&")}"
    }

    /**
     * Supplements user-configured cookies with matching live Android WebView CookieManager cookies
     * (e.g. `__Secure-1PSIDTS`, `__Secure-1PSIDCC`, `NID`, `AEC`) ONLY when the WebView holds the
     * exact same `__Secure-1PSID` account cookie. Safely no-ops in non-WebView / JVM environments.
     */
    private fun readMatchingWebViewSetCookies(cleanPsid: String): List<String> {
        if (cleanPsid.isEmpty()) return emptyList()
        return runCatching {
            val cmClass = Class.forName("android.webkit.CookieManager")
            val getInstance = cmClass.getMethod("getInstance")
            val cm = getInstance.invoke(null) ?: return@runCatching emptyList()
            val getCookie = cmClass.getMethod("getCookie", String::class.java)
            val rawJar = (getCookie.invoke(cm, "https://gemini.google.com") as? String).orEmpty()
            if (rawJar.isBlank()) return@runCatching emptyList()

            val pairs = rawJar.split(";").map { it.trim() }.filter { it.contains("=") }
            val jarPsid = pairs
                .firstOrNull { it.substringBefore('=').trim().equals("__Secure-1PSID", ignoreCase = true) }
                ?.substringAfter('=')
                ?.let { sanitizeCookieToken(it) }
                .orEmpty()
            if (jarPsid != cleanPsid) {
                return@runCatching emptyList()
            }
            pairs
        }.getOrDefault(emptyList())
    }

    private fun fetchSessionTokensBlocking(psid: String, psidts: String): GeminiWebSessionTokens {
        val cleanPsid = sanitizeCookieToken(psid)
        val cleanPsidts = sanitizeCookieToken(psidts)
        if (cleanPsid.isEmpty()) {
            AppDiagnosticLogger.error("GeminiWeb", "Cookie __Secure-1PSID kosong saat memulai permintaan.")
            throw IllegalArgumentException(
                "Cookie __Secure-1PSID belum diisi. Gunakan tombol 'WebView Turbo', 'Browser HP', atau 'Smart Paste Cookie'."
            )
        }

        val webViewJarCookies = readMatchingWebViewSetCookies(cleanPsid)
        val initialCookieHeader = mergeCookieHeaders(cleanPsid, cleanPsidts, webViewJarCookies)
        val hasPsidtsInJar = initialCookieHeader.contains("__Secure-1PSIDTS=", ignoreCase = true)
        if (!hasPsidtsInJar) {
            AppDiagnosticLogger.warn(
                "GeminiWeb",
                "__Secure-1PSIDTS kosong (psid=${cleanPsid.length}ch, webViewJar=${webViewJarCookies.size} cookies). Server Google sering menolak StreamGenerate dengan HTTP 400 tanpa __Secure-1PSIDTS."
            )
        }

        val startMs = System.currentTimeMillis()
        val connection = (URL(GEMINI_APP_URL).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            instanceFollowRedirects = true
            connectTimeout = 20_000
            readTimeout = 25_000
            setRequestProperty("User-Agent", USER_AGENT)
            setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
            setRequestProperty("Accept-Language", "id-ID,id;q=0.9,en-US;q=0.8,en;q=0.7")
            setRequestProperty("Cookie", initialCookieHeader)
        }

        try {
            val code = connection.responseCode
            val elapsedMs = System.currentTimeMillis() - startMs
            val finalUrl = runCatching { connection.url.toString() }.getOrDefault(GEMINI_APP_URL)
            val html = readStream(connection, isError = code !in 200..299)
            if (code !in 200..299) {
                AppDiagnosticLogger.error(
                    "GeminiWeb",
                    "GET /app gagal HTTP $code (${elapsedMs}ms) finalUrl=$finalUrl"
                )
                throw IllegalStateException("HTTP $code saat menghubungi gemini.google.com/app")
            }

            val serverSetCookies = connection.headerFields
                .entries
                .filter { (key, _) -> key != null && key.equals("Set-Cookie", ignoreCase = true) }
                .flatMap { (_, values) -> values.orEmpty() }
                .filter { it.isNotBlank() }

            val combinedSetCookies = webViewJarCookies + serverSetCookies
            val cookieNames = combinedSetCookies.mapNotNull { h ->
                h.substringBefore(';').substringBefore('=').trim().takeIf { it.isNotEmpty() }
            }.distinct()

            val tokens = parseSessionTokensFromHtml(
                html = html,
                setCookieHeaders = combinedSetCookies,
                finalAppUrl = finalUrl
            )
            AppDiagnosticLogger.http(
                "GeminiWeb",
                "GET /app HTTP $code (${elapsedMs}ms) finalUrl=$finalUrl | " +
                    "SNlM0e=${tokens.snlm0eToken.length}ch | " +
                    "bl=${if (tokens.blWasExtractedFromHtml) tokens.blBuildLabel else "(tidak ditemukan)"} | " +
                    "f.sid=${if (tokens.fSid.isNotBlank()) "ada(${tokens.fSid.length}ch)" else "kosong"} | " +
                    "cookies=[${cookieNames.joinToString(",")}]"
            )
            return tokens
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
                val headerVal = """[1,null,null,null,"$hexId"]"""
                discoveredModelHeaders[normalizedSlug] = headerVal
                rejectedModelHeaders.remove(headerVal)
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
    internal fun resolveModelHeader(modelId: String): String? {
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

    private fun looksLikeGeminiRefusal(cleanedText: String): Boolean {
        val lower = cleanedText.lowercase()
        return lower.contains("tidak diprogram untuk itu") ||
            lower.contains("hanya menghasilkan teks") ||
            lower.contains("hanya ai berbasis teks") ||
            lower.contains("tidak bisa membantu anda untuk itu") ||
            lower.contains("not programmed") ||
            lower.contains("text-based ai")
    }

    private fun isPreferredTranslatedOutput(cleanedText: String): Boolean {
        if (cleanedText.isBlank() || looksLikeGeminiRefusal(cleanedText)) return false
        val hasNumberedLine = cleanedText.lineSequence().any { line ->
            NUMBERED_LINE_REGEX.matches(line.trim())
        }
        return hasNumberedLine || cleanedText.lines().size >= 3
    }

    /**
     * Executes `StreamGenerate` with an automatic multi-stage HTTP 400 / model-header fallback ladder:
     * 1. Bare `StreamGenerate` URL (HanaokaYuzu/Gemini-API style, no query params) +
     *    `STANDARD_NULL_METADATA` (`[[prompt], null, null]`) + live/mapped `x-goog-ext-525001261-jspb`.
     * 2. Bare `StreamGenerate` URL + `STANDARD_NULL_METADATA` + NO `x-goog-ext-525001261-jspb`
     *    (caches rejected model header on HTTP 400 so subsequent batches skip Attempt 1).
     * 3. Query-param `StreamGenerate` URL (`bl`/`f.sid`/`_reqid`/`rt=c`, omitting `bl` if not live-extracted) +
     *    `STANDARD_EMPTY_METADATA` (`[[prompt], null, ["","",""]]`) + NO model header.
     * 4. Bare `StreamGenerate` URL + `EXTENDED_NULL_METADATA` (`[[prompt, 0, null, null], null, null]`) +
     *    NO model header.
     * 5. If still failing with HTTP 400/401/403, refreshes session tokens (`SNlM0e`/`cfb2h`/`FdrFJe`)
     *    once and retries bare URL + `STANDARD_NULL_METADATA` + NO model header.
     */
    internal fun executeWithHttp400Fallback(
        initialTokens: GeminiWebSessionTokens,
        combinedPrompt: String,
        initialModelHeaderVal: String?,
        refreshTokens: () -> GeminiWebSessionTokens,
        executor: (tokens: GeminiWebSessionTokens, spec: StreamGenerateAttemptSpec) -> String
    ): String {
        val standardNullFreq = buildStreamGenerateFreqPayload(
            combinedPrompt = combinedPrompt,
            variant = FreqPayloadVariant.STANDARD_NULL_METADATA
        )
        val standardEmptyFreq = buildStreamGenerateFreqPayload(
            combinedPrompt = combinedPrompt,
            variant = FreqPayloadVariant.STANDARD_EMPTY_METADATA
        )
        val extendedNullFreq = buildStreamGenerateFreqPayload(
            combinedPrompt = combinedPrompt,
            variant = FreqPayloadVariant.EXTENDED_NULL_METADATA
        )

        val effectiveModelHeader = initialModelHeaderVal?.takeIf { it !in rejectedModelHeaders }
        var activeTokens = initialTokens
        var lastError: Throwable? = null
        var bestNonBlankText = ""

        // Attempt 1: Bare URL + STANDARD_NULL_METADATA + model routing header (if available & not rejected)
        if (effectiveModelHeader != null) {
            val spec1 = StreamGenerateAttemptSpec(
                attemptLabel = "A1_bare_model_header",
                outerFreqStr = standardNullFreq,
                payloadVariant = FreqPayloadVariant.STANDARD_NULL_METADATA,
                modelHeaderVal = effectiveModelHeader,
                includeQueryParams = false,
                includeFSid = false,
                useExtendedPromptTuple = false
            )
            val res1 = runCatching { executor(activeTokens, spec1) }
            if (res1.isSuccess) {
                val cleaned1 = OpenAiCompatibleClient.cleanAiModelOutput(res1.getOrDefault(""))
                if (cleaned1.isNotBlank()) bestNonBlankText = cleaned1
                if (isPreferredTranslatedOutput(cleaned1)) {
                    return cleaned1
                }
            } else {
                val err = res1.exceptionOrNull()
                lastError = err
                if (err is StreamGenerateHttpException && err.statusCode == 400) {
                    rejectedModelHeaders.add(effectiveModelHeader)
                    AppDiagnosticLogger.warn(
                        "GeminiWeb",
                        "Header model ditolak HTTP 400; ditandai nonaktif sementara & fallback ke model default."
                    )
                }
            }
        }

        // Attempt 2: Bare URL + STANDARD_NULL_METADATA + default Gemini Web model (NO model header)
        val spec2 = StreamGenerateAttemptSpec(
            attemptLabel = "A2_bare_default_null_meta",
            outerFreqStr = standardNullFreq,
            payloadVariant = FreqPayloadVariant.STANDARD_NULL_METADATA,
            modelHeaderVal = null,
            includeQueryParams = false,
            includeFSid = false,
            useExtendedPromptTuple = false
        )
        val res2 = runCatching { executor(activeTokens, spec2) }
        if (res2.isSuccess) {
            val cleaned2 = OpenAiCompatibleClient.cleanAiModelOutput(res2.getOrDefault(""))
            if (cleaned2.isNotBlank()) {
                if (isPreferredTranslatedOutput(cleaned2) || !looksLikeGeminiRefusal(cleaned2)) {
                    return cleaned2
                }
                if (bestNonBlankText.isBlank()) bestNonBlankText = cleaned2
            }
        } else {
            lastError = res2.exceptionOrNull()
        }

        // Attempt 3: Query-param URL (bl/f.sid/_reqid/rt=c) + STANDARD_EMPTY_METADATA + NO model header
        if (bestNonBlankText.isBlank() || looksLikeGeminiRefusal(bestNonBlankText)) {
            val spec3 = StreamGenerateAttemptSpec(
                attemptLabel = "A3_query_empty_meta",
                outerFreqStr = standardEmptyFreq,
                payloadVariant = FreqPayloadVariant.STANDARD_EMPTY_METADATA,
                modelHeaderVal = null,
                includeQueryParams = true,
                includeFSid = activeTokens.fSid.isNotBlank(),
                useExtendedPromptTuple = false
            )
            val res3 = runCatching { executor(activeTokens, spec3) }
            if (res3.isSuccess) {
                val cleaned3 = OpenAiCompatibleClient.cleanAiModelOutput(res3.getOrDefault(""))
                if (cleaned3.isNotBlank()) {
                    if (!looksLikeGeminiRefusal(cleaned3)) {
                        return cleaned3
                    }
                    if (bestNonBlankText.isBlank()) bestNonBlankText = cleaned3
                }
            } else {
                lastError = res3.exceptionOrNull()
            }
        }

        // Attempt 4: Bare URL + EXTENDED_NULL_METADATA [[prompt, 0, null, null], null, null] + NO model header
        if (bestNonBlankText.isBlank() || looksLikeGeminiRefusal(bestNonBlankText)) {
            val spec4 = StreamGenerateAttemptSpec(
                attemptLabel = "A4_bare_extended_tuple",
                outerFreqStr = extendedNullFreq,
                payloadVariant = FreqPayloadVariant.EXTENDED_NULL_METADATA,
                modelHeaderVal = null,
                includeQueryParams = false,
                includeFSid = false,
                useExtendedPromptTuple = true
            )
            val res4 = runCatching { executor(activeTokens, spec4) }
            if (res4.isSuccess) {
                val cleaned4 = OpenAiCompatibleClient.cleanAiModelOutput(res4.getOrDefault(""))
                if (cleaned4.isNotBlank()) {
                    if (!looksLikeGeminiRefusal(cleaned4)) {
                        return cleaned4
                    }
                    if (bestNonBlankText.isBlank()) bestNonBlankText = cleaned4
                }
            } else {
                lastError = res4.exceptionOrNull()
            }
        }

        // Attempt 5: Fresh session token refresh (handles mid-batch SNlM0e / __Secure-1PSIDTS rotation)
        val isHttpAuthOrBadRequest = (lastError as? StreamGenerateHttpException)?.statusCode in listOf(400, 401, 403)
        if (bestNonBlankText.isBlank() && isHttpAuthOrBadRequest) {
            AppDiagnosticLogger.info("GeminiWeb", "Mencoba refresh sesi GET /app untuk memperbarui SNlM0e...")
            val refreshed = runCatching { refreshTokens() }.getOrNull()
            if (refreshed != null) {
                activeTokens = refreshed
                val spec5 = StreamGenerateAttemptSpec(
                    attemptLabel = "A5_refreshed_session_bare",
                    outerFreqStr = standardNullFreq,
                    payloadVariant = FreqPayloadVariant.STANDARD_NULL_METADATA,
                    modelHeaderVal = null,
                    includeQueryParams = false,
                    includeFSid = false,
                    useExtendedPromptTuple = false,
                    refreshSessionBeforeAttempt = true
                )
                val res5 = runCatching { executor(activeTokens, spec5) }
                if (res5.isSuccess) {
                    val cleaned5 = OpenAiCompatibleClient.cleanAiModelOutput(res5.getOrDefault(""))
                    if (cleaned5.isNotBlank()) {
                        return cleaned5
                    }
                } else {
                    lastError = res5.exceptionOrNull()
                }
            }
        }

        if (bestNonBlankText.isNotBlank()) {
            return bestNonBlankText
        }

        val finalErr = lastError
        if (finalErr is StreamGenerateHttpException && finalErr.statusCode == 400) {
            val detailSuffix = finalErr.responseSnippet
                .takeIf { it.isNotBlank() }
                ?.let { " Detail: $it" }
                .orEmpty()
            throw IllegalStateException(
                "HTTP 400 saat memanggil StreamGenerate Gemini Web (setelah 5 tahap fallback otomatis). " +
                    "Pastikan cookie __Secure-1PSID & __Secure-1PSIDTS keduanya terisi dan aktif lewat WebView Turbo, atau cek 'Log HTTP'.$detailSuffix"
            )
        }
        if (finalErr != null) {
            throw finalErr
        }
        throw IllegalStateException(
            "Respons Gemini Web kosong atau cookie membutuhkan penyegaran (__Secure-1PSIDTS)."
        )
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
            // Populate live model routing hashes directly from the current Gemini Web page
            extractLiveModelsFromHtml(tokens.rawHtml)

            val combinedPrompt = buildString {
                appendLine(systemPrompt.trim())
                appendLine()
                appendLine("--- DAFTAR TEKS DIALOG ---")
                append(numberedUserLines.trim())
            }

            executeWithHttp400Fallback(
                initialTokens = tokens,
                combinedPrompt = combinedPrompt,
                initialModelHeaderVal = resolveModelHeader(modelId),
                refreshTokens = { fetchSessionTokensBlocking(psid, psidts) },
                executor = { activeTokens, spec ->
                    executeStreamGenerateRequest(
                        psid = psid,
                        psidts = psidts,
                        tokens = activeTokens,
                        spec = spec
                    )
                }
            )
        }
    }

    private fun sanitizeErrorSnippet(rawError: String): String {
        if (rawError.isBlank()) return ""
        return rawError
            .replace(Regex("<[^>]+>"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
            .take(200)
    }

    private fun executeStreamGenerateRequest(
        psid: String,
        psidts: String,
        tokens: GeminiWebSessionTokens,
        spec: StreamGenerateAttemptSpec
    ): String {
        val reqId = Random.nextInt(100_000, 999_999)
        val endpoint = buildStreamGenerateEndpoint(
            blBuildLabel = tokens.blBuildLabel,
            fSid = tokens.fSid,
            reqId = reqId,
            includeQueryParams = spec.includeQueryParams,
            includeFSid = spec.includeFSid,
            blWasExtractedFromHtml = tokens.blWasExtractedFromHtml
        )

        val postFormBody = "at=" + URLEncoder.encode(tokens.snlm0eToken, "UTF-8") +
            "&f.req=" + URLEncoder.encode(spec.outerFreqStr, "UTF-8")

        val cookieHeader = mergeCookieHeaders(psid, psidts, tokens.setCookieHeaders)
        val hasPsidts = cookieHeader.contains("__Secure-1PSIDTS=", ignoreCase = true)
        val startMs = System.currentTimeMillis()
        val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            connectTimeout = 25_000
            readTimeout = 95_000
            setRequestProperty("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8")
            setRequestProperty("Accept", "*/*")
            setRequestProperty("Accept-Language", "id-ID,id;q=0.9,en-US;q=0.8,en;q=0.7")
            setRequestProperty("User-Agent", USER_AGENT)
            setRequestProperty("Origin", "https://gemini.google.com")
            setRequestProperty("Referer", "https://gemini.google.com/")
            setRequestProperty("X-Same-Domain", "1")
            setRequestProperty("Cookie", cookieHeader)
            if (spec.modelHeaderVal != null) {
                setRequestProperty("x-goog-ext-525001261-jspb", spec.modelHeaderVal)
            }
        }

        try {
            OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use { writer ->
                writer.write(postFormBody)
                writer.flush()
            }

            val code = connection.responseCode
            val elapsedMs = System.currentTimeMillis() - startMs
            val rawResponse = readStream(connection, isError = code !in 200..299)
            if (code !in 200..299) {
                val snippet = sanitizeErrorSnippet(rawResponse)
                AppDiagnosticLogger.error(
                    "GeminiWeb",
                    "POST StreamGenerate [${spec.attemptLabel}] HTTP $code (${elapsedMs}ms) | " +
                        "query=${spec.includeQueryParams}, fSid=${spec.includeFSid}, " +
                        "variant=${spec.payloadVariant.label}, modelHdr=${spec.modelHeaderVal != null}, " +
                        "hasPsidts=$hasPsidts, bodyLen=${postFormBody.length} | resp=$snippet"
                )
                val suffix = if (snippet.isNotBlank()) " ($snippet)" else ""
                throw StreamGenerateHttpException(
                    statusCode = code,
                    responseSnippet = snippet,
                    message = "HTTP $code saat memanggil StreamGenerate Gemini Web$suffix"
                )
            }

            val parsedText = parseBardStreamResponse(rawResponse)
            AppDiagnosticLogger.http(
                "GeminiWeb",
                "POST StreamGenerate [${spec.attemptLabel}] HTTP $code (${elapsedMs}ms) | " +
                    "variant=${spec.payloadVariant.label}, modelHdr=${spec.modelHeaderVal != null} | " +
                    "parsedChars=${parsedText.length}"
            )
            return parsedText
        } finally {
            connection.disconnect()
        }
    }

    /**
     * Parses the multi-line `)]}'` response from `BardFrontendService/StreamGenerate`
     * to extract the candidate response text across all `wrb.fr` frames and nested candidate structures.
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
                    // 1. Standard candidate list at payloadJson[4]
                    val candidates = payloadJson.optJSONArray(4)
                    if (candidates != null && candidates.length() > 0) {
                        val firstCandidate = candidates.optJSONArray(0)
                        if (firstCandidate != null) {
                            val textParts = firstCandidate.optJSONArray(1)
                            if (textParts != null && textParts.length() > 0) {
                                val combinedParts = buildString {
                                    for (partIdx in 0 until textParts.length()) {
                                        val partStr = textParts.optString(partIdx, "")
                                        if (partStr.isNotEmpty()) {
                                            append(partStr)
                                        }
                                    }
                                }.trim()
                                if (combinedParts.isNotBlank()) {
                                    bestCandidate = combinedParts
                                }
                            } else {
                                val directStr = firstCandidate.optString(1, "").trim()
                                if (directStr.isNotBlank()) {
                                    bestCandidate = directStr
                                }
                            }
                        }
                    }

                    // 2. Fallback: recursively locate candidate text following an "rc_" response candidate ID
                    if (bestCandidate.isBlank()) {
                        val fallbackText = extractCandidateAfterRcId(payloadJson)
                        if (fallbackText.isNotBlank()) {
                            bestCandidate = fallbackText
                        }
                    }
                }
            }
        }
        return bestCandidate.trim()
    }

    private fun extractCandidateAfterRcId(node: Any?): String {
        if (node !is JSONArray) return ""
        if (node.length() >= 2) {
            val firstStr = node.optString(0, "")
            if (firstStr.startsWith("rc_") || firstStr.startsWith("c_")) {
                val secondArr = node.optJSONArray(1)
                if (secondArr != null && secondArr.length() > 0) {
                    val joined = buildString {
                        for (i in 0 until secondArr.length()) {
                            val s = secondArr.optString(i, "")
                            if (s.isNotEmpty()) append(s)
                        }
                    }.trim()
                    if (joined.isNotBlank()) return joined
                }
                val secondStr = node.optString(1, "").trim()
                if (secondStr.isNotBlank() && !secondStr.startsWith("[")) {
                    return secondStr
                }
            }
        }
        var best = ""
        for (i in 0 until node.length()) {
            val child = node.optJSONArray(i) ?: continue
            val found = extractCandidateAfterRcId(child)
            if (found.length > best.length) {
                best = found
            }
        }
        return best
    }

    private fun readStream(connection: HttpURLConnection, isError: Boolean): String {
        val stream = if (isError) connection.errorStream else connection.inputStream
        if (stream == null) return ""
        return BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).use { it.readText() }
    }
}
