package com.example.ui.screens

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.ColorPreset
import com.example.model.EditorFontSizeOption
import com.example.model.ThemeMode
import com.example.model.ThemeSettings
import com.example.viewmodel.SubtitleViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ThemeSettingsSheet(
    themeSettings: ThemeSettings,
    viewModel: SubtitleViewModel,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Palette,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Kustomisasi Tema Warna & Tampilan Editor",
                    style = MaterialTheme.typography.titleLarge
                )
            }

            // 1. Mode Pencahayaan
            Text(
                text = "Mode Pencahayaan (Theme Mode):",
                style = MaterialTheme.typography.titleSmall
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                ThemeMode.entries.forEach { mode ->
                    FilterChip(
                        selected = themeSettings.themeMode == mode,
                        onClick = { viewModel.updateThemeMode(mode) },
                        label = { Text(mode.label) },
                        modifier = Modifier.testTag("theme_mode_${mode.name.lowercase()}")
                    )
                }
            }

            // AMOLED Pure Black
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "AMOLED Pure Black (#000000)",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        text = "Latar utama hitam pekat saat Mode Gelap aktif untuk kenyamanan mata.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = themeSettings.pureBlackAmoled,
                    onCheckedChange = { viewModel.updatePureBlackAmoled(it) },
                    modifier = Modifier.testTag("amoled_pure_black_switch")
                )
            }

            // Material You Dynamic Color (Android 12+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Material You Dynamic Color",
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            text = "Gunakan skema warna otomatis dari wallpaper perangkat Android 12+.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = themeSettings.useDynamicColor,
                        onCheckedChange = { viewModel.updateUseDynamicColor(it) },
                        modifier = Modifier.testTag("dynamic_color_switch")
                    )
                }
            }

            HorizontalDivider()

            // 2. 5 Preset Palet Warna Bawaan
            Text(
                text = "Pilihan Palet Aksen Warna (5 Preset Studio):",
                style = MaterialTheme.typography.titleSmall
            )

            ColorPreset.entries.forEach { preset ->
                val isSelected = !themeSettings.useDynamicColor && themeSettings.colorPreset == preset
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { viewModel.updateColorPreset(preset) }
                        .testTag("color_preset_${preset.name.lowercase()}")
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
                            modifier = Modifier.weight(1f)
                        ) {
                            // Dual swatch preview (Primary + Secondary)
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Box(
                                    modifier = Modifier
                                        .size(22.dp)
                                        .clip(CircleShape)
                                        .background(preset.darkPrimary)
                                        .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                                )
                                Box(
                                    modifier = Modifier
                                        .size(22.dp)
                                        .clip(CircleShape)
                                        .background(preset.darkSecondary)
                                        .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = preset.presetName,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = preset.subtitleDesc,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Dipilih",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            HorizontalDivider()

            // 3. Kustomisasi Tipografi Editor Subtitle
            Text(
                text = "Ukuran Font Teks Ekstraksi & Editor Merge:",
                style = MaterialTheme.typography.titleSmall
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                EditorFontSizeOption.entries.forEach { sizeOption ->
                    FilterChip(
                        selected = themeSettings.editorFontSizeSp == sizeOption.sizeSp,
                        onClick = { viewModel.updateEditorFontSizeSp(sizeOption.sizeSp) },
                        label = { Text(sizeOption.label) },
                        modifier = Modifier.testTag("font_size_${sizeOption.sizeSp}")
                    )
                }
            }

            Text(
                text = "Jenis Huruf Pratinjau Editor:",
                style = MaterialTheme.typography.titleSmall
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = themeSettings.useMonospaceFont,
                    onClick = { viewModel.updateUseMonospaceFont(true) },
                    label = { Text("Monospace (Kode & Tag .ASS)") },
                    modifier = Modifier.testTag("font_family_monospace")
                )
                FilterChip(
                    selected = !themeSettings.useMonospaceFont,
                    onClick = { viewModel.updateUseMonospaceFont(false) },
                    label = { Text("Sans-Serif (Dialog Alami)") },
                    modifier = Modifier.testTag("font_family_sans")
                )
            }

            Spacer(modifier = Modifier.height(18.dp))
        }
    }
}
