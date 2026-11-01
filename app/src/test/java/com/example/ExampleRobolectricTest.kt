package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.PreferencesRepository
import com.example.model.ColorPreset
import com.example.model.ThemeMode
import com.example.model.ThemeSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string and persist theme settings and glossary rules`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("SubAI Extractor", appName)

        val repo = PreferencesRepository(context)
        val customSettings = ThemeSettings(
            themeMode = ThemeMode.DARK,
            colorPreset = ColorPreset.EMERALD_ANIME,
            useDynamicColor = false,
            pureBlackAmoled = true,
            editorFontSizeSp = 15,
            useMonospaceFont = true
        )
        repo.saveThemeSettings(customSettings)

        val loaded = repo.loadThemeSettings()
        assertEquals(ColorPreset.EMERALD_ANIME, loaded.colorPreset)
        assertTrue(loaded.pureBlackAmoled)
        assertEquals(15, loaded.editorFontSizeSp)

        val aiSettings = com.example.model.AiProviderSettings(
            preset = com.example.model.AiProviderPreset.GEMINI_WEB_COOKIE,
            geminiSecure1Psid = "g.a000psid_cookie",
            geminiSecure1Psidts = "sidts_cookie",
            combineSystemIntoUserPrompt = true,
            selectedModel = "gemini-2.5-flash"
        )
        repo.saveAiProviderSettings(aiSettings)
        val loadedAi = repo.loadAiProviderSettings()
        assertEquals(com.example.model.AiProviderPreset.GEMINI_WEB_COOKIE, loadedAi.preset)
        assertEquals("g.a000psid_cookie", loadedAi.geminiSecure1Psid)
        assertEquals("sidts_cookie", loadedAi.geminiSecure1Psidts)
        assertTrue(loadedAi.combineSystemIntoUserPrompt)
    }
}
