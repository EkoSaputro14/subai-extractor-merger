package com.example.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AiProviderSettings
import com.example.model.BatchJobItem
import com.example.model.BatchJobStatus
import com.example.model.BatchQueueState
import com.example.model.FileConflictPolicy
import com.example.model.TargetLanguageOption
import com.example.ui.theme.ForestGreenPrimary
import com.example.ui.theme.RetryAmberOrange
import com.example.ui.theme.SageHighlightFill
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
    var showConfigDetails by rememberSaveable { mutableStateOf(false) }
    var showFolderConfig by rememberSaveable { mutableStateOf(false) }
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        viewModel.setBatchNotificationsEnabled(granted)
    }

    val sortedJobs = batchState.sortedJobs

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("batch_queue_screen")
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 12.dp)
        ) {
            // 1. Top Circular Progress & 4-Counter Dashboard Card matching Panel 3
            item(key = "batch_circular_dashboard_card") {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("batch_summary_card"),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    border = BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Batch Translation",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )

                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                IconButton(
                                    onClick = { showFolderConfig = !showFolderConfig },
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Folder,
                                        contentDescription = "Output Folder Batch",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                                IconButton(
                                    onClick = { showConfigDetails = !showConfigDetails },
                                    modifier = Modifier
                                        .size(34.dp)
                                        .testTag("batch_toggle_config_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.MoreVert,
                                        contentDescription = "Pengaturan Batch"
                                    )
                                }
                            }
                        }

                        // Circular Progress Ring (3/4 Selesai) + 4 Metric Columns
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(18.dp)
                        ) {
                            // Left: Circular Progress Ring
                            Box(
                                modifier = Modifier.size(92.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                val progressFraction = if (batchState.totalFiles > 0) {
                                    (batchState.completedCount.toFloat() / batchState.totalFiles.toFloat()).coerceIn(0f, 1f)
                                } else {
                                    0f
                                }
                                val ringColor = if (isDark) MaterialTheme.colorScheme.primary else ForestGreenPrimary
                                val trackColor = MaterialTheme.colorScheme.surfaceContainerHigh

                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    val strokeWidth = 8.dp.toPx()
                                    drawArc(
                                        color = trackColor,
                                        startAngle = 0f,
                                        sweepAngle = 360f,
                                        useCenter = false,
                                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                                    )
                                    drawArc(
                                        color = ringColor,
                                        startAngle = -90f,
                                        sweepAngle = 360f * progressFraction,
                                        useCenter = false,
                                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                                    )
                                }

                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "${batchState.completedCount}/${batchState.totalFiles}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.testTag("batch_overall_progress_text")
                                    )
                                    Text(
                                        text = "Selesai",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            // Right: 4 Metric Counters (Total, Selesai, Proses, Ulang)
                            Row(
                                modifier = Modifier.weight(1f),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                BatchStatColumn(
                                    value = "${batchState.totalFiles}",
                                    label = "Total",
                                    badgeBg = MaterialTheme.colorScheme.surfaceContainerLow,
                                    valueColor = MaterialTheme.colorScheme.onSurface,
                                    testTagId = "metric_total"
                                )
                                BatchStatColumn(
                                    value = "${batchState.completedCount}",
                                    label = "Selesai",
                                    badgeBg = if (isDark) Color(0xFF1F422F) else SageHighlightFill,
                                    valueColor = if (isDark) Color(0xFF8CE0AF) else ForestGreenPrimary,
                                    testTagId = "metric_completed"
                                )
                                BatchStatColumn(
                                    value = "${batchState.processingCount + batchState.queuedCount}",
                                    label = "Proses",
                                    badgeBg = MaterialTheme.colorScheme.surfaceContainerLow,
                                    valueColor = Color(0xFF4F5BD5),
                                    testTagId = "metric_processing"
                                )
                                BatchStatColumn(
                                    value = "${batchState.retryQueuedCount + batchState.needsAttentionCount}",
                                    label = "Ulang",
                                    badgeBg = MaterialTheme.colorScheme.surfaceContainerLow,
                                    valueColor = RetryAmberOrange,
                                    testTagId = "metric_retry_queued"
                                )
                            }
                        }

                        // Quick File Picker & Sample Loader Row
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = onSelectMultipleFiles,
                                enabled = !batchState.isRunning,
                                shape = RoundedCornerShape(50),
                                modifier = Modifier.testTag("batch_select_files_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Pilih Banyak File Subtitle",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Pilih Banyak File", style = MaterialTheme.typography.labelMedium)
                            }

                            OutlinedButton(
                                onClick = { viewModel.loadSampleEpisodeFilesToBatchQueue() },
                                enabled = !batchState.isRunning,
                                shape = RoundedCornerShape(50),
                                modifier = Modifier.testTag("batch_load_samples_button")
                            ) {
                                Text("Muat 4 Sampel Episode", style = MaterialTheme.typography.labelMedium)
                            }

                            OutlinedButton(
                                onClick = { showFolderConfig = !showFolderConfig },
                                shape = RoundedCornerShape(50)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FolderOpen,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Output Folder", style = MaterialTheme.typography.labelMedium)
                            }
                        }

                        if (batchState.preFlightWarnings.isNotEmpty()) {
                            Surface(
                                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f),
                                shape = RoundedCornerShape(10.dp),
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
                                shape = RoundedCornerShape(10.dp),
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
                    }
                }
            }

            // 2. Dedicated Batch Output Folder Card (Always accessible & expandable)
            item(key = "batch_output_folder_card_item") {
                AnimatedVisibility(visible = showFolderConfig || !batchState.isOutputFolderWritable) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("batch_output_folder_card"),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
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
                        }
                    }
                }
            }

            // 3. Expandable AI, Retry & Notification Config Card
            item(key = "batch_config_card_item") {
                AnimatedVisibility(visible = showConfigDetails) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            val targetLangDisplay = if (uiState.targetLanguageOption == TargetLanguageOption.CUSTOM) {
                                uiState.customTargetLanguage
                            } else {
                                uiState.targetLanguageOption.label
                            }

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
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Notifikasi ke Ponsel Saat Batch Selesai",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = "Kirim notifikasi sistem ketika seluruh antrean selesai.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
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
                                Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Ubah Bahasa Target / Model / Cookie AI")
                            }
                        }
                    }
                }
            }

            // 4. Sequential File Queue Cards matching Panel 3
            if (sortedJobs.isEmpty()) {
                item(key = "batch_empty_state") {
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(18.dp),
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
                                text = "Klik 'Pilih Banyak File' atau 'Muat 4 Sampel Episode' untuk memulai antrean terjemahan berurutan.",
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

        // 5. Sticky Bottom Primary Action Bar matching Panel 3 ([ ⏸ Jeda Batch ] [ ⏹ ])
        BatchBottomActionRow(
            batchState = batchState,
            isDark = isDark,
            viewModel = viewModel
        )
    }

    if (showOutputFolderInspectorDialog) {
        OutputFolderContentsDialog(
            batchState = batchState,
            onDismiss = { showOutputFolderInspectorDialog = false }
        )
    }
}

