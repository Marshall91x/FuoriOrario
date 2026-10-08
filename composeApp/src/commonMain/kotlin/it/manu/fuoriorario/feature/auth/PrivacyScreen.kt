package it.manu.fuoriorario.feature.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import it.manu.fuoriorario.core.designsystem.Panel
import it.manu.fuoriorario.core.designsystem.PrimaryButton
import it.manu.fuoriorario.core.theme.FuoriOrarioTheme
import it.manu.fuoriorario.core.viewmodel.UseCaseMutableState
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

private val sections = listOf(
    Res.string.privacy_controller_title to Res.string.privacy_controller_body,
    Res.string.privacy_data_title to Res.string.privacy_data_body,
    Res.string.privacy_purpose_title to Res.string.privacy_purpose_body,
    Res.string.privacy_where_title to Res.string.privacy_where_body,
    Res.string.privacy_delete_title to Res.string.privacy_delete_body
)

/** First-access privacy notice (ADR 0006). "Ho letto" records the acceptance; the session then moves on. */
@Composable
fun PrivacyScreen(vm: PrivacyViewModel = koinViewModel()) {
    val uiState = vm.uiState.collectAsStateWithLifecycle()
    PrivacyStateContent(uiState.value.state, onAccept = vm::onAccept)
}

@Composable
fun PrivacyStateContent(state: UseCaseMutableState<PrivacyScreenState>?, onAccept: () -> Unit) {
    when (state) {
        is UseCaseMutableState.Error -> state.handler.ErrorScreenContent()
        is UseCaseMutableState.ShowData -> PrivacyContent(state.items, onAccept)
        // The notice is there from the start: nothing loads before it.
        UseCaseMutableState.Loading, null -> Unit
    }
}

@Composable
fun PrivacyContent(state: PrivacyScreenState, onAccept: () -> Unit) {
    val c = FuoriOrarioTheme.colors
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
        if (state.failed) {
            Text(
                stringResource(Res.string.login_error_generic),
                color = c.accent,
                style = MaterialTheme.typography.bodyMedium
            )
        }
        PrimaryButton(
            stringResource(Res.string.privacy_accept),
            onAccept,
            Modifier.fillMaxWidth(),
            enabled = !state.busy
        )
    }
}

@Preview
@Composable
private fun PrivacyContentPreview() = FuoriOrarioTheme { PrivacyContent(PrivacyScreenState(failed = true)) {} }
