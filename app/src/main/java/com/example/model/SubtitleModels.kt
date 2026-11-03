package com.example.model

import androidx.compose.ui.graphics.Color

enum class SubtitleFormat(val extension: String, val label: String, val mimeType: String) {
    ASS("ass", ".ASS (Advanced SubStation Alpha)", "text/x-ssa"),
    SSA("ssa", ".SSA (SubStation Alpha)", "text/x-ssa"),
    SRT("srt", ".SRT (SubRip Subtitle)", "application/x-subrip"),
    VTT("vtt", ".VTT (WebVTT)", "text/vtt");

    val isAssFamily: Boolean
        get() = this == ASS || this == SSA
}

data class SubtitleCue(
    val id: Int, // 1-based sequential index
    val rawLineIndex: Int, // Line index in rawLines for ASS/SSA, or block index for SRT/VTT
    val startTime: String,
    val endTime: String,
    val style: String = "Default",
    val actor: String = "",
    val effect: String = "",
    val assPrefix: String = "", // Everything before Text column in ASS Dialogue line
    val leadingAssTags: String = "", // Leading override tags e.g. {\an8}{\pos(960,85)}
    val rawText: String, // Original text content (including inline tags & \N)
    val textWithoutLeadingTags: String, // Text after leadingAssTags (still has inline tags & \N)
    val cleanText: String, // All {\...} tags stripped (still has \N or \n)
    val vttCueHeader: String = "", // Optional cue identifier before timestamp in VTT
    val vttCueSettings: String = "" // Optional positioning settings after timestamp in VTT
)

data class SubtitleDocument(
    val fileName: String,
    val format: SubtitleFormat,
    val rawContent: String,
    val rawLines: List<String>,
    val assEventsFormatColumns: List<String> = emptyList(),
    val cues: List<SubtitleCue>
) {
    val availableStyles: List<String>
        get() = cues.mapNotNull { it.style.takeIf { s -> s.isNotBlank() } }.distinct()

    val availableActors: List<String>
        get() = cues.mapNotNull { it.actor.takeIf { a -> a.isNotBlank() } }.distinct()
}

enum class LineSeparatorMode(val label: String, val separatorToken: String, val description: String) {
    DOUBLE_SLASH("Simbol //", " // ", "Gabung dengan ' // ' & pulihkan ke \\N / baris baru saat merge"),
    PIPE("Simbol |", " | ", "Gabung dengan ' | ' & pulihkan ke \\N / baris baru saat merge"),
    SPACE("Spasi Biasa", " ", "Gabung menjadi 1 baris tunggal permanen dengan spasi"),
    CUSTOM("Kustom", " // ", "Gunakan simbol pemisah kustom pilihan Anda")
}

enum class IndexFormatMode(val label: String, val example: String) {
    BRACKET("[1] Teks", "[1] Halo dunia"),
    DOT("1. Teks", "1. Halo dunia"),
    PIPE("1 | Teks", "1 | Halo dunia");

    fun formatLine(id: Int, text: String): String = when (this) {
        BRACKET -> "[$id] $text"
        DOT -> "$id. $text"
        PIPE -> "$id | $text"
    }
}

enum class BatchSizeOption(val label: String, val size: Int) {
    ALL("Semua", 0),
    BATCH_25("25 Baris", 25),
    BATCH_50("50 Baris", 50),
    BATCH_100("100 Baris", 100),
    BATCH_200("200 Baris", 200)
}

enum class TargetLanguageOption(val label: String, val promptName: String) {
    INDONESIAN("Bahasa Indonesia", "Bahasa Indonesia"),
    ENGLISH("English", "English"),
    MALAY("Bahasa Melayu", "Bahasa Melayu"),
    JAPANESE("日本語 (Jepang)", "Japanese"),
    CUSTOM("Kustom", "")
}

