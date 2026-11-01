package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Cookie
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlayCircleFilled
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.domain.SubtitleParserAndMerger
import com.example.model.AiProviderSettings
import com.example.model.BatchSizeOption
import com.example.model.GlossaryRule
import com.example.model.IndexFormatMode
import com.example.model.LineSeparatorMode
import com.example.model.SubtitleCue
import com.example.model.TargetLanguageOption
import com.example.model.ThemeSettings
import com.example.model.TranslationStylePreset
import com.example.ui.components.OpenAiDirectTranslateCard
import com.example.ui.components.RealtimeSubtitleLineCard
import com.example.ui.components.ResponsivePrimarySecondaryButtons
import com.example.ui.components.StatusPillBadge
import com.example.ui.components.StudioSectionCard
import com.example.ui.components.StudioToggleRow
import com.example.ui.theme.subtitleEditorTextStyle
import com.example.viewmodel.SubtitleUiState
import com.example.viewmodel.SubtitleViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExtractTabScreen(
    uiState: SubtitleUiState,
    aiSettings: AiProviderSettings,
    glossaryRules: List<GlossaryRule>,
    themeSettings: ThemeSettings,
    viewModel: SubtitleViewModel,
    onOpenFilePicker: () -> Unit,
    isWideScreen: Boolean = false,
    modifier: Modifier = Modifier
) {
    val activeBatch = uiState.activeBatchCues
    val activeGlossaryCount = glossaryRules.count { it.isEnabled }
    val targetLanguageLabel = if (uiState.targetLanguageOption == TargetLanguageOption.CUSTOM) {
        uiState.customTargetLanguage.ifBlank { "Bahasa Indonesia" }
    } else {
        uiState.targetLanguageOption.label
    }

    var showAiProviderSheet by rememberSaveable { mutableStateOf(false) }
    var showExtractionRulesSheet by rememberSaveable { mutableStateOf(false) }

    val previewExtractedLines = remember(
        activeBatch,
        uiState.separatorMode,
        uiState.customSeparator,
        uiState.protectAssTags,
        uiState.indexFormatMode
    ) {
        activeBatch.joinToString("\n") { cue ->
            SubtitleParserAndMerger.formatCueForExtraction(
                cue = cue,
                separatorMode = uiState.separatorMode,
                customSeparator = uiState.customSeparator,
                protectAssTags = uiState.protectAssTags,
                indexFormatMode = uiState.indexFormatMode
            )
        }
    }

    // Bottom Sheet 1: AI Provider, Cookie & Model Discovery
    if (showAiProviderSheet) {
        val aiSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showAiProviderSheet = false },
            sheetState = aiSheetState,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OpenAiDirectTranslateCard(
                    uiState = uiState,
                    aiSettings = aiSettings,
                    glossaryRules = glossaryRules,
                    themeSettings = themeSettings,
                    viewModel = viewModel
                )
            }
        }
    }

    // Bottom Sheet 2: Extraction Rules, ASS Tag Protection, Prompt & Raw Preview
    if (showExtractionRulesSheet) {
        val rulesSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showExtractionRulesSheet = false },
            sheetState = rulesSheetState,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                SeparatorAndAssProtectionCard(
                    uiState = uiState,
                    viewModel = viewModel
                )
                AiPromptAndGlossaryConfigCard(
                    uiState = uiState,
                    activeGlossaryCount = activeGlossaryCount,
                    viewModel = viewModel
                )
                BatchPreviewAndCopyCard(
                    uiState = uiState,
                    themeSettings = themeSettings,
                    activeGlossaryCount = activeGlossaryCount,
                    previewExtractedLines = previewExtractedLines,
                    viewModel = viewModel
                )
            }
        }
    }

    if (isWideScreen) {
        // Responsive Dual-Pane for Landscape / Tablet: Compact Controls on Left, Subtitle Cards on Right
        Row(
            modifier = modifier
                .fillMaxSize()
                .imePadding()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            LazyColumn(
                modifier = Modifier
                    .weight(0.95f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                item(key = "compact_control_header") {
                    CompactWorkspaceControlHeader(
                        uiState = uiState,
                        aiSettings = aiSettings,
                        activeGlossaryCount = activeGlossaryCount,
                        viewModel = viewModel,
                        onOpenFilePicker = onOpenFilePicker,
                        onOpenAiSettingsSheet = { showAiProviderSheet = true },
                        onOpenExtractionRulesSheet = { showExtractionRulesSheet = true }
                    )
                }
                item(key = "openai_direct_translate_card_wide") {
                    OpenAiDirectTranslateCard(
                        uiState = uiState,
                        aiSettings = aiSettings,
                        glossaryRules = glossaryRules,
                        themeSettings = themeSettings,
                        viewModel = viewModel
                    )
                }
            }

            LazyColumn(
                modifier = Modifier
                    .weight(1.05f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                realtimeSubtitleCardsSection(
                    activeBatch = activeBatch,
                    uiState = uiState,
                    targetLanguageLabel = targetLanguageLabel,
                    themeSettings = themeSettings,
                    viewModel = viewModel
                )
            }
        }
    } else {
        // Portrait Mobile Layout: 1 Compact Control Header directly above the Subtitle Cards
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .imePadding(),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item(key = "compact_control_header") {
                CompactWorkspaceControlHeader(
                    uiState = uiState,
                    aiSettings = aiSettings,
                    activeGlossaryCount = activeGlossaryCount,
                    viewModel = viewModel,
                    onOpenFilePicker = onOpenFilePicker,
                    onOpenAiSettingsSheet = { showAiProviderSheet = true },
                    onOpenExtractionRulesSheet = { showExtractionRulesSheet = true }
                )
            }

            realtimeSubtitleCardsSection(
                activeBatch = activeBatch,
                uiState = uiState,
                targetLanguageLabel = targetLanguageLabel,
                themeSettings = themeSettings,
                viewModel = viewModel
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CompactWorkspaceControlHeader(
    uiState: SubtitleUiState,
    aiSettings: AiProviderSettings,
    activeGlossaryCount: Int,
    viewModel: SubtitleViewModel,
    onOpenFilePicker: () -> Unit,
    onOpenAiSettingsSheet: () -> Unit,
    onOpenExtractionRulesSheet: () -> Unit
) {
    val context = LocalContext.current
    val activeBatch = uiState.activeBatchCues
    val allBatches = uiState.batches
    val activeBatchNum = uiState.selectedBatchIndex + 1
    var showSampleOptions by rememberSaveable { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("compact_workspace_control_header"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Row 1: File Name, Format/Lines Pill, Active AI Model Pill & Matched Progress
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = uiState.document.fileName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${uiState.document.format.extension.uppercase()} • ${uiState.document.cues.size} Dialog • Cocok ${uiState.matchedCount}/${uiState.document.cues.size}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                StatusPillBadge(
                    text = aiSettings.selectedModel,
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            // Row 2: Quick Action Chips (Buka File, Sampel, Pengaturan AI & Cookie, Aturan & Prompt)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilledTonalButton(
                    onClick = onOpenFilePicker,
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier
                        .defaultMinSize(minHeight = 40.dp)
                        .testTag("open_subtitle_file_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.FolderOpen,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Buka File", style = MaterialTheme.typography.labelLarge)
                }

                FilterChip(
                    selected = false,
                    onClick = onOpenAiSettingsSheet,
                    leadingIcon = {
                        Icon(
                            imageVector = if (aiSettings.preset.isGeminiWebFamily) {
                                Icons.Default.Cookie
                            } else {
                                Icons.Default.SmartToy
                        },
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    label = {
                        val cookieState = if (aiSettings.preset.isGeminiWebFamily) {
                            if (aiSettings.hasGeminiCookie) "Cookie Aktif" else "Set Cookie"
                        } else {
                            aiSettings.preset.label
                        }
                        Text("AI &Model ($cookieState)")
                    },
                    modifier = Modifier
                        .defaultMinSize(minHeight = 40.dp)
                        .testTag("open_ai_settings_sheet_button")
                )

                FilterChip(
                    selected = false,
                    onClick = onOpenExtractionRulesSheet,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    label = { Text("Aturan & Prompt") },
                    modifier = Modifier
                        .defaultMinSize(minHeight = 40.dp)
                        .testTag("open_extraction_rules_sheet_button")
                )

                FilterChip(
                    selected = showSampleOptions,
                    onClick = { showSampleOptions = !showSampleOptions },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Movie,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    label = { Text("Sampel") },
                    modifier = Modifier
                        .defaultMinSize(minHeight = 40.dp)
                        .testTag("toggle_sample_options_chip")
                )
            }

            AnimatedVisibility(visible = showSampleOptions) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            viewModel.loadSampleAss()
                            showSampleOptions = false
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 42.dp)
                            .testTag("load_sample_ass_button")
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Sampel .ASS", style = MaterialTheme.typography.labelMedium)
                    }

                    OutlinedButton(
                        onClick = {
                            viewModel.loadSampleSrt()
                            showSampleOptions = false
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 42.dp)
                            .testTag("load_sample_srt_button")
                    ) {
                        Icon(Icons.Default.Movie, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Sampel .SRT", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }

            // Row 3: Batch Selector Chips (shown when multiple batches exist)
            if (allBatches.size > 1) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    allBatches.forEachIndexed { index, batchList ->
                        val bFirst = batchList.firstOrNull()?.id ?: 1
                        val bLast = batchList.lastOrNull()?.id ?: batchList.size
                        FilterChip(
                            selected = uiState.selectedBatchIndex == index,
                            onClick = { viewModel.selectBatchIndex(index) },
                            label = { Text("Batch ${index + 1} (#$bFirst–#$bLast)") },
                            modifier = Modifier.testTag("batch_chip_$index")
                        )
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))

            // Row 4: Live Translation Status Banner (if active or recent)
            if (uiState.isTranslatingAi || !uiState.aiTranslationStatusText.isNullOrBlank()) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = uiState.aiTranslationStatusText ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.testTag("ai_translation_status_text")
                        )
                        if (uiState.isTranslatingAi) {
                            LinearProgressIndicator(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp))
                            )
                        }
                    }
                }
            }

            // Row 5: Primary Execution Bar (Terjemahkan AI, Semua Batch, Copy + Prompt, Animasi)
            if (uiState.isTranslatingAi) {
                Button(
                    onClick = { viewModel.cancelAiTranslation() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 46.dp)
                        .testTag("stop_ai_translation_button")
                ) {
                    Icon(Icons.Default.StopCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Hentikan Terjemahan AI")
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = { viewModel.translateCurrentBatchWithAi() },
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        modifier = Modifier
                            .weight(1.15f)
                            .defaultMinSize(minHeight = 46.dp)
                            .testTag("translate_active_batch_ai_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = null,
                            modifier = Modifier.size(17.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (allBatches.size > 1) "Terjemahkan #$activeBatchNum" else "Terjemahkan AI",
                            style = MaterialTheme.typography.labelLarge,
                            maxLines = 1
                        )
                    }

                    if (allBatches.size > 1) {
                        FilledTonalButton(
                            onClick = { viewModel.translateAllBatchesWithAi() },
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                            modifier = Modifier
                                .defaultMinSize(minHeight = 46.dp)
                                .testTag("translate_all_batches_ai_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayCircleFilled,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Semua (${allBatches.size})", style = MaterialTheme.typography.labelMedium)
                        }
                    }

                    FilledTonalButton(
                        onClick = {
                            val fullText = viewModel.buildCurrentBatchOutput(withPromptHeader = true)
                            copyToClipboard(context, "SubAI Prompt + Batch", fullText)
                            val glossaryNote = if (uiState.includeGlossaryInPrompt && activeGlossaryCount > 0) {
                                " + $activeGlossaryCount glosarium"
                            } else {
                                ""
                            }
                            viewModel.showBannerMessage(
                                "Disalin ${activeBatch.size} baris + Prompt (${aiSettings.selectedModel})$glossaryNote!"
                            )
                        },
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                        modifier = Modifier
                            .defaultMinSize(minHeight = 46.dp)
                            .testTag("copy_with_prompt_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy dengan Prompt AI",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Copy", style = MaterialTheme.typography.labelLarge)
                    }

                    OutlinedButton(
                        onClick = { viewModel.simulateRealtimeTranslationOnActiveBatch() },
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                        modifier = Modifier
                            .defaultMinSize(minHeight = 46.dp)
                            .testTag("header_replay_realtime_animation_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Putar Animasi Realtime",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Animasi", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}

private fun LazyListScope.realtimeSubtitleCardsSection(
    activeBatch: List<SubtitleCue>,
    uiState: SubtitleUiState,
    targetLanguageLabel: String,
    themeSettings: ThemeSettings,
    viewModel: SubtitleViewModel
) {
    item(key = "realtime_cards_section_header") {
        val translatedInBatch = activeBatch.count { uiState.parsedTranslations.containsKey(it.id) }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 2.dp)
                .testTag("realtime_cards_header_banner"),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Daftar Baris Subtitle ($translatedInBatch/${activeBatch.size} Diterjemahkan)",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Ketuk kartu untuk edit langsung",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }

    items(
        items = activeBatch,
        key = { cue -> "realtime_cue_card_${cue.id}" }
    ) { cue ->
        val formattedLineWithIndex = SubtitleParserAndMerger.formatCueForExtraction(
            cue = cue,
            separatorMode = uiState.separatorMode,
            customSeparator = uiState.customSeparator,
            protectAssTags = uiState.protectAssTags,
            indexFormatMode = uiState.indexFormatMode
        )

        RealtimeSubtitleLineCard(
            cue = cue,
            formattedOriginalText = formattedLineWithIndex,
            translatedText = uiState.parsedTranslations[cue.id],
            isTranslatingThisCue = cue.id in uiState.translatingCueIds,
            isRecentlyTranslated = cue.id in uiState.recentlyTranslatedCueIds,
            targetLanguageLabel = targetLanguageLabel,
            themeSettings = themeSettings,
            onUpdateTranslatedLine = { newText ->
                viewModel.updateSingleCueTranslation(cue.id, newText)
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SeparatorAndAssProtectionCard(
    uiState: SubtitleUiState,
    viewModel: SubtitleViewModel
) {
    StudioSectionCard(
        title = "Pemisah Baris Ganda & Proteksi Tag",
        subtitle = "Gabung dialog multi-baris (\\N / enter) agar penomoran AI tetap presisi",
        icon = Icons.Default.Security,
        iconTint = MaterialTheme.colorScheme.primary
    ) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            LineSeparatorMode.entries.forEach { mode ->
                FilterChip(
                    selected = uiState.separatorMode == mode,
                    onClick = { viewModel.setSeparatorMode(mode) },
                    label = { Text(mode.label) },
                    modifier = Modifier
                        .defaultMinSize(minHeight = 40.dp)
                        .testTag("separator_chip_${mode.name.lowercase()}")
                )
            }
        }

        Text(
            text = uiState.separatorMode.description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary
        )

        AnimatedVisibility(visible = uiState.separatorMode == LineSeparatorMode.CUSTOM) {
            OutlinedTextField(
                value = uiState.customSeparator,
                onValueChange = { viewModel.setCustomSeparator(it) },
                label = { Text("Simbol Pemisah Kustom (Contoh:  //  atau  <br> )") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("custom_separator_input")
            )
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

        StudioToggleRow(
            title = "Sembunyikan Override Tags .ASS ({\\...})",
            description = "Mengamankan {\\an8}, {\\pos(x,y)}, {\\fad} saat ekstraksi dan memulihkannya saat Merge.",
            checked = uiState.protectAssTags,
            onCheckedChange = { viewModel.setProtectAssTags(it) },
            testTag = "protect_ass_tags_switch"
        )

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

        Text(
            text = "Format Penomoran Indeks:",
            style = MaterialTheme.typography.titleSmall
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            IndexFormatMode.entries.forEach { mode ->
                FilterChip(
                    selected = uiState.indexFormatMode == mode,
                    onClick = { viewModel.setIndexFormatMode(mode) },
                    label = { Text(mode.label) },
                    modifier = Modifier.testTag("index_format_${mode.name.lowercase()}")
                )
            }
        }

        Text(
            text = "Ukuran Batch Ekstraksi:",
            style = MaterialTheme.typography.titleSmall
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            BatchSizeOption.entries.forEach { option ->
                FilterChip(
                    selected = uiState.batchSizeOption == option,
                    onClick = { viewModel.setBatchSizeOption(option) },
                    label = { Text(option.label) },
                    modifier = Modifier.testTag("batch_size_${option.name.lowercase()}")
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AiPromptAndGlossaryConfigCard(
    uiState: SubtitleUiState,
    activeGlossaryCount: Int,
    viewModel: SubtitleViewModel
) {
    var showPromptAdvanced by rememberSaveable { mutableStateOf(true) }

    StudioSectionCard(
        title = "Konfigurasi Gaya Bahasa & Glosarium Prompt",
        subtitle = "Berlaku untuk System Prompt terjemahan langsung maupun Copy + Prompt AI",
        icon = Icons.Default.Translate,
        iconTint = MaterialTheme.colorScheme.secondary,
        headerTrailing = {
            FilterChip(
                selected = showPromptAdvanced,
                onClick = { showPromptAdvanced = !showPromptAdvanced },
                label = { Text(if (showPromptAdvanced) "Ringkas" else "Detail") }
            )
        }
    ) {
        StudioToggleRow(
            title = "Sertakan Instruksi System Prompt saat Copy",
            description = "Menambahkan aturan penjagaan nomor baris dan simbol pemisah di atas daftar dialog.",
            checked = uiState.includeAiPromptHeader,
            onCheckedChange = { viewModel.setIncludeAiPromptHeader(it) },
            testTag = "include_ai_prompt_switch"
        )

        StudioToggleRow(
            title = "Sertakan Glosarium di System Prompt (Pre-Translation)",
            description = "Menyisipkan $activeGlossaryCount istilah aktif dari Tab Kamus agar AI konsisten sejak awal.",
            checked = uiState.includeGlossaryInPrompt,
            onCheckedChange = { viewModel.setIncludeGlossaryInPrompt(it) },
            testTag = "include_glossary_prompt_switch"
        )

        AnimatedVisibility(visible = showPromptAdvanced) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                Text(
                    text = "Bahasa Target Terjemahan:",
                    style = MaterialTheme.typography.titleSmall
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    TargetLanguageOption.entries.forEach { lang ->
                        FilterChip(
                            selected = uiState.targetLanguageOption == lang,
                            onClick = { viewModel.setTargetLanguageOption(lang) },
                            label = { Text(lang.label) },
                            modifier = Modifier.testTag("target_lang_${lang.name.lowercase()}")
                        )
                    }
                }

                if (uiState.targetLanguageOption == TargetLanguageOption.CUSTOM) {
                    OutlinedTextField(
                        value = uiState.customTargetLanguage,
                        onValueChange = { viewModel.setCustomTargetLanguage(it) },
                        label = { Text("Masukkan Bahasa Target Kustom") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Text(
                    text = "Preset Gaya Bahasa & Instruksi Kustom:",
                    style = MaterialTheme.typography.titleSmall
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    TranslationStylePreset.entries.forEach { preset ->
                        FilterChip(
                            selected = uiState.stylePreset == preset,
                            onClick = { viewModel.setTranslationStylePreset(preset) },
                            label = { Text(preset.label) },
                            modifier = Modifier.testTag("style_preset_${preset.name.lowercase()}")
                        )
                    }
                }

                OutlinedTextField(
                    value = uiState.customPromptInstruction,
                    onValueChange = { viewModel.setCustomPromptInstruction(it) },
                    label = { Text("Instruksi Gaya Terjemahan (Dapat Diedit Manual)") },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("custom_prompt_instruction_input")
                )
            }
        }
    }
}

@Composable
private fun BatchPreviewAndCopyCard(
    uiState: SubtitleUiState,
    themeSettings: ThemeSettings,
    activeGlossaryCount: Int,
    previewExtractedLines: String,
    viewModel: SubtitleViewModel
) {
    val context = LocalContext.current
    val activeBatch = uiState.activeBatchCues
    val firstId = activeBatch.firstOrNull()?.id ?: 0
    val lastId = activeBatch.lastOrNull()?.id ?: 0
    var showRawTextBox by rememberSaveable { mutableStateOf(false) }

    StudioSectionCard(
        title = "Pratinjau Teks Mentah & Copy Manual (#$firstId–#$lastId)",
        subtitle = "Salin baris bernomor + System Prompt adaptif atau hanya daftar baris",
        icon = Icons.Default.Description,
        iconTint = MaterialTheme.colorScheme.primary,
        headerTrailing = {
            FilterChip(
                selected = showRawTextBox,
                onClick = { showRawTextBox = !showRawTextBox },
                label = { Text(if (showRawTextBox) "Sembunyikan Teks" else "Lihat Teks Mentah") },
                modifier = Modifier.testTag("toggle_raw_extracted_text_chip")
            )
        }
    ) {
        AnimatedVisibility(visible = showRawTextBox) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 140.dp, max = 280.dp),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                border = BorderStroke(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                )
            ) {
                val boxScroll = rememberScrollState()
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(boxScroll)
                        .padding(12.dp)
                ) {
                    Text(
                        text = previewExtractedLines.ifEmpty { "Tidak ada baris dialog yang ditemukan pada file ini." },
                        style = subtitleEditorTextStyle(themeSettings),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.testTag("extracted_preview_text")
                    )
                }
            }
        }

        ResponsivePrimarySecondaryButtons(
            primaryLabel = "Copy + Prompt AI",
            primaryIcon = Icons.Default.AutoAwesome,
            primaryTestTag = "sheet_copy_with_prompt_button",
            onPrimaryClick = {
                val fullText = viewModel.buildCurrentBatchOutput(withPromptHeader = true)
                copyToClipboard(context, "SubAI Prompt + Batch", fullText)
                val glossaryNote = if (uiState.includeGlossaryInPrompt && activeGlossaryCount > 0) {
                    " + $activeGlossaryCount aturan glosarium"
                } else {
                    ""
                }
                viewModel.showBannerMessage(
                    "Disalin ${activeBatch.size} baris + System Prompt AI$glossaryNote ke Clipboard!"
                )
            },
            secondaryLabel = "Copy Baris Saja",
            secondaryIcon = Icons.Default.ContentCopy,
            secondaryTestTag = "copy_lines_only_button",
            onSecondaryClick = {
                val linesOnly = viewModel.buildCurrentBatchOutput(withPromptHeader = false)
                copyToClipboard(context, "SubAI Numbered Lines", linesOnly)
                viewModel.showBannerMessage("Disalin ${activeBatch.size} baris bernomor (tanpa prompt).")
            }
        )
    }
}

fun copyToClipboard(context: Context, label: String, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    clipboard?.setPrimaryClip(ClipData.newPlainText(label, text))
}
