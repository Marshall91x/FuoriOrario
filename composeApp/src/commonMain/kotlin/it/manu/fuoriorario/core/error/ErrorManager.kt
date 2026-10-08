package it.manu.fuoriorario.core.error

import fuoriorario.composeapp.generated.resources.Res
import fuoriorario.composeapp.generated.resources.save_denied
import fuoriorario.composeapp.generated.resources.save_failed
import fuoriorario.composeapp.generated.resources.session_expired
import org.jetbrains.compose.resources.getString

// Copied from sinetwork (error/ErrorManager.kt). In place of `context`, which sinetwork's `checkError(e, context)`
// read the texts from, [checkError] itself: the texts come from Compose Resources (ADR 0009).

interface ErrorManager {
    val handlers: List<ErrorHandler>

    /**
     * The toast text for a failed action. Never `e.message`, as sinetwork did: Supabase's are technical and in
     * English, and the app has always said "Salvataggio non riuscito" instead.
     */
    suspend fun checkError(error: Exception): String = getString(
        when {
            error.isSessionRefused() -> Res.string.session_expired
            error is PermissionDeniedException -> Res.string.save_denied
            else -> Res.string.save_failed
        }
    )

    fun handle(error: Exception): ErrorHandler {
        handlers.forEach { handler ->
            if (handler.canHandleError(error)) {
                handler.e = error
                (handler as? ErrorHandlerWithRetry)?.setFunctionRetry(null)
                return handler
            }
        }
        return FallbackHandler().also {
            it.e = error
        }
    }

    fun handle(error: Exception, retryFunction: (() -> Unit)): ErrorHandler {
        handlers.forEach { handler ->
            if (handler.canHandleError(error)) {
                handler.e = error
                (handler as? ErrorHandlerWithRetry)?.setFunctionRetry(retryFunction)
                return handler
            }
        }
        return FallbackHandler().also {
            it.e = error
        }
    }
}
