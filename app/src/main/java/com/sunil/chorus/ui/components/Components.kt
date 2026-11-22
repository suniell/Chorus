package com.sunil.chorus.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sunil.chorus.ui.theme.*
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin
import kotlin.random.Random

// ─────────────────────────────────────────────
// 1. NightSky: animated background with soft glows and twinkling stars
// ─────────────────────────────────────────────
private data class SkyStar(val x: Float, val y: Float, val radius: Float, val phase: Float)

@Composable
fun NightSky(modifier: Modifier = Modifier, starCount: Int = 90) {
    val stars = remember {
        List(starCount) {
            SkyStar(
                x = Random.nextFloat(),
                y = Random.nextFloat() * 0.85f,          // fewer stars near the bottom
                radius = Random.nextFloat() * 1.4f + 0.4f,
                phase = Random.nextFloat() * 6.28f,
            )
        }
    }
    val transition = rememberInfiniteTransition(label = "sky")
    val time by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(durationMillis = 5000, easing = LinearEasing)),
        label = "twinkle",
    )

    Canvas(modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        // Sky colour, lighter at the top
        drawRect(Brush.verticalGradient(listOf(Dusk, Midnight, MidnightDeep)))

        // Two soft glows, like clouds lit by the moon
        val blueGlow = Offset(w * 0.85f, h * 0.12f)
        drawCircle(
            Brush.radialGradient(listOf(Color(0x553B4C9A), Color.Transparent), center = blueGlow, radius = w * 0.7f),
            radius = w * 0.7f,
            center = blueGlow,
        )
        val violetGlow = Offset(w * 0.1f, h * 0.4f)
        drawCircle(
            Brush.radialGradient(listOf(Color(0x405B3B8C), Color.Transparent), center = violetGlow, radius = w * 0.6f),
            radius = w * 0.6f,
            center = violetGlow,
        )

        // Stars fading in and out
        stars.forEach { star ->
            val twinkle = (sin(time + star.phase) + 1f) / 2f
            drawCircle(
                color = Color.White.copy(alpha = 0.25f + 0.6f * twinkle),
                radius = star.radius.dp.toPx(),
                center = Offset(star.x * w, star.y * h),
            )
        }
    }
}

// ─────────────────────────────────────────────
// 2. Sparkle ✦ and a divider with a sparkle in the middle
// ─────────────────────────────────────────────
@Composable
fun Sparkle(modifier: Modifier = Modifier, color: Color = Gold, sparkleSize: Dp = 14.dp) {
    Canvas(modifier.size(sparkleSize)) {
        val c = center
        val r = size.minDimension / 2f
        val k = r * 0.22f   // how thin the star's arms are
        val path = Path().apply {
            moveTo(c.x, c.y - r)
            lineTo(c.x + k, c.y - k)
            lineTo(c.x + r, c.y)
            lineTo(c.x + k, c.y + k)
            lineTo(c.x, c.y + r)
            lineTo(c.x - k, c.y + k)
            lineTo(c.x - r, c.y)
            lineTo(c.x - k, c.y - k)
            close()
        }
        drawPath(path, color)
    }
}

@Composable
fun SparkleDivider(modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .weight(1f)
                .height(1.dp)
                .background(Brush.horizontalGradient(listOf(Color.Transparent, NightLine)))
        )
        Spacer(Modifier.width(10.dp))
        Sparkle()
        Spacer(Modifier.width(10.dp))
        Box(
            Modifier
                .weight(1f)
                .height(1.dp)
                .background(Brush.horizontalGradient(listOf(NightLine, Color.Transparent)))
        )
    }
}

// ─────────────────────────────────────────────
// 3. Buttons: amber (main action) and outline (second option)
// ─────────────────────────────────────────────
@Composable
fun AmberButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.heightIn(min = 52.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Amber,
            contentColor = MidnightDeep,
            disabledContainerColor = NightLine,
            disabledContentColor = Mist,
        ),
    ) {
        Text(text, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
fun GhostButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = 52.dp),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, NightLine),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = StarWhite),
    ) {
        Text(text, style = MaterialTheme.typography.titleMedium)
    }
}

// ─────────────────────────────────────────────
// 4. Play / pause icon (drawn, so it looks the same on every phone)
// ─────────────────────────────────────────────
@Composable
fun PlayGlyph(isPlaying: Boolean, color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        if (isPlaying) {
            val barWidth = w * 0.3f
            drawRect(color, topLeft = Offset(w * 0.1f, 0f), size = Size(barWidth, h))
            drawRect(color, topLeft = Offset(w * 0.6f, 0f), size = Size(barWidth, h))
        } else {
            val triangle = Path().apply {
                moveTo(w * 0.18f, 0f)
                lineTo(w, h / 2f)
                lineTo(w * 0.18f, h)
                close()
            }
            drawPath(triangle, color)
        }
    }
}

// Big round amber play button
@Composable
fun PlayButton(
    isPlaying: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    buttonSize: Dp = 56.dp,
) {
    Surface(onClick = onClick, shape = CircleShape, color = Amber, modifier = modifier.size(buttonSize)) {
        PlayGlyph(
            isPlaying = isPlaying,
            color = MidnightDeep,
            modifier = Modifier
                .fillMaxSize()
                .padding(buttonSize * 0.32f),
        )
    }
}

