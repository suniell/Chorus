package com.sunil.chorus.ui.screens

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sunil.chorus.data.*
import com.sunil.chorus.ui.components.*
import com.sunil.chorus.ui.theme.*

// The prototype reads at most this many characters from a file
private const val MAX_CHARS = 6000

// ═════════════════════════════════════════════
// BRING A STORY
// sharedText = text or a link shared from another app (null if opened normally)
// ═════════════════════════════════════════════
@Composable
fun AddStoryScreen(
    sharedText: String?,
    onBack: () -> Unit,
    onFound: () -> Unit,
) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current

    // If something was shared, is it a link?
    val sharedUrl = sharedText?.let { Regex("https?://\\S+").find(it)?.value }

    var mode by rememberSaveable { mutableIntStateOf(if (sharedUrl != null) 1 else 0) }  // 0 Text, 1 Link, 2 File
    var text by rememberSaveable { mutableStateOf(if (sharedText != null && sharedUrl == null) sharedText else "") }
    var link by rememberSaveable { mutableStateOf(sharedUrl ?: "") }
    var fileName by rememberSaveable { mutableStateOf<String?>(null) }
    var fileText by rememberSaveable { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    // Android's file picker. It returns the chosen file (or null if cancelled).
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            val content = context.contentResolver.openInputStream(uri)
                ?.bufferedReader()?.use { it.readText() }.orEmpty()
            fileText = content.take(MAX_CHARS)
            fileName = displayName(context, uri)
            error = null
        }
    }

    val canSubmit = when (mode) {
        0 -> text.isNotBlank()
        1 -> link.contains(".")
        else -> fileText.isNotBlank()
    }

    Box(Modifier.fillMaxSize()) {
        NightSky()

        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
        ) {
            BackArrow(onClick = onBack, modifier = Modifier.padding(start = 8.dp))

            Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
            ) {
                Text("Bring a story", style = MaterialTheme.typography.headlineLarge, color = StarWhite)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Chorus finds every speaking part and gives each one a voice.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Mist,
                )

                if (sharedText != null) {
                    Spacer(Modifier.height(12.dp))
                    Row(
                        Modifier
                            .clip(RoundedCornerShape(50))
                            .background(Gold.copy(alpha = 0.15f))
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Sparkle(sparkleSize = 12.dp)
                        Spacer(Modifier.width(6.dp))
                        Text("Shared from another app", style = MaterialTheme.typography.labelMedium, color = Gold)
                    }
                }

                Spacer(Modifier.height(20.dp))
                ModeTabs(selected = mode, onSelect = { mode = it; error = null })
                Spacer(Modifier.height(16.dp))

                when (mode) {
                    0 -> TextTab(
                        value = text,
                        onChange = { text = it; error = null },
                        onExample = { text = StoryParser.EXAMPLE_TEXT },
                    )
                    1 -> LinkTab(value = link, onChange = { link = it; error = null })
                    else -> FileTab(
                        fileName = fileName,
                        fileText = fileText,
                        onPick = { picker.launch(arrayOf("text/plain")) },
                    )
                }

                error?.let {
                    Spacer(Modifier.height(12.dp))
                    Text(it, style = MaterialTheme.typography.bodyMedium, color = Color(0xFFF2A3B5))
                }
                Spacer(Modifier.height(16.dp))
            }

            // ── Bottom button ──
            Column(Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
                AmberButton(
                    text = "Find the voices",
                    enabled = canSubmit,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        val story = when (mode) {
                            0 -> StoryParser.parse(text)
                            1 -> StoryParser.parse(StoryParser.DEMO_LINK_TEXT)
                            else -> StoryParser.parse(
                                fileText,
                                fallbackTitle = fileName?.substringBeforeLast('.') ?: "Untitled story",
                            )
                        }
                        if (story.characters.size < 2) {
                            error = "We couldn't find any dialogue. Put spoken lines in \"quotes\", or write them like NAME: line."
                        } else {
                            DraftStore.draft = story
                            onFound()
                        }
                    },
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "Your first preview is free.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Mist,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

// Gets a file's name, like "chapter1.txt"
private fun displayName(context: Context, uri: Uri): String =
    context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
        ?.use { cursor -> if (cursor.moveToFirst()) cursor.getString(0) else null }
        ?: "story.txt"

private fun wordCount(text: String): Int = text.split(Regex("\\s+")).count { it.isNotBlank() }

// Round "←" back button (reused on other screens)
@Composable
fun BackArrow(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(48.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text("←", style = MaterialTheme.typography.headlineMedium, color = StarWhite)
    }
}

// ─────────────────────────────────────────────
// Text | Link | File
// ─────────────────────────────────────────────
@Composable
private fun ModeTabs(selected: Int, onSelect: (Int) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(50))
            .background(NightCard)
            .padding(4.dp)
    ) {
        listOf("Text", "Link", "File").forEachIndexed { index, label ->
            val isSelected = index == selected
            Box(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(50))
                    .background(if (isSelected) Amber else Color.Transparent)
                    .clickable { onSelect(index) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isSelected) MidnightDeep else Mist,
                )
            }
        }
    }
}

