package com.example.ui.screens

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.DesktopWindows
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AppUpdateUiState
import com.example.model.AiProviderPreset
import com.example.model.AiProviderSettings
import com.example.model.ColorPreset
import com.example.model.GlossaryRule
import com.example.model.TargetLanguageOption
import com.example.model.ThemeMode
import com.example.model.ThemeSettings
import com.example.model.TranslationStylePreset
import com.example.ui.components.GeminiCookieWebViewDialog
import com.example.ui.theme.AlabasterSheetBg
import com.example.ui.theme.DeepForestHeaderBg
import com.example.ui.theme.EmeraldCheckGreen
import com.example.ui.theme.PrimaryForestButton
import com.example.viewmodel.SubtitleUiState
import com.example.viewmodel.SubtitleViewModel
import kotlin.math.roundToInt

/**
 * SCREEN 2A (Left of Image 2): "AI Provider"
 * Reproduces the locked reference design 100%:
 * - Top Banner Card with green SubAI app icon + "Kelola penyedia AI untuk terjemahan subtitle" + 3D teal folder illustration
 * - Card "Penyedia AI" with 6 rows: Gemini Web Cookie (Gratis badge + green ✓), OpenRouter, Google Gemini API, DeepSeek, Groq, Custom OpenAI
 * - Card "Konfigurasi Gemini Web Cookie" with Cookie tersimpan status box + [ ↻ Verifikasi ],
 *   2 side-by-side buttons [ ↗ Login & Ambil Cookie ] + [ 📋 Tempel Cookie ],
 *   and 3 selector rows: Model, Bahasa Target, Gaya Terjemahan
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AiProviderDetailScreenContent(
    uiState: SubtitleUiState,
    aiSettings: AiProviderSettings,
    glossaryRules: List<GlossaryRule>,
    themeSettings: ThemeSettings,
    viewModel: SubtitleViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    var showConfigSection by rememberSaveable { mutableStateOf(true) }
    var showAdvancedInputs by rememberSaveable { mutableStateOf(false) }
    var showCookiePlain by rememberSaveable { mutableStateOf(false) }
    var showApiKeyPlain by rememberSaveable { mutableStateOf(false) }
    var showWebViewCookieGrabber by rememberSaveable { mutableStateOf(false) }

    var showModelDropdown by remember { mutableStateOf(false) }
    var showLanguageDropdown by remember { mutableStateOf(false) }
    var showStyleDropdown by remember { mutableStateOf(false) }

    if (showWebViewCookieGrabber) {
        GeminiCookieWebViewDialog(
            onDismiss = { showWebViewCookieGrabber = false },
            onCookiesExtracted = { extracted ->
                viewModel.applyExtractedCookies(extracted)
            }
        )
    }

    val providerDisplayList = listOf(
        AiProviderPreset.GEMINI_WEB_COOKIE,
        AiProviderPreset.OPENROUTER,
        AiProviderPreset.GOOGLE_GEMINI_API,
        AiProviderPreset.DEEPSEEK,
        AiProviderPreset.GROQ,
        AiProviderPreset.CUSTOM
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Top Banner Card matching Screen 2A of Reference Image
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = if (isDark) Color(0xFF1A2C22) else Color(0xFFE7EFE9),
            border = BorderStroke(
                1.dp,
                if (isDark) Color(0xFF2B4536) else Color(0xFFD6E3D9)
            ),
            modifier = Modifier.fillMaxWidth()
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
                    // Green rounded square SubAI logo icon
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFF1F5D3D),
                        shadowElevation = 2.dp,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            SubAiParallelogramLogo(
                                color = Color.White,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }

                    Text(
                        text = "Kelola penyedia AI untuk\nterjemahan subtitle",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 19.sp
                    )
                }

                // Right 3D Teal Folder / Cards Illustration
                Canvas(modifier = Modifier.size(width = 74.dp, height = 56.dp)) {
                    rotate(-12f, pivot = Offset(size.width * 0.5f, size.height * 0.5f)) {
                        drawRoundRect(
                            color = Color(0xFF9AC5B5).copy(alpha = 0.55f),
                            topLeft = Offset(size.width * 0.14f, size.height * 0.08f),
                            size = Size(size.width * 0.62f, size.height * 0.72f),
                            cornerRadius = CornerRadius(14f, 14f)
                        )
                        drawRoundRect(
                            brush = Brush.linearGradient(
                                colors = listOf(Color(0xFF3A9188), Color(0xFF23665E))
                            ),
                            topLeft = Offset(size.width * 0.24f, size.height * 0.20f),
                            size = Size(size.width * 0.64f, size.height * 0.68f),
                            cornerRadius = CornerRadius(14f, 14f)
                        )
                        drawRoundRect(
                            color = Color.White.copy(alpha = 0.85f),
                            topLeft = Offset(size.width * 0.34f, size.height * 0.38f),
                            size = Size(size.width * 0.36f, 6f),
                            cornerRadius = CornerRadius(3f, 3f)
                        )
                        drawRoundRect(
                            color = Color.White.copy(alpha = 0.65f),
                            topLeft = Offset(size.width * 0.34f, size.height * 0.54f),
                            size = Size(size.width * 0.24f, 6f),
                            cornerRadius = CornerRadius(3f, 3f)
                        )
                    }
                }
            }
        }

        // 2. Card: "Penyedia AI" with 6 Provider Rows matching Screen 2A
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = if (isDark) MaterialTheme.colorScheme.surface else Color.White,
            border = BorderStroke(
                1.dp,
                if (isDark) MaterialTheme.colorScheme.outlineVariant else Color(0xFFE3E9E4)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp, horizontal = 10.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = "Penyedia AI",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )

                providerDisplayList.forEachIndexed { idx, preset ->
                    val isSelected = aiSettings.preset == preset ||
                        (preset == AiProviderPreset.GEMINI_WEB_COOKIE && aiSettings.preset == AiProviderPreset.GEMINI_WEB_SERVER) ||
                        (preset == AiProviderPreset.CUSTOM && aiSettings.preset == AiProviderPreset.OPENAI)

                    AiProviderListRow(
                        preset = preset,
                        selected = isSelected,
                        isDark = isDark,
                        testTag = "ai_preset_${preset.name.lowercase()}",
                        onClick = { viewModel.selectAiProviderPreset(preset) }
                    )

                    if (idx < providerDisplayList.lastIndex) {
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 10.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
                        )
                    }
                }
            }
        }

        // 3. Card: "Konfigurasi Gemini Web Cookie" (or Active Provider Configuration) matching Screen 2A
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = if (isDark) MaterialTheme.colorScheme.surface else Color.White,
            border = BorderStroke(
                1.dp,
                if (isDark) MaterialTheme.colorScheme.outlineVariant else Color(0xFFE3E9E4)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showConfigSection = !showConfigSection }
                        .testTag("toggle_ai_provider_config_chip"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AiProviderBrandIcon(
                            preset = AiProviderPreset.GEMINI_WEB_COOKIE,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = if (aiSettings.preset.isGeminiWebFamily) {
                                "Konfigurasi Gemini Web Cookie"
                            } else {
                                "Konfigurasi ${aiSettings.preset.displayTitle}"
                            },
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Icon(
                        imageVector = if (showConfigSection) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }

                AnimatedVisibility(visible = showConfigSection) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        // Soft Sage Status Box: [ ✓ Cookie tersimpan / Login: eko***@gmail.com   [ ↻ Verifikasi ] ]
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (isDark) Color(0xFF1E3528) else Color(0xFFE1EFE4),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(EmeraldCheckGreen),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(15.dp)
                                        )
                                    }

                                    Column {
                                        Text(
                                            text = if (aiSettings.hasGeminiCookie) "Cookie tersimpan" else "Mode Siap Digunakan",
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = if (aiSettings.hasGeminiCookie) {
                                                "Login: eko***@gmail.com"
                                            } else {
                                                "Klik Login atau Tempel Cookie"
                                            },
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.testTag("model_profile_badge_title")
                                        )
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(18.dp),
                                    color = if (isDark) Color(0xFF284234) else Color.White,
                                    border = BorderStroke(
                                        1.dp,
                                        if (isDark) Color(0xFF395B48) else Color(0xFFD0DDD3)
                                    ),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(18.dp))
                                        .clickable(enabled = !uiState.isVerifyingCookie) {
                                            if (aiSettings.hasGeminiCookie) {
                                                viewModel.verifyGeminiCookiesNow()
                                            } else {
                                                viewModel.fetchModelsFromProvider()
                                            }
                                        }
                                        .testTag("verify_gemini_cookie_button")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                                    ) {
                                        if (uiState.isVerifyingCookie) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(14.dp),
                                                strokeWidth = 2.dp
                                            )
                                        } else {
                                            Icon(
                                                imageVector = Icons.Default.Refresh,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onSurface,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                        Text(
                                            text = "Verifikasi",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }

                        // 2 Side-by-Side Action Buttons: [ ↗ Login & Ambil Cookie ] + [ 📋 Tempel Cookie ]
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = { showWebViewCookieGrabber = true },
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = PrimaryForestButton,
                                    contentColor = Color.White
                                ),
                                modifier = Modifier
                                    .weight(1.15f)
                                    .height(44.dp)
                                    .testTag("open_cookie_webview_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Login & Ambil Cookie",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (isDark) Color(0xFF1F3027) else Color(0xFFF6F8F6),
                                border = BorderStroke(
                                    1.dp,
                                    if (isDark) MaterialTheme.colorScheme.outlineVariant else Color(0xFFD8E0D9)
                                ),
                                modifier = Modifier
                                    .weight(0.85f)
                                    .height(44.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable {
                                        val clip = readFromClipboardText(context)
                                        if (clip.isNotBlank()) {
                                            viewModel.applySmartPastedCookies(clip)
                                        } else {
                                            showAdvancedInputs = !showAdvancedInputs
                                        }
                                    }
                                    .testTag("smart_paste_cookie_button")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentPaste,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Tempel Cookie",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1
                                    )
                                }
                            }
                        }

                        // Toggle for manual cookie / API Key / Endpoint inputs
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Text(
                                text = if (showAdvancedInputs) "Sembunyikan Input Manual ▲" else "Input Cookie / API Key Manual ▼",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = EmeraldCheckGreen,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable { showAdvancedInputs = !showAdvancedInputs }
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        AnimatedVisibility(visible = showAdvancedInputs || !aiSettings.preset.isGeminiWebFamily) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (aiSettings.preset.isGeminiWebFamily) {
                                    OutlinedTextField(
                                        value = aiSettings.geminiSecure1Psid,
                                        onValueChange = { viewModel.updateGeminiSecure1Psid(it) },
                                        label = { Text("Cookie __Secure-1PSID") },
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
                                                    contentDescription = null
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
                                        label = { Text("Cookie __Secure-1PSIDTS") },
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
                                } else {
                                    OutlinedTextField(
                                        value = aiSettings.baseUrl,
                                        onValueChange = { viewModel.updateAiBaseUrl(it) },
                                        label = { Text("Endpoint Base URL") },
                                        singleLine = true,
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("ai_base_url_input")
                                    )

                                    OutlinedTextField(
                                        value = aiSettings.apiKey,
                                        onValueChange = { viewModel.updateAiApiKey(it) },
                                        label = { Text("API Key Provider") },
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
                                                    contentDescription = null
                                                )
                                            }
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("ai_api_key_input")
                                    )
                                }

                                OutlinedTextField(
                                    value = aiSettings.selectedModel,
                                    onValueChange = { viewModel.selectAiModel(it) },
                                    label = { Text("ID Model Kustom") },
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("ai_model_id_input")
                                )
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                        // 3 Selector Rows at Bottom of Card matching Screen 2A:
                        // Row 1: Model -> [ ✦ Gemini 3.0 Flash ⌄ ]
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Model",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Box {
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = if (isDark) Color(0xFF1F3027) else Color(0xFFF6F8F6),
                                    border = BorderStroke(
                                        1.dp,
                                        if (isDark) MaterialTheme.colorScheme.outlineVariant else Color(0xFFDFE6E0)
                                    ),
                                    modifier = Modifier
                                        .width(205.dp)
                                        .height(38.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .clickable { showModelDropdown = true }
                                        .testTag("fetch_ai_models_button")
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(horizontal = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            AiProviderBrandIcon(
                                                preset = AiProviderPreset.GEMINI_WEB_COOKIE,
                                                modifier = Modifier.size(15.dp)
                                            )
                                            Text(
                                                text = formatModelDisplayName(aiSettings.selectedModel),
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        Icon(
                                            imageVector = Icons.Default.KeyboardArrowDown,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                DropdownMenu(
                                    expanded = showModelDropdown,
                                    onDismissRequest = { showModelDropdown = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("↻ Muat Ulang Daftar Model dari Provider") },
                                        onClick = {
                                            showModelDropdown = false
                                            viewModel.fetchModelsFromProvider()
                                        }
                                    )
                                    HorizontalDivider()
                                    aiSettings.availableModelChoices.forEach { modelId ->
                                        DropdownMenuItem(
                                            text = { Text(formatModelDisplayName(modelId)) },
                                            onClick = {
                                                viewModel.selectAiModel(modelId)
                                                showModelDropdown = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        // Row 2: Bahasa Target -> [ 🇮🇩 Indonesia ⌄ ]
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Bahasa Target",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Box {
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = if (isDark) Color(0xFF1F3027) else Color(0xFFF6F8F6),
                                    border = BorderStroke(
                                        1.dp,
                                        if (isDark) MaterialTheme.colorScheme.outlineVariant else Color(0xFFDFE6E0)
                                    ),
                                    modifier = Modifier
                                        .width(205.dp)
                                        .height(38.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .clickable { showLanguageDropdown = true }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(horizontal = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            IndonesianFlagBadge()
                                            Text(
                                                text = when (uiState.targetLanguageOption) {
                                                    TargetLanguageOption.INDONESIAN -> "Indonesia"
                                                    else -> uiState.targetLanguageOption.label
                                                },
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        Icon(
                                            imageVector = Icons.Default.KeyboardArrowDown,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                DropdownMenu(
                                    expanded = showLanguageDropdown,
                                    onDismissRequest = { showLanguageDropdown = false }
                                ) {
                                    TargetLanguageOption.entries.forEach { lang ->
                                        DropdownMenuItem(
                                            text = { Text(lang.label) },
                                            onClick = {
                                                viewModel.setTargetLanguageOption(lang)
                                                showLanguageDropdown = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        // Row 3: Gaya Terjemahan -> [ 📖 Anime Casual ⌄ ]
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Gaya Terjemahan",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Box {
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = if (isDark) Color(0xFF1F3027) else Color(0xFFF6F8F6),
                                    border = BorderStroke(
                                        1.dp,
                                        if (isDark) MaterialTheme.colorScheme.outlineVariant else Color(0xFFDFE6E0)
                                    ),
                                    modifier = Modifier
                                        .width(205.dp)
                                        .height(38.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .clickable { showStyleDropdown = true }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(horizontal = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Outlined.MenuBook,
                                                contentDescription = null,
                                                modifier = Modifier.size(15.dp),
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = uiState.stylePreset.shortTitle,
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        Icon(
                                            imageVector = Icons.Default.KeyboardArrowDown,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                DropdownMenu(
                                    expanded = showStyleDropdown,
                                    onDismissRequest = { showStyleDropdown = false }
                                ) {
                                    TranslationStylePreset.entries.forEach { style ->
                                        DropdownMenuItem(
                                            text = { Text(style.shortTitle) },
                                            onClick = {
                                                viewModel.setTranslationStylePreset(style)
                                                showStyleDropdown = false
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
    }
}

/**
 * SCREEN 2B (Middle of Image 2): "Tema & Tampilan"
 * Reproduces the locked reference design 100%:
 * - Card 1: "Mode Tampilan" with 3 equal segment cards (Ikuti Sistem, Terang, Gelap) + AMOLED Pure Black & Dynamic Color switches
 * - Card 2: "Palet Warna Studio" with 5 glossy wavy rounded-square swatches (Violet, Emerald, Sakura, Ocean, Amber)
 * - Card 3: "Tipografi & Ukuran" with "Jenis Huruf" dropdown ([ Inter ⌄ ]), "Ukuran Font Editor" green slider (16sp), and 2-line preview box
 */