@Composable
private fun BatchStatColumn(
    value: String,
    label: String,
    badgeBg: Color,
    valueColor: Color,
    testTagId: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.testTag(testTagId)
    ) {
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = badgeBg
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = valueColor,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun BatchBottomActionRow(
    batchState: BatchQueueState,
    isDark: Boolean,
    viewModel: SubtitleViewModel
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // Optional secondary row when failed or completed items exist
        if (!batchState.isRunning && (batchState.needsAttentionCount > 0 || batchState.failedCount > 0 || batchState.completedCount > 0)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (batchState.needsAttentionCount > 0 || batchState.failedCount > 0) {
                    OutlinedButton(
                        onClick = { viewModel.retryFailedBatchJobs() },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("batch_retry_failed_button")
                    ) {
                        Icon(Icons.Default.Replay, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Retry Gagal")
                    }
                }
                if (batchState.completedCount > 0) {
                    TextButton(
                        onClick = { viewModel.clearCompletedBatchJobs() },
                        modifier = Modifier.testTag("batch_clear_completed_button")
                    ) {
                        Icon(Icons.Default.CleaningServices, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Bersihkan Selesai")
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val primaryContainerColor = if (isDark) MaterialTheme.colorScheme.primary else ForestGreenPrimary
            val primaryContentColor = if (isDark) MaterialTheme.colorScheme.onPrimary else Color.White

            if (batchState.isRunning) {
                Button(
                    onClick = { viewModel.pauseBatchTranslationQueue() },
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = primaryContainerColor,
                        contentColor = primaryContentColor
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .testTag("batch_pause_button")
                ) {
                    Icon(Icons.Default.Pause, contentDescription = "Jeda Batch", modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Jeda Batch", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                }
            } else if (batchState.isPaused) {
                Button(
                    onClick = { viewModel.resumeBatchTranslationQueue() },
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = primaryContainerColor,
                        contentColor = primaryContentColor
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .testTag("batch_resume_button")
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "Lanjutkan Batch", modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Lanjutkan Batch", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                }
            } else {
                Button(
                    onClick = { viewModel.startBatchTranslationQueue() },
                    enabled = batchState.queuedCount + batchState.retryQueuedCount > 0,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = primaryContainerColor,
                        contentColor = primaryContentColor
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .testTag("batch_start_button")
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "Mulai Batch", modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Mulai Batch", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                }
            }

            // Square Stop/Cancel Button on the right matching Panel 3
            OutlinedIconButton(
                onClick = { viewModel.cancelBatchTranslationQueue() },
                enabled = batchState.isRunning || batchState.isPaused,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier
                    .size(52.dp)
                    .testTag("batch_cancel_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Stop,
                    contentDescription = "Hentikan Batch"
                )
            }
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
    val isActiveProcessing = job.status == BatchJobStatus.PROCESSING || job.status == BatchJobStatus.VALIDATING

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("batch_job_card_${index + 1}"),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(
            width = if (isActiveProcessing) 1.5.dp else 1.dp,
            color = if (isActiveProcessing) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f)
            }
        ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: Document Icon + Bold Filename
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Description,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = job.displayTitleWithRetryTag,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.testTag("batch_job_title_${job.fileName}")
                        )
                        Text(
                            text = "#${job.queueOrder} • ${job.detectedFormat.name} • ${job.totalCues} dialog",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Right: Status Indicator matching Panel 3 of Reference Design
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.testTag("batch_job_status_${job.fileName}")
                ) {
                    when (job.status) {
                        BatchJobStatus.COMPLETED -> {
                            Text(
                                text = "Selesai",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF2D6A4F)
                            )
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Selesai",
                                tint = Color(0xFF2D6A4F),
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        BatchJobStatus.PROCESSING, BatchJobStatus.VALIDATING -> {
                            Text(
                                text = "Memproses... ${job.translatedCuesProgress}/${job.totalCues}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = Color(0xFF4F5BD5)
                            )
                        }

                        BatchJobStatus.RETRY_QUEUED -> {
                            Text(
                                text = "Retry (${job.attemptCount}/${job.maxRetries})",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = RetryAmberOrange
                            )
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Retry Queued",
                                tint = RetryAmberOrange,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        BatchJobStatus.NEEDS_ATTENTION, BatchJobStatus.FAILED -> {
                            Text(
                                text = "Perlu Perhatian",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error
                            )
                            Icon(
                                imageVector = Icons.Default.ErrorOutline,
                                contentDescription = "Error",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        BatchJobStatus.CANCELLED -> {
                            Text(
                                text = "Dibatalkan",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }

                        BatchJobStatus.QUEUED -> {
                            Text(
                                text = "Menunggu",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Inline Linear Progress Bar when processing (matching episode_03.ass in Panel 3!)
            if (isActiveProcessing) {
                val cueFrac = if (job.totalCues > 0) {
                    (job.translatedCuesProgress.toFloat() / job.totalCues.toFloat()).coerceIn(0.05f, 1f)
                } else {
                    0.5f
                }
                LinearProgressIndicator(
                    progress = { cueFrac },
                    color = ForestGreenPrimary,
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .clip(RoundedCornerShape(3.dp))
                )
            }

            if (job.lastFailureReason != null) {
                Text(
                    text = job.lastFailureReason,
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 11.5.sp,
                    color = MaterialTheme.colorScheme.error
                )
            }

            // Compact Queue Order Controls & View Error Details when idle or when errors exist
            if (!isQueueRunning || job.attemptHistory.isNotEmpty() || job.lastValidationResult != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        IconButton(
                            onClick = onMoveUp,
                            enabled = !isQueueRunning && index > 0,
                            modifier = Modifier
                                .size(32.dp)
                                .testTag("batch_move_up_${job.fileName}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowUpward,
                                contentDescription = "Pindah ke Atas",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        IconButton(
                            onClick = onMoveDown,
                            enabled = !isQueueRunning && index < totalJobs - 1,
                            modifier = Modifier
                                .size(32.dp)
                                .testTag("batch_move_down_${job.fileName}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowDownward,
                                contentDescription = "Pindah ke Bawah",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        IconButton(
                            onClick = onRemove,
                            enabled = !isQueueRunning && !isActiveProcessing,
                            modifier = Modifier
                                .size(32.dp)
                                .testTag("batch_remove_${job.fileName}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "Hapus dari Antrean",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    if (job.attemptHistory.isNotEmpty() || job.lastValidationResult != null) {
                        TextButton(
                            onClick = onToggleErrorDetails,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.testTag("batch_view_details_${job.fileName}")
                        ) {
                            Text(
                                text = if (isErrorExpanded) "Tutup Detail" else "Detail Validasi",
                                style = MaterialTheme.typography.labelSmall
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = if (isErrorExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }

            AnimatedVisibility(visible = isErrorExpanded) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        job.lastValidationResult?.let { valRes ->
                            Text(
                                text = "• ${valRes.summaryMessage}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        job.attemptHistory.forEach { rec ->
                            Text(
                                text = "• Percobaan #${rec.attemptNumber}: ${if (rec.passed) "LOLOS" else "GAGAL (${rec.failureType?.label ?: "Error"})"} — ${rec.reason}",
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
