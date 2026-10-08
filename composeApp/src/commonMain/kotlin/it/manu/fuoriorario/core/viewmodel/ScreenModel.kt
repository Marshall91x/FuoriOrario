package it.manu.fuoriorario.core.viewmodel

import androidx.lifecycle.viewModelScope
import it.manu.fuoriorario.core.error.ErrorManager
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString

// Not from sinetwork: what the screens that write share on top of ComposeViewModel.

/**
 * A screen that shows [initial] at once and fills it in as it loads, with actions that toast their outcome.
 * [busy] sets the screen's own "a write is in flight" flag.
 */
abstract class ScreenModel<T>(
    initial: T,
    private val dispatchers: DispatcherProvider,
    errorManager: ErrorManager,
    private val busy: T.(Boolean) -> T
) : ComposeViewModel<T>(
    defaultState = UiState(UseCaseMutableState.ShowData(initial), initial),
    dispatcherProvider = dispatchers,
    errorManager = errorManager
) {
    private val toastChannel = Channel<String>(Channel.BUFFERED)

    /** Messages for the toast: saved, deleted, or why an action failed. */
    val toasts = toastChannel.receiveAsFlow()

    protected val state get() = uiState.value.data!!

    protected suspend fun toast(message: StringResource) = toastChannel.send(getString(message))

    /** A write: a failure toasts and leaves everything on screen as it was, sheets included. */
    protected fun act(block: suspend () -> Unit) {
        update { busy(true) }
        defaultLaunchForChannels(dispatchers.main(), errorFunction = {
            set { busy(false) }
            it.userMessage?.let { message -> toastChannel.send(message) }
        }) {
            block()
            set { busy(false) }
        }
    }

    /** A write ending after a failed load leaves its error screen alone. */
    protected suspend fun set(change: T.() -> T) {
        if (uiState.value.state !is UseCaseMutableState.Error) emitSuccess(state.change())
    }

    /** Unconfined runs it before returning: a keystroke lands before the next one, as with a `remember`. */
    protected fun update(change: T.() -> T) {
        viewModelScope.launch(dispatchers.unconfined()) { set(change) }
    }
}
