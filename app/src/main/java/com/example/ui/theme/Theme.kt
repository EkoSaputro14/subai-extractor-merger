package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.example.model.ColorPreset
import com.example.model.ThemeMode
import com.example.model.ThemeSettings

private fun buildPresetColorScheme(
    preset: ColorPreset,
    darkTheme: Boolean
): ColorScheme {
    return if (darkTheme) {
        darkColorScheme(
            primary = preset.darkPrimary,
            onPrimary = Color(0xFF071D12),
            primaryContainer = preset.darkPrimary.copy(alpha = 0.22f),
            onPrimaryContainer = preset.darkPrimary,
            secondary = preset.darkSecondary,
            onSecondary = Color(0xFF071D12),
            secondaryContainer = preset.darkSecondary.copy(alpha = 0.22f),
            onSecondaryContainer = preset.darkSecondary,
            tertiary = Color(0xFFFFD584),
            background = DarkBackground,
            onBackground = Color(0xFFE8F1EC),
            surface = DarkSurface,
            onSurface = Color(0xFFE8F1EC),
            surfaceContainer = Color(0xFF192720),
            surfaceContainerLow = Color(0xFF141F1A),
            surfaceContainerHigh = Color(0xFF213229),
            surfaceVariant = DarkSurfaceVariant,
            onSurfaceVariant = Color(0xFFB5C7BD),
            outline = Color(0xFF3A4D42),
            outlineVariant = Color(0xFF28382F)
        )
    } else {
        val isForestPreset = preset == ColorPreset.EMERALD_ANIME
        lightColorScheme(
            primary = preset.lightPrimary,
            onPrimary = Color.White,
            primaryContainer = if (isForestPreset) SageHighlightFill else preset.lightPrimary.copy(alpha = 0.16f),
            onPrimaryContainer = if (isForestPreset) Color(0xFF0F291E) else preset.lightPrimary,
            secondary = preset.lightSecondary,
            onSecondary = Color.White,
            secondaryContainer = if (isForestPreset) SageSoftCardFill else preset.lightSecondary.copy(alpha = 0.15f),
            onSecondaryContainer = if (isForestPreset) Color(0xFF163527) else preset.lightSecondary,
            tertiary = RetryAmberOrange,
            background = LightBackground,
            onBackground = Color(0xFF141D18),
            surface = LightSurface,
            onSurface = Color(0xFF141D18),
            surfaceContainer = Color(0xFFF0F4F1),
            surfaceContainerLow = Color(0xFFF6F8F6),
            surfaceContainerHigh = Color(0xFFE7EDE9),
            surfaceVariant = LightSurfaceVariant,
            onSurfaceVariant = Color(0xFF4A5950),
            outline = Color(0xFFBCC7C0),
            outlineVariant = Color(0xFFDCE3DF)
        )
    }
}

@Composable
fun SubAiExtractorTheme(
    themeSettings: ThemeSettings = ThemeSettings(),
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val useDarkTheme = when (themeSettings.themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> systemDark
    }

    val context = LocalContext.current
    val baseScheme = if (themeSettings.useDynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (useDarkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else {
        buildPresetColorScheme(themeSettings.colorPreset, useDarkTheme)
    }

    val finalScheme = if (useDarkTheme && themeSettings.pureBlackAmoled) {
        baseScheme.copy(
            background = PureBlackBackground,
            surface = PureBlackSurface,
            surfaceContainer = PureBlackSurface,
            surfaceContainerLow = PureBlackBackground,
            surfaceContainerHigh = PureBlackSurfaceVariant,
            surfaceVariant = PureBlackSurfaceVariant
        )
    } else {
        baseScheme
    }

    MaterialTheme(
        colorScheme = finalScheme,
        typography = Typography,
        content = content
    )
}
