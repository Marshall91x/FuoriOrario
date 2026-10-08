package it.manu.fuoriorario.core.error

import androidx.compose.runtime.Composable
import fuoriorario.composeapp.generated.resources.Res
import fuoriorario.composeapp.generated.resources.load_failed
import org.jetbrains.compose.resources.stringResource

// Copied from sinetwork (error/FallbackHandler.kt), drawn like the app's other error screens.

class FallbackHandler : ErrorHandler {
    override var e: Exception? = null

    override fun canHandleError(error: Exception): Boolean = true

    @Composable
    override fun ErrorScreenContent() {
        ErrorContent(stringResource(Res.string.load_failed), onRetry = null)
    }
}
