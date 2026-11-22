package com.sunil.chorus.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sunil.chorus.data.*
import com.sunil.chorus.ui.components.*
import com.sunil.chorus.ui.theme.*
import java.util.Calendar

// ═════════════════════════════════════════════
// HOME SCREEN
// ═════════════════════════════════════════════
@Composable
fun HomeScreen(
    onOpenStory: (String) -> Unit,
    onAddStory: () -> Unit,
    onOpenPlus: () -> Unit = {},
) {
    val lastStory = StoryRepository.lastPlayedId?.let { StoryRepository.find(it) }
    val yourStories = StoryRepository.stories.filter { !it.isSample }
    val samples = StoryRepository.stories.filter { it.isSample }

    Box(Modifier.fillMaxSize()) {
        NightSky()

        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(bottom = 110.dp)
        ) {
            // ── Wordmark ──
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Sparkle(sparkleSize = 16.dp)
                Spacer(Modifier.width(8.dp))
                Text("chorus", style = MaterialTheme.typography.headlineMedium, color = StarWhite)
                Spacer(Modifier.weight(1f))
                PlusChip(onClick = onOpenPlus)
            }

            // ── Greeting ──
            Text(
                greeting(),
                style = MaterialTheme.typography.headlineLarge,
                color = StarWhite,
                modifier = Modifier.padding(horizontal = 20.dp),
            )
            Text(
                "What should we perform tonight?",
                style = MaterialTheme.typography.bodyLarge,
                color = Mist,
                modifier = Modifier.padding(horizontal = 20.dp),
            )
            Spacer(Modifier.height(20.dp))

            // ── Continue listening ──
            if (lastStory != null) {
                ContinueCard(
                    story = lastStory,
                    lineIndex = StoryRepository.progress[lastStory.id] ?: 0,
                    onOpen = { onOpenStory(lastStory.id) },
                )
                Spacer(Modifier.height(16.dp))
            }

            // ── Bring a story ──
            AddStoryCard(onClick = onAddStory)
            Spacer(Modifier.height(28.dp))

            // ── Your stories (only when there are some) ──
            if (yourStories.isNotEmpty()) {
                SectionTitle(
                    "Your stories",
                    if (AppPrefs.plus) "Performed from your links and files"
                    else "${yourStories.size} of ${AppPrefs.FREE_STORIES} free stories used",
                )
                Spacer(Modifier.height(12.dp))
                CoverRow(yourStories, onOpenStory)
                Spacer(Modifier.height(28.dp))
            }

            SparkleDivider(Modifier.padding(horizontal = 20.dp))
            Spacer(Modifier.height(28.dp))

            // ── Samples ──
            SectionTitle("Take a listen", "Every character, a different voice")
            Spacer(Modifier.height(12.dp))
            CoverRow(samples, onOpenStory)
        }
    }
}

// "Good morning" / "Good afternoon" / "Good evening"
private fun greeting(): String {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return when (hour) {
        in 5..11 -> "Good morning"
        in 12..16 -> "Good afternoon"
        else -> "Good evening"
    }
}

@Composable
private fun SectionTitle(title: String, subtitle: String) {
    Column(Modifier.padding(horizontal = 20.dp)) {
        Text(title.uppercase(), style = MaterialTheme.typography.labelMedium, color = Gold)
        Spacer(Modifier.height(2.dp))
        Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = Mist)
    }
}

// ─────────────────────────────────────────────
// Continue listening card
// ─────────────────────────────────────────────
@Composable
private fun ContinueCard(story: Story, lineIndex: Int, onOpen: () -> Unit) {
    val total = story.lines.size
    val fraction = (lineIndex + 1f) / total
    val timeLeft = formatDuration(story.secondsFrom(lineIndex))

    Surface(
        onClick = onOpen,
        shape = RoundedCornerShape(18.dp),
        color = NightCard.copy(alpha = 0.9f),
        contentColor = StarWhite,
        border = BorderStroke(1.dp, NightLine),
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth(),
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            BookCover(
                title = "",
                author = "",
                styleIndex = story.coverStyle,
                modifier = Modifier.width(64.dp),
            )
            Spacer(Modifier.width(14.dp))

            Column(Modifier.weight(1f)) {
                Text("CONTINUE LISTENING", style = MaterialTheme.typography.labelSmall, color = Gold)
                Spacer(Modifier.height(4.dp))
                Text(
                    story.title,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    "Line ${lineIndex + 1} of $total · $timeLeft left",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Mist,
                )
                Spacer(Modifier.height(10.dp))
                // Progress bar
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(NightLine)
                ) {
                    Box(
                        Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(fraction)
                            .background(Amber)
                    )
                }
            }
            Spacer(Modifier.width(14.dp))
            PlayButton(isPlaying = false, onClick = onOpen, buttonSize = 48.dp)
        }
    }
}

// ─────────────────────────────────────────────
// "Bring a story" card with a dashed border
// ─────────────────────────────────────────────
@Composable
private fun AddStoryCard(onClick: () -> Unit) {
    Column(
        Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(NightCard.copy(alpha = 0.55f))
            .drawBehind {
                // Dashed outline: 10px line, 8px gap
                drawRoundRect(
                    color = Amber.copy(alpha = 0.6f),
                    cornerRadius = CornerRadius(18.dp.toPx()),
                    style = Stroke(
                        width = 1.5.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10.dp.toPx(), 8.dp.toPx())),
                    ),
                )
            }
            .clickable(onClick = onClick)
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Sparkle(sparkleSize = 18.dp)
        Spacer(Modifier.height(8.dp))
        Text("Bring a story", style = MaterialTheme.typography.headlineMedium, color = StarWhite)
        Spacer(Modifier.height(4.dp))
        Text(
            "Paste a link, upload a file, or share from any app.",
            style = MaterialTheme.typography.bodyMedium,
            color = Mist,
        )
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Link", "File", "Text").forEach { label ->
                Text(
                    label,
                    style = MaterialTheme.typography.labelMedium,
                    color = Amber,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Amber.copy(alpha = 0.12f))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                )
            }
        }
    }
}

// ─────────────────────────────────────────────
// Row of covers with the cast under each
// ─────────────────────────────────────────────
@Composable
private fun CoverRow(stories: List<Story>, onOpenStory: (String) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        items(stories, key = { it.id }) { story ->
            Column(Modifier.width(132.dp)) {
                BookCover(
                    title = story.title,
                    author = story.author,
                    styleIndex = story.coverStyle,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onOpenStory(story.id) },
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    story.castNames,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Mist,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
// Small "✦ Get Plus" pill in the top-right corner
@Composable
private fun PlusChip(onClick: () -> Unit) {
    val plus = AppPrefs.plus
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        color = if (plus) Amber.copy(alpha = 0.15f) else NightCard,
        border = BorderStroke(1.dp, if (plus) Amber else NightLine),
    ) {
        Text(
            if (plus) "✦ Plus" else "✦ Get Plus",
            style = MaterialTheme.typography.labelMedium,
            color = Gold,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
        )
    }
}
