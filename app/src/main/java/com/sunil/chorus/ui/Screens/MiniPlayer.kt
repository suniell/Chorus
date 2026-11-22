package com.sunil.chorus.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sunil.chorus.player.StoryPlayer
import com.sunil.chorus.ui.components.*
import com.sunil.chorus.ui.theme.*

// ═════════════════════════════════════════════
// MINI PLAYER: small bar at the bottom while a story is loaded
// ═════════════════════════════════════════════
@Composable
fun MiniPlayer(onOpen: (storyId: String) -> Unit, modifier: Modifier = Modifier) {
    val story = StoryPlayer.story ?: return   // nothing loaded = no mini player
    val playing = StoryPlayer.isPlaying
    val line = StoryPlayer.currentLine.coerceIn(0, story.lines.lastIndex)
    val speaker = StoryPlayer.currentSpeaker

    Surface(
        onClick = { onOpen(story.id) },
        shape = RoundedCornerShape(16.dp),
        color = NightCard,
        contentColor = StarWhite,
        border = BorderStroke(1.dp, NightLine),
        shadowElevation = 8.dp,
        modifier = modifier
            .navigationBarsPadding()
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .fillMaxWidth(),
    ) {
        Column {
            Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                BookCover(title = "", author = "", styleIndex = story.coverStyle, modifier = Modifier.width(36.dp))
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        story.title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        if (playing && speaker != null) "${speaker.name} is speaking"
                        else "Paused · Line ${line + 1} of ${story.lines.size}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Mist,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Waveform(
                    playing = playing,
                    bars = 8,
                    modifier = Modifier
                        .width(36.dp)
                        .height(18.dp),
                )
                Spacer(Modifier.width(10.dp))
                PlayButton(isPlaying = playing, onClick = { StoryPlayer.togglePlay() }, buttonSize = 40.dp)
            }
            // Thin progress line along the bottom
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(NightLine)
            ) {
                Box(
                    Modifier
                        .fillMaxHeight()
                        .fillMaxWidth((line + 1f) / story.lines.size)
                        .background(Amber)
                )
            }
        }
    }
}