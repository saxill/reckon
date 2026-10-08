package com.sahilchanna.reckon.ui

import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView

/**
 * The equivalents of `Haptics.swift`'s four signals.
 *
 * iOS prepares its generators up front because an unprepareed one can lag the
 * first tap by ~100ms. Android's `performHapticFeedback` has no prepare step —
 * the platform owns the timing — so [warmUp] exists only to keep the call sites
 * identical and is deliberately empty.
 *
 * CONFIRM and REJECT are the right signals for a correct or missed answer, but
 * they only landed in API 30; on older devices the framework ignores unknown
 * constants, so the fallbacks below are what a 26–29 phone actually feels.
 */
class ReckonHaptics(private val view: View) {

    fun tap() = buzz(HapticFeedbackConstants.VIRTUAL_KEY)

    fun pop() = buzz(HapticFeedbackConstants.VIRTUAL_KEY)

    fun success() = buzz(
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) HapticFeedbackConstants.CONFIRM
        else HapticFeedbackConstants.VIRTUAL_KEY,
    )

    fun error() = buzz(
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) HapticFeedbackConstants.REJECT
        else HapticFeedbackConstants.LONG_PRESS,
    )

    private fun buzz(constant: Int) {
        // Respects the system "touch feedback" setting, which is exactly what
        // the user asked for when they turned it off.
        if (view.isHapticFeedbackEnabled) view.performHapticFeedback(constant)
    }

    companion object {
        fun warmUp() = Unit
    }
}

@Composable
fun rememberHaptics(): ReckonHaptics {
    val view = LocalView.current
    return remember(view) { ReckonHaptics(view) }
}
