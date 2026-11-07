package com.example.domain

import com.example.model.BilingualOrder
import com.example.model.FindMatchItem
import com.example.model.GlossaryRule
import com.example.model.IndexFormatMode
import com.example.model.LineSeparatorMode
import com.example.model.MergeCompareItem
import com.example.model.ModelPromptProfile
import com.example.model.SubtitleCue
import com.example.model.SubtitleDocument
import com.example.model.SubtitleFormat
import com.example.model.TargetLanguageOption
import com.example.model.TranslationStylePreset
import java.util.regex.Pattern

data class AiLineParseReport(
    val parsedLines: Map<Int, String>,
    val strategyUsed: String
)

object SubtitleParserAndMerger {

    // Stage A1: Flexible inline numbered line matcher after stripping leading markdown bullets/bold/backticks
    // Matches: [1] Text, [001]: Text, [1] - Text, (#1) Text, (1) Text, #1: Text, 1. Text, 1) Text, 1 | Text, 1: Text, 1 - Text, Baris 1: Text, Line 1: Text
    private val AI_FLEXIBLE_INDEX_LINE_REGEX = Regex(
        """^(?:(?:baris|line|dialog|cue)\s*)?(?:\[\s*#?\s*(\d+)\s*\]\s*[:.\-|]*|\(\s*#?\s*(\d+)\s*\)\s*[:.\-|]*|#\s*(\d+)\s*[:.\-|)+]*|(\d+)\s*(?:\.|\||:|-|\)))\s*(.+)$""",
        RegexOption.IGNORE_CASE
    )

    // Stage A2: Markdown table row e.g. | 1 | Teks terjemahan | or | [1] | Teks |
    private val AI_MARKDOWN_TABLE_ROW_REGEX = Regex(
        """^\|\s*(?:\[\s*#?\s*(\d+)\s*\]|#?\s*(\d+))\s*\|\s*(.+?)\s*\|?$"""
    )

    // Stage A3: JSON key-value line e.g. "1": "Teks" or "[1]": "Teks" or {"id": 1, "text": "Teks"}
    private val AI_JSON_KV_LINE_REGEX = Regex(
        """^["']?\[?#?(\d+)\]?["']?\s*:\s*["'](.+?)["']\s*,?\s*$"""
    )
    private val AI_JSON_OBJ_LINE_REGEX = Regex(
        """^\{[^}]*?"(?:id|index|line)"\s*:\s*["']?(\d+)["']?[^}]*?"(?:text|translation|content)"\s*:\s*"([^"]+)"[^}]*\}\s*,?\s*$""",
        RegexOption.IGNORE_CASE
    )

    // Stage B: Standalone index line where text is placed on the next line(s), e.g. "[1]", "**[1]**", "1.", "#1", "(1)"
    private val AI_STANDALONE_INDEX_LINE_REGEX = Regex(
        """^(?:(?:baris|line|dialog|cue)\s*)?(?:\[\s*#?\s*(\d+)\s*\]|\(\s*#?\s*(\d+)\s*\)|#\s*(\d+)|(\d+)\s*[.):|\-]?)\s*[:.\-|]*$""",
        RegexOption.IGNORE_CASE
    )

    // Conversational AI intro/outro or refusal lines to ignore during Stage D positional fallback
    private val AI_CONVERSATIONAL_PREAMBLE_REGEX = Regex(
        """^(?:berikut\s+(?:adalah|ini)|ini\s+(?:adalah|hasil)|tentu[,!\s]|baik[,!\s]|here\s+(?:is|are)|sure[,!\s]|certainly[,!\s]|catatan\s*:|note\s*:|---\s*daftar|daftar\s+dialog|hasil\s+terjemahan|terjemahan\s+subtitle|semoga\s+membantu|maaf[,\s]+saya\s+tidak\s+dapat|i\s+cannot\s+fulfill|i['’]?m\s+sorry[,\s]+i\s+can['’]?t).*$""",
        RegexOption.IGNORE_CASE
    )

    private val LEADING_ASS_TAGS_REGEX = Regex("""^(?:\{[^}]*\})+""")
    private val ANY_ASS_TAG_REGEX = Regex("""\{[^}]*\}""")
    private val SRT_VTT_TIMESTAMP_REGEX =
        Regex("""^\s*(\d{1,2}:?\d{2}:\d{2}[,.]\d{3})\s*-->\s*(\d{1,2}:?\d{2}:\d{2}[,.]\d{3})(.*)$""")

    fun detectFormat(fileName: String, content: String): SubtitleFormat {
        val lower = fileName.lowercase()
        val cleanContent = content.removePrefix("\uFEFF")
        return when {
            lower.endsWith(".ass") -> SubtitleFormat.ASS
            lower.endsWith(".ssa") -> SubtitleFormat.SSA
            lower.endsWith(".vtt") -> SubtitleFormat.VTT
            lower.endsWith(".srt") -> SubtitleFormat.SRT
            cleanContent.contains("[Script Info]", ignoreCase = true) ||
                cleanContent.contains("[V4+ Styles]", ignoreCase = true) -> SubtitleFormat.ASS
            cleanContent.trimStart().startsWith("WEBVTT", ignoreCase = true) -> SubtitleFormat.VTT
            else -> SubtitleFormat.SRT
        }
    }

