package com.example.ui.screens

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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.BilingualOrder
import com.example.model.MergeCompareItem
import com.example.model.ThemeSettings
import com.example.ui.components.ResponsivePrimarySecondaryButtons
import com.example.ui.components.StatusPillBadge
import com.example.ui.components.StudioSectionCard
import com.example.ui.components.StudioToggleRow
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
    // 0 = All Lines, 1 = Translated Only, 2 = Untranslated Only
    var statusFilterMode by rememberSaveable { mutableIntStateOf(0) }

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

    val filteredCompareItems = remember(compareItems, statusFilterMode) {
        when (statusFilterMode) {
            1 -> compareItems.filter { it.isTranslated }
            2 -> compareItems.filter { !it.isTranslated }
            else -> compareItems
        }
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

    if (isWideScreen) {
        // Dual-Pane Layout for Landscape / Foldable / Tablet
        Row(
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            LazyColumn(
                modifier = Modifier
                    .weight(0.95f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                item(key = "merge_config_card") {
                    MergeConfigAndExportCard(
                        uiState = uiState,
                        viewModel = viewModel,
                        onSaveMergedFile = onSaveMergedFile
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
                item(key = "merge_preview_mode_header") {
                    MergePreviewModeHeaderCard(
                        uiState = uiState,
                        statusFilterMode = statusFilterMode,
                        onChangeStatusFilter = { statusFilterMode = it },
                        viewModel = viewModel
                    )
                }

                if (uiState.showRawMergedPreview) {
                    item(key = "raw_merged_box") {
                        RawMergedCodeSurface(
                            rawMergedPreview = rawMergedPreview,
                            themeSettings = themeSettings
                        )
                    }
                } else {
                    items(
                        items = filteredCompareItems,
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
        }
    } else {
        // Compact 1-Column Virtualized LazyColumn Layout
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(key = "merge_config_card") {
                MergeConfigAndExportCard(
                    uiState = uiState,
                    viewModel = viewModel,
                    onSaveMergedFile = onSaveMergedFile
                )
            }

            item(key = "merge_preview_mode_header") {
                MergePreviewModeHeaderCard(
                    uiState = uiState,
                    statusFilterMode = statusFilterMode,
                    onChangeStatusFilter = { statusFilterMode = it },
                    viewModel = viewModel
                )
            }

            if (uiState.showRawMergedPreview) {
                item(key = "raw_merged_box") {
                    RawMergedCodeSurface(
                        rawMergedPreview = rawMergedPreview,
                        themeSettings = themeSettings
                    )
                }
            } else {
                items(
                    items = filteredCompareItems,
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
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MergeConfigAndExportCard(
    uiState: SubtitleUiState,
    viewModel: SubtitleViewModel,
    onSaveMergedFile: (suggestedFileName: String) -> Unit
) {
    val context = LocalContext.current
    val baseName = uiState.document.fileName.substringBeforeLast(".")
    val ext = uiState.document.format.extension
    val suffix = if (uiState.bilingualMode) "_ID_Bilingual" else "_ID_Merged"
    val suggestedFileName = "${baseName}${suffix}.$ext"

    StudioSectionCard(
        title = "Konfigurasi Smart Regex Merger & Bilingual",
        subtitle = "Pulihkan tag posisi .ASS, baris ganda, dan simpan ke file subtitle siap pakai",
        icon = Icons.Default.Layers,
        iconTint = MaterialTheme.colorScheme.primary
    ) {
        if (uiState.document.format.isAssFamily) {
            StudioToggleRow(
                title = "Pulihkan Leading Tags .ASS ({\\an8}, {\\pos})",
                description = "Memasang kembali tag posisi/animasi di awal dialog sebelum kalimat terjemahan.",
                checked = uiState.restoreLeadingAssTags,
                onCheckedChange = { viewModel.setRestoreLeadingAssTags(it) },
                testTag = "restore_leading_tags_switch"
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
        }

        StudioToggleRow(
            title = "Mode Subtitle Ganda (Bilingual)",
            description = "Menampilkan teks terjemahan sekaligus teks asli dalam satu timestamp.",
            checked = uiState.bilingualMode,
            onCheckedChange = { viewModel.setBilingualMode(it) },
            testTag = "bilingual_mode_switch"
        )

        AnimatedVisibility(visible = uiState.bilingualMode) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Susunan Teks Mode Bilingual:",
                    style = MaterialTheme.typography.labelLarge
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    BilingualOrder.entries.forEach { order ->
                        FilterChip(
                            selected = uiState.bilingualOrder == order,
                            onClick = { viewModel.setBilingualOrder(order) },
                            label = { Text(order.label) },
                            modifier = Modifier.testTag("bilingual_order_${order.name.lowercase()}")
                        )
                    }
                }

                if (uiState.document.format.isAssFamily) {
                    StudioToggleRow(
                        title = "Ukuran & Warna Sekunder Teks Asli (.ASS)",
                        description = "Menyisipkan tag {\\r\\fs34\\1c&HD0DCE5&} pada baris bahasa asli agar terjemahan utama lebih menonjol.",
                        checked = uiState.bilingualAssSecondaryStyling,
                        onCheckedChange = { viewModel.setBilingualAssSecondaryStyling(it) },
                        testTag = "bilingual_ass_secondary_switch"
                    )
                }
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

        ResponsivePrimarySecondaryButtons(
            primaryLabel = "Simpan File (.${ext.uppercase()})",
            primaryIcon = Icons.Default.Download,
            primaryTestTag = "save_merged_file_button",
            onPrimaryClick = { onSaveMergedFile(suggestedFileName) },
            secondaryLabel = "Copy Seluruh File",
            secondaryIcon = Icons.Default.ContentCopy,
            secondaryTestTag = "copy_merged_file_button",
            onSecondaryClick = {
                val fullMerged = viewModel.buildFinalMergedFileContent()
                copyToClipboard(context, "Merged Subtitle File", fullMerged)
                viewModel.showBannerMessage("Seluruh isi file .$ext berhasil disalin ke Clipboard!")
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MergePreviewModeHeaderCard(
    uiState: SubtitleUiState,
    statusFilterMode: Int,
    onChangeStatusFilter: (Int) -> Unit,
    viewModel: SubtitleViewModel
) {
    StudioSectionCard(
        title = "Pratinjau Hasil Merge",
        subtitle = "${uiState.matchedCount} dari ${uiState.document.cues.size} baris dialog telah diterjemahkan",
        icon = Icons.Default.CompareArrows,
        iconTint = MaterialTheme.colorScheme.secondary
    ) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            FilterChip(
                selected = !uiState.showRawMergedPreview,
                onClick = { viewModel.setShowRawMergedPreview(false) },
                label = { Text("Kartu Perbandingan") },
                leadingIcon = {
                    Icon(
                        Icons.Default.CompareArrows,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                },
                modifier = Modifier.testTag("preview_mode_cards_chip")
            )
            FilterChip(
                selected = uiState.showRawMergedPreview,
                onClick = { viewModel.setShowRawMergedPreview(true) },
                label = { Text("Kode Mentah (.${uiState.document.format.extension.uppercase()})") },
                leadingIcon = {
                    Icon(
                        Icons.Default.Code,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                },
                modifier = Modifier.testTag("preview_mode_raw_chip")
            )
        }

        if (!uiState.showRawMergedPreview) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = statusFilterMode == 0,
                    onClick = { onChangeStatusFilter(0) },
                    label = { Text("Semua (${uiState.document.cues.size})") }
                )
                FilterChip(
                    selected = statusFilterMode == 1,
                    onClick = { onChangeStatusFilter(1) },
                    label = { Text("Sudah Cocok (${uiState.matchedCount})") }
                )
                FilterChip(
                    selected = statusFilterMode == 2,
                    onClick = { onChangeStatusFilter(2) },
                    label = { Text("Belum Diterjemahkan (${uiState.missingCueIds.size})") }
                )
            }
        }
    }
}

@Composable
private fun RawMergedCodeSurface(
    rawMergedPreview: String,
    themeSettings: ThemeSettings
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 240.dp, max = 480.dp)
    ) {
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

@Composable
private fun MergeLineComparisonCard(
    item: MergeCompareItem,
    isAssFamily: Boolean,
    themeSettings: ThemeSettings
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
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
                    StatusPillBadge(
                        text = "#${item.cue.id}",
                        containerColor = if (item.isTranslated) {
                            MaterialTheme.colorScheme.secondaryContainer
                        } else {
                            MaterialTheme.colorScheme.errorContainer
                        },
                        contentColor = if (item.isTranslated) {
                            MaterialTheme.colorScheme.onSecondaryContainer
                        } else {
                            MaterialTheme.colorScheme.onErrorContainer
                        }
                    )
                    Text(
                        text = "${item.cue.startTime} → ${item.cue.endTime}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
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
                color = MaterialTheme.colorScheme.secondary,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.testTag("merged_line_preview_${item.cue.id}")
            )
        }
    }
}
