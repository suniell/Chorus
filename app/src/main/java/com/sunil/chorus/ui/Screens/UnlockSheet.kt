package com.sunil.chorus.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sunil.chorus.data.AppPrefs
import com.sunil.chorus.ui.components.*
import com.sunil.chorus.ui.theme.*
import kotlinx.coroutines.launch

// ═════════════════════════════════════════════
// UNLOCK SHEET: "Chorus Plus" paywall that slides up from the bottom.
// Shows how the app would make money. Demo only: no real payment.
// ═════════════════════════════════════════════
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnlockSheet(reason: String?, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    var yearly by remember { mutableStateOf(true) }
    var celebrating by remember { mutableStateOf(false) }

    // Slide the sheet down nicely, then tell the parent it's gone
    val close: () -> Unit = {
        scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = NightCard,
        contentColor = StarWhite,
        dragHandle = { BottomSheetDefaults.DragHandle(color = NightLine) },
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
                .padding(bottom = 20.dp),
        ) {
            if (celebrating) {
                // ── After "buying" ──
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    Sparkle(sparkleSize = 18.dp)
                    Spacer(Modifier.width(10.dp))
                    Sparkle(sparkleSize = 34.dp)
                    Spacer(Modifier.width(10.dp))
                    Sparkle(sparkleSize = 18.dp)
                }
                Spacer(Modifier.height(18.dp))
                Text(
                    "Welcome to Chorus Plus",
                    style = MaterialTheme.typography.headlineMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Your whole library is unlocked. Add as many stories as you like.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Mist,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(24.dp))
                AmberButton("Keep listening", onClick = close, modifier = Modifier.fillMaxWidth())
            } else {
                // ── The offer ──
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Sparkle(sparkleSize = 12.dp)
                    Spacer(Modifier.width(8.dp))
                    Text("CHORUS PLUS", style = MaterialTheme.typography.labelMedium, color = Gold)
                }
                Spacer(Modifier.height(6.dp))
                Text("Unlock the full cast", style = MaterialTheme.typography.headlineMedium)
                if (reason != null) {
                    Spacer(Modifier.height(4.dp))
                    Text(reason, style = MaterialTheme.typography.bodyMedium, color = Mist)
                }
                Spacer(Modifier.height(18.dp))

                Perk("∞", "Unlimited stories", "Free plan: ${AppPrefs.FREE_STORIES} of your own")
                Perk("✦", "Studio voices", "Richer, more natural voices for every character")
                Perk("⇣", "Whole books", "Add long chapters, not just short scenes")
                Perk("☾", "New voices every month", "Early access to new characters")
                Spacer(Modifier.height(18.dp))

                PlanOption(
                    selected = yearly,
                    title = "Yearly",
                    note = "$3.33/mo · Save 33%",
                    price = "$39.99/yr",
                    badge = "BEST VALUE",
                    onClick = { yearly = true },
                )
                Spacer(Modifier.height(10.dp))
                PlanOption(
                    selected = !yearly,
                    title = "Monthly",
                    note = "Cancel anytime",
                    price = "$4.99/mo",
                    badge = null,
                    onClick = { yearly = false },
                )
                Spacer(Modifier.height(20.dp))

                AmberButton(
                    text = "Start 7-day free trial",
                    onClick = {
                        AppPrefs.unlockPlus(context)
                        celebrating = true
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    if (yearly) "Free for 7 days, then $39.99/year." else "Free for 7 days, then $4.99/month.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Mist,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                TextButton(onClick = close, modifier = Modifier.fillMaxWidth()) {
                    Text("Not now", color = Mist)
                }
                Text(
                    "Concept demo · no real payment is taken",
                    style = MaterialTheme.typography.labelSmall,
                    color = Mist.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun Perk(icon: String, title: String, subtitle: String) {
    Row(Modifier.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(Amber.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(icon, color = Gold, style = MaterialTheme.typography.titleMedium)
        }
        Spacer(Modifier.width(14.dp))
        Column {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = Mist)
        }
    }
}

@Composable
private fun PlanOption(
    selected: Boolean,
    title: String,
    note: String,
    price: String,
    badge: String?,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = if (selected) Amber.copy(alpha = 0.10f) else Color.Transparent,
        contentColor = StarWhite,
        border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) Amber else NightLine),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            // Round "radio" dot
            Box(
                Modifier
                    .size(20.dp)
                    .border(2.dp, if (selected) Amber else Mist, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                if (selected) {
                    Box(
                        Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(Amber)
                    )
                }
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(note, style = MaterialTheme.typography.bodyMedium, color = Mist)
            }
            Column(horizontalAlignment = Alignment.End) {
                if (badge != null) {
                    Text(
                        badge,
                        style = MaterialTheme.typography.labelSmall,
                        color = MidnightDeep,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Gold)
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                    Spacer(Modifier.height(4.dp))
                }
                Text(price, style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}
