package com.sunil.chorus.widget

import android.content.Context
import androidx.glance.appwidget.updateAll
import com.sunil.chorus.player.StoryPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ═════════════════════════════════════════════
// Keeps the widget in sync: whenever the player changes
// (play, pause, next line), redraw the widget.
// ═════════════════════════════════════════════
object WidgetUpdater {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var pending: Job? = null
    private var started = false

    fun start(context: Context) {
        if (started) return
        started = true
        val app = context.applicationContext
        StoryPlayer.addListener { refresh(app) }
        refresh(app)
    }

    fun refresh(context: Context) {
        // Wait a moment so many quick changes = one redraw
        pending?.cancel()
        pending = scope.launch {
            delay(400)
            ChorusWidget().updateAll(context)
        }
    }
}
