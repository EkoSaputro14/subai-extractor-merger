package com.example.viewmodel

import android.app.Application
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.data.AndroidBatchOutputWriter
import com.example.data.AppUpdateChecker
import com.example.data.AppUpdateUiState
import com.example.data.BatchNotificationHelper
import com.example.data.GeminiWebCookieClient
import com.example.data.OpenAiCompatibleClient
import com.example.data.ParsedGeminiCookies
import com.example.data.PreferencesRepository
import com.example.domain.BatchCueTranslator
import com.example.domain.BatchFileCandidate
import com.example.domain.BatchQueueEngine
import com.example.domain.BatchTranslationAttemptOutput
import com.example.domain.SampleSubtitles
import com.example.domain.SingleTranslationAiGateway
import com.example.domain.SingleTranslationConfig
import com.example.domain.SingleTranslationPipeline
import com.example.domain.SubtitleParserAndMerger
import com.example.model.AiProviderPreset
import com.example.model.AiProviderSettings
import com.example.model.BatchJobStatus
import com.example.model.BatchQueueState
import com.example.model.BatchSizeOption
import com.example.model.BilingualOrder
import com.example.model.ColorPreset
import com.example.model.ExtractionRulePreset
import com.example.model.FileConflictPolicy
import com.example.model.FindMatchItem
import com.example.model.GlossaryRule
import com.example.model.IndexFormatMode
import com.example.model.LineSeparatorMode
import com.example.model.MergeCompareItem
import com.example.model.SubtitleCue
import com.example.model.SubtitleDocument
import com.example.model.TargetLanguageOption
import com.example.model.ThemeMode
import com.example.model.ThemeSettings
import com.example.model.TranslationStylePreset
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.util.UUID

data class SubtitleUiState(
    val document: SubtitleDocument,
    val isSampleLoaded: Boolean = true,
    // Tab 1: Extract & AI Prompt
    val extractionPreset: ExtractionRulePreset = ExtractionRulePreset.STANDAR,
    val separatorMode: LineSeparatorMode = LineSeparatorMode.DOUBLE_SLASH,
    val customSeparator: String = " // ",
    val protectAssTags: Boolean = true,
    val includeStyleAndActor: Boolean = false,
    val mergeConsecutiveDialogues: Boolean = true,
    val removeComments: Boolean = true,
    val ignoreEmptyLines: Boolean = true,
    val indexFormatMode: IndexFormatMode = IndexFormatMode.BRACKET,
    val batchSizeOption: BatchSizeOption = BatchSizeOption.BATCH_100,
    val selectedBatchIndex: Int = 0,
    val includeAiPromptHeader: Boolean = true,
    val includeGlossaryInPrompt: Boolean = true,
    val targetLanguageOption: TargetLanguageOption = TargetLanguageOption.INDONESIAN,
    val customTargetLanguage: String = "Bahasa Indonesia",
    val stylePreset: TranslationStylePreset = TranslationStylePreset.ANIME_CASUAL,
    val customPromptInstruction: String = TranslationStylePreset.ANIME_CASUAL.styleInstruction,
    // In-App Gemini-Web2API & OpenAI-Compatible Translation Status + Realtime Card Animation
    val isFetchingModels: Boolean = false,
    val isVerifyingCookie: Boolean = false,
    val isTranslatingAi: Boolean = false,
    val aiTranslationStatusText: String? = null,
    val aiCompletedBatches: Int = 0,
    val aiTotalBatches: Int = 0,
    val translatingCueIds: Set<Int> = emptySet(),
    val recentlyTranslatedCueIds: Set<Int> = emptySet(),
    // Tab 2: Paste AI
    val pastedEditorText: String = "",
    val parsedTranslations: Map<Int, String> = emptyMap(),
    val autoApplyGlossaryBeforeMerge: Boolean = false,
    // Tab 3: Find & Replace + Glossary
    val glossarySubTabIndex: Int = 0, // 0 = Quick Find & Replace, 1 = Saved Glossary Rules
    val findQuery: String = "",
    val replaceWith: String = "",
    val findCaseSensitive: Boolean = false,
    val findWholeWord: Boolean = false,
    val findUseRegex: Boolean = false,
    val selectedStyleFilter: String = "ALL",
    val selectedActorFilter: String = "ALL",
    val undoTranslationsSnapshot: Map<Int, String>? = null,
    val lastActionBannerMessage: String? = null,
    // Tab 4: Merge & Save
    val restoreLeadingAssTags: Boolean = true,
    val bilingualMode: Boolean = false,
    val bilingualOrder: BilingualOrder = BilingualOrder.TRANSLATION_TOP,
    val bilingualAssSecondaryStyling: Boolean = true,
    val showRawMergedPreview: Boolean = false
) {
    val batches: List<List<SubtitleCue>>
        get() = SingleTranslationPipeline.chunkCues(document.cues, batchSizeOption)

    val activeBatchCues: List<SubtitleCue>
        get() {
            val allBatches = batches
            if (allBatches.isEmpty()) return emptyList()
            val safeIdx = selectedBatchIndex.coerceIn(0, allBatches.lastIndex)
            return allBatches[safeIdx]
        }

    val matchedCount: Int
        get() = document.cues.count { parsedTranslations.containsKey(it.id) }

    val missingCueIds: List<Int>
        get() = document.cues.map { it.id }.filter { !parsedTranslations.containsKey(it) }

    fun toSingleTranslationConfig(
        aiSettings: AiProviderSettings,
        glossaryRules: List<GlossaryRule>,
        maxRetries: Int = 3
    ): SingleTranslationConfig {
        return SingleTranslationConfig(
            aiSettings = aiSettings,
            sourceLanguage = "Auto",
            targetLanguageOption = targetLanguageOption,
            customTargetLanguage = customTargetLanguage,
            stylePreset = stylePreset,
            customPromptInstruction = customPromptInstruction,
            separatorMode = separatorMode,
            customSeparator = customSeparator,
            protectAssTags = protectAssTags,
            indexFormatMode = indexFormatMode,
            batchSizeOption = batchSizeOption,
            includeAiPromptHeader = includeAiPromptHeader,
            includeGlossaryInPrompt = includeGlossaryInPrompt,
            glossaryRules = glossaryRules,
            autoApplyGlossaryBeforeMerge = autoApplyGlossaryBeforeMerge,
            restoreLeadingAssTags = restoreLeadingAssTags,
            bilingualMode = bilingualMode,
            bilingualOrder = bilingualOrder,
            bilingualAssSecondaryStyling = bilingualAssSecondaryStyling,
            maxRetries = maxRetries,
            maxChunkRepairRetries = 1,
            checkUntranslatedSourceLeakage = true
        )
    }
}

class SubtitleViewModel(application: Application) : AndroidViewModel(application) {

    private val prefsRepo = PreferencesRepository(application.applicationContext)

    private val _themeSettings = MutableStateFlow(prefsRepo.loadThemeSettings())
    val themeSettings: StateFlow<ThemeSettings> = _themeSettings.asStateFlow()

    private val _aiSettings = MutableStateFlow(prefsRepo.loadAiProviderSettings())
    val aiSettings: StateFlow<AiProviderSettings> = _aiSettings.asStateFlow()

    private val _glossaryRules = MutableStateFlow(prefsRepo.loadGlossaryRules())
    val glossaryRules: StateFlow<List<GlossaryRule>> = _glossaryRules.asStateFlow()

    private val notificationHelper = BatchNotificationHelper(application.applicationContext)

    private val defaultBatchOutputDirectory: File by lazy {
        val baseDir = application.applicationContext.getExternalFilesDir(null)
            ?: application.applicationContext.filesDir
        File(baseDir, "Translated_Subtitles").apply { mkdirs() }
    }

    private val _batchQueueState = MutableStateFlow(loadAndRecoverInitialBatchQueueState())
    val batchQueueState: StateFlow<BatchQueueState> = _batchQueueState.asStateFlow()

    private val _appUpdateState = MutableStateFlow(
        AppUpdateUiState(
            currentVersionName = BuildConfig.VERSION_NAME,
            currentVersionCode = BuildConfig.VERSION_CODE,
            repoOwner = BuildConfig.GITHUB_REPO_OWNER,
            repoName = BuildConfig.GITHUB_REPO_NAME
        )
    )
    val appUpdateState: StateFlow<AppUpdateUiState> = _appUpdateState.asStateFlow()

