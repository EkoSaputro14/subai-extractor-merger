package com.example.ui.screens

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.model.ThemeSettings
import com.example.ui.components.ActionButtonKind
import com.example.ui.components.ActionButtonSpec
import com.example.ui.components.ResponsiveActionButtonGrid
import com.example.ui.components.StatusPillBadge
import com.example.ui.components.StudioSectionCard
import com.example.ui.components.StudioToggleRow
import com.example.ui.theme.subtitleEditorTextStyle
import com.example.viewmodel.SubtitleUiState
import com.example.viewmodel.SubtitleViewModel

@Composable
fun PasteTabScreen(
    uiState: SubtitleUiState,
    themeSettings: ThemeSettings,
    viewModel: SubtitleViewModel,
    isWideScreen: Boolean = false,
    modifier: Modifier = Modifier
) {
    if (isWideScreen) {
        Row(
            modifier = modifier
                .fillMaxSize()
                .imePadding()
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
                item(key = "match_tracker_card") {
                    MatchProgressTrackerCard(uiState = uiState)
                }
                item(key = "paste_actions_card") {
                    PasteBatchActionsCard(
                        uiState = uiState,
                        viewModel = viewModel
                    )
                }
            }

            LazyColumn(
                modifier = Modifier
                    .weight(1.05f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                item(key = "paste_editor_card") {
                    NumberedTranslationEditorCard(
                        uiState = uiState,
                        themeSettings = themeSettings,
                        viewModel = viewModel,
                        minHeightDp = 340
                    )
                }
            }
        }
    } else {
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .imePadding(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item(key = "match_tracker_card") {
                MatchProgressTrackerCard(uiState = uiState)
            }
            item(key = "paste_actions_card") {
                PasteBatchActionsCard(
                    uiState = uiState,
                    viewModel = viewModel
                )
            }
            item(key = "paste_editor_card") {
                NumberedTranslationEditorCard(
                    uiState = uiState,
                    themeSettings = themeSettings,
                    viewModel = viewModel,
                    minHeightDp = 260
                )
            }
        }
    }
}

@Composable
private fun MatchProgressTrackerCard(uiState: SubtitleUiState) {
    val totalCues = uiState.document.cues.size.coerceAtLeast(1)
    val matchedCount = uiState.matchedCount
    val progressTarget = (matchedCount.toFloat() / totalCues.toFloat()).coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(targetValue = progressTarget, label = "match_progress")
    val missingIds = uiState.missingCueIds
    val pct = ((matchedCount * 100f) / totalCues).toInt()

    StudioSectionCard(
        title = "$matchedCount / ${uiState.document.cues.size} Baris Cocok",
        subtitle = "Pelacak pemetaan nomor urut terjemahan AI ke indeks timestamp asli",
        icon = if (missingIds.isEmpty()) Icons.Default.CheckCircle else Icons.Default.WarningAmber,
        iconTint = if (missingIds.isEmpty()) {
            MaterialTheme.colorScheme.secondary
        } else {
            MaterialTheme.colorScheme.primary
        },
        headerTrailing = {
            StatusPillBadge(
                text = "$pct% Terpetakan",
                modifier = Modifier.testTag("match_progress_label")
            )
        }
    ) {
        LinearProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = MaterialTheme.colorScheme.secondary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )

        if (missingIds.isEmpty() && uiState.document.cues.isNotEmpty()) {
            Text(
                text = "Semua nomor urut dialog berhasil dipetakan ke timestamp asli tanpa pergeseran.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary
            )
        } else if (missingIds.isNotEmpty()) {
            val previewMissing = missingIds.take(15).joinToString(", ") { "#$it" }
            val moreSuffix = if (missingIds.size > 15) " ... (+${missingIds.size - 15} lainnya)" else ""
            Text(
                text = "Nomor belum diterjemahkan (${missingIds.size} baris): $previewMissing$moreSuffix",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.testTag("missing_ids_warning_text")
            )
        }
    }
}

@Composable
private fun PasteBatchActionsCard(
    uiState: SubtitleUiState,
    viewModel: SubtitleViewModel
) {
    val context = LocalContext.current

    StudioSectionCard(
        title = "Aksi Tempel & Penggabungan Batch Bertahap",
        subtitle = "Tempel hasil terjemahan baru atau gabungkan batch berikutnya tanpa menimpa batch sebelumnya",
        icon = Icons.Default.ContentPaste,
        iconTint = MaterialTheme.colorScheme.primary
    ) {
        ResponsiveActionButtonGrid(
            buttons = listOf(
                ActionButtonSpec(
                    label = "Paste Baru",
                    icon = Icons.Default.ContentPaste,
                    testTag = "paste_replace_button",
                    kind = ActionButtonKind.PRIMARY,
                    onClick = {
                        val clip = readClipboardText(context)
                        viewModel.pasteOrAppendBatch(clip, appendMode = false)
                    }
                ),
                ActionButtonSpec(
                    label = "Tambah Batch",
                    icon = Icons.Default.AddCircleOutline,
                    testTag = "paste_append_button",
                    kind = ActionButtonKind.TONAL,
                    onClick = {
                        val clip = readClipboardText(context)
                        viewModel.pasteOrAppendBatch(clip, appendMode = true)
                    }
                ),
                ActionButtonSpec(
                    label = "Muat Demo AI",
                    icon = Icons.Default.AutoFixHigh,
                    testTag = "fill_demo_ai_button",
                    kind = ActionButtonKind.OUTLINED,
                    onClick = { viewModel.fillDemoAiTranslations() }
                ),
                ActionButtonSpec(
                    label = "Kosongkan",
                    icon = Icons.Default.DeleteSweep,
                    testTag = "clear_pasted_button",
                    kind = ActionButtonKind.OUTLINED,
                    onClick = { viewModel.clearPastedTranslations() }
                )
            )
        )

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

        StudioToggleRow(
            title = "Terapkan Kamus Otomatis (Post-Translation)",
            description = "Menjalankan seluruh aturan kamus aktif secara otomatis saat menempelkan teks maupun sebelum Merge.",
            checked = uiState.autoApplyGlossaryBeforeMerge,
            onCheckedChange = { viewModel.setAutoApplyGlossaryBeforeMerge(it) },
            testTag = "auto_apply_glossary_switch"
        )
    }
}

@Composable
private fun NumberedTranslationEditorCard(
    uiState: SubtitleUiState,
    themeSettings: ThemeSettings,
    viewModel: SubtitleViewModel,
    minHeightDp: Int
) {
    StudioSectionCard(
        title = "Editor Teks Terjemahan Bernomor",
        subtitle = "Mendukung format [1], 1., 1 |, maupun 1: secara otomatis melalui Smart Regex",
        icon = Icons.Default.EditNote,
        iconTint = MaterialTheme.colorScheme.secondary
    ) {
        OutlinedTextField(
            value = uiState.pastedEditorText,
            onValueChange = { viewModel.updatePastedEditorText(it) },
            textStyle = subtitleEditorTextStyle(themeSettings),
            placeholder = {
                Text(
                    "[1] Tempel hasil terjemahan AI di sini...\n[2] Baris kedua // dengan pemisah baris ganda",
                    style = subtitleEditorTextStyle(themeSettings)
                )
            },
            minLines = 10,
            maxLines = 22,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = minHeightDp.dp)
                .testTag("pasted_ai_editor_input")
        )
    }
}

private fun readClipboardText(context: Context): String {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    val item = clipboard?.primaryClip?.getItemAt(0)
    return item?.coerceToText(context)?.toString() ?: ""
}