enum class ExtractionRulePreset(
    val title: String,
    val subtitle: String,
    val keepAssTags: Boolean,
    val includeStyleActor: Boolean,
    val mergeConsecutive: Boolean,
    val removeComments: Boolean,
    val ignoreEmptyLines: Boolean
) {
    STANDAR(
        title = "Standar",
        subtitle = "Umum (SRT/ASS)",
        keepAssTags = true,
        includeStyleActor = false,
        mergeConsecutive = true,
        removeComments = true,
        ignoreEmptyLines = true
    ),
    ANIME(
        title = "Anime",
        subtitle = "Menjaga tag & efek",
        keepAssTags = true,
        includeStyleActor = true,
        mergeConsecutive = false,
        removeComments = true,
        ignoreEmptyLines = true
    ),
    BIOSKOP(
        title = "Bioskop",
        subtitle = "Dialog film/drama",
        keepAssTags = false,
        includeStyleActor = false,
        mergeConsecutive = true,
        removeComments = true,
        ignoreEmptyLines = true
    ),
    MINIMAL(
        title = "Minimal",
        subtitle = "Hapus tag & posisi",
        keepAssTags = false,
        includeStyleActor = false,
        mergeConsecutive = false,
        removeComments = true,
        ignoreEmptyLines = true
    )
}

enum class TranslationStylePreset(
    val label: String,
    val shortTitle: String,
    val shortSubtitle: String,
    val styleInstruction: String
) {
    ANIME_CASUAL(
        label = "Anime Casual",
        shortTitle = "Anime Casual",
        shortSubtitle = "Natural, sesuai gaya subtitle anime",
        styleInstruction = "Terjemahkan teks subtitle berikut ke Bahasa Indonesia.\n\nAturan:\n- Pertahankan nomor baris [ID].\n- Jangan mengubah tag ASS/formatting.\n- Jaga makna, gaya, dan istilah khusus (gunakan kamus glosarium jika ada)."
    ),
    MOVIE_FORMAL(
        label = "Formal Bioskop",
        shortTitle = "Formal Bioskop",
        shortSubtitle = "Rapi dan formal (film/drama)",
        styleInstruction = "Terjemahkan teks subtitle berikut ke Bahasa Indonesia dengan gaya formal bioskop.\n\nAturan:\n- Pertahankan nomor baris [ID].\n- Jangan mengubah tag ASS/formatting.\n- Gunakan Bahasa baku yang mengalir alami, ringkas, dan mudah dibaca cepat."
    ),
    LITERAL(
        label = "Literal",
        shortTitle = "Literal",
        shortSubtitle = "Terjemahan setia, minim adaptasi",
        styleInstruction = "Terjemahkan teks subtitle berikut ke Bahasa Indonesia secara setia dan literal.\n\nAturan:\n- Pertahankan nomor baris [ID].\n- Jangan mengubah tag ASS/formatting.\n- Jaga struktur kalimat asli dengan minim adaptasi bebas."
    ),
    CREATIVE(
        label = "Kreatif",
        shortTitle = "Kreatif",
        shortSubtitle = "Lebih natural dan enak dibaca",
        styleInstruction = "Terjemahkan teks subtitle berikut ke Bahasa Indonesia secara ekspresif dan natural.\n\nAturan:\n- Pertahankan nomor baris [ID].\n- Jangan mengubah tag ASS/formatting.\n- Sesuaikan idiom agar terdengar mengalir dan enak dibaca."
    ),
    DOCUMENTARY_TECH(
        label = "Dokumenter / Informatif",
        shortTitle = "Dokumenter",
        shortSubtitle = "Presisi untuk istilah teknis",
        styleInstruction = "Gunakan struktur kalimat yang presisi, jelas, dan pertahankan keakuratan istilah teknis."
    ),
    CUSTOM(
        label = "Kustom Saya",
        shortTitle = "Kustom Saya",
        shortSubtitle = "Template buatan sendiri",
        styleInstruction = "Terjemahkan teks subtitle berikut ke Bahasa Indonesia.\n\nAturan:\n- Pertahankan nomor baris [ID].\n- Jangan mengubah tag ASS/formatting.\n- Jaga makna, gaya, dan istilah khusus (gunakan kamus glosarium jika ada)."
    )
}

