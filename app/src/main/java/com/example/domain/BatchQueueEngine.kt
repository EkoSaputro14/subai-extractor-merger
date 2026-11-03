package com.example.domain

import com.example.model.BatchAttemptRecord
import com.example.model.BatchFailureType
import com.example.model.BatchJobItem
import com.example.model.BatchJobStatus
import com.example.model.BatchQueueState
import com.example.model.BatchValidationResult
import com.example.model.FileConflictPolicy
import com.example.model.GlossaryRule
import com.example.model.SubtitleCue
import com.example.model.SubtitleDocument
import com.example.model.SubtitleFormat
import java.io.File
import java.util.UUID

/**
 * Abstraction for translating subtitle cues for a single job attempt.
 * In production, backed by the existing GeminiWebCookieClient / OpenAiCompatibleClient pipeline.
 * In automated tests, can also be backed by deterministic fakes to simulate partial translations,
 * corrupted structures, rate limits, or pause/resume flows.
 */
fun interface BatchCueTranslator {
    suspend fun translateCues(
        job: BatchJobItem,
        sourceDocument: SubtitleDocument,
        cuesToTranslate: List<SubtitleCue>,
        onCueProgress: (translatedSoFar: Int) -> Unit
    ): Result<BatchTranslationAttemptOutput>
}

/**
 * Output produced by a [BatchCueTranslator] before or during validation.
 * Allows returning the translated cue map and optionally an overridden merged content string
 * (if a test or custom pipeline wants to test corrupted timestamps/headers).
 */
data class BatchTranslationAttemptOutput(
    val translatedCues: Map<Int, String>,
    val customMergedContent: String? = null,
    val pipelineReport: SingleTranslationPipelineExecutionReport? = null
)

/**
 * Abstraction for writing validated subtitle files to the dedicated Batch Output Folder.
 * Supports both standard filesystem directories ([LocalDirectoryBatchOutputWriter]) and
 * Android SAF DocumentTree URIs.
 */
interface BatchOutputFileWriter {
    fun isOutputFolderWritable(): Pair<Boolean, String?>
    fun writeValidatedSubtitleAtomically(
        desiredFileName: String,
        validatedContent: String,
        conflictPolicy: FileConflictPolicy
    ): Result<SavedBatchOutputFile>
    fun existsAndNonEmpty(outputFilePath: String?): Boolean
    fun cleanupTempFiles()
}

data class SavedBatchOutputFile(
    val finalFileName: String,
    val finalFilePath: String,
    val wasSkippedDueToConflict: Boolean = false
)

/**
 * Safe atomic file writer backed by a local directory (`java.io.File`).
 * Implements the mandatory 5-step atomic write flow:
 * 1. Translate subtitle (done by caller)
 * 2. Validate completeness & structure (done by caller)
 * 3. Write result to a temporary `.tmp` file
 * 4. Verify `.tmp` file was written and is non-empty
 * 5. Atomically move/rename `.tmp` file to the final file name after validation passes.
 */
