package com.example.model

import kotlin.math.roundToInt

/**
 * Status of an individual subtitle file job in the Batch Translation Queue.
 */
enum class BatchJobStatus(
    val displayLabel: String,
    val indonesianLabel: String,
    val isActiveOrPending: Boolean,
    val isTerminal: Boolean
) {
    QUEUED(
        displayLabel = "Queued",
        indonesianLabel = "Menunggu",
        isActiveOrPending = true,
        isTerminal = false
    ),
    PROCESSING(
        displayLabel = "Processing",
        indonesianLabel = "Sedang Diterjemahkan",
        isActiveOrPending = true,
        isTerminal = false
    ),
    VALIDATING(
        displayLabel = "Validating",
        indonesianLabel = "Validasi Kelengkapan",
        isActiveOrPending = true,
        isTerminal = false
    ),
    RETRY_QUEUED(
        displayLabel = "Retry Queued",
        indonesianLabel = "Menunggu Retry di Akhir Antrean",
        isActiveOrPending = true,
        isTerminal = false
    ),
    COMPLETED(
        displayLabel = "Completed",
        indonesianLabel = "Selesai & Lolos Validasi",
        isActiveOrPending = false,
        isTerminal = true
    ),
    FAILED(
        displayLabel = "Failed",
        indonesianLabel = "Gagal Permanen",
        isActiveOrPending = false,
        isTerminal = true
    ),
    NEEDS_ATTENTION(
        displayLabel = "Needs Attention",
        indonesianLabel = "Perlu Perhatian (Batas Retry Tercapai)",
        isActiveOrPending = false,
        isTerminal = true
    ),
    CANCELLED(
        displayLabel = "Cancelled",
        indonesianLabel = "Dibatalkan",
        isActiveOrPending = false,
        isTerminal = true
    )
}

/**
 * Policy for handling filename collisions in the Batch Output Folder.
 * Never overwrites existing files silently unless OVERWRITE is explicitly selected by the user.
 */
enum class FileConflictPolicy(
    val label: String,
    val description: String
) {
    RENAME_WITH_SUFFIX(
        label = "Buat Nama Alternatif (Aman)",
        description = "Contoh: episode_01 (1).srt — File yang sudah ada tidak akan ditimpa"
    ),
    SKIP_IF_EXISTS(
        label = "Lewati Jika Sudah Ada",
        description = "Jangan menimpa file yang sudah ada di folder output"
    ),
    OVERWRITE(
        label = "Timpa File (Konfirmasi Eksplisit)",
        description = "Ganti file tujuan setelah hasil terjemahan baru lolos validasi penuh"
    )
}

/**
 * Classification of why a batch attempt failed.
 */
enum class BatchFailureType(val label: String) {
    VALIDATION_FAILED("Gagal Validasi Struktur/Kelengkapan"),
    INFRASTRUCTURE_ERROR("Error API / Jaringan / Rate Limit / Timeout"),
    FILE_IO_ERROR("Gagal Baca/Tulis File Output")
}

/**
 * Detailed record of a single failed or completed translation attempt for a job.
 */
data class BatchAttemptRecord(
    val attemptNumber: Int,
    val timestampMs: Long,
    val passed: Boolean,
    val failureType: BatchFailureType? = null,
    val reason: String,
    val translatedCueCount: Int = 0,
    val totalSourceCues: Int = 0,
    val missingCueIds: List<Int> = emptyList(),
    val untranslatedCueIds: List<Int> = emptyList(),
    val invalidTimestampCueIds: List<Int> = emptyList()
)

/**
 * Result of structural and completeness validation on a translated subtitle document.
 */