enum class AiProviderPreset(
    val label: String,
    val displayTitle: String,
    val defaultBaseUrl: String,
    val defaultModel: String,
    val recommendedModels: List<String>
) {
    GEMINI_WEB_COOKIE(
        label = "Gemini-Web2API (Cookie)",
        displayTitle = "Gemini Web Cookie",
        defaultBaseUrl = "https://gemini.google.com",
        defaultModel = "gemini-3.8-flash",
        recommendedModels = listOf(
            "gemini-3.0-flash",
            "gemini-3.8-flash",
            "gemini-4-argon",
            "gemini-3.5-flash",
            "gemini-3.5-flash-thinking",
            "gemini-3.0-pro",
            "gemini-2.5-flash",
            "gemini-2.5-pro",
            "gemini-2.0-flash-thinking",
            "gemini-2.0-flash"
        )
    ),
    OPENROUTER(
        label = "OpenRouter",
        displayTitle = "OpenRouter",
        defaultBaseUrl = "https://openrouter.ai/api/v1",
        defaultModel = "google/gemini-3.8-flash",
        recommendedModels = listOf(
            "google/gemini-3.8-flash",
            "google/gemini-2.5-flash",
            "google/gemini-2.5-pro",
            "deepseek/deepseek-chat",
            "deepseek/deepseek-r1",
            "anthropic/claude-3.5-sonnet"
        )
    ),
    GOOGLE_GEMINI_API(
        label = "Google Gemini API",
        displayTitle = "Google Gemini API",
        defaultBaseUrl = "https://generativelanguage.googleapis.com/v1beta/openai",
        defaultModel = "gemini-2.5-flash",
        recommendedModels = listOf(
            "gemini-2.5-flash",
            "gemini-2.5-pro",
            "gemini-2.0-flash"
        )
    ),
    DEEPSEEK(
        label = "DeepSeek",
        displayTitle = "DeepSeek",
        defaultBaseUrl = "https://api.deepseek.com/v1",
        defaultModel = "deepseek-chat",
        recommendedModels = listOf("deepseek-chat", "deepseek-reasoner")
    ),
    GROQ(
        label = "Groq",
        displayTitle = "Groq",
        defaultBaseUrl = "https://api.groq.com/openai/v1",
        defaultModel = "llama-3.3-70b-versatile",
        recommendedModels = listOf(
            "llama-3.3-70b-versatile",
            "llama-3.1-8b-instant",
            "qwen-qwq-32b",
            "gemma2-9b-it"
        )
    ),
    CUSTOM(
        label = "Custom OpenAI",
        displayTitle = "Custom OpenAI",
        defaultBaseUrl = "http://127.0.0.1:8000/v1",
        defaultModel = "gemini-3.8-flash",
        recommendedModels = listOf(
            "gemini-3.8-flash",
            "gemini-4-argon",
            "gemini-3.5-flash",
            "gemini-2.5-flash",
            "gemini-2.5-pro",
            "deepseek-chat",
            "gpt-4o-mini"
        )
    ),
    GEMINI_WEB_SERVER(
        label = "Gemini-Web2API (Server)",
        displayTitle = "Gemini Web Server",
        defaultBaseUrl = "http://127.0.0.1:8000/v1",
        defaultModel = "gemini-3.8-flash",
        recommendedModels = listOf(
            "gemini-3.8-flash",
            "gemini-4-argon",
            "gemini-3.5-flash",
            "gemini-3.5-flash-thinking",
            "gemini-3.0-pro",
            "gemini-3.0-flash",
            "gemini-2.5-flash",
            "gemini-2.5-pro"
        )
    ),
    OPENAI(
        label = "OpenAI",
        displayTitle = "OpenAI Official",
        defaultBaseUrl = "https://api.openai.com/v1",
        defaultModel = "gpt-4o-mini",
        recommendedModels = listOf("gpt-4o-mini", "gpt-4o", "gpt-4.1-mini", "gpt-4.1", "o3-mini")
    );

    val isGeminiWebFamily: Boolean
        get() = this == GEMINI_WEB_COOKIE || this == GEMINI_WEB_SERVER
}