class LocalDirectoryBatchOutputWriter(
    private val outputDirectory: File
) : BatchOutputFileWriter {

    override fun isOutputFolderWritable(): Pair<Boolean, String?> {
        return try {
            if (!outputDirectory.exists() && !outputDirectory.mkdirs()) {
                return false to "Folder output tidak dapat dibuat: ${outputDirectory.absolutePath}"
            }
            if (!outputDirectory.isDirectory) {
                return false to "Lokasi output bukan sebuah folder: ${outputDirectory.absolutePath}"
            }
            val probeFile = File(outputDirectory, ".batch_write_probe_${System.nanoTime()}.tmp")
            probeFile.writeText("ok", Charsets.UTF_8)
            probeFile.delete()
            true to null
        } catch (e: Exception) {
            false to "Folder output tidak dapat ditulisi: ${e.message ?: "Izin ditolak"}"
        }
    }

    override fun writeValidatedSubtitleAtomically(
        desiredFileName: String,
        validatedContent: String,
        conflictPolicy: FileConflictPolicy
    ): Result<SavedBatchOutputFile> {
        return runCatching {
            require(validatedContent.isNotBlank()) {
                "Tidak dapat menyimpan konten terjemahan kosong."
            }
            if (!outputDirectory.exists() && !outputDirectory.mkdirs()) {
                throw IllegalStateException("Gagal membuat folder output: ${outputDirectory.absolutePath}")
            }

            val sanitizedName = sanitizeFileName(desiredFileName)
            val resolvedFile = resolveTargetFile(outputDirectory, sanitizedName, conflictPolicy)
                ?: return@runCatching SavedBatchOutputFile(
                    finalFileName = sanitizedName,
                    finalFilePath = File(outputDirectory, sanitizedName).absolutePath,
                    wasSkippedDueToConflict = true
                )

            // Step 3: Write to temporary file first
            val tempFile = File(outputDirectory, ".${resolvedFile.name}.${UUID.randomUUID()}.tmp")
            try {
                tempFile.writeText(validatedContent, Charsets.UTF_8)

                // Step 4: Verify temporary file is written and non-empty
                if (!tempFile.exists() || tempFile.length() <= 0L) {
                    throw IllegalStateException("File temporer gagal ditulis atau berukuran 0 byte.")
                }
                val readBack = tempFile.readText(Charsets.UTF_8)
                if (readBack.isBlank()) {
                    throw IllegalStateException("Verifikasi baca ulang file temporer gagal.")
                }

                // Step 5: Move/rename temporary file to final file only after verification succeeds
                if (resolvedFile.exists() && conflictPolicy == FileConflictPolicy.OVERWRITE) {
                    resolvedFile.delete()
                }
                val renamed = tempFile.renameTo(resolvedFile)
                if (!renamed) {
                    tempFile.copyTo(resolvedFile, overwrite = conflictPolicy == FileConflictPolicy.OVERWRITE)
                    tempFile.delete()
                }
                if (!resolvedFile.exists() || resolvedFile.length() <= 0L) {
                    throw IllegalStateException("Gagal memindahkan file temporer menjadi file final.")
                }

                SavedBatchOutputFile(
                    finalFileName = resolvedFile.name,
                    finalFilePath = resolvedFile.absolutePath,
                    wasSkippedDueToConflict = false
                )
            } finally {
                if (tempFile.exists()) {
                    tempFile.delete()
                }
            }
        }
    }

    override fun existsAndNonEmpty(outputFilePath: String?): Boolean {
        if (outputFilePath.isNullOrBlank()) return false
        val file = File(outputFilePath)
        return file.exists() && file.isFile && file.length() > 0L
    }

    override fun cleanupTempFiles() {
        if (!outputDirectory.exists() || !outputDirectory.isDirectory) return
        outputDirectory.listFiles()?.forEach { file ->
            if (file.name.endsWith(".tmp") && file.name.startsWith(".")) {
                runCatching { file.delete() }
            }
        }
    }

    companion object {
        fun sanitizeFileName(rawName: String): String {
            val cleaned = rawName.trim().replace(Regex("""[\\/:*?"<>|]"""), "_")
            return cleaned.ifBlank { "translated_subtitle.srt" }
        }

        /**
         * Resolves filename conflict according to [conflictPolicy].
         * Returns `null` if [FileConflictPolicy.SKIP_IF_EXISTS] is active and the file already exists.
         */
        fun resolveTargetFile(
            directory: File,
            desiredFileName: String,
            conflictPolicy: FileConflictPolicy
        ): File? {
            val initialCandidate = File(directory, desiredFileName)
            if (!initialCandidate.exists()) {
                return initialCandidate
            }
            return when (conflictPolicy) {
                FileConflictPolicy.OVERWRITE -> initialCandidate
                FileConflictPolicy.SKIP_IF_EXISTS -> null
                FileConflictPolicy.RENAME_WITH_SUFFIX -> {
                    val dotIndex = desiredFileName.lastIndexOf('.')
                    val baseName = if (dotIndex > 0) desiredFileName.substring(0, dotIndex) else desiredFileName
                    val extension = if (dotIndex > 0) desiredFileName.substring(dotIndex) else ""
                    var counter = 1
                    var candidate = File(directory, "$baseName ($counter)$extension")
                    while (candidate.exists()) {
                        counter++
                        candidate = File(directory, "$baseName ($counter)$extension")
                    }
                    candidate
                }
            }
        }
    }
}

/**
 * Input candidate for adding a file into the Batch Subtitle Translation Queue.
 */
data class BatchFileCandidate(
    val fileName: String,
    val sourceUriString: String,
    val rawContent: String
)

/**
 * Core sequential processor, queue reorder manager, validator coordinator, and end-of-queue
 * retry state machine for the Batch Subtitle Translation Queue.
 */
object BatchQueueEngine {

    val SUPPORTED_EXTENSIONS = setOf("srt", "vtt", "ass", "ssa")

    /**
     * Checks whether a filename has a supported subtitle extension (`.srt`, `.vtt`, `.ass`, `.ssa`).
     */
    fun isSupportedSubtitleFileName(fileName: String): Boolean {
        val ext = fileName.substringAfterLast('.', "").lowercase().trim()
        return ext in SUPPORTED_EXTENSIONS
    }

