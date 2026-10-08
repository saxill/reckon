package com.sahilchanna.reckon.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.sahilchanna.reckon.ui.theme.PressButton
import com.sahilchanna.reckon.ui.theme.SemiBold
import com.sahilchanna.reckon.ui.theme.display
import com.sahilchanna.reckon.ui.theme.palette
import com.sahilchanna.reckon.ui.theme.ui

/**
 * The on-screen pad: 1–9, clear, 0, delete.
 *
 * The engine auto-submits once the answer is long enough, so there is no
 * confirm key here — Return exists only for a hardware keyboard.
 */
@Composable
fun Keypad(
    onDigit: (String) -> Unit,
    onClear: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        for (row in 0 until 3) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                for (column in 0 until 3) {
                    val n = row * 3 + column + 1
                    Key(description = "$n", action = { onDigit("$n") }) {
                        Text(text = "$n", style = display(27f, SemiBold), color = palette.chalk)
                    }
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Key(description = "clear", secondary = true, action = onClear) {
                Text(text = "clear", style = ui(16f, SemiBold), color = palette.mute)
            }
            Key(description = "0", action = { onDigit("0") }) {
                Text(text = "0", style = display(27f, SemiBold), color = palette.chalk)
            }
            Key(description = "delete", secondary = true, action = onDelete) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Backspace,
                    contentDescription = null,
                    tint = palette.mute,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

/**
 * One key. `content` carries the glyph so a digit, a word and an icon can share
 * the same chrome without the caller repeating it three times.
 */
@Composable
private fun RowScope.Key(
    description: String,
    action: () -> Unit,
    secondary: Boolean = false,
    content: @Composable () -> Unit,
) {
    val shape = RoundedCornerShape(18.dp)
    val fill = if (secondary) palette.surface.copy(alpha = 0.6f) else palette.surface

    PressButton(
        onClick = action,
        scale = 0.93f,
        modifier = Modifier
            .weight(1f)
            .semantics { contentDescription = description },
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 64.dp)
                .clip(shape)
                .background(fill, shape)
                .border(1.dp, palette.line, shape),
            contentAlignment = Alignment.Center,
            content = { content() },
        )
    }
}
