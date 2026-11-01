package com.example.domain

import com.example.model.BatchValidationResult
import com.example.model.SubtitleDocument
import com.example.model.SubtitleFormat

/**
 * Validates structural and translation completeness of a subtitle file before it is allowed
 * to be saved as final output and marked as COMPLETED.
 *
 * Validation Policy:
 * - Validation Passed -> Save Final Output -> Mark Completed
 * - Validation Failed -> Reject Attempt -> Requeue at End
 */
object BatchTranslationValidator {

    private val SRT_TIMESTAMP_REGEX = Regex(
        """^\d{2}:\d{2}:\d{2},\d{3}$"""
    )

    private val VTT_TIMESTAMP_REGEX = Regex(
        """^(\d{2}:)?\d{2}:\d{2}\.\d{3}$"""
    )

    private val ASS_TIMESTAMP_REGEX = Regex(
        """^\d+:\d{2}:\d{2}\.\d{2,3}$"""
    )

    private val LEADING_ASS_TAG_BLOCK_REGEX = Regex("""^(\{\\[^}]*\})+""")
    private val NON_TRANSLATABLE_OR_SHORT_PROPER_NOUN_REGEX = Regex(
        """^[\d\s\p{Punct}♪♫…—–\-+*/=<>@#$%^&()\[\]{}|\\:;'".,?!~`]+$"""
    )

