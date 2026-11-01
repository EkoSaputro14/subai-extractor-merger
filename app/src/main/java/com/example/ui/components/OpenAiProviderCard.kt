package com.example.ui.components

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Cookie
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.PlayCircleFilled
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.model.AiProviderPreset
import com.example.model.AiProviderSettings
import com.example.model.GlossaryRule
import com.example.model.ThemeSettings
import com.example.ui.theme.subtitleEditorTextStyle
import com.example.viewmodel.SubtitleUiState
import com.example.viewmodel.SubtitleViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OpenAiDirectTranslateCard(
    uiState: SubtitleUiState,
    aiSettings: AiProviderSettings,
    glossaryRules: List<GlossaryRule>,
    themeSettings: ThemeSettings,
    viewModel: SubtitleViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showApiKeyPlain by rememberSaveable { mutableStateOf(false) }
    var showCookiePlain by rememberSaveable { mutableStateOf(false) }
    var showProviderConfig by rememberSaveable { mutableStateOf(true) }
    var showSystemPromptPreview by rememberSaveable { mutableStateOf(false) }
    var showCookieHelp by rememberSaveable { mutableStateOf(false) }
    var showWebViewCookieGrabber by rememberSaveable { mutableStateOf(false) }

    val modelProfile = aiSettings.activeModelProfile
    val adaptedSystemPrompt = remember(
        aiSettings.selectedModel,
        uiState.separatorMode,
        uiState.customSeparator,
        uiState.indexFormatMode,
        uiState.includeGlossaryInPrompt,
        uiState.targetLanguageOption,
        uiState.customTargetLanguage,
        uiState.stylePreset,
        uiState.customPromptInstruction,
        glossaryRules
    ) {
        viewModel.buildActiveModelSystemPrompt()
    }

    if (showWebViewCookieGrabber) {
        GeminiCookieWebViewDialog(
            onDismiss = { showWebViewCookieGrabber = false },
            onCookiesExtracted = { extracted ->
                viewModel.applyExtractedCookies(extracted)
            }
        )
    }

    StudioSectionCard(
        title = "Terjemahkan AI (Gemini-Web2API & OpenAI)",
        subtitle = "Mendukung Cookie Gemini Web (__Secure-1PSID) & System Prompt adaptif (${aiSettings.selectedModel})",
        icon = Icons.Default.SmartToy,
        iconTint = MaterialTheme.colorScheme.secondary,
        modifier = modifier,
        headerTrailing = {
            FilterChip(
                selected = showProviderConfig,
                onClick = { showProviderConfig = !showProviderConfig },
                label = { Text(if (showProviderConfig) "Ringkas" else "Konfigurasi") },
                modifier = Modifier.testTag("toggle_ai_provider_config_chip")
            )
        }
    ) {
        // Model Profile Adaptation Badge
        Surface(
            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = modelProfile.badgeTitle,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("model_profile_badge_title")
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    StatusPillBadge(
                        text = aiSettings.preset.label,
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Text(
                    text = modelProfile.adaptationSummary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        AnimatedVisibility(visible = showProviderConfig) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Pilih Preset Provider AI:",
                    style = MaterialTheme.typography.titleSmall
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    AiProviderPreset.entries.forEach { preset ->
                        FilterChip(
                            selected = aiSettings.preset == preset,
                            onClick = { viewModel.selectAiProviderPreset(preset) },
                            label = { Text(preset.label) },
                            modifier = Modifier.testTag("ai_preset_${preset.name.lowercase()}")
                        )
                    }
                }

                // Dedicated Cookie Configuration Card for Gemini-Web2API
                if (aiSettings.preset.isGeminiWebFamily) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
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
                                    Icon(
                                        imageVector = Icons.Default.Cookie,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "Autentikasi Cookie Gemini-Web2API",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                StatusPillBadge(
                                    text = if (aiSettings.hasGeminiCookie) "Cookie Tersimpan" else "Cookie Kosong",
                                    containerColor = if (aiSettings.hasGeminiCookie) {
                                        MaterialTheme.colorScheme.primaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.errorContainer
                                    },
                                    contentColor = if (aiSettings.hasGeminiCookie) {
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.onErrorContainer
                                    }
                                )
                            }

                            // 3 Fast Cookie Input Action Buttons: WebView Turbo, External Browser, & Smart Paste
                            ResponsiveActionButtonGrid(
                                buttons = listOf(
                                    ActionButtonSpec(
                                        label = "WebView Turbo (Cepat)",
                                        icon = Icons.Default.Language,
                                        testTag = "open_cookie_webview_button",
                                        kind = ActionButtonKind.PRIMARY,
                                        onClick = { showWebViewCookieGrabber = true }
                                    ),
                                    ActionButtonSpec(
                                        label = "Buka Browser HP",
                                        icon = Icons.Default.Bolt,
                                        testTag = "open_external_browser_cookie_button",
                                        kind = ActionButtonKind.OUTLINED,
                                        onClick = {
                                            runCatching {
                                                val intent = android.content.Intent(
                                                    android.content.Intent.ACTION_VIEW,
                                                    android.net.Uri.parse("https://gemini.google.com/app")
                                                ).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                                context.startActivity(intent)
                                            }
                                        }
                                    ),
                                    ActionButtonSpec(
                                        label = "Smart Paste Cookie",
                                        icon = Icons.Default.ContentPaste,
                                        testTag = "smart_paste_cookie_button",
                                        kind = ActionButtonKind.TONAL,
                                        onClick = {
                                            val clip = readFromClipboard(context)
                                            viewModel.applySmartPastedCookies(clip)
                                        }
                                    )
                                )
                            )

                            OutlinedTextField(
                                value = aiSettings.geminiSecure1Psid,
                                onValueChange = { viewModel.updateGeminiSecure1Psid(it) },
                                label = { Text("Cookie __Secure-1PSID (Wajib)") },
                                placeholder = { Text("g.a000... (atau tekan Smart Paste / Login Otomatis)") },
                                singleLine = true,
                                visualTransformation = if (showCookiePlain) {
                                    VisualTransformation.None
                                } else {
                                    PasswordVisualTransformation()
                                },
                                trailingIcon = {
                                    IconButton(onClick = { showCookiePlain = !showCookiePlain }) {
                                        Icon(
                                            imageVector = if (showCookiePlain) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                            contentDescription = "Tampilkan atau sembunyikan Cookie"
                                        )
                                    }
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("gemini_psid_input")
                            )

                            OutlinedTextField(
                                value = aiSettings.geminiSecure1Psidts,
                                onValueChange = { viewModel.updateGeminiSecure1Psidts(it) },
                                label = { Text("Cookie __Secure-1PSIDTS (Disarankan)") },
                                placeholder = { Text("sidts-CjEB...") },
                                singleLine = true,
                                visualTransformation = if (showCookiePlain) {
                                    VisualTransformation.None
                                } else {
                                    PasswordVisualTransformation()
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("gemini_psidts_input")
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedButton(
                                    onClick = { viewModel.verifyGeminiCookiesNow() },
                                    enabled = !uiState.isVerifyingCookie && aiSettings.hasGeminiCookie,
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .defaultMinSize(minHeight = 48.dp)
                                        .testTag("verify_gemini_cookie_button")
                                ) {
                                    if (uiState.isVerifyingCookie) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            strokeWidth = 2.dp
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.VerifiedUser,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Tes Cookie Gemini")
                                }

                                FilterChip(
                                    selected = showCookieHelp,
                                    onClick = { showCookieHelp = !showCookieHelp },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.HelpOutline,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    },
                                    label = { Text("Panduan Cookie") },
                                    modifier = Modifier
                                        .defaultMinSize(minHeight = 48.dp)
                                        .testTag("toggle_cookie_help_chip")
                                )
                            }

                            AnimatedVisibility(visible = showCookieHelp) {
                                Surface(
                                    color = MaterialTheme.colorScheme.surface,
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(10.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = "3 Cara Memasukkan Cookie Gemini-Web2API:",
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = "1. Paling Mudah (Di HP): Ketuk 'Login & Ambil Cookie Otomatis', login ke gemini.google.com, lalu ketuk 'Ambil Cookie Sekarang'.",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                        Text(
                                            text = "2. Ekstensi Cookie-Editor (Kiwi Browser / PC): Buka gemini.google.com -> Export as JSON / Header String -> ketuk 'Smart Paste Cookie'.",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                        Text(
                                            text = "3. Inspect Element (F12): Tab Application -> Cookies -> https://gemini.google.com -> salin nilai __Secure-1PSID dan __Secure-1PSIDTS.",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Endpoint URL & API Key for Server Mode / OpenAI-Compatible Providers
                if (aiSettings.preset != AiProviderPreset.GEMINI_WEB_COOKIE) {
                    OutlinedTextField(
                        value = aiSettings.baseUrl,
                        onValueChange = { viewModel.updateAiBaseUrl(it) },
                        label = { Text("Endpoint Base URL (Server Gemini-Web2API / OpenAI)") },
                        placeholder = { Text("http://127.0.0.1:8000/v1") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("ai_base_url_input")
                    )

                    if (aiSettings.preset == AiProviderPreset.GEMINI_WEB_SERVER ||
                        aiSettings.preset == AiProviderPreset.CUSTOM
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(
                                "Termux :8000" to "http://127.0.0.1:8000/v1",
                                "Termux :8081" to "http://127.0.0.1:8081/v1",
                                "Emulator :8000" to "http://10.0.2.2:8000/v1"
                            ).forEach { (label, url) ->
                                FilterChip(
                                    selected = aiSettings.baseUrl == url,
                                    onClick = { viewModel.updateAiBaseUrl(url) },
                                    label = { Text(label) }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = aiSettings.apiKey,
                        onValueChange = { viewModel.updateAiApiKey(it) },
                        label = {
                            Text(
                                if (aiSettings.preset == AiProviderPreset.GEMINI_WEB_SERVER) {
                                    "API Key Server (Opsional untuk gemini-web2api lokal)"
                                } else {
                                    "API Key Provider (Opsional jika diatur di Secrets)"
                                }
                            )
                        },
                        placeholder = { Text("sk-... (Kosongkan jika memakai Cookie / Server Lokal)") },
                        singleLine = true,
                        visualTransformation = if (showApiKeyPlain) {
                            VisualTransformation.None
                        } else {
                            PasswordVisualTransformation()
                        },
                        trailingIcon = {
                            IconButton(onClick = { showApiKeyPlain = !showApiKeyPlain }) {
                                Icon(
                                    imageVector = if (showApiKeyPlain) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = "Tampilkan atau sembunyikan API Key"
                                )
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("ai_api_key_input")
                    )

                    StudioToggleRow(
                        title = "Gabung System Prompt ke User Prompt (Mode Gemini Web)",
                        description = "Menjamin instruksi nomor [1] dan simbol pemisah 100% dipatuhi oleh antarmuka Gemini Web.",
                        checked = aiSettings.combineSystemIntoUserPrompt,
                        onCheckedChange = { viewModel.updateCombineSystemIntoUserPrompt(it) },
                        testTag = "combine_system_user_prompt_switch"
                    )
                }

                // Model Input + Fetch Models Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = aiSettings.selectedModel,
                        onValueChange = { viewModel.selectAiModel(it) },
                        label = { Text("Model Bahasa Aktif") },
                        placeholder = { Text("gemini-2.5-flash / gemini-2.5-pro") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("ai_model_id_input")
                    )

                    FilledTonalButton(
                        onClick = { viewModel.fetchModelsFromProvider() },
                        enabled = !uiState.isFetchingModels,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .defaultMinSize(minHeight = 52.dp)
                            .testTag("fetch_ai_models_button")
                    ) {
                        if (uiState.isFetchingModels) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.CloudDownload,
                                contentDescription = "Fetch daftar model",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Fetch Model")
                    }
                }

                // Quick Model Chips (Fetched + Recommended)
                val modelChoices = aiSettings.availableModelChoices
                if (modelChoices.isNotEmpty()) {
                    Text(
                        text = if (aiSettings.fetchedModels.isNotEmpty()) {
                            "Pilih Varian Model (${aiSettings.fetchedModels.size} tersedia):"
                        } else {
                            "Rekomendasi Varian Model (${aiSettings.preset.label}):"
                        },
                        style = MaterialTheme.typography.labelLarge
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        modelChoices.take(30).forEach { modelName ->
                            FilterChip(
                                selected = aiSettings.selectedModel == modelName,
                                onClick = { viewModel.selectAiModel(modelName) },
                                label = { Text(modelName) },
                                modifier = Modifier.testTag("model_chip_$modelName")
                            )
                        }
                    }
                }
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

        // Collapsible Preview of Model-Adapted System Prompt
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "System Prompt Adaptif (${aiSettings.selectedModel})",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.weight(1f)
            )
            FilterChip(
                selected = showSystemPromptPreview,
                onClick = { showSystemPromptPreview = !showSystemPromptPreview },
                label = { Text(if (showSystemPromptPreview) "Tutup Prompt" else "Lihat Prompt") },
                modifier = Modifier.testTag("toggle_system_prompt_preview_chip")
            )
        }

        AnimatedVisibility(visible = showSystemPromptPreview) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 120.dp, max = 240.dp),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                border = BorderStroke(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(12.dp)
                ) {
                    Text(
                        text = adaptedSystemPrompt,
                        style = subtitleEditorTextStyle(themeSettings),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.testTag("adapted_system_prompt_text")
                    )
                }
            }
        }

        // Live Translation Progress Banner
        if (uiState.isTranslatingAi || !uiState.aiTranslationStatusText.isNullOrBlank()) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = uiState.aiTranslationStatusText ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.testTag("ai_translation_status_text")
                    )
                    if (uiState.isTranslatingAi) {
                        val batchFraction = if (uiState.aiTotalBatches > 0) {
                            (uiState.aiCompletedBatches.toFloat() / uiState.aiTotalBatches.toFloat()).coerceIn(0f, 1f)
                        } else {
                            0f
                        }
                        if (uiState.aiTotalBatches > 1) {
                            LinearProgressIndicator(
                                progress = { batchFraction },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                            )
                        } else {
                            LinearProgressIndicator(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                            )
                        }
                    }
                }
            }
        }

        // Direct In-App Translation Buttons (Current Batch vs All Batches + Realtime Animation Demo + Stop)
        if (uiState.isTranslatingAi) {
            Button(
                onClick = { viewModel.cancelAiTranslation() },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 48.dp)
                    .testTag("stop_ai_translation_button")
            ) {
                Icon(Icons.Default.StopCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Hentikan Proses Terjemahan AI")
            }
        } else {
            val totalBatches = uiState.batches.size
            val activeBatchNum = uiState.selectedBatchIndex + 1
            ResponsivePrimarySecondaryButtons(
                primaryLabel = "Terjemahkan Batch #$activeBatchNum (AI)",
                primaryIcon = Icons.Default.Bolt,
                primaryTestTag = "translate_active_batch_ai_button",
                onPrimaryClick = { viewModel.translateCurrentBatchWithAi() },
                secondaryLabel = "Semua Batch ($totalBatches Batch)",
                secondaryIcon = Icons.Default.PlayCircleFilled,
                secondaryTestTag = "translate_all_batches_ai_button",
                onSecondaryClick = { viewModel.translateAllBatchesWithAi() }
            )

            OutlinedButton(
                onClick = { viewModel.simulateRealtimeTranslationOnActiveBatch() },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 48.dp)
                    .testTag("simulate_realtime_card_translation_button")
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Putar Animasi Terjemahan Realtime pada Card")
            }
        }
    }
}

private fun readFromClipboard(context: Context): String {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    val item = clipboard?.primaryClip?.getItemAt(0) ?: return ""
    return item.coerceToText(context)?.toString().orEmpty()
}
