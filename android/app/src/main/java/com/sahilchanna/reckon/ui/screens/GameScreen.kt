package com.sahilchanna.reckon.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.sahilchanna.reckon.model.GameEngine
import com.sahilchanna.reckon.ui.components.Keypad
import com.sahilchanna.reckon.ui.rememberHaptics
import com.sahilchanna.reckon.ui.theme.Black
import com.sahilchanna.reckon.ui.theme.Heavy
import com.sahilchanna.reckon.ui.theme.Layout
import com.sahilchanna.reckon.ui.theme.PressButton
import com.sahilchanna.reckon.ui.theme.SemiBold
import com.sahilchanna.reckon.ui.theme.display
import com.sahilchanna.reckon.ui.theme.palette
import com.sahilchanna.reckon.ui.theme.ui
import kotlin.math.PI
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * A round in progress: HUD, problem, typed answer, keypad.
 *
 * The engine has no timer of its own on Android — it exposes [GameEngine.tick]
 * and this screen pumps it once per frame. That keeps the clock exactly as
 * smooth as the display and costs nothing while the round is paused or over.
 */
@Composable
fun GameScreen(
    engine: GameEngine,
    onQuit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = rememberHaptics()

    LaunchedEffect(engine) {
        while (!engine.isFinished) {
            withFrameNanos { }
            engine.tick()
        }
    }

    LaunchedEffect(engine.feedback) {
        when (engine.feedback) {
            GameEngine.Feedback.CORRECT -> haptics.success()
            GameEngine.Feedback.WRONG -> haptics.error()
            GameEngine.Feedback.IDLE -> Unit
        }
    }

    // Replaces the Swift `Shake` GeometryEffect: three horizontal wobbles over
    // 0.4s, driven linearly so the sine lands back on zero.
    val shake = remember { Animatable(0f) }
    LaunchedEffect(engine.feedback) {
        if (engine.feedback == GameEngine.Feedback.WRONG) {
            shake.snapTo(0f)
            shake.animateTo(1f, animationSpec = tween(durationMillis = 400, easing = LinearEasing))
        }
    }

    val shakeOffset = if (shake.value == 0f) 0 else (8 * sin(shake.value * PI * 3)).roundToInt()

    val answerTint by animateColorAsState(
        targetValue = when (engine.feedback) {
            GameEngine.Feedback.CORRECT -> palette.good
            GameEngine.Feedback.WRONG -> palette.bad
            GameEngine.Feedback.IDLE -> palette.amber
        },
        animationSpec = tween(150),
        label = "answer-tint",
    )

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(palette.ink),
    ) {
        val wide = Layout.isWide(maxWidth, maxHeight)
        val big = wide || maxWidth > 500.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
        ) {
            Hud(engine = engine, onQuit = onQuit)
            Progress(engine = engine, modifier = Modifier.padding(top = 12.dp))

            val keypad: @Composable (Modifier) -> Unit = { m ->
                Keypad(
                    onDigit = {
                        haptics.tap()
                        engine.type(it)
                    },
                    onClear = {
                        haptics.tap()
                        engine.clear()
                    },
                    onDelete = {
                        haptics.tap()
                        engine.backspace()
                    },
                    modifier = m,
                )
            }

            if (wide) {
                // Landscape tablet / desktop windowing: problem and keypad share
                // the width, so the keys stay in reach instead of stretching
                // across the whole screen.
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(vertical = 18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(36.dp),
                ) {
                    Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                        Stage(engine, big = true, shakeOffset = shakeOffset, answerTint = answerTint)
                    }
                    Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                        keypad(Modifier.fillMaxWidth().widthIn(max = 400.dp))
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    Stage(engine, big = big, shakeOffset = shakeOffset, answerTint = answerTint)
                }
                keypad(
                    Modifier
                        .align(Alignment.CenterHorizontally)
                        .fillMaxWidth()
                        .widthIn(max = Layout.maxKeypad)
                        .padding(bottom = 6.dp),
                )
            }
        }
    }
}

