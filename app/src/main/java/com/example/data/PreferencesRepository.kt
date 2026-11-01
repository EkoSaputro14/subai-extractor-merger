package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.model.AiProviderPreset
import com.example.model.AiProviderSettings
import com.example.model.BatchAttemptRecord
import com.example.model.BatchFailureType
import com.example.model.BatchJobItem
import com.example.model.BatchJobStatus
import com.example.model.BatchQueueState
import com.example.model.ColorPreset
import com.example.model.FileConflictPolicy
import com.example.model.GlossaryRule
import com.example.model.SubtitleFormat
import com.example.model.ThemeMode
import com.example.model.ThemeSettings
import org.json.JSONArray
import org.json.JSONObject

class PreferencesRepository(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun loadThemeSettings(): ThemeSettings {
        val rawJson = prefs.getString(KEY_THEME_SETTINGS, null) ?: return ThemeSettings()
        return try {
            val obj = JSONObject(rawJson)
            val themeMode = runCatching {
                ThemeMode.valueOf(obj.optString("themeMode", ThemeMode.DARK.name))
            }.getOrDefault(ThemeMode.DARK)
            val colorPreset = runCatching {
                ColorPreset.valueOf(obj.optString("colorPreset", ColorPreset.OCEAN_BLUE.name))
            }.getOrDefault(ColorPreset.OCEAN_BLUE)
            ThemeSettings(
                themeMode = themeMode,
                colorPreset = colorPreset,
                useDynamicColor = obj.optBoolean("useDynamicColor", false),
                pureBlackAmoled = obj.optBoolean("pureBlackAmoled", false),
                editorFontSizeSp = obj.optInt("editorFontSizeSp", 13).coerceIn(11, 17),
                useMonospaceFont = obj.optBoolean("useMonospaceFont", true)
            )
        } catch (_: Exception) {
            ThemeSettings()
        }
    }

    fun saveThemeSettings(settings: ThemeSettings) {
        val obj = JSONObject().apply {
            put("themeMode", settings.themeMode.name)
            put("colorPreset", settings.colorPreset.name)
            put("useDynamicColor", settings.useDynamicColor)
            put("pureBlackAmoled", settings.pureBlackAmoled)
            put("editorFontSizeSp", settings.editorFontSizeSp)
            put("useMonospaceFont", settings.useMonospaceFont)
        }
        prefs.edit().putString(KEY_THEME_SETTINGS, obj.toString()).apply()
    }

    fun loadAiProviderSettings(): AiProviderSettings {
        val rawJson = prefs.getString(KEY_AI_PROVIDER_SETTINGS, null) ?: return AiProviderSettings()
        return try {
            val obj = JSONObject(rawJson)
            val preset = runCatching {
                AiProviderPreset.valueOf(obj.optString("preset", AiProviderPreset.GEMINI_WEB_COOKIE.name))
            }.getOrDefault(AiProviderPreset.GEMINI_WEB_COOKIE)
            val fetchedArr = obj.optJSONArray("fetchedModels")
            val fetchedList = mutableListOf<String>()
            if (fetchedArr != null) {
                for (i in 0 until fetchedArr.length()) {
                    val m = fetchedArr.optString(i, "").trim()
                    if (m.isNotEmpty()) fetchedList.add(m)
                }
            }
            AiProviderSettings(
                preset = preset,
                baseUrl = obj.optString("baseUrl", preset.defaultBaseUrl).ifBlank { preset.defaultBaseUrl },
                apiKey = obj.optString("apiKey", ""),
                geminiSecure1Psid = obj.optString("geminiSecure1Psid", ""),
                geminiSecure1Psidts = obj.optString("geminiSecure1Psidts", ""),
                combineSystemIntoUserPrompt = obj.optBoolean("combineSystemIntoUserPrompt", true),
                selectedModel = obj.optString("selectedModel", preset.defaultModel).ifBlank { preset.defaultModel },
                fetchedModels = fetchedList,
                temperature = obj.optDouble("temperature", 0.3).toFloat().coerceIn(0f, 1.5f)
            )
        } catch (_: Exception) {
            AiProviderSettings()
        }
    }

    fun saveAiProviderSettings(settings: AiProviderSettings) {
        val fetchedArr = JSONArray()
        settings.fetchedModels.forEach { fetchedArr.put(it) }
        val obj = JSONObject().apply {
            put("preset", settings.preset.name)
            put("baseUrl", settings.baseUrl)
            put("apiKey", settings.apiKey)
            put("geminiSecure1Psid", settings.geminiSecure1Psid)
            put("geminiSecure1Psidts", settings.geminiSecure1Psidts)
            put("combineSystemIntoUserPrompt", settings.combineSystemIntoUserPrompt)
            put("selectedModel", settings.selectedModel)
            put("fetchedModels", fetchedArr)
            put("temperature", settings.temperature.toDouble())
        }
        prefs.edit().putString(KEY_AI_PROVIDER_SETTINGS, obj.toString()).apply()
    }

    fun loadGlossaryRules(): List<GlossaryRule> {
        val rawJson = prefs.getString(KEY_GLOSSARY_RULES, null)
            ?: return defaultGlossaryRules().also { saveGlossaryRules(it) }
        return try {
            val array = JSONArray(rawJson)
            val list = mutableListOf<GlossaryRule>()
            for (i in 0 until array.length()) {
                val item = array.getJSONObject(i)
                val source = item.optString("sourceTerm", "").trim()
                val target = item.optString("targetTerm", "").trim()
                if (source.isNotEmpty()) {
                    list.add(
                        GlossaryRule(
                            id = item.optString("id", "rule_$i"),
                            sourceTerm = source,
                            targetTerm = target,
                            isCaseSensitive = item.optBoolean("isCaseSensitive", false),
                            wholeWord = item.optBoolean("wholeWord", true),
                            isEnabled = item.optBoolean("isEnabled", true)
                        )
                    )
                }
            }
            list
        } catch (_: Exception) {
            defaultGlossaryRules()
        }
    }

    fun saveGlossaryRules(rules: List<GlossaryRule>) {
        val array = JSONArray()
        rules.forEach { rule ->
            val obj = JSONObject().apply {
                put("id", rule.id)
                put("sourceTerm", rule.sourceTerm)
                put("targetTerm", rule.targetTerm)
                put("isCaseSensitive", rule.isCaseSensitive)
                put("wholeWord", rule.wholeWord)
                put("isEnabled", rule.isEnabled)
            }
            array.put(obj)
        }
        prefs.edit().putString(KEY_GLOSSARY_RULES, array.toString()).apply()
    }

    fun saveBatchQueueState(state: BatchQueueState) {
        runCatching {
            val jobsArr = JSONArray()
            state.jobs.forEach { job ->
                val validCuesObj = JSONObject()
                job.validTranslatedCues.forEach { (k, v) -> validCuesObj.put(k.toString(), v) }

                val historyArr = JSONArray()
                job.attemptHistory.forEach { rec ->
                    historyArr.put(
                        JSONObject().apply {
                            put("attemptNumber", rec.attemptNumber)
                            put("timestampMs", rec.timestampMs)
                            put("passed", rec.passed)
                            put("failureType", rec.failureType?.name ?: "")
                            put("reason", rec.reason)
                            put("translatedCueCount", rec.translatedCueCount)
                            put("totalSourceCues", rec.totalSourceCues)
                        }
                    )
                }

                val jobObj = JSONObject().apply {
                    put("jobId", job.jobId)
                    put("batchId", job.batchId)
                    put("fileName", job.fileName)
                    put("sourceUriString", job.sourceUriString)
                    put("rawSourceContent", job.rawSourceContent)
                    put("detectedFormat", job.detectedFormat.name)
                    put("totalCues", job.totalCues)
                    put("queueOrder", job.queueOrder)
                    put("status", job.status.name)
                    put("attemptCount", job.attemptCount)
                    put("maxRetries", job.maxRetries)
                    put("validTranslatedCues", validCuesObj)
                    put("attemptHistory", historyArr)
                    put("outputFilePath", job.outputFilePath ?: "")
                    put("outputFileName", job.outputFileName ?: "")
                    put("lastFailureReason", job.lastFailureReason ?: "")
                    put("lastFailureType", job.lastFailureType?.name ?: "")
                    put("startedAtMs", job.startedAtMs ?: -1L)
                    put("completedAtMs", job.completedAtMs ?: -1L)
                    put("translatedCuesProgress", job.translatedCuesProgress)
                }
                jobsArr.put(jobObj)
            }

            val root = JSONObject().apply {
                put("batchId", state.batchId)
                put("outputFolderUriOrPath", state.outputFolderUriOrPath)
                put("outputFolderDisplayName", state.outputFolderDisplayName)
                put("conflictPolicy", state.conflictPolicy.name)
                put("maxRetries", state.maxRetries)
                put("notificationsEnabled", state.notificationsEnabled)
                put("isCancelled", state.isCancelled)
                put("batchStartedAtMs", state.batchStartedAtMs ?: -1L)
                put("batchFinishedAtMs", state.batchFinishedAtMs ?: -1L)
                put("lastNotificationTitle", state.lastNotificationTitle ?: "")
                put("lastNotificationMessage", state.lastNotificationMessage ?: "")
                put("jobs", jobsArr)
            }
            prefs.edit().putString(KEY_BATCH_QUEUE_STATE, root.toString()).apply()
        }
    }

    fun loadBatchQueueState(defaultOutputFolder: String): BatchQueueState? {
        val rawJson = prefs.getString(KEY_BATCH_QUEUE_STATE, null) ?: return null
        return try {
            val root = JSONObject(rawJson)
            val jobsArr = root.optJSONArray("jobs") ?: JSONArray()
            val jobs = mutableListOf<BatchJobItem>()
            for (i in 0 until jobsArr.length()) {
                val jobObj = jobsArr.getJSONObject(i)
                val validCuesMap = mutableMapOf<Int, String>()
                val validCuesObj = jobObj.optJSONObject("validTranslatedCues")
                if (validCuesObj != null) {
                    val keys = validCuesObj.keys()
                    while (keys.hasNext()) {
                        val k = keys.next()
                        val id = k.toIntOrNull()
                        if (id != null) {
                            validCuesMap[id] = validCuesObj.optString(k, "")
                        }
                    }
                }

                val historyList = mutableListOf<BatchAttemptRecord>()
                val historyArr = jobObj.optJSONArray("attemptHistory")
                if (historyArr != null) {
                    for (j in 0 until historyArr.length()) {
                        val recObj = historyArr.getJSONObject(j)
                        val failTypeStr = recObj.optString("failureType", "")
                        val failType = runCatching { BatchFailureType.valueOf(failTypeStr) }.getOrNull()
                        historyList.add(
                            BatchAttemptRecord(
                                attemptNumber = recObj.optInt("attemptNumber", j + 1),
                                timestampMs = recObj.optLong("timestampMs", 0L),
                                passed = recObj.optBoolean("passed", false),
                                failureType = failType,
                                reason = recObj.optString("reason", ""),
                                translatedCueCount = recObj.optInt("translatedCueCount", 0),
                                totalSourceCues = recObj.optInt("totalSourceCues", 0)
                            )
                        )
                    }
                }

                val status = runCatching {
                    BatchJobStatus.valueOf(jobObj.optString("status", BatchJobStatus.QUEUED.name))
                }.getOrDefault(BatchJobStatus.QUEUED)

                val format = runCatching {
                    SubtitleFormat.valueOf(jobObj.optString("detectedFormat", SubtitleFormat.SRT.name))
                }.getOrDefault(SubtitleFormat.SRT)

                val lastFailTypeStr = jobObj.optString("lastFailureType", "")
                val lastFailType = runCatching { BatchFailureType.valueOf(lastFailTypeStr) }.getOrNull()
                val startedAt = jobObj.optLong("startedAtMs", -1L).takeIf { it > 0L }
                val completedAt = jobObj.optLong("completedAtMs", -1L).takeIf { it > 0L }

                jobs.add(
                    BatchJobItem(
                        jobId = jobObj.optString("jobId", "job_$i"),
                        batchId = jobObj.optString("batchId", "batch_default"),
                        fileName = jobObj.optString("fileName", "subtitle_$i.srt"),
                        sourceUriString = jobObj.optString("sourceUriString", ""),
                        rawSourceContent = jobObj.optString("rawSourceContent", ""),
                        detectedFormat = format,
                        totalCues = jobObj.optInt("totalCues", 0),
                        queueOrder = jobObj.optInt("queueOrder", i + 1),
                        status = status,
                        attemptCount = jobObj.optInt("attemptCount", 0),
                        maxRetries = jobObj.optInt("maxRetries", 5),
                        validTranslatedCues = validCuesMap,
                        attemptHistory = historyList,
                        outputFilePath = jobObj.optString("outputFilePath", "").ifBlank { null },
                        outputFileName = jobObj.optString("outputFileName", "").ifBlank { null },
                        lastFailureReason = jobObj.optString("lastFailureReason", "").ifBlank { null },
                        lastFailureType = lastFailType,
                        startedAtMs = startedAt,
                        completedAtMs = completedAt,
                        translatedCuesProgress = jobObj.optInt("translatedCuesProgress", 0)
                    )
                )
            }

            val conflictPolicy = runCatching {
                FileConflictPolicy.valueOf(
                    root.optString("conflictPolicy", FileConflictPolicy.RENAME_WITH_SUFFIX.name)
                )
            }.getOrDefault(FileConflictPolicy.RENAME_WITH_SUFFIX)

            val savedFolder = root.optString("outputFolderUriOrPath", "").ifBlank { defaultOutputFolder }
            val savedDisplay = root.optString(
                "outputFolderDisplayName",
                "Translated_Subtitles ($savedFolder)"
            )

            BatchQueueState(
                batchId = root.optString("batchId", "batch_default"),
                jobs = jobs,
                isRunning = false,
                isPaused = false,
                isCancelled = root.optBoolean("isCancelled", false),
                outputFolderUriOrPath = savedFolder,
                outputFolderDisplayName = savedDisplay,
                conflictPolicy = conflictPolicy,
                maxRetries = root.optInt("maxRetries", 5).coerceIn(1, 10),
                notificationsEnabled = root.optBoolean("notificationsEnabled", true),
                batchStartedAtMs = root.optLong("batchStartedAtMs", -1L).takeIf { it > 0L },
                batchFinishedAtMs = root.optLong("batchFinishedAtMs", -1L).takeIf { it > 0L },
                lastNotificationTitle = root.optString("lastNotificationTitle", "").ifBlank { null },
                lastNotificationMessage = root.optString("lastNotificationMessage", "").ifBlank { null }
            )
        } catch (_: Exception) {
            null
        }
    }

    companion object {
        private const val PREFS_NAME = "subai_extractor_prefs"
        private const val KEY_THEME_SETTINGS = "theme_settings_json"
        private const val KEY_GLOSSARY_RULES = "glossary_rules_json"
        private const val KEY_AI_PROVIDER_SETTINGS = "ai_provider_settings_json"
        private const val KEY_BATCH_QUEUE_STATE = "batch_queue_state_json"

        fun defaultGlossaryRules(): List<GlossaryRule> = listOf(
            GlossaryRule(
                id = "default_1",
                sourceTerm = "Shibuya Ward",
                targetTerm = "Distrik Shibuya",
                isCaseSensitive = false,
                wholeWord = true,
                isEnabled = true
            ),
            GlossaryRule(
                id = "default_2",
                sourceTerm = "Onii-chan",
                targetTerm = "Kakak",
                isCaseSensitive = false,
                wholeWord = true,
                isEnabled = true
            ),
            GlossaryRule(
                id = "default_3",
                sourceTerm = "Domain Expansion",
                targetTerm = "Perluasan Wilayah",
                isCaseSensitive = false,
                wholeWord = true,
                isEnabled = true
            ),
            GlossaryRule(
                id = "default_4",
                sourceTerm = "Cursed Energy",
                targetTerm = "Energi Kutukan",
                isCaseSensitive = false,
                wholeWord = true,
                isEnabled = true
            ),
            GlossaryRule(
                id = "default_5",
                sourceTerm = "Sorcerer",
                targetTerm = "Penyihir Jujutsu",
                isCaseSensitive = false,
                wholeWord = true,
                isEnabled = true
            )
        )
    }
}
