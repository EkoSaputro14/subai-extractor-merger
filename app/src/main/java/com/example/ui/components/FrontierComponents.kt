package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.FrontierMonoFamily
import com.example.ui.theme.XaiAccent
import com.example.ui.theme.XaiDanger
import com.example.ui.theme.XaiOk
import com.example.ui.theme.XaiWarn

/**
 * Frontier primitives shared by the queue / extract / merge screens.
 *
 * These mirror the canvas layout language: hairline metrics strip, capsule status
 * chips, mono metadata, paper cards with a single dark emphasis.
 */

enum class FrontierTone { INK, OK, WARN, DANGER, MUTED }

@Composable
fun frontierToneColor(tone: FrontierTone): Color = when (tone) {
    FrontierTone.INK -> MaterialTheme.colorScheme.onSurface
    FrontierTone.OK -> XaiOk
    FrontierTone.WARN -> XaiWarn
    FrontierTone.DANGER -> XaiDanger
    FrontierTone.MUTED -> MaterialTheme.colorScheme.onSurfaceVariant
}

/** Capsule status chip: dot + label + hairline border. */
@Composable
fun FrontierStatusChip(
    text: String,
    tone: FrontierTone = FrontierTone.MUTED,
    modifier: Modifier = Modifier
) {
    val dot = frontierToneColor(tone)
    Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(5.dp)
                    .background(dot, CircleShape)
            )
            Text(
                text = text,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

data class FrontierMetric(
    val value: String,
    val label: String,
    val tone: FrontierTone = FrontierTone.INK,
    val testTag: String? = null
)

/** Four-column metrics strip with hairline dividers, as on the Queue screen. */
@Composable
fun FrontierMetricStrip(
    metrics: List<FrontierMetric>,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        metrics.forEachIndexed { index, metric ->
            if (index > 0) {
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(34.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant)
                )
            }
            val columnModifier = if (metric.testTag != null) {
                Modifier
                    .weight(1f)
                    .testTag(metric.testTag)
            } else {
                Modifier.weight(1f)
            }
            Column(
                modifier = columnModifier.padding(start = if (index == 0) 0.dp else 12.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = metric.value,
                    style = MaterialTheme.typography.headlineSmall,
                    color = frontierToneColor(metric.tone)
                )
                Text(
                    text = metric.label,
                    fontSize = 10.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/** Pill segmented control (Satu Bahasa / Bilingual, Teks Asli / Terjemahan). */
@Composable
fun FrontierSegmented(
    labels: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    testTagPrefix: String = "frontier_seg"
) {
    Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            labels.forEachIndexed { index, label ->
                val selected = index == selectedIndex
                Surface(
                    shape = RoundedCornerShape(50),
                    color = if (selected) MaterialTheme.colorScheme.surface else Color.Transparent,
                    border = if (selected) BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant) else null,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("${testTagPrefix}_$index")
                        .clickable { onSelect(index) }
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            fontSize = 11.5.sp,
                            fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
                            color = if (selected) {
                                MaterialTheme.colorScheme.onSurface
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

/** Mono meta row used inside cue cards and job cards. */
@Composable
fun FrontierMonoMeta(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    sizeSp: Float = 10f
) {
    Text(
        text = text,
        fontFamily = FrontierMonoFamily,
        fontSize = sizeSp.sp,
        color = color,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
    )
}

/** Section kicker + optional trailing text, as used above lists. */
@Composable
fun FrontierSectionHeader(
    kicker: String,
    modifier: Modifier = Modifier,
    trailing: String? = null
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = kicker,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (trailing != null) {
            FrontierMonoMeta(text = trailing, sizeSp = 10f)
        }
    }
}

/** Small square order badge used in front of queue file names. */
@Composable
fun FrontierOrderBadge(
    order: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier
    ) {
        Text(
            text = order,
            fontFamily = FrontierMonoFamily,
            fontSize = 9.5.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
        )
    }
}

/** 4dp progress track with ink fill. */
@Composable
fun FrontierProgressBar(
    fraction: Float,
    modifier: Modifier = Modifier,
    fillColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(4.dp)
            .background(MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(50))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .height(4.dp)
                .background(fillColor, RoundedCornerShape(50))
        )
    }
}

/** Paper card container: hairline border, no elevation, optional dark emphasis. */
@Composable
fun FrontierCard(
    modifier: Modifier = Modifier,
    dark: Boolean = false,
    accentBorder: Boolean = false,
    content: @Composable () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (dark) MaterialTheme.colorScheme.inverseSurface else MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = 1.dp,
            color = when {
                accentBorder -> XaiAccent.copy(alpha = 0.55f)
                dark -> MaterialTheme.colorScheme.inverseSurface
                else -> MaterialTheme.colorScheme.outlineVariant
            }
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        content()
    }
}

/** Dot pulse indicator for a running queue. */
@Composable
fun FrontierLiveDot(
    modifier: Modifier = Modifier,
    color: Color = XaiOk
) {
    Box(
        modifier = modifier
            .size(6.dp)
            .background(color, CircleShape)
    )
}

/** Empty spacer helper so call sites stay terse. */
@Composable
fun FrontierGap(heightDp: Int = 8) {
    Spacer(modifier = Modifier.height(heightDp.dp))
}
