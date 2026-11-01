package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FindReplace
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.MergeType
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.QueuePlayNext
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.FindReplace
import androidx.compose.material.icons.outlined.MergeType
import androidx.compose.material.icons.outlined.QueuePlayNext
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.VerticalDivider
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
import androidx.compose.ui.platform.testTag
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
import com.example.ui.screens.MergeTabScreen
import com.example.ui.screens.PasteTabScreen
import com.example.ui.screens.ThemeSettingsSheet
import com.example.ui.theme.SubAiExtractorTheme
import com.example.viewmodel.SubtitleUiState
import com.example.viewmodel.SubtitleViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val initialTab = intent?.getIntExtra("open_tab", 0) ?: 0
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
    initialTab: Int = 0
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val aiSettings by viewModel.aiSettings.collectAsStateWithLifecycle()
    val glossaryRules by viewModel.glossaryRules.collectAsStateWithLifecycle()
    val themeSettings by viewModel.themeSettings.collectAsStateWithLifecycle()
    val batchState by viewModel.batchQueueState.collectAsStateWithLifecycle()

    var selectedTab by rememberSaveable { mutableIntStateOf(initialTab.coerceIn(0, 4)) }
    var showThemeSheet by rememberSaveable { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    // Return to Tab 0 (Ekstrak) when Back is pressed on secondary tabs
    BackHandler(enabled = selectedTab != 0) {
        selectedTab = 0
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

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWideScreen = maxWidth >= 600.dp

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            topBar = {
                TopAppBar(
                    windowInsets = WindowInsets.statusBars,
                    title = {
                        Column {
                            Text(
                                text = "SubAI Extractor & Merger",
                                style = MaterialTheme.typography.titleMedium,
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
                                    modifier = Modifier.size(20.dp)
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
                                    imageVector = Icons.Default.Palette,
                                    contentDescription = "Pengaturan Tema Warna & Editor",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            },
            bottomBar = {
                if (!isWideScreen) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        windowInsets = WindowInsets.navigationBars
                    ) {
                        NavigationBarItem(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
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
                            icon = {
                                BadgedBox(
                                    badge = {
                                        if (uiState.matchedCount > 0) {
                                            Badge {
                                                Text("${uiState.matchedCount}/${uiState.document.cues.size}")
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
                            icon = {
                                BadgedBox(
                                    badge = {
                                        if (enabledRulesCount > 0) {
                                            Badge { Text("$enabledRulesCount") }
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
                                    text = "Find & Kamus",
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            modifier = Modifier.testTag("nav_tab_glossary")
                        )

                        NavigationBarItem(
                            selected = selectedTab == 3,
                            onClick = { selectedTab = 3 },
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
                            icon = {
                                BadgedBox(
                                    badge = {
                                        if (batchState.totalFiles > 0) {
                                            Badge {
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
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surface,
                        modifier = Modifier
                            .fillMaxHeight()
                            .windowInsetsPadding(WindowInsets.navigationBars)
                    ) {
                        NavigationRailItem(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            icon = {
                                Icon(
                                    imageVector = if (selectedTab == 0) Icons.Filled.Description else Icons.Outlined.Description,
                                    contentDescription = "Tab Ekstrak & Copy"
                                )
                            },
                            label = { Text("Ekstrak") },
                            modifier = Modifier.testTag("nav_tab_extract")
                        )

                        NavigationRailItem(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            icon = {
                                BadgedBox(
                                    badge = {
                                        if (uiState.matchedCount > 0) {
                                            Badge {
                                                Text("${uiState.matchedCount}/${uiState.document.cues.size}")
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
                            label = { Text("Paste AI") },
                            modifier = Modifier.testTag("nav_tab_paste")
                        )

                        NavigationRailItem(
                            selected = selectedTab == 2,
                            onClick = { selectedTab = 2 },
                            icon = {
                                BadgedBox(
                                    badge = {
                                        if (enabledRulesCount > 0) {
                                            Badge { Text("$enabledRulesCount") }
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = if (selectedTab == 2) Icons.Filled.FindReplace else Icons.Outlined.FindReplace,
                                        contentDescription = "Tab Find & Replace dan Glosarium"
                                    )
                                }
                            },
                            label = { Text("Kamus") },
                            modifier = Modifier.testTag("nav_tab_glossary")
                        )

                        NavigationRailItem(
                            selected = selectedTab == 3,
                            onClick = { selectedTab = 3 },
                            icon = {
                                Icon(
                                    imageVector = if (selectedTab == 3) Icons.Filled.MergeType else Icons.Outlined.MergeType,
                                    contentDescription = "Tab Merge & Simpan"
                                )
                            },
                            label = { Text("Merge") },
                            modifier = Modifier.testTag("nav_tab_merge")
                        )

                        NavigationRailItem(
                            selected = selectedTab == 4,
                            onClick = { selectedTab = 4 },
                            icon = {
                                BadgedBox(
                                    badge = {
                                        if (batchState.totalFiles > 0) {
                                            Badge {
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
                            label = { Text("Batch") },
                            modifier = Modifier.testTag("nav_tab_batch")
                        )
                    }

                    VerticalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
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
                            onOpenFilePicker = { openSubtitleLauncher.launch(arrayOf("*/*")) },
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
                        onOpenFilePicker = { openSubtitleLauncher.launch(arrayOf("*/*")) },
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
private fun ActiveTabContent(
    selectedTab: Int,
    isWideScreen: Boolean,
    uiState: SubtitleUiState,
    aiSettings: AiProviderSettings,
    glossaryRules: List<GlossaryRule>,
    themeSettings: ThemeSettings,
    batchState: BatchQueueState,
    viewModel: SubtitleViewModel,
    onOpenFilePicker: () -> Unit,
    onSaveMergedFile: (String) -> Unit,
    onSelectMultipleFiles: () -> Unit,
    onChooseOutputFolder: () -> Unit,
    onNavigateToExtractSettings: () -> Unit
) {
    when (selectedTab) {
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
