package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.ThemeSettings

/**
 * Frontier type system: Inter for UI text, Inter Tight for display/headlines,
 * Geist Mono for timecodes, subtitle bodies and AI logs.
 */
val FrontierSansFamily = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold)
)

val FrontierDisplayFamily = FontFamily(
    Font(R.font.intertight_medium, FontWeight.Normal),
    Font(R.font.intertight_medium, FontWeight.Medium),
    Font(R.font.intertight_semibold, FontWeight.SemiBold)
)

val FrontierMonoFamily = FontFamily(
    Font(R.font.geistmono_regular, FontWeight.Normal),
    Font(R.font.geistmono_medium, FontWeight.Medium),
    Font(R.font.geistmono_medium, FontWeight.SemiBold)
)

/** Kept for call sites that still reference the old family names. */
val PlusJakartaSansFamily = FrontierSansFamily
val JetBrainsMonoFamily = FrontierMonoFamily

private val Tight: LineHeightStyle = LineHeightStyle(
    alignment = LineHeightStyle.Alignment.Center,
    trim = LineHeightStyle.Trim.None
)

val Typography = Typography(
    displaySmall = TextStyle(
        fontFamily = FrontierDisplayFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 34.sp,
        lineHeight = 38.sp,
        letterSpacing = (-1.3).sp,
        lineHeightStyle = Tight
    ),
    headlineMedium = TextStyle(
        fontFamily = FrontierDisplayFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 28.sp,
        lineHeight = 32.sp,
        letterSpacing = (-1.1).sp,
        lineHeightStyle = Tight
    ),
    headlineSmall = TextStyle(
        fontFamily = FrontierDisplayFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 22.sp,
        lineHeight = 26.sp,
        letterSpacing = (-0.8).sp,
        lineHeightStyle = Tight
    ),
    titleLarge = TextStyle(
        fontFamily = FrontierDisplayFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 19.sp,
        lineHeight = 24.sp,
        letterSpacing = (-0.5).sp
    ),
    titleMedium = TextStyle(
        fontFamily = FrontierSansFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
        lineHeight = 21.sp,
        letterSpacing = (-0.2).sp
    ),
    titleSmall = TextStyle(
        fontFamily = FrontierSansFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = (-0.1).sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FrontierSansFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FrontierSansFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 19.sp
    ),
    bodySmall = TextStyle(
        fontFamily = FrontierSansFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp
    ),
    labelLarge = TextStyle(
        fontFamily = FrontierSansFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 18.sp
    ),
    labelMedium = TextStyle(
        fontFamily = FrontierSansFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 15.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FrontierSansFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 10.sp,
        lineHeight = 13.sp,
        letterSpacing = 0.2.sp
    )
)

/** Monospace style used for cue timecodes and AI log excerpts. */
fun monoTextStyle(sizeSp: Float = 11f): TextStyle = TextStyle(
    fontFamily = FrontierMonoFamily,
    fontWeight = FontWeight.Normal,
    fontSize = sizeSp.sp,
    lineHeight = (sizeSp + 5).sp
)

fun subtitleEditorTextStyle(settings: ThemeSettings): TextStyle {
    val size = settings.editorFontSizeSp.coerceIn(11, 17)
    return TextStyle(
        fontFamily = if (settings.useMonospaceFont) FrontierMonoFamily else FrontierSansFamily,
        fontWeight = FontWeight.Normal,
        fontSize = size.sp,
        lineHeight = (size + 6).sp
    )
}
