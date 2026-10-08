package com.sahilchanna.reckon.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.outlined.TrackChanges
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.sahilchanna.reckon.model.Difficulty
import com.sahilchanna.reckon.model.GameMode
import com.sahilchanna.reckon.model.Operation
import com.sahilchanna.reckon.model.SaveData
import com.sahilchanna.reckon.ui.components.DifficultyChips
import com.sahilchanna.reckon.ui.components.ModeCard
import com.sahilchanna.reckon.ui.components.OperationChips
import com.sahilchanna.reckon.ui.components.OperationRow
import com.sahilchanna.reckon.ui.components.PaceChart
import com.sahilchanna.reckon.ui.theme.Black
import com.sahilchanna.reckon.ui.theme.Bold
import com.sahilchanna.reckon.ui.theme.Fmt
import com.sahilchanna.reckon.ui.theme.Heavy
import com.sahilchanna.reckon.ui.theme.Layout
import com.sahilchanna.reckon.ui.theme.Medium
import com.sahilchanna.reckon.ui.theme.PressButton
import com.sahilchanna.reckon.ui.theme.SemiBold
import com.sahilchanna.reckon.ui.theme.card
import com.sahilchanna.reckon.ui.theme.display
import com.sahilchanna.reckon.ui.theme.palette
import com.sahilchanna.reckon.ui.theme.ui
import kotlin.math.abs

/** Everything before a round: the headline pace, the three modes, and the settings. */
@Composable
fun HomeScreen(
    data: SaveData,
    onStart: (GameMode, List<Operation>?) -> Unit,
    onToggleOp: (Operation) -> Unit,
    onSetLevel: (Int) -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var confirmReset by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
        // Background: ink, lifted by an amber wash that fades out under the hero.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(palette.ink),
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
                .align(Alignment.TopCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(palette.amber.copy(alpha = 0.10f), Color.Transparent),
                    ),
                ),
        )

        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .widthIn(max = Layout.maxContent)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 8.dp, bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Header(streak = data.dailyStreak)
            Hero(data)
            Modes(data, onStart)
            Profile(data, onStart)
            Settings(data, onToggleOp, onSetLevel)
            Footer(onClear = { confirmReset = true })
        }

        // Cards sliding under the clock need something to sit on; the scrim
        // takes no input, so taps fall through to the list beneath it.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(38.dp)
                .align(Alignment.TopCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(palette.ink.copy(alpha = 0.92f), Color.Transparent),
                    ),
                ),
        )
    }

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = {
                Text(
                    text = "Clear every score and streak on this device?",
                    style = ui(18f, SemiBold),
                    color = palette.chalk,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onReset()
                        confirmReset = false
                    },
                ) {
                    Text(text = "Clear everything", style = ui(15f, SemiBold), color = palette.bad)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmReset = false }) {
                    Text(text = "Keep them", style = ui(15f, SemiBold), color = palette.mute)
                }
            },
            containerColor = palette.surface,
            textContentColor = palette.chalk,
        )
    }
}

@Composable
private fun Header(streak: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row {
            Text(text = "Reckon", style = ui(27f, Heavy), color = palette.chalk)
            Text(text = ".", style = ui(27f, Heavy), color = palette.amber)
        }
        Spacer(Modifier.weight(1f))
        if (streak > 0) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(palette.amber.copy(alpha = 0.14f))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.LocalFireDepartment,
                    contentDescription = "Daily streak $streak days",
                    tint = palette.amber,
                    modifier = Modifier.size(11.dp),
                )
                Text(text = "$streak", style = display(13f, Bold), color = palette.amber)
            }
        }
    }
}

@Composable
private fun Hero(data: SaveData) {
    val median = data.medianPace

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .card(padding = 20.dp, radius = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (median != null) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(text = "%.2f".format(median), style = display(60f, Black), color = palette.amber)
                Text(
                    text = "s",
                    style = display(24f, Heavy),
                    color = palette.amber.copy(alpha = 0.75f),
                )
            }

            Text(text = heroSubtitle(data), style = ui(13f), color = palette.mute)

            if (data.pace.size > 1) {
                PaceChart(pace = data.pace, modifier = Modifier.padding(top = 2.dp))
            }
        } else {
            Text(text = "No rounds yet", style = display(34f, Black), color = palette.chalk)
            Text(
                text = "A sprint takes 60 seconds and gives you a baseline to beat.",
                style = ui(13f),
                color = palette.mute,
            )
        }
    }
}