// ─────────────────────────────────────────────
// 5. Waveform: little bars that dance while playing
// ─────────────────────────────────────────────
@Composable
fun Waveform(
    playing: Boolean,
    modifier: Modifier = Modifier,
    color: Color = Amber,
    bars: Int = 28,
) {
    val heights = remember(bars) {
        val random = Random(7)
        List(bars) { 0.25f + random.nextFloat() * 0.75f }
    }
    val transition = rememberInfiniteTransition(label = "wave")
    val time by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(durationMillis = 1200, easing = LinearEasing)),
        label = "waveTime",
    )
    Canvas(modifier) {
        val slot = size.width / bars
        val barWidth = slot * 0.55f
        heights.forEachIndexed { i, base ->
            val wobble = if (playing) 0.55f + 0.45f * abs(sin(time + i * 0.7f)) else 1f
            val barHeight = size.height * base * wobble
            drawRoundRect(
                color = color,
                topLeft = Offset(i * slot + (slot - barWidth) / 2f, (size.height - barHeight) / 2f),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barWidth / 2f),
            )
        }
    }
}

// ─────────────────────────────────────────────
// 6. QuoteCard: a character's line, tap to "hear" it
// ─────────────────────────────────────────────
@Composable
fun QuoteCard(
    speaker: String,
    quote: String,
    color: Color,
    playing: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = NightCard.copy(alpha = 0.9f),
        contentColor = StarWhite,
        border = BorderStroke(1.dp, NightLine),
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(speaker.uppercase(), style = MaterialTheme.typography.labelSmall, color = color)
            Spacer(Modifier.height(6.dp))
            Text(
                "“$quote”",
                style = ScriptNarration.copy(fontSize = 16.sp, lineHeight = 22.sp),
            )
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Waveform(
                    playing = playing,
                    color = color,
                    bars = 14,
                    modifier = Modifier
                        .weight(1f)
                        .height(16.dp),
                )
                Spacer(Modifier.width(10.dp))
                Box(
                    Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(color),
                    contentAlignment = Alignment.Center,
                ) {
                    PlayGlyph(playing, MidnightDeep, Modifier.size(10.dp))
                }
            }
        }
    }
}

// ─────────────────────────────────────────────
// 7. PaperCard: the cream "script" card
// ─────────────────────────────────────────────
@Composable
fun PaperCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = modifier,
        color = Paper,
        contentColor = Ink,
        shape = RoundedCornerShape(14.dp),
    ) {
        Column(Modifier.padding(16.dp), content = content)
    }
}

// ─────────────────────────────────────────────
// 8. ScriptLine: one line of the story. The current line glows gold.
// ─────────────────────────────────────────────
@Composable
fun ScriptLine(
    speaker: String,
    text: String,
    speakerColor: Color,
    isNarration: Boolean,
    highlighted: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val background by animateColorAsState(
        if (highlighted) Gold.copy(alpha = 0.28f) else Color.Transparent,
        label = "lineBackground",
    )
    val textColor by animateColorAsState(
        if (highlighted) Ink else Ink.copy(alpha = 0.6f),
        label = "lineText",
    )
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(background)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Text(speaker.uppercase(), style = MaterialTheme.typography.labelSmall, color = speakerColor)
        Spacer(Modifier.height(4.dp))
        Text(text, style = if (isNarration) ScriptNarration else ScriptText, color = textColor)
    }
}

// ─────────────────────────────────────────────
// 9. BookCover: a painted-looking cover drawn in code (moon, glow, hills)
// ─────────────────────────────────────────────
private data class CoverStyle(val top: Color, val bottom: Color, val glow: Color)

private val CoverStyles = listOf(
    CoverStyle(Color(0xFF2B3A6B), Color(0xFF0E1430), Color(0xFFF2C879)), // moonlit blue
    CoverStyle(Color(0xFF6B2B3A), Color(0xFF1A0B12), Color(0xFFF4AE86)), // crimson
    CoverStyle(Color(0xFF2B5B4E), Color(0xFF0B1A16), Color(0xFFBFE3C0)), // forest
    CoverStyle(Color(0xFF4E3B7A), Color(0xFF120B22), Color(0xFFC3AEF2)), // violet
    CoverStyle(Color(0xFF7A5A2B), Color(0xFF1E140A), Color(0xFFF2C879)), // amber dusk
)

@Composable
fun BookCover(title: String, author: String, styleIndex: Int, modifier: Modifier = Modifier) {
    val style = CoverStyles[Math.floorMod(styleIndex, CoverStyles.size)]

    Box(
        modifier
            .aspectRatio(2f / 3f)
            .clip(RoundedCornerShape(10.dp))
            .background(Brush.verticalGradient(listOf(style.top, style.bottom)))
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val moon = Offset(w * 0.7f, h * 0.28f)
            // Glow around the moon
            drawCircle(
                Brush.radialGradient(listOf(style.glow.copy(alpha = 0.55f), Color.Transparent), center = moon, radius = w * 0.6f),
                radius = w * 0.6f,
                center = moon,
            )
            // The moon
            drawCircle(style.glow, radius = w * 0.13f, center = moon)
            // Dark hills at the bottom
            val hills = Path().apply {
                moveTo(0f, h * 0.72f)
                quadraticBezierTo(w * 0.3f, h * 0.6f, w * 0.55f, h * 0.7f)
                quadraticBezierTo(w * 0.8f, h * 0.78f, w, h * 0.66f)
                lineTo(w, h)
                lineTo(0f, h)
                close()
            }
            drawPath(hills, Color.Black.copy(alpha = 0.45f))
        }

        Text(
            "CHORUS · PERFORMED",
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
            color = StarWhite.copy(alpha = 0.7f),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 8.dp),
        )
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                title,
                style = MaterialTheme.typography.titleLarge.copy(fontSize = 18.sp, lineHeight = 22.sp),
                color = StarWhite,
                textAlign = TextAlign.Center,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                author,
                style = MaterialTheme.typography.labelSmall,
                color = StarWhite.copy(alpha = 0.7f),
                maxLines = 1,
            )
        }
    }
}