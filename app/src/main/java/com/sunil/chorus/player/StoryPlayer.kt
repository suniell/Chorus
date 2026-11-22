package com.sunil.chorus.player

import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.sunil.chorus.data.Character
import com.sunil.chorus.data.Story
import com.sunil.chorus.data.StoryRepository
import java.util.Locale

// ═════════════════════════════════════════════
// STORY PLAYER: reads a story aloud, one line at a time,
// switching voice / pitch / speed for every character.
// ═════════════════════════════════════════════
object StoryPlayer {

    private var appContext: Context? = null
    private var tts: TextToSpeech? = null
    private val mainThread = Handler(Looper.getMainLooper())
    private var englishVoices: List<Voice> = emptyList()

    private var session = 0
    private var reachedEnd = false

    // ── State the screens can read ──
    var ready by mutableStateOf(false)
        private set
    var failed by mutableStateOf(false)
        private set
    var story by mutableStateOf<Story?>(null)
        private set
    var currentLine by mutableIntStateOf(0)
        private set
    var isPlaying by mutableStateOf(false)
        private set
    var speed by mutableFloatStateOf(1f)
        private set
    var sleepLabel by mutableStateOf("Off")
        private set
    var voiceCount by mutableIntStateOf(0)
        private set

    // Who is speaking the current line
    val currentSpeaker: Character?
        get() = story?.let { s -> s.speakerOf(s.lines[currentLine.coerceIn(0, s.lines.lastIndex)]) }

    // ── Listeners: the background service listens here to update the notification ──
    private val listeners = mutableSetOf<() -> Unit>()
    fun addListener(listener: () -> Unit) { listeners += listener }
    fun removeListener(listener: () -> Unit) { listeners -= listener }
    private fun changed() { listeners.toList().forEach { it() } }

    // Speech should use the MEDIA volume, like music
    private val speechAttributes: AudioAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_MEDIA)
        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
        .build()

    // ── Start the engine (only once) ──
    fun init(context: Context) {
        appContext = context.applicationContext
        if (tts != null) return
        tts = TextToSpeech(context.applicationContext) { status ->
            mainThread.post {
                if (status == TextToSpeech.SUCCESS) {
                    tts?.language = Locale.US
                    tts?.setAudioAttributes(speechAttributes)
                    englishVoices = try {
                        tts?.voices.orEmpty()
                            .filter {
                                it.locale.language == "en" &&
                                    !it.isNetworkConnectionRequired &&
                                    TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED !in it.features
                            }
                            .sortedWith(compareBy({ it.locale.country != "US" }, { it.name }))
                    } catch (e: Exception) {
                        emptyList()
                    }
                    voiceCount = englishVoices.size
                    tts?.setOnUtteranceProgressListener(listener)
                    ready = true
                } else {
                    failed = true
                }
            }
        }
    }

    // ── Controls ──

    fun load(newStory: Story) {
        if (story?.id == newStory.id) return
        stopSpeaking()
        isPlaying = false
        story = newStory
        reachedEnd = false
        currentLine = (StoryRepository.progress[newStory.id] ?: 0).coerceIn(0, newStory.lines.lastIndex)
        changed()
    }

    fun play() {
        val s = story ?: return
        val context = appContext ?: return
        if (!ready) return
        if (reachedEnd) {
            currentLine = 0
            reachedEnd = false
        }
        requestAudioFocus(context)
        isPlaying = true
        StoryRepository.saveProgress(s.id, currentLine)
        speakCurrentLine()

        // Start the background service (it shows the notification and lock-screen controls)
        if (!PlaybackService.running) {
            context.startForegroundService(Intent(context, PlaybackService::class.java))
        }
        changed()
    }

    fun pause() {
        isPlaying = false
        stopSpeaking()
        changed()
    }

    fun togglePlay() {
        if (isPlaying) pause() else play()
    }

    // Stop completely: the notification and mini player disappear
    fun close() {
        isPlaying = false
        stopSpeaking()
        story = null
        abandonAudioFocus()
        changed()
    }

    fun playFrom(index: Int) {
        val s = story ?: return
        stopSpeaking()
        currentLine = index.coerceIn(0, s.lines.lastIndex)
        reachedEnd = false
        play()
    }

    fun skip(delta: Int) {
        val s = story ?: return
        stopSpeaking()
        currentLine = (currentLine + delta).coerceIn(0, s.lines.lastIndex)
        reachedEnd = false
        StoryRepository.saveProgress(s.id, currentLine)
        if (isPlaying) speakCurrentLine()
        changed()
    }

    fun cycleSpeed() {
        speed = when (speed) {
            1f -> 1.25f
            1.25f -> 1.5f
            1.5f -> 0.75f
            else -> 1f
        }
        if (isPlaying) {
            stopSpeaking()
            speakCurrentLine()
        }
    }

    private val sleepAction = Runnable {
        pause()
        sleepLabel = "Off"
    }

    fun cycleSleepTimer() {
        mainThread.removeCallbacks(sleepAction)
        sleepLabel = when (sleepLabel) {
            "Off" -> "5 min"
            "5 min" -> "15 min"
            "15 min" -> "30 min"
            else -> "Off"
        }
        val minutes = sleepLabel.substringBefore(" ").toIntOrNull()
        if (minutes != null) mainThread.postDelayed(sleepAction, minutes * 60_000L)
    }

    // ── Audio focus: pause when a call or another app takes the sound ──
    private var focusRequest: AudioFocusRequest? = null

    private fun requestAudioFocus(context: Context) {
        val audioManager = context.getSystemService(AudioManager::class.java) ?: return
        val request = focusRequest ?: AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(speechAttributes)
            .setOnAudioFocusChangeListener { change ->
                if (change == AudioManager.AUDIOFOCUS_LOSS || change == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
                    mainThread.post { pause() }
                }
            }
            .build()
            .also { focusRequest = it }
        audioManager.requestAudioFocus(request)
    }

    private fun abandonAudioFocus() {
        val context = appContext ?: return
        val request = focusRequest ?: return
        context.getSystemService(AudioManager::class.java)?.abandonAudioFocusRequest(request)
    }

    // ── Inside the engine ──

    private fun stopSpeaking() {
        session++
        tts?.stop()
    }

    private fun voiceFor(index: Int): Voice? {
        if (englishVoices.isEmpty()) return null
        val step = maxOf(1, englishVoices.size / 6)
        return englishVoices[(index * step) % englishVoices.size]
    }

    private fun speakCurrentLine() {
        val s = story ?: return
        val engine = tts ?: return
        val line = s.lines[currentLine]
        val who = s.speakerOf(line)

        voiceFor(who.voice)?.let { engine.voice = it }
        engine.setPitch(who.pitch)
        engine.setSpeechRate(who.rate * speed)
        engine.speak(line.text, TextToSpeech.QUEUE_FLUSH, null, "$session-$currentLine")
    }

    private fun onLineFinished(utteranceId: String?) {
        if (utteranceId != "$session-$currentLine" || !isPlaying) return
        val s = story ?: return
        if (currentLine < s.lines.lastIndex) {
            currentLine++
            StoryRepository.saveProgress(s.id, currentLine)
            speakCurrentLine()
        } else {
            isPlaying = false
            reachedEnd = true
        }
        changed()
    }

    private val listener = object : UtteranceProgressListener() {
        override fun onStart(utteranceId: String?) {}

        override fun onDone(utteranceId: String?) {
            mainThread.post { onLineFinished(utteranceId) }
        }

        @Deprecated("Deprecated in Java")
        override fun onError(utteranceId: String?) {
            mainThread.post { onLineFinished(utteranceId) }
        }
    }
}
