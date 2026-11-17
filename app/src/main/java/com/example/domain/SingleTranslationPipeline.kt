package com.example.domain

import com.example.data.GeminiWebCookieClient
import com.example.data.OpenAiCompatibleClient
import com.example.model.AiProviderPreset
import com.example.model.AiProviderSettings
import com.example.model.BatchSizeOption
import com.example.model.BatchValidationResult
import com.example.model.BilingualOrder
import com.example.model.GlossaryRule
import com.example.model.IndexFormatMode
import com.example.model.LineSeparatorMode
import com.example.model.SubtitleCue
import com.example.model.SubtitleDocument
import com.example.model.TargetLanguageOption
import com.example.model.TranslationStylePreset

/**
 * Unified, single source-of-truth configuration for both Single Translation and Batch Translation.
 * Guarantees that Batch Queue uses the exact same AI provider, model, prompt, chunking,
 * glossary, postprocessing, subtitle formatting, and validation rules as Single Translation.
 */
data class SingleTranslationConfig(
    val aiSettings: AiProviderSettings = AiProviderSettings(),
    val sourceLanguage: String = "Auto",
    val targetLanguageOption: TargetLanguageOption = TargetLanguageOption.INDONESIAN,
    val customTargetLanguage: String = "Bahasa Indonesia",
    val stylePreset: TranslationStylePreset = TranslationStylePreset.ANIME_CASUAL,
    val customPromptInstruction: String = TranslationStylePreset.ANIME_CASUAL.styleInstruction,
    val separatorMode: LineSeparatorMode = LineSeparatorMode.DOUBLE_SLASH,
    val customSeparator: String = " // ",
    val protectAssTags: Boolean = true,
    val indexFormatMode: IndexFormatMode = IndexFormatMode.BRACKET,
    val batchSizeOption: BatchSizeOption = BatchSizeOption.BATCH_100,
    val includeAiPromptHeader: Boolean = true,
    val includeGlossaryInPrompt: Boolean = true,
    val glossaryRules: List<GlossaryRule> = emptyList(),
    val autoApplyGlossaryBeforeMerge: Boolean = false,
    val restoreLeadingAssTags: Boolean = true,
    val bilingualMode: Boolean = false,
    val bilingualOrder: BilingualOrder = BilingualOrder.TRANSLATION_TOP,
    val bilingualAssSecondaryStyling: Boolean = true,
    val maxRetries: Int = 3,
    val maxChunkRepairRetries: Int = 1,
    val checkUntranslatedSourceLeakage: Boolean = true
) {
    val effectiveTargetLanguageLabel: String
        get() = if (targetLanguageOption == TargetLanguageOption.CUSTOM) {
            customTargetLanguage.ifBlank { "Bahasa Indonesia" }
        } else {
            targetLanguageOption.label
        }
}

/**
 * Low-level AI transport gateway invoked by [SingleTranslationPipeline].
 * In production, backed by [ProductionSingleTranslationAiGateway] which calls
 * [GeminiWebCookieClient] or [OpenAiCompatibleClient] identically for Single and Batch translation.
 */
fun interface SingleTranslationAiGateway {
    suspend fun requestChunkTranslation(
        config: SingleTranslationConfig,
        systemPrompt: String,
        numberedUserLines: String
    ): Result<String>
}

/**
 * Production implementation of [SingleTranslationAiGateway] shared by Single and Batch translation.
 */
object ProductionSingleTranslationAiGateway : SingleTranslationAiGateway {
    override suspend fun requestChunkTranslation(
        config: SingleTranslationConfig,
        systemPrompt: String,
        numberedUserLines: String
    ): Result<String> {
        val ai = config.aiSettings
        return if (ai.preset == AiProviderPreset.GEMINI_WEB_COOKIE) {
            GeminiWebCookieClient.translateSubtitleBatchWithCookies(
                psid = ai.geminiSecure1Psid,
                psidts = ai.geminiSecure1Psidts,
                modelId = ai.selectedModel,
                systemPrompt = systemPrompt,
                numberedUserLines = numberedUserLines
            )
        } else {
            OpenAiCompatibleClient.translateSubtitleBatch(
                baseUrl = ai.baseUrl,
                apiKey = ai.apiKey,
                modelId = ai.selectedModel,
                systemPrompt = systemPrompt,
                numberedUserLines = numberedUserLines,
                temperature = ai.temperature,
                combineSystemIntoUserPrompt = ai.combineSystemIntoUserPrompt,
                geminiSecure1Psid = ai.geminiSecure1Psid,
                geminiSecure1Psidts = ai.geminiSecure1Psidts,
                allowNoApiKey = ai.preset.isGeminiWebFamily
            )
        }
    }
}

/**
 * Debug log record for a single chunk processed (or failed) by [SingleTranslationPipeline].
 */
data class AiChunkDebugLogEntry(
    val batchNumber: Int,
    val cueRangeLabel: String,
    val providerLabel: String,
    val modelId: String,
    val strategyUsed: String,
    val mappedCount: Int,
    val expectedCount: Int,
    val rawResponse: String
)

/**
 * Exception thrown when an AI response cannot be mapped even after flexible parsing,
 * format-repair auto-retry, and positional fallback mapping.
 * Preserves the [debugLogEntry] and raw response snippet so the UI can display it immediately.
 */
class SingleTranslationFormatException(
    message: String,
    val debugLogEntry: AiChunkDebugLogEntry,
    val partialValidCues: Map<Int, String> = emptyMap()
) : IllegalStateException(message)

class SingleTranslationPartialException(
    message: String,
    val partialValidCues: Map<Int, String>,
    cause: Throwable? = null
) : IllegalStateException(message, cause)

/**
 * Telemetry record proving that a file (whether via Single Translation or Batch Queue)
 * was processed through [SingleTranslationPipeline] with a specific [SingleTranslationConfig].
 */
