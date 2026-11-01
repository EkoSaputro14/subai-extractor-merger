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
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Numbers
import androidx.compose.material.icons.filled.PlayCircleFilled
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.StopCircle
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
import com.example.ui.theme.ForestGreenPrimary
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

    var selectedCueId by rememberSaveable(uiState.document.fileName) {
        mutableIntStateOf(displayedCues.firstOrNull()?.id ?: 1)
    }
    val selectedCue = remember(displayedCues, allCues, selectedCueId) {
        displayedCues.find { it.id == selectedCueId }
            ?: allCues.find { it.id == selectedCueId }
            ?: displayedCues.firstOrNull()
    }

    var showAiProviderSheet by rememberSaveable { mutableStateOf(false) }
    var showExtractionRulesSheet by rememberSaveable { mutableStateOf(false) }
    var showJumpLineDialog by rememberSaveable { mutableStateOf(false) }
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
        // Also sync batch index if batch contains this cue
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
                    text = "Lompat ke Baris Subtitle",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Masukkan nomor baris (1 – $maxCueId) untuk langsung menuju dan mengedit baris tersebut tanpa perlu scroll manual:",
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
                    Text("Batal")
                }
            }
        )
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

    Column(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Sticky Top Workspace Control Header (Fixed outside LazyColumn — never scrolls away!)
        ExtractWorkspaceTopBar(
            uiState = uiState,
            aiSettings = aiSettings,
            selectedCueId = selectedCue?.id ?: 1,
            showAllCuesInList = showAllCuesInList,
            isWideScreen = isWideScreen,
            viewModel = viewModel,
            onOpenFilePicker = onOpenFilePicker,
            onOpenAiSettingsSheet = { showAiProviderSheet = true },
            onOpenExtractionRulesSheet = { showExtractionRulesSheet = true },
            onOpenJumpLineDialog = {
                jumpLineInput = (selectedCue?.id ?: 1).toString()
                showJumpLineDialog = true
            },
            onToggleShowAllCues = { showAllCuesInList = it }
        )

        if (isWideScreen) {
            // Wide Screen Split Master-Detail Layout matching Panel 2 of Reference Design
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Left Pane: Scrollable 001..N Cue List
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

                // Right Pane: Stationary Active Cue Inspector & Translation Card
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
            // Compact Portrait Layout:
            // Each selected subtitle cue card (#001, #050, #300...) expands INLINE right in place!
            // Users NEVER have to scroll back to line #001 to edit line #300!
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
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
                        onClick = { selectedCueId = cue.id },
                        inlineEditorContent = if (isCueSelected) {
                            {
                                ActiveCueTranslationInspectorCard(
                                    selectedCue = cue,
                                    uiState = uiState,
                                    aiSettings = aiSettings,
                                    activeGlossaryCount = activeGlossaryCount,
                                    isDark = isDark,
                                    viewModel = viewModel,
                                    onExpandRawSheet = { showExtractionRulesSheet = true },
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
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ExtractWorkspaceTopBar(
    uiState: SubtitleUiState,
    aiSettings: AiProviderSettings,
    selectedCueId: Int,
    showAllCuesInList: Boolean,
    isWideScreen: Boolean,
    viewModel: SubtitleViewModel,
    onOpenFilePicker: () -> Unit,
    onOpenAiSettingsSheet: () -> Unit,
    onOpenExtractionRulesSheet: () -> Unit,
    onOpenJumpLineDialog: () -> Unit,
    onToggleShowAllCues: (Boolean) -> Unit
) {
    var showLanguageMenu by remember { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }
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

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("compact_workspace_control_header"),
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
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (isWideScreen) {
                // Only show large title & file chip inside workspace card on wide screens
                // (On compact screens, MainActivity TopAppBar already displays the screen title & active file)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Ekstrak & Terjemahkan",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onOpenFilePicker() }
                            .testTag("open_subtitle_file_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Folder,
                                contentDescription = "Buka File",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = uiState.document.fileName,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "· ${uiState.document.cues.size} dialog",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Responsive Control Pills Row: Language Pill + AI Provider Pill + Jump to #Line Pill + More Menu
            // Never pushed off-screen by long filenames!
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .horizontalScroll(rememberScrollState()),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (!isWideScreen) {
                        // Compact File Picker Pill with constrained width so it never pushes other pills off-screen
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .clickable { onOpenFilePicker() }
                                .testTag("open_subtitle_file_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Folder,
                                    contentDescription = "Buka File",
                                    modifier = Modifier.size(15.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "${uiState.document.cues.size} dlg",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    // Target Language Dropdown Pill
                    Box {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .clickable { showLanguageMenu = true }
                                .testTag("target_language_pill_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Language,
                                    contentDescription = null,
                                    modifier = Modifier.size(15.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = shortLangLabel,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowDown,
                                    contentDescription = null,
                                    modifier = Modifier.size(15.dp)
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

                    // AI Provider & Cookie Pill
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .clickable { onOpenAiSettingsSheet() }
                            .testTag("open_ai_settings_sheet_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                modifier = Modifier.size(15.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = shortAiLabel,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }

                    // Quick Jump to Line # Pill (e.g. jump directly to #300!)
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .clickable { onOpenJumpLineDialog() }
                            .testTag("jump_to_line_pill_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Numbers,
                                contentDescription = "Lompat ke Baris",
                                modifier = Modifier.size(15.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Baris #${String.format(Locale.US, "%03d", selectedCueId)}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                // 3-Dots Overflow Button (Samples, Extraction Rules, Translate All Batches)
                Box {
                    IconButton(
                        onClick = { showMoreMenu = true },
                        modifier = Modifier
                            .size(34.dp)
                            .testTag("open_extraction_rules_sheet_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Opsi Ekstraksi & Sampel"
                        )
                    }

                    DropdownMenu(
                        expanded = showMoreMenu,
                        onDismissRequest = { showMoreMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Lompat ke Nomor Baris...") },
                            leadingIcon = { Icon(Icons.Default.Numbers, contentDescription = null) },
                            onClick = {
                                showMoreMenu = false
                                onOpenJumpLineDialog()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Aturan Ekstraksi & Prompt AI") },
                            leadingIcon = { Icon(Icons.Default.Security, contentDescription = null) },
                            onClick = {
                                showMoreMenu = false
                                onOpenExtractionRulesSheet()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Konfigurasi AI Provider & Cookie") },
                            leadingIcon = { Icon(Icons.Default.AutoAwesome, contentDescription = null) },
                            onClick = {
                                showMoreMenu = false
                                onOpenAiSettingsSheet()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Muat Sampel .ASS Anime") },
                            leadingIcon = { Icon(Icons.Default.AutoAwesome, contentDescription = null) },
                            onClick = {
                                showMoreMenu = false
                                viewModel.loadSampleAss()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Muat Sampel .SRT Film") },
                            leadingIcon = { Icon(Icons.Default.Movie, contentDescription = null) },
                            onClick = {
                                showMoreMenu = false
                                viewModel.loadSampleSrt()
                            }
                        )
                        if (allBatches.size > 1) {
                            DropdownMenuItem(
                                text = { Text("Terjemahkan Semua Batch (${allBatches.size})") },
                                leadingIcon = { Icon(Icons.Default.PlayCircleFilled, contentDescription = null) },
                                onClick = {
                                    showMoreMenu = false
                                    viewModel.translateAllBatchesWithAi()
                                }
                            )
                        }
                    }
                }
            }

            // Batch Selector Chips (when file has >1 batch, plus "Semua Baris" option)
            if (allBatches.size > 1) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = showAllCuesInList,
                        onClick = { onToggleShowAllCues(true) },
                        label = { Text("Semua (${uiState.document.cues.size})") },
                        modifier = Modifier.testTag("batch_chip_all")
                    )
                    allBatches.forEachIndexed { index, batchList ->
                        val bFirst = batchList.firstOrNull()?.id ?: 1
                        val bLast = batchList.lastOrNull()?.id ?: batchList.size
                        FilterChip(
                            selected = !showAllCuesInList && uiState.selectedBatchIndex == index,
                            onClick = {
                                onToggleShowAllCues(false)
                                viewModel.selectBatchIndex(index)
                            },
                            label = { Text("Batch ${index + 1} (#$bFirst–#$bLast)") },
                            modifier = Modifier.testTag("batch_chip_$index")
                        )
                    }
                }
            }

            // Live AI Translation Progress Banner
            if (uiState.isTranslatingAi || !uiState.aiTranslationStatusText.isNullOrBlank()) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = uiState.aiTranslationStatusText ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            maxLines = 2,
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
        else -> if (isDark) Color(0xFF192620) else Color(0xFFF2F5F3)
    }

    val indexBadgeBg = if (isSelected) {
        if (isDark) Color(0xFF142A1E) else Color.White.copy(alpha = 0.78f)
    } else {
        if (isDark) Color(0xFF121C17) else Color(0xFFE5EBE7)
    }

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = bgColor,
        border = if (isSelected) {
            BorderStroke(1.2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
        } else null,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .testTag("realtime_cue_card_${cue.id}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 3-digit zero-padded index badge (001, 002, 003...)
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = indexBadgeBg
                ) {
                    Text(
                        text = String.format(Locale.US, "%03d", cue.id),
                        style = MaterialTheme.typography.labelLarge,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${cue.startTime} → ${cue.endTime}",
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (isTranslated) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary)
                            )
                        }
                    }

                    val displayLine = cue.cleanText.replace("\\N", " ").replace("\n", " ")
                    Text(
                        text = displayLine,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = if (isSelected) 2 else 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (!translatedPreview.isNullOrBlank()) {
                        Text(
                            text = translatedPreview.replace("\\N", " "),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = if (isSelected) 2 else 1,
                            overflow = TextOverflow.Ellipsis
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
