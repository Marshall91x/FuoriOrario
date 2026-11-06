package it.manu.fuoriorario.feature.league

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fuoriorario.composeapp.generated.resources.Res
import fuoriorario.composeapp.generated.resources.cancel
import fuoriorario.composeapp.generated.resources.league_confirm
import fuoriorario.composeapp.generated.resources.league_delete
import fuoriorario.composeapp.generated.resources.league_delete_confirm
import fuoriorario.composeapp.generated.resources.league_fixtures
import fuoriorario.composeapp.generated.resources.league_import
import fuoriorario.composeapp.generated.resources.league_missing
import fuoriorario.composeapp.generated.resources.league_none
import fuoriorario.composeapp.generated.resources.league_pick_ours
import fuoriorario.composeapp.generated.resources.league_preview
import fuoriorario.composeapp.generated.resources.league_reading
import fuoriorario.composeapp.generated.resources.league_reimport
import fuoriorario.composeapp.generated.resources.league_summary
import fuoriorario.composeapp.generated.resources.league_title
import fuoriorario.composeapp.generated.resources.league_unreadable
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.readBytes
import it.manu.fuoriorario.core.designsystem.GhostButton
import it.manu.fuoriorario.core.designsystem.GhostStyle
import it.manu.fuoriorario.core.designsystem.Loader
import it.manu.fuoriorario.core.designsystem.LocalToast
import it.manu.fuoriorario.core.designsystem.Panel
import it.manu.fuoriorario.core.designsystem.PrimaryButton
import it.manu.fuoriorario.core.designsystem.TogglePill
import it.manu.fuoriorario.core.designsystem.show
import it.manu.fuoriorario.core.theme.FuoriOrarioTheme
import it.manu.fuoriorario.core.viewmodel.UseCaseMutableState
import it.manu.fuoriorario.domain.ImportFixture
import it.manu.fuoriorario.domain.ImportPreview
import it.manu.fuoriorario.domain.League
import it.manu.fuoriorario.domain.Leg
import it.manu.fuoriorario.domain.reimports
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDateTime
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/** Squadra → Impostazioni → Campionato (ADR 0011): import the FIP PDF and confirm it, or delete the league. */
@Composable
fun LeagueScreen(vm: LeagueViewModel = koinViewModel()) {
    val uiState = vm.uiState.collectAsStateWithLifecycle()
    val toast = LocalToast.current
    LaunchedEffect(vm) { vm.toasts.collect { launch { toast.show(it) } } }
    val pickPdf = LocalPdfPicker.current(vm::onPdfPicked)
    LeagueStateContent(
        uiState.value.state,
        LeagueActions(
            onImport = pickPdf,
            onOursPicked = vm::onOursPicked,
            onConfirm = vm::onConfirm,
            onCancelPreview = vm::onCancelPreview,
            onDelete = vm::onDelete
        )
    )
}

/**
 * Opens the system's picker for a PDF and hands over how to read it: the ViewModel reads it, so a failure toasts.
 * The UI tests put a file of their own in its place.
 */
val LocalPdfPicker = staticCompositionLocalOf<@Composable ((suspend () -> ByteArray) -> Unit) -> () -> Unit> {
    { onPicked ->
        val picker = rememberFilePickerLauncher(type = FileKitType.File(listOf("pdf"))) { file ->
            if (file != null) onPicked { file.readBytes() }
        }
        ({ picker.launch() })
    }
}

/** What Campionato does: pick the PDF, mark ours, confirm or cancel, delete. */
class LeagueActions(
    val onImport: () -> Unit = {},
    val onOursPicked: (String) -> Unit = {},
    val onConfirm: () -> Unit = {},
    val onCancelPreview: () -> Unit = {},
    val onDelete: () -> Unit = {}
)

@Composable
fun LeagueStateContent(state: UseCaseMutableState<LeagueScreenState>?, actions: LeagueActions) {
    when (state) {
        is UseCaseMutableState.Error -> state.handler.ErrorScreenContent()
        is UseCaseMutableState.ShowData -> LeagueContent(state.items, actions)
        UseCaseMutableState.Loading, null -> Loader()
    }
}

