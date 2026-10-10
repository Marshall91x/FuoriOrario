package it.manu.fuoriorario.feature.games

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fuoriorario.composeapp.generated.resources.Res
import fuoriorario.composeapp.generated.resources.cancel
import fuoriorario.composeapp.generated.resources.game_abandon
import fuoriorario.composeapp.generated.resources.game_abandon_confirm
import fuoriorario.composeapp.generated.resources.game_away
import fuoriorario.composeapp.generated.resources.game_call_ups
import fuoriorario.composeapp.generated.resources.game_home
import fuoriorario.composeapp.generated.resources.game_in_progress
import fuoriorario.composeapp.generated.resources.game_my_points
import fuoriorario.composeapp.generated.resources.game_new
import fuoriorario.composeapp.generated.resources.game_new_title
import fuoriorario.composeapp.generated.resources.game_no_opponent
import fuoriorario.composeapp.generated.resources.game_no_players
import fuoriorario.composeapp.generated.resources.game_note
import fuoriorario.composeapp.generated.resources.game_opponent
import fuoriorario.composeapp.generated.resources.game_resume
import fuoriorario.composeapp.generated.resources.game_start
import fuoriorario.composeapp.generated.resources.games_empty
import it.manu.fuoriorario.core.designsystem.Field
import it.manu.fuoriorario.core.designsystem.GhostButton
import it.manu.fuoriorario.core.designsystem.GhostStyle
import it.manu.fuoriorario.core.designsystem.LoadFailed
import it.manu.fuoriorario.core.designsystem.Loader
import it.manu.fuoriorario.core.designsystem.LocalToast
import it.manu.fuoriorario.core.designsystem.Panel
import it.manu.fuoriorario.core.designsystem.PrimaryButton
import it.manu.fuoriorario.core.designsystem.SegmentedControl
import it.manu.fuoriorario.core.designsystem.TogglePill
import it.manu.fuoriorario.core.designsystem.short
import it.manu.fuoriorario.core.designsystem.show
import it.manu.fuoriorario.core.theme.FuoriOrarioTheme
import it.manu.fuoriorario.core.today
import it.manu.fuoriorario.core.viewmodel.UseCaseMutableState
import it.manu.fuoriorario.domain.CallUp
import it.manu.fuoriorario.domain.Game
import it.manu.fuoriorario.domain.GameDraft
import it.manu.fuoriorario.domain.GameError
import it.manu.fuoriorario.domain.Member
import it.manu.fuoriorario.domain.Quarter
import it.manu.fuoriorario.domain.Role
import it.manu.fuoriorario.domain.Zone
import it.manu.fuoriorario.feature.shots.DateField
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * Partite: the team's games, newest first. Staff also start a new one and record it in Live, or resume the one in
 * progress. [onSecondLevel] tells whether the form or Live is open, a screen without the tab bar.
 */
@Composable
fun GamesScreen(
    staff: Boolean,
    onSecondLevel: (Boolean) -> Unit = {},
    vm: GamesViewModel = koinViewModel { parametersOf(staff) }
) {
    val uiState = vm.uiState.collectAsStateWithLifecycle()
    val toast = LocalToast.current
    LaunchedEffect(vm) { vm.toasts.collect { launch { toast.show(it) } } }
    val secondLevel = uiState.value.data?.secondLevel == true
    LaunchedEffect(secondLevel) { onSecondLevel(secondLevel) }
    DisposableEffect(Unit) { onDispose { onSecondLevel(false) } }
    GamesStateContent(
        uiState.value.state,
        staff,
        GamesActions(
            onNew = vm::onNew,
            onCancelForm = vm::onCancelForm,
            onDateChanged = vm::onDateChanged,
            onOpponentChanged = vm::onOpponentChanged,
            onHomeChanged = vm::onHomeChanged,
            onNoteChanged = vm::onNoteChanged,
            onCallUpToggled = vm::onCallUpToggled,
            onStart = vm::onStart,
            onResume = vm::onResume,
            onAbandon = vm::onAbandon,
            onLeaveLive = vm::onLeaveLive,
            onSelect = vm::onSelect,
            onQuarter = vm::onQuarter,
            onShot = vm::onShot,
            onFreeThrow = vm::onFreeThrow,
            onOpponentScored = vm::onOpponentScored,
            onUndo = vm::onUndo,
            onRemoveEvent = vm::onRemoveEvent,
            onFinish = vm::onFinish,
            onRetry = vm::onRetry,
            onOpen = vm::onOpen,
            onCloseSummary = vm::onCloseSummary,
            onSummaryQuarter = vm::onSummaryQuarter,
            onBoxRow = vm::onBoxRow,
            onDelete = vm::onDelete
        )
    )
}