data class SingleTranslationPipelineExecutionReport(
    val fileName: String,
    val configSnapshot: SingleTranslationConfig,
    val systemPromptUsed: String,
    val chunkSizes: List<Int>,
    val totalCues: Int,
    val translatedCuesCount: Int,
    val chunkRetryCount: Int,
    val mergedContent: String,
    val validationResult: BatchValidationResult,
    val debugLogs: List<AiChunkDebugLogEntry> = emptyList()
)

/**
 * Result of running [SingleTranslationPipeline] on a [SubtitleDocument].
 */
data class SingleTranslationPipelineResult(
    val translatedCues: Map<Int, String>,
    val mergedContent: String,
    val validationResult: BatchValidationResult,
    val report: SingleTranslationPipelineExecutionReport
)

/**
 * Single Source of Truth Translation Pipeline used by BOTH:
 * 1. **Single Translation** (Tab Ekstrak / Paste AI / Merge)
 * 2. **Batch Subtitle Translation Queue** (Tab Batch)
 *
 * Principle: Batch Translation = Single Translation executed automatically and sequentially.
 * There is NO separate batch translation engine, prompt builder, chunking strategy,
 * postprocessor, or merge formatter.
 */
object SingleTranslationPipeline {

    const val PIPELINE_ID = "UNIFIED_SINGLE_TRANSLATION_PIPELINE_V1"

    /**
     * Unified chunking strategy used by both Single Translation (`SubtitleUiState.batches`)
     * and Batch Translation.
     * - When `batchSizeOption.size <= 0` (`BatchSizeOption.ALL`), all cues are kept in a single chunk.
     * - Otherwise, cues are split into contiguous chunks of `batchSizeOption.size`.
     */
    fun chunkCues(
        cues: List<SubtitleCue>,
        batchSizeOption: BatchSizeOption
    ): List<List<SubtitleCue>> {
        if (cues.isEmpty()) return emptyList()
        val chunkSize = batchSizeOption.size
        return if (chunkSize <= 0 || chunkSize >= cues.size) {
            listOf(cues)
        } else {
            cues.chunked(chunkSize)
        }
    }

    /**
     * Unified System Prompt builder used by both Single Translation and Batch Translation.
     */
    fun buildSystemPrompt(config: SingleTranslationConfig): String {
        return SubtitleParserAndMerger.buildModelAdaptedSystemPrompt(
            selectedModel = config.aiSettings.selectedModel,
            separatorMode = config.separatorMode,
            customSeparator = config.customSeparator,
            indexFormatMode = config.indexFormatMode,
            includeGlossaryInPrompt = config.includeGlossaryInPrompt,
            targetLanguageOption = config.targetLanguageOption,
            customTargetLanguage = config.customTargetLanguage,
            stylePreset = config.stylePreset,
            customPromptInstruction = config.customPromptInstruction,
            glossaryRules = config.glossaryRules
        )
    }

    /**
     * Detects canned Gemini Web / LLM refusal replies (e.g., "Saya tidak diprogram untuk itu. Saya hanya menghasilkan teks.",
     * "Saya hanya AI berbasis teks dan tidak bisa membantu Anda untuk itu.") so the pipeline switches immediately to
     * adaptive sub-batch splitting + neutral plain-text prompt instead of repeating the rejected chunk.
     */
    fun isAiRefusalResponse(rawResponse: String): Boolean {
        val trimmed = rawResponse.trim()
        if (trimmed.isEmpty()) return false
        val lower = trimmed.lowercase()
        val hasRefusalPhrase = lower.contains("tidak diprogram untuk itu") ||
            lower.contains("hanya menghasilkan teks") ||
            lower.contains("hanya ai berbasis teks") ||
            lower.contains("tidak bisa membantu anda untuk itu") ||
            lower.contains("tidak dapat membantu anda untuk itu") ||
            lower.contains("maaf, saya tidak bisa membantu") ||
            lower.contains("maaf, saya tidak dapat membantu") ||
            lower.contains("sebagai model bahasa") ||
            lower.contains("i'm not programmed") ||
            lower.contains("i am not programmed") ||
            lower.contains("only generate text") ||
            lower.contains("text-based ai") ||
            lower.contains("cannot help with that") ||
            lower.contains("can't help with that") ||
            lower.contains("cannot assist with that") ||
            lower.contains("can't assist with that") ||
            lower.contains("cannot fulfill this request")
        if (!hasRefusalPhrase) return false
        // Only classify as refusal if there are no substantial numbered dialogue lines
        val numberedLines = trimmed.lineSequence().count { line ->
            line.trim().matches(Regex("""^(?:\[\s*\d+\s*\]|\d+\s*[.|:\-)]|#\d+)\s+\S.*"""))
        }
        return numberedLines < 2
    }

    /**
     * Neutral plain-text literary/dialogue prompt free of file extensions (.ASS/.SRT/.VTT) or technical jargon
     * that can trigger Gemini Web's capability/tool refusal classifier.
     */
    fun buildNeutralDialoguePrompt(
        config: SingleTranslationConfig,
        cueIds: List<Int> = emptyList()
    ): String {
        val targetLang = config.effectiveTargetLanguageLabel
        val activeSep = when (config.separatorMode) {
            LineSeparatorMode.CUSTOM -> config.customSeparator.ifEmpty { " // " }
            else -> config.separatorMode.separatorToken
        }.trim()
        val sampleId = cueIds.firstOrNull() ?: 1
        val sampleLine = config.indexFormatMode.formatLine(sampleId, "Terjemahan baris ke-$sampleId")
        val rangeHint = if (cueIds.size > 1) {
            "Nomor yang harus ada: ${cueIds.first()} s.d. ${cueIds.last()}."
        } else {
            ""
        }
        return buildString {
            appendLine("Terjemahkan baris teks percakapan fiksi bernomor berikut ke dalam $targetLang dengan gaya alami.")
            appendLine("- Ini murni terjemahan teks bacaan biasa.")
            appendLine("- Awali setiap hasil dengan nomor urut yang sama persis seperti input (contoh: $sampleLine). $rangeHint".trim())
            if (config.separatorMode != LineSeparatorMode.SPACE && activeSep.isNotEmpty()) {
                appendLine("- Pertahankan tanda pemisah \"$activeSep\" di posisi jeda kalimat.")
            }
            appendLine("- Jangan terjemahkan token kurung kurawal {...} jika ada.")
            appendLine("- Balas HANYA dengan daftar terjemahan bernomor tanpa komentar pembuka atau penutup.")
        }.trim()
    }

