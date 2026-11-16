package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.BilingualOrder
import com.example.model.MergeCompareItem
import com.example.model.ThemeSettings
import com.example.ui.components.StatusPillBadge
import com.example.ui.theme.ForestGreenPrimary
import com.example.ui.components.FrontierStatusChip
import com.example.ui.components.FrontierMetric
import com.example.ui.components.FrontierStatusChip
import com.example.ui.components.FrontierMetricStrip
import com.example.ui.components.FrontierTone
import com.example.ui.theme.FrontierMonoFamily
import com.example.ui.theme.SageHighlightFill
import com.example.ui.theme.subtitleEditorTextStyle
import com.example.viewmodel.SubtitleUiState
import com.example.viewmodel.SubtitleViewModel

@Composable
fun MergeTabScreen(
    uiState: SubtitleUiState,
    themeSettings: ThemeSettings,
    viewModel: SubtitleViewModel,
    onSaveMergedFile: (suggestedFileName: String) -> Unit,
    isWideScreen: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    var previewCueIndex by rememberSaveable { mutableIntStateOf(0) }
    var showAllLinesComparison by rememberSaveable { mutableStateOf(false) }

    val compareItems = remember(
        uiState.document,
        uiState.parsedTranslations,
        uiState.separatorMode,
        uiState.customSeparator,
        uiState.restoreLeadingAssTags,
        uiState.bilingualMode,
        uiState.bilingualOrder,
        uiState.bilingualAssSecondaryStyling
    ) {
        viewModel.buildMergeCompareList()
    }

    val rawMergedPreview = remember(
        uiState.document,
        uiState.parsedTranslations,
        uiState.separatorMode,
        uiState.customSeparator,
        uiState.restoreLeadingAssTags,
        uiState.bilingualMode,
        uiState.bilingualOrder,
        uiState.bilingualAssSecondaryStyling,
        uiState.autoApplyGlossaryBeforeMerge,
        uiState.showRawMergedPreview
    ) {
        if (uiState.showRawMergedPreview) {
            viewModel.buildFinalMergedFileContent()
        } else {
            ""
        }
    }

    val baseName = uiState.document.fileName.substringBeforeLast(".")
    val ext = uiState.document.format.extension
    val suffix = if (uiState.bilingualMode) "_ID_Bilingual" else "_ID_Merged"
    val suggestedFileName = "${baseName}${suffix}.$ext"

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(bottom = 12.dp)
        ) {
            // 0. Frontier metrics strip (baris / error / tag ASS)
            item(key = "merge_metrics_strip") {
                FrontierMetricStrip(
                    metrics = listOf(
                        FrontierMetric("${uiState.document.cues.size}", "Baris", FrontierTone.INK),
                        FrontierMetric(
                            "${compareItems.count { it.finalMergedLineText.isBlank() }}",
                            "Error",
                            FrontierTone.OK
                        ),
                        FrontierMetric(
                            "${uiState.document.cues.count { it.style.isNotBlank() }}",
                            "Tag ASS",
                            FrontierTone.MUTED
                        )
                    )
                )
            }

            // 1. Segmented Pill Toggle: [ 📄 Satu Bahasa | 📑 Bilingual ] matching Panel 5
            item(key = "merge_mode_segmented_bar") {
                MergeLanguageModePillSelector(
                    bilingualMode = uiState.bilingualMode,
                    isDark = isDark,
                    onSelectBilingual = { viewModel.setBilingualMode(it) }
                )
            }

            // 2. Options Card: Urutan Teks Dropdown + Pertahankan Tag .ASS + Auto-Styling Sub-teks
            item(key = "merge_options_card") {
                MergeOptionsStudioCard(
                    uiState = uiState,
                    viewModel = viewModel
                )
            }

            // 3. "Pratinjau" Scenic Anime Sunset Video Subtitle Preview Card matching Panel 5!
            item(key = "merge_scenic_preview_card") {
                val safeIndex = if (compareItems.isEmpty()) 0 else previewCueIndex.coerceIn(0, compareItems.lastIndex)
                val activePreviewItem = compareItems.getOrNull(safeIndex)

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Pratinjau",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        if (compareItems.size > 1) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                IconButton(
                                    onClick = {
                                        previewCueIndex = if (safeIndex > 0) safeIndex - 1 else compareItems.lastIndex
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Sebelumnya")
                                }
                                Text(
                                    text = "#${(activePreviewItem?.cue?.id ?: 1)} / ${compareItems.size}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                IconButton(
                                    onClick = {
                                        previewCueIndex = if (safeIndex < compareItems.lastIndex) safeIndex + 1 else 0
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Berikutnya")
                                }
                            }
                        }
                    }

                    AnimeSunsetVideoPreviewBox(
                        previewItem = activePreviewItem,
                        bilingualMode = uiState.bilingualMode,
                        bilingualOrder = uiState.bilingualOrder,
                        onToggleExpandList = { showAllLinesComparison = !showAllLinesComparison }
                    )
                }
            }

            // 4. Raw Merged Code View or Expanded Line Comparison Cards
            if (uiState.showRawMergedPreview) {
                item(key = "raw_merged_box") {
                    RawMergedCodeSurface(
                        rawMergedPreview = rawMergedPreview,
                        themeSettings = themeSettings,
                        onCopyRaw = {
                            copyToClipboard(context, "Merged Subtitle File", rawMergedPreview)
                            viewModel.showBannerMessage("Seluruh kode file .$ext berhasil disalin ke Clipboard!")
                        }
                    )
                }
            } else if (showAllLinesComparison || isWideScreen) {
                items(
                    items = compareItems,
                    key = { "merge_cue_${it.cue.id}" }
                ) { item ->
                    MergeLineComparisonCard(
                        item = item,
                        isAssFamily = uiState.document.format.isAssFamily,
                        themeSettings = themeSettings
                    )
                }
            }
        }

        // 5. Sticky Bottom Action Bar matching Screen 8: [ </> Raw ] [ 📥 Simpan Subtitle ]
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = { viewModel.setShowRawMergedPreview(!uiState.showRawMergedPreview) },
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = if (isDark) MaterialTheme.colorScheme.surface else Color.White,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ),
                border = BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant
                ),
                modifier = Modifier
                    .height(50.dp)
                    .testTag("preview_mode_raw_chip")
            ) {
                Icon(
                    imageVector = Icons.Default.Code,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (uiState.showRawMergedPreview) "Tutup" else "Raw",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            val primaryColor = if (isDark) MaterialTheme.colorScheme.primary else com.example.ui.theme.PrimaryForestButton
            val onPrimaryColor = if (isDark) MaterialTheme.colorScheme.onPrimary else Color.White

            Button(
                onClick = { onSaveMergedFile(suggestedFileName) },
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = primaryColor,
                    contentColor = onPrimaryColor
                ),
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp)
                    .testTag("save_merged_file_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Download,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Simpan Subtitle",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun MergeLanguageModePillSelector(
    bilingualMode: Boolean,
    isDark: Boolean,
    onSelectBilingual: (Boolean) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            MergeModeChip(
                label = "Satu Bahasa",
                icon = Icons.Default.Description,
                selected = !bilingualMode,
                testTag = "merge_mode_single_language",
                modifier = Modifier.weight(1f),
                onClick = { onSelectBilingual(false) }
            )
            MergeModeChip(
                label = "Bilingual",
                icon = Icons.Default.Layers,
                selected = bilingualMode,
                testTag = "bilingual_mode_switch",
                modifier = Modifier.weight(1f),
                onClick = { onSelectBilingual(true) }
            )
        }
    }
}