    /**
     * Validates both the in-memory translated cue map and the reconstructed merged file content
     * against the original source [SubtitleDocument].
     *
     * Checks performed:
     * 1. Are all source subtitle entries represented?
     * 2. Are any entries missing?
     * 3. Are there empty blocks or truncated translations?
     * 4. Are there texts that should have been translated but remained identical to the source language
     *    (while tolerating short proper nouns, numbers, punctuation, or non-translatable tokens)?
     * 5. Are all timestamps valid and preserved?
     * 6. Is the subtitle sequence order intact?
     * 7. Is the output file structure valid when re-parsed (including headers and ASS styling tags)?
     * 8. Does the entry count match the source document?
     */
    fun validateSubtitleTranslation(
        sourceDocument: SubtitleDocument,
        translatedMap: Map<Int, String>,
        mergedFileContent: String,
        sourceLanguage: String = "Auto",
        targetLanguage: String = "Indonesian (Bahasa Indonesia)",
        checkUntranslatedSourceLeakage: Boolean = true
    ): BatchValidationResult {
        val totalSourceCues = sourceDocument.cues.size
        if (totalSourceCues == 0) {
            return BatchValidationResult(
                isValid = false,
                totalSourceCues = 0,
                translatedCueCount = 0,
                structureErrors = listOf("File sumber tidak memiliki baris dialog subtitle yang valid."),
                summaryMessage = "Validation Failed: File sumber kosong atau tidak memiliki entri subtitle."
            )
        }

        if (mergedFileContent.isBlank()) {
            return BatchValidationResult(
                isValid = false,
                totalSourceCues = totalSourceCues,
                translatedCueCount = 0,
                missingCueIds = sourceDocument.cues.map { it.id },
                structureErrors = listOf("Konten file hasil merge kosong."),
                summaryMessage = "Validation Failed: Konten hasil terjemahan kosong."
            )
        }

        val missingCueIds = mutableListOf<Int>()
        val emptyOrTruncatedCueIds = mutableListOf<Int>()
        val untranslatedCueIds = mutableListOf<Int>()
        val invalidTimestampCueIds = mutableListOf<Int>()
        val outOfOrderCueIds = mutableListOf<Int>()
        val structureErrors = mutableListOf<String>()

        val sameLanguageRequested = sourceLanguage.isNotBlank() &&
            !sourceLanguage.equals("Auto", ignoreCase = true) &&
            targetLanguage.contains(sourceLanguage.trim(), ignoreCase = true)

        // 1. Inspect every cue in sourceDocument against translatedMap
        for (sourceCue in sourceDocument.cues) {
            val cueId = sourceCue.id
            val rawTranslated = translatedMap[cueId]
            if (rawTranslated == null) {
                missingCueIds.add(cueId)
                continue
            }

            val cleanedTranslation = rawTranslated
                .replace(LEADING_ASS_TAG_BLOCK_REGEX, "")
                .replace("\\N", " ")
                .replace("\\n", " ")
                .trim()

            // Check for empty or obviously truncated text
            if (cleanedTranslation.isEmpty()) {
                emptyOrTruncatedCueIds.add(cueId)
                continue
            }

            if (isTruncatedTranslation(sourceCue.cleanText, cleanedTranslation)) {
                emptyOrTruncatedCueIds.add(cueId)
                continue
            }

            // Check for untranslated source-language leakage (excluding short proper nouns/numbers/symbols)
            if (checkUntranslatedSourceLeakage && !sameLanguageRequested) {
                if (isLikelyUntranslatedSourceText(sourceCue.cleanText, cleanedTranslation)) {
                    untranslatedCueIds.add(cueId)
                }
            }
        }

        // 2. Structural & Format header checks on mergedFileContent
        when (sourceDocument.format) {
            SubtitleFormat.VTT -> {
                if (!mergedFileContent.trimStart().startsWith("WEBVTT")) {
                    structureErrors.add("Header WEBVTT hilang pada hasil output .vtt.")
                }
            }
            SubtitleFormat.ASS, SubtitleFormat.SSA -> {
                if (!mergedFileContent.contains("[Events]", ignoreCase = true)) {
                    structureErrors.add("Bagian [Events] hilang pada hasil output .ass/.ssa.")
                }
                val sourceHasScriptInfo = sourceDocument.rawLines.any {
                    it.trim().equals("[Script Info]", ignoreCase = true)
                }
                if (sourceHasScriptInfo && !mergedFileContent.contains("[Script Info]", ignoreCase = true)) {
                    structureErrors.add("Header [Script Info] hilang pada hasil output .ass/.ssa.")
                }
            }
            SubtitleFormat.SRT -> {
                // Standard block structure verified by re-parsing below
            }
        }

        // 3. Re-parse the generated merged file content to verify structural integrity,
        // cue count, sequence order, timestamps, and leading ASS override tags.
        val reparsedDoc = SubtitleParserAndMerger.parseSubtitle(
            fileName = sourceDocument.fileName,
            rawContent = mergedFileContent
        )

        if (reparsedDoc.cues.size != totalSourceCues) {
            structureErrors.add(
                "Jumlah entri hasil (${reparsedDoc.cues.size}) tidak sesuai dengan sumber ($totalSourceCues)."
            )
            val reparsedIds = reparsedDoc.cues.map { it.id }.toSet()
            for (sourceCue in sourceDocument.cues) {
                if (sourceCue.id !in reparsedIds && sourceCue.id !in missingCueIds) {
                    missingCueIds.add(sourceCue.id)
                }
            }
        }

        // Compare cue-by-cue sequence, timestamps, and preserved ASS tags
        val compareCount = minOf(sourceDocument.cues.size, reparsedDoc.cues.size)
        var previousCueId = 0
        for (idx in 0 until compareCount) {
            val srcCue = sourceDocument.cues[idx]
            val outCue = reparsedDoc.cues[idx]

            // Sequence order check
            if (outCue.id != srcCue.id || outCue.id <= previousCueId) {
                outOfOrderCueIds.add(srcCue.id)
            }
            previousCueId = outCue.id

            // Timestamp preservation & format check
            if (!isValidAndMatchingTimestamp(sourceDocument.format, srcCue.startTime, srcCue.endTime, outCue.startTime, outCue.endTime)) {
                invalidTimestampCueIds.add(srcCue.id)
            }

            // ASS leading override tag check (when source has leadingAssTags and merged output lost them)
            if ((sourceDocument.format == SubtitleFormat.ASS || sourceDocument.format == SubtitleFormat.SSA) &&
                srcCue.leadingAssTags.isNotEmpty()
            ) {
                if (!outCue.rawText.startsWith(srcCue.leadingAssTags)) {
                    structureErrors.add("Tag format ASS '${srcCue.leadingAssTags}' hilang pada entri #${srcCue.id}.")
                }
            }

            // Verify re-parsed cleanText is not blank
            if (outCue.cleanText.isBlank() && srcCue.id !in emptyOrTruncatedCueIds && srcCue.id !in missingCueIds) {
                emptyOrTruncatedCueIds.add(srcCue.id)
            }
        }

        val distinctMissing = missingCueIds.distinct()
        val distinctEmptyOrTruncated = emptyOrTruncatedCueIds.distinct()
        val distinctUntranslated = untranslatedCueIds.distinct()
        val distinctInvalidTimestamps = invalidTimestampCueIds.distinct()
        val distinctOutOfOrder = outOfOrderCueIds.distinct()

        val validTranslatedCount = (totalSourceCues -
            (distinctMissing + distinctEmptyOrTruncated + distinctUntranslated + distinctInvalidTimestamps + distinctOutOfOrder).distinct().size
            ).coerceAtLeast(0)

        val isValid = distinctMissing.isEmpty() &&
            distinctEmptyOrTruncated.isEmpty() &&
            distinctUntranslated.isEmpty() &&
            distinctInvalidTimestamps.isEmpty() &&
            distinctOutOfOrder.isEmpty() &&
            structureErrors.isEmpty() &&
            reparsedDoc.cues.size == totalSourceCues

        val summaryMessage = if (isValid) {
            "Validation Passed: Seluruh $totalSourceCues/$totalSourceCues entri lengkap, timestamp valid, dan struktur ${sourceDocument.format.name} utuh."
        } else {
            buildList {
                if (distinctMissing.isNotEmpty()) {
                    add("Entri hilang: ${distinctMissing.size} baris (${formatIdPreview(distinctMissing)})")
                }
                if (distinctEmptyOrTruncated.isNotEmpty()) {
                    add("Teks kosong/terpotong: ${distinctEmptyOrTruncated.size} baris (${formatIdPreview(distinctEmptyOrTruncated)})")
                }
                if (distinctUntranslated.isNotEmpty()) {
                    add("Belum diterjemahkan: ${distinctUntranslated.size} baris (${formatIdPreview(distinctUntranslated)})")
                }
                if (distinctInvalidTimestamps.isNotEmpty()) {
                    add("Timestamp rusak/berubah: ${distinctInvalidTimestamps.size} baris (${formatIdPreview(distinctInvalidTimestamps)})")
                }
                if (distinctOutOfOrder.isNotEmpty()) {
                    add("Urutan entri tidak valid: ${distinctOutOfOrder.size} baris")
                }
                if (structureErrors.isNotEmpty()) {
                    addAll(structureErrors)
                }
            }.joinToString(" | ").let { "Validation Failed: $it" }
        }

        return BatchValidationResult(
            isValid = isValid,
            totalSourceCues = totalSourceCues,
            translatedCueCount = validTranslatedCount,
            missingCueIds = distinctMissing,
            emptyOrTruncatedCueIds = distinctEmptyOrTruncated,
            untranslatedCueIds = distinctUntranslated,
            invalidTimestampCueIds = distinctInvalidTimestamps,
            outOfOrderCueIds = distinctOutOfOrder,
            structureErrors = structureErrors,
            summaryMessage = summaryMessage
        )
    }

