package com.sahilchanna.reckon.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * One palette, two schemes. Dark is the house style; light is a proper
 * paper-and-ink variant rather than an inversion.
 *
 * Android's resource qualifiers would give the same result through `-night`
 * values, but a composition local keeps the two schemes side by side the way
 * `Palette.swift` does, so a change to one is obvious against the other.
 */
@Immutable
data class ReckonPalette(
    val ink: Color,
    val surface: Color,
    val surface2: Color,
    val line: Color,
    val chalk: Color,
    val mute: Color,
    val amber: Color,
    val good: Color,
    val bad: Color,
)

/** `rgb(0xF3F1EC)` reads the same as the Swift call sites; alpha is always opaque. */
private fun rgb(hex: Long): Color = Color(0xFF000000L or hex)

val DarkPalette = ReckonPalette(
    ink = rgb(0x0B0F12),
    surface = rgb(0x151C22),
    surface2 = rgb(0x1D262E),
    line = rgb(0x2A343C),
    chalk = rgb(0xE9EFF2),
    mute = rgb(0x8797A2),
    amber = rgb(0xFFB020),
    good = rgb(0x3DDC97),
    bad = rgb(0xFF5D5D),
)

val LightPalette = ReckonPalette(
    ink = rgb(0xF3F1EC),
    surface = rgb(0xFFFFFF),
    surface2 = rgb(0xEBE8E1),
    line = rgb(0xDBD6CC),
    chalk = rgb(0x14181B),
    mute = rgb(0x6B7580),
    amber = rgb(0xB86E00),
    good = rgb(0x0E7A55),
    bad = rgb(0xC22B2B),
)

/**
 * Dark by default: that is what the app looks like before the system has had a
 * say, and it is the scheme the design was drawn in.
 */
val LocalReckonPalette = staticCompositionLocalOf { DarkPalette }

/** The active palette. Named lower-case so it reads like a value, not a type. */
val palette: ReckonPalette
    @Composable
    @ReadOnlyComposable
    get() = LocalReckonPalette.current

/** The two label styles the app uses, matching `Fmt` in `Shared/Palette.swift`. */
object Fmt {
    fun seconds(t: Double, decimals: Int = 2): String = "%.${decimals}f".format(t) + "s"

    fun percent(v: Double): String = "${kotlin.math.round(v * 100).toInt()}%"
}
