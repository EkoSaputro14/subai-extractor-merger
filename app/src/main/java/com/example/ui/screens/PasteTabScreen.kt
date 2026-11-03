package com.example.ui.screens

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.example.model.ThemeSettings
import com.example.ui.theme.EmeraldCheckGreen
import com.example.ui.theme.PrimaryForestButton
import com.example.ui.theme.SageHighlightFill
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
    val context = LocalContext.current
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val missingIds = uiState.missingCueIds

    Column(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Top Soft Sage Green Status Pill Card matching Screen 5:
        // [ ✓  120 / 120   Baris terpetakan ]
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = if (isDark) Color(0xFF1F3F2D) else SageHighlightFill,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(EmeraldCheckGreen),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (missingIds.isEmpty()) Icons.Default.Check else Icons.Default.WarningAmber,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                    }

                    Text(
                        text = buildAnnotatedString {
                            withStyle(
                                SpanStyle(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (isDark) Color(0xFFE7F5EC) else Color(0xFF122C1F)
                                )
                            ) {
                                append("${uiState.matchedCount} / ${uiState.document.cues.size}")
                            }
                            append("   ")
                            withStyle(
                                SpanStyle(
                                    fontWeight = FontWeight.Medium,
                                    color = if (isDark) Color(0xFFBBD8C5) else Color(0xFF284D3A)
                                )
                            ) {
                                append("Baris terpetakan")
                            }
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.testTag("match_progress_label")
                    )
                }

                if (missingIds.isNotEmpty()) {
                    val previewMissing = missingIds.take(10).joinToString(", ") { "#$it" }
                    val moreSuffix = if (missingIds.size > 10) " ... (+${missingIds.size - 10} lainnya)" else ""
                    Text(
                        text = "Belum terpetakan (${missingIds.size} baris): $previewMissing$moreSuffix",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isDark) Color(0xFFD6EADF) else Color(0xFF335C47),
                        modifier = Modifier.testTag("missing_ids_warning_text")
                    )
                }
            }
        }

        // 2. Two Equal-Width Action Pills Row matching Screen 5:
        // Left: Solid Forest Green [ 📋 Tempel ] | Right: White [ + Append ]
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = {
                    val clip = readClipboardText(context)
                    viewModel.pasteOrAppendBatch(clip, appendMode = false)
                },
                shape = RoundedCornerShape(23.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isDark) MaterialTheme.colorScheme.primary else PrimaryForestButton,
                    contentColor = if (isDark) MaterialTheme.colorScheme.onPrimary else Color.White
                ),
                modifier = Modifier
                    .weight(1f)
                    .height(46.dp)
                    .testTag("paste_replace_button")
            ) {
                Icon(
                    imageVector = Icons.Default.ContentPaste,
                    contentDescription = null,
                    modifier = Modifier.size(17.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Tempel",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            OutlinedButton(
                onClick = {
                    val clip = readClipboardText(context)
                    viewModel.pasteOrAppendBatch(clip, appendMode = true)
                },
                shape = RoundedCornerShape(23.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = if (isDark) MaterialTheme.colorScheme.surface else Color.White,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ),
                border = BorderStroke(
                    1.dp,
                    if (isDark) MaterialTheme.colorScheme.outlineVariant else Color(0xFFDCE4DF)
                ),
                modifier = Modifier
                    .weight(1f)
                    .height(46.dp)
                    .testTag("paste_append_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Append",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // 3. Main White Rounded Card for Numbered Lines ([1], [2], [3]...) matching Screen 5
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = if (isDark) MaterialTheme.colorScheme.surface else Color.White,
            border = BorderStroke(
                1.dp,
                if (isDark) MaterialTheme.colorScheme.outlineVariant else Color(0xFFE3EAE5)
            ),
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Subtle utility bar at top of editor card for Demo AI & Clear
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Teks Terjemahan ([1], [2], ...)",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isDark) Color(0xFF1F3328) else Color(0xFFEEF3EF),
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { viewModel.fillDemoAiTranslations() }
                                .testTag("fill_demo_ai_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoFixHigh,
                                    contentDescription = null,
                                    modifier = Modifier.size(13.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Muat Demo AI",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isDark) Color(0xFF2B2222) else Color(0xFFF5EFEF),
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { viewModel.clearPastedTranslations() }
                                .testTag("clear_pasted_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteSweep,
                                    contentDescription = null,
                                    modifier = Modifier.size(13.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Kosongkan",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(
                    color = if (isDark) {
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                    } else {
                        Color(0xFFEEF2EF)
                    }
                )

                OutlinedTextField(
                    value = uiState.pastedEditorText,
                    onValueChange = { viewModel.updatePastedEditorText(it) },
                    textStyle = subtitleEditorTextStyle(themeSettings),
                    placeholder = {
                        Text(
                            text = "[1]   Aah... akhirnya sampai juga...\n\n[2]   Jadi ini.... Kota Aqua...?\n\n[3]   Luar biasa...!\n\n[4]   Kita benar-benar berhasil...\n\n[5]   Sekarang, apa yang kita lakukan?",
                            style = subtitleEditorTextStyle(themeSettings),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent
                    ),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 4.dp)
                        .testTag("pasted_ai_editor_input")
                )
            }
        }

        // 4. Bottom White Card: "Auto-Apply Kamus" + Forest Green Switch matching Screen 5
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = if (isDark) MaterialTheme.colorScheme.surface else Color.White,
            border = BorderStroke(
                1.dp,
                if (isDark) MaterialTheme.colorScheme.outlineVariant else Color(0xFFE3EAE5)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Auto-Apply Kamus",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Switch(
                    checked = uiState.autoApplyGlossaryBeforeMerge,
                    onCheckedChange = { viewModel.setAutoApplyGlossaryBeforeMerge(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = EmeraldCheckGreen
                    ),
                    modifier = Modifier.testTag("auto_apply_glossary_switch")
                )
            }
        }
    }
}

private fun readClipboardText(context: Context): String {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    val item = clipboard?.primaryClip?.getItemAt(0)
    return item?.coerceToText(context)?.toString() ?: ""
}