    fun parseSubtitle(fileName: String, rawContent: String): SubtitleDocument {
        val normalizedContent = rawContent
            .removePrefix("\uFEFF")
            .replace("\r\n", "\n")
            .replace("\r", "\n")
        val format = detectFormat(fileName, normalizedContent)
        return when (format) {
            SubtitleFormat.ASS, SubtitleFormat.SSA ->
                parseAssOrSsa(fileName, format, normalizedContent)
            SubtitleFormat.SRT, SubtitleFormat.VTT ->
                parseSrtOrVtt(fileName, format, normalizedContent)
        }
    }

    private fun parseAssOrSsa(
        fileName: String,
        format: SubtitleFormat,
        content: String
    ): SubtitleDocument {
        val lines = content.split("\n")
        var inEventsSection = false
        var formatColumns = listOf(
            "Layer", "Start", "End", "Style", "Name",
            "MarginL", "MarginR", "MarginV", "Effect", "Text"
        )
        var startIdx = 1
        var endIdx = 2
        var styleIdx = 3
        var nameIdx = 4
        var effectIdx = 8

        val cues = mutableListOf<SubtitleCue>()
        var cueCounter = 1

        for ((lineIndex, line) in lines.withIndex()) {
            val trimmed = line.trim()
            if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
                inEventsSection = trimmed.equals("[Events]", ignoreCase = true)
                continue
            }

            if (inEventsSection && trimmed.startsWith("Format:", ignoreCase = true)) {
                val cols = trimmed.substringAfter(":")
                    .split(",")
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                if (cols.size >= 2) {
                    formatColumns = cols
                    startIdx = cols.indexOfFirst { it.equals("Start", ignoreCase = true) }.takeIf { it >= 0 } ?: 1
                    endIdx = cols.indexOfFirst { it.equals("End", ignoreCase = true) }.takeIf { it >= 0 } ?: 2
                    styleIdx = cols.indexOfFirst { it.equals("Style", ignoreCase = true) }.takeIf { it >= 0 } ?: 3
                    nameIdx = cols.indexOfFirst {
                        it.equals("Name", ignoreCase = true) || it.equals("Actor", ignoreCase = true)
                    }.takeIf { it >= 0 } ?: 4
                    effectIdx = cols.indexOfFirst { it.equals("Effect", ignoreCase = true) }.takeIf { it >= 0 } ?: 8
                }
                continue
            }

            if (inEventsSection && trimmed.startsWith("Dialogue:", ignoreCase = true)) {
                val afterDialogueKeyword = line.substringAfter(":")
                val expectedColumns = formatColumns.size.coerceAtLeast(2)
                val parts = afterDialogueKeyword.split(",", limit = expectedColumns)
                if (parts.size < expectedColumns) continue

                val rawText = parts.last()
                val prefixColumns = parts.dropLast(1).joinToString(",")
                val dialogueKeyword = line.substringBefore(":")
                val assPrefix = "$dialogueKeyword:$prefixColumns,"

                val leadingMatch = LEADING_ASS_TAGS_REGEX.find(rawText)
                val leadingAssTags = leadingMatch?.value ?: ""
                val textWithoutLeading = if (leadingAssTags.isNotEmpty()) {
                    rawText.substring(leadingAssTags.length)
                } else {
                    rawText
                }
                val cleanText = ANY_ASS_TAG_REGEX.replace(textWithoutLeading, "")

                val startTime = parts.getOrNull(startIdx)?.trim() ?: ""
                val endTime = parts.getOrNull(endIdx)?.trim() ?: ""
                val style = parts.getOrNull(styleIdx)?.trim() ?: "Default"
                val actor = parts.getOrNull(nameIdx)?.trim() ?: ""
                val effect = parts.getOrNull(effectIdx)?.trim() ?: ""

                if (cleanText.trim().isEmpty() && rawText.trim().isEmpty()) continue

                cues.add(
                    SubtitleCue(
                        id = cueCounter++,
                        rawLineIndex = lineIndex,
                        startTime = startTime,
                        endTime = endTime,
                        style = style,
                        actor = actor,
                        effect = effect,
                        assPrefix = assPrefix,
                        leadingAssTags = leadingAssTags,
                        rawText = rawText,
                        textWithoutLeadingTags = textWithoutLeading,
                        cleanText = cleanText
                    )
                )
            }
        }

