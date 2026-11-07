package com.example.model

import android.net.Uri
import com.example.domain.BatchFileCandidate
import java.text.NumberFormat
import java.util.Locale

/**
 * Navigation routes within the SubAI Studio MVI Linear Batch Workspace.
 */
enum class BatchWorkspaceRoute(
    val headerSubtitle: String,
    val showBackButton: Boolean
) {
    QUEUE_HUB(
        headerSubtitle = "Batch Translation Hub",
        showBackButton = false
    ),
    CORE_EXTRACT(
        headerSubtitle = "Subtitle Extractor & Tag Filter",
        showBackButton = true
    ),
    CORE_TRANSLATE(
        headerSubtitle = "Live AI Stream Workspace",
        showBackButton = true
    ),
    CORE_MERGE(
        headerSubtitle = "Format Integrity Checker",
        showBackButton = true
    ),
    MODAL_INSPECTION(
        headerSubtitle = "Forensic Cue Error Inspector",
        showBackButton = true
    ),
    MODAL_SETTINGS(
        headerSubtitle = "AI Engine & Kredensial",
        showBackButton = true
    ),
    MODAL_STORAGE(
        headerSubtitle = "SAF Scoped Storage & Conflict",
        showBackButton = true
    ),
    MODAL_GLOSSARY(
        headerSubtitle = "Kamus Istilah & Karakter",
        showBackButton = true
    )
}

/**
 * UI representation of a pre-flight validated subtitle file in the Queue.
 */
data class QueueFileItem(
    val jobId: String,
    val orderNumber: Int,
    val fileName: String,
    val cueCount: Int,
    val fileSizeLabel: String,
    val formatLabel: String,
    val statusBadge: String = "SIAP",
    val status: BatchJobStatus = BatchJobStatus.QUEUED
) {
    val formattedOrderPrefix: String
        get() = String.format(Locale.US, "%02d", orderNumber)

    val displayMetadataLine: String
        get() = "$cueCount Cues · $fileSizeLabel · Format $formatLabel"
}

/**
 * Audit line item displayed in Modal_01_Inspection.png (Forensic Cue Error Inspector).
 */
data class ForensicCueAuditItem(
    val cueId: Int,
    val timestampLabel: String,
    val isMissingOrFailed: Boolean,
    val statusLabel: String = if (isMissingOrFailed) "MISSING (Teks Hilang)" else "Lolos Terjemah"
)

/**
 * Summary statistics displayed in State_04_Completed.png.
 */
data class BatchSummaryStats(
    val totalFiles: Int,
    val totalCues: Int,
    val outputFolderPath: String = "/storage/SubAI/Translated/"
) {
    val formattedTotalCues: String
        get() = NumberFormat.getNumberInstance(Locale.US).format(totalCues)
}

/**
 * 1. DEFINISI STATE APLIKASI (QueueUiState):
 * Aplikasi HANYA boleh berada dalam salah satu dari 6 state MVI berikut.
 */
sealed interface QueueUiState {

    /**
     * State.Empty -> Tampilkan State_01_Empty.png (antrean kosong, tombol picker SAF).
     */
    data object Empty : QueueUiState

    /**
     * State.ReadyToStart -> Tampilkan Core_Screen_01_Queue.png
     * (file termuat, pre-flight lolos 100%, tombol "Mulai Batch" aktif).
     */
    data class ReadyToStart(
        val files: List<QueueFileItem>,
        val totalCues: Int,
        val preFlightPassedPercent: Int = 100,
        val estimatedTimeLabel: String = "~03m 15s",
        val estimatedTokensLabel: String = "~12.5k Tokens",
        val lockedConfigLocked: Boolean = false
    ) : QueueUiState {
        val formattedTotalCues: String
            get() = NumberFormat.getNumberInstance(Locale.US).format(totalCues)
    }

    /**
     * State.Processing(currentFile, cueIndex, totalCues, eta) -> Tampilkan State_02_Active.png
     * (hero progress card aktif, antrean berjalan sekuensial).
     */
    data class Processing(
        val currentFile: String,
        val cueIndex: Int,
        val totalCues: Int,
        val eta: String,
        val progressPercent: Int = if (totalCues > 0) ((cueIndex * 100) / totalCues).coerceIn(1, 99) else 32,
        val currentSourceLinePreview: String = "[48] {\\pos(192,240)}Sono hi, jinrui wa omoidashita...",
        val currentTranslatedLinePreview: String = "[48] {\\pos(192,240)}Pada hari itu, umat manusia teringat.."
    ) : QueueUiState

    /**
     * State.Paused(currentFile, cueIndex) -> Tampilkan State_03_Paused.png.
     */
    data class Paused(
        val currentFile: String,
        val cueIndex: Int,
        val totalCues: Int = 150
    ) : QueueUiState

