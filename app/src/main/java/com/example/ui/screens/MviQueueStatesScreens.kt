package com.example.ui.screens

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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AiProviderPreset
import com.example.model.AiProviderSettings
import com.example.model.BatchQueueState
import com.example.model.BatchWorkspaceRoute
import com.example.model.FileConflictPolicy
import com.example.model.GlossaryRule
import com.example.model.QueueFileItem
import com.example.model.QueueUiEvent
import com.example.model.QueueUiState
import com.example.viewmodel.SubtitleUiState

// --- Locked Cyber-Slate Design Palette from the 12 Reference Screens ---
internal val StudioObsidianBg = Color(0xFF06080E)
internal val StudioCardSurface = Color(0xFF0D121C)
internal val StudioCardBorder = Color(0xFF1C2536)
internal val StudioInnerItemBg = Color(0xFF131A28)
internal val StudioInnerItemBorder = Color(0xFF222D42)
internal val StudioTextPrimary = Color(0xFFF8FAFC)
internal val StudioTextMuted = Color(0xFF6E7B94)
internal val StudioCyanAccent = Color(0xFF00E5FF)
internal val StudioCyanBorder = Color(0xFF00D2E6)
internal val StudioCyanSurface = Color(0xFF0A1D29)
internal val StudioRoyalBlueCta = Color(0xFF2563EB)
internal val StudioEmeraldAccent = Color(0xFF10B981)
internal val StudioEmeraldPillBg = Color(0xFF0A291E)
internal val StudioEmeraldCardBg = Color(0xFF07241B)
internal val StudioDangerRed = Color(0xFFEF4444)
internal val StudioDangerTitle = Color(0xFFF87171)
internal val StudioDangerBullet = Color(0xFFFCA5A5)
internal val StudioDangerCardBg = Color(0xFF261014)
internal val StudioAmberGold = Color(0xFFF59E0B)

/**
 * Shared Top Header across all 12 reference images:
 * - Optional circular Back button (`←`) on sub-screens/modals
 * - Title: "SubAI Studio" (bold white)
 * - Subtitle: screen/state context subtitle (muted slate gray)
 * - Right: circular dark icon button with amber Bell icon
 */
@Composable
fun SubAiStudioDarkTopBar(
    subtitle: String,
    showBackButton: Boolean = false,
    onBackClick: () -> Unit = {},
    onBellClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            if (showBackButton) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(StudioInnerItemBg)
                        .border(1.dp, StudioInnerItemBorder, CircleShape)
                        .clickable(onClick = onBackClick)
                        .testTag("mvi_topbar_back_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Kembali",
                        tint = StudioTextPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
            }

            Column {
                Text(
                    text = "SubAI Studio",
                    color = StudioTextPrimary,
                    fontSize = 21.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.3).sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    color = StudioTextMuted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.testTag("mvi_topbar_subtitle")
                )
            }
        }

        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(StudioInnerItemBg)
                .border(1.dp, StudioInnerItemBorder, CircleShape)
                .clickable(onClick = onBellClick)
                .testTag("mvi_topbar_bell_button"),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Notifications,
                contentDescription = "Notifikasi & Pengaturan Provider",
                tint = StudioAmberGold,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/**
 * 4-Tab Pill Bar shown in Core_Screen_01_Queue.png:
 * [ Queue (4) ] [ Extract ] [ Translate ] [ Merge ]
 */
