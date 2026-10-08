package com.sahilchanna.reckon.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.sahilchanna.reckon.model.GameMode
import com.sahilchanna.reckon.ui.theme.Heavy
import com.sahilchanna.reckon.ui.theme.Medium
import com.sahilchanna.reckon.ui.theme.PressButton
import com.sahilchanna.reckon.ui.theme.SemiBold
import com.sahilchanna.reckon.ui.theme.card
import com.sahilchanna.reckon.ui.theme.display
import com.sahilchanna.reckon.ui.theme.palette
import com.sahilchanna.reckon.ui.theme.ui

/** `bolt.fill`, `flag.pattern.checkered`, `calendar` — the SF Symbols' nearest kin. */
fun modeIcon(mode: GameMode): ImageVector = when (mode) {
    GameMode.SPRINT -> Icons.Filled.Bolt
    GameMode.MARATHON -> Icons.Filled.Flag
    GameMode.DAILY -> Icons.Filled.CalendarMonth
}

/** One tappable mode on the home screen: icon, title, blurb, and a best/today figure. */
@Composable
fun ModeCard(
    mode: GameMode,
    value: String,
    caption: String,
    isLocked: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PressButton(
        onClick = onClick,
        enabled = !isLocked,
        modifier = modifier
            .fillMaxWidth()
            .alpha(if (isLocked) 0.6f else 1f)
            .card(),
        contentAlignment = Alignment.CenterStart,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(palette.amber.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (isLocked) Icons.Filled.Check else modeIcon(mode),
                    contentDescription = null,
                    tint = palette.amber,
                    modifier = Modifier.size(17.dp),
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(text = mode.title, style = ui(19f, SemiBold), color = palette.chalk)
                Text(
                    text = if (isLocked) "Played. A fresh set lands tomorrow." else mode.blurb,
                    style = ui(13f),
                    color = palette.mute,
                )
            }

            Spacer(Modifier.weight(1f))

            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(text = value, style = display(21f, Heavy), color = palette.amber)
                Text(text = caption, style = ui(10.5f, Medium), color = palette.mute)
            }
        }
    }
}
