package it.manu.fuoriorario.feature.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fuoriorario.composeapp.generated.resources.Res
import fuoriorario.composeapp.generated.resources.login_change_email
import fuoriorario.composeapp.generated.resources.login_code
import fuoriorario.composeapp.generated.resources.login_code_hint
import fuoriorario.composeapp.generated.resources.login_code_title
import fuoriorario.composeapp.generated.resources.login_email
import fuoriorario.composeapp.generated.resources.login_email_hint
import fuoriorario.composeapp.generated.resources.login_error_code
import fuoriorario.composeapp.generated.resources.login_send
import fuoriorario.composeapp.generated.resources.login_title
import fuoriorario.composeapp.generated.resources.login_verify
import fuoriorario.composeapp.generated.resources.session_expired
import it.manu.fuoriorario.core.designsystem.Field
import it.manu.fuoriorario.core.designsystem.GhostButton
import it.manu.fuoriorario.core.designsystem.Panel
import it.manu.fuoriorario.core.designsystem.PrimaryButton
import it.manu.fuoriorario.core.theme.FuoriOrarioTheme
import it.manu.fuoriorario.core.viewmodel.UseCaseMutableState
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/** Email → 6-digit code. [expired]: the previous session ended without a sign-out, so say why we're back here. */
@Composable
fun LoginScreen(expired: Boolean, vm: LoginViewModel = koinViewModel()) {
    val uiState = vm.uiState.collectAsStateWithLifecycle()
    LoginStateContent(
        uiState.value.state,
        expired,
        onEmailChanged = vm::onEmailChanged,
        onCodeChanged = vm::onCodeChanged,
        onSendCode = vm::onSendCode,
        onVerify = vm::onVerify,
        onChangeEmail = vm::onChangeEmail
    )
}

@Composable
fun LoginStateContent(
    state: UseCaseMutableState<LoginScreenState>?,
    expired: Boolean,
    onEmailChanged: (String) -> Unit,
    onCodeChanged: (String) -> Unit,
    onSendCode: () -> Unit,
    onVerify: () -> Unit,
    onChangeEmail: () -> Unit
) {
    when (state) {
        is UseCaseMutableState.Error -> state.handler.ErrorScreenContent()
        is UseCaseMutableState.ShowData ->
            LoginContent(state.items, expired, onEmailChanged, onCodeChanged, onSendCode, onVerify, onChangeEmail)
        // The form is there from the start: nothing loads before it.
        UseCaseMutableState.Loading, null -> Unit
    }
}

@Composable
fun LoginContent(
    state: LoginScreenState,
    expired: Boolean,
    onEmailChanged: (String) -> Unit,
    onCodeChanged: (String) -> Unit,
    onSendCode: () -> Unit,
    onVerify: () -> Unit,
    onChangeEmail: () -> Unit
) {
    val c = FuoriOrarioTheme.colors
    Panel {
        if (expired) {
            Text(
                stringResource(Res.string.session_expired),
                color = c.accent,
                style = MaterialTheme.typography.bodyMedium
            )
        }
        if (!state.codeSent) {
            Text(stringResource(Res.string.login_title).uppercase(), style = MaterialTheme.typography.titleLarge)
            Text(stringResource(Res.string.login_email_hint), color = c.muted)
            Field(
                label = stringResource(Res.string.login_email),
                value = state.email,
                onValueChange = onEmailChanged,
                tag = "email",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Send),
                onDone = onSendCode
            )
        } else {
            Text(stringResource(Res.string.login_code_title).uppercase(), style = MaterialTheme.typography.titleLarge)
            Text(stringResource(Res.string.login_code_hint, state.normalizedEmail), color = c.muted)
            Field(
                label = stringResource(Res.string.login_code),
                value = state.code,
                onValueChange = onCodeChanged,
                tag = "code",
                keyboardOptions = KeyboardOptions(
                    // Not NumberPassword: Android would offer to save the one-time code as a password.
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done
                ),
                onDone = onVerify,
                textStyle = MaterialTheme.typography.titleLarge.copy(letterSpacing = 0.3.em, fontSize = 26.sp)
            )
        }
        state.error?.let { Text(stringResource(it), color = c.accent, style = MaterialTheme.typography.bodyMedium) }
        if (!state.codeSent) {
            PrimaryButton(
                stringResource(Res.string.login_send),
                onSendCode,
                Modifier.fillMaxWidth(),
                enabled = state.canSend
            )
        } else {
            PrimaryButton(
                stringResource(Res.string.login_verify),
                onVerify,
                Modifier.fillMaxWidth(),
                enabled = state.canVerify
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                GhostButton(stringResource(Res.string.login_change_email), onChangeEmail)
            }
        }
    }
}

@Preview
@Composable
private fun LoginContentPreview() = FuoriOrarioTheme {
    LoginContent(LoginScreenState(email = "luca@example.com"), expired = true, {}, {}, {}, {}, {})
}

@Preview
@Composable
private fun LoginContentCodePreview() = FuoriOrarioTheme {
    LoginContent(
        LoginScreenState(
            email = "luca@example.com",
            code = "1234",
            codeSent = true,
            error = Res.string.login_error_code
        ),
        expired = false,
        {},
        {},
        {},
        {},
        {}
    )
}
