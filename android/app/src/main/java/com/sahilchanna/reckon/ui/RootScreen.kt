package com.sahilchanna.reckon.ui

import android.net.Uri
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import com.sahilchanna.reckon.model.GameEngine
import com.sahilchanna.reckon.model.GameMode
import com.sahilchanna.reckon.model.Operation
import com.sahilchanna.reckon.model.RoundResult
import com.sahilchanna.reckon.model.Store
import com.sahilchanna.reckon.ui.screens.GameScreen
import com.sahilchanna.reckon.ui.screens.HomeScreen
import com.sahilchanna.reckon.ui.screens.ResultScreen
import com.sahilchanna.reckon.ui.theme.palette
import com.sahilchanna.reckon.widget.ReckonWidgetRefresh

private enum class Route { HOME, PLAY, RESULT }

/**
 * Home → Game → Result, and the one place that owns the [Store] and the live
 * [GameEngine].
 *
 * The hardware-keyboard handler lives here rather than on each screen, matching
 * `RootView.swift`: a single handler at the root means the screens never fight
 * over focus, and Return means "start", "submit" or "again" depending only on
 * which route is showing.
 */
@Composable
fun RootScreen(
    deepLink: String? = null,
    onDeepLinkConsumed: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val store = remember { Store(context) }
    val scope = rememberCoroutineScope()
    val data by store.data.collectAsState()
    val haptics = rememberHaptics()

    var route by remember { mutableStateOf(Route.HOME) }
    var engine by remember { mutableStateOf<GameEngine?>(null) }
    var result by remember { mutableStateOf<RoundResult?>(null) }
    var lastDrill by remember { mutableStateOf<List<Operation>?>(null) }

    ReckonHaptics.warmUp()

    // The widget reads the same save file, so refresh its timeline whenever the
    // save changes — that is what WidgetCenter.reloadAllTimelines() does on iOS.
    LaunchedEffect(data) {
        runCatching { ReckonWidgetRefresh.refresh(context) }
    }

    fun begin(mode: GameMode, drill: List<Operation>?) {
        lastDrill = drill
        val game = GameEngine(
            mode = mode,
            operations = drill ?: data.enabledOperations,
            level = data.level,
            scope = scope,
        )
        game.start { finished ->
            result = store.record(finished)
            route = Route.RESULT
        }
        engine = game
        route = Route.PLAY
    }

    fun quit() {
        engine?.abandon()
        engine = null
        route = Route.HOME
    }

    fun again() {
        val finished = result ?: return
        begin(if (finished.mode == GameMode.DAILY) GameMode.SPRINT else finished.mode, lastDrill)
    }

    fun startFromHome() = begin(GameMode.SPRINT, null)

    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(route) {
        // A frame later on iOS; here the requester is simply retried once the
        // new screen has been laid out.
        runCatching { focusRequester.requestFocus() }
    }

    // reckon://sprint and reckon://daily — what the widget taps into.
    LaunchedEffect(deepLink) {
        val link = deepLink ?: return@LaunchedEffect
        if (route != Route.PLAY) {
            when (Uri.parse(link).host) {
                "sprint" -> begin(GameMode.SPRINT, null)
                "daily" ->
                    if (!data.playedDailyToday) begin(GameMode.DAILY, null) else route = Route.HOME
            }
        }
        onDeepLinkConsumed()
    }

    fun onKey(event: KeyEvent): Boolean {
        if (event.type != KeyEventType.KeyDown) return false

        return when (route) {
            Route.HOME -> {
                if (event.isReturn()) {
                    startFromHome()
                    true
                } else {
                    false
                }
            }

            Route.PLAY -> {
                val live = engine ?: return false
                val digit = digitFor(event.key)
                if (digit != null) {
                    haptics.tap()
                    live.type(digit)
                    return true
                }
                when (event.key) {
                    Key.Backspace, Key.Delete -> {
                        haptics.tap()
                        live.backspace()
                        true
                    }
                    Key.Escape -> {
                        quit()
                        true
                    }
                    Key.Enter, Key.NumPadEnter -> {
                        live.commit()
                        true
                    }
                    else -> false
                }
            }

            Route.RESULT -> when (event.key) {
                Key.Enter, Key.NumPadEnter -> {
                    again()
                    true
                }
                Key.Escape -> {
                    route = Route.HOME
                    true
                }
                else -> false
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(palette.ink)
            .safeDrawingPadding()
            .focusRequester(focusRequester)
            .focusable()
            .onPreviewKeyEvent { onKey(it) },
    ) {
        AnimatedContent(
            targetState = route,
            transitionSpec = {
                when {
                    targetState == Route.RESULT ->
                        (fadeIn(tween(380)) + slideInVertically(tween(380)) { it / 8 }) togetherWith
                            fadeOut(tween(200))

                    targetState.ordinal > initialState.ordinal ->
                        (fadeIn(tween(380)) + scaleIn(tween(380), initialScale = 1.02f)) togetherWith
                            fadeOut(tween(200))

                    else ->
                        (fadeIn(tween(380)) + scaleIn(tween(380), initialScale = 0.98f)) togetherWith
                            fadeOut(tween(200))
                }
            },
            label = "route",
        ) { current ->
            when (current) {
                Route.HOME -> HomeScreen(
                    data = data,
                    onStart = ::begin,
                    onToggleOp = store::toggle,
                    onSetLevel = store::setLevel,
                    onReset = store::reset,
                )

                Route.PLAY -> engine?.let { GameScreen(engine = it, onQuit = ::quit) }

                Route.RESULT -> result?.let {
                    ResultScreen(
                        result = it,
                        onAgain = ::again,
                        onHome = { route = Route.HOME },
                    )
                }
            }
        }
    }
}

private fun KeyEvent.isReturn(): Boolean = key == Key.Enter || key == Key.NumPadEnter

/** Digits from the top row and the numeric keypad, or null for anything else. */
private fun digitFor(key: Key): String? = when (key) {
    Key.Zero, Key.NumPad0 -> "0"
    Key.One, Key.NumPad1 -> "1"
    Key.Two, Key.NumPad2 -> "2"
    Key.Three, Key.NumPad3 -> "3"
    Key.Four, Key.NumPad4 -> "4"
    Key.Five, Key.NumPad5 -> "5"
    Key.Six, Key.NumPad6 -> "6"
    Key.Seven, Key.NumPad7 -> "7"
    Key.Eight, Key.NumPad8 -> "8"
    Key.Nine, Key.NumPad9 -> "9"
    else -> null
}
