package com.sahilchanna.reckon.ui.theme

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

object Layout {
    /**
     * Phone-sized column, centred on tablets and in wide windows: a keypad
     * stretched across 1000dp is unusable and card rows become unreadable.
     */
    val maxContent: Dp = 620.dp
    val maxKeypad: Dp = 460.dp

    /** Landscape tablet / desktop windowing: question and keypad sit side by side. */
    fun isWide(width: Dp, height: Dp): Boolean = width > height && width >= 700.dp
}

@Composable
fun ReckonTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val p = if (darkTheme) DarkPalette else LightPalette

    // Material components (dialogs, the text cursor, ripples) pick their colours
    // from here, so they need the same values the hand-rolled surfaces use.
    val scheme = if (darkTheme) {
        darkColorScheme(
            primary = p.amber,
            onPrimary = p.ink,
            background = p.ink,
            onBackground = p.chalk,
            surface = p.surface,
            onSurface = p.chalk,
            surfaceVariant = p.surface2,
            onSurfaceVariant = p.mute,
            outline = p.line,
            error = p.bad,
            onError = p.ink,
        )
    } else {
        lightColorScheme(
            primary = p.amber,
            onPrimary = p.surface,
            background = p.ink,
            onBackground = p.chalk,
            surface = p.surface,
            onSurface = p.chalk,
            surfaceVariant = p.surface2,
            onSurfaceVariant = p.mute,
            outline = p.line,
            error = p.bad,
            onError = p.surface,
        )
    }

    CompositionLocalProvider(LocalReckonPalette provides p) {
        MaterialTheme(colorScheme = scheme, typography = ReckonTypography, content = content)
    }
}

/**
 * Card chrome used by every surface in the app.
 *
 * The background is painted before the padding is applied, so it covers the
 * padding the way SwiftUI's `.padding().background()` does; the border rides on
 * the same rounded shape, and the clip keeps charts from drawing past it.
 */
@Composable
fun Modifier.card(
    padding: Dp = 18.dp,
    radius: Dp = 20.dp,
): Modifier {
    val shape = RoundedCornerShape(radius)
    return this
        .clip(shape)
        .background(palette.surface, shape)
        .border(1.dp, palette.line, shape)
        .padding(padding)
}

/**
 * Stands in for SwiftUI's `.pressable()`: the label shrinks slightly while held,
 * and there is no ripple — the scale *is* the touch feedback, and a ripple on
 * top of it reads as two effects fighting.
 */
@Composable
fun PressButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    scale: Float = 0.97f,
    role: Role = Role.Button,
    contentAlignment: Alignment = Alignment.Center,
    content: @Composable BoxScope.() -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val current by animateFloatAsState(
        targetValue = if (pressed) scale else 1f,
        animationSpec = spring(
            dampingRatio = 0.6f,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "press-scale",
    )

    Box(
        // The press effects go on first so they wrap whatever chrome the caller
        // passes in: a `.card()` in `modifier` ends in padding, and if the
        // clickable sat inside that padding the card's margin would be dead.
        modifier = Modifier
            .graphicsLayer {
                scaleX = current
                scaleY = current
            }
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                role = role,
                onClick = onClick,
            )
            .then(modifier),
        contentAlignment = contentAlignment,
        content = content,
    )
}
