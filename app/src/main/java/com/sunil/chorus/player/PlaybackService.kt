package com.sunil.chorus.player

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.graphics.drawable.Icon
import android.media.MediaMetadata
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.Build
import android.os.IBinder
import com.sunil.chorus.MainActivity
import com.sunil.chorus.R

// ═════════════════════════════════════════════
// PLAYBACK SERVICE: keeps the story going in the background,
// shows the notification, and powers the lock-screen controls.
// ═════════════════════════════════════════════
class PlaybackService : Service() {

    companion object {
        var running = false
            private set

        private const val CHANNEL_ID = "playback"
        private const val NOTIFICATION_ID = 7

        private const val ACTION_TOGGLE = "com.sunil.chorus.TOGGLE"
        private const val ACTION_NEXT = "com.sunil.chorus.NEXT"
        private const val ACTION_PREVIOUS = "com.sunil.chorus.PREVIOUS"
        private const val ACTION_CLOSE = "com.sunil.chorus.CLOSE"

        // Cover colours (background, moon) for the lock-screen art
        private val coverColors = listOf(
            0xFF2B3A6B to 0xFFF2C879,
            0xFF6B2B3A to 0xFFF4AE86,
            0xFF2B5B4E to 0xFFBFE3C0,
            0xFF4E3B7A to 0xFFC3AEF2,
            0xFF7A5A2B to 0xFFF2C879,
        )
    }

    private lateinit var session: MediaSession
    private var inForeground = false
    private var cover: Bitmap? = null
    private var coverStyleDrawn = -1

    // Called by StoryPlayer whenever anything changes
    private val onPlayerChanged: () -> Unit = { update() }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        running = true

        // A notification channel is required on Android 8+
        val channel = NotificationChannel(CHANNEL_ID, "Playback", NotificationManager.IMPORTANCE_LOW).apply {
            description = "Shows the story that is playing"
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)

        // The media session: lock screen, headphone and Bluetooth buttons all come here
        session = MediaSession(this, "Chorus").apply {
            setCallback(object : MediaSession.Callback() {
                override fun onPlay() = StoryPlayer.play()
                override fun onPause() = StoryPlayer.pause()
                override fun onSkipToNext() = StoryPlayer.skip(1)
                override fun onSkipToPrevious() = StoryPlayer.skip(-1)
                override fun onStop() = StoryPlayer.close()
            })
            isActive = true
        }

        StoryPlayer.addListener(onPlayerChanged)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Buttons in the notification arrive here
        when (intent?.action) {
            ACTION_TOGGLE -> StoryPlayer.togglePlay()
            ACTION_NEXT -> StoryPlayer.skip(1)
            ACTION_PREVIOUS -> StoryPlayer.skip(-1)
            ACTION_CLOSE -> {
                StoryPlayer.close()          // this also stops the service (see update)
                return START_NOT_STICKY
            }
        }

        // Android requires a foreground service to show its notification quickly
        if (!inForeground) {
            val notification = buildNotification()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
            inForeground = true
        }
        update()
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        running = false
        StoryPlayer.removeListener(onPlayerChanged)
        session.release()
        super.onDestroy()
    }

    // ── Keep the lock screen and notification in sync with the player ──
    private fun update() {
        val story = StoryPlayer.story
        if (story == null) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            inForeground = false
            stopSelf()
            return
        }

        if (story.coverStyle != coverStyleDrawn) {
            cover = drawCover(story.coverStyle)
            coverStyleDrawn = story.coverStyle
        }

        val playing = StoryPlayer.isPlaying

        // What the lock screen shows: title, who is speaking, cover
        session.setMetadata(
            MediaMetadata.Builder()
                .putString(MediaMetadata.METADATA_KEY_TITLE, story.title)
                .putString(MediaMetadata.METADATA_KEY_ARTIST, StoryPlayer.currentSpeaker?.name ?: "")
                .putString(MediaMetadata.METADATA_KEY_ALBUM, story.author)
                .putBitmap(MediaMetadata.METADATA_KEY_ART, cover)
                .build()
        )

        // Which buttons work, and whether we are playing
        session.setPlaybackState(
            PlaybackState.Builder()
                .setActions(
                    PlaybackState.ACTION_PLAY or PlaybackState.ACTION_PAUSE or
                            PlaybackState.ACTION_PLAY_PAUSE or PlaybackState.ACTION_SKIP_TO_NEXT or
                            PlaybackState.ACTION_SKIP_TO_PREVIOUS or PlaybackState.ACTION_STOP
                )
                .setState(
                    if (playing) PlaybackState.STATE_PLAYING else PlaybackState.STATE_PAUSED,
                    PlaybackState.PLAYBACK_POSITION_UNKNOWN,
                    if (playing) 1f else 0f,
                )
                .build()
        )

        if (inForeground) {
            getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, buildNotification())
        }
    }

    // ── The notification ──
    private fun buildNotification(): Notification {
        val story = StoryPlayer.story
        val playing = StoryPlayer.isPlaying

        // Tapping the notification opens the app (like tapping its icon)
        val openApp = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java)
                .setAction(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_LAUNCHER)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED),
            PendingIntent.FLAG_IMMUTABLE,
        )

        val subtitle = if (story == null) "" else listOfNotNull(
            StoryPlayer.currentSpeaker?.name,
            "Line ${StoryPlayer.currentLine + 1} of ${story.lines.size}",
        ).joinToString(" · ")

        return Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_chorus)
            .setContentTitle(story?.title ?: "Chorus")
            .setContentText(subtitle)
            .setLargeIcon(cover)
            .setContentIntent(openApp)
            .setOngoing(playing)
            .setShowWhen(false)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .addAction(button(android.R.drawable.ic_media_previous, "Previous", ACTION_PREVIOUS))
            .addAction(
                button(
                    if (playing) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play,
                    if (playing) "Pause" else "Play",
                    ACTION_TOGGLE,
                )
            )
            .addAction(button(android.R.drawable.ic_media_next, "Next", ACTION_NEXT))
            .addAction(button(android.R.drawable.ic_menu_close_clear_cancel, "Close", ACTION_CLOSE))
            .setStyle(
                Notification.MediaStyle()
                    .setMediaSession(session.sessionToken)
                    .setShowActionsInCompactView(0, 1, 2)   // previous, play/pause, next
            )
            .build()
    }

    // A notification button that sends an action back to this service
    private fun button(icon: Int, title: String, action: String): Notification.Action {
        val pendingIntent = PendingIntent.getService(
            this,
            action.hashCode(),
            Intent(this, PlaybackService::class.java).setAction(action),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        return Notification.Action.Builder(Icon.createWithResource(this, icon), title, pendingIntent).build()
    }

    // A simple painted cover for the lock screen: night gradient + moon
    private fun drawCover(style: Int): Bitmap {
        val size = 256
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val (sky, moon) = coverColors[Math.floorMod(style, coverColors.size)]
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        paint.shader = LinearGradient(
            0f, 0f, 0f, size.toFloat(),
            sky.toInt(), 0xFF0B1026.toInt(),
            Shader.TileMode.CLAMP,
        )
        canvas.drawRect(0f, 0f, size.toFloat(), size.toFloat(), paint)

        paint.shader = null
        paint.color = moon.toInt()
        canvas.drawCircle(size * 0.68f, size * 0.32f, size * 0.14f, paint)
        return bitmap
    }
}