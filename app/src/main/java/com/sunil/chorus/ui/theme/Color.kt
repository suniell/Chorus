package com.sunil.chorus.ui.theme

import androidx.compose.ui.graphics.Color

// ---------- Night sky ----------
val Dusk = Color(0xFF1E2A5A)          // glow at the top of the sky
val Midnight = Color(0xFF0B1026)      // main background
val MidnightDeep = Color(0xFF060917)  // bottom of the sky
val NightCard = Color(0xFF141B38)     // cards on the night background
val NightLine = Color(0xFF26305A)     // thin borders and dividers
val StarWhite = Color(0xFFEEF0FA)     // main text on dark
val Mist = Color(0xFF8C93B5)          // secondary text on dark

// ---------- Paper (the script) ----------
val Paper = Color(0xFFF4EEDF)
val PaperLine = Color(0xFFDCD2BC)
val Ink = Color(0xFF2A2418)           // text on paper
val InkSoft = Color(0xFF6E6553)       // narrator name, small labels on paper

// ---------- Accents ----------
val Amber = Color(0xFFE2A564)         // play buttons and main actions
val AmberDeep = Color(0xFFB97A3C)
val Gold = Color(0xFFF2C879)          // sparkles and highlights

// ---------- One colour per character ----------
// Darker versions: for names on the cream paper
val VoiceColors = listOf(
    Color(0xFFC0566E), // rose
    Color(0xFF3E7CB1), // blue
    Color(0xFF4E8A5B), // green
    Color(0xFF7A5BB5), // violet
    Color(0xFFC46A3A), // rust
    Color(0xFF8A6D2B), // ochre
)

// Lighter versions: for names on the dark night background
val VoiceColorsLight = listOf(
    Color(0xFFF2A3B5), // pink
    Color(0xFF8EC3F2), // sky
    Color(0xFF9ED3A8), // sage
    Color(0xFFC3AEF2), // lilac
    Color(0xFFF4AE86), // peach
    Color(0xFFF2C879), // gold
)

// Picks a character's colour. Same index = same character colour everywhere.
fun voiceColor(index: Int, onPaper: Boolean): Color {
    val list = if (onPaper) VoiceColors else VoiceColorsLight
    return list[Math.floorMod(index, list.size)]
}