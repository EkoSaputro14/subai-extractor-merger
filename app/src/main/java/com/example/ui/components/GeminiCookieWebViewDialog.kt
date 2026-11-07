package com.example.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Cookie
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.GeminiWebCookieClient
import com.example.data.ParsedGeminiCookies

private const val GEMINI_WEB_APP_URL = "https://gemini.google.com/app"

/**
 * Crash-free Native Cookie Vault & Smart Session Extractor Dialog.
 * Avoids spawning Chromium's isolated WebView renderer process on devices/containers
 * without hardware DRM rendernodes (/dev/dri/renderD128), preventing native
 * aw_browser_terminator crashes while providing instant cookie parsing and extraction.
 */
@Composable
fun GeminiCookieWebViewDialog(
    onDismiss: () -> Unit,
    onCookiesExtracted: (ParsedGeminiCookies) -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    var turboMode by remember { mutableStateOf(true) }
    var rawCookieInput by remember { mutableStateOf("") }
    var manualPsid by remember { mutableStateOf("") }
    var manualPsidts by remember { mutableStateOf("") }
    var detectedCookies by remember { mutableStateOf(ParsedGeminiCookies()) }
    var statusHint by remember {
        mutableStateOf(
            "Smart Cookie Vault aktif (bebas crash WebView renderer). Tempel cookie dari Clipboard, JSON Cookie-Editor, atau masukkan __Secure-1PSID."
        )
    }

    fun evaluateInputs(rawText: String, psidText: String, psidtsText: String): ParsedGeminiCookies {
        val fromRaw = if (rawText.isNotBlank()) {
            GeminiWebCookieClient.parseSmartCookieInput(rawText)
        } else {
            ParsedGeminiCookies()
        }
        val finalPsid = psidText.trim().ifBlank { fromRaw.secure1Psid }
        val finalPsidts = psidtsText.trim().ifBlank { fromRaw.secure1Psidts }
        return ParsedGeminiCookies(
            secure1Psid = finalPsid,
            secure1Psidts = finalPsidts
        )
    }

    // Automatically inspect clipboard on open for any copied cookie header or JSON
    LaunchedEffect(Unit) {
        runCatching {
            val clipText = clipboardManager.getText()?.text?.trim().orEmpty()
            if (clipText.isNotBlank()) {
                val parsed = GeminiWebCookieClient.parseSmartCookieInput(clipText)
                if (parsed.isValid) {
                    rawCookieInput = clipText
                    manualPsid = parsed.secure1Psid
                    manualPsidts = parsed.secure1Psidts
                    detectedCookies = parsed
                    statusHint =
                        "Cookie __Secure-1PSID otomatis terdeteksi dari Clipboard! Ketuk 'Gunakan Cookie Ini' untuk menyimpan."
                }
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnClickOutside = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFF0B0F17),
            border = BorderStroke(1.dp, Color(0xFF1A2234))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                // Top Header Bar
                Surface(
                    color = Color(0xFF111826),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Bolt,
                                    contentDescription = null,
                                    tint = Color(0xFF00E5FF)
                                )
                                Column {
                                    Text(
                                        text = "Cookie Vault Turbo — Ambil Cookie Gemini",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = Color(0xFFF3F6FC),
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Mode Native (Tanpa Crash Renderer WebView)",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF8A96AC),
                                        maxLines = 1
                                    )
                                }
                            }

                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier.testTag("close_cookie_webview_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Tutup",
                                    tint = Color(0xFFF3F6FC)
                                )
                            }
                        }

                        // Turbo Chip & External Browser Launcher
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FilterChip(
                                selected = turboMode,
                                onClick = { turboMode = !turboMode },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Bolt,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                label = { Text(if (turboMode) "Parser Otomatis Aktif" else "Input Manual") },
                                modifier = Modifier.testTag("toggle_webview_turbo_chip")
                            )

                            OutlinedButton(
                                onClick = {
                                    runCatching {
                                        val intent = Intent(
                                            Intent.ACTION_VIEW,
                                            Uri.parse(GEMINI_WEB_APP_URL)
                                        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        context.startActivity(intent)
                                    }.onFailure {
                                        statusHint =
                                            "Tidak dapat membuka browser eksternal di perangkat ini. Silakan tempel nilai cookie langsung di bawah."
                                    }
                                },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .defaultMinSize(minHeight = 40.dp)
                                    .testTag("open_external_browser_from_dialog_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Browser HP",
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        }

                        // Status & Quick Capture Bar
                        Surface(
                            color = if (detectedCookies.isValid) {
                                Color(0xFF0A261E)
                            } else {
                                Color(0xFF141C2B)
                            },
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(
                                1.dp,
                                if (detectedCookies.isValid) Color(0xFF10E596) else Color(0xFF222D42)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = statusHint,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (detectedCookies.isValid) {
                                        Color(0xFF10E596)
                                    } else {
                                        Color(0xFFD5DFEE)
                                    }
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    FilledTonalButton(
                                        onClick = {
                                            val clipText = clipboardManager.getText()?.text?.trim().orEmpty()
                                            if (clipText.isNotBlank() && rawCookieInput.isBlank()) {
                                                rawCookieInput = clipText
                                            }
                                            val result = evaluateInputs(rawCookieInput, manualPsid, manualPsidts)
                                            if (result.isValid) {
                                                detectedCookies = result
                                                manualPsid = result.secure1Psid
                                                manualPsidts = result.secure1Psidts
                                                onCookiesExtracted(result)
                                                onDismiss()
                                            } else {
                                                statusHint =
                                                    "Cookie __Secure-1PSID belum ditemukan. Tempel teks cookie dari Clipboard atau isi kolom __Secure-1PSID di bawah."
                                            }
                                        },
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .defaultMinSize(minHeight = 48.dp)
                                            .testTag("extract_cookie_now_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Cookie,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Ambil Cookie Sekarang")
                                    }

                                    if (detectedCookies.isValid) {
                                        Button(
                                            onClick = {
                                                onCookiesExtracted(detectedCookies)
                                                onDismiss()
                                            },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = Color(0xFF10E596),
                                                contentColor = Color(0xFF04150E)
                                            ),
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier
                                                .weight(1f)
                                                .defaultMinSize(minHeight = 48.dp)
                                                .testTag("save_extracted_cookie_button")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "Gunakan Cookie Ini",
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Main Smart Cookie Input Form
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "1. Smart Paste (JSON Cookie-Editor / Header HTTP / Teks Mentah)",
                        color = Color(0xFF00E5FF),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )

                    OutlinedTextField(
                        value = rawCookieInput,
                        onValueChange = { updated ->
                            rawCookieInput = updated
                            val parsed = evaluateInputs(updated, manualPsid, manualPsidts)
                            detectedCookies = parsed
                            if (parsed.secure1Psid.isNotBlank()) {
                                manualPsid = parsed.secure1Psid
                            }
                            if (parsed.secure1Psidts.isNotBlank()) {
                                manualPsidts = parsed.secure1Psidts
                            }
                        },
                        label = { Text("Tempel JSON Cookie, Header Cookie, atau __Secure-1PSID") },
                        placeholder = {
                            Text(
                                "__Secure-1PSID=g.a000...; __Secure-1PSIDTS=sidts-...",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp
                            )
                        },
                        minLines = 3,
                        maxLines = 5,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("raw_cookie_smart_input")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val clipText = clipboardManager.getText()?.text?.trim().orEmpty()
                                if (clipText.isNotBlank()) {
                                    rawCookieInput = clipText
                                    val parsed = GeminiWebCookieClient.parseSmartCookieInput(clipText)
                                    manualPsid = parsed.secure1Psid
                                    manualPsidts = parsed.secure1Psidts
                                    detectedCookies = evaluateInputs(clipText, manualPsid, manualPsidts)
                                    statusHint = if (detectedCookies.isValid) {
                                        "Berhasil mengekstrak __Secure-1PSID dari Clipboard!"
                                    } else {
                                        "Teks Clipboard ditempel. Pastikan terdapat nilai __Secure-1PSID."
                                    }
                                } else {
                                    statusHint = "Clipboard kosong. Salin cookie terlebih dahulu."
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentPaste,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Tempel dari Clipboard")
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "2. Kredensial Cookie Terpisah (Opsional / Otomatis Terisi)",
                        color = Color(0xFFF3F6FC),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )

                    OutlinedTextField(
                        value = manualPsid,
                        onValueChange = { updated ->
                            manualPsid = updated
                            detectedCookies = evaluateInputs(rawCookieInput, updated, manualPsidts)
                        },
                        label = { Text("__Secure-1PSID (Wajib)") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("manual_psid_input")
                    )

                    OutlinedTextField(
                        value = manualPsidts,
                        onValueChange = { updated ->
                            manualPsidts = updated
                            detectedCookies = evaluateInputs(rawCookieInput, manualPsid, updated)
                        },
                        label = { Text("__Secure-1PSIDTS (Pendamping Sesi)") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("manual_psidts_input")
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = {
                            val finalCookies = evaluateInputs(rawCookieInput, manualPsid, manualPsidts)
                            if (finalCookies.isValid) {
                                onCookiesExtracted(finalCookies)
                                onDismiss()
                            } else {
                                statusHint = "Mohon isi nilai __Secure-1PSID terlebih dahulu sebelum menyimpan."
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF2962FF),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("confirm_save_cookie_vault_button")
                    ) {
                        Text(
                            text = "Simpan & Aktifkan Cookie Gemini",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
