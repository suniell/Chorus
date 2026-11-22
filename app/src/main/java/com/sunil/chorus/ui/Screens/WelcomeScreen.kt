package com.sunil.chorus.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sunil.chorus.ui.components.*
import com.sunil.chorus.ui.theme.*
import kotlinx.coroutines.delay

// The little "performance" shown on the welcome screen
private data class DemoLine(val speaker: String, val quote: String, val colorIndex: Int)

private val demoLines = listOf(
    DemoLine("Rosa", "We're closed. Come back when the sun is up.", 0),
    DemoLine("Felix", "I'm not here for bread. I'm here for the recipe.", 1),
    DemoLine("Narrator", "Outside, the snow kept falling on the quiet street.", 5),
)

// ═════════════════════════════════════════════
// WELCOME SCREEN: shown only the very first time
// ═════════════════════════════════════════════
@Composable
fun WelcomeScreen(onStart: () -> Unit) {
    // Reveal things one by one: 1, 2, 3 = quote cards, 4 = features + button
    var step by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        repeat(4) {
            delay(450)
            step++
        }
    }

    // Which card is "speaking" right now (moves along like a real cast)
    var speaking by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        delay(1800)
        while (true) {
            delay(1700)
            speaking = (speaking + 1) % demoLines.size
        }
    }

    Box(Modifier.fillMaxSize()) {
        NightSky(starCount = 120)

        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
        ) {
            Spacer(Modifier.height(40.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Sparkle(sparkleSize = 22.dp)
                Spacer(Modifier.width(10.dp))
                Text("chorus", style = MaterialTheme.typography.displayLarge, color = StarWhite)
            }
            Spacer(Modifier.height(10.dp))
            Text(
                "Every story,\nperformed by a full cast.",
                style = MaterialTheme.typography.headlineMedium,
                color = StarWhite,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Paste a link, a chapter or a script. Chorus gives every character their own voice.",
                style = MaterialTheme.typography.bodyLarge,
                color = Mist,
            )
            Spacer(Modifier.height(24.dp))

            // ── The cast, appearing one by one ──
            demoLines.forEachIndexed { i, line ->
                AnimatedVisibility(
                    visible = step > i,
                    enter = fadeIn(tween(500)) + slideInVertically(tween(500)) { it / 3 },
                ) {
                    QuoteCard(
                        speaker = line.speaker,
                        quote = line.quote,
                        color = voiceColor(line.colorIndex, onPaper = false),
                        playing = step > 3 && speaking == i,
                        onClick = { speaking = i },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp),
                    )
                }
            }

            // ── Features + start button ──
            AnimatedVisibility(visible = step > 3, enter = fadeIn(tween(600))) {
                Column {
                    Spacer(Modifier.height(14.dp))
                    FeatureRow("☾", "Works offline", "The voices live on your phone")
                    FeatureRow("▶", "Plays when locked", "Controls on your lock screen")
                    FeatureRow("✦", "Any story you love", "Links, text files and scripts")
                    Spacer(Modifier.height(24.dp))
                    AmberButton(
                        text = "Start listening",
                        onClick = onStart,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "No account needed",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Mist,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
private fun FeatureRow(icon: String, title: String, subtitle: String) {
    Row(
        Modifier.padding(vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Amber.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(icon, color = Gold, style = MaterialTheme.typography.titleMedium)
        }
        Spacer(Modifier.width(14.dp))
        Column {
            Text(title, style = MaterialTheme.typography.titleMedium, color = StarWhite)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = Mist)
        }
    }
}