@Composable
fun ThemeAppearanceDetailScreenContent(
    themeSettings: ThemeSettings,
    viewModel: SubtitleViewModel,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    var showFontDropdown by remember { mutableStateOf(false) }

    val studioSwatches = listOf(
        Triple(
            ColorPreset.VIOLET_STUDIO,
            "Violet",
            listOf(Color(0xFF432371), Color(0xFF7B52AB), Color(0xFFB39DDB))
        ),
        Triple(
            ColorPreset.EMERALD_ANIME,
            "Emerald",
            listOf(Color(0xFF145A32), Color(0xFF28965A), Color(0xFF68D391))
        ),
        Triple(
            ColorPreset.CRIMSON_ROSE,
            "Sakura",
            listOf(Color(0xFFB85D79), Color(0xFFE58C9E), Color(0xFFF7C6CE))
        ),
        Triple(
            ColorPreset.OCEAN_BLUE,
            "Ocean",
            listOf(Color(0xFF1B5E8A), Color(0xFF348AC7), Color(0xFF74C0FC))
        ),
        Triple(
            ColorPreset.AMBER_CINEMA,
            "Amber",
            listOf(Color(0xFFA05A1C), Color(0xFFD68928), Color(0xFFF6C362))
        )
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Card 1: Mode Tampilan
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = if (isDark) MaterialTheme.colorScheme.surface else Color.White,
            border = BorderStroke(
                1.dp,
                if (isDark) MaterialTheme.colorScheme.outlineVariant else Color(0xFFE3E9E4)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.DesktopWindows,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Mode Tampilan",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // 3 Segment Cards: [ Ikuti Sistem ] [ Terang ] [ Gelap ]
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ThemeModeSegmentCard(
                        label = "Ikuti Sistem",
                        icon = Icons.Outlined.DesktopWindows,
                        selected = themeSettings.themeMode == ThemeMode.SYSTEM,
                        isDark = isDark,
                        onClick = { viewModel.updateThemeMode(ThemeMode.SYSTEM) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("theme_mode_system")
                    )
                    ThemeModeSegmentCard(
                        label = "Terang",
                        icon = Icons.Outlined.LightMode,
                        selected = themeSettings.themeMode == ThemeMode.LIGHT,
                        isDark = isDark,
                        onClick = { viewModel.updateThemeMode(ThemeMode.LIGHT) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("theme_mode_light")
                    )
                    ThemeModeSegmentCard(
                        label = "Gelap",
                        icon = Icons.Outlined.DarkMode,
                        selected = themeSettings.themeMode == ThemeMode.DARK,
                        isDark = isDark,
                        onClick = { viewModel.updateThemeMode(ThemeMode.DARK) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("theme_mode_dark")
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                // Toggle Row 1: AMOLED Pure Black
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.updatePureBlackAmoled(!themeSettings.pureBlackAmoled) },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.PhoneAndroid,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(21.dp)
                        )
                        Column {
                            Text(
                                text = "AMOLED Pure Black",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Gunakan latar hitam pekat (#000000)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Switch(
                        checked = themeSettings.pureBlackAmoled,
                        onCheckedChange = { viewModel.updatePureBlackAmoled(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = EmeraldCheckGreen,
                            uncheckedThumbColor = Color.White,
                            uncheckedTrackColor = Color(0xFFD5DBD6),
                            uncheckedBorderColor = Color.Transparent
                        ),
                        modifier = Modifier.testTag("amoled_pure_black_switch")
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                // Toggle Row 2: Dynamic Color (Material You)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.updateUseDynamicColor(!themeSettings.useDynamicColor) },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Palette,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(21.dp)
                        )
                        Column {
                            Text(
                                text = "Dynamic Color (Material You)",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Sesuaikan warna dengan wallpaper",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Switch(
                        checked = themeSettings.useDynamicColor,
                        onCheckedChange = { viewModel.updateUseDynamicColor(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = EmeraldCheckGreen,
                            uncheckedThumbColor = Color.White,
                            uncheckedTrackColor = Color(0xFFD5DBD6),
                            uncheckedBorderColor = Color.Transparent
                        ),
                        modifier = Modifier.testTag("dynamic_color_switch")
                    )
                }
            }
        }

        // Card 2: Palet Warna Studio (5 Wavy Rounded-Square Swatches matching Screen 2B)
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = if (isDark) MaterialTheme.colorScheme.surface else Color.White,
            border = BorderStroke(
                1.dp,
                if (isDark) MaterialTheme.colorScheme.outlineVariant else Color(0xFFE3E9E4)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Palette,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Palet Warna Studio",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    studioSwatches.forEach { (preset, shortLabel, colors) ->
                        val isSelected = !themeSettings.useDynamicColor && themeSettings.colorPreset == preset
                        StudioWavyColorSwatchItem(
                            label = shortLabel,
                            gradientColors = colors,
                            selected = isSelected,
                            testTag = "color_preset_${preset.name.lowercase()}",
                            onClick = { viewModel.updateColorPreset(preset) }
                        )
                    }
                }
            }
        }

        // Card 3: Tipografi & Ukuran matching Screen 2B
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = if (isDark) MaterialTheme.colorScheme.surface else Color.White,
            border = BorderStroke(
                1.dp,
                if (isDark) MaterialTheme.colorScheme.outlineVariant else Color(0xFFE3E9E4)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Aa",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Tipografi & Ukuran",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Row 1: Jenis Huruf -> [ Inter ⌄ ]
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Jenis Huruf",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Box {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (isDark) Color(0xFF1F3027) else Color(0xFFF6F8F6),
                            border = BorderStroke(
                                1.dp,
                                if (isDark) MaterialTheme.colorScheme.outlineVariant else Color(0xFFDFE6E0)
                            ),
                            modifier = Modifier
                                .width(136.dp)
                                .height(36.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { showFontDropdown = true }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = if (themeSettings.useMonospaceFont) "JetBrains Mono" else "Inter",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1
                                )
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowDown,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = showFontDropdown,
                            onDismissRequest = { showFontDropdown = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Inter (Sans-Serif)") },
                                onClick = {
                                    viewModel.updateUseMonospaceFont(false)
                                    showFontDropdown = false
                                },
                                modifier = Modifier.testTag("font_family_sans")
                            )
                            DropdownMenuItem(
                                text = { Text("JetBrains Mono (Monospace)") },
                                onClick = {
                                    viewModel.updateUseMonospaceFont(true)
                                    showFontDropdown = false
                                },
                                modifier = Modifier.testTag("font_family_monospace")
                            )
                        }
                    }
                }

                // Row 2: Ukuran Font Editor + Green Slider + "16sp"
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "A",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Ukuran Font Editor",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Slider(
                            value = themeSettings.editorFontSizeSp.toFloat(),
                            onValueChange = { newVal ->
                                viewModel.updateEditorFontSizeSp(newVal.roundToInt().coerceIn(12, 20))
                            },
                            valueRange = 12f..20f,
                            steps = 3,
                            colors = SliderDefaults.colors(
                                thumbColor = Color.White,
                                activeTrackColor = PrimaryForestButton,
                                inactiveTrackColor = Color(0xFFD5DDD7)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("editor_font_size_slider")
                        )

                        Text(
                            text = "${themeSettings.editorFontSizeSp}sp",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Quick size pills (also keeps font_size_12..18 testTags active)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(12, 14, 16, 18).forEach { spSize ->
                            val isSelected = themeSettings.editorFontSizeSp == spSize
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) Color(0xFFDDF1E0) else Color.Transparent,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { viewModel.updateEditorFontSizeSp(spSize) }
                                    .testTag("font_size_$spSize")
                            ) {
                                Text(
                                    text = "${spSize}sp",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) PrimaryForestButton else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }
                }

                // Bottom Preview Box: [01] Contoh teks subtitle... / [02] Contoh terjemahan...
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (isDark) Color(0xFF1A2921) else Color(0xFFF1F4F1),
                    border = BorderStroke(
                        1.dp,
                        if (isDark) Color(0xFF2B3F33) else Color(0xFFE2E7E2)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val fontFamily = if (themeSettings.useMonospaceFont) FontFamily.Monospace else FontFamily.Default
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "[01]   Contoh teks subtitle...",
                            fontSize = themeSettings.editorFontSizeSp.sp,
                            fontFamily = fontFamily,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = buildAnnotatedString {
                                withStyle(
                                    SpanStyle(
                                        fontFamily = fontFamily,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                ) {
                                    append("[02]   ")
                                }
                                withStyle(
                                    SpanStyle(
                                        fontFamily = fontFamily,
                                        fontWeight = FontWeight.Medium,
                                        color = EmeraldCheckGreen
                                    )
                                ) {
                                    append("Contoh terjemahan...")
                                }
                            },
                            fontSize = themeSettings.editorFontSizeSp.sp
                        )
                    }
                }
            }
        }
    }
}