    /**
     * Explicit prompt used when repairing ONLY specific problematic cues (e.g. #194, #196)
     * that were missing, empty, or left untranslated while locking 1:1 cue indices.
     */
    fun buildTargetedProblematicCuesPrompt(
        config: SingleTranslationConfig,
        problemIds: List<Int>
    ): String {
        val targetLang = config.effectiveTargetLanguageLabel
        val activeSep = when (config.separatorMode) {
            LineSeparatorMode.CUSTOM -> config.customSeparator.ifEmpty { " // " }
            else -> config.separatorMode.separatorToken
        }.trim()
        val idListLabel = problemIds.joinToString(", ") { "#$it" }
        return buildString {
            appendLine("Terjemahkan baris teks dialog bernomor berikut ke dalam $targetLang secara lengkap dan alami.")
            appendLine("PERBAIKAN KHUSUS BARIS BERMASALAH ($idListLabel):")
            appendLine("- Jangan meninggalkan kalimat dalam bahasa sumber; setiap baris bernomor WAJIB diterjemahkan ke $targetLang.")
            appendLine("- Kunci 1:1 nomor urut [ID] sesuai input tanpa menggabungkan atau melewati nomor.")
            if (config.separatorMode != LineSeparatorMode.SPACE && activeSep.isNotEmpty()) {
                appendLine("- Pertahankan tanda pemisah \"$activeSep\" jika ada.")
            }
            appendLine("- Balas HANYA dengan daftar baris bernomor tersebut.")
        }.trim()
    }

    /**
     * Detects ASS vector drawing cues (e.g., `{\p1}m 0 0 l ...{\p0}`) or pure symbols/numbers
     * that do not represent spoken text and could confuse an LLM into a capability refusal.
     */
    private fun isAssDrawingOrNonDialogueCue(cue: SubtitleCue): Boolean {
        if (cue.rawText.contains(Regex("""\{\\[^}]*\\p[1-9][^}]*\}|\{\\p[1-9][^}]*\}"""))) {
            return true
        }
        return BatchTranslationValidator.isNonTranslatableSymbolOrNumberCue(cue.cleanText)
    }

    /**
     * Builds an ultra-explicit format-repair System Prompt when the first AI response omitted line numbers.
     */
    fun buildStrictFormatRepairPrompt(
        config: SingleTranslationConfig,
        chunkCues: List<SubtitleCue>
    ): String {
        val basePrompt = buildSystemPrompt(config)
        val firstId = chunkCues.firstOrNull()?.id ?: 1
        val lastId = chunkCues.lastOrNull()?.id ?: chunkCues.size
        val sampleFirst = config.indexFormatMode.formatLine(firstId, "Terjemahan baris pertama...")
        val sampleLast = config.indexFormatMode.formatLine(lastId, "Terjemahan baris terakhir...")
        return buildString {
            appendLine(basePrompt)
            appendLine()
            appendLine("PERINGATAN PERBAIKAN FORMAT WAJIB (AUTO-RETRY):")
            appendLine("- Respons Anda sebelumnya gagal dipetakan karena nomor urut baris tidak ditulis dengan benar.")
            appendLine("- Anda WAJIB mengawali SETIAP baris hasil terjemahan dengan nomor urut dari #$firstId sampai #$lastId:")
            appendLine("  $sampleFirst")
            appendLine("  ...")
            appendLine("  $sampleLast")
            appendLine("- JANGAN menulis kalimat sapaan, penjelasan, atau penutup apa pun.")
        }.trim()
    }

    /**
     * Unified Cue Extraction formatter for a chunk of subtitle cues.
     */
    fun formatChunkForExtraction(
        chunk: List<SubtitleCue>,
        config: SingleTranslationConfig
    ): String {
        return chunk.joinToString("\n") { cue ->
            SubtitleParserAndMerger.formatCueForExtraction(
                cue = cue,
                separatorMode = config.separatorMode,
                customSeparator = config.customSeparator,
                protectAssTags = config.protectAssTags,
                indexFormatMode = config.indexFormatMode
            )
        }
    }

    /**
     * Unified post-processing step (glossary enforcement) applied identically in Single and Batch modes.
     */
    fun applyPostProcessing(
        rawTranslations: Map<Int, String>,
        config: SingleTranslationConfig
    ): Map<Int, String> {
        if (rawTranslations.isEmpty()) return emptyMap()
        return if (config.autoApplyGlossaryBeforeMerge && config.glossaryRules.isNotEmpty()) {
            SubtitleParserAndMerger.applyGlossaryRulesToTranslations(
                rawTranslations,
                config.glossaryRules
            ).first
        } else {
            LinkedHashMap(rawTranslations)
        }
    }

    /**
     * Unified subtitle reconstruction (merging) used by both Single Translation (`buildFinalMergedFileContent`)
     * and Batch Translation (`BatchQueueEngine`).
     */
    fun reconstructMergedContent(
        document: SubtitleDocument,
        translations: Map<Int, String>,
        config: SingleTranslationConfig
    ): String {
        val processedTranslations = applyPostProcessing(translations, config)
        return SubtitleParserAndMerger.generateMergedFileContent(
            document = document,
            translations = processedTranslations,
            separatorMode = config.separatorMode,
            customSeparator = config.customSeparator,
            restoreLeadingAssTags = config.restoreLeadingAssTags,
            bilingualMode = config.bilingualMode,
            bilingualOrder = config.bilingualOrder,
            bilingualAssSecondaryStyling = config.bilingualAssSecondaryStyling
        )
    }