    /**
     * Pre-flight validates and appends multiple subtitle files to the current [BatchQueueState].
     * - Rejects unsupported extensions, unreadable/blank files, or files with 0 subtitle cues.
     * - Rejects duplicate files already present in the queue (matching filename + content).
     * - Assigns sequential `queueOrder` following user selection order.
     */
    fun enqueueFiles(
        currentState: BatchQueueState,
        candidates: List<BatchFileCandidate>,
        clockMs: () -> Long = { System.currentTimeMillis() }
    ): BatchQueueState {
        val warnings = mutableListOf<String>()
        val existingJobs = currentState.sortedJobs.toMutableList()
        var nextOrder = (existingJobs.maxOfOrNull { it.queueOrder } ?: 0) + 1

        for (candidate in candidates) {
            val trimmedName = candidate.fileName.trim()
            if (!isSupportedSubtitleFileName(trimmedName)) {
                warnings.add("Dilewati '$trimmedName': Format tidak didukung (hanya .srt, .vtt, .ass, .ssa).")
                continue
            }
            if (candidate.rawContent.isBlank()) {
                warnings.add("Dilewati '$trimmedName': File kosong atau tidak dapat dibaca.")
                continue
            }
            val parsedDoc = SubtitleParserAndMerger.parseSubtitle(trimmedName, candidate.rawContent)
            if (parsedDoc.cues.isEmpty()) {
                warnings.add("Dilewati '$trimmedName': Tidak ditemukan entri subtitle yang valid di dalam file.")
                continue
            }
            val isDuplicate = existingJobs.any { existing ->
                existing.fileName.equals(trimmedName, ignoreCase = true) &&
                    existing.rawSourceContent == candidate.rawContent &&
                    existing.status != BatchJobStatus.CANCELLED
            }
            if (isDuplicate) {
                warnings.add("Dilewati '$trimmedName': File duplikat sudah ada di dalam antrean.")
                continue
            }

            val job = BatchJobItem(
                jobId = "job_${clockMs()}_${existingJobs.size + 1}_${trimmedName.hashCode().toUInt()}",
                batchId = currentState.batchId,
                fileName = trimmedName,
                sourceUriString = candidate.sourceUriString,
                rawSourceContent = candidate.rawContent,
                detectedFormat = parsedDoc.format,
                totalCues = parsedDoc.cues.size,
                queueOrder = nextOrder++,
                status = BatchJobStatus.QUEUED,
                attemptCount = 0,
                maxRetries = currentState.maxRetries
            )
            existingJobs.add(job)
        }

        val normalizedJobs = normalizeQueueOrder(existingJobs)
        return currentState.copy(
            jobs = normalizedJobs,
            preFlightWarnings = warnings,
            statusBannerMessage = if (warnings.isNotEmpty()) {
                "Ditambahkan ke antrean dengan ${warnings.size} peringatan praperiksa."
            } else {
                "${normalizedJobs.size} file siap dalam antrean Batch Translation."
            }
        )
    }

    /**
     * Moves a job up (earlier) in the queue before processing starts.
     */
    fun moveJobUp(state: BatchQueueState, jobId: String): BatchQueueState {
        val sorted = state.sortedJobs.toMutableList()
        val index = sorted.indexOfFirst { it.jobId == jobId }
        if (index <= 0) return state
        if (sorted[index].status == BatchJobStatus.PROCESSING ||
            sorted[index - 1].status == BatchJobStatus.PROCESSING
        ) {
            return state
        }
        val temp = sorted[index - 1]
        sorted[index - 1] = sorted[index]
        sorted[index] = temp
        val reindexed = sorted.mapIndexed { idx, item -> item.copy(queueOrder = idx + 1) }
        return state.copy(jobs = reindexed)
    }

    /**
     * Moves a job down (later) in the queue before processing starts.
     */
    fun moveJobDown(state: BatchQueueState, jobId: String): BatchQueueState {
        val sorted = state.sortedJobs.toMutableList()
        val index = sorted.indexOfFirst { it.jobId == jobId }
        if (index < 0 || index >= sorted.lastIndex) return state
        if (sorted[index].status == BatchJobStatus.PROCESSING ||
            sorted[index + 1].status == BatchJobStatus.PROCESSING
        ) {
            return state
        }
        val temp = sorted[index + 1]
        sorted[index + 1] = sorted[index]
        sorted[index] = temp
        val reindexed = sorted.mapIndexed { idx, item -> item.copy(queueOrder = idx + 1) }
        return state.copy(jobs = reindexed)
    }

    /**
     * Reorders jobs to match the explicit ordered list of `jobId`s.
     */
    fun reorderJobs(state: BatchQueueState, orderedJobIds: List<String>): BatchQueueState {
        val byId = state.jobs.associateBy { it.jobId }
        val reordered = mutableListOf<BatchJobItem>()
        for (id in orderedJobIds) {
            byId[id]?.let { reordered.add(it) }
        }
        for (job in state.sortedJobs) {
            if (reordered.none { it.jobId == job.jobId }) {
                reordered.add(job)
            }
        }
        val reindexed = reordered.mapIndexed { idx, item -> item.copy(queueOrder = idx + 1) }
        return state.copy(jobs = reindexed)
    }

