package it.manu.fuoriorario.feature.league

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fuoriorario.composeapp.generated.resources.Res
import fuoriorario.composeapp.generated.resources.calendar_all
import fuoriorario.composeapp.generated.resources.calendar_empty
import fuoriorario.composeapp.generated.resources.calendar_empty_staff
import fuoriorario.composeapp.generated.resources.calendar_first_leg
import fuoriorario.composeapp.generated.resources.calendar_ours
import fuoriorario.composeapp.generated.resources.calendar_postponed
import fuoriorario.composeapp.generated.resources.calendar_rest
import fuoriorario.composeapp.generated.resources.calendar_resting
import fuoriorario.composeapp.generated.resources.calendar_round
import fuoriorario.composeapp.generated.resources.calendar_second_leg
import fuoriorario.composeapp.generated.resources.close
import fuoriorario.composeapp.generated.resources.fixture_add
import fuoriorario.composeapp.generated.resources.fixture_away
import fuoriorario.composeapp.generated.resources.fixture_date
import fuoriorario.composeapp.generated.resources.fixture_error_date
import fuoriorario.composeapp.generated.resources.fixture_error_result
import fuoriorario.composeapp.generated.resources.fixture_error_teams
import fuoriorario.composeapp.generated.resources.fixture_home
import fuoriorario.composeapp.generated.resources.fixture_new
import fuoriorario.composeapp.generated.resources.fixture_number
import fuoriorario.composeapp.generated.resources.fixture_played
import fuoriorario.composeapp.generated.resources.fixture_postponed
import fuoriorario.composeapp.generated.resources.fixture_round
import fuoriorario.composeapp.generated.resources.fixture_save
import fuoriorario.composeapp.generated.resources.fixture_scheduled
import fuoriorario.composeapp.generated.resources.fixture_time
import fuoriorario.composeapp.generated.resources.fixture_venue
import fuoriorario.composeapp.generated.resources.league_delete
import fuoriorario.composeapp.generated.resources.league_delete_confirm
import it.manu.fuoriorario.core.ROME
import it.manu.fuoriorario.core.designsystem.Field
import it.manu.fuoriorario.core.designsystem.GhostButton
import it.manu.fuoriorario.core.designsystem.GhostStyle
import it.manu.fuoriorario.core.designsystem.Loader
import it.manu.fuoriorario.core.designsystem.LocalToast
import it.manu.fuoriorario.core.designsystem.Panel
import it.manu.fuoriorario.core.designsystem.PrimaryButton
import it.manu.fuoriorario.core.designsystem.SegmentedControl
import it.manu.fuoriorario.core.designsystem.SelectField
import it.manu.fuoriorario.core.designsystem.short
import it.manu.fuoriorario.core.designsystem.show
import it.manu.fuoriorario.core.theme.FuoriOrarioTheme
import it.manu.fuoriorario.core.viewmodel.UseCaseMutableState
import it.manu.fuoriorario.domain.Fixture
import it.manu.fuoriorario.domain.FixtureStatus
import it.manu.fuoriorario.domain.League
import it.manu.fuoriorario.domain.LeagueTeam
import it.manu.fuoriorario.domain.Leg
import it.manu.fuoriorario.domain.Round
import it.manu.fuoriorario.domain.rounds
import kotlin.time.Instant
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/** Partite → Calendario (ADR 0011): the league's fixtures by round, opened on the next one. */
@Composable
fun CalendarScreen(staff: Boolean, vm: CalendarViewModel = koinViewModel()) {
    val uiState = vm.uiState.collectAsStateWithLifecycle()
    val toast = LocalToast.current
    LaunchedEffect(vm) { vm.toasts.collect { launch { toast.show(it) } } }
    CalendarStateContent(
        uiState.value.state,
        staff,
        CalendarActions(
            onOursChanged = vm::onOursChanged,
            onEdit = vm::onEdit,
            onAdd = vm::onAdd,
            onDraftChanged = vm::onDraftChanged,
            onDismiss = vm::onDismiss,
            onSave = vm::onSave,
            onDelete = vm::onDelete
        )
    )
}

/** What the calendar does; all but [onOursChanged] are staff's. */
class CalendarActions(
    val onOursChanged: (Boolean) -> Unit = {},
    val onEdit: (Fixture) -> Unit = {},
    val onAdd: () -> Unit = {},
    val onDraftChanged: (FixtureDraft) -> Unit = {},
    val onDismiss: () -> Unit = {},
    val onSave: () -> Unit = {},
    val onDelete: () -> Unit = {}
)

@Composable
fun CalendarStateContent(state: UseCaseMutableState<CalendarScreenState>?, staff: Boolean, actions: CalendarActions) {
    when (state) {
        is UseCaseMutableState.Error -> state.handler.ErrorScreenContent()
        is UseCaseMutableState.ShowData -> CalendarContent(state.items, staff, actions)
        UseCaseMutableState.Loading, null -> Loader()
    }
}

