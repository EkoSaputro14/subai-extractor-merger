package com.example.ui.screens

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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
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
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.example.model.FindMatchItem
import com.example.model.GlossaryRule
import com.example.model.ThemeSettings
import com.example.ui.theme.AmberHighlightYellow
import com.example.ui.theme.EmeraldCheckGreen
import com.example.ui.theme.ForestGreenPrimary
import com.example.ui.theme.PrimaryForestButton
import com.example.ui.theme.SageHighlightFill
import com.example.ui.theme.subtitleEditorTextStyle
import com.example.viewmodel.SubtitleUiState
import com.example.viewmodel.SubtitleViewModel

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun GlossaryFindReplaceTabScreen(
    uiState: SubtitleUiState,
    glossaryRules: List<GlossaryRule>,
    themeSettings: ThemeSettings,
    viewModel: SubtitleViewModel,
    isWideScreen: Boolean = false,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    var showAddRuleDialog by rememberSaveable { mutableStateOf(false) }

    val liveMatches = remember(
        uiState.document,
        uiState.parsedTranslations,
        uiState.findQuery,
        uiState.replaceWith,
        uiState.findCaseSensitive,
        uiState.findWholeWord,
        uiState.findUseRegex,
        uiState.selectedStyleFilter,
        uiState.selectedActorFilter
    ) {
        viewModel.computeLiveFindMatches()
    }
    val totalOccurrences = remember(liveMatches) {
        liveMatches.sumOf { it.matchRanges.size }
    }

    if (showAddRuleDialog) {
        AddGlossaryRuleDialog(
            isDark = isDark,
            viewModel = viewModel,
            onDismiss = { showAddRuleDialog = false }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Top 2-Tab Underline Bar matching Screens 6 & 7 ("Find & Replace" | "Kamus")
        SubTabSelectorBar(
            selectedSubTab = uiState.glossarySubTabIndex,
            onSelectSubTab = { viewModel.setGlossarySubTab(it) }
        )

        if (uiState.glossarySubTabIndex == 0) {
            // SCREEN 6: Find & Replace
            if (isWideScreen) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item(key = "find_controls_card") {
                            QuickFindReplaceControlsSection(
                                uiState = uiState,
                                liveMatchesCount = liveMatches.size,
                                totalOccurrences = totalOccurrences,
                                isDark = isDark,
                                viewModel = viewModel
                            )
                        }
                    }

                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        item(key = "results_container_card") {
                            FindResultsListCard(
                                liveMatches = liveMatches,
                                uiState = uiState,
                                themeSettings = themeSettings,
                                isDark = isDark,
                                viewModel = viewModel
                            )
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    item(key = "find_controls_card") {
                        QuickFindReplaceControlsSection(
                            uiState = uiState,
                            liveMatchesCount = liveMatches.size,
                            totalOccurrences = totalOccurrences,
                            isDark = isDark,
                            viewModel = viewModel
                        )
                    }

                    item(key = "results_container_card") {
                        FindResultsListCard(
                            liveMatches = liveMatches,
                            uiState = uiState,
                            themeSettings = themeSettings,
                            isDark = isDark,
                            viewModel = viewModel
                        )
                    }
                }
            }
        } else {
            // SCREEN 7: Kamus (Glossary Rules List + Bottom "+ Tambah Aturan" Forest Green Pill Button)
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 8.dp)
                ) {
                    items(
                        items = glossaryRules,
                        key = { it.id }
                    ) { rule ->
                        GlossaryRuleItemCard(
                            rule = rule,
                            isDark = isDark,
                            viewModel = viewModel
                        )
                    }
                }

                // Optional quick "Terapkan Kamus Sekarang" / "Undo" row if translations exist
                if (uiState.undoTranslationsSnapshot != null) {
                    OutlinedButton(
                        onClick = { viewModel.undoLastReplace() },
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("glossary_undo_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Undo,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Undo Perubahan Kamus")
                    }
                }

                // Bottom Full-Width Forest Green Pill Button matching Screen 7: [ + Tambah Aturan ]
                Button(
                    onClick = { showAddRuleDialog = true },
                    shape = RoundedCornerShape(26.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isDark) MaterialTheme.colorScheme.primary else PrimaryForestButton,
                        contentColor = if (isDark) MaterialTheme.colorScheme.onPrimary else Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("open_add_glossary_dialog_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Tambah Aturan",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun SubTabSelectorBar(
    selectedSubTab: Int,
    onSelectSubTab: (Int) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            UnderlineTabButton(
                text = "Find & Replace",
                selected = selectedSubTab == 0,
                onClick = { onSelectSubTab(0) },
                testTag = "subtab_quick_find_replace",
                modifier = Modifier.weight(1f)
            )
            UnderlineTabButton(
                text = "Kamus",
                selected = selectedSubTab == 1,
                onClick = { onSelectSubTab(1) },
                testTag = "subtab_glossary_rules",
                modifier = Modifier.weight(1f)
            )
        }
        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
        )
    }
}