    private var activeTranslationJob: Job? = null
    private var activeBatchQueueJob: Job? = null
    @Volatile
    private var batchPauseRequested: Boolean = false
    @Volatile
    private var batchCancelRequested: Boolean = false

    private val initialDoc = SubtitleParserAndMerger.parseSubtitle(
        fileName = SampleSubtitles.SAMPLE_ASS_FILENAME,
        rawContent = SampleSubtitles.SAMPLE_ASS_CONTENT
    )
    private val initialDemoTranslations = SubtitleParserAndMerger.parseAiTranslatedLines(
        SampleSubtitles.SAMPLE_ASS_AI_TRANSLATION_DEMO
    )

    private val _uiState = MutableStateFlow(
        SubtitleUiState(
            document = initialDoc,
            isSampleLoaded = true,
            pastedEditorText = SampleSubtitles.SAMPLE_ASS_AI_TRANSLATION_DEMO,
            parsedTranslations = initialDemoTranslations,
            findQuery = "Shibuya Ward",
            replaceWith = "Distrik Shibuya"
        )
    )
    val uiState: StateFlow<SubtitleUiState> = _uiState.asStateFlow()

    // --- Gemini-Web2API Cookie & OpenAI-Compatible Provider Controls ---

    fun selectAiProviderPreset(preset: AiProviderPreset) {
        val current = _aiSettings.value
        val updated = current.copy(
            preset = preset,
            baseUrl = if (preset == AiProviderPreset.CUSTOM) current.baseUrl else preset.defaultBaseUrl,
            selectedModel = preset.defaultModel,
            fetchedModels = emptyList()
        )
        _aiSettings.value = updated
        prefsRepo.saveAiProviderSettings(updated)
    }

    fun updateAiBaseUrl(baseUrl: String) {
        val updated = _aiSettings.value.copy(baseUrl = baseUrl)
        _aiSettings.value = updated
        prefsRepo.saveAiProviderSettings(updated)
    }

    fun updateAiApiKey(apiKey: String) {
        val updated = _aiSettings.value.copy(apiKey = apiKey)
        _aiSettings.value = updated
        prefsRepo.saveAiProviderSettings(updated)
    }

    fun updateGeminiSecure1Psid(psid: String) {
        val updated = _aiSettings.value.copy(geminiSecure1Psid = psid)
        _aiSettings.value = updated
        prefsRepo.saveAiProviderSettings(updated)
    }

    fun updateGeminiSecure1Psidts(psidts: String) {
        val updated = _aiSettings.value.copy(geminiSecure1Psidts = psidts)
        _aiSettings.value = updated
        prefsRepo.saveAiProviderSettings(updated)
    }

    fun updateCombineSystemIntoUserPrompt(combine: Boolean) {
        val updated = _aiSettings.value.copy(combineSystemIntoUserPrompt = combine)
        _aiSettings.value = updated
        prefsRepo.saveAiProviderSettings(updated)
    }

    fun applySmartPastedCookies(rawInput: String) {
        val parsed = GeminiWebCookieClient.parseSmartCookieInput(rawInput)
        if (!parsed.isValid && parsed.secure1Psidts.isBlank()) {
            _uiState.update {
                it.copy(
                    lastActionBannerMessage = "Format cookie tidak ditemukan di Clipboard. Salin dari Cookie-Editor (JSON), header Cookie, atau nilai __Secure-1PSID."
                )
            }
            return
        }
        val current = _aiSettings.value
        val updated = current.copy(
            geminiSecure1Psid = parsed.secure1Psid.ifBlank { current.geminiSecure1Psid },
            geminiSecure1Psidts = parsed.secure1Psidts.ifBlank { current.geminiSecure1Psidts }
        )
        _aiSettings.value = updated
        prefsRepo.saveAiProviderSettings(updated)
        val psidNote = if (parsed.secure1Psid.isNotBlank()) "__Secure-1PSID" else ""
        val psidtsNote = if (parsed.secure1Psidts.isNotBlank()) " + __Secure-1PSIDTS" else ""
        _uiState.update {
            it.copy(
                lastActionBannerMessage = "Smart Paste berhasil mengisi cookie $psidNote$psidtsNote!"
            )
        }
    }

    fun applyExtractedCookies(cookies: ParsedGeminiCookies) {
        val current = _aiSettings.value
        val updated = current.copy(
            geminiSecure1Psid = cookies.secure1Psid.ifBlank { current.geminiSecure1Psid },
            geminiSecure1Psidts = cookies.secure1Psidts.ifBlank { current.geminiSecure1Psidts }
        )
        _aiSettings.value = updated
        prefsRepo.saveAiProviderSettings(updated)
        _uiState.update {
            it.copy(
                lastActionBannerMessage = "Cookie Gemini Web (__Secure-1PSID & __Secure-1PSIDTS) berhasil disimpan otomatis!"
            )
        }
    }

