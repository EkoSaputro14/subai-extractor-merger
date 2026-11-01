package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.FindReplace
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.example.model.FindMatchItem
import com.example.model.GlossaryRule
import com.example.model.ThemeSettings
import com.example.ui.components.ActionButtonKind
import com.example.ui.components.ActionButtonSpec
import com.example.ui.components.ResponsiveActionButtonGrid
import com.example.ui.components.StatusPillBadge
import com.example.ui.components.StudioSectionCard
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

    if (isWideScreen) {
        // Dual-Pane Layout: Left pane for controls, Right pane for virtualized LazyColumn list
        Column(
            modifier = modifier
                .fillMaxSize()
                .imePadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SubTabSelectorCard(
                selectedSubTab = uiState.glossarySubTabIndex,
                enabledGlossaryCount = glossaryRules.count { it.isEnabled },
                onSelectSubTab = { viewModel.setGlossarySubTab(it) }
            )

            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    if (uiState.glossarySubTabIndex == 0) {
                        item(key = "find_controls_card") {
                            QuickFindReplaceControlsCard(
                                uiState = uiState,
                                liveMatchesCount = liveMatches.size,
                                totalOccurrences = totalOccurrences,
                                viewModel = viewModel
                            )
                        }
                    } else {
                        item(key = "add_glossary_card") {
                            AddGlossaryRuleCard(viewModel = viewModel)
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
                        item(key = "matches_header") {
                            StatusPillBadge(
                                text = "Pratinjau Dampak Penggantian (${liveMatches.size} Baris Terdampak)",
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                        items(
                            items = liveMatches,
                            key = { "match_${it.cueId}" }
                        ) { matchItem ->
                            FindMatchPreviewItemCard(
                                matchItem = matchItem,
                                themeSettings = themeSettings
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
        }
    } else {
        // Single-Column Virtualized LazyColumn Layout
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .imePadding(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(key = "subtab_selector") {
                SubTabSelectorCard(
                    selectedSubTab = uiState.glossarySubTabIndex,
                    enabledGlossaryCount = glossaryRules.count { it.isEnabled },
                    onSelectSubTab = { viewModel.setGlossarySubTab(it) }
                )
            }

            if (uiState.glossarySubTabIndex == 0) {
                item(key = "find_controls_card") {
                    QuickFindReplaceControlsCard(
                        uiState = uiState,
                        liveMatchesCount = liveMatches.size,
                        totalOccurrences = totalOccurrences,
                        viewModel = viewModel
                    )
                }

                if (liveMatches.isNotEmpty()) {
                    item(key = "matches_section_title") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Pratinjau Baris Terdampak",
                                style = MaterialTheme.typography.titleSmall
                            )
                            StatusPillBadge(
                                text = "${liveMatches.size} Baris • $totalOccurrences Kecocokan",
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }

                    items(
                        items = liveMatches,
                        key = { "match_${it.cueId}" }
                    ) { matchItem ->
                        FindMatchPreviewItemCard(
                            matchItem = matchItem,
                            themeSettings = themeSettings
                        )
                    }
                }
            } else {
                item(key = "add_glossary_card") {
                    AddGlossaryRuleCard(viewModel = viewModel)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SubTabSelectorCard(
    selectedSubTab: Int,
    enabledGlossaryCount: Int,
    onSelectSubTab: (Int) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
    ) {
        SecondaryTabRow(
            selectedTabIndex = selectedSubTab,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Tab(
                selected = selectedSubTab == 0,
                onClick = { onSelectSubTab(0) },
                text = { Text("Quick Find & Replace") },
                icon = { Icon(Icons.Default.FindReplace, contentDescription = null) },
                modifier = Modifier
                    .defaultMinSize(minHeight = 48.dp)
                    .testTag("subtab_quick_find_replace")
            )
            Tab(
                selected = selectedSubTab == 1,
                onClick = { onSelectSubTab(1) },
                text = { Text("Kamus Glosarium ($enabledGlossaryCount)") },
                icon = { Icon(Icons.Default.Book, contentDescription = null) },
                modifier = Modifier
                    .defaultMinSize(minHeight = 48.dp)
                    .testTag("subtab_glossary_rules")
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun QuickFindReplaceControlsCard(
    uiState: SubtitleUiState,
    liveMatchesCount: Int,
    totalOccurrences: Int,
    viewModel: SubtitleViewModel
) {
    StudioSectionCard(
        title = "Pencarian & Penggantian Instan Pra-Merge",
        subtitle = "Cari & ganti istilah pada teks terjemahan tanpa merusak tag atau timestamp",
        icon = Icons.Default.FindReplace,
        iconTint = MaterialTheme.colorScheme.primary
    ) {
        OutlinedTextField(
            value = uiState.findQuery,
            onValueChange = { viewModel.setFindQuery(it) },
            label = { Text("Cari Teks (Find)") },
            placeholder = { Text("Contoh: Shibuya Ward atau Onii-chan") },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("find_query_input")
        )

        OutlinedTextField(
            value = uiState.replaceWith,
            onValueChange = { viewModel.setReplaceWith(it) },
            label = { Text("Ganti Dengan (Replace With)") },
            placeholder = { Text("Contoh: Distrik Shibuya atau Kakak") },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("replace_with_input")
        )

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            FilterChip(
                selected = uiState.findCaseSensitive,
                onClick = { viewModel.toggleFindCaseSensitive() },
                label = { Text("Case Sensitive (Aa)") },
                modifier = Modifier.testTag("filter_case_sensitive_chip")
            )
            FilterChip(
                selected = uiState.findWholeWord,
                onClick = { viewModel.toggleFindWholeWord() },
                label = { Text("Whole Word (\\bKata\\b)") },
                modifier = Modifier.testTag("filter_whole_word_chip")
            )
            FilterChip(
                selected = uiState.findUseRegex,
                onClick = { viewModel.toggleFindUseRegex() },
                label = { Text("Gunakan Regex (.*)") },
                modifier = Modifier.testTag("filter_regex_chip")
            )
        }

        if (uiState.document.format.isAssFamily) {
            val styles = uiState.document.availableStyles
            val actors = uiState.document.availableActors
            if (styles.isNotEmpty()) {
                Text(
                    text = "Filter Berdasarkan Style .ASS:",
                    style = MaterialTheme.typography.labelLarge
                )
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

            if (actors.isNotEmpty()) {
                Text(
                    text = "Filter Berdasarkan Karakter / Actor .ASS:",
                    style = MaterialTheme.typography.labelLarge
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = uiState.selectedActorFilter == "ALL",
                        onClick = { viewModel.setSelectedActorFilter("ALL") },
                        label = { Text("Semua Karakter") }
                    )
                    actors.forEach { actorName ->
                        FilterChip(
                            selected = uiState.selectedActorFilter == actorName,
                            onClick = { viewModel.setSelectedActorFilter(actorName) },
                            label = { Text(actorName) },
                            modifier = Modifier.testTag("actor_filter_$actorName")
                        )
                    }
                }
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

        Surface(
            color = if (totalOccurrences > 0) {
                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = if (uiState.findQuery.isBlank()) {
                    "Ketik kata kunci pada kolom Cari Teks untuk melihat pratinjau kecocokan."
                } else {
                    "Ditemukan $totalOccurrences kecocokan pada $liveMatchesCount baris dialog"
                },
                style = MaterialTheme.typography.labelLarge,
                color = if (totalOccurrences > 0) {
                    MaterialTheme.colorScheme.onSecondaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier
                    .padding(horizontal = 12.dp, vertical = 10.dp)
                    .testTag("live_match_count_text")
            )
        }

        ResponsiveActionButtonGrid(
            buttons = listOf(
                ActionButtonSpec(
                    label = "Replace All ($totalOccurrences)",
                    icon = Icons.Default.FindReplace,
                    testTag = "replace_all_button",
                    kind = ActionButtonKind.PRIMARY,
                    enabled = liveMatchesCount > 0,
                    onClick = { viewModel.replaceAllMatches() }
                ),
                ActionButtonSpec(
                    label = "Replace Next",
                    icon = Icons.Default.PlayArrow,
                    testTag = "replace_next_button",
                    kind = ActionButtonKind.TONAL,
                    enabled = liveMatchesCount > 0,
                    onClick = { viewModel.replaceNextMatch() }
                ),
                ActionButtonSpec(
                    label = "Undo Replace",
                    icon = Icons.AutoMirrored.Filled.Undo,
                    testTag = "undo_replace_button",
                    kind = ActionButtonKind.OUTLINED,
                    enabled = uiState.undoTranslationsSnapshot != null,
                    onClick = { viewModel.undoLastReplace() }
                )
            )
        )
    }
}

@Composable
private fun FindMatchPreviewItemCard(
    matchItem: FindMatchItem,
    themeSettings: ThemeSettings
) {
    val highlightBg = MaterialTheme.colorScheme.primary.copy(alpha = 0.32f)
    val highlightFg = MaterialTheme.colorScheme.primary

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
                StatusPillBadge(text = "Baris #${matchItem.cueId}")
                val metaBadge = listOfNotNull(
                    matchItem.style.takeIf { it.isNotBlank() },
                    matchItem.actor.takeIf { it.isNotBlank() }
                ).joinToString(" • ")
                if (metaBadge.isNotEmpty()) {
                    Text(
                        text = metaBadge,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

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
                            append(src.substring(range.first, endExclusive))
                        }
                    }
                    cursor = endExclusive
                }
                if (cursor < src.length) {
                    append(src.substring(cursor))
                }
            }

            Text(
                text = highlightedCurrent,
                style = subtitleEditorTextStyle(themeSettings)
            )

            Text(
                text = "→ ${matchItem.previewReplacedText}",
                style = subtitleEditorTextStyle(themeSettings),
                color = MaterialTheme.colorScheme.secondary,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AddGlossaryRuleCard(viewModel: SubtitleViewModel) {
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
            shape = RoundedCornerShape(12.dp),
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
            shape = RoundedCornerShape(12.dp),
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
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
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
