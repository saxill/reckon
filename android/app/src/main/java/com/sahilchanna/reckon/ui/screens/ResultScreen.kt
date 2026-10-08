package com.sahilchanna.reckon.ui.screens

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.outlined.CenterFocusStrong
import androidx.compose.material.icons.outlined.Numbers
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.sahilchanna.reckon.model.GameMode
import com.sahilchanna.reckon.model.RoundResult
import com.sahilchanna.reckon.ui.components.StatTile
import com.sahilchanna.reckon.ui.rememberHaptics
import com.sahilchanna.reckon.ui.theme.Black
import com.sahilchanna.reckon.ui.theme.Bold
import com.sahilchanna.reckon.ui.theme.Fmt
import com.sahilchanna.reckon.ui.theme.Heavy
import com.sahilchanna.reckon.ui.theme.Layout
import com.sahilchanna.reckon.ui.theme.PressButton
import com.sahilchanna.reckon.ui.theme.SemiBold
import com.sahilchanna.reckon.ui.theme.card
import com.sahilchanna.reckon.ui.theme.display
import com.sahilchanna.reckon.ui.theme.palette
import com.sahilchanna.reckon.ui.theme.ui

/** How a finished round reads back: the headline figure, four tiles, and the misses. */
@Composable
fun ResultScreen(
    result: RoundResult,
    onAgain: () -> Unit,
    onHome: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = rememberHaptics()
    var appeared by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        appeared = true
        if (result.isPersonalBest) haptics.success()
    }

    val scale by animateFloatAsState(
        targetValue = if (appeared) 1f else 0.6f,
        animationSpec = spring(dampingRatio = 0.68f, stiffness = Spring.StiffnessLow),
        label = "appear-scale",
    )
    val opacity by animateFloatAsState(
        targetValue = if (appeared) 1f else 0f,
        animationSpec = tween(300),
        label = "appear-alpha",
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(palette.ink),
    ) {
        BoxWithConstraints(modifier = Modifier.weight(1f).fillMaxWidth()) {
            // Short rounds shouldn't leave a void: centre the card stack, and let
            // it scroll normally once the missed list makes it tall.
            val viewport = maxHeight
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                Column(
                    modifier = Modifier
                        .widthIn(max = Layout.maxContent)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .heightIn(min = viewport)
                        .wrapContentHeight(Alignment.CenterVertically)
                        .padding(horizontal = 20.dp, vertical = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                ) {
                    Score(result, scale, opacity)
                    Tiles(result)
                    if (result.misses.isNotEmpty()) Missed(result)
                }
            }
        }

        Actions(result.mode, onHome = onHome, onAgain = onAgain)
    }
}

private fun headline(result: RoundResult): String = when (result.mode) {
    GameMode.SPRINT -> "${result.correct}"
    GameMode.MARATHON -> if (result.ranOutOfLives) "OUT" else Fmt.seconds(result.elapsed, decimals = 1)
    GameMode.DAILY -> "${result.correct}/${result.mode.total ?: 15}"
}

private fun caption(result: RoundResult): String = when (result.mode) {
    GameMode.SPRINT -> "problems solved in 60 seconds"
    GameMode.MARATHON -> if (result.ranOutOfLives) "three misses — the run ends there"
    else "to clear 20 problems"
    GameMode.DAILY -> "on today's set"
}

@Composable
private fun Score(result: RoundResult, scale: Float, opacity: Float) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (result.isPersonalBest) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(palette.good.copy(alpha = 0.15f))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.EmojiEvents,
                    contentDescription = null,
                    tint = palette.good,
                    modifier = Modifier.size(10.dp),
                )
                Text(text = "PERSONAL BEST", style = ui(11f, Heavy), color = palette.good)
            }
        }

        Text(
            text = headline(result),
            style = display(84f, Black),
            color = palette.amber,
            maxLines = 1,
            modifier = Modifier.graphicsLayer {
                scaleX = scale
                scaleY = scale
                alpha = opacity
                transformOrigin = TransformOrigin(0f, 0.5f)
            },
        )

        Text(text = caption(result), style = ui(14f), color = palette.mute)
    }
}

@Composable
private fun Tiles(result: RoundResult) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatTile(
                icon = Icons.Outlined.CenterFocusStrong,
                value = result.accuracy?.let { Fmt.percent(it) } ?: "—",
                label = "accuracy",
                modifier = Modifier.weight(1f),
            )
            StatTile(
                icon = Icons.Outlined.Timer,
                value = result.averageSolve?.let { Fmt.seconds(it) } ?: "—",
                label = "average solve",
                modifier = Modifier.weight(1f),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatTile(
                icon = Icons.Filled.Bolt,
                value = result.fastestSolve?.let { Fmt.seconds(it) } ?: "—",
                label = "fastest solve",
                modifier = Modifier.weight(1f),
            )
            StatTile(
                icon = Icons.Outlined.Numbers,
                value = "${result.seen}",
                label = "problems seen",
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun Missed(result: RoundResult) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .card(padding = 16.dp, radius = 20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = "Missed ${result.misses.size}",
            style = ui(13f, SemiBold),
            color = palette.bad,
        )

        result.misses.forEachIndexed { index, miss ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "${miss.question} = ${miss.answer}",
                    style = display(14f, SemiBold),
                    color = palette.chalk,
                )
                Spacer(Modifier.weight(1f))
                Text(text = "you: ${miss.typed}", style = display(13f), color = palette.mute)
            }
            if (index != result.misses.lastIndex) {
                HorizontalDivider(color = palette.line)
            }
        }
    }
}

@Composable
private fun Actions(mode: GameMode, onHome: () -> Unit, onAgain: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(palette.ink.copy(alpha = 0.96f))
            .padding(horizontal = 20.dp)
            .padding(top = 8.dp, bottom = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        PressButton(onClick = onHome, modifier = Modifier.weight(1f)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 54.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(palette.surface)
                    .border(1.dp, palette.line, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = "Home", style = ui(16f, SemiBold), color = palette.chalk)
            }
        }

        PressButton(onClick = onAgain, modifier = Modifier.weight(1f)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 54.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(palette.amber),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = if (mode == GameMode.DAILY) "Sprint" else "Play again",
                    style = ui(16f, Bold),
                    color = palette.ink,
                )
            }
        }
    }
}