    /**
     * Removes a job from the queue if it is not currently being processed.
     */
    fun removeJob(state: BatchQueueState, jobId: String): BatchQueueState {
        val target = state.jobs.firstOrNull { it.jobId == jobId } ?: return state
        if (target.status == BatchJobStatus.PROCESSING || target.status == BatchJobStatus.VALIDATING) {
            return state
        }
        val remaining = state.sortedJobs.filterNot { it.jobId == jobId }
        return state.copy(jobs = normalizeQueueOrder(remaining))
    }

    /**
     * Clears all COMPLETED jobs from the queue display without deleting their saved output files.
     */
    fun clearCompletedJobs(state: BatchQueueState): BatchQueueState {
        val remaining = state.sortedJobs.filterNot { it.status == BatchJobStatus.COMPLETED }
        return state.copy(jobs = normalizeQueueOrder(remaining))
    }

    /**
     * Re-queues jobs that are in NEEDS_ATTENTION or FAILED so the user can retry them
     * without re-translating already COMPLETED jobs.
     */
    fun retryFailedAndNeedsAttentionJobs(state: BatchQueueState): BatchQueueState {
        var maxOrder = state.jobs.maxOfOrNull { it.queueOrder } ?: 0
        val updated = state.sortedJobs.map { job ->
            if (job.status == BatchJobStatus.NEEDS_ATTENTION || job.status == BatchJobStatus.FAILED) {
                maxOrder++
                job.copy(
                    status = BatchJobStatus.RETRY_QUEUED,
                    attemptCount = 0,
                    maxRetries = state.maxRetries.coerceAtLeast(1),
                    queueOrder = maxOrder,
                    lastFailureReason = null
                )
            } else {
                job
            }
        }
        return state.copy(
            jobs = normalizeQueueOrder(updated),
            isCancelled = false,
            batchFinishedAtMs = null,
            statusBannerMessage = "File yang gagal telah dimasukkan kembali ke antrean retry."
        )
    }

    /**
     * Marks all pending (`QUEUED`, `RETRY_QUEUED`, `PROCESSING`, `VALIDATING`) jobs as `CANCELLED`
     * while preserving already `COMPLETED` jobs and their output files.
     */
    fun cancelBatch(state: BatchQueueState, nowMs: Long = System.currentTimeMillis()): BatchQueueState {
        val updated = state.sortedJobs.map { job ->
            if (job.status.isActiveOrPending) {
                job.copy(
                    status = BatchJobStatus.CANCELLED,
                    completedAtMs = nowMs
                )
            } else {
                job
            }
        }
        return state.copy(
            jobs = updated,
            isRunning = false,
            isPaused = false,
            isCancelled = true,
            currentJobId = null,
            batchFinishedAtMs = nowMs,
            statusBannerMessage = "Proses batch dibatalkan. Hasil file yang sudah selesai tetap aman."
        )
    }

    /**
     * Recovers persisted batch state after an app restart or process death:
     * - Verifies that `COMPLETED` jobs still have their output file on disk (if not, re-queues them).
     * - Resets any job interrupted in `PROCESSING` or `VALIDATING` back to `QUEUED` (or `RETRY_QUEUED`),
     *   cleaning up any temporary `.tmp` files via [outputWriter].
     */
    fun recoverPersistedBatchState(
        persistedState: BatchQueueState,
        outputWriter: BatchOutputFileWriter
    ): BatchQueueState {
        outputWriter.cleanupTempFiles()
        val recoveredJobs = persistedState.sortedJobs.map { job ->
            when (job.status) {
                BatchJobStatus.PROCESSING, BatchJobStatus.VALIDATING -> {
                    val restoredStatus = if (job.attemptCount > 0) {
                        BatchJobStatus.RETRY_QUEUED
                    } else {
                        BatchJobStatus.QUEUED
                    }
                    job.copy(
                        status = restoredStatus,
                        translatedCuesProgress = job.validTranslatedCues.size
                    )
                }
                BatchJobStatus.COMPLETED -> {
                    if (outputWriter.existsAndNonEmpty(job.outputFilePath)) {
                        job
                    } else {
                        // Output file was missing on disk; safely re-queue
                        job.copy(
                            status = BatchJobStatus.QUEUED,
                            outputFilePath = null,
                            outputFileName = null,
                            completedAtMs = null
                        )
                    }
                }
                else -> job
            }
        }

        val hasPendingWork = recoveredJobs.any {
            it.status == BatchJobStatus.QUEUED || it.status == BatchJobStatus.RETRY_QUEUED
        }

        return persistedState.copy(
            jobs = normalizeQueueOrder(recoveredJobs),
            isRunning = false,
            isPaused = hasPendingWork && persistedState.batchStartedAtMs != null,
            currentJobId = null,
            statusBannerMessage = if (hasPendingWork && persistedState.batchStartedAtMs != null) {
                "Antrean batch dipulihkan. Tekan Resume Queue untuk melanjutkan file yang tersisa."
            } else {
                persistedState.statusBannerMessage
            }
        )
    }

