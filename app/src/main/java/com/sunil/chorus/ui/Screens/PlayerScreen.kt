package com.sunil.chorus.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sunil.chorus.data.*
import com.sunil.chorus.player.StoryPlayer
import com.sunil.chorus.ui.components.*
import com.sunil.chorus.ui.theme.*

// ═════════════════════════════════════════════
// PLAYER
// ═════════════════════════════════════════════
@Composable
fun PlayerScreen(storyId: String, onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current

    val story = StoryRepository.find(storyId)
    if (story == null) {
        LaunchedEffect(Unit) { onBack() }
        return
    }

    // Start the voice engine and load this story
    LaunchedEffect(storyId) {
        StoryPlayer.init(context)
        StoryPlayer.load(story)
    }
    // Start playing as soon as the voices are ready
    LaunchedEffect(StoryPlayer.ready, storyId) {
        if (StoryPlayer.ready && StoryPlayer.story?.id == storyId && !StoryPlayer.isPlaying) {
            StoryPlayer.play()
        }
    }

    // Only trust the player's state once it has loaded THIS story
    val isThisStory = StoryPlayer.story?.id == storyId
    val current = if (isThisStory) StoryPlayer.currentLine else (StoryRepository.progress[storyId] ?: 0)
    val playing = isThisStory && StoryPlayer.isPlaying
    val speaker = story.speakerOf(story.lines[current.coerceIn(0, story.lines.lastIndex)])

    // Keep the current line in view
    val listState = rememberLazyListState()
    LaunchedEffect(current) {
        listState.animateScrollToItem((current - 1).coerceAtLeast(0))
    }

    Box(Modifier.fillMaxSize()) {
        NightSky()

        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // ── Top bar ──
            Row(Modifier.padding(start = 8.dp, end = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                BackArrow(onClick = onBack)
                Spacer(Modifier.weight(1f))
                Text("NOW PERFORMING", style = MaterialTheme.typography.labelSmall, color = Gold)
            }

            // ── Cover + title + cast ──
            Row(Modifier.padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                BookCover(title = "", author = "", styleIndex = story.coverStyle, modifier = Modifier.width(76.dp))
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        story.title,
                        style = MaterialTheme.typography.headlineMedium,
                        color = StarWhite,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(story.author, style = MaterialTheme.typography.bodyMedium, color = Mist)
                }
            }
            Spacer(Modifier.height(12.dp))

            // Cast chips: the one speaking now lights up
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(story.characters, key = { it.id }) { character ->
                    CastChip(character, speaking = playing && character.id == speaker.id)
                }
            }
            Spacer(Modifier.height(12.dp))

            // ── The paper script ──
            PaperCard(
                Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth()
            ) {
                LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
                    itemsIndexed(story.lines) { index, line ->
                        val who = story.speakerOf(line)
                        val isNarration = who.id == NARRATOR
                        ScriptLine(
                            speaker = who.name,
                            text = line.text,
                            speakerColor = if (isNarration) InkSoft else voiceColor(who.voice, onPaper = true),
                            isNarration = isNarration,
                            highlighted = index == current,
                            onClick = {
                                StoryPlayer.load(story)
                                StoryPlayer.playFrom(index)
                            },
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))

            // ── Progress ──
            Column(Modifier.padding(horizontal = 20.dp)) {
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
                            .fillMaxWidth((current + 1f) / story.lines.size)
                            .background(Amber)
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    when {
                        StoryPlayer.failed -> "This phone has no text-to-speech engine. Install \"Speech Services by Google\" from the Play Store."
                        !StoryPlayer.ready -> "Warming up the voices…"
                        else -> "Line ${current + 1} of ${story.lines.size} · ${formatDuration(story.secondsFrom(current))} left"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = Mist,
                )
            }
            Spacer(Modifier.height(8.dp))

            // ── Controls ──
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SmallChip(text = speedLabel(StoryPlayer.speed), onClick = { StoryPlayer.cycleSpeed() })
                RoundIconButton(onClick = {
                    StoryPlayer.load(story)
                    StoryPlayer.skip(-1)
                }) { SkipGlyph(forward = false) }

                PlayButton(
                    isPlaying = playing,
                    buttonSize = 68.dp,
                    onClick = {
                        StoryPlayer.load(story)
                        StoryPlayer.togglePlay()
                    },
                )

                RoundIconButton(onClick = {
                    StoryPlayer.load(story)
                    StoryPlayer.skip(1)
                }) { SkipGlyph(forward = true) }
                SmallChip(text = "☾ ${StoryPlayer.sleepLabel}", onClick = { StoryPlayer.cycleSleepTimer() })
            }

            // Honest note if the phone only has one voice
            if (StoryPlayer.ready && StoryPlayer.voiceCount <= 1) {
                Text(
                    "Only one voice is installed, so characters differ by pitch. Add voices in Settings › Text-to-speech.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Mist,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 4.dp),
                )
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

// "1x", "1.25x"
private fun speedLabel(speed: Float): String =
    if (speed % 1f == 0f) "${speed.toInt()}x" else "${speed}x"

// ─────────────────────────────────────────────
// Small pieces used only on this screen
// ─────────────────────────────────────────────
@Composable
private fun CastChip(character: Character, speaking: Boolean) {
    val color = if (character.id == NARRATOR) Gold else voiceColor(character.voice, onPaper = false)
    Row(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = if (speaking) 0.3f else 0.1f))
            .border(
                BorderStroke(1.5.dp, if (speaking) color else Color.Transparent),
                RoundedCornerShape(50),
            )
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            character.name,
            style = MaterialTheme.typography.labelMedium,
            color = if (speaking) StarWhite else Mist,
        )
    }
}

@Composable
private fun SmallChip(text: String, onClick: () -> Unit) {
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        color = StarWhite,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(NightCard)
            .border(1.dp, NightLine, RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    )
}

@Composable
private fun RoundIconButton(onClick: () -> Unit, content: @Composable () -> Unit) {
    Box(
        Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(NightCard)
            .clickable(onClick = onClick)
            .padding(15.dp),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

// ⏮ / ⏭ drawn by hand: a triangle plus a bar
@Composable
private fun SkipGlyph(forward: Boolean) {
    Canvas(Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val barWidth = w * 0.16f
        val triangle = Path().apply {
            if (forward) {
                moveTo(0f, 0f)
                lineTo(w - barWidth, h / 2f)
                lineTo(0f, h)
            } else {
                moveTo(w, 0f)
                lineTo(barWidth, h / 2f)
                lineTo(w, h)
            }
            close()
        }
        drawPath(triangle, StarWhite)
        drawRect(
            StarWhite,
            topLeft = Offset(if (forward) w - barWidth else 0f, 0f),
            size = Size(barWidth, h),
        )
    }
}