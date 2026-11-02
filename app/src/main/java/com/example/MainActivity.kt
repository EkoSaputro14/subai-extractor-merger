package com.example

import android.content.Context
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.DynamicFeed
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalContext
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
import com.example.ui.components.OpenAiDirectTranslateCard
import com.example.ui.screens.BatchQueueTabScreen
import com.example.ui.screens.ExtractTabScreen
import com.example.ui.screens.GlossaryFindReplaceTabScreen
import com.example.ui.screens.HomeDashboardScreen
import com.example.ui.screens.MergeTabScreen
import com.example.ui.screens.PasteTabScreen
import com.example.ui.screens.SettingsScreenContent
import com.example.ui.screens.SubAiOnboardingScreen
import com.example.ui.screens.SubAiParallelogramLogo
import com.example.ui.screens.SubAiSplashScreen
import com.example.ui.theme.AlabasterSheetBg
import com.example.ui.theme.DeepForestHeaderBg
import com.example.ui.theme.ForestGreenDark
import com.example.ui.theme.SubAiExtractorTheme
import com.example.viewmodel.SubtitleUiState
import com.example.viewmodel.SubtitleViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val hasExplicitTab = intent?.hasExtra("open_tab") == true
        val initialTab = if (hasExplicitTab) {
            intent.getIntExtra("open_tab", -1)
        } else {
            -1
        }
        val skipIntro = intent?.getBooleanExtra("skip_intro", false) == true || hasExplicitTab

        setContent {
            val subtitleViewModel: SubtitleViewModel = viewModel()
            val themeSettings by subtitleViewModel.themeSettings.collectAsStateWithLifecycle()

            SubAiExtractorTheme(themeSettings = themeSettings) {
                SubAiExtractorMainScreen(
                    viewModel = subtitleViewModel,
                    initialTab = initialTab,
                    enableFirstLaunchIntro = !skipIntro
                )
            }
        }
    }
}

private const val INTRO_PREFS_NAME = "subai_intro_prefs"
private const val KEY_SEEN_ONBOARDING_V1_3 = "seen_onboarding_v1_3"

