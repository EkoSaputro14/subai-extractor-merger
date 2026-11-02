package com.example.ui.components

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.view.View
import android.webkit.CookieManager
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Cookie
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.GeminiWebCookieClient
import com.example.data.ParsedGeminiCookies
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream

private const val LIGHTWEIGHT_LOGIN_URL =
    "https://accounts.google.com/ServiceLogin?hl=id&continue=https://gemini.google.com/app"
private const val GEMINI_WEB_APP_URL = "https://gemini.google.com/app"

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun GeminiCookieWebViewDialog(
    onDismiss: () -> Unit,
    onCookiesExtracted: (ParsedGeminiCookies) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var detectedCookies by remember { mutableStateOf(ParsedGeminiCookies()) }
    var isLoadingPage by remember { mutableStateOf(true) }
    var turboMode by remember { mutableStateOf(true) }
    var autoCloseOnCapture by remember { mutableStateOf(true) }
    var hasAutoSubmitted by remember { mutableStateOf(false) }
    var currentUrl by remember { mutableStateOf(LIGHTWEIGHT_LOGIN_URL) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var webViewError by remember { mutableStateOf<String?>(null) }
    var statusHint by remember {
        mutableStateOf("WebView Turbo aktif (tanpa gambar/font berat). Login akun Google untuk mengambil cookie otomatis.")
    }

    suspend fun readCookiesInBackground(flushToDisk: Boolean = false): ParsedGeminiCookies =
        withContext(Dispatchers.IO) {
            runCatching {
                val cookieManager = CookieManager.getInstance()
                if (flushToDisk) {
                    cookieManager.flush()
                }
                val geminiCookies = cookieManager.getCookie("https://gemini.google.com").orEmpty()
                val accountsCookies = cookieManager.getCookie("https://accounts.google.com").orEmpty()
                val googleCookies = cookieManager.getCookie("https://www.google.com").orEmpty()
                val rootGoogleCookies = cookieManager.getCookie("https://google.com").orEmpty()
                val combined = "$geminiCookies; $accountsCookies; $googleCookies; $rootGoogleCookies"
                GeminiWebCookieClient.parseSmartCookieInput(combined)
            }.getOrDefault(ParsedGeminiCookies())
        }

    // Background non-blocking cookie poller: checks every 1.2s and auto-captures when valid
    LaunchedEffect(autoCloseOnCapture) {
        while (isActive && !hasAutoSubmitted) {
            val parsed = readCookiesInBackground(flushToDisk = false)
            if (parsed.isValid) {
                detectedCookies = parsed
                if (autoCloseOnCapture && !hasAutoSubmitted) {
                    hasAutoSubmitted = true
                    withContext(Dispatchers.IO) {
                        runCatching { CookieManager.getInstance().flush() }
                    }
                    onCookiesExtracted(parsed)
                    onDismiss()
                    break
                } else {
                    statusHint =
                        "Cookie __Secure-1PSID ditemukan! Ketuk 'Gunakan Cookie Ini' untuk menyimpan."
                }
            }
            delay(1200L)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            runCatching {
                webViewRef?.stopLoading()
                webViewRef?.destroy()
                webViewRef = null
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
                .padding(10.dp),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 4.dp
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Compact Top Bar
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
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
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Column {
                                    Text(
                                        text = "WebView Turbo — Ambil Cookie Gemini",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = currentUrl,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1
                                    )
                                }
                            }

                            IconButton(
                                onClick = {
                                    scope.launch {
                                        val parsed = readCookiesInBackground(flushToDisk = true)
                                        if (parsed.isValid) {
                                            detectedCookies = parsed
                                        }
                                    }
                                    runCatching { webViewRef?.reload() }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Muat Ulang Halaman"
                                )
                            }

                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier.testTag("close_cookie_webview_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Tutup"
                                )
                            }
                        }

                        // Turbo Toggle & External Browser Chips
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FilterChip(
                                selected = turboMode,
                                onClick = {
                                    turboMode = !turboMode
                                    webViewRef?.settings?.apply {
                                        blockNetworkImage = turboMode
                                        loadsImagesAutomatically = !turboMode
                                    }
                                    statusHint = if (turboMode) {
                                        "Mode Turbo aktif: gambar & font berat diblokir agar login super cepat."
                                    } else {
                                        "Mode Penuh aktif: gambar ditampilkan (gunakan jika ada Captcha)."
                                    }
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Bolt,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                label = { Text(if (turboMode) "Turbo Aktif" else "Mode Penuh") },
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

                        // Status & Capture Row
                        Surface(
                            color = if (detectedCookies.isValid) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f)
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = statusHint,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (detectedCookies.isValid) {
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.onSecondaryContainer
                                    }
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    FilledTonalButton(
                                        onClick = {
                                            scope.launch {
                                                val result = readCookiesInBackground(flushToDisk = true)
                                                if (result.isValid) {
                                                    detectedCookies = result
                                                    onCookiesExtracted(result)
                                                    onDismiss()
                                                } else {
                                                    statusHint =
                                                        "Cookie __Secure-1PSID belum terdeteksi. Selesaikan login Google di bawah atau gunakan 'Browser HP' + 'Smart Paste'."
                                                }
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
                                            Text("Gunakan Cookie Ini")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                if (isLoadingPage) {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                    )
                }

                // WebView Area
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    if (webViewError != null) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = webViewError ?: "",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        AndroidView(
                            modifier = Modifier.fillMaxSize(),
                            factory = { ctx ->
                                WebView(ctx).apply {
                                    webViewRef = this
                                    // Software layer prevents rendernode crash on cloud emulators without DRM GPU node
                                    setLayerType(View.LAYER_TYPE_SOFTWARE, null)

                                    runCatching {
                                        val cm = CookieManager.getInstance()
                                        cm.setAcceptCookie(true)
                                        cm.setAcceptThirdPartyCookies(this, true)
                                    }

                                    settings.apply {
                                        javaScriptEnabled = true
                                        domStorageEnabled = true
                                        databaseEnabled = true
                                        // Turbo optimization: block heavy network images & media during login
                                        blockNetworkImage = turboMode
                                        loadsImagesAutomatically = !turboMode
                                        cacheMode = WebSettings.LOAD_DEFAULT
                                        useWideViewPort = true
                                        loadWithOverviewMode = true
                                        setSupportZoom(false)
                                    }

                                    webViewClient = object : WebViewClient() {
                                        override fun shouldInterceptRequest(
                                            view: WebView?,
                                            request: WebResourceRequest?
                                        ): WebResourceResponse? {
                                            if (!turboMode || request == null) {
                                                return super.shouldInterceptRequest(view, request)
                                            }
                                            val urlStr = request.url?.toString()?.lowercase().orEmpty()
                                            val isCaptcha = urlStr.contains("recaptcha") ||
                                                urlStr.contains("captcha") ||
                                                urlStr.contains("challenge")
                                            if (!isCaptcha && isHeavyNonAuthResource(urlStr)) {
                                                return WebResourceResponse(
                                                    "text/plain",
                                                    "UTF-8",
                                                    ByteArrayInputStream(ByteArray(0))
                                                )
                                            }
                                            return super.shouldInterceptRequest(view, request)
                                        }

                                        override fun onPageStarted(
                                            view: WebView?,
                                            url: String?,
                                            favicon: Bitmap?
                                        ) {
                                            super.onPageStarted(view, url, favicon)
                                            isLoadingPage = true
                                            if (!url.isNullOrBlank()) {
                                                currentUrl = url
                                            }
                                        }

                                        override fun onPageFinished(view: WebView?, url: String?) {
                                            super.onPageFinished(view, url)
                                            isLoadingPage = false
                                            if (!url.isNullOrBlank()) {
                                                currentUrl = url
                                            }
                                            scope.launch {
                                                val parsed = readCookiesInBackground(flushToDisk = true)
                                                if (parsed.isValid) {
                                                    detectedCookies = parsed
                                                    if (autoCloseOnCapture && !hasAutoSubmitted) {
                                                        hasAutoSubmitted = true
                                                        onCookiesExtracted(parsed)
                                                        onDismiss()
                                                    }
                                                }
                                            }
                                        }

                                        override fun shouldOverrideUrlLoading(
                                            view: WebView?,
                                            request: WebResourceRequest?
                                        ): Boolean = false

                                        override fun onRenderProcessGone(
                                            view: WebView?,
                                            detail: RenderProcessGoneDetail?
                                        ): Boolean {
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                                isLoadingPage = false
                                                webViewError =
                                                    "Proses perender WebView dihentikan oleh sistem. Gunakan tombol 'Browser HP' + 'Smart Paste Cookie'."
                                            }
                                            return true
                                        }
                                    }

                                    runCatching {
                                        loadUrl(LIGHTWEIGHT_LOGIN_URL)
                                    }.onFailure { err ->
                                        webViewError = "Gagal memuat WebView: ${err.localizedMessage}"
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

private fun isHeavyNonAuthResource(lowerUrl: String): Boolean {
    val pathBeforeQuery = lowerUrl.substringBefore('?')
    return pathBeforeQuery.endsWith(".png") ||
        pathBeforeQuery.endsWith(".jpg") ||
        pathBeforeQuery.endsWith(".jpeg") ||
        pathBeforeQuery.endsWith(".gif") ||
        pathBeforeQuery.endsWith(".webp") ||
        pathBeforeQuery.endsWith(".svg") ||
        pathBeforeQuery.endsWith(".woff2") ||
        pathBeforeQuery.endsWith(".woff") ||
        pathBeforeQuery.endsWith(".ttf") ||
        pathBeforeQuery.endsWith(".otf") ||
        pathBeforeQuery.endsWith(".mp4") ||
        pathBeforeQuery.endsWith(".webm") ||
        lowerUrl.contains("play.google.com/log") ||
        lowerUrl.contains("googletagmanager.com") ||
        lowerUrl.contains("doubleclick.net") ||
        lowerUrl.contains("gstatic.com/lamda/images")
}