@Composable
private fun MergeModeChip(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    testTag: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(50),
        color = if (selected) MaterialTheme.colorScheme.surface else Color.Transparent,
        border = if (selected) BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant) else null,
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(15.dp),
                tint = if (selected) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
                color = if (selected) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
    }
}

@Composable
private fun MergeOptionsStudioCard(
    uiState: SubtitleUiState,
    viewModel: SubtitleViewModel
) {
    var showOrderMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Dropdown Urutan Teks matching Panel 5
            Text(
                text = "Urutan Teks",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Box(modifier = Modifier.fillMaxWidth()) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { showOrderMenu = true }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (uiState.bilingualOrder == BilingualOrder.TRANSLATION_TOP) {
                                "Terjemahan di Atas"
                            } else {
                                "Teks Asli di Atas"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = null
                        )
                    }
                }

                DropdownMenu(
                    expanded = showOrderMenu,
                    onDismissRequest = { showOrderMenu = false }
                ) {
                    BilingualOrder.entries.forEach { order ->
                        DropdownMenuItem(
                            text = { Text(order.label) },
                            onClick = {
                                viewModel.setBilingualOrder(order)
                                showOrderMenu = false
                            }
                        )
                    }
                }
            }

            // Switch 1: Pertahankan Tag .ASS
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Pertahankan Tag .ASS",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Switch(
                    checked = uiState.restoreLeadingAssTags,
                    onCheckedChange = { viewModel.setRestoreLeadingAssTags(it) },
                    modifier = Modifier.testTag("restore_leading_tags_switch")
                )
            }

            // Switch 2: Auto-Styling Sub-teks
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Auto-Styling Sub-teks",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Switch(
                    checked = uiState.bilingualAssSecondaryStyling,
                    onCheckedChange = { viewModel.setBilingualAssSecondaryStyling(it) },
                    modifier = Modifier.testTag("bilingual_ass_secondary_switch")
                )
            }
        }
    }
}

