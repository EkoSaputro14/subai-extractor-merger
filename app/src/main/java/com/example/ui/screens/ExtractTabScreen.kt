package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Numbers
import androidx.compose.material.icons.filled.PlayCircleFilled
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.domain.SubtitleParserAndMerger
import com.example.model.AiProviderPreset
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
import com.example.ui.components.ResponsivePrimarySecondaryButtons
import com.example.ui.components.StudioSectionCard
import com.example.ui.components.StudioToggleRow
import com.example.ui.theme.AlabasterSheetBg
import com.example.ui.theme.DeepForestHeaderBg
import com.example.ui.theme.EmeraldCheckGreen
import com.example.ui.theme.ForestGreenPrimary
import com.example.ui.theme.PrimaryForestButton
import com.example.ui.theme.SageHighlightFill
import com.example.ui.theme.SageSoftCardFill
import com.example.ui.theme.subtitleEditorTextStyle
import com.example.viewmodel.SubtitleUiState
import com.example.viewmodel.SubtitleViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
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
    val allCues = uiState.document.cues
    val activeGlossaryCount = glossaryRules.count { it.isEnabled }
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    // Allow viewing either all lines continuously (e.g. 1..300+) or filtered by active batch
    var showAllCuesInList by rememberSaveable { mutableStateOf(true) }
    val displayedCues = if (showAllCuesInList || uiState.batches.size <= 1) {
        allCues
    } else {
        activeBatch
    }

    // On compact portrait screen, start with -1 (collapsed clean list matching Screen 4 of reference)
    // Tapping any cue card (#001..#300) toggles inline editor right inside that exact card!
    var selectedCueId by rememberSaveable(uiState.document.fileName, isWideScreen) {
        mutableIntStateOf(if (isWideScreen) (displayedCues.firstOrNull()?.id ?: 1) else -1)
    }
    val selectedCue = remember(displayedCues, allCues, selectedCueId, isWideScreen) {
        if (!isWideScreen && selectedCueId == -1) {
            null
        } else {
            displayedCues.find { it.id == selectedCueId }
                ?: allCues.find { it.id == selectedCueId }
                ?: displayedCues.firstOrNull()
        }
    }

    var showAiProviderSheet by rememberSaveable { mutableStateOf(false) }
    var showExtractionRulesSheet by rememberSaveable { mutableStateOf(false) }
    var showPromptSheet by rememberSaveable { mutableStateOf(false) }
    var showJumpLineDialog by rememberSaveable { mutableStateOf(false) }
    var showAiDebugLogDialog by rememberSaveable { mutableStateOf(false) }
    var jumpLineInput by rememberSaveable { mutableStateOf("") }

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

    // Helper to jump to any cue ID (e.g. line 300) and scroll the LazyColumn directly to it
    val jumpToCueId: (Int) -> Unit = { targetId ->
        val clampedId = targetId.coerceIn(
            allCues.firstOrNull()?.id ?: 1,
            allCues.lastOrNull()?.id ?: 1
        )
        selectedCueId = clampedId
        val matchingBatchIndex = uiState.batches.indexOfFirst { batch ->
            batch.any { it.id == clampedId }
        }
        if (matchingBatchIndex >= 0 && matchingBatchIndex != uiState.selectedBatchIndex) {
            viewModel.selectBatchIndex(matchingBatchIndex)
        }
        val targetList = if (showAllCuesInList || uiState.batches.size <= 1) {
            allCues
        } else {
            uiState.batches.getOrNull(matchingBatchIndex.coerceAtLeast(0)) ?: activeBatch
        }
        val targetIndex = targetList.indexOfFirst { it.id == clampedId }
        if (targetIndex >= 0) {
            coroutineScope.launch {
                listState.animateScrollToItem(targetIndex)
            }
        }
    }

    if (showJumpLineDialog) {
        val maxCueId = allCues.lastOrNull()?.id ?: allCues.size.coerceAtLeast(1)
        AlertDialog(
            onDismissRequest = { showJumpLineDialog = false },
            title = {
                Text(
                    text = "Lompat ke Baris & Filter Batch",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Masukkan nomor baris (1 – $maxCueId) untuk langsung menuju dan mengedit baris tersebut di tempat:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = jumpLineInput,
                        onValueChange = { input ->
                            jumpLineInput = input.filter { it.isDigit() }
                        },
                        label = { Text("Nomor Baris (misal: 300)") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("jump_to_line_input")
                    )

                    if (uiState.batches.size > 1) {
                        HorizontalDivider()
                        Text(
                            text = "Filter Tampilan Batch:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = showAllCuesInList,
                                onClick = { showAllCuesInList = true },
                                label = { Text("Semua (${uiState.document.cues.size})") },
                                modifier = Modifier.testTag("batch_chip_all")
                            )
                            uiState.batches.forEachIndexed { index, batchList ->
                                val bFirst = batchList.firstOrNull()?.id ?: 1
                                val bLast = batchList.lastOrNull()?.id ?: batchList.size
                                FilterChip(
                                    selected = !showAllCuesInList && uiState.selectedBatchIndex == index,
                                    onClick = {
                                        showAllCuesInList = false
                                        viewModel.selectBatchIndex(index)
                                    },
                                    label = { Text("Batch ${index + 1} (#$bFirst–#$bLast)") },
                                    modifier = Modifier.testTag("batch_chip_$index")
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val parsed = jumpLineInput.toIntOrNull()
                        if (parsed != null) {
                            jumpToCueId(parsed)
                        }
                        showJumpLineDialog = false
                    },
                    modifier = Modifier.testTag("confirm_jump_to_line_button")
                ) {
                    Text("Lompat & Edit")
                }
            },
            dismissButton = {
                TextButton(onClick = { showJumpLineDialog = false }) {
                    Text("Tutup")
                }
            }
        )
    }

    // Full-Screen 1: AI Provider (Screen 2A of Reference Image)
    if (showAiProviderSheet) {
        Dialog(
            onDismissRequest = { showAiProviderSheet = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(DeepForestHeaderBg)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .padding(horizontal = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { showAiProviderSheet = false }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Kembali",
                                tint = Color.White
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "AI Provider",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    IconButton(onClick = { showAiProviderSheet = false }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Menu",
                            tint = Color.White
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                    color = if (isDark) MaterialTheme.colorScheme.background else AlabasterSheetBg,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 14.dp)
                            .padding(bottom = 28.dp)
                    ) {
                        AiProviderDetailScreenContent(
                            uiState = uiState,
                            aiSettings = aiSettings,
                            glossaryRules = glossaryRules,
                            themeSettings = themeSettings,
                            viewModel = viewModel
                        )
                    }
                }
            }
        }
    }

    // Full-Screen 2: Aturan Ekstraksi (Screen 1A of Reference Image)
    if (showExtractionRulesSheet) {
        Dialog(
            onDismissRequest = { showExtractionRulesSheet = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            ExtractionRulesFullScreen(
                uiState = uiState,
                themeSettings = themeSettings,
                activeGlossaryCount = activeGlossaryCount,
                previewExtractedLines = previewExtractedLines,
                viewModel = viewModel,
                onBack = { showExtractionRulesSheet = false },
                onSwitchToPromptScreen = {
                    showExtractionRulesSheet = false
                    showPromptSheet = true
                }
            )
        }
    }

    // Full-Screen 3: Prompt Terjemahan (Screen 1B of Reference Image)
    if (showPromptSheet) {
        Dialog(
            onDismissRequest = { showPromptSheet = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            TranslationPromptFullScreen(
                uiState = uiState,
                themeSettings = themeSettings,
                activeGlossaryCount = activeGlossaryCount,
                viewModel = viewModel,
                onBack = { showPromptSheet = false },
                onSwitchToExtractionScreen = {
                    showPromptSheet = false
                    showExtractionRulesSheet = true
                }
            )
        }
    }

    if (showAiDebugLogDialog) {
        AiRawResponseDebugLogDialog(
            uiState = uiState,
            viewModel = viewModel,
            onDismiss = { showAiDebugLogDialog = false }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Screen 4 Locked Top Controls:
        // 1. White File Info Card (Folder Icon + Filename + "120 dialog" + Chevron ›)
        // 2. Two Equal-Width Selector Pills ([ 🌐 ID (Indonesia) ▾ ]  [ ✦ Gemini (Web) ▾ ])
        // 3. Full-Width Forest Green Pill Button ([ ✦ Terjemahkan AI ])
        ExtractWorkspaceTopBar(
            uiState = uiState,
            aiSettings = aiSettings,
            showAllCuesInList = showAllCuesInList,
            isDark = isDark,
            viewModel = viewModel,
            onOpenFilePicker = onOpenFilePicker,
            onOpenAiSettingsSheet = { showAiProviderSheet = true },
            onOpenDebugLogDialog = { showAiDebugLogDialog = true },
            onToggleShowAllCues = { showAllCuesInList = it }
        )

        if (isWideScreen) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Card(
                    modifier = Modifier
                        .weight(0.46f)
                        .fillMaxHeight(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    border = BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
                    )
                ) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(
                            items = displayedCues,
                            key = { cue -> "extract_cue_${cue.id}" }
                        ) { cue ->
                            ExtractCueListItemCard(
                                cue = cue,
                                isSelected = selectedCue?.id == cue.id,
                                isTranslated = uiState.parsedTranslations.containsKey(cue.id),
                                isTranslating = cue.id in uiState.translatingCueIds,
                                translatedPreview = uiState.parsedTranslations[cue.id],
                                isDark = isDark,
                                onClick = { selectedCueId = cue.id }
                            )
                        }
                    }
                }

                ActiveCueTranslationInspectorCard(
                    selectedCue = selectedCue,
                    uiState = uiState,
                    aiSettings = aiSettings,
                    activeGlossaryCount = activeGlossaryCount,
                    isDark = isDark,
                    viewModel = viewModel,
                    onExpandRawSheet = { showExtractionRulesSheet = true },
                    onStepCue = { delta ->
                        val currentIdx = displayedCues.indexOfFirst { it.id == selectedCue?.id }
                        if (currentIdx >= 0) {
                            val nextCue = displayedCues.getOrNull(currentIdx + delta)
                            if (nextCue != null) {
                                jumpToCueId(nextCue.id)
                            }
                        }
                    },
                    modifier = Modifier
                        .weight(0.54f)
                        .fillMaxHeight()
                )
            }
        } else {
            // Compact Portrait Layout matching Screen 4 of the 10-screen Reference Image:
            // Clean white cue cards (001, 002, 003...) with green circle checkmarks on the right.
            // Tapping any cue card (#001..#300) expands its inline editor in-place so editing line #300
            // never requires scrolling back to line #001!
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 8.dp)
            ) {
                items(
                    items = displayedCues,
                    key = { cue -> "extract_cue_${cue.id}" }
                ) { cue ->
                    val isCueSelected = selectedCue?.id == cue.id
                    ExtractCueListItemCard(
                        cue = cue,
                        isSelected = isCueSelected,
                        isTranslated = uiState.parsedTranslations.containsKey(cue.id),
                        isTranslating = cue.id in uiState.translatingCueIds,
                        translatedPreview = uiState.parsedTranslations[cue.id],
                        isDark = isDark,
                        onClick = {
                            selectedCueId = if (selectedCueId == cue.id) -1 else cue.id
                        },
                        inlineEditorContent = if (isCueSelected) {
                            {
                                ActiveCueTranslationInspectorCard(
                                    selectedCue = cue,
                                    uiState = uiState,
                                    aiSettings = aiSettings,
                                    activeGlossaryCount = activeGlossaryCount,
                                    isDark = isDark,
                                    viewModel = viewModel,
                                    onExpandRawSheet = { showPromptSheet = true },
                                    onStepCue = { delta ->
                                        val currentIdx = displayedCues.indexOfFirst { it.id == cue.id }
                                        if (currentIdx >= 0) {
                                            val nextCue = displayedCues.getOrNull(currentIdx + delta)
                                            if (nextCue != null) {
                                                jumpToCueId(nextCue.id)
                                            }
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 8.dp)
                                )
                            }
                        } else null
                    )
                }
            }
        }

        // Screen 4 Locked Bottom Bar:
        // Left: Two rounded pill icon buttons ([ ≡ Filter / #Baris ] [ ⇄ Aturan & Salin ])
        // Right: Soft green pill badge "120/120"
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Button 1: Filter / Jump to Line #
                Surface(
                    shape = RoundedCornerShape(21.dp),
                    color = if (isDark) Color(0xFF1F3027) else Color(0xFFE6ECE7),
                    border = BorderStroke(
                        1.dp,
                        if (isDark) Color(0xFF2D4438) else Color(0xFFD5DFD7)
                    ),
                    modifier = Modifier
                        .height(42.dp)
                        .width(54.dp)
                        .clip(RoundedCornerShape(21.dp))
                        .clickable {
                            jumpLineInput = (selectedCue?.id ?: 1).toString()
                            showJumpLineDialog = true
                        }
                        .testTag("jump_to_line_pill_button")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = "Lompat ke Baris & Filter",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Button 2: Swap / Extraction Rules & Prompt Sheet
                Surface(
                    shape = RoundedCornerShape(21.dp),
                    color = if (isDark) Color(0xFF1F3027) else Color(0xFFE6ECE7),
                    border = BorderStroke(
                        1.dp,
                        if (isDark) Color(0xFF2D4438) else Color(0xFFD5DFD7)
                    ),
                    modifier = Modifier
                        .height(42.dp)
                        .width(54.dp)
                        .clip(RoundedCornerShape(21.dp))
                        .clickable { showExtractionRulesSheet = true }
                        .testTag("open_extraction_rules_sheet_button")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.SwapHoriz,
                            contentDescription = "Aturan Ekstraksi & Prompt AI",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Right: Soft Sage Green Counter Pill ("120/120")
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = if (isDark) Color(0xFF214431) else SageHighlightFill
            ) {
                Text(
                    text = "${uiState.matchedCount}/${uiState.document.cues.size}",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) Color(0xFF8CE0AF) else Color(0xFF173F2A),
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ExtractWorkspaceTopBar(
    uiState: SubtitleUiState,
    aiSettings: AiProviderSettings,
    showAllCuesInList: Boolean,
    isDark: Boolean,
    viewModel: SubtitleViewModel,
    onOpenFilePicker: () -> Unit,
    onOpenAiSettingsSheet: () -> Unit,
    onOpenDebugLogDialog: () -> Unit,
    onToggleShowAllCues: (Boolean) -> Unit
) {
    var showLanguageMenu by remember { mutableStateOf(false) }
    val allBatches = uiState.batches

    val shortLangLabel = when (uiState.targetLanguageOption) {
        TargetLanguageOption.INDONESIAN -> "ID (Indonesia)"
        TargetLanguageOption.ENGLISH -> "EN (English)"
        TargetLanguageOption.MALAY -> "MY (Melayu)"
        TargetLanguageOption.JAPANESE -> "JP (Jepang)"
        TargetLanguageOption.CUSTOM -> uiState.customTargetLanguage.ifBlank { "Kustom" }
    }

    val shortAiLabel = when (aiSettings.preset) {
        AiProviderPreset.GEMINI_WEB_COOKIE -> "Gemini (Web)"
        AiProviderPreset.GEMINI_WEB_SERVER -> "Gemini (Server)"
        else -> aiSettings.preset.label
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("compact_workspace_control_header"),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // 1. White File Info Card matching Screen 4 of Reference Image
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = if (isDark) MaterialTheme.colorScheme.surface else Color.White,
            border = BorderStroke(
                1.dp,
                if (isDark) MaterialTheme.colorScheme.outlineVariant else Color(0xFFE3E9E4)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .clickable { onOpenFilePicker() }
                .testTag("open_subtitle_file_button")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isDark) Color(0xFF1E3228) else Color(0xFFECF1ED),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Outlined.Folder,
                                contentDescription = "Buka File Subtitle",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

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
                            text = "${uiState.document.cues.size} dialog",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // 2. Two Equal-Width Selector Pills: [ 🌐 ID (Indonesia) ▾ ]  [ ✦ Gemini (Web) ▾ ]
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.weight(1f)) {
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = if (isDark) MaterialTheme.colorScheme.surface else Color.White,
                    border = BorderStroke(
                        1.dp,
                        if (isDark) MaterialTheme.colorScheme.outlineVariant else Color(0xFFE0E7E2)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .clickable { showLanguageMenu = true }
                        .testTag("target_language_pill_button")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Language,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = shortLangLabel,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                DropdownMenu(
                    expanded = showLanguageMenu,
                    onDismissRequest = { showLanguageMenu = false }
                ) {
                    TargetLanguageOption.entries.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option.label) },
                            onClick = {
                                viewModel.setTargetLanguageOption(option)
                                showLanguageMenu = false
                            }
                        )
                    }
                }
            }

            Surface(
                shape = RoundedCornerShape(22.dp),
                color = if (isDark) MaterialTheme.colorScheme.surface else Color.White,
                border = BorderStroke(
                    1.dp,
                    if (isDark) MaterialTheme.colorScheme.outlineVariant else Color(0xFFE0E7E2)
                ),
                modifier = Modifier
                    .weight(1f)
                    .height(42.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .clickable { onOpenAiSettingsSheet() }
                    .testTag("open_ai_settings_sheet_button")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = Color(0xFF5D5FEF)
                        )
                        Text(
                            text = shortAiLabel,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // 3. Full-Width Forest Green Pill Button: [ ✦ Terjemahkan AI ] matching Screen 4
        if (uiState.isTranslatingAi) {
            Button(
                onClick = { viewModel.cancelAiTranslation() },
                shape = RoundedCornerShape(25.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("cancel_ai_translate_button")
            ) {
                Icon(
                    imageVector = Icons.Default.StopCircle,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Hentikan Terjemahan AI",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        } else {
            Button(
                onClick = {
                    if (allBatches.size > 1 && showAllCuesInList) {
                        viewModel.translateAllBatchesWithAi()
                    } else {
                        viewModel.translateCurrentBatchWithAi()
                    }
                },
                shape = RoundedCornerShape(25.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isDark) MaterialTheme.colorScheme.primary else PrimaryForestButton,
                    contentColor = if (isDark) MaterialTheme.colorScheme.onPrimary else Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("top_translate_ai_button")
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Terjemahkan AI",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Live AI Translation Progress Banner (only visible when active or status exists)
        if (uiState.isTranslatingAi || !uiState.aiTranslationStatusText.isNullOrBlank()) {
            val isErrorStatus = uiState.aiTranslationStatusText?.contains("gagal", ignoreCase = true) == true
            val bannerBg = if (isErrorStatus) {
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.72f)
            } else {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
            }
            val bannerFg = if (isErrorStatus) {
                MaterialTheme.colorScheme.onErrorContainer
            } else {
                MaterialTheme.colorScheme.onPrimaryContainer
            }

            Surface(
                color = bannerBg,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = uiState.aiTranslationStatusText ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = bannerFg,
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis,
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
                    if (!uiState.isTranslatingAi &&
                        (uiState.aiDebugLogs.isNotEmpty() || !uiState.lastAiRawResponsePreview.isNullOrBlank() || isErrorStatus)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { onOpenDebugLogDialog() }
                                    .testTag("open_ai_debug_log_dialog_button")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Description,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "Lihat Respons Mentah & Log Debug",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ExtractCueListItemCard(
    cue: SubtitleCue,
    isSelected: Boolean,
    isTranslated: Boolean,
    isTranslating: Boolean,
    translatedPreview: String?,
    isDark: Boolean,
    onClick: () -> Unit,
    inlineEditorContent: (@Composable () -> Unit)? = null
) {
    val bgColor = when {
        isSelected -> if (isDark) Color(0xFF214431) else SageHighlightFill
        isTranslating -> MaterialTheme.colorScheme.secondaryContainer
        else -> if (isDark) MaterialTheme.colorScheme.surface else Color.White
    }

    val indexBadgeBg = if (isSelected) {
        if (isDark) Color(0xFF142A1E) else Color.White.copy(alpha = 0.85f)
    } else {
        if (isDark) Color(0xFF1E2D24) else Color(0xFFEAEFEA)
    }

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = bgColor,
        border = BorderStroke(
            width = if (isSelected) 1.2.dp else 1.dp,
            color = if (isSelected) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
            } else if (isDark) {
                MaterialTheme.colorScheme.outlineVariant
            } else {
                Color(0xFFE4EAE5)
            }
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .testTag("realtime_cue_card_${cue.id}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Left: 3-digit zero-padded index badge (001, 002, 003...)
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = indexBadgeBg
                ) {
                    Text(
                        text = String.format(Locale.US, "%03d", cue.id),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 11.dp, vertical = 8.dp)
                    )
                }

                // Middle: Timestamp + Dialogue Text
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(
                        text = "${cue.startTime} → ${cue.endTime}",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    val displayLine = cue.cleanText.replace("\\N", " ").replace("\n", " ")
                    Text(
                        text = displayLine,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = if (isSelected) 2 else 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (isSelected && !translatedPreview.isNullOrBlank()) {
                        Text(
                            text = translatedPreview.replace("\\N", " "),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Right: Solid Green Circle Checkmark Badge matching Screen 4 of Reference Image
                if (isTranslated) {
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(EmeraldCheckGreen),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Sudah Diterjemahkan",
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(
                                if (isDark) Color(0xFF25352C) else Color(0xFFE8EFE9)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Belum Diterjemahkan",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }

            // Inline Expandable Editor right inside the selected cue card on compact screens!
            if (inlineEditorContent != null) {
                inlineEditorContent()
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ActiveCueTranslationInspectorCard(
    selectedCue: SubtitleCue?,
    uiState: SubtitleUiState,
    aiSettings: AiProviderSettings,
    activeGlossaryCount: Int,
    isDark: Boolean,
    viewModel: SubtitleViewModel,
    onExpandRawSheet: () -> Unit,
    onStepCue: ((Int) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    // 0 = Teks Asli, 1 = Terjemahan (Default selected per reference design)
    var inspectorTab by rememberSaveable { mutableIntStateOf(1) }

    val cueId = selectedCue?.id ?: 1
    val currentTranslated = uiState.parsedTranslations[cueId].orEmpty()
    var draftText by remember(cueId, currentTranslated) {
        mutableStateOf(currentTranslated)
    }

    LaunchedEffect(currentTranslated) {
        draftText = currentTranslated
    }

    Card(
        modifier = modifier.testTag("active_cue_inspector_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Top Underline Tab Bar: Teks Asli | Terjemahan + Prev/Next Line + Expand button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    InspectorUnderlineTab(
                        title = "Teks Asli",
                        selected = inspectorTab == 0,
                        onClick = { inspectorTab = 0 },
                        modifier = Modifier.weight(1f)
                    )
                    InspectorUnderlineTab(
                        title = "Terjemahan",
                        selected = inspectorTab == 1,
                        onClick = { inspectorTab = 1 },
                        modifier = Modifier.weight(1f)
                    )
                }

                if (onStepCue != null) {
                    IconButton(
                        onClick = { onStepCue(-1) },
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowUp,
                            contentDescription = "Baris Sebelumnya",
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = { onStepCue(1) },
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Baris Berikutnya",
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(
                    onClick = onExpandRawSheet,
                    modifier = Modifier.size(30.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = "Lihat Pratinjau Batch & Prompt",
                        modifier = Modifier.size(17.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))

            if (selectedCue != null) {
                // Original Dialogue Header with Line Number Badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = selectedCue.cleanText.replace("\\N", "\n"),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "#${String.format(Locale.US, "%03d", selectedCue.id)}",
                        style = MaterialTheme.typography.labelMedium,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                if (inspectorTab == 0) {
                    // Teks Asli Metadata & Raw Tag View
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (isDark) Color(0xFF1B2E24) else SageSoftCardFill,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Baris #${String.format(Locale.US, "%03d", selectedCue.id)}  •  ${selectedCue.startTime} → ${selectedCue.endTime}",
                                style = MaterialTheme.typography.labelMedium,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = selectedCue.rawText,
                                style = MaterialTheme.typography.bodyMedium,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                } else {
                    // Soft Sage-Green Editable Translation Box (matching Panel 2 of mockup)
                    OutlinedTextField(
                        value = draftText,
                        onValueChange = { newValue ->
                            draftText = newValue
                            viewModel.updateSingleCueTranslation(selectedCue.id, newValue)
                        },
                        placeholder = {
                            Text(
                                text = "Ketik terjemahan untuk baris #${String.format(Locale.US, "%03d", selectedCue.id)} atau klik 'Terjemahkan AI'...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        },
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = if (isDark) Color(0xFF1B2E24) else SageSoftCardFill,
                            unfocusedContainerColor = if (isDark) Color(0xFF1B2E24) else SageSoftCardFill,
                            focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                            unfocusedBorderColor = Color.Transparent
                        ),
                        minLines = 2,
                        maxLines = 5,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("inspector_translation_input")
                    )
                }
            }

            // Bottom Action Row inside Inspector: [Edit] [Copy] [Check]  ...  [✨ Terjemahkan AI]
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedIconButton(
                        onClick = { inspectorTab = 1 },
                        shape = CircleShape,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Baris",
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    OutlinedIconButton(
                        onClick = {
                            val fullText = viewModel.buildCurrentBatchOutput(withPromptHeader = true)
                            copyToClipboard(context, "SubAI Prompt + Batch", fullText)
                            val glossaryNote = if (uiState.includeGlossaryInPrompt && activeGlossaryCount > 0) {
                                " + $activeGlossaryCount glosarium"
                            } else {
                                ""
                            }
                            viewModel.showBannerMessage(
                                "Disalin ${uiState.activeBatchCues.size} baris + Prompt (${aiSettings.selectedModel})$glossaryNote!"
                            )
                        },
                        shape = CircleShape,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier
                            .size(40.dp)
                            .testTag("copy_with_prompt_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy Prompt + Batch",
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    OutlinedIconButton(
                        onClick = {
                            if (selectedCue != null) {
                                viewModel.updateSingleCueTranslation(selectedCue.id, draftText)
                                viewModel.showBannerMessage(
                                    "Baris #${String.format(Locale.US, "%03d", selectedCue.id)} tersimpan."
                                )
                            }
                        },
                        shape = CircleShape,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircleOutline,
                            contentDescription = "Simpan Baris",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                if (uiState.isTranslatingAi) {
                    Button(
                        onClick = { viewModel.cancelAiTranslation() },
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        ),
                        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp),
                        modifier = Modifier.testTag("stop_ai_translation_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.StopCircle,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Hentikan",
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                } else {
                    Button(
                        onClick = { viewModel.translateCurrentBatchWithAi() },
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isDark) MaterialTheme.colorScheme.primary else ForestGreenPrimary,
                            contentColor = if (isDark) MaterialTheme.colorScheme.onPrimary else Color.White
                        ),
                        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp),
                        modifier = Modifier.testTag("translate_active_batch_ai_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Terjemahkan AI",
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InspectorUnderlineTab(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(top = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Box(
            modifier = Modifier
                .fillMaxWidth(0.75f)
                .height(3.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(
                    if (selected) MaterialTheme.colorScheme.primary else Color.Transparent
                )
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

@Composable
internal fun AiRawResponseDebugLogDialog(
    uiState: SubtitleUiState,
    viewModel: SubtitleViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val logs = uiState.aiDebugLogs

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Log Debug & Respons Mentah AI",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 460.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (logs.isEmpty()) {
                    Text(
                        text = uiState.lastAiRawResponsePreview
                            ?: "Belum ada respons mentah AI yang tercatat pada sesi ini. Jalankan 'Terjemahkan AI' untuk merekam isi respons asli dari provider.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.testTag("ai_debug_log_empty_text")
                    )
                } else {
                    logs.forEach { entry ->
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Batch ${entry.batchNumber} (${entry.cueRangeLabel})",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${entry.mappedCount}/${entry.expectedCount} baris",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (entry.mappedCount > 0) EmeraldCheckGreen else MaterialTheme.colorScheme.error
                                    )
                                }

                                Text(
                                    text = "Provider: ${entry.providerLabel} • Model: ${entry.modelId}\nStrategi: ${entry.strategyUsed}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = entry.rawResponse.ifBlank { "(Respons kosong)" },
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 11.5.sp
                                        ),
                                        modifier = Modifier
                                            .padding(10.dp)
                                            .testTag("ai_debug_raw_response_batch_${entry.batchNumber}")
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            copyToClipboard(
                                                context,
                                                "Raw AI Batch ${entry.batchNumber}",
                                                entry.rawResponse
                                            )
                                            viewModel.showBannerMessage("Respons mentah Batch ${entry.batchNumber} disalin ke Clipboard!")
                                        },
                                        shape = RoundedCornerShape(10.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Salin Log", style = MaterialTheme.typography.labelSmall)
                                    }

                                    if (entry.rawResponse.isNotBlank()) {
                                        Button(
                                            onClick = {
                                                viewModel.applyRawDebugResponseToPasteEditor(entry.rawResponse)
                                                onDismiss()
                                            },
                                            shape = RoundedCornerShape(10.dp),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = PrimaryForestButton,
                                                contentColor = Color.White
                                            ),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("Peta ke Paste AI", style = MaterialTheme.typography.labelSmall)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        dismissButton = {
            if (logs.isNotEmpty()) {
                TextButton(onClick = { viewModel.clearAiDebugLogs() }) {
                    Text("Bersihkan Log")
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Tutup")
            }
        }
    )
}

