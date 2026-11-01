package com.example.ui.screens

import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SettingsSuggest
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.AppUpdateUiState
import com.example.model.ColorPreset
import com.example.model.ThemeMode
import com.example.model.ThemeSettings
import com.example.ui.components.OpenAiDirectTranslateCard
import com.example.ui.theme.ForestGreenPrimary
import com.example.ui.theme.SageHighlightFill
import com.example.viewmodel.SubtitleViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeSettingsSheet(
    themeSettings: ThemeSettings,
    viewModel: SubtitleViewModel,
    onDismiss: () -> Unit
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
            onBack = onDismiss,
            aiProviderSection = {
                OpenAiDirectTranslateCard(
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreenContent(
    themeSettings: ThemeSettings,
    appUpdateState: AppUpdateUiState,
    viewModel: SubtitleViewModel,
    onBack: () -> Unit,
    aiProviderSection: @Composable () -> Unit
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    // 0 = Umum/Semua, 1 = AI Provider, 2 = Tema, 3 = Editor, 4 = Pembaruan
    var selectedCategory by rememberSaveable { mutableIntStateOf(0) }

    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val isWideSettings = maxWidth >= 560.dp

        val categoryItems = listOf(
            Triple(0, "Umum", Icons.Outlined.Settings),
            Triple(1, "AI Provider", Icons.Outlined.Cloud),
            Triple(2, "Tema", Icons.Outlined.Palette),
            Triple(3, "Editor", Icons.Outlined.Edit),
            Triple(4, "Pembaruan", Icons.Outlined.Inventory2)
        )

        val detailPaneComposable: @Composable (Modifier) -> Unit = { paneModifier ->
            if (selectedCategory == 1) {
                // Render AI Provider card directly at full pane width without double-card nesting
                Box(modifier = paneModifier) {
                    aiProviderSection()
                }
            } else {
                Card(
                    modifier = paneModifier,
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    border = BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // 1. Tema Tampilan (Sistem, Terang, Gelap) matching Panel 6
                        if (selectedCategory == 0 || selectedCategory == 2) {
                            Text(
                                text = "Tema Tampilan",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                ThemeModeCardOption(
                                    mode = ThemeMode.SYSTEM,
                                    icon = Icons.Outlined.SettingsSuggest,
                                    isSelected = themeSettings.themeMode == ThemeMode.SYSTEM,
                                    isDark = isDark,
                                    onClick = { viewModel.updateThemeMode(ThemeMode.SYSTEM) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("theme_mode_system")
                                )
                                ThemeModeCardOption(
                                    mode = ThemeMode.LIGHT,
                                    icon = Icons.Outlined.LightMode,
                                    isSelected = themeSettings.themeMode == ThemeMode.LIGHT,
                                    isDark = isDark,
                                    onClick = { viewModel.updateThemeMode(ThemeMode.LIGHT) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("theme_mode_light")
                                )
                                ThemeModeCardOption(
                                    mode = ThemeMode.DARK,
                                    icon = Icons.Outlined.DarkMode,
                                    isSelected = themeSettings.themeMode == ThemeMode.DARK,
                                    isDark = isDark,
                                    onClick = { viewModel.updateThemeMode(ThemeMode.DARK) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("theme_mode_dark")
                                )
                            }

                            // 2. Palet Warna (5 Circular Swatches with Checkmark) matching Panel 6
                            Text(
                                text = "Palet Warna",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                ColorPreset.entries.forEach { preset ->
                                    val isSelected = !themeSettings.useDynamicColor && themeSettings.colorPreset == preset
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(CircleShape)
                                            .background(preset.swatchColor)
                                            .border(
                                                width = if (isSelected) 2.5.dp else 1.dp,
                                                color = if (isSelected) {
                                                    MaterialTheme.colorScheme.onSurface
                                                } else {
                                                    Color.Black.copy(alpha = 0.12f)
                                                },
                                                shape = CircleShape
                                            )
                                            .clickable { viewModel.updateColorPreset(preset) }
                                            .testTag("color_preset_${preset.name.lowercase()}")
                                    ) {
                                        if (isSelected) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = preset.presetName,
                                                tint = Color.White,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            if (selectedCategory == 2) {
                                // Extra AMOLED & Dynamic Color toggles when in dedicated Tema tab
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "AMOLED Pure Black",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    Switch(
                                        checked = themeSettings.pureBlackAmoled,
                                        onCheckedChange = { viewModel.updatePureBlackAmoled(it) },
                                        modifier = Modifier.testTag("amoled_pure_black_switch")
                                    )
                                }
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Material You Dynamic",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                        Switch(
                                            checked = themeSettings.useDynamicColor,
                                            onCheckedChange = { viewModel.updateUseDynamicColor(it) },
                                            modifier = Modifier.testTag("dynamic_color_switch")
                                        )
                                    }
                                }
                            }
                        }

                        // 3. Ukuran Font Segmented Bar [ A | 14 | 16 | 18 | A ] matching Panel 6
                        if (selectedCategory == 0 || selectedCategory == 3) {
                            Text(
                                text = "Ukuran Font",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )

                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerLow,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "A",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable { viewModel.updateEditorFontSizeSp(12) }
                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                            .testTag("font_size_12")
                                    )

                                    listOf(14, 16, 18).forEach { sizeSp ->
                                        val isSizeSelected = themeSettings.editorFontSizeSp == sizeSp
                                        val pillBg = if (isSizeSelected) {
                                            if (isDark) Color(0xFF214431) else SageHighlightFill
                                        } else {
                                            Color.Transparent
                                        }
                                        val pillFg = if (isSizeSelected) {
                                            if (isDark) Color(0xFF8CE0AF) else ForestGreenPrimary
                                        } else {
                                            MaterialTheme.colorScheme.onSurface
                                        }

                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = pillBg,
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(10.dp))
                                                .clickable { viewModel.updateEditorFontSizeSp(sizeSp) }
                                                .testTag("font_size_$sizeSp")
                                        ) {
                                            Text(
                                                text = "$sizeSp",
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = if (isSizeSelected) FontWeight.Bold else FontWeight.Medium,
                                                color = pillFg,
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                            )
                                        }
                                    }

                                    Text(
                                        text = "A",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable { viewModel.updateEditorFontSizeSp(18) }
                                            .padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            if (selectedCategory == 3) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    FilterChip(
                                        selected = themeSettings.useMonospaceFont,
                                        onClick = { viewModel.updateUseMonospaceFont(true) },
                                        label = { Text("Monospace") },
                                        modifier = Modifier.testTag("font_family_monospace")
                                    )
                                    FilterChip(
                                        selected = !themeSettings.useMonospaceFont,
                                        onClick = { viewModel.updateUseMonospaceFont(false) },
                                        label = { Text("Sans-Serif") },
                                        modifier = Modifier.testTag("font_family_sans")
                                    )
                                }
                            }
                        }

                        // 4. Cek Pembaruan Card matching Panel 6
                        if (selectedCategory == 0 || selectedCategory == 4) {
                            AppVersionAndUpdateSection(
                                updateState = appUpdateState,
                                isDark = isDark,
                                onCheckUpdate = { viewModel.checkAppUpdate() },
                                onDownloadLatestApk = { viewModel.downloadLatestUpdateApk() },
                                onOpenReleasePage = { viewModel.openLatestReleaseInBrowser() }
                            )
                        }
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Top Header: [ ← ] Pengaturan
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Kembali"
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Pengaturan",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            if (isWideSettings) {
                // Wide Screen Two-Column Layout matching Panel 6: Left Category Rail (148.dp) + Right Content Pane
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Column(
                        modifier = Modifier.width(148.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        categoryItems.forEach { (idx, label, icon) ->
                            SettingsCategoryPill(
                                icon = icon,
                                label = label,
                                selected = selectedCategory == idx,
                                isDark = isDark,
                                fillWidth = true,
                                onClick = { selectedCategory = idx }
                            )
                        }
                    }

                    detailPaneComposable(Modifier.weight(1f))
                }
            } else {
                // Compact Screen: Horizontal Scrollable Category Pills on Top + 100% Full-Width Detail Pane Below
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    categoryItems.forEach { (idx, label, icon) ->
                        SettingsCategoryPill(
                            icon = icon,
                            label = label,
                            selected = selectedCategory == idx,
                            isDark = isDark,
                            fillWidth = false,
                            onClick = { selectedCategory = idx }
                        )
                    }
                }

                detailPaneComposable(Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun SettingsCategoryPill(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    isDark: Boolean,
    fillWidth: Boolean = true,
    onClick: () -> Unit
) {
    val bg = if (selected) {
        if (isDark) Color(0xFF214431) else SageHighlightFill
    } else {
        if (fillWidth) Color.Transparent else MaterialTheme.colorScheme.surfaceContainerLow
    }
    val fg = if (selected) {
        if (isDark) Color(0xFF8CE0AF) else ForestGreenPrimary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = bg,
        border = if (!fillWidth && !selected) {
            BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        } else null,
        modifier = (if (fillWidth) Modifier.fillMaxWidth() else Modifier)
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = fg,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = fg,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun ThemeModeCardOption(
    mode: ThemeMode,
    icon: ImageVector,
    isSelected: Boolean,
    isDark: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val containerColor = if (isSelected) {
        if (isDark) Color(0xFF28593E) else Color(0xFF2D6A4F)
    } else {
        MaterialTheme.colorScheme.surfaceContainerLow
    }
    val contentColor = if (isSelected) {
        Color.White
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = containerColor,
        border = if (!isSelected) {
            BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        } else null,
        modifier = modifier
            .heightIn(min = 66.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = mode.label,
                tint = contentColor,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = mode.label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = contentColor,
                maxLines = 1
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AppVersionAndUpdateSection(
    updateState: AppUpdateUiState,
    isDark: Boolean,
    onCheckUpdate: () -> Unit,
    onDownloadLatestApk: () -> Unit,
    onOpenReleasePage: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("app_version_update_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Top Row matching Panel 6: "Cek Pembaruan" & "v1.2.0 (Build 3)" + ">"
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = !updateState.isChecking, onClick = onCheckUpdate),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Cek Pembaruan",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "v${updateState.currentVersionName} (Build ${updateState.currentVersionCode})",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.testTag("settings_version_badge")
                    )
                }

                if (updateState.isChecking) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "Cek Versi Terbaru",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilledTonalButton(
                    onClick = onCheckUpdate,
                    enabled = !updateState.isChecking,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("check_app_update_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (updateState.isChecking) "Memeriksa..." else "Cek Versi Terbaru",
                        style = MaterialTheme.typography.labelMedium
                    )
                }

                OutlinedButton(
                    onClick = onOpenReleasePage,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("open_github_release_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("GitHub Release", style = MaterialTheme.typography.labelMedium)
                }
            }

            // Error Message Banner
            AnimatedVisibility(visible = !updateState.errorMessage.isNullOrBlank()) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.75f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.ErrorOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = updateState.errorMessage.orEmpty(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }

            // Up-to-date status card
            val latest = updateState.latestRelease
            AnimatedVisibility(visible = latest != null && !updateState.isUpdateAvailable && updateState.errorMessage == null) {
                if (latest != null) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("up_to_date_status_card")
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = updateState.statusMessage
                                        ?: "Sudah menggunakan versi terbaru (v${updateState.currentVersionName})",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                            if (latest.apkAsset != null) {
                                Text(
                                    text = "Aset rilis aktif: ${latest.apkAsset.name} (${latest.apkAsset.formattedSizeMb})",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.85f)
                                )
                            }
                        }
                    }
                }
            }

            // Update Available card + Direct APK Download button
            AnimatedVisibility(visible = latest != null && updateState.isUpdateAvailable) {
                if (latest != null) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("update_available_card")
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.NewReleases,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Pembaruan Tersedia: v${latest.versionName}",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }

                            if (latest.releaseNotes.isNotBlank()) {
                                Text(
                                    text = latest.releaseNotes.take(320),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }

                            Button(
                                onClick = onDownloadLatestApk,
                                enabled = !updateState.isDownloadingApk,
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isDark) MaterialTheme.colorScheme.primary else ForestGreenPrimary,
                                    contentColor = if (isDark) MaterialTheme.colorScheme.onPrimary else Color.White
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("download_update_apk_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CloudDownload,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                val apkLabel = latest.apkAsset?.let { "Download APK Terbaru (${it.formattedSizeMb})" }
                                    ?: "Unduh Pembaruan v${latest.versionName}"
                                Text(apkLabel)
                            }
                        }
                    }
                }
            }

            // Status unduhan di DownloadManager
            AnimatedVisibility(visible = !updateState.statusMessage.isNullOrBlank() && updateState.lastDownloadId != null) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.7f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = updateState.statusMessage.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }
        }
    }
}
