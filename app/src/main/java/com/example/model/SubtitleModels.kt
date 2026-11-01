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

enum class TranslationStylePreset(val label: String, val styleInstruction: String) {
    ANIME_CASUAL(
        "Anime / Santai Alami",
        "Gunakan gaya percakapan anime/serial yang luwes, alami, tidak kaku, dan pertahankan nuansa emosi karakter."
    ),
    MOVIE_FORMAL(
        "Film / Sinema Standar",
        "Gunakan Bahasa baku yang mengalir alami seperti takarir bioskop/platform streaming resmi, ringkas, dan mudah dibaca cepat."
    ),
    DOCUMENTARY_TECH(
        "Dokumenter / Informatif",
        "Gunakan struktur kalimat yang presisi, jelas, dan pertahankan keakuratan istilah teknis."
    ),
    CUSTOM(
        "Template Kustom",
        "Terjemahkan secara akurat dengan tetap mempertahankan konteks dialog."
    )
}

enum class AiProviderPreset(
    val label: String,
    val defaultBaseUrl: String,
    val defaultModel: String,
    val recommendedModels: List<String>
) {
    GEMINI_WEB_COOKIE(
        label = "Gemini-Web2API (Cookie)",
        defaultBaseUrl = "https://gemini.google.com",
        defaultModel = "gemini-3.8-flash",
        recommendedModels = listOf(
            "gemini-3.8-flash",
            "gemini-4-argon",
            "gemini-3.5-flash",
            "gemini-3.5-flash-thinking",
            "gemini-3.0-pro",
            "gemini-3.0-flash",
            "gemini-2.5-flash",
            "gemini-2.5-pro",
            "gemini-2.0-flash-thinking",
            "gemini-2.0-flash"
        )
    ),
    GEMINI_WEB_SERVER(
        label = "Gemini-Web2API (Server)",
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
            "gemini-2.5-pro",
            "gemini-2.0-flash-thinking",
            "gemini-2.0-flash"
        )
    ),
    DEEPSEEK(
        label = "DeepSeek",
        defaultBaseUrl = "https://api.deepseek.com/v1",
        defaultModel = "deepseek-chat",
        recommendedModels = listOf("deepseek-chat", "deepseek-reasoner")
    ),
    OPENAI(
        label = "OpenAI",
        defaultBaseUrl = "https://api.openai.com/v1",
        defaultModel = "gpt-4o-mini",
        recommendedModels = listOf("gpt-4o-mini", "gpt-4o", "gpt-4.1-mini", "gpt-4.1", "o3-mini")
    ),
    OPENROUTER(
        label = "OpenRouter",
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
    GROQ(
        label = "Groq",
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
        label = "Custom URL",
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
    DARK("Gelap (Default)"),
    LIGHT("Terang"),
    SYSTEM("Ikuti Sistem")
}

enum class ColorPreset(
    val presetName: String,
    val subtitleDesc: String,
    val darkPrimary: Color,
    val lightPrimary: Color,
    val darkSecondary: Color,
    val lightSecondary: Color
) {
    OCEAN_BLUE(
        presetName = "Ocean Blue",
        subtitleDesc = "Bersih, teknis, kontras tinggi untuk kode .ass",
        darkPrimary = Color(0xFF8AB4F8),
        lightPrimary = Color(0xFF1A73E8),
        darkSecondary = Color(0xFF81C995),
        lightSecondary = Color(0xFF1E8E3E)
    ),
    EMERALD_ANIME(
        presetName = "Emerald Anime",
        subtitleDesc = "Segar, nyaman untuk pemeriksaan baris panjang",
        darkPrimary = Color(0xFF6DD58C),
        lightPrimary = Color(0xFF137333),
        darkSecondary = Color(0xFF78D9EC),
        lightSecondary = Color(0xFF007B83)
    ),
    VIOLET_STUDIO(
        presetName = "Violet Studio",
        subtitleDesc = "Nuansa aplikasi editor kreatif modern",
        darkPrimary = Color(0xFFD0BCFF),
        lightPrimary = Color(0xFF6750A4),
        darkSecondary = Color(0xFFCCC2DC),
        lightSecondary = Color(0xFF625B71)
    ),
    AMBER_CINEMA(
        presetName = "Amber Cinema",
        subtitleDesc = "Hangat, terinspirasi warna subtitle klasik bioskop",
        darkPrimary = Color(0xFFFFB951),
        lightPrimary = Color(0xFFB26A00),
        darkSecondary = Color(0xFFFFDDB3),
        lightSecondary = Color(0xFF825500)
    ),
    CRIMSON_ROSE(
        presetName = "Crimson Rose",
        subtitleDesc = "Tegas, memudahkan penandaan baris yang terlewat",
        darkPrimary = Color(0xFFFFB2B7),
        lightPrimary = Color(0xFFC5221F),
        darkSecondary = Color(0xFFF2B8B5),
        lightSecondary = Color(0xFF8C1D18)
    )
}

enum class EditorFontSizeOption(val label: String, val sizeSp: Int) {
    SMALL("Kecil (11sp)", 11),
    MEDIUM("Sedang (13sp)", 13),
    LARGE("Besar (15sp)", 15),
    EXTRA_LARGE("Ekstra Besar (17sp)", 17)
}

data class ThemeSettings(
    val themeMode: ThemeMode = ThemeMode.DARK,
    val colorPreset: ColorPreset = ColorPreset.OCEAN_BLUE,
    val useDynamicColor: Boolean = false,
    val pureBlackAmoled: Boolean = false,
    val editorFontSizeSp: Int = 13,
    val useMonospaceFont: Boolean = true
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