@Composable
private fun AnimeSunsetVideoPreviewBox(
    previewItem: MergeCompareItem?,
    bilingualMode: Boolean,
    bilingualOrder: BilingualOrder,
    onToggleExpandList: () -> Unit
) {
    val sourceLine = previewItem?.cue?.cleanText?.replace("\\N", " ")
        ?: "Aah... we finally made it."
    val translatedLine = previewItem?.rawTranslatedLine?.ifBlank { null }?.replace("\\N", " ")
        ?: "Aah... akhirnya sampai juga."

    val topLine = if (!bilingualMode || bilingualOrder == BilingualOrder.TRANSLATION_TOP) {
        translatedLine
    } else {
        sourceLine
    }
    val bottomLine = if (!bilingualMode) {
        null
    } else if (bilingualOrder == BilingualOrder.TRANSLATION_TOP) {
        sourceLine
    } else {
        translatedLine
    }

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.inverseSurface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Header: mono file cue id + mode badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = previewItem?.let { "#${it.cue.id}" } ?: "#001",
                        fontFamily = FrontierMonoFamily,
                        fontSize = 10.5.sp,
                        color = MaterialTheme.colorScheme.inverseOnSurface.copy(alpha = 0.62f)
                    )
                    Text(
                        text = when {
                            !bilingualMode -> "Satu bahasa"
                            bilingualOrder == BilingualOrder.TRANSLATION_TOP -> "Asli di bawah"
                            else -> "Asli di atas"
                        },
                        fontFamily = FrontierMonoFamily,
                        fontSize = 9.5.sp,
                        color = MaterialTheme.colorScheme.inverseOnSurface.copy(alpha = 0.45f)
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    if (previewItem?.cue != null) {
                        Text(
                            text = "${previewItem.cue.startTime} → ${previewItem.cue.endTime}",
                            fontFamily = FrontierMonoFamily,
                            fontSize = 9.5.sp,
                            color = MaterialTheme.colorScheme.inverseOnSurface.copy(alpha = 0.45f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (bottomLine != null) {
                        Text(
                            text = if (bilingualOrder == BilingualOrder.TRANSLATION_TOP) bottomLine else topLine,
                            fontSize = 12.5.sp,
                            lineHeight = 18.sp,
                            color = MaterialTheme.colorScheme.inverseOnSurface.copy(alpha = 0.58f),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(MaterialTheme.colorScheme.inverseOnSurface.copy(alpha = 0.14f))
                        )
                        Text(
                            text = if (bilingualOrder == BilingualOrder.TRANSLATION_TOP) topLine else bottomLine,
                            fontSize = 13.5.sp,
                            lineHeight = 19.sp,
                            color = MaterialTheme.colorScheme.inverseOnSurface,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    } else {
                        Text(
                            text = topLine,
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                            color = MaterialTheme.colorScheme.inverseOnSurface,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.inverseOnSurface.copy(alpha = 0.12f),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(10.dp)
                    .size(28.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onToggleExpandList)
                    .testTag("preview_mode_cards_chip")
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = "Lihat Semua Baris",
                        tint = MaterialTheme.colorScheme.inverseOnSurface,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun RawMergedCodeSurface(
    rawMergedPreview: String,
    themeSettings: ThemeSettings,
    onCopyRaw: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 200.dp, max = 420.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Kode File Subtitle Hasil Merge",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
                OutlinedButton(
                    onClick = onCopyRaw,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.testTag("copy_merged_file_button")
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Copy Semua", style = MaterialTheme.typography.labelSmall)
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            val rawScroll = rememberScrollState()
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rawScroll)
                    .padding(14.dp)
            ) {
                Text(
                    text = rawMergedPreview,
                    style = subtitleEditorTextStyle(themeSettings),
                    modifier = Modifier.testTag("raw_merged_code_text")
                )
            }
        }
    }
}

@Composable
private fun MergeLineComparisonCard(
    item: MergeCompareItem,
    isAssFamily: Boolean,
    themeSettings: ThemeSettings
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant
        ),
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "#${item.cue.id}",
                        fontFamily = FrontierMonoFamily,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${item.cue.startTime} → ${item.cue.endTime}",
                        fontFamily = FrontierMonoFamily,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (!item.isTranslated) {
                        FrontierStatusChip(text = "Belum", tone = FrontierTone.DANGER)
                    }
                }

                if (item.cue.leadingAssTags.isNotEmpty()) {
                    StatusPillBadge(
                        text = item.cue.leadingAssTags,
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            if (isAssFamily && (item.cue.style.isNotBlank() || item.cue.actor.isNotBlank())) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Subtitles,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Style: ${item.cue.style}" +
                            (if (item.cue.actor.isNotBlank()) " • Actor: ${item.cue.actor}" else ""),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Text(
                text = "Asli: ${item.cue.cleanText}",
                style = subtitleEditorTextStyle(themeSettings),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = "Merge: ${item.finalMergedLineText}",
                style = subtitleEditorTextStyle(themeSettings),
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.testTag("merged_line_preview_${item.cue.id}")
            )
        }
    }
}
