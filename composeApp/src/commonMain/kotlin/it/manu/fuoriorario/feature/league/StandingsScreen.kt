package it.manu.fuoriorario.feature.league

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fuoriorario.composeapp.generated.resources.Res
import fuoriorario.composeapp.generated.resources.calendar_empty
import fuoriorario.composeapp.generated.resources.calendar_empty_staff
import fuoriorario.composeapp.generated.resources.standings_conceded
import fuoriorario.composeapp.generated.resources.standings_diff
import fuoriorario.composeapp.generated.resources.standings_lost
import fuoriorario.composeapp.generated.resources.standings_played
import fuoriorario.composeapp.generated.resources.standings_points
import fuoriorario.composeapp.generated.resources.standings_position
import fuoriorario.composeapp.generated.resources.standings_scored
import fuoriorario.composeapp.generated.resources.standings_team
import fuoriorario.composeapp.generated.resources.standings_won
import it.manu.fuoriorario.core.designsystem.Loader
import it.manu.fuoriorario.core.designsystem.Panel
import it.manu.fuoriorario.core.designsystem.Table
import it.manu.fuoriorario.core.designsystem.TableColumn
import it.manu.fuoriorario.core.designsystem.TableRow
import it.manu.fuoriorario.core.theme.FuoriOrarioTheme
import it.manu.fuoriorario.core.viewmodel.UseCaseMutableState
import it.manu.fuoriorario.domain.Fixture
import it.manu.fuoriorario.domain.FixtureStatus
import it.manu.fuoriorario.domain.League
import it.manu.fuoriorario.domain.LeagueTeam
import it.manu.fuoriorario.domain.Leg
import it.manu.fuoriorario.domain.Standing
import it.manu.fuoriorario.domain.standings
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/** Partite → Classifica: from the league's results, for the whole team. */
@Composable
fun StandingsScreen(staff: Boolean, vm: StandingsViewModel = koinViewModel()) {
    val uiState = vm.uiState.collectAsStateWithLifecycle()
    StandingsStateContent(uiState.value.state, staff)
}

@Composable
fun StandingsStateContent(state: UseCaseMutableState<StandingsScreenState>?, staff: Boolean) {
    when (state) {
        is UseCaseMutableState.Error -> state.handler.ErrorScreenContent()
        is UseCaseMutableState.ShowData -> StandingsContent(state.items, staff)
        UseCaseMutableState.Loading, null -> Loader()
    }
}

/** Pos and Squadra stay still, the numbers scroll; our row is highlighted. Without a league, how to get one. */
@Composable
fun StandingsContent(state: StandingsScreenState, staff: Boolean) {
    val c = FuoriOrarioTheme.colors
    if (state.league == null) {
        Panel {
            Text(stringResource(Res.string.calendar_empty), color = c.muted)
            if (staff) Text(stringResource(Res.string.calendar_empty_staff), color = c.muted)
        }
        return
    }
    Panel {
        Table(
            listOf(
                TableColumn("position", stringResource(Res.string.standings_position), 36.dp),
                TableColumn("team", stringResource(Res.string.standings_team), 150.dp)
            ),
            listOf(
                TableColumn("points", stringResource(Res.string.standings_points)),
                TableColumn("played", stringResource(Res.string.standings_played)),
                TableColumn("won", stringResource(Res.string.standings_won)),
                TableColumn("lost", stringResource(Res.string.standings_lost)),
                TableColumn("scored", stringResource(Res.string.standings_scored)),
                TableColumn("conceded", stringResource(Res.string.standings_conceded)),
                TableColumn("diff", stringResource(Res.string.standings_diff))
            ),
            state.standings.mapIndexed { i, s ->
                TableRow(
                    "standing_${s.team.id}",
                    listOf("${i + 1}", s.team.name) + s.cells(),
                    highlighted = s.team.ours,
                    bold = s.team.ours
                )
            }
        )
    }
}

private fun Standing.cells() = listOf(points, played, won, lost, scored, conceded).map { it.toString() } +
    (if (diff > 0) "+$diff" else "$diff")

@Preview
@Composable
private fun StandingsContentPreview() = FuoriOrarioTheme {
    val league = League(
        "l",
        "U19",
        listOf(LeagueTeam("a", "ARTEMIS BASKET A.S.D.", ours = true), LeagueTeam("b", "ASD Atessa Basket")),
        listOf(Fixture("1", 1, Leg.FIRST, null, "b", "a", null, FixtureStatus.PLAYED, 55, 62))
    )
    StandingsContent(StandingsScreenState(league, league.standings()), staff = false)
}