    /**
     * Selects the next eligible job (`QUEUED` or `RETRY_QUEUED`) strictly by `queueOrder`.
     */
    fun findNextRunnableJob(state: BatchQueueState): BatchJobItem? {
        return state.sortedJobs.firstOrNull {
            it.status == BatchJobStatus.QUEUED || it.status == BatchJobStatus.RETRY_QUEUED
        }
    }

    /**
     * Executes the Batch Subtitle Translation Queue sequentially one file at a time until:
     * - All jobs reach a terminal state (`COMPLETED`, `NEEDS_ATTENTION`, `FAILED`), OR
     * - `shouldPause()` returns true (clean pause between jobs or during a job), OR
     * - `shouldCancel()` returns true.
     *
     * Strictly enforces:
     * 1. One-by-one sequential execution (no parallel file processing).
     * 2. Completeness & structure validation via [BatchTranslationValidator] before saving.
     * 3. Atomic `.tmp` -> final file saving via [BatchOutputFileWriter].
     * 4. End-of-queue retry (`RETRY_QUEUED` moved to the back of the queue) when validation or API fails,
     *    so subsequent waiting files (`episode_03.srt`, `episode_04.srt`) are processed BEFORE
     *    the failed file (`episode_02.srt [RETRY #1]`) is retried!
     * 5. Bounded retries (`maxRetries`), transitioning to `NEEDS_ATTENTION` when exhausted.
     */
    suspend fun executeSequentialQueue(
        initialState: BatchQueueState,
        translator: BatchCueTranslator,
        outputWriter: BatchOutputFileWriter,
        translationConfig: SingleTranslationConfig? = null,
        glossaryRules: List<GlossaryRule> = translationConfig?.glossaryRules ?: emptyList(),
        protectAssStructuralTags: Boolean = translationConfig?.restoreLeadingAssTags ?: true,
        sourceLanguage: String = translationConfig?.sourceLanguage ?: "Auto",
        targetLanguage: String = translationConfig?.effectiveTargetLanguageLabel ?: "Indonesian (Bahasa Indonesia)",
        checkUntranslatedSourceLeakage: Boolean = translationConfig?.checkUntranslatedSourceLeakage ?: true,
        shouldPause: () -> Boolean = { false },
        shouldCancel: () -> Boolean = { false },
        clockMs: () -> Long = { System.currentTimeMillis() },
        onStateChanged: (BatchQueueState) -> Unit = {}
    ): BatchQueueState {
        // Validate output folder before starting
        val (writable, folderErr) = outputWriter.isOutputFolderWritable()
        if (!writable) {
            val blockedState = initialState.copy(
                isRunning = false,
                isOutputFolderWritable = false,
                outputFolderValidationMessage = folderErr ?: "Folder output tidak dapat diakses.",
                statusBannerMessage = folderErr ?: "Folder output tidak dapat diakses."
            )
            onStateChanged(blockedState)
            return blockedState
        }

        var state = initialState.copy(
            isRunning = true,
            isPaused = false,
            isCancelled = false,
            isOutputFolderWritable = true,
            outputFolderValidationMessage = null,
            batchStartedAtMs = initialState.batchStartedAtMs ?: clockMs(),
            batchFinishedAtMs = null
        )
        onStateChanged(state)

        while (true) {
            if (shouldCancel()) {
                state = cancelBatch(state, clockMs())
                onStateChanged(state)
                return state
            }
            if (shouldPause()) {
                state = state.copy(
                    isRunning = false,
                    isPaused = true,
                    currentJobId = null,
                    statusBannerMessage = "Antrean dijeda (Paused). Tekan Resume untuk melanjutkan."
                )
                onStateChanged(state)
                return state
            }

            val nextJob = findNextRunnableJob(state) ?: break

            state = processSingleJobAttempt(
                currentState = state,
                jobId = nextJob.jobId,
                translator = translator,
                outputWriter = outputWriter,
                translationConfig = translationConfig,
                glossaryRules = glossaryRules,
                protectAssStructuralTags = protectAssStructuralTags,
                sourceLanguage = sourceLanguage,
                targetLanguage = targetLanguage,
                checkUntranslatedSourceLeakage = checkUntranslatedSourceLeakage,
                shouldPause = shouldPause,
                shouldCancel = shouldCancel,
                clockMs = clockMs,
                onStateChanged = onStateChanged
            )
        }

        val finishedState = state.copy(
            isRunning = false,
            isPaused = false,
            currentJobId = null,
            batchFinishedAtMs = clockMs(),
            statusBannerMessage = when {
                state.completedCount == state.totalFiles && state.totalFiles > 0 ->
                    "Semua ${state.totalFiles} file subtitle berhasil diterjemahkan dan lolos validasi."
                state.needsAttentionCount > 0 || state.failedCount > 0 ->
                    "Antrean selesai: ${state.completedCount} berhasil, ${state.needsAttentionCount + state.failedCount} perlu perhatian."
                else -> state.statusBannerMessage
            }
        )
        onStateChanged(finishedState)
        return finishedState
    }