        return SubtitleDocument(
            fileName = fileName,
            format = format,
            rawContent = content,
            rawLines = lines,
            assEventsFormatColumns = formatColumns,
            cues = cues
        )
    }

    private fun parseSrtOrVtt(
        fileName: String,
        format: SubtitleFormat,
        content: String
    ): SubtitleDocument {
        val blocks = content.trim().split(Regex("""\n\s*\n"""))
        val cues = mutableListOf<SubtitleCue>()
        var cueCounter = 1

        for ((blockIndex, rawBlock) in blocks.withIndex()) {
            val blockLines = rawBlock.lines().map { it.trimEnd() }
            if (blockLines.isEmpty()) continue

            val firstLine = blockLines.first().trim()
            if (format == SubtitleFormat.VTT &&
                (firstLine.startsWith("WEBVTT", ignoreCase = true) ||
                    firstLine.startsWith("NOTE", ignoreCase = true) ||
                    firstLine.startsWith("STYLE", ignoreCase = true))
            ) {
                continue
            }

            var timestampLineIdx = -1
            var startTime = ""
            var endTime = ""
            var vttSettings = ""

            for ((idx, l) in blockLines.withIndex()) {
                val match = SRT_VTT_TIMESTAMP_REGEX.matchEntire(l)
                if (match != null) {
                    timestampLineIdx = idx
                    startTime = match.groupValues[1]
                    endTime = match.groupValues[2]
                    vttSettings = match.groupValues[3].trim()
                    break
                }
            }

            if (timestampLineIdx == -1 || timestampLineIdx + 1 >= blockLines.size) continue

            val vttHeader = if (timestampLineIdx > 0) {
                blockLines.subList(0, timestampLineIdx).joinToString("\n").trim()
            } else {
                ""
            }

            val dialogueLines = blockLines.subList(timestampLineIdx + 1, blockLines.size)
            val rawText = dialogueLines.joinToString("\n")
            val cleanText = ANY_ASS_TAG_REGEX.replace(rawText, "")

            cues.add(
                SubtitleCue(
                    id = cueCounter++,
                    rawLineIndex = blockIndex,
                    startTime = startTime,
                    endTime = endTime,
                    rawText = rawText,
                    textWithoutLeadingTags = rawText,
                    cleanText = cleanText,
                    vttCueHeader = vttHeader,
                    vttCueSettings = vttSettings
                )
            )
        }

        return SubtitleDocument(
            fileName = fileName,
            format = format,
            rawContent = content,
            rawLines = blocks,
            cues = cues
        )
    }

    /**
     * F-02: Joins multi-line dialogues (\N, \n, raw newline) into a single line with the selected separator
     * and optionally protects/hides ASS override tags {\...}.
     */
    fun formatCueForExtraction(
        cue: SubtitleCue,
        separatorMode: LineSeparatorMode,
        customSeparator: String,
        protectAssTags: Boolean,
        indexFormatMode: IndexFormatMode
    ): String {
        val baseText = if (protectAssTags) {
            cue.cleanText
        } else {
            cue.textWithoutLeadingTags.ifEmpty { cue.rawText }
        }

        val activeSeparator = when (separatorMode) {
            LineSeparatorMode.CUSTOM -> customSeparator.ifEmpty { " // " }
            else -> separatorMode.separatorToken
        }

        val singleLineText = baseText
            .replace("\\N", activeSeparator)
            .replace("\\n", activeSeparator)
            .replace("\n", activeSeparator)
            .replace(Regex("""\s{2,}"""), " ")
            .trim()

        return indexFormatMode.formatLine(cue.id, singleLineText)
    }

    /**
     * Builds the System Prompt that dynamically adapts its instructions and examples
     * to the selected language model (Reasoning, Compact Few-Shot, or Flagship Contextual),
     * incorporating the exact subtitle formatting rules, separator rules, style guide, and glossary.
     */
    fun buildModelAdaptedSystemPrompt(
        selectedModel: String,
        separatorMode: LineSeparatorMode,
        customSeparator: String,
        indexFormatMode: IndexFormatMode,
        includeGlossaryInPrompt: Boolean,
        targetLanguageOption: TargetLanguageOption,
        customTargetLanguage: String,
        stylePreset: TranslationStylePreset,
        customPromptInstruction: String,
        glossaryRules: List<GlossaryRule>
    ): String {
        val modelProfile = ModelPromptProfile.fromModelId(selectedModel)

        val targetLang = if (targetLanguageOption == TargetLanguageOption.CUSTOM) {
            customTargetLanguage.ifBlank { "Bahasa Indonesia" }
        } else {
            targetLanguageOption.promptName
        }

        val styleGuide = if (stylePreset == TranslationStylePreset.CUSTOM) {
            customPromptInstruction.ifBlank { stylePreset.styleInstruction }
        } else {
            customPromptInstruction.ifBlank { stylePreset.styleInstruction }
        }

        val activeSeparator = when (separatorMode) {
            LineSeparatorMode.CUSTOM -> customSeparator.ifEmpty { " // " }
            else -> separatorMode.separatorToken
        }
        val trimmedSep = activeSeparator.trim()

        val separatorInstruction = if (separatorMode == LineSeparatorMode.SPACE) {
            "- Pertahankan setiap nomor tetap dalam 1 baris lurus (jangan membuat baris baru/enter di tengah kalimat)."
        } else {
            "- PENTING: Pertahankan simbol pemisah baris ganda \"$trimmedSep\" di posisi jeda kalimat yang sesuai pada hasil terjemahan agar struktur 2 baris subtitle tetap utuh."
        }

        val indexPrefixExample = indexFormatMode.example.substringBefore("Halo").trim()

        val activeGlossary = glossaryRules.filter { it.isEnabled && it.sourceTerm.isNotBlank() }
        val glossarySection = if (includeGlossaryInPrompt && activeGlossary.isNotEmpty()) {
            val rulesText = activeGlossary.joinToString("\n") { rule ->
                "  • \"${rule.sourceTerm}\" -> \"${rule.targetTerm}\""
            }
            "\nATURAN GLOSARIUM ISTILAH & KARAKTER (WAJIB DITERAPKAN KONSISTEN):\n$rulesText\n"
        } else {
            ""
        }

        val modelSpecificSection = when (modelProfile) {
            ModelPromptProfile.REASONING_DIRECT -> buildString {
                appendLine("INSTRUKSI KHUSUS MODEL REASONING ($selectedModel):")
                appendLine("- Lakukan proses penalaran secara efisien dan jangan menyertakan penjelasan analisis di dalam hasil akhir.")
                appendLine("- Keluaran akhir (setelah blok pemikiran) WAJIB langsung dimulai dari baris bernomor pertama tanpa kalimat sapaan, tanpa markdown code block (```), dan tanpa catatan kaki.")
            }

            ModelPromptProfile.COMPACT_FEW_SHOT -> {
                val sampleIn1 = indexFormatMode.formatLine(
                    1,
                    if (separatorMode == LineSeparatorMode.SPACE) "Wait here! The signal is blocked." else "Wait here!${activeSeparator}The signal is blocked."
                )
                val sampleOut1 = indexFormatMode.formatLine(
                    1,
                    if (separatorMode == LineSeparatorMode.SPACE) "Tunggu di sini! Sinyalnya terblokir." else "Tunggu di sini!${activeSeparator}Sinyalnya terblokir."
                )
                buildString {
                    appendLine("CONTOH POLA INPUT → OUTPUT WAJIB ($selectedModel):")
                    appendLine("  Input  : $sampleIn1")
                    appendLine("  Output : $sampleOut1")
                    appendLine("- Ikuti pola di atas secara presisi: jumlah baris Output harus 100% sama dengan jumlah baris Input.")
                }
            }

            ModelPromptProfile.FLAGSHIP_CONTEXTUAL -> buildString {
                appendLine("OPTIMASI KONTEKS DIALOG ($selectedModel):")
                appendLine("- Perhatikan kesinambungan percakapan antar-baris berurutan agar kata ganti dan emosi karakter terasa hidup.")
                appendLine("- Jaga kepadatan karakter per baris agar nyaman dibaca sesuai durasi tayang subtitle.")
            }
        }

        return buildString {
            appendLine("Anda adalah mesin penerjemah subtitle profesional (.ASS / .SRT / .VTT) yang dioptimalkan untuk model $selectedModel.")
            appendLine("Tugas Anda: Terjemahkan setiap baris dialog subtitle yang diberikan ke dalam $targetLang.")
            appendLine("GAYA TERJEMAHAN: $styleGuide")
            appendLine()
            appendLine("ATURAN FORMAT WAJIB (JANGAN DILANGGAR):")
            appendLine("- Pertahankan nomor urut di awal setiap baris persis sesuai format aslinya ($indexPrefixExample).")
            appendLine("- Jangan pernah menggabungkan dua nomor berbeda atau melewati nomor mana pun (pemetaan 1:1).")
            appendLine(separatorInstruction)
            appendLine("- Jika terdapat tag kurung kurawal {.*} di dalam teks, biarkan tag tersebut utuh tanpa diterjemahkan.")
            appendLine("- Berikan HANYA daftar baris bernomor hasil terjemahan tanpa pengantar atau penutup.")
            appendLine()
            append(modelSpecificSection)
            if (glossarySection.isNotEmpty()) {
                append(glossarySection)
            }
        }.trim()
    }

    /**
     * F-03: Generates AI Prompt (adapted to the active model) + extracted numbered lines for a batch of cues.
     */
    fun buildAiPromptAndExtractedText(
        cuesSlice: List<SubtitleCue>,
        separatorMode: LineSeparatorMode,
        customSeparator: String,
        protectAssTags: Boolean,
        indexFormatMode: IndexFormatMode,
        includeAiPromptHeader: Boolean,
        includeGlossaryInPrompt: Boolean,
        targetLanguageOption: TargetLanguageOption,
        customTargetLanguage: String,
        stylePreset: TranslationStylePreset,
        customPromptInstruction: String,
        glossaryRules: List<GlossaryRule>,
        selectedModel: String = "deepseek-chat"
    ): String {
        val extractedLines = cuesSlice.joinToString("\n") { cue ->
            formatCueForExtraction(
                cue = cue,
                separatorMode = separatorMode,
                customSeparator = customSeparator,
                protectAssTags = protectAssTags,
                indexFormatMode = indexFormatMode
            )
        }

        if (!includeAiPromptHeader) {
            return extractedLines
        }

        val systemPromptHeader = buildModelAdaptedSystemPrompt(
            selectedModel = selectedModel,
            separatorMode = separatorMode,
            customSeparator = customSeparator,
            indexFormatMode = indexFormatMode,
            includeGlossaryInPrompt = includeGlossaryInPrompt,
            targetLanguageOption = targetLanguageOption,
            customTargetLanguage = customTargetLanguage,
            stylePreset = stylePreset,
            customPromptInstruction = customPromptInstruction,
            glossaryRules = glossaryRules
        )

        return buildString {
            appendLine(systemPromptHeader)
            appendLine()
            appendLine("--- DAFTAR DIALOG (${cuesSlice.firstOrNull()?.id ?: 1} s.d. ${cuesSlice.lastOrNull()?.id ?: cuesSlice.size}) ---")
            append(extractedLines)
        }
    }

    /**
     * F-04: Parses pasted or AI-returned output into a map of `cue.id -> translated single-line text`.
     * Supports:
     * - Stage A: Flexible inline numbering (`[1]`, `**[1]**`, `- [1]:`, `[001]`, `1.`, `**1.**`, `(1)`, `#1`, `Baris 1:`, Markdown tables, JSON lines)
     * - Stage B: Multi-line standalone index blocks (`[1]\nTeks terjemahan`)
     * - Relative index remapping (`1..N` -> `expectedCues[0..N-1].id` for chunks starting at ID > 1)
     * - Stage D: Positional fallback mapping when `allowPositionalFallback = true` and the model omitted line numbers
     */
    fun parseAiTranslatedLines(
        pastedText: String,
        expectedCues: List<SubtitleCue> = emptyList(),
        allowPositionalFallback: Boolean = false
    ): Map<Int, String> {
        return parseAiTranslatedLinesWithReport(
            pastedText = pastedText,
            expectedCues = expectedCues,
            allowPositionalFallback = allowPositionalFallback
        ).parsedLines
    }

    fun parseAiTranslatedLinesWithReport(
        pastedText: String,
        expectedCues: List<SubtitleCue> = emptyList(),
        allowPositionalFallback: Boolean = false
    ): AiLineParseReport {
        val rawParsed = linkedMapOf<Int, String>()
        var usedStandaloneBlockStage = false
        val normalized = pastedText
            .replace("\uFEFF", "")
            .replace("\u200B", "")
            .replace("\u200C", "")
            .replace("\u200D", "")
            .replace("\r\n", "\n")
            .replace("\r", "\n")

        var pendingStandaloneCueId: Int? = null

        for (rawLine in normalized.lines()) {
            val trimmed = rawLine.trim()
            if (trimmed.isEmpty()) continue

            // Ignore markdown code fences or table divider rows like |---|---|
            if (trimmed.startsWith("```") || trimmed.matches(Regex("""^\|?\s*:?-{2,}:?\s*(?:\|\s*:?-{2,}:?\s*)+\|?$"""))) {
                continue
            }

            // 1. Check Stage A2: Markdown table row | 1 | Text |
            val tableMatch = AI_MARKDOWN_TABLE_ROW_REGEX.matchEntire(trimmed)
            if (tableMatch != null) {
                val idStr = tableMatch.groupValues[1].ifEmpty { tableMatch.groupValues[2] }
                val cueId = idStr.toIntOrNull()
                val body = cleanExtractedBodyText(tableMatch.groupValues[3])
                if (cueId != null && body.isNotEmpty() && !body.equals("Text", ignoreCase = true) && !body.equals("Terjemahan", ignoreCase = true)) {
                    rawParsed[cueId] = body
                    pendingStandaloneCueId = null
                    continue
                }
            }

            // 2. Check Stage A3: JSON object or JSON key-value line
            val jsonObjMatch = AI_JSON_OBJ_LINE_REGEX.matchEntire(trimmed)
            if (jsonObjMatch != null) {
                val cueId = jsonObjMatch.groupValues[1].toIntOrNull()
                val body = cleanExtractedBodyText(jsonObjMatch.groupValues[2])
                if (cueId != null && body.isNotEmpty()) {
                    rawParsed[cueId] = body
                    pendingStandaloneCueId = null
                    continue
                }
            }
            val jsonKvMatch = AI_JSON_KV_LINE_REGEX.matchEntire(trimmed)
            if (jsonKvMatch != null) {
                val cueId = jsonKvMatch.groupValues[1].toIntOrNull()
                val body = cleanExtractedBodyText(jsonKvMatch.groupValues[2])
                if (cueId != null && body.isNotEmpty()) {
                    rawParsed[cueId] = body
                    pendingStandaloneCueId = null
                    continue
                }
            }

            // Normalize leading markdown bullets (-, *, •, >) and bold/italic/backtick wrappers around index prefix
            val unwrappedLine = stripLeadingMarkdownIndexWrappers(trimmed)

            // 3. Check Stage A1: Flexible inline numbered line ([1] Text, **[1]**: Text, 1. Text, #1 Text, etc.)
            val inlineMatch = AI_FLEXIBLE_INDEX_LINE_REGEX.matchEntire(unwrappedLine)
            if (inlineMatch != null) {
                val idStr = inlineMatch.groupValues.drop(1).dropLast(1).firstOrNull { it.isNotEmpty() }.orEmpty()
                val cueId = idStr.toIntOrNull()
                val translatedBody = cleanExtractedBodyText(inlineMatch.groupValues.last())
                if (cueId != null && translatedBody.isNotEmpty()) {
                    rawParsed[cueId] = translatedBody
                    pendingStandaloneCueId = null
                    continue
                }
            }

            // 4. Check Stage B: Standalone index line where the translated text is on the following line(s)
            val standaloneMatch = AI_STANDALONE_INDEX_LINE_REGEX.matchEntire(unwrappedLine)
            if (standaloneMatch != null) {
                val idStr = standaloneMatch.groupValues.drop(1).firstOrNull { it.isNotEmpty() }.orEmpty()
                val cueId = idStr.toIntOrNull()
                if (cueId != null) {
                    pendingStandaloneCueId = cueId
                    continue
                }
            }

            // If we have a pending standalone index from the previous line, attach this line as its body
            val activePendingId = pendingStandaloneCueId
            if (activePendingId != null) {
                val cleanBody = cleanExtractedBodyText(unwrappedLine)
                if (cleanBody.isNotEmpty() && !AI_CONVERSATIONAL_PREAMBLE_REGEX.matches(cleanBody)) {
                    val existing = rawParsed[activePendingId]
                    rawParsed[activePendingId] = if (existing.isNullOrEmpty()) {
                        cleanBody
                    } else {
                        "$existing // $cleanBody"
                    }
                    usedStandaloneBlockStage = true
                    continue
                }
            }
        }

        // If Stage A or Stage B produced indexed lines, remap relative 1..N indices if needed
        if (rawParsed.isNotEmpty()) {
            val remapped = remapRelativeIndicesIfNeeded(rawParsed, expectedCues)
            val strategyLabel = if (usedStandaloneBlockStage) {
                "Parser Blok Nomor Multi-Baris"
            } else {
                "Parser Nomor Fleksibel"
            }
            return AiLineParseReport(
                parsedLines = remapped,
                strategyUsed = strategyLabel
            )
        }

        // 5. Stage D: Positional Fallback Mapping when AI omitted line numbers completely
        if (allowPositionalFallback && expectedCues.isNotEmpty()) {
            val candidateLines = extractCleanUnnumberedCandidateLines(normalized)
            val minAcceptableLines = if (expectedCues.size <= 2) {
                1
            } else {
                maxOf(2, expectedCues.size / 3)
            }
            if (candidateLines.size >= minAcceptableLines) {
                val positionalMap = linkedMapOf<Int, String>()
                val limit = minOf(expectedCues.size, candidateLines.size)
                for (i in 0 until limit) {
                    val body = cleanExtractedBodyText(candidateLines[i])
                    if (body.isNotEmpty()) {
                        positionalMap[expectedCues[i].id] = body
                    }
                }
                if (positionalMap.isNotEmpty()) {
                    return AiLineParseReport(
                        parsedLines = positionalMap,
                        strategyUsed = "Pemetaan Baris Otomatis (Positional Fallback)"
                    )
                }
            }
        }

        return AiLineParseReport(
            parsedLines = emptyMap(),
            strategyUsed = "Tidak Terpetakan"
        )
    }

    private fun stripLeadingMarkdownIndexWrappers(trimmedLine: String): String {
        // Strip leading markdown bullets or blockquotes: "- **[1]** Teks" -> "**[1]** Teks"
        var working = trimmedLine.replaceFirst(Regex("""^(?:[-*•>+]+\s+)+"""), "").trim()
        // Unwrap bold/italic/code markers around the leading number token: "**[1]**: Teks" -> "[1]: Teks"
        working = working.replaceFirst(
            Regex("""^[*_`~]+(\[\s*#?\s*\d+\s*\]\s*[:.\-|]*|\(\s*#?\s*\d+\s*\)\s*[:.\-|]*|#\s*\d+\s*[:.\-|]*|\d+\s*[.|:\-)]?)[*_`~]+\s*[:.\-|]*\s*"""),
            "$1 "
        )
        // Also strip leading bold/code markers if they only precede the bracket e.g. "**[1] Teks**"
        if (working.startsWith("**") && working.endsWith("**") && working.length > 4) {
            working = working.substring(2, working.length - 2).trim()
        }
        working = working.replaceFirst(Regex("""^[*_`~]+(?=\[\s*#?\s*\d+\s*\]|\(\s*#?\s*\d+\s*\)|#\s*\d+|\d+\s*[.|:\-)])"""), "")
        return working.trim()
    }

    private fun cleanExtractedBodyText(rawBody: String): String {
        var body = rawBody.trim()
        // Strip leftover leading markdown separator after index e.g. "** - Teks" or ": Teks"
        body = body.replaceFirst(Regex("""^[*_`~]+\s*[:.\-|]*\s*"""), "").trim()
        body = body.replaceFirst(Regex("""^[:\-|]+\s+"""), "").trim()
        // Strip outer wrapping quotes if the entire line was quoted by the LLM (and wasn't originally quoted)
        if ((body.startsWith("\"") && body.endsWith("\"")) || (body.startsWith("“") && body.endsWith("”"))) {
            if (body.length > 2 && body.count { it == '"' } == 2) {
                body = body.substring(1, body.length - 1).trim()
            }
        }
        return body
    }

    private fun remapRelativeIndicesIfNeeded(
        rawParsed: Map<Int, String>,
        expectedCues: List<SubtitleCue>
    ): Map<Int, String> {
        if (rawParsed.isEmpty() || expectedCues.isEmpty()) return rawParsed

        val expectedIds = expectedCues.map { it.id }.toSet()
        val directMatchCount = rawParsed.keys.count { it in expectedIds }

        // If the chunk starts at an ID > 1 (e.g. 101..200) and the model output 1..N with 0 direct matches,
        // remap 1..N directly to expectedCues[0..N-1].id so multi-chunk translations never lose chunk #2+.
        if (directMatchCount == 0 && expectedCues.first().id != 1 && (rawParsed.containsKey(1) || rawParsed.containsKey(0))) {
            val isZeroBased = !rawParsed.containsKey(1) && rawParsed.containsKey(0)
            val remapped = linkedMapOf<Int, String>()
            for ((relativeIdx, text) in rawParsed.entries) {
                val listIdx = if (isZeroBased) relativeIdx else relativeIdx - 1
                val targetCue = expectedCues.getOrNull(listIdx) ?: continue
                remapped[targetCue.id] = text
            }
            if (remapped.isNotEmpty()) {
                return remapped
            }
        }

        // Also handle 0-based indexing [0]..[N-1] when expectedCues starts at 1
        if (!rawParsed.containsKey(expectedCues.last().id) && rawParsed.containsKey(0) && expectedCues.first().id == 1) {
            val shifted = linkedMapOf<Int, String>()
            for ((zeroIdx, text) in rawParsed.entries) {
                val targetCue = expectedCues.getOrNull(zeroIdx) ?: continue
                shifted[targetCue.id] = text
            }
            if (shifted.size >= rawParsed.size) {
                return shifted
            }
        }

        return rawParsed
    }

    private fun extractCleanUnnumberedCandidateLines(normalizedText: String): List<String> {
        val candidates = mutableListOf<String>()
        for (rawLine in normalizedText.lines()) {
            val trimmed = rawLine.trim()
            if (trimmed.isEmpty()) continue
            if (trimmed.startsWith("```") || trimmed.startsWith("---") || trimmed.startsWith("***") || trimmed.startsWith("===")) {
                continue
            }
            if (trimmed.startsWith("#")) continue
            val withoutBullet = trimmed.replaceFirst(Regex("""^(?:[-*•>+]+\s+)+"""), "").trim()
            if (withoutBullet.isEmpty()) continue
            if (AI_CONVERSATIONAL_PREAMBLE_REGEX.matches(withoutBullet)) continue
            candidates.add(withoutBullet)
        }
        return candidates
    }

    /**
     * Reconstructs the pasted text buffer from a map of cueId -> translatedText.
     */
    fun formatTranslationsMapToEditorText(
        translations: Map<Int, String>,
        indexFormatMode: IndexFormatMode
    ): String {
        return translations.entries
            .sortedBy { it.key }
            .joinToString("\n") { (id, text) ->
                indexFormatMode.formatLine(id, text)
            }
    }

    /**
     * F-05: Computes live Find & Replace matches with support for Case Sensitive, Whole Word, Regex,
     * and .ASS Style/Actor filtering.
     */
    fun findMatchesInTranslations(
        document: SubtitleDocument,
        translations: Map<Int, String>,
        findQuery: String,
        replaceWith: String,
        caseSensitive: Boolean,
        wholeWord: Boolean,
        useRegex: Boolean,
        selectedStyleFilter: String?,
        selectedActorFilter: String?
    ): List<FindMatchItem> {
        if (findQuery.isEmpty() || translations.isEmpty()) return emptyList()

        val regex = buildSearchRegex(
            query = findQuery,
            caseSensitive = caseSensitive,
            wholeWord = wholeWord,
            useRegex = useRegex
        ) ?: return emptyList()

        val cueById = document.cues.associateBy { it.id }
        val results = mutableListOf<FindMatchItem>()

        for ((cueId, currentText) in translations.entries.sortedBy { it.key }) {
            val cue = cueById[cueId] ?: continue

            if (!selectedStyleFilter.isNullOrBlank() && selectedStyleFilter != "ALL") {
                if (!cue.style.equals(selectedStyleFilter, ignoreCase = true)) continue
            }
            if (!selectedActorFilter.isNullOrBlank() && selectedActorFilter != "ALL") {
                if (!cue.actor.equals(selectedActorFilter, ignoreCase = true)) continue
            }

            val matches = regex.findAll(currentText).toList()
            if (matches.isEmpty()) continue

            val ranges = matches.map { it.range }
            val safeReplacement = if (useRegex) replaceWith else Regex.escapeReplacement(replaceWith)
            val previewReplaced = runCatching {
                regex.replace(currentText, safeReplacement)
            }.getOrDefault(currentText)

            results.add(
                FindMatchItem(
                    cueId = cueId,
                    style = cue.style,
                    actor = cue.actor,
                    currentTranslatedText = currentText,
                    previewReplacedText = previewReplaced,
                    matchRanges = ranges
                )
            )
        }
        return results
    }

    fun applyGlossaryRulesToTranslations(
        translations: Map<Int, String>,
        rules: List<GlossaryRule>
    ): Pair<Map<Int, String>, Int> {
        val activeRules = rules.filter { it.isEnabled && it.sourceTerm.isNotBlank() }
        if (activeRules.isEmpty() || translations.isEmpty()) return translations to 0

        val updated = LinkedHashMap(translations)
        var totalReplacements = 0

        for ((cueId, originalLine) in updated.entries) {
            var workingLine = originalLine
            for (rule in activeRules) {
                val regex = buildSearchRegex(
                    query = rule.sourceTerm,
                    caseSensitive = rule.isCaseSensitive,
                    wholeWord = rule.wholeWord,
                    useRegex = false
                ) ?: continue
                val count = regex.findAll(workingLine).count()
                if (count > 0) {
                    totalReplacements += count
                    workingLine = regex.replace(workingLine, Regex.escapeReplacement(rule.targetTerm))
                }
            }
            updated[cueId] = workingLine
        }

        return updated to totalReplacements
    }

    fun buildSearchRegex(
        query: String,
        caseSensitive: Boolean,
        wholeWord: Boolean,
        useRegex: Boolean
    ): Regex? {
        if (query.isEmpty()) return null
        return try {
            val basePattern = if (useRegex) query else Pattern.quote(query)
            val fullPattern = if (wholeWord) """\b(?:$basePattern)\b""" else basePattern
            val options = if (caseSensitive) emptySet() else setOf(RegexOption.IGNORE_CASE)
            Regex(fullPattern, options)
        } catch (_: Exception) {
            null
        }
    }

    /**
     * F-04: Reconstructs the merged line for a single SubtitleCue, restoring multi-line breaks (\N or \n),
     * leading ASS override tags ({\an8}, {\pos}), and optional Bilingual secondary line styling.
     */
    fun buildMergedCueText(
        cue: SubtitleCue,
        format: SubtitleFormat,
        translatedSingleLine: String?,
        separatorMode: LineSeparatorMode,
        customSeparator: String,
        restoreLeadingAssTags: Boolean,
        bilingualMode: Boolean,
        bilingualOrder: BilingualOrder,
        bilingualAssSecondaryStyling: Boolean
    ): String {
        val lineBreakTarget = if (format.isAssFamily) "\\N" else "\n"

        if (translatedSingleLine.isNullOrBlank()) {
            return cue.rawText
        }

        val restoredTranslatedBody = restoreMultiLineBreaks(
            singleLine = translatedSingleLine,
            separatorMode = separatorMode,
            customSeparator = customSeparator,
            targetLineBreak = lineBreakTarget
        )

        val prefixTags = if (format.isAssFamily && restoreLeadingAssTags) {
            cue.leadingAssTags
        } else {
            ""
        }

        if (!bilingualMode) {
            return "$prefixTags$restoredTranslatedBody"
        }

        val originalCleanLine = cue.cleanText.trim()
        val formattedOriginalForBilingual = if (format.isAssFamily && bilingualAssSecondaryStyling) {
            "{\\r\\fs34\\1c&HD0DCE5&}$originalCleanLine"
        } else {
            originalCleanLine
        }

        return when (bilingualOrder) {
            BilingualOrder.TRANSLATION_TOP ->
                "$prefixTags$restoredTranslatedBody$lineBreakTarget$formattedOriginalForBilingual"
            BilingualOrder.ORIGINAL_TOP ->
                "$prefixTags$formattedOriginalForBilingual$lineBreakTarget$restoredTranslatedBody"
        }
    }

    private fun restoreMultiLineBreaks(
        singleLine: String,
        separatorMode: LineSeparatorMode,
        customSeparator: String,
        targetLineBreak: String
    ): String {
        if (separatorMode == LineSeparatorMode.SPACE) {
            return singleLine.trim()
        }
        val escapedReplacement = Regex.escapeReplacement(targetLineBreak)
        var result = singleLine
        if (separatorMode == LineSeparatorMode.CUSTOM && customSeparator.trim().isNotEmpty()) {
            val escapedCustom = Pattern.quote(customSeparator.trim())
            result = result.replace(Regex("""\s*$escapedCustom\s*"""), escapedReplacement)
        }
        result = result
            .replace(Regex("""\s*//\s*"""), escapedReplacement)
            .replace(Regex("""\s*\|\s*"""), escapedReplacement)
        return result.trim()
    }

    fun generateMergedFileContent(
        document: SubtitleDocument,
        translations: Map<Int, String>,
        separatorMode: LineSeparatorMode,
        customSeparator: String,
        restoreLeadingAssTags: Boolean,
        bilingualMode: Boolean,
        bilingualOrder: BilingualOrder,
        bilingualAssSecondaryStyling: Boolean
    ): String {
        return when (document.format) {
            SubtitleFormat.ASS, SubtitleFormat.SSA -> {
                val mutableLines = document.rawLines.toMutableList()
                for (cue in document.cues) {
                    val mergedText = buildMergedCueText(
                        cue = cue,
                        format = document.format,
                        translatedSingleLine = translations[cue.id],
                        separatorMode = separatorMode,
                        customSeparator = customSeparator,
                        restoreLeadingAssTags = restoreLeadingAssTags,
                        bilingualMode = bilingualMode,
                        bilingualOrder = bilingualOrder,
                        bilingualAssSecondaryStyling = bilingualAssSecondaryStyling
                    )
                    if (cue.rawLineIndex in mutableLines.indices) {
                        mutableLines[cue.rawLineIndex] = "${cue.assPrefix}$mergedText"
                    }
                }
                mutableLines.joinToString("\n")
            }

            SubtitleFormat.SRT -> {
                document.cues.joinToString("\n\n") { cue ->
                    val mergedText = buildMergedCueText(
                        cue = cue,
                        format = SubtitleFormat.SRT,
                        translatedSingleLine = translations[cue.id],
                        separatorMode = separatorMode,
                        customSeparator = customSeparator,
                        restoreLeadingAssTags = false,
                        bilingualMode = bilingualMode,
                        bilingualOrder = bilingualOrder,
                        bilingualAssSecondaryStyling = false
                    )
                    "${cue.id}\n${cue.startTime} --> ${cue.endTime}\n$mergedText"
                } + "\n"
            }

            SubtitleFormat.VTT -> {
                val body = document.cues.joinToString("\n\n") { cue ->
                    val mergedText = buildMergedCueText(
                        cue = cue,
                        format = SubtitleFormat.VTT,
                        translatedSingleLine = translations[cue.id],
                        separatorMode = separatorMode,
                        customSeparator = customSeparator,
                        restoreLeadingAssTags = false,
                        bilingualMode = bilingualMode,
                        bilingualOrder = bilingualOrder,
                        bilingualAssSecondaryStyling = false
                    )
                    val settingsSuffix = if (cue.vttCueSettings.isNotEmpty()) " ${cue.vttCueSettings}" else ""
                    val headerPrefix = if (cue.vttCueHeader.isNotEmpty()) "${cue.vttCueHeader}\n" else ""
                    "$headerPrefix${cue.startTime} --> ${cue.endTime}$settingsSuffix\n$mergedText"
                }
                "WEBVTT\n\n$body\n"
            }
        }
    }

    fun buildMergeCompareItems(
        document: SubtitleDocument,
        translations: Map<Int, String>,
        separatorMode: LineSeparatorMode,
        customSeparator: String,
        restoreLeadingAssTags: Boolean,
        bilingualMode: Boolean,
        bilingualOrder: BilingualOrder,
        bilingualAssSecondaryStyling: Boolean
    ): List<MergeCompareItem> {
        return document.cues.map { cue ->
            val rawTranslated = translations[cue.id] ?: ""
            val finalMerged = buildMergedCueText(
                cue = cue,
                format = document.format,
                translatedSingleLine = rawTranslated.takeIf { it.isNotBlank() },
                separatorMode = separatorMode,
                customSeparator = customSeparator,
                restoreLeadingAssTags = restoreLeadingAssTags,
                bilingualMode = bilingualMode,
                bilingualOrder = bilingualOrder,
                bilingualAssSecondaryStyling = bilingualAssSecondaryStyling
            )
            MergeCompareItem(
                cue = cue,
                isTranslated = rawTranslated.isNotBlank(),
                finalMergedLineText = finalMerged,
                rawTranslatedLine = rawTranslated
            )
        }
    }
}
