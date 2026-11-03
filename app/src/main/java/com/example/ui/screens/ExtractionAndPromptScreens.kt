package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Dataset
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Eco
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.ShortText
import androidx.compose.material.icons.outlined.Tv
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.BatchSizeOption
import com.example.model.ExtractionRulePreset
import com.example.model.IndexFormatMode
import com.example.model.LineSeparatorMode
import com.example.model.TargetLanguageOption
import com.example.model.ThemeSettings
import com.example.model.TranslationStylePreset
import com.example.ui.theme.AlabasterSheetBg
import com.example.ui.theme.DeepForestHeaderBg
import com.example.ui.theme.EmeraldCheckGreen
import com.example.ui.theme.PrimaryForestButton
import com.example.ui.theme.SageHighlightFill
import com.example.ui.theme.subtitleEditorTextStyle
import com.example.viewmodel.SubtitleUiState
import com.example.viewmodel.SubtitleViewModel

/**
 * SCREEN 1A (Left of Image 1): "Aturan Ekstraksi"
 * Reproduces the locked reference design 100%:
 * - Deep Forest Green Top Bar: ← Aturan Ekstraksi ⋮
 * - Hero Banner with glowing forest wave background + 3D tilted document & green `{...}` pill badge
 * - Rounded Alabaster Sheet (#F3F6F3, topStart/topEnd = 28.dp)
 * - 2-Tab Bar: [ Preset | </> Kustom ] with green bottom underline on active tab
 * - 2x2 Preset Cards: Standar (selected with mint fill, green border & top-right ✓ badge), Anime, Bioskop, Minimal
 * - Card "Opsi Ekstraksi" with 5 green switches
 * - Card "Preview Hasil Ekstraksi" with [1] and [2] lines
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExtractionRulesFullScreen(
    uiState: SubtitleUiState,
    themeSettings: ThemeSettings,
    activeGlossaryCount: Int,
    previewExtractedLines: String,
    viewModel: SubtitleViewModel,
    onBack: () -> Unit,
    onSwitchToPromptScreen: () -> Unit
) {
    val context = LocalContext.current
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    // 0 = Preset, 1 = Kustom
    var activeSubTab by rememberSaveable { mutableIntStateOf(0) }
    var menuExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepForestHeaderBg)
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        // Top Header Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("extraction_rules_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Kembali",
                        tint = Color.White
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Aturan Ekstraksi",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Opsi Lainnya",
                        tint = Color.White
                    )
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Buka Prompt Terjemahan") },
                        leadingIcon = { Icon(Icons.Default.AutoAwesome, contentDescription = null) },
                        onClick = {
                            menuExpanded = false
                            onSwitchToPromptScreen()
                        }
                    )
                }
            }
        }

        // Hero Banner inside Deep Forest Header Area
        ExtractionHeroBanner(
            title = "Aturan Ekstraksi",
            subtitle = "Atur bagaimana subtitle diekstrak menjadi teks bersih untuk diterjemahkan.",
            badgeText = "{...}",
            badgeBgColor = Color(0xFF237A4B),
            badgeTextColor = Color.White,
            leadingIcon = Icons.Outlined.AutoAwesome,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 14.dp)
        )

        // Rounded-Top Alabaster Sheet
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
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 16.dp)
                    .padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 2-Tab Segmented Bar with Green Underline: [ 🥞 Preset | </> Kustom ]
                ReferenceTwoTabHeader(
                    leftTitle = "Preset",
                    leftIcon = Icons.Outlined.Layers,
                    rightTitle = "Kustom",
                    rightIcon = Icons.Default.Code,
                    selectedTab = activeSubTab,
                    isDark = isDark,
                    onSelectTab = { activeSubTab = it }
                )

                if (activeSubTab == 0) {
                    // 2x2 Preset Grid matching Screen 1A of Reference Image
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            ExtractionPresetGridCard(
                                preset = ExtractionRulePreset.STANDAR,
                                icon = Icons.Outlined.Eco,
                                selected = uiState.extractionPreset == ExtractionRulePreset.STANDAR,
                                isDark = isDark,
                                onClick = { viewModel.selectExtractionPreset(ExtractionRulePreset.STANDAR) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("extraction_preset_standar")
                            )
                            ExtractionPresetGridCard(
                                preset = ExtractionRulePreset.ANIME,
                                icon = Icons.Outlined.Tv,
                                selected = uiState.extractionPreset == ExtractionRulePreset.ANIME,
                                isDark = isDark,
                                onClick = { viewModel.selectExtractionPreset(ExtractionRulePreset.ANIME) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("extraction_preset_anime")
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            ExtractionPresetGridCard(
                                preset = ExtractionRulePreset.BIOSKOP,
                                icon = Icons.Outlined.Movie,
                                selected = uiState.extractionPreset == ExtractionRulePreset.BIOSKOP,
                                isDark = isDark,
                                onClick = { viewModel.selectExtractionPreset(ExtractionRulePreset.BIOSKOP) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("extraction_preset_bioskop")
                            )
                            ExtractionPresetGridCard(
                                preset = ExtractionRulePreset.MINIMAL,
                                icon = Icons.Outlined.ShortText,
                                selected = uiState.extractionPreset == ExtractionRulePreset.MINIMAL,
                                isDark = isDark,
                                onClick = { viewModel.selectExtractionPreset(ExtractionRulePreset.MINIMAL) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("extraction_preset_minimal")
                            )
                        }
                    }
                } else {
                    // Kustom Tab: Separator, Index Format & Batch Size options
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (isDark) MaterialTheme.colorScheme.surface else Color.White,
                        border = BorderStroke(
                            1.dp,
                            if (isDark) MaterialTheme.colorScheme.outlineVariant else Color(0xFFE3E9E4)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "Pemisah Baris Ganda:",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                LineSeparatorMode.entries.forEach { mode ->
                                    FilterChip(
                                        selected = uiState.separatorMode == mode,
                                        onClick = { viewModel.setSeparatorMode(mode) },
                                        label = { Text(mode.label) },
                                        modifier = Modifier.testTag("separator_chip_${mode.name.lowercase()}")
                                    )
                                }
                            }
                            AnimatedVisibility(visible = uiState.separatorMode == LineSeparatorMode.CUSTOM) {
                                OutlinedTextField(
                                    value = uiState.customSeparator,
                                    onValueChange = { viewModel.setCustomSeparator(it) },
                                    label = { Text("Simbol Pemisah Kustom") },
                                    singleLine = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("custom_separator_input")
                                )
                            }

                            Text(
                                text = "Format Penomoran Indeks:",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
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
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
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
                }

                // Card: "Opsi Ekstraksi" with 5 green switches matching Reference Image 1A
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (isDark) MaterialTheme.colorScheme.surface else Color.White,
                    border = BorderStroke(
                        1.dp,
                        if (isDark) MaterialTheme.colorScheme.outlineVariant else Color(0xFFE3E9E4)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 14.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Opsi Ekstraksi",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))

                        ExtractionOptionSwitchRow(
                            icon = Icons.Outlined.AutoAwesome,
                            label = "Pertahankan tag ASS ({\\..})",
                            checked = uiState.protectAssTags,
                            onCheckedChange = { viewModel.setProtectAssTags(it) },
                            testTag = "protect_ass_tags_switch"
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.28f)
                        )
                        ExtractionOptionSwitchRow(
                            icon = Icons.Default.Schedule,
                            label = "Sertakan nama style/aktor",
                            checked = uiState.includeStyleAndActor,
                            onCheckedChange = { viewModel.setIncludeStyleAndActor(it) },
                            testTag = "include_style_actor_switch"
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.28f)
                        )
                        ExtractionOptionSwitchRow(
                            icon = Icons.Default.History,
                            label = "Gabungkan baris dialog berurutan",
                            checked = uiState.mergeConsecutiveDialogues,
                            onCheckedChange = { viewModel.setMergeConsecutiveDialogues(it) },
                            testTag = "merge_consecutive_switch"
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.28f)
                        )
                        ExtractionOptionSwitchRow(
                            icon = Icons.Outlined.Shield,
                            label = "Hapus komentar (Comment)",
                            checked = uiState.removeComments,
                            onCheckedChange = { viewModel.setRemoveComments(it) },
                            testTag = "remove_comments_switch"
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.28f)
                        )
                        ExtractionOptionSwitchRow(
                            icon = Icons.Default.Schedule,
                            label = "Abaikan baris kosong",
                            checked = uiState.ignoreEmptyLines,
                            onCheckedChange = { viewModel.setIgnoreEmptyLines(it) },
                            testTag = "ignore_empty_lines_switch"
                        )
                    }
                }

                // Card: "Preview Hasil Ekstraksi" matching Reference Image 1A
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (isDark) MaterialTheme.colorScheme.surface else Color.White,
                    border = BorderStroke(
                        1.dp,
                        if (isDark) MaterialTheme.colorScheme.outlineVariant else Color(0xFFE3E9E4)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Visibility,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "Preview Hasil Ekstraksi",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Text(
                                text = "Prompt AI ›",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldCheckGreen,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { onSwitchToPromptScreen() }
                                    .padding(horizontal = 6.dp, vertical = 4.dp)
                                    .testTag("switch_to_prompt_screen_button")
                            )
                        }

                        val firstCueText = uiState.document.cues.getOrNull(0)?.cleanText
                            ?.replace("\\N", " ")
                            ?.ifBlank { "Aah.... akhirnya sampai juga..." }
                            ?: "Aah.... akhirnya sampai juga..."
                        val secondCueText = uiState.document.cues.getOrNull(1)?.cleanText
                            ?.replace("\\N", " ")
                            ?.ifBlank { "Jadi ini.... Kota Aqua....?" }
                            ?: "Jadi ini.... Kota Aqua....?"

                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (isDark) Color(0xFF1A2921) else Color(0xFFECEFEC),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = buildAnnotatedString {
                                        withStyle(
                                            SpanStyle(
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.Medium,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        ) {
                                            append("[1]   ")
                                        }
                                        val firstWord = firstCueText.substringBefore(" ")
                                        val restWords = firstCueText.substringAfter(" ", "")
                                        withStyle(
                                            SpanStyle(
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.SemiBold,
                                                color = EmeraldCheckGreen
                                            )
                                        ) {
                                            append(firstWord)
                                        }
                                        if (restWords.isNotEmpty()) {
                                            withStyle(
                                                SpanStyle(
                                                    fontFamily = FontFamily.Monospace,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                            ) {
                                                append(" $restWords")
                                            }
                                        }
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.testTag("extracted_preview_text")
                                )

                                Text(
                                    text = "[2]   $secondCueText",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontFamily = FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        // Copy Actions (preserving existing testTags for automated tests)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    val linesOnly = viewModel.buildCurrentBatchOutput(withPromptHeader = false)
                                    copyToClipboard(context, "SubAI Numbered Lines", linesOnly)
                                    viewModel.showBannerMessage("Disalin ${uiState.activeBatchCues.size} baris bernomor.")
                                },
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("copy_lines_only_button")
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Salin Baris", maxLines = 1)
                            }

                            Button(
                                onClick = {
                                    val fullText = viewModel.buildCurrentBatchOutput(withPromptHeader = true)
                                    copyToClipboard(context, "SubAI Prompt + Batch", fullText)
                                    val glossaryNote = if (uiState.includeGlossaryInPrompt && activeGlossaryCount > 0) {
                                        " + $activeGlossaryCount aturan glosarium"
                                    } else {
                                        ""
                                    }
                                    viewModel.showBannerMessage(
                                        "Disalin ${uiState.activeBatchCues.size} baris + Prompt AI$glossaryNote!"
                                    )
                                },
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = PrimaryForestButton,
                                    contentColor = Color.White
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("sheet_copy_with_prompt_button")
                            ) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Copy + Prompt", maxLines = 1)
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * SCREEN 1B (Right of Image 1): "Prompt Terjemahan"
 * Reproduces the locked reference design 100%:
 * - Deep Forest Green Top Bar: ← Prompt Terjemahan ⋮
 * - Hero Banner with sparkle icon + "Prompt Terjemahan" + 3D tilted document & blue `AI` badge
 * - Rounded Alabaster Sheet (#F3F6F3, topStart/topEnd = 28.dp)
 * - 2-Tab Bar: [ ⊞ Template | </> Kustom ] with green bottom underline on active tab
 * - 5 Template Rows in White Card: Anime Casual (selected with mint highlight & green ✓), Formal Bioskop, Literal, Kreatif, Kustom Saya
 * - Card "Isi Prompt" with `[ {} Variabel ]` button + editable prompt box
 * - Card "Parameter Tambahan" with chevron ›
 * - Full-width Forest Green Pill Button: `[ ▷ Uji Prompt ]`
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TranslationPromptFullScreen(
    uiState: SubtitleUiState,
    themeSettings: ThemeSettings,
    activeGlossaryCount: Int,
    viewModel: SubtitleViewModel,
    onBack: () -> Unit,
    onSwitchToExtractionScreen: () -> Unit
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    // 0 = Template, 1 = Kustom
    var activeSubTab by rememberSaveable { mutableIntStateOf(0) }
    var showAdditionalParams by rememberSaveable { mutableStateOf(false) }
    var showVariableMenu by remember { mutableStateOf(false) }
    var menuExpanded by remember { mutableStateOf(false) }

    val templateOptions = listOf(
        Triple(TranslationStylePreset.ANIME_CASUAL, Icons.Outlined.Tv, false),
        Triple(TranslationStylePreset.MOVIE_FORMAL, Icons.Outlined.Movie, false),
        Triple(TranslationStylePreset.LITERAL, Icons.AutoMirrored.Outlined.MenuBook, false),
        Triple(TranslationStylePreset.CREATIVE, Icons.Outlined.AutoAwesome, false),
        Triple(TranslationStylePreset.CUSTOM, Icons.Outlined.PersonOutline, true)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepForestHeaderBg)
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        // Top Header Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("prompt_screen_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Kembali",
                        tint = Color.White
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Prompt Terjemahan",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Opsi Lainnya",
                        tint = Color.White
                    )
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Buka Aturan Ekstraksi") },
                        leadingIcon = { Icon(Icons.Default.Tune, contentDescription = null) },
                        onClick = {
                            menuExpanded = false
                            onSwitchToExtractionScreen()
                        }
                    )
                }
            }
        }

        // Hero Banner inside Deep Forest Header Area
        ExtractionHeroBanner(
            title = "Prompt Terjemahan",
            subtitle = "Atur instruksi (prompt) yang dikirim ke AI saat menerjemahkan subtitle.",
            badgeText = "AI",
            badgeBgColor = Color(0xFFD5E1FF),
            badgeTextColor = Color(0xFF284594),
            leadingIcon = Icons.Default.AutoAwesome,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 14.dp)
        )

        // Rounded-Top Alabaster Sheet
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
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 16.dp)
                    .padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 2-Tab Segmented Bar with Green Underline: [ ⊞ Template | </> Kustom ]
                ReferenceTwoTabHeader(
                    leftTitle = "Template",
                    leftIcon = Icons.Outlined.GridView,
                    rightTitle = "Kustom",
                    rightIcon = Icons.Default.Code,
                    selectedTab = activeSubTab,
                    isDark = isDark,
                    onSelectTab = { tabIdx ->
                        activeSubTab = tabIdx
                        if (tabIdx == 1) {
                            viewModel.setTranslationStylePreset(TranslationStylePreset.CUSTOM)
                        }
                    }
                )

                // Card 1: 5 Template Rows matching Reference Image 1B
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (isDark) MaterialTheme.colorScheme.surface else Color.White,
                    border = BorderStroke(
                        1.dp,
                        if (isDark) MaterialTheme.colorScheme.outlineVariant else Color(0xFFE3E9E4)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        templateOptions.forEachIndexed { index, (preset, icon, hasChevron) ->
                            val isSelected = uiState.stylePreset == preset
                            PromptTemplateListRow(
                                title = preset.shortTitle,
                                subtitle = preset.shortSubtitle,
                                icon = icon,
                                selected = isSelected,
                                showChevronInsteadOfRadio = hasChevron && !isSelected,
                                isDark = isDark,
                                testTag = "style_preset_${preset.name.lowercase()}",
                                onClick = {
                                    viewModel.setTranslationStylePreset(preset)
                                    if (preset == TranslationStylePreset.CUSTOM) {
                                        activeSubTab = 1
                                    }
                                }
                            )
                            if (index < templateOptions.lastIndex) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 12.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
                                )
                            }
                        }
                    }
                }

                // Card 2: "Isi Prompt" + [ {} Variabel ] + Editable Prompt Box matching Reference Image 1B
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (isDark) MaterialTheme.colorScheme.surface else Color.White,
                    border = BorderStroke(
                        1.dp,
                        if (isDark) MaterialTheme.colorScheme.outlineVariant else Color(0xFFE3E9E4)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Edit,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "Isi Prompt",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Box {
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = if (isDark) Color(0xFF1F3027) else Color(0xFFF3F6F3),
                                    border = BorderStroke(
                                        1.dp,
                                        if (isDark) MaterialTheme.colorScheme.outlineVariant else Color(0xFFDADFD9)
                                    ),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(16.dp))
                                        .clickable { showVariableMenu = true }
                                        .testTag("prompt_variables_button")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = "{}",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "Variabel",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }

                                DropdownMenu(
                                    expanded = showVariableMenu,
                                    onDismissRequest = { showVariableMenu = false }
                                ) {
                                    listOf(
                                        "[ID]" to "Nomor Baris ([1], [2]...)",
                                        "{target_lang}" to "Bahasa Target (${uiState.targetLanguageOption.label})",
                                        "{glossary}" to "Kamus Glosarium Aktif ($activeGlossaryCount istilah)"
                                    ).forEach { (token, desc) ->
                                        DropdownMenuItem(
                                            text = { Text("$token — $desc") },
                                            onClick = {
                                                showVariableMenu = false
                                                val updated = if (uiState.customPromptInstruction.contains(token)) {
                                                    uiState.customPromptInstruction
                                                } else {
                                                    "${uiState.customPromptInstruction.trimEnd()}\n- Gunakan $token"
                                                }
                                                viewModel.setCustomPromptInstruction(updated)
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = uiState.customPromptInstruction,
                                onValueChange = { viewModel.setCustomPromptInstruction(it) },
                                shape = RoundedCornerShape(14.dp),
                                textStyle = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    lineHeight = 19.sp
                                ),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = if (isDark) Color(0xFF192820) else Color(0xFFF3F5F3),
                                    unfocusedContainerColor = if (isDark) Color(0xFF192820) else Color(0xFFF3F5F3),
                                    focusedBorderColor = EmeraldCheckGreen.copy(alpha = 0.5f),
                                    unfocusedBorderColor = if (isDark) Color(0xFF2B3F33) else Color(0xFFE2E7E2)
                                ),
                                minLines = 5,
                                maxLines = 8,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("custom_prompt_instruction_input")
                            )

                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(10.dp)
                                    .size(15.dp)
                            )
                        }
                    }
                }

                // Card 3: "Parameter Tambahan" with chevron › matching Reference Image 1B
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
                        .clickable { showAdditionalParams = !showAdditionalParams }
                        .testTag("additional_prompt_params_card")
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Settings,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "Parameter Tambahan",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        AnimatedVisibility(visible = showAdditionalParams) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
                                    .padding(bottom = 14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))

                                ExtractionOptionSwitchRow(
                                    icon = Icons.Outlined.AutoAwesome,
                                    label = "Sertakan Instruksi System Prompt saat Copy",
                                    checked = uiState.includeAiPromptHeader,
                                    onCheckedChange = { viewModel.setIncludeAiPromptHeader(it) },
                                    testTag = "include_ai_prompt_switch"
                                )

                                ExtractionOptionSwitchRow(
                                    icon = Icons.Outlined.Dataset,
                                    label = "Sertakan Glosarium di System Prompt ($activeGlossaryCount istilah)",
                                    checked = uiState.includeGlossaryInPrompt,
                                    onCheckedChange = { viewModel.setIncludeGlossaryInPrompt(it) },
                                    testTag = "include_glossary_prompt_switch"
                                )

                                Text(
                                    text = "Bahasa Target Terjemahan:",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold
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
                            }
                        }
                    }
                }

                // Bottom Primary Button: [ ▷ Uji Prompt ] matching Reference Image 1B
                Button(
                    onClick = {
                        viewModel.translateCurrentBatchWithAi()
                        onBack()
                    },
                    shape = RoundedCornerShape(26.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryForestButton,
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("test_prompt_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Uji Prompt",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun ExtractionHeroBanner(
    title: String,
    subtitle: String,
    badgeText: String,
    badgeBgColor: Color,
    badgeTextColor: Color,
    leadingIcon: ImageVector,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = Color(0xFF193D2C),
        border = BorderStroke(1.dp, Color(0xFF2B6046)),
        modifier = modifier
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            // Subtle glowing wave backdrop
            Canvas(modifier = Modifier.matchParentSize()) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFF398C60).copy(alpha = 0.32f), Color.Transparent),
                        center = Offset(size.width * 0.82f, size.height * 0.45f),
                        radius = size.width * 0.45f
                    ),
                    center = Offset(size.width * 0.82f, size.height * 0.45f),
                    radius = size.width * 0.45f
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = leadingIcon,
                            contentDescription = null,
                            tint = Color(0xFF8BE8B0),
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                    }
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.82f),
                        lineHeight = 17.sp
                    )
                }

                // Right 3D Tilted Document + Pill Badge Illustration
                Box(
                    modifier = Modifier
                        .size(width = 86.dp, height = 76.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        rotate(degrees = 10f, pivot = Offset(size.width * 0.48f, size.height * 0.5f)) {
                            val docW = size.width * 0.68f
                            val docH = size.height * 0.86f
                            val left = size.width * 0.12f
                            val top = size.height * 0.06f
                            drawRoundRect(
                                brush = Brush.linearGradient(
                                    colors = listOf(Color(0xFFF3F8F5), Color(0xFFD5E4DC))
                                ),
                                topLeft = Offset(left, top),
                                size = Size(docW, docH),
                                cornerRadius = CornerRadius(22f, 22f)
                            )
                            // 3 green horizontal lines inside document
                            val lineX = left + docW * 0.18f
                            drawRoundRect(
                                color = Color(0xFF7CB798),
                                topLeft = Offset(lineX, top + docH * 0.25f),
                                size = Size(docW * 0.42f, 8f),
                                cornerRadius = CornerRadius(4f, 4f)
                            )
                            drawRoundRect(
                                color = Color(0xFF9AC5AE),
                                topLeft = Offset(lineX, top + docH * 0.45f),
                                size = Size(docW * 0.62f, 8f),
                                cornerRadius = CornerRadius(4f, 4f)
                            )
                            drawRoundRect(
                                color = Color(0xFF9AC5AE),
                                topLeft = Offset(lineX, top + docH * 0.64f),
                                size = Size(docW * 0.48f, 8f),
                                cornerRadius = CornerRadius(4f, 4f)
                            )
                        }
                    }

                    // Overlapping Bottom-Right Badge ("{...}" or "AI")
                    Surface(
                        shape = RoundedCornerShape(11.dp),
                        color = badgeBgColor,
                        shadowElevation = 4.dp,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .offset(x = (-2).dp, y = (-4).dp)
                    ) {
                        Text(
                            text = badgeText,
                            style = MaterialTheme.typography.labelMedium,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.ExtraBold,
                            color = badgeTextColor,
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ReferenceTwoTabHeader(
    leftTitle: String,
    leftIcon: ImageVector,
    rightTitle: String,
    rightIcon: ImageVector,
    selectedTab: Int,
    isDark: Boolean,
    onSelectTab: (Int) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = if (isDark) Color(0xFF1A2820) else Color(0xFFE8ECE8),
        modifier = Modifier
            .fillMaxWidth()
            .height(46.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(3.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            listOf(
                Triple(0, leftTitle, leftIcon),
                Triple(1, rightTitle, rightIcon)
            ).forEach { (idx, title, icon) ->
                val isSelected = selectedTab == idx
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .clip(RoundedCornerShape(15.dp))
                        .background(
                            if (isSelected) {
                                if (isDark) Color(0xFF243A2E) else Color.White
                            } else {
                                Color.Transparent
                            }
                        )
                        .clickable { onSelectTab(idx) }
                ) {
                    Row(
                        modifier = Modifier.align(Alignment.Center),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = if (isSelected) EmeraldCheckGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(17.dp)
                        )
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.onSurface
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }

                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth(0.72f)
                                .height(3.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(PrimaryForestButton)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ExtractionPresetGridCard(
    preset: ExtractionRulePreset,
    icon: ImageVector,
    selected: Boolean,
    isDark: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bg = if (selected) {
        if (isDark) Color(0xFF1F422E) else Color(0xFFDBF2DF)
    } else {
        if (isDark) MaterialTheme.colorScheme.surface else Color(0xFFF7F9F7)
    }
    val borderColor = if (selected) {
        Color(0xFF309B5A)
    } else if (isDark) {
        MaterialTheme.colorScheme.outlineVariant
    } else {
        Color(0xFFE2E8E3)
    }

    Box(modifier = modifier) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = bg,
            border = BorderStroke(if (selected) 1.6.dp else 1.dp, borderColor),
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .clickable(onClick = onClick)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = preset.title,
                    tint = if (selected) PrimaryForestButton else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = preset.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = preset.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        if (selected) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 8.dp, end = 8.dp)
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(EmeraldCheckGreen),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Aktif",
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@Composable
private fun ExtractionOptionSwitchRow(
    icon: ImageVector,
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(19.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = EmeraldCheckGreen,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = Color(0xFFD5DBD6),
                uncheckedBorderColor = Color.Transparent
            ),
            modifier = Modifier.testTag(testTag)
        )
    }
}

@Composable
private fun PromptTemplateListRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    selected: Boolean,
    showChevronInsteadOfRadio: Boolean,
    isDark: Boolean,
    testTag: String,
    onClick: () -> Unit
) {
    val bg = if (selected) {
        if (isDark) Color(0xFF1F422E) else Color(0xFFDDF1E0)
    } else {
        Color.Transparent
    }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = bg,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = if (selected) EmeraldCheckGreen else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(21.dp)
                )
                Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            when {
                selected -> {
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(EmeraldCheckGreen),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Terpilih",
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
                showChevronInsteadOfRadio -> {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
                else -> {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .border(
                                width = 1.6.dp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                                shape = CircleShape
                            )
                    )
                }
            }
        }
    }
}
