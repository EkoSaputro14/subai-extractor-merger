package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.model.ColorPreset
import com.example.model.ThemeMode
import com.example.model.ThemeSettings

/** Frontier geometry: paper cards, pill controls, hairline borders. */
val FrontierShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

private fun buildPresetColorScheme(
    preset: ColorPreset,
    darkTheme: Boolean
): ColorScheme {
    val accent = if (darkTheme) preset.darkPrimary else preset.lightPrimary
    val accentSecondary = if (darkTheme) preset.darkSecondary else preset.lightSecondary

    return if (darkTheme) {
        darkColorScheme(
            primary = XaiDarkOnBg,
            onPrimary = XaiDarkBackground,
            primaryContainer = XaiDarkSurfaceVariant,
            onPrimaryContainer = XaiDarkOnBg,
            secondary = accent,
            onSecondary = XaiDarkBackground,
            secondaryContainer = accent.copy(alpha = 0.20f),
            onSecondaryContainer = accent,
            tertiary = XaiAccent,
            onTertiary = XaiDarkBackground,
            background = XaiDarkBackground,
            onBackground = XaiDarkOnBg,
            surface = XaiDarkSurface,
            onSurface = XaiDarkOnBg,
            surfaceContainer = XaiDarkSurfaceVariant,
            surfaceContainerLow = XaiDarkSurface,
            surfaceContainerHigh = Color(0xFF242422),
            surfaceVariant = XaiDarkSurfaceVariant,
            onSurfaceVariant = XaiDarkOnSurfaceVariant,
            outline = XaiDarkBorder,
            outlineVariant = XaiDarkBorder,
            inverseSurface = XaiDarkOnBg,
            inverseOnSurface = XaiDarkBackground,
            error = XaiDanger,
            onError = Color.White
        )
    } else {
        lightColorScheme(
            primary = XaiInk,
            onPrimary = Color.White,
            primaryContainer = XaiSurfaceVariant,
            onPrimaryContainer = XaiInk,
            secondary = accentSecondary.takeIf { preset == ColorPreset.MONO_FRONTIER } ?: accent,
            onSecondary = Color.White,
            secondaryContainer = XaiSurfaceVariant,
            onSecondaryContainer = XaiInk,
            tertiary = XaiAccent,
            onTertiary = Color.White,
            background = XaiBackground,
            onBackground = XaiInk,
            surface = XaiSurface,
            onSurface = XaiInk,
            surfaceContainer = XaiSurface,
            surfaceContainerLow = XaiBackground,
            surfaceContainerHigh = XaiSurfaceVariant,
            surfaceVariant = XaiSurfaceVariant,
            onSurfaceVariant = XaiInkMuted,
            outline = XaiBorderStrong,
            outlineVariant = XaiBorder,
            inverseSurface = XaiInk,
            inverseOnSurface = Color(0xFFF5F5F2),
            error = XaiDanger,
            onError = Color.White
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
        shapes = FrontierShapes,
        content = content
    )
}