    /**
     * Unified translation validation used by both Single Translation and Batch Translation.
     */
    fun validateTranslation(
        document: SubtitleDocument,
        translations: Map<Int, String>,
        mergedContent: String,
        config: SingleTranslationConfig
    ): BatchValidationResult {
        val processedTranslations = applyPostProcessing(translations, config)
        return BatchTranslationValidator.validateSubtitleTranslation(
            sourceDocument = document,
            translatedMap = processedTranslations,
            mergedFileContent = mergedContent,
            sourceLanguage = config.sourceLanguage,
            targetLanguage = config.effectiveTargetLanguageLabel,
            checkUntranslatedSourceLeakage = config.checkUntranslatedSourceLeakage,
            expectLeadingAssTagsRestored = config.restoreLeadingAssTags
        )
    }

    /**
     * Executes the complete Single Translation Pipeline on [sourceDocument] (or specific [chunkIndices]
     * when the user translates a single batch tab in Single Translation UI).
     *
     * Features:
     * - Adaptive Sub-Batch Splitting + Neutral Prompt Recovery when Gemini Web returns refusals
     *   (e.g., "Saya tidak diprogram untuk itu. Saya hanya menghasilkan teks.").
     * - Targeted Problematic-Cue Re-Translation + 1:1 Alignment Lock so only problematic lines
     *   (e.g. #194, #196) are re-translated without dropping or shifting surrounding valid cues.
     */
    suspend fun executePipeline(
        sourceDocument: SubtitleDocument,
        config: SingleTranslationConfig,
        aiGateway: SingleTranslationAiGateway = ProductionSingleTranslationAiGateway,
        chunkIndicesToProcess: List<Int>? = null,
        initialTranslations: Map<Int, String> = emptyMap(),
        shouldAbort: () -> Boolean = { false },
        onChunkStarted: suspend (chunkIndex: Int, totalChunks: Int, chunkCues: List<SubtitleCue>) -> Unit = { _, _, _ -> },
        onChunkDebugLog: suspend (AiChunkDebugLogEntry) -> Unit = {},
        onChunkTranslated: suspend (
            chunkIndex: Int,
            totalChunks: Int,
            chunkTranslations: Map<Int, String>,
            accumulatedTranslations: Map<Int, String>
        ) -> Unit = { _, _, _, _ -> }
    ): Result<SingleTranslationPipelineResult> {
        return runCatching {
            val allChunks = chunkCues(sourceDocument.cues, config.batchSizeOption)
            val targetIndices = chunkIndicesToProcess
                ?.filter { it in allChunks.indices }
                ?: allChunks.indices.toList()

            val systemPrompt = buildSystemPrompt(config)
            val accumulatedMap = LinkedHashMap<Int, String>(initialTranslations)
            val recordedDebugLogs = mutableListOf<AiChunkDebugLogEntry>()
            var totalChunkRetries = 0

            for ((_, chunkIdx) in targetIndices.withIndex()) {
                if (shouldAbort()) {
                    throw IllegalStateException("Proses penerjemahan dihentikan oleh pengguna.")
                }

                val fullChunkCues = allChunks[chunkIdx]
                if (fullChunkCues.isEmpty()) continue

                // Pre-preserve ASS vector drawing / symbol-only cues so AI doesn't choke on them
                for (cue in fullChunkCues) {
                    if (!accumulatedMap.containsKey(cue.id) && isAssDrawingOrNonDialogueCue(cue)) {
                        accumulatedMap[cue.id] = cue.cleanText.trim().ifEmpty { "..." }
                    }
                }

                // When resuming with previously valid cues, skip chunks that are already 100% present
                if (chunkIndicesToProcess == null && initialTranslations.isNotEmpty() &&
                    fullChunkCues.all { accumulatedMap.containsKey(it.id) }
                ) {
                    continue
                }

                val chunkCues = if (chunkIndicesToProcess == null && initialTranslations.isNotEmpty()) {
                    fullChunkCues.filter { !accumulatedMap.containsKey(it.id) }
                } else {
                    fullChunkCues
                }
                if (chunkCues.isEmpty()) continue

                onChunkStarted(chunkIdx, allChunks.size, chunkCues)

                val firstId = chunkCues.firstOrNull()?.id ?: 1
                val lastId = chunkCues.lastOrNull()?.id ?: chunkCues.size
                val cueRangeLabel = "#$firstId–#$lastId"

                val userLinesText = formatChunkForExtraction(chunkCues, config)
                val primaryResult = aiGateway.requestChunkTranslation(
                    config = config,
                    systemPrompt = systemPrompt,
                    numberedUserLines = userLinesText
                ).flattenNestedResult()

                val primaryResponse = primaryResult.getOrElse { err ->
                    throw SingleTranslationPartialException(
                        message = err.message ?: "Gagal menerjemahkan chunk ${chunkIdx + 1}.",
                        partialValidCues = LinkedHashMap(accumulatedMap),
                        cause = err
                    )
                }

                val primaryWasRefusal = isAiRefusalResponse(primaryResponse)

                // Stage A & Stage B: Flexible inline & multi-line numbered parsing
                val initialParseReport = if (primaryWasRefusal) {
                    AiLineParseReport(emptyMap(), "Penolakan AI Terdeteksi")
                } else {
                    SubtitleParserAndMerger.parseAiTranslatedLinesWithReport(
                        pastedText = primaryResponse,
                        expectedCues = chunkCues,
                        allowPositionalFallback = false
                    )
                }
                val parsedChunk = LinkedHashMap(initialParseReport.parsedLines)
                var activeStrategy = initialParseReport.strategyUsed
                var latestRawResponse = primaryResponse

                // Stage C: Auto-Retry with strict format-lock prompt if Stage A & B found 0 indexed lines (skip if refusal)
                if (parsedChunk.isEmpty() && !primaryWasRefusal && !shouldAbort() && config.maxChunkRepairRetries > 0) {
                    totalChunkRetries++
                    val strictRepairPrompt = buildStrictFormatRepairPrompt(config, chunkCues)
                    val formatRetryResult = aiGateway.requestChunkTranslation(
                        config = config,
                        systemPrompt = strictRepairPrompt,
                        numberedUserLines = userLinesText
                    ).flattenNestedResult()

                    if (formatRetryResult.isSuccess) {
                        val retryRawText = formatRetryResult.getOrThrow()
                        if (retryRawText.isNotBlank()) {
                            latestRawResponse = retryRawText
                        }
                        if (!isAiRefusalResponse(retryRawText)) {
                            val retryParseReport = SubtitleParserAndMerger.parseAiTranslatedLinesWithReport(
                                pastedText = retryRawText,
                                expectedCues = chunkCues,
                                allowPositionalFallback = false
                            )
                            if (retryParseReport.parsedLines.isNotEmpty()) {
                                parsedChunk.putAll(retryParseReport.parsedLines)
                                activeStrategy = "Auto-Retry Format (${retryParseReport.strategyUsed})"
                            } else {
                                // Stage D1: Positional Fallback on retry response
                                val retryFallbackReport = SubtitleParserAndMerger.parseAiTranslatedLinesWithReport(
                                    pastedText = retryRawText,
                                    expectedCues = chunkCues,
                                    allowPositionalFallback = true
                                )
                                if (retryFallbackReport.parsedLines.isNotEmpty()) {
                                    parsedChunk.putAll(retryFallbackReport.parsedLines)
                                    activeStrategy = "Auto-Retry + ${retryFallbackReport.strategyUsed}"
                                }
                            }
                        }
                    }
                }

                // Stage D2: Positional Fallback on primary response if still empty & not refusal
                if (parsedChunk.isEmpty() && !primaryWasRefusal) {
                    val primaryFallbackReport = SubtitleParserAndMerger.parseAiTranslatedLinesWithReport(
                        pastedText = primaryResponse,
                        expectedCues = chunkCues,
                        allowPositionalFallback = true
                    )
                    if (primaryFallbackReport.parsedLines.isNotEmpty()) {
                        parsedChunk.putAll(primaryFallbackReport.parsedLines)
                        activeStrategy = primaryFallbackReport.strategyUsed
                    }
                }

                // Stage E: Adaptive Sub-Batch Splitting + Neutral Prompt Recovery on refusal or unmapped batch
                if (parsedChunk.isEmpty() && !shouldAbort()) {
                    totalChunkRetries++
                    val adaptiveResult = recoverChunkWithAdaptiveSubBatches(
                        cues = chunkCues,
                        config = config,
                        aiGateway = aiGateway,
                        shouldAbort = shouldAbort,
                        onSubBatchTranslated = { partialSubMap ->
                            val processedSub = applyPostProcessing(partialSubMap, config)
                            accumulatedMap.putAll(processedSub)
                            onChunkTranslated(
                                chunkIdx,
                                allChunks.size,
                                processedSub,
                                LinkedHashMap(accumulatedMap)
                            )
                        }
                    )
                    if (adaptiveResult.recoveredMap.isNotEmpty()) {
                        parsedChunk.putAll(adaptiveResult.recoveredMap)
                        activeStrategy = "Pemulihan Sub-Batch Adaptif & Prompt Netral"
                    }
                    if (adaptiveResult.lastRawSnippet.isNotBlank()) {
                        latestRawResponse = adaptiveResult.lastRawSnippet
                    }
                }

                if (parsedChunk.isEmpty()) {
                    val cleanSnippet = latestRawResponse
                        .replace("\r\n", " ")
                        .replace("\n", " ")
                        .replace(Regex("""\s{2,}"""), " ")
                        .trim()
                        .take(180)
                        .ifEmpty { "(Respons kosong)" }

                    val failedLogEntry = AiChunkDebugLogEntry(
                        batchNumber = chunkIdx + 1,
                        cueRangeLabel = cueRangeLabel,
                        providerLabel = config.aiSettings.preset.displayTitle,
                        modelId = config.aiSettings.selectedModel,
                        strategyUsed = "Gagal Parsing / Penolakan AI",
                        mappedCount = 0,
                        expectedCount = chunkCues.size,
                        rawResponse = latestRawResponse
                    )
                    recordedDebugLogs.add(failedLogEntry)
                    onChunkDebugLog(failedLogEntry)

                    throw SingleTranslationFormatException(
                        message = "Respons AI pada Batch ${chunkIdx + 1} tidak memiliki format nomor urut yang valid. Cuplikan respons AI: \"$cleanSnippet\"",
                        debugLogEntry = failedLogEntry,
                        partialValidCues = LinkedHashMap(accumulatedMap)
                    )
                }

                // If the AI skipped a subset of cue IDs within this chunk, perform chunk-level recovery
                var repairAttempt = 0
                while (repairAttempt < config.maxChunkRepairRetries) {
                    val missingInChunk = chunkCues.filter { !parsedChunk.containsKey(it.id) }
                    if (missingInChunk.isEmpty()) break
                    if (shouldAbort()) break

                    repairAttempt++
                    totalChunkRetries++

                    val retryWindowCues = if (missingInChunk.size <= 12 && chunkCues.size > 20) {
                        buildContextPreservingWindow(chunkCues, missingInChunk)
                    } else {
                        chunkCues
                    }
                    val retryUserLines = formatChunkForExtraction(retryWindowCues, config)
                    val retryResult = aiGateway.requestChunkTranslation(
                        config = config,
                        systemPrompt = systemPrompt,
                        numberedUserLines = retryUserLines
                    ).flattenNestedResult()
                    if (retryResult.isSuccess) {
                        val retryText = retryResult.getOrThrow()
                        if (!isAiRefusalResponse(retryText)) {
                            val repairedLines = SubtitleParserAndMerger.parseAiTranslatedLines(
                                pastedText = retryText,
                                expectedCues = retryWindowCues,
                                allowPositionalFallback = (retryWindowCues.size == missingInChunk.size)
                            )
                            for (missingCue in missingInChunk) {
                                repairedLines[missingCue.id]?.let { parsedChunk[missingCue.id] = it }
                            }
                        } else if (missingInChunk.size <= 25) {
                            val subRecovered = recoverChunkWithAdaptiveSubBatches(
                                cues = missingInChunk,
                                config = config,
                                aiGateway = aiGateway,
                                shouldAbort = shouldAbort
                            )
                            parsedChunk.putAll(subRecovered.recoveredMap)
                        }
                    }
                }

                // Preserve pure symbol/number/ASS-drawing cues
                for (cue in chunkCues) {
                    if (!parsedChunk.containsKey(cue.id) && isAssDrawingOrNonDialogueCue(cue)) {
                        parsedChunk[cue.id] = cue.cleanText.trim().ifEmpty { "..." }
                    }
                }

                val successLogEntry = AiChunkDebugLogEntry(
                    batchNumber = chunkIdx + 1,
                    cueRangeLabel = cueRangeLabel,
                    providerLabel = config.aiSettings.preset.displayTitle,
                    modelId = config.aiSettings.selectedModel,
                    strategyUsed = activeStrategy,
                    mappedCount = parsedChunk.size,
                    expectedCount = chunkCues.size,
                    rawResponse = latestRawResponse
                )
                recordedDebugLogs.add(successLogEntry)
                onChunkDebugLog(successLogEntry)

                val processedChunk = applyPostProcessing(parsedChunk, config)
                accumulatedMap.putAll(processedChunk)

                onChunkTranslated(
                    chunkIdx,
                    allChunks.size,
                    processedChunk,
                    LinkedHashMap(accumulatedMap)
                )
            }

            // Phase B: Targeted Problematic-Cue Re-Translation + 1:1 Alignment Lock
            // Runs ONLY when processing the full document (chunkIndicesToProcess == null) and
            // validation still detects specific missing, empty, or untranslated cues (e.g. #194, #196).
            var finalTranslations = applyPostProcessing(accumulatedMap, config)
            var mergedContent = reconstructMergedContent(
                document = sourceDocument,
                translations = finalTranslations,
                config = config
            )
            var validationResult = validateTranslation(
                document = sourceDocument,
                translations = finalTranslations,
                mergedContent = mergedContent,
                config = config
            )

            if (chunkIndicesToProcess == null && !validationResult.isValid && !shouldAbort()) {
                val cueById = sourceDocument.cues.associateBy { it.id }
                var repairPass = 0
                while (!validationResult.isValid && repairPass < 2 && !shouldAbort()) {
                    val problemIds = (
                        validationResult.missingCueIds +
                            validationResult.emptyOrTruncatedCueIds +
                            validationResult.untranslatedCueIds
                        ).distinct().sorted()
                    if (problemIds.isEmpty() || problemIds.size > 40) break

                    repairPass++
                    totalChunkRetries++

                    val patchedAny = repairProblematicCuesWithAlignmentLock(
                        sourceDocument = sourceDocument,
                        cueById = cueById,
                        problemIds = problemIds,
                        accumulatedMap = accumulatedMap,
                        config = config,
                        aiGateway = aiGateway,
                        useSingleCueDirectMode = (repairPass >= 2),
                        shouldAbort = shouldAbort
                    )
                    if (!patchedAny && repairPass >= 2) break

                    finalTranslations = applyPostProcessing(accumulatedMap, config)
                    mergedContent = reconstructMergedContent(
                        document = sourceDocument,
                        translations = finalTranslations,
                        config = config
                    )
                    validationResult = validateTranslation(
                        document = sourceDocument,
                        translations = finalTranslations,
                        mergedContent = mergedContent,
                        config = config
                    )
                }
            }

            val report = SingleTranslationPipelineExecutionReport(
                fileName = sourceDocument.fileName,
                configSnapshot = config,
                systemPromptUsed = systemPrompt,
                chunkSizes = targetIndices.map { allChunks[it].size },
                totalCues = sourceDocument.cues.size,
                translatedCuesCount = finalTranslations.size,
                chunkRetryCount = totalChunkRetries,
                mergedContent = mergedContent,
                validationResult = validationResult,
                debugLogs = recordedDebugLogs
            )

            SingleTranslationPipelineResult(
                translatedCues = finalTranslations,
                mergedContent = mergedContent,
                validationResult = validationResult,
                report = report
            )
        }.flattenNestedResult()
    }

