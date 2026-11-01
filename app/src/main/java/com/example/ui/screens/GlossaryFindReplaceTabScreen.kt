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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapHoriz
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
import androidx.compose.material3.Text
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
import com.example.ui.components.ActionButtonKind
import com.example.ui.components.ActionButtonSpec
import com.example.ui.components.ResponsiveActionButtonGrid
import com.example.ui.components.StudioSectionCard
import com.example.ui.theme.ForestGreenPrimary
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

    Column(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Underline Tab Header matching Panel 4 ("Find & Replace" | "Kamus (Glosarium)")
        SubTabSelectorCard(
            selectedSubTab = uiState.glossarySubTabIndex,
            enabledGlossaryCount = glossaryRules.count { it.isEnabled },
            onSelectSubTab = { viewModel.setGlossarySubTab(it) }
        )

        if (isWideScreen) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    if (uiState.glossarySubTabIndex == 0) {
                        item(key = "find_controls_card") {
                            QuickFindReplaceControlsCard(
                                uiState = uiState,
                                liveMatchesCount = liveMatches.size,
                                totalOccurrences = totalOccurrences,
                                isDark = isDark,
                                viewModel = viewModel
                            )
                        }
                    } else {
                        item(key = "add_glossary_card") {
                            AddGlossaryRuleCard(isDark = isDark, viewModel = viewModel)
                        }
                        item(key = "apply_glossary_card") {
                            ApplyGlossaryBatchCard(
                                uiState = uiState,
                                totalRules = glossaryRules.size,
                                viewModel = viewModel
                            )
                        }
                    }
                }

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    if (uiState.glossarySubTabIndex == 0) {
                        item(key = "results_container_card") {
                            FindResultsListCard(
                                liveMatches = liveMatches,
                                totalOccurrences = totalOccurrences,
                                uiState = uiState,
                                themeSettings = themeSettings,
                                isDark = isDark,
                                viewModel = viewModel
                            )
                        }
                    } else {
                        items(
                            items = glossaryRules,
                            key = { it.id }
                        ) { rule ->
                            GlossaryRuleItemCard(
                                rule = rule,
                                viewModel = viewModel
                            )
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                if (uiState.glossarySubTabIndex == 0) {
                    item(key = "find_controls_card") {
                        QuickFindReplaceControlsCard(
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
                            totalOccurrences = totalOccurrences,
                            uiState = uiState,
                            themeSettings = themeSettings,
                            isDark = isDark,
                            viewModel = viewModel
                        )
                    }
                } else {
                    item(key = "add_glossary_card") {
                        AddGlossaryRuleCard(isDark = isDark, viewModel = viewModel)
                    }
                    item(key = "apply_glossary_card") {
                        ApplyGlossaryBatchCard(
                            uiState = uiState,
                            totalRules = glossaryRules.size,
                            viewModel = viewModel
                        )
                    }
                    items(
                        items = glossaryRules,
                        key = { it.id }
                    ) { rule ->
                        GlossaryRuleItemCard(
                            rule = rule,
                            viewModel = viewModel
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SubTabSelectorCard(
    selectedSubTab: Int,
    enabledGlossaryCount: Int,
    onSelectSubTab: (Int) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f))
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp, start = 16.dp, end = 16.dp),
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
                    text = if (enabledGlossaryCount > 0) {
                        "Kamus (Glosarium $enabledGlossaryCount)"
                    } else {
                        "Kamus (Glosarium)"
                    },
                    selected = selectedSubTab == 1,
                    onClick = { onSelectSubTab(1) },
                    testTag = "subtab_glossary_rules",
                    modifier = Modifier.weight(1f)
                )
            }
        }
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
                    if (selected) MaterialTheme.colorScheme.primary else Color.Transparent
                )
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun QuickFindReplaceControlsCard(
    uiState: SubtitleUiState,
    liveMatchesCount: Int,
    totalOccurrences: Int,
    isDark: Boolean,
    viewModel: SubtitleViewModel
) {
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
            // Pill Search Input (🔍) matching Panel 4
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
                placeholder = { Text("Cari teks (contoh: Senpai)") },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("find_query_input")
            )

            // Pill Replace Input (⇄) matching Panel 4
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
                placeholder = { Text("Ganti dengan (contoh: Senpai / Kakak)") },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("replace_with_input")
            )

            // Toggle Switches FlowRow: Case sensitive + Regex + Whole Word (responsive on compact widths)
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Switch(
                        checked = uiState.findCaseSensitive,
                        onCheckedChange = { viewModel.toggleFindCaseSensitive() },
                        modifier = Modifier.testTag("filter_case_sensitive_chip")
                    )
                    Text(
                        text = "Case sensitive",
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Switch(
                        checked = uiState.findUseRegex,
                        onCheckedChange = { viewModel.toggleFindUseRegex() },
                        modifier = Modifier.testTag("filter_regex_chip")
                    )
                    Text(
                        text = "Regex",
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                FilterChip(
                    selected = uiState.findWholeWord,
                    onClick = { viewModel.toggleFindWholeWord() },
                    label = { Text("Kata Utuh", style = MaterialTheme.typography.labelSmall) },
                    modifier = Modifier.testTag("filter_whole_word_chip")
                )
            }

            // Full-Width Dark Forest Green Pill Button: [ ⇄ Ganti Semua ] matching Panel 4
            val primaryColor = if (isDark) MaterialTheme.colorScheme.primary else ForestGreenPrimary
            val onPrimaryColor = if (isDark) MaterialTheme.colorScheme.onPrimary else Color.White

            Button(
                onClick = { viewModel.replaceAllMatches() },
                enabled = liveMatchesCount > 0,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = primaryColor,
                    contentColor = onPrimaryColor
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
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
                            shape = RoundedCornerShape(12.dp),
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
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("undo_replace_button")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Undo")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FindResultsListCard(
    liveMatches: List<FindMatchItem>,
    totalOccurrences: Int,
    uiState: SubtitleUiState,
    themeSettings: ThemeSettings,
    isDark: Boolean,
    viewModel: SubtitleViewModel
) {
    var showStyleActorFilters by rememberSaveable { mutableStateOf(false) }

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

                if (uiState.document.format.isAssFamily) {
                    IconButton(
                        onClick = { showStyleActorFilters = !showStyleActorFilters },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = "Filter Style & Actor"
                        )
                    }
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
                        "Ketik kata kunci di atas (mis. nama karakter atau istilah) untuk menyorot hasil pencarian."
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
    // Yellow highlight matching Panel 4 of Reference Design ([12] ... Senpai ...)
    val highlightBg = if (isDark) Color(0xFF7A6414) else Color(0xFFFFF199)
    val highlightFg = if (isDark) Color(0xFFFFF8D6) else Color(0xFF1F1A00)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
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
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AddGlossaryRuleCard(
    isDark: Boolean,
    viewModel: SubtitleViewModel
) {
    var newSourceTerm by rememberSaveable { mutableStateOf("") }
    var newTargetTerm by rememberSaveable { mutableStateOf("") }
    var newCaseSensitive by rememberSaveable { mutableStateOf(false) }
    var newWholeWord by rememberSaveable { mutableStateOf(true) }

    StudioSectionCard(
        title = "Tambah Istilah / Nama Karakter ke Kamus",
        subtitle = "Disimpan permanen secara lokal untuk dipakai pada episode berikutnya",
        icon = Icons.Default.Book,
        iconTint = MaterialTheme.colorScheme.primary
    ) {
        OutlinedTextField(
            value = newSourceTerm,
            onValueChange = { newSourceTerm = it },
            label = { Text("Istilah Asli / Salah Terjemah") },
            placeholder = { Text("Contoh: Shibuya Ward") },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("glossary_source_input")
        )
        OutlinedTextField(
            value = newTargetTerm,
            onValueChange = { newTargetTerm = it },
            label = { Text("Istilah Baku Tujuan") },
            placeholder = { Text("Contoh: Distrik Shibuya") },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
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
                label = { Text("Whole Word (Kata Utuh)") }
            )
            FilterChip(
                selected = newCaseSensitive,
                onClick = { newCaseSensitive = !newCaseSensitive },
                label = { Text("Case Sensitive (Aa)") }
            )
        }

        Button(
            onClick = {
                viewModel.addGlossaryRule(
                    sourceTerm = newSourceTerm,
                    targetTerm = newTargetTerm,
                    isCaseSensitive = newCaseSensitive,
                    wholeWord = newWholeWord
                )
                newSourceTerm = ""
                newTargetTerm = ""
            },
            enabled = newSourceTerm.isNotBlank(),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isDark) MaterialTheme.colorScheme.primary else ForestGreenPrimary,
                contentColor = if (isDark) MaterialTheme.colorScheme.onPrimary else Color.White
            ),
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 48.dp)
                .testTag("add_glossary_rule_button")
        ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Simpan Istilah ke Kamus")
        }
    }
}

