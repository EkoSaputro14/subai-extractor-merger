package com.example

import android.app.NotificationManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.BatchNotificationHelper
import com.example.domain.BatchCueTranslator
import com.example.domain.BatchFileCandidate
import com.example.domain.BatchQueueEngine
import com.example.domain.BatchTranslationAttemptOutput
import com.example.domain.BatchTranslationValidator
import com.example.domain.LocalDirectoryBatchOutputWriter
import com.example.domain.SampleSubtitles
import com.example.domain.SubtitleParserAndMerger
import com.example.model.BatchJobStatus
import com.example.model.BatchQueueState
import com.example.model.BatchTerminalOutcome
import com.example.model.FileConflictPolicy
import com.example.model.SubtitleCue
import com.example.model.SubtitleDocument
import com.example.model.SubtitleFormat
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.io.File
import java.nio.file.Files

/**
 * Comprehensive Automated Verification Suite for Batch Subtitle Translation Queue
 * covering mandatory Scenarios Test A through Test O.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class BatchTranslationQueueTest {

    private lateinit var tempOutputDir: File

    @Before
    fun setUp() {
        tempOutputDir = Files.createTempDirectory("batch_subtitle_test_out").toFile()
    }

    @After
    fun tearDown() {
        tempOutputDir.deleteRecursively()
    }

    private fun buildFourEpisodeCandidates(): List<BatchFileCandidate> = listOf(
        BatchFileCandidate(
            fileName = "episode_01.srt",
            sourceUriString = "file:///source/episode_01.srt",
            rawContent = SampleSubtitles.SAMPLE_BATCH_EPISODE_01_SRT
        ),
        BatchFileCandidate(
            fileName = "episode_02.srt",
            sourceUriString = "file:///source/episode_02.srt",
            rawContent = SampleSubtitles.SAMPLE_BATCH_EPISODE_02_SRT
        ),
        BatchFileCandidate(
            fileName = "episode_03.srt",
            sourceUriString = "file:///source/episode_03.srt",
            rawContent = SampleSubtitles.SAMPLE_BATCH_EPISODE_01_SRT.replace("Shibuya Ward", "Central Station")
        ),
        BatchFileCandidate(
            fileName = "episode_04.srt",
            sourceUriString = "file:///source/episode_04.srt",
            rawContent = SampleSubtitles.SAMPLE_BATCH_EPISODE_02_SRT.replace("Basement Level 3", "Platform 9")
        )
    )

    private fun translateAllCuesToIndonesian(cues: List<SubtitleCue>): Map<Int, String> {
        return cues.associate { cue ->
            cue.id to "Terjemahan baris ke-${cue.id}: dialog bahasa Indonesia yang lengkap dan valid."
        }
    }

    /**
     * Test A — Urutan pemrosesan berurutan
     * Pilih 4 file subtitle dengan urutan tertentu (serta uji pengubahan urutan sebelum mulai).
     * Pastikan pemrosesan berjalan sesuai urutan tersebut (1 -> 2 -> 3 -> 4) secara berurutan satu per satu.
     */
    @Test
    fun testA_sequentialProcessingOrderAndReordering() = runTest {
        val writer = LocalDirectoryBatchOutputWriter(tempOutputDir)
        var state = BatchQueueEngine.enqueueFiles(
            BatchQueueState(outputFolderUriOrPath = tempOutputDir.absolutePath),
            buildFourEpisodeCandidates()
        )
        assertEquals(4, state.totalFiles)
        assertEquals(
            listOf("episode_01.srt", "episode_02.srt", "episode_03.srt", "episode_04.srt"),
            state.sortedJobs.map { it.fileName }
        )

        // Test move down & move up before starting so order returns to 1, 2, 3, 4
        val job1Id = state.sortedJobs[0].jobId
        state = BatchQueueEngine.moveJobDown(state, job1Id)
        assertEquals("episode_02.srt", state.sortedJobs[0].fileName)
        state = BatchQueueEngine.moveJobUp(state, job1Id)
        assertEquals("episode_01.srt", state.sortedJobs[0].fileName)

        val executionOrder = mutableListOf<String>()
        var activeConcurrentCount = 0
        var maxObservedConcurrency = 0

        val translator = BatchCueTranslator { job, _, cuesToTranslate, onCueProgress ->
            activeConcurrentCount++
            maxObservedConcurrency = maxOf(maxObservedConcurrency, activeConcurrentCount)
            executionOrder.add(job.fileName)
            val map = translateAllCuesToIndonesian(cuesToTranslate)
            onCueProgress(map.size)
            activeConcurrentCount--
            Result.success(BatchTranslationAttemptOutput(map))
        }

        val finalState = BatchQueueEngine.executeSequentialQueue(
            initialState = state,
            translator = translator,
            outputWriter = writer
        )

        assertEquals(1, maxObservedConcurrency)
        assertEquals(
            listOf("episode_01.srt", "episode_02.srt", "episode_03.srt", "episode_04.srt"),
            executionOrder
        )
        assertEquals(4, finalState.completedCount)
        assertEquals(BatchTerminalOutcome.ALL_SUCCEEDED, finalState.terminalOutcome)
    }

    /**
     * Test B — Pemilihan folder output batch
     * Pilih folder output khusus, jalankan batch, pastikan seluruh file hasil tersimpan di folder tersebut
     * dan konten sumber tidak berubah.
     */
    @Test
    fun testB_dedicatedBatchOutputFolderSavesAllFilesWithoutModifyingSources() = runTest {
        val customBatchFolder = File(tempOutputDir, "Custom_Translated_Subtitles")
        val writer = LocalDirectoryBatchOutputWriter(customBatchFolder)
        val candidates = buildFourEpisodeCandidates()
        val originalSourceSnapshots = candidates.associate { it.fileName to it.rawContent }

        val state = BatchQueueEngine.enqueueFiles(
            BatchQueueState(
                outputFolderUriOrPath = customBatchFolder.absolutePath,
                outputFolderDisplayName = customBatchFolder.absolutePath
            ),
            candidates
        )

        val finalState = BatchQueueEngine.executeSequentialQueue(
            initialState = state,
            translator = { _, _, cues, _ ->
                Result.success(BatchTranslationAttemptOutput(translateAllCuesToIndonesian(cues)))
            },
            outputWriter = writer
        )

        assertTrue(customBatchFolder.exists() && customBatchFolder.isDirectory)
        val savedNames = customBatchFolder.listFiles()?.map { it.name }?.sorted() ?: emptyList()
        assertEquals(
            listOf("episode_01.srt", "episode_02.srt", "episode_03.srt", "episode_04.srt"),
            savedNames
        )
        // Ensure no leftover .tmp files exist
        assertTrue(customBatchFolder.listFiles()!!.none { it.name.endsWith(".tmp") })
        // Ensure source content in each job is untouched
        for (job in finalState.sortedJobs) {
            assertEquals(originalSourceSnapshots[job.fileName], job.rawSourceContent)
        }
    }

    /**
     * Test C — Validasi terjemahan lengkap
     * Berikan hasil terjemahan yang semua entrinya lengkap.
     * Pastikan validasi berhasil, file final disimpan, dan status menjadi Completed.
     */
    @Test
    fun testC_completeTranslationPassesValidationAndMarksCompleted() = runTest {
        val writer = LocalDirectoryBatchOutputWriter(tempOutputDir)
        val candidate = buildFourEpisodeCandidates().first()
        val state = BatchQueueEngine.enqueueFiles(BatchQueueState(), listOf(candidate))

        val finalState = BatchQueueEngine.executeSequentialQueue(
            initialState = state,
            translator = { _, _, cues, _ ->
                Result.success(BatchTranslationAttemptOutput(translateAllCuesToIndonesian(cues)))
            },
            outputWriter = writer
        )

        val job = finalState.sortedJobs.single()
        assertEquals(BatchJobStatus.COMPLETED, job.status)
        assertNotNull(job.lastValidationResult)
        assertTrue(job.lastValidationResult!!.isValid)
        assertTrue(File(tempOutputDir, "episode_01.srt").exists())
    }

    /**
     * Test D — Validasi terjemahan tidak lengkap (entri hilang)
     * Simulasikan hasil terjemahan dengan beberapa entri hilang.
     * Pastikan validasi gagal, file final tidak disimpan sebagai hasil akhir, dan pekerjaan masuk ke Retry Queued di akhir antrean.
     */
    @Test
    fun testD_incompleteTranslationWithMissingEntriesFailsValidationAndRequeues() = runTest {
        val writer = LocalDirectoryBatchOutputWriter(tempOutputDir)
        val candidate = buildFourEpisodeCandidates().first() // has 3 cues: #1, #2, #3
        val state = BatchQueueEngine.enqueueFiles(BatchQueueState(maxRetries = 3), listOf(candidate))
        val jobId = state.sortedJobs.single().jobId

        // Return only cue #1 (cues #2 and #3 are missing!)
        val afterAttempt1 = BatchQueueEngine.processSingleJobAttempt(
            currentState = state,
            jobId = jobId,
            translator = { _, _, _, _ ->
                Result.success(
                    BatchTranslationAttemptOutput(
                        translatedCues = mapOf(1 to "Kereta ke Distrik Shibuya berangkat sekarang.")
                    )
                )
            },
            outputWriter = writer
        )

        val job = afterAttempt1.sortedJobs.single()
        assertEquals(BatchJobStatus.RETRY_QUEUED, job.status)
        assertEquals(1, job.attemptCount)
        assertFalse(job.lastValidationResult!!.isValid)
        assertEquals(listOf(2, 3), job.lastValidationResult!!.missingCueIds)
        // Final file MUST NOT be written when validation fails
        assertFalse(File(tempOutputDir, "episode_01.srt").exists())
    }

    /**
     * Test E — Validasi teks masih dalam bahasa sumber
     * Simulasikan hasil terjemahan dengan beberapa entri masih belum diterjemahkan (identik dengan bahasa sumber).
     * Pastikan validator mendeteksinya dan memasukkan file ke retry queue.
     */
    @Test
    fun testE_untranslatedSourceLanguageTextFailsValidationAndRequeues() = runTest {
        val writer = LocalDirectoryBatchOutputWriter(tempOutputDir)
        val candidate = buildFourEpisodeCandidates().first()
        val sourceDoc = SubtitleParserAndMerger.parseSubtitle(candidate.fileName, candidate.rawContent)
        val state = BatchQueueEngine.enqueueFiles(BatchQueueState(maxRetries = 3), listOf(candidate))
        val jobId = state.sortedJobs.single().jobId

        // Cue #1 and #3 are translated, but Cue #2 is left identical to English source text
        val partialUntranslatedMap = mapOf(
            1 to "Kereta menuju Distrik Shibuya berangkat sekarang.",
            2 to sourceDoc.cues[1].cleanText, // Untranslated English sentence!
            3 to "Ya, setiap Penyihir Jujutsu di regu kita sudah siap."
        )

        val afterAttempt1 = BatchQueueEngine.processSingleJobAttempt(
            currentState = state,
            jobId = jobId,
            translator = { _, _, _, _ ->
                Result.success(BatchTranslationAttemptOutput(partialUntranslatedMap))
            },
            outputWriter = writer
        )

        val job = afterAttempt1.sortedJobs.single()
        assertEquals(BatchJobStatus.RETRY_QUEUED, job.status)
        assertFalse(job.lastValidationResult!!.isValid)
        assertTrue(2 in job.lastValidationResult!!.untranslatedCueIds)
        assertFalse(File(tempOutputDir, "episode_01.srt").exists())
    }

    /**
     * Test F — Validasi timestamp atau struktur rusak
     * Simulasikan hasil terjemahan dengan timestamp rusak atau urutan blok berubah.
     * Pastikan validator menolak hasil tersebut dan tidak menandai file sebagai selesai.
     */
    @Test
    fun testF_corruptedTimestampOrStructureFailsValidation() = runTest {
        val writer = LocalDirectoryBatchOutputWriter(tempOutputDir)
        val candidate = buildFourEpisodeCandidates().first()
        val state = BatchQueueEngine.enqueueFiles(BatchQueueState(maxRetries = 3), listOf(candidate))
        val jobId = state.sortedJobs.single().jobId

        val corruptedSrtContent = """
            1
            99:99:99,999 --> INVALID_TIMECODE
            Terjemahan baris pertama namun timestamp rusak.

            2
            00:00:04,500 --> 00:00:08,100
            Terjemahan baris kedua.
        """.trimIndent()

        val afterAttempt1 = BatchQueueEngine.processSingleJobAttempt(
            currentState = state,
            jobId = jobId,
            translator = { _, _, cues, _ ->
                Result.success(
                    BatchTranslationAttemptOutput(
                        translatedCues = translateAllCuesToIndonesian(cues),
                        customMergedContent = corruptedSrtContent
                    )
                )
            },
            outputWriter = writer
        )

        val job = afterAttempt1.sortedJobs.single()
        assertEquals(BatchJobStatus.RETRY_QUEUED, job.status)
        assertFalse(job.lastValidationResult!!.isValid)
        assertFalse(File(tempOutputDir, "episode_01.srt").exists())
    }

    /**
     * Test G — Mekanisme retry di akhir antrean
     * Antrean: 1. episode_01.srt, 2. episode_02.srt, 3. episode_03.srt, 4. episode_04.srt
     * Simulasikan episode_02.srt gagal validasi pada percobaan pertama lalu berhasil pada percobaan kedua.
     * Pastikan urutan eksekusi adalah:
     * 1. episode_01.srt (selesai)
     * 2. episode_02.srt (gagal validasi -> masuk akhir antrean sebagai episode_02.srt [RETRY #1])
     * 3. episode_03.srt (selesai)
     * 4. episode_04.srt (selesai)
     * 5. episode_02.srt [RETRY #1] (selesai)
     */
    @Test
    fun testG_failedFileIsMovedToEndOfQueueAndRetriedAfterWaitingFiles() = runTest {
        val writer = LocalDirectoryBatchOutputWriter(tempOutputDir)
        val state = BatchQueueEngine.enqueueFiles(
            BatchQueueState(maxRetries = 3),
            buildFourEpisodeCandidates()
        )

        val attemptExecutionLog = mutableListOf<String>()
        var queueOrderAfterEp2Fail: List<String> = emptyList()

        val translator = BatchCueTranslator { job, _, cuesToTranslate, _ ->
            attemptExecutionLog.add("${job.fileName}#attempt${job.attemptCount}")
            if (job.fileName == "episode_02.srt" && job.attemptCount == 1) {
                // Attempt #1 on episode_02.srt: omit cue #3 so validation fails!
                val incomplete = translateAllCuesToIndonesian(cuesToTranslate).filterKeys { it != 3 }
                Result.success(BatchTranslationAttemptOutput(incomplete))
            } else {
                // All other attempts succeed (and for episode_02 retry #1, targeted repair translates missing cue #3)
                Result.success(BatchTranslationAttemptOutput(translateAllCuesToIndonesian(cuesToTranslate)))
            }
        }

        val finalState = BatchQueueEngine.executeSequentialQueue(
            initialState = state,
            translator = translator,
            outputWriter = writer,
            onStateChanged = { intermediate ->
                val ep2 = intermediate.jobs.firstOrNull { it.fileName == "episode_02.srt" }
                if (ep2?.status == BatchJobStatus.RETRY_QUEUED && queueOrderAfterEp2Fail.isEmpty()) {
                    // Capture the remaining active queue order right after episode_02 fails attempt #1
                    queueOrderAfterEp2Fail = intermediate.sortedJobs
                        .filter { it.status == BatchJobStatus.QUEUED || it.status == BatchJobStatus.RETRY_QUEUED }
                        .map { it.displayTitleWithRetryTag }
                }
            }
        )

        // Verify that immediately after episode_02 failed Attempt #1, the remaining queue order was:
        // 1. episode_03.srt, 2. episode_04.srt, 3. episode_02.srt [RETRY #1]
        assertEquals(
            listOf("episode_03.srt", "episode_04.srt", "episode_02.srt [RETRY #1]"),
            queueOrderAfterEp2Fail
        )

        // Verify exact execution sequence of attempts: 1 -> 2 (fail) -> 3 -> 4 -> 2 (retry #1)
        assertEquals(
            listOf(
                "episode_01.srt#attempt1",
                "episode_02.srt#attempt1",
                "episode_03.srt#attempt1",
                "episode_04.srt#attempt1",
                "episode_02.srt#attempt2"
            ),
            attemptExecutionLog
        )

        assertEquals(4, finalState.completedCount)
        assertEquals(BatchTerminalOutcome.ALL_SUCCEEDED, finalState.terminalOutcome)
    }

    /**
     * Test H — Beberapa file gagal lalu berhasil pada retry
     * Simulasikan file ke-1 dan ke-3 gagal pada percobaan pertama, lalu berhasil pada percobaan kedua.
     * Pastikan keduanya masuk ke akhir antrean sesuai urutan kegagalan (1 -> 2 -> 3 -> 4 -> 1 [RETRY #1] -> 3 [RETRY #1]).
     */
    @Test
    fun testH_multipleFailedFilesRequeueAtEndInFailureOrderAndSucceedOnRetry() = runTest {
        val writer = LocalDirectoryBatchOutputWriter(tempOutputDir)
        val state = BatchQueueEngine.enqueueFiles(
            BatchQueueState(maxRetries = 3),
            buildFourEpisodeCandidates()
        )

        val attemptExecutionLog = mutableListOf<String>()

        val translator = BatchCueTranslator { job, _, cuesToTranslate, _ ->
            attemptExecutionLog.add("${job.fileName}#attempt${job.attemptCount}")
            if ((job.fileName == "episode_01.srt" || job.fileName == "episode_03.srt") && job.attemptCount == 1) {
                // Fail validation on attempt 1 by returning missing cue #2
                val incomplete = translateAllCuesToIndonesian(cuesToTranslate).filterKeys { it != 2 }
                Result.success(BatchTranslationAttemptOutput(incomplete))
            } else {
                Result.success(BatchTranslationAttemptOutput(translateAllCuesToIndonesian(cuesToTranslate)))
            }
        }

        val finalState = BatchQueueEngine.executeSequentialQueue(
            initialState = state,
            translator = translator,
            outputWriter = writer
        )

        assertEquals(
            listOf(
                "episode_01.srt#attempt1",
                "episode_02.srt#attempt1",
                "episode_03.srt#attempt1",
                "episode_04.srt#attempt1",
                "episode_01.srt#attempt2",
                "episode_03.srt#attempt2"
            ),
            attemptExecutionLog
        )
        assertEquals(4, finalState.completedCount)
        assertEquals(BatchTerminalOutcome.ALL_SUCCEEDED, finalState.terminalOutcome)
    }

    /**
     * Test I — Kegagalan berulang hingga batas maksimum retry
     * Simulasikan sebuah file selalu gagal validasi hingga mencapai batas maksimum retry.
     * Pastikan sistem berhenti mengulang file tersebut setelah batas maksimum, mengubah statusnya
     * menjadi Needs Attention, tidak mengalami loop tak terbatas, dan tidak menandai seluruh batch sebagai berhasil penuh.
     */
    @Test
    fun testI_repeatedFailureStopsAtMaxRetriesAndMarksNeedsAttention() = runTest {
        val writer = LocalDirectoryBatchOutputWriter(tempOutputDir)
        val candidates = buildFourEpisodeCandidates().take(2) // episode_01.srt and episode_02.srt
        val state = BatchQueueEngine.enqueueFiles(
            BatchQueueState(maxRetries = 3),
            candidates
        )

        val attemptLog = mutableListOf<String>()

        val translator = BatchCueTranslator { job, _, cuesToTranslate, _ ->
            attemptLog.add("${job.fileName}#attempt${job.attemptCount}")
            if (job.fileName == "episode_02.srt") {
                // Always fail validation on episode_02.srt
                Result.success(BatchTranslationAttemptOutput(emptyMap()))
            } else {
                Result.success(BatchTranslationAttemptOutput(translateAllCuesToIndonesian(cuesToTranslate)))
            }
        }

        val finalState = BatchQueueEngine.executeSequentialQueue(
            initialState = state,
            translator = translator,
            outputWriter = writer
        )

        val ep1 = finalState.jobs.first { it.fileName == "episode_01.srt" }
        val ep2 = finalState.jobs.first { it.fileName == "episode_02.srt" }

        assertEquals(BatchJobStatus.COMPLETED, ep1.status)
        assertEquals(BatchJobStatus.NEEDS_ATTENTION, ep2.status)
        assertEquals(3, ep2.attemptCount)
        assertEquals(3, ep2.attemptHistory.size)
        // Ensure episode_02 was NOT saved as a final file
        assertFalse(File(tempOutputDir, "episode_02.srt").exists())
        // Ensure batch is marked FINISHED_WITH_ISSUES, NOT ALL_SUCCEEDED
        assertEquals(BatchTerminalOutcome.FINISHED_WITH_ISSUES, finalState.terminalOutcome)
    }

    /**
     * Test J — Pause, resume, dan cancel
     * Uji penjedaan, pelanjutan, dan pembatalan antrean.
     * Pastikan tidak ada file yang rusak dan pekerjaan yang sudah Completed tidak diproses ulang.
     */
    @Test
    fun testJ_pauseResumeAndCancelPreserveCompletedFilesWithoutReprocessing() = runTest {
        val writer = LocalDirectoryBatchOutputWriter(tempOutputDir)
        val state = BatchQueueEngine.enqueueFiles(
            BatchQueueState(maxRetries = 3),
            buildFourEpisodeCandidates()
        )

        var shouldPauseNow = false
        val translatedFilesHistory = mutableListOf<String>()

        // 1. Run until episode_02 finishes, then pause before episode_03 starts
        val pausedState = BatchQueueEngine.executeSequentialQueue(
            initialState = state,
            translator = { job, _, cues, _ ->
                translatedFilesHistory.add(job.fileName)
                if (job.fileName == "episode_02.srt") {
                    shouldPauseNow = true
                }
                Result.success(BatchTranslationAttemptOutput(translateAllCuesToIndonesian(cues)))
            },
            outputWriter = writer,
            shouldPause = { shouldPauseNow }
        )

        assertTrue(pausedState.isPaused)
        assertFalse(pausedState.isRunning)
        assertEquals(2, pausedState.completedCount)
        assertEquals(2, pausedState.queuedCount)
        assertEquals(listOf("episode_01.srt", "episode_02.srt"), translatedFilesHistory)

        // 2. Resume queue and pause after episode_03 finishes, then Cancel the rest (episode_04)
        shouldPauseNow = false
        var shouldCancelNow = false
        val afterCancelState = BatchQueueEngine.executeSequentialQueue(
            initialState = pausedState,
            translator = { job, _, cues, _ ->
                translatedFilesHistory.add(job.fileName)
                if (job.fileName == "episode_03.srt") {
                    shouldCancelNow = true
                }
                Result.success(BatchTranslationAttemptOutput(translateAllCuesToIndonesian(cues)))
            },
            outputWriter = writer,
            shouldPause = { shouldPauseNow },
            shouldCancel = { shouldCancelNow }
        )

        // Verify episode_01 and episode_02 were NOT re-processed on resume!
        assertEquals(
            listOf("episode_01.srt", "episode_02.srt", "episode_03.srt"),
            translatedFilesHistory
        )
        assertTrue(afterCancelState.isCancelled)
        assertEquals(BatchTerminalOutcome.CANCELLED, afterCancelState.terminalOutcome)
        val ep4 = afterCancelState.jobs.first { it.fileName == "episode_04.srt" }
        assertEquals(BatchJobStatus.CANCELLED, ep4.status)
    }

    /**
     * Test K — Pemulihan setelah proses terputus
     * Simulasikan aplikasi atau worker berhenti di tengah antrean, lalu dijalankan kembali.
     * Pastikan antrean dapat dipulihkan dengan aman tanpa memproses ulang file yang sudah Completed.
     */
    @Test
    fun testK_crashRecoveryCleansUpTempFilesAndResumesWithoutReprocessingCompleted() = runTest {
        val writer = LocalDirectoryBatchOutputWriter(tempOutputDir)
        val initialState = BatchQueueEngine.enqueueFiles(
            BatchQueueState(maxRetries = 3, batchStartedAtMs = 1000L),
            buildFourEpisodeCandidates().take(3)
        )

        // Write valid completed output for episode_01.srt on disk
        val ep1File = File(tempOutputDir, "episode_01.srt").apply {
            writeText(SampleSubtitles.SAMPLE_BATCH_EPISODE_01_SRT)
        }
        // Write a leftover .tmp file simulating mid-write crash on episode_02.srt
        val orphanTempFile = File(tempOutputDir, ".episode_02.srt.12345.tmp").apply {
            writeText("PARTIAL_UNFINISHED_CONTENT")
        }

        val interruptedJobs = initialState.sortedJobs.map { job ->
            when (job.fileName) {
                "episode_01.srt" -> job.copy(
                    status = BatchJobStatus.COMPLETED,
                    outputFileName = "episode_01.srt",
                    outputFilePath = ep1File.absolutePath,
                    attemptCount = 1
                )
                "episode_02.srt" -> job.copy(
                    status = BatchJobStatus.PROCESSING, // Interrupted mid-process!
                    attemptCount = 1
                )
                else -> job // episode_03.srt is still QUEUED
            }
        }
        val persistedCrashState = initialState.copy(
            jobs = interruptedJobs,
            isRunning = true
        )

        // Recover state on startup
        val recoveredState = BatchQueueEngine.recoverPersistedBatchState(persistedCrashState, writer)

        // 1. Orphan .tmp file must be cleaned up
        assertFalse(orphanTempFile.exists())
        // 2. episode_01.srt remains COMPLETED; episode_02.srt transitions to RETRY_QUEUED
        assertEquals(BatchJobStatus.COMPLETED, recoveredState.jobs.first { it.fileName == "episode_01.srt" }.status)
        assertEquals(BatchJobStatus.RETRY_QUEUED, recoveredState.jobs.first { it.fileName == "episode_02.srt" }.status)
        assertEquals(BatchJobStatus.QUEUED, recoveredState.jobs.first { it.fileName == "episode_03.srt" }.status)

        // 3. Resume the recovered state and verify episode_01.srt is NOT re-translated
        val resumedExecutionFiles = mutableListOf<String>()
        val finalState = BatchQueueEngine.executeSequentialQueue(
            initialState = recoveredState,
            translator = { job, _, cues, _ ->
                resumedExecutionFiles.add(job.fileName)
                Result.success(BatchTranslationAttemptOutput(translateAllCuesToIndonesian(cues)))
            },
            outputWriter = writer
        )

        assertFalse("episode_01.srt" in resumedExecutionFiles)
        assertEquals(listOf("episode_02.srt", "episode_03.srt"), resumedExecutionFiles)
        assertEquals(3, finalState.completedCount)
    }

    /**
     * Test L — Notifikasi selesai ke ponsel
     * Pastikan notifikasi hanya dikirim ketika seluruh antrean benar-benar selesai dan isi pesannya
     * sesuai dengan hasil akhir (Kondisi A: Semua berhasil, Kondisi B: Ada file gagal, Kondisi C: Dibatalkan).
     */
    @Test
    fun testL_phoneNotificationsSentOnlyAtTerminalStateWithAccurateMessages() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val shadowNotificationManager = shadowOf(notificationManager)
        val helper = BatchNotificationHelper(context)
        val writer = LocalDirectoryBatchOutputWriter(tempOutputDir)

        val initialState = BatchQueueEngine.enqueueFiles(
            BatchQueueState(maxRetries = 2, notificationsEnabled = true),
            buildFourEpisodeCandidates().take(2)
        )

        // 1. While jobs are still QUEUED, no notification payload should be generated
        assertNull(BatchNotificationHelper.buildNotificationPayload(initialState))

        // 2. Condition A: All Succeeded
        val allSuccessState = BatchQueueEngine.executeSequentialQueue(
            initialState = initialState,
            translator = { _, _, cues, _ ->
                Result.success(BatchTranslationAttemptOutput(translateAllCuesToIndonesian(cues)))
            },
            outputWriter = writer
        )
        val payloadA = helper.notifyBatchTerminalState(allSuccessState)
        assertNotNull(payloadA)
        assertEquals("Batch Translation Completed", payloadA!!.title)
        assertEquals("Semua file subtitle berhasil diterjemahkan dan lolos validasi.", payloadA.message)
        assertTrue(shadowNotificationManager.allNotifications.isNotEmpty())

        // 3. Condition B: Finished with Issues (1 succeeded, 1 Needs Attention)
        val partialFailState = BatchQueueEngine.executeSequentialQueue(
            initialState = initialState,
            translator = { job, _, cues, _ ->
                if (job.fileName == "episode_02.srt") {
                    Result.failure(IllegalStateException("Simulated Rate Limit 429"))
                } else {
                    Result.success(BatchTranslationAttemptOutput(translateAllCuesToIndonesian(cues)))
                }
            },
            outputWriter = writer
        )
        val payloadB = helper.notifyBatchTerminalState(partialFailState)
        assertNotNull(payloadB)
        assertEquals("Batch Translation Finished with Issues", payloadB!!.title)
        assertEquals(
            "Antrean telah selesai diproses, tetapi beberapa file memerlukan perhatian.",
            payloadB.message
        )

        // 4. Condition C: Cancelled
        val cancelledState = BatchQueueEngine.cancelBatch(initialState)
        val payloadC = helper.notifyBatchTerminalState(cancelledState)
        assertNotNull(payloadC)
        assertEquals("Batch Translation Cancelled", payloadC!!.title)
        assertEquals(
            "Proses batch dibatalkan sebelum seluruh file selesai diterjemahkan.",
            payloadC.message
        )
    }

    /**
     * Test M — Konflik nama file pada output folder
     * Simulasikan file dengan nama yang sama sudah ada di folder output.
     * Pastikan sistem mengikuti kebijakan penanganan konflik dan tidak menimpa file secara diam-diam.
     */
    @Test
    fun testM_outputFolderFilenameConflictDoesNotOverwriteExistingFileSilently() = runTest {
        val writer = LocalDirectoryBatchOutputWriter(tempOutputDir)
        // Pre-create episode_01.srt in output folder
        val existingFile = File(tempOutputDir, "episode_01.srt").apply {
            writeText("EXISTING_ORIGINAL_FILE_DO_NOT_OVERWRITE", Charsets.UTF_8)
        }

        val state = BatchQueueEngine.enqueueFiles(
            BatchQueueState(
                outputFolderUriOrPath = tempOutputDir.absolutePath,
                conflictPolicy = FileConflictPolicy.RENAME_WITH_SUFFIX
            ),
            listOf(buildFourEpisodeCandidates().first())
        )

        val finalState = BatchQueueEngine.executeSequentialQueue(
            initialState = state,
            translator = { _, _, cues, _ ->
                Result.success(BatchTranslationAttemptOutput(translateAllCuesToIndonesian(cues)))
            },
            outputWriter = writer
        )

        val completedJob = finalState.sortedJobs.single()
        assertEquals(BatchJobStatus.COMPLETED, completedJob.status)
        // Existing file is untouched
        assertEquals("EXISTING_ORIGINAL_FILE_DO_NOT_OVERWRITE", existingFile.readText(Charsets.UTF_8))
        // New file is saved with safe suffix: episode_01 (1).srt
        assertEquals("episode_01 (1).srt", completedJob.outputFileName)
        assertTrue(File(tempOutputDir, "episode_01 (1).srt").exists())
    }

    /**
     * Test N — File sumber tidak valid atau rusak
     * Masukkan satu file subtitle yang rusak/kosong/format salah di antara beberapa file valid.
     * Pastikan file tidak valid ditangani dengan jelas pada praperiksa dan tidak menghentikan seluruh proses batch untuk file lain.
     */
    @Test
    fun testN_invalidOrCorruptSourceFilesHandledCleanlyWithoutBreakingValidFiles() = runTest {
        val writer = LocalDirectoryBatchOutputWriter(tempOutputDir)
        val mixedCandidates = listOf(
            BatchFileCandidate("broken_empty.srt", "file:///broken.srt", "   "),
            BatchFileCandidate("unsupported_video.mp4", "file:///video.mp4", "binary"),
            BatchFileCandidate("corrupt_no_cues.srt", "file:///corrupt.srt", "not a subtitle at all"),
            buildFourEpisodeCandidates()[0],
            buildFourEpisodeCandidates()[0], // Duplicate of episode_01.srt!
            buildFourEpisodeCandidates()[1]
        )

        val state = BatchQueueEngine.enqueueFiles(BatchQueueState(), mixedCandidates)
        // 4 invalid/duplicate candidates rejected, 2 valid files queued
        assertEquals(2, state.totalFiles)
        assertEquals(4, state.preFlightWarnings.size)

        val finalState = BatchQueueEngine.executeSequentialQueue(
            initialState = state,
            translator = { _, _, cues, _ ->
                Result.success(BatchTranslationAttemptOutput(translateAllCuesToIndonesian(cues)))
            },
            outputWriter = writer
        )

        assertEquals(2, finalState.completedCount)
        assertEquals(BatchTerminalOutcome.ALL_SUCCEEDED, finalState.terminalOutcome)
    }

    /**
     * Test O — Format subtitle yang didukung (.srt, .vtt, .ass, .ssa)
     * Uji seluruh format subtitle yang didukung oleh aplikasi pada mode batch, pastikan struktur
     * masing-masing format (WEBVTT header, [Script Info]/[Events] dan tag {\an8} pada .ass/.ssa) tetap valid.
     */
    @Test
    fun testO_allSupportedSubtitleFormatsSrtVttAssSsaPassBatchValidationAndPreserveStructure() = runTest {
        val writer = LocalDirectoryBatchOutputWriter(tempOutputDir)
        val multiFormatCandidates = SampleSubtitles.buildBatchEpisodeSampleCandidates() + listOf(
            BatchFileCandidate(
                fileName = "episode_05.ssa",
                sourceUriString = "sample://episode_05.ssa",
                rawContent = SampleSubtitles.SAMPLE_BATCH_EPISODE_04_ASS
            )
        )

        val state = BatchQueueEngine.enqueueFiles(BatchQueueState(), multiFormatCandidates)
        assertEquals(5, state.totalFiles)

        val finalState = BatchQueueEngine.executeSequentialQueue(
            initialState = state,
            translator = { _, _, cues, _ ->
                Result.success(BatchTranslationAttemptOutput(translateAllCuesToIndonesian(cues)))
            },
            outputWriter = writer
        )

        assertEquals(5, finalState.completedCount)
        assertEquals(BatchTerminalOutcome.ALL_SUCCEEDED, finalState.terminalOutcome)

        // Verify .vtt output kept WEBVTT header
        val vttContent = File(tempOutputDir, "episode_03.vtt").readText(Charsets.UTF_8)
        assertTrue(vttContent.startsWith("WEBVTT"))

        // Verify .ass and .ssa outputs kept [Script Info], [Events], and leading {\an8\pos(960,85)} tags
        val assContent = File(tempOutputDir, "episode_04.ass").readText(Charsets.UTF_8)
        assertTrue(assContent.contains("[Script Info]"))
        assertTrue(assContent.contains("[Events]"))
        assertTrue(assContent.contains("{\\an8\\pos(960,85)}"))

        val ssaContent = File(tempOutputDir, "episode_05.ssa").readText(Charsets.UTF_8)
        assertTrue(ssaContent.contains("[Events]"))
        assertTrue(ssaContent.contains("{\\an8\\pos(960,85)}"))
    }
}