@Composable
private fun Hud(engine: GameEngine, onQuit: () -> Unit) {
    val urgent = engine.remaining?.let { it <= 10 } ?: false

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        PressButton(
            onClick = onQuit,
            scale = 0.9f,
            modifier = Modifier.size(34.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(palette.surface),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "End round",
                    tint = palette.mute,
                    modifier = Modifier.size(13.dp),
                )
            }
        }

        Text(
            text = engine.clockText,
            style = display(22f, Heavy),
            color = if (urgent) palette.bad else palette.chalk,
        )

        Spacer(Modifier.weight(1f))

        val total = engine.total
        if (total != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Lives(livesLeft = engine.livesLeft)
                Text(
                    text = "${min(engine.answered + 1, total)}/$total",
                    style = display(15f, SemiBold),
                    color = palette.mute,
                )
            }
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Text(text = "${engine.correct}", style = display(17f, Heavy), color = palette.chalk)
                Text(text = "solved", style = ui(13f), color = palette.mute)
            }
        }
    }
}

@Composable
private fun Lives(livesLeft: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        for (i in 0 until 3) {
            Icon(
                imageVector = if (i < livesLeft) Icons.Filled.Favorite
                else Icons.Filled.FavoriteBorder,
                contentDescription = null,
                tint = if (i < livesLeft) palette.bad else palette.surface2,
                modifier = Modifier.size(11.dp),
            )
        }
    }
}

@Composable
private fun Progress(engine: GameEngine, modifier: Modifier = Modifier) {
    val fill by animateFloatAsState(
        targetValue = engine.progress.toFloat(),
        animationSpec = tween(durationMillis = 60, easing = LinearEasing),
        label = "progress",
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(5.dp)
            .clip(RoundedCornerShape(50))
            .background(palette.surface2),
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(fill.coerceIn(0f, 1f))
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(palette.amber.copy(alpha = 0.75f), palette.amber),
                    ),
                ),
        )
    }
}

@Composable
private fun Stage(
    engine: GameEngine,
    big: Boolean,
    shakeOffset: Int,
    answerTint: Color,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .offset { IntOffset(shakeOffset, 0) },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(if (big) 26.dp else 20.dp),
    ) {
        AnimatedContent(
            targetState = engine.problem.question,
            transitionSpec = {
                (fadeIn() + slideInVertically { it / 3 }) togetherWith fadeOut()
            },
            label = "question",
        ) { question ->
            Text(
                text = question,
                style = display(questionSize(question, big), Black),
                color = palette.chalk,
                maxLines = 1,
                textAlign = TextAlign.Center,
            )
        }

        val caretAlpha by rememberInfiniteTransition(label = "caret").animateFloat(
            initialValue = 0.18f,
            targetValue = 0.55f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 700, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "caret-blink",
        )
        val empty = engine.typed.isEmpty()
        val caretWidth by animateDpAsState(
            targetValue = if (empty) 54.dp else 150.dp,
            label = "caret-width",
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (!empty) {
                Text(
                    text = engine.typed,
                    style = display(if (big) 64f else 50f, Heavy),
                    color = answerTint,
                )
            }
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .offset(y = 6.dp)
                    .width(caretWidth)
                    .height(4.dp)
                    .clip(RoundedCornerShape(50))
                    .background(answerTint.copy(alpha = if (empty) caretAlpha else 0.45f)),
            )
        }

        Text(
            text = if (engine.feedback == GameEngine.Feedback.WRONG) "${engine.problem.answer}" else " ",
            style = display(15f, SemiBold),
            color = palette.bad.copy(
                alpha = if (engine.feedback == GameEngine.Feedback.WRONG) 1f else 0f,
            ),
        )
    }
}

/**
 * `minimumScaleFactor(0.55)` has no Compose equivalent below 1.8, so the size is
 * stepped down by length instead — long problems like `5716 + 3902` are the
 * only ones that ever needed it.
 */
private fun questionSize(question: String, big: Boolean): Float {
    val base = if (big) 74f else 56f
    return when {
        question.length <= 8 -> base
        question.length <= 11 -> base * 0.8f
        question.length <= 14 -> base * 0.66f
        else -> base * 0.55f
    }
}
