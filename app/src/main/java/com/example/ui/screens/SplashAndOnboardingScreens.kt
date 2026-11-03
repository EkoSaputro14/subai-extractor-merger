package com.example.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/**
 * Screen 1 of the Locked 10-Screen Reference Design:
 * Full-bleed Japanese twilight landscape (Mount Fuji, Red Torii Gate, Cherry Blossoms,
 * and Misty Lake Reflection) with centered SubAI Extractor & Merger branding and bottom mint spinner.
 */
@Composable
fun SubAiSplashScreen(
    onFinished: () -> Unit,
    autoAdvanceDelayMs: Long = 1350L
) {
    LaunchedEffect(autoAdvanceDelayMs) {
        if (autoAdvanceDelayMs > 0L) {
            delay(autoAdvanceDelayMs)
            onFinished()
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "splash_spinner")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "splash_spinner_rotation"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable { onFinished() }
            .testTag("splash_screen")
    ) {
        // Full-bleed Painterly Japanese Lake + Mount Fuji + Red Torii + Sakura Illustration
        JapaneseFujiToriiSakuraCanvas(
            modifier = Modifier.fillMaxSize()
        )

        // Center-Upper Branding Overlay matching Screen 1
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.weight(0.33f))

            SubAiParallelogramLogo(
                color = Color.White,
                modifier = Modifier.size(50.dp)
            )

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "SubAI",
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                letterSpacing = (-0.5).sp
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = "Extractor & Merger",
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White.copy(alpha = 0.96f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.weight(0.55f))

            // Bottom Mint-Green Circular Loading Ring matching Screen 1
            Canvas(
                modifier = Modifier
                    .size(34.dp)
                    .padding(bottom = 4.dp)
            ) {
                val stroke = 3.2.dp.toPx()
                drawArc(
                    color = Color(0xFF5FD383).copy(alpha = 0.25f),
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                )
                drawArc(
                    color = Color(0xFF66DF8C),
                    startAngle = rotation - 90f,
                    sweepAngle = 250f,
                    useCenter = false,
                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                )
            }

            Spacer(modifier = Modifier.height(28.dp))
        }
    }
}

/**
 * Screen 2 of the Locked 10-Screen Reference Design:
 * Onboarding screen with stacked frosted format cards (.ART, .SRT, VTT, A -> 文),
 * headline, 3 pager dots, and Mint-Green "Mulai ->" pill button.
 */
