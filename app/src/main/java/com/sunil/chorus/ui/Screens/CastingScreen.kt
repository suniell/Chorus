package com.sunil.chorus.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.sunil.chorus.data.*
import com.sunil.chorus.ui.components.*
import com.sunil.chorus.ui.theme.*
import kotlinx.coroutines.delay

// ═════════════════════════════════════════════
// CASTING: shows the steps, then the cast Chorus found
// ═════════════════════════════════════════════
@Composable
fun CastingScreen(onBack: () -> Unit, onStart: (storyId: String) -> Unit) {
    BackHandler(onBack = onBack)

    val draft = DraftStore.draft
    if (draft == null) {
        LaunchedEffect(Unit) { onBack() }
        return
    }

    // The cast can be changed here (tap a voice), so keep our own copy
    val cast = remember { mutableStateListOf<Character>().apply { addAll(draft.characters) } }

    // 0 = reading, 1 = finding parts, 2 = casting, 3 = done
    var step by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        repeat(3) {
            delay(850)
            step++
        }
    }

    val steps = listOf(
        "Reading your story",
        "Finding every speaking part",
        "Casting a voice for each character",
    )

    Box(Modifier.fillMaxSize()) {
        NightSky()

        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(bottom = 24.dp)
        ) {
            BackArrow(onClick = onBack, modifier = Modifier.padding(start = 8.dp))

            Column(Modifier.padding(horizontal = 20.dp)) {
                Text(draft.title, style = MaterialTheme.typography.headlineLarge, color = StarWhite)
                Spacer(Modifier.height(16.dp))

                // ── The 3 steps ──
                steps.forEachIndexed { index, label ->
                    StepRow(label = label, done = step > index, active = step == index)
                }

                // ── The result appears when all steps are done ──
                AnimatedVisibility(visible = step >= 3, enter = fadeIn() + expandVertically()) {
                    Column {
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "${draft.lines.size} lines · ${cast.size - 1} characters + narrator · about ${formatDuration(draft.secondsFrom(0))}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Mist,
                        )
                        Spacer(Modifier.height(16.dp))
                        SparkleDivider()
                        Spacer(Modifier.height(16.dp))

                        Text("THE CAST", style = MaterialTheme.typography.labelMedium, color = Gold)
                        Text("Tap a voice to change it.", style = MaterialTheme.typography.bodyMedium, color = Mist)
                        Spacer(Modifier.height(12.dp))

                        cast.forEachIndexed { index, character ->
                            CastRow(
                                character = character,
                                lineCount = draft.lines.count { it.speakerId == character.id },
                                onCycleVoice = {
                                    val next = (character.voice + 1) % 5
                                    cast[index] = character.copy(
                                        voice = next,
                                        pitch = VoicePitch[next],
                                        rate = VoiceRate[next],
                                    )
                                },
                            )
                            Spacer(Modifier.height(8.dp))
                        }

                        // ── Script preview ──
                        Spacer(Modifier.height(12.dp))
                        PaperCard(Modifier.fillMaxWidth()) {
                            Text("PREVIEW", style = MaterialTheme.typography.labelSmall, color = InkSoft)
                            Spacer(Modifier.height(6.dp))
                            draft.lines.take(4).forEachIndexed { i, line ->
                                val who = cast.firstOrNull { it.id == line.speakerId } ?: cast.first()
                                val isNarration = who.id == NARRATOR
                                ScriptLine(
                                    speaker = who.name,
                                    text = line.text,
                                    speakerColor = if (isNarration) InkSoft else voiceColor(who.voice, onPaper = true),
                                    isNarration = isNarration,
                                    highlighted = i == 0,
                                    onClick = {},
                                )
                            }
                        }

                        Spacer(Modifier.height(20.dp))
                        AmberButton(
                            text = "Start listening",
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                val finished = draft.copy(characters = cast.toList())
                                StoryRepository.add(finished)
                                StoryRepository.saveProgress(finished.id, 0)
                                // Don't clear the draft here: this screen is still visible while it
                                // slides away, and an empty draft would send us back to "Bring a story"
                                onStart(finished.id)
                            },
                        )
                        Spacer(Modifier.height(10.dp))
                        GhostButton(
                            text = "Edit the text",
                            onClick = onBack,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }
    }
}

// One step: ✦ when done, spinner while working, dot while waiting
@Composable
private fun StepRow(label: String, done: Boolean, active: Boolean) {
    Row(
        Modifier.padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(20.dp), contentAlignment = Alignment.Center) {
            when {
                done -> Sparkle(sparkleSize = 16.dp)
                active -> CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    color = Amber,
                    strokeWidth = 2.dp,
                )
                else -> Box(
                    Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(Mist)
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Text(
            label,
            style = MaterialTheme.typography.bodyLarge,
            color = if (done || active) StarWhite else Mist,
        )
    }
}

// One cast member: coloured circle, name, line count, voice chip
@Composable
private fun CastRow(character: Character, lineCount: Int, onCycleVoice: () -> Unit) {
    val isNarrator = character.id == NARRATOR
    val color = if (isNarrator) Gold else voiceColor(character.voice, onPaper = false)

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = NightCard.copy(alpha = 0.9f),
        contentColor = StarWhite,
        border = BorderStroke(1.dp, NightLine),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(color),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    character.name.take(1).uppercase(),
                    style = MaterialTheme.typography.titleLarge,
                    color = MidnightDeep,
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(character.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    if (lineCount == 1) "1 line" else "$lineCount lines",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Mist,
                )
            }
            Text(
                if (isNarrator) "Narrator voice" else "Voice ${character.voice + 1} · ${pitchLabel(character.pitch)}",
                style = MaterialTheme.typography.labelMedium,
                color = color,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(color.copy(alpha = 0.14f))
                    .then(if (isNarrator) Modifier else Modifier.clickable(onClick = onCycleVoice))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            )
        }
    }
}