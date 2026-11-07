package com.example

import com.example.data.OpenAiCompatibleClient
import com.example.domain.SampleSubtitles
import com.example.domain.SubtitleParserAndMerger
import com.example.model.BilingualOrder
import com.example.model.GlossaryRule
import com.example.model.IndexFormatMode
import com.example.model.LineSeparatorMode
import com.example.model.ModelPromptProfile
import com.example.model.SubtitleFormat
import com.example.model.TargetLanguageOption
import com.example.model.TranslationStylePreset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun parseAss_preservesStylesCommentsAndExtractsLeadingTags() {
        val doc = SubtitleParserAndMerger.parseSubtitle(
            fileName = SampleSubtitles.SAMPLE_ASS_FILENAME,
            rawContent = SampleSubtitles.SAMPLE_ASS_CONTENT
        )

        assertEquals(SubtitleFormat.ASS, doc.format)
        assertEquals(8, doc.cues.size)

        val firstCue = doc.cues.first()
        assertEquals(1, firstCue.id)
        assertEquals("TopSign", firstCue.style)
        assertEquals("Sign", firstCue.actor)
        assertEquals("{\\an8\\pos(960,85)\\fad(150,150)}", firstCue.leadingAssTags)
        assertTrue(firstCue.cleanText.contains("Shibuya Ward\\NB1F Tokyu"))
    }

    @Test
    fun mergeAss_restoresLeadingTagsAndMultiLineBreaksAndPreservesComments() {
        val doc = SubtitleParserAndMerger.parseSubtitle(
            fileName = SampleSubtitles.SAMPLE_ASS_FILENAME,
            rawContent = SampleSubtitles.SAMPLE_ASS_CONTENT
        )
        val translations = SubtitleParserAndMerger.parseAiTranslatedLines(
            SampleSubtitles.SAMPLE_ASS_AI_TRANSLATION_DEMO
        )
        assertEquals(8, translations.size)

        val (afterGlossary, replacementsCount) = SubtitleParserAndMerger.applyGlossaryRulesToTranslations(
            translations = translations,
            rules = listOf(
                GlossaryRule("1", "Shibuya Ward", "Distrik Shibuya", isCaseSensitive = false, wholeWord = true, isEnabled = true),
                GlossaryRule("2", "Onii-chan", "Kakak", isCaseSensitive = false, wholeWord = true, isEnabled = true),
                GlossaryRule("3", "Domain Expansion", "Perluasan Wilayah", isCaseSensitive = false, wholeWord = true, isEnabled = true)
            )
        )
        assertTrue(replacementsCount >= 5)

        val mergedContent = SubtitleParserAndMerger.generateMergedFileContent(
            document = doc,
            translations = afterGlossary,
            separatorMode = LineSeparatorMode.DOUBLE_SLASH,
            customSeparator = " // ",
            restoreLeadingAssTags = true,
            bilingualMode = false,
            bilingualOrder = BilingualOrder.TRANSLATION_TOP,
            bilingualAssSecondaryStyling = true
        )

        assertTrue(mergedContent.contains("[V4+ Styles]"))
        assertTrue(mergedContent.contains("Comment: 0,0:00:00.00,0:00:05.00,Default,Typesetter,0,0,0,,--- Bagian OP dan Papan Lokasi Stasiun ---"))
        assertTrue(
            mergedContent.contains(
                "Dialogue: 0,0:00:01.50,0:00:05.20,TopSign,Sign,0,0,0,,{\\an8\\pos(960,85)\\fad(150,150)}31 Oktober 2018 - Distrik Shibuya\\NPintu Masuk B1F Toserba Tokyu"
            )
        )
    }

    @Test
    fun findAndReplace_respectsAssStyleAndActorFilters() {
        val doc = SubtitleParserAndMerger.parseSubtitle(
            fileName = SampleSubtitles.SAMPLE_ASS_FILENAME,
            rawContent = SampleSubtitles.SAMPLE_ASS_CONTENT
        )
        val translations = SubtitleParserAndMerger.parseAiTranslatedLines(
            SampleSubtitles.SAMPLE_ASS_AI_TRANSLATION_DEMO
        )

        val topSignMatches = SubtitleParserAndMerger.findMatchesInTranslations(
            document = doc,
            translations = translations,
            findQuery = "Shibuya Ward",
            replaceWith = "Distrik Shibuya",
            caseSensitive = false,
            wholeWord = true,
            useRegex = false,
            selectedStyleFilter = "TopSign",
            selectedActorFilter = "ALL"
        )
        assertEquals(1, topSignMatches.size)
        assertEquals(1, topSignMatches.first().cueId)

        val allMatches = SubtitleParserAndMerger.findMatchesInTranslations(
            document = doc,
            translations = translations,
            findQuery = "Shibuya Ward",
            replaceWith = "Distrik Shibuya",
            caseSensitive = false,
            wholeWord = true,
            useRegex = false,
            selectedStyleFilter = "ALL",
            selectedActorFilter = "ALL"
        )
        assertEquals(3, allMatches.size)
    }

    @Test
    fun bilingualModeAss_placesTranslationTopAndSecondaryStyledOriginalBottom() {
        val doc = SubtitleParserAndMerger.parseSubtitle(
            fileName = SampleSubtitles.SAMPLE_ASS_FILENAME,
            rawContent = SampleSubtitles.SAMPLE_ASS_CONTENT
        )
        val cue = doc.cues[5]
        val mergedLine = SubtitleParserAndMerger.buildMergedCueText(
            cue = cue,
            format = SubtitleFormat.ASS,
            translatedSingleLine = "Perluasan Wilayah: // Kehampaan Tanpa Batas!",
            separatorMode = LineSeparatorMode.DOUBLE_SLASH,
            customSeparator = " // ",
            restoreLeadingAssTags = true,
            bilingualMode = true,
            bilingualOrder = BilingualOrder.TRANSLATION_TOP,
            bilingualAssSecondaryStyling = true
        )

        assertTrue(mergedLine.startsWith("{\\an2\\b1}"))
        assertTrue(mergedLine.contains("Perluasan Wilayah:\\NKehampaan Tanpa Batas!\\N{\\r\\fs34\\1c&HD0DCE5&}Domain Expansion:\\NUnlimited Void!"))
    }

    @Test
    fun modelAdaptedSystemPrompt_adaptsToSelectedModelAndCleansThinkTags() {
        assertEquals(ModelPromptProfile.REASONING_DIRECT, ModelPromptProfile.fromModelId("deepseek-reasoner"))
        assertEquals(ModelPromptProfile.COMPACT_FEW_SHOT, ModelPromptProfile.fromModelId("gpt-4o-mini"))
        assertEquals(ModelPromptProfile.FLAGSHIP_CONTEXTUAL, ModelPromptProfile.fromModelId("deepseek-chat"))

        val reasoningPrompt = SubtitleParserAndMerger.buildModelAdaptedSystemPrompt(
            selectedModel = "deepseek-reasoner",
            separatorMode = LineSeparatorMode.DOUBLE_SLASH,
            customSeparator = " // ",
            indexFormatMode = IndexFormatMode.BRACKET,
            includeGlossaryInPrompt = true,
            targetLanguageOption = TargetLanguageOption.INDONESIAN,
            customTargetLanguage = "Bahasa Indonesia",
            stylePreset = TranslationStylePreset.ANIME_CASUAL,
            customPromptInstruction = TranslationStylePreset.ANIME_CASUAL.styleInstruction,
            glossaryRules = listOf(
                GlossaryRule("1", "Shibuya Ward", "Distrik Shibuya", isCaseSensitive = false, wholeWord = true, isEnabled = true)
            )
        )
        assertTrue(reasoningPrompt.contains("INSTRUKSI KHUSUS MODEL REASONING (deepseek-reasoner)"))
        assertTrue(reasoningPrompt.contains("\"Shibuya Ward\" -> \"Distrik Shibuya\""))

        val compactPrompt = SubtitleParserAndMerger.buildModelAdaptedSystemPrompt(
            selectedModel = "gpt-4o-mini",
            separatorMode = LineSeparatorMode.PIPE,
            customSeparator = " | ",
            indexFormatMode = IndexFormatMode.DOT,
            includeGlossaryInPrompt = false,
            targetLanguageOption = TargetLanguageOption.INDONESIAN,
            customTargetLanguage = "Bahasa Indonesia",
            stylePreset = TranslationStylePreset.MOVIE_FORMAL,
            customPromptInstruction = TranslationStylePreset.MOVIE_FORMAL.styleInstruction,
            glossaryRules = emptyList()
        )
        assertTrue(compactPrompt.contains("CONTOH POLA INPUT → OUTPUT WAJIB (gpt-4o-mini)"))
        assertTrue(compactPrompt.contains("1. Wait here! | The signal is blocked."))

        val rawR1Output = "<think>\nTranslating line 1 and 2...\n</think>\n```text\n[1] Halo // Dunia\n[2] Uji coba\n```"
        val cleaned = OpenAiCompatibleClient.cleanAiModelOutput(rawR1Output)
        val parsed = SubtitleParserAndMerger.parseAiTranslatedLines(cleaned)
        assertEquals(2, parsed.size)
        assertEquals("Halo // Dunia", parsed[1])
    }

    @Test
    fun geminiWebCookieSmartParser_extractsPsidAndPsidtsFromJsonHeaderAndEnv() {
        val cookieEditorJson = """
            [
              {"name": "__Secure-1PSID", "value": "g.a000testPsidValue123"},
              {"name": "__Secure-1PSIDTS", "value": "sidts-testPsidtsValue456"}
            ]
        """.trimIndent()
        val parsedFromJson = com.example.data.GeminiWebCookieClient.parseSmartCookieInput(cookieEditorJson)
        assertEquals("g.a000testPsidValue123", parsedFromJson.secure1Psid)
        assertEquals("sidts-testPsidtsValue456", parsedFromJson.secure1Psidts)

        val headerRaw = "Cookie: __Secure-1PSID=g.a000headerPsid; __Secure-1PSIDTS=sidts-headerPsidts; other=1"
        val parsedFromHeader = com.example.data.GeminiWebCookieClient.parseSmartCookieInput(headerRaw)
        assertEquals("g.a000headerPsid", parsedFromHeader.secure1Psid)
        assertEquals("sidts-headerPsidts", parsedFromHeader.secure1Psidts)

        val envRaw = "SECURE_1PSID=\"g.a000envPsid\"\nSECURE_1PSIDTS=\"sidts-envPsidts\""
        val parsedFromEnv = com.example.data.GeminiWebCookieClient.parseSmartCookieInput(envRaw)
        assertEquals("g.a000envPsid", parsedFromEnv.secure1Psid)
        assertEquals("sidts-envPsidts", parsedFromEnv.secure1Psidts)

        assertEquals(ModelPromptProfile.COMPACT_FEW_SHOT, ModelPromptProfile.fromModelId("gemini-3.8-flash"))
        assertEquals(ModelPromptProfile.FLAGSHIP_CONTEXTUAL, ModelPromptProfile.fromModelId("gemini-4-argon"))
        assertEquals(ModelPromptProfile.REASONING_DIRECT, ModelPromptProfile.fromModelId("gemini-3.5-flash-thinking"))
        assertEquals(ModelPromptProfile.COMPACT_FEW_SHOT, ModelPromptProfile.fromModelId("gemini-2.5-flash"))
        assertEquals(ModelPromptProfile.FLAGSHIP_CONTEXTUAL, ModelPromptProfile.fromModelId("gemini-2.5-pro"))
        assertEquals(ModelPromptProfile.REASONING_DIRECT, ModelPromptProfile.fromModelId("gemini-2.0-flash-thinking"))
    }

    @Test
    fun accurateModelDiscovery_parsesAndRanksGemini38FlashAndArgonAndFixesAuditBugs() {
        val sampleModelsJson = """
            {
              "object": "list",
              "data": [
                {"id": "gemini-2.5-flash"},
                {"id": "models/gemini-4-argon"},
                {"id": "gemini-3.8-flash"},
                {"id": "gemini-3.5-flash-thinking"}
              ]
            }
        """.trimIndent()

        val parsedModels = OpenAiCompatibleClient.parseModelsFromJsonBody(sampleModelsJson)
        assertEquals(4, parsedModels.size)
        assertEquals("gemini-3.8-flash", parsedModels[0])
        assertEquals("gemini-4-argon", parsedModels[1])
        assertEquals("gemini-3.5-flash-thinking", parsedModels[2])

        // Verify candidate endpoints for server URL without /v1
        val endpoints = OpenAiCompatibleClient.buildCandidateModelEndpoints("http://127.0.0.1:8081")
        assertTrue(endpoints.contains("http://127.0.0.1:8081/v1/models"))
        assertTrue(endpoints.contains("http://127.0.0.1:8081/models"))

        // Verify UTF-8 BOM handling on .VTT
        val vttWithBom = "\uFEFFWEBVTT\n\n00:00:01.000 --> 00:00:03.000\nHello World"
        val vttDoc = SubtitleParserAndMerger.parseSubtitle("test.vtt", vttWithBom)
        assertEquals(SubtitleFormat.VTT, vttDoc.format)
        assertEquals(1, vttDoc.cues.size)
        assertEquals("Hello World", vttDoc.cues.first().cleanText)

        // Verify Find & Replace with literal '$' does not crash in non-regex mode
        val matchesWithDollar = SubtitleParserAndMerger.findMatchesInTranslations(
            document = vttDoc,
            translations = mapOf(1 to "Harga tiket 100 yen"),
            findQuery = "100 yen",
            replaceWith = "$100 USD",
            caseSensitive = false,
            wholeWord = false,
            useRegex = false,
            selectedStyleFilter = "ALL",
            selectedActorFilter = "ALL"
        )
        assertEquals(1, matchesWithDollar.size)
        assertEquals("Harga tiket $100 USD", matchesWithDollar.first().previewReplacedText)
    }

    @Test
    fun flexibleAiParserAndAutoRetryFallback_handlesMarkdownTwoLineBlocksAndUnnumberedResponses() {
        val doc = SubtitleParserAndMerger.parseSubtitle(
            fileName = SampleSubtitles.SAMPLE_SRT_FILENAME,
            rawContent = SampleSubtitles.SAMPLE_SRT_CONTENT
        )

        // 1. Test Stage A: Markdown bold, bullets, colons, backticks, parentheses, and # prefixes
        val markdownResponse = """
            Berikut adalah hasil terjemahan subtitle Anda:
            - **[1]** Aah... akhirnya sampai juga...
            * `[2]`: Jadi ini... Kota Aqua...?
            > **3.** Luar biasa...!
            (#4) Kita benar-benar berhasil...
            Baris 5: Sekarang, apa yang kita lakukan?
            | [6] | Tunggu sebentar di sini! |
        """.trimIndent()

        val stageAParsed = SubtitleParserAndMerger.parseAiTranslatedLines(markdownResponse)
        assertEquals(6, stageAParsed.size)
        assertEquals("Aah... akhirnya sampai juga...", stageAParsed[1])
        assertEquals("Jadi ini... Kota Aqua...?", stageAParsed[2])
        assertEquals("Luar biasa...!", stageAParsed[3])
        assertEquals("Kita benar-benar berhasil...", stageAParsed[4])
        assertEquals("Sekarang, apa yang kita lakukan?", stageAParsed[5])
        assertEquals("Tunggu sebentar di sini!", stageAParsed[6])

        // 2. Test Stage B: Standalone index on one line and translated text on the next line
        val twoLineBlockResponse = """
            **[1]**
            Baris pertama di bawah nomor
            [2]
            Baris kedua di bawah nomor
            #3
            Baris ketiga di bawah nomor
        """.trimIndent()

        val stageBReport = SubtitleParserAndMerger.parseAiTranslatedLinesWithReport(twoLineBlockResponse)
        assertEquals(3, stageBReport.parsedLines.size)
        assertEquals("Baris pertama di bawah nomor", stageBReport.parsedLines[1])
        assertEquals("Baris kedua di bawah nomor", stageBReport.parsedLines[2])
        assertEquals("Baris ketiga di bawah nomor", stageBReport.parsedLines[3])
        assertEquals("Parser Blok Nomor Multi-Baris", stageBReport.strategyUsed)

        // 3. Test Stage D: Positional Fallback when AI omits line numbers completely
        val threeCues = doc.cues.take(3)
        val unnumberedResponse = """
            Berikut adalah terjemahan ke Bahasa Indonesia:
            Halo semuanya, selamat datang!
            Apakah kalian sudah siap?
            Mari kita mulai sekarang!
        """.trimIndent()

        val stageDReport = SubtitleParserAndMerger.parseAiTranslatedLinesWithReport(
            pastedText = unnumberedResponse,
            expectedCues = threeCues,
            allowPositionalFallback = true
        )
        assertEquals(3, stageDReport.parsedLines.size)
        assertEquals("Halo semuanya, selamat datang!", stageDReport.parsedLines[threeCues[0].id])
        assertEquals("Apakah kalian sudah siap?", stageDReport.parsedLines[threeCues[1].id])
        assertEquals("Mari kita mulai sekarang!", stageDReport.parsedLines[threeCues[2].id])
        assertEquals("Pemetaan Baris Otomatis (Positional Fallback)", stageDReport.strategyUsed)
    }
}

