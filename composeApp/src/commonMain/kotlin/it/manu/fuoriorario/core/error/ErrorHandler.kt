package it.manu.fuoriorario.core.error

import androidx.compose.runtime.Composable

// Copied from sinetwork (error/ErrorHandler.kt).

interface ErrorHandler {
    var e: Exception?

    fun canHandleError(error: Exception): Boolean

    @Composable
    fun ErrorScreenContent()
}

interface ErrorHandlerWithRetry : ErrorHandler {
    var retryFunction: (() -> Unit)?

    fun setFunctionRetry(retryFunction: (() -> Unit)?) {
        this.retryFunction = retryFunction
    }
}
