package com.example

import com.example.domain.BatchFileCandidate
import com.example.domain.BatchQueueEngine
import com.example.domain.LocalDirectoryBatchOutputWriter
import com.example.domain.SingleTranslationAiGateway
import com.example.domain.SingleTranslationConfig
import com.example.domain.SingleTranslationPipeline
import com.example.domain.SubtitleParserAndMerger
import com.example.model.BatchJobStatus
import com.example.model.BatchQueueState
import com.example.model.BatchSizeOption
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.nio.file.Files

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PipelineRefusalAndTargetedRepairTest {

    private lateinit var tempOutputDir: File

    @Before
    fun setUp() {
        tempOutputDir = Files.createTempDirectory("pipeline_recovery_test").toFile()
    }

    @After
    fun tearDown() {
        tempOutputDir.deleteRecursively()
    }

    @Test
    fun geminiWebRefusal_triggersAdaptiveSubBatchSplittingAndNeutralPromptAndCompletes100Percent() = runTest {
        // Build a 30-cue ASS episode containing one suggestive/sensitive comedy line at cue #18
        val cuesBlock = (1..30).joinToString("\n") { idx ->
            val secStart = idx * 2
            val secEnd = secStart + 1
            val start = "0:00:%02d.00".format(secStart)
            val end = "0:00:%02d.50".format(secEnd)
            val text = if (idx == 18) {
                "{\\i1}Do not peek at my secret swimsuit charm!"
            } else {
                "Line number $idx dialogue from the summoning guild."
            }
            "Dialogue: 0,$start,$end,Default,Hero,0,0,0,,$text"
        }
        val assContent = """
            [Script Info]
            Title: Dark Summoner Ep 07
            ScriptType: v4.00+

            [V4+ Styles]
            Format: Name, Fontname, Fontsize, PrimaryColour, SecondaryColour, OutlineColour, BackColour, Bold, Italic, Underline, StrikeOut, ScaleX, ScaleY, Spacing, Angle, BorderStyle, Outline, Shadow, Alignment, MarginL, MarginR, MarginV, Encoding
            Style: Default,Arial,48,&H00FFFFFF,&H000000FF,&H00000000,&H64000000,-1,0,0,0,100,100,0,0,1,2,1,2,20,20,25,1

            [Events]
            Format: Layer, Start, End, Style, Name, MarginL, MarginR, MarginV, Effect, Text
            $cuesBlock
        """.trimIndent()

        val sourceDoc = SubtitleParserAndMerger.parseSubtitle("dark_summoner_07.ass", assContent)
        val observedPrompts = mutableListOf<String>()
        val observedBatchSizes = mutableListOf<Int>()

        val refusalGateway = SingleTranslationAiGateway { config, systemPrompt, numberedUserLines ->
            observedPrompts.add(systemPrompt)
            val parsedInput = SubtitleParserAndMerger.parseAiTranslatedLines(numberedUserLines)
            val cueCountInCall = if (parsedInput.isNotEmpty()) parsedInput.size else 1
            observedBatchSizes.add(cueCountInCall)

            // 1. Refuse when standard large batch (>25) is sent with technical phrasing
            if (parsedInput.size > 25) {
                return@SingleTranslationAiGateway Result.success(
                    "Saya tidak diprogram untuk itu. Saya hanya menghasilkan teks."
                )
            }
            // 2. Simulate safety/refusal on any multi-line sub-batch > 1 that includes cue #18
            if (parsedInput.size > 1 && parsedInput.containsKey(18)) {
                return@SingleTranslationAiGateway Result.success(
                    "Saya hanya AI berbasis teks dan tidak bisa membantu Anda untuk itu."
                )
            }
            // 3. Isolated 1-cue plain sentence mode succeeds for cue #18
            if (parsedInput.isEmpty() && numberedUserLines.contains("swimsuit", ignoreCase = true)) {
                return@SingleTranslationAiGateway Result.success(
                    "Jangan mengintip jimat baju renang rahasiaku!"
                )
            }
            if (parsedInput.size == 1 && parsedInput.containsKey(18)) {
                // Force direct single-cue mode once to verify 1-line isolation fallback too
                if (systemPrompt.contains("Awali setiap hasil")) {
                    return@SingleTranslationAiGateway Result.success(
                        "Saya hanya AI berbasis teks dan tidak bisa membantu Anda untuk itu."
                    )
                }
                return@SingleTranslationAiGateway Result.success(
                    config.indexFormatMode.formatLine(18, "Jangan mengintip jimat baju renang rahasiaku!")
                )
            }

            val out = parsedInput.entries.joinToString("\n") { (id, _) ->
                config.indexFormatMode.formatLine(id, "Dialog terjemahan bersih baris ke-$id dari serikat pemanggil.")
            }
            Result.success(out)
        }

        val config = SingleTranslationConfig(batchSizeOption = BatchSizeOption.BATCH_100)
        val writer = LocalDirectoryBatchOutputWriter(tempOutputDir)
        val state = BatchQueueEngine.enqueueFiles(
            BatchQueueState(outputFolderUriOrPath = tempOutputDir.absolutePath),
            listOf(BatchFileCandidate("dark_summoner_07.ass", "file:///ds07.ass", assContent))
        )

        val finalState = BatchQueueEngine.executeSequentialQueue(
            initialState = state,
            translator = SingleTranslationPipeline.createBatchCueTranslator(
                configProvider = { config },
                aiGateway = refusalGateway
            ),
            outputWriter = writer,
            translationConfig = config
        )

        val job = finalState.sortedJobs.single()
        assertEquals(BatchJobStatus.COMPLETED, job.status)
        assertTrue(job.lastValidationResult!!.isValid)
        assertEquals(30, job.validTranslatedCues.size)
        assertEquals("Jangan mengintip jimat baju renang rahasiaku!", job.validTranslatedCues[18])

        // Verify adaptive splitting actually broke down from 30 -> <=25 -> smaller
        assertTrue(observedBatchSizes.any { it <= 25 && it > 1 })
        assertTrue(observedBatchSizes.contains(1))
        assertTrue(observedPrompts.any { it.contains("murni terjemahan teks bacaan biasa") })

        // Verify strict 1:1 cue count, timestamp preservation, and ASS tag restoration in saved file
        val savedContent = File(tempOutputDir, "dark_summoner_07.ass").readText(Charsets.UTF_8)
        val reparsed = SubtitleParserAndMerger.parseSubtitle("dark_summoner_07.ass", savedContent)
        assertEquals(sourceDoc.cues.size, reparsed.cues.size)
        for (i in sourceDoc.cues.indices) {
            assertEquals(sourceDoc.cues[i].id, reparsed.cues[i].id)
            assertEquals(sourceDoc.cues[i].startTime, reparsed.cues[i].startTime)
            assertEquals(sourceDoc.cues[i].endTime, reparsed.cues[i].endTime)
            if (sourceDoc.cues[i].leadingAssTags.isNotEmpty()) {
                assertTrue(reparsed.cues[i].rawText.startsWith(sourceDoc.cues[i].leadingAssTags))
            }
        }
    }

    @Test
    fun partialValidationFailure_retranslatesOnlyProblematicCuesWithStrict1to1AlignmentLock() = runTest {
        val cuesBlock = (1..10).joinToString("\n") { idx ->
            val start = "0:00:%02d.00".format(idx * 2)
            val end = "0:00:%02d.90".format(idx * 2 + 1)
            val prefix = if (idx == 4 || idx == 7) "{\\an8}" else ""
            val line = when (idx) {
                4 -> "Why are you still standing near the dungeon door?"
                7 -> "Because we cannot leave our friends behind right now!"
                else -> "Everything inside the guild hall is ready for line $idx."
            }
            "Dialogue: 0,$start,$end,Default,Speaker,0,0,0,,$prefix$line"
        }
        val assContent = """
            [Script Info]
            Title: Targeted Repair Alignment Test
            ScriptType: v4.00+

            [V4+ Styles]
            Format: Name, Fontname, Fontsize, PrimaryColour, SecondaryColour, OutlineColour, BackColour, Bold, Italic, Underline, StrikeOut, ScaleX, ScaleY, Spacing, Angle, BorderStyle, Outline, Shadow, Alignment, MarginL, MarginR, MarginV, Encoding
            Style: Default,Arial,48,&H00FFFFFF,&H000000FF,&H00000000,&H64000000,-1,0,0,0,100,100,0,0,1,2,1,2,20,20,25,1

            [Events]
            Format: Layer, Start, End, Style, Name, MarginL, MarginR, MarginV, Effect, Text
            $cuesBlock
        """.trimIndent()

        val sourceDoc = SubtitleParserAndMerger.parseSubtitle("ep_repair.ass", assContent)
        var callCount = 0
        val repairCallsRequestedIds = mutableListOf<Set<Int>>()

        val gateway = SingleTranslationAiGateway { config, systemPrompt, numberedUserLines ->
            callCount++
            val parsedInput = SubtitleParserAndMerger.parseAiTranslatedLines(numberedUserLines)
            if (callCount == 1) {
                // First pass leaves #4 and #7 untranslated in English while translating the other 8 cues
                val out = parsedInput.entries.joinToString("\n") { (id, srcText) ->
                    val text = if (id == 4 || id == 7) {
                        srcText // Untranslated source leak!
                    } else {
                        "Terjemahan awal valid baris #$id di aula serikat."
                    }
                    config.indexFormatMode.formatLine(id, text)
                }
                return@SingleTranslationAiGateway Result.success(out)
            }

            // Subsequent call MUST be targeted repair ONLY covering #4 and #7 (not full 1..10 list!)
            assertFalse(
                "Targeted repair must not re-translate all 10 cues",
                parsedInput.size >= 10
            )
            repairCallsRequestedIds.add(parsedInput.keys)
            assertTrue(systemPrompt.contains("PERBAIKAN KHUSUS BARIS BERMASALAH"))

            val repairedOut = parsedInput.entries.joinToString("\n") { (id, _) ->
                val text = when (id) {
                    4 -> "Kenapa kamu masih berdiri di dekat pintu ruang bawah tanah?"
                    7 -> "Karena kita tidak bisa meninggalkan teman-teman sekarang!"
                    else -> "Konteks tetangga #$id"
                }
                config.indexFormatMode.formatLine(id, text)
            }
            Result.success(repairedOut)
        }

        val config = SingleTranslationConfig(batchSizeOption = BatchSizeOption.BATCH_100)
        val result = SingleTranslationPipeline.executePipeline(
            sourceDocument = sourceDoc,
            config = config,
            aiGateway = gateway
        ).getOrThrow()

        assertTrue(result.validationResult.isValid)
        assertEquals(10, result.translatedCues.size)
        // Surrounding valid cues must retain their exact original valid translations!
        assertEquals("Terjemahan awal valid baris #3 di aula serikat.", result.translatedCues[3])
        assertEquals("Terjemahan awal valid baris #5 di aula serikat.", result.translatedCues[5])
        // Problematic cues #4 and #7 must be repaired cleanly!
        assertEquals("Kenapa kamu masih berdiri di dekat pintu ruang bawah tanah?", result.translatedCues[4])
        assertEquals("Karena kita tidak bisa meninggalkan teman-teman sekarang!", result.translatedCues[7])
        assertTrue(repairCallsRequestedIds.isNotEmpty())

        // Verify strict 1:1 alignment, zero skipped cues, exact timestamps, and ASS tag preservation
        val reparsed = SubtitleParserAndMerger.parseSubtitle("ep_repair.ass", result.mergedContent)
        assertEquals(10, reparsed.cues.size)
        for (i in sourceDoc.cues.indices) {
            assertEquals(sourceDoc.cues[i].id, reparsed.cues[i].id)
            assertEquals(sourceDoc.cues[i].startTime, reparsed.cues[i].startTime)
            assertEquals(sourceDoc.cues[i].endTime, reparsed.cues[i].endTime)
            if (sourceDoc.cues[i].leadingAssTags.isNotEmpty()) {
                assertTrue(reparsed.cues[i].rawText.startsWith("{\\an8}"))
            }
        }
    }
}