private fun heroSubtitle(data: SaveData): String {
    val rounds = data.pace.size
    var text = "median per solve · last $rounds round" + if (rounds == 1) "" else "s"
    if (rounds > 2) {
        val median = data.medianPace
        val last = data.pace.lastOrNull()
        if (median != null && last != null) {
            val delta = last - median
            text += when {
                abs(delta) < 0.06 -> " · holding steady"
                delta < 0 -> " · last round ${Fmt.seconds(-delta)} faster"
                else -> " · last round ${Fmt.seconds(delta)} slower"
            }
        }
    }
    return text
}

@Composable
private fun Modes(data: SaveData, onStart: (GameMode, List<Operation>?) -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        ModeCard(
            mode = GameMode.SPRINT,
            value = if (data.bestSprint > 0) "${data.bestSprint}" else "—",
            caption = "best",
            isLocked = false,
            onClick = { onStart(GameMode.SPRINT, null) },
        )
        ModeCard(
            mode = GameMode.MARATHON,
            value = data.bestMarathon?.let { Fmt.seconds(it, decimals = 1) } ?: "—",
            caption = "best",
            isLocked = false,
            onClick = { onStart(GameMode.MARATHON, null) },
        )
        ModeCard(
            mode = GameMode.DAILY,
            value = if (data.playedDailyToday) "${data.dailyScore}/15" else "—",
            caption = "today",
            isLocked = data.playedDailyToday,
            onClick = { onStart(GameMode.DAILY, null) },
        )
    }
}

@Composable
private fun Profile(data: SaveData, onStart: (GameMode, List<Operation>?) -> Unit) {
    val weakest = data.weakestOperation

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .card(),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = "Where your time goes",
            style = ui(13f, Medium),
            color = palette.mute,
            modifier = Modifier.padding(bottom = 6.dp),
        )

        for (op in Operation.entries) {
            OperationRow(
                operation = op,
                stats = data.statsFor(op),
                worstAverage = weakest?.second,
                isWorst = weakest?.first == op,
            )
        }

        if (weakest != null) {
            val line = palette.line
            PressButton(
                onClick = { onStart(GameMode.SPRINT, listOf(weakest.first)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp)
                    .drawBehind {
                        drawRoundRect(
                            color = line,
                            topLeft = Offset.Zero,
                            size = size,
                            cornerRadius = CornerRadius(14.dp.toPx()),
                            style = Stroke(
                                width = 1.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(
                                    floatArrayOf(5.dp.toPx(), 4.dp.toPx()),
                                ),
                            ),
                        )
                    }
                    .padding(vertical = 13.dp),
                contentAlignment = Alignment.Center,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.TrackChanges,
                        contentDescription = null,
                        tint = palette.chalk,
                        modifier = Modifier.size(13.dp),
                    )
                    Text(
                        text = "Drill ${weakest.first.symbol} only — slowest at ${
                            Fmt.seconds(weakest.second)
                        }",
                        style = ui(13f, SemiBold),
                        color = palette.chalk,
                    )
                }
            }
        }
    }
}

@Composable
private fun Settings(
    data: SaveData,
    onToggleOp: (Operation) -> Unit,
    onSetLevel: (Int) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .card(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = "Operations", style = ui(13f, Medium), color = palette.mute)
            OperationChips(enabled = data.enabledOperations, onToggle = onToggleOp)
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "Difficulty", style = ui(13f, Medium), color = palette.mute)
                Spacer(Modifier.weight(1f))
                Text(
                    text = Difficulty.label(data.level),
                    style = ui(13f, SemiBold),
                    color = palette.chalk,
                )
            }
            DifficultyChips(level = data.level, onSetLevel = onSetLevel)
        }
    }
}

@Composable
private fun Footer(onClear: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = "Runs fully offline. Scores stay on this device.",
            style = ui(11.5f),
            color = palette.mute,
        )
        PressButton(onClick = onClear) {
            Text(
                text = "Clear all scores",
                style = ui(11.5f, SemiBold),
                color = palette.mute,
                textDecoration = TextDecoration.Underline,
            )
        }
    }
}
