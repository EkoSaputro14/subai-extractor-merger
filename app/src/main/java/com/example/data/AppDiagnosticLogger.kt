package com.example.data

import com.example.BuildConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class DiagnosticLogLevel(val label: String) {
    INFO("INFO"),
    HTTP("HTTP"),
    WARN("WARN"),
    ERROR("ERROR")
}

data class DiagnosticLogEntry(
    val id: Long,
    val timestampText: String,
    val level: DiagnosticLogLevel,
    val tag: String,
    val message: String
)

/**
 * In-memory ring-buffer diagnostic logger for HTTP/API requests, cookie session handshakes,
 * fallback attempts, and batch translation events. Automatically redacts sensitive tokens.
 */
object AppDiagnosticLogger {

    private const val MAX_ENTRIES = 400
    private val lock = Any()
    private val buffer = ArrayDeque<DiagnosticLogEntry>(MAX_ENTRIES)
    private var nextId = 1L

    private val _logs = MutableStateFlow<List<DiagnosticLogEntry>>(emptyList())
    val logs: StateFlow<List<DiagnosticLogEntry>> = _logs.asStateFlow()

    private val SECRET_VALUE_REGEX = Regex(
        """(__Secure-1PSID(?:TS|CC)?|SNlM0e|Authorization|Bearer|api[_-]?key|at)\s*[=:]\s*([^\s;&,"']{8,})""",
        RegexOption.IGNORE_CASE
    )

    fun info(tag: String, message: String) = append(DiagnosticLogLevel.INFO, tag, message)

    fun http(tag: String, message: String) = append(DiagnosticLogLevel.HTTP, tag, message)

    fun warn(tag: String, message: String) = append(DiagnosticLogLevel.WARN, tag, message)

    fun error(tag: String, message: String) = append(DiagnosticLogLevel.ERROR, tag, message)

    fun redactSecrets(raw: String): String {
        if (raw.isBlank()) return raw
        return SECRET_VALUE_REGEX.replace(raw) { match ->
            val key = match.groupValues[1]
            val secret = match.groupValues[2]
            val prefix = secret.take(4)
            "$key=$prefix***(${secret.length}ch)"
        }
    }

    private fun append(level: DiagnosticLogLevel, tag: String, message: String) {
        val ts = SimpleDateFormat("HH:mm:ss.SSS", Locale.US).format(Date())
        val safeMessage = redactSecrets(message.trim())
        val snapshot: List<DiagnosticLogEntry>
        synchronized(lock) {
            if (buffer.size >= MAX_ENTRIES) {
                buffer.removeFirst()
            }
            buffer.addLast(
                DiagnosticLogEntry(
                    id = nextId++,
                    timestampText = ts,
                    level = level,
                    tag = tag,
                    message = safeMessage
                )
            )
            snapshot = buffer.toList()
        }
        _logs.value = snapshot
    }

    fun clear() {
        synchronized(lock) {
            buffer.clear()
        }
        _logs.value = emptyList()
    }

    fun exportAsPlainText(): String {
        val entries = _logs.value
        return buildString {
            appendLine("=== SubAI Extractor & Merger Diagnostic Log ===")
            appendLine("App Version: v${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE})")
            appendLine("Total Entries: ${entries.size}")
            appendLine("---------------------------------------------")
            if (entries.isEmpty()) {
                appendLine("(Belum ada log tercatat pada sesi ini)")
            } else {
                entries.forEach { entry ->
                    appendLine("[${entry.timestampText}] [${entry.level.label}] [${entry.tag}] ${entry.message}")
                }
            }
        }
    }
}