@Composable
private fun UnderlineTabButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .testTag(testTag),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier.padding(vertical = 8.dp)
        )
        Box(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .height(3.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(
                    if (selected) PrimaryForestButton else Color.Transparent
                )
        )
    }
}

@Composable
private fun QuickFindReplaceControlsSection(
    uiState: SubtitleUiState,
    liveMatchesCount: Int,
    totalOccurrences: Int,
    isDark: Boolean,
    viewModel: SubtitleViewModel
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // 1. Search Input Pill (🔍 Senpai  ✕) matching Screen 6
        OutlinedTextField(
            value = uiState.findQuery,
            onValueChange = { viewModel.setFindQuery(it) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Cari",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            trailingIcon = {
                if (uiState.findQuery.isNotEmpty()) {
                    IconButton(onClick = { viewModel.setFindQuery("") }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Hapus Pencarian",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            },
            placeholder = { Text("Cari teks (contoh: Senpai)") },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = if (isDark) MaterialTheme.colorScheme.surface else Color.White,
                unfocusedContainerColor = if (isDark) MaterialTheme.colorScheme.surface else Color.White,
                focusedBorderColor = PrimaryForestButton.copy(alpha = 0.6f),
                unfocusedBorderColor = if (isDark) MaterialTheme.colorScheme.outlineVariant else Color(0xFFE1E8E3)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("find_query_input")
        )

        // 2. Replace Input Pill (⇄ Senpai  ✕) matching Screen 6
        OutlinedTextField(
            value = uiState.replaceWith,
            onValueChange = { viewModel.setReplaceWith(it) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.SwapHoriz,
                    contentDescription = "Ganti Dengan",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            trailingIcon = {
                if (uiState.replaceWith.isNotEmpty()) {
                    IconButton(onClick = { viewModel.setReplaceWith("") }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Hapus Pengganti",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            },
            placeholder = { Text("Ganti dengan (contoh: Senpai)") },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = if (isDark) MaterialTheme.colorScheme.surface else Color.White,
                unfocusedContainerColor = if (isDark) MaterialTheme.colorScheme.surface else Color.White,
                focusedBorderColor = PrimaryForestButton.copy(alpha = 0.6f),
                unfocusedBorderColor = if (isDark) MaterialTheme.colorScheme.outlineVariant else Color(0xFFE1E8E3)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("replace_with_input")
        )

        // 3. Three Compact Toggle Pills Row matching Screen 6:
        // [ Aa ]   [ (•) Regex ]   [ (•) Whole ]
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Pill 1: [ Aa ]
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = if (uiState.findCaseSensitive) {
                    if (isDark) Color(0xFF214431) else SageHighlightFill
                } else {
                    if (isDark) MaterialTheme.colorScheme.surface else Color.White
                },
                border = BorderStroke(
                    1.dp,
                    if (uiState.findCaseSensitive) {
                        PrimaryForestButton.copy(alpha = 0.35f)
                    } else if (isDark) {
                        MaterialTheme.colorScheme.outlineVariant
                    } else {
                        Color(0xFFE1E8E3)
                    }
                ),
                modifier = Modifier
                    .height(38.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .clickable { viewModel.toggleFindCaseSensitive() }
                    .testTag("filter_case_sensitive_chip")
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    Text(
                        text = "Aa",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (uiState.findCaseSensitive) {
                            if (isDark) Color(0xFF8CE0AF) else Color(0xFF173F2A)
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }

            // Pill 2: [ (•) Regex ]
            CompactTogglePill(
                label = "Regex",
                checked = uiState.findUseRegex,
                isDark = isDark,
                testTag = "filter_regex_chip",
                onClick = { viewModel.toggleFindUseRegex() },
                modifier = Modifier.weight(1f)
            )

            // Pill 3: [ (•) Whole ]
            CompactTogglePill(
                label = "Whole",
                checked = uiState.findWholeWord,
                isDark = isDark,
                testTag = "filter_whole_word_chip",
                onClick = { viewModel.toggleFindWholeWord() },
                modifier = Modifier.weight(1f)
            )
        }

        // 4. Full-Width Forest Green Pill Button: [ ⇄ Ganti Semua (3) ] matching Screen 6
        Button(
            onClick = { viewModel.replaceAllMatches() },
            enabled = liveMatchesCount > 0,
            shape = RoundedCornerShape(24.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isDark) MaterialTheme.colorScheme.primary else PrimaryForestButton,
                contentColor = if (isDark) MaterialTheme.colorScheme.onPrimary else Color.White
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("replace_all_button")
        ) {
            Icon(
                imageVector = Icons.Default.SwapHoriz,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (totalOccurrences > 0) "Ganti Semua ($totalOccurrences)" else "Ganti Semua",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
        }

        if (uiState.undoTranslationsSnapshot != null || liveMatchesCount > 0) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (liveMatchesCount > 0) {
                    OutlinedButton(
                        onClick = { viewModel.replaceNextMatch() },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("replace_next_button")
                    ) {
                        Text("Ganti Berikutnya")
                    }
                }
                if (uiState.undoTranslationsSnapshot != null) {
                    OutlinedButton(
                        onClick = { viewModel.undoLastReplace() },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("undo_replace_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Undo,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Undo")
                    }
                }
            }
        }
    }
}

@Composable
private fun CompactTogglePill(
    label: String,
    checked: Boolean,
    isDark: Boolean,
    testTag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = if (checked) {
            if (isDark) Color(0xFF214431) else SageHighlightFill
        } else {
            if (isDark) MaterialTheme.colorScheme.surface else Color.White
        },
        border = BorderStroke(
            1.dp,
            if (checked) {
                PrimaryForestButton.copy(alpha = 0.35f)
            } else if (isDark) {
                MaterialTheme.colorScheme.outlineVariant
            } else {
                Color(0xFFE1E8E3)
            }
        ),
        modifier = modifier
            .height(38.dp)
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            // Mini Pill Track + Thumb indicator matching Screen 6
            Box(
                modifier = Modifier
                    .width(26.dp)
                    .height(15.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (checked) EmeraldCheckGreen else Color(0xFFD5DDD7)
                    )
                    .padding(horizontal = 2.dp),
                contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart
            ) {
                Box(
                    modifier = Modifier
                        .size(11.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun FindResultsListCard(
    liveMatches: List<FindMatchItem>,
    uiState: SubtitleUiState,
    themeSettings: ThemeSettings,
    isDark: Boolean,
    viewModel: SubtitleViewModel
) {
    var showStyleActorFilters by rememberSaveable { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) MaterialTheme.colorScheme.surface else Color.White
        ),
        border = BorderStroke(
            1.dp,
            if (isDark) MaterialTheme.colorScheme.outlineVariant else Color(0xFFE2E8E3)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Hasil (${liveMatches.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.testTag("live_match_count_text")
                )

                IconButton(
                    onClick = { showStyleActorFilters = !showStyleActorFilters },
                    modifier = Modifier.size(30.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.FilterList,
                        contentDescription = "Filter Style & Actor",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            AnimatedVisibility(visible = showStyleActorFilters && uiState.document.format.isAssFamily) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    val styles = uiState.document.availableStyles
                    if (styles.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = uiState.selectedStyleFilter == "ALL",
                                onClick = { viewModel.setSelectedStyleFilter("ALL") },
                                label = { Text("Semua Style") }
                            )
                            styles.forEach { styleName ->
                                FilterChip(
                                    selected = uiState.selectedStyleFilter == styleName,
                                    onClick = { viewModel.setSelectedStyleFilter(styleName) },
                                    label = { Text(styleName) },
                                    modifier = Modifier.testTag("style_filter_$styleName")
                                )
                            }
                        }
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))

            if (liveMatches.isEmpty()) {
                Text(
                    text = if (uiState.findQuery.isBlank()) {
                        "Ketik kata kunci di atas (mis. Senpai) untuk menyorot hasil pencarian."
                    } else {
                        "Tidak ditemukan kecocokan untuk \"${uiState.findQuery}\"."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            } else {
                liveMatches.forEachIndexed { idx, matchItem ->
                    FindMatchCompactRow(
                        matchItem = matchItem,
                        themeSettings = themeSettings,
                        isDark = isDark
                    )
                    if (idx < liveMatches.lastIndex) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                    }
                }
            }
        }
    }
}

@Composable
private fun FindMatchCompactRow(
    matchItem: FindMatchItem,
    themeSettings: ThemeSettings,
    isDark: Boolean
) {
    // Warm Yellow Highlight matching Screen 6 ([12] ... Senpai ...)
    val highlightBg = if (isDark) Color(0xFF7A6414) else AmberHighlightYellow
    val highlightFg = if (isDark) Color(0xFFFFF8D6) else Color(0xFF1F1A00)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "[${matchItem.cueId}]",
            style = MaterialTheme.typography.bodyMedium,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        val highlightedCurrent = buildAnnotatedString {
            val src = matchItem.currentTranslatedText
            var cursor = 0
            for (range in matchItem.matchRanges) {
                if (range.first > cursor && range.first <= src.length) {
                    append(src.substring(cursor, range.first))
                }
                val endExclusive = (range.last + 1).coerceAtMost(src.length)
                if (range.first in 0 until endExclusive) {
                    withStyle(
                        SpanStyle(
                            background = highlightBg,
                            color = highlightFg,
                            fontWeight = FontWeight.Bold
                        )
                    ) {
                        append(" ${src.substring(range.first, endExclusive)} ")
                    }
                }
                cursor = endExclusive
            }
            if (cursor < src.length) {
                append(src.substring(cursor))
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = highlightedCurrent,
                style = subtitleEditorTextStyle(themeSettings),
                color = MaterialTheme.colorScheme.onSurface
            )
            if (matchItem.previewReplacedText != matchItem.currentTranslatedText) {
                Text(
                    text = "→ ${matchItem.previewReplacedText}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun GlossaryRuleItemCard(
    rule: GlossaryRule,
    isDark: Boolean,
    viewModel: SubtitleViewModel
) {
    // Matches Screen 7: White Rounded Card with [ 📄  Senpai  →  Senpai      (•) ]
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (isDark) MaterialTheme.colorScheme.surface else Color.White,
        border = BorderStroke(
            width = 1.dp,
            color = if (isDark) MaterialTheme.colorScheme.outlineVariant else Color(0xFFE3EAE5)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Description,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(19.dp)
                )

                Text(
                    text = "${rule.sourceTerm}   →   ${rule.targetTerm}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (rule.isEnabled) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(
                    checked = rule.isEnabled,
                    onCheckedChange = { viewModel.toggleGlossaryRuleEnabled(rule.id) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = EmeraldCheckGreen
                    ),
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("toggle_glossary_${rule.id}")
                )

                IconButton(
                    onClick = { viewModel.deleteGlossaryRule(rule.id) },
                    modifier = Modifier
                        .size(32.dp)
                        .testTag("delete_glossary_${rule.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Hapus aturan glosarium",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AddGlossaryRuleDialog(
    isDark: Boolean,
    viewModel: SubtitleViewModel,
    onDismiss: () -> Unit
) {
    var newSourceTerm by rememberSaveable { mutableStateOf("") }
    var newTargetTerm by rememberSaveable { mutableStateOf("") }
    var newCaseSensitive by rememberSaveable { mutableStateOf(false) }
    var newWholeWord by rememberSaveable { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Tambah Aturan Kamus",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = newSourceTerm,
                    onValueChange = { newSourceTerm = it },
                    label = { Text("Istilah Asli / Sumber") },
                    placeholder = { Text("Contoh: Mahou") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("glossary_source_input")
                )
                OutlinedTextField(
                    value = newTargetTerm,
                    onValueChange = { newTargetTerm = it },
                    label = { Text("Istilah Terjemahan Baku") },
                    placeholder = { Text("Contoh: Sihir") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("glossary_target_input")
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    FilterChip(
                        selected = newWholeWord,
                        onClick = { newWholeWord = !newWholeWord },
                        label = { Text("Whole Word") }
                    )
                    FilterChip(
                        selected = newCaseSensitive,
                        onClick = { newCaseSensitive = !newCaseSensitive },
                        label = { Text("Case Sensitive (Aa)") }
                    )
                }

                OutlinedButton(
                    onClick = {
                        viewModel.applyAllGlossaryRulesNow()
                        onDismiss()
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("apply_all_glossary_button")
                ) {
                    Icon(Icons.Default.AutoFixHigh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Terapkan Semua Aturan Sekarang")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (newSourceTerm.isNotBlank()) {
                        viewModel.addGlossaryRule(
                            sourceTerm = newSourceTerm,
                            targetTerm = newTargetTerm,
                            isCaseSensitive = newCaseSensitive,
                            wholeWord = newWholeWord
                        )
                        onDismiss()
                    }
                },
                enabled = newSourceTerm.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isDark) MaterialTheme.colorScheme.primary else PrimaryForestButton,
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .defaultMinSize(minHeight = 44.dp)
                    .testTag("add_glossary_rule_button")
            ) {
                Text("Simpan Aturan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}
