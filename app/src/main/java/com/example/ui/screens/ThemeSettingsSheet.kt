package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.BuildConfig
import com.example.data.AppUpdateUiState
import com.example.domain.SubtitleParserAndMerger
import com.example.model.ThemeSettings
import com.example.ui.theme.PrimaryForestButton
import com.example.ui.theme.SageHighlightFill
import com.example.viewmodel.SubtitleViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeSettingsSheet(
    themeSettings: ThemeSettings,
    viewModel: SubtitleViewModel,
    onDismiss: () -> Unit,
    onReplayOnboarding: (() -> Unit)? = null
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val appUpdateState by viewModel.appUpdateState.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val aiSettings by viewModel.aiSettings.collectAsStateWithLifecycle()
    val glossaryRules by viewModel.glossaryRules.collectAsStateWithLifecycle()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.background
    ) {
        SettingsScreenContent(
            themeSettings = themeSettings,
            appUpdateState = appUpdateState,
            viewModel = viewModel,
            onReplayOnboarding = onReplayOnboarding,
            aiProviderSection = {
                AiProviderDetailScreenContent(
                    uiState = uiState,
                    aiSettings = aiSettings,
                    glossaryRules = glossaryRules,
                    themeSettings = themeSettings,
                    viewModel = viewModel
                )
            }
        )
    }
}