    /**
     * Checks if a translation is suspiciously truncated (e.g., ends with an unclosed bracket/tag,
     * an explicit truncation marker like "[TRUNCATED]", or is a 1-character cut-off of a long sentence).
     */
    private fun isTruncatedTranslation(sourceText: String, translatedText: String): Boolean {
        val trimmed = translatedText.trim()
        if (trimmed.isEmpty()) return true
        if (trimmed.contains("[TRUNCATED]", ignoreCase = true) ||
            trimmed.contains("<cut>", ignoreCase = true)
        ) {
            return true
        }
        // Unclosed ASS override brace
        val openBraces = trimmed.count { it == '{' }
        val closeBraces = trimmed.count { it == '}' }
        if (openBraces != closeBraces) {
            return true
        }
        // Source is a long multi-word sentence (>= 35 chars) but translation is just 1 char (and not punctuation/interjection)
        val cleanSource = sourceText.replace("\\N", " ").trim()
        if (cleanSource.length >= 35 && trimmed.length == 1 && trimmed[0].isLetterOrDigit()) {
            return true
        }
        return false
    }

    /**
     * Detects whether a subtitle cue was left untranslated in the source language, while
     * allowing proper nouns, short interjections ("OK", "Ah!", "Hmm"), numbers, and symbols.
     */
    internal fun isLikelyUntranslatedSourceText(sourceText: String, translatedText: String): Boolean {
        val normSource = normalizeForComparison(sourceText)
        val normTrans = normalizeForComparison(translatedText)

        if (normSource.isEmpty() || normTrans.isEmpty()) return false
        if (normSource != normTrans) return false

        // Pure numbers, punctuation, music notes, or symbols do not need translation
        if (NON_TRANSLATABLE_OR_SHORT_PROPER_NOUN_REGEX.matches(normSource)) {
            return false
        }

        val words = normSource.split(Regex("""\s+""")).filter { it.isNotBlank() }
        // Single short word or universal interjection (e.g., "OK", "Okay", "Hmm", "Ah", "Oh", "Ha ha", "Naomi", "Tokyo")
        val universalInterjections = setOf(
            "ok", "okay", "hmm", "hm", "ah", "oh", "eh", "uh", "huh", "wow", "hey", "hi", "halo", "haha", "hahaha"
        )
        if (words.all { it.lowercase().trimEnd('.', '!', '?', ',') in universalInterjections }) {
            return false
        }

        // Allow 1-2 capitalized proper nouns (e.g. "Tanaka-san!", "John Smith", "New York")
        if (words.size <= 2 && words.all { word ->
                val stripped = word.trim { !it.isLetterOrDigit() }
                stripped.isEmpty() || stripped[0].isUpperCase() || stripped.length <= 3
            }
        ) {
            return false
        }

        // If the sentence has >= 3 words (or >= 12 alphabetic characters with non-capitalized words)
        // and is 100% identical to the source text, it was left untranslated.
        val alphaCount = normSource.count { it.isLetter() }
        val nonAsciiLetterCount = normSource.count { it.isLetter() && it.code > 127 }
        if (nonAsciiLetterCount >= 2) {
            // Non-ASCII scripts (Japanese, Korean, Chinese, Cyrillic, Arabic, etc.) left identical
            return true
        }
        return words.size >= 3 && alphaCount >= 10
    }