/** The league of the season with Importa and Elimina, or the preview to confirm. */
@Composable
fun LeagueContent(state: LeagueScreenState, actions: LeagueActions) {
    val c = FuoriOrarioTheme.colors
    val preview = state.preview
    Panel {
        Text(stringResource(Res.string.league_title).uppercase(), style = MaterialTheme.typography.titleLarge)
        if (preview != null) return@Panel PreviewToConfirm(state, preview, actions)
        val league = state.league
        if (league == null) {
            Text(stringResource(Res.string.league_none), color = c.muted)
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(league.name)
                Text(
                    stringResource(Res.string.league_summary, league.fixtures.size, league.teams.size),
                    color = c.muted,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        if (state.busy) Text(stringResource(Res.string.league_reading), color = c.muted)
        if (state.unreadable) Text(stringResource(Res.string.league_unreadable), color = c.accent)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            PrimaryButton(
                stringResource(Res.string.league_import),
                actions.onImport,
                Modifier.testTag("league_import"),
                enabled = !state.busy
            )
            if (league != null) {
                GhostButton(
                    stringResource(
                        if (state.confirmingDelete) Res.string.league_delete_confirm else Res.string.league_delete
                    ),
                    actions.onDelete,
                    Modifier.testTag("league_delete"),
                    style = if (state.confirmingDelete) GhostStyle.DANGER else GhostStyle.PLAIN,
                    enabled = !state.busy
                )
            }
        }
    }
}

/** Name, teams to mark ours, fixtures; on a reimport what stays. Confirm needs ours. */
@Composable
private fun PreviewToConfirm(state: LeagueScreenState, preview: ImportPreview, actions: LeagueActions) {
    val c = FuoriOrarioTheme.colors
    Text(
        stringResource(Res.string.league_preview).uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = c.muted
    )
    Text(preview.name)
    if (preview.reimports(state.league)) {
        Text(stringResource(Res.string.league_reimport), color = c.muted, style = MaterialTheme.typography.bodyMedium)
    }
    Text(stringResource(Res.string.league_pick_ours), color = c.muted, style = MaterialTheme.typography.bodyMedium)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        preview.teams.forEach { team ->
            TogglePill(team, team == state.ours, { actions.onOursPicked(team) }, Modifier.testTag("ours_$team"))
        }
    }
    if (state.missing.isNotEmpty()) {
        Text(stringResource(Res.string.league_missing, state.missing.size), color = c.accent)
        state.league?.let { league ->
            state.missing.forEach { f ->
                Text(
                    "${roundLabel(f.round, f.leg)} · ${league.team(f.homeId).name} – ${league.team(f.awayId).name}",
                    color = c.muted,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
    Text(
        stringResource(Res.string.league_fixtures, preview.fixtures.size),
        style = MaterialTheme.typography.titleMedium
    )
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        preview.fixtures.forEach { f ->
            Text(
                "${roundLabel(f.round, f.leg)} · ${f.startsAt?.let { whenText(it) } ?: "—"} · ${f.home} – ${f.away}",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        GhostButton(stringResource(Res.string.cancel), actions.onCancelPreview)
        PrimaryButton(
            stringResource(Res.string.league_confirm),
            actions.onConfirm,
            Modifier.testTag("league_confirm"),
            enabled = state.ours != null && !state.busy
        )
    }
}

@Preview
@Composable
private fun LeagueContentPreview() = FuoriOrarioTheme {
    val preview = ImportPreview(
        "Under 19 Maschile Regionale - PRIMA FASE",
        listOf("ARTEMIS BASKET A.S.D.", "ASD Atessa Basket"),
        listOf(
            ImportFixture(
                "000691",
                1,
                Leg.FIRST,
                LocalDateTime(2026, 11, 9, 18, 30),
                "ASD Atessa Basket",
                "ARTEMIS BASKET A.S.D."
            )
        )
    )
    Column {
        LeagueContent(LeagueScreenState(League("l", "U19", emptyList(), emptyList())), LeagueActions())
        LeagueContent(LeagueScreenState(preview = preview, ours = "ARTEMIS BASKET A.S.D."), LeagueActions())
    }
}
