package com.sunil.chorus.widget

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.ActionParameters
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.sunil.chorus.MainActivity
import com.sunil.chorus.data.StoryRepository
import com.sunil.chorus.data.formatDuration
import com.sunil.chorus.player.StoryPlayer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// Widgets use their own colour type. Same colour for light and dark mode.
private fun fixed(color: Color) = ColorProvider(day = color, night = color)

private val WidgetBg = fixed(Color(0xFF141B38))
private val WidgetWhite = fixed(Color(0xFFEEF0FA))
private val WidgetMist = fixed(Color(0xFF8C93B5))
private val WidgetAmber = fixed(Color(0xFFE2A564))
private val WidgetInk = fixed(Color(0xFF0B1026))

// ═════════════════════════════════════════════
// "CONTINUE LISTENING" WIDGET for the home screen
// ═════════════════════════════════════════════
class ChorusWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        // The widget can wake up before the app: load the saved library first
        withContext(Dispatchers.Main) { StoryRepository.attach(context) }
        provideContent { WidgetContent(context) }
    }
}

// Android finds the widget through this receiver (listed in the manifest)
class ChorusWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ChorusWidget()
}

// Tapping play/pause on the widget while the story is loaded
class TogglePlayAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        withContext(Dispatchers.Main) { StoryPlayer.togglePlay() }
    }
}

// Opens the app straight into the player for this story
private fun openIntent(context: Context, storyId: String?): Intent =
    Intent(context, MainActivity::class.java)
        .putExtra(MainActivity.EXTRA_OPEN_STORY, storyId)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)

@Composable
private fun WidgetContent(context: Context) {
    val story = StoryRepository.lastPlayedId?.let { StoryRepository.find(it) }

    // "live" = this story is loaded in the player right now, so play/pause can work directly
    val live = story != null && StoryPlayer.ready && StoryPlayer.story?.id == story.id
    val playing = live && StoryPlayer.isPlaying
    val open = actionStartActivity(openIntent(context, story?.id))

    Row(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(WidgetBg)
            .cornerRadius(24.dp)
            .padding(14.dp)
            .clickable(open),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (story == null) {
            Column {
                Text("✦ Chorus", style = TextStyle(color = WidgetAmber, fontSize = 16.sp, fontWeight = FontWeight.Bold))
                Text("Pick a story to start listening", style = TextStyle(color = WidgetMist, fontSize = 13.sp))
            }
        } else {
            val line = (if (live) StoryPlayer.currentLine else StoryRepository.progress[story.id] ?: 0)
                .coerceIn(0, story.lines.lastIndex)
            val subtitle = if (playing) {
                "${story.speakerOf(story.lines[line]).name} is speaking"
            } else {
                "Line ${line + 1} of ${story.lines.size} · ${formatDuration(story.secondsFrom(line))} left"
            }

            Image(
                provider = ImageProvider(drawCover(story.coverStyle)),
                contentDescription = null,
                modifier = GlanceModifier.size(60.dp).cornerRadius(12.dp),
            )
            Spacer(GlanceModifier.width(12.dp))
            Column(GlanceModifier.defaultWeight()) {
                Text(
                    "CONTINUE LISTENING",
                    style = TextStyle(color = WidgetAmber, fontSize = 10.sp, fontWeight = FontWeight.Bold),
                )
                Text(
                    story.title,
                    style = TextStyle(color = WidgetWhite, fontSize = 16.sp, fontWeight = FontWeight.Bold),
                    maxLines = 1,
                )
                Text(subtitle, style = TextStyle(color = WidgetMist, fontSize = 12.sp), maxLines = 1)
            }
            Spacer(GlanceModifier.width(8.dp))
            // Amber play button: toggles if loaded, otherwise opens the player (which auto-plays)
            Box(
                modifier = GlanceModifier
                    .size(48.dp)
                    .cornerRadius(24.dp)
                    .background(WidgetAmber)
                    .clickable(if (live) actionRunCallback<TogglePlayAction>() else open),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    if (playing) "❚❚" else "▶",
                    style = TextStyle(color = WidgetInk, fontSize = 18.sp, fontWeight = FontWeight.Bold),
                )
            }
        }
    }
}

// Same painted cover as the lock screen: night gradient + moon
private val coverColors = listOf(
    0xFF2B3A6B to 0xFFF2C879,
    0xFF6B2B3A to 0xFFF4AE86,
    0xFF2B5B4E to 0xFFBFE3C0,
    0xFF4E3B7A to 0xFFC3AEF2,
    0xFF7A5A2B to 0xFFF2C879,
)

private fun drawCover(style: Int): Bitmap {
    val size = 160
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val (sky, moon) = coverColors[Math.floorMod(style, coverColors.size)]
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    paint.shader = LinearGradient(0f, 0f, 0f, size.toFloat(), sky.toInt(), 0xFF0B1026.toInt(), Shader.TileMode.CLAMP)
    canvas.drawRect(0f, 0f, size.toFloat(), size.toFloat(), paint)
    paint.shader = null
    paint.color = moon.toInt()
    canvas.drawCircle(size * 0.68f, size * 0.32f, size * 0.14f, paint)
    return bitmap
}
