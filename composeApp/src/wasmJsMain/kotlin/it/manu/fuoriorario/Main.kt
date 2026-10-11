package it.manu.fuoriorario

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import fuoriorario.composeapp.generated.resources.Res
import fuoriorario.composeapp.generated.resources.allStringArrayResources
import fuoriorario.composeapp.generated.resources.allStringResources
import kotlinx.browser.document
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.getStringArray

/**
 * Loads every string before the first frame, like runAppTest. On the web a string loads on first use, and the load is
 * shared: if the composable that started it leaves mid-load, the others waiting keep "" for good (#88, the logo).
 */
@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    MainScope().launch {
        // A failed preload falls back to loading on first use: the app still starts.
        runCatching {
            Res.allStringResources.values.forEach { getString(it) }
            Res.allStringArrayResources.values.forEach { getStringArray(it) }
        }
        ComposeViewport(document.body!!) { App() }
    }
}
