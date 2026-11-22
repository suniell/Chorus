package com.sunil.chorus.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlin.math.max
import kotlin.math.roundToInt

// The id used for the narrator in every story
const val NARRATOR = "narrator"

// One voice in the story
data class Character(
    val id: String,
    val name: String,
    val voice: Int,           // colour index, and later which voice speaks
    val pitch: Float = 1f,    // 1.0 = normal. Higher = lighter voice, lower = deeper
    val rate: Float = 1f,     // 1.0 = normal speed
)

// One line of the story: who says it, and what they say
data class Line(
    val speakerId: String,
    val text: String,
)

data class Story(
    val id: String,
    val title: String,
    val author: String,
    val coverStyle: Int,      // which painted cover to draw
    val characters: List<Character>,
    val lines: List<Line>,
    val isSample: Boolean = true,   // true = comes with the app, false = added by the user
) {
    // Finds who says a line (falls back to the narrator)
    fun speakerOf(line: Line): Character =
        characters.firstOrNull { it.id == line.speakerId } ?: characters.first { it.id == NARRATOR }

    // Everyone except the narrator, e.g. "Maya · Theo · The Keeper"
    val castNames: String
        get() = characters.filter { it.id != NARRATOR }.joinToString(" · ") { it.name }

    // Rough listening time, in seconds
    fun secondsFrom(lineIndex: Int): Int =
        lines.drop(lineIndex).sumOf { estimateSeconds(it.text) }
}

// About 2.6 words per second of speech
fun estimateSeconds(text: String): Int = max(2, (text.split(" ").size / 2.6).roundToInt())

// 45 -> "45 sec", 130 -> "2 min"
fun formatDuration(seconds: Int): String =
    if (seconds < 60) "$seconds sec" else "${(seconds + 30) / 60} min"

// ═════════════════════════════════════════════
// The ONE shared place that holds all stories and listening progress
// ═════════════════════════════════════════════
object StoryRepository {

    val stories = mutableStateListOf<Story>().apply { addAll(SampleStories.all) }

    // Which line each story is at (story id -> line index). Pretend we stopped Pride and Prejudice at line 4.
    val progress = mutableStateMapOf("pride" to 3)

    // The story shown in "Continue listening"
    var lastPlayedId by mutableStateOf<String?>("pride")

    private var appContext: Context? = null

    // Call once when the app (or the widget) starts: loads what was saved on the phone
    @Synchronized
    fun attach(context: Context) {
        if (appContext != null) return
        appContext = context.applicationContext
        val saved = LibraryStore.load(context) ?: return
        stories.addAll(0, saved.stories.filter { s -> stories.none { it.id == s.id } })
        progress.clear()
        progress.putAll(saved.progress)
        lastPlayedId = saved.lastPlayedId
    }

    fun find(id: String): Story? = stories.firstOrNull { it.id == id }

    fun add(story: Story) {
        stories.add(0, story)
        persist()
    }

    fun saveProgress(storyId: String, lineIndex: Int) {
        progress[storyId] = lineIndex
        lastPlayedId = storyId
        persist()
    }

    // Write everything to the phone
    private fun persist() {
        val context = appContext ?: return
        LibraryStore.save(
            context,
            LibraryStore.Saved(
                stories = stories.filter { !it.isSample },
                progress = progress.toMap(),
                lastPlayedId = lastPlayedId,
            ),
        )
    }
}
