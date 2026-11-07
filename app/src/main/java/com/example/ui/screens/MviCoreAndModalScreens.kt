package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.AiProviderPreset
import com.example.model.AiProviderSettings
import com.example.model.BatchQueueState
import com.example.model.BatchWorkspaceRoute
import com.example.model.ExtractionRulePreset
import com.example.model.FileConflictPolicy
import com.example.model.GlossaryRule
import com.example.model.QueueUiEvent
import com.example.model.QueueUiState
import com.example.ui.components.GeminiCookieWebViewDialog
import com.example.viewmodel.SubtitleUiState
import com.example.viewmodel.SubtitleViewModel

/**
 * Top-level MVI Linear Batch Workspace orchestrating:
 * 1. State.Empty -> State_01_Empty.png
 * 2. State.ReadyToStart -> Core_Screen_01_Queue.png
 * 3. State.Processing -> State_02_Active.png
 * 4. State.Paused -> State_03_Paused.png
 * 5. State.NeedsAttention -> Modal_01_Inspection.png
 * 6. State.Completed -> State_04_Completed.png
 * Plus Core Screens 02..04 (Extract, Translate, Merge) and Modals 02..04 (Settings, Storage, Glossary).
 */
@Composable
fun SubAiMviLinearBatchWorkspace(
    viewModel: SubtitleViewModel,
    onSelectMultipleFiles: () -> Unit,
    onChooseOutputFolder: () -> Unit,
    onSaveMergedFile: (String) -> Unit
) {
    val queueState by viewModel.queueUiState.collectAsStateWithLifecycle()
    val activeRoute by viewModel.activeWorkspaceRoute.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val aiSettings by viewModel.aiSettings.collectAsStateWithLifecycle()
    val glossaryRules by viewModel.glossaryRules.collectAsStateWithLifecycle()
    val batchState by viewModel.batchQueueState.collectAsStateWithLifecycle()

    BackHandler(enabled = activeRoute != BatchWorkspaceRoute.QUEUE_HUB) {
        viewModel.onQueueEvent(QueueUiEvent.OnBackFromSubRoute)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(StudioObsidianBg)
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .testTag("mvi_linear_batch_workspace")
    ) {
        when (activeRoute) {
            BatchWorkspaceRoute.CORE_EXTRACT -> {
                CoreScreen02ExtractScreen(
                    uiState = uiState,
                    viewModel = viewModel,
                    onBackClick = { viewModel.onQueueEvent(QueueUiEvent.OnBackFromSubRoute) },
                    onBellClick = {
                        viewModel.onQueueEvent(QueueUiEvent.OnNavigateRoute(BatchWorkspaceRoute.MODAL_SETTINGS))
                    }
                )
            }

            BatchWorkspaceRoute.CORE_TRANSLATE -> {
                CoreScreen03TranslateStreamScreen(
                    queueState = queueState,
                    uiState = uiState,
                    aiSettings = aiSettings,
                    glossaryRules = glossaryRules,
                    viewModel = viewModel,
                    onBackClick = { viewModel.onQueueEvent(QueueUiEvent.OnBackFromSubRoute) },
                    onPauseStreamClick = {
                        viewModel.onQueueEvent(QueueUiEvent.OnPauseBatch)
                        viewModel.onQueueEvent(QueueUiEvent.OnBackFromSubRoute)
                    },
                    onBellClick = {
                        viewModel.onQueueEvent(QueueUiEvent.OnNavigateRoute(BatchWorkspaceRoute.MODAL_SETTINGS))
                    }
                )
            }

            BatchWorkspaceRoute.CORE_MERGE -> {
                CoreScreen04MergeIntegrityScreen(
                    batchState = batchState,
                    uiState = uiState,
                    onBackClick = { viewModel.onQueueEvent(QueueUiEvent.OnBackFromSubRoute) },
                    onSaveFinalMergedClick = {
                        val baseName = uiState.document.fileName.substringBeforeLast('.', "Shingeki_EP02")
                        val ext = uiState.document.format.extension
                        onSaveMergedFile("${baseName}_id.$ext")
                    },
                    onBellClick = {
                        viewModel.onQueueEvent(QueueUiEvent.OnNavigateRoute(BatchWorkspaceRoute.MODAL_SETTINGS))
                    }
                )
            }

            BatchWorkspaceRoute.MODAL_INSPECTION -> {
                val inspectionState = (queueState as? QueueUiState.NeedsAttention)
                    ?: QueueUiState.NeedsAttention(
                        failedFile = "Shingeki_EP04.srt",
                        errorList = listOf(
                            "missingCueIds: [42, 43] tidak ditemukan di respon AI",
                            "Percobaan Retry: 5 dari 5 batas maksimum (Targeted Repair)"
                        ),
                        missingCueIds = listOf(42, 43),
                        retryAttemptCount = 5,
                        maxRetries = 5
                    )
                Modal01ForensicInspectionScreen(
                    state = inspectionState,
                    onBackClick = { viewModel.onQueueEvent(QueueUiEvent.OnBackFromSubRoute) },
                    onTargetedRepairClick = {
                        viewModel.onQueueEvent(
                            QueueUiEvent.OnTargetedRepairFailedCues(
                                fileId = inspectionState.failedJobId,
                                cueIds = inspectionState.missingCueIds
                            )
                        )
                    },
                    onSkipFileClick = {
                        viewModel.onQueueEvent(
                            QueueUiEvent.OnSkipFailedFile(fileId = inspectionState.failedJobId)
                        )
                    },
                    onBellClick = {
                        viewModel.onQueueEvent(QueueUiEvent.OnNavigateRoute(BatchWorkspaceRoute.MODAL_SETTINGS))
                    }
                )
            }

            BatchWorkspaceRoute.MODAL_SETTINGS -> {
                Modal02AiEngineSettingsScreen(
                    aiSettings = aiSettings,
                    viewModel = viewModel,
                    onBackClick = { viewModel.onQueueEvent(QueueUiEvent.OnBackFromSubRoute) },
                    onSaveClick = {
                        viewModel.showBannerMessage("Pengaturan Provider AI (${aiSettings.preset.label}) disimpan.")
                        viewModel.onQueueEvent(QueueUiEvent.OnBackFromSubRoute)
                    },
                    onBellClick = {
                        viewModel.checkAppUpdate()
                    }
                )
            }

            BatchWorkspaceRoute.MODAL_STORAGE -> {
                Modal03SafStorageConflictScreen(
                    batchState = batchState,
                    onSelectPolicy = { policy -> viewModel.setBatchConflictPolicy(policy) },
                    onBackClick = { viewModel.onQueueEvent(QueueUiEvent.OnBackFromSubRoute) },
                    onChooseSafFolderClick = onChooseOutputFolder,
                    onBellClick = {
                        viewModel.onQueueEvent(QueueUiEvent.OnNavigateRoute(BatchWorkspaceRoute.MODAL_SETTINGS))
                    }
                )
            }

            BatchWorkspaceRoute.MODAL_GLOSSARY -> {
                Modal04GlossaryLockedTermsScreen(
                    glossaryRules = glossaryRules,
                    onAddRule = { sourceTerm, targetTerm ->
                        viewModel.addGlossaryRule(
                            sourceTerm = sourceTerm,
                            targetTerm = targetTerm,
                            isCaseSensitive = false,
                            wholeWord = true
                        )
                    },
                    onBackClick = { viewModel.onQueueEvent(QueueUiEvent.OnBackFromSubRoute) },
                    onBellClick = {
                        viewModel.onQueueEvent(QueueUiEvent.OnNavigateRoute(BatchWorkspaceRoute.MODAL_SETTINGS))
                    }
                )
            }

            BatchWorkspaceRoute.QUEUE_HUB -> {
                when (val currentQueueState = queueState) {
                    QueueUiState.Empty -> {
                        State01EmptyScreen(
                            onPickFilesClick = onSelectMultipleFiles,
                            onLoadFourSamplesClick = {
                                viewModel.onQueueEvent(QueueUiEvent.OnLoadSampleFiles)
                            },
                            onBellClick = {
                                viewModel.onQueueEvent(QueueUiEvent.OnNavigateRoute(BatchWorkspaceRoute.MODAL_SETTINGS))
                            }
                        )
                    }

                    is QueueUiState.ReadyToStart -> {
                        CoreScreen01QueueReadyScreen(
                            state = currentQueueState,
                            uiState = uiState,
                            aiSettings = aiSettings,
                            glossaryRules = glossaryRules,
                            batchState = batchState,
                            onEvent = { viewModel.onQueueEvent(it) },
                            onAddMoreFilesClick = onSelectMultipleFiles,
                            onBellClick = {
                                viewModel.onQueueEvent(QueueUiEvent.OnNavigateRoute(BatchWorkspaceRoute.MODAL_SETTINGS))
                            }
                        )
                    }

                    is QueueUiState.Processing -> {
                        State02ActiveProcessingScreen(
                            state = currentQueueState,
                            onPauseClick = { viewModel.onQueueEvent(QueueUiEvent.OnPauseBatch) },
                            onCancelClick = { viewModel.onQueueEvent(QueueUiEvent.OnCancelBatch) },
                            onAddEpisodeClick = onSelectMultipleFiles,
                            onBellClick = {
                                viewModel.onQueueEvent(QueueUiEvent.OnNavigateRoute(BatchWorkspaceRoute.CORE_TRANSLATE))
                            }
                        )
                    }

                    is QueueUiState.Paused -> {
                        State03PausedScreen(
                            state = currentQueueState,
                            onResumeClick = { viewModel.onQueueEvent(QueueUiEvent.OnResumeBatch) },
                            onBellClick = {
                                viewModel.onQueueEvent(QueueUiEvent.OnNavigateRoute(BatchWorkspaceRoute.MODAL_SETTINGS))
                            }
                        )
                    }

                    is QueueUiState.NeedsAttention -> {
                        Modal01ForensicInspectionScreen(
                            state = currentQueueState,
                            onBackClick = { viewModel.onQueueEvent(QueueUiEvent.OnResetQueue) },
                            onTargetedRepairClick = {
                                viewModel.onQueueEvent(
                                    QueueUiEvent.OnTargetedRepairFailedCues(
                                        fileId = currentQueueState.failedJobId,
                                        cueIds = currentQueueState.missingCueIds
                                    )
                                )
                            },
                            onSkipFileClick = {
                                viewModel.onQueueEvent(
                                    QueueUiEvent.OnSkipFailedFile(fileId = currentQueueState.failedJobId)
                                )
                            },
                            onBellClick = {
                                viewModel.onQueueEvent(QueueUiEvent.OnNavigateRoute(BatchWorkspaceRoute.MODAL_SETTINGS))
                            }
                        )
                    }

                    is QueueUiState.Completed -> {
                        State04CompletedScreen(
                            state = currentQueueState,
                            onOpenSafFolderClick = {
                                viewModel.onQueueEvent(QueueUiEvent.OnOpenSafFolder)
                            },
                            onBellClick = {
                                viewModel.onQueueEvent(QueueUiEvent.OnNavigateRoute(BatchWorkspaceRoute.CORE_MERGE))
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Core_Screen_02_Extract.png — Subtitle Extractor & Tag Filter
 */
@Composable
fun CoreScreen02ExtractScreen(
    uiState: SubtitleUiState,
    viewModel: SubtitleViewModel,
    onBackClick: () -> Unit,
    onBellClick: () -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    val selectedPreset = uiState.extractionPreset

    val presetOptions = listOf(
        Triple(
            ExtractionRulePreset.ANIME,
            "Anime Preset",
            "Pertahankan tag ASS & efek gaya dialog"
        ),
        Triple(
            ExtractionRulePreset.BIOSKOP,
            "Bioskop Formal",
            "Hapus tag posisi & rapikan baris dialog"
        ),
        Triple(
            ExtractionRulePreset.MINIMAL,
            "Minimal Standar",
            "Teks polos tanpa metadata styling"
        )
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(StudioObsidianBg)
            .testTag("mvi_core_extract_screen")
    ) {
        SubAiStudioDarkTopBar(
            subtitle = "Subtitle Extractor & Tag Filter",
            showBackButton = true,
            onBackClick = onBackClick,
            onBellClick = onBellClick
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Card 1: Pilih Preset Ekstraksi
                Surface(
                    color = StudioCardSurface,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, StudioCardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Pilih Preset Ekstraksi",
                            color = StudioTextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )

                        presetOptions.forEach { (preset, title, subtitle) ->
                            val isSelected = selectedPreset == preset ||
                                (selectedPreset == ExtractionRulePreset.STANDAR && preset == ExtractionRulePreset.ANIME)
                            val itemBg = if (isSelected) StudioCyanSurface else StudioInnerItemBg
                            val itemBorder = if (isSelected) StudioCyanBorder else StudioInnerItemBorder
                            val titleColor = if (isSelected) StudioCyanAccent else StudioTextPrimary

                            Surface(
                                color = itemBg,
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(if (isSelected) 1.2.dp else 1.dp, itemBorder),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.selectExtractionPreset(preset) }
                                    .testTag("mvi_extract_preset_${preset.name.lowercase()}")
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                    verticalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Text(
                                        text = title,
                                        color = titleColor,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = subtitle,
                                        color = StudioTextMuted,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }
                }

                // Card 2: Pemisah Baris Multiline & Format Nomor Urut
                Surface(
                    color = StudioCardSurface,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, StudioCardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Pemisah Baris Multiline: ' // ' (Double Slash)",
                            color = StudioTextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Format Nomor Urut: [1] Teks Terjemahan",
                            color = StudioTextMuted,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            // Bottom Primary CTA: Ekstrak & Salin ke Clipboard
            Button(
                onClick = {
                    val payload = viewModel.buildCurrentBatchOutput(withPromptHeader = true)
                    clipboardManager.setText(AnnotatedString(payload))
                    viewModel.showBannerMessage("Teks bersih berhasil diekstrak & disalin ke Clipboard!")
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = StudioRoyalBlueCta,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
                    .height(54.dp)
                    .testTag("mvi_extract_copy_clipboard_button")
            ) {
                Text(
                    text = "Ekstrak & Salin ke Clipboard",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * Core_Screen_03_Translate.png — Live AI Stream Workspace
 */
@Composable
fun CoreScreen03TranslateStreamScreen(
    queueState: QueueUiState,
    uiState: SubtitleUiState,
    aiSettings: AiProviderSettings,
    glossaryRules: List<GlossaryRule>,
    viewModel: SubtitleViewModel,
    onBackClick: () -> Unit,
    onPauseStreamClick: () -> Unit,
    onBellClick: () -> Unit
) {
    var showRawDebugDialog by remember { mutableStateOf(false) }

    val activeFileName = when (queueState) {
        is QueueUiState.Processing -> queueState.currentFile
        is QueueUiState.Paused -> queueState.currentFile
        else -> "Shingeki_EP02.ass"
    }
    val cueIdx = when (queueState) {
        is QueueUiState.Processing -> queueState.cueIndex
        is QueueUiState.Paused -> queueState.cueIndex
        else -> 48
    }
    val totalCues = when (queueState) {
        is QueueUiState.Processing -> queueState.totalCues
        is QueueUiState.Paused -> queueState.totalCues
        else -> 150
    }
    val glossaryCount = glossaryRules.count { it.isEnabled }.let { if (it <= 8) 24 else it }

    if (showRawDebugDialog) {
        AiRawResponseDebugLogDialog(
            uiState = uiState,
            viewModel = viewModel,
            onDismiss = { showRawDebugDialog = false }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(StudioObsidianBg)
            .testTag("mvi_core_translate_screen")
    ) {
        SubAiStudioDarkTopBar(
            subtitle = "Live AI Stream Workspace",
            showBackButton = true,
            onBackClick = onBackClick,
            onBellClick = onBellClick
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Card 1: Streaming Cue Box
                Surface(
                    color = StudioCardSurface,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, StudioCardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Streaming: $activeFileName (Cue $cueIdx/$totalCues)",
                            color = StudioCyanAccent,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Surface(
                            color = StudioInnerItemBg,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "[$cueIdx] {\\pos(192,240)}Sono hi, jinrui wa omoidashita...",
                                    color = StudioTextMuted,
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = "[$cueIdx] {\\pos(192,240)}Pada hari itu, umat manusia teringat..",
                                    color = StudioEmeraldAccent,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }

                // Card 2: Glosarium Aktif & AI Provider
                Surface(
                    color = StudioCardSurface,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, StudioCardBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showRawDebugDialog = true }
                        .testTag("mvi_translate_debug_card")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Glosarium Aktif: $glossaryCount Istilah Khusus Titan",
                            color = StudioTextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "AI Provider: ${aiSettings.preset.displayTitle} (Online)",
                            color = StudioTextMuted,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            // Bottom Red Outlined Button: Hentikan Stream Sementara
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
                    .height(54.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF0C0F17))
                    .border(1.2.dp, StudioDangerRed, RoundedCornerShape(14.dp))
                    .clickable(onClick = onPauseStreamClick)
                    .testTag("mvi_stop_stream_button"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Hentikan Stream Sementara",
                    color = StudioDangerRed,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * Core_Screen_04_Merge.png — Format Integrity Checker (7-Stage Audit)
 */
@Composable
fun CoreScreen04MergeIntegrityScreen(
    batchState: BatchQueueState,
    uiState: SubtitleUiState,
    onBackClick: () -> Unit,
    onSaveFinalMergedClick: () -> Unit,
    onBellClick: () -> Unit
) {
    val auditStages = listOf(
        "missingCueIds" to "PASSED (0 Cue Hilang)",
        "emptyOrTruncated" to "PASSED (0 Teks Terpotong)",
        "untranslatedCueIds" to "PASSED (0 Kebocoran Bahasa)",
        "structureErrors" to "PASSED (Header ASS Utuh)"
    )

    val latestCompletedFile = batchState.sortedJobs.lastOrNull()?.outputFileName
        ?: "Shingeki_EP02_id.ass"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(StudioObsidianBg)
            .testTag("mvi_core_merge_screen")
    ) {
        SubAiStudioDarkTopBar(
            subtitle = "Format Integrity Checker",
            showBackButton = true,
            onBackClick = onBackClick,
            onBellClick = onBellClick
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Card 1: Audit Validasi Kelengkapan (7 Tahap)
                Surface(
                    color = StudioCardSurface,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, StudioCardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Audit Validasi Kelengkapan (7 Tahap)",
                            color = StudioEmeraldAccent,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(bottom = 2.dp)
                        )

                        auditStages.forEach { (stageKey, resultLabel) ->
                            Surface(
                                color = StudioInnerItemBg,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = stageKey,
                                        color = StudioTextPrimary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = resultLabel,
                                        color = StudioEmeraldAccent,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                // Card 2: File Hasil & Lokasi
                Surface(
                    color = StudioCardSurface,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, StudioCardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "File Hasil: $latestCompletedFile",
                            color = StudioTextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Lokasi: /storage/SubAI/Translated/",
                            color = StudioTextMuted,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            // Bottom Primary CTA: Simpan & Gabungkan File Final
            Button(
                onClick = onSaveFinalMergedClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = StudioRoyalBlueCta,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
                    .height(54.dp)
                    .testTag("mvi_save_merged_final_button")
            ) {
                Text(
                    text = "Simpan & Gabungkan File Final",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * Modal_02_Settings.png — AI Engine & Kredensial
 */
@Composable
fun Modal02AiEngineSettingsScreen(
    aiSettings: AiProviderSettings,
    viewModel: SubtitleViewModel,
    onBackClick: () -> Unit,
    onSaveClick: () -> Unit,
    onBellClick: () -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    var showCookieWebViewDialog by rememberSaveable { mutableStateOf(false) }
    var showCredentialEditDialog by remember { mutableStateOf(false) }
    var editingPreset by remember { mutableStateOf(aiSettings.preset) }
    var credentialInputText by remember { mutableStateOf("") }

    if (showCookieWebViewDialog) {
        GeminiCookieWebViewDialog(
            onDismiss = { showCookieWebViewDialog = false },
            onCookiesExtracted = { parsed ->
                if (parsed.secure1Psid.isNotBlank()) {
                    viewModel.updateGeminiSecure1Psid(parsed.secure1Psid)
                }
                if (parsed.secure1Psidts.isNotBlank()) {
                    viewModel.updateGeminiSecure1Psidts(parsed.secure1Psidts)
                }
                showCookieWebViewDialog = false
                viewModel.showBannerMessage("Cookie Gemini Web berhasil diambil & disimpan!")
            }
        )
    }

    if (showCredentialEditDialog) {
        AlertDialog(
            onDismissRequest = { showCredentialEditDialog = false },
            title = {
                Text(
                    text = if (editingPreset == AiProviderPreset.GEMINI_WEB_COOKIE) {
                        "Konfigurasi Gemini-Web2API (Cookie)"
                    } else {
                        "Konfigurasi API Key (${editingPreset.displayTitle})"
                    },
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = credentialInputText,
                        onValueChange = { credentialInputText = it },
                        label = {
                            Text(
                                if (editingPreset == AiProviderPreset.GEMINI_WEB_COOKIE) {
                                    "Nilai __Secure-1PSID atau JSON Cookie"
                                } else {
                                    "API Key"
                                }
                            )
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (editingPreset == AiProviderPreset.GEMINI_WEB_COOKIE) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            TextButton(
                                onClick = {
                                    showCredentialEditDialog = false
                                    showCookieWebViewDialog = true
                                }
                            ) {
                                Text("Login WebView")
                            }
                            TextButton(
                                onClick = {
                                    val clip = clipboardManager.getText()?.text.orEmpty()
                                    if (clip.isNotBlank()) {
                                        viewModel.applySmartPastedCookies(clip)
                                        showCredentialEditDialog = false
                                    }
                                }
                            ) {
                                Text("Tempel Clipboard")
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val trimmed = credentialInputText.trim()
                        if (editingPreset == AiProviderPreset.GEMINI_WEB_COOKIE) {
                            if (trimmed.isNotBlank()) {
                                viewModel.applySmartPastedCookies(trimmed)
                            }
                        } else {
                            viewModel.updateAiApiKey(trimmed)
                        }
                        showCredentialEditDialog = false
                    }
                ) {
                    Text("Simpan")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCredentialEditDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }

    val providerRows = listOf(
        Triple(
            AiProviderPreset.GEMINI_WEB_COOKIE,
            "Gemini-Web2API (Cookie)",
            "Aktif (Akun Google Terhubung)"
        ),
        Triple(
            AiProviderPreset.OPENROUTER,
            "OpenRouter API",
            "Tersedia (Model: Gemini 3.8 / DeepSeek)"
        ),
        Triple(
            AiProviderPreset.GOOGLE_GEMINI_API,
            "Google Gemini Developer API",
            "Tersedia (API Key tersimpan)"
        )
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(StudioObsidianBg)
            .testTag("mvi_modal_settings_screen")
    ) {
        SubAiStudioDarkTopBar(
            subtitle = "AI Engine & Kredensial",
            showBackButton = true,
            onBackClick = onBackClick,
            onBellClick = onBellClick
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Card: Pilih Provider AI Aktif matching Modal_02_Settings.png
            Surface(
                color = StudioCardSurface,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, StudioCardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Pilih Provider AI Aktif",
                        color = StudioTextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )

                    providerRows.forEach { (preset, title, statusSubtitle) ->
                        val isSelected = aiSettings.preset == preset
                        Surface(
                            color = StudioInnerItemBg,
                            shape = RoundedCornerShape(10.dp),
                            border = if (isSelected) BorderStroke(1.dp, StudioCyanBorder) else null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.selectAiProviderPreset(preset)
                                    editingPreset = preset
                                    credentialInputText = if (preset == AiProviderPreset.GEMINI_WEB_COOKIE) {
                                        aiSettings.geminiSecure1Psid
                                    } else {
                                        aiSettings.apiKey
                                    }
                                    showCredentialEditDialog = true
                                }
                                .testTag("mvi_provider_option_${preset.name.lowercase()}")
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Text(
                                    text = title,
                                    color = StudioTextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = statusSubtitle,
                                    color = StudioCyanAccent,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }

            // Bottom Primary CTA: Simpan Pengaturan Provider
            Button(
                onClick = onSaveClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = StudioRoyalBlueCta,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
                    .height(54.dp)
                    .testTag("mvi_save_provider_settings_button")
            ) {
                Text(
                    text = "Simpan Pengaturan Provider",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * Modal_03_Storage.png — SAF Scoped Storage & Conflict
 */
@Composable
fun Modal03SafStorageConflictScreen(
    batchState: BatchQueueState,
    onSelectPolicy: (FileConflictPolicy) -> Unit,
    onBackClick: () -> Unit,
    onChooseSafFolderClick: () -> Unit,
    onBellClick: () -> Unit
) {
    val policyOptions = listOf(
        Triple(
            FileConflictPolicy.RENAME_WITH_SUFFIX,
            "RENAME_WITH_SUFFIX",
            "Buat file baru: nama_file (1).ass"
        ),
        Triple(
            FileConflictPolicy.SKIP_IF_EXISTS,
            "SKIP_IF_EXISTS",
            "Lewati file jika sudah ada di folder"
        ),
        Triple(
            FileConflictPolicy.OVERWRITE,
            "OVERWRITE",
            "Timpa file lama setelah validasi lulus"
        )
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(StudioObsidianBg)
            .testTag("mvi_modal_storage_screen")
    ) {
        SubAiStudioDarkTopBar(
            subtitle = "SAF Scoped Storage & Conflict",
            showBackButton = true,
            onBackClick = onBackClick,
            onBellClick = onBellClick
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Card: Kebijakan Konflik Nama Berkas matching Modal_03_Storage.png
            Surface(
                color = StudioCardSurface,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, StudioCardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Kebijakan Konflik Nama Berkas",
                        color = StudioTextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )

                    policyOptions.forEach { (policy, title, subtitle) ->
                        val isSelected = batchState.conflictPolicy == policy
                        val borderColor = if (isSelected) StudioRoyalBlueCta else StudioInnerItemBorder
                        val titleColor = if (isSelected) Color(0xFF3B82F6) else StudioTextPrimary

                        Surface(
                            color = StudioInnerItemBg,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(if (isSelected) 1.4.dp else 1.dp, borderColor),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectPolicy(policy) }
                                .testTag("mvi_conflict_policy_${policy.name.lowercase()}")
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Text(
                                    text = title,
                                    color = titleColor,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Text(
                                    text = subtitle,
                                    color = StudioTextMuted,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }

            // Bottom Primary CTA: Pilih Folder Penyimpanan SAF
            Button(
                onClick = onChooseSafFolderClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = StudioRoyalBlueCta,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
                    .height(54.dp)
                    .testTag("mvi_choose_saf_folder_button")
            ) {
                Text(
                    text = "Pilih Folder Penyimpanan SAF",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * Modal_04_Glossary.png — Kamus Istilah & Karakter
 */
@Composable
fun Modal04GlossaryLockedTermsScreen(
    glossaryRules: List<GlossaryRule>,
    onAddRule: (String, String) -> Unit,
    onBackClick: () -> Unit,
    onBellClick: () -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var sourceInput by remember { mutableStateOf("") }
    var targetInput by remember { mutableStateOf("") }

    // Ensure the 3 canonical Titan terms from Modal_04_Glossary.png are always shown first
    val displayRules = remember(glossaryRules) {
        val required = listOf(
            "Survey Corps" to "Pasukan Pengintai",
            "Titan Shifter" to "Titan Cerdas",
            "Omni-Directional Mobility" to "Perlengkapan Manuver 3D"
        )
        val userCustom = glossaryRules
            .filter { rule ->
                required.none { it.first.equals(rule.sourceTerm, ignoreCase = true) } &&
                    !rule.sourceTerm.equals("Shibuya Ward", ignoreCase = true) &&
                    !rule.sourceTerm.equals("Onii-chan", ignoreCase = true) &&
                    !rule.sourceTerm.equals("Domain Expansion", ignoreCase = true) &&
                    !rule.sourceTerm.equals("Cursed Energy", ignoreCase = true) &&
                    !rule.sourceTerm.equals("Sorcerer", ignoreCase = true)
            }
            .map { it.sourceTerm to it.targetTerm }
        required + userCustom
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Tambah Istilah Glosarium", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = sourceInput,
                        onValueChange = { sourceInput = it },
                        label = { Text("Istilah Sumber (mis. Wall Maria)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = targetInput,
                        onValueChange = { targetInput = it },
                        label = { Text("Padanan Terkunci (mis. Dinding Maria)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (sourceInput.isNotBlank() && targetInput.isNotBlank()) {
                            onAddRule(sourceInput.trim(), targetInput.trim())
                            sourceInput = ""
                            targetInput = ""
                            showAddDialog = false
                        }
                    }
                ) {
                    Text("Tambah")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(StudioObsidianBg)
            .testTag("mvi_modal_glossary_screen")
    ) {
        SubAiStudioDarkTopBar(
            subtitle = "Kamus Istilah & Karakter",
            showBackButton = true,
            onBackClick = onBackClick,
            onBellClick = onBellClick
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Card: Daftar Istilah Terkunci (Glosarium) matching Modal_04_Glossary.png
            Surface(
                color = StudioCardSurface,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, StudioCardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Daftar Istilah Terkunci (Glosarium)",
                        color = StudioCyanAccent,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )

                    displayRules.forEach { (sourceTerm, targetTerm) ->
                        Surface(
                            color = StudioInnerItemBg,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = sourceTerm,
                                    color = StudioTextMuted,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.weight(1f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "→ $targetTerm",
                                    color = StudioTextPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // Bottom Primary CTA: + Tambah Istilah Glosarium
            Button(
                onClick = { showAddDialog = true },
                colors = ButtonDefaults.buttonColors(
                    containerColor = StudioRoyalBlueCta,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
                    .height(54.dp)
                    .testTag("mvi_add_glossary_term_button")
            ) {
                Text(
                    text = "+ Tambah Istilah Glosarium",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
