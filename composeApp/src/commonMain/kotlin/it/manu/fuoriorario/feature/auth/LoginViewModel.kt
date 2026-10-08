package it.manu.fuoriorario.feature.auth

import androidx.lifecycle.viewModelScope
import fuoriorario.composeapp.generated.resources.Res
import fuoriorario.composeapp.generated.resources.login_error_code
import fuoriorario.composeapp.generated.resources.login_error_generic
import fuoriorario.composeapp.generated.resources.login_error_not_in_team
import it.manu.fuoriorario.core.error.ErrorManager
import it.manu.fuoriorario.core.viewmodel.ComposeViewModel
import it.manu.fuoriorario.core.viewmodel.DispatcherProvider
import it.manu.fuoriorario.core.viewmodel.UiState
import it.manu.fuoriorario.core.viewmodel.UseCaseMutableState
import it.manu.fuoriorario.feature.auth.data.AuthRepository
import it.manu.fuoriorario.feature.auth.data.InvalidCodeException
import it.manu.fuoriorario.feature.auth.data.NotInTeamException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource

const val CODE_LENGTH = 6

/** The login form: email, then the 6-digit code. [error] is the form's, shown under the field. */
data class LoginScreenState(
    val email: String = "",
    val code: String = "",
    val codeSent: Boolean = false,
    val busy: Boolean = false,
    val error: StringResource? = null
) {
    val normalizedEmail get() = email.trim().lowercase()
    val canSend get() = "@" in normalizedEmail && !busy
    val canVerify get() = code.length == CODE_LENGTH && !busy
}

/** Email → 6-digit code. Signing in is the whole outcome: the session moves the app on (MainViewModel). */
class LoginViewModel(private val auth: AuthRepository, dispatchers: DispatcherProvider, errorManager: ErrorManager) :
    ComposeViewModel<LoginScreenState>(
        defaultState = LoginScreenState().let { UiState(UseCaseMutableState.ShowData(it), it) },
        dispatcherProvider = dispatchers,
        errorManager = errorManager
    ) {
    private val state get() = uiState.value.data ?: LoginScreenState()

    fun onEmailChanged(email: String) = update { copy(email = email) }

    fun onCodeChanged(code: String) = update { copy(code = code.filter(Char::isDigit).take(CODE_LENGTH)) }

    fun onSendCode() {
        if (!state.canSend) return
        val email = state.normalizedEmail
        submit({ auth.sendCode(email) }) { copy(codeSent = true) }
    }

    fun onVerify() {
        if (!state.canVerify) return
        val (email, code) = state.normalizedEmail to state.code
        submit({ auth.verifyCode(email, code) })
    }

    fun onChangeEmail() = update { copy(codeSent = false, code = "", error = null) }

    /** Domain refusals become the form's error; anything else the generic one, inline as before the rework. */
    private fun submit(call: suspend () -> Unit, onDone: LoginScreenState.() -> LoginScreenState = { this }) {
        update { copy(busy = true, error = null) }
        defaultLaunchForChannels(errorFunction = { fail(Res.string.login_error_generic) }) {
            try {
                call()
                set { onDone().copy(busy = false) }
            } catch (_: NotInTeamException) {
                fail(Res.string.login_error_not_in_team)
            } catch (_: InvalidCodeException) {
                fail(Res.string.login_error_code)
            }
        }
    }

    private suspend fun fail(error: StringResource) = set { copy(busy = false, error = error) }

    private suspend fun set(change: LoginScreenState.() -> LoginScreenState) = emitSuccess(state.change())

    /** Unconfined runs it before returning: a keystroke lands before the next one, as with a `remember`. */
    private fun update(change: LoginScreenState.() -> LoginScreenState) {
        viewModelScope.launch(Dispatchers.Unconfined) { set(change) }
    }
}