@Composable
fun SubAiExtractorMainScreen(
    viewModel: SubtitleViewModel,
    initialTab: Int = -1,
    enableFirstLaunchIntro: Boolean = false
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val aiSettings by viewModel.aiSettings.collectAsStateWithLifecycle()
    val glossaryRules by viewModel.glossaryRules.collectAsStateWithLifecycle()
    val themeSettings by viewModel.themeSettings.collectAsStateWithLifecycle()
    val batchState by viewModel.batchQueueState.collectAsStateWithLifecycle()

    // Intro state: 0 = Main App, 1 = Screen 1 Splash, 2 = Screen 2 Onboarding
    var introStep by rememberSaveable {
        val prefs = context.getSharedPreferences(INTRO_PREFS_NAME, Context.MODE_PRIVATE)
        val alreadySeen = prefs.getBoolean(KEY_SEEN_ONBOARDING_V1_3, false)
        mutableIntStateOf(if (enableFirstLaunchIntro && !alreadySeen) 1 else 0)
    }

    // -1 = Beranda (Screen 3)
    //  0 = Ekstrak & Terjemahkan (Screen 4)
    //  1 = Paste AI (Screen 5)
    //  2 = Find & Kamus (Screens 6 & 7)
    //  3 = Merge Subtitle (Screen 8)
    //  4 = Batch Queue (Screen 9)
    //  5 = Pengaturan (Screen 10)
    var selectedTab by rememberSaveable { mutableIntStateOf(initialTab.coerceIn(-1, 5)) }
    val snackbarHostState = remember { SnackbarHostState() }
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    // Return to Beranda (-1) when Back is pressed on secondary screens (0..5)
    BackHandler(enabled = introStep == 0 && selectedTab != -1) {
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

    // Screen 1 -> Screen 2 -> Screen 3 flow matching the 10-screen Reference Image
    if (introStep == 1) {
        SubAiSplashScreen(
            onFinished = { introStep = 2 }
        )
        return
    }

    if (introStep == 2) {
        SubAiOnboardingScreen(
            onStartClicked = {
                context.getSharedPreferences(INTRO_PREFS_NAME, Context.MODE_PRIVATE)
                    .edit()
                    .putBoolean(KEY_SEEN_ONBOARDING_V1_3, true)
                    .apply()
                introStep = 0
                selectedTab = -1
            }
        )
        return
    }

    val activeScreenTitle = when (selectedTab) {
        -1 -> "SubAI"
        0 -> "Ekstrak & Terjemahkan"
        1 -> "Paste AI"
        2 -> "Find & Kamus"
        3 -> "Merge Subtitle"
        4 -> "Batch Queue"
        5 -> "Pengaturan"
        else -> "SubAI"
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWideScreen = maxWidth >= 600.dp

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = DeepForestHeaderBg,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
        ) { innerPadding ->
            if (isWideScreen) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    ForestStudioNavigationRail(
                        selectedTab = selectedTab,
                        onSelectTab = { selectedTab = it },
                        onOpenSettings = { selectedTab = 5 }
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(if (isDark) MaterialTheme.colorScheme.background else AlabasterSheetBg)
                    ) {
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
                            onOpenSettings = { selectedTab = 5 },
                            onReplayOnboarding = { introStep = 1 },
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
                // Compact Portrait Layout matching Screens 3..10 of the 10-screen Reference Image
                if (selectedTab == -1) {
                    // SCREEN 3: Home Dashboard (Full-screen Deep Forest Green with integrated header & bottom dock)
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        HomeDashboardScreen(
                            uiState = uiState,
                            batchState = batchState,
                            onNavigateToTab = { selectedTab = it },
                            onOpenFilePicker = { openSubtitleLauncher.launch(arrayOf("*/*")) },
                            onOpenSettings = { selectedTab = 5 }
                        )
                    }
                } else {
                    // SCREENS 4..10: Deep Forest Green Outer Frame (#132F23) + White Top Bar (← Title  ⋮)
                    // + Rounded-Top Alabaster Sheet (#F3F6F3, RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(DeepForestHeaderBg)
                            .windowInsetsPadding(WindowInsets.statusBars)
                    ) {
                        SecondaryScreenForestHeader(
                            title = activeScreenTitle,
                            showOverflowMenu = selectedTab != 5,
                            onBackToHome = { selectedTab = -1 },
                            onSelectTab = { selectedTab = it },
                            onOpenFilePicker = { openSubtitleLauncher.launch(arrayOf("*/*")) },
                            onLoadSampleAss = { viewModel.loadSampleAss() },
                            onLoadSampleSrt = { viewModel.loadSampleSrt() }
                        )

                        Surface(
                            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                            color = if (isDark) MaterialTheme.colorScheme.background else AlabasterSheetBg,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .windowInsetsPadding(WindowInsets.navigationBars)
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
                                    onOpenSettings = { selectedTab = 5 },
                                    onReplayOnboarding = { introStep = 1 },
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
            }
        }
    }
}

@Composable
private fun SecondaryScreenForestHeader(
    title: String,
    showOverflowMenu: Boolean,
    onBackToHome: () -> Unit,
    onSelectTab: (Int) -> Unit,
    onOpenFilePicker: () -> Unit,
    onLoadSampleAss: () -> Unit,
    onLoadSampleSrt: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            IconButton(
                onClick = onBackToHome,
                modifier = Modifier.testTag("topbar_back_to_home_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Kembali ke Beranda",
                    tint = Color.White
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (showOverflowMenu) {
            Box {
                IconButton(
                    onClick = { menuExpanded = true },
                    modifier = Modifier.testTag("topbar_overflow_menu_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Menu Navigasi & Opsi",
                        tint = Color.White
                    )
                }

                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Buka File Subtitle (.ASS/.SRT/.VTT)") },
                        leadingIcon = { Icon(Icons.Default.FolderOpen, contentDescription = null) },
                        onClick = {
                            menuExpanded = false
                            onOpenFilePicker()
                        },
                        modifier = Modifier.testTag("topbar_open_file_button")
                    )
                    DropdownMenuItem(
                        text = { Text("Muat Sampel .ASS Anime") },
                        leadingIcon = { Icon(Icons.Default.AutoAwesome, contentDescription = null) },
                        onClick = {
                            menuExpanded = false
                            onLoadSampleAss()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Muat Sampel .SRT Film") },
                        leadingIcon = { Icon(Icons.Default.Movie, contentDescription = null) },
                        onClick = {
                            menuExpanded = false
                            onLoadSampleSrt()
                        }
                    )
                    HorizontalDivider()
                    DropdownMenuItem(
                        text = { Text("Ekstrak & Terjemahkan") },
                        leadingIcon = { Icon(Icons.Outlined.Description, contentDescription = null) },
                        onClick = {
                            menuExpanded = false
                            onSelectTab(0)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Paste AI") },
                        leadingIcon = { Icon(Icons.Outlined.ContentPaste, contentDescription = null) },
                        onClick = {
                            menuExpanded = false
                            onSelectTab(1)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Find & Kamus") },
                        leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                        onClick = {
                            menuExpanded = false
                            onSelectTab(2)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Merge Subtitle") },
                        leadingIcon = { Icon(Icons.Outlined.Layers, contentDescription = null) },
                        onClick = {
                            menuExpanded = false
                            onSelectTab(3)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Batch Queue") },
                        leadingIcon = { Icon(Icons.Outlined.DynamicFeed, contentDescription = null) },
                        onClick = {
                            menuExpanded = false
                            onSelectTab(4)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Pengaturan") },
                        leadingIcon = { Icon(Icons.Outlined.Settings, contentDescription = null) },
                        onClick = {
                            menuExpanded = false
                            onSelectTab(5)
                        }
                    )
                }
            }
        }
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
                    contentDescription = "Batch Queue",
                    selected = selectedTab == 4,
                    testTag = "nav_tab_batch",
                    onClick = { onSelectTab(4) }
                )
            }

            ForestRailIconItem(
                icon = Icons.Outlined.Settings,
                contentDescription = "Pengaturan",
                selected = selectedTab == 5,
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
    onReplayOnboarding: () -> Unit,
    onSaveMergedFile: (String) -> Unit,
    onSelectMultipleFiles: () -> Unit,
    onChooseOutputFolder: () -> Unit,
    onNavigateToExtractSettings: () -> Unit
) {
    val appUpdateState by viewModel.appUpdateState.collectAsStateWithLifecycle()

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

        5 -> SettingsScreenContent(
            themeSettings = themeSettings,
            appUpdateState = appUpdateState,
            viewModel = viewModel,
            onReplayOnboarding = onReplayOnboarding,
            aiProviderSection = {
                OpenAiDirectTranslateCard(
                    uiState = uiState,
                    aiSettings = aiSettings,
                    glossaryRules = glossaryRules,
                    themeSettings = themeSettings,
                    viewModel = viewModel
                )
            }
        )
    }
}
