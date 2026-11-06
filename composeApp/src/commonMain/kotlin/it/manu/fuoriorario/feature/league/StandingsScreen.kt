package it.manu.fuoriorario.feature.league

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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

private val NAME_WIDTH = 150.dp
private val CELL_WIDTH = 44.dp

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
    val scroll = rememberScrollState()
    val columns = listOf(
        Res.string.standings_points,
        Res.string.standings_played,
        Res.string.standings_won,
        Res.string.standings_lost,
        Res.string.standings_scored,
        Res.string.standings_conceded,
        Res.string.standings_diff
    ).map { stringResource(it) }
    Panel {
        Column {
            StandingRow(
                "${stringResource(Res.string.standings_position)}  ${stringResource(Res.string.standings_team)}",
                columns,
                c.muted,
                scroll
            )
            state.standings.forEachIndexed { i, s ->
                HorizontalDivider(Modifier.width(NAME_WIDTH + CELL_WIDTH * columns.size), color = c.line)
                StandingRow(
                    "${i + 1}  ${s.team.name}",
                    s.cells(),
                    c.ink,
                    scroll,
                    Modifier.background(
                        if (s.team.ours) c.surface2 else Color.Transparent,
                        RoundedCornerShape(6.dp)
                    ),
                    tag = "standing_${i + 1}",
                    bold = s.team.ours
                )
            }
        }
    }
}

private fun Standing.cells() = listOf(points, played, won, lost, scored, conceded).map { it.toString() } +
    (if (diff > 0) "+$diff" else "$diff")

@Composable
private fun StandingRow(
    name: String,
    cells: List<String>,
    color: Color,
    scroll: ScrollState,
    modifier: Modifier = Modifier,
    tag: String? = null,
    bold: Boolean = false
) {
    Row(
        modifier.then(tag?.let { Modifier.testTag(it) } ?: Modifier).padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            name,
            Modifier.width(NAME_WIDTH),
            color = color,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        // Every row shares [scroll], so the header and the rows stay aligned (as the box score).
        Row(Modifier.horizontalScroll(scroll, overscrollEffect = null)) {
            cells.forEach { cell ->
                Text(
                    cell,
                    Modifier.width(CELL_WIDTH),
                    color = color,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

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
