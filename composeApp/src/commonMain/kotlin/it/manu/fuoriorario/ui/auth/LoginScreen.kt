package it.manu.fuoriorario.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import fuoriorario.composeapp.generated.resources.Res
import fuoriorario.composeapp.generated.resources.login_change_email
import fuoriorario.composeapp.generated.resources.login_code
import fuoriorario.composeapp.generated.resources.login_code_hint
import fuoriorario.composeapp.generated.resources.login_code_title
import fuoriorario.composeapp.generated.resources.login_email
import fuoriorario.composeapp.generated.resources.login_email_hint
import fuoriorario.composeapp.generated.resources.login_error_code
import fuoriorario.composeapp.generated.resources.login_error_generic
import fuoriorario.composeapp.generated.resources.login_error_not_in_team
import fuoriorario.composeapp.generated.resources.login_send
import fuoriorario.composeapp.generated.resources.login_title
import fuoriorario.composeapp.generated.resources.login_verify
import it.manu.fuoriorario.data.AuthRepository
import it.manu.fuoriorario.data.InvalidCodeException
import it.manu.fuoriorario.data.NotInTeamException
import it.manu.fuoriorario.ui.components.Field
import it.manu.fuoriorario.ui.components.GhostButton
import it.manu.fuoriorario.ui.components.Panel
import it.manu.fuoriorario.ui.components.PrimaryButton
import it.manu.fuoriorario.ui.theme.FuoriOrarioTheme
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

private const val CODE_LENGTH = 6

/** Email → 6-digit code. On success the repository's session switches the app to the signed-in UI. */
@Composable
fun LoginScreen(auth: AuthRepository) {
    val c = FuoriOrarioTheme.colors
    var email by rememberSaveable { mutableStateOf("") }
    var code by rememberSaveable { mutableStateOf("") }
    var codeSent by rememberSaveable { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<StringResource?>(null) }
    val scope = rememberCoroutineScope()
    val normalizedEmail = email.trim().lowercase()

    fun submit(action: suspend () -> Unit) {
        if (busy) return
        scope.launch {
            busy = true
            error = null
            try {
                action()
            } catch (e: CancellationException) {
                throw e
            } catch (_: NotInTeamException) {
                error = Res.string.login_error_not_in_team
            } catch (_: InvalidCodeException) {
                error = Res.string.login_error_code
            } catch (_: Exception) {
                error = Res.string.login_error_generic
            } finally {
                busy = false
            }
        }
    }

    val canSend = "@" in normalizedEmail && !busy
    val sendCode = { if (canSend) submit { auth.sendCode(normalizedEmail).also { codeSent = true } } }
    val canVerify = code.length == CODE_LENGTH && !busy
    val verify = { if (canVerify) submit { auth.verifyCode(normalizedEmail, code) } }

    Panel {
        if (!codeSent) {
            Text(stringResource(Res.string.login_title).uppercase(), style = MaterialTheme.typography.titleLarge)
            Text(stringResource(Res.string.login_email_hint), color = c.muted)
            Field(
                label = stringResource(Res.string.login_email),
                value = email,
                onValueChange = { email = it },
                tag = "email",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Send),
                onDone = sendCode
            )
        } else {
            Text(stringResource(Res.string.login_code_title).uppercase(), style = MaterialTheme.typography.titleLarge)
            Text(stringResource(Res.string.login_code_hint, normalizedEmail), color = c.muted)
            Field(
                label = stringResource(Res.string.login_code),
                value = code,
                onValueChange = { new -> code = new.filter(Char::isDigit).take(CODE_LENGTH) },
                tag = "code",
                keyboardOptions = KeyboardOptions(
                    // Not NumberPassword: Android would offer to save the one-time code as a password.
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done
                ),
                onDone = verify,
                textStyle = MaterialTheme.typography.titleLarge.copy(letterSpacing = 0.3.em, fontSize = 26.sp)
            )
        }
        error?.let { Text(stringResource(it), color = c.accent, style = MaterialTheme.typography.bodyMedium) }
        if (!codeSent) {
            PrimaryButton(stringResource(Res.string.login_send), sendCode, Modifier.fillMaxWidth(), enabled = canSend)
        } else {
            PrimaryButton(stringResource(Res.string.login_verify), verify, Modifier.fillMaxWidth(), enabled = canVerify)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                GhostButton(stringResource(Res.string.login_change_email), {
                    codeSent = false
                    code = ""
                    error = null
                })
            }
        }
    }
}
