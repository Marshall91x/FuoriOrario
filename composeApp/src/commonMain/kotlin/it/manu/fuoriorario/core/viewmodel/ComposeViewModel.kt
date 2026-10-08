package it.manu.fuoriorario.core.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import it.manu.fuoriorario.core.error.ErrorHandler
import it.manu.fuoriorario.core.error.ErrorManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// Copied from sinetwork (utils/ComposeViewModel.kt): same public API. Adapted as ADR 0009 says: no Context
// (ErrorManager.checkError replaces checkError(e, context)), `::class.simpleName`, network errors are kotlinx.io.IOException.

abstract class ComposeViewModel<T>(
    defaultState: UiState<T> = UiState(),
    private val dispatcherProvider: DispatcherProvider = DefaultDispatcherProvider(),
    private val errorManager: ErrorManager
) : ViewModel() {
    private val _uiState: MutableStateFlow<UiState<T>> = MutableStateFlow(defaultState)

    val uiState: StateFlow<UiState<T>> = _uiState.asStateFlow()

    suspend fun emitLoading() = _uiState.emit(uiState.value.copy(state = UseCaseMutableState.Loading))

    suspend fun emitSuccess(data: T) = _uiState.emit(
        uiState.value.copy(
            state = UseCaseMutableState.ShowData(data),
            data = data
        )
    )

    suspend fun emitIdle() = _uiState.emit(uiState.value.copy(state = null))

    suspend fun emitError(handler: ErrorHandler) =
        _uiState.emit(_uiState.value.copy(state = UseCaseMutableState.Error(handler = handler)))

    fun defaultLaunch(
        dispatcher: CoroutineDispatcher = dispatcherProvider.io(),
        emitError: Boolean = true,
        block: suspend CoroutineScope.() -> Unit
    ) {
        defaultLaunchInternal(
            dispatcher = dispatcher,
            emitError = emitError,
            block = block
        )
    }

    fun defaultLaunchWithRetry(
        dispatcher: CoroutineDispatcher = dispatcherProvider.io(),
        emitError: Boolean = true,
        maxRetry: Int,
        retryFunction: (() -> Unit),
        block: suspend CoroutineScope.() -> Unit
    ) {
        defaultLaunchInternal(
            dispatcher = dispatcher,
            emitError = emitError,
            maxRetry = maxRetry,
            block = block,
            retryFunction = retryFunction
        )
    }

    fun defaultLaunchForChannels(
        dispatcher: CoroutineDispatcher = dispatcherProvider.io(),
        errorFunction: suspend (ErrorException) -> Unit,
        block: suspend CoroutineScope.() -> Unit
    ) {
        viewModelScope.launch(dispatcher + coroutineExceptionHandler) {
            try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                when (e) {
                    is CustomException -> {
                        errorFunction(
                            ErrorException(
                                e.code,
                                e.userMessage,
                                e.systemMessage
                            )
                        )
                    }

                    else -> {
                        println("Error: exception: ${e::class.simpleName} $e")
                        errorFunction(
                            ErrorException(
                                code = 0,
                                userMessage = errorManager.checkError(e),
                                systemMessage = e.message
                            )
                        )
                    }
                }
            }
        }
    }

    internal fun defaultLaunchInternal(
        dispatcher: CoroutineDispatcher = dispatcherProvider.io(),
        emitError: Boolean = true,
        maxRetry: Int = 3,
        retryFunction: ((() -> Unit)?) = null,
        block: suspend CoroutineScope.() -> Unit
    ) {
        var attempt = 0

        fun internalLaunch() {
            viewModelScope.launch(dispatcher + coroutineExceptionHandler) {
                try {
                    block()
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    if (emitError) {
                        if (isNetworkError(e, maxRetry = maxRetry, retryAttempt = attempt) {
                                attempt++
                                internalLaunch()
                            }
                        ) {
                            return@launch
                        }
                        retryFunction?.let { function ->
                            emitError(errorManager.handle(e, function))
                        } ?: emitError(errorManager.handle(e))
                    }
                }
            }
        }
        internalLaunch()
    }

    private val coroutineExceptionHandler =
        CoroutineExceptionHandler { _, exception ->
            println("Error: exception: ${exception::class.simpleName} $exception")
        }

    private suspend fun isNetworkError(e: Exception, maxRetry: Int, retryAttempt: Int, block: () -> Unit): Boolean {
        if (e.isNetworkException()) {
            if (retryAttempt < maxRetry) {
                emitError(
                    errorManager.handle(
                        error = e,
                        retryFunction = block
                    )
                )
            } else {
                emitError(
                    errorManager.handle(
                        error = e
                    )
                )
            }
            return true
        } else {
            return false
        }
    }

    fun resetState() {
        _uiState.value = _uiState.value.copy(state = null)
    }
}

interface DispatcherProvider {
    fun main(): CoroutineDispatcher = Dispatchers.Main

    fun default(): CoroutineDispatcher = Dispatchers.Default

    /** `Dispatchers.Default` on wasm, where `Dispatchers.IO` doesn't exist. */
    fun io(): CoroutineDispatcher = ioDispatcher

    fun unconfined(): CoroutineDispatcher = Dispatchers.Unconfined
}

class DefaultDispatcherProvider : DispatcherProvider

data class UiState<T>(val state: UseCaseMutableState<T>? = null, val data: T? = null)

internal expect val ioDispatcher: CoroutineDispatcher
