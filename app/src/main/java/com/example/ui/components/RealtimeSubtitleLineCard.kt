package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.SubtitleCue
import com.example.model.ThemeSettings
import com.example.ui.theme.subtitleEditorTextStyle
import kotlinx.coroutines.delay

/**
 * Minimalist Dual-Layer Subtitle Card:
 * - Single-line compact metadata header: `#1 • 00:00:01.50 → 00:00:05.20 • Style`
 * - Top Layer: Original language text (clean, readable)
 * - Bottom Layer: Realtime animated translation text directly underneath
 * - Tap anywhere on the card (or the edit icon) to open inline editing.
 */
@Composable
fun RealtimeSubtitleLineCard(
    cue: SubtitleCue,
    formattedOriginalText: String,
    translatedText: String?,
    isTranslatingThisCue: Boolean,
    isRecentlyTranslated: Boolean,
    targetLanguageLabel: String,
    themeSettings: ThemeSettings,
    onUpdateTranslatedLine: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val hasTranslation = !translatedText.isNullOrBlank()
    var isEditingInline by rememberSaveable(cue.id) { mutableStateOf(false) }
    var inlineEditBuffer by rememberSaveable(cue.id, translatedText) {
        mutableStateOf(translatedText.orEmpty())
    }

    var revealedCharCount by remember(cue.id, translatedText) {
        mutableIntStateOf(
            if (isRecentlyTranslated && !translatedText.isNullOrEmpty()) 0 else (translatedText?.length ?: 0)
        )
    }

    LaunchedEffect(cue.id, translatedText, isRecentlyTranslated) {
        val fullText = translatedText.orEmpty()
        if (fullText.isEmpty()) {
            revealedCharCount = 0
            return@LaunchedEffect
        }
        if (!isRecentlyTranslated) {
            revealedCharCount = fullText.length
            return@LaunchedEffect
        }
        revealedCharCount = 0
        val totalLen = fullText.length
        val step = (totalLen / 16).coerceAtLeast(2)
        var current = 0
        while (current < totalLen) {
            current = (current + step).coerceAtMost(totalLen)
            revealedCharCount = current
            delay(16L)
        }
        revealedCharCount = totalLen
    }

    val pulseAlpha = if (isTranslatingThisCue) {
        val infiniteTransition = rememberInfiniteTransition(label = "realtime_card_pulse_${cue.id}")
        val animatedAlpha by infiniteTransition.animateFloat(
            initialValue = 0.45f,
            targetValue = 1.0f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 680, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulse_alpha"
        )
        animatedAlpha
    } else {
        1.0f
    }

    val borderColor by animateColorAsState(
        targetValue = when {
            isTranslatingThisCue -> MaterialTheme.colorScheme.primary.copy(alpha = pulseAlpha)
            isEditingInline -> MaterialTheme.colorScheme.primary
            isRecentlyTranslated && hasTranslation -> MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
            hasTranslation -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f)
            else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
        },
        animationSpec = tween(durationMillis = 250),
        label = "card_border_color"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
            .clickable(enabled = !isEditingInline && !isTranslatingThisCue) {
                inlineEditBuffer = translatedText.orEmpty()
                isEditingInline = true
            }
            .testTag("realtime_subtitle_card_${cue.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            width = if (isTranslatingThisCue || isRecentlyTranslated || isEditingInline) 1.5.dp else 1.dp,
            color = borderColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 9.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // --- Single-Line Compact Header: #ID • Start → End • Style/Actor + Edit Trigger ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "#${cue.id}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Text(
                        text = "• ${cue.startTime} → ${cue.endTime}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (cue.style.isNotBlank() && cue.style != "Default") {
                        Text(
                            text = if (cue.actor.isNotBlank()) "• ${cue.style}/${cue.actor}" else "• ${cue.style}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.secondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    if (cue.leadingAssTags.isNotEmpty()) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "ASS Tag",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                            )
                        }
                    }
                }

                IconButton(
                    onClick = {
                        if (isEditingInline) {
                            onUpdateTranslatedLine(inlineEditBuffer)
                            isEditingInline = false
                        } else {
                            inlineEditBuffer = translatedText.orEmpty()
                            isEditingInline = true
                        }
                    },
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .size(32.dp)
                        .testTag("edit_card_line_button_${cue.id}")
                ) {
                    Icon(
                        imageVector = if (isEditingInline) Icons.Default.Check else Icons.Default.Edit,
                        contentDescription = "Edit baris #${cue.id}",
                        tint = if (hasTranslation) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // --- TOP LAYER: BAHASA ASLI (Clean & Compact) ---
            Text(
                text = formattedOriginalText,
                style = subtitleEditorTextStyle(themeSettings),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("card_original_text_${cue.id}")
            )

            // --- BOTTOM LAYER: REALTIME ANIMATION / TRANSLATED TEXT / INLINE EDIT ---
            AnimatedVisibility(
                visible = isTranslatingThisCue || hasTranslation || isEditingInline,
                enter = expandVertically(animationSpec = tween(220)) + fadeIn(animationSpec = tween(180)),
                exit = shrinkVertically(animationSpec = tween(180)) + fadeOut(animationSpec = tween(150))
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                    )

                    when {
                        isTranslatingThisCue -> {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f * pulseAlpha),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .padding(horizontal = 10.dp, vertical = 8.dp)
                                    .testTag("card_translating_indicator_${cue.id}"),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier
                                        .size(15.dp)
                                        .alpha(pulseAlpha)
                                )
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "Menerjemahkan ke $targetLanguageLabel...",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    LinearProgressIndicator(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(3.dp)
                                            .clip(RoundedCornerShape(2.dp))
                                    )
                                }
                            }
                        }

                        isEditingInline -> {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                OutlinedTextField(
                                    value = inlineEditBuffer,
                                    onValueChange = {
                                        inlineEditBuffer = it
                                        onUpdateTranslatedLine(it)
                                    },
                                    label = { Text("Terjemahan ($targetLanguageLabel) #${cue.id}") },
                                    textStyle = subtitleEditorTextStyle(themeSettings),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .defaultMinSize(minHeight = 48.dp)
                                        .testTag("card_inline_edit_input_${cue.id}")
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedButton(
                                        onClick = { isEditingInline = false },
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.defaultMinSize(minHeight = 36.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = null,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Tutup", style = MaterialTheme.typography.labelMedium)
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Button(
                                        onClick = {
                                            onUpdateTranslatedLine(inlineEditBuffer)
                                            isEditingInline = false
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.defaultMinSize(minHeight = 36.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Simpan", style = MaterialTheme.typography.labelMedium)
                                    }
                                }
                            }
                        }

                        hasTranslation -> {
                            val fullTranslated = translatedText.orEmpty()
                            val safeCount = revealedCharCount.coerceIn(0, fullTranslated.length)
                            val displayedTranslated = if (safeCount < fullTranslated.length) {
                                fullTranslated.substring(0, safeCount) + " ▍"
                            } else {
                                fullTranslated
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        color = MaterialTheme.colorScheme.secondaryContainer.copy(
                                            alpha = if (isRecentlyTranslated) 0.55f else 0.35f
                                        ),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .padding(horizontal = 10.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = if (safeCount < fullTranslated.length) {
                                        Icons.Default.Translate
                                    } else {
                                        Icons.Default.CheckCircle
                                    },
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier
                                        .padding(top = 2.dp)
                                        .size(15.dp)
                                )

                                Box(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = displayedTranslated,
                                        style = subtitleEditorTextStyle(themeSettings),
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.testTag("card_translated_text_${cue.id}")
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