    private data class AdaptiveRecoveryOutcome(
        val recoveredMap: Map<Int, String>,
        val lastRawSnippet: String
    )

    /**
     * Progressively splits a refused or unmapped chunk into smaller sub-batches (25 -> 8 -> 2 -> 1)
     * using a neutral plain-text prompt until each safe group translates cleanly.
     */
    private suspend fun recoverChunkWithAdaptiveSubBatches(
        cues: List<SubtitleCue>,
        config: SingleTranslationConfig,
        aiGateway: SingleTranslationAiGateway,
        shouldAbort: () -> Boolean,
        onSubBatchTranslated: suspend (Map<Int, String>) -> Unit = {}
    ): AdaptiveRecoveryOutcome {
        val recovered = LinkedHashMap<Int, String>()
        var lastSnippet = ""

        suspend fun processGroup(group: List<SubtitleCue>) {
            if (group.isEmpty() || shouldAbort()) return

            val neutralPrompt = buildNeutralDialoguePrompt(config, group.map { it.id })
            val payload = formatChunkForExtraction(group, config)
            val result = aiGateway.requestChunkTranslation(
                config = config,
                systemPrompt = neutralPrompt,
                numberedUserLines = payload
            ).flattenNestedResult()

            val rawText = result.getOrNull().orEmpty()
            if (rawText.isNotBlank()) {
                lastSnippet = rawText
            }

            val refused = result.isFailure || rawText.isBlank() || isAiRefusalResponse(rawText)
            if (!refused) {
                val parsed = SubtitleParserAndMerger.parseAiTranslatedLinesWithReport(
                    pastedText = rawText,
                    expectedCues = group,
                    allowPositionalFallback = true
                ).parsedLines

                val groupCueIds = group.map { it.id }.toSet()
                val validForGroup = parsed.filterKeys { it in groupCueIds && parsed[it]?.isNotBlank() == true }
                if (validForGroup.isNotEmpty()) {
                    recovered.putAll(validForGroup)
                    onSubBatchTranslated(validForGroup)
                    val remaining = group.filter { !recovered.containsKey(it.id) }
                    if (remaining.isEmpty()) return
                    // Split any missed cues in this group into smaller pieces
                    val nextSize = nextSmallerSubBatchSize(remaining.size)
                    for (sub in remaining.chunked(nextSize)) {
                        processGroup(sub)
                    }
                    return
                }
            }

            // Still refused or unmapped: split further down to 1 cue at a time
            if (group.size > 1) {
                val nextSize = nextSmallerSubBatchSize(group.size)
                for (sub in group.chunked(nextSize)) {
                    processGroup(sub)
                }
            } else {
                // Single cue fallback: ultra-minimal plain sentence request without numbering brackets
                val singleCue = group.first()
                if (isAssDrawingOrNonDialogueCue(singleCue)) {
                    val preserved = singleCue.cleanText.trim().ifEmpty { "..." }
                    recovered[singleCue.id] = preserved
                    onSubBatchTranslated(mapOf(singleCue.id to preserved))
                    return
                }

                val singleText = formatCueBodyPlain(singleCue, config)
                if (singleText.isNotBlank() && !shouldAbort()) {
                    val directPrompt = "Terjemahkan kalimat teks percakapan berikut ke dalam ${config.effectiveTargetLanguageLabel} secara alami. Jawab hanya dengan satu baris terjemahannya."
                    val directResult = aiGateway.requestChunkTranslation(
                        config = config,
                        systemPrompt = directPrompt,
                        numberedUserLines = singleText
                    ).flattenNestedResult()
                    val directRaw = directResult.getOrNull().orEmpty().trim()
                    if (directRaw.isNotBlank() && !isAiRefusalResponse(directRaw)) {
                        val cleanedSingle = cleanSingleLineDirectTranslation(directRaw, singleCue.id)
                        if (cleanedSingle.isNotBlank()) {
                            recovered[singleCue.id] = cleanedSingle
                            onSubBatchTranslated(mapOf(singleCue.id to cleanedSingle))
                        }
                    } else if (directRaw.isNotBlank()) {
                        lastSnippet = directRaw
                    }
                }
            }
        }

        val initialStep = if (cues.size > 25) 25 else nextSmallerSubBatchSize(cues.size)
        for (sub in cues.chunked(initialStep)) {
            if (shouldAbort()) break
            processGroup(sub)
        }

        return AdaptiveRecoveryOutcome(
            recoveredMap = recovered,
            lastRawSnippet = lastSnippet
        )
    }

