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
            onPrimary = Color(0xFF071320),
            primaryContainer = preset.darkPrimary.copy(alpha = 0.22f),
            onPrimaryContainer = preset.darkPrimary,
            secondary = preset.darkSecondary,
            onSecondary = Color(0xFF061A10),
            secondaryContainer = preset.darkSecondary.copy(alpha = 0.22f),
            onSecondaryContainer = preset.darkSecondary,
            tertiary = Color(0xFFFFD584),
            background = DarkBackground,
            onBackground = Color(0xFFE8EEF5),
            surface = DarkSurface,
            onSurface = Color(0xFFE8EEF5),
            surfaceVariant = DarkSurfaceVariant,
            onSurfaceVariant = Color(0xFFB5C4D6)
        )
    } else {
        lightColorScheme(
            primary = preset.lightPrimary,
            onPrimary = Color.White,
            primaryContainer = preset.lightPrimary.copy(alpha = 0.14f),
            onPrimaryContainer = preset.lightPrimary,
            secondary = preset.lightSecondary,
            onSecondary = Color.White,
            secondaryContainer = preset.lightSecondary.copy(alpha = 0.14f),
            onSecondaryContainer = preset.lightSecondary,
            tertiary = Color(0xFF935800),
            background = LightBackground,
            onBackground = Color(0xFF141B24),
            surface = LightSurface,
            onSurface = Color(0xFF141B24),
            surfaceVariant = LightSurfaceVariant,
            onSurfaceVariant = Color(0xFF425062)
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
