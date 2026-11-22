package com.sunil.chorus

import android.content.Intent
import android.graphics.Color as AndroidColor
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.sunil.chorus.data.AppPrefs
import com.sunil.chorus.data.StoryRepository
import com.sunil.chorus.widget.WidgetUpdater
import com.sunil.chorus.ui.screens.AddStoryScreen
import com.sunil.chorus.ui.screens.CastingScreen
import com.sunil.chorus.ui.screens.HomeScreen
import com.sunil.chorus.ui.screens.MiniPlayer
import com.sunil.chorus.ui.screens.PlayerScreen
import com.sunil.chorus.ui.screens.UnlockSheet
import com.sunil.chorus.ui.screens.WelcomeScreen
import com.sunil.chorus.ui.theme.ChorusTheme

class MainActivity : ComponentActivity() {

    companion object {
        // The widget puts a story id here to open the player directly
        const val EXTRA_OPEN_STORY = "open_story"
    }

    // A story the widget asked us to open (null = nothing to do)
    private var openRequest by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppPrefs.attach(this)           // welcome seen? Plus unlocked?
        StoryRepository.attach(this)    // load your saved library
        WidgetUpdater.start(this)       // keep the home-screen widget in sync
        if (savedInstanceState == null) openRequest = intent?.getStringExtra(EXTRA_OPEN_STORY)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
        )
        val shared = sharedTextFrom(intent)
        setContent {
            ChorusTheme {
                TempNavigation(
                    sharedText = shared,
                    openRequest = openRequest,
                    onOpenHandled = { openRequest = null },
                )
            }
        }
    }

    // The app was already open and the widget was tapped
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent.getStringExtra(EXTRA_OPEN_STORY)?.let { openRequest = it }
    }
}

private fun sharedTextFrom(intent: Intent?): String? =
    if (intent?.action == Intent.ACTION_SEND && intent.type?.startsWith("text/") == true) {
        intent.getStringExtra(Intent.EXTRA_TEXT)
    } else {
        null
    }

// The order of screens: going "up" this list slides left, going back slides right
private val screenOrder = mapOf("welcome" to 0, "home" to 1, "add" to 2, "casting" to 3, "player" to 4)

// Screen switcher with smooth slide + fade animations
@Composable
fun TempNavigation(sharedText: String?, openRequest: String?, onOpenHandled: () -> Unit) {
    val context = LocalContext.current
    var screen by rememberSaveable {
        mutableStateOf(
            when {
                !AppPrefs.seenWelcome -> "welcome"
                sharedText != null -> "add"
                else -> "home"
            }
        )
    }
    var openStoryId by rememberSaveable { mutableStateOf("pride") }

    // The Chorus Plus sheet
    var showPlus by rememberSaveable { mutableStateOf(false) }
    var plusReason by rememberSaveable { mutableStateOf<String?>(null) }

    // "Bring a story": free plan allows a few, then the Plus sheet appears
    val goToAdd = {
        if (AppPrefs.canAddStory()) {
            screen = "add"
        } else {
            plusReason = "You've used all ${AppPrefs.FREE_STORIES} free stories."
            showPlus = true
        }
    }

    // Widget tapped: jump straight into the player
    LaunchedEffect(openRequest) {
        if (openRequest != null) {
            openStoryId = openRequest
            screen = "player"
            onOpenHandled()
        }
    }

    AnimatedContent(
        targetState = screen,
        transitionSpec = {
            val forward = (screenOrder[targetState] ?: 0) >= (screenOrder[initialState] ?: 0)
            val direction = if (forward) 1 else -1
            (fadeIn(tween(320)) + slideInHorizontally(tween(320)) { width -> direction * width / 5 }) togetherWith
                (fadeOut(tween(220)) + slideOutHorizontally(tween(320)) { width -> -direction * width / 5 })
        },
        label = "screens",
    ) { current ->
        when (current) {
            "welcome" -> WelcomeScreen(
                onStart = {
                    AppPrefs.markWelcomeSeen(context)
                    screen = if (sharedText != null) "add" else "home"
                },
            )
            "home" -> Box(Modifier.fillMaxSize()) {
                HomeScreen(
                    onOpenStory = { id ->
                        openStoryId = id
                        screen = "player"
                    },
                    onAddStory = goToAdd,
                    onOpenPlus = {
                        plusReason = null
                        showPlus = true
                    },
                )
                // Floats at the bottom while a story is loaded
                MiniPlayer(
                    onOpen = { id ->
                        openStoryId = id
                        screen = "player"
                    },
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
            }
            "add" -> AddStoryScreen(
                sharedText = sharedText,
                onBack = { screen = "home" },
                onFound = { screen = "casting" },
            )
            "casting" -> CastingScreen(
                onBack = { screen = "add" },
                onStart = { id ->
                    openStoryId = id
                    screen = "player"
                },
            )
            "player" -> PlayerScreen(
                storyId = openStoryId,
                onBack = { screen = "home" },
            )
        }
    }

    if (showPlus) {
        UnlockSheet(
            reason = plusReason,
            onDismiss = {
                showPlus = false
                // They unlocked because they wanted to add a story: take them there
                if (AppPrefs.plus && plusReason != null) screen = "add"
                plusReason = null
            },
        )
    }
}
