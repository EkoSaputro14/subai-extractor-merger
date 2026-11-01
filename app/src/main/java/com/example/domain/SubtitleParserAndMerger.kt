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

object SubtitleParserAndMerger {

    // Matches [1] Text, 1. Text, 1 | Text, 1: Text, 1 - Text, 1) Text
    private val AI_INDEX_LINE_REGEX =
        Regex("""^\s*(?:\[\s*(\d+)\s*\]|(\d+)\s*(?:\.|\||:|-|\)))\s*(.+)$""")

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
     * F-04: Parses pasted AI output into a map of cue.id -> translated single-line text.
     */
    fun parseAiTranslatedLines(pastedText: String): Map<Int, String> {
        val result = linkedMapOf<Int, String>()
        val normalized = pastedText.replace("\r\n", "\n").replace("\r", "\n")
        for (rawLine in normalized.lines()) {
            val trimmed = rawLine.trim()
            if (trimmed.isEmpty()) continue
            val match = AI_INDEX_LINE_REGEX.matchEntire(trimmed) ?: continue
            val idStr = match.groupValues[1].ifEmpty { match.groupValues[2] }
            val cueId = idStr.toIntOrNull() ?: continue
            val translatedBody = match.groupValues[3].trim()
            if (translatedBody.isNotEmpty()) {
                result[cueId] = translatedBody
            }
        }
        return result
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
