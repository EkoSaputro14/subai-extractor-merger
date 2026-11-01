package com.example.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.AiProviderSettings
import com.example.model.BatchJobItem
import com.example.model.BatchJobStatus
import com.example.model.BatchQueueState
import com.example.model.FileConflictPolicy
import com.example.model.TargetLanguageOption
import com.example.viewmodel.SubtitleUiState
import com.example.viewmodel.SubtitleViewModel
import java.io.File

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BatchQueueTabScreen(
    uiState: SubtitleUiState,
    aiSettings: AiProviderSettings,
    batchState: BatchQueueState,
    viewModel: SubtitleViewModel,
    onSelectMultipleFiles: () -> Unit,
    onChooseOutputFolder: () -> Unit,
    onNavigateToExtractSettings: () -> Unit
) {
    val expandedErrorJobIds = remember { mutableStateMapOf<String, Boolean>() }
    var showOutputFolderInspectorDialog by remember { mutableStateOf(false) }
    var showConfigDetails by remember { mutableStateOf(false) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        viewModel.setBatchNotificationsEnabled(granted)
    }

    val sortedJobs = batchState.sortedJobs

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("batch_queue_screen"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Header & File Selection Controls
        item {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                )
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Batch Subtitle Translation Queue",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Terjemahkan banyak file (.srt, .vtt, .ass, .ssa) secara berurutan dengan validasi struktur & retry otomatis di akhir antrean.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onSelectMultipleFiles,
                            enabled = !batchState.isRunning,
                            modifier = Modifier.testTag("batch_select_files_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Pilih Banyak File Subtitle",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Pilih Banyak File Subtitle")
                        }

                        OutlinedButton(
                            onClick = { viewModel.loadSampleEpisodeFilesToBatchQueue() },
                            enabled = !batchState.isRunning,
                            modifier = Modifier.testTag("batch_load_samples_button")
                        ) {
                            Text("Muat 4 File Sampel Episode")
                        }
                    }

                    if (batchState.preFlightWarnings.isNotEmpty()) {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("batch_preflight_warnings")
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "Peringatan Praperiksa File:",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                batchState.preFlightWarnings.forEach { warn ->
                                    Text(
                                        text = "• $warn",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 2. Dedicated Batch Output Folder Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("batch_output_folder_card"),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Folder,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Output Folder Khusus Batch",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Surface(
                            color = if (batchState.isOutputFolderWritable) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.errorContainer
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("batch_folder_writable_badge")
                        ) {
                            Text(
                                text = if (batchState.isOutputFolderWritable) "Dapat Ditulisi" else "Akses Ditolak",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (batchState.isOutputFolderWritable) {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onErrorContainer
                                },
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Text(
                        text = batchState.outputFolderDisplayName,
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.testTag("batch_output_folder_path_text")
                    )

                    if (batchState.outputFolderValidationMessage != null) {
                        Text(
                            text = batchState.outputFolderValidationMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onChooseOutputFolder,
                            enabled = !batchState.isRunning,
                            modifier = Modifier.testTag("batch_choose_output_folder_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.FolderOpen,
                                contentDescription = "Choose Output Folder",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Choose Output Folder")
                        }

                        OutlinedButton(
                            onClick = { showOutputFolderInspectorDialog = true },
                            modifier = Modifier.testTag("batch_open_output_folder_button")
                        ) {
                            Text("Open Output Folder")
                        }

                        TextButton(
                            onClick = { viewModel.resetBatchOutputFolderToDefault() },
                            enabled = !batchState.isRunning
                        ) {
                            Text("Reset ke Default")
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))

                    Text(
                        text = "Kebijakan Konflik Nama File:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FileConflictPolicy.entries.forEach { policy ->
                            FilterChip(
                                selected = batchState.conflictPolicy == policy,
                                onClick = {
                                    if (!batchState.isRunning) {
                                        viewModel.setBatchConflictPolicy(policy)
                                    }
                                },
                                label = { Text(policy.label, style = MaterialTheme.typography.labelSmall) },
                                modifier = Modifier.testTag("batch_conflict_policy_${policy.name}")
                            )
                        }
                    }
                    Text(
                        text = batchState.conflictPolicy.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // 3. Translation Settings, Retry Limit & Phone Notification Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                )
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val targetLangDisplay = if (uiState.targetLanguageOption == TargetLanguageOption.CUSTOM) {
                        uiState.customTargetLanguage
                    } else {
                        uiState.targetLanguageOption.label
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Pengaturan AI, Retry & Notifikasi",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Target: $targetLangDisplay • Model: ${aiSettings.selectedModel} • Max Retry: ${batchState.maxRetries}x",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(
                            onClick = { showConfigDetails = !showConfigDetails },
                            modifier = Modifier.testTag("batch_toggle_config_button")
                        ) {
                            Icon(
                                imageVector = if (showConfigDetails) Icons.Default.ExpandLess else Icons.Default.Tune,
                                contentDescription = "Pengaturan Batch"
                            )
                        }
                    }

                    AnimatedVisibility(visible = showConfigDetails) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            HorizontalDivider()
                            Text(
                                text = "Batas Maksimum Retry per File (Jika Gagal Validasi/Error):",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf(2, 3, 5, 8).forEach { retryOpt ->
                                    FilterChip(
                                        selected = batchState.maxRetries == retryOpt,
                                        onClick = { viewModel.setBatchMaxRetries(retryOpt) },
                                        label = { Text("$retryOpt Kali") },
                                        modifier = Modifier.testTag("batch_max_retries_$retryOpt")
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.NotificationsActive,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Column {
                                        Text(
                                            text = "Notifikasi ke Ponsel Saat Batch Selesai",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            text = "Kirim notifikasi sistem ketika seluruh antrean selesai atau membutuhkan perhatian.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Switch(
                                    checked = batchState.notificationsEnabled,
                                    onCheckedChange = { enabled ->
                                        if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                        } else {
                                            viewModel.setBatchNotificationsEnabled(enabled)
                                        }
                                    },
                                    modifier = Modifier.testTag("batch_notification_switch")
                                )
                            }

                            OutlinedButton(
                                onClick = onNavigateToExtractSettings,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Ubah Bahasa Target / Model / Cookie AI di Tab Ekstrak")
                            }
                        }
                    }
                }
            }
        }

        // 4. Real-Time Batch Summary Dashboard & Progress
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("batch_summary_card"),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                )
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Ringkasan & Progres Antrean",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${batchState.overallProgressPercent}%",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.testTag("batch_overall_progress_text")
                        )
                    }

                    LinearProgressIndicator(
                        progress = { batchState.overallProgressPercent / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .testTag("batch_overall_progress_bar")
                    )

                    // Metrics Grid
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SummaryMetricChip("Total Files", "${batchState.totalFiles}", "metric_total")
                        SummaryMetricChip("Completed", "${batchState.completedCount}", "metric_completed")
                        SummaryMetricChip("Processing", "${batchState.processingCount}", "metric_processing")
                        SummaryMetricChip("Queued", "${batchState.queuedCount}", "metric_queued")
                        SummaryMetricChip("Retry Queued", "${batchState.retryQueuedCount}", "metric_retry_queued")
                        SummaryMetricChip("Needs Attention", "${batchState.needsAttentionCount}", "metric_needs_attention")
                    }

                    val activeJob = batchState.currentJob
                    if (activeJob != null) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = "Current File: ${activeJob.displayTitleWithRetryTag}",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = "Current Attempt: #${batchState.currentAttemptDisplay} dari ${activeJob.maxRetries} • Status: ${activeJob.status.displayLabel} (${activeJob.translatedCuesProgress}/${activeJob.totalCues} entri)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                val etaMs = batchState.estimatedRemainingMs
                                if (etaMs != null && etaMs > 0L) {
                                    Text(
                                        text = "Estimasi Sisa Waktu: ~${(etaMs / 1000L).coerceAtLeast(1L)} detik",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }
                    }

                    if (batchState.statusBannerMessage != null) {
                        Text(
                            text = batchState.statusBannerMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.testTag("batch_status_banner_text")
                        )
                    }

                    if (batchState.lastNotificationTitle != null && batchState.lastNotificationMessage != null) {
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("batch_last_notification_card")
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.NotificationsActive,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = batchState.lastNotificationTitle,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                    Text(
                                        text = batchState.lastNotificationMessage,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                            }
                        }
                    }

                    // 5. Queue Control Buttons
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (!batchState.isRunning && !batchState.isPaused) {
                            Button(
                                onClick = { viewModel.startBatchTranslationQueue() },
                                enabled = batchState.queuedCount + batchState.retryQueuedCount > 0,
                                modifier = Modifier.testTag("batch_start_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Start Batch",
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Start Batch")
                            }
                        }

                        if (batchState.isRunning) {
                            OutlinedButton(
                                onClick = { viewModel.pauseBatchTranslationQueue() },
                                modifier = Modifier.testTag("batch_pause_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Pause,
                                    contentDescription = "Pause Queue",
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Pause Queue")
                            }
                        }

                        if (batchState.isPaused && !batchState.isRunning) {
                            Button(
                                onClick = { viewModel.resumeBatchTranslationQueue() },
                                modifier = Modifier.testTag("batch_resume_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Resume Queue",
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Resume Queue")
                            }
                        }

                        if (batchState.isRunning || batchState.isPaused) {
                            OutlinedButton(
                                onClick = { viewModel.cancelBatchTranslationQueue() },
                                modifier = Modifier.testTag("batch_cancel_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Cancel,
                                    contentDescription = "Cancel Batch",
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Cancel Batch")
                            }
                        }

                        if (!batchState.isRunning && (batchState.needsAttentionCount > 0 || batchState.failedCount > 0)) {
                            OutlinedButton(
                                onClick = { viewModel.retryFailedBatchJobs() },
                                modifier = Modifier.testTag("batch_retry_failed_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Replay,
                                    contentDescription = "Retry Failed Jobs",
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Retry Failed Jobs")
                            }
                        }

                        if (!batchState.isRunning && batchState.completedCount > 0) {
                            TextButton(
                                onClick = { viewModel.clearCompletedBatchJobs() },
                                modifier = Modifier.testTag("batch_clear_completed_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CleaningServices,
                                    contentDescription = "Clear Completed Jobs",
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Clear Completed Jobs")
                            }
                        }
                    }
                }
            }
        }

        // 6. Queue Items Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Daftar Urutan Antrean (${sortedJobs.size} File)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Diproses berurutan 1 per 1",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (sortedJobs.isEmpty()) {
            item {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerLowest,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("batch_empty_queue_placeholder")
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Antrean Batch Masih Kosong",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Pilih beberapa file subtitle (.srt, .vtt, .ass, .ssa) atau klik 'Muat 4 File Sampel Episode' untuk melihat cara kerja antrean berurutan.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            itemsIndexed(
                items = sortedJobs,
                key = { _, job -> job.jobId }
            ) { index, job ->
                val isErrorExpanded = expandedErrorJobIds[job.jobId] == true
                BatchQueueJobItemCard(
                    index = index,
                    totalJobs = sortedJobs.size,
                    job = job,
                    isQueueRunning = batchState.isRunning,
                    isErrorExpanded = isErrorExpanded,
                    onToggleErrorDetails = {
                        expandedErrorJobIds[job.jobId] = !isErrorExpanded
                    },
                    onMoveUp = { viewModel.moveBatchJobUp(job.jobId) },
                    onMoveDown = { viewModel.moveBatchJobDown(job.jobId) },
                    onRemove = { viewModel.removeBatchJob(job.jobId) }
                )
            }
        }
    }

    if (showOutputFolderInspectorDialog) {
        OutputFolderContentsDialog(
            batchState = batchState,
            onDismiss = { showOutputFolderInspectorDialog = false }
        )
    }
}