/**
 * Determines how the System Prompt adapts its structure and formatting constraints
 * to the selected language model (Gemini 3.8 Flash, Gemini 4 Argon, Thinking, Pro, DeepSeek, OpenAI, etc.).
 */
enum class ModelPromptProfile(
    val badgeTitle: String,
    val adaptationSummary: String
) {
    REASONING_DIRECT(
        badgeTitle = "Profil Model Reasoning / Thinking (Gemini Thinking / R1)",
        adaptationSummary = "Menekan analisis bertele-tele & mengunci keluaran akhir pasca-<think> agar 100% hanya daftar baris bernomor tanpa teks pengantar."
    ),
    COMPACT_FEW_SHOT(
        badgeTitle = "Profil Model Cepat / Gemini Flash (Few-Shot Lock)",
        adaptationSummary = "Menyisipkan contoh konkret Input → Output sesuai format nomor & pemisah aktif agar Gemini Flash / model cepat tidak menggeser indeks baris."
    ),
    FLAGSHIP_CONTEXTUAL(
        badgeTitle = "Profil Model Frontier / Argon & Pro Kontekstual",
        adaptationSummary = "Mengoptimalkan kesinambungan antar-dialog, pendalaman karakter, serta kepatuhan presisi pada aturan pemisah dan glosarium."
    );

    companion object {
        fun fromModelId(modelId: String): ModelPromptProfile {
            val lower = modelId.lowercase().trim()
            return when {
                lower.contains("reasoner") ||
                    lower.contains("deepseek-r1") ||
                    lower.endsWith("/r1") ||
                    lower.contains("qwq") ||
                    lower.startsWith("o1") ||
                    lower.startsWith("o3") ||
                    lower.contains("/o1") ||
                    lower.contains("/o3") ||
                    lower.contains("thinking") ||
                    lower.contains("@think") -> REASONING_DIRECT

                lower.contains("-mini") ||
                    lower.endsWith("mini") ||
                    lower.contains("flash") ||
                    lower.contains("instant") ||
                    lower.contains("8b") ||
                    lower.contains("7b") ||
                    lower.contains("9b") ||
                    lower.contains("gemma") ||
                    lower.contains("nano") ||
                    lower.contains("lite") -> COMPACT_FEW_SHOT

                else -> FLAGSHIP_CONTEXTUAL
            }
        }
    }
}

data class AiProviderSettings(
    val preset: AiProviderPreset = AiProviderPreset.GEMINI_WEB_COOKIE,
    val baseUrl: String = AiProviderPreset.GEMINI_WEB_COOKIE.defaultBaseUrl,
    val apiKey: String = "",
    val geminiSecure1Psid: String = "",
    val geminiSecure1Psidts: String = "",
    val combineSystemIntoUserPrompt: Boolean = true,
    val selectedModel: String = AiProviderPreset.GEMINI_WEB_COOKIE.defaultModel,
    val fetchedModels: List<String> = emptyList(),
    val temperature: Float = 0.3f
) {
    val activeModelProfile: ModelPromptProfile
        get() = ModelPromptProfile.fromModelId(selectedModel)

    val hasGeminiCookie: Boolean
        get() = geminiSecure1Psid.trim().isNotEmpty()

    val availableModelChoices: List<String>
        get() = (fetchedModels + preset.recommendedModels + listOf(selectedModel))
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
}

enum class BilingualOrder(val label: String, val description: String) {
    TRANSLATION_TOP("Terjemahan di Atas (Rekomendasi)", "Baris atas: Terjemahan | Baris bawah: Teks Asli (ukuran/warna sekunder pada .ASS)"),
    ORIGINAL_TOP("Teks Asli di Atas", "Baris atas: Teks Asli | Baris bawah: Terjemahan")
}