// ─────────────────────────────────────────────
// Tab 1: paste text
// ─────────────────────────────────────────────
@Composable
private fun TextTab(value: String, onChange: (String) -> Unit, onExample: () -> Unit) {
    Column {
        Box(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 220.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(NightCard.copy(alpha = 0.9f))
                .border(1.dp, NightLine, RoundedCornerShape(14.dp))
                .padding(16.dp)
        ) {
            if (value.isEmpty()) {
                Text(
                    "Paste a chapter or a scene. Dialogue in \"quotes\" or NAME: line format works best.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Mist,
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onChange,
                minLines = 8,
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = StarWhite),
                cursorBrush = SolidColor(Amber),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "${wordCount(value)} words",
                style = MaterialTheme.typography.bodyMedium,
                color = Mist,
                modifier = Modifier.weight(1f),
            )
            Text(
                "Try an example",
                style = MaterialTheme.typography.labelMedium,
                color = Amber,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .clickable(onClick = onExample)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
            )
        }
    }
}

// ─────────────────────────────────────────────
// Tab 2: paste a link
// ─────────────────────────────────────────────
@Composable
private fun LinkTab(value: String, onChange: (String) -> Unit) {
    Column {
        Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(NightCard.copy(alpha = 0.9f))
                .border(1.dp, NightLine, RoundedCornerShape(14.dp))
                .padding(16.dp)
        ) {
            if (value.isEmpty()) {
                Text("archiveofourown.org/works/…", style = MaterialTheme.typography.bodyLarge, color = Mist)
            }
            BasicTextField(
                value = value,
                onValueChange = onChange,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = StarWhite),
                cursorBrush = SolidColor(Amber),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Spacer(Modifier.height(16.dp))
        Text("WORKS WITH", style = MaterialTheme.typography.labelSmall, color = Mist)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Wattpad", "AO3", "Reddit", "Substack").forEach { site ->
                Text(
                    site,
                    style = MaterialTheme.typography.labelMedium,
                    color = Gold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Gold.copy(alpha = 0.12f))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        Text(
            "Prototype note: links load a demo story here. The real app would fetch the chapter from the link.",
            style = MaterialTheme.typography.bodyMedium,
            color = Mist,
        )
    }
}

// ─────────────────────────────────────────────
// Tab 3: pick a .txt file
// ─────────────────────────────────────────────
@Composable
private fun FileTab(fileName: String?, fileText: String, onPick: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(NightCard.copy(alpha = 0.6f))
            .drawBehind {
                drawRoundRect(
                    color = Amber.copy(alpha = 0.6f),
                    cornerRadius = CornerRadius(14.dp.toPx()),
                    style = Stroke(
                        width = 1.5.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10.dp.toPx(), 8.dp.toPx())),
                    ),
                )
            }
            .clickable(onClick = onPick)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Sparkle(sparkleSize = 18.dp)
        Spacer(Modifier.height(10.dp))
        if (fileName == null) {
            Text("Choose a .txt file", style = MaterialTheme.typography.titleLarge, color = StarWhite)
            Spacer(Modifier.height(4.dp))
            Text(
                "epub, pdf and docx come later. This prototype reads plain text.",
                style = MaterialTheme.typography.bodyMedium,
                color = Mist,
                textAlign = TextAlign.Center,
            )
        } else {
            Text(fileName, style = MaterialTheme.typography.titleLarge, color = StarWhite, textAlign = TextAlign.Center)
            Spacer(Modifier.height(4.dp))
            Text(
                "${wordCount(fileText)} words loaded · tap to choose another",
                style = MaterialTheme.typography.bodyMedium,
                color = Mist,
            )
        }
    }
}