/** What Partite does, from the list to Live and Riepilogo. */
class GamesActions(
    val onNew: () -> Unit = {},
    val onCancelForm: () -> Unit = {},
    val onDateChanged: (LocalDate) -> Unit = {},
    val onOpponentChanged: (String) -> Unit = {},
    val onHomeChanged: (Boolean) -> Unit = {},
    val onNoteChanged: (String) -> Unit = {},
    val onCallUpToggled: (String, Boolean) -> Unit = { _, _ -> },
    val onStart: () -> Unit = {},
    val onResume: () -> Unit = {},
    val onAbandon: () -> Unit = {},
    val onLeaveLive: () -> Unit = {},
    val onSelect: (String) -> Unit = {},
    val onQuarter: (Quarter) -> Unit = {},
    val onShot: (Zone, Boolean) -> Unit = { _, _ -> },
    val onFreeThrow: (Boolean) -> Unit = {},
    val onOpponentScored: (Int) -> Unit = {},
    val onUndo: () -> Unit = {},
    val onRemoveEvent: (Int) -> Unit = {},
    val onFinish: () -> Unit = {},
    val onRetry: () -> Unit = {},
    val onOpen: (Game) -> Unit = {},
    val onCloseSummary: () -> Unit = {},
    val onSummaryQuarter: (Quarter?) -> Unit = {},
    val onBoxRow: (String) -> Unit = {},
    val onDelete: () -> Unit = {}
)

@Composable
fun GamesStateContent(state: UseCaseMutableState<GamesScreenState>?, staff: Boolean, actions: GamesActions) {
    when (state) {
        is UseCaseMutableState.Error -> state.handler.ErrorScreenContent()
        is UseCaseMutableState.ShowData -> GamesContent(state.items, staff, actions)
        UseCaseMutableState.Loading, null -> Loader()
    }
}

/** Live if open, else the new game form, else a Riepilogo, else the list. */
@Composable
fun GamesContent(state: GamesScreenState, staff: Boolean, actions: GamesActions) {
    val draft = state.draft
    val form = state.form
    val summary = state.summary
    when {
        state.live && draft != null -> GameLive(draft, state.busy, actions)
        form != null -> NewGame(form, state.players, actions)
        summary != null -> Summary(summary, staff, state.busy, actions)
        else -> {
            if (staff) {
                draft?.let { InProgress(it, state.confirmingAbandon, actions) }
                PrimaryButton(
                    stringResource(Res.string.game_new),
                    actions.onNew,
                    Modifier.testTag("game_new"),
                    enabled = draft == null
                )
            }
            state.games?.let { GameList(it, state.points, actions.onOpen) }
                ?: if (state.loadFailed) LoadFailed(actions.onRetry) else Unit
        }
    }
}

@Preview
@Composable
private fun GamesContentPreview() = FuoriOrarioTheme {
    val draft = GameDraft("d", LocalDate(2026, 10, 10), "Fortitudo", home = false)
    val games = listOf(Game(LocalDate(2026, 10, 3), "Virtus", home = true, ourScore = 60, theirScore = 52))
    Column { GamesContent(GamesScreenState(games, draft = draft), staff = true, GamesActions()) }
}

/** "Casa" or "Trasferta". */
@Composable
internal fun homeLabel(home: Boolean) = stringResource(if (home) Res.string.game_home else Res.string.game_away)