    private fun nextSmallerSubBatchSize(currentSize: Int): Int {
        return when {
            currentSize > 25 -> 25
            currentSize > 8 -> 8
            currentSize > 2 -> 2
            else -> 1
        }
    }

    /**
     * Re-translates ONLY problematic cue IDs (`missingCueIds`, `emptyOrTruncatedCueIds`, `untranslatedCueIds`)
     * while strictly preserving all surrounding valid cues, indices `1..N`, ASS leading tags, and timestamps.
     */
    private suspend fun repairProblematicCuesWithAlignmentLock(
        sourceDocument: SubtitleDocument,
        cueById: Map<Int, SubtitleCue>,
        problemIds: List<Int>,
        accumulatedMap: LinkedHashMap<Int, String>,
        config: SingleTranslationConfig,
        aiGateway: SingleTranslationAiGateway,
        useSingleCueDirectMode: Boolean,
        shouldAbort: () -> Boolean
    ): Boolean {
        var patchedAny = false
        val problemIdSet = problemIds.toSet()

        if (!useSingleCueDirectMode) {
            val repairPrompt = buildTargetedProblematicCuesPrompt(config, problemIds)
            for (groupIds in problemIds.chunked(8)) {
                if (shouldAbort()) break
                val problemCues = groupIds.mapNotNull { cueById[it] }
                if (problemCues.isEmpty()) continue

                // Include at most 1 adjacent cue around the window for dialogue context while locking exact cue IDs
                val anchoredWindow = buildContextPreservingWindow(sourceDocument.cues, problemCues)
                val useAnchored = anchoredWindow.size <= problemCues.size + 4
                val cuesToSend = if (useAnchored) anchoredWindow else problemCues

                val payload = formatChunkForExtraction(cuesToSend, config)
                val responseResult = aiGateway.requestChunkTranslation(
                    config = config,
                    systemPrompt = repairPrompt,
                    numberedUserLines = payload
                ).flattenNestedResult()

                val rawText = responseResult.getOrNull().orEmpty()
                if (rawText.isNotBlank() && !isAiRefusalResponse(rawText)) {
                    val parsed = SubtitleParserAndMerger.parseAiTranslatedLines(
                        pastedText = rawText,
                        expectedCues = cuesToSend,
                        allowPositionalFallback = (!useAnchored || cuesToSend.size == problemCues.size)
                    )
                    for (id in groupIds) {
                        val candidate = parsed[id]?.trim().orEmpty()
                        val srcCue = cueById[id]
                        if (id in problemIdSet &&
                            candidate.isNotBlank() &&
                            !isAiRefusalResponse(candidate) &&
                            (srcCue == null || candidate != srcCue.cleanText.trim())
                        ) {
                            accumulatedMap[id] = candidate
                            patchedAny = true
                        }
                    }
                }
            }
        } else {
            // Pass 2: 1-by-1 direct plain translation strictly for remaining stubborn cues
            val targetLang = config.effectiveTargetLanguageLabel
            for (id in problemIds) {
                if (shouldAbort()) break
                val cue = cueById[id] ?: continue
                if (isAssDrawingOrNonDialogueCue(cue)) {
                    accumulatedMap[id] = cue.cleanText.trim().ifEmpty { "..." }
                    patchedAny = true
                    continue
                }
                val plainBody = formatCueBodyPlain(cue, config)
                if (plainBody.isBlank()) continue
                val directPrompt = buildString {
                    appendLine("Terjemahkan kalimat dialog berikut ke dalam $targetLang yang alami.")
                    appendLine("WAJIB diterjemahkan ke $targetLang, jangan mengulang kata bahasa aslinya.")
                    append("Balas HANYA dengan kalimat hasil terjemahan tanpa nomor atau komentar.")
                }
                val responseResult = aiGateway.requestChunkTranslation(
                    config = config,
                    systemPrompt = directPrompt,
                    numberedUserLines = plainBody
                ).flattenNestedResult()

                val rawText = responseResult.getOrNull().orEmpty().trim()
                if (rawText.isNotBlank() && !isAiRefusalResponse(rawText)) {
                    val cleaned = cleanSingleLineDirectTranslation(rawText, id)
                    if (cleaned.isNotBlank()) {
                        accumulatedMap[id] = cleaned
                        patchedAny = true
                    }
                }
            }
        }

        return patchedAny
    }

