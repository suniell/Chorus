package com.sunil.chorus.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.concurrent.Executors

// ═════════════════════════════════════════════
// LIBRARY STORE: saves your stories + progress to a file on the phone,
// so nothing is lost when the app is closed. No internet needed.
// ═════════════════════════════════════════════
object LibraryStore {

    // Everything we keep on the phone
    data class Saved(
        val stories: List<Story>,          // only the stories YOU added
        val progress: Map<String, Int>,    // story id -> line index
        val lastPlayedId: String?,
    )

    // Writing happens on a helper thread so the app never stutters
    private val writer = Executors.newSingleThreadExecutor()

    private fun file(context: Context) = File(context.filesDir, "library.json")

    fun load(context: Context): Saved? {
        val f = file(context)
        if (!f.exists()) return null
        return try {
            val root = JSONObject(f.readText())
            val storyArray = root.optJSONArray("stories") ?: JSONArray()
            val stories = (0 until storyArray.length()).map { storyFrom(storyArray.getJSONObject(it)) }
            val progressJson = root.optJSONObject("progress") ?: JSONObject()
            val progress = mutableMapOf<String, Int>()
            progressJson.keys().forEach { key -> progress[key] = progressJson.getInt(key) }
            val last = if (root.isNull("lastPlayedId")) null else root.getString("lastPlayedId")
            Saved(stories, progress, last)
        } catch (e: Exception) {
            null   // a broken file: just start fresh
        }
    }

    fun save(context: Context, saved: Saved) {
        val app = context.applicationContext
        val text = JSONObject().apply {
            put("stories", JSONArray().apply { saved.stories.forEach { put(storyTo(it)) } })
            put("progress", JSONObject().apply { saved.progress.forEach { (id, line) -> put(id, line) } })
            put("lastPlayedId", saved.lastPlayedId ?: JSONObject.NULL)
        }.toString()

        writer.execute {
            // Write to a temporary file first, then swap, so a power cut can't corrupt it
            val temp = File(app.filesDir, "library.json.tmp")
            temp.writeText(text)
            temp.renameTo(file(app))
        }
    }

    // ── Story <-> JSON ──

    private fun storyTo(story: Story) = JSONObject().apply {
        put("id", story.id)
        put("title", story.title)
        put("author", story.author)
        put("coverStyle", story.coverStyle)
        put("characters", JSONArray().apply {
            story.characters.forEach { c ->
                put(JSONObject().apply {
                    put("id", c.id)
                    put("name", c.name)
                    put("voice", c.voice)
                    put("pitch", c.pitch.toDouble())
                    put("rate", c.rate.toDouble())
                })
            }
        })
        put("lines", JSONArray().apply {
            story.lines.forEach { l ->
                put(JSONObject().apply {
                    put("s", l.speakerId)
                    put("t", l.text)
                })
            }
        })
    }

    private fun storyFrom(json: JSONObject): Story {
        val chars = json.getJSONArray("characters")
        val lines = json.getJSONArray("lines")
        return Story(
            id = json.getString("id"),
            title = json.getString("title"),
            author = json.getString("author"),
            coverStyle = json.getInt("coverStyle"),
            characters = (0 until chars.length()).map {
                val c = chars.getJSONObject(it)
                Character(
                    id = c.getString("id"),
                    name = c.getString("name"),
                    voice = c.getInt("voice"),
                    pitch = c.getDouble("pitch").toFloat(),
                    rate = c.getDouble("rate").toFloat(),
                )
            },
            lines = (0 until lines.length()).map {
                val l = lines.getJSONObject(it)
                Line(l.getString("s"), l.getString("t"))
            },
            isSample = false,
        )
    }
}