@Composable
fun SettingsScreenContent(
    themeSettings: ThemeSettings,
    appUpdateState: AppUpdateUiState,
    viewModel: SubtitleViewModel,
    onReplayOnboarding: (() -> Unit)? = null,
    selectedCategoryState: Int? = null,
    onCategorySelected: ((Int) -> Unit)? = null,
    aiProviderSection: @Composable () -> Unit
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val glossaryRules by viewModel.glossaryRules.collectAsStateWithLifecycle()
    val activeGlossaryCount = glossaryRules.count { it.isEnabled }

    // 0 = Umum (Menu + Theme/Update), 1 = AI Provider, 2 = Tema & Tampilan, 3 = Editor, 4 = Pembaruan, 5 = Tentang
    var internalSelectedCategory by rememberSaveable { mutableIntStateOf(0) }
    val selectedCategory = selectedCategoryState ?: internalSelectedCategory
    val updateCategory: (Int) -> Unit = { newCat ->
        internalSelectedCategory = newCat
        onCategorySelected?.invoke(newCat)
    }

    // Full-screen overlays for Aturan Ekstraksi & Prompt Terjemahan when accessed from Settings -> Editor
    var showExtractionFullScreen by rememberSaveable { mutableStateOf(false) }
    var showPromptFullScreen by rememberSaveable { mutableStateOf(false) }

    if (showExtractionFullScreen) {
        val previewExtractedLines = uiState.activeBatchCues.joinToString("\n") { cue ->
            SubtitleParserAndMerger.formatCueForExtraction(
                cue = cue,
                separatorMode = uiState.separatorMode,
                customSeparator = uiState.customSeparator,
                protectAssTags = uiState.protectAssTags,
                indexFormatMode = uiState.indexFormatMode
            )
        }
        Dialog(
            onDismissRequest = { showExtractionFullScreen = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            ExtractionRulesFullScreen(
                uiState = uiState,
                themeSettings = themeSettings,
                activeGlossaryCount = activeGlossaryCount,
                previewExtractedLines = previewExtractedLines,
                viewModel = viewModel,
                onBack = { showExtractionFullScreen = false },
                onSwitchToPromptScreen = {
                    showExtractionFullScreen = false
                    showPromptFullScreen = true
                }
            )
        }
    }

    if (showPromptFullScreen) {
        Dialog(
            onDismissRequest = { showPromptFullScreen = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            TranslationPromptFullScreen(
                uiState = uiState,
                themeSettings = themeSettings,
                activeGlossaryCount = activeGlossaryCount,
                viewModel = viewModel,
                onBack = { showPromptFullScreen = false },
                onSwitchToExtractionScreen = {
                    showPromptFullScreen = false
                    showExtractionFullScreen = true
                }
            )
        }
    }

    // Back from a dedicated sub-screen (1..5) returns to 0 (Pengaturan Menu)
    BackHandler(enabled = selectedCategory != 0) {
        updateCategory(0)
    }

    val categoryItems = listOf(
        Triple(0, "Umum", Icons.Outlined.Settings),
        Triple(1, "AI Provider", Icons.Default.AutoAwesome),
        Triple(2, "Tema & Tampilan", Icons.Outlined.Palette),
        Triple(3, "Editor", Icons.Outlined.Edit),
        Triple(4, "Pembaruan", Icons.Outlined.Sync),
        Triple(5, "Tentang", Icons.Outlined.Info)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .padding(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        when (selectedCategory) {
            1 -> {
                // Dedicated Screen 2A: AI Provider
                aiProviderSection()
            }

            2 -> {
                // Dedicated Screen 2B: Tema & Tampilan
                ThemeAppearanceDetailScreenContent(
                    themeSettings = themeSettings,
                    viewModel = viewModel
                )
            }

            4 -> {
                // Dedicated Screen 2C: Pembaruan Aplikasi
                AppUpdateDetailScreenContent(
                    updateState = appUpdateState,
                    onCheckUpdate = { viewModel.checkAppUpdate() },
                    onDownloadLatestApk = { viewModel.downloadLatestUpdateApk() },
                    onOpenReleasePage = { viewModel.openLatestReleaseInBrowser() }
                )
            }

            else -> {
                // Screen 10 of the 10-screen Reference Design:
                // 6 Vertical Category Cards ("Umum" highlighted in soft sage #D7ECD9)
                categoryItems.forEach { (idx, label, icon) ->
                    SettingsCategoryCardRow(
                        icon = icon,
                        label = label,
                        selected = selectedCategory == idx,
                        isDark = isDark,
                        testTag = "settings_category_$idx",
                        onClick = { updateCategory(idx) }
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                if (selectedCategory == 0) {
                    // Umum: Full Theme & Appearance + App Update sections accessible right below the 6 category rows
                    ThemeAppearanceDetailScreenContent(
                        themeSettings = themeSettings,
                        viewModel = viewModel
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    AppUpdateDetailScreenContent(
                        updateState = appUpdateState,
                        onCheckUpdate = { viewModel.checkAppUpdate() },
                        onDownloadLatestApk = { viewModel.downloadLatestUpdateApk() },
                        onOpenReleasePage = { viewModel.openLatestReleaseInBrowser() }
                    )
                } else if (selectedCategory == 3) {
                    // Editor: Quick launchers for Screen 1A (Aturan Ekstraksi) & Screen 1B (Prompt Terjemahan) + Typography
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
                            Text(
                                text = "Aturan Ekstraksi & Prompt Terjemahan",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = { showExtractionFullScreen = true },
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = PrimaryForestButton,
                                        contentColor = Color.White
                                    ),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.size(6.dp))
                                    Text("Aturan Ekstraksi", maxLines = 1)
                                }
                                OutlinedButton(
                                    onClick = { showPromptFullScreen = true },
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.size(6.dp))
                                    Text("Prompt AI", maxLines = 1)
                                }
                            }
                        }
                    }

                    ThemeAppearanceDetailScreenContent(
                        themeSettings = themeSettings,
                        viewModel = viewModel
                    )
                } else if (selectedCategory == 5) {
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
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "SubAI Extractor & Merger",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Versi v${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE})\nAlur kerja lengkap ekstraksi, terjemahan AI, kamus glosarium, merge bilingual, dan antrean batch subtitle (.ASS, .SSA, .SRT, .VTT).",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (onReplayOnboarding != null) {
                                OutlinedButton(
                                    onClick = onReplayOnboarding,
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Tampilkan Layar Splash & Onboarding")
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
private fun SettingsCategoryCardRow(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    isDark: Boolean,
    testTag: String,
    onClick: () -> Unit
) {
    val bg = if (selected) {
        if (isDark) Color(0xFF214431) else SageHighlightFill
    } else {
        if (isDark) MaterialTheme.colorScheme.surface else Color.White
    }
    val fg = if (selected) {
        if (isDark) Color(0xFF8CE0AF) else Color(0xFF132F23)
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = bg,
        border = BorderStroke(
            width = 1.dp,
            color = if (selected) {
                PrimaryForestButton.copy(alpha = 0.25f)
            } else if (isDark) {
                MaterialTheme.colorScheme.outlineVariant
            } else {
                Color(0xFFE4EAE5)
            }
        ),
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = fg,
                    modifier = Modifier.size(21.dp)
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                    color = fg
                )
            }

            if (!selected) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
