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
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = if (isDark) MaterialTheme.colorScheme.surface else Color.White,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ),
                border = BorderStroke(
                    1.dp,
                    if (isDark) MaterialTheme.colorScheme.outlineVariant else Color(0xFFDCE4DF)
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
                shape = RoundedCornerShape(24.dp),
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
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val activeBg = if (isDark) Color(0xFF214431) else SageHighlightFill
            val activeFg = if (isDark) Color(0xFF8CE0AF) else ForestGreenPrimary

            // Satu Bahasa
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = if (!bilingualMode) activeBg else Color.Transparent,
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { onSelectBilingual(false) }
                    .testTag("merge_mode_single_language")
            ) {
                Row(
                    modifier = Modifier.padding(vertical = 10.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = null,
                        modifier = Modifier.size(17.dp),
                        tint = if (!bilingualMode) activeFg else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Satu Bahasa",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (!bilingualMode) FontWeight.Bold else FontWeight.Medium,
                        color = if (!bilingualMode) activeFg else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Bilingual
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = if (bilingualMode) activeBg else Color.Transparent,
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { onSelectBilingual(true) }
                    .testTag("bilingual_mode_switch")
            ) {
                Row(
                    modifier = Modifier.padding(vertical = 10.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Layers,
                        contentDescription = null,
                        modifier = Modifier.size(17.dp),
                        tint = if (bilingualMode) activeFg else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Bilingual",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (bilingualMode) FontWeight.Bold else FontWeight.Medium,
                        color = if (bilingualMode) activeFg else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
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
    val cleanOriginal = previewItem?.cue?.cleanText?.replace("\\N", " ")
        ?: "Aah... akhirnya sampai juga..."
    val cleanTranslated = previewItem?.rawTranslatedLine?.ifBlank { null }?.replace("\\N", " ")
        ?: "Ah... finally, we've arrived..."

    val topLine = if (!bilingualMode) {
        cleanTranslated
    } else if (bilingualOrder == BilingualOrder.TRANSLATION_TOP) {
        cleanTranslated
    } else {
        cleanOriginal
    }

    val bottomLine = if (!bilingualMode) {
        null
    } else if (bilingualOrder == BilingualOrder.TRANSLATION_TOP) {
        cleanOriginal
    } else {
        cleanTranslated
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(190.dp)
            .testTag("merge_video_preview_box"),
        shape = RoundedCornerShape(22.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Scenic Anime Twilight Sunset Sky with Clouds & Power Lines Silhouette
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height

                // 1. Twilight Sunset Sky Gradient
                drawRect(
                    brush = Brush.verticalGradient(
                        0.0f to Color(0xFF6B82C8),
                        0.45f to Color(0xFFB792B8),
                        0.80f to Color(0xFFEFA895),
                        1.0f to Color(0xFFFAD0A2)
                    )
                )

                // 2. Soft Glowing Sunset Clouds
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFFFFDFC2).copy(alpha = 0.75f), Color.Transparent),
                        center = Offset(w * 0.45f, h * 0.48f),
                        radius = w * 0.36f
                    ),
                    center = Offset(w * 0.45f, h * 0.48f),
                    radius = w * 0.36f
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFFF6B7B0).copy(alpha = 0.65f), Color.Transparent),
                        center = Offset(w * 0.72f, h * 0.36f),
                        radius = w * 0.28f
                    ),
                    center = Offset(w * 0.72f, h * 0.36f),
                    radius = w * 0.28f
                )

                // 3. Distant Horizon & Foliage Silhouette on Bottom-Left
                val hillsPath = Path().apply {
                    moveTo(0f, h * 0.78f)
                    quadraticTo(w * 0.25f, h * 0.68f, w * 0.55f, h * 0.86f)
                    quadraticTo(w * 0.80f, h * 0.92f, w, h * 0.82f)
                    lineTo(w, h)
                    lineTo(0f, h)
                    close()
                }
                drawPath(
                    path = hillsPath,
                    color = Color(0xFF1D2436).copy(alpha = 0.88f)
                )

                // 4. Anime Utility Pole & Power Lines on the Right
                val poleX = w * 0.88f
                drawLine(
                    color = Color(0xFF1C202B),
                    start = Offset(poleX, h * 0.42f),
                    end = Offset(poleX, h),
                    strokeWidth = 5f
                )
                drawLine(
                    color = Color(0xFF1C202B),
                    start = Offset(poleX - 18f, h * 0.48f),
                    end = Offset(poleX + 18f, h * 0.48f),
                    strokeWidth = 3f
                )
                // Power cables sweeping across the sunset sky
                val wirePath = Path().apply {
                    moveTo(poleX, h * 0.48f)
                    quadraticTo(w * 0.94f, h * 0.56f, w, h * 0.52f)
                    moveTo(poleX, h * 0.48f)
                    quadraticTo(w * 0.60f, h * 0.32f, w * 0.25f, 0f)
                }
                drawPath(
                    path = wirePath,
                    color = Color(0xFF1C202B).copy(alpha = 0.55f),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.6f)
                )
            }

            // Expand Button in Top-Right Corner
            Surface(
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.28f),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(10.dp)
                    .size(32.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onToggleExpandList)
                    .testTag("preview_mode_cards_chip")
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = "Lihat Semua Baris",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Translucent Dark Rounded Subtitle Overlay Box in Center-Bottom matching Panel 5!
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF1B2028).copy(alpha = 0.76f),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.18f)),
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = topLine,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (bottomLine != null) {
                        Text(
                            text = bottomLine,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.82f),
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
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
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.testTag("merged_line_preview_${item.cue.id}")
            )
        }
    }
}