@Composable
private fun SummaryMetricChip(
    label: String,
    value: String,
    testTagId: String
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.testTag(testTagId)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun BatchQueueJobItemCard(
    index: Int,
    totalJobs: Int,
    job: BatchJobItem,
    isQueueRunning: Boolean,
    isErrorExpanded: Boolean,
    onToggleErrorDetails: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onRemove: () -> Unit
) {
    val statusColor = when (job.status) {
        BatchJobStatus.COMPLETED -> Color(0xFF1B8755)
        BatchJobStatus.PROCESSING, BatchJobStatus.VALIDATING -> MaterialTheme.colorScheme.primary
        BatchJobStatus.RETRY_QUEUED -> Color(0xFFD97706)
        BatchJobStatus.NEEDS_ATTENTION, BatchJobStatus.FAILED -> MaterialTheme.colorScheme.error
        BatchJobStatus.CANCELLED -> MaterialTheme.colorScheme.outline
        BatchJobStatus.QUEUED -> MaterialTheme.colorScheme.secondary
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("batch_job_card_${index + 1}"),
        border = BorderStroke(
            width = if (job.status == BatchJobStatus.PROCESSING || job.status == BatchJobStatus.VALIDATING) 2.dp else 1.dp,
            color = if (job.status == BatchJobStatus.PROCESSING || job.status == BatchJobStatus.VALIDATING) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.outlineVariant
            }
        ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "#${job.queueOrder}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${job.queueOrder}. \"${job.displayTitleWithRetryTag}\" — ${job.status.indonesianLabel}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.testTag("batch_job_title_${job.fileName}")
                        )
                        Text(
                            text = "Format: ${job.detectedFormat.name} • ${job.totalCues} entri • Percobaan: ${job.attemptCount}/${job.maxRetries}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Status Badge
                Surface(
                    color = statusColor.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, statusColor.copy(alpha = 0.5f)),
                    modifier = Modifier.testTag("batch_job_status_${job.fileName}")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val icon = when (job.status) {
                            BatchJobStatus.COMPLETED -> Icons.Default.CheckCircle
                            BatchJobStatus.NEEDS_ATTENTION, BatchJobStatus.FAILED -> Icons.Default.ErrorOutline
                            BatchJobStatus.RETRY_QUEUED -> Icons.Default.Refresh
                            else -> Icons.Default.WarningAmber
                        }
                        if (job.status == BatchJobStatus.COMPLETED ||
                            job.status == BatchJobStatus.NEEDS_ATTENTION ||
                            job.status == BatchJobStatus.RETRY_QUEUED
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = statusColor,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        Text(
                            text = job.status.displayLabel,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = statusColor
                        )
                    }
                }
            }

            if (job.outputFileName != null && job.status == BatchJobStatus.COMPLETED) {
                Text(
                    text = "Disimpan sebagai: ${job.outputFileName} (${job.outputFilePath ?: ""})",
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    color = Color(0xFF1B8755)
                )
            }

            if (job.lastFailureReason != null) {
                Text(
                    text = job.lastFailureReason,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            // Reorder, Remove & View Error Details row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    IconButton(
                        onClick = onMoveUp,
                        enabled = !isQueueRunning && index > 0,
                        modifier = Modifier.testTag("batch_move_up_${job.fileName}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowUpward,
                            contentDescription = "Pindah ke Atas"
                        )
                    }
                    IconButton(
                        onClick = onMoveDown,
                        enabled = !isQueueRunning && index < totalJobs - 1,
                        modifier = Modifier.testTag("batch_move_down_${job.fileName}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowDownward,
                            contentDescription = "Pindah ke Bawah"
                        )
                    }
                    IconButton(
                        onClick = onRemove,
                        enabled = !isQueueRunning &&
                            job.status != BatchJobStatus.PROCESSING &&
                            job.status != BatchJobStatus.VALIDATING,
                        modifier = Modifier.testTag("batch_remove_${job.fileName}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Hapus dari Antrean"
                        )
                    }
                }

                if (job.attemptHistory.isNotEmpty() || job.lastValidationResult != null) {
                    TextButton(
                        onClick = onToggleErrorDetails,
                        modifier = Modifier.testTag("batch_view_details_${job.fileName}")
                    ) {
                        Text(if (isErrorExpanded) "Hide Details" else "View Error Details")
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = if (isErrorExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            AnimatedVisibility(visible = isErrorExpanded) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Riwayat Percobaan & Laporan Validasi Struktur:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                        job.lastValidationResult?.let { valRes ->
                            Text(
                                text = "• Ringkasan Validasi: ${valRes.summaryMessage}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        job.attemptHistory.forEach { rec ->
                            Text(
                                text = "• Attempt #${rec.attemptNumber}: ${if (rec.passed) "PASSED" else "FAILED (${rec.failureType?.label ?: "Error"})"} — ${rec.reason}",
                                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OutputFolderContentsDialog(
    batchState: BatchQueueState,
    onDismiss: () -> Unit
) {
    val completedJobs = batchState.sortedJobs.filter { it.status == BatchJobStatus.COMPLETED }
    val localFolderFiles = remember(batchState.outputFolderUriOrPath, batchState.completedCount) {
        val path = batchState.outputFolderUriOrPath
        if (path.isNotBlank() && !path.startsWith("content://")) {
            val dir = File(path)
            if (dir.exists() && dir.isDirectory) {
                dir.listFiles()?.filter { it.isFile && !it.name.endsWith(".tmp") }?.map { it.name } ?: emptyList()
            } else emptyList()
        } else {
            completedJobs.mapNotNull { it.outputFileName }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Isi Folder Output Batch") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Lokasi Folder:\n${batchState.outputFolderDisplayName}",
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace)
                )
                HorizontalDivider()
                if (localFolderFiles.isEmpty() && completedJobs.isEmpty()) {
                    Text(
                        text = "Belum ada file subtitle hasil terjemahan yang tersimpan di folder ini.",
                        style = MaterialTheme.typography.bodySmall
                    )
                } else {
                    Text(
                        text = "File yang telah lolos validasi & tersimpan (${maxOf(localFolderFiles.size, completedJobs.size)}):",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    val names = (localFolderFiles + completedJobs.mapNotNull { it.outputFileName }).distinct()
                    names.forEach { name ->
                        Text(
                            text = "├── $name",
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Tutup")
            }
        }
    )
}