    private fun normalizeForComparison(text: String): String {
        return text
            .replace(LEADING_ASS_TAG_BLOCK_REGEX, "")
            .replace("\\N", " ")
            .replace("\\n", " ")
            .replace(Regex("""\s+"""), " ")
            .trim()
    }

    private fun isValidAndMatchingTimestamp(
        format: SubtitleFormat,
        srcStart: String,
        srcEnd: String,
        outStart: String,
        outEnd: String
    ): Boolean {
        if (srcStart.isBlank() || srcEnd.isBlank() || outStart.isBlank() || outEnd.isBlank()) {
            return false
        }
        if (srcStart.trim() != outStart.trim() || srcEnd.trim() != outEnd.trim()) {
            return false
        }
        return when (format) {
            SubtitleFormat.SRT ->
                SRT_TIMESTAMP_REGEX.matches(outStart.trim()) && SRT_TIMESTAMP_REGEX.matches(outEnd.trim())
            SubtitleFormat.VTT ->
                VTT_TIMESTAMP_REGEX.matches(outStart.trim()) && VTT_TIMESTAMP_REGEX.matches(outEnd.trim())
            SubtitleFormat.ASS, SubtitleFormat.SSA ->
                ASS_TIMESTAMP_REGEX.matches(outStart.trim()) && ASS_TIMESTAMP_REGEX.matches(outEnd.trim())
        }
    }

    private fun formatIdPreview(ids: List<Int>): String {
        if (ids.isEmpty()) return ""
        val preview = ids.take(5).joinToString(", ") { "#$it" }
        return if (ids.size > 5) "$preview, ... (+${ids.size - 5})" else preview
    }
}