    /**
     * Processes a single attempt for [jobId] using the shared [SingleTranslationPipeline].
     * - Always parses from `job.rawSourceContent` (never from a corrupted or failed output).
     * - Always translates from the full `sourceDocument.cues` so every retry preserves full
     *   chunking and dialogue context identically to Single Translation.
     * - Reconstructs and validates the subtitle using the exact [SingleTranslationConfig] rules.
     */
    suspend fun processSingleJobAttempt(
        currentState: BatchQueueState,
        jobId: String,
        translator: BatchCueTranslator,
        outputWriter: BatchOutputFileWriter,
        translationConfig: SingleTranslationConfig? = null,
        glossaryRules: List<GlossaryRule> = translationConfig?.glossaryRules ?: emptyList(),
        protectAssStructuralTags: Boolean = translationConfig?.restoreLeadingAssTags ?: true,
        sourceLanguage: String = translationConfig?.sourceLanguage ?: "Auto",
        targetLanguage: String = translationConfig?.effectiveTargetLanguageLabel ?: "Indonesian (Bahasa Indonesia)",
        checkUntranslatedSourceLeakage: Boolean = translationConfig?.checkUntranslatedSourceLeakage ?: true,
        shouldPause: () -> Boolean = { false },
        shouldCancel: () -> Boolean = { false },
        clockMs: () -> Long = { System.currentTimeMillis() },
        onStateChanged: (BatchQueueState) -> Unit = {}
    ): BatchQueueState {
        val targetJob = currentState.jobs.firstOrNull { it.jobId == jobId } ?: return currentState
        val newAttemptNumber = targetJob.attemptCount + 1
        val attemptStartMs = clockMs()

        val effectiveConfig = (translationConfig ?: SingleTranslationConfig()).copy(
            glossaryRules = glossaryRules,
            autoApplyGlossaryBeforeMerge = translationConfig?.autoApplyGlossaryBeforeMerge
                ?: glossaryRules.isNotEmpty(),
            restoreLeadingAssTags = protectAssStructuralTags,
            sourceLanguage = sourceLanguage,
            checkUntranslatedSourceLeakage = checkUntranslatedSourceLeakage
        )

        // 1. Mark job as PROCESSING
        val processingJob = targetJob.copy(
            status = BatchJobStatus.PROCESSING,
            attemptCount = newAttemptNumber,
            startedAtMs = targetJob.startedAtMs ?: attemptStartMs,
            translatedCuesProgress = 0
        )
        var state = updateJobInState(currentState, processingJob).copy(
            currentJobId = jobId,
            statusBannerMessage = "Menerjemahkan ${processingJob.displayTitleWithRetryTag} (Percobaan #$newAttemptNumber/${processingJob.maxRetries})..."
        )
        onStateChanged(state)

        // Always parse from the original immutable rawSourceContent
        val sourceDocument = SubtitleParserAndMerger.parseSubtitle(
            fileName = processingJob.fileName,
            rawContent = processingJob.rawSourceContent
        )

        // Always pass the full original sourceDocument.cues so retries run the complete Single Translation Pipeline
        // from the original source file with full chunk context (never translating isolated/broken fragments).
        val cuesToTranslate = sourceDocument.cues

        // 2. Invoke Single Translation Pipeline via translator
        val translationResult = translator.translateCues(
            job = processingJob,
            sourceDocument = sourceDocument,
            cuesToTranslate = cuesToTranslate,
            onCueProgress = { translatedSoFar ->
                val totalDone = translatedSoFar.coerceIn(0, sourceDocument.cues.size)
                val updatedJob = processingJob.copy(translatedCuesProgress = totalDone)
                state = updateJobInState(state, updatedJob)
                onStateChanged(state)
            }
        ).flattenNestedResult()

        // Check if cancelled or paused during translation
        if (shouldCancel()) {
            return cancelBatch(state, clockMs())
        }
        if (shouldPause() && translationResult.isFailure) {
            // Revert attempt increment if paused cleanly mid-flight
            val pausedJob = targetJob.copy(
                status = if (targetJob.attemptCount > 0) BatchJobStatus.RETRY_QUEUED else BatchJobStatus.QUEUED
            )
            return updateJobInState(state, pausedJob).copy(
                isRunning = false,
                isPaused = true,
                currentJobId = null
            )
        }

        if (translationResult.isFailure) {
            val errorMsg = translationResult.exceptionOrNull()?.message
                ?: "Gagal menghubungi layanan AI saat menerjemahkan subtitle."
            return handleFailedAttemptAndRequeueAtEnd(
                state = state,
                job = processingJob,
                attemptNumber = newAttemptNumber,
                failureType = BatchFailureType.INFRASTRUCTURE_ERROR,
                reason = errorMsg,
                validationResult = null,
                newlyValidCues = emptyMap(),
                nowMs = clockMs(),
                onStateChanged = onStateChanged
            )
        }

        val attemptOutput = translationResult.getOrThrow()
        val combinedTranslatedMap = LinkedHashMap<Int, String>().apply {
            putAll(attemptOutput.translatedCues)
        }

        // 3. Transition job to VALIDATING
        val validatingJob = processingJob.copy(
            status = BatchJobStatus.VALIDATING,
            translatedCuesProgress = combinedTranslatedMap.size
        )
        state = updateJobInState(state, validatingJob).copy(
            statusBannerMessage = "Memvalidasi struktur & kelengkapan ${validatingJob.fileName}..."
        )
        onStateChanged(state)

        // Apply post-processing glossary rules identically to Single Translation
        val finalTranslationsForMerge = SingleTranslationPipeline.applyPostProcessing(
            rawTranslations = combinedTranslatedMap,
            config = effectiveConfig
        )

        // Generate merged subtitle content using SingleTranslationPipeline.reconstructMergedContent
        // (unless customMergedContent was explicitly supplied by a unit test testing corrupted timestamps)
        val mergedContent = attemptOutput.customMergedContent
            ?: SingleTranslationPipeline.reconstructMergedContent(
                document = sourceDocument,
                translations = finalTranslationsForMerge,
                config = effectiveConfig
            )

        // 4. Run structural & completeness validation matching Single Translation criteria
        val validation = BatchTranslationValidator.validateSubtitleTranslation(
            sourceDocument = sourceDocument,
            translatedMap = finalTranslationsForMerge,
            mergedFileContent = mergedContent,
            sourceLanguage = sourceLanguage,
            targetLanguage = targetLanguage,
            checkUntranslatedSourceLeakage = checkUntranslatedSourceLeakage,
            expectLeadingAssTagsRestored = effectiveConfig.restoreLeadingAssTags
        )

        if (!validation.isValid) {
            return handleFailedAttemptAndRequeueAtEnd(
                state = state,
                job = validatingJob,
                attemptNumber = newAttemptNumber,
                failureType = BatchFailureType.VALIDATION_FAILED,
                reason = validation.summaryMessage,
                validationResult = validation,
                newlyValidCues = emptyMap(),
                nowMs = clockMs(),
                onStateChanged = onStateChanged
            )
        }

        // 5. Validation passed! Perform safe atomic write (.tmp -> verify -> final file)
        val writeResult = outputWriter.writeValidatedSubtitleAtomically(
            desiredFileName = validatingJob.fileName,
            validatedContent = mergedContent,
            conflictPolicy = state.conflictPolicy
        )

        if (writeResult.isFailure) {
            val ioError = writeResult.exceptionOrNull()?.message ?: "Gagal menyimpan file ke folder output."
            return handleFailedAttemptAndRequeueAtEnd(
                state = state,
                job = validatingJob,
                attemptNumber = newAttemptNumber,
                failureType = BatchFailureType.FILE_IO_ERROR,
                reason = ioError,
                validationResult = validation,
                newlyValidCues = combinedTranslatedMap,
                nowMs = clockMs(),
                onStateChanged = onStateChanged
            )
        }

        val savedFile = writeResult.getOrThrow()
        val completedRecord = BatchAttemptRecord(
            attemptNumber = newAttemptNumber,
            timestampMs = clockMs(),
            passed = true,
            failureType = null,
            reason = validation.summaryMessage,
            translatedCueCount = validation.translatedCueCount,
            totalSourceCues = validation.totalSourceCues
        )

        val completedJob = validatingJob.copy(
            status = BatchJobStatus.COMPLETED,
            validTranslatedCues = combinedTranslatedMap,
            attemptHistory = validatingJob.attemptHistory + completedRecord,
            lastValidationResult = validation,
            outputFilePath = savedFile.finalFilePath,
            outputFileName = savedFile.finalFileName,
            lastFailureReason = null,
            lastFailureType = null,
            completedAtMs = clockMs(),
            translatedCuesProgress = sourceDocument.cues.size
        )

        state = updateJobInState(state, completedJob).copy(
            statusBannerMessage = "Selesai & tervalidasi: ${savedFile.finalFileName}"
        )
        onStateChanged(state)
        return state
    }

