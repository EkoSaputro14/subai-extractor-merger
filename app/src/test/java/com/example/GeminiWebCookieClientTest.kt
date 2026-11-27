package com.example

import com.example.data.AppDiagnosticLogger
import com.example.data.GeminiWebCookieClient
import org.json.JSONArray
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class GeminiWebCookieClientTest {

    @Before
    fun setUp() {
        GeminiWebCookieClient.clearRuntimeCachesForTesting()
        AppDiagnosticLogger.clear()
    }

    @Test
    fun buildStreamGenerateFreqPayload_standardNullMetadataMatchesGeminiWebApiFormat() {
        val prompt = "[1] Hello world"
        val outerFreq = GeminiWebCookieClient.buildStreamGenerateFreqPayload(
            combinedPrompt = prompt,
            variant = GeminiWebCookieClient.FreqPayloadVariant.STANDARD_NULL_METADATA
        )

        val outerArr = JSONArray(outerFreq)
        assertEquals(2, outerArr.length())
        assertTrue(outerArr.isNull(0))

        val innerArr = JSONArray(outerArr.getString(1))
        assertEquals(3, innerArr.length())
        val promptTuple = innerArr.getJSONArray(0)
        assertEquals(1, promptTuple.length())
        assertEquals(prompt, promptTuple.getString(0))
        assertTrue(innerArr.isNull(1))
        assertTrue(innerArr.isNull(2))
    }

    @Test
    fun buildStreamGenerateFreqPayload_standardEmptyMetadataAndExtendedVariants() {
        val prompt = "[1] Hello world"
        val emptyMetaFreq = GeminiWebCookieClient.buildStreamGenerateFreqPayload(
            combinedPrompt = prompt,
            variant = GeminiWebCookieClient.FreqPayloadVariant.STANDARD_EMPTY_METADATA
        )
        val innerEmptyMeta = JSONArray(JSONArray(emptyMetaFreq).getString(1))
        val convMeta = innerEmptyMeta.getJSONArray(2)
        assertEquals(3, convMeta.length())
        assertEquals("", convMeta.getString(0))
        assertEquals("", convMeta.getString(1))
        assertEquals("", convMeta.getString(2))

        val extendedFreq = GeminiWebCookieClient.buildStreamGenerateFreqPayload(
            combinedPrompt = prompt,
            variant = GeminiWebCookieClient.FreqPayloadVariant.EXTENDED_NULL_METADATA
        )
        val innerExtended = JSONArray(JSONArray(extendedFreq).getString(1))
        val promptTuple = innerExtended.getJSONArray(0)
        assertEquals(4, promptTuple.length())
        assertEquals(prompt, promptTuple.getString(0))
        assertEquals(0, promptTuple.getInt(1))
        assertTrue(promptTuple.isNull(2))
        assertTrue(promptTuple.isNull(3))
        assertTrue(innerExtended.isNull(2))
    }

    @Test
    fun parseSessionTokensFromHtml_extractsNormalAndEscapedTokensAndBuildsBareOrQueryEndpoints() {
        val sampleHtml = """
            <script>
            WIZ_global_data = {
              \"SNlM0e\": \"AbCdEfGhIj123456:9876543210\",
              \"cfb2h\": \"boq_assistant-bard-web-server_20260115.04_p0\",
              \"FdrFJe\": \"-482910384756129384\"
            };
            </script>
        """.trimIndent()

        val tokens = GeminiWebCookieClient.parseSessionTokensFromHtml(sampleHtml)
        assertEquals("AbCdEfGhIj123456:9876543210", tokens.snlm0eToken)
        assertEquals("boq_assistant-bard-web-server_20260115.04_p0", tokens.blBuildLabel)
        assertTrue(tokens.blWasExtractedFromHtml)
        assertEquals("-482910384756129384", tokens.fSid)

        val bareEndpoint = GeminiWebCookieClient.buildStreamGenerateEndpoint(
            blBuildLabel = tokens.blBuildLabel,
            fSid = tokens.fSid,
            reqId = 123456,
            includeQueryParams = false
        )
        assertFalse(bareEndpoint.contains("?"))
        assertTrue(bareEndpoint.endsWith("/StreamGenerate"))

        val queryEndpoint = GeminiWebCookieClient.buildStreamGenerateEndpoint(
            blBuildLabel = tokens.blBuildLabel,
            fSid = tokens.fSid,
            reqId = 123456,
            includeQueryParams = true,
            includeFSid = true,
            blWasExtractedFromHtml = true
        )
        assertTrue(queryEndpoint.contains("bl=boq_assistant-bard-web-server_20260115.04_p0"))
        assertTrue(queryEndpoint.contains("&f.sid=-482910384756129384"))
        assertTrue(queryEndpoint.contains("&_reqid=123456&rt=c"))

        // If bl was NOT extracted from live HTML, never send stale hardcoded bl= query param
        val noStaleBlEndpoint = GeminiWebCookieClient.buildStreamGenerateEndpoint(
            blBuildLabel = "boq_stale",
            fSid = tokens.fSid,
            reqId = 123456,
            includeQueryParams = true,
            includeFSid = true,
            blWasExtractedFromHtml = false
        )
        assertFalse(noStaleBlEndpoint.contains("bl="))
    }

    @Test
    fun mergeCookieHeaders_appliesRotatedSetCookiesWithoutDuplicatesOrDeletedValues() {
        val merged = GeminiWebCookieClient.mergeCookieHeaders(
            psid = "g.a000validLongPsidToken1234567890",
            psidts = "sidts-old-value",
            setCookieHeaders = listOf(
                "__Secure-1PSIDTS=sidts-refreshed-value; Path=/; Secure; HttpOnly",
                "__Secure-1PSIDCC=cc-token-999; Domain=.google.com; Path=/",
                "NID=511=nid-token-abc; Path=/",
                "__Secure-1PSID=deleted; Max-Age=0"
            )
        )

        assertTrue(merged.contains("__Secure-1PSID=g.a000validLongPsidToken1234567890"))
        assertTrue(merged.contains("__Secure-1PSIDTS=sidts-refreshed-value"))
        assertFalse(merged.contains("sidts-old-value"))
        assertTrue(merged.contains("__Secure-1PSIDCC=cc-token-999"))
        assertTrue(merged.contains("NID=511=nid-token-abc"))
        assertFalse(merged.contains("deleted"))
    }

    @Test
    fun executeWithHttp400Fallback_dropsStaleModelHeaderAndCachesRejectionForNextBatches() {
        val tokens = GeminiWebCookieClient.GeminiWebSessionTokens(
            snlm0eToken = "token123",
            blBuildLabel = "boq_test",
            fSid = "998877"
        )
        val staleHeader = """[1,null,null,null,"71c2d248d3b102ff"]"""
        val recordedSpecs = mutableListOf<GeminiWebCookieClient.StreamGenerateAttemptSpec>()

        val firstResult = GeminiWebCookieClient.executeWithHttp400Fallback(
            initialTokens = tokens,
            combinedPrompt = "[1] Hello",
            initialModelHeaderVal = staleHeader,
            refreshTokens = { tokens }
        ) { _, spec ->
            recordedSpecs.add(spec)
            if (spec.modelHeaderVal != null) {
                throw GeminiWebCookieClient.StreamGenerateHttpException(
                    statusCode = 400,
                    responseSnippet = "Invalid model header",
                    message = "HTTP 400"
                )
            }
            "[1] Halo"
        }

        assertEquals("[1] Halo", firstResult)
        assertEquals(2, recordedSpecs.size)
        assertEquals(staleHeader, recordedSpecs[0].modelHeaderVal)
        assertFalse(recordedSpecs[0].includeQueryParams)
        assertNull(recordedSpecs[1].modelHeaderVal)
        assertFalse(recordedSpecs[1].includeQueryParams)
        assertEquals(
            GeminiWebCookieClient.FreqPayloadVariant.STANDARD_NULL_METADATA,
            recordedSpecs[1].payloadVariant
        )

        // Second batch should skip the rejected stale header immediately (only 1 network call)
        val secondBatchSpecs = mutableListOf<GeminiWebCookieClient.StreamGenerateAttemptSpec>()
        val secondResult = GeminiWebCookieClient.executeWithHttp400Fallback(
            initialTokens = tokens,
            combinedPrompt = "[2] Goodbye",
            initialModelHeaderVal = staleHeader,
            refreshTokens = { tokens }
        ) { _, spec ->
            secondBatchSpecs.add(spec)
            "[2] Sampai jumpa"
        }

        assertEquals("[2] Sampai jumpa", secondResult)
        assertEquals(1, secondBatchSpecs.size)
        assertNull(secondBatchSpecs[0].modelHeaderVal)
    }

    @Test
    fun executeWithHttp400Fallback_traversesAll5FallbackStagesAndLogsDiagnostics() {
        val initialTokens = GeminiWebCookieClient.GeminiWebSessionTokens(
            snlm0eToken = "stale_snlm0e",
            blBuildLabel = "boq_old",
            fSid = "112233"
        )
        val refreshedTokens = GeminiWebCookieClient.GeminiWebSessionTokens(
            snlm0eToken = "fresh_snlm0e",
            blBuildLabel = "boq_new",
            fSid = "445566"
        )

        // Scenario 1: Attempt 4 (extended tuple) succeeds after A2 and A3 fail with HTTP 400
        val specsForExtended = mutableListOf<GeminiWebCookieClient.StreamGenerateAttemptSpec>()
        val extendedResult = GeminiWebCookieClient.executeWithHttp400Fallback(
            initialTokens = initialTokens,
            combinedPrompt = "[1] Hi",
            initialModelHeaderVal = null,
            refreshTokens = { refreshedTokens }
        ) { _, spec ->
            specsForExtended.add(spec)
            if (!spec.useExtendedPromptTuple) {
                throw GeminiWebCookieClient.StreamGenerateHttpException(400, "Bad Request", "HTTP 400")
            }
            "[1] Hai"
        }
        assertEquals("[1] Hai", extendedResult)
        assertEquals(3, specsForExtended.size)
        assertFalse(specsForExtended[0].includeQueryParams) // A2 bare
        assertTrue(specsForExtended[1].includeQueryParams)  // A3 query params + empty meta
        assertTrue(specsForExtended[2].useExtendedPromptTuple) // A4 extended tuple

        // Scenario 2: All initial stages fail with HTTP 400 -> Attempt 5 refreshes SNlM0e and succeeds
        var refreshCount = 0
        val refreshResult = GeminiWebCookieClient.executeWithHttp400Fallback(
            initialTokens = initialTokens,
            combinedPrompt = "[1] Thanks",
            initialModelHeaderVal = null,
            refreshTokens = {
                refreshCount++
                refreshedTokens
            }
        ) { activeTokens, spec ->
            if (activeTokens.snlm0eToken != "fresh_snlm0e") {
                throw GeminiWebCookieClient.StreamGenerateHttpException(400, "Expired SNlM0e", "HTTP 400")
            }
            assertTrue(spec.refreshSessionBeforeAttempt)
            "[1] Terima kasih"
        }
        assertEquals(1, refreshCount)
        assertEquals("[1] Terima kasih", refreshResult)

        // Scenario 3: All attempts fail -> throws clear Indonesian HTTP 400 error mentioning Log HTTP
        val failure = runCatching {
            GeminiWebCookieClient.executeWithHttp400Fallback(
                initialTokens = initialTokens,
                combinedPrompt = "[1] Test",
                initialModelHeaderVal = null,
                refreshTokens = { refreshedTokens }
            ) { _, _ ->
                throw GeminiWebCookieClient.StreamGenerateHttpException(400, "Bad Request", "HTTP 400")
            }
        }.exceptionOrNull()
        assertNotNull(failure)
        assertTrue(failure!!.message!!.contains("HTTP 400"))
        assertTrue(failure.message!!.contains("Log HTTP"))
    }

    @Test
    fun diagnosticLogger_redactsCookiesAndTokensInExportedText() {
        AppDiagnosticLogger.http(
            "GeminiWeb",
            "Cookie __Secure-1PSID=g.a000superSecretCookieToken12345; __Secure-1PSIDTS=sidts-secret99999999"
        )
        val exported = AppDiagnosticLogger.exportAsPlainText()
        assertFalse(exported.contains("superSecretCookieToken12345"))
        assertFalse(exported.contains("sidts-secret99999999"))
        assertTrue(exported.contains("__Secure-1PSID=g.a0***"))
        assertTrue(exported.contains("__Secure-1PSIDTS=sidt***"))
    }
}