@Composable
fun SubAiOnboardingScreen(
    onStartClicked: () -> Unit
) {
    var activeSlide by remember { mutableIntStateOf(0) }

    val slideTitles = listOf(
        "Ekstrak, Terjemahkan,\n& Gabungkan Subtitle",
        "Terjemahan AI Presisi\n& Kamus Konsisten",
        "Antrean Batch Otomatis\n& Merge Bilingual"
    )
    val slideSubtitles = listOf(
        "Alur kerja lengkap dengan bantuan AI\nuntuk fansubber, kreator, dan penerjemah.",
        "Pertahankan tag styling .ASS, nomor baris,\ndan terapkan glosarium istilah secara otomatis.",
        "Proses banyak episode sekaligus dengan\npipeline Single Translation yang identik."
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF10281E),
                        Color(0xFF18382B),
                        Color(0xFF11291F)
                    )
                )
            )
            .testTag("onboarding_screen")
    ) {
        // Subtle radial ambient glow behind the central illustration
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF3E8E63).copy(alpha = 0.28f),
                        Color.Transparent
                    ),
                    center = Offset(size.width * 0.5f, size.height * 0.34f),
                    radius = size.width * 0.62f
                ),
                center = Offset(size.width * 0.5f, size.height * 0.34f),
                radius = size.width * 0.62f
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // Center Stacked Subtitle Format Cards Illustration matching Screen 2
            OnboardingStackedFormatIllustration(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(290.dp)
            )

            // Typography + Dots + Mulai Button
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = slideTitles[activeSlide],
                    fontSize = 23.sp,
                    lineHeight = 30.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = slideSubtitles[activeSlide],
                    fontSize = 13.5.sp,
                    lineHeight = 20.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color(0xFFB5C8BE),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                // 3 Pager Indicator Dots matching Screen 2
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(3) { index ->
                        val isSelected = index == activeSlide
                        Box(
                            modifier = Modifier
                                .size(if (isSelected) 8.dp else 7.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isSelected) Color.White else Color(0xFF567566)
                                )
                                .clickable { activeSlide = index }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(26.dp))

                // Primary Mint-Green Pill CTA Button: [ Mulai -> ] matching Screen 2
                Surface(
                    shape = RoundedCornerShape(27.dp),
                    color = Color.Transparent,
                    shadowElevation = 6.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .clip(RoundedCornerShape(27.dp))
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color(0xFF66D889),
                                    Color(0xFF4EC273)
                                )
                            )
                        )
                        .clickable { onStartClicked() }
                        .testTag("onboarding_start_button")
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Mulai",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0E291C)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = Color(0xFF0E291C),
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun OnboardingStackedFormatIllustration(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        // Decorative glowing dots & sparkles around the cards
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val dotColor = Color(0xFF8BE2A8).copy(alpha = 0.35f)
            drawCircle(dotColor, radius = 4.dp.toPx(), center = Offset(w * 0.20f, h * 0.22f))
            drawCircle(dotColor, radius = 3.dp.toPx(), center = Offset(w * 0.78f, h * 0.18f))
            drawCircle(dotColor, radius = 5.dp.toPx(), center = Offset(w * 0.16f, h * 0.68f))
            drawCircle(dotColor, radius = 3.5.dp.toPx(), center = Offset(w * 0.84f, h * 0.72f))
        }

        // Back frosted card layer
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFF3E5E50).copy(alpha = 0.45f),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.16f)),
            modifier = Modifier
                .width(190.dp)
                .height(145.dp)
                .offset(x = 12.dp, y = (-16).dp)
                .rotate(6f)
        ) {}

        // Main Frosted Document Card with horizontal lines
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFF566E63).copy(alpha = 0.72f),
            border = BorderStroke(1.2.dp, Color.White.copy(alpha = 0.25f)),
            shadowElevation = 8.dp,
            modifier = Modifier
                .width(196.dp)
                .height(152.dp)
                .offset(x = (-8).dp, y = (-14).dp)
                .rotate(-5f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 22.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .width(56.dp)
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.White.copy(alpha = 0.55f))
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.White.copy(alpha = 0.38f))
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.72f)
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.White.copy(alpha = 0.32f))
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.60f)
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.White.copy(alpha = 0.25f))
                )
            }
        }

        // Top-Right Pastel Blue Badge: ".ART"
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = Color(0xFFA8CCE8),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.45f)),
            shadowElevation = 6.dp,
            modifier = Modifier
                .offset(x = 52.dp, y = (-48).dp)
                .rotate(6f)
        ) {
            Text(
                text = ".ART",
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF1D3549),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp)
            )
        }

        // Bottom-Left Warm Taupe Frosted Badge: ".SRT"
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = Color(0xFF9C9A96).copy(alpha = 0.88f),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.35f)),
            shadowElevation = 6.dp,
            modifier = Modifier
                .offset(x = (-58).dp, y = 26.dp)
                .rotate(-6f)
        ) {
            Text(
                text = ".SRT",
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp)
            )
        }

        // Middle-Right Warm Peach/Cream Badge: "VTT"
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = Color(0xFFE6CBB6),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.5f)),
            shadowElevation = 6.dp,
            modifier = Modifier
                .offset(x = 58.dp, y = 20.dp)
                .rotate(4f)
        ) {
            Text(
                text = "VTT",
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF2B2623),
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 9.dp)
            )
        }

        // Bottom-Center Crisp White Card: "A -> 文"
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFFF5F8F6),
            border = BorderStroke(1.dp, Color.White),
            shadowElevation = 10.dp,
            modifier = Modifier
                .offset(x = 0.dp, y = 76.dp)
        ) {
            Text(
                text = "A \u2192 \u6587",
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF17382A),
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
            )
        }
    }
}