data class BatchValidationResult(
    val isValid: Boolean,
    val totalSourceCues: Int,
    val translatedCueCount: Int,
    val missingCueIds: List<Int> = emptyList(),
    val emptyOrTruncatedCueIds: List<Int> = emptyList(),
    val untranslatedCueIds: List<Int> = emptyList(),
    val invalidTimestampCueIds: List<Int> = emptyList(),
    val outOfOrderCueIds: List<Int> = emptyList(),
    val structureErrors: List<String> = emptyList(),
    val summaryMessage: String
) {
    /**
     * Returns all cue IDs that need re-translation on a targeted retry attempt.
     */
    val failedCueIds: Set<Int>
        get() = (missingCueIds + emptyOrTruncatedCueIds + untranslatedCueIds + invalidTimestampCueIds + outOfOrderCueIds).toSet()
}

/**
 * Represents a single subtitle file job inside the Batch Subtitle Translation Queue.
 */
data class BatchJobItem(
    val jobId: String,
    val batchId: String,
    val fileName: String,
    val sourceUriString: String,
    val rawSourceContent: String,
    val detectedFormat: SubtitleFormat,
    val totalCues: Int,
    val queueOrder: Int,
    val status: BatchJobStatus = BatchJobStatus.QUEUED,
    val attemptCount: Int = 0,
    val maxRetries: Int = 5,
    /**
     * Cache of cues that have already passed individual validation in previous attempts,
     * enabling targeted repair of missing/untranslated entries on retry while preserving
     * valid entries from the original source structure.
     */
    val validTranslatedCues: Map<Int, String> = emptyMap(),
    val attemptHistory: List<BatchAttemptRecord> = emptyList(),
    val lastValidationResult: BatchValidationResult? = null,
    val outputFilePath: String? = null,
    val outputFileName: String? = null,
    val lastFailureReason: String? = null,
    val lastFailureType: BatchFailureType? = null,
    val startedAtMs: Long? = null,
    val completedAtMs: Long? = null,
    val translatedCuesProgress: Int = 0
) {
    /**
     * Human-readable queue label with retry tag if applicable, e.g.:
     * `episode_02.srt [RETRY #1]`
     */
    val displayTitleWithRetryTag: String
        get() = when {
            status == BatchJobStatus.RETRY_QUEUED && attemptCount > 0 ->
                "$fileName [RETRY #$attemptCount]"
            status == BatchJobStatus.PROCESSING && attemptCount > 1 ->
                "$fileName [RETRY #${attemptCount - 1}]"
            else -> fileName
        }

    val durationMs: Long?
        get() = if (startedAtMs != null && completedAtMs != null && completedAtMs >= startedAtMs) {
            completedAtMs - startedAtMs
        } else null
}

/**
 * Terminal outcome classification for the entire Batch Queue, used by both UI and Phone Notifications.
 */
enum class BatchTerminalOutcome {
    IN_PROGRESS_OR_IDLE,
    ALL_SUCCEEDED,
    FINISHED_WITH_ISSUES,
    CANCELLED
}

/**
 * Complete state of the Batch Subtitle Translation Queue.
 */
