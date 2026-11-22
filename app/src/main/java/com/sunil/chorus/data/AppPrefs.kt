package com.sunil.chorus.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

// ═════════════════════════════════════════════
// APP PREFS: small yes/no settings saved on the phone
//  - has the user seen the welcome screen?
//  - have they unlocked Chorus Plus? (demo only, no real payment)
// ═════════════════════════════════════════════
object AppPrefs {

    // Free plan: this many stories of your own
    const val FREE_STORIES = 3

    var seenWelcome by mutableStateOf(false)
        private set
    var plus by mutableStateOf(false)
        private set

    private var loaded = false

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences("chorus_prefs", Context.MODE_PRIVATE)

    fun attach(context: Context) {
        if (loaded) return
        loaded = true
        val p = prefs(context)
        seenWelcome = p.getBoolean("seen_welcome", false)
        plus = p.getBoolean("plus", false)
    }

    fun markWelcomeSeen(context: Context) {
        seenWelcome = true
        prefs(context).edit().putBoolean("seen_welcome", true).apply()
    }

    fun unlockPlus(context: Context) {
        plus = true
        prefs(context).edit().putBoolean("plus", true).apply()
    }

    // How many stories the user added themselves
    fun userStoryCount(): Int = StoryRepository.stories.count { !it.isSample }

    fun canAddStory(): Boolean = plus || userStoryCount() < FREE_STORIES
}