@Composable
private fun JapaneseFujiToriiSakuraCanvas(
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // 1. Twilight Sky Gradient (Deep Teal-Slate top -> Warm Golden-Peach horizon -> Dark Teal Lake bottom)
        drawRect(
            brush = Brush.verticalGradient(
                0.00f to Color(0xFF0E2229),
                0.28f to Color(0xFF1C353E),
                0.48f to Color(0xFF5D5E66),
                0.60f to Color(0xFFD69F89),
                0.67f to Color(0xFFE8B89A),
                0.70f to Color(0xFF2A444E),
                0.85f to Color(0xFF162C35),
                1.00f to Color(0xFF0B1920)
            )
        )

        // 2. Soft Sunset Glow behind Mount Fuji
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFFFFDFBA).copy(alpha = 0.55f),
                    Color(0xFFE5987E).copy(alpha = 0.22f),
                    Color.Transparent
                ),
                center = Offset(w * 0.52f, h * 0.60f),
                radius = w * 0.55f
            ),
            center = Offset(w * 0.52f, h * 0.60f),
            radius = w * 0.55f
        )

        // 3. Distant Mountain Range & Snow-Capped Mount Fuji
        val horizonY = h * 0.66f
        val fujiPeakLeft = w * 0.44f
        val fujiPeakRight = w * 0.56f
        val fujiPeakY = h * 0.49f

        val fujiPath = Path().apply {
            moveTo(w * 0.08f, horizonY)
            quadraticTo(w * 0.30f, h * 0.61f, fujiPeakLeft, fujiPeakY)
            lineTo(fujiPeakRight, fujiPeakY)
            quadraticTo(w * 0.70f, h * 0.61f, w * 0.95f, horizonY)
            close()
        }
        drawPath(
            path = fujiPath,
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF3A4D5C),
                    Color(0xFF283845)
                ),
                startY = fujiPeakY,
                endY = horizonY
            )
        )

        // Mount Fuji Snow Cap
        val snowCapPath = Path().apply {
            moveTo(fujiPeakLeft, fujiPeakY)
            lineTo(fujiPeakRight, fujiPeakY)
            lineTo(w * 0.61f, h * 0.545f)
            lineTo(w * 0.57f, h * 0.535f)
            lineTo(w * 0.53f, h * 0.55f)
            lineTo(w * 0.50f, h * 0.535f)
            lineTo(w * 0.46f, h * 0.55f)
            lineTo(w * 0.40f, h * 0.542f)
            close()
        }
        drawPath(
            path = snowCapPath,
            color = Color(0xFFF3ECE8).copy(alpha = 0.88f)
        )

        // 4. Misty Horizon Layers & Left/Right Shore Silhouettes
        val leftShore = Path().apply {
            moveTo(0f, h * 0.62f)
            quadraticTo(w * 0.22f, h * 0.63f, w * 0.38f, horizonY + 8f)
            lineTo(0f, horizonY + 14f)
            close()
        }
        drawPath(leftShore, color = Color(0xFF14242C))

        val rightShore = Path().apply {
            moveTo(w, h * 0.58f)
            quadraticTo(w * 0.78f, h * 0.60f, w * 0.62f, horizonY + 6f)
            lineTo(w, horizonY + 12f)
            close()
        }
        drawPath(rightShore, color = Color(0xFF162730))

        // 5. Traditional Red Japanese Torii Gate on the Left Shore
        val toriiCenterX = w * 0.22f
        val toriiBaseY = h * 0.685f
        val toriiTopY = h * 0.565f
        val pillarOffset = w * 0.055f
        val toriiRed = Color(0xFFC83E37)
        val toriiDarkRed = Color(0xFF8F2622)

        // Left & Right Pillars
        drawLine(
            color = toriiRed,
            start = Offset(toriiCenterX - pillarOffset, toriiTopY + 6f),
            end = Offset(toriiCenterX - pillarOffset * 1.08f, toriiBaseY),
            strokeWidth = 7f
        )
        drawLine(
            color = toriiRed,
            start = Offset(toriiCenterX + pillarOffset, toriiTopY + 6f),
            end = Offset(toriiCenterX + pillarOffset * 1.08f, toriiBaseY),
            strokeWidth = 7f
        )
        // Top curved Kasagi beam
        val kasagiPath = Path().apply {
            moveTo(toriiCenterX - pillarOffset * 1.65f, toriiTopY - 4f)
            quadraticTo(toriiCenterX, toriiTopY + 8f, toriiCenterX + pillarOffset * 1.65f, toriiTopY - 4f)
            lineTo(toriiCenterX + pillarOffset * 1.50f, toriiTopY + 10f)
            quadraticTo(toriiCenterX, toriiTopY + 16f, toriiCenterX - pillarOffset * 1.50f, toriiTopY + 10f)
            close()
        }
        drawPath(kasagiPath, color = toriiDarkRed)
        // Lower Nuki crossbeam
        drawLine(
            color = toriiRed,
            start = Offset(toriiCenterX - pillarOffset * 1.35f, toriiTopY + 26f),
            end = Offset(toriiCenterX + pillarOffset * 1.35f, toriiTopY + 26f),
            strokeWidth = 6f
        )

        // 6. Lake Water Ripples & Warm Reflections
        for (i in 0..8) {
            val rippleY = horizonY + 18f + i * (h * 0.025f)
            val rippleWidth = w * (0.28f - i * 0.02f).coerceAtLeast(0.06f)
            drawRoundRect(
                color = Color(0xFFE7A985).copy(alpha = (0.26f - i * 0.025f).coerceAtLeast(0.04f)),
                topLeft = Offset(w * 0.50f - rippleWidth / 2f, rippleY),
                size = Size(rippleWidth, 3.5f),
                cornerRadius = CornerRadius(2f, 2f)
            )
            // Red Torii reflection on left water
            val toriiReflectW = w * (0.10f - i * 0.008f).coerceAtLeast(0.02f)
            drawRoundRect(
                color = Color(0xFFC83E37).copy(alpha = (0.28f - i * 0.028f).coerceAtLeast(0.03f)),
                topLeft = Offset(toriiCenterX - toriiReflectW / 2f, rippleY + 6f),
                size = Size(toriiReflectW, 3f),
                cornerRadius = CornerRadius(2f, 2f)
            )
        }

        // 7. Cherry Blossom (Sakura) Branches on Upper-Left and Upper-Right
        val branchColor = Color(0xFF121A20)
        // Top-Right Branch
        val rightBranch = Path().apply {
            moveTo(w, h * 0.05f)
            quadraticTo(w * 0.78f, h * 0.11f, w * 0.56f, h * 0.17f)
            moveTo(w * 0.84f, h * 0.09f)
            quadraticTo(w * 0.75f, h * 0.18f, w * 0.66f, h * 0.24f)
            moveTo(w, h * 0.16f)
            quadraticTo(w * 0.86f, h * 0.21f, w * 0.74f, h * 0.27f)
        }
        drawPath(rightBranch, color = branchColor, style = Stroke(width = 4.5f, cap = StrokeCap.Round))

        // Top-Left Branch
        val leftBranch = Path().apply {
            moveTo(0f, h * 0.08f)
            quadraticTo(w * 0.16f, h * 0.13f, w * 0.28f, h * 0.20f)
            moveTo(0f, h * 0.22f)
            quadraticTo(w * 0.12f, h * 0.25f, w * 0.22f, h * 0.30f)
        }
        drawPath(leftBranch, color = branchColor, style = Stroke(width = 4f, cap = StrokeCap.Round))

        // Sakura Blossom Clusters (Pink & Soft Rose Petals)
        val sakuraPositions = listOf(
            Offset(w * 0.88f, h * 0.07f),
            Offset(w * 0.80f, h * 0.10f),
            Offset(w * 0.72f, h * 0.13f),
            Offset(w * 0.63f, h * 0.16f),
            Offset(w * 0.57f, h * 0.17f),
            Offset(w * 0.76f, h * 0.19f),
            Offset(w * 0.68f, h * 0.23f),
            Offset(w * 0.86f, h * 0.18f),
            Offset(w * 0.78f, h * 0.25f),
            Offset(w * 0.92f, h * 0.14f),
            Offset(w * 0.08f, h * 0.10f),
            Offset(w * 0.16f, h * 0.13f),
            Offset(w * 0.24f, h * 0.18f),
            Offset(w * 0.10f, h * 0.23f),
            Offset(w * 0.19f, h * 0.28f),
            Offset(w * 0.05f, h * 0.17f)
        )

        sakuraPositions.forEachIndexed { idx, center ->
            val baseRadius = if (idx % 2 == 0) 11f else 8.5f
            val petalOffsets = listOf(
                Offset(0f, -baseRadius),
                Offset(baseRadius * 0.95f, -baseRadius * 0.3f),
                Offset(baseRadius * 0.6f, baseRadius * 0.8f),
                Offset(-baseRadius * 0.6f, baseRadius * 0.8f),
                Offset(-baseRadius * 0.95f, -baseRadius * 0.3f)
            )
            petalOffsets.forEach { offset ->
                drawCircle(
                    color = if (idx % 3 == 0) Color(0xFFF7B9C8) else Color(0xFFEDA0B4),
                    radius = baseRadius * 0.68f,
                    center = center + offset
                )
            }
            drawCircle(
                color = Color(0xFFD16582),
                radius = baseRadius * 0.28f,
                center = center
            )
        }
    }
}
