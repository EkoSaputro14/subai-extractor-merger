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
    val validationResult: BatchValidationResult
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
     * Every chunk is processed sequentially with identical prompt, model config, chunk size,
     * response parsing, automatic missing-cue repair within a chunk, glossary post-processing,
     * subtitle reconstruction, and validation.
     */
    suspend fun executePipeline(
        sourceDocument: SubtitleDocument,
        config: SingleTranslationConfig,
        aiGateway: SingleTranslationAiGateway = ProductionSingleTranslationAiGateway,
        chunkIndicesToProcess: List<Int>? = null,
        initialTranslations: Map<Int, String> = emptyMap(),
        shouldAbort: () -> Boolean = { false },
        onChunkStarted: suspend (chunkIndex: Int, totalChunks: Int, chunkCues: List<SubtitleCue>) -> Unit = { _, _, _ -> },
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
            var totalChunkRetries = 0

            for ((stepIdx, chunkIdx) in targetIndices.withIndex()) {
                if (shouldAbort()) {
                    throw IllegalStateException("Proses penerjemahan dihentikan oleh pengguna.")
                }

                val chunkCues = allChunks[chunkIdx]
                if (chunkCues.isEmpty()) continue

                onChunkStarted(chunkIdx, allChunks.size, chunkCues)

                val userLinesText = formatChunkForExtraction(chunkCues, config)
                val primaryResponse = aiGateway.requestChunkTranslation(
                    config = config,
                    systemPrompt = systemPrompt,
                    numberedUserLines = userLinesText
                ).flattenNestedResult().getOrElse { err ->
                    throw IllegalStateException(err.message ?: "Gagal menerjemahkan chunk ${chunkIdx + 1}.")
                }

                val parsedChunk = LinkedHashMap(
                    SubtitleParserAndMerger.parseAiTranslatedLines(
                        pastedText = primaryResponse,
                        expectedCues = chunkCues
                    )
                )

                if (parsedChunk.isEmpty()) {
                    throw IllegalStateException(
                        "Respons AI pada Batch ${chunkIdx + 1} tidak memiliki format nomor urut yang valid."
                    )
                }

                // If the AI skipped a subset of cue IDs within this chunk, perform chunk-level recovery
                // using the exact same prompt & model configuration (with context lines preserved).
                var repairAttempt = 0
                while (repairAttempt < config.maxChunkRepairRetries) {
                    val missingInChunk = chunkCues.filter { !parsedChunk.containsKey(it.id) }
                    if (missingInChunk.isEmpty()) break
                    if (shouldAbort()) break

                    repairAttempt++
                    totalChunkRetries++

                    // Re-request the full chunk (or missing window with surrounding context) so numbering stays contiguous
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
                        val repairedLines = SubtitleParserAndMerger.parseAiTranslatedLines(
                            pastedText = retryResult.getOrThrow(),
                            expectedCues = retryWindowCues
                        )
                        for (missingCue in missingInChunk) {
                            repairedLines[missingCue.id]?.let { parsedChunk[missingCue.id] = it }
                        }
                    }
                }

                // For any cue that is purely non-translatable symbols/punctuation/numbers (e.g. "...", "♪", "?!")
                // and was omitted by the model, preserve its cleanText so valid files never fail over a "♪" cue.
                for (cue in chunkCues) {
                    if (!parsedChunk.containsKey(cue.id) &&
                        BatchTranslationValidator.isNonTranslatableSymbolOrNumberCue(cue.cleanText)
                    ) {
                        parsedChunk[cue.id] = cue.cleanText.trim().ifEmpty { "..." }
                    }
                }

                val processedChunk = applyPostProcessing(parsedChunk, config)
                accumulatedMap.putAll(processedChunk)

                onChunkTranslated(
                    chunkIdx,
                    allChunks.size,
                    processedChunk,
                    LinkedHashMap(accumulatedMap)
                )
            }

            val finalTranslations = applyPostProcessing(accumulatedMap, config)
            val mergedContent = reconstructMergedContent(
                document = sourceDocument,
                translations = finalTranslations,
                config = config
            )
            val validationResult = validateTranslation(
                document = sourceDocument,
                translations = finalTranslations,
                mergedContent = mergedContent,
                config = config
            )
            val report = SingleTranslationPipelineExecutionReport(
                fileName = sourceDocument.fileName,
                configSnapshot = config,
                systemPromptUsed = systemPrompt,
                chunkSizes = targetIndices.map { allChunks[it].size },
                totalCues = sourceDocument.cues.size,
                translatedCuesCount = finalTranslations.size,
                chunkRetryCount = totalChunkRetries,
                mergedContent = mergedContent,
                validationResult = validationResult
            )

            SingleTranslationPipelineResult(
                translatedCues = finalTranslations,
                mergedContent = mergedContent,
                validationResult = validationResult,
                report = report
            )
        }.flattenNestedResult()
    }

    /**
     * Creates a [BatchCueTranslator] adapter that executes [SingleTranslationPipeline.executePipeline]
     * for every file in the Batch Queue.
     * This guarantees 100% Pipeline Identity between Single Translation and Batch Translation.
     */
    fun createBatchCueTranslator(
        configProvider: () -> SingleTranslationConfig,
        aiGateway: SingleTranslationAiGateway = ProductionSingleTranslationAiGateway,
        shouldAbort: () -> Boolean = { false },
        onPipelineReport: (SingleTranslationPipelineExecutionReport) -> Unit = {}
    ): BatchCueTranslator {
        return BatchCueTranslator { _, sourceDocument, _, onCueProgress ->
            val config = configProvider()
            val result = executePipeline(
                sourceDocument = sourceDocument,
                config = config,
                aiGateway = aiGateway,
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

