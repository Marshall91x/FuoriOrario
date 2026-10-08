package it.manu.fuoriorario.ui.team

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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import androidx.compose.ui.unit.sp
import fuoriorario.composeapp.generated.resources.Res
import fuoriorario.composeapp.generated.resources.overview_free_30
import fuoriorario.composeapp.generated.resources.overview_hint
import fuoriorario.composeapp.generated.resources.overview_plan
import fuoriorario.composeapp.generated.resources.overview_player
import fuoriorario.composeapp.generated.resources.overview_shots_7
import fuoriorario.composeapp.generated.resources.overview_three_30
import fuoriorario.composeapp.generated.resources.overview_title
import fuoriorario.composeapp.generated.resources.overview_week
import it.manu.fuoriorario.core.designsystem.LoadFailed
import it.manu.fuoriorario.core.designsystem.Panel
import it.manu.fuoriorario.core.theme.FuoriOrarioTheme
import it.manu.fuoriorario.core.today
import it.manu.fuoriorario.data.PlanRepository
import it.manu.fuoriorario.domain.Member
import it.manu.fuoriorario.domain.OverviewRow
import it.manu.fuoriorario.domain.Period
import it.manu.fuoriorario.domain.PlanLevel
import it.manu.fuoriorario.domain.overviewRow
import it.manu.fuoriorario.domain.planLevel
import it.manu.fuoriorario.domain.progress
import it.manu.fuoriorario.domain.weekOf
import it.manu.fuoriorario.feature.shots.data.ShotRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/** Under this the table scrolls sideways instead of squeezing the numbers. */
private val TABLE_MIN_WIDTH = 400.dp
private val NUMBER_WIDTH = 64.dp
private val PLAN_WIDTH = 72.dp

/**
 * Quadro squadra (PRD F6): a row per player of [players], in their order, with this week's plan and recent shots.
 * Tapping a row runs [onOpen] for that player.
 */
@Composable
fun TeamOverviewScreen(players: List<Member>, shots: ShotRepository, plans: PlanRepository, onOpen: (Member) -> Unit) {
    val c = FuoriOrarioTheme.colors
    var rows by remember { mutableStateOf<List<OverviewRow>?>(null) }
    var loadFailed by remember { mutableStateOf(false) }
    var attempt by remember { mutableIntStateOf(0) }
    LaunchedEffect(players, attempt) {
        loadFailed = false
        try {
            val today = today()
            // Two requests for the whole team, however many players.
            rows = coroutineScope {
                val week = async { plans.teamWeek(weekOf(today)) }
                val sessions = async { shots.teamSessions(Period.DAYS_30.start(today)) }
                val (items, checks) = week.await()
                val byPlayer = sessions.await().groupBy { it.memberId }
                players.map { player ->
                    overviewRow(
                        progress(
                            items.filter {
                                it.memberId == player.id
                            },
                            checks
                        ),
                        byPlayer[player.id].orEmpty(),
                        today
                    )
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // No numbers from before next to the error.
            rows = null
            loadFailed = true
        }
    }

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
        rows?.let { Table(players.zip(it), onOpen) }
    }
    if (loadFailed) {
        LoadFailed { attempt++ }
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
