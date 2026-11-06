package it.manu.fuoriorario.feature.league

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
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
import it.manu.fuoriorario.core.ROME
import it.manu.fuoriorario.core.designsystem.Loader
import it.manu.fuoriorario.core.designsystem.Panel
import it.manu.fuoriorario.core.designsystem.SegmentedControl
import it.manu.fuoriorario.core.designsystem.short
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
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/** Partite → Calendario (ADR 0011): the league's fixtures by round, opened on the next one. */
@Composable
fun CalendarScreen(staff: Boolean, vm: CalendarViewModel = koinViewModel()) {
    val uiState = vm.uiState.collectAsStateWithLifecycle()
    CalendarStateContent(uiState.value.state, staff, vm::onOursChanged)
}

@Composable
fun CalendarStateContent(
    state: UseCaseMutableState<CalendarScreenState>?,
    staff: Boolean,
    onOursChanged: (Boolean) -> Unit
) {
    when (state) {
        is UseCaseMutableState.Error -> state.handler.ErrorScreenContent()
        is UseCaseMutableState.ShowData -> CalendarContent(state.items, staff, onOursChanged)
        UseCaseMutableState.Loading, null -> Loader()
    }
}

/** "Nostre" or "Tutto il girone", then a panel per round with who rests. */
@Composable
fun CalendarContent(state: CalendarScreenState, staff: Boolean, onOursChanged: (Boolean) -> Unit) {
    val c = FuoriOrarioTheme.colors
    val league = state.league
    if (league == null) {
        Panel {
            Text(stringResource(Res.string.calendar_empty), color = c.muted)
            if (staff) Text(stringResource(Res.string.calendar_empty_staff), color = c.muted)
        }
        return
    }
    SegmentedControl(
        mapOf(true to Res.string.calendar_ours, false to Res.string.calendar_all),
        state.ours,
        onOursChanged,
        tag = { if (it) "calendar_ours" else "calendar_all" }
    )
    state.rounds.forEachIndexed { i, round ->
        // Scrolling to the next round is the screen's own: the ViewModel says which.
        val requester = remember { BringIntoViewRequester() }
        if (i == state.open) LaunchedEffect(state.open, state.ours) { requester.bringIntoView() }
        RoundPanel(league, round, state.ours, Modifier.bringIntoViewRequester(requester))
    }
}

@Composable
private fun RoundPanel(league: League, round: Round, ours: Boolean, modifier: Modifier) {
    val c = FuoriOrarioTheme.colors
    Panel(modifier) {
        Text(roundLabel(round.number, round.leg).uppercase(), style = MaterialTheme.typography.titleMedium)
        Column {
            round.fixtures.forEachIndexed { i, fixture ->
                if (i > 0) HorizontalDivider(color = c.line)
                FixtureRow(league, fixture)
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

/** When, home – away (ours in bold), venue; the result once played. */
@Composable
private fun FixtureRow(league: League, fixture: Fixture) {
    val c = FuoriOrarioTheme.colors
    Row(
        Modifier.fillMaxWidth().padding(vertical = 9.dp).testTag("fixture_${fixture.number}"),
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
        CalendarContent(CalendarScreenState(league, ours = false, rounds = league.rounds()), staff = true) {}
    }
}
