package com.sunil.chorus.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

// Holds the story between the "Bring a story" screen and the "Casting" screen
object DraftStore {
    var draft by mutableStateOf<Story?>(null)
}

// Pitch and speed for each of the 5 character voices (index = voice number)
val VoicePitch = listOf(1.15f, 0.85f, 1.3f, 0.75f, 1.0f)
val VoiceRate = listOf(1.0f, 0.95f, 1.1f, 0.9f, 1.05f)

// "Lighter", "Natural" or "Deeper"
fun pitchLabel(pitch: Float): String = when {
    pitch >= 1.1f -> "Lighter"
    pitch <= 0.9f -> "Deeper"
    else -> "Natural"
}

// ═════════════════════════════════════════════
// STORY PARSER: turns plain text into characters + lines
// ═════════════════════════════════════════════
object StoryParser {

    // Words that mean someone is speaking
    private const val VERBS =
        "said|says|asked|replied|whispered|shouted|cried|answered|called|muttered|added|snapped|yelled|exclaimed|continued|began"

    // "MAYA: Then who turned on the light?"
    private val scriptLine = Regex("""^([A-Z][A-Za-z.'\- ]{0,24}):\s+(.+)$""")

    // Anything inside "double quotes"
    private val quoted = Regex("\"([^\"]+)\"")

    // "said Rosa"  /  "asked Felix Grey"
    private val verbThenName = Regex("""\b(?:$VERBS)\s+([A-Z][a-z]+(?:\s[A-Z][a-z]+)?)""")

    // "Rosa said"  /  "Priya replied"
    private val nameThenVerb = Regex("""\b([A-Z][a-z]+(?:\s[A-Z][a-z]+)?)\s+(?:$VERBS)\b""")

    // Removes "said Rosa", "Felix asked", "he replied", "the stranger said" from narration
    private val attribution = Regex(
        """^(?:(?:$VERBS)\s+\w+(?:\s[A-Z]\w+)?|\w+\s+(?:$VERBS)|the\s+\w+\s+(?:$VERBS))\b[,.]?\s*""",
        RegexOption.IGNORE_CASE,
    )

    // Capitalised words that are NOT names
    private val notNames = setOf(
        "He", "She", "They", "It", "I", "We", "You", "The", "Then", "And", "But",
        "Someone", "Nobody", "Everyone", "His", "Her", "That", "This",
    )

    fun parse(raw: String, fallbackTitle: String = "Untitled story"): Story {
        // Turn curly quotes into straight ones so one rule handles both
        val text = raw
            .replace('\u201C', '"').replace('\u201D', '"')
            .replace('\u2018', '\'').replace('\u2019', '\'')
            .trim()

        val paragraphs = text.split(Regex("\\n+")).map { it.trim() }.filter { it.isNotEmpty() }

        // Title = first line, if it looks like a title (short, no quotes, no full stop)
        var title = fallbackTitle
        var body = paragraphs
        val first = paragraphs.firstOrNull()
        if (first != null && paragraphs.size > 1 && first.length <= 60 &&
            !first.contains('"') && !first.endsWith(".") && !scriptLine.matches(first)
        ) {
            title = first
            body = paragraphs.drop(1)
        }

        // Is this written like a play script ("NAME: line")?
        val scriptMode = body.count { scriptLine.matches(it) } >= maxOf(2, body.size / 2)

        val result = mutableListOf<Pair<String, String>>()   // (speaker name or NARRATOR, text)
        var last: String? = null
        var beforeLast: String? = null

        fun spoke(name: String) {
            if (name != last) {
                beforeLast = last
                last = name
            }
        }

        fun addNarration(fragment: String) {
            var clean = fragment.trim().trimStart(',', '.', ';', ':', '-', ' ')
            clean = attribution.replace(clean, "").trim()
            if (clean.split(" ").count { it.isNotBlank() } < 4) return   // too short to be worth reading
            result += NARRATOR to clean.replaceFirstChar { it.uppercase() }
        }

        for (paragraph in body) {
            // ── Script format ──
            if (scriptMode) {
                val match = scriptLine.matchEntire(paragraph)
                if (match != null) {
                    val name = match.groupValues[1].trim().lowercase()
                        .split(" ").joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
                    result += name to match.groupValues[2].trim()
                    spoke(name)
                } else {
                    result += NARRATOR to paragraph
                }
                continue
            }

            // ── Normal prose ──
            val quotes = quoted.findAll(paragraph).toList()
            if (quotes.isEmpty()) {
                result += NARRATOR to paragraph
                continue
            }

            // Who is speaking in this paragraph? Look at the words outside the quotes.
            val outside = quoted.replace(paragraph, " | ")
            val speaker = findSpeaker(outside) ?: (beforeLast ?: "Someone")

            var cursor = 0
            for (q in quotes) {
                addNarration(paragraph.substring(cursor, q.range.first))
                result += speaker to cleanDialogue(q.groupValues[1])
                cursor = q.range.last + 1
            }
            addNarration(paragraph.substring(cursor))
            spoke(speaker)
        }

        // ── Build the cast, in order of first appearance ──
        val names = result.map { it.first }.filter { it != NARRATOR }.distinct()
        val idByName = names.mapIndexed { i, name -> name to "c$i" }.toMap()

        val characters = listOf(Character(NARRATOR, "Narrator", voice = 5)) +
                names.mapIndexed { i, name ->
                    val voice = i % 5
                    Character("c$i", name, voice = voice, pitch = VoicePitch[voice], rate = VoiceRate[voice])
                }

        val lines = result.map { (who, line) ->
            Line(if (who == NARRATOR) NARRATOR else idByName.getValue(who), line)
        }

        return Story(
            id = "user_${System.currentTimeMillis()}",
            title = title,
            author = "Added by you",
            coverStyle = Math.floorMod(title.hashCode(), 5),
            characters = characters,
            lines = lines,
            isSample = false,
        )
    }

    private fun findSpeaker(outsideText: String): String? {
        verbThenName.find(outsideText)?.groupValues?.get(1)
            ?.takeIf { it.substringBefore(" ") !in notNames }?.let { return it }
        nameThenVerb.find(outsideText)?.groupValues?.get(1)
            ?.takeIf { it.substringBefore(" ") !in notNames }?.let { return it }
        return null
    }

    // "We're closed," -> "We're closed."
    private fun cleanDialogue(line: String): String {
        val trimmed = line.trim().trimEnd(',', ' ')
        return if (trimmed.isNotEmpty() && trimmed.last() in ".!?…") trimmed else "$trimmed."
    }

    // Used by the "Try an example" button
    val EXAMPLE_TEXT = """
        The Midnight Bakery

        The bell above the door rang at exactly midnight.
        "We're closed," said Rosa, without looking up from the dough.
        "I'm not here for bread," said Felix. "I'm here for the recipe."
        Rosa finally looked up. Snow was melting on his coat.
        "Which recipe?" asked Rosa.
        "The one your grandmother never wrote down," said Felix.
        "Then you already know I can't give it to you."
        Felix smiled and set a small brass key on the counter.
    """.trimIndent()

    // Used for links in this prototype (a real app would download the chapter)
    val DEMO_LINK_TEXT = """
        The Sleepover

        "Did you hear that?" whispered Priya.
        "It's an old house," said Leo. "Old houses make noises."
        "That noise said my name," Priya replied.
        Leo stopped on the stairs. The flashlight flickered twice, then went out.
        "Okay," said Ben from the doorway. "I'm waiting in the car."
    """.trimIndent()
}