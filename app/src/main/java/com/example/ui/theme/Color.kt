package com.example.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * SubAI Frontier design tokens.
 *
 * Visual language extracted from the x.ai reference design and re-applied here:
 * warm off-white surfaces, near-black ink, hairline borders, pill controls,
 * monochrome emphasis with a single orange accent.
 *
 * Canonical names use the Xai prefix. The legacy Forest and Sage names are kept as
 * aliases because they are referenced by every screen; they now resolve to the Frontier
 * palette so the whole app renders in the new design without touching call sites.
 */

// ---------------------------------------------------------------- canonical ink
val XaiBackground = Color(0xFFFFFFFF)
val XaiSurface = Color(0xFFF9F8F6)
val XaiSurfaceVariant = Color(0xFFF1F0EC)
val XaiInk = Color(0xFF0A0A0A)
val XaiInkMuted = Color(0xFF6E6E68)
val XaiBorder = Color(0xFFE7E5E0)
val XaiBorderStrong = Color(0xFFD8D5CF)
val XaiAccent = Color(0xFFFF5C1A)
val XaiAccentSoft = Color(0xFFFFE9DE)
val XaiDarkSheet = Color(0xFF141413)
val XaiDarkSheetBorder = Color(0xFF2A2A28)
val XaiOnDarkMuted = Color(0xFF9C9C96)

// ------------------------------------------------------------- status semantics
val XaiOk = Color(0xFF1F7A45)
val XaiOkSoft = Color(0xFFE7F2EB)
val XaiWarn = Color(0xFFB4560E)
val XaiWarnSoft = Color(0xFFFDF3E8)
val XaiDanger = Color(0xFFC02A1A)
val XaiDangerSoft = Color(0xFFFDECEA)

// ------------------------------------------------------------------ dark (theme)
val XaiDarkBackground = Color(0xFF0A0A0A)
val XaiDarkSurface = Color(0xFF141413)
val XaiDarkSurfaceVariant = Color(0xFF1E1E1C)
val XaiDarkOnBg = Color(0xFFF5F5F2)
val XaiDarkOnSurfaceVariant = Color(0xFFA8A8A1)
val XaiDarkBorder = Color(0xFF2E2E2B)

// ------------------------------------------------------- legacy alias: surfaces
val LightBackground = XaiBackground
val LightSurface = XaiSurface
val LightSurfaceVariant = XaiSurfaceVariant

val DarkBackground = XaiDarkBackground
val DarkSurface = XaiDarkSurface
val DarkSurfaceVariant = XaiDarkSurfaceVariant

val PureBlackBackground = Color(0xFF000000)
val PureBlackSurface = Color(0xFF0A0A0A)
val PureBlackSurfaceVariant = Color(0xFF141413)

// ----------------------------------------------- legacy alias: brand / headers
/** Chrome colour for app bars, nav rail and dark hero sheets. */
val ForestGreenDark = XaiInk
val ForestGreenPrimary = XaiInk
val DeepForestHeaderBg = XaiInk
val DeepForestGradientTop = Color(0xFF141413)
val DeepForestGradientBottom = Color(0xFF0A0A0A)

val AlabasterSheetBg = XaiSurface
val HomeTileAlabaster = XaiSurfaceVariant
val HomeHighlightGreenTop = Color(0xFF1C1C1A)
val HomeHighlightGreenBottom = Color(0xFF0A0A0A)
val HomeHighlightMintBorder = XaiAccent

/** Primary action fill: solid ink pills. */
val PrimaryForestButton = XaiInk
/** Secondary/inverted action fill: paper pill with hairline border. */
val PrimaryMintCta = XaiSurfaceVariant
val SageHighlightFill = XaiSurfaceVariant
val SageSoftCardFill = XaiSurface
val SageCompletedGreen = XaiOk
val EmeraldCheckGreen = XaiOk
val AmberHighlightYellow = XaiWarnSoft
val RetryAmberOrange = XaiWarn
