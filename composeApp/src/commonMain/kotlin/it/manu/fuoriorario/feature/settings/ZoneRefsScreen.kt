package it.manu.fuoriorario.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fuoriorario.composeapp.generated.resources.Res
import fuoriorario.composeapp.generated.resources.refs_default
import fuoriorario.composeapp.generated.resources.refs_error
import fuoriorario.composeapp.generated.resources.refs_hint
import fuoriorario.composeapp.generated.resources.refs_restore
import fuoriorario.composeapp.generated.resources.refs_save
import fuoriorario.composeapp.generated.resources.refs_title
import it.manu.fuoriorario.core.designsystem.Field
import it.manu.fuoriorario.core.designsystem.GhostButton
import it.manu.fuoriorario.core.designsystem.LocalToast
import it.manu.fuoriorario.core.designsystem.Panel
import it.manu.fuoriorario.core.designsystem.PrimaryButton
import it.manu.fuoriorario.core.designsystem.show
import it.manu.fuoriorario.core.theme.FuoriOrarioTheme
import it.manu.fuoriorario.core.viewmodel.UseCaseMutableState
import it.manu.fuoriorario.domain.DEFAULT_ZONE_REFS
import it.manu.fuoriorario.domain.Zone
import it.manu.fuoriorario.feature.shots.zoneName
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/**
 * Staff's Riferimenti (PRD F5): the expected percentage per zone, two per row, each with its default below.
 * Restore only fills in the defaults: nothing is stored until Salva, which colours every player's map.
 */
@Composable
fun ZoneRefsScreen(vm: ZoneRefsViewModel = koinViewModel()) {
    val uiState = vm.uiState.collectAsStateWithLifecycle()
    val toast = LocalToast.current
    LaunchedEffect(vm) { vm.toasts.collect { launch { toast.show(it) } } }
    ZoneRefsStateContent(
        uiState.value.state,
        ZoneRefsActions(onRefChanged = vm::onRefChanged, onSave = vm::onSave, onRestore = vm::onRestore)
    )
}

/** What the riferimenti do: edit a field, save, restore the defaults. */
class ZoneRefsActions(
    val onRefChanged: (Zone, String) -> Unit = { _, _ -> },
    val onSave: () -> Unit = {},
    val onRestore: () -> Unit = {}
)

@Composable
fun ZoneRefsStateContent(state: UseCaseMutableState<ZoneRefsScreenState>?, actions: ZoneRefsActions) {
    when (state) {
        is UseCaseMutableState.Error -> state.handler.ErrorScreenContent()
        is UseCaseMutableState.ShowData -> ZoneRefsContent(state.items, actions)
        UseCaseMutableState.Loading, null -> Unit
    }
}

/** Nothing until the riferimenti arrive. */
@Composable
fun ZoneRefsContent(state: ZoneRefsScreenState, actions: ZoneRefsActions) {
    val c = FuoriOrarioTheme.colors
    val fields = state.draft ?: return
    Panel {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(stringResource(Res.string.refs_title).uppercase(), style = MaterialTheme.typography.titleLarge)
            Text(stringResource(Res.string.refs_hint), color = c.muted, style = MaterialTheme.typography.bodyMedium)
        }
        Zone.entries.chunked(2).forEach { pair ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                pair.forEach { zone ->
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Field(
                            stringResource(zoneName.getValue(zone)),
                            fields.getValue(zone),
                            { actions.onRefChanged(zone, it) },
                            "ref_${zone.name.lowercase()}",
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                        Text(
                            stringResource(Res.string.refs_default, "${DEFAULT_ZONE_REFS.getValue(zone)}%"),
                            color = c.muted,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
        }
        if (state.invalid) {
            Text(stringResource(Res.string.refs_error), color = c.accent, style = MaterialTheme.typography.bodyMedium)
        }
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            PrimaryButton(stringResource(Res.string.refs_save), actions.onSave, enabled = !state.busy)
            GhostButton(stringResource(Res.string.refs_restore), actions.onRestore)
        }
    }
}

@Preview
@Composable
private fun ZoneRefsContentPreview() = FuoriOrarioTheme {
    Column {
        ZoneRefsContent(ZoneRefsScreenState(DEFAULT_ZONE_REFS.mapValues { it.value.toString() }), ZoneRefsActions())
    }
}
