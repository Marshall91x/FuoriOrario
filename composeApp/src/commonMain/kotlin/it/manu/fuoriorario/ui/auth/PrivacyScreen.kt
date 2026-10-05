package it.manu.fuoriorario.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import fuoriorario.composeapp.generated.resources.Res
import fuoriorario.composeapp.generated.resources.login_error_generic
import fuoriorario.composeapp.generated.resources.privacy_accept
import fuoriorario.composeapp.generated.resources.privacy_controller_body
import fuoriorario.composeapp.generated.resources.privacy_controller_title
import fuoriorario.composeapp.generated.resources.privacy_data_body
import fuoriorario.composeapp.generated.resources.privacy_data_title
import fuoriorario.composeapp.generated.resources.privacy_delete_body
import fuoriorario.composeapp.generated.resources.privacy_delete_title
import fuoriorario.composeapp.generated.resources.privacy_intro
import fuoriorario.composeapp.generated.resources.privacy_purpose_body
import fuoriorario.composeapp.generated.resources.privacy_purpose_title
import fuoriorario.composeapp.generated.resources.privacy_title
import fuoriorario.composeapp.generated.resources.privacy_where_body
import fuoriorario.composeapp.generated.resources.privacy_where_title
import it.manu.fuoriorario.data.AuthRepository
import it.manu.fuoriorario.ui.components.Panel
import it.manu.fuoriorario.ui.components.PrimaryButton
import it.manu.fuoriorario.ui.theme.FuoriOrarioTheme
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

private val sections = listOf(
    Res.string.privacy_controller_title to Res.string.privacy_controller_body,
    Res.string.privacy_data_title to Res.string.privacy_data_body,
    Res.string.privacy_purpose_title to Res.string.privacy_purpose_body,
    Res.string.privacy_where_title to Res.string.privacy_where_body,
    Res.string.privacy_delete_title to Res.string.privacy_delete_body
)

/** First-access privacy notice (ADR 0006). "Ho letto" records the acceptance; the session then moves on. */
@Composable
fun PrivacyScreen(auth: AuthRepository) {
    val c = FuoriOrarioTheme.colors
    var busy by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Panel {
        Text(stringResource(Res.string.privacy_title).uppercase(), style = MaterialTheme.typography.titleLarge)
        Text(stringResource(Res.string.privacy_intro), color = c.muted)
        sections.forEach { (title, body) ->
            // Prototype `.label` over body text.
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(title).uppercase(), style = MaterialTheme.typography.labelSmall, color = c.muted)
                Text(stringResource(body), style = MaterialTheme.typography.bodyMedium)
            }
        }
        if (failed) {
            Text(
                stringResource(Res.string.login_error_generic),
                color = c.accent,
                style = MaterialTheme.typography.bodyMedium
            )
        }
        PrimaryButton(
            stringResource(Res.string.privacy_accept),
            {
                scope.launch {
                    busy = true
                    failed = false
                    try {
                        auth.acknowledgePrivacy()
                    } catch (e: CancellationException) {
                        throw e
                    } catch (_: Exception) {
                        failed = true
                    } finally {
                        busy = false
                    }
                }
            },
            Modifier.fillMaxWidth(),
            enabled = !busy
        )
    }
}
