package it.manu.fuoriorario.core.error

// Copied from sinetwork (error/ErrorManager.kt), without `context`: the texts come from Compose Resources (ADR 0009).

interface ErrorManager {
    val handlers: List<ErrorHandler>

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