/** "Nostre" or "Tutto il girone" (staff: "+ Gara"), then a panel per round with who rests. Staff tap a fixture to edit it. */
@Composable
fun CalendarContent(state: CalendarScreenState, staff: Boolean, actions: CalendarActions) {
    val c = FuoriOrarioTheme.colors
    val league = state.league
    if (league == null) {
        Panel {
            Text(stringResource(Res.string.calendar_empty), color = c.muted)
            if (staff) Text(stringResource(Res.string.calendar_empty_staff), color = c.muted)
        }
        return
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        SegmentedControl(
            mapOf(true to Res.string.calendar_ours, false to Res.string.calendar_all),
            state.ours,
            actions.onOursChanged,
            tag = { if (it) "calendar_ours" else "calendar_all" },
            modifier = Modifier.weight(1f, fill = false)
        )
        Spacer(Modifier.weight(1f))
        if (staff) GhostButton(stringResource(Res.string.fixture_add), actions.onAdd, Modifier.testTag("fixture_add"))
    }
    val onEdit = actions.onEdit.takeIf { staff }
    state.rounds.forEachIndexed { i, round ->
        // Scrolling to the next round is the screen's own: the ViewModel says which.
        val requester = remember { BringIntoViewRequester() }
        if (i == state.open) LaunchedEffect(state.open, state.ours) { requester.bringIntoView() }
        RoundPanel(league, round, state.ours, onEdit, Modifier.bringIntoViewRequester(requester))
    }
    if (staff) state.editing?.let { FixtureSheet(league, it, state, actions) }
}

