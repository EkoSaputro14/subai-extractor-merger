package com.example

import com.example.data.AppUpdateChecker
import com.example.data.SemanticVersion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AppUpdateCheckerTest {

    @Test
    fun testSemanticVersionParsingAndComparison() {
        val v110 = SemanticVersion.parse("v1.1.0")
        assertEquals(1, v110.major)
        assertEquals(1, v110.minor)
        assertEquals(0, v110.patch)

        assertTrue(AppUpdateChecker.isNewerVersion("1.1.0", "v1.1.1"))
        assertTrue(AppUpdateChecker.isNewerVersion("1.1.0", "v1.2.0"))
        assertTrue(AppUpdateChecker.isNewerVersion("1.1.0", "2.0.0"))
        assertFalse(AppUpdateChecker.isNewerVersion("1.1.0", "v1.1.0"))
        assertFalse(AppUpdateChecker.isNewerVersion("1.1.0", "v1.0.9"))
    }

    @Test
    fun testParseGitHubReleaseJsonWithApkAsset() {
        val sampleJson = """
            {
              "tag_name": "v1.2.0",
              "name": "SubAI Extractor & Merger v1.2.0",
              "body": "- Added new Batch Subtitle Translation Queue\n- In-App Update Checker",
              "published_at": "2026-11-01T02:30:00Z",
              "html_url": "https://github.com/EkoSaputro14/subai-extractor-merger/releases/tag/v1.2.0",
              "assets": [
                {
                  "name": "SubAI-Extractor-Merger-v1.2.0.apk",
                  "browser_download_url": "https://github.com/EkoSaputro14/subai-extractor-merger/releases/download/v1.2.0/SubAI-Extractor-Merger-v1.2.0.apk",
                  "size": 15728640,
                  "content_type": "application/vnd.android.package-archive"
                }
              ]
            }
        """.trimIndent()

        val info = AppUpdateChecker.parseReleaseJson(sampleJson)
        assertEquals("v1.2.0", info.tagName)
        assertEquals("1.2.0", info.versionName)
        assertEquals("SubAI Extractor & Merger v1.2.0", info.releaseTitle)
        assertEquals("2026-11-01", info.publishedAt)
        assertNotNull(info.apkAsset)
        assertEquals("SubAI-Extractor-Merger-v1.2.0.apk", info.apkAsset?.name)
        assertEquals("15.0 MB", info.apkAsset?.formattedSizeMb)
        assertTrue(AppUpdateChecker.isNewerVersion("1.1.0", info.versionName))
    }
}