/** "Partita in corso": resume it, or abandon it with a second tap. */
@Composable
private fun InProgress(draft: GameDraft, confirming: Boolean, actions: GamesActions) {
    Panel {
        Text(stringResource(Res.string.game_in_progress).uppercase(), style = MaterialTheme.typography.titleMedium)
        val score = draft.score
        Text("${draft.opponent} · ${homeLabel(draft.home)} · ${score.us}–${score.them}")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            PrimaryButton(stringResource(Res.string.game_resume), actions.onResume, Modifier.testTag("game_resume"))
            GhostButton(
                stringResource(if (confirming) Res.string.game_abandon_confirm else Res.string.game_abandon),
                actions.onAbandon,
                Modifier.testTag("game_abandon"),
                style = if (confirming) GhostStyle.DANGER else GhostStyle.PLAIN
            )
        }
    }
}

/** Date, opponent, home or away, result, and a player's own [points]; tapping a game opens its Riepilogo. */
@Composable
private fun GameList(games: List<Game>, points: Map<String, Int>, onOpen: (Game) -> Unit) {
    val c = FuoriOrarioTheme.colors
    Panel {
        if (games.isEmpty()) Text(stringResource(Res.string.games_empty), color = c.muted)
        Column {
            games.forEachIndexed { i, game ->
                if (i > 0) HorizontalDivider(color = c.line)
                Row(
                    Modifier.fillMaxWidth().clickable { onOpen(game) }.padding(vertical = 11.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(game.date.short(), color = c.muted)
                    Column(Modifier.weight(1f)) {
                        Text(game.opponent, fontWeight = FontWeight.SemiBold)
                        Text(homeLabel(game.home), color = c.muted, style = MaterialTheme.typography.bodyMedium)
                    }
                    points[game.id]?.let { Text(stringResource(Res.string.game_my_points, it), color = c.muted) }
                    Text("${game.ourScore}–${game.theirScore}", style = MaterialTheme.typography.headlineMedium)
                }
            }
        }
    }
}

/** "#7 Luca B.", or just the name without a number. */
internal fun chipLabel(number: String?, name: String) = number?.let { "#$it " }.orEmpty() + name

internal val CallUp.label get() = chipLabel(number, name)

/** "Nuova partita": date, opponent, home or away, note, convocati. Nothing goes online until "Termina partita". */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun NewGame(form: GameForm, players: List<Member>, actions: GamesActions) {
    val c = FuoriOrarioTheme.colors
    // ponytail: deprecated, NavigationEventHandler when its compose artifact is among our dependencies (as in PlayEditor).
    @Suppress("DEPRECATION")
    BackHandler(onBack = actions.onCancelForm)
    Panel {
        Text(stringResource(Res.string.game_new_title).uppercase(), style = MaterialTheme.typography.titleMedium)
        DateField(form.date, today(), actions.onDateChanged)
        Field(stringResource(Res.string.game_opponent), form.opponent, actions.onOpponentChanged, "game_opponent")
        if (form.error == GameError.NO_OPPONENT) Text(stringResource(Res.string.game_no_opponent), color = c.accent)
        SegmentedControl(
            mapOf(true to Res.string.game_home, false to Res.string.game_away),
            form.home,
            actions.onHomeChanged,
            tag = { if (it) "game_home" else "game_away" }
        )
        Field(
            stringResource(Res.string.game_note),
            form.note,
            actions.onNoteChanged,
            "game_note",
            singleLine = false
        )
        Text(
            stringResource(Res.string.game_call_ups).uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = c.muted
        )
        if (players.isEmpty()) Text(stringResource(Res.string.game_no_players), color = c.muted)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            players.forEach { p ->
                TogglePill(
                    chipLabel(p.jerseyNumber, p.displayName),
                    p.id in form.callUps,
                    { on -> actions.onCallUpToggled(p.id!!, on) },
                    Modifier.testTag("call_up_${p.id}")
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GhostButton(stringResource(Res.string.cancel), actions.onCancelForm)
            PrimaryButton(stringResource(Res.string.game_start), actions.onStart, Modifier.testTag("game_start"))
        }
    }
}

@Preview
@Composable
private fun NewGamePreview() = FuoriOrarioTheme {
    val players = listOf(Member("Luca B.", Role.PLAYER, jerseyNumber = "7", id = "luca"))
    Column { NewGame(GameForm(LocalDate(2026, 10, 10), "Virtus", callUps = setOf("luca")), players, GamesActions()) }
}