    /**
     * State.NeedsAttention(failedFile, errorList) -> Buka Modal_01_Inspection.png
     * (eksekusi targeted retry 5x di akhir antrean).
     */
    data class NeedsAttention(
        val failedFile: String,
        val errorList: List<String>,
        val failedJobId: String = "",
        val missingCueIds: List<Int> = listOf(42, 43),
        val retryAttemptCount: Int = 5,
        val maxRetries: Int = 5,
        val cueAuditItems: List<ForensicCueAuditItem> = defaultForensicAuditItems(missingCueIds)
    ) : QueueUiState {
        val targetedRepairButtonLabel: String
            get() = when {
                missingCueIds.size == 2 -> "Perbaiki Cue ${missingCueIds[0]} & ${missingCueIds[1]} Saja"
                missingCueIds.size == 1 -> "Perbaiki Cue ${missingCueIds[0]} Saja"
                missingCueIds.isNotEmpty() -> "Perbaiki ${missingCueIds.size} Cue Saja"
                else -> "Perbaiki Cue Gagal Saja"
            }

        companion object {
            fun defaultForensicAuditItems(missingIds: List<Int>): List<ForensicCueAuditItem> {
                val targetMissing = if (missingIds.isNotEmpty()) missingIds else listOf(42, 43)
                val firstMissing = targetMissing.first()
                val secondMissing = targetMissing.getOrElse(1) { firstMissing + 1 }
                val prevId = (firstMissing - 1).coerceAtLeast(1)
                val nextId = secondMissing + 1
                return listOf(
                    ForensicCueAuditItem(
                        cueId = prevId,
                        timestampLabel = "00:03:12.100",
                        isMissingOrFailed = false
                    ),
                    ForensicCueAuditItem(
                        cueId = firstMissing,
                        timestampLabel = "00:03:15.500",
                        isMissingOrFailed = true
                    ),
                    ForensicCueAuditItem(
                        cueId = secondMissing,
                        timestampLabel = "00:03:18.200",
                        isMissingOrFailed = true
                    ),
                    ForensicCueAuditItem(
                        cueId = nextId,
                        timestampLabel = "00:03:22.000",
                        isMissingOrFailed = false
                    )
                )
            }
        }
    }

    /**
     * State.Completed(summaryStats) -> Tampilkan State_04_Completed.png
     * (perayaan selesai, buka SAF).
     */
    data class Completed(
        val summaryStats: BatchSummaryStats
    ) : QueueUiState
}

/**
 * 2. DAFTAR EVENT PENGGUNA (QueueUiEvent):
 * Setiap interaksi pengguna HARUS mengubah state melalui event eksplisit.
 */
sealed interface QueueUiEvent {
    /**
     * Event.OnAddFiles(uris: List<Uri>)
     * -> Sistem menjalankan PreFlightCheckValidator.
     * -> Jika valid, masukkan ke SequentialQueue, ubah state ke ReadyToStart.
     */
    data class OnAddFiles(val uris: List<Uri>) : QueueUiEvent

    /**
     * Varian kandidat langsung (untuk pemuatan sampel episode atau pengujian deterministik).
     */
    data class OnAddFileCandidates(val candidates: List<BatchFileCandidate>) : QueueUiEvent

    /**
     * Memuat 4 berkas sampel siap uji (Shingeki_EP01.ass s/d Shingeki_EP04.srt).
     */
    data object OnLoadSampleFiles : QueueUiEvent

    /**
     * Event.OnStartBatch
     * -> Sistem mengunci konfigurasi (Target Lang, Preset, SAF Path).
     * -> Jalankan Coroutine Worker: Eksekusi File 01 -> Chunking -> Streaming -> 7-Stage Validation.
     * -> Jika lulus, commit file ke Scoped Storage, lanjut otomatis ke File 02.
     */
    data object OnStartBatch : QueueUiEvent

    /**
     * Menjeda proses batch yang sedang berjalan -> State.Paused(currentFile, cueIndex).
     */
    data object OnPauseBatch : QueueUiEvent

    /**
     * Melanjutkan proses batch dari posisi cue yang dijeda -> State.Processing.
     */
    data object OnResumeBatch : QueueUiEvent

    /**
     * Membatalkan proses batch aktif.
     */
    data object OnCancelBatch : QueueUiEvent

    /**
     * Event.OnQueueFailed(file, error)
     * -> JANGAN HENTIKAN BATCH! Pindahkan file gagal ke antrean paling akhir (End-of-Queue Retry).
     * -> Lanjutkan file berikutnya. Jika sudah 5x retry tetap gagal, trigger State.NeedsAttention.
     */
    data class OnQueueFailed(
        val file: String,
        val error: String,
        val jobId: String = "",
        val missingCueIds: List<Int> = listOf(42, 43)
    ) : QueueUiEvent

    /**
     * Event.OnOpenInspectionModal(fileId)
     * -> Tampilkan perbandingan diff cue yang hilang (Modal 01).
     */
    data class OnOpenInspectionModal(val fileId: String) : QueueUiEvent

    /**
     * Eksekusi perbaikan terarah (Targeted Repair) hanya pada cue yang hilang di Modal 01.
     */
    data class OnTargetedRepairFailedCues(
        val fileId: String,
        val cueIds: List<Int> = emptyList()
    ) : QueueUiEvent

    /**
     * Melewati berkas yang gagal di Modal 01 dan menyelesaikan sisa batch.
     */
    data class OnSkipFailedFile(val fileId: String) : QueueUiEvent

    /**
     * Navigasi antar sub-layar Core (Queue, Extract, Translate, Merge) dan Modal (Settings, Storage, Glossary, Inspection).
     */
    data class OnNavigateRoute(val route: BatchWorkspaceRoute) : QueueUiEvent

    /**
     * Kembali dari sub-layar/modal ke Queue Hub.
     */
    data object OnBackFromSubRoute : QueueUiEvent

    /**
     * Membuka folder penyimpanan hasil SAF pada State.Completed.
     */
    data object OnOpenSafFolder : QueueUiEvent

    /**
     * Mereset antrean kembali ke State.Empty.
     */
    data object OnResetQueue : QueueUiEvent
}
