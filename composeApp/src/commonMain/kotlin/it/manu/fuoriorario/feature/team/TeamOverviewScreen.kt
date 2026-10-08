package it.manu.fuoriorario.feature.team

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fuoriorario.composeapp.generated.resources.Res
import fuoriorario.composeapp.generated.resources.overview_free_30
import fuoriorario.composeapp.generated.resources.overview_hint
import fuoriorario.composeapp.generated.resources.overview_plan
import fuoriorario.composeapp.generated.resources.overview_player
import fuoriorario.composeapp.generated.resources.overview_shots_7
import fuoriorario.composeapp.generated.resources.overview_three_30
import fuoriorario.composeapp.generated.resources.overview_title
import fuoriorario.composeapp.generated.resources.overview_week
import it.manu.fuoriorario.core.designsystem.Panel
import it.manu.fuoriorario.core.theme.FuoriOrarioTheme
import it.manu.fuoriorario.core.viewmodel.UseCaseMutableState
import it.manu.fuoriorario.domain.Member
import it.manu.fuoriorario.domain.OverviewRow
import it.manu.fuoriorario.domain.PlanLevel
import it.manu.fuoriorario.domain.Role
import it.manu.fuoriorario.domain.planLevel
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/** Under this the table scrolls sideways instead of squeezing the numbers. */
private val TABLE_MIN_WIDTH = 400.dp
private val NUMBER_WIDTH = 64.dp
private val PLAN_WIDTH = 72.dp

/**
 * Quadro squadra (PRD F6): a row per player, in the roster's order, with this week's plan and recent shots.
 * [players] come from the roster, already loaded. Tapping a row makes staff follow that player, then runs [onOpened].
 */
@Composable
fun TeamOverviewScreen(
    players: List<Member>,
    onOpened: () -> Unit,
    vm: TeamOverviewViewModel = koinViewModel { parametersOf(players) }
) {
    val uiState = vm.uiState.collectAsStateWithLifecycle()
    TeamOverviewStateContent(uiState.value.state) {
        vm.onOpen(it)
        onOpened()
    }
}

@Composable
fun TeamOverviewStateContent(state: UseCaseMutableState<TeamOverviewScreenState>?, onOpen: (Member) -> Unit) {
    when (state) {
        // No numbers from before next to the error.
        is UseCaseMutableState.Error -> state.handler.ErrorScreenContent()
        is UseCaseMutableState.ShowData -> TeamOverviewContent(state.items, onOpen)
        UseCaseMutableState.Loading, null -> Unit
    }
}

/** The title at once, the table once loaded. */
@Composable
fun TeamOverviewContent(state: TeamOverviewScreenState, onOpen: (Member) -> Unit) {
    val c = FuoriOrarioTheme.colors
    Panel {
        Column {
            Text(
                stringResource(Res.string.overview_week).uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = c.muted
            )
            Text(stringResource(Res.string.overview_title).uppercase(), style = MaterialTheme.typography.titleLarge)
        }
        Text(stringResource(Res.string.overview_hint), color = c.muted, style = MaterialTheme.typography.bodyMedium)
        state.rows?.let { Table(it, onOpen) }
    }
}

/** Prototype `.tablewrap table`: name left, numbers right, sideways scroll when narrow. */
@Composable
private fun Table(rows: List<Pair<Member, OverviewRow>>, onOpen: (Member) -> Unit) {
    val c = FuoriOrarioTheme.colors
    BoxWithConstraints {
        Column(Modifier.horizontalScroll(rememberScrollState()).width(max(maxWidth, TABLE_MIN_WIDTH))) {
            Row(Modifier.padding(vertical = 6.dp)) {
                Header(Res.string.overview_player, null)
                Header(Res.string.overview_plan, PLAN_WIDTH)
                Header(Res.string.overview_shots_7, NUMBER_WIDTH)
                Header(Res.string.overview_free_30, NUMBER_WIDTH)
                Header(Res.string.overview_three_30, NUMBER_WIDTH)
            }
            rows.forEach { (player, row) ->
                HorizontalDivider(color = c.line)
                Row(
                    Modifier
                        .fillMaxWidth()
                        .testTag("overview_${player.id}")
                        .clickable { onOpen(player) }
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f).padding(horizontal = 6.dp)) {
                        Row {
                            player.jerseyNumber?.let {
                                Text(
                                    "#$it",
                                    Modifier.padding(end = 4.dp),
                                    color = c.muted,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Text(player.displayName, fontWeight = FontWeight.SemiBold)
                        }
                        player.position?.let {
                            Text(it, color = c.muted, style = MaterialTheme.typography.bodyMedium, fontSize = 12.sp)
                        }
                    }
                    Box(Modifier.width(PLAN_WIDTH).padding(horizontal = 6.dp), contentAlignment = Alignment.CenterEnd) {
                        PlanPill(row.plan)
                    }
                    Number("${row.attempted7}")
                    Number(row.freeThrows30.percentText())
                    Number(row.three30.percentText())
                }
            }
        }
    }
}

private fun Int?.percentText() = this?.let { "$it%" } ?: "—"

/** Prototype `th`; null [width] takes what's left. */
@Composable
private fun RowScope.Header(label: StringResource, width: Dp?) {
    Text(
        stringResource(label).uppercase(),
        (if (width == null) Modifier.weight(1f) else Modifier.width(width)).padding(horizontal = 6.dp),
        color = FuoriOrarioTheme.colors.muted,
        style = MaterialTheme.typography.labelSmall,
        textAlign = if (width == null) TextAlign.Start else TextAlign.End,
        maxLines = 1
    )
}

/** Prototype `td`: display font, right aligned. */
@Composable
private fun Number(text: String) {
    Text(
        text,
        Modifier.width(NUMBER_WIDTH).padding(horizontal = 6.dp),
        style = MaterialTheme.typography.titleMedium,
        fontSize = 17.sp,
        fontWeight = FontWeight.Medium,
        textAlign = TextAlign.End
    )
}

/** Prototype `.pill`: green from 75%, orange from 40%, grey below or without a plan. */
@Composable
private fun PlanPill(percent: Int?) {
    val c = FuoriOrarioTheme.colors
    val (background, color) = when (planLevel(percent)) {
        PlanLevel.GOOD -> c.good.copy(alpha = 0.18f) to c.good
        PlanLevel.MID -> c.accentSoft to c.accent
        PlanLevel.LOW -> c.surface2 to c.muted
    }
    Text(
        percent.percentText(),
        Modifier
            .widthIn(min = 44.dp)
            .background(background, RoundedCornerShape(50))
            .padding(horizontal = 8.dp, vertical = 1.dp),
        color = color,
        style = MaterialTheme.typography.titleMedium,
        fontSize = 15.sp,
        fontWeight = FontWeight.Medium,
        textAlign = TextAlign.Center
    )
}

@Preview
@Composable
private fun TeamOverviewContentPreview() = FuoriOrarioTheme {
    TeamOverviewContent(
        TeamOverviewScreenState(
            listOf(
                Member("Luca B.", Role.PLAYER, jerseyNumber = "7", position = "Guardia", id = "l") to
                    OverviewRow(plan = 80, attempted7 = 40, freeThrows30 = 75, three30 = 34),
                Member("Anna", Role.PLAYER, id = "a") to
                    OverviewRow(plan = null, attempted7 = 0, freeThrows30 = null, three30 = null)
            )
        ),
        onOpen = {}
    )
}