    fun verifyGeminiCookiesNow() {
        val current = _aiSettings.value
        if (_uiState.value.isVerifyingCookie) return
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isVerifyingCookie = true,
                    aiTranslationStatusText = "Memverifikasi cookie __Secure-1PSID ke gemini.google.com..."
                )
            }
            val result = GeminiWebCookieClient.verifyCookies(
                psid = current.geminiSecure1Psid,
                psidts = current.geminiSecure1Psidts
            )
            result.onSuccess { msg ->
                _uiState.update {
                    it.copy(
                        isVerifyingCookie = false,
                        aiTranslationStatusText = msg,
                        lastActionBannerMessage = msg
                    )
                }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        isVerifyingCookie = false,
                        aiTranslationStatusText = "Verifikasi gagal: ${err.localizedMessage}",
                        lastActionBannerMessage = "Verifikasi Cookie gagal: ${err.localizedMessage}"
                    )
                }
            }
        }
    }

    fun selectAiModel(modelId: String) {
        val updated = _aiSettings.value.copy(selectedModel = modelId)
        _aiSettings.value = updated
        prefsRepo.saveAiProviderSettings(updated)
    }

    fun fetchModelsFromProvider() {
        val settings = _aiSettings.value
        if (_uiState.value.isFetchingModels) return

        viewModelScope.launch {
            _uiState.update { it.copy(isFetchingModels = true) }

            val result = if (settings.preset == AiProviderPreset.GEMINI_WEB_COOKIE) {
                GeminiWebCookieClient.fetchModelsForCookieMode(
                    psid = settings.geminiSecure1Psid,
                    psidts = settings.geminiSecure1Psidts
                )
            } else {
                OpenAiCompatibleClient.fetchModels(
                    baseUrl = settings.baseUrl,
                    apiKey = settings.apiKey,
                    geminiSecure1Psid = settings.geminiSecure1Psid,
                    geminiSecure1Psidts = settings.geminiSecure1Psidts
                )
            }

            result.onSuccess { models ->
                val newSelected = if (settings.selectedModel in models) {
                    settings.selectedModel
                } else {
                    models.firstOrNull() ?: settings.selectedModel
                }
                val updated = _aiSettings.value.copy(
                    fetchedModels = models,
                    selectedModel = newSelected
                )
                _aiSettings.value = updated
                prefsRepo.saveAiProviderSettings(updated)
                val sourceLabel = if (settings.preset == AiProviderPreset.GEMINI_WEB_COOKIE) {
                    if (settings.hasGeminiCookie) "sesi Cookie Gemini Web" else "katalog Gemini-Web2API"
                } else {
                    settings.preset.label
                }
                _uiState.update {
                    it.copy(
                        isFetchingModels = false,
                        lastActionBannerMessage = "Berhasil mengambil ${models.size} model dari $sourceLabel (termasuk ${models.firstOrNull() ?: newSelected})!"
                    )
                }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        isFetchingModels = false,
                        lastActionBannerMessage = "Gagal Fetch Model: ${err.localizedMessage ?: "Periksa Base URL / Cookie / API Key"}"
                    )
                }
            }
        }
    }

    fun buildActiveSingleTranslationConfig(): SingleTranslationConfig {
        return _uiState.value.toSingleTranslationConfig(
            aiSettings = _aiSettings.value,
            glossaryRules = _glossaryRules.value,
            maxRetries = _batchQueueState.value.maxRetries
        )
    }

    fun buildActiveModelSystemPrompt(): String {
        return SingleTranslationPipeline.buildSystemPrompt(buildActiveSingleTranslationConfig())
    }

    fun translateCurrentBatchWithAi() {
        val currentBatchIdx = _uiState.value.selectedBatchIndex
        launchBatchTranslationJob(batchIndices = listOf(currentBatchIdx))
    }

    fun translateAllBatchesWithAi() {
        val totalBatches = _uiState.value.batches.size
        if (totalBatches == 0) return
        launchBatchTranslationJob(batchIndices = (0 until totalBatches).toList())
    }

    fun cancelAiTranslation() {
        activeTranslationJob?.cancel()
        activeTranslationJob = null
        _uiState.update {
            it.copy(
                isTranslatingAi = false,
                translatingCueIds = emptySet(),
                aiTranslationStatusText = "Terjemahan AI dihentikan oleh pengguna.",
                lastActionBannerMessage = "Proses terjemahan AI dihentikan."
            )
        }
    }

    /**
     * Updates a single subtitle cue's translation directly from the Dual-Layer Realtime Card.
     */
    fun updateSingleCueTranslation(cueId: Int, newTranslatedText: String) {
        val currentState = _uiState.value
        val updatedMap = LinkedHashMap(currentState.parsedTranslations).apply {
            if (newTranslatedText.isBlank()) {
                remove(cueId)
            } else {
                put(cueId, newTranslatedText.trim())
            }
        }
        val formattedText = SubtitleParserAndMerger.formatTranslationsMapToEditorText(
            updatedMap,
            currentState.indexFormatMode
        )
        _uiState.update {
            it.copy(
                parsedTranslations = updatedMap,
                pastedEditorText = formattedText
            )
        }
    }

    /**
     * Replays or simulates a live, line-by-line realtime translation animation on the active batch cards
     * so the user can visually inspect the original language on top and the animated translated language below.
     */
    fun simulateRealtimeTranslationOnActiveBatch() {
        if (_uiState.value.isTranslatingAi) return

        activeTranslationJob = viewModelScope.launch {
            val sInitial = _uiState.value
            val activeBatch = sInitial.activeBatchCues
            if (activeBatch.isEmpty()) return@launch

            val batchCueIds = activeBatch.map { it.id }.toSet()
            val demoPool = if (sInitial.document.fileName == SampleSubtitles.SAMPLE_SRT_FILENAME) {
                SubtitleParserAndMerger.parseAiTranslatedLines(SampleSubtitles.SAMPLE_SRT_AI_TRANSLATION_DEMO)
            } else {
                SubtitleParserAndMerger.parseAiTranslatedLines(SampleSubtitles.SAMPLE_ASS_AI_TRANSLATION_DEMO)
            }

            // Prepare target translations for each cue in the active batch
            val targetBatchMap = linkedMapOf<Int, String>()
            for (cue in activeBatch) {
                val existingOrDemo = sInitial.parsedTranslations[cue.id]
                    ?: demoPool[cue.id]
                    ?: cue.cleanText.replace("\\N", " // ").replace("\n", " // ")
                targetBatchMap[cue.id] = existingOrDemo
            }

            val glossaryAdjusted = if (sInitial.autoApplyGlossaryBeforeMerge) {
                SubtitleParserAndMerger.applyGlossaryRulesToTranslations(
                    targetBatchMap,
                    _glossaryRules.value
                ).first
            } else {
                targetBatchMap
            }

            // Step 1: Clear active batch translations briefly and mark all cues in batch as translating
            val mapWithoutActiveBatch = LinkedHashMap(sInitial.parsedTranslations).apply {
                batchCueIds.forEach { remove(it) }
            }
            _uiState.update {
                it.copy(
                    isTranslatingAi = true,
                    aiCompletedBatches = 0,
                    aiTotalBatches = 1,
                    parsedTranslations = mapWithoutActiveBatch,
                    translatingCueIds = batchCueIds,
                    recentlyTranslatedCueIds = emptySet(),
                    aiTranslationStatusText = "Animasi Penerjemahan Realtime berjalan pada ${activeBatch.size} kartu baris..."
                )
            }

            try {
                delay(320L)
                val progressiveMap = LinkedHashMap(mapWithoutActiveBatch)
                val remainingTranslating = batchCueIds.toMutableSet()
                val newlyRevealed = linkedSetOf<Int>()

                for ((idx, cue) in activeBatch.withIndex()) {
                    val translatedLine = glossaryAdjusted[cue.id] ?: continue
                    progressiveMap[cue.id] = translatedLine
                    remainingTranslating.remove(cue.id)
                    newlyRevealed.add(cue.id)

                    val editorText = SubtitleParserAndMerger.formatTranslationsMapToEditorText(
                        progressiveMap,
                        _uiState.value.indexFormatMode
                    )
                    _uiState.update {
                        it.copy(
                            parsedTranslations = LinkedHashMap(progressiveMap),
                            pastedEditorText = editorText,
                            translatingCueIds = remainingTranslating.toSet(),
                            recentlyTranslatedCueIds = newlyRevealed.toSet(),
                            aiTranslationStatusText = "Menerjemahkan baris #${cue.id} (${idx + 1}/${activeBatch.size}) secara realtime..."
                        )
                    }
                    delay(140L)
                }

                _uiState.update {
                    it.copy(
                        isTranslatingAi = false,
                        aiCompletedBatches = 1,
                        translatingCueIds = emptySet(),
                        aiTranslationStatusText = "Animasi selesai! ${activeBatch.size} baris ditampilkan di bawah teks bahasa asli."
                    )
                }
            } catch (_: CancellationException) {
                // Cancelled
            }
        }
    }

    private fun launchBatchTranslationJob(batchIndices: List<Int>) {
        if (_uiState.value.isTranslatingAi || batchIndices.isEmpty()) return

        activeTranslationJob = viewModelScope.launch {
            val sInitial = _uiState.value
            val totalToRun = batchIndices.size

            _uiState.update {
                it.copy(
                    isTranslatingAi = true,
                    aiCompletedBatches = 0,
                    aiTotalBatches = totalToRun,
                    recentlyTranslatedCueIds = emptySet(),
                    undoTranslationsSnapshot = it.parsedTranslations.takeIf { m -> m.isNotEmpty() },
                    aiTranslationStatusText = "Menyiapkan terjemahan dengan model ${_aiSettings.value.selectedModel}..."
                )
            }

            try {
                var completedStepCount = 0
                val config = buildActiveSingleTranslationConfig()
                val pipelineResult = SingleTranslationPipeline.executePipeline(
                    sourceDocument = sInitial.document,
                    config = config,
                    chunkIndicesToProcess = batchIndices,
                    initialTranslations = sInitial.parsedTranslations,
                    onChunkStarted = { chunkIndex, totalChunks, chunkCues ->
                        val batchCueIds = chunkCues.map { it.id }.toSet()
                        val firstId = chunkCues.firstOrNull()?.id ?: 1
                        val lastId = chunkCues.lastOrNull()?.id ?: chunkCues.size
                        val currentAi = _aiSettings.value
                        _uiState.update {
                            it.copy(
                                selectedBatchIndex = chunkIndex,
                                translatingCueIds = batchCueIds,
                                aiTranslationStatusText = "Menerjemahkan Batch ${chunkIndex + 1}/$totalChunks (#$firstId–#$lastId) via ${currentAi.selectedModel}..."
                            )
                        }
                    },
                    onChunkTranslated = { chunkIndex, _, processedNewLines, _ ->
                        val latestState = _uiState.value
                        val batchCueIds = latestState.translatingCueIds
                        val progressiveMap = LinkedHashMap(latestState.parsedTranslations)
                        val remainingTranslating = batchCueIds.toMutableSet()
                        val newlyRevealed = LinkedHashSet(latestState.recentlyTranslatedCueIds)
                        val delayPerLineMs = if (processedNewLines.size > 40) 20L else 45L

                        for ((cueId, translatedLine) in processedNewLines.entries) {
                            progressiveMap[cueId] = translatedLine
                            remainingTranslating.remove(cueId)
                            newlyRevealed.add(cueId)

                            val formattedEditorText = SubtitleParserAndMerger.formatTranslationsMapToEditorText(
                                progressiveMap,
                                _uiState.value.indexFormatMode
                            )
                            _uiState.update {
                                it.copy(
                                    parsedTranslations = LinkedHashMap(progressiveMap),
                                    pastedEditorText = formattedEditorText,
                                    translatingCueIds = remainingTranslating.toSet(),
                                    recentlyTranslatedCueIds = newlyRevealed.toSet(),
                                    aiTranslationStatusText = "Menampilkan terjemahan baris #$cueId secara realtime..."
                                )
                            }
                            delay(delayPerLineMs)
                        }

                        completedStepCount++
                        _uiState.update {
                            it.copy(
                                translatingCueIds = emptySet(),
                                aiCompletedBatches = completedStepCount,
                                aiTranslationStatusText = "Batch ${chunkIndex + 1} selesai (${processedNewLines.size} baris dipetakan)."
                            )
                        }
                    }
                )

                pipelineResult.onSuccess {
                    val finalMatched = _uiState.value.matchedCount
                    val totalCues = _uiState.value.document.cues.size
                    _uiState.update {
                        it.copy(
                            isTranslatingAi = false,
                            translatingCueIds = emptySet(),
                            aiTranslationStatusText = "Selesai! $finalMatched / $totalCues baris dialog berhasil diterjemahkan & siap di-Merge.",
                            lastActionBannerMessage = "Terjemahan AI selesai ($finalMatched/$totalCues baris cocok)!"
                        )
                    }
                }.onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isTranslatingAi = false,
                            translatingCueIds = emptySet(),
                            aiTranslationStatusText = "Terjemahan AI gagal: ${error.localizedMessage}",
                            lastActionBannerMessage = "Terjemahan AI gagal: ${error.localizedMessage}"
                        )
                    }
                }
            } catch (_: CancellationException) {
                // Handled in cancelAiTranslation
            }
        }
    }

    // --- File Loading & Samples ---

    fun loadSampleAss() {
        activeTranslationJob?.cancel()
        activeTranslationJob = null
        val doc = SubtitleParserAndMerger.parseSubtitle(
            fileName = SampleSubtitles.SAMPLE_ASS_FILENAME,
            rawContent = SampleSubtitles.SAMPLE_ASS_CONTENT
        )
        val demoMap = SubtitleParserAndMerger.parseAiTranslatedLines(
            SampleSubtitles.SAMPLE_ASS_AI_TRANSLATION_DEMO
        )
        _uiState.update {
            it.copy(
                document = doc,
                isSampleLoaded = true,
                selectedBatchIndex = 0,
                isTranslatingAi = false,
                translatingCueIds = emptySet(),
                recentlyTranslatedCueIds = emptySet(),
                pastedEditorText = SampleSubtitles.SAMPLE_ASS_AI_TRANSLATION_DEMO,
                parsedTranslations = demoMap,
                selectedStyleFilter = "ALL",
                selectedActorFilter = "ALL",
                undoTranslationsSnapshot = null,
                lastActionBannerMessage = "Sampel .ASS Anime berhasil dimuat (${doc.cues.size} baris dialog)"
            )
        }
    }

    fun loadSampleSrt() {
        activeTranslationJob?.cancel()
        activeTranslationJob = null
        val doc = SubtitleParserAndMerger.parseSubtitle(
            fileName = SampleSubtitles.SAMPLE_SRT_FILENAME,
            rawContent = SampleSubtitles.SAMPLE_SRT_CONTENT
        )
        val demoMap = SubtitleParserAndMerger.parseAiTranslatedLines(
            SampleSubtitles.SAMPLE_SRT_AI_TRANSLATION_DEMO
        )
        _uiState.update {
            it.copy(
                document = doc,
                isSampleLoaded = true,
                selectedBatchIndex = 0,
                isTranslatingAi = false,
                translatingCueIds = emptySet(),
                recentlyTranslatedCueIds = emptySet(),
                pastedEditorText = SampleSubtitles.SAMPLE_SRT_AI_TRANSLATION_DEMO,
                parsedTranslations = demoMap,
                selectedStyleFilter = "ALL",
                selectedActorFilter = "ALL",
                undoTranslationsSnapshot = null,
                lastActionBannerMessage = "Sampel .SRT Film berhasil dimuat (${doc.cues.size} baris dialog)"
            )
        }
    }

    fun loadSubtitleFromUri(uri: Uri) {
        val context = getApplication<Application>().applicationContext
        try {
            activeTranslationJob?.cancel()
            activeTranslationJob = null
            val fileName = resolveFileName(uri) ?: "subtitle.ass"
            val content = context.contentResolver.openInputStream(uri)?.use { stream ->
                BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).readText()
            } ?: return

            val cleanContent = content.removePrefix("\uFEFF")
            val parsedDoc = SubtitleParserAndMerger.parseSubtitle(fileName, cleanContent)

            _uiState.update {
                it.copy(
                    document = parsedDoc,
                    isSampleLoaded = false,
                    selectedBatchIndex = 0,
                    isTranslatingAi = false,
                    translatingCueIds = emptySet(),
                    recentlyTranslatedCueIds = emptySet(),
                    pastedEditorText = "",
                    parsedTranslations = emptyMap(),
                    selectedStyleFilter = "ALL",
                    selectedActorFilter = "ALL",
                    undoTranslationsSnapshot = null,
                    lastActionBannerMessage = "File $fileName dimuat (${parsedDoc.cues.size} dialog terdeteksi)"
                )
            }
        } catch (e: Exception) {
            _uiState.update {
                it.copy(lastActionBannerMessage = "Gagal membaca file: ${e.localizedMessage ?: "Format tidak dikenali"}")
            }
        }
    }

    fun saveMergedSubtitleToUri(uri: Uri): Boolean {
        val context = getApplication<Application>().applicationContext
        return try {
            val mergedContent = buildFinalMergedFileContent()
            context.contentResolver.openOutputStream(uri, "wt")?.use { outStream ->
                OutputStreamWriter(outStream, Charsets.UTF_8).use { writer ->
                    writer.write(mergedContent)
                }
            }
            _uiState.update {
                it.copy(lastActionBannerMessage = "File hasil Merge berhasil disimpan ke perangkat!")
            }
            true
        } catch (e: Exception) {
            _uiState.update {
                it.copy(lastActionBannerMessage = "Gagal menyimpan file: ${e.localizedMessage}")
            }
            false
        }
    }

    private fun resolveFileName(uri: Uri): String? {
        val context = getApplication<Application>().applicationContext
        var name: String? = null
        if (uri.scheme == "content") {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (idx >= 0) {
                        name = cursor.getString(idx)
                    }
                }
            }
        }
        return name ?: uri.lastPathSegment?.substringAfterLast('/')
    }

    // --- Tab 1: Extract & Copy Controls ---

    fun selectExtractionPreset(preset: ExtractionRulePreset) {
        _uiState.update {
            it.copy(
                extractionPreset = preset,
                protectAssTags = preset.keepAssTags,
                includeStyleAndActor = preset.includeStyleActor,
                mergeConsecutiveDialogues = preset.mergeConsecutive,
                removeComments = preset.removeComments,
                ignoreEmptyLines = preset.ignoreEmptyLines
            )
        }
    }

    fun setSeparatorMode(mode: LineSeparatorMode) {
        _uiState.update { it.copy(separatorMode = mode) }
    }

    fun setCustomSeparator(custom: String) {
        _uiState.update { it.copy(customSeparator = custom) }
    }

    fun setProtectAssTags(protect: Boolean) {
        _uiState.update { it.copy(protectAssTags = protect) }
    }

    fun setIncludeStyleAndActor(include: Boolean) {
        _uiState.update { it.copy(includeStyleAndActor = include) }
    }

    fun setMergeConsecutiveDialogues(merge: Boolean) {
        _uiState.update { it.copy(mergeConsecutiveDialogues = merge) }
    }

    fun setRemoveComments(remove: Boolean) {
        _uiState.update { it.copy(removeComments = remove) }
    }

    fun setIgnoreEmptyLines(ignore: Boolean) {
        _uiState.update { it.copy(ignoreEmptyLines = ignore) }
    }

    fun setIndexFormatMode(mode: IndexFormatMode) {
        _uiState.update { it.copy(indexFormatMode = mode) }
    }

    fun setBatchSizeOption(option: BatchSizeOption) {
        _uiState.update { it.copy(batchSizeOption = option, selectedBatchIndex = 0) }
    }

    fun selectBatchIndex(index: Int) {
        _uiState.update { it.copy(selectedBatchIndex = index) }
    }

    fun setIncludeAiPromptHeader(include: Boolean) {
        _uiState.update { it.copy(includeAiPromptHeader = include) }
    }

    fun setIncludeGlossaryInPrompt(include: Boolean) {
        _uiState.update { it.copy(includeGlossaryInPrompt = include) }
    }

    fun setTargetLanguageOption(option: TargetLanguageOption) {
        _uiState.update { it.copy(targetLanguageOption = option) }
    }

    fun setCustomTargetLanguage(lang: String) {
        _uiState.update { it.copy(customTargetLanguage = lang) }
    }

    fun setTranslationStylePreset(preset: TranslationStylePreset) {
        _uiState.update {
            it.copy(
                stylePreset = preset,
                customPromptInstruction = preset.styleInstruction
            )
        }
    }

    fun setCustomPromptInstruction(instruction: String) {
        _uiState.update { it.copy(customPromptInstruction = instruction) }
    }

    fun buildCurrentBatchOutput(withPromptHeader: Boolean): String {
        val s = _uiState.value
        val ai = _aiSettings.value
        return SubtitleParserAndMerger.buildAiPromptAndExtractedText(
            cuesSlice = s.activeBatchCues,
            separatorMode = s.separatorMode,
            customSeparator = s.customSeparator,
            protectAssTags = s.protectAssTags,
            indexFormatMode = s.indexFormatMode,
            includeAiPromptHeader = withPromptHeader,
            includeGlossaryInPrompt = s.includeGlossaryInPrompt,
            targetLanguageOption = s.targetLanguageOption,
            customTargetLanguage = s.customTargetLanguage,
            stylePreset = s.stylePreset,
            customPromptInstruction = s.customPromptInstruction,
            glossaryRules = _glossaryRules.value,
            selectedModel = ai.selectedModel
        )
    }

    // --- Tab 2: Paste AI & Batch Append Controls ---

    fun updatePastedEditorText(newText: String) {
        var parsed = SubtitleParserAndMerger.parseAiTranslatedLines(newText)
        if (_uiState.value.autoApplyGlossaryBeforeMerge && parsed.isNotEmpty()) {
            parsed = SubtitleParserAndMerger.applyGlossaryRulesToTranslations(
                parsed,
                _glossaryRules.value
            ).first
        }
        _uiState.update {
            it.copy(
                pastedEditorText = newText,
                parsedTranslations = parsed
            )
        }
    }

    fun pasteOrAppendBatch(clipboardText: String, appendMode: Boolean) {
        if (clipboardText.isBlank()) {
            _uiState.update { it.copy(lastActionBannerMessage = "Clipboard kosong.") }
            return
        }
        val incomingParsed = SubtitleParserAndMerger.parseAiTranslatedLines(clipboardText)
        val currentState = _uiState.value

        val combinedMap = if (appendMode) {
            LinkedHashMap(currentState.parsedTranslations).apply {
                putAll(incomingParsed)
            }
        } else {
            LinkedHashMap(incomingParsed)
        }

        val finalMap = if (currentState.autoApplyGlossaryBeforeMerge) {
            SubtitleParserAndMerger.applyGlossaryRulesToTranslations(
                combinedMap,
                _glossaryRules.value
            ).first
        } else {
            combinedMap
        }

        val formattedText = if (finalMap.isNotEmpty()) {
            SubtitleParserAndMerger.formatTranslationsMapToEditorText(
                finalMap,
                currentState.indexFormatMode
            )
        } else {
            if (appendMode && currentState.pastedEditorText.isNotBlank()) {
                currentState.pastedEditorText + "\n" + clipboardText.trim()
            } else {
                clipboardText.trim()
            }
        }

        _uiState.update {
            it.copy(
                undoTranslationsSnapshot = currentState.parsedTranslations.takeIf { m -> m.isNotEmpty() },
                pastedEditorText = formattedText,
                parsedTranslations = finalMap,
                lastActionBannerMessage = if (appendMode) {
                    "Ditambahkan ${incomingParsed.size} baris baru (Total cocok: ${finalMap.size}/${it.document.cues.size})"
                } else {
                    "Ditempel ${incomingParsed.size} baris terjemahan AI"
                }
            )
        }
    }

    fun clearPastedTranslations() {
        val current = _uiState.value
        _uiState.update {
            it.copy(
                undoTranslationsSnapshot = current.parsedTranslations.takeIf { m -> m.isNotEmpty() },
                pastedEditorText = "",
                parsedTranslations = emptyMap(),
                lastActionBannerMessage = "Editor Paste AI dikosongkan"
            )
        }
    }

    fun fillDemoAiTranslations() {
        val current = _uiState.value
        val demoText = if (current.document.fileName == SampleSubtitles.SAMPLE_SRT_FILENAME) {
            SampleSubtitles.SAMPLE_SRT_AI_TRANSLATION_DEMO
        } else if (current.document.fileName == SampleSubtitles.SAMPLE_ASS_FILENAME) {
            SampleSubtitles.SAMPLE_ASS_AI_TRANSLATION_DEMO
        } else {
            current.document.cues.joinToString("\n") { cue ->
                SubtitleParserAndMerger.formatCueForExtraction(
                    cue = cue,
                    separatorMode = current.separatorMode,
                    customSeparator = current.customSeparator,
                    protectAssTags = true,
                    indexFormatMode = current.indexFormatMode
                )
            }
        }
        pasteOrAppendBatch(demoText, appendMode = false)
    }

    fun setAutoApplyGlossaryBeforeMerge(enabled: Boolean) {
        _uiState.update { state ->
            val updatedMap = if (enabled && state.parsedTranslations.isNotEmpty()) {
                SubtitleParserAndMerger.applyGlossaryRulesToTranslations(
                    state.parsedTranslations,
                    _glossaryRules.value
                ).first
            } else {
                state.parsedTranslations
            }
            val updatedText = if (enabled && updatedMap.isNotEmpty()) {
                SubtitleParserAndMerger.formatTranslationsMapToEditorText(updatedMap, state.indexFormatMode)
            } else {
                state.pastedEditorText
            }
            state.copy(
                autoApplyGlossaryBeforeMerge = enabled,
                parsedTranslations = updatedMap,
                pastedEditorText = updatedText
            )
        }
    }

    // --- Tab 3: Find & Replace & Glossary Rules ---

    fun setGlossarySubTab(index: Int) {
        _uiState.update { it.copy(glossarySubTabIndex = index) }
    }

    fun setFindQuery(query: String) {
        _uiState.update { it.copy(findQuery = query) }
    }

    fun setReplaceWith(replace: String) {
        _uiState.update { it.copy(replaceWith = replace) }
    }

    fun toggleFindCaseSensitive() {
        _uiState.update { it.copy(findCaseSensitive = !it.findCaseSensitive) }
    }

    fun toggleFindWholeWord() {
        _uiState.update { it.copy(findWholeWord = !it.findWholeWord) }
    }

    fun toggleFindUseRegex() {
        _uiState.update { it.copy(findUseRegex = !it.findUseRegex) }
    }

    fun setSelectedStyleFilter(style: String) {
        _uiState.update { it.copy(selectedStyleFilter = style) }
    }

    fun setSelectedActorFilter(actor: String) {
        _uiState.update { it.copy(selectedActorFilter = actor) }
    }

    fun computeLiveFindMatches(): List<FindMatchItem> {
        val s = _uiState.value
        return SubtitleParserAndMerger.findMatchesInTranslations(
            document = s.document,
            translations = s.parsedTranslations,
            findQuery = s.findQuery,
            replaceWith = s.replaceWith,
            caseSensitive = s.findCaseSensitive,
            wholeWord = s.findWholeWord,
            useRegex = s.findUseRegex,
            selectedStyleFilter = s.selectedStyleFilter,
            selectedActorFilter = s.selectedActorFilter
        )
    }

    fun replaceNextMatch() {
        val s = _uiState.value
        val matches = computeLiveFindMatches()
        val firstItem = matches.firstOrNull() ?: return
        val regex = SubtitleParserAndMerger.buildSearchRegex(
            query = s.findQuery,
            caseSensitive = s.findCaseSensitive,
            wholeWord = s.findWholeWord,
            useRegex = s.findUseRegex
        ) ?: return

        val currentLine = s.parsedTranslations[firstItem.cueId] ?: return
        val firstRegexMatch = regex.find(currentLine) ?: return
        val safeReplacement = if (s.findUseRegex) s.replaceWith else Regex.escapeReplacement(s.replaceWith)
        val updatedLine = runCatching {
            regex.replaceFirst(currentLine, safeReplacement)
        }.getOrElse {
            currentLine.replaceRange(firstRegexMatch.range, s.replaceWith)
        }

        val updatedMap = LinkedHashMap(s.parsedTranslations).apply {
            put(firstItem.cueId, updatedLine)
        }
        val updatedText = SubtitleParserAndMerger.formatTranslationsMapToEditorText(
            updatedMap,
            s.indexFormatMode
        )

        _uiState.update {
            it.copy(
                undoTranslationsSnapshot = s.parsedTranslations,
                parsedTranslations = updatedMap,
                pastedEditorText = updatedText,
                lastActionBannerMessage = "Diganti pada baris #${firstItem.cueId}"
            )
        }
    }

    fun replaceAllMatches() {
        val s = _uiState.value
        val matches = computeLiveFindMatches()
        if (matches.isEmpty()) return

        val updatedMap = LinkedHashMap(s.parsedTranslations)
        var totalOccurrenceCount = 0
        for (item in matches) {
            totalOccurrenceCount += item.matchRanges.size
            updatedMap[item.cueId] = item.previewReplacedText
        }

        val updatedText = SubtitleParserAndMerger.formatTranslationsMapToEditorText(
            updatedMap,
            s.indexFormatMode
        )

        _uiState.update {
            it.copy(
                undoTranslationsSnapshot = s.parsedTranslations,
                parsedTranslations = updatedMap,
                pastedEditorText = updatedText,
                lastActionBannerMessage = "Berhasil mengganti $totalOccurrenceCount kecocokan pada ${matches.size} baris dialog"
            )
        }
    }

    fun undoLastReplace() {
        val s = _uiState.value
        val snapshot = s.undoTranslationsSnapshot ?: return
        val restoredText = SubtitleParserAndMerger.formatTranslationsMapToEditorText(
            snapshot,
            s.indexFormatMode
        )
        _uiState.update {
            it.copy(
                parsedTranslations = snapshot,
                pastedEditorText = restoredText,
                undoTranslationsSnapshot = null,
                lastActionBannerMessage = "Penggantian terakhir telah diurungkan (Undo berhasil)"
            )
        }
    }

    fun addGlossaryRule(
        sourceTerm: String,
        targetTerm: String,
        isCaseSensitive: Boolean,
        wholeWord: Boolean
    ) {
        val cleanSource = sourceTerm.trim()
        val cleanTarget = targetTerm.trim()
        if (cleanSource.isEmpty()) return

        val newRule = GlossaryRule(
            id = "rule_${UUID.randomUUID()}",
            sourceTerm = cleanSource,
            targetTerm = cleanTarget,
            isCaseSensitive = isCaseSensitive,
            wholeWord = wholeWord,
            isEnabled = true
        )
        val updated = _glossaryRules.value + newRule
        _glossaryRules.value = updated
        prefsRepo.saveGlossaryRules(updated)
        _uiState.update {
            it.copy(lastActionBannerMessage = "Aturan kamus \"$cleanSource\" -> \"$cleanTarget\" disimpan")
        }
    }

    fun toggleGlossaryRuleEnabled(ruleId: String) {
        val updated = _glossaryRules.value.map {
            if (it.id == ruleId) it.copy(isEnabled = !it.isEnabled) else it
        }
        _glossaryRules.value = updated
        prefsRepo.saveGlossaryRules(updated)
    }

    fun deleteGlossaryRule(ruleId: String) {
        val updated = _glossaryRules.value.filterNot { it.id == ruleId }
        _glossaryRules.value = updated
        prefsRepo.saveGlossaryRules(updated)
    }

    fun applyAllGlossaryRulesNow() {
        val s = _uiState.value
        if (s.parsedTranslations.isEmpty()) {
            _uiState.update {
                it.copy(lastActionBannerMessage = "Belum ada teks terjemahan di Tab Paste AI untuk diterapkan glosarium.")
            }
            return
        }
        val (updatedMap, count) = SubtitleParserAndMerger.applyGlossaryRulesToTranslations(
            s.parsedTranslations,
            _glossaryRules.value
        )
        if (count == 0) {
            _uiState.update {
                it.copy(lastActionBannerMessage = "Semua istilah sudah sesuai glosarium (0 perubahan).")
            }
            return
        }
        val updatedText = SubtitleParserAndMerger.formatTranslationsMapToEditorText(
            updatedMap,
            s.indexFormatMode
        )
        _uiState.update {
            it.copy(
                undoTranslationsSnapshot = s.parsedTranslations,
                parsedTranslations = updatedMap,
                pastedEditorText = updatedText,
                lastActionBannerMessage = "Glosarium diterapkan: $count istilah diperbarui secara otomatis!"
            )
        }
    }

    // --- Tab 4: Merge & Save Controls ---

    fun setRestoreLeadingAssTags(restore: Boolean) {
        _uiState.update { it.copy(restoreLeadingAssTags = restore) }
    }

    fun setBilingualMode(enabled: Boolean) {
        _uiState.update { it.copy(bilingualMode = enabled) }
    }

    fun setBilingualOrder(order: BilingualOrder) {
        _uiState.update { it.copy(bilingualOrder = order) }
    }

    fun setBilingualAssSecondaryStyling(enabled: Boolean) {
        _uiState.update { it.copy(bilingualAssSecondaryStyling = enabled) }
    }

    fun setShowRawMergedPreview(showRaw: Boolean) {
        _uiState.update { it.copy(showRawMergedPreview = showRaw) }
    }

    fun buildFinalMergedFileContent(): String {
        val s = _uiState.value
        val config = buildActiveSingleTranslationConfig()
        return SingleTranslationPipeline.reconstructMergedContent(
            document = s.document,
            translations = s.parsedTranslations,
            config = config
        )
    }

    fun buildMergeCompareList(): List<MergeCompareItem> {
        val s = _uiState.value
        return SubtitleParserAndMerger.buildMergeCompareItems(
            document = s.document,
            translations = s.parsedTranslations,
            separatorMode = s.separatorMode,
            customSeparator = s.customSeparator,
            restoreLeadingAssTags = s.restoreLeadingAssTags,
            bilingualMode = s.bilingualMode,
            bilingualOrder = s.bilingualOrder,
            bilingualAssSecondaryStyling = s.bilingualAssSecondaryStyling
        )
    }

    fun clearBannerMessage() {
        _uiState.update { it.copy(lastActionBannerMessage = null) }
    }

    fun showBannerMessage(msg: String) {
        _uiState.update { it.copy(lastActionBannerMessage = msg) }
    }

    // --- Theme Settings Controls (F-06) ---

    fun updateThemeMode(mode: ThemeMode) {
        val updated = _themeSettings.value.copy(themeMode = mode)
        _themeSettings.value = updated
        prefsRepo.saveThemeSettings(updated)
    }

    fun updateColorPreset(preset: ColorPreset) {
        val updated = _themeSettings.value.copy(colorPreset = preset, useDynamicColor = false)
        _themeSettings.value = updated
        prefsRepo.saveThemeSettings(updated)
    }

    fun updateUseDynamicColor(enabled: Boolean) {
        val updated = _themeSettings.value.copy(useDynamicColor = enabled)
        _themeSettings.value = updated
        prefsRepo.saveThemeSettings(updated)
    }

    fun updatePureBlackAmoled(enabled: Boolean) {
        val updated = _themeSettings.value.copy(pureBlackAmoled = enabled)
        _themeSettings.value = updated
        prefsRepo.saveThemeSettings(updated)
    }

    fun updateEditorFontSizeSp(sizeSp: Int) {
        val updated = _themeSettings.value.copy(editorFontSizeSp = sizeSp.coerceIn(11, 17))
        _themeSettings.value = updated
        prefsRepo.saveThemeSettings(updated)
    }

    fun updateUseMonospaceFont(useMono: Boolean) {
        val updated = _themeSettings.value.copy(useMonospaceFont = useMono)
        _themeSettings.value = updated
        prefsRepo.saveThemeSettings(updated)
    }

    // --- Tab 5: Batch Subtitle Translation Queue ---

    private fun loadAndRecoverInitialBatchQueueState(): BatchQueueState {
        val defaultPath = defaultBatchOutputDirectory.absolutePath
        val defaultDisplay = "Translated_Subtitles ($defaultPath)"
        val persisted = prefsRepo.loadBatchQueueState(defaultPath)
            ?: return BatchQueueState(
                outputFolderUriOrPath = defaultPath,
                outputFolderDisplayName = defaultDisplay,
                isOutputFolderWritable = true
            )
        val writer = AndroidBatchOutputWriter(
            context = getApplication<Application>().applicationContext,
            folderUriOrPath = persisted.outputFolderUriOrPath.ifBlank { defaultPath }
        )
        val recovered = BatchQueueEngine.recoverPersistedBatchState(persisted, writer)
        prefsRepo.saveBatchQueueState(recovered)
        return recovered
    }

    private fun updateAndPersistBatchState(transform: (BatchQueueState) -> BatchQueueState) {
        _batchQueueState.update { current ->
            val next = transform(current)
            prefsRepo.saveBatchQueueState(next)
            next
        }
    }

    fun addUrisToBatchQueue(uris: List<Uri>) {
        if (uris.isEmpty()) return
        val context = getApplication<Application>().applicationContext
        val candidates = mutableListOf<BatchFileCandidate>()
        val readWarnings = mutableListOf<String>()

        for (uri in uris) {
            val fileName = resolveFileName(uri) ?: "subtitle_${uri.lastPathSegment ?: "unknown"}.srt"
            try {
                val rawText = context.contentResolver.openInputStream(uri)?.use { stream ->
                    BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).readText()
                }
                if (rawText == null) {
                    readWarnings.add("Dilewati '$fileName': Gagal membuka stream file.")
                } else {
                    candidates.add(
                        BatchFileCandidate(
                            fileName = fileName,
                            sourceUriString = uri.toString(),
                            rawContent = rawText.removePrefix("\uFEFF")
                        )
                    )
                }
            } catch (e: Exception) {
                readWarnings.add("Dilewati '$fileName': ${e.localizedMessage ?: "Tidak dapat dibaca"}")
            }
        }

        updateAndPersistBatchState { current ->
            val next = BatchQueueEngine.enqueueFiles(current, candidates)
            val allWarnings = readWarnings + next.preFlightWarnings
            next.copy(
                preFlightWarnings = allWarnings,
                statusBannerMessage = if (allWarnings.isNotEmpty()) {
                    "File ditambahkan (${next.totalFiles} dalam antrean, ${allWarnings.size} peringatan praperiksa)."
                } else {
                    "Berhasil menambahkan ${candidates.size} file ke antrean Batch Translation."
                }
            )
        }
    }

    fun addCandidatesToBatchQueue(candidates: List<BatchFileCandidate>) {
        updateAndPersistBatchState { current ->
            BatchQueueEngine.enqueueFiles(current, candidates)
        }
    }

    fun loadSampleEpisodeFilesToBatchQueue() {
        val sampleCandidates = SampleSubtitles.buildBatchEpisodeSampleCandidates()
        addCandidatesToBatchQueue(sampleCandidates)
    }

    fun moveBatchJobUp(jobId: String) {
        updateAndPersistBatchState { current ->
            BatchQueueEngine.moveJobUp(current, jobId)
        }
    }

    fun moveBatchJobDown(jobId: String) {
        updateAndPersistBatchState { current ->
            BatchQueueEngine.moveJobDown(current, jobId)
        }
    }

    fun removeBatchJob(jobId: String) {
        updateAndPersistBatchState { current ->
            BatchQueueEngine.removeJob(current, jobId)
        }
    }

    fun setBatchOutputFolderUri(treeUri: Uri) {
        val context = getApplication<Application>().applicationContext
        runCatching {
            val takeFlags = android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
                android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            context.contentResolver.takePersistableUriPermission(treeUri, takeFlags)
        }
        val uriString = treeUri.toString()
        val writer = AndroidBatchOutputWriter(context, uriString)
        val (writable, errMsg) = writer.isOutputFolderWritable()
        val readableSegment = treeUri.lastPathSegment?.substringAfterLast(':') ?: uriString
        updateAndPersistBatchState { current ->
            current.copy(
                outputFolderUriOrPath = uriString,
                outputFolderDisplayName = "SAF Folder: $readableSegment ($uriString)",
                isOutputFolderWritable = writable,
                outputFolderValidationMessage = errMsg,
                statusBannerMessage = if (writable) {
                    "Folder output batch dipilih: $readableSegment"
                } else {
                    errMsg ?: "Folder output tidak dapat ditulisi."
                }
            )
        }
    }

    fun resetBatchOutputFolderToDefault() {
        val defaultPath = defaultBatchOutputDirectory.absolutePath
        val writer = AndroidBatchOutputWriter(getApplication<Application>().applicationContext, defaultPath)
        val (writable, errMsg) = writer.isOutputFolderWritable()
        updateAndPersistBatchState { current ->
            current.copy(
                outputFolderUriOrPath = defaultPath,
                outputFolderDisplayName = "Translated_Subtitles ($defaultPath)",
                isOutputFolderWritable = writable,
                outputFolderValidationMessage = errMsg,
                statusBannerMessage = "Folder output batch dikembalikan ke Translated_Subtitles."
            )
        }
    }

    fun setBatchConflictPolicy(policy: FileConflictPolicy) {
        updateAndPersistBatchState { current ->
            current.copy(conflictPolicy = policy)
        }
    }

    fun setBatchMaxRetries(maxRetries: Int) {
        val coerced = maxRetries.coerceIn(1, 10)
        updateAndPersistBatchState { current ->
            current.copy(
                maxRetries = coerced,
                jobs = current.jobs.map { job ->
                    if (!job.status.isTerminal) job.copy(maxRetries = coerced) else job
                }
            )
        }
    }

    fun setBatchNotificationsEnabled(enabled: Boolean) {
        updateAndPersistBatchState { current ->
            current.copy(notificationsEnabled = enabled)
        }
    }

    fun clearCompletedBatchJobs() {
        updateAndPersistBatchState { current ->
            BatchQueueEngine.clearCompletedJobs(current)
        }
    }

    fun retryFailedBatchJobs() {
        updateAndPersistBatchState { current ->
            BatchQueueEngine.retryFailedAndNeedsAttentionJobs(current)
        }
        startBatchTranslationQueue()
    }

    fun pauseBatchTranslationQueue() {
        batchPauseRequested = true
        updateAndPersistBatchState { current ->
            current.copy(
                statusBannerMessage = "Menjeda antrean setelah langkah saat ini selesai..."
            )
        }
    }

    fun resumeBatchTranslationQueue() {
        startBatchTranslationQueue()
    }

    fun cancelBatchTranslationQueue() {
        batchCancelRequested = true
        activeBatchQueueJob?.cancel()
        activeBatchQueueJob = null
        val cancelledState = BatchQueueEngine.cancelBatch(_batchQueueState.value)
        val notifPayload = notificationHelper.notifyBatchTerminalState(cancelledState)
        val finalState = cancelledState.copy(
            lastNotificationTitle = notifPayload?.title ?: cancelledState.lastNotificationTitle,
            lastNotificationMessage = notifPayload?.message ?: cancelledState.lastNotificationMessage
        )
        _batchQueueState.value = finalState
        prefsRepo.saveBatchQueueState(finalState)
    }

    fun startBatchTranslationQueue(customTranslatorForTesting: BatchCueTranslator? = null) {
        val current = _batchQueueState.value
        if (current.isRunning) return
        val hasRunnable = BatchQueueEngine.findNextRunnableJob(current) != null
        if (!hasRunnable) {
            updateAndPersistBatchState {
                it.copy(statusBannerMessage = "Tidak ada file berstatus Queued atau Retry Queued untuk diproses.")
            }
            return
        }

        // Validate AI credentials when using real translator
        val ai = _aiSettings.value
        if (customTranslatorForTesting == null) {
            if (ai.preset == AiProviderPreset.GEMINI_WEB_COOKIE && ai.geminiSecure1Psid.trim().isEmpty()) {
                updateAndPersistBatchState {
                    it.copy(
                        statusBannerMessage = "Cookie __Secure-1PSID belum diisi! Buka pengaturan AI atau pilih provider lain."
                    )
                }
                return
            }
            if (!ai.preset.isGeminiWebFamily && ai.apiKey.trim().isEmpty()) {
                updateAndPersistBatchState {
                    it.copy(
                        statusBannerMessage = "API Key untuk ${ai.preset.label} belum diisi! Buka pengaturan AI terlebih dahulu."
                    )
                }
                return
            }
        }

        batchPauseRequested = false
        batchCancelRequested = false

        val folderPath = current.outputFolderUriOrPath.ifBlank { defaultBatchOutputDirectory.absolutePath }
        val outputWriter = AndroidBatchOutputWriter(
            context = getApplication<Application>().applicationContext,
            folderUriOrPath = folderPath
        )
        val translator = customTranslatorForTesting ?: buildProductionBatchCueTranslator()

        activeBatchQueueJob = viewModelScope.launch {
            try {
                val activeConfig = buildActiveSingleTranslationConfig()

                val finalState = BatchQueueEngine.executeSequentialQueue(
                    initialState = _batchQueueState.value.copy(
                        outputFolderUriOrPath = folderPath
                    ),
                    translator = translator,
                    outputWriter = outputWriter,
                    translationConfig = activeConfig,
                    glossaryRules = if (activeConfig.autoApplyGlossaryBeforeMerge) activeConfig.glossaryRules else emptyList(),
                    protectAssStructuralTags = activeConfig.restoreLeadingAssTags,
                    sourceLanguage = activeConfig.sourceLanguage,
                    targetLanguage = activeConfig.effectiveTargetLanguageLabel,
                    checkUntranslatedSourceLeakage = activeConfig.checkUntranslatedSourceLeakage,
                    shouldPause = { batchPauseRequested },
                    shouldCancel = { batchCancelRequested },
                    onStateChanged = { intermediate ->
                        _batchQueueState.value = intermediate
                        prefsRepo.saveBatchQueueState(intermediate)
                    }
                )

                // If queue reached a terminal state (not merely paused), trigger native phone notification
                if (finalState.isQueueCompletelyFinished) {
                    val notifPayload = notificationHelper.notifyBatchTerminalState(finalState)
                    val notifiedState = finalState.copy(
                        lastNotificationTitle = notifPayload?.title ?: finalState.lastNotificationTitle,
                        lastNotificationMessage = notifPayload?.message ?: finalState.lastNotificationMessage
                    )
                    _batchQueueState.value = notifiedState
                    prefsRepo.saveBatchQueueState(notifiedState)
                }
            } catch (_: CancellationException) {
                // Handled in cancelBatchTranslationQueue
            }
        }
    }

    /**
     * Delegates Batch Queue file translation directly to [SingleTranslationPipeline]
     * with the exact [SingleTranslationConfig] used by Single Translation.
     */
    internal fun buildProductionBatchCueTranslator(
        aiGateway: SingleTranslationAiGateway = com.example.domain.ProductionSingleTranslationAiGateway
    ): BatchCueTranslator {
        return SingleTranslationPipeline.createBatchCueTranslator(
            configProvider = { buildActiveSingleTranslationConfig() },
            aiGateway = aiGateway,
            shouldAbort = { batchCancelRequested || batchPauseRequested }
        )
    }

    // --- App Version & GitHub Release Update Checker ---

    fun checkAppUpdate() {
        val currentState = _appUpdateState.value
        if (currentState.isChecking) return

        _appUpdateState.update {
            it.copy(
                isChecking = true,
                errorMessage = null,
                statusMessage = "Memeriksa rilis terbaru di GitHub (${it.repoOwner}/${it.repoName})..."
            )
        }

        viewModelScope.launch {
            val result = AppUpdateChecker.checkLatestRelease(
                owner = currentState.repoOwner,
                repo = currentState.repoName,
                currentVersionName = currentState.currentVersionName
            )
            result.fold(
                onSuccess = { (hasNewer, releaseInfo) ->
                    val statusMsg = if (hasNewer) {
                        "Tersedia versi baru v${releaseInfo.versionName} (${releaseInfo.releaseTitle})!"
                    } else {
                        "Aplikasi Anda sudah menggunakan versi terbaru (v${currentState.currentVersionName})."
                    }
                    _appUpdateState.update {
                        it.copy(
                            isChecking = false,
                            isUpdateAvailable = hasNewer,
                            latestRelease = releaseInfo,
                            statusMessage = statusMsg,
                            errorMessage = null
                        )
                    }
                },
                onFailure = { err ->
                    _appUpdateState.update {
                        it.copy(
                            isChecking = false,
                            errorMessage = err.message ?: "Gagal memeriksa pembaruan dari GitHub Releases.",
                            statusMessage = null
                        )
                    }
                }
            )
        }
    }

    fun downloadLatestUpdateApk() {
        val context = getApplication<Application>().applicationContext
        val state = _appUpdateState.value
        val release = state.latestRelease ?: return
        val apkAsset = release.apkAsset

        if (apkAsset == null || apkAsset.downloadUrl.isBlank()) {
            if (release.htmlUrl.isNotBlank()) {
                AppUpdateChecker.openUrlInBrowser(context, release.htmlUrl)
            }
            _appUpdateState.update {
                it.copy(
                    statusMessage = "Membuka halaman rilis GitHub di browser untuk mengunduh APK."
                )
            }
            return
        }

        _appUpdateState.update {
            it.copy(
                isDownloadingApk = true,
                errorMessage = null,
                statusMessage = "Memulai unduhan ${apkAsset.name} (${apkAsset.formattedSizeMb})..."
            )
        }

        val enqueueResult = AppUpdateChecker.enqueueApkDownload(
            context = context,
            asset = apkAsset,
            versionTag = release.tagName
        )

        enqueueResult.fold(
            onSuccess = { (downloadId, fileName) ->
                _appUpdateState.update {
                    it.copy(
                        isDownloadingApk = false,
                        lastDownloadId = downloadId,
                        downloadedFileName = fileName,
                        statusMessage = "Mengunduh $fileName ke folder Downloads! Cek notifikasi sistem untuk memasang APK setelah selesai."
                    )
                }
                _uiState.update {
                    it.copy(
                        lastActionBannerMessage = "Unduhan $fileName dimulai di DownloadManager!"
                    )
                }
            },
            onFailure = { err ->
                // Fallback to direct browser download if DownloadManager is restricted
                val opened = AppUpdateChecker.openUrlInBrowser(context, apkAsset.downloadUrl)
                _appUpdateState.update {
                    it.copy(
                        isDownloadingApk = false,
                        statusMessage = if (opened) {
                            "Mengunduh ${apkAsset.name} melalui browser perangkat..."
                        } else null,
                        errorMessage = if (!opened) {
                            "Gagal memulai unduhan: ${err.message}"
                        } else null
                    )
                }
            }
        )
    }

    fun openLatestReleaseInBrowser() {
        val context = getApplication<Application>().applicationContext
        val state = _appUpdateState.value
        val targetUrl = state.latestRelease?.htmlUrl?.takeIf { it.isNotBlank() }
            ?: "https://github.com/${state.repoOwner}/${state.repoName}/releases"
        AppUpdateChecker.openUrlInBrowser(context, targetUrl)
    }
}

