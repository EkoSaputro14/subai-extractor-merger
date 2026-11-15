package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.NorthEast
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.BuildConfig
import com.example.model.BatchQueueState
import com.example.ui.theme.FrontierMonoFamily
import com.example.viewmodel.SubtitleUiState

/**
 * Frontier home dashboard.
 *
 * Paper-white canvas, near-black ink, hairline cards and one dark emphasis card —
 * the same language as the extracted reference design:
 *  - brand row (mark + wordmark + version)
 *  - 2x2 feature grid (one ink tile, three paper tiles)
 *  - dark batch queue card
 *  - recent file card
 *  - floating capsule navigation dock
 */
@Composable
fun HomeDashboardScreen(
    uiState: SubtitleUiState,
    batchState: BatchQueueState,
    onNavigateToTab: (Int) -> Unit,
    onOpenFilePicker: () -> Unit,
    onOpenSettings: () -> Unit,
    onReplayIntro: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(scheme.background)
            .testTag("home_dashboard_screen"),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 540.dp)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // 1. Brand row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp, bottom = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onReplayIntro?.invoke() }
                            .padding(vertical = 4.dp, horizontal = 2.dp)
                    ) {
                        SubAiParallelogramLogo(
                            color = scheme.onBackground,
                            modifier = Modifier.size(26.dp)
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "SubAI",
                                style = MaterialTheme.typography.headlineSmall,
                                color = scheme.onBackground
                            )
                            Text(
                                text = "v${BuildConfig.VERSION_NAME}",
                                fontFamily = FrontierMonoFamily,
                                fontSize = 10.sp,
                                color = scheme.onSurfaceVariant,
                                modifier = Modifier.testTag("home_version_badge")
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(50),
                        color = scheme.surfaceVariant,
                        modifier = Modifier.size(38.dp)
                    ) {
                        IconButton(
                            onClick = onOpenSettings,
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("open_theme_settings_button")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Settings,
                                contentDescription = "Pengaturan",
                                tint = scheme.onBackground,
                                modifier = Modifier.size(19.dp)
                            )
                        }
                    }
                }

                // 2. Feature grid
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    HomeSquareFeatureTile(
                        title = "Ekstrak\n& Terjemahkan",
                        icon = Icons.Default.NorthEast,
                        isHighlighted = true,
                        testTag = "home_card_extract",
                        onClick = { onNavigateToTab(0) },
                        modifier = Modifier.weight(1f)
                    )
                    HomeSquareFeatureTile(
                        title = "Paste AI",
                        icon = Icons.Outlined.ContentPaste,
                        isHighlighted = false,
                        testTag = "home_card_paste",
                        onClick = { onNavigateToTab(1) },
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    HomeSquareFeatureTile(
                        title = "Find & Kamus",
                        icon = Icons.Outlined.Search,
                        isHighlighted = false,
                        testTag = "home_card_glossary",
                        onClick = { onNavigateToTab(2) },
                        modifier = Modifier.weight(1f)
                    )
                    HomeSquareFeatureTile(
                        title = "Merge",
                        icon = Icons.Outlined.Layers,
                        isHighlighted = false,
                        testTag = "home_card_merge",
                        onClick = { onNavigateToTab(3) },
                        modifier = Modifier.weight(1f)
                    )
                }

                // 3. Dark batch queue card
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.inverseSurface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .clickable { onNavigateToTab(4) }
                        .testTag("home_card_batch")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Batch Queue",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.inverseOnSurface
                            )
                            Text(
                                text = "${batchState.completedCount}/${batchState.totalFiles} file",
                                fontFamily = FrontierMonoFamily,
                                fontSize = 10.5.sp,
                                color = MaterialTheme.colorScheme.inverseOnSurface.copy(alpha = 0.65f)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val total = batchState.totalFiles.coerceAtLeast(1)
                            Column(modifier = Modifier.weight(1f)) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(4.dp)
                                        .clip(RoundedCornerShape(50))
                                        .background(MaterialTheme.colorScheme.inverseOnSurface.copy(alpha = 0.22f))
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .fillMaxWidth(
                                                (batchState.completedCount.toFloat() / total).coerceIn(0f, 1f)
                                            )
                                            .clip(RoundedCornerShape(50))
                                            .background(MaterialTheme.colorScheme.inverseOnSurface)
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Layers,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.inverseOnSurface.copy(alpha = 0.65f),
                                modifier = Modifier.size(14.dp)
                            )
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.inverseOnSurface,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                // 4. Recent file card
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = scheme.surface,
                    border = BorderStroke(1.dp, scheme.outlineVariant),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onNavigateToTab(0) }
                        .testTag("home_recent_file_card")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(11.dp),
                                color = scheme.surfaceVariant,
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(11.dp))
                                    .clickable { onOpenFilePicker() }
                                    .testTag("home_open_file_button")
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Outlined.Folder,
                                        contentDescription = "Buka File Subtitle",
                                        tint = scheme.onSurface,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Text(
                                    text = uiState.document.fileName,
                                    fontFamily = FrontierMonoFamily,
                                    fontSize = 12.sp,
                                    color = scheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${uiState.document.cues.size} dialog · baru saja",
                                    fontSize = 11.5.sp,
                                    color = scheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "Buka File",
                            tint = scheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 5. Floating capsule dock
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = scheme.surface,
                border = BorderStroke(1.dp, scheme.outlineVariant),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .width(52.dp)
                            .height(34.dp)
                            .clip(RoundedCornerShape(17.dp))
                            .background(scheme.primary)
                            .clickable { onNavigateToTab(-1) }
                            .testTag("nav_tab_home"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Home,
                            contentDescription = "Beranda",
                            tint = scheme.onPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = { onNavigateToTab(4) },
                        modifier = Modifier.testTag("nav_tab_batch")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.History,
                            contentDescription = "Riwayat & Batch Queue",
                            tint = scheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = onOpenFilePicker,
                        modifier = Modifier.testTag("home_dock_open_file")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Folder,
                            contentDescription = "Pilih File Subtitle",
                            tint = scheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = onOpenSettings,
                        modifier = Modifier.testTag("home_dock_settings")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.PersonOutline,
                            contentDescription = "Profil AI & Pengaturan",
                            tint = scheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeSquareFeatureTile(
    title: String,
    icon: ImageVector,
    isHighlighted: Boolean,
    testTag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val containerColor = if (isHighlighted) scheme.primary else scheme.surface
    val contentColor = if (isHighlighted) scheme.onPrimary else scheme.onSurface
    val borderStroke = if (isHighlighted) null else BorderStroke(1.dp, scheme.outlineVariant)

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = containerColor,
        border = borderStroke,
        modifier = modifier
            .height(118.dp)
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .testTag(testTag)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = contentColor,
                modifier = Modifier.size(22.dp)
            )

            Text(
                text = title,
                fontSize = 15.sp,
                lineHeight = 19.sp,
                fontWeight = FontWeight.Medium,
                color = contentColor,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
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

        val bar1 = Path().apply {
            moveTo(w * 0.25f, h * 0.12f)
            lineTo(w * 0.50f, h * 0.12f)
            lineTo(w * 0.30f, h * 0.88f)
            lineTo(w * 0.05f, h * 0.88f)
            close()
        }
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