/**
 * SCREEN 2C (Right of Image 2): "Pembaruan Aplikasi"
 * Reproduces the locked reference design 100%:
 * - Hero Banner with SubAI logo + "SubAI Extractor & Merger" + "v1.4.0 (Build 7)" + "Versi Terpasang" badge + 3D Rocket launch illustration
 * - Main White Card with Soft Green Gradient "Pembaruan Tersedia" banner, "Apa yang baru?" green bullets,
 *   and full-width green pill button `[ 📥 Download APK Terbaru (16.2 MB) ]`
 * - Bottom 2 Action Cards: "Cek Versi" and "GitHub Releases"
 */
@Composable
fun AppUpdateDetailScreenContent(
    updateState: AppUpdateUiState,
    onCheckUpdate: () -> Unit,
    onDownloadLatestApk: () -> Unit,
    onOpenReleasePage: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    var showChangelog by rememberSaveable { mutableStateOf(true) }

    val latest = updateState.latestRelease
    val displayedVersion = latest?.versionName ?: updateState.currentVersionName
    val displayedSizeMb = latest?.apkAsset?.formattedSizeMb ?: "16.2 MB"
    val displayedDate = latest?.publishedAt
        ?.take(10)
        ?.takeIf { it.isNotBlank() }
        ?: "24 Okt 2026"

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Rocket Hero Banner matching Screen 2C of Reference Image
        RocketUpdateHeroBanner(
            currentVersionName = updateState.currentVersionName,
            currentVersionCode = updateState.currentVersionCode
        )

        // 2. Main White Card ("Pembaruan Tersedia" + "Apa yang baru?" + Download CTA)
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = if (isDark) MaterialTheme.colorScheme.surface else Color.White,
            border = BorderStroke(
                1.dp,
                if (isDark) MaterialTheme.colorScheme.outlineVariant else Color(0xFFE3E9E4)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("app_version_update_card")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Soft Green Highlight Banner inside Card
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (isDark) Color(0xFF1F3E2C) else Color(0xFFDCF2DF),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(
                            if (updateState.isUpdateAvailable) "update_available_card" else "up_to_date_status_card"
                        )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Left Green Square Download Icon
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = EmeraldCheckGreen,
                            modifier = Modifier.size(46.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.CloudDownload,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(
                                text = if (updateState.isUpdateAvailable) {
                                    "Pembaruan Tersedia"
                                } else {
                                    updateState.statusMessage ?: "Pembaruan Tersedia"
                                },
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "v$displayedVersion",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Description,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = displayedSizeMb,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.CalendarToday,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = displayedDate,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                // "Apa yang baru?" Section with Green Bullet Points
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showChangelog = !showChangelog },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Apa yang baru?",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Icon(
                            imageVector = if (showChangelog) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    AnimatedVisibility(visible = showChangelog) {
                        Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                            val notes = listOf(
                                "Peningkatan stabilitas proses batch",
                                "Opsi Auto-Retry yang lebih andal",
                                "Perbaikan akurasi pemetaan baris",
                                "Optimasi performa & penggunaan memori"
                            )
                            notes.forEach { item ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(7.dp)
                                            .clip(CircleShape)
                                            .background(EmeraldCheckGreen)
                                    )
                                    Text(
                                        text = item,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }

                if (!updateState.errorMessage.isNullOrBlank()) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.75f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.ErrorOutline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = updateState.errorMessage.orEmpty(),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }

                // Full-width Green Pill Button: [ 📥 Download APK Terbaru (16.2 MB) ]
                Button(
                    onClick = {
                        if (latest?.apkAsset != null) {
                            onDownloadLatestApk()
                        } else {
                            onOpenReleasePage()
                        }
                    },
                    enabled = !updateState.isDownloadingApk,
                    shape = RoundedCornerShape(25.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF1F6E43),
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("download_update_apk_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudDownload,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Download APK Terbaru ($displayedSizeMb)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // 3. Bottom Action Card 1: "Cek Versi"
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = if (isDark) MaterialTheme.colorScheme.surface else Color.White,
            border = BorderStroke(
                1.dp,
                if (isDark) MaterialTheme.colorScheme.outlineVariant else Color(0xFFE3E9E4)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .clickable(enabled = !updateState.isChecking, onClick = onCheckUpdate)
                .testTag("check_app_update_button")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Sync,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(22.dp)
                    )
                    Column {
                        Text(
                            text = if (updateState.isChecking) "Memeriksa Versi..." else "Cek Versi",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Periksa pembaruan di GitHub Releases",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (updateState.isChecking) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // 4. Bottom Action Card 2: "GitHub Releases"
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = if (isDark) MaterialTheme.colorScheme.surface else Color.White,
            border = BorderStroke(
                1.dp,
                if (isDark) MaterialTheme.colorScheme.outlineVariant else Color(0xFFE3E9E4)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .clickable(onClick = onOpenReleasePage)
                .testTag("open_github_release_button")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    GitHubOctocatIcon(modifier = Modifier.size(22.dp))
                    Column {
                        Text(
                            text = "GitHub Releases",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${updateState.repoOwner}/${updateState.repoName}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Icon(
                    imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun RocketUpdateHeroBanner(
    currentVersionName: String,
    currentVersionCode: Int
) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = Color(0xFF193C2C),
        border = BorderStroke(1.dp, Color(0xFF2D6048)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(148.dp)
        ) {
            // Starry night + clouds + launching 3D rocket Canvas illustration matching Screen 2C
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height

                // Soft glowing aura around rocket
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFF69C98E).copy(alpha = 0.28f), Color.Transparent),
                        center = Offset(w * 0.78f, h * 0.42f),
                        radius = w * 0.38f
                    ),
                    center = Offset(w * 0.78f, h * 0.42f),
                    radius = w * 0.38f
                )

                // Orbit ring
                rotate(-18f, pivot = Offset(w * 0.76f, h * 0.48f)) {
                    drawOval(
                        color = Color(0xFFA2E3B8).copy(alpha = 0.25f),
                        topLeft = Offset(w * 0.54f, h * 0.22f),
                        size = Size(w * 0.42f, h * 0.44f),
                        style = Stroke(width = 2.5f)
                    )
                }

                // Soft clouds at bottom-right
                drawCircle(
                    color = Color(0xFFD5E6DC).copy(alpha = 0.35f),
                    radius = h * 0.28f,
                    center = Offset(w * 0.65f, h * 0.96f)
                )
                drawCircle(
                    color = Color(0xFFE3F0E8).copy(alpha = 0.45f),
                    radius = h * 0.34f,
                    center = Offset(w * 0.80f, h * 0.94f)
                )
                drawCircle(
                    color = Color(0xFFD5E6DC).copy(alpha = 0.40f),
                    radius = h * 0.26f,
                    center = Offset(w * 0.94f, h * 0.92f)
                )

                // Rocket exhaust flame
                rotate(24f, pivot = Offset(w * 0.80f, h * 0.44f)) {
                    val flamePath = Path().apply {
                        moveTo(w * 0.77f, h * 0.56f)
                        lineTo(w * 0.80f, h * 0.92f)
                        lineTo(w * 0.83f, h * 0.56f)
                        close()
                    }
                    drawPath(
                        path = flamePath,
                        brush = Brush.verticalGradient(
                            colors = listOf(Color(0xFFFFE082), Color(0xFFFF8F00), Color.Transparent)
                        )
                    )

                    // Rocket fins
                    val leftFin = Path().apply {
                        moveTo(w * 0.76f, h * 0.45f)
                        lineTo(w * 0.71f, h * 0.57f)
                        lineTo(w * 0.77f, h * 0.55f)
                        close()
                    }
                    drawPath(leftFin, color = Color(0xFF429B8E))

                    val rightFin = Path().apply {
                        moveTo(w * 0.84f, h * 0.45f)
                        lineTo(w * 0.89f, h * 0.57f)
                        lineTo(w * 0.83f, h * 0.55f)
                        close()
                    }
                    drawPath(rightFin, color = Color(0xFF429B8E))

                    // Rocket fuselage
                    drawRoundRect(
                        brush = Brush.linearGradient(
                            colors = listOf(Color.White, Color(0xFFD6E6EC))
                        ),
                        topLeft = Offset(w * 0.755f, h * 0.16f),
                        size = Size(w * 0.09f, h * 0.40f),
                        cornerRadius = CornerRadius(28f, 28f)
                    )

                    // Rocket window
                    drawCircle(
                        color = Color(0xFF398AB9),
                        radius = w * 0.024f,
                        center = Offset(w * 0.80f, h * 0.32f)
                    )
                    drawCircle(
                        color = Color(0xFF90CAF9),
                        radius = w * 0.015f,
                        center = Offset(w * 0.80f, h * 0.32f)
                    )
                }
            }

            // Left Content: Logo + "SubAI Extractor & Merger" + "v1.4.0 (Build 7)" + "Versi Terpasang"
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                SubAiParallelogramLogo(
                    color = Color.White,
                    modifier = Modifier.size(28.dp)
                )

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "SubAI\nExtractor & Merger",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        lineHeight = 24.sp
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "v$currentVersionName (Build $currentVersionCode)",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White.copy(alpha = 0.88f),
                            modifier = Modifier.testTag("settings_version_badge")
                        )

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color.White.copy(alpha = 0.22f)
                        ) {
                            Text(
                                text = "Versi Terpasang",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AiProviderListRow(
    preset: AiProviderPreset,
    selected: Boolean,
    isDark: Boolean,
    testTag: String,
    onClick: () -> Unit
) {
    val bg = if (selected) {
        if (isDark) Color(0xFF1F422E) else Color(0xFFDDF1E0)
    } else {
        Color.Transparent
    }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = bg,
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AiProviderBrandIcon(
                    preset = preset,
                    modifier = Modifier.size(22.dp)
                )
                Text(
                    text = preset.displayTitle,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (preset == AiProviderPreset.GEMINI_WEB_COOKIE) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isDark) Color(0xFF28593E) else Color(0xFFC3E8CB)
                    ) {
                        Text(
                            text = "Gratis",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color(0xFF9BEAB5) else Color(0xFF1B5E37),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                if (selected) {
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(EmeraldCheckGreen),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Aktif",
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .border(
                                width = 1.6.dp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                                shape = CircleShape
                            )
                    )
                }
            }
        }
    }
}

@Composable
private fun AiProviderBrandIcon(
    preset: AiProviderPreset,
    modifier: Modifier = Modifier
) {
    when (preset) {
        AiProviderPreset.GEMINI_WEB_COOKIE,
        AiProviderPreset.GEMINI_WEB_SERVER -> {
            // 4-point blue/purple Gemini star
            Canvas(modifier = modifier) {
                val w = size.width
                val h = size.height
                val starPath = Path().apply {
                    moveTo(w * 0.5f, 0f)
                    quadraticTo(w * 0.52f, h * 0.48f, w, h * 0.5f)
                    quadraticTo(w * 0.52f, h * 0.52f, w * 0.5f, h)
                    quadraticTo(w * 0.48f, h * 0.52f, 0f, h * 0.5f)
                    quadraticTo(w * 0.48f, h * 0.48f, w * 0.5f, 0f)
                    close()
                }
                drawPath(
                    path = starPath,
                    brush = Brush.linearGradient(
                        colors = listOf(Color(0xFF4E82EE), Color(0xFF8B5CF6))
                    )
                )
            }
        }

        AiProviderPreset.OPENROUTER -> {
            // Purple infinity symbol ∞
            Canvas(modifier = modifier) {
                val w = size.width
                val h = size.height
                drawCircle(
                    color = Color(0xFF6C47FF),
                    radius = w * 0.22f,
                    center = Offset(w * 0.30f, h * 0.5f),
                    style = Stroke(width = 4.2f)
                )
                drawCircle(
                    color = Color(0xFF6C47FF),
                    radius = w * 0.22f,
                    center = Offset(w * 0.70f, h * 0.5f),
                    style = Stroke(width = 4.2f)
                )
            }
        }

        AiProviderPreset.GOOGLE_GEMINI_API -> {
            // Multi-color Google G
            Canvas(modifier = modifier) {
                val stroke = size.width * 0.20f
                val arcSize = Size(size.width - stroke, size.height - stroke)
                val topLeft = Offset(stroke / 2f, stroke / 2f)
                drawArc(
                    color = Color(0xFFEA4335),
                    startAngle = -145f,
                    sweepAngle = 105f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Butt)
                )
                drawArc(
                    color = Color(0xFFFBBC05),
                    startAngle = 140f,
                    sweepAngle = 75f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Butt)
                )
                drawArc(
                    color = Color(0xFF34A853),
                    startAngle = 45f,
                    sweepAngle = 95f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Butt)
                )
                drawArc(
                    color = Color(0xFF4285F4),
                    startAngle = -15f,
                    sweepAngle = 65f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Butt)
                )
                drawLine(
                    color = Color(0xFF4285F4),
                    start = Offset(size.width * 0.52f, size.height * 0.5f),
                    end = Offset(size.width * 0.92f, size.height * 0.5f),
                    strokeWidth = stroke
                )
            }
        }

        AiProviderPreset.DEEPSEEK -> {
            // Blue whale icon
            Canvas(modifier = modifier) {
                drawCircle(
                    color = Color(0xFF2563EB),
                    radius = size.width * 0.36f,
                    center = Offset(size.width * 0.45f, size.height * 0.54f)
                )
                val tail = Path().apply {
                    moveTo(size.width * 0.72f, size.height * 0.52f)
                    lineTo(size.width * 0.96f, size.height * 0.28f)
                    lineTo(size.width * 0.92f, size.height * 0.68f)
                    close()
                }
                drawPath(tail, color = Color(0xFF2563EB))
                drawCircle(
                    color = Color.White,
                    radius = size.width * 0.06f,
                    center = Offset(size.width * 0.34f, size.height * 0.46f)
                )
            }
        }

        AiProviderPreset.GROQ -> {
            // Orange bold "g"
            Box(
                modifier = modifier,
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "g",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFFEA580C)
                )
            }
        }

        AiProviderPreset.CUSTOM,
        AiProviderPreset.OPENAI -> {
            // OpenAI-style hexagonal knot
            Canvas(modifier = modifier) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val r = size.width * 0.26f
                for (i in 0 until 6) {
                    rotate(i * 60f, pivot = center) {
                        drawRoundRect(
                            color = Color(0xFF1F2937),
                            topLeft = Offset(center.x - r * 0.55f, center.y - r * 1.35f),
                            size = Size(r * 1.1f, r * 1.1f),
                            cornerRadius = CornerRadius(8f, 8f),
                            style = Stroke(width = 3.2f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun IndonesianFlagBadge() {
    Surface(
        shape = RoundedCornerShape(3.dp),
        border = BorderStroke(0.5.dp, Color(0xFFD0D5DD)),
        modifier = Modifier.size(width = 18.dp, height = 13.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(Color(0xFFE11D48))
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(Color.White)
            )
        }
    }
}

@Composable
private fun ThemeModeSegmentCard(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    isDark: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bg = if (selected) {
        PrimaryForestButton
    } else {
        if (isDark) Color(0xFF1E2D24) else Color(0xFFF3F6F3)
    }
    val fg = if (selected) {
        Color.White
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = bg,
        border = if (!selected) {
            BorderStroke(
                1.dp,
                if (isDark) MaterialTheme.colorScheme.outlineVariant else Color(0xFFE0E6E1)
            )
        } else null,
        modifier = modifier
            .height(64.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 6.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = fg,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(5.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = fg,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun StudioWavyColorSwatchItem(
    label: String,
    gradientColors: List<Color>,
    selected: Boolean,
    testTag: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .testTag(testTag)
    ) {
        Box(
            modifier = Modifier.size(56.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .then(
                        if (selected) {
                            Modifier
                                .border(2.dp, PrimaryForestButton, RoundedCornerShape(15.dp))
                                .padding(3.dp)
                        } else {
                            Modifier
                        }
                    )
                    .clip(RoundedCornerShape(13.dp))
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawRect(
                        brush = Brush.linearGradient(
                            colors = listOf(gradientColors[0], gradientColors[1]),
                            start = Offset(0f, 0f),
                            end = Offset(size.width, size.height)
                        )
                    )
                    val wavePath = Path().apply {
                        moveTo(0f, size.height * 0.65f)
                        quadraticTo(
                            size.width * 0.45f,
                            size.height * 0.35f,
                            size.width,
                            size.height * 0.48f
                        )
                        lineTo(size.width, size.height)
                        lineTo(0f, size.height)
                        close()
                    }
                    drawPath(
                        path = wavePath,
                        brush = Brush.linearGradient(
                            colors = listOf(gradientColors[1], gradientColors[2])
                        )
                    )
                }
            }

            if (selected) {
                Surface(
                    shape = CircleShape,
                    color = Color.White,
                    shadowElevation = 2.dp,
                    border = BorderStroke(1.dp, PrimaryForestButton),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 2.dp, y = (-2).dp)
                        .size(19.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Terpilih",
                            tint = PrimaryForestButton,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }
        }

        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun GitHubOctocatIcon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        drawCircle(
            color = Color(0xFF1F2328),
            radius = size.width * 0.46f,
            center = Offset(size.width * 0.5f, size.height * 0.5f)
        )
        // Inner white cat head silhouette
        drawCircle(
            color = Color.White,
            radius = size.width * 0.24f,
            center = Offset(size.width * 0.5f, size.height * 0.48f)
        )
        val leftEar = Path().apply {
            moveTo(size.width * 0.30f, size.height * 0.38f)
            lineTo(size.width * 0.28f, size.height * 0.20f)
            lineTo(size.width * 0.44f, size.height * 0.28f)
            close()
        }
        drawPath(leftEar, color = Color.White)
        val rightEar = Path().apply {
            moveTo(size.width * 0.70f, size.height * 0.38f)
            lineTo(size.width * 0.72f, size.height * 0.20f)
            lineTo(size.width * 0.56f, size.height * 0.28f)
            close()
        }
        drawPath(rightEar, color = Color.White)
    }
}

/**
 * Header decorative artwork for "Tema & Tampilan" matching Screen 2B of Reference Image:
 * Mount Fuji snow peak + pink cherry blossoms peeking from top-right above the rounded sheet!
 */
@Composable
fun ThemeFujiSakuraHeaderBackdrop(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Mount Fuji Peak on the right
        val mtnPath = Path().apply {
            moveTo(w * 0.42f, h)
            quadraticTo(w * 0.62f, h * 0.55f, w * 0.68f, h * 0.14f)
            lineTo(w * 0.76f, h * 0.14f)
            quadraticTo(w * 0.84f, h * 0.55f, w * 0.98f, h)
            close()
        }
        drawPath(
            path = mtnPath,
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF4E6E8E), Color(0xFF233D4D))
            )
        )

        // Snow cap ridges
        val snowPath = Path().apply {
            moveTo(w * 0.68f, h * 0.14f)
            lineTo(w * 0.76f, h * 0.14f)
            lineTo(w * 0.81f, h * 0.45f)
            lineTo(w * 0.76f, h * 0.36f)
            lineTo(w * 0.72f, h * 0.48f)
            lineTo(w * 0.69f, h * 0.36f)
            lineTo(w * 0.64f, h * 0.44f)
            close()
        }
        drawPath(path = snowPath, color = Color(0xFFEAF2F8))

        // Pink Cherry Blossom clusters around Fuji
        val sakuraCenters = listOf(
            Offset(w * 0.50f, h * 0.78f),
            Offset(w * 0.54f, h * 0.68f),
            Offset(w * 0.58f, h * 0.82f),
            Offset(w * 0.82f, h * 0.58f),
            Offset(w * 0.86f, h * 0.45f),
            Offset(w * 0.90f, h * 0.65f),
            Offset(w * 0.94f, h * 0.38f),
            Offset(w * 0.96f, h * 0.55f)
        )
        sakuraCenters.forEachIndexed { idx, center ->
            drawCircle(
                color = if (idx % 2 == 0) Color(0xFFF6A6B8) else Color(0xFFFAD0DA),
                radius = 9f + (idx % 3) * 3f,
                center = center
            )
        }
    }
}

private fun formatModelDisplayName(modelId: String): String {
    return when (modelId.lowercase()) {
        "gemini-3.0-flash" -> "Gemini 3.0 Flash"
        "gemini-3.8-flash" -> "Gemini 3.8 Flash"
        "gemini-4-argon" -> "Gemini 4 Argon"
        "gemini-2.5-flash" -> "Gemini 2.5 Flash"
        "gemini-2.5-pro" -> "Gemini 2.5 Pro"
        else -> modelId
    }
}

private fun readFromClipboardText(context: Context): String {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    val item = clipboard?.primaryClip?.getItemAt(0) ?: return ""
    return item.coerceToText(context)?.toString().orEmpty()
}
