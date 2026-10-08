package com.sahilchanna.reckon.ui

import androidx.compose.runtime.Composable
import com.sahilchanna.reckon.ui.theme.ReckonTheme

/**
 * The Compose tree, the equivalent of `ReckonApp.swift`: theme in, root screen
 * out. Kept separate from `MainActivity` so a preview or a test can host the
 * whole app without an Activity.
 */
@Composable
fun ReckonApp(
    deepLink: String? = null,
    onDeepLinkConsumed: () -> Unit = {},
) {
    ReckonTheme {
        RootScreen(deepLink = deepLink, onDeepLinkConsumed = onDeepLinkConsumed)
    }
}
