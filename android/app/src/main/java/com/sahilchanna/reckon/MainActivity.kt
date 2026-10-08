package com.sahilchanna.reckon

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.sahilchanna.reckon.ui.ReckonApp

/**
 * The single Activity. Its only job beyond hosting the Compose tree is to turn
 * the `reckon://` deep link the widget fires into a value the root screen can
 * act on.
 *
 * The link is tracked through `addOnNewIntentListener` rather than an override
 * of `onNewIntent`: the listener is public API and its parameter is non-null,
 * so it cannot drift with the framework's nullability.
 */
class MainActivity : ComponentActivity() {

    private var deepLink by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        deepLink = intent?.data?.toString()
        addOnNewIntentListener { newIntent ->
            setIntent(newIntent)
            deepLink = newIntent.data?.toString()
        }

        setContent {
            ReckonApp(
                deepLink = deepLink,
                onDeepLinkConsumed = { deepLink = null },
            )
        }
    }
}