@Composable
private fun ApplyGlossaryBatchCard(
    uiState: SubtitleUiState,
    totalRules: Int,
    viewModel: SubtitleViewModel
) {
    StudioSectionCard(
        title = "Eksekusi Kamus ($totalRules Aturan Tersimpan)",
        subtitle = "Terapkan seluruh aturan kamus yang aktif secara otomatis ke teks terjemahan",
        icon = Icons.Default.AutoFixHigh,
        iconTint = MaterialTheme.colorScheme.secondary
    ) {
        ResponsiveActionButtonGrid(
            buttons = buildList {
                add(
                    ActionButtonSpec(
                        label = "Terapkan Semua ke Terjemahan",
                        icon = Icons.Default.AutoFixHigh,
                        testTag = "apply_all_glossary_button",
                        kind = ActionButtonKind.PRIMARY,
                        onClick = { viewModel.applyAllGlossaryRulesNow() }
                    )
                )
                if (uiState.undoTranslationsSnapshot != null) {
                    add(
                        ActionButtonSpec(
                            label = "Undo Perubahan",
                            icon = Icons.AutoMirrored.Filled.Undo,
                            testTag = "glossary_undo_button",
                            kind = ActionButtonKind.OUTLINED,
                            onClick = { viewModel.undoLastReplace() }
                        )
                    )
                }
            }
        )
    }
}

@Composable
private fun GlossaryRuleItemCard(
    rule: GlossaryRule,
    viewModel: SubtitleViewModel
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
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
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "\"${rule.sourceTerm}\"  →  \"${rule.targetTerm}\"",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (rule.isEnabled) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        Color.Gray
                    }
                )
                val flags = buildList {
                    if (rule.wholeWord) add("Kata Utuh")
                    if (rule.isCaseSensitive) add("Case Sensitive")
                }.joinToString(" • ")
                if (flags.isNotEmpty()) {
                    Text(
                        text = flags,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(
                    checked = rule.isEnabled,
                    onCheckedChange = { viewModel.toggleGlossaryRuleEnabled(rule.id) },
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("toggle_glossary_${rule.id}")
                )
                IconButton(
                    onClick = { viewModel.deleteGlossaryRule(rule.id) },
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("delete_glossary_${rule.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Hapus aturan glosarium",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}
