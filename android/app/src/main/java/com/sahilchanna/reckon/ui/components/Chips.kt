package com.sahilchanna.reckon.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.sahilchanna.reckon.model.Difficulty
import com.sahilchanna.reckon.model.Operation
import com.sahilchanna.reckon.ui.rememberHaptics
import com.sahilchanna.reckon.ui.theme.Bold
import com.sahilchanna.reckon.ui.theme.PressButton
import com.sahilchanna.reckon.ui.theme.SemiBold
import com.sahilchanna.reckon.ui.theme.display
import com.sahilchanna.reckon.ui.theme.palette

/** Which drills are in play. At least one stays on, so the last chip won't turn off. */
@Composable
fun OperationChips(
    enabled: List<Operation>,
    onToggle: (Operation) -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = rememberHaptics()

    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        for (op in Operation.entries) {
            val on = enabled.contains(op)
            val fill by animateColorAsState(
                targetValue = if (on) palette.chalk else palette.surface,
                animationSpec = spring(dampingRatio = 0.7f),
                label = "op-fill",
            )
            val stroke by animateColorAsState(
                targetValue = if (on) Color.Transparent else palette.line,
                animationSpec = spring(dampingRatio = 0.7f),
                label = "op-stroke",
            )

            PressButton(
                onClick = {
                    haptics.pop()
                    onToggle(op)
                },
                scale = 0.94f,
                modifier = Modifier
                    .weight(1f)
                    .semantics {
                        contentDescription = op.title
                        selected = on
                    },
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 46.dp)
                        .clip(RoundedCornerShape(13.dp))
                        .background(fill)
                        .border(1.dp, stroke, RoundedCornerShape(13.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = op.symbol,
                        style = display(20f, Bold),
                        color = if (on) palette.ink else palette.mute,
                    )
                }
            }
        }
    }
}

/** Difficulty 1–5. The label above the row comes from the caller. */
@Composable
fun DifficultyChips(
    level: Int,
    onSetLevel: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = rememberHaptics()

    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        for (value in Difficulty.range) {
            val on = level == value
            val fill by animateColorAsState(
                targetValue = if (on) palette.chalk else palette.surface,
                animationSpec = spring(dampingRatio = 0.72f),
                label = "lvl-fill",
            )
            val stroke by animateColorAsState(
                targetValue = if (on) Color.Transparent else palette.line,
                animationSpec = spring(dampingRatio = 0.72f),
                label = "lvl-stroke",
            )

            PressButton(
                onClick = {
                    haptics.pop()
                    onSetLevel(value)
                },
                scale = 0.94f,
                modifier = Modifier
                    .weight(1f)
                    .semantics { selected = on },
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(fill)
                        .border(1.dp, stroke, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "$value",
                        style = display(17f, SemiBold),
                        color = if (on) palette.ink else palette.mute,
                    )
                }
            }
        }
    }
}