    private fun formatCueBodyPlain(cue: SubtitleCue, config: SingleTranslationConfig): String {
        val baseText = if (config.protectAssTags) {
            cue.cleanText
        } else {
            cue.textWithoutLeadingTags.ifEmpty { cue.rawText }
        }
        val activeSep = when (config.separatorMode) {
            LineSeparatorMode.CUSTOM -> config.customSeparator.ifEmpty { " // " }
            else -> config.separatorMode.separatorToken
        }
        return baseText
            .replace("\\N", activeSep)
            .replace("\\n", activeSep)
            .replace("\n", activeSep)
            .replace(Regex("""\s{2,}"""), " ")
            .trim()
    }

    private fun cleanSingleLineDirectTranslation(rawText: String, cueId: Int): String {
        val parsed = SubtitleParserAndMerger.parseAiTranslatedLines(rawText, allowPositionalFallback = false)
        parsed[cueId]?.takeIf { it.isNotBlank() }?.let { return it }
        parsed.values.firstOrNull()?.takeIf { it.isNotBlank() }?.let { return it }
        val nonEmptyLines = rawText.lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.startsWith("```") }
        return nonEmptyLines.joinToString(" // ")
            .replaceFirst(Regex("""^\[\s*#?\s*\d+\s*\]\s*[:.\-|]*\s*"""), "")
            .trim()
    }

    /**
     * Creates a [BatchCueTranslator] adapter that executes [SingleTranslationPipeline.executePipeline]
     * for every file in the Batch Queue.
     * Preserves previously valid cues (`job.validTranslatedCues`) on retry so already-translated
     * chunks are not re-sent and only remaining/problematic lines are repaired.
     */
    fun createBatchCueTranslator(
        configProvider: () -> SingleTranslationConfig,
        aiGateway: SingleTranslationAiGateway = ProductionSingleTranslationAiGateway,
        shouldAbort: () -> Boolean = { false },
        onPipelineReport: (SingleTranslationPipelineExecutionReport) -> Unit = {}
    ): BatchCueTranslator {
        return BatchCueTranslator { job, sourceDocument, _, onCueProgress ->
            val config = configProvider()
            val seedValidCues = job.validTranslatedCues
            val result = executePipeline(
                sourceDocument = sourceDocument,
                config = config,
                aiGateway = aiGateway,
                initialTranslations = seedValidCues,
                shouldAbort = shouldAbort,
                onChunkTranslated = { _, _, _, accumulated ->
                    onCueProgress(accumulated.size)
                }
            ).flattenNestedResult()
            if (result.isSuccess) {
                val pipelineResult = result.getOrThrow()
                onPipelineReport(pipelineResult.report)
                Result.success(
                    BatchTranslationAttemptOutput(
                        translatedCues = pipelineResult.translatedCues,
                        customMergedContent = pipelineResult.mergedContent,
                        pipelineReport = pipelineResult.report
                    )
                )
            } else {
                Result.failure(
                    result.exceptionOrNull()
                        ?: IllegalStateException("Gagal menerjemahkan file melalui SingleTranslationPipeline.")
                )
            }
        }
    }

    /**
     * Builds a contiguous window of cues around missing cues so the LLM always sees
     * conversational context and sequential numbering.
     */
    private fun buildContextPreservingWindow(
        chunkCues: List<SubtitleCue>,
        missingCues: List<SubtitleCue>
    ): List<SubtitleCue> {
        val missingIds = missingCues.map { it.id }.toSet()
        val selectedIndices = sortedSetOf<Int>()
        for ((idx, cue) in chunkCues.withIndex()) {
            if (cue.id in missingIds) {
                if (idx - 1 >= 0) selectedIndices.add(idx - 1)
                selectedIndices.add(idx)
                if (idx + 1 < chunkCues.size) selectedIndices.add(idx + 1)
            }
        }
        return selectedIndices.map { chunkCues[it] }
    }
}

/**
 * Unwraps any nested [Result] instances that Kotlin's coroutine continuation / SAM bridge
 * may box when a `suspend` function in a `fun interface` returns inline value class [Result].
 */
@Suppress("UNCHECKED_CAST")
internal fun <T> Result<T>.flattenNestedResult(): Result<T> {
    var current: Any? = this
    while (current is Result<*>) {
        if (current.isFailure) {
            return Result.failure(
                current.exceptionOrNull() ?: IllegalStateException("Unknown nested Result failure")
            )
        }
        val inner = current.getOrNull()
        if (inner is Result<*>) {
            current = inner
        } else {
            return Result.success(inner as T)
        }
    }
    return this
}

