package com.sahilchanna.reckon.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Rounded, tight and tabular — numbers should never jitter as they change.
 *
 * iOS gets "rounded" from the system font's design axis. Android has no
 * rounded system family to ask for, so these keep the tabular-figures half of
 * the promise (the part that matters for a live clock and a typed answer) and
 * fall back to the platform sans. Everything else — size, weight, tracking —
 * matches the Swift call sites.
 */
fun display(size: Float, weight: FontWeight = FontWeight.Bold): TextStyle =
    TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = weight,
        fontSize = size.sp,
        fontFeatureSettings = "tnum",
    )

fun ui(size: Float, weight: FontWeight = FontWeight.Normal): TextStyle =
    TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = weight,
        fontSize = size.sp,
    )

/** Named weight aliases, so call sites read like the Swift suffixes. */
val Heavy: FontWeight get() = FontWeight.ExtraBold
val Black: FontWeight get() = FontWeight.Black
val SemiBold: FontWeight get() = FontWeight.SemiBold
val Medium: FontWeight get() = FontWeight.Medium
val Bold: FontWeight get() = FontWeight.Bold

/**
 * Material components (dialogs, ripples) should not look like a different app.
 *
 * Left at its defaults on purpose: every Text in the app passes one of the
 * styles above, and Material 3's default family is already the platform sans on
 * Android — which is what [ui] and [display] ask for too.
 */
val ReckonTypography = Typography()
