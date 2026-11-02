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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
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
import com.example.ui.theme.HomeHighlightGreenBottom
import com.example.ui.theme.HomeHighlightGreenTop
import com.example.ui.theme.HomeHighlightMintBorder
import com.example.ui.theme.HomeTileAlabaster
import com.example.viewmodel.SubtitleUiState

/**
 * Screen 3 of the Locked 10-Screen Reference Design:
 * Deep Forest Green Home Dashboard with:
 * - Top Brand Header (`// SubAI v1.x.x` + Settings gear)
 * - 2x2 Grid (`Ekstrak & Terjemahkan` highlighted green, `Paste AI`, `Find & Kamus`, `Merge`)
 * - Full-width `Batch Queue` card
 * - Full-width Recent File card (`KonoSuba_E01.ass • 120 dialog - Baru saja`)
 * - Bottom rounded Alabaster Dock (`Home`, `History/Batch`, `Folder`, `Profile/AI`)
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
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF122D21),
                        Color(0xFF17382A),
                        Color(0xFF122B20)
                    )
                )
            )
            .testTag("home_dashboard_screen"),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 540.dp)
                .padding(horizontal = 18.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Scrollable Upper Content (Header + 2x2 Grid + Batch Queue + Recent File)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 1. Top Brand Header Row matching Screen 3
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp, bottom = 6.dp),
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
                            color = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                        Column {
                            Text(
                                text = "SubAI",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                lineHeight = 22.sp
                            )
                            Text(
                                text = "v${BuildConfig.VERSION_NAME}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFFA7C4B5),
                                modifier = Modifier.testTag("home_version_badge")
                            )
                        }
                    }

                    IconButton(
                        onClick = onOpenSettings,
                        modifier = Modifier
                            .size(42.dp)
                            .testTag("open_theme_settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = "Pengaturan",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                // 2. 2x2 Feature Cards Grid matching Screen 3
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Top-Left: Highlighted Green Card ("Ekstrak & Terjemahkan")
                    HomeSquareFeatureTile(
                        title = "Ekstrak\n& Terjemahkan",
                        icon = Icons.Default.NorthEast,
                        isHighlighted = true,
                        testTag = "home_card_extract",
                        onClick = { onNavigateToTab(0) },
                        modifier = Modifier.weight(1f)
                    )

                    // Top-Right: Light Alabaster Card ("Paste AI")
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
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Bottom-Left: Light Alabaster Card ("Find & Kamus")
                    HomeSquareFeatureTile(
                        title = "Find & Kamus",
                        icon = Icons.Outlined.Search,
                        isHighlighted = false,
                        testTag = "home_card_glossary",
                        onClick = { onNavigateToTab(2) },
                        modifier = Modifier.weight(1f)
                    )

                    // Bottom-Right: Light Alabaster Card ("Merge")
                    HomeSquareFeatureTile(
                        title = "Merge",
                        icon = Icons.Outlined.Layers,
                        isHighlighted = false,
                        testTag = "home_card_merge",
                        onClick = { onNavigateToTab(3) },
                        modifier = Modifier.weight(1f)
                    )
                }

                // 3. Full-Width Horizontal Card: "Batch Queue" matching Screen 3
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = HomeTileAlabaster,
                    shadowElevation = 2.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(74.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .clickable { onNavigateToTab(4) }
                        .testTag("home_card_batch")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Layers,
                                contentDescription = "Batch Queue",
                                tint = Color(0xFF173327),
                                modifier = Modifier.size(28.dp)
                            )
                            Column {
                                Text(
                                    text = "Batch Queue",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF173327)
                                )
                                if (batchState.totalFiles > 0) {
                                    Text(
                                        text = "${batchState.completedCount}/${batchState.totalFiles} file selesai",
                                        fontSize = 12.sp,
                                        color = Color(0xFF4E665A)
                                    )
                                }
                            }
                        }

                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = Color(0xFF5A6D63),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                // 4. Full-Width Recent File Card matching Screen 3 ("KonoSuba_E01.ass • 120 dialog - Baru saja")
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFFE5ECE7),
                    shadowElevation = 2.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
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
                                color = Color(0xFFD4E0D8),
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { onOpenFilePicker() }
                                    .testTag("home_open_file_button")
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Outlined.Folder,
                                        contentDescription = "Buka File Subtitle",
                                        tint = Color(0xFF173327),
                                        modifier = Modifier.size(22.dp)
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
                                    text = uiState.document.fileName,
                                    fontSize = 14.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF173327),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${uiState.document.cues.size} dialog - Baru saja",
                                    fontSize = 12.sp,
                                    color = Color(0xFF4F6359),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "Buka File",
                            tint = Color(0xFF5A6D63),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 5. Bottom Rounded Alabaster Navigation Dock matching Screen 3
            Surface(
                shape = RoundedCornerShape(26.dp),
                color = Color(0xFFEEF2EE),
                shadowElevation = 4.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(62.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 1. Active Home Pill (Solid Forest Green Pill with White Home Icon)
                    Box(
                        modifier = Modifier
                            .width(52.dp)
                            .height(38.dp)
                            .clip(RoundedCornerShape(19.dp))
                            .background(Color(0xFF236441))
                            .clickable { onNavigateToTab(-1) }
                            .testTag("nav_tab_home"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Home,
                            contentDescription = "Beranda",
                            tint = Color.White,
                            modifier = Modifier.size(21.dp)
                        )
                    }

                    // 2. History / Batch Queue Icon
                    IconButton(
                        onClick = { onNavigateToTab(4) },
                        modifier = Modifier.testTag("nav_tab_batch")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.History,
                            contentDescription = "Riwayat & Batch Queue",
                            tint = Color(0xFF23382E),
                            modifier = Modifier.size(23.dp)
                        )
                    }

                    // 3. Folder / Open Subtitle File Picker Icon
                    IconButton(
                        onClick = onOpenFilePicker,
                        modifier = Modifier.testTag("home_dock_open_file")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Folder,
                            contentDescription = "Pilih File Subtitle",
                            tint = Color(0xFF23382E),
                            modifier = Modifier.size(23.dp)
                        )
                    }

                    // 4. Profile / AI Provider & Settings Icon
                    IconButton(
                        onClick = onOpenSettings,
                        modifier = Modifier.testTag("home_dock_settings")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.PersonOutline,
                            contentDescription = "Profil AI & Pengaturan",
                            tint = Color(0xFF23382E),
                            modifier = Modifier.size(24.dp)
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
    val bgModifier = if (isHighlighted) {
        Modifier.background(
            Brush.verticalGradient(
                colors = listOf(HomeHighlightGreenTop, HomeHighlightGreenBottom)
            )
        )
    } else {
        Modifier.background(HomeTileAlabaster)
    }

    val contentColor = if (isHighlighted) Color.White else Color(0xFF173327)
    val borderStroke = if (isHighlighted) {
        BorderStroke(1.5.dp, HomeHighlightMintBorder)
    } else {
        null
    }

    Surface(
        shape = RoundedCornerShape(22.dp),
        color = Color.Transparent,
        border = borderStroke,
        shadowElevation = if (isHighlighted) 6.dp else 2.dp,
        modifier = modifier
            .height(126.dp)
            .clip(RoundedCornerShape(22.dp))
            .then(bgModifier)
            .clickable(onClick = onClick)
            .testTag(testTag)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = contentColor,
                modifier = Modifier.size(26.dp)
            )

            Text(
                text = title,
                fontSize = 15.5.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.Bold,
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
