package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FindReplace
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MergeType
import androidx.compose.material.icons.filled.QueuePlayNext
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.DynamicFeed
import androidx.compose.material.icons.outlined.FindReplace
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.MergeType
import androidx.compose.material.icons.outlined.QueuePlayNext
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.model.AiProviderSettings
import com.example.model.BatchQueueState
import com.example.model.GlossaryRule
import com.example.model.ThemeSettings
import com.example.ui.screens.BatchQueueTabScreen
import com.example.ui.screens.ExtractTabScreen
import com.example.ui.screens.GlossaryFindReplaceTabScreen
import com.example.ui.screens.HomeDashboardScreen
import com.example.ui.screens.MergeTabScreen
import com.example.ui.screens.PasteTabScreen
import com.example.ui.screens.SubAiParallelogramLogo
import com.example.ui.screens.ThemeSettingsSheet
import com.example.ui.theme.ForestGreenDark
import com.example.ui.theme.SageHighlightFill
import com.example.ui.theme.SubAiExtractorTheme
import com.example.viewmodel.SubtitleUiState
import com.example.viewmodel.SubtitleViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Default to -1 (Beranda / Home Dashboard) unless a specific tab is requested via Intent
        val initialTab = if (intent?.hasExtra("open_tab") == true) {
            intent.getIntExtra("open_tab", -1)
        } else {
            -1
        }
        setContent {
            val subtitleViewModel: SubtitleViewModel = viewModel()
            val themeSettings by subtitleViewModel.themeSettings.collectAsStateWithLifecycle()

            SubAiExtractorTheme(themeSettings = themeSettings) {
                SubAiExtractorMainScreen(
                    viewModel = subtitleViewModel,
                    initialTab = initialTab
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubAiExtractorMainScreen(
    viewModel: SubtitleViewModel,
    initialTab: Int = -1
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val aiSettings by viewModel.aiSettings.collectAsStateWithLifecycle()
    val glossaryRules by viewModel.glossaryRules.collectAsStateWithLifecycle()
    val themeSettings by viewModel.themeSettings.collectAsStateWithLifecycle()
    val batchState by viewModel.batchQueueState.collectAsStateWithLifecycle()

    // -1 = Beranda (Home Dashboard), 0 = Ekstrak, 1 = Paste AI, 2 = Find & Kamus, 3 = Merge, 4 = Batch
    var selectedTab by rememberSaveable { mutableIntStateOf(initialTab.coerceIn(-1, 4)) }
    var showThemeSheet by rememberSaveable { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    // Return to Beranda (-1) when Back is pressed on secondary tabs
    BackHandler(enabled = selectedTab != -1) {
        selectedTab = -1
    }

    val openSubtitleLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.loadSubtitleFromUri(uri)
        }
    }

    val createSubtitleLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("*/*")
    ) { uri ->
        if (uri != null) {
            viewModel.saveMergedSubtitleToUri(uri)
        }
    }

    val openMultipleSubtitlesLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.addUrisToBatchQueue(uris)
        }
    }

    val openBatchOutputFolderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { treeUri ->
        if (treeUri != null) {
            viewModel.setBatchOutputFolderUri(treeUri)
        }
    }

    LaunchedEffect(uiState.lastActionBannerMessage) {
        val message = uiState.lastActionBannerMessage
        if (!message.isNullOrBlank()) {
            snackbarHostState.showSnackbar(message)
            viewModel.clearBannerMessage()
        }
    }

    val enabledRulesCount = glossaryRules.count { it.isEnabled }

    val activeScreenTitle = when (selectedTab) {
        -1 -> "SubAI Extractor & Merger"
        0 -> "Ekstrak & Terjemahkan"
        1 -> "Paste AI"
        2 -> "Find & Kamus"
        3 -> "Merge Subtitle"
        4 -> "Batch Translation"
        else -> "SubAI Extractor & Merger"
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWideScreen = maxWidth >= 600.dp

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            topBar = {
                if (!isWideScreen) {
                    TopAppBar(
                        windowInsets = WindowInsets.statusBars,
                        navigationIcon = {
                            if (selectedTab != -1) {
                                IconButton(
                                    onClick = { selectedTab = -1 },
                                    modifier = Modifier.testTag("topbar_back_to_home_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Kembali ke Beranda"
                                    )
                                }
                            } else {
                                Box(
                                    modifier = Modifier.padding(start = 14.dp, end = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    SubAiParallelogramLogo(
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                        },
                        title = {
                            Column {
                                Text(
                                    text = activeScreenTitle,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${uiState.document.fileName} • ${uiState.document.cues.size} dialog (${uiState.matchedCount} cocok)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.testTag("active_file_subtitle")
                                )
                            }
                        },
                        actions = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                FilledTonalIconButton(
                                    onClick = {
                                        openSubtitleLauncher.launch(arrayOf("*/*"))
                                    },
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize()
                                        .testTag("topbar_open_file_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.FolderOpen,
                                        contentDescription = "Buka File Subtitle (.ASS/.SRT/.VTT)",
                                        modifier = Modifier.size(19.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(4.dp))
                                IconButton(
                                    onClick = { showThemeSheet = true },
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize()
                                        .testTag("open_theme_settings_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Settings,
                                        contentDescription = "Pengaturan Aplikasi & Tema",
                                        tint = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.background
                        )
                    )
                }
            },
            bottomBar = {
                if (!isWideScreen) {
                    val navItemColors = NavigationBarItemDefaults.colors(
                        indicatorColor = if (isDark) Color(0xFF214431) else SageHighlightFill,
                        selectedIconColor = if (isDark) Color(0xFF8CE0AF) else ForestGreenDark,
                        selectedTextColor = if (isDark) Color(0xFF8CE0AF) else ForestGreenDark
                    )

                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        windowInsets = WindowInsets.navigationBars
                    ) {
                        NavigationBarItem(
                            selected = selectedTab == -1,
                            onClick = { selectedTab = -1 },
                            colors = navItemColors,
                            icon = {
                                Icon(
                                    imageVector = if (selectedTab == -1) Icons.Filled.Home else Icons.Outlined.Home,
                                    contentDescription = "Beranda"
                                )
                            },
                            label = {
                                Text(
                                    text = "Beranda",
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            modifier = Modifier.testTag("nav_tab_home")
                        )

                        NavigationBarItem(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            colors = navItemColors,
                            icon = {
                                Icon(
                                    imageVector = if (selectedTab == 0) Icons.Filled.Description else Icons.Outlined.Description,
                                    contentDescription = "Tab Ekstrak & Copy"
                                )
                            },
                            label = {
                                Text(
                                    text = "Ekstrak",
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            modifier = Modifier.testTag("nav_tab_extract")
                        )

                        NavigationBarItem(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            colors = navItemColors,
                            icon = {
                                BadgedBox(
                                    badge = {
                                        if (uiState.matchedCount > 0) {
                                            val isAllMatched = uiState.matchedCount >= uiState.document.cues.size && uiState.document.cues.isNotEmpty()
                                            Badge(
                                                containerColor = if (isAllMatched) ForestGreenDark else MaterialTheme.colorScheme.primary,
                                                contentColor = Color.White
                                            ) {
                                                Text(if (isAllMatched) "✓" else "${uiState.matchedCount}")
                                            }
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = if (selectedTab == 1) Icons.Filled.ContentPaste else Icons.Outlined.ContentPaste,
                                        contentDescription = "Tab Paste AI"
                                    )
                                }
                            },
                            label = {
                                Text(
                                    text = "Paste AI",
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            modifier = Modifier.testTag("nav_tab_paste")
                        )

                        NavigationBarItem(
                            selected = selectedTab == 2,
                            onClick = { selectedTab = 2 },
                            colors = navItemColors,
                            icon = {
                                BadgedBox(
                                    badge = {
                                        if (enabledRulesCount > 0) {
                                            Badge(
                                                containerColor = ForestGreenDark,
                                                contentColor = Color.White
                                            ) {
                                                Text("$enabledRulesCount")
                                            }
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = if (selectedTab == 2) Icons.Filled.FindReplace else Icons.Outlined.FindReplace,
                                        contentDescription = "Tab Find & Replace dan Glosarium"
                                    )
                                }
                            },
                            label = {
                                Text(
                                    text = "Kamus",
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            modifier = Modifier.testTag("nav_tab_glossary")
                        )

                        NavigationBarItem(
                            selected = selectedTab == 3,
                            onClick = { selectedTab = 3 },
                            colors = navItemColors,
                            icon = {
                                Icon(
                                    imageVector = if (selectedTab == 3) Icons.Filled.MergeType else Icons.Outlined.MergeType,
                                    contentDescription = "Tab Merge & Simpan"
                                )
                            },
                            label = {
                                Text(
                                    text = "Merge",
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            modifier = Modifier.testTag("nav_tab_merge")
                        )

                        NavigationBarItem(
                            selected = selectedTab == 4,
                            onClick = { selectedTab = 4 },
                            colors = navItemColors,
                            icon = {
                                BadgedBox(
                                    badge = {
                                        if (batchState.totalFiles > 0) {
                                            Badge(
                                                containerColor = ForestGreenDark,
                                                contentColor = Color.White
                                            ) {
                                                Text("${batchState.completedCount}/${batchState.totalFiles}")
                                            }
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = if (selectedTab == 4) Icons.Filled.QueuePlayNext else Icons.Outlined.QueuePlayNext,
                                        contentDescription = "Tab Batch Subtitle Translation Queue"
                                    )
                                }
                            },
                            label = {
                                Text(
                                    text = "Batch",
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            modifier = Modifier.testTag("nav_tab_batch")
                        )
                    }
                }
            }
        ) { innerPadding ->
            if (isWideScreen) {
                // Wide Screen Layout with Dark Forest Green Navigation Rail (matching Panel 2 of Reference Design!)
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    ForestStudioNavigationRail(
                        selectedTab = selectedTab,
                        onSelectTab = { selectedTab = it },
                        onOpenSettings = { showThemeSheet = true }
                    )

                    Box(modifier = Modifier.fillMaxSize()) {
                        ActiveTabContent(
                            selectedTab = selectedTab,
                            isWideScreen = true,
                            uiState = uiState,
                            aiSettings = aiSettings,
                            glossaryRules = glossaryRules,
                            themeSettings = themeSettings,
                            batchState = batchState,
                            viewModel = viewModel,
                            onNavigateToTab = { selectedTab = it },
                            onOpenFilePicker = { openSubtitleLauncher.launch(arrayOf("*/*")) },
                            onOpenSettings = { showThemeSheet = true },
                            onSaveMergedFile = { suggestedFileName ->
                                createSubtitleLauncher.launch(suggestedFileName)
                            },
                            onSelectMultipleFiles = {
                                openMultipleSubtitlesLauncher.launch(arrayOf("*/*"))
                            },
                            onChooseOutputFolder = {
                                openBatchOutputFolderLauncher.launch(null)
                            },
                            onNavigateToExtractSettings = {
                                selectedTab = 0
                            }
                        )
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    ActiveTabContent(
                        selectedTab = selectedTab,
                        isWideScreen = false,
                        uiState = uiState,
                        aiSettings = aiSettings,
                        glossaryRules = glossaryRules,
                        themeSettings = themeSettings,
                        batchState = batchState,
                        viewModel = viewModel,
                        onNavigateToTab = { selectedTab = it },
                        onOpenFilePicker = { openSubtitleLauncher.launch(arrayOf("*/*")) },
                        onOpenSettings = { showThemeSheet = true },
                        onSaveMergedFile = { suggestedFileName ->
                            createSubtitleLauncher.launch(suggestedFileName)
                        },
                        onSelectMultipleFiles = {
                            openMultipleSubtitlesLauncher.launch(arrayOf("*/*"))
                        },
                        onChooseOutputFolder = {
                            openBatchOutputFolderLauncher.launch(null)
                        },
                        onNavigateToExtractSettings = {
                            selectedTab = 0
                        }
                    )
                }
            }
        }
    }

    if (showThemeSheet) {
        ThemeSettingsSheet(
            themeSettings = themeSettings,
            viewModel = viewModel,
            onDismiss = { showThemeSheet = false }
        )
    }
}

@Composable
private fun ForestStudioNavigationRail(
    selectedTab: Int,
    onSelectTab: (Int) -> Unit,
    onOpenSettings: () -> Unit
) {
    Surface(
        color = ForestGreenDark,
        shape = RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp),
        modifier = Modifier
            .fillMaxHeight()
            .width(68.dp)
            .windowInsetsPadding(WindowInsets.navigationBars)
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Top Brand Mark (//)
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onSelectTab(-1) },
                    contentAlignment = Alignment.Center
                ) {
                    SubAiParallelogramLogo(
                        color = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                ForestRailIconItem(
                    icon = Icons.Outlined.Home,
                    contentDescription = "Beranda",
                    selected = selectedTab == -1,
                    testTag = "nav_tab_home",
                    onClick = { onSelectTab(-1) }
                )
                ForestRailIconItem(
                    icon = Icons.Outlined.Description,
                    contentDescription = "Ekstrak & Terjemahkan",
                    selected = selectedTab == 0,
                    testTag = "nav_tab_extract",
                    onClick = { onSelectTab(0) }
                )
                ForestRailIconItem(
                    icon = Icons.Outlined.ContentPaste,
                    contentDescription = "Paste AI",
                    selected = selectedTab == 1,
                    testTag = "nav_tab_paste",
                    onClick = { onSelectTab(1) }
                )
                ForestRailIconItem(
                    icon = Icons.Outlined.Search,
                    contentDescription = "Find & Kamus",
                    selected = selectedTab == 2,
                    testTag = "nav_tab_glossary",
                    onClick = { onSelectTab(2) }
                )
                ForestRailIconItem(
                    icon = Icons.Outlined.Layers,
                    contentDescription = "Merge Subtitle",
                    selected = selectedTab == 3,
                    testTag = "nav_tab_merge",
                    onClick = { onSelectTab(3) }
                )
                ForestRailIconItem(
                    icon = Icons.Outlined.DynamicFeed,
                    contentDescription = "Batch Translation",
                    selected = selectedTab == 4,
                    testTag = "nav_tab_batch",
                    onClick = { onSelectTab(4) }
                )
            }

            // Bottom Settings Gear Icon
            ForestRailIconItem(
                icon = Icons.Outlined.Settings,
                contentDescription = "Pengaturan",
                selected = false,
                testTag = "open_theme_settings_button",
                onClick = onOpenSettings
            )
        }
    }
}

@Composable
private fun ForestRailIconItem(
    icon: ImageVector,
    contentDescription: String,
    selected: Boolean,
    testTag: String,
    onClick: () -> Unit
) {
    val bg = if (selected) Color(0xFF2D5E46) else Color.Transparent
    val tint = if (selected) Color(0xFFB7EFC5) else Color.White.copy(alpha = 0.78f)

    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(22.dp)
        )
    }
}

@Composable
private fun ActiveTabContent(
    selectedTab: Int,
    isWideScreen: Boolean,
    uiState: SubtitleUiState,
    aiSettings: AiProviderSettings,
    glossaryRules: List<GlossaryRule>,
    themeSettings: ThemeSettings,
    batchState: BatchQueueState,
    viewModel: SubtitleViewModel,
    onNavigateToTab: (Int) -> Unit,
    onOpenFilePicker: () -> Unit,
    onOpenSettings: () -> Unit,
    onSaveMergedFile: (String) -> Unit,
    onSelectMultipleFiles: () -> Unit,
    onChooseOutputFolder: () -> Unit,
    onNavigateToExtractSettings: () -> Unit
) {
    when (selectedTab) {
        -1 -> HomeDashboardScreen(
            uiState = uiState,
            batchState = batchState,
            onNavigateToTab = onNavigateToTab,
            onOpenFilePicker = onOpenFilePicker,
            onOpenSettings = onOpenSettings
        )

        0 -> ExtractTabScreen(
            uiState = uiState,
            aiSettings = aiSettings,
            glossaryRules = glossaryRules,
            themeSettings = themeSettings,
            viewModel = viewModel,
            onOpenFilePicker = onOpenFilePicker,
            isWideScreen = isWideScreen
        )

        1 -> PasteTabScreen(
            uiState = uiState,
            themeSettings = themeSettings,
            viewModel = viewModel,
            isWideScreen = isWideScreen
        )

        2 -> GlossaryFindReplaceTabScreen(
            uiState = uiState,
            glossaryRules = glossaryRules,
            themeSettings = themeSettings,
            viewModel = viewModel,
            isWideScreen = isWideScreen
        )

        3 -> MergeTabScreen(
            uiState = uiState,
            themeSettings = themeSettings,
            viewModel = viewModel,
            onSaveMergedFile = onSaveMergedFile,
            isWideScreen = isWideScreen
        )

        4 -> BatchQueueTabScreen(
            uiState = uiState,
            aiSettings = aiSettings,
            batchState = batchState,
            viewModel = viewModel,
            onSelectMultipleFiles = onSelectMultipleFiles,
            onChooseOutputFolder = onChooseOutputFolder,
            onNavigateToExtractSettings = onNavigateToExtractSettings
        )
    }
}