    /**
     * Handles a failed attempt:
     * - Records the attempt in `attemptHistory`.
     * - Does NOT save partial/invalid output as final output.
     * - If `attemptNumber < job.maxRetries`, sets status to `RETRY_QUEUED` and moves the job
     *   to the **very end of the queue** (`maxQueueOrder + 1`) so all subsequent waiting files
     *   are processed before this file is retried.
     * - If `attemptNumber >= job.maxRetries`, sets status to `NEEDS_ATTENTION` (never loops forever).
     */
    private fun handleFailedAttemptAndRequeueAtEnd(
        state: BatchQueueState,
        job: BatchJobItem,
        attemptNumber: Int,
        failureType: BatchFailureType,
        reason: String,
        validationResult: BatchValidationResult?,
        newlyValidCues: Map<Int, String>,
        nowMs: Long,
        onStateChanged: (BatchQueueState) -> Unit
    ): BatchQueueState {
        val attemptRecord = BatchAttemptRecord(
            attemptNumber = attemptNumber,
            timestampMs = nowMs,
            passed = false,
            failureType = failureType,
            reason = reason,
            translatedCueCount = validationResult?.translatedCueCount ?: newlyValidCues.size,
            totalSourceCues = job.totalCues,
            missingCueIds = validationResult?.missingCueIds ?: emptyList(),
            untranslatedCueIds = validationResult?.untranslatedCueIds ?: emptyList(),
            invalidTimestampCueIds = validationResult?.invalidTimestampCueIds ?: emptyList()
        )

        val updatedHistory = job.attemptHistory + attemptRecord
        val maxRetries = job.maxRetries.coerceAtLeast(1)

        if (attemptNumber < maxRetries) {
            // Move to the VERY END of the queue so subsequent waiting files run first
            val currentMaxOrder = state.jobs.maxOfOrNull { it.queueOrder } ?: 0
            val requeuedJob = job.copy(
                status = BatchJobStatus.RETRY_QUEUED,
                attemptCount = attemptNumber,
                queueOrder = currentMaxOrder + 1,
                validTranslatedCues = newlyValidCues,
                attemptHistory = updatedHistory,
                lastValidationResult = validationResult,
                lastFailureReason = reason,
                lastFailureType = failureType,
                translatedCuesProgress = newlyValidCues.size
            )
            val reorderedJobs = normalizeQueueOrder(
                state.jobs.map { if (it.jobId == job.jobId) requeuedJob else it }
            )
            val nextState = state.copy(
                jobs = reorderedJobs,
                statusBannerMessage = "${job.fileName} belum lolos validasi (Percobaan #$attemptNumber) -> Dipindah ke akhir antrean [RETRY #$attemptNumber]."
            )
            onStateChanged(nextState)
            return nextState
        } else {
            // Exhausted maxRetries -> Mark as NEEDS_ATTENTION (never infinite-loop)
            val exhaustedJob = job.copy(
                status = BatchJobStatus.NEEDS_ATTENTION,
                attemptCount = attemptNumber,
                validTranslatedCues = newlyValidCues,
                attemptHistory = updatedHistory,
                lastValidationResult = validationResult,
                lastFailureReason = "Batas maksimum $maxRetries percobaan tercapai. Alasan terakhir: $reason",
                lastFailureType = failureType,
                completedAtMs = nowMs
            )
            val nextState = updateJobInState(state, exhaustedJob).copy(
                statusBannerMessage = "${job.fileName} memerlukan perhatian setelah $attemptNumber percobaan gagal."
            )
            onStateChanged(nextState)
            return nextState
        }
    }

    private fun updateJobInState(state: BatchQueueState, updatedJob: BatchJobItem): BatchQueueState {
        val updatedList = state.jobs.map { if (it.jobId == updatedJob.jobId) updatedJob else it }
        return state.copy(jobs = normalizeQueueOrder(updatedList))
    }

    private fun normalizeQueueOrder(jobs: List<BatchJobItem>): List<BatchJobItem> {
        return jobs.sortedBy { it.queueOrder }.mapIndexed { index, item ->
            item.copy(queueOrder = index + 1)
        }
    }
}