@Composable
private fun RoundPanel(league: League, round: Round, ours: Boolean, onEdit: ((Fixture) -> Unit)?, modifier: Modifier) {
    val c = FuoriOrarioTheme.colors
    Panel(modifier) {
        Text(roundLabel(round.number, round.leg).uppercase(), style = MaterialTheme.typography.titleMedium)
        Column {
            round.fixtures.forEachIndexed { i, fixture ->
                if (i > 0) HorizontalDivider(color = c.line)
                FixtureRow(league, fixture, onEdit)
            }
        }
        val usResting = league.ours?.let { it in round.resting } == true
        when {
            ours && usResting -> Text(stringResource(Res.string.calendar_rest), color = c.muted)
            !ours && round.resting.isNotEmpty() -> Text(
                stringResource(Res.string.calendar_resting, round.resting.joinToString { it.name }),
                color = c.muted,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

/** When, home – away (ours in bold), venue; the result once played. [onEdit] null: not staff. */
@Composable
private fun FixtureRow(league: League, fixture: Fixture, onEdit: ((Fixture) -> Unit)?) {
    val c = FuoriOrarioTheme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .testTag("fixture_${fixture.number}")
            .then(if (onEdit != null) Modifier.clickable { onEdit(fixture) } else Modifier)
            .padding(vertical = 9.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                fixture.startsAt?.let { whenText(it) } ?: stringResource(Res.string.calendar_postponed),
                color = c.muted,
                style = MaterialTheme.typography.bodyMedium
            )
            TeamName(league.team(fixture.homeId))
            TeamName(league.team(fixture.awayId))
            fixture.venue?.let { Text(it, color = c.muted, style = MaterialTheme.typography.labelSmall) }
        }
        if (fixture.status == FixtureStatus.PLAYED) {
            Text("${fixture.homeScore}–${fixture.awayScore}", style = MaterialTheme.typography.headlineMedium)
        }
    }
}

@Composable
private fun TeamName(team: LeagueTeam) =
    Text(team.name, fontWeight = if (team.ours) FontWeight.Bold else FontWeight.Normal)

/** "3ª andata". */
@Composable
internal fun roundLabel(number: Int, leg: Leg) = stringResource(
    Res.string.calendar_round,
    number,
    stringResource(if (leg == Leg.FIRST) Res.string.calendar_first_leg else Res.string.calendar_second_leg)
)

/** "9 nov · 18:30", in Rome. */
@Composable
internal fun whenText(at: Instant) = whenText(at.toLocalDateTime(ROME))

@Composable
internal fun whenText(at: LocalDateTime) =
    "${at.date.short()} · ${at.hour.toString().padStart(2, '0')}:${at.minute.toString().padStart(2, '0')}"

/** Modifica (number, date, time, venue, status, result, "Elimina") or "+ Gara" (round, home and away too). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FixtureSheet(league: League, draft: FixtureDraft, state: CalendarScreenState, actions: CalendarActions) {
    val c = FuoriOrarioTheme.colors
    val change = actions.onDraftChanged
    ModalBottomSheet(
        actions.onDismiss,
        containerColor = c.surface,
        shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp),
        dragHandle = null
    ) {
        Column(
            Modifier.verticalScroll(rememberScrollState()).padding(16.dp, 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        if (draft.new) {
                            stringResource(Res.string.fixture_new)
                        } else {
                            stringResource(Res.string.fixture_number, draft.number)
                        }.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = c.muted
                    )
                    if (!draft.new) {
                        Text(
                            "${league.team(draft.homeId!!).name} – ${league.team(draft.awayId!!).name}",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
                GhostButton(stringResource(Res.string.close), actions.onDismiss)
            }
            if (draft.new) {
                val rounds = league.rounds().map { it.number to it.leg }.ifEmpty { listOf(1 to Leg.FIRST) }
                val labels = rounds.associateWith { (n, leg) -> roundLabel(n, leg) }
                SelectField(
                    stringResource(Res.string.fixture_round),
                    draft.round to draft.leg,
                    rounds,
                    { labels[it].orEmpty() },
                    "fixture_round"
                ) { (n, leg) -> change(draft.copy(round = n, leg = leg)) }
                val teams = league.teams.map { it.id }
                val name = { id: String? -> id?.let { league.team(it).name } ?: "—" }
                SelectField(stringResource(Res.string.fixture_home), draft.homeId, teams, name, "fixture_home") {
                    change(draft.copy(homeId = it))
                }
                SelectField(stringResource(Res.string.fixture_away), draft.awayId, teams, name, "fixture_away") {
                    change(draft.copy(awayId = it))
                }
            } else {
                SegmentedControl(
                    mapOf(
                        FixtureStatus.SCHEDULED to Res.string.fixture_scheduled,
                        FixtureStatus.POSTPONED to Res.string.fixture_postponed,
                        FixtureStatus.PLAYED to Res.string.fixture_played
                    ),
                    draft.status,
                    { change(draft.copy(status = it)) },
                    tag = { "fixture_status_$it" }
                )
            }
            // Rinviata = senza data.
            if (draft.status != FixtureStatus.POSTPONED) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Field(
                        stringResource(Res.string.fixture_date),
                        draft.date,
                        { change(draft.copy(date = it)) },
                        "fixture_date",
                        Modifier.weight(1f)
                    )
                    Field(
                        stringResource(Res.string.fixture_time),
                        draft.time,
                        { change(draft.copy(time = it)) },
                        "fixture_time",
                        Modifier.weight(1f)
                    )
                }
            }
            Field(stringResource(Res.string.fixture_venue), draft.venue, {
                change(draft.copy(venue = it))
            }, "fixture_venue")
            if (draft.status == FixtureStatus.PLAYED) {
                val digits = KeyboardOptions(keyboardType = KeyboardType.Number)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Field(
                        league.team(draft.homeId!!).name,
                        draft.homeScore,
                        { change(draft.copy(homeScore = it.filter(Char::isDigit).take(3))) },
                        "fixture_home_score",
                        Modifier.weight(1f),
                        keyboardOptions = digits
                    )
                    Field(
                        league.team(draft.awayId!!).name,
                        draft.awayScore,
                        { change(draft.copy(awayScore = it.filter(Char::isDigit).take(3))) },
                        "fixture_away_score",
                        Modifier.weight(1f),
                        keyboardOptions = digits
                    )
                }
            }
            state.error?.let {
                Text(
                    stringResource(errorText.getValue(it)),
                    color = c.accent,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            PrimaryButton(
                stringResource(Res.string.fixture_save),
                actions.onSave,
                Modifier.fillMaxWidth().testTag("fixture_save"),
                enabled = !state.busy
            )
            if (!draft.new) {
                GhostButton(
                    stringResource(
                        if (state.confirmingDelete) Res.string.league_delete_confirm else Res.string.league_delete
                    ),
                    actions.onDelete,
                    Modifier.testTag("fixture_delete"),
                    style = if (state.confirmingDelete) GhostStyle.DANGER else GhostStyle.PLAIN,
                    enabled = !state.busy
                )
            }
        }
    }
}

private val errorText = mapOf(
    FixtureError.TEAMS to Res.string.fixture_error_teams,
    FixtureError.DATE to Res.string.fixture_error_date,
    FixtureError.RESULT to Res.string.fixture_error_result
)

@Preview
@Composable
private fun CalendarContentPreview() = FuoriOrarioTheme {
    val us = LeagueTeam("a", "ARTEMIS BASKET A.S.D.", ours = true)
    val league = League(
        "l",
        "U19",
        listOf(us, LeagueTeam("b", "ASD Atessa Basket"), LeagueTeam("c", "A.S.DIL. VASTO BASKET")),
        listOf(
            Fixture("1", 1, Leg.FIRST, Instant.parse("2026-11-09T17:30:00Z"), "b", "a", "Palestra Scuola Media"),
            Fixture("2", 2, Leg.FIRST, null, "c", "b")
        )
    )
    Column {
        CalendarContent(
            CalendarScreenState(league, ours = false, rounds = league.rounds()),
            staff = true,
            CalendarActions()
        )
    }
}
