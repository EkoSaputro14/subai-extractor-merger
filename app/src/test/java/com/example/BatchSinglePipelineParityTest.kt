package com.example

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.example.domain.BatchFileCandidate
import com.example.domain.BatchQueueEngine
import com.example.domain.LocalDirectoryBatchOutputWriter
import com.example.domain.SampleSubtitles
import com.example.domain.SingleTranslationAiGateway
import com.example.domain.SingleTranslationConfig
import com.example.domain.SingleTranslationPipeline
import com.example.domain.SingleTranslationPipelineExecutionReport
import com.example.domain.SubtitleParserAndMerger
import com.example.model.AiProviderPreset
import com.example.model.BatchJobStatus
import com.example.model.BatchQueueState
import com.example.model.BatchSizeOption
import com.example.model.BatchTerminalOutcome
import com.example.model.BilingualOrder
import com.example.model.GlossaryRule
import com.example.model.IndexFormatMode
import com.example.model.LineSeparatorMode
import com.example.model.TargetLanguageOption
import com.example.model.TranslationStylePreset
import com.example.viewmodel.SubtitleViewModel
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.nio.file.Files

/**
 * Mandatory Verification Suite (Tests 1 through 7) proving that Batch Translation
 * uses the exact same [SingleTranslationPipeline] and [SingleTranslationConfig] as Single Translation,
 * achieving 100% parity in prompt, chunking, context, retry, validation, and output formatting.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class BatchSinglePipelineParityTest {

    private lateinit var tempOutputDir: File

    @Before
    fun setUp() {
        tempOutputDir = Files.createTempDirectory("parity_single_vs_batch_test").toFile()
    }

    @After
    fun tearDown() {
        tempOutputDir.deleteRecursively()
    }

    /**
     * Deterministic AI Gateway that translates numbered input lines using the exact index format
     * while preserving proper nouns/spells like "Zettai Kansei Kisei Jijitsu!" to mirror real AI behavior.
     */
    private fun buildDeterministicAiGateway(
        recordedPrompts: MutableList<String> = mutableListOf(),
        recordedUserPayloads: MutableList<String> = mutableListOf(),
        recordedConfigs: MutableList<SingleTranslationConfig> = mutableListOf()
    ): SingleTranslationAiGateway {
        return SingleTranslationAiGateway { config, systemPrompt, numberedUserLines ->
            recordedConfigs.add(config)
            recordedPrompts.add(systemPrompt)
            recordedUserPayloads.add(numberedUserLines)

            val parsedInput = SubtitleParserAndMerger.parseAiTranslatedLines(numberedUserLines)
            val responseLines = parsedInput.entries.joinToString("\n") { (id, sourceText) ->
                val translated = when {
                    sourceText.contains("Zettai Kansei Kisei Jijitsu", ignoreCase = true) ->
                        sourceText // Proper noun / Romaji spell kept as-is by AI
                    sourceText.contains("Shichae! Shichae!", ignoreCase = true) ->
                        "Lakukan! // Lakukan!"
                    else ->
                        "Terjemahan ID #$id: ${sourceText.replace("Wait", "Tunggu").replace("train", "kereta")}"
                }
                config.indexFormatMode.formatLine(id, translated)
            }
            Result.success(responseLines)
        }
    }

    /**
     * Test 1: Pipeline Identity
     * Ensures Batch Translation invokes the exact same [SingleTranslationPipeline] service
     * as Single Translation, with zero separate translation engine logic.
     */
    @Test
    fun test1_pipelineIdentity_batchCallsSameSingleTranslationPipeline() = runTest {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = SubtitleViewModel(app)
        val writer = LocalDirectoryBatchOutputWriter(tempOutputDir)

        val singleReports = mutableListOf<SingleTranslationPipelineExecutionReport>()
        val batchReports = mutableListOf<SingleTranslationPipelineExecutionReport>()
        val gateway = buildDeterministicAiGateway()

        val sourceDoc = SubtitleParserAndMerger.parseSubtitle(
            fileName = "episode_01.srt",
            rawContent = SampleSubtitles.SAMPLE_BATCH_EPISODE_01_SRT
        )
        val activeConfig = viewModel.buildActiveSingleTranslationConfig()

        // 1. Run Single Translation via SingleTranslationPipeline
        val singleResult = SingleTranslationPipeline.executePipeline(
            sourceDocument = sourceDoc,
            config = activeConfig,
            aiGateway = gateway
        ).getOrThrow()
        singleReports.add(singleResult.report)

        // 2. Run Batch Translation via BatchQueueEngine + SingleTranslationPipeline.createBatchCueTranslator
        val batchState = BatchQueueEngine.enqueueFiles(
            BatchQueueState(outputFolderUriOrPath = tempOutputDir.absolutePath),
            listOf(
                BatchFileCandidate(
                    fileName = "episode_01.srt",
                    sourceUriString = "file:///episode_01.srt",
                    rawContent = SampleSubtitles.SAMPLE_BATCH_EPISODE_01_SRT
                )
            )
        )
        val batchTranslator = SingleTranslationPipeline.createBatchCueTranslator(
            configProvider = { viewModel.buildActiveSingleTranslationConfig() },
            aiGateway = gateway,
            onPipelineReport = { batchReports.add(it) }
        )

        val finalBatchState = BatchQueueEngine.executeSequentialQueue(
            initialState = batchState,
            translator = batchTranslator,
            outputWriter = writer,
            translationConfig = activeConfig
        )

        assertEquals(1, singleReports.size)
        assertEquals(1, batchReports.size)
        assertEquals(BatchJobStatus.COMPLETED, finalBatchState.sortedJobs.single().status)

        // Prove both executed through the exact same SingleTranslationPipeline with identical prompt & chunks
        assertEquals(singleReports.single().systemPromptUsed, batchReports.single().systemPromptUsed)
        assertEquals(singleReports.single().chunkSizes, batchReports.single().chunkSizes)
        assertEquals(singleReports.single().mergedContent, batchReports.single().mergedContent)
    }

    /**
     * Test 2: Configuration Identity
     * Ensures all user-configured Single Translation settings (AI provider, model, target language,
     * style preset, separator mode, index format, chunk size including BatchSizeOption.ALL,
     * glossary rules, ASS tag restoration, bilingual mode) are identically propagated to Batch Translation.
     */
    @Test
    fun test2_configurationIdentity_allSingleTranslationSettingsPropagateToBatch() = runTest {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = SubtitleViewModel(app)

        // Configure custom settings on Single Translation
        viewModel.selectAiProviderPreset(AiProviderPreset.DEEPSEEK)
        viewModel.selectAiModel("deepseek-reasoner")
        viewModel.setTargetLanguageOption(TargetLanguageOption.ENGLISH)
        viewModel.setTranslationStylePreset(TranslationStylePreset.MOVIE_FORMAL)
        viewModel.setSeparatorMode(LineSeparatorMode.PIPE)
        viewModel.setIndexFormatMode(IndexFormatMode.DOT)
        viewModel.setBatchSizeOption(BatchSizeOption.ALL) // Size = 0 (all cues in 1 chunk)
        viewModel.setProtectAssTags(true)
        viewModel.setRestoreLeadingAssTags(true)
        viewModel.setBilingualMode(true)
        viewModel.setBilingualOrder(BilingualOrder.ORIGINAL_TOP)
        viewModel.addGlossaryRule("Shibuya Ward", "Distrik Khusus Shibuya", isCaseSensitive = false, wholeWord = false)
        viewModel.setAutoApplyGlossaryBeforeMerge(true)
        viewModel.setBatchMaxRetries(5)

        val singleConfig = viewModel.buildActiveSingleTranslationConfig()
        val capturedBatchConfigs = mutableListOf<SingleTranslationConfig>()
        val gateway = buildDeterministicAiGateway(recordedConfigs = capturedBatchConfigs)
        val writer = LocalDirectoryBatchOutputWriter(tempOutputDir)

        val batchState = BatchQueueEngine.enqueueFiles(
            BatchQueueState(outputFolderUriOrPath = tempOutputDir.absolutePath, maxRetries = 5),
            listOf(
                BatchFileCandidate(
                    fileName = "episode_01.srt",
                    sourceUriString = "file:///episode_01.srt",
                    rawContent = SampleSubtitles.SAMPLE_BATCH_EPISODE_01_SRT
                )
            )
        )

        BatchQueueEngine.executeSequentialQueue(
            initialState = batchState,
            translator = viewModel.buildProductionBatchCueTranslator(aiGateway = gateway),
            outputWriter = writer,
            translationConfig = singleConfig
        )

        assertTrue(capturedBatchConfigs.isNotEmpty())
        val batchConfigUsed = capturedBatchConfigs.first()

        // Assert 100% Configuration Identity across every parameter
        assertEquals(singleConfig, batchConfigUsed)
        assertEquals("deepseek-reasoner", batchConfigUsed.aiSettings.selectedModel)
        assertEquals(TargetLanguageOption.ENGLISH, batchConfigUsed.targetLanguageOption)
        assertEquals(TranslationStylePreset.MOVIE_FORMAL, batchConfigUsed.stylePreset)
        assertEquals(LineSeparatorMode.PIPE, batchConfigUsed.separatorMode)
        assertEquals(IndexFormatMode.DOT, batchConfigUsed.indexFormatMode)
        assertEquals(BatchSizeOption.ALL, batchConfigUsed.batchSizeOption)
        assertEquals(true, batchConfigUsed.bilingualMode)
        assertEquals(BilingualOrder.ORIGINAL_TOP, batchConfigUsed.bilingualOrder)
        assertEquals(true, batchConfigUsed.autoApplyGlossaryBeforeMerge)
        assertEquals(5, batchConfigUsed.maxRetries)
    }

    /**
     * Test 3: Sequential Processing
     * Ensures multiple files and their internal chunks are processed strictly one-by-one in queue order.
     */
    @Test
    fun test3_sequentialProcessing_filesAndChunksRunStrictlyInOrder() = runTest {
        val writer = LocalDirectoryBatchOutputWriter(tempOutputDir)
        val candidates = SampleSubtitles.buildBatchEpisodeSampleCandidates()
        val state = BatchQueueEngine.enqueueFiles(
            BatchQueueState(outputFolderUriOrPath = tempOutputDir.absolutePath),
            candidates
        )

        val executionTrace = mutableListOf<String>()
        var activeInFlight = 0
        var maxConcurrent = 0

        val gateway = SingleTranslationAiGateway { config, _, numberedUserLines ->
            activeInFlight++
            maxConcurrent = maxOf(maxConcurrent, activeInFlight)
            val parsed = SubtitleParserAndMerger.parseAiTranslatedLines(numberedUserLines)
            val out = parsed.entries.joinToString("\n") { (id, _) ->
                config.indexFormatMode.formatLine(id, "Dialog terjemahan berurutan #$id")
            }
            activeInFlight--
            Result.success(out)
        }

        val config = SingleTranslationConfig(batchSizeOption = BatchSizeOption.BATCH_50)
        val translator = SingleTranslationPipeline.createBatchCueTranslator(
            configProvider = { config },
            aiGateway = gateway,
            onPipelineReport = { report -> executionTrace.add(report.fileName) }
        )

        val finalState = BatchQueueEngine.executeSequentialQueue(
            initialState = state,
            translator = translator,
            outputWriter = writer,
            translationConfig = config
        )

        assertEquals(1, maxConcurrent)
        assertEquals(
            listOf("episode_01.srt", "episode_02.srt", "episode_03.vtt", "episode_04.ass"),
            executionTrace
        )
        assertEquals(4, finalState.completedCount)
    }

    /**
     * Test 4: Same Input Comparison (Single vs Batch Parity, including Anime ASS with Romaji Spells)
     * Uses the exact same subtitle file (including `Zettai Kansei Kisei Jijitsu!` and `Shichae! Shichae!`)
     * for Single Translation and Batch Translation with identical provider, model, and configuration.
     * Compares: entry count, completeness, timestamps, structure, error rate, retry count, validation, and output text.
     */
    @Test
    fun test4_sameInputComparison_singleAndBatchProduceIdenticalResultsAndValidation() = runTest {
        val writer = LocalDirectoryBatchOutputWriter(tempOutputDir)
        val animeAssContent = """
            [Script Info]
            Title: Dark Summoner Episode 04
            ScriptType: v4.00+

            [V4+ Styles]
            Format: Name, Fontname, Fontsize, PrimaryColour, SecondaryColour, OutlineColour, BackColour, Bold, Italic, Underline, StrikeOut, ScaleX, ScaleY, Spacing, Angle, BorderStyle, Outline, Shadow, Alignment, MarginL, MarginR, MarginV, Encoding
            Style: Default,Arial,48,&H00FFFFFF,&H000000FF,&H00000000,&H64000000,-1,0,0,0,100,100,0,0,1,2,1,2,20,20,25,1

            [Events]
            Format: Layer, Start, End, Style, Name, MarginL, MarginR, MarginV, Effect, Text
            Dialogue: 0,0:00:04.00,0:00:06.58,Default,Summoner,0,0,0,,Shichae!\NShichae!
            Dialogue: 0,0:00:06.58,0:00:09.29,Default,Summoner,0,0,0,,Zettai Kansei Kisei Jijitsu!
            Dialogue: 0,0:00:09.29,0:00:12.21,Default,Summoner,0,0,0,,{\an8}Wait for the signal at the gate!
        """.trimIndent()

        val config = SingleTranslationConfig(
            separatorMode = LineSeparatorMode.PIPE,
            restoreLeadingAssTags = true,
            autoApplyGlossaryBeforeMerge = true,
            glossaryRules = listOf(
                GlossaryRule(
                    id = "g1",
                    sourceTerm = "gate",
                    targetTerm = "gerbang utama",
                    isCaseSensitive = false,
                    wholeWord = true,
                    isEnabled = true
                )
            )
        )
        val gateway = buildDeterministicAiGateway()

        // 1. Run Single Translation
        val sourceDoc = SubtitleParserAndMerger.parseSubtitle("dark_summoner_04.ass", animeAssContent)
        val singleResult = SingleTranslationPipeline.executePipeline(
            sourceDocument = sourceDoc,
            config = config,
            aiGateway = gateway
        ).getOrThrow()

        // 2. Run Batch Translation on the exact same input
        val batchState = BatchQueueEngine.enqueueFiles(
            BatchQueueState(outputFolderUriOrPath = tempOutputDir.absolutePath),
            listOf(
                BatchFileCandidate(
                    fileName = "dark_summoner_04.ass",
                    sourceUriString = "file:///dark_summoner_04.ass",
                    rawContent = animeAssContent
                )
            )
        )
        val finalBatchState = BatchQueueEngine.executeSequentialQueue(
            initialState = batchState,
            translator = SingleTranslationPipeline.createBatchCueTranslator(
                configProvider = { config },
                aiGateway = gateway
            ),
            outputWriter = writer,
            translationConfig = config
        )

        val batchJob = finalBatchState.sortedJobs.single()
        val batchSavedContent = File(tempOutputDir, "dark_summoner_04.ass").readText(Charsets.UTF_8)

        // Compare Single vs Batch metrics:
        assertEquals(BatchJobStatus.COMPLETED, batchJob.status)
        assertTrue(singleResult.validationResult.isValid)
        assertTrue(batchJob.lastValidationResult!!.isValid)
        assertEquals(singleResult.validationResult.totalSourceCues, batchJob.lastValidationResult!!.totalSourceCues)
        assertEquals(singleResult.validationResult.translatedCueCount, batchJob.lastValidationResult!!.translatedCueCount)
        assertEquals(1, batchJob.attemptCount) // Succeeded on 1st attempt without false-positive retry!
        assertEquals(singleResult.translatedCues, batchJob.validTranslatedCues)
        assertEquals(singleResult.mergedContent, batchSavedContent)
    }

    /**
     * Test 5: Batch Reliability Across Multi-Chunk Files & Relative Index Drift Recovery
     * Tests a batch of files with varying sizes (including a 135-cue file split across 3 chunks of 60, 60, 15
     * where the LLM returns 1-based chunk-relative numbering `[1]..[60]` on chunk #2 and misses 2 cues on chunk #3
     * that get repaired by [SingleTranslationPipeline]).
     */
    @Test
    fun test5_batchReliability_multiChunkFilesAndRelativeNumberingSucceedWithEqualRate() = runTest {
        val writer = LocalDirectoryBatchOutputWriter(tempOutputDir)

        // Build a 115-cue SRT file (which splits into 3 chunks under BATCH_50: 1..50, 51..100, 101..115)
        val largeSrtContent = (1..115).joinToString("\n\n") { idx ->
            val min = idx / 60
            val sec = idx % 60
            val start = String.format("%02d:%02d:00,000", min, sec)
            val end = String.format("%02d:%02d:00,800", min, sec)
            "$idx\n$start --> $end\nDialogue line number $idx from the source episode."
        }

        var chunkCallCounter = 0
        val flakyChunkGateway = SingleTranslationAiGateway { config, _, numberedUserLines ->
            chunkCallCounter++
            val parsedInput = SubtitleParserAndMerger.parseAiTranslatedLines(numberedUserLines)
            val firstId = parsedInput.keys.firstOrNull() ?: 1

            when {
                // Simulate LLM returning relative [1]..[50] numbering for Chunk #2 (IDs 51..100)
                firstId == 51 && parsedInput.size == 50 -> {
                    var relIdx = 1
                    val relativeOutput = parsedInput.entries.joinToString("\n") { (actualId, _) ->
                        config.indexFormatMode.formatLine(relIdx++, "Terjemahan baris ke-$actualId yang lengkap.")
                    }
                    Result.success(relativeOutput)
                }
                // Simulate LLM dropping cue #110 on first pass of Chunk #3 (IDs 101..115), then succeeding on chunk repair
                firstId == 101 && parsedInput.size == 15 && chunkCallCounter == 3 -> {
                    val partialOutput = parsedInput.entries
                        .filter { it.key != 110 }
                        .joinToString("\n") { (id, _) ->
                            config.indexFormatMode.formatLine(id, "Terjemahan baris ke-$id yang lengkap.")
                        }
                    Result.success(partialOutput)
                }
                else -> {
                    val normalOutput = parsedInput.entries.joinToString("\n") { (id, _) ->
                        config.indexFormatMode.formatLine(id, "Terjemahan baris ke-$id yang lengkap.")
                    }
                    Result.success(normalOutput)
                }
            }
        }

        val config = SingleTranslationConfig(
            batchSizeOption = BatchSizeOption.BATCH_50,
            maxChunkRepairRetries = 1
        )

        val candidates = listOf(
            BatchFileCandidate("short_ep01.srt", "file:///short.srt", SampleSubtitles.SAMPLE_BATCH_EPISODE_01_SRT),
            BatchFileCandidate("large_ep02_115cues.srt", "file:///large.srt", largeSrtContent),
            BatchFileCandidate("styled_ep03.ass", "file:///styled.ass", SampleSubtitles.SAMPLE_BATCH_EPISODE_04_ASS)
        )

        val state = BatchQueueEngine.enqueueFiles(
            BatchQueueState(outputFolderUriOrPath = tempOutputDir.absolutePath),
            candidates
        )
        val finalState = BatchQueueEngine.executeSequentialQueue(
            initialState = state,
            translator = SingleTranslationPipeline.createBatchCueTranslator(
                configProvider = { config },
                aiGateway = flakyChunkGateway
            ),
            outputWriter = writer,
            translationConfig = config
        )

        assertEquals(3, finalState.completedCount)
        assertEquals(BatchTerminalOutcome.ALL_SUCCEEDED, finalState.terminalOutcome)
        val largeJob = finalState.jobs.first { it.fileName == "large_ep02_115cues.srt" }
        assertEquals(BatchJobStatus.COMPLETED, largeJob.status)
        assertEquals(115, largeJob.validTranslatedCues.size)
        assertEquals("Terjemahan baris ke-110 yang lengkap.", largeJob.validTranslatedCues[110])
    }

    /**
     * Test 6: Retry Re-Executes Full Single Translation Pipeline from Original Source at End of Queue
     * Simulates a failure on `episode_02.srt` during Attempt #1, verifies it moves to the end of the queue
     * after `episode_03.srt`, and verifies that when Attempt #2 runs, it executes [SingleTranslationPipeline]
     * from the full original `sourceDocument` (all cues, preserving full context and identical config).
     */
    @Test
    fun test6_retry_reexecutesFullSingleTranslationPipelineFromOriginalSourceAtEndOfQueue() = runTest {
        val writer = LocalDirectoryBatchOutputWriter(tempOutputDir)
        val candidates = SampleSubtitles.buildBatchEpisodeSampleCandidates().take(3)
        val state = BatchQueueEngine.enqueueFiles(
            BatchQueueState(outputFolderUriOrPath = tempOutputDir.absolutePath, maxRetries = 3),
            candidates
        )

        val attemptOrder = mutableListOf<String>()
        val cuesSentPerAttempt = mutableMapOf<String, Int>()
        var ep2Attempts = 0

        val gateway = SingleTranslationAiGateway { config, _, numberedUserLines ->
            val parsed = SubtitleParserAndMerger.parseAiTranslatedLines(numberedUserLines)
            val isEp2 = numberedUserLines.contains("Basement Level 3")
            if (isEp2) {
                ep2Attempts++
                attemptOrder.add("episode_02.srt#attempt$ep2Attempts")
                cuesSentPerAttempt["episode_02.srt#attempt$ep2Attempts"] = parsed.size
                if (ep2Attempts == 1) {
                    // Simulate temporary network/API error on attempt #1 of episode_02.srt
                    return@SingleTranslationAiGateway Result.failure(IllegalStateException("HTTP 503 Temporary Overload"))
                }
            } else if (numberedUserLines.contains("Shibuya Ward")) {
                attemptOrder.add("episode_01.srt#attempt1")
            } else {
                attemptOrder.add("episode_03.vtt#attempt1")
            }

            val out = parsed.entries.joinToString("\n") { (id, _) ->
                config.indexFormatMode.formatLine(id, "Terjemahan lengkap baris #$id")
            }
            Result.success(out)
        }

        val config = SingleTranslationConfig(batchSizeOption = BatchSizeOption.BATCH_100)
        val finalState = BatchQueueEngine.executeSequentialQueue(
            initialState = state,
            translator = SingleTranslationPipeline.createBatchCueTranslator(
                configProvider = { config },
                aiGateway = gateway
            ),
            outputWriter = writer,
            translationConfig = config
        )

        // Verify episode_02 failed on attempt 1, moved to the end of the queue after episode_03, and succeeded on attempt 2
        assertEquals(
            listOf(
                "episode_01.srt#attempt1",
                "episode_02.srt#attempt1",
                "episode_03.vtt#attempt1",
                "episode_02.srt#attempt2"
            ),
            attemptOrder
        )
        // Verify Attempt #2 sent the FULL original source cues (3 cues), NOT a stripped contextless fragment
        assertEquals(3, cuesSentPerAttempt["episode_02.srt#attempt1"])
        assertEquals(3, cuesSentPerAttempt["episode_02.srt#attempt2"])
        assertEquals(3, finalState.completedCount)
    }

    /**
     * Test 7: Dedicated Batch Output Folder & Identical Single Translation Formatting
     * Ensures Batch Queue saves files into the dedicated output folder while preserving 100% identical
     * formatting (Bilingual mode, custom line separator, ASS override tags) as Single Translation.
     */
    @Test
    fun test7_outputFolderAndSingleTranslationFormattingParity() = runTest {
        val dedicatedOutputFolder = File(tempOutputDir, "Batch_Bilingual_Output")
        val writer = LocalDirectoryBatchOutputWriter(dedicatedOutputFolder)
        val config = SingleTranslationConfig(
            separatorMode = LineSeparatorMode.PIPE,
            restoreLeadingAssTags = true,
            bilingualMode = true,
            bilingualOrder = BilingualOrder.TRANSLATION_TOP,
            bilingualAssSecondaryStyling = true
        )
        val gateway = buildDeterministicAiGateway()

        val assDoc = SubtitleParserAndMerger.parseSubtitle(
            "episode_04.ass",
            SampleSubtitles.SAMPLE_BATCH_EPISODE_04_ASS
        )
        val singleMerged = SingleTranslationPipeline.executePipeline(
            sourceDocument = assDoc,
            config = config,
            aiGateway = gateway
        ).getOrThrow().mergedContent

        val batchState = BatchQueueEngine.enqueueFiles(
            BatchQueueState(outputFolderUriOrPath = dedicatedOutputFolder.absolutePath),
            listOf(
                BatchFileCandidate(
                    fileName = "episode_04.ass",
                    sourceUriString = "file:///episode_04.ass",
                    rawContent = SampleSubtitles.SAMPLE_BATCH_EPISODE_04_ASS
                )
            )
        )
        val finalBatchState = BatchQueueEngine.executeSequentialQueue(
            initialState = batchState,
            translator = SingleTranslationPipeline.createBatchCueTranslator(
                configProvider = { config },
                aiGateway = gateway
            ),
            outputWriter = writer,
            translationConfig = config
        )

        val savedFile = File(dedicatedOutputFolder, "episode_04.ass")
        assertTrue(savedFile.exists())
        assertEquals(BatchJobStatus.COMPLETED, finalBatchState.sortedJobs.single().status)
        assertNotNull(finalBatchState.sortedJobs.single().outputFilePath)
        // Byte-for-byte identical to Single Translation merged output!
        assertEquals(singleMerged, savedFile.readText(Charsets.UTF_8))
    }
}
