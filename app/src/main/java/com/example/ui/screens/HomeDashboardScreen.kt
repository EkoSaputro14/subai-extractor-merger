package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.NorthEast
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.DynamicFeed
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.BuildConfig
import com.example.model.BatchQueueState
import com.example.viewmodel.SubtitleUiState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeDashboardScreen(
    uiState: SubtitleUiState,
    batchState: BatchQueueState,
    onNavigateToTab: (Int) -> Unit,
    onOpenFilePicker: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .testTag("home_dashboard_screen")
    ) {
        val isWide = maxWidth >= 640.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Hero Banner Card with Scenic Mountain & Radar Crosshair Artwork + 5 Feature Cards
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDark) Color(0xFF14211B) else Color(0xFFF3F6F4)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                border = BorderStroke(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                )
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    // Scenic Mountain + Radar Circle + Crosshair Artwork dynamically matching card size
                    MountainHeroArtwork(
                        isDark = isDark,
                        modifier = Modifier.matchParentSize()
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = if (isWide) 22.dp else 16.dp, vertical = 18.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Top Row: SubAI Brand Mark + Version Pill
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                SubAiParallelogramLogo(
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(22.dp)
                                )
                                Text(
                                    text = "SubAI",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(50),
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                                border = BorderStroke(
                                    1.dp,
                                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                ),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .clickable { onOpenSettings() }
                                    .testTag("home_version_badge")
                            ) {
                                Text(
                                    text = "v${BuildConfig.VERSION_NAME}",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                                )
                            }
                        }

                        // Hero Headline & Subtitle
                        Column(
                            modifier = Modifier.fillMaxWidth(if (isWide) 0.62f else 0.95f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "SubAI\nExtractor & Merger",
                                fontSize = if (isWide) 36.sp else 28.sp,
                                lineHeight = if (isWide) 42.sp else 34.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Ekstrak, terjemahkan, edit, dan gabungkan subtitle dengan bantuan AI.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 20.sp
                            )
                        }

                        // 5 Feature Cards Row (Wide) or Responsive 2-Column Grid (Compact Portrait)
                        val featureItems = listOf(
                            HomeFeatureCardSpec(
                                tabIndex = 0,
                                title = "Ekstrak",
                                subtitle = "Ambil teks dari subtitle",
                                icon = Icons.Default.NorthEast,
                                isHighlighted = true,
                                badgeText = null,
                                testTag = "home_card_extract"
                            ),
                            HomeFeatureCardSpec(
                                tabIndex = 1,
                                title = "Paste AI",
                                subtitle = "Tempel hasil terjemahan",
                                icon = Icons.Outlined.ContentPaste,
                                isHighlighted = false,
                                badgeText = if (uiState.matchedCount > 0) "${uiState.matchedCount}/${uiState.document.cues.size}" else null,
                                testTag = "home_card_paste"
                            ),
                            HomeFeatureCardSpec(
                                tabIndex = 2,
                                title = "Find & Kamus",
                                subtitle = "Cari, ganti, dan atur glosarium",
                                icon = Icons.Outlined.Search,
                                isHighlighted = false,
                                badgeText = null,
                                testTag = "home_card_glossary"
                            ),
                            HomeFeatureCardSpec(
                                tabIndex = 3,
                                title = "Merge",
                                subtitle = "Gabungkan & simpan subtitle",
                                icon = Icons.Outlined.Layers,
                                isHighlighted = false,
                                badgeText = null,
                                testTag = "home_card_merge"
                            ),
                            HomeFeatureCardSpec(
                                tabIndex = 4,
                                title = "Batch",
                                subtitle = "Proses banyak file sekaligus",
                                icon = Icons.Outlined.DynamicFeed,
                                isHighlighted = false,
                                badgeText = if (batchState.totalFiles > 0) "${batchState.completedCount}/${batchState.totalFiles}" else null,
                                testTag = "home_card_batch"
                            )
                        )

                        if (isWide) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                featureItems.forEach { spec ->
                                    HomeFeatureActionCard(
                                        spec = spec,
                                        isDark = isDark,
                                        onClick = { onNavigateToTab(spec.tabIndex) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        } else {
                            // Responsive 2-Column Grid on Portrait Phones so all 5 cards fit cleanly without clipping
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    HomeFeatureActionCard(
                                        spec = featureItems[0],
                                        isDark = isDark,
                                        onClick = { onNavigateToTab(featureItems[0].tabIndex) },
                                        modifier = Modifier.weight(1f)
                                    )
                                    HomeFeatureActionCard(
                                        spec = featureItems[1],
                                        isDark = isDark,
                                        onClick = { onNavigateToTab(featureItems[1].tabIndex) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    HomeFeatureActionCard(
                                        spec = featureItems[2],
                                        isDark = isDark,
                                        onClick = { onNavigateToTab(featureItems[2].tabIndex) },
                                        modifier = Modifier.weight(1f)
                                    )
                                    HomeFeatureActionCard(
                                        spec = featureItems[3],
                                        isDark = isDark,
                                        onClick = { onNavigateToTab(featureItems[3].tabIndex) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                HomeFeatureActionCard(
                                    spec = featureItems[4],
                                    isDark = isDark,
                                    onClick = { onNavigateToTab(featureItems[4].tabIndex) },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }

                        // Bottom "File Terakhir" Card inside the Dashboard container
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = if (isDark) {
                                Color(0xFF1B2B23).copy(alpha = 0.94f)
                            } else {
                                Color.White.copy(alpha = 0.94f)
                            },
                            border = BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(18.dp))
                                .clickable { onNavigateToTab(0) }
                                .testTag("home_recent_file_card")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                        modifier = Modifier.size(42.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Outlined.Folder,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }

                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(end = 8.dp),
                                        verticalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        Text(
                                            text = "File Terakhir",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = uiState.document.fileName,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${uiState.document.cues.size} dialog  •  ${uiState.matchedCount} cocok",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    FilledTonalIconButton(
                                        onClick = onOpenFilePicker,
                                        modifier = Modifier
                                            .size(36.dp)
                                            .testTag("home_open_file_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.FolderOpen,
                                            contentDescription = "Buka File Subtitle Baru",
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                        contentDescription = "Buka Ekstrak",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
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

private data class HomeFeatureCardSpec(
    val tabIndex: Int,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val isHighlighted: Boolean,
    val badgeText: String?,
    val testTag: String
)

@Composable
private fun HomeFeatureActionCard(
    spec: HomeFeatureCardSpec,
    isDark: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val containerBrush = if (spec.isHighlighted) {
        if (isDark) {
            Brush.verticalGradient(
                listOf(Color(0xFF28593E), Color(0xFF1E4430))
            )
        } else {
            Brush.verticalGradient(
                listOf(Color(0xFFBDE5C2), Color(0xFFDDF1E0))
            )
        }
    } else {
        if (isDark) {
            Brush.verticalGradient(
                listOf(Color(0xFF1D2E25).copy(alpha = 0.94f), Color(0xFF192820).copy(alpha = 0.94f))
            )
        } else {
            Brush.verticalGradient(
                listOf(Color(0xFFF8FAF8).copy(alpha = 0.94f), Color(0xFFFFFFFF).copy(alpha = 0.96f))
            )
        }
    }

    val contentColor = if (spec.isHighlighted && !isDark) {
        Color(0xFF0E291C)
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    val subtitleColor = if (spec.isHighlighted && !isDark) {
        Color(0xFF264A37)
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color.Transparent,
        border = BorderStroke(
            width = 1.dp,
            color = if (spec.isHighlighted) {
                Color(0xFF8CC996).copy(alpha = 0.7f)
            } else {
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f)
            }
        ),
        shadowElevation = 2.dp,
        modifier = modifier
            .heightIn(min = 132.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(containerBrush)
            .clickable(onClick = onClick)
            .testTag(spec.testTag)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = spec.icon,
                    contentDescription = spec.title,
                    tint = contentColor,
                    modifier = Modifier.size(22.dp)
                )

                if (!spec.badgeText.isNullOrBlank()) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = spec.badgeText,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = spec.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = contentColor
                )
                Text(
                    text = spec.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 11.5.sp,
                    lineHeight = 15.sp,
                    color = subtitleColor,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun SubAiParallelogramLogo(
    color: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Left slanted bar
        val bar1 = Path().apply {
            moveTo(w * 0.25f, h * 0.12f)
            lineTo(w * 0.50f, h * 0.12f)
            lineTo(w * 0.30f, h * 0.88f)
            lineTo(w * 0.05f, h * 0.88f)
            close()
        }
        // Right slanted bar
        val bar2 = Path().apply {
            moveTo(w * 0.62f, h * 0.12f)
            lineTo(w * 0.87f, h * 0.12f)
            lineTo(w * 0.67f, h * 0.88f)
            lineTo(w * 0.42f, h * 0.88f)
            close()
        }

        drawPath(path = bar1, color = color)
        drawPath(path = bar2, color = color)
    }
}

@Composable
private fun MountainHeroArtwork(
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // 1. Soft atmospheric sky background gradient
        drawRect(
            brush = Brush.horizontalGradient(
                colors = if (isDark) {
                    listOf(Color(0xFF14211B), Color(0xFF1B2E25), Color(0xFF233B2F))
                } else {
                    listOf(Color(0xFFF3F6F4), Color(0xFFEDF2EE), Color(0xFFE3ECE6))
                }
            )
        )

        // 2. Delicate geometric radar circle + crosshair '+' in upper-right sky (matching mockup)
        val circleCenter = Offset(w * 0.72f, h * 0.26f)
        val circleRadius = minOf(w, h) * 0.20f
        val reticleColor = if (isDark) {
            Color(0xFF74C69D).copy(alpha = 0.32f)
        } else {
            Color(0xFF6B9E78).copy(alpha = 0.38f)
        }

        drawCircle(
            color = reticleColor,
            radius = circleRadius,
            center = circleCenter,
            style = Stroke(width = 1.5f)
        )
        // Crosshair '+' at circle center
        val crossSize = 14f
        drawLine(
            color = reticleColor.copy(alpha = 0.65f),
            start = Offset(circleCenter.x - crossSize, circleCenter.y),
            end = Offset(circleCenter.x + crossSize, circleCenter.y),
            strokeWidth = 2f
        )
        drawLine(
            color = reticleColor.copy(alpha = 0.65f),
            start = Offset(circleCenter.x, circleCenter.y - crossSize),
            end = Offset(circleCenter.x, circleCenter.y + crossSize),
            strokeWidth = 2f
        )

        // 3. Distant misty mountain ridge on the right
        val distantMountain = Path().apply {
            moveTo(w * 0.45f, h * 0.78f)
            lineTo(w * 0.62f, h * 0.50f)
            lineTo(w * 0.71f, h * 0.56f)
            lineTo(w * 0.87f, h * 0.24f)
            lineTo(w * 0.95f, h * 0.32f)
            lineTo(w * 1.0f, h * 0.22f)
            lineTo(w * 1.0f, h * 0.85f)
            close()
        }
        drawPath(
            path = distantMountain,
            brush = Brush.verticalGradient(
                colors = if (isDark) {
                    listOf(Color(0xFF354F42), Color(0xFF1E3027))
                } else {
                    listOf(Color(0xFF6E7F75), Color(0xFFA7B8AE))
                },
                startY = h * 0.2f,
                endY = h * 0.8f
            )
        )

        // 4. Main rugged alpine peak with sunlit snowy/rocky facets & mossy green slopes
        val mainPeakShadow = Path().apply {
            moveTo(w * 0.38f, h * 0.82f)
            lineTo(w * 0.58f, h * 0.58f)
            lineTo(w * 0.68f, h * 0.61f)
            lineTo(w * 0.86f, h * 0.25f)
            lineTo(w * 1.0f, h * 0.36f)
            lineTo(w * 1.0f, h * 0.90f)
            close()
        }
        drawPath(
            path = mainPeakShadow,
            brush = Brush.verticalGradient(
                colors = if (isDark) {
                    listOf(Color(0xFF2B4637), Color(0xFF182A20))
                } else {
                    listOf(Color(0xFF475B4E), Color(0xFF2F4236))
                },
                startY = h * 0.25f,
                endY = h * 0.85f
            )
        )

        // Mossy alpine ridge highlights
        val mossySlope = Path().apply {
            moveTo(w * 0.44f, h * 0.80f)
            lineTo(w * 0.64f, h * 0.57f)
            lineTo(w * 0.76f, h * 0.44f)
            lineTo(w * 0.86f, h * 0.25f)
            lineTo(w * 0.90f, h * 0.46f)
            lineTo(w * 1.0f, h * 0.54f)
            lineTo(w * 1.0f, h * 0.85f)
            close()
        }
        drawPath(
            path = mossySlope,
            brush = Brush.linearGradient(
                colors = if (isDark) {
                    listOf(Color(0xFF4A7256), Color(0xFF284232))
                } else {
                    listOf(Color(0xFF7B9477), Color(0xFF48604B))
                },
                start = Offset(w * 0.6f, h * 0.3f),
                end = Offset(w, h * 0.8f)
            )
        )

        // Left-to-right soft mist fade so text on the left is always 100% legible
        drawRect(
            brush = Brush.horizontalGradient(
                0.0f to if (isDark) Color(0xFF14211B) else Color(0xFFF3F6F4),
                0.48f to if (isDark) Color(0xFF14211B).copy(alpha = 0.88f) else Color(0xFFF3F6F4).copy(alpha = 0.88f),
                0.85f to Color.Transparent
            )
        )
    }
}