data class GlossaryRule(
    val id: String,
    val sourceTerm: String,
    val targetTerm: String,
    val isCaseSensitive: Boolean = false,
    val wholeWord: Boolean = true,
    val isEnabled: Boolean = true
)

enum class ThemeMode(val label: String) {
    SYSTEM("Sistem"),
    LIGHT("Terang"),
    DARK("Gelap")
}

enum class ColorPreset(
    val presetName: String,
    val subtitleDesc: String,
    val darkPrimary: Color,
    val lightPrimary: Color,
    val darkSecondary: Color,
    val lightSecondary: Color,
    val swatchColor: Color
) {
    EMERALD_ANIME(
        presetName = "Forest Sage",
        subtitleDesc = "Hijau hutan & sage lembut khas SubAI Studio",
        darkPrimary = Color(0xFF74C69D),
        lightPrimary = Color(0xFF1B4332),
        darkSecondary = Color(0xFF95D5B2),
        lightSecondary = Color(0xFF2D6A4F),
        swatchColor = Color(0xFF2D6A4F)
    ),
    VIOLET_STUDIO(
        presetName = "Violet Studio",
        subtitleDesc = "Nuansa aplikasi editor kreatif modern",
        darkPrimary = Color(0xFFD0BCFF),
        lightPrimary = Color(0xFF6750A4),
        darkSecondary = Color(0xFFCCC2DC),
        lightSecondary = Color(0xFF625B71),
        swatchColor = Color(0xFF8E7CC3)
    ),
    CRIMSON_ROSE(
        presetName = "Sakura Rose",
        subtitleDesc = "Lembut & tegas, memudahkan penandaan baris",
        darkPrimary = Color(0xFFFFB2B7),
        lightPrimary = Color(0xFFB3261E),
        darkSecondary = Color(0xFFF2B8B5),
        lightSecondary = Color(0xFF8C1D18),
        swatchColor = Color(0xFFF4979C)
    ),
    OCEAN_BLUE(
        presetName = "Ocean Blue",
        subtitleDesc = "Bersih, teknis, kontras tinggi untuk kode .ass",
        darkPrimary = Color(0xFF8AB4F8),
        lightPrimary = Color(0xFF1A73E8),
        darkSecondary = Color(0xFF81C995),
        lightSecondary = Color(0xFF1E8E3E),
        swatchColor = Color(0xFF5FA8D3)
    ),
    AMBER_CINEMA(
        presetName = "Amber Gold",
        subtitleDesc = "Hangat, terinspirasi warna subtitle klasik bioskop",
        darkPrimary = Color(0xFFFFB951),
        lightPrimary = Color(0xFFB26A00),
        darkSecondary = Color(0xFFFFDDB3),
        lightSecondary = Color(0xFF825500),
        swatchColor = Color(0xFFF3C663)
    )
}

enum class EditorFontSizeOption(val label: String, val shortLabel: String, val sizeSp: Int) {
    SMALL("Kecil (12sp)", "12", 12),
    MEDIUM("Sedang (14sp)", "14", 14),
    LARGE("Besar (16sp)", "16", 16),
    EXTRA_LARGE("Ekstra (18sp)", "18", 18)
}

data class ThemeSettings(
    val themeMode: ThemeMode = ThemeMode.LIGHT,
    val colorPreset: ColorPreset = ColorPreset.EMERALD_ANIME,
    val useDynamicColor: Boolean = false,
    val pureBlackAmoled: Boolean = false,
    val editorFontSizeSp: Int = 16,
    val useMonospaceFont: Boolean = false
)

data class FindMatchItem(
    val cueId: Int,
    val style: String,
    val actor: String,
    val currentTranslatedText: String,
    val previewReplacedText: String,
    val matchRanges: List<IntRange>
)

data class MergeCompareItem(
    val cue: SubtitleCue,
    val isTranslated: Boolean,
    val finalMergedLineText: String,
    val rawTranslatedLine: String
)