data class BatchQueueState(
    val batchId: String = "batch_default",
    val jobs: List<BatchJobItem> = emptyList(),
    val isRunning: Boolean = false,
    val isPaused: Boolean = false,
    val isCancelled: Boolean = false,
    val outputFolderUriOrPath: String = "",
    val outputFolderDisplayName: String = "Translated_Subtitles (Default App Folder)",
    val isOutputFolderWritable: Boolean = true,
    val outputFolderValidationMessage: String? = null,
    val conflictPolicy: FileConflictPolicy = FileConflictPolicy.RENAME_WITH_SUFFIX,
    val maxRetries: Int = 5,
    val notificationsEnabled: Boolean = true,
    val currentJobId: String? = null,
    val batchStartedAtMs: Long? = null,
    val batchFinishedAtMs: Long? = null,
    val lastNotificationTitle: String? = null,
    val lastNotificationMessage: String? = null,
    val preFlightWarnings: List<String> = emptyList(),
    val statusBannerMessage: String? = null
) {
    val sortedJobs: List<BatchJobItem>
        get() = jobs.sortedBy { it.queueOrder }

    val totalFiles: Int
        get() = jobs.size

    val completedCount: Int
        get() = jobs.count { it.status == BatchJobStatus.COMPLETED }

    val processingCount: Int
        get() = jobs.count { it.status == BatchJobStatus.PROCESSING || it.status == BatchJobStatus.VALIDATING }

    val queuedCount: Int
        get() = jobs.count { it.status == BatchJobStatus.QUEUED }

    val retryQueuedCount: Int
        get() = jobs.count { it.status == BatchJobStatus.RETRY_QUEUED }

    val needsAttentionCount: Int
        get() = jobs.count { it.status == BatchJobStatus.NEEDS_ATTENTION }

    val failedCount: Int
        get() = jobs.count { it.status == BatchJobStatus.FAILED }

    val cancelledCount: Int
        get() = jobs.count { it.status == BatchJobStatus.CANCELLED }

    val currentJob: BatchJobItem?
        get() = jobs.firstOrNull { it.jobId == currentJobId }
            ?: jobs.firstOrNull { it.status == BatchJobStatus.PROCESSING || it.status == BatchJobStatus.VALIDATING }

    val currentAttemptDisplay: Int
        get() = currentJob?.let { if (it.attemptCount > 0) it.attemptCount else 1 } ?: 0

    /**
     * Real overall progress percentage (0..100) based on validated completed files and
     * partial cue progress of the currently processing file.
     * Note: A file in RETRY_QUEUED or NEEDS_ATTENTION is never counted as 100% completed.
     */
    val overallProgressPercent: Int
        get() {
            if (jobs.isEmpty()) return 0
            val totalCueUnits = jobs.sumOf { it.totalCues.coerceAtLeast(1) }.toDouble()
            if (totalCueUnits <= 0.0) return 0

            val completedUnits = jobs.sumOf { job ->
                val jobTotal = job.totalCues.coerceAtLeast(1)
                when (job.status) {
                    BatchJobStatus.COMPLETED -> jobTotal.toDouble()
                    BatchJobStatus.VALIDATING -> jobTotal * 0.95
                    BatchJobStatus.PROCESSING -> job.translatedCuesProgress.coerceIn(0, jobTotal - 1).toDouble()
                    BatchJobStatus.RETRY_QUEUED -> (job.validTranslatedCues.size.coerceAtMost(jobTotal - 1)) * 0.5
                    else -> 0.0
                }
            }
            val rawPercent = ((completedUnits / totalCueUnits) * 100.0).roundToInt()
            return if (completedCount == jobs.size) 100 else rawPercent.coerceIn(0, 99)
        }

    /**
     * Estimated remaining time in milliseconds based on average duration of completed jobs.
     */
    val estimatedRemainingMs: Long?
        get() {
            val remainingJobs = queuedCount + retryQueuedCount + processingCount
            if (remainingJobs <= 0) return 0L
            val durations = jobs.mapNotNull { it.durationMs }.filter { it > 0L }
            if (durations.isEmpty()) return null
            val avgDurationMs = durations.average().toLong()
            return avgDurationMs * remainingJobs
        }

    /**
     * True only when the batch has jobs and NO job is left in QUEUED, PROCESSING, VALIDATING, or RETRY_QUEUED.
     */
    val isQueueCompletelyFinished: Boolean
        get() = jobs.isNotEmpty() && jobs.none { it.status.isActiveOrPending }

    /**
     * Determines the terminal outcome of the batch queue.
     * Strictly guarantees that ALL_SUCCEEDED is ONLY returned when EVERY job is COMPLETED.
     */
    val terminalOutcome: BatchTerminalOutcome
        get() {
            if (!isQueueCompletelyFinished) return BatchTerminalOutcome.IN_PROGRESS_OR_IDLE
            if (isCancelled || cancelledCount > 0) return BatchTerminalOutcome.CANCELLED
            if (completedCount == jobs.size && needsAttentionCount == 0 && failedCount == 0) {
                return BatchTerminalOutcome.ALL_SUCCEEDED
            }
            return BatchTerminalOutcome.FINISHED_WITH_ISSUES
        }
}