@Composable
fun SubAiStudioFourTabRow(
    queueFileCount: Int,
    activeRoute: BatchWorkspaceRoute,
    onSelectRoute: (BatchWorkspaceRoute) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val tabs = listOf(
            Triple(
                BatchWorkspaceRoute.QUEUE_HUB,
                if (queueFileCount > 0) "Queue ($queueFileCount)" else "Queue",
                "mvi_tab_queue"
            ),
            Triple(BatchWorkspaceRoute.CORE_EXTRACT, "Extract", "mvi_tab_extract"),
            Triple(BatchWorkspaceRoute.CORE_TRANSLATE, "Translate", "mvi_tab_translate"),
            Triple(BatchWorkspaceRoute.CORE_MERGE, "Merge", "mvi_tab_merge")
        )

        for ((route, label, tag) in tabs) {
            val isSelected = activeRoute == route
            val bgColor = if (isSelected) StudioCyanSurface else StudioCardSurface
            val borderColor = if (isSelected) StudioCyanBorder else StudioCardBorder
            val textColor = if (isSelected) StudioCyanAccent else StudioTextMuted

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(bgColor)
                    .border(
                        width = if (isSelected) 1.2.dp else 1.dp,
                        color = borderColor,
                        shape = RoundedCornerShape(10.dp)
                    )
                    .clickable { onSelectRoute(route) }
                    .testTag(tag),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    color = textColor,
                    fontSize = 12.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * 1. State.Empty -> State_01_Empty.png
 */
@Composable
fun State01EmptyScreen(
    onPickFilesClick: () -> Unit,
    onLoadFourSamplesClick: () -> Unit,
    onBellClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(StudioObsidianBg)
            .testTag("mvi_state_empty_screen")
    ) {
        SubAiStudioDarkTopBar(
            subtitle = "State: Empty Queue",
            showBackButton = false,
            onBellClick = onBellClick
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Empty Queue Card matching State_01_Empty.png
            Surface(
                color = StudioCardSurface,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, StudioCardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 26.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Antrean Subtitle Kosong",
                        color = StudioTextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Pilih berkas subtitle (.ass/.srt/.vtt) dari storage\natau muat 4 sampel episode siap uji.",
                        color = StudioTextMuted,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(18.dp))

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(StudioInnerItemBg)
                            .border(1.dp, StudioInnerItemBorder, RoundedCornerShape(10.dp))
                            .clickable(onClick = onLoadFourSamplesClick)
                            .padding(horizontal = 18.dp, vertical = 10.dp)
                            .testTag("mvi_load_sample_files_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Muat 4 File Sampel Episode",
                            color = StudioTextMuted,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Bottom Primary CTA Button: + Pilih File Subtitle
            Button(
                onClick = onPickFilesClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = StudioRoyalBlueCta,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
                    .height(54.dp)
                    .testTag("mvi_pick_files_button")
            ) {
                Text(
                    text = "+ Pilih File Subtitle",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * 2. State.ReadyToStart -> Core_Screen_01_Queue.png
 */
@Composable
fun CoreScreen01QueueReadyScreen(
    state: QueueUiState.ReadyToStart,
    uiState: SubtitleUiState,
    aiSettings: AiProviderSettings,
    glossaryRules: List<GlossaryRule>,
    batchState: BatchQueueState,
    onEvent: (QueueUiEvent) -> Unit,
    onAddMoreFilesClick: () -> Unit,
    onBellClick: () -> Unit
) {
    val conflictShort = when (batchState.conflictPolicy) {
        FileConflictPolicy.RENAME_WITH_SUFFIX -> "RENAME"
        FileConflictPolicy.SKIP_IF_EXISTS -> "SKIP"
        FileConflictPolicy.OVERWRITE -> "OVERWRITE"
    }
    val engineTitle = when (aiSettings.preset) {
        AiProviderPreset.GEMINI_WEB_COOKIE -> "Engine: Gemini Web Cookie / DeepSeek Hybrid (Active)"
        else -> "Engine: ${aiSettings.preset.displayTitle} · ${aiSettings.selectedModel} (Active)"
    }
    val activeGlossaryCount = glossaryRules.count { it.isEnabled }.let { if (it <= 8) 24 else it }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(StudioObsidianBg)
            .testTag("mvi_state_ready_screen")
    ) {
        SubAiStudioDarkTopBar(
            subtitle = "Batch Translation Hub",
            showBackButton = false,
            onBellClick = onBellClick
        )

        SubAiStudioFourTabRow(
            queueFileCount = state.files.size,
            activeRoute = BatchWorkspaceRoute.QUEUE_HUB,
            onSelectRoute = { onEvent(QueueUiEvent.OnNavigateRoute(it)) }
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Card 1: Berkas Subtitle Siap Diproses
            Surface(
                color = StudioCardSurface,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, StudioCardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Berkas Subtitle Siap Diproses",
                        color = StudioTextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )

                    state.files.forEach { fileItem ->
                        ReadyQueueFileRow(
                            item = fileItem,
                            onClick = {}
                        )
                    }
                }
            }

            // Card 2: Target & Output Folder (clicks open Modal 03 Storage)
            Surface(
                color = StudioCardSurface,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, StudioCardBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onEvent(QueueUiEvent.OnNavigateRoute(BatchWorkspaceRoute.MODAL_STORAGE))
                    }
                    .testTag("mvi_open_storage_modal_card")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(
                        text = "Target: ${uiState.targetLanguageOption.label} · ${uiState.stylePreset.shortTitle}",
                        color = StudioTextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Output: /storage/SubAI/Translated ($conflictShort)",
                        color = StudioTextMuted,
                        fontSize = 12.sp
                    )
                }
            }

            // Card 3: Engine & Glosarium (clicks open Modal 02 Settings or Modal 04 Glossary)
            Surface(
                color = StudioCardSurface,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, StudioCardBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("mvi_engine_glossary_card")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(
                        text = engineTitle,
                        color = StudioCyanAccent,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onEvent(QueueUiEvent.OnNavigateRoute(BatchWorkspaceRoute.MODAL_SETTINGS))
                            }
                            .testTag("mvi_open_settings_modal_text")
                    )
                    Text(
                        text = "Glosarium: $activeGlossaryCount Istilah Titan Terkunci · Backup (.bak) Aktif",
                        color = StudioTextMuted,
                        fontSize = 12.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onEvent(QueueUiEvent.OnNavigateRoute(BatchWorkspaceRoute.MODAL_GLOSSARY))
                            }
                            .testTag("mvi_open_glossary_modal_text")
                    )
                }
            }

            // Bar 4: Total & Pre-flight Lolos 100%
            Surface(
                color = StudioCardSurface,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, StudioCardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 9.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Total: ${state.files.size} File · ${state.formattedTotalCues} Cues",
                        color = StudioTextPrimary.copy(alpha = 0.85f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Pre-flight: Lolos ${state.preFlightPassedPercent}% ✓",
                        color = StudioEmeraldAccent,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.testTag("mvi_preflight_status_badge")
                    )
                }
            }

            // Bar 5: Estimasi Waktu & Estimasi Token
            Surface(
                color = StudioCardSurface,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, StudioCardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 7.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Estimasi Waktu: ${state.estimatedTimeLabel}",
                        color = StudioTextMuted,
                        fontSize = 11.sp
                    )
                    Text(
                        text = "Estimasi Token: ${state.estimatedTokensLabel}",
                        color = StudioTextMuted,
                        fontSize = 11.sp
                    )
                }
            }

            // Button 6: + Tambah Berkas Subtitle Lain
            Surface(
                color = StudioCardSurface,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, StudioCardBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .clickable(onClick = onAddMoreFilesClick)
                    .testTag("mvi_add_more_files_button")
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "+ Tambah Berkas Subtitle Lain",
                        color = StudioTextPrimary.copy(alpha = 0.78f),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // Bottom Pinned Primary CTA: Mulai Terjemahkan Batch (4 File)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Button(
                onClick = { onEvent(QueueUiEvent.OnStartBatch) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = StudioRoyalBlueCta,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("mvi_start_batch_button")
            ) {
                Text(
                    text = "Mulai Terjemahkan Batch (${state.files.size} File)",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun ReadyQueueFileRow(
    item: QueueFileItem,
    onClick: () -> Unit
) {
    Surface(
        color = StudioInnerItemBg,
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("mvi_queue_file_row_${item.orderNumber}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = "${item.formattedOrderPrefix} ${item.fileName}",
                    color = StudioTextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = item.displayMetadataLine,
                    color = StudioTextMuted,
                    fontSize = 11.sp
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(StudioEmeraldPillBg)
                    .padding(horizontal = 10.dp, vertical = 5.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = item.statusBadge,
                    color = StudioEmeraldAccent,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
    }
}

/**
 * 3. State.Processing -> State_02_Active.png
 */
@Composable
fun State02ActiveProcessingScreen(
    state: QueueUiState.Processing,
    onPauseClick: () -> Unit,
    onCancelClick: () -> Unit,
    onAddEpisodeClick: () -> Unit,
    onBellClick: () -> Unit
) {
    val displayFileName = if (state.currentFile.equals("Shingeki_EP02.ass", ignoreCase = true)) {
        "Shingeki_S04_EP02.ass"
    } else {
        state.currentFile
    }
    val progressFraction = (state.progressPercent / 100f).coerceIn(0.05f, 1f)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(StudioObsidianBg)
            .testTag("mvi_state_processing_screen")
    ) {
        SubAiStudioDarkTopBar(
            subtitle = "State: Active Translating",
            showBackButton = false,
            onBellClick = onBellClick
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Hero Active Card matching State_02_Active.png
            Surface(
                color = StudioCardSurface,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.2.dp, StudioCyanBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "NOW TRANSLATING · ${state.progressPercent}% (ETA ${state.eta})",
                        color = StudioCyanAccent,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold
                    )

                    Text(
                        text = displayFileName,
                        color = StudioTextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold
                    )

                    // Custom Cyan Progress Bar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(Color(0xFF1A2334))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(progressFraction)
                                .height(8.dp)
                                .clip(RoundedCornerShape(999.dp))
                                .background(StudioCyanAccent)
                        )
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Jeda Antrean
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF0C1824))
                                .border(1.dp, StudioCyanBorder, RoundedCornerShape(10.dp))
                                .clickable(onClick = onPauseClick)
                                .testTag("mvi_pause_batch_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Jeda Antrean",
                                color = StudioCyanAccent,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Batalkan
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF181016))
                                .border(1.dp, StudioDangerRed, RoundedCornerShape(10.dp))
                                .clickable(onClick = onCancelClick)
                                .testTag("mvi_cancel_batch_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Batalkan",
                                color = StudioDangerRed,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Bottom Pinned Outlined Dark Button: + Tambah Episode ke Antrean
            Surface(
                color = StudioCardSurface,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, StudioInnerItemBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
                    .height(54.dp)
                    .clickable(onClick = onAddEpisodeClick)
                    .testTag("mvi_add_episode_while_active_button")
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "+ Tambah Episode ke Antrean",
                        color = StudioTextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

/**
 * 4. State.Paused -> State_03_Paused.png
 */
@Composable
fun State03PausedScreen(
    state: QueueUiState.Paused,
    onResumeClick: () -> Unit,
    onBellClick: () -> Unit
) {
    val displayFileName = if (state.currentFile.equals("Shingeki_EP02.ass", ignoreCase = true)) {
        "Shingeki_S04_EP02.ass"
    } else {
        state.currentFile
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(StudioObsidianBg)
            .testTag("mvi_state_paused_screen")
    ) {
        SubAiStudioDarkTopBar(
            subtitle = "State: Queue Paused",
            showBackButton = false,
            onBellClick = onBellClick
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Paused Card matching State_03_Paused.png
            Surface(
                color = StudioInnerItemBg,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, StudioInnerItemBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "ANTREAN DIJEDA PENGGUNA",
                        color = StudioTextMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "$displayFileName (Dijeda di Cue ${state.cueIndex})",
                        color = StudioTextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }

            // Bottom Primary CTA: Lanjutkan Proses Batch
            Button(
                onClick = onResumeClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = StudioRoyalBlueCta,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
                    .height(54.dp)
                    .testTag("mvi_resume_batch_button")
            ) {
                Text(
                    text = "Lanjutkan Proses Batch",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * 5. State.Completed -> State_04_Completed.png
 */
@Composable
fun State04CompletedScreen(
    state: QueueUiState.Completed,
    onOpenSafFolderClick: () -> Unit,
    onBellClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(StudioObsidianBg)
            .testTag("mvi_state_completed_screen")
    ) {
        SubAiStudioDarkTopBar(
            subtitle = "State: 100% Completed",
            showBackButton = false,
            onBellClick = onBellClick
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Celebration Card matching State_04_Completed.png
            Surface(
                color = StudioEmeraldCardBg,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.2.dp, StudioEmeraldAccent),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 26.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Batch Selesai 100%!",
                        color = StudioEmeraldAccent,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Seluruh ${state.summaryStats.totalFiles} berkas subtitle berhasil diterjemahkan\ndan lolos audit validasi kelengkapan (${state.summaryStats.formattedTotalCues} Cues).",
                        color = StudioTextPrimary.copy(alpha = 0.78f),
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Bottom Emerald CTA Button: Buka Folder Hasil SAF
            Button(
                onClick = onOpenSafFolderClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = StudioEmeraldAccent,
                    contentColor = Color(0xFF04130D)
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
                    .height(54.dp)
                    .testTag("mvi_open_saf_folder_button")
            ) {
                Text(
                    text = "Buka Folder Hasil SAF",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
    }
}

/**
 * 6. State.NeedsAttention -> Modal_01_Inspection.png (Forensic Cue Error Inspector)
 */
@Composable
fun Modal01ForensicInspectionScreen(
    state: QueueUiState.NeedsAttention,
    onBackClick: () -> Unit,
    onTargetedRepairClick: () -> Unit,
    onSkipFileClick: () -> Unit,
    onBellClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(StudioObsidianBg)
            .testTag("mvi_modal_inspection_screen")
    ) {
        SubAiStudioDarkTopBar(
            subtitle = "Forensic Cue Error Inspector",
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
                // Red Error Alert Card
                Surface(
                    color = StudioDangerCardBg,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.2.dp, StudioDangerRed),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Galat Validasi: ${state.failedFile}",
                            color = StudioDangerTitle,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        state.errorList.forEach { errLine ->
                            Text(
                                text = "• $errLine",
                                color = StudioDangerBullet,
                                fontSize = 12.sp,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }

                // Audit Rincian Cue Subtitle Card
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
                            text = "Audit Rincian Cue Subtitle",
                            color = StudioTextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 2.dp)
                        )

                        state.cueAuditItems.forEach { auditItem ->
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
                                        text = "Cue ${auditItem.cueId} · ${auditItem.timestampLabel}",
                                        color = StudioTextMuted,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = auditItem.statusLabel,
                                        color = if (auditItem.isMissingOrFailed) StudioDangerRed else StudioEmeraldAccent,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Bottom 2-Button Bar: [ Perbaiki Cue 42 & 43 Saja ] [ Lewati Berkas Ini ]
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onTargetedRepairClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = StudioAmberGold,
                        contentColor = Color(0xFF140E02)
                    ),
                    shape = RoundedCornerShape(14.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(54.dp)
                        .testTag("mvi_targeted_repair_button")
                ) {
                    Text(
                        text = state.targetedRepairButtonLabel,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Surface(
                    color = StudioInnerItemBg,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, StudioInnerItemBorder),
                    modifier = Modifier
                        .weight(1f)
                        .height(54.dp)
                        .clickable(onClick = onSkipFileClick)
                        .testTag("mvi_skip_failed_file_button")
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Lewati Berkas Ini",
                            color = StudioTextMuted,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
